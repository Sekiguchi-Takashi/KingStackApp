package com.appathy.kingstack.ai

import com.appathy.kingstack.core.Engine
import com.appathy.kingstack.core.GameState
import com.appathy.kingstack.core.Move
import com.appathy.kingstack.core.Rules

object HintAI {

    const val DEFAULT_DEPTH = 2
    const val BEAM_WIDTH = 4

    data class Suggestion(val move: Move, val reason: String, val value: Int)

    fun best(state: GameState, depth: Int = DEFAULT_DEPTH): Suggestion? {
        val moves = Rules.legalMoves(state)
        if (moves.isEmpty()) return null
        var best: Suggestion? = null
        for (move in moves) {
            val next = Engine.applyMove(state, move)
            val value = search(next, depth - 1)
            if (best == null || value > best!!.value) {
                best = Suggestion(move, reasonFor(state, move, next), value)
            }
        }
        return best
    }

    private fun search(state: GameState, depth: Int): Int {
        val base = BoardAnalyzer.evaluate(state)
        if (depth <= 0) return base
        val moves = Rules.legalMoves(state)
        if (moves.isEmpty()) return base
        val beam = moves
            .map { Engine.applyMove(state, it) }
            .sortedByDescending { BoardAnalyzer.evaluate(it) }
            .take(BEAM_WIDTH)
        var deepest = base
        for (candidate in beam) {
            val value = search(candidate, depth - 1)
            if (value > deepest) deepest = value
        }
        return base + deepest / 2
    }

    private fun reasonFor(before: GameState, move: Move, after: GameState): String {
        if (after.completedCount > before.completedCount) return "A〜Kが完成して列が確定します"
        if (after.activeSlotCount > before.activeSlotCount) return "キングが先頭に来て新しい列が開放されます"
        val emptyBefore = countEmpty(before)
        val emptyAfter = countEmpty(after)
        if (emptyAfter > emptyBefore) return "空列を作れます。次の一手の自由度が上がります"
        val movedSize = before.slots[move.from].size - move.fromIndex
        if (movedSize > 1) return "同じスートの${movedSize}枚をまとめて移動できます"
        val top = before.slots[move.to].lastOrNull()
        if (top != null && top.rank == 13) return "キングの上にQを乗せて列を育てられます"
        return "合法手を増やしてデッドロックを避けられます"
    }

    private fun countEmpty(state: GameState): Int {
        var count = 0
        for (i in state.usableSlots) {
            if (state.slots[i].isEmpty()) count++
        }
        return count
    }
}
