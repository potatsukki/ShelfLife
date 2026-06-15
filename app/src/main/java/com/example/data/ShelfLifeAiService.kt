package com.example.data

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AiUnavailableException(message: String) : Exception(message)

data class RecipeContext(
    val recipeName: String,
    val availableIngredients: List<String>,
    val missingIngredients: List<String>,
    val steps: List<String>
)

class ShelfLifeAiService {
    private val functions: FirebaseFunctions? = runCatching { FirebaseFunctions.getInstance() }.getOrNull()

    suspend fun generateRecipes(ingredients: List<Ingredient>): List<SavedRecipe> {
        val callable = functions ?: throw AiUnavailableException("Firebase Functions is not configured.")
        val payload = mapOf(
            "ingredients" to ingredients.map {
                mapOf(
                    "name" to it.name,
                    "quantity" to it.quantity,
                    "unit" to it.unit,
                    "category" to it.category,
                    "location" to it.location,
                    "expirationDate" to it.expirationDate
                )
            }
        )
        val result = callable.getHttpsCallable("generateRecipes").callAwait(payload)
        val data = result.data
        val recipes = when (data) {
            is List<*> -> data
            is Map<*, *> -> data["recipes"] as? List<*> ?: emptyList<Any>()
            else -> emptyList<Any>()
        }
        return recipes.mapIndexedNotNull { index, item ->
            val obj = item as? Map<*, *> ?: return@mapIndexedNotNull null
            val name = obj.string("name").ifBlank { "Recipe ${index + 1}" }
            val ingredientsList = obj.list("ingredients")
            val stepsList = obj.list("steps")
            SavedRecipe(
                id = obj.string("id").ifBlank { "ai_${System.currentTimeMillis()}_$index" },
                userId = "",
                name = name,
                prepTime = obj.string("prepTime").ifBlank { "25 min" },
                difficulty = obj.string("difficulty").ifBlank { "Medium" },
                imageResUrl = obj.string("imageUrl").ifBlank { obj.string("imageResUrl") },
                whySuggested = obj.string("whySuggested").ifBlank { "Based on your pantry" },
                ingredientsCsv = ingredientsList.joinToString(", ") { ingredient ->
                    (ingredient as? Map<*, *>)?.string("name").orEmpty()
                }.ifBlank { obj.string("ingredientsCsv") },
                stepsCsv = stepsList.joinToString("|") { step ->
                    when (step) {
                        is Map<*, *> -> step.string("text")
                        else -> step?.toString().orEmpty()
                    }
                }.ifBlank { obj.string("stepsCsv") },
                imageProvider = obj.string("imageProvider"),
                photographerName = obj.string("photographerName"),
                photographerUrl = obj.string("photographerUrl"),
                photoPageUrl = obj.string("photoPageUrl"),
                ingredientsJson = JSONArray(ingredientsList).toString(),
                stepsJson = JSONArray(stepsList.map { step ->
                    when (step) {
                        is Map<*, *> -> step
                        else -> mapOf("text" to step.toString())
                    }
                }).toString()
            )
        }
    }

    suspend fun askAssistant(
        chatHistory: List<Pair<String, Boolean>>,
        latestUserMessage: String,
        recipeContext: RecipeContext? = null,
        pantryIngredients: List<Ingredient> = emptyList()
    ): String {
        val callable = functions ?: throw AiUnavailableException("Firebase Functions is not configured.")
        val payload = mapOf(
            "message" to latestUserMessage,
            "history" to chatHistory.map {
                mapOf("role" to if (it.second) "user" else "assistant", "content" to it.first)
            },
            "recipeContext" to recipeContext?.let {
                mapOf(
                    "recipeName" to it.recipeName,
                    "availableIngredients" to it.availableIngredients,
                    "missingIngredients" to it.missingIngredients,
                    "steps" to it.steps
                )
            },
            "pantry" to pantryIngredients.map {
                mapOf("name" to it.name, "quantity" to it.quantity, "unit" to it.unit)
            }
        )
        val result = callable.getHttpsCallable("chatAssistant").callAwait(payload)
        val data = result.data
        return (data as? Map<*, *>)?.string("reply")
            ?: throw AiUnavailableException("Kitchen AI returned an empty response.")
    }

    suspend fun identifyBarcodeFallback(barcode: String): Ingredient? {
        val callable = functions ?: throw AiUnavailableException("Firebase Functions is not configured.")
        val result = callable.getHttpsCallable("identifyBarcodeFallback").callAwait(mapOf("barcode" to barcode))
        val data = result.data as? Map<*, *> ?: return null
        val name = data.string("name").trim()
        if (name.isBlank()) return null
        return Ingredient(
            userId = "",
            name = name,
            category = data.string("category").ifBlank { "Pantry" },
            quantity = data.double("quantity") ?: 1.0,
            unit = data.string("unit").ifBlank { "pcs" },
            expirationDate = ProductDates.offsetDate(10),
            purchaseDate = ProductDates.offsetDate(0),
            location = data.string("location").ifBlank { "Pantry" },
            notes = data.string("notes").ifBlank { "Expiration date estimated. Review before adding." }
        )
    }

    private suspend fun com.google.firebase.functions.HttpsCallableReference.callAwait(data: Any):
        com.google.firebase.functions.HttpsCallableResult =
        suspendCancellableCoroutine { continuation ->
            call(data)
                .addOnSuccessListener { continuation.resume(it) }
                .addOnFailureListener { continuation.resumeWithException(it) }
        }
}

private fun Map<*, *>.string(key: String): String = this[key]?.toString().orEmpty()

private fun Map<*, *>.double(key: String): Double? = when (val value = this[key]) {
    is Number -> value.toDouble()
    is String -> value.toDoubleOrNull()
    else -> null
}

private fun Map<*, *>.list(key: String): List<*> = this[key] as? List<*> ?: emptyList<Any>()

object ProductDates {
    fun offsetDate(offsetDays: Int): String {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DATE, offsetDays)
        return format.format(cal.time)
    }
}
