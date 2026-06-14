package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShelfLifeDao {

    // --- Ingredient (Pantry Inventory) ---
    @Query("SELECT * FROM ingredients ORDER BY expirationDate ASC")
    fun getAllIngredients(): Flow<List<Ingredient>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredient(ingredient: Ingredient)

    @Delete
    suspend fun deleteIngredient(ingredient: Ingredient)

    @Query("DELETE FROM ingredients WHERE id = :id")
    suspend fun deleteIngredientById(id: Int)

    @Query("SELECT * FROM ingredients WHERE id = :id")
    suspend fun getIngredientById(id: Int): Ingredient?

    // --- Shopping List ---
    @Query("SELECT * FROM shopping_items ORDER BY id DESC")
    fun getAllShoppingItems(): Flow<List<ShoppingItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItem(item: ShoppingItem)

    @Update
    suspend fun updateShoppingItem(item: ShoppingItem)

    @Delete
    suspend fun deleteShoppingItem(item: ShoppingItem)

    @Query("DELETE FROM shopping_items WHERE isChecked = 1")
    suspend fun clearCheckedShoppingItems()

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun deleteShoppingItemById(id: Int)

    @Query("DELETE FROM ingredients")
    suspend fun deleteAllIngredients()

    @Query("DELETE FROM shopping_items")
    suspend fun deleteAllShoppingItems()

    @Query("DELETE FROM saved_recipes")
    suspend fun deleteAllSavedRecipes()

    // --- Saved Recipes ---
    @Query("SELECT * FROM saved_recipes ORDER BY timestamp DESC")
    fun getAllSavedRecipes(): Flow<List<SavedRecipe>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedRecipe(recipe: SavedRecipe)

    @Delete
    suspend fun deleteSavedRecipe(recipe: SavedRecipe)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_recipes WHERE id = :id)")
    fun isRecipeSavedFlow(id: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_recipes WHERE id = :id)")
    suspend fun isRecipeSavedSync(id: String): Boolean
}
