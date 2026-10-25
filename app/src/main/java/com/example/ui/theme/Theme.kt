package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SentinelColorScheme = darkColorScheme(
    primary = SentinelGreen,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFA7F3D0),

    secondary = SentinelBlue,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF0C4A6E),
    onSecondaryContainer = Color(0xFFBAE6FD),

    tertiary = SentinelPurple,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF4C1D95),
    onTertiaryContainer = Color(0xFFDDD6FE),

    background = SentinelDarkBg,
    onBackground = SentinelTextPrimary,

    surface = SentinelSurface,
    onSurface = SentinelTextPrimary,
    surfaceVariant = SentinelSurfaceVariant,
    onSurfaceVariant = SentinelTextSecondary,

    outline = SentinelBorder,
    outlineVariant = Color(0xFF1E293B),

    error = SentinelRed,
    onError = Color.White,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // We enforce our custom crafted dark theme for the developer-focused experience
    MaterialTheme(
        colorScheme = SentinelColorScheme,
        typography = Typography,
        content = content
    )
}
