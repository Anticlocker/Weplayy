package com.sundram.weplay.data

import android.content.Context

data class CoinPack(
    val id: String,
    val coins: Long,
    val bonus: Long = 0L,
    val price: Int,
    val tag: String? = null,
) {
    val total: Long get() = coins + bonus
}

data class TopUpRecord(
    val packId: String,
    val coins: Long,
    val price: Int,
    val at: Long,
)

val DemoCoinPacks = listOf(
    CoinPack("coins_100", 100, 0, 99),
    CoinPack("coins_500", 500, 50, 449, "POPULAR"),
    CoinPack("coins_1200", 1_200, 150, 899, "BEST"),
    CoinPack("coins_3000", 3_000, 500, 1_999),
    CoinPack("coins_6000", 6_000, 1_200, 3_799),
    CoinPack("coins_12000", 12_000, 3_000, 7_499),
)

class TopUpStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("weplay_topup", Context.MODE_PRIVATE)

    fun history(userId: String): List<TopUpRecord> =
        prefs.getString(key(userId), null)
            ?.lineSequence()
            ?.mapNotNull(::decode)
            ?.toList()
            .orEmpty()

    fun add(userId: String, record: TopUpRecord) {
        val next = (listOf(record) + history(userId)).take(20)
        prefs.edit()
            .putString(key(userId), next.joinToString("\n", transform = ::encode))
            .apply()
    }

    private fun key(userId: String): String = "history_$userId"

    private fun encode(record: TopUpRecord): String =
        "${record.packId}|${record.coins}|${record.price}|${record.at}"

    private fun decode(line: String): TopUpRecord? {
        val parts = line.split("|")
        if (parts.size != 4) return null
        return TopUpRecord(
            packId = parts[0],
            coins = parts[1].toLongOrNull() ?: return null,
            price = parts[2].toIntOrNull() ?: return null,
            at = parts[3].toLongOrNull() ?: return null,
        )
    }
}
