package com.ditzy.nurquran.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Emerald = Color(0xFF0B4B3A)
val DeepEmerald = Color(0xFF082F24)
val Jade = Color(0xFF28765D)
val Gold = Color(0xFFD5B15F)
val Cream = Color(0xFFF7F3E8)
val SoftCream = Color(0xFFFFFDF7)
val Ink = Color(0xFF1D352D)
val MutedInk = Color(0xFF758179)
val PaleGold = Color(0xFFF4E8C6)
val PaleGreen = Color(0xFFE7F0E9)

private val NurColors = lightColorScheme(
    primary = Emerald,
    onPrimary = Color.White,
    secondary = Gold,
    onSecondary = DeepEmerald,
    background = Cream,
    onBackground = Ink,
    surface = SoftCream,
    onSurface = Ink,
    surfaceVariant = PaleGreen,
    onSurfaceVariant = Emerald,
    outline = Color(0xFFD6DDD6),
    error = Color(0xFFB3261E)
)

@Composable
fun NurQuranTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NurColors, content = content)
}
