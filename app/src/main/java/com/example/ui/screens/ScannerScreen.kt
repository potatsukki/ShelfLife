package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShelfLifeViewModel

@Composable
fun ScannerScreen(
    viewModel: ShelfLifeViewModel,
    onNavigateToAddManual: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.isDark
    val context = LocalContext.current
    val scannerState by viewModel.scannerState.collectAsState()

    // Moving laser line animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 240f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = 80.dp), // navbar offset
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top instruction header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Barcode Scanner",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Point camera at the item's barcode to scan and log it.",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SoftGrayText,
                textAlign = TextAlign.Center
            )
        }

        // Viewfinder center Box
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.Black.copy(alpha = 0.85f))
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(32.dp))
                .clickable {
                    viewModel.scanRealBarcode(context)
                },
            contentAlignment = Alignment.Center
        ) {
            // Simulated camera scanner blurry feedback backdrop
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(96.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (scannerState == "scanning") "Scanning barcode..." else "Camera View Finder",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Interactive moving laser line
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(3.dp)
                    .offset(y = (-110).dp + laserOffset.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                if (scannerState == "scanning") Color.Yellow else MaterialTheme.colorScheme.primaryContainer,
                                Color.Transparent
                            )
                        )
                    )
            )

            // Success overlay HUD
            androidx.compose.animation.AnimatedVisibility(
                visible = scannerState != null && scannerState != "scanning",
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(SageGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Success", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = scannerState ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else OnMintContainer,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.resetScanner() },
                            colors = ButtonDefaults.buttonColors(containerColor = SageGreen),
                            shape = CircleShape
                        ) {
                            Text("Scan Another", color = Color.White)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        var customBarcode by remember { mutableStateOf("") }

        if (scannerState == null || scannerState == "scanning") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Outlined Barcode Entry
                OutlinedTextField(
                    value = customBarcode,
                    onValueChange = { customBarcode = it },
                    placeholder = { Text("Or paste real barcode... (e.g. 5449000000996)", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                        unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(0.9f),
                    trailingIcon = {
                        if (customBarcode.isNotBlank()) {
                            IconButton(onClick = { viewModel.simulateScan(customBarcode) }) {
                                Icon(imageVector = Icons.Default.Search, contentDescription = "Lookup", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Primary real-camera scanner button
                Button(
                    onClick = { viewModel.scanRealBarcode(context) },
                    enabled = scannerState != "scanning",
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (scannerState == "scanning") {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Querying Database...", color = Color.White, fontSize = 14.sp)
                        } else {
                            Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White)
                            Text("Open Scanner Camera", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Secondary quick-simulator button
                OutlinedButton(
                    onClick = { viewModel.simulateScan(customBarcode.ifBlank { null }) },
                    enabled = scannerState != "scanning",
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(42.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FlashOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(if (customBarcode.isBlank()) "Quick Simulation Demo" else "Lookup Typed Barcode", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Presets Shelf
                Text(
                    text = "Live Test Presets (Open Food Facts & OpenRouter)",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SoftGrayText,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Row of beautiful capsules
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    val presets = listOf(
                        "Nutella" to "3017611154000",
                        "Coca Cola" to "5449000000996",
                        "Spaghetti" to "0737628005076"
                    )
                    presets.forEach { (name, code) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickable {
                                    customBarcode = code
                                    viewModel.simulateScan(code)
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Manual Fallback Entry Section
        val isDark = MaterialTheme.colorScheme.isDark
        val entryCardBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White

        Card(
            colors = CardDefaults.cardColors(containerColor = entryCardBg),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Don't have a barcode?",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Add details manually instead.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SoftGrayText
                    )
                }

                Button(
                    onClick = onNavigateToAddManual,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = CircleShape
                ) {
                    Text("Add Manually", color = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else OnMintContainer, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
