package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "ingredients")
data class Ingredient(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
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
    val userId: String,
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "pcs",
    val isChecked: Boolean = false,
    val category: String = "My List", // "Suggested from Low Stock", "Missing for Recipes", "My List"
    val sourceRecipeName: String? = null // if added from a recipe
)

@Entity(tableName = "saved_recipes", primaryKeys = ["id", "userId"])
data class SavedRecipe(
    val id: String,
    val userId: String,
    val name: String,
    val prepTime: String,
    val difficulty: String,
    val imageResUrl: String,
    val whySuggested: String,
    val ingredientsCsv: String, // comma separated or simple format
    val stepsCsv: String, // pipe separated or simple format
    val imageProvider: String = "",
    val photographerName: String = "",
    val photographerUrl: String = "",
    val photoPageUrl: String = "",
    val ingredientsJson: String = "",
    val stepsJson: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class RecipeIngredient(
    val name: String,
    val quantity: Double? = null,
    val unit: String = "",
    val required: Boolean = true
) {
    val displayText: String
        get() {
            val qty = quantity?.takeIf { it > 0.0 }?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
            }
            return listOfNotNull(qty, unit.takeIf { it.isNotBlank() }, name)
                .joinToString(" ")
                .trim()
        }
}

fun SavedRecipe.recipeIngredients(): List<RecipeIngredient> {
    if (ingredientsJson.isNotBlank()) {
        runCatching {
            val array = JSONArray(ingredientsJson)
            return (0 until array.length()).mapNotNull { index ->
                val obj = array.optJSONObject(index) ?: return@mapNotNull null
                val name = obj.optString("name").trim()
                if (name.isBlank()) return@mapNotNull null
                RecipeIngredient(
                    name = name,
                    quantity = obj.optDoubleOrNull("quantity"),
                    unit = obj.optString("unit").trim(),
                    required = obj.optBoolean("required", true)
                )
            }
        }
    }
    return ingredientsCsv.split(",")
        .map { it.trim().replace("- have", "", ignoreCase = true).replace("- Missing", "", ignoreCase = true) }
        .filter { it.isNotBlank() }
        .map { RecipeIngredient(name = it) }
}

fun SavedRecipe.recipeSteps(): List<String> {
    if (stepsJson.isNotBlank()) {
        runCatching {
            val array = JSONArray(stepsJson)
            return (0 until array.length()).mapNotNull { index ->
                val value = array.opt(index)
                when (value) {
                    is JSONObject -> value.optString("text").trim()
                    else -> value?.toString()?.trim()
                }?.takeIf { it.isNotBlank() }
            }
        }
    }
    return stepsCsv
        .split("|", "\n")
        .flatMap { part -> part.split(Regex("""(?<=\.)\s+(?=\d+\.|\p{Lu})""")) }
        .map { it.trim().replace(Regex("""^\d+[\).\s-]+"""), "") }
        .filter { it.isNotBlank() }
}

private fun JSONObject.optDoubleOrNull(name: String): Double? {
    if (!has(name) || isNull(name)) return null
    return runCatching { getDouble(name) }.getOrNull()
}
