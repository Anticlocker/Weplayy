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
import com.sundram.weplay.ui.nav.Routes
import com.sundram.weplay.ui.screens.SubScreen
import com.sundram.weplay.ui.theme.CyanBrand
import com.sundram.weplay.ui.theme.MintGreen
import com.sundram.weplay.ui.theme.TextMuted
import com.sundram.weplay.ui.theme.Gold

@Composable
fun VoiceSettingsScreen(nav: NavHostController) {
    val prefs = remember { UserPrefs(nav.context) }
    var showQuality by remember { mutableStateOf(false) }
    var showOutput by remember { mutableStateOf(false) }

    val qualityLabel = when (prefs.audioQuality) {
        "low" -> "Low"
        "high" -> "High"
        else -> "Medium"
    }
    val outputLabel = if (prefs.defaultOutput == "earpiece") "Earpiece" else "Speaker"

    SubScreen(nav, "Voice & Audio", "Microphone, quality and output") {
        SettingsGroup("Microphone") {
            SettingsSwitchRow(
                icon = R.drawable.ic_mic,
                tint = CyanBrand,
                title = "Noise suppression",
                subtitle = "Filter background noise while you speak",
                checked = prefs.noiseSuppression,
            ) { prefs.noiseSuppression = it }
            SettingsRowDivider()
            SettingsSwitchRow(
                icon = R.drawable.ic_bolt,
                tint = Gold,
                title = "Volume boost",
                subtitle = "Raise non-speakers to a comfortable level",
                checked = prefs.volumeBoost,
            ) { prefs.volumeBoost = it }
        }

        Spacer(Modifier.height(16.dp))

        SettingsGroup("Audio output") {
            SettingsRow(
                icon = R.drawable.ic_nav_room,
                tint = CyanBrand,
                title = "Audio quality",
                subtitle = "Higher quality uses more data",
                value = qualityLabel,
                onClick = { showQuality = true },
            )
            SettingsRowDivider()
            SettingsRow(
                icon = R.drawable.ic_phone,
                tint = MintGreen,
                title = "Default output",
                subtitle = "Where new rooms play sound by default",
                value = outputLabel,
                onClick = { showOutput = true },
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            "Audio settings apply to new rooms. Live rooms keep the quality they were opened with.",
            color = TextMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
        )
    }

    if (showQuality) {
        SettingsChoiceDialog(
            title = "Audio quality",
            options = listOf(
                SettingsChoice("low", "Low", "Saves the most data"),
                SettingsChoice("medium", "Medium", "Balanced clarity and data use"),
                SettingsChoice("high", "High", "Best clarity, uses more data"),
            ),
            selected = prefs.audioQuality,
            onSelect = { prefs.audioQuality = it },
            onDismiss = { showQuality = false },
        )
    }

    if (showOutput) {
        SettingsChoiceDialog(
            title = "Default output",
            options = listOf(
                SettingsChoice("speaker", "Speaker", "Play room audio loudly on the built-in speaker"),
                SettingsChoice("earpiece", "Earpiece", "Play room audio like a phone call"),
            ),
            selected = prefs.defaultOutput,
            onSelect = { prefs.defaultOutput = it },
            onDismiss = { showOutput = false },
        )
    }
}
