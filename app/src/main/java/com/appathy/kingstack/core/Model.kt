package com.appathy.kingstack.core

const val MAX_SLOTS = 9
const val INITIAL_SLOTS = 5
const val MAX_KING_UNLOCK = 4
const val COMPLETE_TARGET = 4
const val RUN_LENGTH = 13

enum class Suit(val mark: String, val isRed: Boolean) {
    SPADE("\u2660", false),
    HEART("\u2665", true),
    DIAMOND("\u2666", true),
    CLUB("\u2663", false)
}

data class Card(val id: Int) {
    val suit: Suit
        get() = Suit.values()[id / 13]

    val rank: Int
        get() = id % 13 + 1

    val label: String
        get() = when (rank) {
            1 -> "A"
            11 -> "J"
            12 -> "Q"
            13 -> "K"
            else -> rank.toString()
        }
}

enum class Difficulty(val label: String, val aiBias: Double) {
    EASY("EASY", 0.90),
    NORMAL("NORMAL", 0.65),
    HARD("HARD", 0.35),
    EXPERT("EXPERT", 0.10)
}

enum class GameStatus { PLAYING, CLEAR, GAME_OVER }

data class Move(val from: Int, val fromIndex: Int, val to: Int)

data class GameState(
    val slots: List<List<Card>> = List(MAX_SLOTS) { emptyList() },
    val activeSlotCount: Int = INITIAL_SLOTS,
    val drawPile: List<Card> = emptyList(),
    val completedCount: Int = 0,
    /** A〜Kが揃って凍結した列。カードは残るが、以後は移動先にも配札先にもならない。 */
    val frozenSlots: Set<Int> = emptySet(),
    val score: Int = 0,
    val combo: Int = 0,
    val maxCombo: Int = 0,
    val moveCount: Int = 0,
    val drawCount: Int = 0,
    val redrawCount: Int = 0,
    val hintCount: Int = 0,
    val undoCount: Int = 0,
    val redrawAvailable: Boolean = false,
    val kingCount: Int = 0,
    val creditedKings: Set<Int> = emptySet(),
    val difficulty: Difficulty = Difficulty.NORMAL,
    val daily: Boolean = false,
    val status: GameStatus = GameStatus.PLAYING
) {
    val cardsOnBoard: Int
        get() = slots.sumOf { it.size }

    /** まだ使える列（開放済みかつ未凍結）。配札先もここ。 */
    val usableSlots: List<Int>
        get() = (0 until activeSlotCount).filter { !frozenSlots.contains(it) }

    val remainingCards: Int
        get() = cardsOnBoard + drawPile.size
}
