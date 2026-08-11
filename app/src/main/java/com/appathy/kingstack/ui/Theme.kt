package com.appathy.kingstack.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Palette {
    val Background = Color(0xFF0B1A15)
    val Felt = Color(0xFF123027)
    val FeltDeep = Color(0xFF0E251E)
    val Surface = Color(0xFF17362C)
    val Gold = Color(0xFFE3B84F)
    val GoldDim = Color(0xFF8A7233)
    val TextMain = Color(0xFFF2EFE4)
    val TextDim = Color(0xFF9BB0A6)
    val Danger = Color(0xFFC0553F)
    val SlotEmpty = Color(0x22FFFFFF)
    val SlotLocked = Color(0x11FFFFFF)
    val Highlight = Color(0xFF7FD4A8)

    fun cardFace(design: Int): Color = if (design == 0) Color(0xFFF7F1E1) else Color(0xFF1B2430)
    fun cardEdge(design: Int): Color = if (design == 0) Color(0xFFB9A87F) else Color(0xFF4A5A6E)
    fun cardText(design: Int, red: Boolean): Color = when {
        design == 0 && red -> Color(0xFFB03A2E)
        design == 0 -> Color(0xFF1C2620)
        red -> Color(0xFFEB7B6A)
        else -> Color(0xFFE6EDF5)
    }
}

private val Scheme = darkColorScheme(
    primary = Palette.Gold,
    onPrimary = Palette.Background,
    secondary = Palette.Highlight,
    background = Palette.Background,
    onBackground = Palette.TextMain,
    surface = Palette.Surface,
    onSurface = Palette.TextMain,
    error = Palette.Danger
)

@Composable
fun KingStackTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
