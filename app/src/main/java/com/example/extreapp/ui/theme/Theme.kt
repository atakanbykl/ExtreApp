package com.example.extreapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AppleBlueDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF00386B),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = AppleIndigoDark,
    onSecondary = Color.White,
    tertiary = AppleGreenDark,
    background = SurfaceGroupedDark,
    onBackground = Color(0xFFF5F5F7),
    surface = SurfaceCardDark,
    onSurface = Color(0xFFF5F5F7),
    surfaceVariant = SurfaceElevatedDark,
    onSurfaceVariant = Color(0xFFAEAEB2),
    outline = Color(0xFF3A3A3C),
    outlineVariant = Color(0xFF2C2C2E),
    error = AppleRedDark
)

private val LightColorScheme = lightColorScheme(
    primary = AppleBlueLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5F1FF),
    onPrimaryContainer = Color(0xFF003266),
    secondary = AppleIndigoLight,
    onSecondary = Color.White,
    tertiary = AppleGreenLight,
    background = SurfaceGroupedLight,
    onBackground = Color(0xFF1C1C1E),
    surface = SurfaceCardLight,
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF636366),
    outline = Color(0xFFD1D1D6),
    outlineVariant = Color(0xFFE5E5EA),
    error = AppleRedLight
)

@Composable
fun ExtreAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
