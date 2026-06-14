package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ingredients")
data class Ingredient(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String, // "Produce", "Dairy", "Meat", "Grains", "Pantry", etc.
    val quantity: Double,
    val unit: String, // "pcs", "kg", "g", "L", "ml", "pack"
    val expirationDate: String, // yyyy-MM-dd
    val purchaseDate: String, // yyyy-MM-dd
    val location: String, // "Fridge", "Pantry", "Freezer"
    val lowStockThreshold: Double = 1.0,
    val notes: String = ""
)

@Entity(tableName = "shopping_items")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "pcs",
    val isChecked: Boolean = false,
    val category: String = "My List", // "Suggested from Low Stock", "Missing for Recipes", "My List"
    val sourceRecipeName: String? = null // if added from a recipe
)

@Entity(tableName = "saved_recipes")
data class SavedRecipe(
    @PrimaryKey val id: String, // Use lowercase identifier or similar
    val name: String,
    val prepTime: String,
    val difficulty: String,
    val imageResUrl: String,
    val whySuggested: String,
    val ingredientsCsv: String, // comma separated or simple format
    val stepsCsv: String, // pipe separated or simple format
    val timestamp: Long = System.currentTimeMillis()
)
