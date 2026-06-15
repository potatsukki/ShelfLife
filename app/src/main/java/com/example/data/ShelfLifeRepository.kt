package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ShelfLifeRepository(
    private val dao: ShelfLifeDao,
    private val aiService: ShelfLifeAiService
) {
    fun allIngredients(userId: String): Flow<List<Ingredient>> = dao.getAllIngredients(userId)

    suspend fun insertIngredient(ingredient: Ingredient) = withContext(Dispatchers.IO) {
        dao.insertIngredient(ingredient)
    }

    suspend fun deleteIngredient(ingredient: Ingredient) = withContext(Dispatchers.IO) {
        dao.deleteIngredient(ingredient)
    }

    suspend fun deleteIngredientById(id: Int, userId: String) = withContext(Dispatchers.IO) {
        dao.deleteIngredientById(id, userId)
    }

    suspend fun getIngredientById(id: Int, userId: String): Ingredient? = withContext(Dispatchers.IO) {
        dao.getIngredientById(id, userId)
    }

    fun allShoppingItems(userId: String): Flow<List<ShoppingItem>> = dao.getAllShoppingItems(userId)

    suspend fun insertShoppingItem(item: ShoppingItem) = withContext(Dispatchers.IO) {
        dao.insertShoppingItem(item)
    }

    suspend fun updateShoppingItem(item: ShoppingItem) = withContext(Dispatchers.IO) {
        dao.updateShoppingItem(item)
    }

    suspend fun deleteShoppingItem(item: ShoppingItem) = withContext(Dispatchers.IO) {
        dao.deleteShoppingItem(item)
    }

    suspend fun clearCheckedShoppingItems(userId: String) = withContext(Dispatchers.IO) {
        dao.clearCheckedShoppingItems(userId)
    }

    suspend fun deleteShoppingItemById(id: Int, userId: String) = withContext(Dispatchers.IO) {
        dao.deleteShoppingItemById(id, userId)
    }

    fun allSavedRecipes(userId: String): Flow<List<SavedRecipe>> = dao.getAllSavedRecipes(userId)

    suspend fun insertSavedRecipe(recipe: SavedRecipe) = withContext(Dispatchers.IO) {
        dao.insertSavedRecipe(recipe)
    }

    suspend fun deleteSavedRecipe(recipe: SavedRecipe) = withContext(Dispatchers.IO) {
        dao.deleteSavedRecipe(recipe)
    }

    fun isRecipeSavedFlow(id: String, userId: String): Flow<Boolean> = dao.isRecipeSavedFlow(id, userId)

    suspend fun isRecipeSavedSync(id: String, userId: String): Boolean = withContext(Dispatchers.IO) {
        dao.isRecipeSavedSync(id, userId)
    }

    suspend fun getAIRecipeSuggestions(userId: String, pantryIngredients: List<Ingredient>): List<SavedRecipe> =
        withContext(Dispatchers.IO) {
            aiService.generateRecipes(pantryIngredients).map { it.copy(userId = userId) }
        }

    suspend fun askAssistant(
        chatHistory: List<Pair<String, Boolean>>,
        latestUserMessage: String,
        recipeContext: RecipeContext?,
        pantryIngredients: List<Ingredient>
    ): AssistantReply =
        withContext(Dispatchers.IO) {
            aiService.askAssistant(chatHistory, latestUserMessage, recipeContext, pantryIngredients)
        }

    suspend fun lookupProduct(userId: String, barcode: String): Ingredient? = withContext(Dispatchers.IO) {
        ProductLookupHelper.lookupBarcode(
            barcode = barcode,
            userId = userId,
            aiFallback = { aiService.identifyBarcodeFallback(barcode)?.copy(userId = userId) }
        )
    }

    suspend fun clearAllData(userId: String) = withContext(Dispatchers.IO) {
        dao.deleteAllIngredients(userId)
        dao.deleteAllShoppingItems(userId)
        dao.deleteAllSavedRecipes(userId)
    }
}
