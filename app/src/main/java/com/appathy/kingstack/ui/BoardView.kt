package com.appathy.kingstack.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appathy.kingstack.core.Card
import com.appathy.kingstack.core.GameState
import com.appathy.kingstack.core.MAX_SLOTS
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

private const val CARD_ASPECT = 0.72f
private val ROW_GAP = 3.dp
private val LEFT_PAD = 3.dp
private val STEP_MAX = 26.dp
private val STEP_MIN = 12.dp

/** ドラッグしていない状態を表す番兵。 */
private const val NO_DRAG = -2

/** 盤面の外に指がある状態を表す番兵。 */
private const val OUTSIDE = -1

/**
 * 画面の上から下へ並べる列の順番。
 * ロック中の6列目・7列目を上に置く。開放は下から進むので、ロック帯は常に一番上にまとまる。
 */
private val DISPLAY_ORDER = listOf(6, 5, 0, 1, 2, 3, 4)

/**
 * 列は横方向に伸びる帯。1枚目が左端で、右へ行くほど新しい。
 * 重ね順は正順なので、一番新しいカードが右端で全面見える。
 * 隠れる側は左になるため、左側のインデックスで数字とマークが読める。
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

        val rankSize = (cardHeight.value * 0.26f).coerceIn(11f, 22f).sp
        val suitSize = (cardHeight.value * 0.23f).coerceIn(10f, 20f).sp

        val bandPx = with(density) { (rowHeight + ROW_GAP).toPx() }
        val stepPx = with(density) { step.toPx() }
        val cardWidthPx = with(density) { cardWidth.toPx() }
        val cardHeightPx = with(density) { cardHeight.toPx() }
        val leftPx = with(density) { LEFT_PAD.toPx() }
        val slotSizes = state.slots.map { it.size }
        val activeCount = state.activeSlotCount

        // キングで列が増えた瞬間を捉えて、その列だけを短く光らせる。
        var lastActive by remember { mutableStateOf(activeCount) }
        var unlockedRow by remember { mutableStateOf(-1) }
        val pulse = remember { Animatable(0f) }
        LaunchedEffect(activeCount) {
            if (activeCount > lastActive) {
                unlockedRow = activeCount - 1
                lastActive = activeCount
                if (animate) {
                    pulse.snapTo(0f)
                    pulse.animateTo(1f, tween(durationMillis = 1100, easing = LinearEasing))
                }
                unlockedRow = -1
            } else {
                lastActive = activeCount
            }
        }

        // 指の座標はレイアウト時にだけ読む。合成をやり直さないので追従が軽い。
        val dragPoint = remember { mutableStateOf<Offset?>(null) }
        // 指が乗っている列。またいだときだけ変わるので、再合成はごく稀。
        var pointerSlot by remember { mutableStateOf(NO_DRAG) }

        fun rowAt(point: Offset): Int {
            if (bandPx <= 0f) return OUTSIDE
            val position = floor(point.y / bandPx).toInt()
            return if (position in 0 until MAX_SLOTS) DISPLAY_ORDER[position] else OUTSIDE
        }

        /**
         * 画面座標から「どの列の何枚目か」を求める。
         * 正順に重ねているので、その座標で一番手前にあるのは最も番号の大きいカード。
         * カードの上でなければ index に -1 を返し、その場合は列そのものが対象になる。
         */
        fun locate(point: Offset): Pair<Int, Int>? {
            if (stepPx <= 0f) return null
            val row = rowAt(point)
            if (row == OUTSIDE || row >= activeCount) return null
            val size = slotSizes[row]
            if (size == 0) return row to -1
            val x = point.x - leftPx
            if (x < 0f) return row to -1
            var index = floor(x / stepPx).toInt()
            if (index > size - 1) {
                index = size - 1
                if (x > index * stepPx + cardWidthPx) return row to -1
            }
            return row to index
        }

        val carrying = selection != null && pointerSlot != NO_DRAG && pointerSlot != selection.first

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
                    // 同じカードを指し続けている間は通知しない。無駄な再合成を止めて滑らかにする。
                    var lastHit: Pair<Int, Int>? = null
                    detectDragGestures(
                        onDragStart = { point ->
                            dragPoint.value = point
                            pointerSlot = rowAt(point)
                            val hit = locate(point)
                            lastHit = hit
                            if (hit != null) onDragStart(hit.first, hit.second)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragPoint.value = change.position
                            val row = rowAt(change.position)
                            if (row != pointerSlot) pointerSlot = row
                            val hit = locate(change.position)
                            if (hit != lastHit) {
                                lastHit = hit
                                if (hit != null) onDragMove(hit.first, hit.second)
                            }
                        },
                        onDragEnd = {
                            dragPoint.value = null
                            pointerSlot = NO_DRAG
                            lastHit = null
                            onDragEnd()
                        },
                        onDragCancel = {
                            dragPoint.value = null
                            pointerSlot = NO_DRAG
                            lastHit = null
                            onDragEnd()
                        }
                    )
                }
        ) {
            for (position in 0 until MAX_SLOTS) {
                val row = DISPLAY_ORDER[position]
                val locked = row >= activeCount
                val cards = state.slots[row]
                val selectedStart =
                    if (selection != null && selection.first == row) selection.second else null

                Box(
                    modifier = Modifier
                        .offset(y = (rowHeight + ROW_GAP) * position)
                        .fillMaxWidth()
                        .height(rowHeight)
                ) {
                    RowBand(
                        height = rowHeight,
                        locked = locked,
                        legal = targets.contains(row),
                        hovered = hover == row,
                        hinted = hintTo == row,
                        unlock = if (row == unlockedRow) pulse.value else -1f
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

                        for (index in cards.indices) {
                            val isSelected = selectedStart != null && index >= selectedStart
                            if (isSelected && carrying) continue
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
                                topOfStack = index == cards.size - 1,
                                rankSize = rankSize,
                                suitSize = suitSize
                            )
                        }
                    }
                }
            }

            if (carrying && selection != null) {
                CarriedStack(
                    cards = state.slots[selection.first].drop(selection.second),
                    point = dragPoint,
                    cardWidth = cardWidth,
                    cardHeight = cardHeight,
                    cardWidthPx = cardWidthPx,
                    cardHeightPx = cardHeightPx,
                    step = step,
                    design = design,
                    rankSize = rankSize,
                    suitSize = suitSize
                )
            }
        }
    }
}

/**
 * 指について動く運搬中のカード。
 * 座標の読み取りを offset のラムダに閉じ込めてあるので、指を動かしても再合成は起きない。
 */
@Composable
private fun CarriedStack(
    cards: List<Card>,
    point: State<Offset?>,
    cardWidth: Dp,
    cardHeight: Dp,
    cardWidthPx: Float,
    cardHeightPx: Float,
    step: Dp,
    design: Int,
    rankSize: TextUnit,
    suitSize: TextUnit
) {
    Box(
        modifier = Modifier.offset {
            val current = point.value ?: return@offset IntOffset.Zero
            IntOffset(
                (current.x - cardWidthPx / 2f).roundToInt(),
                (current.y - cardHeightPx / 2f).roundToInt()
            )
        }
    ) {
        cards.forEachIndexed { index, card ->
            CardFace(
                card = card,
                width = cardWidth,
                height = cardHeight,
                offsetX = step * index,
                design = design,
                selected = true,
                hinted = false,
                animate = false,
                topOfStack = index == cards.size - 1,
                rankSize = rankSize,
                suitSize = suitSize
            )
        }
    }
}

/**
 * 列の帯。
 * unlock は開放演出の進行度で、0から1へ進む間だけ光る。-1 は演出なし。
 */
@Composable
private fun RowBand(
    height: Dp,
    locked: Boolean,
    legal: Boolean,
    hovered: Boolean,
    hinted: Boolean,
    unlock: Float
) {
    // 3回ほど明滅させながら、全体としては減衰させる。
    val flash = if (unlock < 0f) 0f
    else ((1f - unlock) * abs(sin(unlock * 3f * PI_F))).coerceIn(0f, 1f)

    val fill = when {
        hovered -> Color(0x33E3B84F)
        legal -> Color(0x1AE3B84F)
        locked -> Palette.SlotLocked
        else -> Color(0x0AFFFFFF)
    }
    val edge = when {
        flash > 0f -> Palette.Gold
        hovered -> Palette.Gold
        legal -> Palette.GoldDim
        hinted -> Palette.Highlight
        else -> Color(0x14FFFFFF)
    }
    val borderWidth = when {
        flash > 0f -> (1f + 3f * flash).dp
        hovered || legal || hinted -> 2.dp
        else -> 1.dp
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .scale(1f + 0.05f * flash)
            .clip(RoundedCornerShape(6.dp))
            .background(fill)
            .border(borderWidth, edge, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.CenterEnd
    ) {
        if (flash > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Palette.Gold.copy(alpha = 0.55f * flash))
            )
        }
        if (locked) {
            Text(
                text = "K で開放",
                color = Palette.GoldDim,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 10.dp)
            )
        }
        if (unlock >= 0f) {
            Text(
                text = "列 開放",
                color = Palette.TextMain.copy(alpha = (1f - unlock).coerceIn(0f, 1f)),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

private const val PI_F = 3.1415927f

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
    topOfStack: Boolean,
    rankSize: TextUnit,
    suitSize: TextUnit
) {
    val target = if (selected) 1.10f else 1f
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
        // 隠れるのは右側なので、インデックスは左上の1箇所だけでよい。
        Index(card, design, rankSize, suitSize, Modifier.align(Alignment.TopStart))
    }
}

@Composable
private fun Index(
    card: Card,
    design: Int,
    rankSize: TextUnit,
    suitSize: TextUnit,
    modifier: Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 3.dp, vertical = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = card.label,
            color = Palette.cardText(design, card.suit),
            fontSize = rankSize,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = card.suit.mark,
            color = Palette.cardText(design, card.suit),
            fontSize = suitSize
        )
    }
}

@Composable
fun DotRow(total: Int, filled: Int, activeColor: Color, size: Dp = 10.dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until total) {
            Box(
                modifier = Modifier
                    .padding(end = 3.dp)
                    .size(size)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (i < filled) activeColor else Color(0x33FFFFFF))
            )
        }
    }
}
