package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.data.RecipeIngredient
import com.example.data.RecipeOrigin
import com.example.data.SavedRecipe
import com.example.data.recipeIngredients
import com.example.data.recipeSteps
import com.example.ui.viewmodel.ShelfLifeViewModel
import org.json.JSONArray
import org.json.JSONObject

private data class EditableIngredient(
    val name: String = "",
    val quantity: String = "",
    val unit: String = "pcs",
    val required: Boolean = true
)

@Composable
fun RecipeEditorScreen(
    viewModel: ShelfLifeViewModel,
    existing: SavedRecipe?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var description by remember(existing?.id) { mutableStateOf(existing?.description.orEmpty()) }
    var servings by remember(existing?.id) { mutableStateOf((existing?.baseServings ?: 2).toString()) }
    var prepTime by remember(existing?.id) { mutableStateOf(existing?.prepTime.orEmpty()) }
    var cookTime by remember(existing?.id) { mutableStateOf(existing?.cookTime.orEmpty()) }
    var difficulty by remember(existing?.id) { mutableStateOf(existing?.difficulty?.ifBlank { "Easy" } ?: "Easy") }
    var imageUri by remember(existing?.id) { mutableStateOf(existing?.localImageUri.orEmpty()) }
    var ingredients by remember(existing?.id) {
        mutableStateOf(
            existing?.recipeIngredients()?.map {
                EditableIngredient(
                    name = it.name,
                    quantity = it.quantity?.let(::formatEditorNumber).orEmpty(),
                    unit = it.unit.ifBlank { "pcs" },
                    required = it.required
                )
            }?.ifEmpty { listOf(EditableIngredient()) } ?: listOf(EditableIngredient())
        )
    }
    var steps by remember(existing?.id) {
        mutableStateOf(existing?.recipeSteps()?.ifEmpty { listOf("") } ?: listOf(""))
    }
    var error by remember { mutableStateOf<String?>(null) }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            imageUri = uri.toString()
        }
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
                }
                Text(
                    if (existing == null) "Create Recipe" else "Edit Recipe",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        bottomBar = {
            Button(
                onClick = {
                    val parsedServings = servings.toIntOrNull()
                    val cleanIngredients = ingredients.mapNotNull {
                        val cleanName = it.name.trim()
                        if (cleanName.isBlank()) null else RecipeIngredient(
                            name = cleanName,
                            quantity = it.quantity.toDoubleOrNull(),
                            unit = it.unit.trim(),
                            required = it.required
                        )
                    }
                    val cleanSteps = steps.map(String::trim).filter(String::isNotBlank)
                    error = when {
                        name.isBlank() -> "Recipe name is required."
                        parsedServings == null || parsedServings <= 0 -> "Servings must be greater than zero."
                        cleanIngredients.isEmpty() -> "Add at least one ingredient."
                        cleanSteps.isEmpty() -> "Add at least one cooking step."
                        ingredients.any { it.name.isNotBlank() && it.quantity.isNotBlank() && it.quantity.toDoubleOrNull() == null } ->
                            "Ingredient quantities must be numeric."
                        else -> null
                    }
                    if (error == null) {
                        val now = System.currentTimeMillis()
                        viewModel.saveUserRecipe(
                            SavedRecipe(
                                id = existing?.id ?: "user-$now",
                                userId = existing?.userId.orEmpty(),
                                name = name.trim(),
                                prepTime = prepTime.trim(),
                                difficulty = difficulty,
                                imageResUrl = imageUri,
                                whySuggested = description.trim(),
                                ingredientsCsv = cleanIngredients.joinToString(", ") { it.displayText },
                                stepsCsv = cleanSteps.joinToString("|"),
                                ingredientsJson = JSONArray(cleanIngredients.map {
                                    JSONObject()
                                        .put("name", it.name)
                                        .put("quantity", it.quantity ?: JSONObject.NULL)
                                        .put("unit", it.unit)
                                        .put("required", it.required)
                                }).toString(),
                                stepsJson = JSONArray(cleanSteps.map { JSONObject().put("text", it) }).toString(),
                                origin = RecipeOrigin.USER,
                                isFavorite = existing?.isFavorite ?: false,
                                description = description.trim(),
                                baseServings = parsedServings!!,
                                cookTime = cookTime.trim(),
                                localImageUri = imageUri,
                                createdAt = existing?.createdAt ?: now,
                                updatedAt = now
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save Recipe")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedButton(
                    onClick = { photoLauncher.launch(arrayOf("image/*")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (imageUri.isBlank()) "Choose Recipe Photo (Optional)" else "Change Recipe Photo")
                }
            }
            item { EditorField(name, { name = it }, "Recipe Name *", "e.g. Family Pizza") }
            item { EditorField(description, { description = it }, "Description", "What makes this recipe useful?") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    EditorField(servings, { servings = it }, "Servings *", "2", Modifier.weight(1f), KeyboardType.Number)
                    EditorField(prepTime, { prepTime = it }, "Prep Time", "15 min", Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    EditorField(cookTime, { cookTime = it }, "Cook Time", "25 min", Modifier.weight(1f))
                    SimpleDropdown(
                        value = difficulty,
                        options = listOf("Easy", "Medium", "Hard"),
                        onSelected = { difficulty = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                SectionHeader("Ingredients", "Add Ingredient") {
                    ingredients = ingredients + EditableIngredient()
                }
            }
            itemsIndexed(ingredients) { index, ingredient ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EditorField(
                            ingredient.name,
                            { value -> ingredients = ingredients.toMutableList().also { it[index] = ingredient.copy(name = value) } },
                            "Ingredient",
                            "e.g. Eggs"
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            EditorField(
                                ingredient.quantity,
                                { value -> ingredients = ingredients.toMutableList().also { it[index] = ingredient.copy(quantity = value) } },
                                "Quantity",
                                "3",
                                Modifier.weight(1f),
                                KeyboardType.Decimal
                            )
                            EditorField(
                                ingredient.unit,
                                { value -> ingredients = ingredients.toMutableList().also { it[index] = ingredient.copy(unit = value) } },
                                "Unit",
                                "pcs",
                                Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    if (ingredients.size > 1) ingredients = ingredients.filterIndexed { i, _ -> i != index }
                                }
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove ingredient", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = ingredient.required,
                                onCheckedChange = { checked ->
                                    ingredients = ingredients.toMutableList().also { it[index] = ingredient.copy(required = checked) }
                                }
                            )
                            Text("Required for automatic shopping and Pantry deduction")
                        }
                    }
                }
            }
            item {
                SectionHeader("Cooking Steps", "Add Step") { steps = steps + "" }
            }
            itemsIndexed(steps) { index, step ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("${index + 1}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        EditorField(
                            step,
                            { value -> steps = steps.toMutableList().also { it[index] = value } },
                            "Step ${index + 1}",
                            "Describe this cooking step",
                            Modifier.weight(1f)
                        )
                        Column {
                            IconButton(
                                onClick = {
                                    if (index > 0) {
                                        steps = steps.toMutableList().also {
                                            val moved = it.removeAt(index)
                                            it.add(index - 1, moved)
                                        }
                                    }
                                },
                                enabled = index > 0
                            ) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move step up") }
                            IconButton(
                                onClick = {
                                    if (index < steps.lastIndex) {
                                        steps = steps.toMutableList().also {
                                            val moved = it.removeAt(index)
                                            it.add(index + 1, moved)
                                        }
                                    }
                                },
                                enabled = index < steps.lastIndex
                            ) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move step down") }
                            IconButton(
                                onClick = { if (steps.size > 1) steps = steps.filterIndexed { i, _ -> i != index } }
                            ) { Icon(Icons.Default.DeleteOutline, contentDescription = "Remove step", tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
            error?.let { message ->
                item { Text(message, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun EditorField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
private fun SimpleDropdown(
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Difficulty"
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            shape = RoundedCornerShape(14.dp)
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

@Composable
private fun SectionHeader(title: String, action: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        TextButton(onClick = onAction) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text(action)
        }
    }
}

private fun formatEditorNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
