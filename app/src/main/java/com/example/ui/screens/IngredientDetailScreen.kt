package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Ingredient
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShelfLifeViewModel

@Composable
fun IngredientDetailScreen(
    viewModel: ShelfLifeViewModel,
    id: Int,
    onBack: () -> Unit,
    onEdit: (Int) -> Unit
) {
    val ingredientFlow = remember(id) { viewModel.getIngredientDetails(id) }
    val item by ingredientFlow.collectAsState(initial = null)

    if (item == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val ingredient = item!!
    val daysLeft = viewModel.getDaysExpiry(ingredient.expirationDate)
    val isDark = MaterialTheme.colorScheme.isDark
    val cardBgColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White
    var confirmAction by remember { mutableStateOf<String?>(null) }

    // Layout configuration depending on alert state
    val (statusColor, statusBg, statusText) = when {
        daysLeft < 0 -> Triple(
            if (isDark) Color(0xFFFFB4AB) else SoftCoralError,
            if (isDark) SoftCoralErrorContainer.copy(alpha = 0.2f) else SoftCoralErrorContainer,
            "Expired"
        )
        daysLeft == 0 -> Triple(
            if (isDark) Color(0xFFFFB4AB) else SoftCoralError,
            if (isDark) SoftCoralErrorContainer.copy(alpha = 0.2f) else SoftCoralErrorContainer,
            "Expires Today"
        )
        daysLeft in 1..3 -> Triple(
            if (isDark) PeachContainer else OnPeachContainer,
            if (isDark) PeachSecondary.copy(alpha = 0.4f) else PeachContainer,
            "Expiring Soon ($daysLeft days left)"
        )
        else -> Triple(
            if (isDark) MintContainer else SageGreen,
            if (isDark) SageGreen.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            "Fresh ($daysLeft days left)"
        )
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
                    Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Go back")
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = ingredient.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { onEdit(ingredient.id) }) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit ingredient")
                }
            }
        },
        bottomBar = {},
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large Emoji Box
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(statusBg),
                contentAlignment = Alignment.Center
            ) {
                val emoji = when (ingredient.category) {
                    "Produce" -> "🥬"
                    "Dairy" -> "🥛"
                    "Meat" -> "🍗"
                    "Grains" -> "🍞"
                    "Pantry" -> "🧂"
                    else -> "🍎"
                }
                Text(emoji, fontSize = 48.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = ingredient.name,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Large quantity
            Text(
                text = "${ingredient.quantity} ${ingredient.unit}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Expiration status banner
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(statusBg)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = statusText,
                    color = statusColor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Metadata grid
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Item Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    DetailFieldRow(label = "Category", value = ingredient.category, icon = Icons.Default.Category)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    DetailFieldRow(label = "Primary Location", value = ingredient.location, icon = Icons.Default.LocationOn)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    DetailFieldRow(label = "Purchase Date", value = ingredient.purchaseDate, icon = Icons.Default.CalendarToday)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    DetailFieldRow(label = "Expiration Date", value = ingredient.expirationDate, icon = Icons.Default.CalendarMonth)
                    
                    if (ingredient.notes.isNotBlank()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Notes, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Notes", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(ingredient.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // AI Smart Tip Box
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💡", fontSize = 24.sp)
                    Column {
                        Text("ShelfLife Tip", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                        Text(
                            text = when (ingredient.category) {
                                "Produce" -> "Keep vegetables dry by enclosing a paper towel in the storage pouch. This keeps produce fresh 3-4 days longer!"
                                "Dairy" -> "Store milk in the back of the fridge, not in the door, to maintain a completely solid, cool temperature."
                                "Meat" -> "If cooking later than 2 days, portion and wrap raw meat tightly before committing to deep freeze cycles."
                                else -> "Check dates and consume before expiration. Added recipes help use this item quickly!"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Dynamic Core Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Action: Add Replenish Item to Shopping List
                Button(
                    onClick = {
                        viewModel.addShoppingItem(
                            name = ingredient.name,
                            category = "Suggested from Low Stock"
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AddShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("Add to Shopping List", color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.labelLarge)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Action: Mark as Used (Delete)
                Button(
                    onClick = { confirmAction = "used" },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                            Text("Mark Used", color = Color.White, style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    // Action: Delete completely
                    OutlinedButton(
                        onClick = { confirmAction = "delete" },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftCoralError),
                        border = BorderStroke(1.dp, SoftCoralError),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = SoftCoralError)
                            Text("Delete", color = SoftCoralError, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }

    confirmAction?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text(if (action == "used") "Mark item as used?" else "Delete ingredient?") },
            text = {
                Text(
                    if (action == "used") {
                        "This removes ${ingredient.name} from your pantry because it has been used."
                    } else {
                        "This permanently removes ${ingredient.name} from your pantry."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmAction = null
                        viewModel.deleteIngredient(ingredient)
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (action == "used") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(if (action == "used") "Mark Used" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailFieldRow(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
