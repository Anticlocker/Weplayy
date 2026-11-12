package com.sundram.weplay.platform.content

import com.sundram.weplay.data.GameCard
import com.sundram.weplay.data.RoomCard

data class FeedPost(
    val who: String,
    val text: String,
    val likes: Int,
    val comments: Int,
)

data class FeedEvent(
    val who: String,
    val action: String,
    val time: String,
)

data class HomeFeed(
    val games: List<GameCard>,
    val rooms: List<RoomCard>,
)

data class DiscoverFeed(
    val trending: List<String>,
    val events: List<FeedEvent>,
    val posts: List<FeedPost>,
)

interface ContentRepository {
    suspend fun homeFeed(): Result<HomeFeed>
    suspend fun discoverFeed(): Result<DiscoverFeed>
}
