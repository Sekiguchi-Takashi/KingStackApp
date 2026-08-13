package com.appathy.kingstack.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.appathy.kingstack.core.Suit

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

    /**
     * 同じ赤どうし・同じ黒どうしを見分けられるよう、スートごとに色を分ける。
     * ハートとスペードを純色、ダイヤとクラブを従来の落ち着いた色にしている。
     */
    fun cardText(design: Int, suit: Suit): Color = if (design == 0) {
        when (suit) {
            Suit.HEART -> Color(0xFFD41111)
            Suit.DIAMOND -> Color(0xFFB0603A)
            Suit.SPADE -> Color(0xFF000000)
            Suit.CLUB -> Color(0xFF2E4A3C)
        }
    } else {
        when (suit) {
            Suit.HEART -> Color(0xFFFF5A5A)
            Suit.DIAMOND -> Color(0xFFEB9B6A)
            Suit.SPADE -> Color(0xFFFFFFFF)
            Suit.CLUB -> Color(0xFFAFC6BA)
        }
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
