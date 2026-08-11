package com.appathy.kingstack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.appathy.kingstack.core.GameController
import com.appathy.kingstack.data.Feedback
import com.appathy.kingstack.data.SaveStore
import com.appathy.kingstack.ui.DifficultyScreen
import com.appathy.kingstack.ui.GameScreen
import com.appathy.kingstack.ui.HomeScreen
import com.appathy.kingstack.ui.KingStackTheme
import com.appathy.kingstack.ui.RulesScreen
import com.appathy.kingstack.ui.SettingsScreen
import com.appathy.kingstack.ui.StatsScreen

enum class Screen { HOME, DIFFICULTY, GAME, STATS, RULES, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KingStackTheme {
                KingStackApp()
            }
        }
    }
}

@Composable
fun KingStackApp() {
    val context = LocalContext.current
    val controller = remember {
        val store = SaveStore(context.applicationContext)
        GameController(store, Feedback(context.applicationContext))
    }
    var screen by remember { mutableStateOf(Screen.HOME) }
    var previous by remember { mutableStateOf(Screen.HOME) }
    var hasSave by remember { mutableStateOf(controller.hasSavedGame()) }

    BackHandler(enabled = screen != Screen.HOME) {
        screen = if (screen == Screen.RULES && previous == Screen.GAME) Screen.GAME else Screen.HOME
    }

    when (screen) {
        Screen.HOME -> HomeScreen(
            hasSave = hasSave,
            onContinue = {
                if (controller.resumeSaved()) screen = Screen.GAME else hasSave = false
            },
            onPlay = { screen = Screen.DIFFICULTY },
            onDaily = {
                controller.startDaily()
                screen = Screen.GAME
            },
            onStats = { screen = Screen.STATS },
            onRules = {
                previous = Screen.HOME
                screen = Screen.RULES
            },
            onSettings = { screen = Screen.SETTINGS }
        )

        Screen.DIFFICULTY -> DifficultyScreen(
            onSelect = { difficulty ->
                controller.startNew(difficulty)
                screen = Screen.GAME
            },
            onBack = { screen = Screen.HOME }
        )

        Screen.GAME -> GameScreen(
            controller = controller,
            onHome = {
                hasSave = controller.hasSavedGame()
                screen = Screen.HOME
            },
            onRules = {
                previous = Screen.GAME
                screen = Screen.RULES
            }
        )

        Screen.STATS -> StatsScreen(controller) { screen = Screen.HOME }

        Screen.RULES -> RulesScreen {
            screen = if (previous == Screen.GAME) Screen.GAME else Screen.HOME
        }

        Screen.SETTINGS -> SettingsScreen(controller) { screen = Screen.HOME }
    }
}
