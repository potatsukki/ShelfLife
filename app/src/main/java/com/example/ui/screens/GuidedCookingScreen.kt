package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.IngredientNormalizer
import com.example.data.IngredientUsage
import com.example.data.SavedRecipe
import com.example.data.recipeSteps
import com.example.data.scaledIngredients
import com.example.ui.viewmodel.ShelfLifeViewModel

private data class UsageDraft(
    val name: String,
    val quantity: String,
    val unit: String,
    val skipped: Boolean = false
)

@Composable
fun GuidedCookingScreen(
    viewModel: ShelfLifeViewModel,
    recipe: SavedRecipe,
    servings: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val steps = remember(recipe) { recipe.recipeSteps() }
    val scaledIngredients = remember(recipe, servings) {
        recipe.scaledIngredients(servings).filter { it.required && it.quantity != null && it.quantity > 0.0 }
    }
    val pantry by viewModel.ingredients.collectAsState()
    val completionState by viewModel.cookingCompletionState.collectAsState()
    var currentStep by remember(recipe.id) { mutableIntStateOf(0) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var showUsageReview by remember { mutableStateOf(false) }
    var usageDrafts by remember(recipe.id, servings) {
        mutableStateOf(
            scaledIngredients.map {
                UsageDraft(
                    name = it.name,
                    quantity = it.quantity?.let(::formatUsageNumber).orEmpty(),
                    unit = it.unit.ifBlank { "pcs" }
                )
            }
        )
    }

    LaunchedEffect(completionState.message) {
        completionState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearCookingCompletion()
            onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Guided Cooking", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("${recipe.name} • $servings servings", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        LinearProgressIndicator(
            progress = { if (steps.isEmpty()) 0f else (currentStep + 1).toFloat() / steps.size.toFloat() },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            steps.forEachIndexed { index, step ->
                val active = index == currentStep
                val complete = index < currentStep
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            active -> MaterialTheme.colorScheme.primaryContainer
                            complete -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.surface
                        }
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(if (complete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            if (complete) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            else Text("${index + 1}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(step, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { if (currentStep > 0) currentStep-- },
                enabled = currentStep > 0,
                modifier = Modifier.weight(1f),
                shape = CircleShape
            ) { Text("Back") }
            Button(
                onClick = {
                    if (currentStep < steps.lastIndex) currentStep++ else showFinishDialog = true
                },
                enabled = !completionState.isApplying,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f),
                shape = CircleShape
            ) {
                Text(if (currentStep < steps.lastIndex) "Next Step" else "Finish Cooking", color = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Did you use the planned ingredient amounts?") },
            text = { Text("ShelfLife can deduct the recipe amounts from matching Pantry items.") },
            confirmButton = {
                Button(
                    onClick = {
                        showFinishDialog = false
                        viewModel.applyCookingUsage(recipe, servings)
                    }
                ) { Text("Yes, deduct") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showFinishDialog = false
                        showUsageReview = true
                    }
                ) { Text("No, review amounts") }
            }
        )
    }

    if (showUsageReview) {
        AlertDialog(
            onDismissRequest = { showUsageReview = false },
            title = { Text("Review used ingredients") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    usageDrafts.forEachIndexed { index, draft ->
                        val balance = pantry.filter { IngredientNormalizer.matches(it.name, draft.name) }
                            .sumOf { it.quantity }
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = !draft.skipped,
                                        onCheckedChange = { checked ->
                                            usageDrafts = usageDrafts.toMutableList().also {
                                                it[index] = draft.copy(skipped = !checked)
                                            }
                                        }
                                    )
                                    Column {
                                        Text(draft.name, fontWeight = FontWeight.Bold)
                                        Text("Pantry balance: ${formatUsageNumber(balance)} matching units/lots", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = draft.quantity,
                                        onValueChange = { value ->
                                            usageDrafts = usageDrafts.toMutableList().also {
                                                it[index] = draft.copy(quantity = value)
                                            }
                                        },
                                        label = { Text("Used") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        enabled = !draft.skipped,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = draft.unit,
                                        onValueChange = { value ->
                                            usageDrafts = usageDrafts.toMutableList().also {
                                                it[index] = draft.copy(unit = value)
                                            }
                                        },
                                        label = { Text("Unit") },
                                        enabled = !draft.skipped,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val usages = usageDrafts
                            .filterNot { it.skipped }
                            .mapNotNull { draft ->
                                val qty = draft.quantity.toDoubleOrNull() ?: return@mapNotNull null
                                IngredientUsage(draft.name, qty, draft.unit.ifBlank { "pcs" })
                            }
                        showUsageReview = false
                        viewModel.applyCookingUsage(recipe, servings, usages)
                    }
                ) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { showUsageReview = false }) { Text("Cancel") } }
        )
    }
}

private fun formatUsageNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value).trimEnd('0').trimEnd('.')
