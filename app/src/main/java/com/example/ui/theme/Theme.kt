package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MintContainer,
    onPrimary = OnMintContainer,
    primaryContainer = SageGreen,
    onPrimaryContainer = WarmBackground,
    secondary = PeachContainer,
    onSecondary = OnPeachContainer,
    secondaryContainer = PeachSecondary,
    onSecondaryContainer = WarmBackground,
    tertiary = BlueContainer,
    onTertiary = OnBlueContainer,
    background = DeepWalnutText,
    onBackground = WarmBackground,
    surface = DeepWalnutText,
    onSurface = WarmBackground,
    surfaceVariant = SoftGrayText,
    onSurfaceVariant = WarmBackground,
    error = SoftCoralError,
    errorContainer = SoftCoralErrorContainer,
    onError = OnSoftCoralContainer
)

private val LightColorScheme = lightColorScheme(
    primary = SageGreen,
    onPrimary = CardBackground,
    primaryContainer = MintContainer,
    onPrimaryContainer = OnMintContainer,
    secondary = PeachSecondary,
    onSecondary = CardBackground,
    secondaryContainer = PeachContainer,
    onSecondaryContainer = OnPeachContainer,
    tertiary = BlueTertiary,
    onTertiary = CardBackground,
    tertiaryContainer = BlueContainer,
    onTertiaryContainer = OnBlueContainer,
    background = WarmBackground,
    onBackground = DeepWalnutText,
    surface = WarmBackground,
    onSurface = DeepWalnutText,
    surfaceVariant = SurfaceContainerHigh,
    onSurfaceVariant = SoftGrayText,
    error = SoftCoralError,
    errorContainer = SoftCoralErrorContainer,
    onError = CardBackground,
    onErrorContainer = OnSoftCoralContainer,
    outline = OutlineColor,
    outlineVariant = OutlineVariantColor
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamic logic by default to preserve cozy kitchen visual brand colors beautifully
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

val androidx.compose.material3.ColorScheme.isDark: Boolean
    get() = this.background == DeepWalnutText

