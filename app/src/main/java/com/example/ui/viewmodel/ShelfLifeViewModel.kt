package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

data class ScannerUiState(
    val isLoading: Boolean = false,
    val barcode: String? = null,
    val pendingIngredient: Ingredient? = null,
    val message: String? = null,
    val isError: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class ShelfLifeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ShelfLifeDatabase.getDatabase(application)
    private val settingsStore = AppSettingsStore(application)
    private val repository = ShelfLifeRepository(db.dao, ShelfLifeAiService())
    private val firebaseConfigured = FirebaseApp.getApps(application).isNotEmpty()
    private val firebaseAuth: FirebaseAuth? = if (firebaseConfigured) FirebaseAuth.getInstance() else null

    val settingsState: StateFlow<SettingsState> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsState())

    val isDarkMode: StateFlow<Boolean> = settingsState
        .map { it.isDarkMode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _authUiState = MutableStateFlow(
        AuthUiState(
            isFirebaseConfigured = firebaseConfigured,
            errorMessage = if (firebaseConfigured) null else "Firebase is not configured. Add app/google-services.json, enable Email/Password auth, and sync the project."
        )
    )
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    private val _currentRoute = MutableStateFlow("splash")
    val currentRoute: StateFlow<String> = _currentRoute.asStateFlow()

    private val activeUserId: StateFlow<String> = authUiState
        .map { it.userId.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    init {
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
        _currentRoute.value = route
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

    fun signUp(email: String, password: String) {
        val auth = firebaseAuth ?: return setAuthSetupError()
        if (!validateCredentials(email, password)) return
        _authUiState.value = _authUiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { auth.createUserWithEmailAndPassword(email.trim(), password).awaitTask() }
                .onSuccess { navigateTo("dashboard") }
                .onFailure { setAuthError(it.localizedMessage ?: "Unable to create account.") }
            _authUiState.value = _authUiState.value.copy(isLoading = false)
        }
    }

    fun signOut() {
        firebaseAuth?.signOut()
        _suggestedRecipes.value = emptyList()
        _chatHistory.value = emptyList()
        _scannerUiState.value = ScannerUiState()
        navigateTo("auth")
    }

    fun clearAuthError() {
        _authUiState.value = _authUiState.value.copy(errorMessage = null)
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

    val filteredIngredients: StateFlow<List<Ingredient>> = combine(
        ingredients, searchQuery, _selectedCategory
    ) { list, query, cat ->
        list.asSequence()
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
            .filter { ingredient ->
                when (cat) {
                    "All" -> true
                    "Fresh" -> getDaysExpiry(ingredient.expirationDate) > 3
                    "Expiring Soon" -> getDaysExpiry(ingredient.expirationDate) in 0..3
                    "Expired" -> getDaysExpiry(ingredient.expirationDate) < 0
                    "Low Stock" -> ingredient.quantity <= ingredient.lowStockThreshold
                    else -> ingredient.category.equals(cat, ignoreCase = true)
                }
            }
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(cat: String) {
        _selectedCategory.value = cat
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
        notes: String
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
                    notes = notes.trim()
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

    fun addShoppingItem(name: String, category: String, sourceRecipeName: String? = null) {
        val userId = activeUserId.value
        val cleanName = name.trim()
        if (userId.isBlank() || cleanName.isBlank()) return
        val exists = shoppingItems.value.any { it.name.equals(cleanName, ignoreCase = true) && !it.isChecked }
        if (exists) return
        viewModelScope.launch {
            repository.insertShoppingItem(
                ShoppingItem(
                    userId = userId,
                    name = cleanName,
                    category = category,
                    sourceRecipeName = sourceRecipeName
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

    private val _suggestedRecipes = MutableStateFlow<List<SavedRecipe>>(emptyList())
    val suggestedRecipes: StateFlow<List<SavedRecipe>> = _suggestedRecipes.asStateFlow()

    private val _recipeGenerationState = MutableStateFlow(RecipeGenerationState())
    val recipeGenerationState: StateFlow<RecipeGenerationState> = _recipeGenerationState.asStateFlow()
    val recipeLoading: StateFlow<Boolean> = recipeGenerationState
        .map { it.isLoading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val savedRecipes: StateFlow<List<SavedRecipe>> = activeUserId
        .flatMapLatest { userId -> if (userId.isBlank()) flowOf(emptyList()) else repository.allSavedRecipes(userId) }
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
                    _suggestedRecipes.value = recipes
                    _recipeGenerationState.value = RecipeGenerationState(
                        errorMessage = if (recipes.isEmpty()) "No recipe suggestions were returned. Try again with more pantry items." else null
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
            if (repository.isRecipeSavedSync(userRecipe.id, userId)) {
                repository.deleteSavedRecipe(userRecipe)
            } else {
                repository.insertSavedRecipe(userRecipe)
            }
        }
    }

    fun isRecipeSaved(id: String): Flow<Boolean> {
        val userId = activeUserId.value
        return if (userId.isBlank()) flowOf(false) else repository.isRecipeSavedFlow(id, userId)
    }

    private val _chatHistory = MutableStateFlow<List<Pair<String, Boolean>>>(emptyList())
    val chatHistory: StateFlow<List<Pair<String, Boolean>>> = _chatHistory.asStateFlow()

    private val _assistantState = MutableStateFlow(AssistantState())
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()
    val chatLoading: StateFlow<Boolean> = assistantState
        .map { it.isLoading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun sendChatMessage(message: String) {
        val cleanMessage = message.trim()
        if (cleanMessage.isBlank()) return
        viewModelScope.launch {
            val current = _chatHistory.value + (cleanMessage to true)
            _chatHistory.value = current
            _assistantState.value = AssistantState(isLoading = true)
            runCatching { repository.askAssistant(current.dropLast(1), cleanMessage) }
                .onSuccess { reply ->
                    _chatHistory.value = _chatHistory.value + (reply to false)
                    _assistantState.value = AssistantState()
                }
                .onFailure { error ->
                    _assistantState.value = AssistantState(
                        errorMessage = error.localizedMessage ?: "Kitchen AI is unavailable right now."
                    )
                    _chatHistory.value = _chatHistory.value + ("Kitchen AI is unavailable right now. Check your Firebase Functions setup and try again." to false)
                }
        }
    }

    fun clearChat() {
        _chatHistory.value = emptyList()
        _assistantState.value = AssistantState()
    }

    fun setDarkMode(value: Boolean) = viewModelScope.launch { settingsStore.setDarkMode(value) }
    fun setExpirationAlerts(value: Boolean) = viewModelScope.launch { settingsStore.setExpirationAlerts(value) }
    fun setLowStockAlerts(value: Boolean) = viewModelScope.launch { settingsStore.setLowStockAlerts(value) }
    fun setVegetarianMode(value: Boolean) = viewModelScope.launch { settingsStore.setVegetarianMode(value) }
    fun setSmartRecipeIdeas(value: Boolean) = viewModelScope.launch { settingsStore.setSmartRecipeIdeas(value) }
    fun setMetricMeasurements(value: Boolean) = viewModelScope.launch { settingsStore.setMetricMeasurements(value) }
    fun setHouseholdSharing(value: Boolean) = viewModelScope.launch { settingsStore.setHouseholdSharing(value) }

    private val _scannerUiState = MutableStateFlow(ScannerUiState())
    val scannerUiState: StateFlow<ScannerUiState> = _scannerUiState.asStateFlow()

    fun scanRealBarcode(context: android.content.Context) {
        _scannerUiState.value = ScannerUiState(isLoading = true, message = "Opening camera scanner...")
        try {
            val scanner = com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(context)
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    val code = barcode.rawValue
                    if (!code.isNullOrBlank()) lookupBarcode(code) else {
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

    fun lookupBarcode(barcode: String) {
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
        viewModelScope.launch {
            _scannerUiState.value = ScannerUiState(isLoading = true, barcode = codeToLookup, message = "Looking up barcode...")
            runCatching { repository.lookupProduct(userId, codeToLookup) }
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
                    _scannerUiState.value = ScannerUiState(
                        barcode = codeToLookup,
                        message = error.localizedMessage ?: "Product lookup failed.",
                        isError = true
                    )
                }
        }
    }

    fun confirmScannedIngredient() {
        val ingredient = _scannerUiState.value.pendingIngredient ?: return
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
            _suggestedRecipes.value = emptyList()
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
}
