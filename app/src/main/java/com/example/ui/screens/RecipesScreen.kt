package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.text.style.TextOverflow
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
    onNavigateToChat: () -> Unit,
    onCreateRecipe: () -> Unit
) {
    val recipes by viewModel.suggestedRecipes.collectAsState()
    val loading by viewModel.recipeLoading.collectAsState()
    val recipeState by viewModel.recipeGenerationState.collectAsState()
    val regeneratingRecipeId by viewModel.regeneratingRecipeId.collectAsState()
    val isDark = MaterialTheme.colorScheme.isDark

    var activeTab by remember { mutableStateOf("Suggested") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val tabActiveBg = if (isDark) MaterialTheme.colorScheme.primaryContainer else Color.White
        val tabInactiveColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SoftGrayText

        // Recipe library tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp)
        ) {
            listOf(
                "Suggested" to "Suggested",
                "MyRecipes" to "My Recipes",
                "Favorites" to "Favorites"
            ).forEach { (key, label) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(if (activeTab == key) tabActiveBg else Color.Transparent)
                        .clickable { activeTab = key }
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (activeTab == key) MaterialTheme.colorScheme.onPrimaryContainer else tabInactiveColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }

        if (activeTab == "Suggested") {
            // "✨ Match Fresh Pantry Elements" action button
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF34483B) else MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    val compactLayout = maxWidth < 320.dp

                    if (compactLayout) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "AI Cooking Engine",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isDark) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Match ingredients expiring or low in stock to custom recipes.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color.White.copy(alpha = 0.78f)
                                    else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = { viewModel.triggerRecipeSuggestions() },
                                enabled = !loading,
                                colors = recipeMatchButtonColors(isDark),
                                shape = CircleShape,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                MatchButtonContent(loading = loading)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "AI Cooking Engine",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isDark) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Match ingredients expiring or low in stock to custom recipes.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color.White.copy(alpha = 0.78f)
                                    else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = { viewModel.triggerRecipeSuggestions() },
                                enabled = !loading,
                                colors = recipeMatchButtonColors(isDark),
                                shape = CircleShape
                            ) {
                                MatchButtonContent(loading = loading)
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
                        Text("Reading pantry and generating recipe ideas...", color = SoftGrayText, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
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
                            onClick = { onNavigateToRecipeDetail(recipe) },
                            onRegenerate = { viewModel.regenerateSuggestedRecipe(recipe) },
                            isRegenerating = regeneratingRecipeId == recipe.id,
                            onDelete = { viewModel.deleteMatchedRecipe(recipe) }
                        )
                    }
                }
            }
        } else {
            val savedList by if (activeTab == "MyRecipes") {
                viewModel.userRecipes.collectAsState()
            } else {
                viewModel.favoriteRecipes.collectAsState()
            }

            if (savedList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = if (activeTab == "MyRecipes") Icons.Default.MenuBook else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            if (activeTab == "MyRecipes") "Create your first recipe" else "No favorite recipes saved",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (activeTab == "MyRecipes") {
                                "Save family recipes with exact ingredients and step-by-step instructions."
                            } else {
                                "Open any recipe and tap the heart icon to save it here."
                            },
                            color = SoftGrayText,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                        if (activeTab == "MyRecipes") {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onCreateRecipe, shape = CircleShape) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Create Recipe")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (activeTab == "MyRecipes") {
                        item {
                            Button(
                                onClick = onCreateRecipe,
                                modifier = Modifier.fillMaxWidth(),
                                shape = CircleShape
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Create New Recipe")
                            }
                        }
                    }
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
    }
}

@Composable
private fun MatchButtonContent(loading: Boolean) {
    val contentColor = LocalContentColor.current
    if (loading) {
        CircularProgressIndicator(
            color = contentColor,
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp
        )
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Text("Match", color = contentColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun recipeMatchButtonColors(isDark: Boolean) = ButtonDefaults.buttonColors(
    containerColor = if (isDark) Color(0xFF8FCFA8) else SageGreen,
    contentColor = if (isDark) Color(0xFF123522) else Color.White,
    disabledContainerColor = if (isDark) Color(0xFF5F7968) else SageGreen.copy(alpha = 0.55f),
    disabledContentColor = if (isDark) Color.White.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.7f)
)

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun RecipeItemCard(
    recipe: SavedRecipe,
    viewModel: ShelfLifeViewModel,
    onClick: () -> Unit,
    onRegenerate: (() -> Unit)? = null,
    isRegenerating: Boolean = false,
    onDelete: (() -> Unit)? = null
) {
    val isSaved by viewModel.isRecipeSaved(recipe.id).collectAsState(initial = false)
    val isDark = MaterialTheme.colorScheme.isDark
    val cardBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White
    var showDeleteDialog by remember { mutableStateOf(false) }

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
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    onRegenerate?.let { regenerate ->
                        IconButton(
                            onClick = regenerate,
                            enabled = !isRegenerating,
                            modifier = Modifier.size(36.dp)
                        ) {
                            if (isRegenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Regenerate recipe",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    onDelete?.let {
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete recipe",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

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
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val allIngredients = recipe.recipeIngredients()
                    val previewItems = allIngredients.take(3)
                    previewItems.forEach { item ->
                        Box(
                            modifier = Modifier
                                .widthIn(max = 120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SoftGrayText,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (allIngredients.size > 3) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "+${allIngredients.size - 3} more",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SoftGrayText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete recipe?") },
            text = { Text("This removes the recipe from suggestions and favorites.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
