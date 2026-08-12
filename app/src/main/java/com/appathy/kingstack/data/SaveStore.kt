package com.appathy.kingstack.data

import android.content.Context
import com.appathy.kingstack.core.Card
import com.appathy.kingstack.core.Difficulty
import com.appathy.kingstack.core.GameState
import com.appathy.kingstack.core.GameStatus
import com.appathy.kingstack.core.MAX_SLOTS
import org.json.JSONArray
import org.json.JSONObject

data class Settings(
    val sound: Boolean = true,
    val vibration: Boolean = true,
    val animation: Boolean = true,
    val cardDesign: Int = 0,
    val leftHanded: Boolean = false,
    val strictDraw: Boolean = true,
    val redrawEnabled: Boolean = true
)

data class Stats(
    val plays: Int = 0,
    val clears: Int = 0,
    val bestScore: Int = 0,
    val bestChain: Int = 0,
    val minDraw: Int = 0,
    val minRedraw: Int = 0,
    val recent: List<Boolean> = emptyList(),
    val dailyDate: String = ""
) {
    val clearRate: Int
        get() = if (plays == 0) 0 else clears * 100 / plays
}

class SaveStore(context: Context) {

    private val prefs = context.getSharedPreferences("kingstack", Context.MODE_PRIVATE)

    fun saveGame(state: GameState) {
        if (state.status != GameStatus.PLAYING) {
            clearGame()
            return
        }
        val root = JSONObject()
        val slots = JSONArray()
        for (slot in state.slots) {
            val array = JSONArray()
            for (card in slot) array.put(card.id)
            slots.put(array)
        }
        root.put("slots", slots)
        val pile = JSONArray()
        for (card in state.drawPile) pile.put(card.id)
        root.put("pile", pile)
        val kings = JSONArray()
        for (id in state.creditedKings) kings.put(id)
        root.put("kings", kings)
        root.put("active", state.activeSlotCount)
        root.put("completed", state.completedCount)
        root.put("score", state.score)
        root.put("combo", state.combo)
        root.put("maxCombo", state.maxCombo)
        root.put("moves", state.moveCount)
        root.put("draws", state.drawCount)
        root.put("redraws", state.redrawCount)
        root.put("hints", state.hintCount)
        root.put("undos", state.undoCount)
        root.put("redrawAvailable", state.redrawAvailable)
        root.put("kingCount", state.kingCount)
        root.put("difficulty", state.difficulty.name)
        root.put("daily", state.daily)
        prefs.edit().putString("game", root.toString()).apply()
    }

    fun loadGame(): GameState? {
        val text = prefs.getString("game", null) ?: return null
        return try {
            val root = JSONObject(text)
            val slotsJson = root.getJSONArray("slots")
            val slots = ArrayList<List<Card>>()
            for (i in 0 until MAX_SLOTS) {
                val array = if (i < slotsJson.length()) slotsJson.getJSONArray(i) else JSONArray()
                val cards = ArrayList<Card>()
                for (j in 0 until array.length()) cards.add(Card(array.getInt(j)))
                slots.add(cards)
            }
            val pileJson = root.getJSONArray("pile")
            val pile = ArrayList<Card>()
            for (i in 0 until pileJson.length()) pile.add(Card(pileJson.getInt(i)))
            val kingsJson = root.getJSONArray("kings")
            val kings = HashSet<Int>()
            for (i in 0 until kingsJson.length()) kings.add(kingsJson.getInt(i))
            GameState(
                slots = slots,
                activeSlotCount = root.getInt("active"),
                drawPile = pile,
                completedCount = root.getInt("completed"),
                score = root.getInt("score"),
                combo = root.getInt("combo"),
                maxCombo = root.getInt("maxCombo"),
                moveCount = root.getInt("moves"),
                drawCount = root.getInt("draws"),
                redrawCount = root.getInt("redraws"),
                hintCount = root.getInt("hints"),
                undoCount = root.getInt("undos"),
                redrawAvailable = root.getBoolean("redrawAvailable"),
                kingCount = root.getInt("kingCount"),
                creditedKings = kings,
                difficulty = Difficulty.valueOf(root.getString("difficulty")),
                daily = root.optBoolean("daily", false),
                status = GameStatus.PLAYING
            )
        } catch (e: Exception) {
            null
        }
    }

    fun hasSavedGame(): Boolean = prefs.contains("game")

    fun clearGame() {
        prefs.edit().remove("game").apply()
    }

    fun loadSettings(): Settings = Settings(
        sound = prefs.getBoolean("sound", true),
        vibration = prefs.getBoolean("vibration", true),
        animation = prefs.getBoolean("animation", true),
        cardDesign = prefs.getInt("cardDesign", 0),
        leftHanded = prefs.getBoolean("leftHanded", false),
        strictDraw = prefs.getBoolean("strictDraw", true),
        redrawEnabled = prefs.getBoolean("redrawEnabled", true)
    )

    fun saveSettings(settings: Settings) {
        prefs.edit()
            .putBoolean("sound", settings.sound)
            .putBoolean("vibration", settings.vibration)
            .putBoolean("animation", settings.animation)
            .putInt("cardDesign", settings.cardDesign)
            .putBoolean("leftHanded", settings.leftHanded)
            .putBoolean("strictDraw", settings.strictDraw)
            .putBoolean("redrawEnabled", settings.redrawEnabled)
            .apply()
    }

    fun loadStats(): Stats {
        val recentText = prefs.getString("recent", "") ?: ""
        val recent = recentText.mapNotNull {
            when (it) {
                '1' -> true
                '0' -> false
                else -> null
            }
        }
        return Stats(
            plays = prefs.getInt("plays", 0),
            clears = prefs.getInt("clears", 0),
            bestScore = prefs.getInt("bestScore", 0),
            bestChain = prefs.getInt("bestChain", 0),
            minDraw = prefs.getInt("minDraw", 0),
            minRedraw = prefs.getInt("minRedraw", 0),
            recent = recent,
            dailyDate = prefs.getString("dailyDate", "") ?: ""
        )
    }

    fun saveStats(stats: Stats) {
        val recentText = stats.recent.takeLast(10).joinToString("") { if (it) "1" else "0" }
        prefs.edit()
            .putInt("plays", stats.plays)
            .putInt("clears", stats.clears)
            .putInt("bestScore", stats.bestScore)
            .putInt("bestChain", stats.bestChain)
            .putInt("minDraw", stats.minDraw)
            .putInt("minRedraw", stats.minRedraw)
            .putString("recent", recentText)
            .putString("dailyDate", stats.dailyDate)
            .apply()
    }

    fun resetAll() {
        prefs.edit().clear().apply()
    }
}
