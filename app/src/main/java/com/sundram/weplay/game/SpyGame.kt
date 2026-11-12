package com.sundram.weplay.game

import kotlin.random.Random

/**
 * Engine for pass-and-play rounds of "Who's the Spy".
 */

data class SpyCategory(
    val id: String,
    val name: String,
    val words: List<String>,
)

data class SpyPlayer(val name: String, val isSpy: Boolean = false, val votedOut: Boolean = false)

enum class SpyPhase { SETUP, REVEAL, DISCUSS, VOTE, RESULT }

data class SpyState(
    val phase: SpyPhase = SpyPhase.SETUP,
    val category: SpyCategory? = null,
    val players: List<SpyPlayer> = emptyList(),
    val word: String = "",
    val spyIndex: Int = -1,
    val revealIndex: Int = 0,          // whose turn it is during REVEAL
    val discussSeconds: Int = 240,
    val votes: Map<Int, Int> = mutableMapOf(),  // voter -> target
    val round: Int = 1,
    val log: List<String> = listOf(),
) {
    val alive get() = players.filterIndexed { i, _ -> !players[i].votedOut }
}

object SpyData {
    val categories = listOf(
        SpyCategory("bolly", "Bollywood", listOf(
            "Sholay", "Dilwale Dulhania Le Jayenge", "3 Idiots", "Gully Boy",
            "Andaaz Apna Apna", "Kuch Kuch Hota Hai", "Munna Bhai MBBS", "PK",
            "Dangal", "Zindagi Na Milegi Dobara"
        )),
        SpyCategory("food", "Food", listOf(
            "Biryani", "Masala Dosa", "Chole Bhature", "Pav Bhaji",
            "Pani Puri", "Butter Chicken", "Vada Pav", "Samosa",
            "Rogan Josh", "Tandoori Chicken"
        )),
        SpyCategory("place", "Places", listOf(
            "Goa", "Manali", "Kerala", "Ladakh", "Varanasi",
            "Jaipur", "Andaman Islands", "Darjeeling", "Rishikesh", "Udaipur"
        )),
        SpyCategory("tech", "Tech", listOf(
            "Android Studio", "WhatsApp", "Bluetooth", "YouTube",
            "Instagram", "ChatGPT", "Spotify", "Paytm",
            "Ola", "Google Maps"
        )),
        SpyCategory("games", "Party Games", listOf(
            "Ludo", "Who's the Spy", "Draw & Guess", "Werewolf",
            "Truth or Dare", "Antakshari", "Charades", "Pictionary",
            "Bingo", "Musical Chairs"
        )),
    )
}

class SpyGame(seed: Long = System.currentTimeMillis()) {
    private val rnd = Random(seed)
    var state: SpyState = SpyState()
        private set
    var onChange: (SpyState) -> Unit = {}

    fun reset() { state = SpyState(); changed() }

    /** setup: player names + category choose */
    fun start(names: List<String>, categoryId: String) {
        val cat = SpyData.categories.first { it.id == categoryId }
        val spyIdx = rnd.nextInt(names.size)
        val word = cat.words.random(rnd)
        state = SpyState(
            phase = SpyPhase.REVEAL,
            category = cat,
            players = names.mapIndexed { i, n -> SpyPlayer(n, isSpy = i == spyIdx) },
            word = word,
            spyIndex = spyIdx,
            revealIndex = 0,
            discussSeconds = 240,
            round = 1,
        )
        changed()
    }

    /** Advance to the next player during REVEAL; after the last one, move to DISCUSS. */
    fun nextReveal() {
        if (state.phase != SpyPhase.REVEAL) return
        val next = state.revealIndex + 1
        state = if (next >= state.players.size) {
            state.copy(phase = SpyPhase.DISCUSS).with("Discussion start — ${state.discussSeconds / 60} min")
        } else {
            state.copy(revealIndex = next)
        }
        changed()
    }

    fun setDiscussSeconds(s: Int) {
        if (state.phase != SpyPhase.DISCUSS) return
        state = state.copy(discussSeconds = s)
        changed()
    }

    /** timer tick, driven by an external LaunchedEffect */
    fun tick() {
        if (state.phase != SpyPhase.DISCUSS) return
        val s = state.discussSeconds
        if (s <= 1) {
            state = state.copy(phase = SpyPhase.VOTE).with("Time up — vote now!")
        } else {
            state = state.copy(discussSeconds = s - 1)
        }
        changed()
    }

    fun skipToVote() {
        if (state.phase == SpyPhase.DISCUSS) {
            state = state.copy(phase = SpyPhase.VOTE).with("Skipped to vote")
            changed()
        }
    }

    /** voter index to target index (both list indices of human players) */
    fun castVote(voter: Int, target: Int) {
        if (state.phase != SpyPhase.VOTE) return
        if (state.players[voter].votedOut) return
        if (state.players[target].votedOut) return
        state = state.copy(votes = state.votes + (voter to target))
        // all living players have voted → resolve the result
        val aliveIdx = state.players.indices.filter { !state.players[it].votedOut }
        if (aliveIdx.all { state.votes.containsKey(it) }) resolve()
        changed()
    }

    private fun resolve() {
        val aliveIdx = state.players.indices.filter { !state.players[it].votedOut }
        val tally = aliveIdx.mapNotNull { state.votes[it] }.groupingBy { it }.eachCount()
        val maxVotes = tally.values.maxOrNull() ?: 0
        val targets = tally.filterValues { it == maxVotes }.keys.toList()
        when {
            targets.size > 1 -> {
                state = state.copy(phase = SpyPhase.RESULT)
                    .with("Tie! Spy escapes this time (${state.players[state.spyIndex].name} was SPY · word: ${state.word})")
            }
            targets.first() == state.spyIndex -> {
                val players = state.players.mapIndexed { i, p -> if (i == targets.first()) p.copy(votedOut = true) else p }
                state = state.copy(players = players, phase = SpyPhase.RESULT)
                    .with("Crew wins! Spy caught — ${players[targets.first()].name}")
            }
            else -> {
                val players = state.players.mapIndexed { i, p -> if (i == targets.first()) p.copy(votedOut = true) else p }
                val spyAlive = !players[state.spyIndex].votedOut
                val aliveCount = players.count { !it.votedOut }
                state = if (spyAlive && aliveCount <= 2) {
                    state.copy(players = players, phase = SpyPhase.RESULT)
                        .with("Spy wins! (" + state.players[state.spyIndex].name + " was SPY)")
                } else {
                    state.copy(
                        players = players, phase = SpyPhase.DISCUSS,
                        votes = emptyMap(), discussSeconds = 120,
                    ).with("${players[targets.first()].name} was not the spy — new round!")
                }
            }
        }
    }

    private fun SpyState.with(m: String): SpyState = copy(log = (listOf(m) + log).take(6))
    private fun changed() = onChange(state)
}
