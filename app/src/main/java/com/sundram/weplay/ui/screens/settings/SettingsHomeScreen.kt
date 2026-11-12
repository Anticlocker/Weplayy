package com.sundram.weplay.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sundram.weplay.R
import com.sundram.weplay.data.UserPrefs
import com.sundram.weplay.data.UserProfile
import com.sundram.weplay.ui.components.UserAvatar
import com.sundram.weplay.ui.nav.Routes
import com.sundram.weplay.ui.screens.SubScreen
import com.sundram.weplay.ui.theme.CyanBrand
import com.sundram.weplay.ui.theme.MintGreen
import com.sundram.weplay.ui.theme.PinkGift
import com.sundram.weplay.ui.theme.CardWhite
import com.sundram.weplay.ui.theme.TextMuted
import com.sundram.weplay.ui.theme.Gold
import com.sundram.weplay.ui.theme.TextMain

@Composable
fun SettingsHomeScreen(
    nav: NavHostController,
    profile: UserProfile,
) {
    val context = LocalContext.current
    val prefs = remember { UserPrefs(context) }
    val verified = profile.phoneVerified || profile.emailVerified
    val pushLabel = if (prefs.pushEnabled) "On" else "Off"
    val qualityLabel = when (prefs.audioQuality) {
        "low" -> "Low"
        "high" -> "High"
        else -> "Medium"
    }
    val languageLabel = if (prefs.language == "hi") "Hindi" else "English"

    SubScreen(nav, "Settings", "Account, privacy and app preferences") {
        ProfileSummary(profile, verified)

        SettingsGroup("Account") {
            SettingsRow(
                icon = R.drawable.ic_edit,
                tint = CyanBrand,
                title = "Account",
                subtitle = "Profile, username and security",
                value = "@${profile.username}",
                onClick = { nav.navigate(Routes.ACCOUNT_SETTINGS) },
            )
        }

        SettingsGroup("Preferences") {
            SettingsRow(
                icon = R.drawable.ic_bell,
                tint = PinkGift,
                title = "Message notification",
                subtitle = "Push alerts and quiet hours",
                value = pushLabel,
                valueColor = if (prefs.pushEnabled) MintGreen else TextMuted,
                onClick = { nav.navigate(Routes.NOTIFICATION_SETTINGS) },
            )
            SettingsRowDivider()
            SettingsRow(
                icon = R.drawable.ic_users,
                tint = CyanBrand,
                title = "Privacy",
                subtitle = "DMs, blocking and reports",
                onClick = { nav.navigate(Routes.PRIVACY_SETTINGS) },
            )
            SettingsRowDivider()
            SettingsRow(
                icon = R.drawable.ic_settings,
                tint = TextMuted,
                title = "General",
                subtitle = "Language, theme and storage",
                value = languageLabel,
                onClick = { nav.navigate(Routes.GENERAL_SETTINGS) },
            )
        }

        SettingsGroup("Support") {
            SettingsRow(
                icon = R.drawable.ic_bolt,
                tint = Gold,
                title = "Quick repair",
                subtitle = "Run basic local checks",
                onClick = { nav.navigate(Routes.SUPPORT_SETTINGS) },
            )
            SettingsRowDivider()
            SettingsRow(
                icon = R.drawable.ic_mic,
                tint = CyanBrand,
                title = "Network test",
                subtitle = "Check server connection status",
                onClick = { nav.navigate(Routes.VOICE_SETTINGS) },
            )
            SettingsRowDivider()
            SettingsRow(
                icon = R.drawable.ic_plus,
                tint = TextMuted,
                title = "Upload logs",
                subtitle = "Support diagnostics",
                onClick = { nav.navigate(Routes.SUPPORT_SETTINGS) },
            )
            SettingsRowDivider()
            SettingsRow(
                icon = R.drawable.ic_info,
                tint = TextMuted,
                title = "About us",
                subtitle = "Version, terms and licenses",
                onClick = { nav.navigate(Routes.ABOUT) },
            )
        }

        SettingsGroup("Account access") {
            SettingsRow(
                icon = R.drawable.ic_users,
                tint = CyanBrand,
                title = "Switch account",
                subtitle = "Sign in with another account",
                showChevron = false,
                onClick = { nav.navigate(Routes.ACCOUNT_SETTINGS) },
            )
            SettingsRowDivider()
            SettingsRow(
                icon = R.drawable.ic_logout,
                tint = PinkGift,
                title = "Logout",
                subtitle = "Return to the sign-in screen",
                showChevron = false,
                onClick = { nav.navigate(Routes.ACCOUNT_SETTINGS) },
            )
        }

        Text(
            "WePlay ${appVersion(context)} · local build",
            color = TextMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 2.dp),
        )
    }
}

@Composable
private fun ProfileSummary(profile: UserProfile, verified: Boolean) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(CardWhite, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(profile.name, profile.photoPath, size = 58.dp)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        profile.name,
                        color = TextMain,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (verified) {
                        Spacer(Modifier.width(5.dp))
                        Icon(
                            painter = painterResource(R.drawable.ic_verified),
                            contentDescription = null,
                            tint = CyanBrand,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
                Text(
                    "@${profile.username}",
                    color = CyanBrand,
                    fontSize = 12.sp,
                )
                Text(
                    "ID ${profile.id}",
                    color = TextMuted,
                    fontSize = 11.sp,
                )
            }
        }
        if (profile.bio.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                profile.bio,
                color = TextMuted,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
        }
    }
    Spacer(Modifier.height(16.dp))
}
