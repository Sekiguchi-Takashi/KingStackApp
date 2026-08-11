package com.appathy.kingstack.core

import com.appathy.kingstack.ai.DrawAI
import kotlin.random.Random

object Engine {

    const val SCORE_MOVE = 10
    const val SCORE_RUN_MOVE = 20
    const val SCORE_COMPLETE = 1000
    const val SCORE_KING = 300
    const val SCORE_CHAIN = 100
    const val SCORE_DRAW = -50
    const val SCORE_REDRAW = -100
    const val SCORE_HINT = -50
    const val CHAIN_STEP = 5

    fun newDeck(rng: Random): List<Card> = (0 until 52).map { Card(it) }.shuffled(rng)

    fun newGame(difficulty: Difficulty, rng: Random, daily: Boolean = false): GameState {
        val deck = newDeck(rng)
        val slots = MutableList(MAX_SLOTS) { mutableListOf<Card>() }
        for (i in 0 until INITIAL_SLOTS) {
            slots[i].add(deck[i])
        }
        val base = GameState(
            slots = slots.map { it.toList() },
            activeSlotCount = INITIAL_SLOTS,
            drawPile = deck.drop(INITIAL_SLOTS),
            difficulty = difficulty,
            daily = daily
        )
        return refresh(base)
    }

    fun applyMove(state: GameState, move: Move): GameState {
        val slots = state.slots.map { it.toMutableList() }
        val from = slots[move.from]
        if (move.fromIndex !in from.indices) return state
        val moving = from.subList(move.fromIndex, from.size).toList()
        repeat(moving.size) { from.removeAt(from.size - 1) }
        slots[move.to].addAll(moving)

        val combo = state.combo + 1
        var score = state.score + if (moving.size > 1) SCORE_RUN_MOVE else SCORE_MOVE
        if (combo % CHAIN_STEP == 0) score += SCORE_CHAIN

        val next = state.copy(
            slots = slots.map { it.toList() },
            moveCount = state.moveCount + 1,
            combo = combo,
            maxCombo = maxOf(state.maxCombo, combo),
            score = score,
            redrawAvailable = false
        )
        return refresh(next)
    }

    /** 配札。DrawAIが候補を評価して重み付きランダムで1組を選ぶ。 */
    fun deal(state: GameState, bias: Double, rng: Random): GameState {
        if (state.drawPile.isEmpty()) return state
        val count = minOf(state.activeSlotCount, state.drawPile.size)
        val chosen = DrawAI.chooseDeal(state, count, bias, rng)
        val ids = chosen.map { it.id }.toSet()
        val slots = state.slots.map { it.toMutableList() }
        for (i in chosen.indices) {
            slots[i].add(chosen[i])
        }
        val next = state.copy(
            slots = slots.map { it.toList() },
            drawPile = state.drawPile.filterNot { ids.contains(it.id) },
            drawCount = state.drawCount + 1,
            score = state.score + SCORE_DRAW,
            combo = 0,
            redrawAvailable = true
        )
        return refresh(next)
    }

    /**
     * 完成列の退避・キングによる列開放・勝敗判定をまとめて行う。
     * 盤面が変化したあとは必ずここを通す。
     */
    fun refresh(state: GameState): GameState {
        val slots = state.slots.map { it.toMutableList() }
        var score = state.score
        var completed = state.completedCount
        var combo = state.combo
        var maxCombo = state.maxCombo

        var changed = true
        while (changed) {
            changed = false
            for (i in 0 until state.activeSlotCount) {
                if (Rules.isCompleted(slots[i])) {
                    repeat(RUN_LENGTH) { slots[i].removeAt(slots[i].size - 1) }
                    completed++
                    combo++
                    maxCombo = maxOf(maxCombo, combo)
                    score += SCORE_COMPLETE + combo * 50
                    changed = true
                }
            }
        }

        var kingCount = state.kingCount
        var active = state.activeSlotCount
        val credited = state.creditedKings.toMutableSet()
        for (i in 0 until state.activeSlotCount) {
            val slot = slots[i]
            if (slot.isEmpty()) continue
            // 列の先頭（最下段）または、露出している一番上のカード。
            // 同じキングは一度しか加算されない。
            for (king in listOf(slot.first(), slot.last())) {
                if (king.rank != RUN_LENGTH) continue
                if (kingCount >= MAX_KING_UNLOCK) break
                if (credited.contains(king.id)) continue
                credited.add(king.id)
                kingCount++
                active++
                score += SCORE_KING
            }
        }

        val result = state.copy(
            slots = slots.map { it.toList() },
            activeSlotCount = active,
            completedCount = completed,
            score = score,
            combo = combo,
            maxCombo = maxCombo,
            kingCount = kingCount,
            creditedKings = credited
        )

        val status = when {
            result.completedCount >= COMPLETE_TARGET -> GameStatus.CLEAR
            result.drawPile.isEmpty() && Rules.productiveMoves(result).isEmpty() -> GameStatus.GAME_OVER
            else -> GameStatus.PLAYING
        }
        return result.copy(status = status)
    }
}
