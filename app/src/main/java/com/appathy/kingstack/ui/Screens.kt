package com.appathy.kingstack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appathy.kingstack.core.Difficulty
import com.appathy.kingstack.core.GameController

@Composable
private fun ScreenFrame(title: String, onBack: (() -> Unit)?, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Background)
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(title, color = Palette.Gold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        content()
        if (onBack != null) {
            Spacer(modifier = Modifier.height(24.dp))
            MenuButton("戻る", false) { onBack() }
        }
    }
}

@Composable
fun MenuButton(label: String, primary: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp).padding(vertical = 3.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) Palette.Gold else Palette.Surface,
            contentColor = if (primary) Palette.Background else Palette.TextMain
        )
    ) {
        Text(label, fontSize = 15.sp, fontWeight = if (primary) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun HomeScreen(
    hasSave: Boolean,
    onContinue: () -> Unit,
    onPlay: () -> Unit,
    onDaily: () -> Unit,
    onStats: () -> Unit,
    onRules: () -> Unit,
    onSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("KING", color = Palette.Gold, fontSize = 46.sp, fontWeight = FontWeight.Bold)
        Text("STACK", color = Palette.TextMain, fontSize = 46.sp, fontWeight = FontWeight.Bold)
        Text(
            "積んで、開放して、配り直す。",
            color = Palette.TextDim,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 28.dp)
        )
        if (hasSave) MenuButton("つづきから", true, onContinue)
        MenuButton("PLAY GAME", !hasSave, onPlay)
        MenuButton("DAILY CHALLENGE", false, onDaily)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) { MenuButton("戦績", false, onStats) }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) { MenuButton("ルール", false, onRules) }
        }
        MenuButton("設定", false, onSettings)
    }
}

@Composable
fun DifficultyScreen(onSelect: (Difficulty) -> Unit, onBack: () -> Unit) {
    ScreenFrame("難易度", onBack) {
        Text(
            "難易度は配札AIの「有利な札を選ぶ確率」を変えます。EXPERTでは山札はほとんど容赦しません。",
            color = Palette.TextDim,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        for (difficulty in Difficulty.values()) {
            MenuButton(difficulty.label, difficulty == Difficulty.NORMAL) { onSelect(difficulty) }
        }
    }
}

@Composable
fun StatsScreen(controller: GameController, onBack: () -> Unit) {
    val stats = controller.stats
    ScreenFrame("戦績", onBack) {
        StatRow("プレイ回数", stats.plays.toString())
        StatRow("クリア回数", stats.clears.toString())
        StatRow("クリア率", stats.clearRate.toString() + "%")
        StatRow("BEST SCORE", stats.bestScore.toString())
        StatRow("BEST CHAIN", stats.bestChain.toString())
        StatRow("最少配札", if (stats.minDraw == 0) "-" else stats.minDraw.toString())
        StatRow("最少リドロー", if (stats.clears == 0) "-" else stats.minRedraw.toString())
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Palette.TextDim, fontSize = 14.sp)
        Text(value, color = Palette.TextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RulesScreen(onBack: () -> Unit) {
    ScreenFrame("ルール", onBack) {
        RuleBlock("目的", "A〜Kの13枚を1列に揃えて退避させる。4セット完成でCLEAR。")
        RuleBlock("置き方", "カードは「数字が1つ大きいカード」の上に置けます。空列には何でも置けます。")
        RuleBlock("まとめて移動", "同じスートで連番に並んだカードは、まとめて移動できます。スートが混ざっていると1枚ずつです。")
        RuleBlock("キング", "キングが列の先頭（一番下）に来ると、ロックされた列が1つ開放されます。5列→6列→7列まで。")
        RuleBlock("配札", "「カードを配る」で、開放中の全列に1枚ずつ配られます。スコアは-50。")
        RuleBlock("リドロー", "配った直後、まだ1枚もカードを動かしていない場合に限り、1回だけ配り直せます。スコアは-100。")
        RuleBlock("デッドロック", "動かせる手がなく、山札も尽きたらGAME OVERです。")
        RuleBlock("AI", "配札はランダムではありません。AIが盤面を評価し、難易度に応じた有利さで札を選びます。")
    }
}

@Composable
private fun RuleBlock(title: String, body: String) {
    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        Text(title, color = Palette.Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(body, color = Palette.TextMain, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
fun SettingsScreen(controller: GameController, onBack: () -> Unit) {
    val settings = controller.settings
    var confirmReset by remember { mutableStateOf(false) }

    ScreenFrame("設定", onBack) {
        ToggleRow("サウンド", settings.sound) { controller.updateSettings(settings.copy(sound = it)) }
        ToggleRow("振動", settings.vibration) { controller.updateSettings(settings.copy(vibration = it)) }
        ToggleRow("アニメーション", settings.animation) { controller.updateSettings(settings.copy(animation = it)) }
        ToggleRow("左利きモード", settings.leftHanded) { controller.updateSettings(settings.copy(leftHanded = it)) }
        ToggleRow("配札は手詰まり時のみ", settings.strictDraw) { controller.updateSettings(settings.copy(strictDraw = it)) }
        Text(
            "ONにすると仕様書どおり「合法手がないときだけ配札できる」厳格モードになります。",
            color = Palette.TextDim,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("カードデザイン", color = Palette.TextMain, fontSize = 14.sp)
            TextButton(onClick = {
                controller.updateSettings(settings.copy(cardDesign = if (settings.cardDesign == 0) 1 else 0))
            }) {
                Text(if (settings.cardDesign == 0) "CLASSIC" else "MIDNIGHT", color = Palette.Gold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        MenuButton("データリセット", false) { confirmReset = true }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    controller.resetAllData()
                }) { Text("削除する") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("やめる") }
            },
            title = { Text("データリセット") },
            text = { Text("戦績・設定・保存中のゲームをすべて削除します。元に戻せません。") }
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Palette.TextMain, fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
