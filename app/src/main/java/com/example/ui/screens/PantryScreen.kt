package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Ingredient
import com.example.data.IngredientDateType
import com.example.data.IngredientStatus
import com.example.ui.components.ShelfLifeEmptyState
import com.example.ui.theme.*
import com.example.ui.viewmodel.PantrySortOption
import com.example.ui.viewmodel.ShelfLifeViewModel
import kotlin.math.roundToInt

@Composable
fun PantryScreen(
    viewModel: ShelfLifeViewModel,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToReceiptScanner: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.isDark
    val textInputBgColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    val chipBgColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface

    val searchVal by viewModel.searchQuery.collectAsState()
    val activeCat by viewModel.selectedCategory.collectAsState()
    val filteredIngredients by viewModel.filteredIngredients.collectAsState()
    val sortOption by viewModel.pantrySortOption.collectAsState()
    var selectedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var pendingDeleteIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val selectionMode = selectedIds.isNotEmpty()

    LaunchedEffect(filteredIngredients) {
        selectedIds = selectedIds.intersect(filteredIngredients.mapTo(mutableSetOf()) { it.id })
    }

    val categories = listOf(
        "All", "Produce", "Dairy", "Meat", "Grains", "Pantry", 
        "Fresh", "Expiring Soon", "Expired", "Low Stock"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            AnimatedVisibility(visible = selectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedIds = emptySet() }) {
                        Icon(Icons.Default.Close, contentDescription = "Exit selection")
                    }
                    Text(
                        text = "${selectedIds.size} selected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            selectedIds = if (selectedIds.size == filteredIngredients.size) {
                                emptySet()
                            } else {
                                filteredIngredients.mapTo(mutableSetOf()) { it.id }
                            }
                        }
                    ) {
                        Text(if (selectedIds.size == filteredIngredients.size) "Clear" else "Select all")
                    }
                    IconButton(onClick = { pendingDeleteIds = selectedIds }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete selected",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Header Search View
            OutlinedTextField(
                value = searchVal,
                onValueChange = { viewModel.setSearchQuery(it) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search icon",
                        modifier = Modifier.size(26.dp)
                    )
                },
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
                    .height(64.dp)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("pantry_search_input")
            )

            // Horizontal filters chips (Clean custom capsules)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                categories.forEach { cat ->
                    val isActive = cat == activeCat
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary else chipBgColor
                            )
                            .border(
                                width = 1.dp,
                                color = if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                shape = CircleShape
                            )
                            .clickable { viewModel.selectCategory(cat) }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SoftGrayText
                            }
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${filteredIngredients.size} ${if (filteredIngredients.size == 1) "item" else "items"}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "In your pantry",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(38.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                        )
                        Box {
                            OutlinedButton(
                                onClick = { sortMenuExpanded = true },
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Sort: ${sortOption.label}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 118.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Choose pantry sort",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false }
                            ) {
                                PantrySortOption.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label) },
                                        leadingIcon = {
                                            if (option == sortOption) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.setPantrySortOption(option)
                                            sortMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onNavigateToReceiptScanner,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.55f else 0.45f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Scan Receipt",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredIngredients, key = { it.id }) { item ->
                        val daysLeft = remember(item.id, item.expirationDate, item.hasTrackedDate) {
                            if (item.hasTrackedDate) viewModel.getDaysExpiry(item.expirationDate) else null
                        }
                        val progressPercent = remember(item.id, item.purchaseDate, item.expirationDate, item.hasTrackedDate) {
                            if (item.hasTrackedDate) expiryProgress(item.purchaseDate, item.expirationDate) else 0f
                        }
                        SwipeRevealPantryItem(
                            item = item,
                            daysLeft = daysLeft,
                            progressPercent = progressPercent,
                            selected = item.id in selectedIds,
                            selectionMode = selectionMode,
                            onClick = {
                                if (selectionMode) {
                                    selectedIds = selectedIds.toggle(item.id)
                                } else {
                                    onNavigateToDetail(item.id)
                                }
                            },
                            onLongClick = {
                                selectedIds = selectedIds + item.id
                            },
                            onDeleteRequest = {
                                pendingDeleteIds = setOf(item.id)
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button
        AnimatedVisibility(
            visible = !selectionMode,
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
        FloatingActionButton(
            onClick = onNavigateToAdd,
            containerColor = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
            modifier = Modifier
                .padding(bottom = 96.dp, end = 20.dp)
                .testTag("add_item_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Ingredient", tint = Color.White, modifier = Modifier.size(28.dp))
        }
        }

        if (pendingDeleteIds.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { pendingDeleteIds = emptySet() },
                title = {
                    Text(
                        if (pendingDeleteIds.size == 1) "Delete ingredient?"
                        else "Delete ${pendingDeleteIds.size} ingredients?"
                    )
                },
                text = {
                    Text("This permanently removes the selected pantry ${if (pendingDeleteIds.size == 1) "item" else "items"}.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteIngredients(pendingDeleteIds)
                            selectedIds = selectedIds - pendingDeleteIds
                            pendingDeleteIds = emptySet()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteIds = emptySet() }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun SwipeRevealPantryItem(
    item: Ingredient,
    daysLeft: Int?,
    progressPercent: Float,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.isDark
    val revealWidth = 88.dp
    val revealWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { revealWidth.toPx() }
    var offsetPx by remember(item.id) { mutableFloatStateOf(0f) }

    LaunchedEffect(selectionMode) {
        if (selectionMode) offsetPx = 0f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDark) Color(0xFFB3261E) else MaterialTheme.colorScheme.errorContainer)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(revealWidth)
                .fillMaxHeight()
                .clickable {
                    offsetPx = 0f
                    onDeleteRequest()
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = if (isDark) Color.White else MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    "Delete",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isDark) Color.White else MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetPx.roundToInt(), 0) }
                .draggable(
                    enabled = !selectionMode,
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        offsetPx = (offsetPx + delta).coerceIn(-revealWidthPx, 0f)
                    },
                    onDragStopped = {
                        offsetPx = if (offsetPx <= -revealWidthPx * 0.38f) -revealWidthPx else 0f
                    }
                )
        ) {
            PantryIngredientItem(
                item = item,
                daysLeft = daysLeft,
                progressPercent = progressPercent,
                selected = selected,
                selectionMode = selectionMode,
                onClick = onClick,
                onLongClick = onLongClick
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PantryIngredientItem(
    item: Ingredient,
    daysLeft: Int?,
    progressPercent: Float,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.isDark
    val cardBgColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    val displayName = remember(item.id, item.name, item.brand) { displayIngredientName(item) }
    val detailText = remember(item.id, item.category, item.location, item.storageCondition, item.itemStatus) {
        buildList {
            add(item.category)
            add(item.location)
            if (item.storageCondition.isNotBlank()) add(item.storageCondition)
            if (item.itemStatus != IngredientStatus.SEALED) add(item.itemStatus)
        }.joinToString(" · ")
    }

    val (statusColor, statusBg, statusText) = when {
        !item.hasTrackedDate -> Triple(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            "No printed date"
        )
        daysLeft!! < 0 -> Triple(
            if (isDark) Color(0xFFFFB4AB) else SoftCoralError,
            if (isDark) SoftCoralErrorContainer.copy(alpha = 0.2f) else SoftCoralErrorContainer,
            "${IngredientDateType.shortLabel(item.dateType)} passed"
        )
        daysLeft == 0 -> Triple(
            if (isDark) Color(0xFFFFB4AB) else SoftCoralError,
            if (isDark) SoftCoralErrorContainer.copy(alpha = 0.2f) else SoftCoralErrorContainer,
            "${IngredientDateType.shortLabel(item.dateType)} today"
        )
        daysLeft in 1..3 -> Triple(
            if (isDark) PeachContainer else OnPeachContainer,
            if (isDark) PeachSecondary.copy(alpha = 0.4f) else PeachContainer,
            "${IngredientDateType.shortLabel(item.dateType)} in $daysLeft days"
        )
        else -> Triple(
            if (isDark) MintContainer else SageGreen,
            if (isDark) SageGreen.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            "${IngredientDateType.shortLabel(item.dateType)} in $daysLeft days"
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(24.dp)
            )
            .testTag("ingredient_card_${item.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (selectionMode) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                            .border(
                                1.5.dp,
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.55f else 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = pantryCategoryIcon(item.category),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${formatPantryQuantity(item.quantity)} ${item.unit}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }

                    Text(
                        text = detailText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isDark) MaterialTheme.colorScheme.background else statusBg.copy(alpha = 0.55f))
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = item.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (item.hasTrackedDate) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progressPercent },
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.25f else 0.35f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(CircleShape)
                )
            }
        }
    }
}

private fun Set<Int>.toggle(id: Int): Set<Int> =
    if (id in this) this - id else this + id

private fun formatPantryQuantity(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

private fun displayIngredientName(item: Ingredient): String {
    val brand = item.brand.trim()
    val name = item.name.trim()
    if (brand.isBlank()) return name

    val normalizedBrand = normalizeDisplayName(brand)
    val normalizedName = normalizeDisplayName(name)
    return if (normalizedName.startsWith(normalizedBrand)) {
        name
    } else {
        "$brand $name"
    }
}

private fun normalizeDisplayName(value: String): String =
    value
        .lowercase()
        .replace(Regex("""[^\p{L}\p{N}\s]"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()

private fun pantryCategoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector =
    when (category) {
        "Produce", "Vegetables" -> Icons.Default.Eco
        "Dairy" -> Icons.Default.LocalDrink
        "Meat", "Poultry" -> Icons.Default.Restaurant
        "Grains", "Bakery" -> Icons.Default.BakeryDining
        "Pantry", "Spices" -> Icons.Default.Inventory2
        else -> Icons.Default.Kitchen
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
