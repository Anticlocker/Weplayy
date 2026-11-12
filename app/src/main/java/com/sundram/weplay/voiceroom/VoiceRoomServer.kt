package com.sundram.weplay.voiceroom

import com.sundram.weplay.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Where the app's self-hosted LiveKit infrastructure lives.
 *
 * These values come from `local.properties` (git-ignored) and are compiled into
 * [BuildConfig], so no server address or credential is hardcoded in source:
 *
 * ```
 * livekit.url=wss://call.example.com:7880
 * livekit.token.url=https://call.example.com:8081/voice/token
 * livekit.token.key=optional x-weplay-token-key value
 * ```
 *
 * The API key and secret never appear here. A LiveKit access token can only be
 * minted with the secret, so that stays on the server behind [tokenUrl].
 */
object VoiceRoomServer {

    /** SFU signalling url, or blank when no server has been configured. */
    val url: String = BuildConfig.LIVEKIT_URL.trim()

    /** Endpoint that mints short-lived join tokens, or blank when unconfigured. */
    val tokenUrl: String = BuildConfig.LIVEKIT_TOKEN_URL.trim()

    /** Optional shared header for the token endpoint; blank when not required. */
    val tokenKey: String = BuildConfig.LIVEKIT_TOKEN_KEY.trim()

    /** True when a real voice server can be reached; false means offline preview. */
    val isConfigured: Boolean = url.isNotEmpty() && tokenUrl.isNotEmpty()

    internal fun tokenEndpoint(roomId: String, identity: String, displayName: String): String {
        fun enc(value: String) = URLEncoder.encode(value, "UTF-8")
        val separator = if (tokenUrl.contains('?')) '&' else '?'
        return "$tokenUrl$separator" +
            "room=${enc(roomId)}" +
            "&identity=${enc(identity)}" +
            "&name=${enc(displayName)}"
    }
}

/** Result of asking the token endpoint for a join token. */
sealed interface TokenResult {
    data class Success(val token: String) : TokenResult
    data class Failure(val reason: String) : TokenResult
}

/**
 * Fetches a short-lived LiveKit access token from our own server.
 *
 * Uses [HttpURLConnection] deliberately: the app has no HTTP client dependency and
 * this is the only network call it makes besides the WebRTC connection itself.
 */
class VoiceTokenService {

    suspend fun fetch(roomId: String, identity: String, displayName: String): TokenResult =
        withContext(Dispatchers.IO) {
            if (!VoiceRoomServer.isConfigured) {
                return@withContext TokenResult.Failure("No voice server configured")
            }
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(VoiceRoomServer.tokenEndpoint(roomId, identity, displayName))
                    .openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    instanceFollowRedirects = true
                    if (VoiceRoomServer.tokenKey.isNotBlank()) {
                        setRequestProperty("x-weplay-token-key", VoiceRoomServer.tokenKey)
                    }
                }
                val status = connection.responseCode
                val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
                    ?.use { it.readBytes().toString(Charsets.UTF_8) }
                    .orEmpty()
                if (status !in 200..299) {
                    return@withContext TokenResult.Failure("Token server returned HTTP $status")
                }
                val token = parseToken(body)
                if (token.isNullOrBlank()) {
                    TokenResult.Failure("Token server returned an empty token")
                } else {
                    TokenResult.Success(token)
                }
            } catch (error: Exception) {
                TokenResult.Failure(error.message ?: "Token request failed")
            } finally {
                connection?.disconnect()
            }
        }

    /**
     * Accepts either a JSON body (`{"token": "..."}`) or a raw JWT body.
     * Every field is treated as optional because the response comes from an
     * external service that may answer with an error shape we did not predict.
     */
    private fun parseToken(body: String): String? {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return null
        if (!trimmed.startsWith("{")) return trimmed
        return runCatching {
            JSONObject(trimmed).optString("token").ifBlank { null }
        }.getOrNull()
    }

    private companion object {
        const val TIMEOUT_MS = 10_000
    }
}
