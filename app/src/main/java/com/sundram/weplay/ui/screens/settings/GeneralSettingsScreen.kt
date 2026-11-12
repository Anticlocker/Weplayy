package com.sundram.weplay.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sundram.weplay.R
import com.sundram.weplay.data.UserPrefs
import com.sundram.weplay.ui.screens.SubScreen
import com.sundram.weplay.ui.theme.CyanBrand
import com.sundram.weplay.ui.theme.MintGreen
import com.sundram.weplay.ui.theme.TextMuted
import com.sundram.weplay.ui.theme.Gold
import com.sundram.weplay.ui.theme.ThemeMode

@Composable
fun GeneralSettingsScreen(nav: NavHostController) {
    val context = nav.context
    val prefs = remember { UserPrefs(context) }
    var showLanguage by remember { mutableStateOf(false) }
    var showTheme by remember { mutableStateOf(false) }

    val languageLabel = if (prefs.language == "hi") "Hindi" else "English"
    val themeLabel = when (ThemeMode.value) {
        "light" -> "Light"
        "dark" -> "Dark"
        else -> "System"
    }
    val localSize = remember { directorySize(context.filesDir) }

    SubScreen(nav, "General", "Language, storage and data") {
        SettingsGroup("App") {
            SettingsRow(
                icon = R.drawable.ic_comment,
                tint = CyanBrand,
                title = "Language",
                subtitle = "Text shown in date and time formats",
                value = languageLabel,
                onClick = { showLanguage = true },
            )
            SettingsRowDivider()
            SettingsRow(
                icon = R.drawable.ic_theme,
                tint = MintGreen,
                title = "Theme",
                subtitle = "System, light or dark palette",
                value = themeLabel,
                onClick = { showTheme = true },
            )
            SettingsRowDivider()
            SettingsSwitchRow(
                icon = R.drawable.ic_bolt,
                tint = Gold,
                title = "Data saver",
                subtitle = "Lower audio quality on mobile data",
                checked = prefs.dataSaver,
            ) { prefs.dataSaver = it }
        }

        Spacer(Modifier.height(16.dp))

        SettingsGroup("Storage") {
            SettingsRow(
                icon = R.drawable.ic_bag,
                tint = MintGreen,
                title = "Local data",
                subtitle = "Profile photos, logs and reports stored on this device",
                value = if (localSize > 0L) formatBytes(localSize) else "—",
                enabled = false,
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            "System follows the device setting. Light and dark palettes use the same screens.",
            color = TextMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
        )
    }

    if (showLanguage) {
        SettingsChoiceDialog(
            title = "Language",
            options = listOf(
                SettingsChoice("en", "English"),
                SettingsChoice("hi", "Hindi"),
            ),
            selected = prefs.language,
            onSelect = { prefs.language = it },
            onDismiss = { showLanguage = false },
        )
    }

    if (showTheme) {
        SettingsChoiceDialog(
            title = "Theme",
            options = listOf(
                SettingsChoice("system", "System", "Follow the device setting"),
                SettingsChoice("light", "Light", "Always use the light palette"),
                SettingsChoice("dark", "Dark", "Always use the dark palette"),
            ),
            selected = ThemeMode.value,
            onSelect = { ThemeMode.save(context, it) },
            onDismiss = { showTheme = false },
        )
    }
}
