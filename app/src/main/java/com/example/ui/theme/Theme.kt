package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CommandDeckColorScheme = darkColorScheme(
    primary = DeckCyan,
    onPrimary = DeckBackground,
    primaryContainer = Color(0xFF0E3A4A),
    onPrimaryContainer = Color(0xFFA5F3FC),
    secondary = DeckPurple,
    onSecondary = DeckBackground,
    secondaryContainer = Color(0xFF3B1E54),
    onSecondaryContainer = Color(0xFFF3E8FF),
    tertiary = DeckAmber,
    onTertiary = DeckBackground,
    tertiaryContainer = Color(0xFF452A05),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = DeckBackground,
    onBackground = DeckTextPrimary,
    surface = DeckSurface,
    onSurface = DeckTextPrimary,
    surfaceVariant = DeckSurfaceElevated,
    onSurfaceVariant = DeckTextSecondary,
    outline = DeckBorderGlass,
    outlineVariant = Color(0x1F94A3B8),
    error = DeckRed,
    onError = DeckBackground
)

@Composable
fun CommandDeckTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CommandDeckColorScheme,
        typography = Typography,
        content = content
    )
}

// Keep backwards-compatibility alias if referenced in tests or previews
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    CommandDeckTheme(content = content)
}
