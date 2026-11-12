package com.sundram.weplay.data

import com.sundram.weplay.R

data class GiftItem(
    val id: String,
    val name: String,
    val desc: String,
    val icon: Int,
    val tint: Long,
    val cost: Long = 0L,   // coin cost for DM gifts
)

data class BackpackItem(
    val id: String,
    val name: String,
    val kind: String,
    val desc: String,
    val level: Int,
    val frame: Long? = null,
)

val DemoGifts = listOf(
    GiftItem("rose", "Rose", "A classic rose for the stage.", R.drawable.ic_heart, 0xFFFF738F, cost = 10L),
    GiftItem("star", "Lucky Star", "A little luck for the next round.", R.drawable.ic_star, 0xFF00CCF9, cost = 25L),
    GiftItem("box", "Gift Box", "A small surprise for a friend.", R.drawable.ic_gift, 0xFF50DCA2, cost = 50L),
    GiftItem("bolt", "Power Bolt", "Boost the energy in the room.", R.drawable.ic_bolt, 0xFF9B8CFF, cost = 100L),
    GiftItem("coins", "Coin Bag", "Share a handful of coins.", R.drawable.ic_coin, 0xFFFF6512, cost = 200L),
    GiftItem("crown", "Golden Crown", "For the room royalty.", R.drawable.ic_crown, 0xFFF7E25A, cost = 500L),
)

fun defaultGiftCount(id: String): Int = when (id) {
    "rose" -> 3
    "star" -> 1
    "box" -> 2
    "coins" -> 4
    else -> 0
}

val DemoBackpack = listOf(
    BackpackItem(
        "frame_cyan", "Cyan Halo", "Avatar frame", "A clean cyan ring around your avatar.",
        level = 1, frame = 0xFF00CCF9,
    ),
    BackpackItem(
        "bubble_blue", "Blue Bubble", "Chat bubble", "A deep blue bubble in room chat.",
        level = 1,
    ),
    BackpackItem(
        "frame_rose", "Rose Halo", "Avatar frame", "A warm rose ring around your avatar.",
        level = 2, frame = 0xFFFF738F,
    ),
    BackpackItem(
        "badge_founder", "Founder Badge", "Badge", "Shown next to your name in rooms.",
        level = 3,
    ),
    BackpackItem(
        "frame_gold", "Gold Halo", "Avatar frame", "A bright gold ring around your avatar.",
        level = 4, frame = 0xFFF7E25A,
    ),
    BackpackItem(
        "entry_stars", "Star Entry", "Entry effect", "A short star trail when you enter a room.",
        level = 5,
    ),
)
