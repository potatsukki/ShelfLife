package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import com.example.data.IngredientNormalizer
import com.example.data.RecipeOrigin
import com.example.data.SavedRecipe
import com.example.data.recipeSteps
import com.example.data.scaledIngredients
import com.example.ui.components.ShelfLifeAsyncImage
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShelfLifeViewModel

@Composable
fun RecipeDetailScreen(
    viewModel: ShelfLifeViewModel,
    recipe: SavedRecipe,
    onBack: () -> Unit,
    onStartCooking: (Int) -> Unit = {},
    onChat: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDuplicate: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val isDark = MaterialTheme.colorScheme.isDark
    val context = LocalContext.current
    val isSaved by viewModel.isRecipeSaved(recipe.id).collectAsState(initial = false)
    val pantryIngredients by viewModel.ingredients.collectAsState()
    var servings by remember(recipe.id) { mutableIntStateOf(recipe.baseServings.coerceAtLeast(1)) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val scaledIngredients = remember(recipe, servings) { recipe.scaledIngredients(servings) }
    val available = remember(scaledIngredients, pantryIngredients) {
        scaledIngredients.filter { ingredient ->
            pantryIngredients.any { pantry -> IngredientNormalizer.matches(ingredient.name, pantry.name) }
        }
    }
    val missing = remember(scaledIngredients, available) { scaledIngredients - available.toSet() }
    val stepsList = remember(recipe) { recipe.recipeSteps() }

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
                    Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Go back")
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "Recipe Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onChat) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Chat about this recipe",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { viewModel.toggleSaveRecipe(recipe) }) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Save recipe",
                        tint = if (isSaved) Color.Red else MaterialTheme.colorScheme.primary
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Recipe actions")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (recipe.origin == RecipeOrigin.USER) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = { showMenu = false; onEdit() }
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicate") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                onClick = { showMenu = false; onDuplicate() }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                            onClick = { showMenu = false; showDeleteDialog = true }
                        )
                    }
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                ShelfLifeAsyncImage(
                    imageUrl = recipe.imageResUrl.ifBlank { recipe.localImageUri },
                    contentDescription = recipe.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (recipe.prepTime.isNotBlank()) DetailStat(Icons.Default.AccessTime, recipe.prepTime)
                    if (recipe.cookTime.isNotBlank()) DetailStat(Icons.Default.LocalFireDepartment, recipe.cookTime)
                    DetailStat(Icons.Default.Star, recipe.difficulty.ifBlank { "Easy" })
                }
            }

            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    recipe.name,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                val description = recipe.description.ifBlank { recipe.whySuggested }
                if (description.isNotBlank()) {
                    Text(
                        description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SoftGrayText
                    )
                }
                if (recipe.imageProvider.isNotBlank() && recipe.photographerName.isNotBlank()) {
                    Text(
                        "Photo by ${recipe.photographerName} on ${recipe.imageProvider}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ServingsCard(servings = servings, onDecrease = { if (servings > 1) servings-- }, onIncrease = { servings++ })

                Text(
                    "Ingredients Match",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IngredientsMatchSection(available, missing, isDark)

                if (missing.isNotEmpty()) {
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
                            Toast.makeText(context, "Added ${missing.size} missing items to Shopping List.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFFFFD69A) else MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = OnPeachContainer
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Add Missing Items to Shopping List", fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    "Cooking Steps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                stepsList.forEachIndexed { index, step ->
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${index + 1}", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                        }
                        Text(step, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Button(
                    onClick = { onStartCooking(servings) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Guided Cooking", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete recipe?") },
            text = {
                Text(
                    if (recipe.origin == RecipeOrigin.USER) {
                        "${recipe.name} will be permanently removed."
                    } else {
                        "This removes the recipe from suggestions and favorites."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        if (recipe.origin == RecipeOrigin.USER) {
                            onDelete()
                        } else {
                            viewModel.deleteMatchedRecipe(recipe)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun DetailStat(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ServingsCard(servings: Int, onDecrease: () -> Unit, onIncrease: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Servings", fontWeight = FontWeight.Bold)
                Text("Shopping and cooking quantities scale from this.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrease) { Icon(Icons.Default.Remove, contentDescription = "Reduce servings") }
                Text("$servings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onIncrease) { Icon(Icons.Default.Add, contentDescription = "Increase servings") }
            }
        }
    }
}

@Composable
private fun IngredientsMatchSection(
    available: List<com.example.data.RecipeIngredient>,
    missing: List<com.example.data.RecipeIngredient>,
    isDark: Boolean
) {
    val cardBgColor = if (isDark) Color(0xFF354139) else Color.White
    val availableColor = if (isDark) Color(0xFF9DDEB4) else SageGreen
    val missingColor = if (isDark) Color(0xFFFFB4AB) else SoftCoralError
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IngredientColumn("Available", available, availableColor, cardBgColor, Modifier.weight(1f), "None match.")
        IngredientColumn("Missing", missing, missingColor, cardBgColor, Modifier.weight(1f), "Ready to cook!")
    }
}

@Composable
private fun IngredientColumn(
    title: String,
    items: List<com.example.data.RecipeIngredient>,
    color: Color,
    background: Color,
    modifier: Modifier,
    emptyText: String
) {
    Card(colors = CardDefaults.cardColors(containerColor = background), shape = RoundedCornerShape(20.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(if (title == "Available") Icons.Default.CheckCircle else Icons.Default.Warning, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Text("$title (${items.size})", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (items.isEmpty()) {
                Text(emptyText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                items.forEach {
                    Text("• ${it.displayText}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp), color = if (title == "Missing") color else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
