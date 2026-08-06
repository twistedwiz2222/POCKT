package com.pockt.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF080A0D)
val Surface = Color(0xFF11151A)
val Raised = Color(0xFF181D23)
val Mint = Color(0xFF61E7B6)
val Coral = Color(0xFFFF7B72)
val Text = Color(0xFFF4F7F6)
val Muted = Color(0xFF94A09C)

private val PocktColors = darkColorScheme(
    primary = Mint,
    onPrimary = Ink,
    secondary = Color(0xFF9CB8FF),
    background = Ink,
    onBackground = Text,
    surface = Surface,
    onSurface = Text,
    surfaceVariant = Raised,
    onSurfaceVariant = Muted,
    error = Coral,
)

@Composable fun PocktTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PocktColors, content = content)
}
