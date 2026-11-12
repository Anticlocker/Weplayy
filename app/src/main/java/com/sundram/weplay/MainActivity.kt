package com.sundram.weplay

import android.app.Activity
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sundram.weplay.data.MessagingStore
import com.sundram.weplay.data.ProfileStore
import com.sundram.weplay.data.UserProfile
import com.sundram.weplay.ui.nav.Routes
import com.sundram.weplay.ui.nav.WePlayBottomBar
import com.sundram.weplay.ui.screens.AboutScreen
import com.sundram.weplay.ui.screens.AuthScreen
import com.sundram.weplay.ui.screens.ChatScreen
import com.sundram.weplay.ui.screens.CreateVoiceRoomScreen
import com.sundram.weplay.ui.screens.MeSettingsScreen
import com.sundram.weplay.ui.screens.RankingScreen
import com.sundram.weplay.ui.screens.UserProfileDetailScreen
import com.sundram.weplay.ui.screens.UserProfileScreen
import com.sundram.weplay.ui.screens.BackpackScreen
import com.sundram.weplay.ui.screens.DiscoverScreen
import com.sundram.weplay.ui.screens.FamilyScreen
import com.sundram.weplay.ui.screens.GiftsScreen
import com.sundram.weplay.ui.screens.LiveVoiceRoomScreen
import com.sundram.weplay.ui.screens.LudoScreen
import com.sundram.weplay.ui.screens.MeScreen
import com.sundram.weplay.ui.screens.MessagesScreen
import com.sundram.weplay.ui.screens.ProfileEditScreen
import com.sundram.weplay.ui.screens.SpyScreen
import com.sundram.weplay.ui.screens.TopUpScreen
import com.sundram.weplay.ui.screens.settings.AccountSettingsScreen
import com.sundram.weplay.ui.screens.settings.BlockedUsersScreen
import com.sundram.weplay.ui.screens.settings.DndScheduleScreen
import com.sundram.weplay.ui.screens.settings.GeneralSettingsScreen
import com.sundram.weplay.ui.screens.settings.HostEarningsScreen
import com.sundram.weplay.ui.screens.settings.NotificationSettingsScreen
import com.sundram.weplay.ui.screens.settings.PaymentMethodsScreen
import com.sundram.weplay.ui.screens.settings.PrivacySettingsScreen
import com.sundram.weplay.ui.screens.settings.ReportHistoryScreen
import com.sundram.weplay.ui.screens.settings.SettingsHomeScreen
import com.sundram.weplay.ui.screens.settings.SupportSettingsScreen
import com.sundram.weplay.ui.screens.settings.TransactionsScreen
import com.sundram.weplay.ui.screens.settings.VoiceSettingsScreen
import com.sundram.weplay.ui.screens.settings.WalletSettingsScreen
import com.sundram.weplay.ui.screens.GameLobbyScreen
import com.sundram.weplay.ui.screens.VoiceRoomListScreen
import com.sundram.weplay.ui.theme.ThemeMode
import com.sundram.weplay.ui.theme.WePlayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeMode.load(this)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
            } else {
                SystemBarStyle.dark(AndroidColor.argb(0xB3, 0, 0, 0))
            },
        )
        setContent {
            val darkTheme = ThemeMode.isDark(isSystemInDarkTheme())
            WePlayTheme(darkTheme = darkTheme) {
                ApplySystemBarIcons(darkTheme)
                val context = LocalContext.current
                val store = remember { ProfileStore(context) }
                var profile by remember { mutableStateOf(store.load()) }

                val current = profile
                if (current == null) {
                    AuthScreen { created ->
                        store.save(created)
                        profile = created
                    }
                } else {
                    WePlayApp(
                        profile = current,
                        onSaveProfile = { updated ->
                            store.save(updated)
                            profile = updated
                        },
                        onLogout = {
                            store.clear()
                            ThemeMode.load(this@MainActivity)
                            profile = null
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ApplySystemBarIcons(darkTheme: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
}

@Composable
private fun WePlayApp(
    profile: UserProfile,
    onSaveProfile: (UserProfile) -> Unit,
    onLogout: () -> Unit,
) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBottom = route in Routes.tabs

    val context = LocalContext.current
    val profileState = rememberUpdatedState(profile)
    val store = remember(profile.id) { MessagingStore(context) { profileState.value } }
    var msgUnread by remember(profile.id) { mutableStateOf(store.unreadTotal()) }

    Scaffold(
        bottomBar = {
            if (showBottom) WePlayBottomBar(nav, route, msgBadge = msgUnread)
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
    ) { pad ->
        NavHost(
            navController = nav,
            startDestination = Routes.HUB,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Routes.HUB) {
                GameLobbyScreen(nav, pad, profile)
            }
            composable(Routes.ROOM) {
                VoiceRoomListScreen(nav, pad)
            }
            composable(Routes.ROOM_DETAIL) { entry ->
                LiveVoiceRoomScreen(nav, entry.arguments?.getString("roomId"), profile)
            }
            composable(Routes.DISCOVER) {
                DiscoverScreen(nav, pad)
            }
            composable(Routes.MSG) {
                MessagesScreen(nav, pad, store, onUnreadChange = { msgUnread = it })
            }
            composable(Routes.CHAT) { entry ->
                ChatScreen(
                    nav = nav,
                    userId = entry.arguments?.getString("userId") ?: "",
                    store = store,
                    profile = profile,
                    onSaveCoins = { newBal ->
                        val updated = profile.copy(coins = newBal)
                        onSaveProfile(updated)
                    },
                )
            }
            composable(Routes.USER) { entry ->
                UserProfileScreen(
                    nav,
                    entry.arguments?.getString("userId") ?: "",
                    store,
                )
            }
            composable(Routes.USER_DETAIL) { entry ->
                UserProfileDetailScreen(
                    nav = nav,
                    userId = entry.arguments?.getString("userId") ?: "",
                )
            }
            composable(Routes.RANKING) {
                RankingScreen(nav = nav)
            }
            composable(Routes.ME_SETTINGS) {
                MeSettingsScreen(nav = nav)
            }
            composable(Routes.CREATE_ROOM) {
                CreateVoiceRoomScreen(nav = nav)
            }
            composable(Routes.ME) {
                MeScreen(nav, pad, profile, onLogout)
            }
            composable(Routes.LUDO) {
                LudoScreen(nav)
            }
            composable(Routes.SPY) {
                SpyScreen(nav)
            }
            composable(Routes.PROFILE_EDIT) {
                ProfileEditScreen(nav, profile, onSaveProfile)
            }
            composable(Routes.GIFTS) {
                GiftsScreen(nav, profile)
            }
            composable(Routes.FAMILY) {
                FamilyScreen(nav, profile, onSaveProfile)
            }
            composable(Routes.BACKPACK) {
                BackpackScreen(nav, profile, onSaveProfile)
            }
            composable(Routes.TOPUP) {
                TopUpScreen(nav, profile, onSaveProfile)
            }
            composable(Routes.SETTINGS) {
                SettingsHomeScreen(nav, profile)
            }
            composable(Routes.ACCOUNT_SETTINGS) {
                AccountSettingsScreen(nav, profile, onSaveProfile, onLogout)
            }
            composable(Routes.PRIVACY_SETTINGS) {
                PrivacySettingsScreen(nav, profile)
            }
            composable(Routes.BLOCKED_USERS) {
                BlockedUsersScreen(nav, profile)
            }
            composable(Routes.REPORT_HISTORY) {
                ReportHistoryScreen(nav, profile)
            }
            composable(Routes.NOTIFICATION_SETTINGS) {
                NotificationSettingsScreen(nav)
            }
            composable(Routes.DND_SCHEDULE) {
                DndScheduleScreen(nav)
            }
            composable(Routes.VOICE_SETTINGS) {
                VoiceSettingsScreen(nav)
            }
            composable(Routes.GENERAL_SETTINGS) {
                GeneralSettingsScreen(nav)
            }
            composable(Routes.WALLET_SETTINGS) {
                WalletSettingsScreen(nav, profile)
            }
            composable(Routes.WALLET_TRANSACTIONS) {
                TransactionsScreen(nav, profile)
            }
            composable(Routes.PAYMENT_METHODS) {
                PaymentMethodsScreen(nav, profile)
            }
            composable(Routes.HOST_EARNINGS) {
                HostEarningsScreen(nav)
            }
            composable(Routes.SUPPORT_SETTINGS) {
                SupportSettingsScreen(nav, profile)
            }
            composable(Routes.ABOUT) {
                AboutScreen(nav)
            }
        }
    }
}
