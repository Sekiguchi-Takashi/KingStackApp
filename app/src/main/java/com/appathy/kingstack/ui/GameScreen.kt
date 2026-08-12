package com.appathy.kingstack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appathy.kingstack.core.COMPLETE_TARGET
import com.appathy.kingstack.core.GameController
import com.appathy.kingstack.core.GameStatus
import com.appathy.kingstack.core.MAX_SLOTS

@Composable
fun GameScreen(
    controller: GameController,
    onHome: () -> Unit,
    onRules: () -> Unit
) {
    val state = controller.state
    val settings = controller.settings
    var paused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Background)
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        // 1行目: スコアと進捗。高さを詰めて盤面に場所を譲る。
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = state.score.toString(),
                    color = Palette.Gold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (state.daily) "  DAILY" else "  " + state.difficulty.label,
                    color = Palette.TextDim,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                DotRow(COMPLETE_TARGET, state.completedCount, Palette.Gold, 10.dp)
                Spacer(modifier = Modifier.width(10.dp))
                DotRow(MAX_SLOTS, state.activeSlotCount, Palette.Highlight, 7.dp)
            }
        }

        // 2行目: 操作ボタン。配る以外はすべてここに集約する。
        val actions: @Composable () -> Unit = {
            SmallAction("MENU", true) { paused = true }
            Spacer(modifier = Modifier.width(6.dp))
            SmallAction("Undo", controller.canUndo) { controller.undo() }
            Spacer(modifier = Modifier.width(6.dp))
            SmallAction("Hint", state.status == GameStatus.PLAYING) { controller.useHint() }
            if (settings.redrawEnabled) {
                Spacer(modifier = Modifier.width(6.dp))
                SmallAction("リドロー", controller.canRedraw) { controller.redraw() }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = if (settings.leftHanded) Arrangement.Start else Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            actions()
        }

        // 3行目: 状態表示。
        val hint = controller.hint
        val message = controller.lastMessage
        Box(
            modifier = Modifier.fillMaxWidth().height(20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            when {
                hint != null -> Text(
                    text = "HINT: " + hint.reason,
                    color = Palette.Highlight,
                    fontSize = 11.sp
                )
                message != null -> Text(text = message, color = Palette.Danger, fontSize = 11.sp)
                state.combo >= 2 -> Text(
                    text = "CHAIN × " + state.combo,
                    color = Palette.Gold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                controller.stuck -> Text(
                    text = "手詰まりです。カードを配れます",
                    color = Palette.TextDim,
                    fontSize = 11.sp
                )
                else -> Text(
                    text = "移動 " + state.moveCount + " / 配札 " + state.drawCount +
                        " / 前進できる手 " + controller.productiveCount,
                    color = Palette.TextDim,
                    fontSize = 11.sp
                )
            }
        }

        // 盤面。残りの高さをすべて使う。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Palette.Felt, RoundedCornerShape(8.dp))
                .padding(3.dp)
        ) {
            BoardView(
                state = state,
                design = settings.cardDesign,
                animate = settings.animation,
                selection = controller.selection,
                targets = controller.legalTargets,
                hover = controller.hover,
                hintFrom = controller.hint?.let { it.move.from to it.move.fromIndex },
                hintTo = controller.hint?.move?.to,
                onTap = { slot, index -> controller.tapCard(slot, index) },
                onDragStart = { slot, index -> controller.dragStart(slot, index) },
                onDragMove = { slot, index -> controller.dragMove(slot, index) },
                onDragEnd = { controller.dragEnd() }
            )
        }

        // 残り枚数は配るボタンの真上に置く。
        Box(
            modifier = Modifier.fillMaxWidth().height(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "残り " + state.drawPile.size + " 枚",
                color = Palette.TextDim,
                fontSize = 11.sp
            )
        }

        val dealCount = minOf(state.activeSlotCount, state.drawPile.size)
        Button(
            onClick = { controller.drawCards() },
            enabled = controller.canPressDraw,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Palette.Gold,
                contentColor = Palette.Background,
                disabledContainerColor = Palette.GoldDim,
                disabledContentColor = Color(0x66000000)
            )
        ) {
            val label = if (state.drawPile.isEmpty()) "山札なし"
            else "カードを配る（" + dealCount + "枚）"
            Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (paused && state.status == GameStatus.PLAYING) {
        AlertDialog(
            onDismissRequest = { paused = false },
            confirmButton = {
                TextButton(onClick = { paused = false }) { Text("続ける") }
            },
            dismissButton = {
                TextButton(onClick = {
                    paused = false
                    onHome()
                }) { Text("ホーム") }
            },
            title = { Text("PAUSE") },
            text = {
                Column {
                    TextButton(onClick = {
                        paused = false
                        controller.restart()
                    }) { Text("リスタート") }
                    TextButton(onClick = {
                        paused = false
                        onRules()
                    }) { Text("ルール") }
                }
            }
        )
    }

    if (state.status == GameStatus.CLEAR) {
        ResultDialog(
            title = "CLEAR",
            lines = listOf(
                "スコア" to state.score.toString(),
                "完成数" to (state.completedCount.toString() + " / " + COMPLETE_TARGET),
                "移動数" to state.moveCount.toString(),
                "配札数" to state.drawCount.toString(),
                "リドロー数" to state.redrawCount.toString(),
                "最大CHAIN" to state.maxCombo.toString()
            ),
            primary = Pair("もう一度", { controller.restart() }),
            secondary = Pair("ホーム", onHome)
        )
    }

    if (state.status == GameStatus.GAME_OVER) {
        ResultDialog(
            title = "GAME OVER",
            lines = listOf(
                "スコア" to state.score.toString(),
                "完成数" to (state.completedCount.toString() + " / " + COMPLETE_TARGET),
                "残りカード" to state.cardsOnBoard.toString(),
                "移動数" to state.moveCount.toString(),
                "最大CHAIN" to state.maxCombo.toString()
            ),
            primary = Pair("リトライ", { controller.restart() }),
            secondary = Pair("ホーム", onHome)
        )
    }
}

@Composable
private fun SmallAction(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.height(38.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Palette.Surface,
            contentColor = Palette.TextMain,
            disabledContainerColor = Color(0x11FFFFFF),
            disabledContentColor = Palette.TextDim
        )
    ) {
        Text(label, fontSize = 13.sp)
    }
}

@Composable
private fun ResultDialog(
    title: String,
    lines: List<Pair<String, String>>,
    primary: Pair<String, () -> Unit>,
    secondary: Pair<String, () -> Unit>
) {
    AlertDialog(
        onDismissRequest = { },
        confirmButton = {
            TextButton(onClick = primary.second) { Text(primary.first) }
        },
        dismissButton = {
            TextButton(onClick = secondary.second) { Text(secondary.first) }
        },
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                for (line in lines) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(line.first, color = Palette.TextDim, fontSize = 13.sp)
                        Text(line.second, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    )
}
