package com.sundram.weplay.voiceroom

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Offline stand-in for [LiveKitVoiceRoomEngine].
 *
 * Used when `livekit.url` / `livekit.token.url` are not present in
 * `local.properties`, so the room screen — seats, speaking ring, mic toggle, chat
 * feed — can be built and reviewed before the VPS is ready. It never opens a
 * socket; everything it shows is generated locally and clearly labelled as a
 * simulation in the header.
 */
class DemoVoiceRoomEngine(
    private val scope: CoroutineScope,
) : VoiceRoomEngine {

    private val _state = MutableStateFlow(
        VoiceRoomState(
            transportLabel = "Offline preview · no server configured",
            connection = RoomConnection.SIMULATED,
        ),
    )
    override val state: StateFlow<VoiceRoomState> = _state

    private val members = mutableListOf<RoomMemberVo>()
    private val chat = ArrayDeque<RoomMessage>()
    private var messageSeq = 0L
    private var scriptIndex = 0
    private var tickJob: Job? = null

    override suspend fun start(roomId: String, identity: String, displayName: String) {
        members.clear()
        chat.clear()
        scriptIndex = 0
        members += GHOSTS.map { it.copy() }
        members += RoomMemberVo(
            identity = identity.ifBlank { "self" },
            name = displayName.ifBlank { "You" },
            seat = null,
            micOn = false,
            speaking = false,
            isSelf = true,
            joinedAtMs = System.currentTimeMillis(),
        )
        appendMessage(systemMessage("Offline preview: seats and chat are simulated. Add livekit.url to local.properties to join for real."))
        appendMessage(scriptMessage(GHOSTS[0].name, "welcome in! grab a seat if you want to talk"))
        _state.update {
            it.copy(
                roomId = roomId,
                connection = RoomConnection.SIMULATED,
                members = members.toList(),
                error = null,
            )
        }
        tickJob?.cancel()
        tickJob = scope.launch { runScript() }
    }

    override suspend fun stop() {
        tickJob?.cancel()
        tickJob = null
        _state.update { it.copy(connection = RoomConnection.DISCONNECTED) }
    }

    override suspend fun setMicrophone(enabled: Boolean) {
        members.replaceAll { if (it.isSelf) it.copy(micOn = enabled, speaking = false) else it }
        publish()
    }

    override suspend fun sendChat(text: String) {
        val body = text.trim()
        if (body.isEmpty()) return
        val self = members.firstOrNull { it.isSelf } ?: return
        chat.addLast(RoomMessage(nextId(), self.name, body, isSelf = true))
        trimChat()
        val reply = REPLIES.getOrElse(scriptIndex) { REPLIES.last() }
        scriptIndex++
        val responder = members.filter { !it.isSelf }.randomOrNull()
        publish()
        scope.launch {
            delay(1200)
            if (responder != null) chat.addLast(RoomMessage(nextId(), responder.name, reply, isSelf = false))
            trimChat()
            publish()
        }
    }

    override suspend fun takeSeat(seat: Int) {
        if (seat !in 0 until VOICE_ROOM_SEAT_COUNT) return
        val self = members.firstOrNull { it.isSelf } ?: return
        val takenByOther = members.any { !it.isSelf && it.seat == seat }
        if (takenByOther) {
            chat.addLast(systemMessage("Seat ${seat + 1} is already taken."))
            publish()
            return
        }
        members.replaceAll { if (it.isSelf) it.copy(seat = seat) else it }
        publish()
    }

    override suspend fun leaveSeat() {
        members.replaceAll { if (it.isSelf) it.copy(seat = null, speaking = false) else it }
        publish()
    }

    /** Cycles speaking highlights so the ring animation is visible without a server. */
    private suspend fun CoroutineScope.runScript() {
        var step = 0
        while (isActive) {
            delay(2600)
            step++
            val talkers = members.filter { !it.isSelf && it.hasSeat && it.micOn }
            if (talkers.isNotEmpty()) {
                val active = talkers[step % talkers.size]
                members.replaceAll {
                    if (it.isSelf) {
                        it.copy(speaking = it.micOn && step % 7 == 0)
                    } else {
                        it.copy(speaking = it.identity == active.identity)
                    }
                }
            }
            SCRIPT_LINES.getOrNull(scriptIndex)?.let { (who, line) ->
                val author = members.firstOrNull { !it.isSelf && it.name == who }?.name ?: who
                chat.addLast(RoomMessage(nextId(), author, line, isSelf = false))
                trimChat()
                scriptIndex++
            }
            publish()
        }
    }

    private fun publish() {
        val selfMicOn = members.firstOrNull { it.isSelf }?.micOn == true
        _state.update {
            it.copy(
                members = members.toList(),
                messages = chat.toList(),
                selfMicOn = selfMicOn,
            )
        }
    }

    private fun trimChat() {
        while (chat.size > MAX_CHAT_HISTORY) chat.removeFirst()
    }

    private fun systemMessage(body: String) =
        RoomMessage(nextId(), "System", body, isSelf = false, system = true)

    private fun scriptMessage(author: String, body: String) =
        RoomMessage(nextId(), author, body, isSelf = false)

    private fun appendMessage(message: RoomMessage) {
        chat.addLast(message)
        trimChat()
        publish()
    }

    private fun nextId(): Long = ++messageSeq

    private companion object {
        const val MAX_CHAT_HISTORY = 200

        val GHOSTS = listOf(
            RoomMemberVo("ghost-host", "Neha", seat = 0, micOn = true, speaking = true, isSelf = false, joinedAtMs = 0L),
            RoomMemberVo("ghost-arjun", "Arjun", seat = 1, micOn = true, speaking = false, isSelf = false, joinedAtMs = 0L),
            RoomMemberVo("ghost-tara", "Tara", seat = 2, micOn = false, speaking = false, isSelf = false, joinedAtMs = 0L),
            RoomMemberVo("ghost-vikram", "Vikram", seat = 4, micOn = true, speaking = false, isSelf = false, joinedAtMs = 0L),
        )

        val SCRIPT_LINES = listOf(
            "Arjun" to "next round is mic grab, ready when you are",
            "Neha" to "who wants to host the drawing round?",
            "Vikram" to "I'm in, put me down for spy",
            "Tara" to "muted for now, my mic is acting up",
        )

        val REPLIES = listOf(
            "nice, see you on stage",
            "one sec, grabbing water",
            "let's start after the next join",
        )
    }
}
