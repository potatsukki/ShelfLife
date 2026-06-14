package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShelfLifeDao {

    // --- Ingredient (Pantry Inventory) ---
    @Query("SELECT * FROM ingredients WHERE userId = :userId ORDER BY expirationDate ASC")
    fun getAllIngredients(userId: String): Flow<List<Ingredient>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredient(ingredient: Ingredient)

    @Delete
    suspend fun deleteIngredient(ingredient: Ingredient)

    @Query("DELETE FROM ingredients WHERE id = :id AND userId = :userId")
    suspend fun deleteIngredientById(id: Int, userId: String)

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

    @Query("DELETE FROM ingredients WHERE userId = :userId")
    suspend fun deleteAllIngredients(userId: String)

    @Query("DELETE FROM shopping_items WHERE userId = :userId")
    suspend fun deleteAllShoppingItems(userId: String)

    @Query("DELETE FROM saved_recipes WHERE userId = :userId")
    suspend fun deleteAllSavedRecipes(userId: String)

    // --- Saved Recipes ---
    @Query("SELECT * FROM saved_recipes WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllSavedRecipes(userId: String): Flow<List<SavedRecipe>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedRecipe(recipe: SavedRecipe)

    @Delete
    suspend fun deleteSavedRecipe(recipe: SavedRecipe)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_recipes WHERE id = :id AND userId = :userId)")
    fun isRecipeSavedFlow(id: String, userId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_recipes WHERE id = :id AND userId = :userId)")
    suspend fun isRecipeSavedSync(id: String, userId: String): Boolean
}
