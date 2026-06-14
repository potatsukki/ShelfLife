package com.example.data

import android.util.Log
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ShelfLifeRepository(private val dao: ShelfLifeDao) {

    // --- Ingredients ---
    val allIngredients: Flow<List<Ingredient>> = dao.getAllIngredients()

    suspend fun insertIngredient(ingredient: Ingredient) = withContext(Dispatchers.IO) {
        dao.insertIngredient(ingredient)
    }

    suspend fun deleteIngredient(ingredient: Ingredient) = withContext(Dispatchers.IO) {
        dao.deleteIngredient(ingredient)
    }

    suspend fun deleteIngredientById(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteIngredientById(id)
    }

    suspend fun getIngredientById(id: Int): Ingredient? = withContext(Dispatchers.IO) {
        dao.getIngredientById(id)
    }

    // --- Shopping items ---
    val allShoppingItems: Flow<List<ShoppingItem>> = dao.getAllShoppingItems()

    suspend fun insertShoppingItem(item: ShoppingItem) = withContext(Dispatchers.IO) {
        dao.insertShoppingItem(item)
    }

    suspend fun updateShoppingItem(item: ShoppingItem) = withContext(Dispatchers.IO) {
        dao.updateShoppingItem(item)
    }

    suspend fun deleteShoppingItem(item: ShoppingItem) = withContext(Dispatchers.IO) {
        dao.deleteShoppingItem(item)
    }

    suspend fun clearCheckedShoppingItems() = withContext(Dispatchers.IO) {
        dao.clearCheckedShoppingItems()
    }

    suspend fun deleteShoppingItemById(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteShoppingItemById(id)
    }

    // --- Saved Recipes ---
    val allSavedRecipes: Flow<List<SavedRecipe>> = dao.getAllSavedRecipes()

    suspend fun insertSavedRecipe(recipe: SavedRecipe) = withContext(Dispatchers.IO) {
        dao.insertSavedRecipe(recipe)
    }

    suspend fun deleteSavedRecipe(recipe: SavedRecipe) = withContext(Dispatchers.IO) {
        dao.deleteSavedRecipe(recipe)
    }

    fun isRecipeSavedFlow(id: String): Flow<Boolean> = dao.isRecipeSavedFlow(id)

    suspend fun isRecipeSavedSync(id: String): Boolean = withContext(Dispatchers.IO) {
        dao.isRecipeSavedSync(id)
    }

    // --- AI Features (OpenRouter API with robust offline fallback) ---

    val staticRecipes = listOf(
        SavedRecipe(
            id = "chicken_stir_fry",
            name = "Chicken Stir Fry",
            prepTime = "20 min",
            difficulty = "Easy",
            imageResUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuAQpywRVsQ0wBZrqaOJgmjmYGCaiLIp9iAlIPA1WO1fr-5tlUFhrT2lZdhmQ-MVNXlSYymK7nWeUXfGa6cltB4DdJQMDm-Eof9nze9uY4dAk7UJ9ZPQ_17bYOOkYO8-tEE6U0tf49Uzq1OVqy0IIg4SLhU6NYqYBEJ62q9juL17O-JIsrGy2I15NS84wvLr207-nSyT-vWtNFmSQ4srX-lfRnsnzbk9BXb8PJf4pv9MGgd53xn8ldal1D8m3kIeGt_53G7g_AbJo4ts",
            whySuggested = "Uses chicken and bell pepper expiring soon.",
            ingredientsCsv = "Chicken (2 lbs),Bell Pepper (2 medium),Onion (1 large),Soy Sauce (3 tbsp - Missing),Garlic (2 cloves - Missing)",
            stepsCsv = "Slice the chicken breast, bell pepper, and onion into thin, uniform strips. Mince the garlic if you have it.|Heat a large skillet or wok over medium-high heat with a splash of oil. Add the chicken and cook until browned, about 5-7 minutes.|Add the bell peppers and onions to the pan. Stir frequently for another 4 minutes until vegetables are tender-crisp.|Pour soy sauce over the mixture and toss to coat. Serve hot."
        ),
        SavedRecipe(
            id = "veggie_omelet",
            name = "Veggie Omelet",
            prepTime = "10 min",
            difficulty = "Easy",
            imageResUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuDZzzyHKGyym0wlRiCGc7ftl3t-HTNNmN3-kXhTRnyZ8KZKv5jQtC6RUU4KN6K5-2onqAe5mYbXfj_T5ksbeg8-HSEm1RByKEwytdEs_HvRbOtiXSW0N_zJd8-0124OcuHQrDYmuvnUUPg8Tj9pF7bq83umDVzII03UnG09PBZJpNZqneb0-SiDxnuW2RCB6RMCZUDi1Qiz8BIW3sEHWqo1JKVatQATb2OuzS6QOPVze-cE1Nb7X18q0UdqNeA80OyNjB41aZmTNPQv",
            whySuggested = "Uses eggs and spinach expiring soon.",
            ingredientsCsv = "Eggs (3 pcs),Spinach (1 pack),Cheese (1/2 cup - Missing),Salt & Pepper (to taste)",
            stepsCsv = "Crack eggs into a bowl, season with salt and pepper, and whisk until fully blended.|Heat a non-stick skillet over medium-low heat with a little butter or oil. Pour in eggs.|As eggs set, lift edges with a spatula to let uncooked egg run underneath. When almost set, add spinach and cheese on one half.|Fold the omelet in half using the spatula. Slide onto a plate and enjoy warm."
        ),
        SavedRecipe(
            id = "tomato_egg_stir_fry",
            name = "Tomato Egg Stir-fry",
            prepTime = "15 min",
            difficulty = "Easy",
            imageResUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuB0nEl22-KZ7k4RxeOkpibtGYxCkKcGTc5nnZWCp0Z9cOEmVb36N0rrny9y38LHsJ8QJ6wZddZm4u-M0uj4womhjJl52d9caGqsWXGBRUzjtDnc_0w1-EBZudrlry7nZB7zfo9_p8bEzo_D0tzTfe3nzPxyZb5Uy1hoPWCIcXTwfDLT-OY7wOuIL4xSWfCR8sLdDW951AOHteonRE3DvL-55R3Hw-vDSP00vl-0ijdBdpO3IE2x_w9JtujRg22mzZqioe9h9do4YCrz",
            whySuggested = "Quick, comforting, and perfect over rice.",
            ingredientsCsv = "Eggs (3 pcs),Tomatoes (2 medium - Missing),Green Onion (1 stalk),Soy Sauce (1 tsp),Water (1 tbsp)",
            stepsCsv = "Beat the eggs in a bowl with a pinch of salt. Cut tomatoes into wedges.|Heat oil in a pan, pour in eggs, and scramble quickly until soft set. Remove eggs and set aside.|Add a bit more oil to the pan, cook the tomatoes until they soften and release juices, about 3-4 minutes.|Return eggs to the pan, add soy sauce, water, and sliced green onions. Stir family to combine and serve over warm white rice."
        ),
        SavedRecipe(
            id = "fried_rice",
            name = "Classic Egg Fried Rice",
            prepTime = "15 min",
            difficulty = "Easy",
            imageResUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuBiSjSukWU6DkmPm6amlJr8Qs6ln-ukcMdexW74NZpWuI13jEwvvNCmkwHUjaqmybqSHorPm7ROQtObFxfJFi78wosLQEwEMv9FYFDsjSY1fgsHeOAZVQ0bbrpvPzyJcw5dRRi8XScXt1sDn3Ymuf3jRHRIR1RM_SbMO1dvqcgZWyzwig7dfscko2dOsKmKluqtzfOqYinVLyBj9DzbPYwUNcYVkMjPEzWBxi6YGdESO6OGTwNy24Lc16Y17N00iJxg-KxJv8WV581U",
            whySuggested = "Great way to utilize leftover rice and eggs.",
            ingredientsCsv = "Rice (1 kg),Eggs (2 pcs),Green Beans (1/2 cup - Missing),Sesame Oil (1 tbsp - Missing),Soy Sauce (2 tbsp)",
            stepsCsv = "Heat a large skillet or wok with vegetable oil over high heat. Add beaten eggs and scramble until set, then set aside.|Add a bit more oil to the wok, scramble cold pre-cooked leftover rice, breaking any lumps with a spatula.|Pour soy sauce and green beans or carrots. Stir-fry for 3-5 minutes until heated through and fragrant.|Toss in scrambled eggs and optional sesame oil. Serve immediately as a fast culinary staple."
        )
    )

    suspend fun getAIRecipeSuggestions(pantryIngredients: List<Ingredient>): List<SavedRecipe> = withContext(Dispatchers.IO) {
        val ingredientsList = if (pantryIngredients.isEmpty()) {
            "Empty pantry"
        } else {
            pantryIngredients.joinToString { "- ${it.name} (${it.quantity} ${it.unit}), location: ${it.location}, expires: ${it.expirationDate}" }
        }

        val prompt = """
            We have a list of ingredients in our kitchen pantry:
            $ingredientsList
            
            Can you suggest 2 practical, delicious recipes? For each recipe, provide:
            1. Title
            2. Prep Time (minutes)
            3. Difficulty (Easy/Medium/Hard)
            4. Why suggested based on low-stock/expiring items
            5. Bulleted list of ingredients with quantities, marking whether we have it in pantry (based on list above) or if it is "Missing"
            6. Numbered steps to cook
            
            Give the output strictly in standard JSON format matching this array schema:
            [
              {
                "id": "recipe_one_id",
                "name": "Recipe Name",
                "prepTime": "15 min",
                "difficulty": "Easy",
                "imageResUrl": "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&auto=format&fit=crop",
                "whySuggested": "A concise reason pointing to expiring items",
                "ingredientsCsv": "Ingredient (Quantity - have), Missing Ingredient (Quantity - Missing)",
                "stepsCsv": "Step 1 text|Step 2 text|Step 3 text"
              }
            ]
            
            Do not wrap in markdown or backticks on output, just return the raw JSON array.
        """.trimIndent()

        try {
            val request = OpenRouterRequest(
                model = OpenRouterConfig.defaultModel,
                messages = listOf(
                    OpenRouterMessage(role = "user", content = prompt)
                ),
                temperature = 0.2f,
                response_format = OpenRouterResponseFormat(type = "json_object")
            )
            val response = OpenRouterClient.service.getChatCompletion(
                authHeader = OpenRouterConfig.authHeader,
                request = request
            )
            val responseText = response.choices?.firstOrNull()?.message?.content
            if (!responseText.isNullOrBlank()) {
                Log.d("ShelfLifeRepository", "OpenRouter suggestion response: $responseText")
                val parsed = parseRecipesJson(responseText)
                if (parsed.isNotEmpty()) {
                    return@withContext parsed
                }
            }
            staticRecipes
        } catch (e: Exception) {
            Log.e("ShelfLifeRepository", "Failed calling OpenRouter API: ${e.message}")
            staticRecipes
        }
    }

    suspend fun askAssistant(chatHistory: List<Pair<String, Boolean>>, latestUserMessage: String): String = withContext(Dispatchers.IO) {
        try {
            val messages = mutableListOf<OpenRouterMessage>()
            messages.add(
                OpenRouterMessage(
                    role = "system",
                    content = "You are Kitchen AI, the smart cooking and pantry assistant inside the ShelfLife android app. " +
                            "Your tone is warm, friendly, positive, and practical. Help the user cook beautifully, plan meals, " +
                            "reduce food waste, and organize their pantry."
                )
            )
            chatHistory.forEach { (text, isUser) ->
                messages.add(
                    OpenRouterMessage(
                        role = if (isUser) "user" else "assistant",
                        content = text
                    )
                )
            }
            messages.add(OpenRouterMessage(role = "user", content = latestUserMessage))

            val request = OpenRouterRequest(
                model = OpenRouterConfig.defaultModel,
                messages = messages,
                temperature = 0.7f
            )
            val response = OpenRouterClient.service.getChatCompletion(
                authHeader = OpenRouterConfig.authHeader,
                request = request
            )
            response.choices?.firstOrNull()?.message?.content
                ?: "I couldn't process that, please try again."
        } catch (e: Exception) {
            Log.e("ShelfLifeRepository", "Failed calling assistant: ${e.message}")
            handleOfflineChat(latestUserMessage)
        }
    }

    private fun handleOfflineChat(message: String): String {
        val msg = message.lowercase()
        return when {
            msg.contains("egg") && msg.contains("tomato") -> {
                "You can make a classic Tomato Egg Stir-fry! It perfectly uses the items you have (Eggs) and takes only about 15 minutes to prepare. Simply scramble the eggs first, then stir-fry sliced tomatoes, and fold them back in together with some soy sauce!"
            }
            msg.contains("spinach") || msg.contains("waste") -> {
                "To reduce spinach waste, you can steam it or toss it directly into a breakfast omelet! If you have too much spinach, blending it with water and freezing it into green ice cubes is a genius way to save it for future smoothies!"
            }
            msg.contains("recipe") || msg.contains("cook") -> {
                "Based on your stock, I recommend trying a Chicken Stir Fry or a fluffy Veggie Omelet. You can find these in the AI Recipes tab, and they'll help use up expiring items today!"
            }
            msg.contains("expire") || msg.contains("alert") -> {
                "ShelfLife automatically sends notifications 3 days before any ingredient expires, making it easy to plan meals around what needs attention. You can adjust this threshold in Settings!"
            }
            else -> {
                "Hi! I am your ShelfLife Kitchen AI assistant. If OpenRouter is configured, I can generate customized recipes based strictly on your pantry. Offline, I can help you with pantry tips or suggest classic recipes. Try asking: 'What can I cook with eggs and tomato?'"
            }
        }
    }

    suspend fun lookupProduct(barcode: String): Ingredient? {
        return ProductLookupHelper.lookupBarcode(barcode)
    }

    private fun parseRecipesJson(jsonStr: String): List<SavedRecipe> {
        try {
            var clean = jsonStr.trim()
            val startIdx = clean.indexOf('[')
            val endIdx = clean.lastIndexOf(']')
            if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                clean = clean.substring(startIdx, endIdx + 1)
            }
            val list = mutableListOf<SavedRecipe>()
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(Array<RecipeJsonModel>::class.java)
            val models = adapter.fromJson(clean)
            if (models != null) {
                for (m in models) {
                    list.add(
                        SavedRecipe(
                            id = m.id ?: "gen_${System.currentTimeMillis()}",
                            name = m.name ?: "Suggested Recipe",
                            prepTime = m.prepTime ?: "20 min",
                            difficulty = m.difficulty ?: "Easy",
                            imageResUrl = m.imageResUrl ?: "https://lh3.googleusercontent.com/aida-public/AB6AXuAvvfQSjCwITjsH56P_OI6kHMlTrpCCLHxalfB3_KRfFXjF-2-mrJi2RaANmx4QyN11gWF8JyspNLP2_Pr4rx7FSkiySlV_ShgGDyBYrvV30M4L-0r8IdCz1M3movy-0Fw9-NV01tLIiNKutbDFtJAFexbWEUrUzIbJ8U3qRJLXiKZxf-UTDESzmcko5YPNg1tLy9aJvBtvg5K-Q7SzMmc97MfhH9dU9h3p3z-aHEabpGg2NhE-6WmH4dv8yHC2pRuEIwY0uHOl4ZUd",
                            whySuggested = m.whySuggested ?: "Utilizes your pantry items.",
                            ingredientsCsv = m.ingredientsCsv ?: "",
                            stepsCsv = m.stepsCsv ?: ""
                        )
                    )
                }
            }
            return list
        } catch (e: Exception) {
            Log.e("ShelfLifeRepository", "Failed parsing generated recipes JSON: ${e.message}")
            return emptyList()
        }
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        try {
            dao.deleteAllIngredients()
            dao.deleteAllShoppingItems()
            dao.deleteAllSavedRecipes()
            Log.d("ShelfLifeRepository", "Database cleared successfully.")
        } catch (e: Exception) {
            Log.e("ShelfLifeRepository", "Failed to clear database: ${e.message}")
        }
    }
}

@JsonClass(generateAdapter = true)
data class RecipeJsonModel(
    val id: String?,
    val name: String?,
    val prepTime: String?,
    val difficulty: String?,
    val imageResUrl: String?,
    val whySuggested: String?,
    val ingredientsCsv: String?,
    val stepsCsv: String?
)
