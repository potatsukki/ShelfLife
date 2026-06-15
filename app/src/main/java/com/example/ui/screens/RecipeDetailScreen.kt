package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SavedRecipe
import com.example.data.recipeSteps
import com.example.ui.components.ShelfLifeAsyncImage
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShelfLifeViewModel
import com.example.ui.viewmodel.splitRecipeIngredients

@Composable
fun RecipeDetailScreen(
    viewModel: ShelfLifeViewModel,
    recipe: SavedRecipe,
    onBack: () -> Unit,
    onStartCooking: () -> Unit = {}
) {
    val isDark = MaterialTheme.colorScheme.isDark
    val context = LocalContext.current
    val isSaved by viewModel.isRecipeSaved(recipe.id).collectAsState(initial = false)
    val pantryIngredients by viewModel.ingredients.collectAsState()

    val ingredientSplit = remember(recipe, pantryIngredients) {
        splitRecipeIngredients(recipe, pantryIngredients)
    }
    val inPantry = ingredientSplit.available
    val missing = ingredientSplit.missing

    // Parse steps
    val stepsList = remember(recipe) {
        recipe.recipeSteps()
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
                    text = "Recipe Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.toggleSaveRecipe(recipe) }) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Save recipe",
                        tint = if (isSaved) Color.Red else MaterialTheme.colorScheme.primary
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
                .verticalScroll(rememberScrollState())
                .padding(bottom = 40.dp)
        ) {
            // Hero Image
            Box(
                modifier = Modifier
                    .fillOuterWidth()
                    .height(200.dp)
            ) {
                ShelfLifeAsyncImage(
                    imageUrl = recipe.imageResUrl,
                    contentDescription = recipe.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Back overlay stats
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text(recipe.prepTime, color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(16.dp))
                        Text(recipe.difficulty, color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // Title
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = recipe.whySuggested,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SoftGrayText,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                if (recipe.imageProvider.isNotBlank() && recipe.photographerName.isNotBlank()) {
                    Text(
                        text = "Photo by ${recipe.photographerName} on ${recipe.imageProvider}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Ingredients bento split section
                Text(
                    text = "Ingredients Match",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val cardBgColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left bento column: items in pantry
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardBgColor),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SageGreen, modifier = Modifier.size(18.dp))
                                Text("Available (${inPantry.size})", style = MaterialTheme.typography.labelSmall, color = SageGreen, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            if (inPantry.isEmpty()) {
                                Text("None match.", style = MaterialTheme.typography.bodySmall, color = SoftGrayText)
                            } else {
                                inPantry.forEach { item ->
                                    Text(
                                        text = "• ${item.displayText}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Right bento column: items missing
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardBgColor),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = SoftCoralError, modifier = Modifier.size(18.dp))
                                Text("Missing (${missing.size})", style = MaterialTheme.typography.labelSmall, color = SoftCoralError, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            if (missing.isEmpty()) {
                                Text("Ready to cook!", style = MaterialTheme.typography.bodySmall, color = SageGreen, fontWeight = FontWeight.Bold)
                            } else {
                                missing.forEach { item ->
                                    Text(
                                        text = "• ${item.displayText}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = SoftCoralError
                                    )
                                }
                            }
                        }
                    }
                }

                // Add missing items to list Action Button
                if (missing.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            missing.forEach { item ->
                                viewModel.addShoppingItem(
                                    name = item.name,
                                    category = "Missing for Recipes",
                                    sourceRecipeName = recipe.name,
                                    quantity = item.quantity ?: 1.0,
                                    unit = item.unit.ifBlank { "pcs" }
                                )
                            }
                            Toast.makeText(context, "Added ${missing.size} items to shopping list!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AddShoppingCart, contentDescription = null, tint = OnPeachContainer)
                            Text("Add Missing Items to Shopping List", color = OnPeachContainer, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Chronological steps sequence
                Text(
                    text = "Cooking Steps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    stepsList.forEachIndexed { index, step ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Checked badge / indicator index
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${index + 1}", color = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else OnMintContainer, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Start Cook Button
                Button(
                    onClick = onStartCooking,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Guided Cooking", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

// Support extension modifier for width
private fun Modifier.fillOuterWidth() = this.fillMaxWidth()
