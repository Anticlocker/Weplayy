package com.sundram.weplay.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sundram.weplay.R
import com.sundram.weplay.data.DemoBackpack
import com.sundram.weplay.data.UserProfile
import com.sundram.weplay.data.ageFromDob
import com.sundram.weplay.ui.components.SectionHeader
import com.sundram.weplay.ui.components.UserAvatar
import com.sundram.weplay.ui.nav.Routes
import com.sundram.weplay.ui.theme.CardWhite
import com.sundram.weplay.ui.theme.CyanBrand
import com.sundram.weplay.ui.theme.Divider
import com.sundram.weplay.ui.theme.Gold
import com.sundram.weplay.ui.theme.MintGreen
import com.sundram.weplay.ui.theme.PageWhite
import com.sundram.weplay.ui.theme.PinkGift
import com.sundram.weplay.ui.theme.TagBlue
import com.sundram.weplay.ui.theme.TagPurple
import com.sundram.weplay.ui.theme.TextBody
import com.sundram.weplay.ui.theme.TextMain
import com.sundram.weplay.ui.theme.TextMuted

@Composable
fun MeScreen(
    nav: NavHostController,
    pad: PaddingValues,
    profile: UserProfile,
    onLogout: () -> Unit,
) {
    var showLogout by rememberSaveable { mutableStateOf(false) }
    val frame = DemoBackpack.firstOrNull { it.id == profile.frame }?.frame?.let { Color(it) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageWhite)
            .statusBarsPadding()
            .padding(pad)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(CyanBrand.copy(alpha = 0.16f), PageWhite)
                    )
                )
                .padding(horizontal = 18.dp, vertical = 22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(profile.name, profile.photoPath, size = 76.dp, ring = frame)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            profile.name,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMain,
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            painter = painterResource(R.drawable.ic_verified),
                            contentDescription = null,
                            tint = CyanBrand,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text("ID: ${profile.id}", fontSize = 11.sp, color = TextBody)
                    Spacer(Modifier.height(5.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(MintGreen, CircleShape))
                        Spacer(Modifier.width(5.dp))
                        Text("Online", fontSize = 10.sp, color = TextBody)
                        Spacer(Modifier.width(10.dp))
                        Text("Age ${ageFromDob(profile.dobMillis)}", fontSize = 10.sp, color = TextMuted)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(CardWhite, RoundedCornerShape(16.dp))
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Stat(R.drawable.ic_coin, "%,d".format(profile.coins), "Coins", Gold) {
                nav.navigate(Routes.TOPUP)
            }
            WalletDivider()
            Stat(R.drawable.ic_bolt, "${profile.level}", "Level", CyanBrand)
            WalletDivider()
            Stat(R.drawable.ic_star, "0", "Badges", PinkGift)
        }

        Spacer(Modifier.height(18.dp))
        Column(Modifier.padding(horizontal = 16.dp)) {
            SectionHeader("Account")
            Spacer(Modifier.height(4.dp))
        }
        Column(Modifier.padding(horizontal = 16.dp)) {
            MenuRow(R.drawable.ic_plus, Gold, "Top up coins", "Add coins to your wallet") {
                nav.navigate(Routes.TOPUP)
            }
            MenuRow(R.drawable.ic_edit, TagBlue, "Edit profile", "Photo, name and birthday") {
                nav.navigate(Routes.PROFILE_EDIT)
            }
            MenuRow(R.drawable.ic_gift, PinkGift, "My gifts", "Collection and daily reward") {
                nav.navigate(Routes.GIFTS)
            }
            MenuRow(R.drawable.ic_crown, Gold, "Family hub", profile.family ?: "No family yet") {
                nav.navigate(Routes.FAMILY)
            }
            MenuRow(R.drawable.ic_bag, CyanBrand, "Backpack", "Frames, bubbles and badges") {
                nav.navigate(Routes.BACKPACK)
            }
            MenuRow(R.drawable.ic_settings, TagPurple, "Settings", "Notifications, sound and account") {
                nav.navigate(Routes.SETTINGS)
            }
            MenuRow(R.drawable.ic_logout, PinkGift, "Log out", "Sign out of this device") {
                showLogout = true
            }
            MenuRow(R.drawable.ic_info, TextMuted, "About", "Version and policies") {
                nav.navigate(Routes.ABOUT)
            }
        }

        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(
                if (com.sundram.weplay.core.AppConfig.isApiConfigured) "Connected to server" else "Local mode — server not configured",
                fontSize = 11.sp,
                color = TextMuted,
            )
        }
    }

    if (showLogout) {
        AlertDialog(
            onDismissRequest = { showLogout = false },
            containerColor = CardWhite,
            title = { Text("Log out?", color = TextMain, fontWeight = FontWeight.Bold) },
            text = { Text("You will return to the sign-in screen.", color = TextBody, fontSize = 13.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogout = false
                        onLogout()
                    }
                ) { Text("Log out", color = PinkGift, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showLogout = false }) { Text("Cancel", color = TextMuted) }
            },
        )
    }
}

@Composable
private fun RowScope.Stat(
    icon: Int,
    value: String,
    label: String,
    tint: Color,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .weight(1f)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .background(tint.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(7.dp))
        Column {
            Text(value, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(label, color = TextMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun WalletDivider() {
    Box(Modifier.width(1.dp).height(30.dp).background(Divider))
}

@Composable
private fun MenuRow(icon: Int, tint: Color, title: String, sub: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .background(tint.copy(alpha = 0.12f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(title, color = TextMain, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(sub, color = TextMuted, fontSize = 11.sp)
        Spacer(Modifier.width(4.dp))
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(16.dp),
        )
    }
}
