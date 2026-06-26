package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    suspend fun deleteIngredientsByIds(ids: List<Int>, userId: String) = withContext(Dispatchers.IO) {
        dao.deleteIngredientsByIds(ids, userId)
    }

    suspend fun getIngredientById(id: Int, userId: String): Ingredient? = withContext(Dispatchers.IO) {
        dao.getIngredientById(id, userId)
    }

    suspend fun getIngredientSnapshot(userId: String): List<Ingredient> = withContext(Dispatchers.IO) {
        dao.getIngredientSnapshot(userId)
    }

    fun allShoppingItems(userId: String): Flow<List<ShoppingItem>> = dao.getAllShoppingItems(userId)

    suspend fun insertShoppingItem(item: ShoppingItem) = withContext(Dispatchers.IO) {
        dao.insertShoppingItem(item)
    }

    suspend fun mergeShoppingItem(item: ShoppingItem) = withContext(Dispatchers.IO) {
        val existing = dao.getShoppingSnapshot(item.userId)
        val result = ShoppingListMerger.merge(existing, item)
        result.mergedItem?.let { dao.updateShoppingItem(it) }
        result.newItem?.let { dao.insertShoppingItem(it) }
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
    fun userRecipes(userId: String): Flow<List<SavedRecipe>> = dao.getUserRecipes(userId)
    fun favoriteRecipes(userId: String): Flow<List<SavedRecipe>> = dao.getFavoriteRecipes(userId)

    suspend fun insertSavedRecipe(recipe: SavedRecipe) = withContext(Dispatchers.IO) {
        dao.insertSavedRecipe(recipe)
    }

    suspend fun deleteSavedRecipe(recipe: SavedRecipe) = withContext(Dispatchers.IO) {
        dao.deleteSavedRecipe(recipe)
    }

    suspend fun getSavedRecipe(id: String, userId: String): SavedRecipe? = withContext(Dispatchers.IO) {
        dao.getSavedRecipe(id, userId)
    }

    suspend fun deleteSavedRecipeById(id: String, userId: String) = withContext(Dispatchers.IO) {
        dao.deleteSavedRecipeById(id, userId)
    }

    fun isRecipeSavedFlow(id: String, userId: String): Flow<Boolean> = dao.isRecipeSavedFlow(id, userId)

    suspend fun isRecipeSavedSync(id: String, userId: String): Boolean = withContext(Dispatchers.IO) {
        dao.isRecipeSavedSync(id, userId)
    }

    fun recommendedRecipes(userId: String): Flow<List<SavedRecipe>> =
        dao.getRecommendedRecipes(userId).map { recipes ->
            recipes.map(RecommendedRecipe::toSavedRecipe)
        }

    suspend fun replaceRecommendedRecipes(userId: String, recipes: List<SavedRecipe>) =
        withContext(Dispatchers.IO) {
            dao.replaceRecommendedRecipes(
                userId,
                recipes.mapIndexed { index, recipe ->
                    recipe.copy(userId = userId).toRecommendedRecipe(index)
                }
            )
        }

    suspend fun getAIRecipeSuggestions(userId: String, pantryIngredients: List<Ingredient>): List<SavedRecipe> =
        withContext(Dispatchers.IO) {
            aiService.generateRecipes(pantryIngredients).map { it.copy(userId = userId) }
        }

    suspend fun askAssistant(
        chatHistory: List<Pair<String, Boolean>>,
        latestUserMessage: String,
        recipeContext: RecipeContext?,
        pantryIngredients: List<Ingredient>,
        isRepeatedQuestion: Boolean = false
    ): AssistantReply =
        withContext(Dispatchers.IO) {
            aiService.askAssistant(chatHistory, latestUserMessage, recipeContext, pantryIngredients, isRepeatedQuestion)
        }

    suspend fun cleanupReceiptItems(receiptText: String): List<ReceiptCleanupItem> =
        withContext(Dispatchers.IO) {
            aiService.cleanupReceiptItems(receiptText)
        }

    suspend fun lookupProduct(userId: String, barcode: String): Ingredient? = withContext(Dispatchers.IO) {
        ProductLookupHelper.lookupBarcode(
            barcode = barcode,
            userId = userId,
            aiFallback = { aiService.identifyBarcodeFallback(barcode)?.copy(userId = userId) }
        )
    }

    suspend fun deductPantry(userId: String, usages: List<IngredientUsage>): PantryDeductionPlan =
        withContext(Dispatchers.IO) {
            val plan = PantryDeductionPlanner.plan(dao.getIngredientSnapshot(userId), usages)
            dao.applyPantryDeduction(plan.updates, plan.deleteIds, userId)
            plan
        }

    suspend fun transferShoppingToPantry(
        userId: String,
        updates: List<Ingredient>,
        inserts: List<Ingredient>,
        shoppingIds: List<Int>
    ) = withContext(Dispatchers.IO) {
        dao.transferShoppingToPantry(updates, inserts, shoppingIds, userId)
    }

    suspend fun clearAllData(userId: String) = withContext(Dispatchers.IO) {
        dao.deleteAllIngredients(userId)
        dao.deleteAllShoppingItems(userId)
        dao.deleteAllSavedRecipes(userId)
        dao.deleteRecommendedRecipes(userId)
    }
}
