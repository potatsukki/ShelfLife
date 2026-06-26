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
    val unit: String,
    val expirationDate: String, // yyyy-MM-dd, or blank when there is no printed date
    val purchaseDate: String, // yyyy-MM-dd
    val location: String, // "Fridge", "Pantry", "Freezer"
    val lowStockThreshold: Double = -1.0, // negative means reminder disabled
    val notes: String = "",
    val dateType: String = IngredientDateType.EXPIRATION,
    val itemStatus: String = IngredientStatus.SEALED,
    val openedDate: String = "",
    val storageCondition: String = "",
    val brand: String = "",
    val barcode: String = "",
    val packageSize: String = "",
    val store: String = "",
    val price: Double? = null
) {
    val hasTrackedDate: Boolean
        get() = dateType != IngredientDateType.NO_PRINTED_DATE && expirationDate.isNotBlank()

    val dateLabel: String
        get() = IngredientDateType.fieldLabel(dateType)

    val lowStockReminderEnabled: Boolean
        get() = lowStockThreshold >= 0.0
}

object IngredientDateType {
    const val EXPIRATION = "Expiration Date"
    const val BEST_BEFORE = "Best Before / Best By"
    const val USE_BY = "Use By"
    const val ESTIMATED_USE_BY = "Estimated Use-By Date"
    const val NO_PRINTED_DATE = "No Printed Date"

    val options = listOf(EXPIRATION, BEST_BEFORE, USE_BY, ESTIMATED_USE_BY, NO_PRINTED_DATE)

    fun fieldLabel(type: String): String = when (type) {
        BEST_BEFORE -> "Best Before Date"
        USE_BY -> "Use By Date"
        ESTIMATED_USE_BY -> "Estimated Use-By Date"
        NO_PRINTED_DATE -> "No Printed Date"
        else -> "Expiration Date"
    }

    fun shortLabel(type: String): String = when (type) {
        BEST_BEFORE -> "Best Before"
        USE_BY -> "Use By"
        ESTIMATED_USE_BY -> "Estimated Use-By"
        NO_PRINTED_DATE -> "No Printed Date"
        else -> "Expires"
    }
}

object IngredientStatus {
    const val SEALED = "Sealed"
    const val OPENED = "Opened"
    const val COOKED_LEFTOVER = "Cooked / Leftover"
    const val FROZEN = "Frozen"
    const val FINISHED = "Finished / Consumed"
    const val DISCARDED = "Discarded / Spoiled"

    val options = listOf(SEALED, OPENED, COOKED_LEFTOVER, FROZEN, FINISHED, DISCARDED)

    fun requiresOpenedDate(status: String): Boolean =
        status == OPENED || status == COOKED_LEFTOVER
}

@Entity(tableName = "shopping_items")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "pcs",
    val isChecked: Boolean = false,
    val category: String = "My List", // "Suggested from Low Stock", "Missing for Recipes", "My List"
    val sourceRecipeName: String? = null, // legacy single source
    val sourceRecipeNamesJson: String = ""
) {
    fun sourceRecipeNames(): List<String> {
        if (sourceRecipeNamesJson.isNotBlank()) {
            return sourceRecipeNamesJson
                .removePrefix("[")
                .removeSuffix("]")
                .split(",")
                .map {
                    it.trim()
                        .removeSurrounding("\"")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\")
                }
                .filter { it.isNotBlank() }
                .distinct()
        }
        return listOfNotNull(sourceRecipeName?.takeIf { it.isNotBlank() })
    }
}

object RecipeOrigin {
    const val AI = "AI"
    const val USER = "USER"
}

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
    val timestamp: Long = System.currentTimeMillis(),
    val origin: String = RecipeOrigin.AI,
    val isFavorite: Boolean = true,
    val description: String = "",
    val baseServings: Int = 1,
    val cookTime: String = "",
    val localImageUri: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recommended_recipes", primaryKeys = ["id", "userId"])
data class RecommendedRecipe(
    val id: String,
    val userId: String,
    val position: Int,
    val name: String,
    val prepTime: String,
    val difficulty: String,
    val imageResUrl: String,
    val whySuggested: String,
    val ingredientsCsv: String,
    val stepsCsv: String,
    val imageProvider: String = "",
    val photographerName: String = "",
    val photographerUrl: String = "",
    val photoPageUrl: String = "",
    val ingredientsJson: String = "",
    val stepsJson: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

fun SavedRecipe.toRecommendedRecipe(position: Int): RecommendedRecipe = RecommendedRecipe(
    id = id,
    userId = userId,
    position = position,
    name = name,
    prepTime = prepTime,
    difficulty = difficulty,
    imageResUrl = imageResUrl,
    whySuggested = whySuggested,
    ingredientsCsv = ingredientsCsv,
    stepsCsv = stepsCsv,
    imageProvider = imageProvider,
    photographerName = photographerName,
    photographerUrl = photographerUrl,
    photoPageUrl = photoPageUrl,
    ingredientsJson = ingredientsJson,
    stepsJson = stepsJson,
    timestamp = timestamp
)

fun RecommendedRecipe.toSavedRecipe(): SavedRecipe = SavedRecipe(
    id = id,
    userId = userId,
    name = name,
    prepTime = prepTime,
    difficulty = difficulty,
    imageResUrl = imageResUrl,
    whySuggested = whySuggested,
    ingredientsCsv = ingredientsCsv,
    stepsCsv = stepsCsv,
    imageProvider = imageProvider,
    photographerName = photographerName,
    photographerUrl = photographerUrl,
    photoPageUrl = photoPageUrl,
    ingredientsJson = ingredientsJson,
    stepsJson = stepsJson,
    timestamp = timestamp
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

fun SavedRecipe.scaledIngredients(servings: Int): List<RecipeIngredient> {
    val safeBase = baseServings.coerceAtLeast(1)
    val safeServings = servings.coerceAtLeast(1)
    val multiplier = safeServings.toDouble() / safeBase.toDouble()
    return recipeIngredients().map { ingredient ->
        ingredient.copy(quantity = ingredient.quantity?.times(multiplier))
    }
}

private fun JSONObject.optDoubleOrNull(name: String): Double? {
    if (!has(name) || isNull(name)) return null
    return runCatching { getDouble(name) }.getOrNull()
}
