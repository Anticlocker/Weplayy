package com.sundram.weplay.platform.content

import com.sundram.weplay.data.Demo

class LocalContentRepository : ContentRepository {
    override suspend fun homeFeed(): Result<HomeFeed> =
        Result.success(HomeFeed(Demo.games, Demo.rooms))

    override suspend fun discoverFeed(): Result<DiscoverFeed> = Result.success(
        DiscoverFeed(
            trending = listOf("Ludo", "Who's the Spy", "KTV", "Werewolf", "Draw & Guess"),
            events = listOf(
                FeedEvent("Ankit", "beat Sundram at Ludo", "2m"),
                FeedEvent("Priya", "found the spy in round one — 3 votes", "9m"),
                FeedEvent("Desi Kings", "moved to family rank 12 today", "1h"),
                FeedEvent("Raj", "turned 5 coins into 500 coins", "2h"),
                FeedEvent("Sam", "opened a new room: Late Night Vibes", "3h"),
            ),
            posts = listOf(
                FeedPost("Sneha", "anyone playing Ludo? building a squad", 24, 8),
                FeedPost("Ankit", "I was the spy four times in a row. Luck is zero.", 11, 5),
                FeedPost("Vikas", "when is Draw & Guess coming?", 30, 14),
            ),
        ),
    )
}
