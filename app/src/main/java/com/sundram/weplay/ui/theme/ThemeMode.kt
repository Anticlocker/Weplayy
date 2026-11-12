package com.sundram.weplay.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sundram.weplay.data.UserPrefs

object ThemeMode {

    var value by mutableStateOf("system")
        private set

    fun load(context: Context) {
        value = UserPrefs(context).theme
    }

    fun save(context: Context, mode: String) {
        value = mode
        UserPrefs(context).theme = mode
    }

    fun isDark(systemDark: Boolean): Boolean = when (value) {
        "light" -> false
        "dark" -> true
        else -> systemDark
    }
}
