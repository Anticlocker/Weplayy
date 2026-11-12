package com.sundram.weplay.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sundram.weplay.R
import com.sundram.weplay.ui.theme.CardWhite
import com.sundram.weplay.ui.theme.CyanBrand
import com.sundram.weplay.ui.theme.Gold
import com.sundram.weplay.ui.theme.KtvRed
import com.sundram.weplay.ui.theme.OnBrand
import com.sundram.weplay.ui.theme.PageWhite
import com.sundram.weplay.ui.theme.PinkGift
import com.sundram.weplay.ui.theme.Stroke
import com.sundram.weplay.ui.theme.TextBody
import com.sundram.weplay.ui.theme.TextMain
import com.sundram.weplay.ui.theme.TextMuted
import com.sundram.weplay.ui.theme.WarnOrange
import com.sundram.weplay.ui.nav.Routes

private val MeBg = Color(0xFFF5F6FA)
private val HeaderBg = Color(0xFFE8EAF2)
private val ProfileBrown = Color(0xFF7A5C53)
private val QuickVip = Color(0xFFFFD54A)
private val QuickShop = Color(0xFFFF6F91)
private val QuickPlay = Color(0xFF4FB8FF)
private val QuickHome = Color(0xFFFF6B6B)
private val NewBadge = Color(0xFFF0C040)
private val EarnGift = Color(0xFFF5A623)

private data class QuickItem(
    val label: String,
    val icon: Int,
    val tint: Color,
    val newBadge: Boolean = false,
)

private val quickItems = listOf(
    QuickItem("VIP Center", R.drawable.ic_bolt, QuickVip),
    QuickItem("Shop", R.drawable.ic_bag, QuickShop),
    QuickItem("PLAY Show", R.drawable.ic_play, QuickPlay, newBadge = true),
    QuickItem("My Home", R.drawable.ic_users, QuickHome),
)

private data class MenuItem(
    val label: String,
    val icon: Int,
    val tint: Color,
    val trailing: String? = null,
    val badge: Boolean = false,
)

/** Built inside composition because TextMuted follows the live palette. */
private fun buildMenuItems(muted: Color, card: Color, brand: Color) = listOf(
    MenuItem("Moments", R.drawable.ic_heart, brand),
    MenuItem("Stats", R.drawable.ic_crown, Gold),
    MenuItem("Visitors", R.drawable.ic_users, brand),
    MenuItem("Invite Friends", R.drawable.ic_plus, brand, trailing = "Earn gift"),
    MenuItem("Badge", R.drawable.ic_star, QuickVip),
    MenuItem("Contributions", R.drawable.ic_comment, muted),
    MenuItem("Language", R.drawable.ic_theme, brand, trailing = "En", badge = true),
    MenuItem("Parental control mode", R.drawable.ic_info, muted),
)

/** Me / Settings hub screen: profile header, quick actions, settings list. */
@Composable
fun MeSettingsScreen(
    nav: NavHostController,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeBg)
            .verticalScroll(rememberScrollState()),
    ) {
        // Profile header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(HeaderBg)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(72.dp)
                        .background(ProfileBrown, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "0",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Osama bin Moham...",
                        color = TextMain,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .height(6.dp)
                                .width(180.dp)
                                .background(Color(0xFFD9D9D9), RoundedCornerShape(3.dp)),
                        ) {
                            Box(
                                Modifier
                                    .width(84.dp)
                                    .height(6.dp)
                                    .background(CyanBrand, RoundedCornerShape(3.dp)),
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("Lv.1", color = TextBody, fontSize = 12.sp)
                        Text("5/60", color = TextBody, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp).clickable {},
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Quick action row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            quickItems.forEach { item ->
                QuickBox(
                    label = item.label,
                    icon = item.icon,
                    tint = item.tint,
                    newBadge = item.newBadge,
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Settings list
        val menuItems = buildMenuItems(
            muted = TextMuted,
            card = CardWhite,
            brand = CyanBrand,
        )
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            menuItems.forEach { item ->
                SettingsRow(
                    label = item.label,
                    icon = item.icon,
                    tint = item.tint,
                    trailing = item.trailing,
                    badge = item.badge,
                    onClick = {},
                )
                DividerLine()
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun QuickBox(
    label: String,
    icon: Int,
    tint: Color,
    newBadge: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .background(tint, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (newBadge) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(20.dp)
                        .background(NewBadge, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "New",
                        color = OnBrand,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            color = TextMain,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun SettingsRow(
    label: String,
    icon: Int,
    tint: Color,
    trailing: String?,
    badge: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .background(tint.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            color = TextMain,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        if (badge) {
            Box(
                Modifier
                    .background(KtvRed, CircleShape)
                    .border(1.dp, CardWhite, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "4 d 4 h",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        if (trailing != null) {
            Text(
                trailing,
                color = EarnGift,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        } else if (!badge) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun DividerLine() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Stroke)
            .padding(horizontal = 16.dp),
    )
}
