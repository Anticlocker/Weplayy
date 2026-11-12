package com.sundram.weplay.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserPrefs(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("weplay_prefs", Context.MODE_PRIVATE)

    var invites: Boolean
        get() = prefs.getBoolean("invites", true)
        set(value) = prefs.edit().putBoolean("invites", value).apply()

    var messages: Boolean
        get() = prefs.getBoolean("messages", true)
        set(value) = prefs.edit().putBoolean("messages", value).apply()

    var sounds: Boolean
        get() = prefs.getBoolean("sounds", true)
        set(value) = prefs.edit().putBoolean("sounds", value).apply()

    var vibration: Boolean
        get() = prefs.getBoolean("vibration", true)
        set(value) = prefs.edit().putBoolean("vibration", value).apply()

    var dmPermission: String
        get() = prefs.getString("dm_permission", "everyone") ?: "everyone"
        set(value) = prefs.edit().putString("dm_permission", value).apply()

    var invitePermission: String
        get() = prefs.getString("invite_permission", "everyone") ?: "everyone"
        set(value) = prefs.edit().putString("invite_permission", value).apply()

    var showOnline: Boolean
        get() = prefs.getBoolean("show_online", true)
        set(value) = prefs.edit().putBoolean("show_online", value).apply()

    var showGifts: Boolean
        get() = prefs.getBoolean("show_gifts", true)
        set(value) = prefs.edit().putBoolean("show_gifts", value).apply()

    var privateAccount: Boolean
        get() = prefs.getBoolean("private_account", false)
        set(value) = prefs.edit().putBoolean("private_account", value).apply()

    var pushEnabled: Boolean
        get() = prefs.getBoolean("push_enabled", true)
        set(value) = prefs.edit().putBoolean("push_enabled", value).apply()

    var notifyFollowers: Boolean
        get() = prefs.getBoolean("notify_followers", true)
        set(value) = prefs.edit().putBoolean("notify_followers", value).apply()

    var notifyGifts: Boolean
        get() = prefs.getBoolean("notify_gifts", true)
        set(value) = prefs.edit().putBoolean("notify_gifts", value).apply()

    var notifyRoomInvites: Boolean
        get() = prefs.getBoolean("invites", true)
        set(value) = prefs.edit().putBoolean("invites", value).apply()

    var notifyDms: Boolean
        get() = prefs.getBoolean("messages", true)
        set(value) = prefs.edit().putBoolean("messages", value).apply()

    var notifyAnnouncements: Boolean
        get() = prefs.getBoolean("notify_announcements", true)
        set(value) = prefs.edit().putBoolean("notify_announcements", value).apply()

    var dndEnabled: Boolean
        get() = prefs.getBoolean("dnd_enabled", false)
        set(value) = prefs.edit().putBoolean("dnd_enabled", value).apply()

    var dndStartMinute: Int
        get() = prefs.getInt("dnd_start_minute", 23 * 60)
        set(value) = prefs.edit().putInt("dnd_start_minute", value.coerceIn(0, 1439)).apply()

    var dndEndMinute: Int
        get() = prefs.getInt("dnd_end_minute", 7 * 60)
        set(value) = prefs.edit().putInt("dnd_end_minute", value.coerceIn(0, 1439)).apply()

    var audioQuality: String
        get() = prefs.getString("audio_quality", "medium") ?: "medium"
        set(value) = prefs.edit().putString("audio_quality", value).apply()

    var defaultOutput: String
        get() = prefs.getString("default_output", "speaker") ?: "speaker"
        set(value) = prefs.edit().putString("default_output", value).apply()

    var noiseSuppression: Boolean
        get() = prefs.getBoolean("noise_suppression", true)
        set(value) = prefs.edit().putBoolean("noise_suppression", value).apply()

    var volumeBoost: Boolean
        get() = prefs.getBoolean("volume_boost", false)
        set(value) = prefs.edit().putBoolean("volume_boost", value).apply()

    var language: String
        get() = prefs.getString("language", "en") ?: "en"
        set(value) = prefs.edit().putString("language", value).apply()

    var theme: String
        get() = prefs.getString("theme", "system") ?: "system"
        set(value) = prefs.edit().putString("theme", value).apply()

    var dataSaver: Boolean
        get() = prefs.getBoolean("data_saver", false)
        set(value) = prefs.edit().putBoolean("data_saver", value).apply()

    var myRating: Int
        get() = prefs.getInt("my_rating", 0)
        set(value) = prefs.edit().putInt("my_rating", value.coerceIn(0, 5)).apply()

    fun giftCount(id: String): Int = prefs.getInt("gift_$id", defaultGiftCount(id))

    fun setGiftCount(id: String, count: Int) {
        prefs.edit().putInt("gift_$id", count.coerceAtLeast(0)).apply()
    }

    fun claimedToday(): Boolean = prefs.getString("daily_claim", null) == dayStamp()

    fun claimDailyGift(id: String): Boolean {
        if (claimedToday()) return false
        prefs.edit()
            .putString("daily_claim", dayStamp())
            .putInt("gift_$id", giftCount(id) + 1)
            .apply()
        return true
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun dayStamp(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
