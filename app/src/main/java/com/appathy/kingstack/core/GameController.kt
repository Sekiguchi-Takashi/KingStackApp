package com.appathy.kingstack.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.appathy.kingstack.ai.DifficultyAI
import com.appathy.kingstack.ai.HintAI
import com.appathy.kingstack.data.Feedback
import com.appathy.kingstack.data.SaveStore
import com.appathy.kingstack.data.Settings
import com.appathy.kingstack.data.Stats
import java.util.Calendar
import kotlin.random.Random

class GameController(
    private val store: SaveStore,
    private val feedback: Feedback
) {

    var state by mutableStateOf(Engine.newGame(Difficulty.NORMAL, Random.Default))
        private set

    var settings by mutableStateOf(store.loadSettings())
        private set

    var stats by mutableStateOf(store.loadStats())
        private set

    var selection by mutableStateOf<Pair<Int, Int>?>(null)
        private set

    var hint by mutableStateOf<HintAI.Suggestion?>(null)
        private set

    var lastMessage by mutableStateOf<String?>(null)

    /** ドラッグ中に指が乗っている列。ここで指を離すと移動が確定する。 */
    var hover by mutableStateOf<Int?>(null)
        private set

    private val undoStack = ArrayDeque<GameState>()
    private var preDraw: GameState? = null
    private var recorded = false
    private var rng: Random = Random.Default

    val legalTargets: Set<Int>
        get() {
            val sel = selection ?: return emptySet()
            return Rules.legalTargets(state, sel.first, sel.second)
        }

    /** 前進する手の数。0なら手詰まり扱い。 */
    val productiveCount: Int
        get() = Rules.productiveMoves(state).size

    val stuck: Boolean
        get() = productiveCount == 0

    /** ボタンを押せるか。押した結果を説明したいので厳格モードでも押させる。 */
    val canPressDraw: Boolean
        get() = state.status == GameStatus.PLAYING && state.drawPile.isNotEmpty()

    val canDraw: Boolean
        get() = canPressDraw && (!settings.strictDraw || stuck)

    val canRedraw: Boolean
        get() = settings.redrawEnabled && state.status == GameStatus.PLAYING &&
            state.redrawAvailable && preDraw != null

    val canUndo: Boolean
        get() = state.status == GameStatus.PLAYING && undoStack.isNotEmpty()

    fun hasSavedGame(): Boolean = store.hasSavedGame()

    fun startNew(difficulty: Difficulty) {
        rng = Random.Default
        reset(Engine.newGame(difficulty, rng))
    }

    fun startDaily() {
        val calendar = Calendar.getInstance()
        val seed = calendar.get(Calendar.YEAR) * 10000L +
            (calendar.get(Calendar.MONTH) + 1) * 100L +
            calendar.get(Calendar.DAY_OF_MONTH)
        rng = Random(seed)
        reset(Engine.newGame(Difficulty.NORMAL, rng, daily = true))
    }

    fun resumeSaved(): Boolean {
        val saved = store.loadGame() ?: return false
        rng = Random.Default
        undoStack.clear()
        preDraw = null
        recorded = false
        selection = null
        hint = null
        state = saved
        return true
    }

    fun restart() {
        startNew(state.difficulty)
    }

    private fun reset(next: GameState) {
        undoStack.clear()
        preDraw = null
        recorded = false
        selection = null
        hover = null
        hint = null
        lastMessage = null
        state = next
        store.saveGame(state)
    }

    fun tapCard(slot: Int, index: Int) {
        if (state.status != GameStatus.PLAYING) return
        if (index < 0) {
            tapSlot(slot)
            return
        }
        val current = selection
        if (current != null && current.first != slot) {
            tryMove(current.first, current.second, slot)
            return
        }
        if (current != null && current.first == slot && current.second == index) {
            selection = null
            return
        }
        val column = state.slots[slot]
        if (index !in column.indices) return
        if (!Rules.isMovableRun(column, index)) {
            feedback.reject(settings)
            lastMessage = "その位置からはまとめて動かせません"
            return
        }
        selection = slot to index
        lastMessage = null
    }

    /**
     * 指が触れた瞬間。動かせないカードならアクティブにしない。
     */
    fun dragStart(slot: Int, index: Int) {
        if (state.status != GameStatus.PLAYING) return
        hover = null
        if (index < 0) return
        val column = state.slots.getOrNull(slot) ?: return
        if (index in column.indices && Rules.isMovableRun(column, index)) {
            selection = slot to index
            lastMessage = null
        }
    }

    /**
     * なぞっている最中。
     * 同じ列の上では最後に触れたカードをアクティブにするが、動かせないカードは飛ばして
     * 直前のカードをアクティブのまま保つ。別の列の帯に入ったら、そこが移動先候補になる。
     */
    fun dragMove(slot: Int, index: Int) {
        if (state.status != GameStatus.PLAYING) return
        val current = selection
        if (current == null) {
            if (index < 0) return
            val column = state.slots.getOrNull(slot) ?: return
            if (index in column.indices && Rules.isMovableRun(column, index)) {
                selection = slot to index
            }
            return
        }
        if (slot == current.first) {
            hover = null
            if (index < 0) return
            val column = state.slots[slot]
            if (index in column.indices && Rules.isMovableRun(column, index)) {
                selection = slot to index
            }
            return
        }
        hover = if (legalTargets.contains(slot)) slot else null
    }

    /** 指を離した瞬間。移動先候補の帯の上なら、その列の末尾へ置く。 */
    fun dragEnd() {
        val current = selection
        val target = hover
        hover = null
        if (current == null || target == null) return
        tryMove(current.first, current.second, target)
    }

    fun tapSlot(slot: Int) {
        if (state.status != GameStatus.PLAYING) return
        val current = selection
        if (current == null) return
        if (current.first == slot) {
            selection = null
            return
        }
        tryMove(current.first, current.second, slot)
    }

    private fun tryMove(from: Int, fromIndex: Int, to: Int) {
        val move = Move(from, fromIndex, to)
        if (!Rules.legalTargets(state, from, fromIndex).contains(to)) {
            feedback.reject(settings)
            lastMessage = "そこには置けません"
            selection = null
            return
        }
        val before = state
        pushUndo(before)
        val next = Engine.applyMove(before, move)
        state = next
        selection = null
        hover = null
        hint = null
        lastMessage = null
        if (next.completedCount > before.completedCount || next.activeSlotCount > before.activeSlotCount) {
            feedback.complete(settings)
        } else {
            feedback.move(settings)
        }
        afterChange()
    }

    fun drawCards() {
        if (!canDraw) {
            if (settings.strictDraw) lastMessage = "まだ前進できる手が" + productiveCount + "つあります"
            return
        }
        val before = state
        pushUndo(before)
        preDraw = before
        state = Engine.deal(before, currentBias(), rng)
        selection = null
        hint = null
        feedback.deal(settings)
        afterChange()
    }

    fun redraw() {
        val base = preDraw
        if (!canRedraw || base == null) return
        val dealt = Engine.deal(
            base.copy(
                score = base.score + Engine.SCORE_REDRAW,
                redrawCount = state.redrawCount + 1
            ),
            currentBias(),
            rng
        )
        state = dealt.copy(redrawAvailable = false)
        preDraw = null
        selection = null
        hint = null
        feedback.deal(settings)
        afterChange()
    }

    fun undo() {
        if (!canUndo) return
        val previous = undoStack.removeLast()
        state = previous.copy(undoCount = state.undoCount + 1, combo = 0)
        preDraw = null
        selection = null
        hint = null
        lastMessage = null
        store.saveGame(state)
    }

    fun useHint() {
        if (state.status != GameStatus.PLAYING) return
        val suggestion = HintAI.best(state)
        if (suggestion == null) {
            hint = null
            lastMessage = if (state.drawPile.isEmpty()) "手がありません" else "動かせる手がありません。カードを配ってください"
            return
        }
        hint = suggestion
        state = state.copy(
            hintCount = state.hintCount + 1,
            score = state.score + Engine.SCORE_HINT
        )
        store.saveGame(state)
    }

    fun clearHint() {
        hint = null
    }

    fun updateSettings(next: Settings) {
        settings = next
        store.saveSettings(next)
    }

    fun resetAllData() {
        store.resetAll()
        settings = store.loadSettings()
        stats = store.loadStats()
        startNew(Difficulty.NORMAL)
    }

    private fun currentBias(): Double =
        DifficultyAI.bias(state.difficulty, stats.recent)

    private fun pushUndo(snapshot: GameState) {
        undoStack.addLast(snapshot)
        while (undoStack.size > 60) undoStack.removeFirst()
    }

    private fun afterChange() {
        store.saveGame(state)
        if (state.status != GameStatus.PLAYING && !recorded) {
            recorded = true
            recordResult(state.status == GameStatus.CLEAR)
        }
    }

    private fun recordResult(cleared: Boolean) {
        val current = stats
        val minDraw = if (cleared) {
            if (current.minDraw == 0) state.drawCount else minOf(current.minDraw, state.drawCount)
        } else current.minDraw
        val minRedraw = if (cleared) {
            if (current.minRedraw == 0 && current.clears == 0) state.redrawCount
            else minOf(current.minRedraw, state.redrawCount)
        } else current.minRedraw
        val next = current.copy(
            plays = current.plays + 1,
            clears = current.clears + if (cleared) 1 else 0,
            bestScore = maxOf(current.bestScore, state.score),
            bestChain = maxOf(current.bestChain, state.maxCombo),
            minDraw = minDraw,
            minRedraw = minRedraw,
            recent = (current.recent + cleared).takeLast(10)
        )
        stats = next
        store.saveStats(next)
    }
}
