package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ScannerUiState(
    val isLoading: Boolean = false,
    val barcode: String? = null,
    val pendingIngredient: Ingredient? = null,
    val message: String? = null,
    val isError: Boolean = false
)

class ShelfLifeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ShelfLifeDatabase.getDatabase(application)
    private val repository = ShelfLifeRepository(db.dao)

    // --- Active Navigation Route ---
    private val _currentRoute = MutableStateFlow("splash")
    val currentRoute: StateFlow<String> = _currentRoute.asStateFlow()

    fun navigateTo(route: String) {
        _currentRoute.value = route
    }

    // --- Onboarding Navigation ---
    private val _onboardingStep = MutableStateFlow(0)
    val onboardingStep: StateFlow<Int> = _onboardingStep.asStateFlow()

    fun nextOnboarding() {
        if (_onboardingStep.value < 2) {
            _onboardingStep.value += 1
        } else {
            navigateTo("dashboard")
        }
    }

    fun skipOnboarding() {
        navigateTo("dashboard")
    }

    // --- Pantry Inventory ---
    val ingredients: StateFlow<List<Ingredient>> = repository.allIngredients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val filteredIngredients: StateFlow<List<Ingredient>> = combine(
        ingredients, searchQuery, _selectedCategory
    ) { list, query, cat ->
        var res = list
        if (query.isNotBlank()) {
            res = res.filter { it.name.contains(query, ignoreCase = true) }
        }
        if (cat != "All") {
            // Support filter variants: "Fresh", "Expiring Soon", "Expired", "Low Stock"
            when (cat) {
                "Fresh" -> {
                    res = res.filter { getDaysExpiry(it.expirationDate) > 3 }
                }
                "Expiring Soon" -> {
                    res = res.filter { 
                        val days = getDaysExpiry(it.expirationDate)
                        days in 0..3 
                    }
                }
                "Expired" -> {
                    res = res.filter { getDaysExpiry(it.expirationDate) < 0 }
                }
                "Low Stock" -> {
                    res = res.filter { it.quantity <= it.lowStockThreshold }
                }
                else -> {
                    res = res.filter { it.category.equals(cat, ignoreCase = true) }
                }
            }
        }
        res
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(cat: String) {
        _selectedCategory.value = cat
    }

    // Ingredient actions
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
        viewModelScope.launch {
            val ing = Ingredient(
                name = name,
                category = category,
                quantity = quantity,
                unit = unit,
                expirationDate = expirationDate,
                purchaseDate = purchaseDate,
                location = location,
                lowStockThreshold = lowStockThreshold,
                notes = notes
            )
            repository.insertIngredient(ing)
        }
    }

    fun updateIngredient(ingredient: Ingredient) {
        viewModelScope.launch {
            repository.insertIngredient(ingredient)
        }
    }

    fun deleteIngredient(ingredient: Ingredient) {
        viewModelScope.launch {
            repository.deleteIngredient(ingredient)
        }
    }

    fun getIngredientDetails(id: Int): Flow<Ingredient?> = flow {
        emit(repository.getIngredientById(id))
    }

    // --- Shopping List ---
    val shoppingItems: StateFlow<List<ShoppingItem>> = repository.allShoppingItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addShoppingItem(name: String, category: String, sourceRecipeName: String? = null) {
        viewModelScope.launch {
            val item = ShoppingItem(
                name = name,
                category = category,
                sourceRecipeName = sourceRecipeName
            )
            repository.insertShoppingItem(item)
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
        viewModelScope.launch {
            repository.clearCheckedShoppingItems()
        }
    }

    // --- AI Recipe Ideas ---
    private val _suggestedRecipes = MutableStateFlow<List<SavedRecipe>>(emptyList())
    val suggestedRecipes: StateFlow<List<SavedRecipe>> = _suggestedRecipes.asStateFlow()

    private val _recipeLoading = MutableStateFlow(false)
    val recipeLoading: StateFlow<Boolean> = _recipeLoading.asStateFlow()

    val savedRecipes: StateFlow<List<SavedRecipe>> = repository.allSavedRecipes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected active recipe for details
    private val _selectedRecipe = MutableStateFlow<SavedRecipe?>(null)
    val selectedRecipe: StateFlow<SavedRecipe?> = _selectedRecipe.asStateFlow()

    fun selectRecipe(recipe: SavedRecipe?) {
        _selectedRecipe.value = recipe
    }

    fun triggerRecipeSuggestions() {
        viewModelScope.launch {
            _recipeLoading.value = true
            try {
                val suggestions = repository.getAIRecipeSuggestions(ingredients.value)
                _suggestedRecipes.value = suggestions
            } catch (e: Exception) {
                // Ignore error, repository automatically falls back safely
            } finally {
                _recipeLoading.value = false
            }
        }
    }

    fun toggleSaveRecipe(recipe: SavedRecipe) {
        viewModelScope.launch {
            if (repository.isRecipeSavedSync(recipe.id)) {
                repository.deleteSavedRecipe(recipe)
            } else {
                repository.insertSavedRecipe(recipe)
            }
        }
    }

    fun isRecipeSaved(id: String): Flow<Boolean> = repository.isRecipeSavedFlow(id)

    // --- AI Kitchen Assistant (Chat) ---
    private val _chatHistory = MutableStateFlow<List<Pair<String, Boolean>>>(
        listOf(
            "What can I cook with eggs and tomato?" to true,
            "You can make a classic Tomato Egg Stir-fry! It perfectly uses the items you have and takes only about 15 minutes to prepare." to false
        )
    )
    val chatHistory: StateFlow<List<Pair<String, Boolean>>> = _chatHistory.asStateFlow()

    private val _chatLoading = MutableStateFlow(false)
    val chatLoading: StateFlow<Boolean> = _chatLoading.asStateFlow()

    fun sendChatMessage(message: String) {
        if (message.isBlank()) return
        viewModelScope.launch {
            // Append user message
            val current = _chatHistory.value.toMutableList()
            current.add(message to true)
            _chatHistory.value = current
            _chatLoading.value = true

            try {
                val reply = repository.askAssistant(current.dropLast(1), message)
                val updated = _chatHistory.value.toMutableList()
                updated.add(reply to false)
                _chatHistory.value = updated
            } catch (e: Exception) {
                val updated = _chatHistory.value.toMutableList()
                updated.add("Error: I couldn't reach the server. Let me check my notes: ${e.localizedMessage}" to false)
                _chatHistory.value = updated
            } finally {
                _chatLoading.value = false
            }
        }
    }

    fun clearChat() {
        _chatHistory.value = emptyList()
    }

    // --- Settings Preferences ---
    val isDarkMode = MutableStateFlow(false)
    val notificationExpirationAlerts = MutableStateFlow(true)
    val notificationLowStockAlerts = MutableStateFlow(true)
    val dietaryVegetarian = MutableStateFlow(true)
    val smartRecipeIdeas = MutableStateFlow(true)
    val measurementSystemMetric = MutableStateFlow(true)
    val userHouseholdSharing = MutableStateFlow(false)

    // --- Barcode Scanner lookup and confirmation ---
    private val _scannerUiState = MutableStateFlow(ScannerUiState())
    val scannerUiState: StateFlow<ScannerUiState> = _scannerUiState.asStateFlow()

    fun scanRealBarcode(context: android.content.Context) {
        _scannerUiState.value = ScannerUiState(isLoading = true, message = "Opening camera scanner...")
        try {
            val scanner = com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(context)
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    val code = barcode.rawValue
                    if (!code.isNullOrBlank()) {
                        lookupBarcode(code)
                    } else {
                        _scannerUiState.value = ScannerUiState(
                            message = "No barcode was captured. Try again or enter the code manually.",
                            isError = true
                        )
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("ShelfLifeViewModel", "Scan error: ${e.message}")
                    _scannerUiState.value = ScannerUiState(
                        message = "Scanner is not available here. Try a real device or enter the barcode manually.",
                        isError = true
                    )
                }
        } catch (e: Exception) {
            android.util.Log.e("ShelfLifeViewModel", "Scanner launch error: ${e.message}")
            _scannerUiState.value = ScannerUiState(
                message = "Scanning failed to initialize: ${e.localizedMessage}",
                isError = true
            )
        }
    }

    fun lookupBarcode(barcode: String) {
        viewModelScope.launch {
            val codeToLookup = barcode.trim()
            if (codeToLookup.isBlank()) {
                _scannerUiState.value = ScannerUiState(
                    message = "Enter a barcode first.",
                    isError = true
                )
                return@launch
            }

            _scannerUiState.value = ScannerUiState(
                isLoading = true,
                barcode = codeToLookup,
                message = "Looking up barcode..."
            )

            val fetched = repository.lookupProduct(codeToLookup)
            if (fetched != null) {
                _scannerUiState.value = ScannerUiState(
                    barcode = codeToLookup,
                    pendingIngredient = fetched,
                    message = "Product found. Review it before adding to your pantry."
                )
            } else {
                _scannerUiState.value = ScannerUiState(
                    barcode = codeToLookup,
                    message = "No product match found for $codeToLookup. Try another barcode or add it manually.",
                    isError = true
                )
            }
        }
    }

    fun confirmScannedIngredient() {
        val ingredient = _scannerUiState.value.pendingIngredient ?: return
        viewModelScope.launch {
            repository.insertIngredient(ingredient)
            _scannerUiState.value = ScannerUiState(
                message = "${ingredient.name} was added to your ${ingredient.location}.",
                barcode = _scannerUiState.value.barcode
            )
        }
    }

    fun resetScanner() {
        _scannerUiState.value = ScannerUiState()
    }

    // --- Prep Prepopulated Demo Data & Surgical Clean ---
    init {
        // Clear all mock seeding. Start with a completely fresh, empty pantry and list.
        _suggestedRecipes.value = emptyList()
        viewModelScope.launch {
            try {
                // Read and surgically delete initial mockup seeds so database starts fresh
                val dummyIngredients = setOf("Spinach", "Chicken", "Rice", "Milk")
                repository.allIngredients.first().forEach { ing ->
                    if (dummyIngredients.contains(ing.name)) {
                        repository.deleteIngredient(ing)
                    }
                }
                val dummyShopping = setOf("Rice", "Olive Oil", "Soy Sauce", "Apples", "Bread")
                repository.allShoppingItems.first().forEach { item ->
                    if (dummyShopping.contains(item.name)) {
                        repository.deleteShoppingItem(item)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ShelfLifeViewModel", "Error while purging old seeded mock context: ${e.message}")
            }
        }
    }

    fun clearAllPantryAndData() {
        viewModelScope.launch {
            repository.clearAllData()
            _suggestedRecipes.value = emptyList()
        }
    }

    // Helper functions
    private fun getOffsetDate(offsetDays: Int): String {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DATE, offsetDays)
        return format.format(cal.time)
    }

    fun getDaysExpiry(targetDateStr: String): Int {
        try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val expiry = format.parse(targetDateStr) ?: return 0
            val today = format.parse(format.format(java.util.Date())) ?: return 0
            val diff = expiry.time - today.time
            return (diff / (1000 * 60 * 60 * 24)).toInt()
        } catch (e: Exception) {
            return 3 // Default fallback
        }
    }
}
