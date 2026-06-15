package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShelfLifeViewModel
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AddIngredientScreen(
    viewModel: ShelfLifeViewModel,
    onBack: () -> Unit,
    ingredientId: Int? = null
) {
    val isDark = MaterialTheme.colorScheme.isDark
    val inputBgColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White

    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Produce") }
    var quantityStr by remember { mutableStateOf("1.0") }
    var unit by remember { mutableStateOf("pcs") }
    
    // Auto-prepopulate dates
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val todayStr = remember { dateFormat.format(Date()) }
    val sevenDaysStr = remember { 
        val c = Calendar.getInstance()
        c.add(Calendar.DATE, 7)
        dateFormat.format(c.time)
    }

    var purchaseDate by remember { mutableStateOf(todayStr) }
    var expirationDate by remember { mutableStateOf(sevenDaysStr) }
    var location by remember { mutableStateOf("Fridge") }
    var thresholdStr by remember { mutableStateOf("1.0") }
    var notes by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }
    val existingIngredient by remember(ingredientId) {
        if (ingredientId == null) flowOf(null) else viewModel.getIngredientDetails(ingredientId)
    }.collectAsState(initial = null)

    LaunchedEffect(existingIngredient?.id) {
        val ingredient = existingIngredient ?: return@LaunchedEffect
        name = ingredient.name
        category = ingredient.category
        quantityStr = ingredient.quantity.toString()
        unit = ingredient.unit
        purchaseDate = ingredient.purchaseDate
        expirationDate = ingredient.expirationDate
        location = ingredient.location
        thresholdStr = ingredient.lowStockThreshold.toString()
        notes = ingredient.notes
    }

    val categories = listOf("Produce", "Dairy", "Meat", "Grains", "Pantry")
    val units = listOf("pcs", "kg", "g", "L", "ml", "pack")
    val locations = listOf("Fridge", "Pantry", "Freezer")

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Go back")
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (ingredientId == null) "Add Ingredient" else "Edit Ingredient",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = {}, enabled = false) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color.Transparent)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Name field
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Ingredient Name *",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("e.g. Organic Strawberries") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = inputBgColor,
                        unfocusedContainerColor = inputBgColor,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ingredient_name_field")
                )
            }

            // Category list selector chips
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val selected = cat == category
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else inputBgColor)
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { category = cat }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val emoji = when (cat) {
                                "Produce" -> "🥬"
                                "Dairy" -> "🥛"
                                "Meat" -> "🍗"
                                "Grains" -> "🍞"
                                "Pantry" -> "🧂"
                                else -> "🍎"
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(emoji, fontSize = 20.sp)
                                Text(
                                    text = cat,
                                    fontSize = 10.sp,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SoftGrayText
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Quantity and Unit row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1.5f)) {
                    Text(
                        text = "Quantity",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = inputBgColor,
                            unfocusedContainerColor = inputBgColor,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ingredient_qty_field")
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Unit",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    var isUnitExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = inputBgColor,
                                unfocusedContainerColor = inputBgColor,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isUnitExpanded = true }
                        )
                        DropdownMenu(
                            expanded = isUnitExpanded,
                            onDismissRequest = { isUnitExpanded = false }
                        ) {
                            units.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u) },
                                    onClick = {
                                        unit = u
                                        isUnitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Location picker
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Storage Location",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    locations.forEach { loc ->
                        val selected = loc == location
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else inputBgColor)
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { location = loc }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = loc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SoftGrayText
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Expiration Date field (Simple date strings, prepopulated for high comfort)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Purchase Date",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = purchaseDate,
                        onValueChange = { purchaseDate = it },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = inputBgColor,
                            unfocusedContainerColor = inputBgColor,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Expiration Date *",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = expirationDate,
                        onValueChange = { expirationDate = it },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = inputBgColor,
                            unfocusedContainerColor = inputBgColor,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expiration_date_field")
                    )
                }
            }

            // Low stock threshold
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Low Stock Alert Threshold",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = thresholdStr,
                    onValueChange = { thresholdStr = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    placeholder = { Text("Alert when stocks drop below this amount") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = inputBgColor,
                        unfocusedContainerColor = inputBgColor,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Notes field
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Notes (Optional)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("Optional: brand, storage notes, or meal plan") },
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = inputBgColor,
                        unfocusedContainerColor = inputBgColor,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            formError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Save actions
            Button(
                onClick = {
                    val qty = quantityStr.toDoubleOrNull()
                    val threshold = thresholdStr.toDoubleOrNull()
                    formError = when {
                        name.isBlank() -> "Ingredient name is required."
                        qty == null || qty <= 0.0 -> "Quantity must be a number greater than zero."
                        threshold == null || threshold < 0.0 -> "Low stock threshold must be zero or greater."
                        !isValidDate(purchaseDate) || !isValidDate(expirationDate) -> "Dates must use YYYY-MM-DD format."
                        else -> null
                    }
                    if (formError == null) {
                        val existing = existingIngredient
                        if (existing == null) {
                            viewModel.addIngredient(
                                name = name,
                                category = category,
                                quantity = qty!!,
                                unit = unit,
                                expirationDate = expirationDate,
                                purchaseDate = purchaseDate,
                                location = location,
                                lowStockThreshold = threshold!!,
                                notes = notes
                            )
                        } else {
                            viewModel.updateIngredient(
                                existing.copy(
                                    name = name.trim(),
                                    category = category,
                                    quantity = qty!!,
                                    unit = unit,
                                    expirationDate = expirationDate,
                                    purchaseDate = purchaseDate,
                                    location = location,
                                    lowStockThreshold = threshold!!,
                                    notes = notes.trim()
                                )
                            )
                        }
                        onBack()
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp)
                    .testTag("save_ingredient_button")
            ) {
                Text(if (ingredientId == null) "Save Ingredient" else "Update Ingredient", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

private fun isValidDate(value: String): Boolean {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        format.isLenient = false
        format.parse(value)
        true
    } catch (_: Exception) {
        false
    }
}
