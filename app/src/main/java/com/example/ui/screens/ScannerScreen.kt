package com.example.ui.screens

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.Ingredient
import com.example.ui.theme.SageGreen
import com.example.ui.theme.SoftCoralError
import com.example.ui.theme.SoftCoralErrorContainer
import com.example.ui.theme.SoftGrayText
import com.example.ui.theme.isDark
import com.example.ui.viewmodel.ShelfLifeViewModel

@Composable
fun ScannerScreen(
    viewModel: ShelfLifeViewModel
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.isDark
    val scannerUiState by viewModel.scannerUiState.collectAsState()
    var manualBarcode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScannerLaunchPanel(
            isLoading = scannerUiState.isLoading,
            onScan = { viewModel.scanRealBarcode(context) }
        )

        scannerUiState.pendingIngredient?.let { ingredient ->
            ProductResultCard(
                ingredient = ingredient,
                barcode = scannerUiState.barcode,
                onAdd = { editedIngredient -> viewModel.confirmScannedIngredient(editedIngredient) },
                onScanAgain = { viewModel.resetScanner() }
            )
        }

        if (scannerUiState.message != null && scannerUiState.pendingIngredient == null && !scannerUiState.isLoading) {
            ScanMessageCard(
                message = scannerUiState.message.orEmpty(),
                isError = scannerUiState.isError,
                onClear = { viewModel.resetScanner() }
            )
        }

        Text(
            text = "OR",
            style = MaterialTheme.typography.labelMedium,
            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SoftGrayText,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        ManualBarcodeCard(
            barcode = manualBarcode,
            isLoading = scannerUiState.isLoading,
            onBarcodeChange = { manualBarcode = it },
            onLookup = { viewModel.lookupBarcode(manualBarcode) }
        )
    }
}

@Composable
private fun ScannerLaunchPanel(
    isLoading: Boolean,
    onScan: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_line")
    val lineOffset by infiniteTransition.animateFloat(
        initialValue = -86f,
        targetValue = 86f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanner_line_offset"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F241F)),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clickable(enabled = !isLoading, onClick = onScan)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .border(2.dp, SageGreen, RoundedCornerShape(22.dp))
                    .background(Color.Black.copy(alpha = 0.22f))
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(3.dp)
                    .offset(y = lineOffset.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, SageGreen, Color.Transparent)
                        )
                    )
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = SageGreen, modifier = Modifier.size(44.dp))
                    Text(
                        text = "Looking up product...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(78.dp)
                    )
                    Button(
                        onClick = onScan,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF9DDEB4),
                            contentColor = Color(0xFF123522)
                        ),
                        shape = CircleShape
                    ) {
                        Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Scan Barcode", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductResultCard(
    ingredient: Ingredient,
    barcode: String?,
    onAdd: (Ingredient) -> Unit,
    onScanAgain: () -> Unit
) {
    var isEditing by remember(ingredient) { mutableStateOf(false) }
    var name by remember(ingredient) { mutableStateOf(ingredient.name) }
    var category by remember(ingredient) { mutableStateOf(ingredient.category) }
    var quantity by remember(ingredient) { mutableStateOf(ingredient.quantity.toString()) }
    var unit by remember(ingredient) { mutableStateOf(ingredient.unit) }
    var location by remember(ingredient) { mutableStateOf(ingredient.location) }
    var expirationDate by remember(ingredient) { mutableStateOf(ingredient.expirationDate) }
    var packageSize by remember(ingredient) { mutableStateOf(ingredient.packageSize) }
    var brand by remember(ingredient) { mutableStateOf(ingredient.brand) }
    var editedBarcode by remember(ingredient, barcode) { mutableStateOf(ingredient.barcode.ifBlank { barcode.orEmpty() }) }
    var notes by remember(ingredient) { mutableStateOf(ingredient.notes) }
    val editedIngredient = ingredient.copy(
        name = name.trim().ifBlank { ingredient.name },
        category = category.trim().ifBlank { ingredient.category },
        quantity = quantity.toDoubleOrNull() ?: ingredient.quantity,
        unit = unit.trim().ifBlank { ingredient.unit },
        location = location.trim().ifBlank { ingredient.location },
        expirationDate = expirationDate.trim(),
        packageSize = packageSize.trim(),
        brand = brand.trim(),
        barcode = editedBarcode.trim(),
        notes = notes.trim()
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SageGreen,
                    modifier = Modifier.size(34.dp)
                )
                Column {
                    Text(
                        text = "Product found",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = barcode ?: "Scanned barcode",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { isEditing = !isEditing }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit scanned product",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (isEditing) {
                ScanEditField("Product name", name, { name = it })
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ScanEditField("Category", category, { category = it }, Modifier.weight(1f))
                    ScanEditField("Location", location, { location = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ScanEditField("Quantity", quantity, { quantity = it }, Modifier.weight(1f), KeyboardType.Decimal)
                    ScanEditField("Unit", unit, { unit = it }, Modifier.weight(1f))
                }
                ScanEditField("Expiration / Use-By Date", expirationDate, { expirationDate = it }, placeholder = "YYYY-MM-DD")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ScanEditField("Package size", packageSize, { packageSize = it }, Modifier.weight(1f))
                    ScanEditField("Brand", brand, { brand = it }, Modifier.weight(1f))
                }
                ScanEditField("Barcode", editedBarcode, { editedBarcode = it.filter(Char::isDigit).take(32) }, keyboardType = KeyboardType.Number)
                ScanEditField(
                    label = "Nutrition / notes",
                    value = notes,
                    onValueChange = { notes = it },
                    singleLine = false,
                    minLines = 2
                )
            } else {
                Text(
                    text = ingredient.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProductChip(Icons.Default.LocalOffer, ingredient.category)
                    ProductChip(Icons.Default.Inventory2, "${ingredient.quantity} ${ingredient.unit}")
                }
                ProductChip(
                    Icons.Default.Inventory2,
                    if (ingredient.hasTrackedDate) {
                        "${ingredient.location} · ${ingredient.dateLabel}: ${ingredient.expirationDate}"
                    } else {
                        "${ingredient.location} · No printed date"
                    }
                )
                if (ingredient.notes.isNotBlank()) {
                    Text(
                        text = ingredient.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onAdd(editedIngredient) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (MaterialTheme.colorScheme.isDark) Color(0xFF9DDEB4) else SageGreen,
                        contentColor = if (MaterialTheme.colorScheme.isDark) Color(0xFF123522) else Color.White
                    ),
                    shape = CircleShape,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add to pantry", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onScanAgain,
                    shape = CircleShape,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Scan again")
                }
            }
        }
    }
}

@Composable
private fun ScanEditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.background,
            unfocusedContainerColor = MaterialTheme.colorScheme.background,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun ProductChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ScanMessageCard(
    message: String,
    isError: Boolean,
    onClear: () -> Unit
) {
    val container = if (isError) SoftCoralErrorContainer else MaterialTheme.colorScheme.primaryContainer
    val content = if (isError) SoftCoralError else MaterialTheme.colorScheme.onPrimaryContainer

    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (isError) Icons.Default.Search else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = content,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(onClick = onClear, shape = CircleShape) {
                Text("Clear")
            }
        }
    }
}

@Composable
private fun ManualBarcodeCard(
    barcode: String,
    isLoading: Boolean,
    onBarcodeChange: (String) -> Unit,
    onLookup: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = barcode,
                onValueChange = { value ->
                    onBarcodeChange(value.filter { it.isDigit() }.take(32))
                },
                placeholder = { Text("Enter barcode") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                ),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onLookup,
                enabled = barcode.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 16.dp),
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Search Product", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
