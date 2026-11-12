package com.sundram.weplay.platform.auth

import com.sundram.weplay.data.ProfileStore
import com.sundram.weplay.data.UserProfile

class LocalAuthRepository(
    private val store: ProfileStore,
    private val sessions: SessionStore,
) : AuthRepository {
    override suspend fun signIn(credentials: AuthCredentials): Result<AuthSession> {
        val identifier = credentials.username?.trim()
            ?: credentials.phone?.trim()
            ?: credentials.email?.trim()

        if (identifier.isNullOrEmpty()) {
            return Result.failure(IllegalArgumentException("Enter a username, phone or email to sign in."))
        }

        // Demo sign-in: every signed-in identifier is accepted so the app can run
        // on a device until the auth server contract goes live. The password is
        // not checked or stored; only the resulting session is kept on the device.
        val userId = sessions.load()?.userId
            ?: store.load()?.id
            ?: "WP-" + Integer.toUnsignedString(identifier.hashCode())
        val session = AuthSession(
            userId = userId,
            accessToken = "demo-token-${userId}",
        )
        sessions.save(session, identifier, credentials.provider)
        return Result.success(session)
    }

    override suspend fun completeProfile(draft: ProfileDraft): Result<UserProfile> {
        val profile = UserProfile(
            name = draft.name,
            dobMillis = draft.dobMillis,
            id = store.load()?.id ?: "WP-" + (100000..999999).random(),
            photoPath = draft.photoPath,
            provider = draft.provider,
            phone = draft.phone,
            email = draft.email,
        )
        store.save(profile)
        return Result.success(profile)
    }

    override suspend fun currentProfile(): Result<UserProfile?> = Result.success(store.load())

    override suspend fun signOut(): Result<Unit> {
        store.clear()
        sessions.clear()
        return Result.success(Unit)
    }
}
