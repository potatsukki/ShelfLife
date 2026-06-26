package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryDomainTest {
    @Test
    fun shoppingMergeCombinesSameIngredientAndCompatibleUnits() {
        val existing = ShoppingItem(
            id = 1,
            userId = "u1",
            name = "Eggs",
            quantity = 3.0,
            unit = "pcs",
            category = "Missing for Recipes",
            sourceRecipeName = "Pizza"
        )
        val incoming = ShoppingItem(
            userId = "u1",
            name = "egg",
            quantity = 4.0,
            unit = "pcs",
            category = "Missing for Recipes",
            sourceRecipeName = "Pie"
        )

        val result = ShoppingListMerger.merge(listOf(existing), incoming)

        assertEquals(null, result.newItem)
        assertEquals(7.0, result.mergedItem!!.quantity, 0.0001)
        assertEquals(listOf("Pizza", "Pie"), result.mergedItem.sourceRecipeNames())
    }

    @Test
    fun pantryDeductionConsumesEarliestTrackedDateFirstAndRemovesZeroRows() {
        val later = ingredient(id = 1, quantity = 6.0, expirationDate = "2026-06-30")
        val earlier = ingredient(id = 2, quantity = 3.0, expirationDate = "2026-06-20")

        val plan = PantryDeductionPlanner.plan(
            pantry = listOf(later, earlier),
            usages = listOf(IngredientUsage("Eggs", 4.0, "pcs"))
        )

        assertEquals(listOf(2), plan.deleteIds)
        assertEquals(1, plan.updates.size)
        assertEquals(5.0, plan.updates.first { it.id == 1 }.quantity, 0.0001)
        assertTrue(plan.shortfalls.isEmpty())
    }

    @Test
    fun pantryDeductionNeverCreatesNegativeStockAndReportsShortfall() {
        val plan = PantryDeductionPlanner.plan(
            pantry = listOf(ingredient(id = 1, quantity = 2.0)),
            usages = listOf(IngredientUsage("Eggs", 5.0, "pcs"))
        )

        assertEquals(listOf(1), plan.deleteIds)
        assertEquals(3.0, plan.shortfalls.single().quantity, 0.0001)
    }

    @Test
    fun unitConverterMergesCompatibleMassUnits() {
        val result = ShoppingListMerger.merge(
            existing = listOf(
                ShoppingItem(
                    id = 1,
                    userId = "u1",
                    name = "Flour",
                    quantity = 1.0,
                    unit = "kg"
                )
            ),
            incoming = ShoppingItem(
                userId = "u1",
                name = "flours",
                quantity = 500.0,
                unit = "g"
            )
        )

        assertEquals(1.5, result.mergedItem!!.quantity, 0.0001)
        assertEquals("kg", result.mergedItem.unit)
    }

    private fun ingredient(
        id: Int,
        quantity: Double,
        expirationDate: String = "2026-06-20"
    ) = Ingredient(
        id = id,
        userId = "u1",
        name = "Eggs",
        category = "Dairy",
        quantity = quantity,
        unit = "pcs",
        expirationDate = expirationDate,
        purchaseDate = "2026-06-16",
        location = "Fridge"
    )
}
