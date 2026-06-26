package com.example.data

import java.util.Locale
import kotlin.math.abs

object IngredientNormalizer {
    private val ignoredWords = setOf(
        "fresh", "dried", "chopped", "sliced", "minced", "optional", "large", "small", "medium"
    )

    fun normalize(value: String): String = value
        .lowercase(Locale.US)
        .replace(Regex("""[^\p{L}\p{N}\s]"""), " ")
        .split(Regex("""\s+"""))
        .filter { it.isNotBlank() && it !in ignoredWords }
        .map(::singularize)
        .joinToString(" ")
        .trim()

    fun matches(first: String, second: String): Boolean {
        val a = normalize(first)
        val b = normalize(second)
        return a.isNotBlank() && b.isNotBlank() && (
            a == b ||
                (a.length >= 4 && b.contains(a)) ||
                (b.length >= 4 && a.contains(b))
            )
    }

    private fun singularize(token: String): String = when {
        token.endsWith("ies") && token.length > 4 -> token.dropLast(3) + "y"
        token.endsWith("es") && token.length > 4 && !token.endsWith("ese") -> token.dropLast(2)
        token.endsWith("s") && token.length > 3 && !token.endsWith("ss") -> token.dropLast(1)
        else -> token
    }
}

object UnitConverter {
    private val massToGrams = mapOf("g" to 1.0, "kg" to 1000.0)
    private val volumeToMl = mapOf(
        "ml" to 1.0,
        "l" to 1000.0,
        "tsp" to 4.92892,
        "tbsp" to 14.7868,
        "cup" to 236.588
    )

    fun canonicalUnit(unit: String): String = when (unit.trim().lowercase(Locale.US)) {
        "liter", "liters", "litre", "litres", "l" -> "L"
        "milliliter", "milliliters", "millilitre", "millilitres", "ml" -> "ml"
        "gram", "grams", "g" -> "g"
        "kilogram", "kilograms", "kg" -> "kg"
        "piece", "pieces", "pc", "pcs" -> "pcs"
        else -> unit.trim().lowercase(Locale.US)
    }

    fun areCompatible(first: String, second: String): Boolean {
        val a = canonicalUnit(first)
        val b = canonicalUnit(second)
        return a == b ||
            (a.lowercase(Locale.US) in massToGrams && b.lowercase(Locale.US) in massToGrams) ||
            (a.lowercase(Locale.US) in volumeToMl && b.lowercase(Locale.US) in volumeToMl)
    }

    fun convert(quantity: Double, from: String, to: String): Double? {
        val source = canonicalUnit(from).lowercase(Locale.US)
        val target = canonicalUnit(to).lowercase(Locale.US)
        if (source == target) return quantity
        massToGrams[source]?.let { sourceFactor ->
            val targetFactor = massToGrams[target] ?: return@let
            return quantity * sourceFactor / targetFactor
        }
        volumeToMl[source]?.let { sourceFactor ->
            val targetFactor = volumeToMl[target] ?: return@let
            return quantity * sourceFactor / targetFactor
        }
        return null
    }
}

data class ShoppingMergeResult(
    val mergedItem: ShoppingItem?,
    val newItem: ShoppingItem?
)

object ShoppingListMerger {
    fun merge(
        existing: List<ShoppingItem>,
        incoming: ShoppingItem
    ): ShoppingMergeResult {
        val match = existing.firstOrNull {
            !it.isChecked &&
                IngredientNormalizer.matches(it.name, incoming.name) &&
                UnitConverter.areCompatible(it.unit, incoming.unit)
        }
        if (match == null) return ShoppingMergeResult(null, incoming)

        val converted = UnitConverter.convert(incoming.quantity, incoming.unit, match.unit)
            ?: return ShoppingMergeResult(null, incoming)
        val sources = (match.sourceRecipeNames() + incoming.sourceRecipeNames()).distinct()
        return ShoppingMergeResult(
            mergedItem = match.copy(
                quantity = match.quantity + converted,
                category = if (match.category == "My List") incoming.category else match.category,
                sourceRecipeName = sources.firstOrNull(),
                sourceRecipeNamesJson = encodeStringArray(sources)
            ),
            newItem = null
        )
    }

    private fun encodeStringArray(values: List<String>): String =
        values.joinToString(prefix = "[", postfix = "]") { value ->
            "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        }
}

data class IngredientUsage(
    val name: String,
    val quantity: Double,
    val unit: String
)

data class PantryDeductionPlan(
    val updates: List<Ingredient>,
    val deleteIds: List<Int>,
    val shortfalls: List<IngredientUsage>
) {
    val updatedCount: Int get() = updates.size
    val removedCount: Int get() = deleteIds.size
}

object PantryDeductionPlanner {
    private const val EPSILON = 0.000001

    fun plan(pantry: List<Ingredient>, usages: List<IngredientUsage>): PantryDeductionPlan {
        val mutablePantry = pantry.associateBy { it.id }.toMutableMap()
        val shortfalls = mutableListOf<IngredientUsage>()

        usages.filter { it.quantity > 0.0 }.forEach { usage ->
            var remaining = usage.quantity
            val candidates = mutablePantry.values
                .filter {
                    IngredientNormalizer.matches(it.name, usage.name) &&
                        UnitConverter.areCompatible(it.unit, usage.unit)
                }
                .sortedWith(
                    compareBy<Ingredient> { if (it.hasTrackedDate) 0 else 1 }
                        .thenBy { it.expirationDate.ifBlank { "9999-12-31" } }
                        .thenBy { it.id }
                )

            candidates.forEach { pantryItem ->
                if (remaining <= EPSILON) return@forEach
                val availableInUsageUnit = UnitConverter.convert(
                    pantryItem.quantity,
                    pantryItem.unit,
                    usage.unit
                ) ?: return@forEach
                val consumedInUsageUnit = minOf(remaining, availableInUsageUnit)
                val consumedInPantryUnit = UnitConverter.convert(
                    consumedInUsageUnit,
                    usage.unit,
                    pantryItem.unit
                ) ?: return@forEach
                mutablePantry[pantryItem.id] = pantryItem.copy(
                    quantity = (pantryItem.quantity - consumedInPantryUnit).coerceAtLeast(0.0)
                )
                remaining -= consumedInUsageUnit
            }
            if (remaining > EPSILON) {
                shortfalls += usage.copy(quantity = remaining)
            }
        }

        val changed = mutablePantry.values.filter { updated ->
            val original = pantry.firstOrNull { it.id == updated.id } ?: return@filter false
            abs(original.quantity - updated.quantity) > EPSILON
        }
        return PantryDeductionPlan(
            updates = changed.filter { it.quantity > EPSILON },
            deleteIds = changed.filter { it.quantity <= EPSILON }.map { it.id },
            shortfalls = shortfalls
        )
    }
}
