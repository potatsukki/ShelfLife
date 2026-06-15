package com.example.data

import com.example.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AiUnavailableException(message: String) : Exception(message)

data class RecipeContext(
    val recipeName: String,
    val availableIngredients: List<String>,
    val missingIngredients: List<String>,
    val steps: List<String>
)

data class RecipeUpdateSuggestion(
    val summary: String,
    val ingredients: List<RecipeIngredient>,
    val steps: List<String>
)

data class AssistantReply(
    val message: String,
    val recipeUpdate: RecipeUpdateSuggestion? = null
)

class ShelfLifeAiService {
    private val baseUrl = BuildConfig.SHELFLIFE_WORKER_BASE_URL.trim().trimEnd('/')
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateRecipes(ingredients: List<Ingredient>): List<SavedRecipe> {
        val response = postJson(
            path = "generateRecipes",
            body = JSONObject().put(
                "ingredients",
                JSONArray(ingredients.map {
                    JSONObject()
                        .put("name", it.name)
                        .put("quantity", it.quantity)
                        .put("unit", it.unit)
                        .put("category", it.category)
                        .put("location", it.location)
                        .put("expirationDate", it.expirationDate)
                })
            )
        )
        val recipes = response.optJSONArray("recipes") ?: JSONArray()
        return (0 until recipes.length()).mapNotNull { index ->
            val obj = recipes.optJSONObject(index) ?: return@mapNotNull null
            val ingredientsArray = obj.optJSONArray("ingredients") ?: JSONArray()
            val stepsArray = obj.optJSONArray("steps") ?: JSONArray()
            SavedRecipe(
                id = obj.optString("id").ifBlank { "ai_${System.currentTimeMillis()}_$index" },
                userId = "",
                name = obj.optString("name").ifBlank { "Recipe ${index + 1}" },
                prepTime = obj.optString("prepTime").ifBlank { "25 min" },
                difficulty = obj.optString("difficulty").ifBlank { "Medium" },
                imageResUrl = obj.optString("imageUrl"),
                whySuggested = obj.optString("whySuggested").ifBlank { "Based on your pantry" },
                ingredientsCsv = ingredientsArray.toStringList("name").joinToString(", "),
                stepsCsv = stepsArray.toStepTextList().joinToString("|"),
                imageProvider = obj.optString("imageProvider"),
                photographerName = obj.optString("photographerName"),
                photographerUrl = obj.optString("photographerUrl"),
                photoPageUrl = obj.optString("photoPageUrl"),
                ingredientsJson = ingredientsArray.toString(),
                stepsJson = stepsArray.toString()
            )
        }
    }

    suspend fun askAssistant(
        chatHistory: List<Pair<String, Boolean>>,
        latestUserMessage: String,
        recipeContext: RecipeContext? = null,
        pantryIngredients: List<Ingredient> = emptyList()
    ): AssistantReply {
        val body = JSONObject()
            .put("message", latestUserMessage)
            .put(
                "history",
                JSONArray(chatHistory.map {
                    JSONObject()
                        .put("role", if (it.second) "user" else "assistant")
                        .put("content", it.first)
                })
            )
            .put(
                "pantry",
                JSONArray(pantryIngredients.map {
                    JSONObject()
                        .put("name", it.name)
                        .put("quantity", it.quantity)
                        .put("unit", it.unit)
                })
            )
        recipeContext?.let {
            body.put(
                "recipeContext",
                JSONObject()
                    .put("recipeName", it.recipeName)
                    .put("availableIngredients", JSONArray(it.availableIngredients))
                    .put("missingIngredients", JSONArray(it.missingIngredients))
                    .put("steps", JSONArray(it.steps))
            )
        }
        val response = postJson("chatAssistant", body)
        val reply = response.optString("reply")
            .ifBlank { throw AiUnavailableException("Kitchen AI returned an empty response.") }
        val update = response.optJSONObject("recipeUpdate")?.let { obj ->
            val ingredients = obj.optJSONArray("ingredients")?.toRecipeIngredients().orEmpty()
            val steps = obj.optJSONArray("steps")?.toStepTextList().orEmpty()
            if (ingredients.isEmpty() || steps.isEmpty()) {
                null
            } else {
                RecipeUpdateSuggestion(
                    summary = obj.optString("summary").ifBlank { "Use the suggested ingredient changes" },
                    ingredients = ingredients,
                    steps = steps
                )
            }
        }
        return AssistantReply(message = reply, recipeUpdate = update)
    }

    suspend fun identifyBarcodeFallback(barcode: String): Ingredient? {
        val response = postJson("identifyBarcodeFallback", JSONObject().put("barcode", barcode))
        val name = response.optString("name").trim()
        if (name.isBlank()) return null
        return Ingredient(
            userId = "",
            name = name,
            category = response.optString("category").ifBlank { "Pantry" },
            quantity = response.optDoubleOrNull("quantity") ?: 1.0,
            unit = response.optString("unit").ifBlank { "pcs" },
            expirationDate = ProductDates.offsetDate(10),
            purchaseDate = ProductDates.offsetDate(0),
            location = response.optString("location").ifBlank { "Pantry" },
            notes = response.optString("notes").ifBlank { "Expiration date estimated. Review before adding." }
        )
    }

    private suspend fun postJson(path: String, body: JSONObject): JSONObject {
        if (baseUrl.isBlank()) {
            throw AiUnavailableException("ShelfLife Worker URL is not configured. Set SHELFLIFE_WORKER_BASE_URL in .env.")
        }
        val token = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.awaitToken()
            ?: throw AiUnavailableException("Sign in before using ShelfLife AI.")
        val request = Request.Builder()
            .url("$baseUrl/$path")
            .addHeader("Authorization", "Bearer $token")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val message = runCatching { JSONObject(raw).optString("error") }.getOrNull()
                    throw AiUnavailableException(message?.takeIf { it.isNotBlank() } ?: "ShelfLife AI failed with status ${response.code}.")
                }
                JSONObject(raw)
            }
        }
    }

    private suspend fun com.google.android.gms.tasks.Task<com.google.firebase.auth.GetTokenResult>.awaitToken(): String =
        suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { result ->
                val token = result.token
                if (token.isNullOrBlank()) {
                    continuation.resumeWithException(AiUnavailableException("Firebase did not return an auth token."))
                } else {
                    continuation.resume(token)
                }
            }
            addOnFailureListener { continuation.resumeWithException(it) }
        }
}

private fun JSONArray.toStringList(key: String): List<String> =
    (0 until length()).mapNotNull { index ->
        optJSONObject(index)?.optString(key)?.trim()?.takeIf { it.isNotBlank() }
            ?: optString(index).trim().takeIf { it.isNotBlank() }
    }

private fun JSONArray.toStepTextList(): List<String> =
    (0 until length()).mapNotNull { index ->
        optJSONObject(index)?.optString("text")?.trim()?.takeIf { it.isNotBlank() }
            ?: optString(index).trim().takeIf { it.isNotBlank() }
    }

private fun JSONArray.toRecipeIngredients(): List<RecipeIngredient> =
    (0 until length()).mapNotNull { index ->
        val obj = optJSONObject(index) ?: return@mapNotNull null
        val name = obj.optString("name").trim()
        if (name.isBlank()) return@mapNotNull null
        RecipeIngredient(
            name = name,
            quantity = obj.optDoubleOrNull("quantity"),
            unit = obj.optString("unit").trim(),
            required = obj.optBoolean("required", true)
        )
    }

private fun JSONObject.optDoubleOrNull(key: String): Double? =
    if (has(key) && !isNull(key)) runCatching { getDouble(key) }.getOrNull() else null

object ProductDates {
    fun offsetDate(offsetDays: Int): String {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DATE, offsetDays)
        return format.format(cal.time)
    }
}
