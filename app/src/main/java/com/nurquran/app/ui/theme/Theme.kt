package com.nurquran.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Emerald,
    onPrimary = SoftCream,
    secondary = Gold,
    onSecondary = DeepEmerald,
    tertiary = Jade,
    background = Cream,
    onBackground = InkGreen,
    surface = SoftCream,
    onSurface = InkGreen,
    surfaceVariant = Color(0xFFE8EDE5),
    onSurfaceVariant = MutedGreen,
    outline = Color(0xFFB7C4BA)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8BC8A7),
    onPrimary = DeepEmerald,
    secondary = Gold,
    onSecondary = DeepEmerald,
    tertiary = Color(0xFFA7D6BE),
    background = DarkCanvas,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = Color(0xFF244638),
    onSurfaceVariant = Color(0xFFB1C4B8),
    outline = Color(0xFF5C7869)
)

@Composable
fun NurQuranTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
