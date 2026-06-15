package com.example.data

import android.util.Log
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

// --- Open Food Facts API Moshi Models ---

@JsonClass(generateAdapter = true)
data class OFFProduct(
    val product_name: String? = null,
    val categories: String? = null,
    val quantity: String? = null,
    val brands: String? = null
)

@JsonClass(generateAdapter = true)
data class OFFResponse(
    val status: Int? = 0,
    val product: OFFProduct? = null
)

// --- Retrofit Service ---

interface OpenFoodFactsApiService {
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProductDetails(
        @Path("barcode") barcode: String
    ): OFFResponse
}

// --- OFF Client Builders ---

object OFFClient {
    private const val BASE_URL = "https://world.openfoodfacts.org/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val service: OpenFoodFactsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenFoodFactsApiService::class.java)
    }
}

// --- Helper Functions to Parse Quantity and Map Category ---

object ProductLookupHelper {

    suspend fun lookupBarcode(
        barcode: String,
        userId: String,
        aiFallback: suspend () -> Ingredient?
    ): Ingredient? = withContext(Dispatchers.IO) {
        val trimmed = barcode.trim()
        if (trimmed.isBlank()) return@withContext null

        try {
            val response = OFFClient.service.getProductDetails(trimmed)
            if (response.status == 1 && response.product != null) {
                val prod = response.product
                val rawName = prod.product_name ?: "Unknown Food Item"
                val brand = prod.brands?.split(",")?.firstOrNull()?.trim()
                val fullName = if (brand.isNullOrBlank()) rawName else "$brand $rawName"
                
                val category = mapCategoriesToPantryCategory(prod.categories, rawName)
                val (qty, unit) = parseQuantity(prod.quantity)

                return@withContext Ingredient(
                    userId = userId,
                    name = fullName,
                    category = category,
                    quantity = qty,
                    unit = unit,
                    expirationDate = ProductDates.offsetDate(10),
                    purchaseDate = ProductDates.offsetDate(0),
                    location = mapCategoryToLocation(category),
                    notes = "Expiration date estimated. Review before adding."
                )
            }
        } catch (e: Exception) {
            Log.e("ProductLookup", "OpenFoodFacts API error: ${e.message}")
        }

        try {
            return@withContext aiFallback()
        } catch (e: Exception) {
            Log.e("ProductLookup", "AI barcode fallback error: ${e.message}")
        }

        null
    }

    private fun mapCategoriesToPantryCategory(offCategories: String?, productName: String): String {
        val combined = "${offCategories ?: ""} ${productName}".lowercase()
        return when {
            combined.contains("milk") || combined.contains("cheese") || combined.contains("butter") ||
            combined.contains("yogurt") || combined.contains("cream") || combined.contains("dairy") -> "Dairy"
            combined.contains("spinach") || combined.contains("salad") || combined.contains("vegetable") ||
            combined.contains("fruit") || combined.contains("berry") || combined.contains("tomato") ||
            combined.contains("lettuce") || combined.contains("basil") || combined.contains("garlic") ||
            combined.contains("onion") || combined.contains("produce") -> "Produce"
            combined.contains("chicken") || combined.contains("beef") || combined.contains("pork") ||
            combined.contains("meat") || combined.contains("ham") || combined.contains("fish") ||
            combined.contains("turkey") || combined.contains("seafood") -> "Meat"
            combined.contains("rice") || combined.contains("pasta") || combined.contains("spaghetti") ||
            combined.contains("flour") || combined.contains("noodle") || combined.contains("grain") ||
            combined.contains("oat") || combined.contains("cereal") -> "Grains"
            combined.contains("bread") || combined.contains("pastry") || combined.contains("bakery") ||
            combined.contains("bagel") || combined.contains("croissant") -> "Bakery"
            combined.contains("beverage") || combined.contains("drink") || combined.contains("juice") ||
            combined.contains("soda") || combined.contains("cola") || combined.contains("tea") ||
            combined.contains("coffee") || combined.contains("water") -> "Beverages"
            else -> "Pantry"
        }
    }

    private fun mapCategoryToLocation(category: String): String {
        return when (category) {
            "Dairy", "Produce" -> "Fridge"
            "Meat" -> "Freezer"
            else -> "Pantry"
        }
    }

    private fun parseQuantity(qtyStr: String?): Pair<Double, String> {
        if (qtyStr.isNullOrBlank()) return Pair(1.0, "pcs")
        try {
            val clean = qtyStr.trim().lowercase()
            // Extract numeric values and text units
            val digitRegex = """([0-9.,]+)""".toRegex()
            val matchDigit = digitRegex.find(clean)?.value?.replace(",", ".")?.toDoubleOrNull() ?: 1.0

            val unit = when {
                clean.contains("kg") || clean.contains("kilogram") -> "kg"
                clean.contains("ml") || clean.contains("milliliter") -> "ml"
                clean.contains("cl") || clean.contains("dl") -> "ml"
                clean.contains("l") || clean.contains("liter") -> "L"
                clean.contains("g") || clean.contains("gram") -> "g"
                clean.contains("pack") || clean.contains("pkt") -> "pack"
                else -> "pcs"
            }
            return Pair(matchDigit, unit)
        } catch (e: Exception) {
            return Pair(1.0, "pcs")
        }
    }

}
