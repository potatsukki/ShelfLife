package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.notifications.PantryNotificationScheduler
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.seconds
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class RecipeGenerationState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class AssistantState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class ChatMessage(
    val id: Long = System.nanoTime(),
    val text: String,
    val isUser: Boolean,
    val recipeUpdate: RecipeUpdateSuggestion? = null,
    val isApplied: Boolean = false
)

data class ScannerUiState(
    val isLoading: Boolean = false,
    val barcode: String? = null,
    val pendingIngredient: Ingredient? = null,
    val message: String? = null,
    val isError: Boolean = false
)

data class ReceiptImportState(
    val isLoading: Boolean = false,
    val rawText: String = "",
    val items: List<ReceiptCleanupItem> = emptyList(),
    val message: String? = null,
    val isError: Boolean = false
)

data class CookingCompletionState(
    val isApplying: Boolean = false,
    val message: String? = null,
    val hasShortfall: Boolean = false
)

data class PantryImportDraft(
    val shoppingItemId: Int,
    val name: String,
    val quantity: Double,
    val unit: String,
    val category: String = "Pantry",
    val location: String = "Pantry",
    val storageCondition: String = "Cool dry place",
    val dateType: String = IngredientDateType.NO_PRINTED_DATE,
    val expirationDate: String = "",
    val itemStatus: String = IngredientStatus.SEALED,
    val mergeIngredientId: Int? = null
)

data class PantryTransferState(
    val isApplying: Boolean = false,
    val message: String? = null
)

enum class PantrySortOption(val label: String) {
    EXPIRING_SOONEST("Expiring Soonest"),
    ALPHABETICAL_ASC("Alphabetical A-Z"),
    ALPHABETICAL_DESC("Alphabetical Z-A"),
    NEWEST_FIRST("Newest First"),
    OLDEST_FIRST("Oldest First"),
    QUANTITY_LOW_HIGH("Quantity Low to High"),
    QUANTITY_HIGH_LOW("Quantity High to Low")
}

@OptIn(ExperimentalCoroutinesApi::class)
class ShelfLifeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ShelfLifeDatabase.getDatabase(application)
    private val settingsStore = AppSettingsStore(application)
    private val repository = ShelfLifeRepository(db.dao, ShelfLifeAiService())
    private val firebaseConfigured = FirebaseApp.getApps(application).isNotEmpty()
    private val firebaseAuth: FirebaseAuth? = if (firebaseConfigured) FirebaseAuth.getInstance() else null

    val settingsState: StateFlow<SettingsState> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsState())

    private val _authUiState = MutableStateFlow(
        AuthUiState(
            isFirebaseConfigured = firebaseConfigured,
            errorMessage = if (firebaseConfigured) null else "Firebase is not configured. Add app/google-services.json, enable Email/Password auth, and sync the project."
        )
    )
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    private val _currentRoute = MutableStateFlow("splash")
    val currentRoute: StateFlow<String> = _currentRoute.asStateFlow()

    private val routeBackStack = mutableListOf<String>()

    private val noBackStackRoutes = setOf(
        "splash",
        "auth",
        "onboarding_1",
        "onboarding_2",
        "onboarding_3"
    )

    private val activeUserId: StateFlow<String> = authUiState
        .map { it.userId.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    init {
        viewModelScope.launch {
            settingsState
                .map { it.expirationAlerts to it.lowStockAlerts }
                .distinctUntilChanged()
                .collect {
                    PantryNotificationScheduler.refresh(getApplication(), settingsState.value)
                }
        }
        firebaseAuth?.addAuthStateListener { auth ->
            val user = auth.currentUser
            _authUiState.value = AuthUiState(
                isFirebaseConfigured = true,
                isAuthenticated = user != null,
                userId = user?.uid,
                email = user?.email,
                displayName = user?.displayName,
                photoUrl = user?.photoUrl?.toString()
            )
        }
    }

    fun finishSplash() {
        val settings = settingsState.value
        val auth = authUiState.value
        _currentRoute.value = when {
            !settings.onboardingCompleted -> "onboarding_1"
            auth.isAuthenticated -> "dashboard"
            else -> "auth"
        }
    }

    fun navigateTo(route: String) {
        val current = _currentRoute.value
        if (route == current) return

        if (current !in noBackStackRoutes && route !in noBackStackRoutes) {
            routeBackStack.remove(route)
            routeBackStack.add(current)
            if (routeBackStack.size > 30) routeBackStack.removeAt(0)
        }

        _currentRoute.value = route
    }

    fun navigateBackTo(route: String) {
        _currentRoute.value = route
    }

    fun handleSystemBack(): Boolean {
        val current = _currentRoute.value

        if (current == "dashboard" || current in noBackStackRoutes) {
            return false
        }

        val previous = routeBackStack.removeLastOrNull()
        if (!previous.isNullOrBlank() && previous != current) {
            _currentRoute.value = previous
            return true
        }

        _currentRoute.value = parentRouteFor(current)
        return true
    }

    private fun parentRouteFor(route: String): String {
        return when {
            route == "ingredient_detail" || route == "add_ingredient" || route == "edit_ingredient" -> "pantry"
            route == "recipe_detail" || route == "add_recipe" || route == "edit_recipe" || route == "chat" || route == "guided_cooking" -> "recipes"
            route.startsWith("recipe_detail/") -> "dashboard"
            route == "settings" || route == "profile" -> "dashboard"
            route == "pantry" || route == "scanner" || route == "recipes" || route == "shopping_list" -> "dashboard"
            else -> "dashboard"
        }
    }

    fun nextOnboarding() {
        when (_currentRoute.value) {
            "onboarding_1" -> navigateTo("onboarding_2")
            "onboarding_2" -> navigateTo("onboarding_3")
            else -> completeOnboarding()
        }
    }

    fun skipOnboarding() = completeOnboarding()

    private fun completeOnboarding() {
        viewModelScope.launch {
            settingsStore.setOnboardingCompleted(true)
            navigateTo(if (authUiState.value.isAuthenticated) "dashboard" else "auth")
        }
    }

    fun signIn(email: String, password: String) {
        val auth = firebaseAuth ?: return setAuthSetupError()
        if (!validateCredentials(email, password)) return
        _authUiState.value = _authUiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { auth.signInWithEmailAndPassword(email.trim(), password).awaitTask() }
                .onSuccess { navigateTo("dashboard") }
                .onFailure { setAuthError(it.localizedMessage ?: "Unable to sign in.") }
            _authUiState.value = _authUiState.value.copy(isLoading = false)
        }
    }

    fun signUp(email: String, password: String, firstName: String = "", lastName: String = "") {
        val auth = firebaseAuth ?: return setAuthSetupError()
        if (!validateCredentials(email, password)) return
        val fullName = listOf(firstName.trim(), lastName.trim())
            .filter { it.isNotBlank() }
            .joinToString(" ")
        _authUiState.value = _authUiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                val result = auth.createUserWithEmailAndPassword(email.trim(), password).awaitTask()
                if (fullName.isNotBlank()) {
                    result.user?.updateProfile(
                        userProfileChangeRequest { displayName = fullName }
                    )?.awaitTask()
                    _authUiState.value = _authUiState.value.copy(displayName = fullName)
                }
            }
                .onSuccess { navigateTo("dashboard") }
                .onFailure { setAuthError(it.localizedMessage ?: "Unable to create account.") }
            _authUiState.value = _authUiState.value.copy(isLoading = false)
        }
    }

    fun signInWithGoogleIdToken(idToken: String?) {
        val auth = firebaseAuth ?: return setAuthSetupError()
        if (idToken.isNullOrBlank()) {
            setAuthError("Google sign-in did not return a valid token. Try again.")
            return
        }
        _authUiState.value = _authUiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            runCatching { auth.signInWithCredential(credential).awaitTask() }
                .onSuccess { navigateTo("dashboard") }
                .onFailure { setAuthError(it.localizedMessage ?: "Unable to sign in with Google.") }
            _authUiState.value = _authUiState.value.copy(isLoading = false)
        }
    }

    fun signOut() {
        firebaseAuth?.signOut()
        _chatHistory.value = emptyList()
        _scannerUiState.value = ScannerUiState()
        navigateTo("auth")
    }

    fun clearAuthError() {
        _authUiState.value = _authUiState.value.copy(errorMessage = null)
    }

    fun reportAuthError(message: String) {
        setAuthError(message)
    }

    private fun validateCredentials(email: String, password: String): Boolean {
        return when {
            email.isBlank() || !email.contains("@") -> {
                setAuthError("Enter a valid email address.")
                false
            }
            password.length < 6 -> {
                setAuthError("Password must be at least 6 characters.")
                false
            }
            else -> true
        }
    }

    private fun setAuthSetupError() = setAuthError("Firebase is not configured. Add app/google-services.json and sync Android Studio.")

    private fun setAuthError(message: String) {
        _authUiState.value = _authUiState.value.copy(
            isLoading = false,
            isFirebaseConfigured = firebaseConfigured,
            errorMessage = message
        )
    }

    val ingredients: StateFlow<List<Ingredient>> = activeUserId
        .flatMapLatest { userId -> if (userId.isBlank()) flowOf(emptyList()) else repository.allIngredients(userId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _pantrySortOption = MutableStateFlow(PantrySortOption.EXPIRING_SOONEST)
    val pantrySortOption: StateFlow<PantrySortOption> = _pantrySortOption.asStateFlow()

    val filteredIngredients: StateFlow<List<Ingredient>> = combine(
        ingredients, searchQuery, _selectedCategory, pantrySortOption
    ) { list, query, cat, sortOption ->
        list.asSequence()
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
            .filter { ingredient ->
                when (cat) {
                    "All" -> true
                    "Fresh" -> ingredient.hasTrackedDate && getDaysExpiry(ingredient.expirationDate) > 3
                    "Expiring Soon" -> ingredient.hasTrackedDate &&
                        getDaysExpiry(ingredient.expirationDate) in 0..3
                    "Expired" -> ingredient.hasTrackedDate && getDaysExpiry(ingredient.expirationDate) < 0
                    "Low Stock" -> ingredient.lowStockReminderEnabled &&
                        ingredient.quantity <= ingredient.lowStockThreshold
                    else -> ingredient.category.equals(cat, ignoreCase = true)
                }
            }
            .toList()
            .sortForPantry(sortOption)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(cat: String) {
        _selectedCategory.value = cat
    }

    fun setPantrySortOption(option: PantrySortOption) {
        _pantrySortOption.value = option
    }

    private fun List<Ingredient>.sortForPantry(option: PantrySortOption): List<Ingredient> =
        when (option) {
            PantrySortOption.EXPIRING_SOONEST -> sortedWith(
                compareBy<Ingredient> { if (it.hasTrackedDate) 0 else 1 }
                    .thenBy { it.expirationDate }
                    .thenBy { it.name.lowercase(Locale.US) }
            )
            PantrySortOption.ALPHABETICAL_ASC -> sortedBy { it.name.lowercase(Locale.US) }
            PantrySortOption.ALPHABETICAL_DESC -> sortedByDescending { it.name.lowercase(Locale.US) }
            PantrySortOption.NEWEST_FIRST -> sortedWith(
                compareByDescending<Ingredient> { it.purchaseDate }
                    .thenByDescending { it.id }
            )
            PantrySortOption.OLDEST_FIRST -> sortedWith(
                compareBy<Ingredient> { it.purchaseDate }
                    .thenBy { it.id }
            )
            PantrySortOption.QUANTITY_LOW_HIGH -> sortedBy { it.quantity }
            PantrySortOption.QUANTITY_HIGH_LOW -> sortedByDescending { it.quantity }
        }

    fun addIngredient(
        name: String,
        category: String,
        quantity: Double,
        unit: String,
        expirationDate: String,
        purchaseDate: String,
        location: String,
        lowStockThreshold: Double,
        notes: String,
        dateType: String,
        itemStatus: String,
        openedDate: String,
        storageCondition: String,
        brand: String,
        barcode: String,
        packageSize: String,
        store: String,
        price: Double?
    ) {
        val userId = activeUserId.value
        if (userId.isBlank()) return
        viewModelScope.launch {
            repository.insertIngredient(
                Ingredient(
                    userId = userId,
                    name = name.trim(),
                    category = category,
                    quantity = quantity,
                    unit = unit,
                    expirationDate = expirationDate,
                    purchaseDate = purchaseDate,
                    location = location,
                    lowStockThreshold = lowStockThreshold,
                    notes = notes.trim(),
                    dateType = dateType,
                    itemStatus = itemStatus,
                    openedDate = openedDate,
                    storageCondition = storageCondition,
                    brand = brand.trim(),
                    barcode = barcode.trim(),
                    packageSize = packageSize.trim(),
                    store = store.trim(),
                    price = price
                )
            )
        }
    }

    fun updateIngredient(ingredient: Ingredient) {
        val userId = activeUserId.value
        if (userId.isBlank()) return
        viewModelScope.launch {
            repository.insertIngredient(ingredient.copy(userId = userId))
        }
    }

    fun deleteIngredient(ingredient: Ingredient) {
        val userId = activeUserId.value
        if (userId.isBlank()) return
        viewModelScope.launch {
            repository.deleteIngredient(ingredient.copy(userId = userId))
        }
    }

    fun getIngredientDetails(id: Int): Flow<Ingredient?> {
        val userId = activeUserId.value
        return if (userId.isBlank()) flowOf(null) else flow {
            emit(repository.getIngredientById(id, userId))
        }
    }

    val shoppingItems: StateFlow<List<ShoppingItem>> = activeUserId
        .flatMapLatest { userId -> if (userId.isBlank()) flowOf(emptyList()) else repository.allShoppingItems(userId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addShoppingItem(
        name: String,
        category: String,
        sourceRecipeName: String? = null,
        quantity: Double = 1.0,
        unit: String = "pcs"
    ) {
        val userId = activeUserId.value
        val cleanName = name.trim()
        if (userId.isBlank() || cleanName.isBlank()) return
        viewModelScope.launch {
            repository.mergeShoppingItem(
                ShoppingItem(
                    userId = userId,
                    name = cleanName,
                    quantity = quantity,
                    unit = unit.ifBlank { "pcs" },
                    category = category,
                    sourceRecipeName = sourceRecipeName,
                    sourceRecipeNamesJson = encodeRecipeSources(
                        listOfNotNull(sourceRecipeName?.takeIf { it.isNotBlank() })
                    )
                )
            )
        }
    }

    fun toggleShoppingItem(item: ShoppingItem) {
        viewModelScope.launch {
            repository.updateShoppingItem(item.copy(isChecked = !item.isChecked))
        }
    }

    fun deleteShoppingItem(item: ShoppingItem) {
        viewModelScope.launch {
            repository.deleteShoppingItem(item)
        }
    }

    fun clearCheckedShoppingItems() {
        val userId = activeUserId.value
        if (userId.isBlank()) return
        viewModelScope.launch {
            repository.clearCheckedShoppingItems(userId)
        }
    }

    val suggestedRecipes: StateFlow<List<SavedRecipe>> = activeUserId
        .flatMapLatest { userId ->
            if (userId.isBlank()) flowOf(emptyList())
            else repository.recommendedRecipes(userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _recipeGenerationState = MutableStateFlow(RecipeGenerationState())
    val recipeGenerationState: StateFlow<RecipeGenerationState> = _recipeGenerationState.asStateFlow()
    val recipeLoading: StateFlow<Boolean> = recipeGenerationState
        .map { it.isLoading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    private val _regeneratingRecipeId = MutableStateFlow<String?>(null)
    val regeneratingRecipeId: StateFlow<String?> = _regeneratingRecipeId.asStateFlow()

    val userRecipes: StateFlow<List<SavedRecipe>> = activeUserId
        .flatMapLatest { userId -> if (userId.isBlank()) flowOf(emptyList()) else repository.userRecipes(userId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteRecipes: StateFlow<List<SavedRecipe>> = activeUserId
        .flatMapLatest { userId -> if (userId.isBlank()) flowOf(emptyList()) else repository.favoriteRecipes(userId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedRecipe = MutableStateFlow<SavedRecipe?>(null)
    val selectedRecipe: StateFlow<SavedRecipe?> = _selectedRecipe.asStateFlow()

    fun selectRecipe(recipe: SavedRecipe?) {
        _selectedRecipe.value = recipe
    }

    fun triggerRecipeSuggestions() {
        val userId = activeUserId.value
        if (userId.isBlank()) return
        if (!settingsState.value.smartRecipeIdeas) {
            _recipeGenerationState.value = RecipeGenerationState(errorMessage = "Smart recipe suggestions are disabled in Settings.")
            return
        }
        if (ingredients.value.isEmpty()) {
            _recipeGenerationState.value = RecipeGenerationState(errorMessage = "Add pantry ingredients before generating recipes.")
            return
        }
        viewModelScope.launch {
            _recipeGenerationState.value = RecipeGenerationState(isLoading = true)
            runCatching { repository.getAIRecipeSuggestions(userId, ingredients.value) }
                .onSuccess { recipes ->
                    val existing = suggestedRecipes.value
                    val merged = if (existing.isEmpty()) {
                        recipes
                    } else {
                        val seenNames = existing.map { normalizedRecipeName(it.name) }.toMutableSet()
                        val seenIds = existing.map { it.id }.toMutableSet()
                        existing + recipes.filter { recipe ->
                            val normalizedName = normalizedRecipeName(recipe.name)
                            val isNew = normalizedName !in seenNames && recipe.id !in seenIds
                            if (isNew) {
                                seenNames += normalizedName
                                seenIds += recipe.id
                            }
                            isNew
                        }
                    }
                    repository.replaceRecommendedRecipes(userId, merged)
                    _recipeGenerationState.value = RecipeGenerationState(
                        errorMessage = if (merged.size == existing.size) "No new recipe suggestions were returned. Try again later." else null
                    )
                }
                .onFailure { error ->
                    _recipeGenerationState.value = RecipeGenerationState(
                        errorMessage = error.localizedMessage ?: "AI recipes are unavailable right now."
                    )
                }
        }
    }

    fun toggleSaveRecipe(recipe: SavedRecipe) {
        val userId = activeUserId.value
        if (userId.isBlank()) return
        viewModelScope.launch {
            val userRecipe = recipe.copy(userId = userId)
            val persisted = repository.getSavedRecipe(userRecipe.id, userId)
            if (persisted != null) {
                if (persisted.origin == RecipeOrigin.USER) {
                    val updated = persisted.copy(
                        isFavorite = !persisted.isFavorite,
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.insertSavedRecipe(updated)
                    if (_selectedRecipe.value?.id == updated.id) _selectedRecipe.value = updated
                } else {
                    repository.deleteSavedRecipe(persisted)
                }
            } else {
                repository.insertSavedRecipe(
                    userRecipe.copy(
                        isFavorite = true,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun regenerateSuggestedRecipe(recipe: SavedRecipe) {
        val userId = activeUserId.value
        if (userId.isBlank() || _regeneratingRecipeId.value != null) return
        if (ingredients.value.isEmpty()) {
            _recipeGenerationState.value = RecipeGenerationState(errorMessage = "Add pantry ingredients before regenerating recipes.")
            return
        }
        viewModelScope.launch {
            _regeneratingRecipeId.value = recipe.id
            _recipeGenerationState.value = RecipeGenerationState()
            runCatching { repository.getAIRecipeSuggestions(userId, ingredients.value) }
                .onSuccess { generated ->
                    val current = suggestedRecipes.value
                    val selectedName = normalizedRecipeName(recipe.name)
                    val blockedNames = current
                        .filterNot { it.id == recipe.id }
                        .map { normalizedRecipeName(it.name) }
                        .toSet()
                    val replacement = generated.firstOrNull {
                        val name = normalizedRecipeName(it.name)
                        name != selectedName && name !in blockedNames
                    }

                    if (replacement == null) {
                        _recipeGenerationState.value = RecipeGenerationState(
                            errorMessage = "No different replacement recipe was returned. Try again."
                        )
                    } else {
                        val updated = current.map {
                            if (it.id == recipe.id) replacement.copy(userId = userId) else it
                        }
                        repository.replaceRecommendedRecipes(userId, updated)
                        if (_selectedRecipe.value?.id == recipe.id) {
                            _selectedRecipe.value = replacement.copy(userId = userId)
                        }
                    }
                }
                .onFailure { error ->
                    _recipeGenerationState.value = RecipeGenerationState(
                        errorMessage = error.localizedMessage ?: "Recipe regeneration failed."
                    )
                }
            _regeneratingRecipeId.value = null
        }
    }

    fun deleteMatchedRecipe(recipe: SavedRecipe) {
        val userId = activeUserId.value
        if (userId.isBlank()) return
        viewModelScope.launch {
            val filtered = suggestedRecipes.value.filterNot { it.id == recipe.id }
            repository.replaceRecommendedRecipes(userId, filtered)
            repository.deleteSavedRecipeById(recipe.id, userId)
            if (_selectedRecipe.value?.id == recipe.id) _selectedRecipe.value = null
            val route = _currentRoute.value
            if (route == "recipe_detail" || route.startsWith("recipe_detail/")) {
                navigateTo("recipes")
            }
        }
    }

    private fun normalizedRecipeName(name: String): String =
        name.trim().lowercase(Locale.US)

    fun isRecipeSaved(id: String): Flow<Boolean> {
        val userId = activeUserId.value
        return if (userId.isBlank()) flowOf(false) else repository.isRecipeSavedFlow(id, userId)
    }

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()
    private val _rememberedUserName = MutableStateFlow<String?>(null)

    private val _assistantState = MutableStateFlow(AssistantState())
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()
    val chatLoading: StateFlow<Boolean> = assistantState
        .map { it.isLoading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun sendChatMessage(message: String) {
        val cleanMessage = message.trim()
        if (cleanMessage.isBlank()) return
        viewModelScope.launch {
            val previousHistory = _chatHistory.value
            extractSharedName(cleanMessage)?.let { _rememberedUserName.value = it }
            val repeatedQuestion = previousHistory
                .filter { it.isUser }
                .any { normalizedChatText(it.text) == normalizedChatText(cleanMessage) }
            val current = previousHistory + ChatMessage(text = cleanMessage, isUser = true)
            _chatHistory.value = current

            if (asksForRememberedName(cleanMessage)) {
                val name = _rememberedUserName.value
                    ?: authUiState.value.displayName?.takeIf { it.isNotBlank() }
                val reply = if (name.isNullOrBlank()) {
                    "I do not know your name yet. Tell me your name and I will remember it during this chat."
                } else {
                    "Your name is $name."
                }
                _chatHistory.value += ChatMessage(text = reply, isUser = false)
                return@launch
            }

            _assistantState.value = AssistantState(isLoading = true)

            runCatching {
                repository.askAssistant(
                    chatHistory = current.dropLast(1).map { it.text to it.isUser },
                    latestUserMessage = cleanMessage,
                    recipeContext = selectedRecipe.value?.toRecipeContext(ingredients.value),
                    pantryIngredients = ingredients.value,
                    isRepeatedQuestion = repeatedQuestion
                )
            }
                .onSuccess { reply ->
                    _chatHistory.value += ChatMessage(
                        text = reply.message,
                        isUser = false,
                        recipeUpdate = reply.recipeUpdate
                    )
                    _assistantState.value = AssistantState()
                }
                .onFailure { error ->
                    _assistantState.value = AssistantState(
                        errorMessage = error.localizedMessage ?: "Kitchen AI is unavailable right now."
                    )
                    _chatHistory.value += ChatMessage(
                        text = "Kitchen AI is unavailable right now. Check your Cloudflare Worker setup and try again.",
                        isUser = false
                    )
                }
        }
    }

    private fun normalizedChatText(text: String): String =
        text.trim().lowercase(Locale.US).replace(Regex("\\s+"), " ")

    private fun extractSharedName(text: String): String? {
        val patterns = listOf(
            Regex("""(?i)\bmy name is\s+([a-z][a-z\s.'-]{0,40})"""),
            Regex("""(?i)\bi am\s+([a-z][a-z\s.'-]{0,40})"""),
            Regex("""(?i)\bi'm\s+([a-z][a-z\s.'-]{0,40})""")
        )
        return patterns.firstNotNullOfOrNull { pattern ->
            pattern.find(text)?.groupValues?.getOrNull(1)
        }?.trim(' ', '.', '!', '?', ',')
            ?.split(Regex("\\s+"))
            ?.take(3)
            ?.joinToString(" ")
            ?.takeIf { it.isNotBlank() }
    }

    private fun asksForRememberedName(text: String): Boolean {
        val normalized = normalizedChatText(text)
        return "my name" in normalized || "know my name" in normalized || "what is my name" in normalized
    }

    fun saveUserRecipe(recipe: SavedRecipe) {
        val userId = activeUserId.value
        if (userId.isBlank()) return
        val now = System.currentTimeMillis()
        val persisted = recipe.copy(
            userId = userId,
            origin = RecipeOrigin.USER,
            createdAt = recipe.createdAt.takeIf { it > 0L } ?: now,
            updatedAt = now,
            timestamp = now
        )
        viewModelScope.launch {
            repository.insertSavedRecipe(persisted)
            _selectedRecipe.value = persisted
            navigateTo("recipe_detail")
        }
    }

    fun deleteUserRecipe(recipe: SavedRecipe) {
        val userId = activeUserId.value
        if (userId.isBlank() || recipe.origin != RecipeOrigin.USER) return
        viewModelScope.launch {
            repository.deleteSavedRecipeById(recipe.id, userId)
            if (_selectedRecipe.value?.id == recipe.id) _selectedRecipe.value = null
            navigateTo("recipes")
        }
    }

    fun duplicateUserRecipe(recipe: SavedRecipe) {
        val now = System.currentTimeMillis()
        saveUserRecipe(
            recipe.copy(
                id = "user-$now",
                name = "${recipe.name} Copy",
                origin = RecipeOrigin.USER,
                isFavorite = false,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    private val _cookingCompletionState = MutableStateFlow(CookingCompletionState())
    val cookingCompletionState: StateFlow<CookingCompletionState> = _cookingCompletionState.asStateFlow()

    fun applyCookingUsage(recipe: SavedRecipe, servings: Int, customUsages: List<IngredientUsage>? = null) {
        val userId = activeUserId.value
        if (userId.isBlank()) return
        val usages = customUsages ?: recipe.scaledIngredients(servings)
            .filter { it.required && it.quantity != null && it.quantity > 0.0 }
            .map { IngredientUsage(it.name, it.quantity!!, it.unit.ifBlank { "pcs" }) }
        viewModelScope.launch {
            _cookingCompletionState.value = CookingCompletionState(isApplying = true)
            runCatching { repository.deductPantry(userId, usages) }
                .onSuccess { plan ->
                    val base = "${plan.updatedCount} ingredients updated, ${plan.removedCount} items removed."
                    val warning = if (plan.shortfalls.isNotEmpty()) {
                        " Pantry had insufficient stock for ${plan.shortfalls.joinToString { it.name }}."
                    } else ""
                    _cookingCompletionState.value = CookingCompletionState(
                        message = base + warning,
                        hasShortfall = plan.shortfalls.isNotEmpty()
                    )
                }
                .onFailure {
                    _cookingCompletionState.value = CookingCompletionState(
                        message = it.localizedMessage ?: "Unable to update Pantry."
                    )
                }
        }
    }

    fun clearCookingCompletion() {
        _cookingCompletionState.value = CookingCompletionState()
    }

    private val _pantryTransferState = MutableStateFlow(PantryTransferState())
    val pantryTransferState: StateFlow<PantryTransferState> = _pantryTransferState.asStateFlow()

    fun transferCheckedItemsToPantry(drafts: List<PantryImportDraft>) {
        val userId = activeUserId.value
        if (userId.isBlank() || drafts.isEmpty()) return
        val pantryById = ingredients.value.associateBy { it.id }
        val updates = mutableListOf<Ingredient>()
        val inserts = mutableListOf<Ingredient>()
        val transferredIds = mutableListOf<Int>()

        drafts.forEach { draft ->
            if (draft.name.isBlank() || draft.quantity <= 0.0 || draft.unit.isBlank()) return@forEach
            val existing = draft.mergeIngredientId?.let(pantryById::get)
            if (existing != null && UnitConverter.areCompatible(existing.unit, draft.unit)) {
                val converted = UnitConverter.convert(draft.quantity, draft.unit, existing.unit) ?: return@forEach
                updates += existing.copy(quantity = existing.quantity + converted)
            } else {
                inserts += Ingredient(
                    userId = userId,
                    name = draft.name.trim(),
                    category = draft.category,
                    quantity = draft.quantity,
                    unit = draft.unit,
                    expirationDate = draft.expirationDate,
                    purchaseDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                    location = draft.location,
                    dateType = draft.dateType,
                    itemStatus = draft.itemStatus,
                    storageCondition = draft.storageCondition
                )
            }
            transferredIds += draft.shoppingItemId
        }

        viewModelScope.launch {
            _pantryTransferState.value = PantryTransferState(isApplying = true)
            runCatching {
                repository.transferShoppingToPantry(userId, updates, inserts, transferredIds)
            }.onSuccess {
                _pantryTransferState.value = PantryTransferState(
                    message = "${transferredIds.size} shopping items added to Pantry."
                )
            }.onFailure {
                _pantryTransferState.value = PantryTransferState(
                    message = it.localizedMessage ?: "Unable to add shopping items to Pantry."
                )
            }
        }
    }

    fun clearPantryTransferState() {
        _pantryTransferState.value = PantryTransferState()
    }

    fun deleteIngredients(ids: Set<Int>) {
        val userId = activeUserId.value
        if (userId.isBlank() || ids.isEmpty()) return
        viewModelScope.launch {
            repository.deleteIngredientsByIds(ids.toList(), userId)
        }
    }

    fun applyRecipeUpdate(messageId: Long) {
        val message = _chatHistory.value.firstOrNull { it.id == messageId } ?: return
        val update = message.recipeUpdate ?: return
        val currentRecipe = _selectedRecipe.value ?: return
        val updatedRecipe = currentRecipe.copy(
            ingredientsCsv = update.ingredients.joinToString(", ") { it.displayText },
            stepsCsv = update.steps.joinToString("|"),
            ingredientsJson = JSONArray(update.ingredients.map {
                JSONObject()
                    .put("name", it.name)
                    .put("quantity", it.quantity ?: JSONObject.NULL)
                    .put("unit", it.unit)
                    .put("required", it.required)
            }).toString(),
            stepsJson = JSONArray(update.steps.map { JSONObject().put("text", it) }).toString(),
            whySuggested = update.summary.ifBlank { currentRecipe.whySuggested }
        )
        _selectedRecipe.value = updatedRecipe
        val updatedRecommendations = suggestedRecipes.value.map {
            if (it.id == updatedRecipe.id) updatedRecipe else it
        }
        _chatHistory.value = _chatHistory.value.map {
            if (it.id == messageId) it.copy(isApplied = true) else it
        }
        val userId = activeUserId.value
        if (userId.isNotBlank()) {
            viewModelScope.launch {
                repository.replaceRecommendedRecipes(userId, updatedRecommendations)
                if (repository.isRecipeSavedSync(updatedRecipe.id, userId)) {
                    repository.insertSavedRecipe(updatedRecipe.copy(userId = userId))
                }
            }
        }
    }

    fun clearChat() {
        _chatHistory.value = emptyList()
        _assistantState.value = AssistantState()
    }

    fun setDarkMode(value: Boolean) = viewModelScope.launch { settingsStore.setDarkMode(value) }
    fun setExpirationAlerts(value: Boolean) = viewModelScope.launch {
        settingsStore.setExpirationAlerts(value)
        PantryNotificationScheduler.refresh(getApplication(), settingsState.value.copy(expirationAlerts = value))
    }
    fun setLowStockAlerts(value: Boolean) = viewModelScope.launch {
        settingsStore.setLowStockAlerts(value)
        PantryNotificationScheduler.refresh(getApplication(), settingsState.value.copy(lowStockAlerts = value))
    }
    fun setVegetarianMode(value: Boolean) = viewModelScope.launch { settingsStore.setVegetarianMode(value) }
    fun setSmartRecipeIdeas(value: Boolean) = viewModelScope.launch { settingsStore.setSmartRecipeIdeas(value) }
    fun setMetricMeasurements(value: Boolean) = viewModelScope.launch { settingsStore.setMetricMeasurements(value) }
    fun setHouseholdSharing(value: Boolean) = viewModelScope.launch { settingsStore.setHouseholdSharing(value) }

    private val _scannerUiState = MutableStateFlow(ScannerUiState())
    val scannerUiState: StateFlow<ScannerUiState> = _scannerUiState.asStateFlow()

    private val _receiptImportState = MutableStateFlow(ReceiptImportState())
    val receiptImportState: StateFlow<ReceiptImportState> = _receiptImportState.asStateFlow()

    fun cleanupReceiptText(rawText: String) {
        if (rawText.isBlank()) {
            _receiptImportState.value = ReceiptImportState(
                rawText = rawText,
                message = "No receipt text detected. Try scanning again.",
                isError = true
            )
            return
        }
        _receiptImportState.value = ReceiptImportState(isLoading = true, rawText = rawText)
        viewModelScope.launch {
            runCatching { repository.cleanupReceiptItems(rawText) }
                .onSuccess { items ->
                    _receiptImportState.value = if (items.isEmpty()) {
                        ReceiptImportState(
                            rawText = rawText,
                            message = "No grocery items found. Try a clearer receipt photo.",
                            isError = true
                        )
                    } else {
                        ReceiptImportState(
                            rawText = rawText,
                            items = items,
                            message = "Found ${items.size} receipt items. Review before adding."
                        )
                    }
                }
                .onFailure { error ->
                    _receiptImportState.value = ReceiptImportState(
                        rawText = rawText,
                        message = error.localizedMessage ?: "Receipt cleanup failed.",
                        isError = true
                    )
                }
        }
    }

    fun resetReceiptImport() {
        _receiptImportState.value = ReceiptImportState()
    }

    fun addReceiptItemsToPantry(items: List<ReceiptCleanupItem>) {
        val userId = activeUserId.value
        if (userId.isBlank()) {
            _receiptImportState.value = _receiptImportState.value.copy(
                isLoading = false,
                message = "Sign in before adding receipt items.",
                isError = true
            )
            return
        }
        if (items.isEmpty()) {
            _receiptImportState.value = _receiptImportState.value.copy(
                isLoading = false,
                message = "Select at least one item to add.",
                isError = true
            )
            return
        }

        viewModelScope.launch {
            runCatching {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                var addedCount = 0

                items.forEach { item ->
                    val cleanName = item.name.trim()
                    if (cleanName.isBlank()) return@forEach

                    val quantity = item.quantity.takeIf { it > 0.0 } ?: 1.0
                    val unit = item.unit.ifBlank { "pcs" }
                    val category = item.category.ifBlank { "Pantry" }
                    val brand = item.brand.trim()
                    val existing = ingredients.value.firstOrNull { ingredient ->
                        IngredientNormalizer.matches(ingredient.name, cleanName)
                    }

                    if (existing != null && UnitConverter.areCompatible(existing.unit, unit)) {
                        val convertedQuantity = UnitConverter.convert(quantity, unit, existing.unit) ?: quantity
                        val existingDate = "${existing.dateType} ${existing.expirationDate.ifBlank { "none" }}"
                        val mergeNote = "Merged from receipt scan on $today: added ${formatReceiptImportQuantity(quantity)} $unit with no printed date. Existing date: $existingDate."
                        val mergedNotes = listOf(existing.notes.trim(), mergeNote)
                            .filter { it.isNotBlank() }
                            .joinToString("\n")

                        repository.insertIngredient(
                            existing.copy(
                                quantity = existing.quantity + convertedQuantity,
                                notes = mergedNotes
                            )
                        )
                    } else {
                        repository.insertIngredient(
                            Ingredient(
                                userId = userId,
                                name = cleanName,
                                brand = brand,
                                category = category,
                                quantity = quantity,
                                unit = unit,
                                expirationDate = "",
                                purchaseDate = today,
                                location = "Pantry",
                                lowStockThreshold = -1.0,
                                notes = "Imported from receipt scan.",
                                dateType = IngredientDateType.NO_PRINTED_DATE,
                                itemStatus = IngredientStatus.SEALED,
                                openedDate = "",
                                storageCondition = "Cool dry place",
                                barcode = "",
                                packageSize = "",
                                store = "",
                                price = null
                            )
                        )
                    }
                    addedCount++
                }
                addedCount
            }.onSuccess { addedCount ->
                _receiptImportState.value = _receiptImportState.value.copy(
                    isLoading = false,
                    message = if (addedCount > 0) {
                        "Added $addedCount receipt items to Pantry."
                    } else {
                        "Select at least one item to add."
                    },
                    isError = addedCount == 0
                )
            }.onFailure { error ->
                _receiptImportState.value = _receiptImportState.value.copy(
                    isLoading = false,
                    message = error.localizedMessage ?: "Unable to add receipt items.",
                    isError = true
                )
            }
        }
    }

    fun scanRealBarcode(context: android.content.Context) {
        if (_scannerUiState.value.isLoading) return
        _scannerUiState.value = ScannerUiState(isLoading = true, message = "Opening camera scanner...")
        viewModelScope.launch {
            kotlinx.coroutines.delay(20.seconds)
            if (_scannerUiState.value.isLoading && _scannerUiState.value.barcode.isNullOrBlank()) {
                _scannerUiState.value = ScannerUiState(
                    message = "Scanner took too long. Try again or enter the barcode manually.",
                    isError = true
                )
            }
        }
        try {
            val scanner = com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(context)
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    val code = barcode.rawValue
                    if (!code.isNullOrBlank()) lookupBarcode(code, allowWhileLoading = true) else {
                        _scannerUiState.value = ScannerUiState(
                            message = "No barcode was captured. Try again or enter the code manually.",
                            isError = true
                        )
                    }
                }
                .addOnFailureListener {
                    _scannerUiState.value = ScannerUiState(
                        message = "Scanner is not available here. Try a real device or enter the barcode manually.",
                        isError = true
                    )
                }
        } catch (e: Exception) {
            _scannerUiState.value = ScannerUiState(
                message = "Scanning failed to initialize: ${e.localizedMessage}",
                isError = true
            )
        }
    }

    fun lookupBarcode(barcode: String, allowWhileLoading: Boolean = false) {
        if (_scannerUiState.value.isLoading && !allowWhileLoading) return
        val userId = activeUserId.value
        val codeToLookup = barcode.trim()
        if (userId.isBlank()) {
            _scannerUiState.value = ScannerUiState(message = "Sign in before adding pantry items.", isError = true)
            return
        }
        if (codeToLookup.isBlank()) {
            _scannerUiState.value = ScannerUiState(message = "Enter a barcode first.", isError = true)
            return
        }
        _scannerUiState.value = ScannerUiState(isLoading = true, barcode = codeToLookup, message = "Looking up barcode...")
        viewModelScope.launch {
            runCatching { withTimeout(12.seconds) { repository.lookupProduct(userId, codeToLookup) } }
                .onSuccess { fetched ->
                    _scannerUiState.value = if (fetched != null) {
                        ScannerUiState(
                            barcode = codeToLookup,
                            pendingIngredient = fetched,
                            message = "Product found. Review it before adding to your pantry."
                        )
                    } else {
                        ScannerUiState(
                            barcode = codeToLookup,
                            message = "No product match found for $codeToLookup. Try another barcode or add it manually.",
                            isError = true
                        )
                    }
                }
                .onFailure { error ->
                    val message = if (error is TimeoutCancellationException) {
                        "Product lookup took too long. Try again, search manually, or add the item yourself."
                    } else {
                        error.localizedMessage ?: "Product lookup failed."
                    }
                    _scannerUiState.value = ScannerUiState(
                        barcode = codeToLookup,
                        message = message,
                        isError = true
                    )
                }
        }
    }

    fun confirmScannedIngredient(editedIngredient: Ingredient? = null) {
        val ingredient = editedIngredient ?: _scannerUiState.value.pendingIngredient ?: return
        viewModelScope.launch {
            repository.insertIngredient(ingredient.copy(userId = activeUserId.value))
            _scannerUiState.value = ScannerUiState(
                message = "${ingredient.name} was added to your ${ingredient.location}.",
                barcode = _scannerUiState.value.barcode
            )
        }
    }

    fun resetScanner() {
        _scannerUiState.value = ScannerUiState()
    }

    fun clearAllPantryAndData() {
        val userId = activeUserId.value
        if (userId.isBlank()) return
        viewModelScope.launch {
            repository.clearAllData(userId)
        }
    }

    fun getDaysExpiry(targetDateStr: String): Int {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val expiry = format.parse(targetDateStr) ?: return 0
            val today = format.parse(format.format(Date())) ?: return 0
            ((expiry.time - today.time) / (1000 * 60 * 60 * 24)).toInt()
        } catch (_: Exception) {
            0
        }
    }

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T =
        suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { continuation.resume(it) }
            addOnFailureListener { continuation.resumeWithException(it) }
        }

    private fun SavedRecipe.toRecipeContext(pantry: List<Ingredient>): RecipeContext {
        val split = splitRecipeIngredients(this, pantry)
        return RecipeContext(
            recipeName = name,
            availableIngredients = split.available.map { it.displayText },
            missingIngredients = split.missing.map { it.displayText },
            steps = recipeSteps()
        )
    }

    private fun formatReceiptImportQuantity(value: Double): String =
        if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
        }
}

data class RecipeIngredientSplit(
    val available: List<RecipeIngredient>,
    val missing: List<RecipeIngredient>
)

fun splitRecipeIngredients(recipe: SavedRecipe, pantry: List<Ingredient>): RecipeIngredientSplit {
    val recipeIngredients = recipe.recipeIngredients()
    val pantryNames = pantry.map { normalizeIngredientName(it.name) }.filter { it.isNotBlank() }
    val available = mutableListOf<RecipeIngredient>()
    val missing = mutableListOf<RecipeIngredient>()

    recipeIngredients.forEach { ingredient ->
        val normalized = normalizeIngredientName(ingredient.name)
        val matched = pantryNames.any { pantryName ->
            pantryName == normalized ||
                (normalized.length >= 4 && pantryName.contains(normalized)) ||
                (pantryName.length >= 4 && normalized.contains(pantryName))
        }
        if (matched) available += ingredient else missing += ingredient
    }
    return RecipeIngredientSplit(available = available, missing = missing)
}

fun normalizeIngredientName(value: String): String {
    return IngredientNormalizer.normalize(value)
}

private fun encodeRecipeSources(values: List<String>): String =
    values.joinToString(prefix = "[", postfix = "]") { value ->
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    }
