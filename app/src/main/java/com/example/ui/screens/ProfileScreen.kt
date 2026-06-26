package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardBackground
import com.example.ui.theme.MintContainer
import com.example.ui.theme.SageGreen
import com.example.ui.theme.SoftCoralError
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.WarmBackground
import com.example.ui.viewmodel.ShelfLifeViewModel

@Composable
fun ProfileScreen(
    viewModel: ShelfLifeViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val authState by viewModel.authUiState.collectAsState()
    var showSignOutDialog by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.background != WarmBackground
    val surfaceCard = if (isDark) MaterialTheme.colorScheme.surfaceVariant else CardBackground
    val signOutContainer = if (isDark) Color(0xFF2B201F) else Color(0xFFFFF0EE)
    val signOutContent = if (isDark) Color(0xFFFFB4AB) else SoftCoralError
    val signOutBorder = signOutContent.copy(alpha = if (isDark) 0.55f else 0.35f)

    val confirmSignOutContainer = if (isDark) Color(0xFFFFB4AB) else SoftCoralError
    val confirmSignOutContent = if (isDark) Color(0xFF690005) else Color.White
    val profileCardBrush = if (isDark) {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f),
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(Color(0xFFF9FAF1), Color(0xFFF6F7EE), Color(0xFFF3F4EA))
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                text = "Profile",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(22.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(profileCardBrush)
                    .padding(horizontal = 22.dp, vertical = 26.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(topStart = 120.dp, topEnd = 160.dp))
                        .background(
                            if (isDark) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
                            } else {
                                MintContainer.copy(alpha = 0.12f)
                            }
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .fillMaxWidth()
                        .height(88.dp)
                        .clip(RoundedCornerShape(topStart = 90.dp, topEnd = 90.dp))
                        .background(
                            if (isDark) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.10f)
                            } else {
                                WarmBackground.copy(alpha = 0.55f)
                            }
                        )
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(136.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(MintContainer, SageGreen))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = authState.initials,
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = authState.displayLabel,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        ProfileInfoRow(
            icon = Icons.Default.AccountCircle,
            label = "Account provider",
            value = if (authState.email?.contains("gmail", true) == true) {
                "Google or Email"
            } else {
                "Email"
            },
            containerColor = surfaceCard,
            isDark = isDark
        )

        Spacer(Modifier.height(18.dp))

        ProfileInfoRow(
            icon = Icons.Default.Email,
            label = "Email",
            value = authState.email ?: "No email available",
            containerColor = surfaceCard,
            isDark = isDark
        )

        Spacer(Modifier.height(48.dp))

        OutlinedButton(
            onClick = {
                showSignOutDialog = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = signOutContainer,
                contentColor = signOutContent
            ),
            border = BorderStroke(1.2.dp, signOutBorder),
            shape = RoundedCornerShape(999.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = signOutContent
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Sign Out",
                color = signOutContent,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        Spacer(Modifier.height(18.dp))
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign out?") },
            text = { Text("Are you sure you want to sign out of ShelfLife?") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut()
                        Toast.makeText(context, "Signed out successfully.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = confirmSignOutContainer,
                        contentColor = confirmSignOutContent
                    )
                ) {
                    Text("Sign Out")
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

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    containerColor: Color,
    isDark: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDark) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
                        } else {
                            SurfaceContainerHigh.copy(alpha = 0.6f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

        }
    }
}
