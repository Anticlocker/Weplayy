package com.sundram.weplay.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.sundram.weplay.ui.theme.Stroke
import com.sundram.weplay.ui.theme.TextBody
import com.sundram.weplay.ui.theme.TextMain
import com.sundram.weplay.ui.theme.TextMuted

private val RankingBg = Color(0xFF6A5ACD)
private val TabActive = Color.White
private val TabInactive = Color(0xFFB8B8D8)

private data class RankUser(
    val rank: Int,
    val name: String,
    val charm: Int,
    val avatarRes: Int,
    val crown: Int = 0,
)

private val top3 = listOf(
    RankUser(rank = 2, name = "KUSH BO...", charm = 667288, avatarRes = R.drawable.ic_users, crown = 2),
    RankUser(rank = 1, name = "D", charm = 834027, avatarRes = R.drawable.ic_users, crown = 1),
    RankUser(rank = 3, name = "NEXUS", charm = 588424, avatarRes = R.drawable.ic_users, crown = 3),
)

private val rankList = listOf(
    RankUser(rank = 4, name = "HItensionl", charm = 416822, avatarRes = R.drawable.ic_users),
    RankUser(rank = 5, name = "ZULFIQAR", charm = 377365, avatarRes = R.drawable.ic_users),
    RankUser(rank = 6, name = "ghan", charm = 373752, avatarRes = R.drawable.ic_users),
    RankUser(rank = 7, name = "Mr.Santy", charm = 364403, avatarRes = R.drawable.ic_users),
    RankUser(rank = 8, name = "Jazzy", charm = 339240, avatarRes = R.drawable.ic_users),
    RankUser(rank = 9, name = "Solace", charm = 303194, avatarRes = R.drawable.ic_users),
)

/** Ranking screen with top-3 banner cards and scrollable list below. */
@Composable
fun RankingScreen(
    nav: NavHostController,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RankingBg)
            .verticalScroll(rememberScrollState()),
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 12.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_back),
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(40.dp).clickable { nav.popBackStack() },
            )
            Spacer(Modifier.weight(1f))
            Text(
                "Ranking",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Icon(
                painter = painterResource(R.drawable.ic_theme),
                contentDescription = "Help",
                tint = Color.White,
                modifier = Modifier.size(40.dp).clickable {},
            )
        }

        Spacer(Modifier.height(16.dp))

        // Category tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CategoryTab("Popularity", active = true)
            CategoryTab("VIP", active = false)
            CategoryTab("Couple", active = false)
            CategoryTab("Room", active = false)
            CategoryTab("BFF", active = false)
            CategoryTab("Family", active = false)
        }

        Spacer(Modifier.height(14.dp))

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip("Today", active = false)
            FilterChip("Yesterday", active = true)
            FilterChip("Celebrity", active = false)
            FilterChip("Annual", active = false)
            FilterChip("Global", active = false, showArrow = true)
        }

        Spacer(Modifier.height(18.dp))

        // Top 3 cards
        TopThreeCards()

        Spacer(Modifier.height(20.dp))

        // Rank list
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            rankList.forEach { user ->
                RankListItem(user)
                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(24.dp))

        // Bottom current user bar
        BottomUserBar()
    }
}

@Composable
private fun CategoryTab(label: String, active: Boolean) {
    Text(
        label,
        color = if (active) TabActive else TabInactive,
        fontSize = 16.sp,
        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
    )
}

@Composable
private fun FilterChip(label: String, active: Boolean, showArrow: Boolean = false) {
    Row(
        modifier = Modifier
            .background(
                if (active) Color.White else Color(0x33FFFFFF),
                RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .clickable {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = if (active) RankingBg else Color.White,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
        )
        if (showArrow) {
            Spacer(Modifier.width(4.dp))
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun TopThreeCards() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TopCard(user = top3[0], positionColor = Color(0xFF5D9CEC), modifier = Modifier.weight(1f))
        TopCard(user = top3[1], positionColor = Color(0xFF2ECC71), modifier = Modifier.weight(1f))
        TopCard(user = top3[2], positionColor = Color(0xFFD4A017), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TopCard(user: RankUser, positionColor: Color, modifier: Modifier = Modifier) {
    val gradient = when (user.rank) {
        1 -> Brush.linearGradient(listOf(Color(0xFF2ECC71), Color(0xFF27AE60)))
        2 -> Brush.linearGradient(listOf(Color(0xFF5D9CEC), Color(0xFF3498DB)))
        else -> Brush.linearGradient(listOf(Color(0xFFD4A017), Color(0xFFB7950B)))
    }

    Box(
        modifier = modifier
            .height(240.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(gradient),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Crown badge
            Box(
                Modifier
                    .size(32.dp)
                    .background(Gold, CircleShape)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    user.crown.toString(),
                    color = OnBrand,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            // Avatar circle
            Box(
                Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color.White, CircleShape)
                    .border(3.dp, positionColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(user.avatarRes),
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(56.dp),
                )
            }
            // Name
            Text(
                user.name,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            // Charm score
            Text(
                user.charm.toString(),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun RankListItem(user: RankUser) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Rank number
        Text(
            user.rank.toString(),
            color = TextMain,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(24.dp),
        )
        Spacer(Modifier.width(10.dp))
        // Avatar
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(CyanBrand, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(user.avatarRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        // Name
        Text(
            user.name,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            painter = painterResource(R.drawable.ic_crown),
            contentDescription = null,
            tint = Gold,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.weight(1f))
        // Charm score
        Column(horizontalAlignment = Alignment.End) {
            Text(
                user.charm.toString(),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text("Charm", color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun BottomUserBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF8D6E63), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "0",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                "Osama bin Moham...",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text("Charm 0", color = TextMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .height(44.dp)
                .padding(horizontal = 16.dp)
                .background(CyanBrand, RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Go Receive Gift",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
