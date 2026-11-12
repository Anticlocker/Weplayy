package com.sundram.weplay.platform.profile

import com.sundram.weplay.data.ProfileStore
import com.sundram.weplay.data.UserProfile

class LocalProfileRepository(private val store: ProfileStore) : ProfileRepository {
    override suspend fun getMe(): Result<UserProfile?> = Result.success(store.load())

    override suspend fun updateMe(update: UserProfileUpdate): Result<UserProfile> {
        val current = store.load() ?: return Result.failure(IllegalStateException("No local profile"))
        val next = current.copy(
            name = update.name ?: current.name,
            bio = update.bio ?: current.bio,
            gender = update.gender ?: current.gender,
            photoPath = update.photoPath ?: current.photoPath,
        )
        store.save(next)
        return Result.success(next)
    }
}
