package com.appathy.kingstack.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appathy.kingstack.core.Card
import com.appathy.kingstack.core.GameState
import com.appathy.kingstack.core.MAX_SLOTS
import kotlin.math.ceil
import kotlin.math.floor

private const val CARD_ASPECT = 0.70f
private val ROW_GAP = 4.dp
private val LEFT_PAD = 4.dp
private val STEP_MAX = 20.dp
private val STEP_MIN = 11.dp

/**
 * 列は画面の横方向に伸びる帯。1枚目が左端で、以降は右へ少しずつずらして重ねる。
 * 重ね順は反転させていて、若い番号のカードほど手前に描く。
 * これによりどのカードも右端だけが必ず露出するので、右端に寄せた数字とマークが常に見える。
 */
@Composable
fun BoardView(
    state: GameState,
    design: Int,
    animate: Boolean,
    selection: Pair<Int, Int>?,
    targets: Set<Int>,
    hover: Int?,
    hintFrom: Pair<Int, Int>?,
    hintTo: Int?,
    onTap: (Int, Int) -> Unit,
    onDragStart: (Int, Int) -> Unit,
    onDragMove: (Int, Int) -> Unit,
    onDragEnd: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current

        val rowHeight = (maxHeight - ROW_GAP * (MAX_SLOTS - 1)) / MAX_SLOTS
        val cardHeight = rowHeight
        val cardWidth = cardHeight * CARD_ASPECT
        val maxCards = state.slots.maxOfOrNull { it.size } ?: 1
        val available = maxWidth - LEFT_PAD * 2 - cardWidth
        val step = if (maxCards <= 1) STEP_MAX
        else (available / (maxCards - 1)).coerceIn(STEP_MIN, STEP_MAX)

        val bandPx = with(density) { (rowHeight + ROW_GAP).toPx() }
        val stepPx = with(density) { step.toPx() }
        val cardWidthPx = with(density) { cardWidth.toPx() }
        val leftPx = with(density) { LEFT_PAD.toPx() }
        val slotSizes = state.slots.map { it.size }
        val activeCount = state.activeSlotCount

        /**
         * 画面座標から「どの列の何枚目か」を求める。
         * カードの上でなければ index に -1 を返し、その場合は列そのものが対象になる。
         */
        fun locate(point: Offset): Pair<Int, Int>? {
            if (bandPx <= 0f || stepPx <= 0f) return null
            val row = floor(point.y / bandPx).toInt()
            if (row < 0 || row >= activeCount) return null
            val size = slotSizes[row]
            if (size == 0) return row to -1
            val x = point.x - leftPx
            if (x < 0f) return row to -1
            val lowest = ceil((x - cardWidthPx) / stepPx).toInt().coerceAtLeast(0)
            val highest = floor(x / stepPx).toInt()
            if (lowest > highest) return row to -1
            if (lowest > size - 1) return row to -1
            return row to lowest
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(slotSizes, activeCount, stepPx) {
                    detectTapGestures { point ->
                        val hit = locate(point) ?: return@detectTapGestures
                        onTap(hit.first, hit.second)
                    }
                }
                .pointerInput(slotSizes, activeCount, stepPx) {
                    detectDragGestures(
                        onDragStart = { point ->
                            val hit = locate(point)
                            if (hit != null) onDragStart(hit.first, hit.second)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val hit = locate(change.position)
                            if (hit != null) onDragMove(hit.first, hit.second)
                        },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragEnd() }
                    )
                }
        ) {
            for (row in 0 until MAX_SLOTS) {
                val locked = row >= activeCount
                val cards = state.slots[row]
                val selectedStart =
                    if (selection != null && selection.first == row) selection.second else null

                Box(
                    modifier = Modifier
                        .offset(y = (rowHeight + ROW_GAP) * row)
                        .fillMaxWidth()
                        .height(rowHeight)
                ) {
                    RowBand(
                        height = rowHeight,
                        locked = locked,
                        legal = targets.contains(row),
                        hovered = hover == row,
                        hinted = hintTo == row
                    )

                    if (!locked) {
                        if (cards.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .offset(x = LEFT_PAD)
                                    .width(cardWidth)
                                    .height(cardHeight)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Palette.SlotEmpty)
                            )
                        }

                        val order = ArrayList<Int>(cards.size)
                        for (index in cards.indices.reversed()) {
                            if (selectedStart == null || index < selectedStart) order.add(index)
                        }
                        if (selectedStart != null) {
                            for (index in selectedStart until cards.size) order.add(index)
                        }

                        for (index in order) {
                            val isSelected = selectedStart != null && index >= selectedStart
                            val isHinted = hintFrom != null &&
                                hintFrom.first == row && index >= hintFrom.second
                            CardFace(
                                card = cards[index],
                                width = cardWidth,
                                height = cardHeight,
                                offsetX = LEFT_PAD + step * index,
                                design = design,
                                selected = isSelected,
                                hinted = isHinted,
                                animate = animate,
                                topOfStack = index == cards.size - 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowBand(
    height: Dp,
    locked: Boolean,
    legal: Boolean,
    hovered: Boolean,
    hinted: Boolean
) {
    val fill = when {
        hovered -> Color(0x33E3B84F)
        legal -> Color(0x1AE3B84F)
        locked -> Palette.SlotLocked
        else -> Color(0x0AFFFFFF)
    }
    val edge = when {
        hovered -> Palette.Gold
        legal -> Palette.GoldDim
        hinted -> Palette.Highlight
        else -> Color(0x14FFFFFF)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(fill)
            .border(if (hovered || legal || hinted) 2.dp else 1.dp, edge, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.CenterEnd
    ) {
        if (locked) {
            Text(
                text = "K で開放",
                color = Palette.GoldDim,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 10.dp)
            )
        }
    }
}

@Composable
private fun CardFace(
    card: Card,
    width: Dp,
    height: Dp,
    offsetX: Dp,
    design: Int,
    selected: Boolean,
    hinted: Boolean,
    animate: Boolean,
    topOfStack: Boolean
) {
    val target = if (selected) 1.16f else 1f
    val scale = if (animate) {
        animateFloatAsState(targetValue = target, label = "scale").value
    } else {
        target
    }
    val edge = when {
        selected -> Palette.Gold
        hinted -> Palette.Highlight
        topOfStack -> Palette.GoldDim
        else -> Palette.cardEdge(design)
    }
    Box(
        modifier = Modifier
            .offset(x = offsetX)
            .width(width)
            .height(height)
            .scale(scale)
            .clip(RoundedCornerShape(6.dp))
            .background(Palette.cardFace(design))
            .border(if (selected || hinted) 2.dp else 1.dp, edge, RoundedCornerShape(6.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 3.dp, top = 2.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = card.label,
                color = Palette.cardText(design, card.suit.isRed),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = card.suit.mark,
                color = Palette.cardText(design, card.suit.isRed),
                fontSize = 12.sp
            )
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
