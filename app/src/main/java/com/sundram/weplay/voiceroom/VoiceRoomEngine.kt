package com.sundram.weplay.voiceroom

import kotlinx.coroutines.flow.StateFlow

/**
 * Transport abstraction for a voice room.
 *
 * Two implementations exist:
 *  - [LiveKitVoiceRoomEngine]: real audio + chat against a self-hosted LiveKit SFU.
 *  - [DemoVoiceRoomEngine]: offline simulation used until a server is configured,
 *    so the room screen is exerciseable without any network.
 *
 * Implementations are owned by [VoiceRoomViewModel] and must be stopped with
 * [stop] before the view model is cleared.
 */
interface VoiceRoomEngine {

    val state: StateFlow<VoiceRoomState>

    /**
     * Connect and keep the room snapshot in sync.
     *
     * @param roomId LiveKit room name, e.g. "r1".
     * @param identity stable per-user id, used as the LiveKit participant identity.
     * @param displayName name other participants see.
     */
    suspend fun start(roomId: String, identity: String, displayName: String)

    /** Leave the room and release every resource held by the engine. */
    suspend fun stop()

    /** Publish (or stop publishing) local microphone audio. */
    suspend fun setMicrophone(enabled: Boolean)

    /** Send a text chat line to everyone in the room. */
    suspend fun sendChat(text: String)

    /** Take a seat on the stage, moving out of the listener area. */
    suspend fun takeSeat(seat: Int)

    /** Give up the current seat and become a listener. */
    suspend fun leaveSeat()
}
