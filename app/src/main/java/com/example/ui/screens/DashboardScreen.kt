package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SavedRecipe
import com.example.ui.components.ShelfLifeAsyncImage
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShelfLifeViewModel

@Composable
fun DashboardScreen(viewModel: ShelfLifeViewModel) {
    val ingredients by viewModel.ingredients.collectAsState()
    val authState by viewModel.authUiState.collectAsState()
    val suggestedRecipes by viewModel.suggestedRecipes.collectAsState()

    // Dynamic Calculations
    val expiringCount = remember(ingredients) {
        ingredients.count {
            if (!it.hasTrackedDate) return@count false
            val days = viewModel.getDaysExpiry(it.expirationDate)
            days in 0..3
        }
    }
    val lowStockCount = remember(ingredients) {
        ingredients.count {
            it.lowStockReminderEnabled && it.quantity <= it.lowStockThreshold
        }
    }
    val totalCount = ingredients.size

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
    ) {
        // Hello Section
        val firstName = authState.displayLabel.trim().split(" ").firstOrNull()
            .takeUnless { it.isNullOrBlank() } ?: "there"
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Hello, $firstName",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Your smart kitchen companion.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Expiration Warning alert card
        val urgentExpiringItem = remember(ingredients) {
            ingredients
                .filter { it.hasTrackedDate && viewModel.getDaysExpiry(it.expirationDate) in 0..1 }
                .minByOrNull { viewModel.getDaysExpiry(it.expirationDate) }
        }

        if (urgentExpiringItem != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "${urgentExpiringItem.name} ${urgentExpiringItem.dateLabel.lowercase()} is " +
                            "${if (viewModel.getDaysExpiry(urgentExpiringItem.expirationDate) == 0) "today" else "tomorrow"}. " +
                            "Use it soon or update the item if it is already gone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val isDark = MaterialTheme.colorScheme.isDark

        // Bento Grid: Summary Cards
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Expiring Soon
                BentoCard(
                    title = "Expiring Soon",
                    subtitle = "Needs attention",
                    count = expiringCount.toString(),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    icon = Icons.Default.HourglassEmpty,
                    modifier = Modifier.weight(1f),
                    onClick = { 
                        viewModel.selectCategory("Expiring Soon")
                        viewModel.navigateTo("pantry") 
                    }
                )

                // Low Stock
                BentoCard(
                    title = "Low Stock",
                    subtitle = "Add to list",
                    count = lowStockCount.toString(),
                    containerColor = if (isDark) Color(0xFF53432B) else PeachContainer,
                    textColor = if (isDark) Color(0xFFFDDEAE) else OnPeachContainer,
                    icon = Icons.Default.ShoppingBag,
                    modifier = Modifier.weight(1f),
                    onClick = { 
                        viewModel.selectCategory("Low Stock")
                        viewModel.navigateTo("pantry") 
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pantry Items
                BentoCard(
                    title = "Pantry Items",
                    subtitle = "Fully stocked",
                    count = totalCount.toString(),
                    containerColor = if (isDark) Color(0xFF1E3F43) else BlueContainer,
                    textColor = if (isDark) Color(0xFF8CBCC2) else OnBlueContainer,
                    icon = Icons.Default.Inventory,
                    modifier = Modifier.weight(1f),
                    onClick = { 
                        viewModel.selectCategory("All")
                        viewModel.navigateTo("pantry") 
                    }
                )

                BentoCard(
                    title = "Suggested Meals",
                    subtitle = if (suggestedRecipes.isEmpty()) "Generate ideas" else "Ready to cook",
                    count = suggestedRecipes.size.toString(),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    icon = Icons.Default.RestaurantMenu,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo("recipes") }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Actions Scroll
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                QuickActionButton(
                    label = "Add Item",
                    icon = Icons.Default.Add,
                    onClick = { viewModel.navigateTo("add_ingredient") }
                )
                QuickActionButton(
                    label = "Scan",
                    icon = Icons.Default.QrCodeScanner,
                    onClick = { viewModel.navigateTo("scanner") }
                )
                QuickActionButton(
                    label = "View Pantry",
                    icon = Icons.Default.Kitchen,
                    onClick = { 
                        viewModel.selectCategory("All")
                        viewModel.navigateTo("pantry") 
                    }
                )
                QuickActionButton(
                    label = "AI Recipes",
                    icon = Icons.Default.AutoAwesome,
                    onClick = { viewModel.navigateTo("recipes") }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recommended Today",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                if (suggestedRecipes.isNotEmpty()) {
                    Text(
                        text = "View all",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.navigateTo("recipes") }
                            .padding(6.dp)
                    )
                }
            }

            if (suggestedRecipes.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clickable { viewModel.navigateTo("recipes") }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                "No recommendations yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Generate recipe matches from your pantry.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    suggestedRecipes.take(8).forEach { recipe ->
                        DashboardRecipeCard(
                            recipe = recipe,
                            viewModel = viewModel,
                            onClick = {
                                viewModel.selectRecipe(recipe)
                                viewModel.navigateTo("recipe_detail/${recipe.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardRecipeCard(
    recipe: SavedRecipe,
    viewModel: ShelfLifeViewModel,
    onClick: () -> Unit
) {
    val saved by viewModel.isRecipeSaved(recipe.id).collectAsState(initial = false)
    val isDark = MaterialTheme.colorScheme.isDark
    val cardColor = if (isDark) Color(0xFF242825) else Color.White

    Card(
        modifier = Modifier
            .width(152.dp)
            .height(216.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
            ) {
                ShelfLifeAsyncImage(
                    imageUrl = recipe.imageResUrl,
                    contentDescription = recipe.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                IconButton(
                    onClick = { viewModel.toggleSaveRecipe(recipe) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.36f))
                ) {
                    Icon(
                        imageVector = if (saved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (saved) "Remove favorite" else "Add favorite",
                        tint = if (saved) Color(0xFFFF5B62) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.88f))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = recipe.difficulty.ifBlank { "Suggested" },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 11.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = recipe.prepTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun BentoCard(
    title: String,
    subtitle: String,
    count: String,
    containerColor: Color,
    textColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
                }
                Text(
                    text = count,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .width(72.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
