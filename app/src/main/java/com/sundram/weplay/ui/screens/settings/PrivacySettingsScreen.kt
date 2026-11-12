package com.sundram.weplay.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sundram.weplay.R
import com.sundram.weplay.data.DirectoryUser
import com.sundram.weplay.data.MessagingStore
import com.sundram.weplay.data.UserPrefs
import com.sundram.weplay.data.UserProfile
import com.sundram.weplay.ui.components.InitialsAvatar
import com.sundram.weplay.ui.components.PrimaryButton
import com.sundram.weplay.ui.nav.Routes
import com.sundram.weplay.ui.screens.SubScreen
import com.sundram.weplay.ui.theme.CyanBrand
import com.sundram.weplay.ui.theme.MintGreen
import com.sundram.weplay.ui.theme.PinkGift
import com.sundram.weplay.ui.theme.CardWhite
import com.sundram.weplay.ui.theme.TextMuted
import com.sundram.weplay.ui.theme.Gold
import com.sundram.weplay.ui.theme.TextMain

private val ReportReasons = listOf(
    "Harassment or abuse",
    "Hate speech",
    "Spam or scam",
    "Inappropriate content",
    "Impersonation",
    "Other",
)

private val DmChoices = listOf(
    SettingsChoice("everyone", "Everyone", "Any WePlay user can message you"),
    SettingsChoice("followers", "Followers only", "Only people who follow you can message you"),
    SettingsChoice("nobody", "Nobody", "No one can start a new direct message"),
)

private val InviteChoices = listOf(
    SettingsChoice("everyone", "Everyone", "Any WePlay user can invite you to a room"),
    SettingsChoice("following", "Following only", "Only people you follow can invite you"),
)

@Composable
fun PrivacySettingsScreen(nav: NavHostController, profile: UserProfile) {
    val context = LocalContext.current
    val prefs = remember { UserPrefs(context) }
    val store = remember(profile.id) { MessagingStore(context) { profile } }
    var dialog by remember { mutableStateOf<String?>(null) }
    var showOnline by remember { mutableStateOf(prefs.showOnline) }
    var showGifts by remember { mutableStateOf(prefs.showGifts) }
    var privateAccount by remember { mutableStateOf(prefs.privateAccount) }

    val blockedCount = store.blockedUsers().size
    val reportCount = store.reports().size

    SubScreen(nav, "Privacy & Safety", "Control who can reach you") {
        SettingsGroup("Direct messages") {
            SettingsRow(
                icon = R.drawable.ic_comment,
                tint = CyanBrand,
                title = "Who can DM me",
                subtitle = DmChoices.firstOrNull { it.value == prefs.dmPermission }?.label
                    ?: "Everyone",
                onClick = { dialog = "dm" },
            )
            SettingsRowDivider()
            SettingsRow(
                icon = R.drawable.ic_users,
                tint = MintGreen,
                title = "Who can invite me",
                subtitle = InviteChoices.firstOrNull { it.value == prefs.invitePermission }?.label
                    ?: "Everyone",
                onClick = { dialog = "invite" },
            )
        }

        SettingsGroup("Profile visibility") {
            SettingsSwitchRow(
                icon = R.drawable.ic_verified,
                tint = MintGreen,
                title = "Show online status",
                subtitle = "Let others see when you are active",
                checked = showOnline,
            ) {
                showOnline = it
                prefs.showOnline = it
            }
            SettingsRowDivider()
            SettingsSwitchRow(
                icon = R.drawable.ic_gift,
                tint = PinkGift,
                title = "Show gifts received",
                subtitle = "Display your gift collection on your profile",
                checked = showGifts,
            ) {
                showGifts = it
                prefs.showGifts = it
            }
            SettingsRowDivider()
            SettingsSwitchRow(
                icon = R.drawable.ic_users,
                tint = CyanBrand,
                title = "Private account",
                subtitle = "New followers need your approval",
                checked = privateAccount,
            ) {
                privateAccount = it
                prefs.privateAccount = it
            }
        }

        SettingsGroup("Safety tools") {
            SettingsRow(
                icon = R.drawable.ic_close,
                tint = PinkGift,
                title = "Blocked users",
                subtitle = "People who cannot contact you",
                value = blockedCount.toString(),
                onClick = { nav.navigate(Routes.BLOCKED_USERS) },
            )
            SettingsRowDivider()
            SettingsRow(
                icon = R.drawable.ic_info,
                tint = Gold,
                title = "Report history",
                subtitle = "Reports you have submitted",
                value = reportCount.toString(),
                onClick = { nav.navigate(Routes.REPORT_HISTORY) },
            )
        }

        Text(
            "These preferences are stored on this device. Users and rooms are simulated in this build.",
            color = TextMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }

    when (dialog) {
        "dm" -> SettingsChoiceDialog(
            title = "Who can DM me",
            options = DmChoices,
            selected = prefs.dmPermission,
            onSelect = { prefs.dmPermission = it },
            onDismiss = { dialog = null },
        )

        "invite" -> SettingsChoiceDialog(
            title = "Who can invite me",
            options = InviteChoices,
            selected = prefs.invitePermission,
            onSelect = { prefs.invitePermission = it },
            onDismiss = { dialog = null },
        )
    }
}

@Composable
fun BlockedUsersScreen(nav: NavHostController, profile: UserProfile) {
    val context = LocalContext.current
    val store = remember(profile.id) { MessagingStore(context) { profile } }
    var blocked by remember { mutableStateOf(store.blockedUsers()) }
    val available = MessagingStore.DIRECTORY.filter { demo ->
        blocked.none { it.id == demo.id }
    }

    SubScreen(nav, "Blocked users", "People who cannot contact you") {
        if (blocked.isEmpty()) {
            SettingsEmptyState(
                title = "No blocked users",
                body = "Block someone from a room or profile and they will appear here.",
            )
        } else {
            SettingsGroup("Blocked") {
                blocked.forEachIndexed { index, user ->
                    BlockedUserRow(
                        user = user,
                        action = "Unblock",
                        actionColor = CyanBrand,
                    ) {
                        store.unblock(user.id)
                        blocked = store.blockedUsers()
                        Toast.makeText(context, "${user.name} unblocked", Toast.LENGTH_SHORT).show()
                    }
                    if (index != blocked.lastIndex) SettingsRowDivider()
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        SettingsGroup("Recent users") {
            if (available.isEmpty()) {
                Text(
                    "No more recent users to block.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(14.dp),
                )
            } else {
                available.forEachIndexed { index, user ->
                    BlockedUserRow(
                        user = user,
                        action = "Block",
                        actionColor = PinkGift,
                    ) {
                        store.block(user.id)
                        blocked = store.blockedUsers()
                        Toast.makeText(context, "${user.name} blocked", Toast.LENGTH_SHORT).show()
                    }
                    if (index != available.lastIndex) SettingsRowDivider()
                }
            }
        }

        Text(
            "Blocking is local to this demo. Blocked people are removed from your friend list " +
                "and cannot message you.",
            color = TextMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

@Composable
private fun BlockedUserRow(
    user: DirectoryUser,
    action: String,
    actionColor: Color,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InitialsAvatar(name = user.name, size = 38.dp)
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(user.name, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(user.id, color = TextMuted, fontSize = 11.sp)
        }
        Text(
            action,
            color = actionColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(actionColor.copy(alpha = 0.13f), RoundedCornerShape(9.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 11.dp, vertical = 6.dp),
        )
    }
}

@Composable
fun ReportHistoryScreen(nav: NavHostController, profile: UserProfile) {
    val context = LocalContext.current
    val store = remember(profile.id) { MessagingStore(context) { profile } }
    var reports by remember { mutableStateOf(store.reports()) }
    var showNew by remember { mutableStateOf(false) }
    var showReason by remember { mutableStateOf(false) }
    var showTarget by remember { mutableStateOf(false) }
    var targetId by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf(ReportReasons.first()) }
    val targetChoices = MessagingStore.DIRECTORY.map {
        SettingsChoice(it.id, it.name, "@${it.id}")
    }
    val targetName = MessagingStore.DIRECTORY.firstOrNull { it.id == targetId }?.name

    SubScreen(nav, "Report history", "Reports you have submitted") {
        PrimaryButton("Report a user") { showNew = true }
        Spacer(Modifier.height(16.dp))

        if (reports.isEmpty()) {
            SettingsEmptyState(
                title = "No reports yet",
                body = "Reports you submit from rooms or profiles will be listed here.",
            )
        } else {
            SettingsGroup("Submitted reports") {
                reports.forEachIndexed { index, report ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                store.userById(report.targetId)?.name ?: report.targetId,
                                color = TextMain,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            SettingsValueBadge("Submitted", MintGreen)
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(report.reason, color = TextMuted, fontSize = 12.sp)
                        Text(
                            "@${report.targetId} · ${formatStamp(report.at)}",
                            color = TextMuted,
                            fontSize = 10.sp,
                        )
                    }
                    if (index != reports.lastIndex) SettingsRowDivider()
                }
            }
        }

        Text(
            "Reports are stored on this device. No moderation service is connected in this build.",
            color = TextMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }

    if (showNew) {
        AlertDialog(
            onDismissRequest = { showNew = false },
            containerColor = CardWhite,
            title = { Text("Report a user", color = TextMain, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("User", color = TextMuted, fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(CardWhite, RoundedCornerShape(12.dp))
                            .clickable { showTarget = true }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            targetName ?: "Choose a user",
                            color = if (targetId.isEmpty()) TextMuted else TextMain,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Text("Change", color = CyanBrand, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Reason", color = TextMuted, fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(CardWhite, RoundedCornerShape(12.dp))
                            .clickable { showReason = true }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            reason,
                            color = TextMain,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Text("Change", color = CyanBrand, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Submitting a report also blocks the user. Reports are saved locally.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = targetId.isNotEmpty(),
                    onClick = {
                        store.report(targetId, reason)
                        reports = store.reports()
                        targetId = ""
                        reason = ReportReasons.first()
                        showNew = false
                        Toast.makeText(context, "Report submitted", Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Text(
                        "Submit",
                        color = if (targetId.isNotEmpty()) CyanBrand else TextMuted,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showNew = false }) { Text("Cancel", color = TextMuted) }
            },
        )
    }

    if (showTarget) {
        SettingsChoiceDialog(
            title = "Report a user",
            options = targetChoices,
            selected = targetId,
            onSelect = { targetId = it },
            onDismiss = { showTarget = false },
        )
    }

    if (showReason) {
        SettingsChoiceDialog(
            title = "Report reason",
            options = ReportReasons.map { SettingsChoice(it, it) },
            selected = reason,
            onSelect = { reason = it },
            onDismiss = { showReason = false },
        )
    }
}
