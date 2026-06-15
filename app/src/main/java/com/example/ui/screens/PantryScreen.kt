package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Ingredient
import com.example.ui.components.ShelfLifeEmptyState
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShelfLifeViewModel

@Composable
fun PantryScreen(
    viewModel: ShelfLifeViewModel,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToAdd: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.isDark
    val cardBgColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White
    val textInputBgColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White
    val chipBgColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White

    val searchVal by viewModel.searchQuery.collectAsState()
    val activeCat by viewModel.selectedCategory.collectAsState()
    val filteredIngredients by viewModel.filteredIngredients.collectAsState()

    val categories = listOf(
        "All", "Produce", "Dairy", "Meat", "Grains", "Pantry", 
        "Fresh", "Expiring Soon", "Expired", "Low Stock"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp) // Leave roomy space for floating bottom bar
        ) {
            // Header Search View
            OutlinedTextField(
                value = searchVal,
                onValueChange = { viewModel.setSearchQuery(it) },
                leadingIcon = { Icon(imageVector = Icons.Outlined.Search, contentDescription = "Search icon") },
                trailingIcon = {
                    if (searchVal.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                placeholder = { Text("Search rice, eggs, milk...") },
                singleLine = true,
                maxLines = 1,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = textInputBgColor,
                    unfocusedContainerColor = textInputBgColor,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = if (isDark) Color.Transparent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                    unfocusedLeadingIconColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else SoftGrayText.copy(alpha = 0.6f),
                    focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
                    unfocusedTrailingIconColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else SoftGrayText.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("pantry_search_input")
            )

            // Horizontal filters chips (Clean custom capsules)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isActive = cat == activeCat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primaryContainer
                                else if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White
                            )
                            .clickable { viewModel.selectCategory(cat) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SoftGrayText
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredIngredients.isEmpty()) {
                // Empty view state
                ShelfLifeEmptyState(
                    emoji = "🥑",
                    title = "No ingredients found",
                    description = "Try searching something else, changing filter, or add new ingredients to your pantry.",
                    actionButtonText = "Add Ingredient",
                    onActionClick = onNavigateToAdd
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredIngredients, key = { it.id }) { item ->
                        PantryIngredientItem(
                            item = item,
                            getDaysExpiry = { viewModel.getDaysExpiry(it) },
                            onClick = { onNavigateToDetail(item.id) }
                        )
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onNavigateToAdd,
            containerColor = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 96.dp, end = 20.dp)
                .testTag("add_item_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Ingredient", tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
fun PantryIngredientItem(
    item: Ingredient,
    getDaysExpiry: (String) -> Int,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.isDark
    val cardBgColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White
    val daysLeft = getDaysExpiry(item.expirationDate)

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
            "Expires in $daysLeft days"
        )
        else -> Triple(
            if (isDark) MintContainer else SageGreen,
            if (isDark) SageGreen.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            "Expires in $daysLeft days"
        )
    }

    val progressPercent = remember(item.purchaseDate, item.expirationDate, daysLeft) {
        expiryProgress(item.purchaseDate, item.expirationDate)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("ingredient_card_${item.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(statusBg),
                    contentAlignment = Alignment.Center
                ) {
                    val emoji = when (item.category) {
                        "Produce", "Vegetables" -> "🥬"
                        "Dairy" -> "🥛"
                        "Meat", "Poultry" -> "🍗"
                        "Grains", "Bakery" -> "🍞"
                        "Pantry", "Spices" -> "🧂"
                        else -> "🍎"
                    }
                    Text(emoji, fontSize = 22.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${item.quantity} ${item.unit}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )

                        // Location indicator badge
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isDark) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = item.location,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SoftGrayText,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Expiration Time linear indicator
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progressPercent },
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
            )
        }
    }
}

private fun expiryProgress(purchaseDate: String, expirationDate: String): Float {
    return try {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val purchase = format.parse(purchaseDate)?.time ?: return 0f
        val expiry = format.parse(expirationDate)?.time ?: return 0f
        val today = format.parse(format.format(java.util.Date()))?.time ?: return 0f
        val total = (expiry - purchase).coerceAtLeast(1L)
        ((today - purchase).toFloat() / total.toFloat()).coerceIn(0f, 1f)
    } catch (_: Exception) {
        0f
    }
}
