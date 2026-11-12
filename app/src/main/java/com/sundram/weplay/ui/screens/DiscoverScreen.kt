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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sundram.weplay.R
import com.sundram.weplay.core.EmptyState
import com.sundram.weplay.core.ErrorState
import com.sundram.weplay.core.LoadingState
import com.sundram.weplay.core.UiState
import com.sundram.weplay.platform.content.DiscoverFeed
import com.sundram.weplay.platform.content.FeedEvent
import com.sundram.weplay.platform.content.FeedPost
import com.sundram.weplay.platform.content.LocalContentRepository
import com.sundram.weplay.ui.components.InitialsAvatar
import com.sundram.weplay.ui.components.SectionHeader
import com.sundram.weplay.ui.theme.ChipBg
import com.sundram.weplay.ui.theme.CyanDeep
import com.sundram.weplay.ui.theme.OrangeAccent
import com.sundram.weplay.ui.theme.PageWhite
import com.sundram.weplay.ui.theme.PinkGift
import com.sundram.weplay.ui.theme.Stroke
import com.sundram.weplay.ui.theme.TagBlue
import com.sundram.weplay.ui.theme.TagBlueBg
import com.sundram.weplay.ui.theme.TagGreen
import com.sundram.weplay.ui.theme.TagGreenBg
import com.sundram.weplay.ui.theme.TagOrange
import com.sundram.weplay.ui.theme.TagOrangeBg
import com.sundram.weplay.ui.theme.TextBody
import com.sundram.weplay.ui.theme.TextMain
import com.sundram.weplay.ui.theme.TextMuted

private data class ExploreCard(val label: String, val icon: Int, val tint: Color, val bg: Color)

private val exploreCards = listOf(
    ExploreCard("Games", R.drawable.ic_nav_games, TagOrange, TagOrangeBg),
    ExploreCard("Rooms", R.drawable.ic_nav_room, TagBlue, TagBlueBg),
    ExploreCard("Nearby", R.drawable.ic_users, TagGreen, TagGreenBg),
)

@Composable
fun DiscoverScreen(nav: NavHostController, pad: PaddingValues) {
    val repository = remember { LocalContentRepository() }
    var refresh by remember { mutableStateOf(0) }
    var state by remember { mutableStateOf<UiState<DiscoverFeed>>(UiState.Loading) }

    LaunchedEffect(refresh) {
        state = UiState.Loading
        state = repository.discoverFeed().fold(
            onSuccess = { feed ->
                if (feed.events.isEmpty() && feed.posts.isEmpty() && feed.trending.isEmpty()) {
                    UiState.Empty("No moments or updates are available right now.")
                } else {
                    UiState.Success(feed)
                }
            },
            onFailure = { UiState.Error(it.message ?: "Could not load discover feed.", retryable = true) },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageWhite)
            .statusBarsPadding()
            .padding(pad),
    ) {
        when (val current = state) {
            UiState.Loading -> LoadingState(Modifier.weight(1f))
            is UiState.Empty -> EmptyState(current.message, Modifier.weight(1f))
            is UiState.Error -> ErrorState(current.message, current.retryable, { refresh++ }, Modifier.weight(1f))
            is UiState.Success -> DiscoverContent(current.data)
        }
    }
}

@Composable
private fun DiscoverContent(feed: DiscoverFeed) {
    val events = feed.events
    val posts = feed.posts
    val trending = feed.trending

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text(
                "Discover",
                color = TextMain,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp),
            )
            Spacer(Modifier.height(14.dp))
            MomentsCard()
            Spacer(Modifier.height(12.dp))
            ExploreRow()
            Spacer(Modifier.height(16.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(trending) { tag ->
                    Text(
                        "#$tag",
                        color = TagBlue,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .background(TagBlueBg, RoundedCornerShape(20.dp))
                            .padding(horizontal = 11.dp, vertical = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            SectionHeader("Live updates", "Today")
            Spacer(Modifier.height(4.dp))
        }

        items(events) { event ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PageWhite)
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InitialsAvatar(event.who, size = 38.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(event.who, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(event.action, color = TextMuted, fontSize = 11.sp)
                }
                Text(event.time, color = TextMuted, fontSize = 10.sp)
            }
        }

        item {
            Spacer(Modifier.height(10.dp))
            SectionHeader("Moments", "Community")
            Spacer(Modifier.height(4.dp))
        }

        items(posts) { post ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(PageWhite, RoundedCornerShape(16.dp))
                    .border(1.dp, Stroke, RoundedCornerShape(16.dp))
                    .padding(13.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InitialsAvatar(post.who, size = 30.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(post.who, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                Text(post.text, color = TextBody, fontSize = 13.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Reaction(R.drawable.ic_heart, "${post.likes}", PinkGift)
                    Spacer(Modifier.width(16.dp))
                    Reaction(R.drawable.ic_comment, "${post.comments}", CyanDeep)
                }
            }
        }
    }
}

@Composable
private fun MomentsCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PageWhite, RoundedCornerShape(16.dp))
            .border(1.dp, Stroke, RoundedCornerShape(16.dp))
            .clickable { }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(30.dp)
                .background(ChipBg, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_star),
                contentDescription = null,
                tint = OrangeAccent,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text("Moments", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ExploreRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        exploreCards.forEach { card ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(PageWhite, RoundedCornerShape(16.dp))
                    .border(1.dp, Stroke, RoundedCornerShape(16.dp))
                    .clickable { }
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    painter = painterResource(card.icon),
                    contentDescription = card.label,
                    tint = card.tint,
                    modifier = Modifier.size(30.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(card.label, color = TextBody, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun Reaction(icon: Int, count: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(count, color = TextMuted, fontSize = 11.sp)
    }
}
