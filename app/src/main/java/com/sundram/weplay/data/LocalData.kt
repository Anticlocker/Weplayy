package com.sundram.weplay.data

import android.content.Context
import java.io.File

/**
 * Removes every local record owned by the signed-in account.
 */
fun clearLocalAccount(context: Context) {
    val app = context.applicationContext
    ProfileStore(app).clear()
    UserPrefs(app).clear()
    WalletStore(app).clear()
    SupportStore(app).clear()
    app.getSharedPreferences("weplay_topup", Context.MODE_PRIVATE)
        .edit()
        .clear()
        .apply()
    app.getSharedPreferences("weplay_messaging", Context.MODE_PRIVATE)
        .edit()
        .clear()
        .apply()
    app.getSharedPreferences("weplay_safety", Context.MODE_PRIVATE)
        .edit()
        .clear()
        .apply()
    File(app.filesDir, "chat_media").deleteRecursively()
}
