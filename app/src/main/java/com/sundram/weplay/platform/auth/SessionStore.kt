package com.sundram.weplay.platform.auth

import android.content.Context

/**
 * Single source of truth for the signed-in session on this device.
 *
 * The session persists across process restarts so the auth layer can hand the
 * client shell a stable identity even before a backend is connected. Passwords
 * are never written here; only the accepted identifier and the issued token.
 */
class SessionStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(session: AuthSession, identifier: String, provider: String) {
        prefs.edit()
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_IDENTIFIER, identifier)
            .putString(KEY_PROVIDER, provider)
            .apply()
    }

    fun load(): AuthSession? {
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        return AuthSession(
            userId = userId,
            accessToken = prefs.getString(KEY_ACCESS_TOKEN, null),
        )
    }

    fun identifier(): String? = prefs.getString(KEY_IDENTIFIER, null)

    fun provider(): String? = prefs.getString(KEY_PROVIDER, null)

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_NAME = "weplay_auth"
        const val KEY_USER_ID = "user_id"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_IDENTIFIER = "identifier"
        const val KEY_PROVIDER = "provider"
    }
}
