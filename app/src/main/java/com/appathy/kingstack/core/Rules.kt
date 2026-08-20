package com.appathy.kingstack.core

/**
 * ルールはUIから完全に独立させる。
 * 列数・配札枚数・キング条件などの変更はこのファイルとModel.ktの定数だけで完結する。
 */
object Rules {

    /** カードは「1つ大きい数字」の上に置ける。空列には何でも置ける。 */
    fun canPlace(card: Card, onto: Card?): Boolean {
        if (onto == null) return true
        return onto.rank == card.rank + 1
    }

    /** fromIndex から列の一番上までが「同一スート・連番・正しい順序」か。 */
    fun isMovableRun(slot: List<Card>, fromIndex: Int): Boolean {
        if (fromIndex < 0 || fromIndex >= slot.size) return false
        var i = fromIndex
        while (i < slot.size - 1) {
            val lower = slot[i]
            val upper = slot[i + 1]
            if (lower.suit != upper.suit) return false
            if (lower.rank != upper.rank + 1) return false
            i++
        }
        return true
    }

    /** 一括移動の起点になりうるインデックス（上から下へ連続する範囲）。 */
    fun runStartIndices(slot: List<Card>): List<Int> {
        val result = mutableListOf<Int>()
        var i = slot.size - 1
        while (i >= 0 && isMovableRun(slot, i)) {
            result.add(i)
            i--
        }
        return result
    }

    /** 一括移動できる最長の起点。 */
    fun longestRunStart(slot: List<Card>): Int {
        if (slot.isEmpty()) return -1
        return runStartIndices(slot).minOrNull() ?: (slot.size - 1)
    }

    /**
     * そのカードに触れたときに掴む位置。
     * 重なって続いているカードは束ごと動かしたいので、上のカードではなく束の根元を返す。
     * 動かせないカードに触れた場合は -1。
     */
    fun grabStart(slot: List<Card>, index: Int): Int {
        if (index !in slot.indices) return -1
        val start = longestRunStart(slot)
        if (start < 0) return -1
        return if (index >= start) start else -1
    }

    fun legalMoves(state: GameState): List<Move> {
        val moves = mutableListOf<Move>()
        val usable = state.usableSlots
        for (from in usable) {
            val slot = state.slots[from]
            if (slot.isEmpty()) continue
            for (index in runStartIndices(slot)) {
                val head = slot[index]
                for (to in usable) {
                    if (to == from) continue
                    val dest = state.slots[to]
                    // 列まるごとを空列へ動かすだけの手は意味がないので除外する
                    if (dest.isEmpty() && index == 0) continue
                    if (canPlace(head, dest.lastOrNull())) {
                        moves.add(Move(from, index, to))
                    }
                }
            }
        }
        return moves
    }

    fun legalTargets(state: GameState, from: Int, fromIndex: Int): Set<Int> {
        if (state.frozenSlots.contains(from)) return emptySet()
        val slot = state.slots.getOrNull(from) ?: return emptySet()
        if (fromIndex !in slot.indices) return emptySet()
        if (!isMovableRun(slot, fromIndex)) return emptySet()
        val head = slot[fromIndex]
        val result = mutableSetOf<Int>()
        for (to in state.usableSlots) {
            if (to == from) continue
            val dest = state.slots[to]
            if (dest.isEmpty() && fromIndex == 0) continue
            if (canPlace(head, dest.lastOrNull())) result.add(to)
        }
        return result
    }

    /**
     * その手が盤面を前進させるか。
     * カードを置けば必ず1つ隣接ができるが、移動元で隣接を1つ壊すなら差し引きゼロ。
     * 空列への移動は隣接を増やさないので「前進」には数えない。
     */
    fun isProductive(state: GameState, move: Move): Boolean {
        val src = state.slots[move.from]
        val dest = state.slots[move.to]
        if (move.fromIndex !in src.indices) return false
        val gained = if (dest.isEmpty()) 0 else 1
        val lost = if (move.fromIndex > 0 &&
            src[move.fromIndex - 1].rank == src[move.fromIndex].rank + 1
        ) 1 else 0
        return gained - lost > 0
    }

    /** 前進する手だけを返す。「手詰まり」はこれが空の状態を指す。 */
    fun productiveMoves(state: GameState): List<Move> =
        legalMoves(state).filter { isProductive(state, it) }

    /** 列の一番上13枚が K,Q,J,...,A の順で積まれていれば完成。 */
    fun isCompleted(slot: List<Card>): Boolean {
        if (slot.size < RUN_LENGTH) return false
        val tail = slot.takeLast(RUN_LENGTH)
        for (i in tail.indices) {
            if (tail[i].rank != RUN_LENGTH - i) return false
        }
        return true
    }
}
