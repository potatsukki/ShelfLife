package com.example.data

import com.example.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GetTokenResult
import kotlinx.coroutines.CancellationException
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

class ShelfLifeAiService(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    suspend fun generateRecipes(ingredients: List<Ingredient>): List<SavedRecipe> {
        val payload = JSONObject()
            .put("ingredients", JSONArray(ingredients.map { it.toJson() }))
        val data = post("generateRecipes", payload)
        val recipes = data.optJSONArray("recipes") ?: JSONArray()
        return (0 until recipes.length()).mapNotNull { index ->
            val obj = recipes.optJSONObject(index) ?: return@mapNotNull null
            val name = obj.optString("name").ifBlank { "Recipe ${index + 1}" }
            val ingredientsList = obj.optJSONArray("ingredients") ?: JSONArray()
            val stepsList = obj.optJSONArray("steps") ?: JSONArray()
            SavedRecipe(
                id = obj.optString("id").ifBlank { "ai_${System.currentTimeMillis()}_$index" },
                userId = "",
                name = name,
                prepTime = obj.optString("prepTime").ifBlank { "25 min" },
                difficulty = obj.optString("difficulty").ifBlank { "Medium" },
                imageResUrl = obj.optString("imageUrl").ifBlank { obj.optString("imageResUrl") },
                whySuggested = obj.optString("whySuggested").ifBlank { "Based on your pantry" },
                ingredientsCsv = ingredientsList.joinNames().ifBlank { obj.optString("ingredientsCsv") },
                stepsCsv = stepsList.joinSteps().ifBlank { obj.optString("stepsCsv") },
                imageProvider = obj.optString("imageProvider"),
                photographerName = obj.optString("photographerName"),
                photographerUrl = obj.optString("photographerUrl"),
                photoPageUrl = obj.optString("photoPageUrl"),
                ingredientsJson = if (ingredientsList.length() > 0) ingredientsList.toString() else obj.optString("ingredientsJson"),
                stepsJson = if (stepsList.length() > 0) stepsList.toString() else obj.optString("stepsJson")
            )
        }
    }

    suspend fun askAssistant(
        chatHistory: List<Pair<String, Boolean>>,
        latestUserMessage: String,
        recipeContext: RecipeContext? = null,
        pantryIngredients: List<Ingredient> = emptyList()
    ): String {
        val payload = JSONObject()
            .put("message", latestUserMessage)
            .put(
                "history",
                JSONArray(chatHistory.map {
                    JSONObject()
                        .put("role", if (it.second) "user" else "assistant")
                        .put("content", it.first)
                })
            )
            .put("recipeContext", recipeContext?.toJson())
            .put("pantry", JSONArray(pantryIngredients.map { it.toJson() }))

        return post("chatAssistant", payload)
            .optString("reply")
            .ifBlank { throw AiUnavailableException("Kitchen AI returned an empty response.") }
    }

    suspend fun identifyBarcodeFallback(barcode: String): Ingredient? {
        val data = post("identifyBarcodeFallback", JSONObject().put("barcode", barcode))
        val name = data.optString("name").trim()
        if (name.isBlank()) return null
        return Ingredient(
            userId = "",
            name = name,
            category = data.optString("category").ifBlank { "Pantry" },
            quantity = data.optDoubleOrNull("quantity") ?: 1.0,
            unit = data.optString("unit").ifBlank { "pcs" },
            expirationDate = ProductDates.offsetDate(10),
            purchaseDate = ProductDates.offsetDate(0),
            location = data.optString("location").ifBlank { "Pantry" },
            notes = data.optString("notes").ifBlank { "Expiration date estimated. Review before adding." }
        )
    }

    private suspend fun post(path: String, payload: JSONObject): JSONObject {
        val baseUrl = BuildConfig.SHELFLIFE_WORKER_BASE_URL.trim().trimEnd('/')
        if (baseUrl.isBlank() || baseUrl.contains("replace-after-worker-deploy")) {
            throw AiUnavailableException("Cloudflare Worker URL is not configured. Set SHELFLIFE_WORKER_BASE_URL in .env.")
        }

        val token = firebaseAuth.currentUser?.getIdToken(false)?.await()?.token
            ?: throw AiUnavailableException("Sign in before using ShelfLife AI.")
        val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("$baseUrl/$path")
            .header("Authorization", "Bearer $token")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            val json = runCatching { JSONObject(text) }.getOrElse { JSONObject().put("error", text) }
            if (!response.isSuccessful) {
                throw AiUnavailableException(json.optString("error").ifBlank { "ShelfLife AI request failed (${response.code})." })
            }
            json.optString("error").takeIf { it.isNotBlank() }?.let { throw AiUnavailableException(it) }
            return json
        }
    }

    private suspend fun com.google.android.gms.tasks.Task<GetTokenResult>.await(): GetTokenResult =
        suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { continuation.resume(it) }
            addOnFailureListener { continuation.resumeWithException(it) }
            addOnCanceledListener { continuation.resumeWithException(CancellationException("Firebase token request was canceled.")) }
        }
}

private fun Ingredient.toJson(): JSONObject =
    JSONObject()
        .put("name", name)
        .put("quantity", quantity)
        .put("unit", unit)
        .put("category", category)
        .put("location", location)
        .put("expirationDate", expirationDate)

private fun RecipeContext.toJson(): JSONObject =
    JSONObject()
        .put("recipeName", recipeName)
        .put("availableIngredients", JSONArray(availableIngredients))
        .put("missingIngredients", JSONArray(missingIngredients))
        .put("steps", JSONArray(steps))

private fun JSONArray.joinNames(): String =
    (0 until length()).joinToString(", ") { index ->
        val value = opt(index)
        when (value) {
            is JSONObject -> value.optString("name")
            else -> value?.toString().orEmpty()
        }
    }.trim()

private fun JSONArray.joinSteps(): String =
    (0 until length()).joinToString("|") { index ->
        val value = opt(index)
        when (value) {
            is JSONObject -> value.optString("text")
            else -> value?.toString().orEmpty()
        }
    }.trim()

private fun JSONObject.optDoubleOrNull(name: String): Double? {
    if (!has(name) || isNull(name)) return null
    return runCatching { getDouble(name) }.getOrNull()
}

object ProductDates {
    fun offsetDate(offsetDays: Int): String {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DATE, offsetDays)
        return format.format(cal.time)
    }
}
