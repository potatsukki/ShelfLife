package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShelfLifeViewModel

@Composable
fun SplashScreen(viewModel: ShelfLifeViewModel) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        viewModel.navigateTo("onboarding_1")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // App Logo Box
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingBasket,
                    contentDescription = "ShelfLife Logo",
                    tint = OnMintContainer,
                    modifier = Modifier.size(70.dp)
                )
                // Small Leaf Accent
                Icon(
                    imageVector = Icons.Default.Eco,
                    contentDescription = null,
                    tint = SageGreen,
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 12.dp, end = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ShelfLife",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your smart kitchen companion.",
                style = MaterialTheme.typography.bodyLarge,
                color = SoftGrayText,
                textAlign = TextAlign.Center
            )
        }

        // Subtly Moving Loading Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
        ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = alpha)))
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = alpha * 0.7f)))
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = alpha * 0.4f)))
        }
    }
}

@Composable
fun OnboardingScreenOne(viewModel: ShelfLifeViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Skip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, end = 20.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = "Skip",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { viewModel.skipOnboarding() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            // Visual Image Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(8.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("https://lh3.googleusercontent.com/aida-public/AB6AXuDn1b_5FbATUKArud5cL8ySxE9HnDbCm-im_11Or0nuaxkc2tPmrAAyUasWFnlYiUW_e2MYouf_nrMrPjuiQM3fwhJbM1EjMPq7wNu7ThOjvZ_X8cvDW2ijQ7-F0TiGznZICNgNsbfpse1avxBLTjn_7m_YC3L3FtRN3bpIWwXpuanmRSEP3xPV64WCSK6fxlziwjqe8QckwHnmwnQ7EeatUd8gogbXNCM3zD3klHLrQZDB2YwGhdcXhYw-cAgRQ5I5Wt5c13h-pxo-")
                        .crossfade(true)
                        .build(),
                    contentDescription = "Pantry organizaton image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Track what’s in your kitchen",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Keep your pantry organized and never forget what ingredients you already have.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp, start = 24.dp, end = 24.dp)
        ) {
            // Progress dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Box(modifier = Modifier.size(width = 24.dp, height = 6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant))
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant))
            }

            // Next button
            Button(
                onClick = { viewModel.navigateTo("onboarding_2") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Next", color = OnMintContainer, style = MaterialTheme.typography.labelLarge)
                    Icon(imageVector = Icons.AutoMirrored.Default.ArrowForward, contentDescription = null, tint = OnMintContainer)
                }
            }
        }
    }
}

@Composable
fun OnboardingScreenTwo(viewModel: ShelfLifeViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Skip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, end = 20.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = "Skip",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { viewModel.skipOnboarding() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            // Visual stacked cards simulating ingredients alerts
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Card 1: Spinach (Expiring Today)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .offset(y = (-40).dp)
                        .rotate(-3f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SoftCoralErrorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🥬", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Spinach", style = MaterialTheme.typography.labelLarge)
                            Text("Expires Today", color = SoftCoralError, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = SoftCoralError)
                    }
                }

                // Background Card 3: Eggs (Expiring Soon)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .offset(y = 40.dp)
                        .rotate(3f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PeachContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🥚", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Eggs", style = MaterialTheme.typography.labelLarge)
                            Text("Expiring Soon (2 days)", color = OnPeachContainer, style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = OnPeachContainer)
                    }
                }

                // Foreground Card 2: Milk (Fresh)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = 0.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🥛", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Milk", style = MaterialTheme.typography.labelLarge)
                            Text("Fresh (5 days left)", color = SageGreen, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SageGreen)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Reduce food waste",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Get reminders before ingredients expire so you can use them on time.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp, start = 24.dp, end = 24.dp)
        ) {
            // Progress dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant))
                Box(modifier = Modifier.size(width = 24.dp, height = 6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant))
            }

            // Next button
            Button(
                onClick = { viewModel.navigateTo("onboarding_3") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Next", color = OnMintContainer, style = MaterialTheme.typography.labelLarge)
                    Icon(imageVector = Icons.AutoMirrored.Default.ArrowForward, contentDescription = null, tint = OnMintContainer)
                }
            }
        }
    }
}

@Composable
fun OnboardingScreenThree(viewModel: ShelfLifeViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Skip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, end = 20.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = "Skip",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { viewModel.skipOnboarding() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            // Foreground Image card with AI Match overlay
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data("https://lh3.googleusercontent.com/aida-public/AB6AXuBevjdipu45hKqZpw1J6_k7NHztbdk-6JIQ0mBYYtZN6PLJKJ6E6jj7uv27d2LGTY299mrrjKSbZxorAr5hpECi9sPYg_Icrcm4D1Y0VRGM6yOmVfgqiWEReO8mcIJzfRCBWKGiFFrrdJfjmP4sd9TS-66gkppA_zdq4ep8YKBxZxeFRwmvomnITMjbn6jBMu8nJx22G7S8syqlCENlqXp0IyLgIzdj7YuriReakiEfAZoJvgVG_LMERfjAtUMzLzNZ-PV4U2DAWOHv")
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Chicken Stir Fry",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Chicken Stir Fry",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp), tint = SoftGrayText)
                            Text("20 mins prep", style = MaterialTheme.typography.bodySmall, color = SoftGrayText)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(PeachContainer)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Uses 2 expiring items", style = MaterialTheme.typography.labelSmall, color = OnPeachContainer)
                        }
                    }

                    // Floating AI Match label
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 10.dp, y = (-10).dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("✨ AI Match", style = MaterialTheme.typography.labelSmall, color = OnMintContainer, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Cook smarter with AI",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Get recipe ideas based on the ingredients already in your pantry.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp, start = 24.dp, end = 24.dp)
        ) {
            // Progress dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant))
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant))
                Box(modifier = Modifier.size(width = 24.dp, height = 6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            }

            // Next button -> Go dashboard!
            Button(
                onClick = { viewModel.navigateTo("dashboard") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Get Started", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
