package com.sundram.weplay.game

import kotlin.random.Random

/**
 * Pure Ludo engine: four players, 52-cell ring, six-cell home stretch.
 */

enum class PColor { RED, GREEN, YELLOW, BLUE }

data class Token(val idx: Int, val pos: Int) {
    companion object {
        const val BASE = -1
        const val RING_MAX = 50              // ring = rel 0..50 (51 cells)
        const val HOME_FROM = 51             // 51..56 = home stretch
        const val DONE = 57
    }
}

data class LudoPlayer(
    val color: PColor,
    val start: Int,                       // ring offset
    val isBot: Boolean,
    val tokens: List<Token>,              // indices 0..3
) {
    val finished: Int get() = tokens.count { it.pos == Token.DONE }
}

data class LudoState(
    val players: List<LudoPlayer>,
    val current: Int = 0,
    val dice: Int = 0,
    val phase: Phase = Phase.ROLL,
    val streak: Int = 0,                  // consecutive sixes this turn
    val winner: Int = -1,
    val log: List<String> = listOf("Roll the dice!"),
) {
    enum class Phase { ROLL, MOVE, OVER }

    val currentColor: PColor get() = players[current].color
    val currentPlayer: LudoPlayer get() = players[current]

    fun legalMoves(dice: Int): List<Int> = buildList {
        players[current].tokens.forEachIndexed { i, t ->
            when {
                t.pos == Token.DONE -> Unit
                t.pos == Token.BASE -> if (dice == 6) add(i)
                t.pos + dice <= Token.DONE -> add(i)
            }
        }
    }
}

class LudoGame(seed: Long = System.currentTimeMillis()) {
    private val rnd = Random(seed)
    var state: LudoState = init()
        private set
    var onChange: (LudoState) -> Unit = {}

    // safe cells: home-entry posts (player starts) + middle caps
    private val safeCells = setOf(0, 13, 26, 39, 12, 25, 38, 51)

    private fun init(): LudoState = LudoState(
        players = listOf(
            LudoPlayer(PColor.RED, 0, false, List(4) { Token(it, Token.BASE) }),
            LudoPlayer(PColor.GREEN, 13, true, List(4) { Token(it, Token.BASE) }),
            LudoPlayer(PColor.YELLOW, 26, true, List(4) { Token(it, Token.BASE) }),
            LudoPlayer(PColor.BLUE, 39, true, List(4) { Token(it, Token.BASE) }),
        )
    )

    fun reset() { state = init(); changed() }

    fun roll() {
        if (state.phase != LudoState.Phase.ROLL) return
        val d = rnd.nextInt(1, 7)
        if (d == 6 && state.streak >= 2) {
            state = state.copy(dice = d, streak = 0, phase = LudoState.Phase.ROLL)
                .with("3 sixes in a row — turn cancelled!")
            nextPlayer(false)
            changed()
            botRollNext()
            return
        }
        val streak = if (d == 6) state.streak + 1 else 0
        state = state.copy(dice = d, streak = streak, phase = LudoState.Phase.MOVE)
        val who = if (state.currentPlayer.isBot) "Bot ${state.currentColor}" else state.currentColor.name
        state = state.with("$who rolled $d")
        if (state.legalMoves(d).isEmpty()) {
            state = state.with("no move possible")
            nextPlayer(d == 6)
            changed()
            botRollNext()
            return
        }
        changed()
    }

    /** idx = token index of current legal move. Returns true if handled. */
    fun moveToken(idx: Int): Boolean {
        val s = state
        if (s.phase != LudoState.Phase.MOVE) return false
        if (idx !in s.legalMoves(s.dice)) return false
        val p = s.currentPlayer
        val t = p.tokens[idx]
        val from = t.pos
        val to = if (from == Token.BASE) 0 else from + s.dice

        var captured = 0
        var msg = when {
            from == Token.BASE -> "${p.color} launched a token"
            to == Token.DONE -> "${p.color} token is home!"
            to >= Token.HOME_FROM -> "${p.color} enters home stretch"
            else -> "${p.color} moves $from to $to"
        }

        var players = s.players.toMutableList()
        players[s.current] = p.copy(tokens = p.tokens.map { if (it.idx == idx) Token(it.idx, to) else it })

        // capture on ring only, not on safe cells
        if (to in 0..Token.RING_MAX) {
            val land = (p.start + to) % 52
            if (land !in safeCells) {
                players.indices.filter { it != s.current }.forEach { pi ->
                    val op = players[pi]
                    val opTokens = op.tokens.map { ot ->
                        if (ot.pos in 0..Token.RING_MAX && (op.start + ot.pos) % 52 == land) {
                            captured++
                            Token(ot.idx, Token.BASE)
                        } else ot
                    }
                    if (captured > 0) players[pi] = op.copy(tokens = opTokens)
                }
            }
        }
        if (captured > 0) msg = "Captured! $msg"

        val finishedCount = players[s.current].finished
        val extra = state.dice == 6 || captured > 0 || (t.pos != Token.DONE && to == Token.DONE)
        state = s.copy(players = players).with(msg)

        if (finishedCount == 4) {
            state = state.copy(phase = LudoState.Phase.OVER, winner = s.current)
                .with("${p.color} wins the game!")
            changed()
            return true
        }

        nextPlayer(extra)
        changed()
        botRollNext()
        return true
    }

    /** Greedy bot move; returns -1 when no legal move exists. */
    fun botPick(): Int {
        val s = state
        val legal = s.legalMoves(s.dice)
        if (legal.isEmpty()) return -1
        val p = s.currentPlayer
        var best = legal.first()
        var bestScore = Double.NEGATIVE_INFINITY
        for (i in legal) {
            val t = p.tokens[i]
            val to = if (t.pos == Token.BASE) 0 else t.pos + s.dice
            var score = rnd.nextInt(0, 15).toDouble()                  // jitter
            when {
                to == Token.DONE -> score += 1000                      // finish
                to >= Token.HOME_FROM -> score += 800 + to             // safe-ish
                t.pos == Token.BASE -> score += 700                    // release
                else -> {
                    val land = (p.start + to) % 52
                    val captures = s.players.any { op ->
                        op.color != p.color && op.tokens.any {
                            it.pos in 0..Token.RING_MAX && (op.start + it.pos) % 52 == land
                        }
                    }
                    score += if (captures && land !in safeCells) 900.0 else to.toDouble()
                }
            }
            if (score > bestScore) { bestScore = score; best = i }
        }
        return best
    }

    // ---- flow helpers ----

    private fun nextPlayer(extra: Boolean) {
        if (extra) {
            state = state.copy(current = state.current, phase = LudoState.Phase.ROLL, streak = if (state.dice == 6) state.streak else 0)
        } else {
            state = state.copy(current = (state.current + 1) % 4, phase = LudoState.Phase.ROLL, streak = 0)
        }
    }

    private fun botRollNext() {
        if (state.phase != LudoState.Phase.OVER && state.currentPlayer.isBot) {
            state = state.with("Bot is thinking...")
        }
    }

    private fun LudoState.with(m: String): LudoState {
        val base = log.take(5)
        return copy(log = (listOf(m) + base).take(6))
    }

    private fun changed() = onChange(state)
}
