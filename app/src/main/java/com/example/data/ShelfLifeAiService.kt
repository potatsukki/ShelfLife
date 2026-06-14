package com.example.data

import com.google.firebase.FirebaseApp
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AiUnavailableException(message: String) : Exception(message)

class ShelfLifeAiService {
    private val functions: FirebaseFunctions?
        get() = try {
            FirebaseFunctions.getInstance()
        } catch (_: IllegalStateException) {
            null
        }

    suspend fun generateRecipes(ingredients: List<Ingredient>): List<SavedRecipe> {
        val result = call(
            "generateRecipes",
            mapOf(
                "ingredients" to ingredients.map {
                    mapOf(
                        "name" to it.name,
                        "category" to it.category,
                        "quantity" to it.quantity,
                        "unit" to it.unit,
                        "expirationDate" to it.expirationDate,
                        "location" to it.location
                    )
                }
            )
        )
        val list = (result.data as? List<*>) ?: return emptyList()
        return list.mapNotNull { item ->
            val map = item as? Map<*, *> ?: return@mapNotNull null
            SavedRecipe(
                id = map["id"]?.toString().orEmpty().ifBlank { "ai_${System.currentTimeMillis()}" },
                userId = "",
                name = map["name"]?.toString().orEmpty(),
                prepTime = map["prepTime"]?.toString().orEmpty().ifBlank { "20 min" },
                difficulty = map["difficulty"]?.toString().orEmpty().ifBlank { "Easy" },
                imageResUrl = map["imageResUrl"]?.toString().orEmpty(),
                whySuggested = map["whySuggested"]?.toString().orEmpty(),
                ingredientsCsv = map["ingredientsCsv"]?.toString().orEmpty(),
                stepsCsv = map["stepsCsv"]?.toString().orEmpty()
            ).takeIf { it.name.isNotBlank() && it.stepsCsv.isNotBlank() }
        }
    }

    suspend fun askAssistant(chatHistory: List<Pair<String, Boolean>>, latestUserMessage: String): String {
        val result = call(
            "chatAssistant",
            mapOf(
                "message" to latestUserMessage,
                "history" to chatHistory.takeLast(12).map { (text, isUser) ->
                    mapOf("role" to if (isUser) "user" else "assistant", "content" to text)
                }
            )
        )
        val map = result.data as? Map<*, *>
        return map?.get("reply")?.toString().orEmpty()
            .ifBlank { throw AiUnavailableException("The assistant returned an empty response.") }
    }

    suspend fun identifyBarcodeFallback(barcode: String): Ingredient? {
        val result = call("identifyBarcodeFallback", mapOf("barcode" to barcode))
        val map = result.data as? Map<*, *> ?: return null
        val name = map["name"]?.toString().orEmpty()
        if (name.isBlank()) return null
        return Ingredient(
            userId = "",
            name = name,
            category = map["category"]?.toString().orEmpty().ifBlank { "Pantry" },
            quantity = map["quantity"]?.toString()?.toDoubleOrNull() ?: 1.0,
            unit = map["unit"]?.toString().orEmpty().ifBlank { "pcs" },
            expirationDate = ProductDates.offsetDate(10),
            purchaseDate = ProductDates.offsetDate(0),
            location = mapCategoryToLocation(map["category"]?.toString().orEmpty()),
            notes = map["notes"]?.toString().orEmpty().ifBlank { "Identified from barcode fallback" }
        )
    }

    private suspend fun call(name: String, data: Map<String, Any?>): HttpsCallableResult {
        val client = functions ?: throw AiUnavailableException(
            "Firebase is not configured. Add app/google-services.json and deploy Firebase Functions."
        )
        return suspendCancellableCoroutine { continuation ->
            client.getHttpsCallable(name).call(data)
                .addOnSuccessListener { continuation.resume(it) }
                .addOnFailureListener { continuation.resumeWithException(it) }
        }
    }

    private fun mapCategoryToLocation(category: String): String {
        return when (category) {
            "Dairy", "Produce" -> "Fridge"
            "Meat" -> "Freezer"
            else -> "Pantry"
        }
    }
}

object ProductDates {
    fun offsetDate(offsetDays: Int): String {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DATE, offsetDays)
        return format.format(cal.time)
    }
}
