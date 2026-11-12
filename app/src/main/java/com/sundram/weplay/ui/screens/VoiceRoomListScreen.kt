package com.sundram.weplay.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.sundram.weplay.data.Demo
import com.sundram.weplay.data.RoomCard
import com.sundram.weplay.ui.components.CoverThumb
import com.sundram.weplay.ui.components.tagColors
import com.sundram.weplay.ui.nav.Routes
import com.sundram.weplay.ui.theme.CyanBrand
import com.sundram.weplay.ui.theme.CoverGlass
import com.sundram.weplay.ui.theme.CoverGlassDeep
import com.sundram.weplay.ui.theme.Gold
import com.sundram.weplay.ui.theme.MedalBronze
import com.sundram.weplay.ui.theme.MedalSilver
import com.sundram.weplay.ui.theme.PageWhite
import com.sundram.weplay.ui.theme.PurpleHot
import com.sundram.weplay.ui.theme.PurpleHotDeep
import com.sundram.weplay.ui.theme.TextMain
import com.sundram.weplay.ui.theme.TextMuted

private val PageTabs = listOf("Related", "All", "Popular")
private val CategoryTabs = listOf("All", "Casual", "Games", "KTV", "Deduction")

@Composable
fun VoiceRoomListScreen(nav: NavHostController, pad: PaddingValues) {
    var pageTab by rememberSaveable { mutableStateOf("All") }
    var category by rememberSaveable { mutableStateOf("All") }
    val rooms = Demo.rooms.filter { category == "All" || it.tag == category }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageWhite)
            .statusBarsPadding()
            .padding(pad),
    ) {
        PageHeader(nav, pageTab) { pageTab = it }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { HotRoomBanner(nav) }
            item { CategoryRow(category) { category = it } }
            items(rooms, key = { it.id }) { room ->
                RoomRow(room) { nav.navigate(Routes.ROOM_DETAIL.replace("{roomId}", room.id)) }
            }
        }
    }
}

@Composable
private fun PageHeader(nav: NavHostController, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PageTabs.forEach { tab ->
                val active = tab == selected
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        tab,
                        color = if (active) TextMain else TextMuted,
                        fontSize = 19.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.clickable { onSelect(tab) },
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .size(7.dp)
                            .background(
                                if (active) CyanBrand else Color.Transparent,
                                CircleShape
                            )
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = "Search",
            tint = TextMain,
            modifier = Modifier.size(23.dp),
        )
        Spacer(Modifier.width(14.dp))
        Box(
            Modifier
                .size(30.dp)
                .background(CyanBrand, CircleShape)
                .clickable { nav.navigate(Routes.CREATE_ROOM) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_plus),
                contentDescription = "Create room",
                tint = Color.White,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

@Composable
private fun HotRoomBanner(nav: NavHostController) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(124.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(PurpleHot, PurpleHotDeep))),
    ) {
        Box(
            Modifier
                .size(120.dp)
                .background(CoverGlass, CircleShape)
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
        )
        Box(
            Modifier
                .size(70.dp)
                .background(CoverGlass, CircleShape)
                .align(Alignment.TopStart)
                .padding(start = 30.dp, top = 10.dp)
        )
        Column(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 18.dp),
        ) {
            Text("HOT ROOM", color = Gold, fontSize = 25.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(7.dp))
            Text(
                "View the ranking  >",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(CoverGlass, RoundedCornerShape(12.dp))
                    .clickable { nav.navigate(Routes.RANKING) }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            repeat(6) { i ->
                Box(
                    Modifier
                        .size(5.dp)
                        .background(
                            if (i == 0) Color.White else CoverGlassDeep,
                            CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(selected: String, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        items(CategoryTabs) { label ->
            val active = label == selected
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    label,
                    color = if (active) TextMain else TextMuted,
                    fontSize = 17.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.clickable { onSelect(label) },
                )
                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier
                        .width(18.dp)
                        .height(3.dp)
                        .background(if (active) CyanBrand else Color.Transparent, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Composable
private fun RoomRow(room: RoomCard, onClick: () -> Unit) {
    val (tagBg, tagFg) = tagColors(room.tag)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverThumb(room.title, Modifier.size(60.dp), corner = 14)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                room.title,
                color = TextMain,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    room.tag,
                    color = tagFg,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .background(tagBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
                Spacer(Modifier.width(7.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_users),
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text("${room.members}", color = TextMuted, fontSize = 11.sp)
                Spacer(Modifier.width(7.dp))
                Text(room.host, color = TextMuted, fontSize = 11.sp, maxLines = 1)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(Gold, MedalBronze, MedalSilver).forEach { medal ->
                Box(
                    Modifier
                        .size(15.dp)
                        .background(medal.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                )
            }
        }
    }
}
