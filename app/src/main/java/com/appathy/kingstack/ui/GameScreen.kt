package com.appathy.kingstack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Background)
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("SCORE", color = Palette.TextDim, fontSize = 10.sp)
                Text(
                    text = state.score.toString(),
                    color = Palette.Gold,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (state.daily) "DAILY" else state.difficulty.label,
                    color = Palette.TextDim,
                    fontSize = 10.sp
                )
                TextButton(onClick = { paused = true }) {
                    Text("MENU", color = Palette.TextMain, fontSize = 14.sp)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("完成", color = Palette.TextDim, fontSize = 11.sp)
            Spacer(modifier = Modifier.width(6.dp))
            DotRow(COMPLETE_TARGET, state.completedCount, Palette.Gold, 12.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Text("列", color = Palette.TextDim, fontSize = 11.sp)
            Spacer(modifier = Modifier.width(6.dp))
            DotRow(MAX_SLOTS, state.activeSlotCount, Palette.Highlight, 9.dp)
            Spacer(modifier = Modifier.weight(1f))
            Text("残り ${state.drawPile.size}", color = Palette.TextDim, fontSize = 11.sp)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Palette.Felt, RoundedCornerShape(10.dp))
                .padding(vertical = 8.dp)
                .verticalScroll(scroll)
        ) {
            BoardView(
                state = state,
                design = settings.cardDesign,
                animate = settings.animation,
                selection = controller.selection,
                targets = controller.legalTargets,
                hintFrom = controller.hint?.let { it.move.from to it.move.fromIndex },
                hintTo = controller.hint?.move?.to,
                onCardTap = { slot, index -> controller.tapCard(slot, index) },
                onSlotTap = { slot -> controller.tapSlot(slot) }
            )
        }

        val hint = controller.hint
        val message = controller.lastMessage
        Box(modifier = Modifier.fillMaxWidth().height(34.dp), contentAlignment = Alignment.Center) {
            when {
                hint != null -> Text(
                    text = "HINT: ${hint.reason}",
                    color = Palette.Highlight,
                    fontSize = 12.sp
                )
                message != null -> Text(text = message, color = Palette.Danger, fontSize = 12.sp)
                state.combo >= 2 -> Text(
                    text = "CHAIN × ${state.combo}",
                    color = Palette.Gold,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                controller.stuck -> Text(
                    text = "動かせる手がありません",
                    color = Palette.TextDim,
                    fontSize = 12.sp
                )
                else -> Text(
                    text = "移動 ${state.moveCount} / 配札 ${state.drawCount}",
                    color = Palette.TextDim,
                    fontSize = 11.sp
                )
            }
        }

        val actions: @Composable () -> Unit = {
            SmallAction("Undo", controller.canUndo) { controller.undo() }
            Spacer(modifier = Modifier.width(8.dp))
            SmallAction("Hint", state.status == GameStatus.PLAYING) { controller.useHint() }
            Spacer(modifier = Modifier.width(8.dp))
            SmallAction("リドロー", controller.canRedraw) { controller.redraw() }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = if (settings.leftHanded) Arrangement.Start else Arrangement.End
        ) {
            actions()
        }

        Button(
            onClick = { controller.drawCards() },
            enabled = controller.canDraw,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Palette.Gold,
                contentColor = Palette.Background,
                disabledContainerColor = Palette.GoldDim,
                disabledContentColor = Color(0x66000000)
            )
        ) {
            val dealCount = minOf(state.activeSlotCount, state.drawPile.size)
            val label = if (state.drawPile.isEmpty()) "山札なし" else "カードを配る（" + dealCount + "枚）"
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
                "完成数" to "${state.completedCount} / $COMPLETE_TARGET",
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
                "完成数" to "${state.completedCount} / $COMPLETE_TARGET",
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
