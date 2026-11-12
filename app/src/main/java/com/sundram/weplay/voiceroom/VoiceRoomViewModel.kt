package com.sundram.weplay.voiceroom

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Owns one voice-room session.
 *
 * Chooses the transport from configuration: a real [LiveKitVoiceRoomEngine] when
 * [VoiceRoomServer] has a url and token endpoint, otherwise [DemoVoiceRoomEngine]
 * so the screen still works on a machine with no server.
 */
class VoiceRoomViewModel(
    appContext: Context,
    private val roomId: String,
    private val identity: String,
    private val displayName: String,
) : ViewModel() {

    private val context = appContext.applicationContext

    private val engine: VoiceRoomEngine = if (VoiceRoomServer.isConfigured) {
        LiveKitVoiceRoomEngine(context, viewModelScope)
    } else {
        DemoVoiceRoomEngine(viewModelScope)
    }

    val state: StateFlow<VoiceRoomState> = engine.state

    /** True once the user has granted RECORD_AUDIO; drives the first-run prompt. */
    val microphoneGranted: Boolean
        get() = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    /** LiveKit only unblocks mic publishing after the runtime permission exists. */
    val usesLiveServer: Boolean get() = VoiceRoomServer.isConfigured

    init {
        viewModelScope.launch {
            engine.start(roomId, identity, displayName)
        }
    }

    /**
     * Turns the microphone on, asking for the permission first when needed.
     *
     * @param requestPermission invoked when RECORD_AUDIO has not been granted yet;
     * the screen forwards the result through [onMicrophonePermissionResult].
     */
    fun toggleMicrophone(requestPermission: () -> Unit) {
        val target = !state.value.selfMicOn
        if (target && usesLiveServer && !microphoneGranted) {
            requestPermission()
            return
        }
        viewModelScope.launch { engine.setMicrophone(target) }
    }

    fun onMicrophonePermissionResult(granted: Boolean) {
        if (!granted) return
        viewModelScope.launch { engine.setMicrophone(true) }
    }

    fun sendMessage(text: String) {
        val body = text.trim()
        if (body.isEmpty()) return
        viewModelScope.launch { engine.sendChat(body) }
    }

    fun takeSeat(seat: Int) {
        viewModelScope.launch { engine.takeSeat(seat) }
    }

    fun leaveSeat() {
        viewModelScope.launch { engine.leaveSeat() }
    }

    /** Retries a failed connection without leaving the screen. */
    fun reconnect() {
        viewModelScope.launch {
            engine.stop()
            engine.start(roomId, identity, displayName)
        }
    }

    override fun onCleared() {
        // viewModelScope is cancelled here, so release on a detached scope.
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            engine.stop()
        }
    }

    companion object {
        fun factory(
            context: Context,
            roomId: String,
            identity: String,
            displayName: String,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: androidx.lifecycle.viewmodel.CreationExtras,
            ): T = VoiceRoomViewModel(
                appContext = context.applicationContext,
                roomId = roomId,
                identity = identity,
                displayName = displayName,
            ) as T
        }
    }
}
