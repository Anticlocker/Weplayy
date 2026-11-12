package com.sundram.weplay.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sundram.weplay.R
import com.sundram.weplay.core.EmptyState
import com.sundram.weplay.core.ErrorState
import com.sundram.weplay.core.LoadingState
import com.sundram.weplay.core.UiState
import com.sundram.weplay.data.Demo
import com.sundram.weplay.data.GameCard
import com.sundram.weplay.data.RoomCard
import com.sundram.weplay.data.UserProfile
import com.sundram.weplay.platform.content.HomeFeed
import com.sundram.weplay.platform.content.LocalContentRepository
import com.sundram.weplay.ui.components.CoverThumb
import com.sundram.weplay.ui.components.GameArtwork
import com.sundram.weplay.ui.components.LivePill
import com.sundram.weplay.ui.components.RoundIconButton
import com.sundram.weplay.ui.components.SectionHeader
import com.sundram.weplay.ui.components.TagPill
import com.sundram.weplay.ui.components.UserAvatar
import com.sundram.weplay.ui.nav.Routes
import com.sundram.weplay.ui.theme.BgSoft
import com.sundram.weplay.ui.theme.CardWhite
import com.sundram.weplay.ui.theme.ChipBg
import com.sundram.weplay.ui.theme.CoverGlass
import com.sundram.weplay.ui.theme.CoverText
import com.sundram.weplay.ui.theme.CoverTextDim
import com.sundram.weplay.ui.theme.CyanBrand
import com.sundram.weplay.ui.theme.HeroBlue
import com.sundram.weplay.ui.theme.HeroCyan
import com.sundram.weplay.ui.theme.OnBrand
import com.sundram.weplay.ui.theme.PinkGift
import com.sundram.weplay.ui.theme.PlayGold
import com.sundram.weplay.ui.theme.ScrimBadge
import com.sundram.weplay.ui.theme.ScrimCircle
import com.sundram.weplay.ui.theme.ScrimFade
import com.sundram.weplay.ui.theme.Stroke
import com.sundram.weplay.ui.theme.TagBlue
import com.sundram.weplay.ui.theme.TagBlueBg
import com.sundram.weplay.ui.theme.TagPurple
import com.sundram.weplay.ui.theme.TagPurpleBg
import com.sundram.weplay.ui.theme.Teal
import com.sundram.weplay.ui.theme.TealBg
import com.sundram.weplay.ui.theme.TextBody
import com.sundram.weplay.ui.theme.TextMain
import com.sundram.weplay.ui.theme.TextMuted
import java.util.Calendar

private val Filters = listOf("All", "Playable", "Hot", "Coming soon")

private data class QuickLink(val label: String, val icon: Int, val tint: Color, val bg: Color)

private val quickLinks = listOf(
    QuickLink("Ranking", R.drawable.ic_crown, TagPurple, TagPurpleBg),
    QuickLink("Tasks", R.drawable.ic_bell, TagBlue, TagBlueBg),
    QuickLink("Friends", R.drawable.ic_users, Teal, TealBg),
)

@Composable
fun WePlayHubScreen(nav: NavHostController, pad: PaddingValues, profile: UserProfile) {
    val repository = remember { LocalContentRepository() }
    var refresh by remember { mutableStateOf(0) }
    var feedState by remember { mutableStateOf<UiState<HomeFeed>>(UiState.Loading) }
    var filter by rememberSaveable { mutableStateOf("All") }
    var info by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(refresh) {
        feedState = UiState.Loading
        feedState = repository.homeFeed().fold(
            onSuccess = { feed ->
                if (feed.games.isEmpty() && feed.rooms.isEmpty()) UiState.Empty("No games or rooms are available right now.")
                else UiState.Success(feed)
            },
            onFailure = { UiState.Error(it.message ?: "Could not load home feed.", retryable = true) },
        )
    }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Hello"
    }

    when (val state = feedState) {
        UiState.Loading -> Column(Modifier.fillMaxSize().background(BgSoft).statusBarsPadding().padding(pad)) { LoadingState(Modifier.weight(1f)) }
        is UiState.Empty -> Column(Modifier.fillMaxSize().background(BgSoft).statusBarsPadding().padding(pad)) { EmptyState(state.message, Modifier.weight(1f)) }
        is UiState.Error -> Column(Modifier.fillMaxSize().background(BgSoft).statusBarsPadding().padding(pad)) { ErrorState(state.message, state.retryable, { refresh++ }, Modifier.weight(1f)) }
        is UiState.Success -> {
            val games = state.data.games.filter {
                when (filter) { "Playable" -> it.playable; "Hot" -> it.tag != null; "Coming soon" -> !it.playable; else -> true }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().background(BgSoft).statusBarsPadding().padding(pad),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    TopBar(profile = profile, greeting = greeting,
                        onSearch = { info = "Search" to "Search is not available yet." },
                        onAlerts = { info = "Notifications" to "No new notifications right now." })
                }
                item(span = { GridItemSpan(maxLineSpan) }) { HeroBanner() }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column { Spacer(Modifier.height(6.dp)); QuickRow { link ->
                        val body = when (link.label) {
                            "Ranking" -> "Leaderboards will appear when the server contract is connected."
                            "Tasks" -> "Daily tasks will appear when the server contract is connected."
                            else -> "Friends will appear when the server contract is connected."
                        }; info = link.label to body
                    } }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column { Spacer(Modifier.height(6.dp)); SectionHeader("Live rooms", "See all") { nav.navigate(Routes.ROOM) }
                        Spacer(Modifier.height(10.dp)); RoomsStrip(state.data.rooms) { room -> nav.navigate(Routes.ROOM_DETAIL.replace("{roomId}", room.id)) } }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column { Spacer(Modifier.height(6.dp)); GamesHeader(gamesCount = state.data.games.size, onGameRoom = { nav.navigate(Routes.ROOM) })
                        Spacer(Modifier.height(10.dp)); FilterRow(filter) { filter = it } }
                }
                if (games.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { EmptyState("No games match this filter.", Modifier.padding(top = 12.dp)) }
                } else {
                    items(games, key = { it.id }) { game ->
                        GameTile(game) { if (!game.playable) return@GameTile; when (game.id) { "ludo" -> nav.navigate(Routes.LUDO); "spy" -> nav.navigate(Routes.SPY) } }
                    }
                }
            }
        }
    }
    info?.let { (title, body) ->
        AlertDialog(onDismissRequest = { info = null }, containerColor = CardWhite,
            title = { Text(title, color = TextMain, fontWeight = FontWeight.Bold) },
            text = { Text(body, color = TextBody, fontSize = 13.sp, lineHeight = 19.sp) },
            confirmButton = { TextButton(onClick = { info = null }) { Text("Got it", color = CyanBrand, fontWeight = FontWeight.Bold) } })
    }
}

@Composable
private fun TopBar(profile: UserProfile, greeting: String, onSearch: () -> Unit, onAlerts: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(profile.name, profile.photoPath, size = 44.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(greeting, color = TextMuted, fontSize = 11.sp)
                Spacer(Modifier.width(4.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_verified),
                    contentDescription = null,
                    tint = CyanBrand,
                    modifier = Modifier.size(13.dp),
                )
            }
            Text(
                profile.name,
                color = TextMain,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        RoundIconButton(
            painter = painterResource(R.drawable.ic_search),
            onClick = onSearch,
            contentDescription = "Search",
        )
        Spacer(Modifier.width(8.dp))
        Box {
            RoundIconButton(
                painter = painterResource(R.drawable.ic_bell),
                onClick = onAlerts,
                contentDescription = "Notifications",
            )
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(9.dp)
                    .background(PinkGift, CircleShape)
                    .border(2.dp, BgSoft, CircleShape)
            )
        }
    }
}

@Composable
private fun HeroBanner() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(HeroCyan, HeroBlue))),
    ) {
        Box(
            Modifier
                .size(150.dp)
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp)
                .background(CoverGlass, CircleShape)
        )
        Column(Modifier.padding(16.dp).fillMaxSize()) {
            Text(
                "DAILY COMBO",
                color = OnBrand,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(CoverGlass, RoundedCornerShape(5.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Play 3 games,\nearn 500 coins",
                color = OnBrand,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 23.sp,
            )
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(3) { i ->
                    Box(
                        Modifier
                            .width(24.dp)
                            .height(5.dp)
                            .background(
                                if (i < 2) OnBrand else OnBrand.copy(alpha = 0.27f),
                                RoundedCornerShape(3.dp),
                            )
                    )
                    Spacer(Modifier.width(5.dp))
                }
                Text("2/3 today", color = OnBrand.copy(alpha = 0.8f), fontSize = 10.sp)
            }
        }
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp)
                .size(72.dp)
                .background(CoverGlass, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_star),
                contentDescription = null,
                tint = OnBrand,
                modifier = Modifier.size(38.dp),
            )
        }
    }
}

@Composable
private fun QuickRow(onInfo: (QuickLink) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        quickLinks.forEach { link ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(CardWhite, RoundedCornerShape(16.dp))
                    .border(1.dp, Stroke, RoundedCornerShape(16.dp))
                    .clickable { onInfo(link) }
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(46.dp)
                        .background(link.bg, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(link.icon),
                        contentDescription = link.label,
                        tint = link.tint,
                        modifier = Modifier.size(23.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(link.label, color = TextBody, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun GamesHeader(gamesCount: Int, onGameRoom: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Games", color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(6.dp))
        Text("$gamesCount titles", color = TextMuted, fontSize = 11.sp)
        Spacer(Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(ChipBg, RoundedCornerShape(20.dp))
                .clickable(onClick = onGameRoom)
                .padding(horizontal = 13.dp, vertical = 8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_nav_games),
                contentDescription = null,
                tint = TextMain,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("Game Room", color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun RoomsStrip(rooms: List<RoomCard>, onOpen: (RoomCard) -> Unit) {
    if (rooms.isEmpty()) {
        EmptyState("No live rooms available right now.")
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(rooms.size, key = { rooms[it].id }) { index ->
            RoomMiniCard(rooms[index], onOpen)
        }
    }
}

@Composable
private fun RoomMiniCard(room: RoomCard, onOpen: (RoomCard) -> Unit) {
    Column(
        modifier = Modifier
            .width(164.dp)
            .clickable { onOpen(room) },
    ) {
        Box {
            CoverThumb(room.title, Modifier.fillMaxWidth().height(84.dp))
            Box(Modifier.padding(8.dp)) { LivePill() }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .background(ScrimBadge, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_users),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text("${room.members}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            room.title,
            color = TextMain,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "${room.host} · ${room.tag}",
            color = TextMuted,
            fontSize = 10.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun FilterRow(selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Filters.forEach { label ->
            val active = label == selected
            Text(
                label,
                color = if (active) OnBrand else TextBody,
                fontSize = 12.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier
                    .background(
                        if (active) CyanBrand else CardWhite,
                        RoundedCornerShape(20.dp),
                    )
                    .border(1.dp, if (active) CyanBrand else Stroke, RoundedCornerShape(20.dp))
                    .clickable { onSelect(label) }
                    .padding(horizontal = 13.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
private fun GameTile(game: GameCard, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        GameArtwork(game.id, Modifier.fillMaxSize(), corner = 18)
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(74.dp)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, ScrimFade))
                )
        )
        if (game.tag != null) {
            Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) {
                TagPill(
                    game.tag,
                    bg = if (game.tag == "HOT") PinkGift else CyanBrand,
                )
            }
        }
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(11.dp),
        ) {
            Text(game.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(game.desc, color = CoverText, fontSize = 10.sp, maxLines = 1)
        }
        if (game.playable) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .size(26.dp)
                    .background(ScrimCircle, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_play),
                    contentDescription = null,
                    tint = PlayGold,
                    modifier = Modifier.size(14.dp),
                )
            }
        } else {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .background(ScrimBadge, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text("SOON", color = CoverTextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
