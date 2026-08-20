package com.appathy.kingstack.ai

import com.appathy.kingstack.core.Card
import com.appathy.kingstack.core.Difficulty
import com.appathy.kingstack.core.GameState
import kotlin.random.Random

/**
 * 完全ランダムではなく、候補を評価してから重み付きランダムで選ぶ。
 * biasは「最良候補をそのまま採用する確率」。
 */
object DrawAI {

    const val CANDIDATES = 8

    fun chooseDeal(
        state: GameState,
        targets: List<Int>,
        bias: Double,
        rng: Random
    ): List<Card> {
        val pile = state.drawPile
        val count = minOf(targets.size, pile.size)
        if (count == 0) return emptyList()
        if (pile.size <= count) return pile

        val scored = (0 until CANDIDATES)
            .map { pile.shuffled(rng).take(count) }
            .map { it to evaluateDeal(state, targets, it) }
            .sortedByDescending { it.second }

        if (rng.nextDouble() < bias) return scored.first().first

        val rest = scored.drop(1)
        if (rest.isEmpty()) return scored.first().first
        val weights = rest.indices.map { (rest.size - it).toDouble() }
        var point = rng.nextDouble() * weights.sum()
        for (i in rest.indices) {
            point -= weights[i]
            if (point <= 0.0) return rest[i].first
        }
        return rest.last().first
    }

    private fun evaluateDeal(state: GameState, targets: List<Int>, cards: List<Card>): Int {
        val slots = state.slots.map { it.toMutableList() }
        for (i in cards.indices) {
            slots[targets[i]].add(cards[i])
        }
        val simulated = state.copy(slots = slots.map { it.toList() })
        return BoardAnalyzer.evaluate(simulated)
    }
}

object DifficultyAI {

    /**
     * 動的難易度。連勝が続けば少し辛く、連敗が続けば少し優しくする。
     * ただし必ずクリアできる状態にはしない（上限0.95・下限0.05）。
     */
    fun bias(difficulty: Difficulty, recentResults: List<Boolean>): Double {
        var value = difficulty.aiBias
        if (recentResults.size >= 3) {
            val rate = recentResults.count { it }.toDouble() / recentResults.size
            if (rate > 0.8) value -= 0.10
            if (rate < 0.3) value += 0.10
        }
        return value.coerceIn(0.05, 0.95)
    }
}
