package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IngredientDateType
import com.example.data.IngredientStatus
import com.example.ui.theme.SoftGrayText
import com.example.ui.theme.isDark
import com.example.ui.viewmodel.ShelfLifeViewModel
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AddIngredientScreen(
    viewModel: ShelfLifeViewModel,
    onBack: () -> Unit,
    ingredientId: Int? = null
) {
    val isDark = MaterialTheme.colorScheme.isDark
    val inputBackground = if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val today = remember { dateFormat.format(Date()) }
    val estimatedDate = remember {
        Calendar.getInstance().run {
            add(Calendar.DATE, 7)
            dateFormat.format(time)
        }
    }

    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Produce") }
    var quantity by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("pcs") }
    var location by remember { mutableStateOf("Fridge") }
    var purchaseDate by remember { mutableStateOf(today) }
    var dateType by remember { mutableStateOf(IngredientDateType.ESTIMATED_USE_BY) }
    var trackedDate by remember { mutableStateOf(estimatedDate) }
    var itemStatus by remember { mutableStateOf(IngredientStatus.SEALED) }
    var openedDate by remember { mutableStateOf("") }
    var storageCondition by remember { mutableStateOf("") }
    var lowStockReminder by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var packageSize by remember { mutableStateOf("") }
    var store by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }
    var showMoreDetails by remember(ingredientId) { mutableStateOf(ingredientId != null) }

    val existingIngredient by remember(ingredientId) {
        if (ingredientId == null) flowOf(null) else viewModel.getIngredientDetails(ingredientId)
    }.collectAsState(initial = null)

    LaunchedEffect(existingIngredient?.id) {
        existingIngredient?.let { ingredient ->
            name = ingredient.name
            category = ingredient.category
            quantity = formatNumber(ingredient.quantity)
            unit = ingredient.unit
            location = ingredient.location
            purchaseDate = ingredient.purchaseDate
            dateType = ingredient.dateType
            trackedDate = ingredient.expirationDate
            itemStatus = ingredient.itemStatus
            openedDate = ingredient.openedDate
            storageCondition = ingredient.storageCondition
            lowStockReminder = ingredient.lowStockThreshold
                .takeIf { it >= 0.0 }
                ?.let(::formatNumber)
                .orEmpty()
            brand = ingredient.brand
            barcode = ingredient.barcode
            packageSize = ingredient.packageSize
            store = ingredient.store
            price = ingredient.price?.let(::formatNumber).orEmpty()
            notes = ingredient.notes
        }
    }

    val categories = listOf("Produce", "Dairy", "Meat", "Grains", "Pantry")
    val units = listOf(
        "pcs", "pack", "box", "can", "bottle", "jar", "container",
        "kg", "g", "L", "ml", "cup", "tbsp", "tsp"
    )
    val locations = listOf("Fridge", "Pantry", "Freezer")
    val storageConditions = listOf(
        "Normal fridge",
        "Crisper drawer",
        "Freezer",
        "Room temperature",
        "Cool dry place",
        "Refrigerate after opening"
    )
    val requiresOpenedDate = IngredientStatus.requiresOpenedDate(itemStatus)
    val requiresTrackedDate = dateType != IngredientDateType.NO_PRINTED_DATE

    LaunchedEffect(dateType) {
        if (dateType == IngredientDateType.NO_PRINTED_DATE) trackedDate = ""
    }

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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (ingredientId == null) "Add Ingredient" else "Edit Ingredient",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {}, enabled = false) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0f)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (ingredientId == null) {
                    "Track quantity, storage, and freshness."
                } else {
                    "Update item details and freshness tracking."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FormSection(title = "Basic Info") {
                FormTextField(
                    label = "Ingredient Name *",
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "e.g. Organic Strawberries",
                    inputBackground = inputBackground,
                    modifier = Modifier.testTag("ingredient_name_field")
                )

                FormLabel("Category *")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    categories.forEach { option ->
                        val selected = category == option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else inputBackground
                                )
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    category = option
                                    if (
                                        ingredientId == null &&
                                        option == "Produce" &&
                                        dateType == IngredientDateType.EXPIRATION
                                    ) {
                                        dateType = IngredientDateType.ESTIMATED_USE_BY
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(categoryEmoji(option), fontSize = 19.sp)
                                Text(
                                    text = option,
                                    fontSize = 10.sp,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else if (isDark) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        SoftGrayText
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            FormSection(title = "Stock & Storage") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    FormTextField(
                        label = "Quantity *",
                        value = quantity,
                        onValueChange = { quantity = it },
                        inputBackground = inputBackground,
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier
                            .weight(1.35f)
                            .testTag("ingredient_qty_field")
                    )
                    FormDropdown(
                        label = "Unit *",
                        value = unit,
                        options = units,
                        onSelected = { unit = it },
                        inputBackground = inputBackground,
                        modifier = Modifier.weight(1f)
                    )
                }

                FormLabel("Storage Location *")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    locations.forEach { option ->
                        val selected = location == option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else inputBackground
                                )
                                .border(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { location = option }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            FormSection(title = "Date Tracking") {
                FormDropdown(
                    label = "Date Type *",
                    value = dateType,
                    options = IngredientDateType.options,
                    onSelected = {
                        dateType = it
                        if (it == IngredientDateType.NO_PRINTED_DATE) trackedDate = ""
                    },
                    inputBackground = inputBackground
                )

                if (category == "Produce") {
                    Text(
                        text = "Fresh produce usually has no strict printed expiration. " +
                            "Estimated Use-By Date is recommended.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (requiresTrackedDate) {
                    FormTextField(
                        label = "${IngredientDateType.fieldLabel(dateType)} *",
                        value = trackedDate,
                        onValueChange = { trackedDate = it },
                        placeholder = "YYYY-MM-DD",
                        inputBackground = inputBackground,
                        modifier = Modifier.testTag("expiration_date_field")
                    )
                } else {
                    Text(
                        text = "No expiration alerts will be sent until a date is added.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showMoreDetails = !showMoreDetails }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showMoreDetails) "Hide details" else "More details (optional)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (showMoreDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            if (showMoreDetails) {
                FormSection(title = "More Details (Optional)") {
                    FormDropdown(
                        label = "Storage Condition",
                        value = storageCondition,
                        placeholder = "Select storage condition",
                        options = storageConditions,
                        onSelected = { storageCondition = it },
                        inputBackground = inputBackground,
                        allowClear = true
                    )

                    FormTextField(
                        label = "Purchase Date",
                        value = purchaseDate,
                        onValueChange = { purchaseDate = it },
                        placeholder = "YYYY-MM-DD",
                        inputBackground = inputBackground
                    )

                    FormDropdown(
                        label = "Item Status *",
                        value = itemStatus,
                        options = IngredientStatus.options,
                        onSelected = {
                            itemStatus = it
                            if (!IngredientStatus.requiresOpenedDate(it)) openedDate = ""
                        },
                        inputBackground = inputBackground
                    )

                    if (requiresOpenedDate) {
                        FormTextField(
                            label = "Opened Date *",
                            value = openedDate,
                            onValueChange = { openedDate = it },
                            placeholder = "YYYY-MM-DD",
                            inputBackground = inputBackground
                        )
                    }

                    FormTextField(
                        label = "Low Stock Reminder",
                        value = lowStockReminder,
                        onValueChange = { lowStockReminder = it },
                        placeholder = "Notify me when quantity reaches... ($unit)",
                        inputBackground = inputBackground,
                        keyboardType = KeyboardType.Decimal
                    )

                    Text(
                        text = "Product details",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    FormTextField(
                        label = "Brand",
                        value = brand,
                        onValueChange = { brand = it },
                        placeholder = "e.g. Dole",
                        inputBackground = inputBackground
                    )
                    FormTextField(
                        label = "Barcode",
                        value = barcode,
                        onValueChange = { barcode = it.filter(Char::isDigit) },
                        placeholder = "e.g. 5449000000996",
                        inputBackground = inputBackground,
                        keyboardType = KeyboardType.Number
                    )
                    FormTextField(
                        label = "Package Size",
                        value = packageSize,
                        onValueChange = { packageSize = it },
                        placeholder = "e.g. 500 g or 12-pack",
                        inputBackground = inputBackground
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        FormTextField(
                            label = "Store / Purchased From",
                            value = store,
                            onValueChange = { store = it },
                            placeholder = "e.g. Local market",
                            inputBackground = inputBackground,
                            modifier = Modifier.weight(1.4f)
                        )
                        FormTextField(
                            label = "Price",
                            value = price,
                            onValueChange = { price = it },
                            placeholder = "0.00",
                            inputBackground = inputBackground,
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    FormTextField(
                        label = "Notes",
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = "Optional: storage notes, meal plan, or handling instructions",
                        inputBackground = inputBackground,
                        singleLine = false,
                        minLines = 2
                    )
                }
            }

            formError?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Button(
                onClick = {
                    val parsedQuantity = quantity.toDoubleOrNull()
                    val parsedReminder = lowStockReminder
                        .takeIf(String::isNotBlank)
                        ?.toDoubleOrNull()
                    val parsedPrice = price.takeIf(String::isNotBlank)?.toDoubleOrNull()

                    formError = when {
                        name.isBlank() -> "Ingredient name is required."
                        category.isBlank() -> "Category is required."
                        parsedQuantity == null || parsedQuantity <= 0.0 ->
                            "Quantity must be greater than zero."
                        unit.isBlank() -> "Unit is required."
                        location.isBlank() -> "Storage location is required."
                        dateType.isBlank() -> "Date type is required."
                        itemStatus.isBlank() -> "Item status is required."
                        purchaseDate.isNotBlank() && !isValidDate(purchaseDate) ->
                            "Purchase date must use YYYY-MM-DD format."
                        requiresTrackedDate && !isValidDate(trackedDate) ->
                            "${IngredientDateType.fieldLabel(dateType)} is required in YYYY-MM-DD format."
                        requiresOpenedDate && !isValidDate(openedDate) ->
                            "Opened date is required in YYYY-MM-DD format."
                        lowStockReminder.isNotBlank() &&
                            (parsedReminder == null || parsedReminder < 0.0) ->
                            "Low stock reminder must be zero or greater."
                        price.isNotBlank() && (parsedPrice == null || parsedPrice < 0.0) ->
                            "Price must be a valid amount of zero or greater."
                        else -> null
                    }

                    if (formError == null) {
                        val reminder = parsedReminder ?: -1.0
                        val dateValue = if (requiresTrackedDate) trackedDate else ""
                        val openedDateValue = if (requiresOpenedDate) openedDate else ""
                        val existing = existingIngredient

                        if (existing == null) {
                            viewModel.addIngredient(
                                name = name,
                                category = category,
                                quantity = parsedQuantity!!,
                                unit = unit,
                                expirationDate = dateValue,
                                purchaseDate = purchaseDate,
                                location = location,
                                lowStockThreshold = reminder,
                                notes = notes,
                                dateType = dateType,
                                itemStatus = itemStatus,
                                openedDate = openedDateValue,
                                storageCondition = storageCondition,
                                brand = brand,
                                barcode = barcode,
                                packageSize = packageSize,
                                store = store,
                                price = parsedPrice
                            )
                        } else {
                            viewModel.updateIngredient(
                                existing.copy(
                                    name = name.trim(),
                                    category = category,
                                    quantity = parsedQuantity!!,
                                    unit = unit,
                                    expirationDate = dateValue,
                                    purchaseDate = purchaseDate,
                                    location = location,
                                    lowStockThreshold = reminder,
                                    notes = notes.trim(),
                                    dateType = dateType,
                                    itemStatus = itemStatus,
                                    openedDate = openedDateValue,
                                    storageCondition = storageCondition,
                                    brand = brand.trim(),
                                    barcode = barcode.trim(),
                                    packageSize = packageSize.trim(),
                                    store = store.trim(),
                                    price = parsedPrice
                                )
                            )
                        }
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 40.dp)
                    .testTag("save_ingredient_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = CircleShape
            ) {
                Text(
                    text = if (ingredientId == null) "Save Ingredient" else "Update Ingredient",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                RoundedCornerShape(22.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        content()
    }
}

@Composable
private fun FormLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun FormTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    inputBackground: Color,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    Column(modifier = modifier) {
        FormLabel(label)
        Spacer(Modifier.size(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = if (placeholder.isBlank()) null else ({ Text(placeholder) }),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = singleLine,
            minLines = minLines,
            shape = RoundedCornerShape(16.dp),
            colors = formFieldColors(inputBackground)
        )
    }
}

@Composable
private fun FormDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    inputBackground: Color,
    modifier: Modifier = Modifier,
    placeholder: String = "Select an option",
    allowClear: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        FormLabel(label)
        Spacer(Modifier.size(6.dp))
        Box {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                placeholder = { Text(placeholder) },
                trailingIcon = {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Show options")
                },
                shape = RoundedCornerShape(16.dp),
                colors = formFieldColors(inputBackground)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { expanded = true }
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (allowClear && value.isNotBlank()) {
                    DropdownMenuItem(
                        text = { Text("None") },
                        onClick = {
                            onSelected("")
                            expanded = false
                        }
                    )
                }
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun formFieldColors(inputBackground: Color) = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = inputBackground,
    unfocusedContainerColor = inputBackground,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
)

private fun isValidDate(value: String): Boolean = try {
    SimpleDateFormat("yyyy-MM-dd", Locale.US).run {
        isLenient = false
        parse(value)
    }
    true
} catch (_: Exception) {
    false
}

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

private fun categoryEmoji(category: String): String = when (category) {
    "Produce" -> "🥬"
    "Dairy" -> "🥛"
    "Meat" -> "🍗"
    "Grains" -> "🍞"
    "Pantry" -> "🧂"
    else -> "🍎"
}
