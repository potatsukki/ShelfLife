package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SavedRecipe
import com.example.data.recipeIngredients
import com.example.ui.components.ShelfLifeAsyncImage
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShelfLifeViewModel

@Composable
fun RecipesScreen(
    viewModel: ShelfLifeViewModel,
    onNavigateToRecipeDetail: (SavedRecipe) -> Unit,
    onNavigateToChat: () -> Unit
) {
    val recipes by viewModel.suggestedRecipes.collectAsState()
    val loading by viewModel.recipeLoading.collectAsState()
    val recipeState by viewModel.recipeGenerationState.collectAsState()

    var activeTab by remember { mutableStateOf("Suggested") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = 80.dp) // Leave navigation bar padding
    ) {
        val isDark = MaterialTheme.colorScheme.isDark
        val tabActiveBg = if (isDark) MaterialTheme.colorScheme.primaryContainer else Color.White
        val tabInactiveColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SoftGrayText

        // Toggle tab selectors: "Suggested Match", "My Favorites"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(if (activeTab == "Suggested") tabActiveBg else Color.Transparent)
                    .clickable { activeTab = "Suggested" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "✨ Suggested Match",
                    color = if (activeTab == "Suggested") MaterialTheme.colorScheme.onPrimaryContainer else tabInactiveColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(if (activeTab == "Favorites") tabActiveBg else Color.Transparent)
                    .clickable { activeTab = "Favorites" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "❤️ My Favorites",
                    color = if (activeTab == "Favorites") MaterialTheme.colorScheme.onPrimaryContainer else tabInactiveColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        if (activeTab == "Suggested") {
            // "✨ Match Fresh Pantry Elements" action button
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Cooking Engine",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Match ingredients expiring or low in stock to custom recipes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Button(
                        onClick = { viewModel.triggerRecipeSuggestions() },
                        enabled = !loading,
                        colors = ButtonDefaults.buttonColors(containerColor = SageGreen),
                        shape = CircleShape
                    ) {
                        if (loading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text("Match", color = Color.White)
                            }
                        }
                    }
                }
            }

            if (!recipeState.errorMessage.isNullOrBlank() && !loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(recipeState.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
                }
            } else if (recipes.isEmpty() && !loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No recipe suggestions yet. Add pantry items, then tap Match.", color = SoftGrayText, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
                }
            } else if (loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Reading pantry & generating recipe ideas with OpenRouter...", color = SoftGrayText, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(recipes) { recipe ->
                        RecipeItemCard(
                            recipe = recipe,
                            viewModel = viewModel,
                            onClick = { onNavigateToRecipeDetail(recipe) }
                        )
                    }
                }
            }
        } else {
            // Favorites Tab
            val savedList by viewModel.savedRecipes.collectAsState()

            if (savedList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text("💔", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No favorite recipes saved",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Open recipe suggestions and tap the heart icon to save favorites.",
                            color = SoftGrayText,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(savedList) { recipe ->
                        RecipeItemCard(
                            recipe = recipe,
                            viewModel = viewModel,
                            onClick = { onNavigateToRecipeDetail(recipe) }
                        )
                    }
                }
            }
        }

        // Floating Quick Assistant helper tip
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable { onNavigateToChat() }
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("👩🏼‍🍳", fontSize = 24.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Chat with Kitchen AI Assistant", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                    Text("Ask how to substitute ingredients or get custom tips.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

@Composable
fun RecipeItemCard(
    recipe: SavedRecipe,
    viewModel: ShelfLifeViewModel,
    onClick: () -> Unit
) {
    val isSaved by viewModel.isRecipeSaved(recipe.id).collectAsState(initial = false)
    val isDark = MaterialTheme.colorScheme.isDark
    val cardBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                ShelfLifeAsyncImage(
                    imageUrl = recipe.imageResUrl,
                    contentDescription = recipe.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Time badge overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.9f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(12.dp), tint = SoftGrayText)
                        Text(recipe.prepTime, style = MaterialTheme.typography.labelSmall, color = SoftGrayText, fontSize = 10.sp)
                    }
                }

                // Difficulty badge overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(CircleShape)
                        .background(
                            if (recipe.difficulty == "Easy") {
                                if (isDark) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primaryContainer
                            } else {
                                if (isDark) MaterialTheme.colorScheme.secondaryContainer else PeachContainer
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        recipe.difficulty,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (recipe.difficulty == "Easy") {
                            if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else OnMintContainer
                        } else {
                            if (isDark) MaterialTheme.colorScheme.onSecondaryContainer else OnPeachContainer
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = recipe.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = { viewModel.toggleSaveRecipe(recipe) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Save recipe",
                            tint = if (isSaved) Color.Red else SoftGrayText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Suggestion reason
                Text(
                    text = recipe.whySuggested,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SoftGrayText
                )

                // Preview structured ingredients
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val allIngredients = recipe.recipeIngredients()
                    val previewItems = allIngredients.take(3)
                    previewItems.forEach { item ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SoftGrayText,
                                fontSize = 10.sp
                            )
                        }
                    }
                    if (allIngredients.size > 3) {
                        Text("+${allIngredients.size - 3} more", style = MaterialTheme.typography.labelSmall, color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SoftGrayText)
                    }
                }
            }
        }
    }
}
