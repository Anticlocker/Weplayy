package com.sundram.weplay.ui.screens.settings

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sundram.weplay.R
import com.sundram.weplay.data.UserPrefs
import com.sundram.weplay.ui.nav.Routes
import com.sundram.weplay.ui.screens.SubScreen
import com.sundram.weplay.ui.theme.CyanBrand
import com.sundram.weplay.ui.theme.MintGreen
import com.sundram.weplay.ui.theme.PinkGift
import com.sundram.weplay.ui.theme.TextMuted
import com.sundram.weplay.ui.theme.Gold

@Composable
fun NotificationSettingsScreen(nav: NavHostController) {
    val context = LocalContext.current
    val prefs = remember { UserPrefs(context) }
    var push by remember { mutableStateOf(prefs.pushEnabled) }
    var followers by remember { mutableStateOf(prefs.notifyFollowers) }
    var gifts by remember { mutableStateOf(prefs.notifyGifts) }
    var roomInvites by remember { mutableStateOf(prefs.notifyRoomInvites) }
    var dms by remember { mutableStateOf(prefs.notifyDms) }
    var announcements by remember { mutableStateOf(prefs.notifyAnnouncements) }
    var sounds by remember { mutableStateOf(prefs.sounds) }
    var vibration by remember { mutableStateOf(prefs.vibration) }
    var dnd by remember { mutableStateOf(prefs.dndEnabled) }

    SubScreen(nav, "Notifications", "Choose what reaches you") {
        SettingsGroup("Push notifications") {
            SettingsSwitchRow(
                icon = R.drawable.ic_bell,
                tint = PinkGift,
                title = "Push notifications",
                subtitle = "Master switch for alerts on this device",
                checked = push,
            ) {
                push = it
                prefs.pushEnabled = it
            }
        }

        if (!push) {
            Text(
                "Push is off. Alert toggles below are paused until you turn it back on.",
                color = Gold,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(start = 2.dp, bottom = 12.dp),
            )
        }

        SettingsGroup("Alerts") {
            SettingsSwitchRow(
                icon = R.drawable.ic_users,
                tint = CyanBrand,
                title = "New followers",
                subtitle = "When someone starts following you",
                checked = followers,
                enabled = push,
            ) {
                followers = it
                prefs.notifyFollowers = it
            }
            SettingsRowDivider()
            SettingsSwitchRow(
                icon = R.drawable.ic_gift,
                tint = PinkGift,
                title = "Gifts received",
                subtitle = "When someone sends you a gift",
                checked = gifts,
                enabled = push,
            ) {
                gifts = it
                prefs.notifyGifts = it
            }
            SettingsRowDivider()
            SettingsSwitchRow(
                icon = R.drawable.ic_play,
                tint = Gold,
                title = "Room invites",
                subtitle = "When a friend opens a room",
                checked = roomInvites,
                enabled = push,
            ) {
                roomInvites = it
                prefs.notifyRoomInvites = it
            }
            SettingsRowDivider()
            SettingsSwitchRow(
                icon = R.drawable.ic_comment,
                tint = CyanBrand,
                title = "Direct messages",
                subtitle = "New private messages",
                checked = dms,
                enabled = push,
            ) {
                dms = it
                prefs.notifyDms = it
            }
            SettingsRowDivider()
            SettingsSwitchRow(
                icon = R.drawable.ic_info,
                tint = TextMuted,
                title = "System announcements",
                subtitle = "App updates and important notices",
                checked = announcements,
                enabled = push,
            ) {
                announcements = it
                prefs.notifyAnnouncements = it
            }
        }

        SettingsGroup("In-app feedback") {
            SettingsSwitchRow(
                icon = R.drawable.ic_play,
                tint = MintGreen,
                title = "Sound",
                subtitle = "Play a sound for in-app alerts",
                checked = sounds,
            ) {
                sounds = it
                prefs.sounds = it
            }
            SettingsRowDivider()
            SettingsSwitchRow(
                icon = R.drawable.ic_bolt,
                tint = Gold,
                title = "Vibration",
                subtitle = "Short vibration for alerts and actions",
                checked = vibration,
            ) {
                vibration = it
                prefs.vibration = it
            }
        }

        SettingsGroup("Quiet hours") {
            SettingsSwitchRow(
                icon = R.drawable.ic_bell,
                tint = TextMuted,
                title = "Do Not Disturb",
                subtitle = "Pause alerts during a schedule",
                checked = dnd,
            ) {
                dnd = it
                prefs.dndEnabled = it
            }
            SettingsRowDivider()
            SettingsRow(
                icon = R.drawable.ic_calendar,
                tint = CyanBrand,
                title = "Schedule",
                subtitle = "Start and end time for quiet hours",
                value = "${formatTime(prefs.dndStartMinute)} – ${formatTime(prefs.dndEndMinute)}",
                onClick = { nav.navigate(Routes.DND_SCHEDULE) },
            )
        }

        Text(
            "Push delivery is not connected in this build. Toggles and quiet hours are saved locally.",
            color = TextMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

@Composable
fun DndScheduleScreen(nav: NavHostController) {
    val context = LocalContext.current
    val prefs = remember { UserPrefs(context) }
    var enabled by remember { mutableStateOf(prefs.dndEnabled) }
    var start by remember { mutableStateOf(prefs.dndStartMinute) }
    var end by remember { mutableStateOf(prefs.dndEndMinute) }

    fun pickTime(initial: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(
            context,
            { _, hour, minute -> onPicked(hour * 60 + minute) },
            initial / 60,
            initial % 60,
            false,
        ).show()
    }

    SubScreen(nav, "Quiet hours", "Pause alerts on a schedule") {
        SettingsGroup {
            SettingsSwitchRow(
                icon = R.drawable.ic_bell,
                tint = TextMuted,
                title = "Do Not Disturb",
                subtitle = "Silence alerts during this window",
                checked = enabled,
            ) {
                enabled = it
                prefs.dndEnabled = it
            }
        }

        SettingsGroup("Schedule") {
            SettingsRow(
                icon = R.drawable.ic_calendar,
                tint = CyanBrand,
                title = "Starts",
                subtitle = "Quiet hours begin",
                value = formatTime(start),
                onClick = {
                    pickTime(start) {
                        start = it
                        prefs.dndStartMinute = it
                    }
                },
            )
            SettingsRowDivider()
            SettingsRow(
                icon = R.drawable.ic_calendar,
                tint = PinkGift,
                title = "Ends",
                subtitle = "Quiet hours finish",
                value = formatTime(end),
                onClick = {
                    pickTime(end) {
                        end = it
                        prefs.dndEndMinute = it
                    }
                },
            )
        }

        if (start == end) {
            Text(
                "Start and end times are the same, so quiet hours will not block anything.",
                color = Gold,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(start = 2.dp, bottom = 12.dp),
            )
        }

        Text(
            "The default window is 11:00 PM to 7:00 AM. Push delivery is simulated in this build.",
            color = TextMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}
