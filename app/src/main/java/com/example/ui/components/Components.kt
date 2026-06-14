package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.*

// --- Custom Reusable Top Bar ---
@Composable
fun ShelfLifeTopBar(
    title: String,
    onAvatarClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar Image
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onAvatarClick() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data("https://lh3.googleusercontent.com/aida-public/AB6AXuDF_w9cpOE2usbzEdTM9c3iOKc43nzRphhx545sr5vT-xMI7Ehv-_TxUFUxDgbr2eNTu5AZEzenScBCtKifIqwcof8gh1nDbROj5ekx2E2VdZcXJ3SRZcdR1qvwBLG5Y3oEGfzUmfQX_fW7HrCMEG74R4dqoZSmSDpaOoMr8Fe9PMJBqKZcBhWwoo-CwZHxlqZPPB4nLX34ayx06wQ8JlBWs-jD-BjL6PzTHsq_ri0LREQUjk5DqZgHXQ_SR-tk6XdlXgVu9SZP5RGW")
                    .crossfade(true)
                    .build(),
                contentDescription = "User profile",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )

        // Settings Icon Action
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// --- Custom Reusable Floating Bottom Bar ---
@Composable
fun ShelfLifeBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    // Normalise sub-routes to identify highlighted tab
    val normalizedRoute = when {
        currentRoute.startsWith("ingredient_detail") -> "pantry"
        currentRoute.startsWith("recipe_detail") -> "recipes"
        currentRoute == "add_ingredient" -> "pantry"
        else -> currentRoute
    }

    val isDark = MaterialTheme.colorScheme.isDark
    val bottomBarContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding() // Safely wrap system navigation key lines
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = bottomBarContainerColor),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    label = "Home",
                    icon = Icons.Default.Home,
                    active = normalizedRoute == "dashboard",
                    onClick = { onNavigate("dashboard") }
                )
                BottomNavItem(
                    label = "Pantry",
                    icon = Icons.Default.Kitchen,
                    active = normalizedRoute == "pantry",
                    onClick = { onNavigate("pantry") }
                )
                BottomNavItem(
                    label = "Scan",
                    icon = Icons.Default.QrCodeScanner,
                    active = normalizedRoute == "scanner",
                    onClick = { onNavigate("scanner") }
                )
                BottomNavItem(
                    label = "Recipes",
                    icon = Icons.Default.RestaurantMenu,
                    active = normalizedRoute == "recipes",
                    onClick = { onNavigate("recipes") }
                )
                BottomNavItem(
                    label = "Shopping",
                    icon = Icons.Default.ShoppingCart,
                    active = normalizedRoute == "shopping_list",
                    onClick = { onNavigate("shopping_list") }
                )
            }
        }
    }
}

@Composable
private fun RowScope.BottomNavItem(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.isDark
    val activeTint = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else OnMintContainer
    val inactiveTint = MaterialTheme.colorScheme.onSurfaceVariant
    val textAndIconColor = if (active) activeTint else inactiveTint

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(32.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Active indicator pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = textAndIconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = textAndIconColor
        )
    }
}

// --- Empty State helper ---
@Composable
fun ShelfLifeEmptyState(
    emoji: String,
    title: String,
    description: String,
    actionButtonText: String? = null,
    onActionClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 36.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionButtonText != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = CircleShape
            ) {
                Text(actionButtonText, color = Color.White)
            }
        }
    }
}
