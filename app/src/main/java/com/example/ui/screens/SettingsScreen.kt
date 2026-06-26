package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SoftCoralError
import com.example.ui.theme.SoftGrayText
import com.example.ui.theme.WarmBackground
import com.example.ui.viewmodel.ShelfLifeViewModel

@Composable
fun SettingsScreen(
    viewModel: ShelfLifeViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settingsState.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }
    var pendingAlertToggle by remember { mutableStateOf<AlertToggle?>(null) }
    var notificationPermissionDenied by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.background != WarmBackground
    val notificationsAllowed = notificationsAllowed(context)
    val alertsEnabled = settings.expirationAlerts || settings.lowStockAlerts
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val target = pendingAlertToggle
        pendingAlertToggle = null
        if (granted && notificationsAllowed(context)) {
            notificationPermissionDenied = false
            when (target) {
                AlertToggle.Expiration -> viewModel.setExpirationAlerts(true)
                AlertToggle.LowStock -> viewModel.setLowStockAlerts(true)
                null -> Unit
            }
            Toast.makeText(context, "Daily pantry reminders enabled.", Toast.LENGTH_SHORT).show()
        } else {
            notificationPermissionDenied = true
            when (target) {
                AlertToggle.Expiration -> viewModel.setExpirationAlerts(false)
                AlertToggle.LowStock -> viewModel.setLowStockAlerts(false)
                null -> Unit
            }
            Toast.makeText(
                context,
                "Notifications are blocked. Enable them in Android Settings to receive pantry reminders.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun updateAlertToggle(target: AlertToggle, enabled: Boolean) {
        if (!enabled) {
            when (target) {
                AlertToggle.Expiration -> viewModel.setExpirationAlerts(false)
                AlertToggle.LowStock -> viewModel.setLowStockAlerts(false)
            }
            Toast.makeText(context, "Pantry reminders updated.", Toast.LENGTH_SHORT).show()
            return
        }

        if (notificationsAllowed(context)) {
            notificationPermissionDenied = false
            when (target) {
                AlertToggle.Expiration -> viewModel.setExpirationAlerts(true)
                AlertToggle.LowStock -> viewModel.setLowStockAlerts(true)
            }
            Toast.makeText(context, "Daily pantry reminders enabled.", Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !postNotificationsGranted(context)) {
            pendingAlertToggle = target
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            notificationPermissionDenied = true
            when (target) {
                AlertToggle.Expiration -> viewModel.setExpirationAlerts(false)
                AlertToggle.LowStock -> viewModel.setLowStockAlerts(false)
            }
            Toast.makeText(
                context,
                "Notifications are blocked. Enable them in Android Settings to receive pantry reminders.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Go back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        SettingsSectionTitle("Alerts & Notifications")
        SettingsToggleRow(
            label = "Expiration Date Alerts",
            description = "Get daily reminders for expired and expiring pantry items",
            checked = settings.expirationAlerts,
            onCheckedChange = { updateAlertToggle(AlertToggle.Expiration, it) },
            icon = Icons.Default.NotificationsActive,
            isDark = isDark,
            enabled = true
        )
        SettingsToggleRow(
            label = "Low Stock Alerts",
            description = "Get daily reminders when tracked items are running low",
            checked = settings.lowStockAlerts,
            onCheckedChange = { updateAlertToggle(AlertToggle.LowStock, it) },
            icon = Icons.AutoMirrored.Filled.TrendingDown,
            isDark = isDark,
            enabled = true
        )
        NotificationStatusCard(
            status = notificationStatus(
                context = context,
                alertsEnabled = alertsEnabled,
                permissionDenied = notificationPermissionDenied
            ),
            isDark = isDark,
            onOpenSettings = { openNotificationSettings(context) }
        )

        SettingsSectionTitle("Dietary Preferences")
        SettingsToggleRow(
            label = "Vegetarian Match Focus",
            description = "Vegetarian recipe filtering is coming soon",
            checked = settings.vegetarianMode,
            onCheckedChange = viewModel::setVegetarianMode,
            icon = Icons.Default.Spa,
            isDark = isDark,
            enabled = false,
            badge = "Soon"
        )
        SettingsToggleRow(
            label = "Smart Recipe Suggestions",
            description = "Enable real-time DeepSeek recipe ideas",
            checked = settings.smartRecipeIdeas,
            onCheckedChange = viewModel::setSmartRecipeIdeas,
            icon = Icons.Default.AutoAwesome,
            isDark = isDark
        )

        SettingsSectionTitle("System & Localization")
        SettingsToggleRow(
            label = "Dark Theme",
            description = "Switch between cozy Warm Hearth theme and Dark Walnut theme",
            checked = settings.isDarkMode,
            onCheckedChange = viewModel::setDarkMode,
            icon = Icons.Default.DarkMode,
            isDark = isDark
        )
        SettingsToggleRow(
            label = "Metric Measurement System",
            description = "Measurement conversion is coming soon",
            checked = settings.metricMeasurements,
            onCheckedChange = viewModel::setMetricMeasurements,
            icon = Icons.Default.Scale,
            isDark = isDark,
            enabled = false,
            badge = "Soon"
        )
        SettingsToggleRow(
            label = "Household Sync",
            description = "Family sharing is coming soon",
            checked = settings.householdSharing,
            onCheckedChange = viewModel::setHouseholdSharing,
            icon = Icons.Default.Group,
            isDark = isDark,
            enabled = false,
            badge = "Soon"
        )

        Text(
            text = "Settings marked Soon are saved for later but are not active features yet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )

        Spacer(Modifier.height(14.dp))

        Button(
            onClick = { showResetDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDark) Color(0xFF2B201F) else SoftCoralError.copy(alpha = 0.14f),
                contentColor = if (isDark) Color(0xFFFFB4AB) else SoftCoralError
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.size(19.dp)
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = "Clear Database (Start Fresh)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Database") },
            text = {
                Text(
                    "Permanently delete all ingredients, shopping items, and saved recipes? " +
                        "This action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        viewModel.clearAllPantryAndData()
                        Toast.makeText(
                            context,
                            "Pantry database reset successfully.",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
}

private enum class AlertToggle {
    Expiration,
    LowStock
}

private data class NotificationStatus(
    val text: String,
    val showOpenSettings: Boolean
)

@Composable
private fun NotificationStatusCard(
    status: NotificationStatus,
    isDark: Boolean,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF253229) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = status.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (status.showOpenSettings) {
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onOpenSettings) {
                    Text("Open Settings")
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    isDark: Boolean,
    enabled: Boolean = true,
    badge: String? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 9.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) {
                Color(0xFF2D382F)
            } else {
                Color.White
            }
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDark) {
                            MaterialTheme.colorScheme.background.copy(alpha = 0.65f)
                        } else {
                            WarmBackground
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (badge != null) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        SoftGrayText
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = if (enabled) onCheckedChange else null,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedBorderColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = Color(0xFF7A807B),
                    uncheckedTrackColor = Color(0xFFE6E3E7),
                    uncheckedBorderColor = Color(0xFF858585)
                )
            )
        }
    }
}

private fun notificationStatus(
    context: Context,
    alertsEnabled: Boolean,
    permissionDenied: Boolean
): NotificationStatus {
    val permissionGranted = postNotificationsGranted(context)
    val systemAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
    return when {
        permissionGranted && systemAllowed && alertsEnabled ->
            NotificationStatus("Notifications allowed · Daily pantry check enabled", showOpenSettings = false)
        permissionGranted && systemAllowed ->
            NotificationStatus("Notifications allowed · Alerts are off", showOpenSettings = false)
        permissionDenied || permissionGranted ->
            NotificationStatus("Notifications blocked · Enable in Android Settings", showOpenSettings = true)
        else ->
            NotificationStatus("Permission required · Turn on an alert to enable reminders", showOpenSettings = false)
    }
}

private fun notificationsAllowed(context: Context): Boolean =
    postNotificationsGranted(context) &&
        NotificationManagerCompat.from(context).areNotificationsEnabled()

private fun postNotificationsGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

private fun openNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }
    runCatching { context.startActivity(intent) }
        .onFailure {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            )
        }
}
