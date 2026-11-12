package com.sundram.weplay.data

import android.content.Context

data class TopUpEntry(
    val packId: String,
    val coins: Long,
    val price: Int,
    val at: Long,
)

data class SavedCard(
    val brand: String,
    val last4: String,
    val expiry: String,
)

class WalletStore(context: Context) {

    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("weplay_wallet", Context.MODE_PRIVATE)
    private val topUpPrefs = app.getSharedPreferences("weplay_topup", Context.MODE_PRIVATE)

    /** Top-up history written by the top-up screen. */
    fun topUps(ownerId: String): List<TopUpEntry> =
        topUpPrefs.getString("history_$ownerId", null)
            ?.lineSequence()
            ?.mapNotNull(::decodeTopUp)
            ?.toList()
            .orEmpty()

    fun cards(ownerId: String): List<SavedCard> =
        prefs.getString("cards_$ownerId", null)
            ?.lineSequence()
            ?.mapNotNull(::decodeCard)
            ?.toList()
            .orEmpty()

    fun addCard(ownerId: String, card: SavedCard) {
        val next = (listOf(card) + cards(ownerId)).distinctBy { it.last4 to it.expiry }.take(6)
        prefs.edit().putString("cards_$ownerId", encodeCards(next)).apply()
    }

    fun removeCard(ownerId: String, card: SavedCard) {
        val next = cards(ownerId).filterNot { it.last4 == card.last4 && it.expiry == card.expiry }
        prefs.edit().putString("cards_$ownerId", encodeCards(next)).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun decodeTopUp(line: String): TopUpEntry? {
        val parts = line.split("|")
        if (parts.size != 4) return null
        return TopUpEntry(
            packId = parts[0],
            coins = parts[1].toLongOrNull() ?: return null,
            price = parts[2].toIntOrNull() ?: return null,
            at = parts[3].toLongOrNull() ?: return null,
        )
    }

    private fun encodeCards(cards: List<SavedCard>): String =
        cards.joinToString("\n") { "${it.brand}|${it.last4}|${it.expiry}" }

    private fun decodeCard(line: String): SavedCard? {
        val parts = line.split("|")
        if (parts.size != 3) return null
        return SavedCard(brand = parts[0], last4 = parts[1], expiry = parts[2])
    }
}
