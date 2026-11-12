package com.sundram.weplay.platform.profile

import com.sundram.weplay.data.UserProfile

data class UserProfileUpdate(
    val name: String? = null,
    val bio: String? = null,
    val gender: String? = null,
    val photoPath: String? = null,
)

interface ProfileRepository {
    suspend fun getMe(): Result<UserProfile?>
    suspend fun updateMe(update: UserProfileUpdate): Result<UserProfile>
}
