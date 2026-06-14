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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShelfLifeViewModel

@Composable
fun SettingsScreen(
    viewModel: ShelfLifeViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    val settings by viewModel.settingsState.collectAsState()
    val authState by viewModel.authUiState.collectAsState()

    var showSignOutDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 96.dp) // navbar offset
    ) {
        val isDark = MaterialTheme.colorScheme.isDark
        val cardBgColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White

        // User Profile Card
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = authState.initials,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                Column {
                    Text(
                        text = authState.displayLabel,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = authState.email ?: "Signed in user",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SoftGrayText
                    )
                }
            }
        }

        // Toggles sections: Alerts Notifications
        SettingsHeader(title = "Alerts & Notifications")
        SettingsToggleRow(
            label = "Expiration Date Alerts",
            description = "Notify me 3 days before any item expires",
            isChecked = settings.expirationAlerts,
            onCheckedChange = { viewModel.setExpirationAlerts(it) },
            icon = Icons.Default.NotificationsActive
        )
        SettingsToggleRow(
            label = "Low Stock Alerts",
            description = "Notify me when pantry items hit threshold",
            isChecked = settings.lowStockAlerts,
            onCheckedChange = { viewModel.setLowStockAlerts(it) },
            icon = Icons.AutoMirrored.Filled.TrendingDown
        )

        // Dietary preferences
        SettingsHeader(title = "Dietary Preferences")
        SettingsToggleRow(
            label = "Vegetarian Match Focus",
            description = "Prioritize vegetarian recipe suggestions",
            isChecked = settings.vegetarianMode,
            onCheckedChange = { viewModel.setVegetarianMode(it) },
            icon = Icons.Default.Spa
        )
        SettingsToggleRow(
            label = "Smart Recipe Suggestions",
            description = "Enable real-time OpenRouter recipe ideas",
            isChecked = settings.smartRecipeIdeas,
            onCheckedChange = { viewModel.setSmartRecipeIdeas(it) },
            icon = Icons.Default.AutoAwesome
        )

        // Configuration
        SettingsHeader(title = "System & Localization")
        SettingsToggleRow(
            label = "Dark Theme",
            description = "Switch between cozy Warm Hearth theme and Dark Walnut theme",
            isChecked = settings.isDarkMode,
            onCheckedChange = { viewModel.setDarkMode(it) },
            icon = Icons.Default.DarkMode
        )
        SettingsToggleRow(
            label = "Metric Measurement System",
            description = "Use Kilograms and Liters (else Pounds and Cups)",
            isChecked = settings.metricMeasurements,
            onCheckedChange = { viewModel.setMetricMeasurements(it) },
            icon = Icons.Default.Scale
        )
        SettingsToggleRow(
            label = "Household Sync",
            description = "Share pantry state with family members",
            isChecked = settings.householdSharing,
            onCheckedChange = { viewModel.setHouseholdSharing(it) },
            icon = Icons.Default.Group
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Reset Database button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = { showResetDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Text("Clear Database (Start Fresh)", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Log out button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = { showSignOutDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = SoftCoralErrorContainer),
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = SoftCoralError)
                    Text("Sign Out of ShelfLife", color = OnSoftCoralContainer, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Reset Database") },
                text = { Text("Are you sure you want to permanently delete all ingredients, shopping items, and saved recipes? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showResetDialog = false
                            viewModel.clearAllPantryAndData()
                            Toast.makeText(context, "Pantry database reset successfully!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Reset", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showSignOutDialog) {
            AlertDialog(
                onDismissRequest = { showSignOutDialog = false },
                title = { Text("Sign Out Confirmation") },
                text = { Text("Are you sure you want to log out? This will preserve your offline pantry state safely.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showSignOutDialog = false
                            viewModel.signOut()
                            Toast.makeText(context, "Signed out successfully.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SoftCoralError)
                    ) {
                        Text("Sign Out", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSignOutDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    label: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector
) {
    val isDark = MaterialTheme.colorScheme.isDark
    val cardBgColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(text = description, style = MaterialTheme.typography.bodySmall, color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SoftGrayText)
                }
            }

            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
