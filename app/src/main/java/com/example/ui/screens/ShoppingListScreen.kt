package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ShoppingItem
import com.example.data.IngredientDateType
import com.example.data.IngredientNormalizer
import com.example.data.IngredientStatus
import com.example.data.UnitConverter
import com.example.ui.components.ShelfLifeEmptyState
import com.example.ui.theme.*
import com.example.ui.viewmodel.PantryImportDraft
import com.example.ui.viewmodel.ShelfLifeViewModel

@Composable
fun ShoppingListScreen(viewModel: ShelfLifeViewModel) {
    val items by viewModel.shoppingItems.collectAsState()
    val pantry by viewModel.ingredients.collectAsState()
    val transferState by viewModel.pantryTransferState.collectAsState()
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.isDark
    val textInputBgColor = if (isDark) Color(0xFF354139) else Color.White

    var activeInput by remember { mutableStateOf("") }
    var showClearCheckedDialog by remember { mutableStateOf(false) }
    var showTransferSheet by remember { mutableStateOf(false) }
    var transferDrafts by remember { mutableStateOf<List<PantryImportDraft>>(emptyList()) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var addItemName by remember { mutableStateOf("") }
    var addItemQuantity by remember { mutableStateOf("1") }
    var addItemUnit by remember { mutableStateOf("pcs") }

    val unitOptions = listOf(
        "pcs",
        "pack",
        "box",
        "can",
        "bottle",
        "jar",
        "container",
        "kg",
        "g",
        "L",
        "ml",
        "cup",
        "tbsp",
        "tsp"
    )
    val checkedItems = remember(items) { items.filter { it.isChecked } }

    // Group items by category to display as checklist headers
    val groupedItems = remember(items) {
        items.groupBy { it.category }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header configuration
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Shopping List",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                if (checkedItems.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = {
                                transferDrafts = checkedItems.map { item ->
                                    val match = pantry.firstOrNull { pantryItem ->
                                        IngredientNormalizer.matches(pantryItem.name, item.name) &&
                                            UnitConverter.areCompatible(pantryItem.unit, item.unit)
                                    }
                                    PantryImportDraft(
                                        shoppingItemId = item.id,
                                        name = item.name,
                                        quantity = item.quantity,
                                        unit = item.unit,
                                        category = match?.category ?: "Pantry",
                                        location = match?.location ?: "Pantry",
                                        storageCondition = match?.storageCondition ?: "Cool dry place",
                                        mergeIngredientId = match?.id
                                    )
                                }
                                showTransferSheet = true
                            }
                        ) {
                            Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Add to Pantry", style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(
                            onClick = { showClearCheckedDialog = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (isDark) MaterialTheme.colorScheme.error else SoftCoralError
                            )
                        ) {
                            Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Clear", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = activeInput,
                    onValueChange = { activeInput = it },
                    placeholder = { Text("Add items quickly") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = textInputBgColor,
                        unfocusedContainerColor = textInputBgColor,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = if (isDark) Color(0xFF68736C) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("shopping_quick_field")
                )

                IconButton(
                    onClick = {
                        if (activeInput.isNotBlank()) {
                            addItemName = activeInput.trim()
                            addItemQuantity = "1"
                            addItemUnit = "pcs"
                            showAddItemDialog = true
                        }
                    },
                    enabled = activeInput.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (activeInput.isNotBlank()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add custom item",
                        tint = if (activeInput.isNotBlank()) Color.White else (
                            if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else SoftGrayText
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (items.isEmpty()) {
                ShelfLifeEmptyState(
                    emoji = "📝",
                    title = "Your list is empty",
                    description = "Add custom items above or open Pantry details to replenish low stock items."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Display groups
                    groupedItems.forEach { (category, categoryItems) ->
                        item {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = when (category) {
                                    "Suggested from Low Stock" -> {
                                        if (isDark) Color(0xFFFDDEAE) else OnPeachContainer
                                    }
                                    "Missing for Recipes" -> {
                                        if (isDark) Color(0xFFFFB4AB) else SoftCoralError
                                    }
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        items(categoryItems, key = { it.id }) { item ->
                            ShoppingItemRow(
                                item = item,
                                onCheckedChange = { viewModel.toggleShoppingItem(item) },
                                onDelete = { viewModel.deleteShoppingItem(item) }
                            )
                        }
                    }
                }
            }
        }

        if (showClearCheckedDialog) {
            AlertDialog(
                onDismissRequest = { showClearCheckedDialog = false },
                title = { Text("Clear checked items?") },
                text = { Text("This removes all completed shopping items from your list.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showClearCheckedDialog = false
                            viewModel.clearCheckedShoppingItems()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Clear")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearCheckedDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showAddItemDialog) {
            AlertDialog(
                onDismissRequest = { showAddItemDialog = false },
                title = { Text("Add ${addItemName.ifBlank { "item" }}") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = addItemQuantity,
                                onValueChange = { value ->
                                    addItemQuantity = value.filter { it.isDigit() || it == '.' }
                                },
                                label = { Text("Quantity") },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                                ),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            SimpleInlineDropdown(
                                label = "Unit",
                                value = addItemUnit,
                                options = unitOptions,
                                onSelected = { addItemUnit = it },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val qty = addItemQuantity.toDoubleOrNull()
                            if (addItemName.isNotBlank() && qty != null && qty > 0.0 && addItemUnit.isNotBlank()) {
                                viewModel.addShoppingItem(
                                    name = addItemName.trim(),
                                    category = "My List",
                                    quantity = qty,
                                    unit = addItemUnit
                                )
                                activeInput = ""
                                showAddItemDialog = false
                            }
                        },
                        enabled = addItemName.isNotBlank() &&
                            (addItemQuantity.toDoubleOrNull()?.let { it > 0.0 } == true) &&
                            addItemUnit.isNotBlank()
                    ) {
                        Text("Add Item")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddItemDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        transferState.message?.let { message ->
            LaunchedEffect(message) {
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                viewModel.clearPantryTransferState()
                showTransferSheet = false
            }
        }

        if (showTransferSheet) {
            AlertDialog(
                onDismissRequest = { showTransferSheet = false },
                title = { Text("Add checked items to Pantry") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 460.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        transferDrafts.forEachIndexed { index, draft ->
                            val mergeTarget = draft.mergeIngredientId?.let { id -> pantry.firstOrNull { it.id == id } }
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(14.dp)) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(draft.name, fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = draft.quantity.toString().trimEnd('0').trimEnd('.'),
                                            onValueChange = { value ->
                                                val qty = value.toDoubleOrNull() ?: draft.quantity
                                                transferDrafts = transferDrafts.toMutableList().also { it[index] = draft.copy(quantity = qty) }
                                            },
                                            label = { Text("Quantity") },
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f)
                                        )
                                        SimpleInlineDropdown(
                                            label = "Unit",
                                            value = draft.unit,
                                            options = unitOptions,
                                            onSelected = { value ->
                                                transferDrafts = transferDrafts.toMutableList().also {
                                                    it[index] = draft.copy(unit = value)
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SimpleInlineDropdown(
                                            label = "Category",
                                            value = draft.category,
                                            options = listOf("Produce", "Dairy", "Meat", "Grains", "Pantry"),
                                            onSelected = { value -> transferDrafts = transferDrafts.toMutableList().also { it[index] = draft.copy(category = value) } },
                                            modifier = Modifier.weight(1f)
                                        )
                                        SimpleInlineDropdown(
                                            label = "Location",
                                            value = draft.location,
                                            options = listOf("Fridge", "Pantry", "Freezer"),
                                            onSelected = { value -> transferDrafts = transferDrafts.toMutableList().also { it[index] = draft.copy(location = value) } },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    SimpleInlineDropdown(
                                        label = "Date Type",
                                        value = draft.dateType,
                                        options = IngredientDateType.options,
                                        onSelected = { value -> transferDrafts = transferDrafts.toMutableList().also { it[index] = draft.copy(dateType = value) } },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    if (draft.dateType != IngredientDateType.NO_PRINTED_DATE) {
                                        OutlinedTextField(
                                            value = draft.expirationDate,
                                            onValueChange = { value -> transferDrafts = transferDrafts.toMutableList().also { it[index] = draft.copy(expirationDate = value) } },
                                            label = { Text(IngredientDateType.fieldLabel(draft.dateType)) },
                                            placeholder = { Text("YYYY-MM-DD") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    SimpleInlineDropdown(
                                        label = "Item Status",
                                        value = draft.itemStatus,
                                        options = IngredientStatus.options.take(4),
                                        onSelected = { value -> transferDrafts = transferDrafts.toMutableList().also { it[index] = draft.copy(itemStatus = value) } },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    if (mergeTarget != null) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = draft.mergeIngredientId != null,
                                                onCheckedChange = { checked ->
                                                    transferDrafts = transferDrafts.toMutableList().also {
                                                        it[index] = draft.copy(mergeIngredientId = if (checked) mergeTarget.id else null)
                                                    }
                                                }
                                            )
                                            Text("Merge with ${mergeTarget.name} (${formatShoppingQuantity(mergeTarget.quantity)} ${mergeTarget.unit})")
                                        }
                                    } else {
                                        Text("Creates a new pantry item.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.transferCheckedItemsToPantry(transferDrafts) },
                        enabled = !transferState.isApplying
                    ) { Text("Add to Pantry") }
                },
                dismissButton = {
                    TextButton(onClick = { showTransferSheet = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun ShoppingItemRow(
    item: ShoppingItem,
    onCheckedChange: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.isDark
    val cardBgColor = if (isDark) Color(0xFF29332D) else Color.White

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isDark) Color(0xFF46534B) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onCheckedChange() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Circle checkbox matching custom aesthetic
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(
                            if (item.isChecked) MaterialTheme.colorScheme.primary
                            else Color.Transparent
                        )
                        .border(
                            1.5.dp,
                            if (item.isChecked) MaterialTheme.colorScheme.primary
                            else if (isDark) Color(0xFFB7C5BC) else MaterialTheme.colorScheme.outline,
                            CircleShape
                        )
                        .clickable { onCheckedChange() },
                    contentAlignment = Alignment.Center
                ) {
                    if (item.isChecked) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Checked", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (item.isChecked) {
                            if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else SoftGrayText.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = "${formatShoppingQuantity(item.quantity)} ${item.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else SoftGrayText
                    )
                    val sources = item.sourceRecipeNames()
                    if (sources.isNotEmpty()) {
                        Text(
                            text = "For: ${sources.joinToString()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else SoftGrayText.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = if (isDark) 0.35f else 0.65f))
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete item",
                    tint = if (isDark) Color(0xFFFFB4AB) else SoftCoralError,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatShoppingQuantity(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

@Composable
private fun SimpleInlineDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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
