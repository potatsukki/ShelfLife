package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShelfLifeDao {

    // --- Ingredient (Pantry Inventory) ---
    @Query(
        """
        SELECT * FROM ingredients
        WHERE userId = :userId
        ORDER BY CASE WHEN expirationDate = '' THEN 1 ELSE 0 END, expirationDate ASC
        """
    )
    fun getAllIngredients(userId: String): Flow<List<Ingredient>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredient(ingredient: Ingredient)

    @Update
    suspend fun updateIngredients(ingredients: List<Ingredient>)

    @Delete
    suspend fun deleteIngredient(ingredient: Ingredient)

    @Query("DELETE FROM ingredients WHERE id = :id AND userId = :userId")
    suspend fun deleteIngredientById(id: Int, userId: String)

    @Query("DELETE FROM ingredients WHERE id IN (:ids) AND userId = :userId")
    suspend fun deleteIngredientsByIds(ids: List<Int>, userId: String)

    @Query("SELECT * FROM ingredients WHERE id = :id AND userId = :userId")
    suspend fun getIngredientById(id: Int, userId: String): Ingredient?

    // --- Shopping List ---
    @Query("SELECT * FROM shopping_items WHERE userId = :userId ORDER BY id DESC")
    fun getAllShoppingItems(userId: String): Flow<List<ShoppingItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItem(item: ShoppingItem)

    @Update
    suspend fun updateShoppingItem(item: ShoppingItem)

    @Delete
    suspend fun deleteShoppingItem(item: ShoppingItem)

    @Query("DELETE FROM shopping_items WHERE isChecked = 1 AND userId = :userId")
    suspend fun clearCheckedShoppingItems(userId: String)

    @Query("DELETE FROM shopping_items WHERE id = :id AND userId = :userId")
    suspend fun deleteShoppingItemById(id: Int, userId: String)

    @Query("DELETE FROM shopping_items WHERE id IN (:ids) AND userId = :userId")
    suspend fun deleteShoppingItemsByIds(ids: List<Int>, userId: String)

    @Query("SELECT * FROM ingredients WHERE userId = :userId")
    suspend fun getIngredientSnapshot(userId: String): List<Ingredient>

    @Query("SELECT * FROM shopping_items WHERE userId = :userId")
    suspend fun getShoppingSnapshot(userId: String): List<ShoppingItem>

    @Query("DELETE FROM ingredients WHERE userId = :userId")
    suspend fun deleteAllIngredients(userId: String)

    @Query("DELETE FROM shopping_items WHERE userId = :userId")
    suspend fun deleteAllShoppingItems(userId: String)

    @Query("DELETE FROM saved_recipes WHERE userId = :userId")
    suspend fun deleteAllSavedRecipes(userId: String)

    // --- Saved Recipes ---
    @Query("SELECT * FROM saved_recipes WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getAllSavedRecipes(userId: String): Flow<List<SavedRecipe>>

    @Query("SELECT * FROM saved_recipes WHERE userId = :userId AND origin = 'USER' ORDER BY updatedAt DESC")
    fun getUserRecipes(userId: String): Flow<List<SavedRecipe>>

    @Query("SELECT * FROM saved_recipes WHERE userId = :userId AND isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteRecipes(userId: String): Flow<List<SavedRecipe>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedRecipe(recipe: SavedRecipe)

    @Delete
    suspend fun deleteSavedRecipe(recipe: SavedRecipe)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_recipes WHERE id = :id AND userId = :userId AND isFavorite = 1)")
    fun isRecipeSavedFlow(id: String, userId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_recipes WHERE id = :id AND userId = :userId AND isFavorite = 1)")
    suspend fun isRecipeSavedSync(id: String, userId: String): Boolean

    @Query("SELECT * FROM saved_recipes WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getSavedRecipe(id: String, userId: String): SavedRecipe?

    @Query("DELETE FROM saved_recipes WHERE id = :id AND userId = :userId")
    suspend fun deleteSavedRecipeById(id: String, userId: String)

    @Query("SELECT * FROM recommended_recipes WHERE userId = :userId ORDER BY position ASC")
    fun getRecommendedRecipes(userId: String): Flow<List<RecommendedRecipe>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecommendedRecipes(recipes: List<RecommendedRecipe>)

    @Query("DELETE FROM recommended_recipes WHERE userId = :userId")
    suspend fun deleteRecommendedRecipes(userId: String)

    @Transaction
    suspend fun replaceRecommendedRecipes(userId: String, recipes: List<RecommendedRecipe>) {
        deleteRecommendedRecipes(userId)
        if (recipes.isNotEmpty()) insertRecommendedRecipes(recipes)
    }

    @Transaction
    suspend fun applyPantryDeduction(
        updates: List<Ingredient>,
        deleteIds: List<Int>,
        userId: String
    ) {
        if (updates.isNotEmpty()) updateIngredients(updates)
        if (deleteIds.isNotEmpty()) deleteIngredientsByIds(deleteIds, userId)
    }

    @Transaction
    suspend fun transferShoppingToPantry(
        ingredientUpdates: List<Ingredient>,
        ingredientInserts: List<Ingredient>,
        shoppingIds: List<Int>,
        userId: String
    ) {
        if (ingredientUpdates.isNotEmpty()) updateIngredients(ingredientUpdates)
        ingredientInserts.forEach { insertIngredient(it) }
        if (shoppingIds.isNotEmpty()) deleteShoppingItemsByIds(shoppingIds, userId)
    }
}
