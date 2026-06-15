package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ShoppingItem
import com.example.ui.components.ShelfLifeEmptyState
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShelfLifeViewModel

@Composable
fun ShoppingListScreen(viewModel: ShelfLifeViewModel) {
    val items by viewModel.shoppingItems.collectAsState()
    val isDark = MaterialTheme.colorScheme.isDark
    val textInputBgColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White

    var activeInput by remember { mutableStateOf("") }
    var showClearCheckedDialog by remember { mutableStateOf(false) }

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
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp) // Leave navigation bar padding
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

                if (items.any { it.isChecked }) {
                TextButton(
                    onClick = { showClearCheckedDialog = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (isDark) MaterialTheme.colorScheme.error else SoftCoralError
                    )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Clear Checked", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Fast insertion bar textfield
            // Fast insertion bar textfield
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
                    placeholder = { Text("Add items quickly... e.g. Bananas") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = textInputBgColor,
                        unfocusedContainerColor = textInputBgColor,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = if (isDark) Color.Transparent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
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
                            viewModel.addShoppingItem(activeInput, "My List")
                            activeInput = ""
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
                        tint = if (activeInput.isNotBlank()) Color.White else (if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else SoftGrayText)
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
    }
}

@Composable
fun ShoppingItemRow(
    item: ShoppingItem,
    onCheckedChange: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.isDark
    val cardBgColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = Modifier
            .fillMaxWidth()
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
                            else if (isDark) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clickable { onCheckedChange() },
                    contentAlignment = Alignment.Center
                ) {
                    if (item.isChecked) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Checked", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                Column {
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
                        text = "${item.quantity} ${item.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else SoftGrayText
                    )
                    if (item.sourceRecipeName != null) {
                        Text(
                            text = "For: ${item.sourceRecipeName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else SoftGrayText.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete item",
                    tint = if (isDark) MaterialTheme.colorScheme.error else SoftCoralError
                )
            }
        }
    }
}
