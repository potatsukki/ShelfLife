package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShelfLifeViewModel

@Composable
fun DashboardScreen(viewModel: ShelfLifeViewModel) {
    val ingredients by viewModel.ingredients.collectAsState()

    // Dynamic Calculations
    val expiringCount = remember(ingredients) {
        ingredients.count { 
            val days = viewModel.getDaysExpiry(it.expirationDate)
            days in 0..3
        }
    }
    val lowStockCount = remember(ingredients) {
        ingredients.count { it.quantity <= it.lowStockThreshold }
    }
    val totalCount = ingredients.size

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(bottom = 96.dp) // padding to avoid bottom navbar overlap
    ) {
        // Hello Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Hello, Alex",
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
        val expiringSpinach = remember(ingredients) {
            ingredients.firstOrNull { it.name.contains("spinach", ignoreCase = true) }
        }

        if (expiringSpinach != null && viewModel.getDaysExpiry(expiringSpinach.expirationDate) <= 1) {
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
                        text = "Your spinach is expiring today. Consider using it in a salad or smoothie.",
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

                // Suggested Meals
                BentoCard(
                    title = "Suggested Meals",
                    subtitle = "Ready to cook",
                    count = "4",
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

        // Recommended Today card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "Recommended Today",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            val isDark = MaterialTheme.colorScheme.isDark
            val cardBgColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White

            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        // Directly open recommended Stir Fry
                        val stirFry = viewModel.suggestedRecipes.value.firstOrNull { it.id == "chicken_stir_fry" }
                        viewModel.selectRecipe(stirFry)
                        viewModel.navigateTo("recipe_detail/chicken_stir_fry")
                    }
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data("https://lh3.googleusercontent.com/aida-public/AB6AXuAQpywRVsQ0wBZrqaOJgmjmYGCaiLIp9iAlIPA1WO1fr-5tlUFhrT2lZdhmQ-MVNXlSYymK7nWeUXfGa6cltB4DdJQMDm-Eof9nze9uY4dAk7UJ9ZPQ_17bYOOkYO8-tEE6U0tf49Uzq1OVqy0IIg4SLhU6NYqYBEJ62q9juL17O-JIsrGy2I15NS84wvLr207-nSyT-vWtNFmSQ4srX-lfRnsnzbk9BXb8PJf4pv9MGgd53xn8ldal1D8m3kIeGt_53G7g_AbJo4ts")
                                .crossfade(true)
                                .build(),
                            contentDescription = "Chicken Stir Fry",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // 20 min overlay badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.9f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                Text("20 min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Chicken Stir Fry",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.BookmarkBorder, contentDescription = "Bookmark", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Reason Badge
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SecondaryFixed.copy(alpha = 0.3f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Eco, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Reason: Uses chicken and bell pepper expiring soon.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
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
