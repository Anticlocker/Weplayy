package com.sundram.weplay.data

data class GameCard(
    val id: String,
    val title: String,
    val desc: String,
    val tag: String? = null,
    val playable: Boolean = false,
)

data class RoomCard(
    val id: String,
    val title: String,
    val host: String,
    val members: Int,
    val tag: String,
    val live: Boolean = true,
)

data class ChatItem(
    val id: String,
    val name: String,
    val lastMsg: String,
    val time: String,
    val unread: Int = 0,
)

object Demo {
    val games = listOf(
        GameCard("ludo", "Ludo", "Board · vs bots", "HOT", playable = true),
        GameCard("spy", "Who's the Spy", "Deduction · pass & play", "NEW", playable = true),
        GameCard("draw", "Draw & Guess", "Party · drawing"),
        GameCard("bingo", "Bingo", "Luck · numbers"),
        GameCard("sing", "Mic Grab", "Voice · music"),
        GameCard("hum", "Guess the Hum", "Music · guess"),
        GameCard("squid", "Squid Run", "Action · survival"),
        GameCard("sheep", "Sheep Merge", "Puzzle · merge"),
    )

    val rooms = listOf(
        RoomCard("r1", "Tea Time Talks", "Priya", 7, "Casual"),
        RoomCard("r2", "Werewolf Night", "Raj", 10, "Deduction"),
        RoomCard("r3", "Music & Chill", "Ankit", 4, "KTV"),
        RoomCard("r4", "Desi Gamers", "Sam", 6, "Games"),
        RoomCard("r5", "Late Night Vibes", "Sneha", 2, "Casual"),
        RoomCard("r6", "Sing-Along", "Kavya", 9, "KTV"),
    )

    val chats = listOf(
        ChatItem("c1", "Ankit", "match tomorrow?", "2m", 2),
        ChatItem("c2", "Tea Time Talks", "Priya: listen to this", "12m", 1),
        ChatItem("c3", "Sneha", "you there?", "1h"),
        ChatItem("c4", "Werewolf Night", "game starts soon", "3h"),
        ChatItem("c5", "Family: Desi Kings", "complete the task!", "5h", 3),
    )
}
