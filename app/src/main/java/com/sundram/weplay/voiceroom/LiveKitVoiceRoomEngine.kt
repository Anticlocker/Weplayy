package com.sundram.weplay.voiceroom

import android.content.Context
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.participant.LocalParticipant
import io.livekit.android.room.participant.Participant
import io.livekit.android.room.track.DataPublishReliability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Real voice room on top of a self-hosted LiveKit SFU.
 *
 * Audio and the chat channel travel over one WebRTC connection to our own server.
 * The client never sees the LiveKit API key or secret: it asks [VoiceTokenService]
 * for a short-lived token that the server minted and hands that to [Room.connect].
 *
 * The visible room is rebuilt from the LiveKit object graph after every event
 * instead of being patched event by event, so seats, mic and speaking state stay
 * consistent with the server even if individual events are coalesced.
 */
class LiveKitVoiceRoomEngine(
    private val appContext: Context,
    private val scope: CoroutineScope,
    private val tokenService: VoiceTokenService = VoiceTokenService(),
) : VoiceRoomEngine {

    private val _state = MutableStateFlow(VoiceRoomState(transportLabel = "LiveKit · self-hosted"))
    override val state: StateFlow<VoiceRoomState> = _state

    private var room: Room? = null
    private var eventsJob: Job? = null
    private var selfIdentity = ""
    private var seat: Int? = null
    private var micOn = false
    private var messageSeq = 0L
    private val chat = ArrayDeque<RoomMessage>()

    override suspend fun start(roomId: String, identity: String, displayName: String) {
        selfIdentity = identity
        _state.update {
            it.copy(roomId = roomId, connection = RoomConnection.CONNECTING, error = null)
        }
        appendMessage(systemMessage("Connecting to the voice server…"))

        val token = when (val result = tokenService.fetch(roomId, identity, displayName)) {
            is TokenResult.Failure -> {
                fail(result.reason)
                return
            }

            is TokenResult.Success -> result.token
        }

        val newRoom = LiveKit.create(appContext)
        room = newRoom
        eventsJob = scope.launch { collectEvents(newRoom) }
        try {
            newRoom.connect(VoiceRoomServer.url, token)
            newRoom.localParticipant.updateAttributes(seatAttributes())
        } catch (error: Exception) {
            fail(error.message ?: "Could not reach the voice server")
            releaseRoom()
            return
        }
        publishSnapshot()
    }

    override suspend fun stop() {
        releaseRoom()
        _state.update { it.copy(connection = RoomConnection.DISCONNECTED) }
    }

    override suspend fun setMicrophone(enabled: Boolean) {
        val active = room ?: return
        val result = runCatching { active.localParticipant.setMicrophoneEnabled(enabled) }
        result.exceptionOrNull()?.let { error ->
            _state.update {
                it.copy(
                    selfMicOn = micOn,
                    error = "Microphone unavailable: ${error.message ?: "grant the microphone permission first"}",
                )
            }
            publishSnapshot()
            return
        }
        if (!result.getOrDefault(false)) {
            val action = if (enabled) "on" else "off"
            _state.update { it.copy(error = "The microphone could not be turned $action.") }
        } else {
            micOn = enabled
            _state.update { it.copy(selfMicOn = enabled, error = null) }
        }
        publishSnapshot()
    }

    override suspend fun sendChat(text: String) {
        val body = text.trim()
        if (body.isEmpty()) return
        val active = room ?: return
        // LiveKit has no chat primitive, so the message is a reliable data packet
        // on its own topic. Payload is the raw UTF-8 text; the author is taken from
        // the sender participant, which the server authenticated and cannot be forged.
        val result = active.localParticipant.publishData(
            body.toByteArray(Charsets.UTF_8),
            DataPublishReliability.RELIABLE,
            VOICE_CHAT_TOPIC,
        )
        if (result.isSuccess) {
            appendMessage(RoomMessage(nextId(), displayNameOf(active.localParticipant), body, isSelf = true))
        } else {
            appendMessage(systemMessage("Message not delivered. Try again in a moment."))
        }
    }

    override suspend fun takeSeat(seat: Int) {
        if (seat !in 0 until VOICE_ROOM_SEAT_COUNT) return
        if (state.value.members.any { it.seat == seat }) return
        this.seat = seat
        room?.localParticipant?.updateAttributes(seatAttributes())
        publishSnapshot()
    }

    override suspend fun leaveSeat() {
        seat = null
        room?.localParticipant?.updateAttributes(seatAttributes())
        publishSnapshot()
    }

    private suspend fun collectEvents(active: Room) {
        active.events.collect { event ->
            when (event) {
                is RoomEvent.Connected,
                is RoomEvent.Reconnected,
                -> _state.update { it.copy(connection = RoomConnection.CONNECTED, error = null) }

                is RoomEvent.Reconnecting ->
                    _state.update { it.copy(connection = RoomConnection.RECONNECTING) }

                is RoomEvent.FailedToConnect -> {
                    fail(event.error.message ?: "The voice server rejected the connection")
                    releaseRoom()
                }

                is RoomEvent.Disconnected -> {
                    val reason = event.error?.message ?: "Left the room"
                    _state.update {
                        it.copy(connection = RoomConnection.DISCONNECTED, error = reason)
                    }
                    appendMessage(systemMessage("Disconnected: $reason"))
                }

                is RoomEvent.DataReceived -> onDataReceived(event)

                else -> Unit
            }
            publishSnapshot()
        }
    }

    private fun onDataReceived(event: RoomEvent.DataReceived) {
        if (event.topic != VOICE_CHAT_TOPIC) return
        val sender = event.participant ?: return
        val body = String(event.data, Charsets.UTF_8).trim()
        if (body.isEmpty()) return
        appendMessage(
            RoomMessage(
                id = nextId(),
                author = displayNameOf(sender),
                body = body,
                isSelf = identityOf(sender) == selfIdentity,
            ),
        )
    }

    private fun fail(reason: String) {
        _state.update { it.copy(connection = RoomConnection.FAILED, error = reason) }
        appendMessage(systemMessage(reason))
    }

    /** Rebuilds the visible room from whatever LiveKit currently knows. */
    private fun publishSnapshot() {
        val active = room
        val participants: List<Participant> = buildList {
            if (active != null) {
                add(active.localParticipant)
                addAll(active.remoteParticipants.values)
            }
        }
        val members = participants.map { participant ->
            val isSelf = participant is LocalParticipant
            val micEnabled = if (isSelf) micOn else participant.isMicrophoneEnabled
            RoomMemberVo(
                identity = identityOf(participant),
                name = displayNameOf(participant),
                seat = participant.attributes[ATTR_SEAT]?.toIntOrNull()
                    ?.takeIf { it in 0 until VOICE_ROOM_SEAT_COUNT },
                micOn = micEnabled,
                speaking = participant.isSpeaking && micEnabled,
                isSelf = isSelf,
                joinedAtMs = participant.joinedAt ?: System.currentTimeMillis(),
            )
        }
        _state.update { previous ->
            previous.copy(
                members = members,
                selfMicOn = micOn,
                connection = when (active?.state) {
                    Room.State.CONNECTING -> RoomConnection.CONNECTING
                    Room.State.CONNECTED -> RoomConnection.CONNECTED
                    Room.State.RECONNECTING -> RoomConnection.RECONNECTING
                    Room.State.DISCONNECTED -> previous.connection
                    null -> previous.connection
                },
            )
        }
    }

    // The SFU deletes an attribute only when its value is an empty string.
    private fun seatAttributes(): Map<String, String> =
        mapOf(ATTR_SEAT to (seat?.toString() ?: ""))

    private fun identityOf(participant: Participant): String =
        participant.identity?.value ?: participant.sid?.value ?: "guest"

    private fun displayNameOf(participant: Participant): String =
        participant.name?.takeIf { it.isNotBlank() }
            ?: identityOf(participant).takeIf { it.isNotBlank() }
            ?: "Guest"

    private fun systemMessage(body: String) =
        RoomMessage(nextId(), "System", body, isSelf = false, system = true)

    private fun appendMessage(message: RoomMessage) {
        chat.addLast(message)
        while (chat.size > MAX_CHAT_HISTORY) chat.removeFirst()
        _state.update { it.copy(messages = chat.toList()) }
    }

    private fun nextId(): Long = ++messageSeq

    private fun releaseRoom() {
        eventsJob?.cancel()
        eventsJob = null
        val active = room ?: return
        room = null
        micOn = false
        runCatching { active.disconnect() }
        runCatching { active.release() }
    }

    private companion object {
        const val MAX_CHAT_HISTORY = 200
    }
}
