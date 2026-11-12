package com.sundram.weplay.platform.auth

import com.sundram.weplay.data.UserProfile

data class AuthSession(
    val userId: String,
    val accessToken: String? = null,
    val refreshToken: String? = null,
)

data class AuthCredentials(
    val provider: String,
    val username: String? = null,
    val password: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val otp: String? = null,
)

data class ProfileDraft(
    val name: String,
    val dobMillis: Long,
    val photoPath: String? = null,
    val provider: String,
    val phone: String? = null,
    val email: String? = null,
)

interface AuthRepository {
    suspend fun signIn(credentials: AuthCredentials): Result<AuthSession>
    suspend fun completeProfile(draft: ProfileDraft): Result<UserProfile>
    suspend fun currentProfile(): Result<UserProfile?>
    suspend fun signOut(): Result<Unit>
}
