package com.sundram.weplay.ui.nav

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.sundram.weplay.R
import com.sundram.weplay.ui.components.UnreadBadge
import com.sundram.weplay.ui.theme.CardWhite
import com.sundram.weplay.ui.theme.CyanBrand
import com.sundram.weplay.ui.theme.Divider
import com.sundram.weplay.ui.theme.TextMain
import com.sundram.weplay.ui.theme.TextMuted

object Routes {
    const val HUB = "tab_hub"
    const val ROOM = "tab_room"
    const val DISCOVER = "tab_discover"
    const val MSG = "tab_msg"
    const val ME = "tab_me"
    const val ROOM_DETAIL = "room/{roomId}"
    const val LUDO = "game/ludo"
    const val SPY = "game/spy"
    const val PROFILE_EDIT = "profile/edit"
    const val GIFTS = "profile/gifts"
    const val FAMILY = "profile/family"
    const val BACKPACK = "profile/backpack"
    const val SETTINGS = "profile/settings"
    const val ABOUT = "profile/about"
    const val ACCOUNT_SETTINGS = "settings/account"
    const val PRIVACY_SETTINGS = "settings/privacy"
    const val BLOCKED_USERS = "settings/privacy/blocked"
    const val REPORT_HISTORY = "settings/privacy/reports"
    const val NOTIFICATION_SETTINGS = "settings/notifications"
    const val DND_SCHEDULE = "settings/notifications/dnd"
    const val VOICE_SETTINGS = "settings/voice"
    const val GENERAL_SETTINGS = "settings/general"
    const val WALLET_SETTINGS = "settings/wallet"
    const val WALLET_TRANSACTIONS = "settings/wallet/transactions"
    const val PAYMENT_METHODS = "settings/wallet/payment-methods"
    const val HOST_EARNINGS = "settings/wallet/host-earnings"
    const val SUPPORT_SETTINGS = "settings/support"
    const val TOPUP = "profile/topup"
    const val CHAT = "chat/{userId}"
    const val USER = "user/{userId}"
    const val USER_DETAIL = "profile/detail/{userId}"
    const val RANKING = "tab_ranking"
    const val ME_SETTINGS = "tab_me_settings"
    const val CREATE_ROOM = "tab_create_room"

    fun chatRoute(id: String) = "chat/$id"
    fun userRoute(id: String) = "user/$id"
    fun userDetailRoute(id: String) = "profile/detail/$id"
    fun roomRoute(id: String) = "room/$id"

    val tabs = setOf(HUB, ROOM, DISCOVER, MSG, ME)
}

private data class TabItem(
    val route: String,
    @DrawableRes val icon: Int,
    val label: String,
    val badge: () -> Int? = { null },
)

private val tabs = listOf(
    TabItem(Routes.HUB, R.drawable.ic_nav_games, "WePlay"),
    TabItem(Routes.ROOM, R.drawable.ic_nav_room, "Voice Room"),
    TabItem(Routes.MSG, R.drawable.ic_nav_msg, "Chats"),
    TabItem(Routes.DISCOVER, R.drawable.ic_nav_discover, "Discover"),
    TabItem(Routes.ME, R.drawable.ic_nav_me, "Me"),
)

private fun switch(controller: NavHostController, route: String) {
    controller.navigate(route) {
        popUpTo(controller.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun WePlayBottomBar(nav: NavHostController, current: String?, msgBadge: Int = 0) {
    val navBarBg = Color.White
    val inactiveColor = Color(0xFFB0BEC5)
    val activeLabelColor = Color(0xFF151C33)
    val badgeRed = Color(0xFFFF4D4F)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = navBarBg,
        tonalElevation = 0.dp,
    ) {
        Column(Modifier.navigationBarsPadding()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(0.8.dp)
                    .background(Color(0xFFEEEEEE)),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { tab ->
                    val selected = current == tab.route
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { switch(nav, tab.route) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(tab.icon),
                                contentDescription = tab.label,
                                tint = if (selected) CyanBrand else inactiveColor,
                                modifier = Modifier.size(26.dp),
                            )
                            // Chats badge with count
                            if (tab.route == Routes.MSG) {
                                val badgeCount = if (msgBadge > 0) msgBadge else 1
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 8.dp, y = (-4).dp)
                                        .size(16.dp)
                                        .background(badgeRed, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "$badgeCount",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            // Discover and Me red dot badges
                            if (tab.route == Routes.DISCOVER || tab.route == Routes.ME) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 3.dp, y = (-2).dp)
                                        .size(6.dp)
                                        .background(badgeRed, CircleShape),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
