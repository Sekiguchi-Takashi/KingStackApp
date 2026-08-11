package com.appathy.kingstack.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appathy.kingstack.core.Card
import com.appathy.kingstack.core.GameState
import com.appathy.kingstack.core.MAX_SLOTS

@Composable
fun CardFace(
    card: Card,
    width: Dp,
    height: Dp,
    design: Int,
    selected: Boolean,
    hinted: Boolean,
    animate: Boolean,
    onClick: () -> Unit
) {
    val lift = if (animate) animateDpAsState(if (selected) 6.dp else 0.dp, label = "lift").value
    else if (selected) 6.dp else 0.dp
    val edge = when {
        selected -> Palette.Gold
        hinted -> Palette.Highlight
        else -> Palette.cardEdge(design)
    }
    Box(
        modifier = Modifier
            .offset(y = -lift)
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(Palette.cardFace(design))
            .border(if (selected || hinted) 2.dp else 1.dp, edge, RoundedCornerShape(6.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(start = 4.dp, top = 2.dp)) {
            Text(
                text = card.label,
                color = Palette.cardText(design, card.suit.isRed),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = card.suit.mark,
                color = Palette.cardText(design, card.suit.isRed),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun BoardView(
    state: GameState,
    design: Int,
    animate: Boolean,
    selection: Pair<Int, Int>?,
    targets: Set<Int>,
    hintFrom: Pair<Int, Int>?,
    hintTo: Int?,
    onCardTap: (Int, Int) -> Unit,
    onSlotTap: (Int) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val gap = 3.dp
        val cardWidth = (maxWidth - gap * (MAX_SLOTS + 1)) / MAX_SLOTS
        val cardHeight = cardWidth * 1.42f
        val overlap = cardHeight * 0.32f

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = gap)) {
            for (slotIndex in 0 until MAX_SLOTS) {
                val locked = slotIndex >= state.activeSlotCount
                val cards = state.slots[slotIndex]
                val columnHeight =
                    if (cards.isEmpty()) cardHeight else cardHeight + overlap * (cards.size - 1)
                Box(
                    modifier = Modifier
                        .width(cardWidth)
                        .height(columnHeight)
                ) {
                    SlotBase(
                        width = cardWidth,
                        height = cardHeight,
                        locked = locked,
                        highlighted = targets.contains(slotIndex),
                        onClick = { if (!locked) onSlotTap(slotIndex) }
                    )
                    if (!locked) {
                        cards.forEachIndexed { cardIndex, card ->
                            val isSelected = selection != null &&
                                selection.first == slotIndex && cardIndex >= selection.second
                            val isHinted = hintFrom != null &&
                                hintFrom.first == slotIndex && cardIndex >= hintFrom.second
                            Box(modifier = Modifier.offset(y = overlap * cardIndex)) {
                                CardFace(
                                    card = card,
                                    width = cardWidth,
                                    height = cardHeight,
                                    design = design,
                                    selected = isSelected,
                                    hinted = isHinted,
                                    animate = animate,
                                    onClick = { onCardTap(slotIndex, cardIndex) }
                                )
                            }
                        }
                    }
                    if (hintTo == slotIndex) {
                        Box(
                            modifier = Modifier
                                .width(cardWidth)
                                .height(columnHeight)
                                .border(2.dp, Palette.Highlight, RoundedCornerShape(6.dp))
                        )
                    }
                }
                if (slotIndex < MAX_SLOTS - 1) Spacer(modifier = Modifier.width(gap))
            }
        }
    }
}

@Composable
private fun SlotBase(
    width: Dp,
    height: Dp,
    locked: Boolean,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    val fill = if (locked) Palette.SlotLocked else Palette.SlotEmpty
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(fill)
            .border(
                if (highlighted) 2.dp else 1.dp,
                if (highlighted) Palette.Gold else Color(0x22FFFFFF),
                RoundedCornerShape(6.dp)
            )
            .clickable(enabled = !locked) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (locked) {
            Text(text = "K", color = Palette.GoldDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DotRow(total: Int, filled: Int, activeColor: Color, size: Dp = 10.dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until total) {
            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(size)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (i < filled) activeColor else Color(0x33FFFFFF))
            )
        }
    }
}
