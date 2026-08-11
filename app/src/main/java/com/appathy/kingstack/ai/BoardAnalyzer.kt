package com.appathy.kingstack.ai

import com.appathy.kingstack.core.Card
import com.appathy.kingstack.core.GameState
import com.appathy.kingstack.core.MAX_KING_UNLOCK
import com.appathy.kingstack.core.RUN_LENGTH
import com.appathy.kingstack.core.Rules

/**
 * 仕様書16章の評価関数。
 * 重みはここだけを触れば調整できる。
 */
object BoardAnalyzer {

    const val W_COMPLETION = 1000
    const val W_KING_UNLOCK = 300
    const val W_EMPTY_SLOT = 150
    const val W_STACK_LENGTH = 20
    const val W_FUTURE_MOVE = 10
    const val W_CHAIN_POTENTIAL = 5
    const val W_DEADLOCK_RISK = -300
    const val W_TIGHT = -100
    const val W_ISOLATED = -15

    fun evaluate(state: GameState): Int {
        var value = 0
        val moves = Rules.legalMoves(state)
        value += state.completedCount * W_COMPLETION
        value += moves.size * W_FUTURE_MOVE

        var emptySlots = 0
        for (i in 0 until state.activeSlotCount) {
            val slot = state.slots[i]
            if (slot.isEmpty()) {
                emptySlots++
                value += W_EMPTY_SLOT
                continue
            }
            val start = Rules.longestRunStart(slot)
            val runLength = slot.size - start
            value += runLength * W_STACK_LENGTH
            value += longestSequence(slot) * W_CHAIN_POTENTIAL
            value += (RUN_LENGTH - distanceToCompletion(slot)) * 4
            if (slot.size == 1) value += W_ISOLATED
        }

        if (state.kingCount < MAX_KING_UNLOCK) {
            value += kingUnlockPotential(state, emptySlots)
        }

        if (moves.isEmpty()) {
            value += W_DEADLOCK_RISK
            if (state.drawPile.isEmpty()) value += W_DEADLOCK_RISK * 3
        } else if (moves.size <= 2) {
            value += W_TIGHT
        }
        return value
    }

    /** 列の上から見て連番がどれだけ続いているか（スート不問）。 */
    private fun longestSequence(slot: List<Card>): Int {
        var best = 1
        var current = 1
        for (i in 0 until slot.size - 1) {
            if (slot[i].rank == slot[i + 1].rank + 1) {
                current++
                if (current > best) best = current
            } else {
                current = 1
            }
        }
        return best
    }

    /** A〜K完成まであと何枚必要か（一番進んでいる列の視点）。 */
    private fun distanceToCompletion(slot: List<Card>): Int {
        var length = 1
        for (i in slot.size - 1 downTo 1) {
            if (slot[i - 1].rank == slot[i].rank + 1) length++ else break
        }
        return RUN_LENGTH - length
    }

    private fun kingUnlockPotential(state: GameState, emptySlots: Int): Int {
        var value = 0
        for (i in 0 until state.activeSlotCount) {
            val slot = state.slots[i]
            val base = slot.firstOrNull() ?: continue
            if (base.rank == RUN_LENGTH) {
                value += W_KING_UNLOCK / 2
                continue
            }
            val top = slot.last()
            if (top.rank == RUN_LENGTH && emptySlots > 0) {
                value += W_KING_UNLOCK
            }
        }
        return value
    }
}
