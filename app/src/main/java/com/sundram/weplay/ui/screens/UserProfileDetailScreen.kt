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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

private val ProfileCover = Color(0xFF6E5244)
private val ProfileCardBg = Color(0xFFFFFFFF)
private val BffCardBg = Color(0xFFFDE8B8)
private val LevelBadge = Color(0xFFFF6512)

private data class BffCard(
    val title: String,
    val icon: Int,
)

private val bffCards = listOf(
    BffCard("Bro and sis", R.drawable.ic_heart),
    BffCard("Besties", R.drawable.ic_gift),
    BffCard("Bro", R.drawable.ic_bolt),
    BffCard("Confidant", R.drawable.ic_star),
)

@Composable
fun UserProfileDetailScreen(
    nav: NavHostController,
    userId: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageWhite)
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
                modifier = Modifier
                    .size(40.dp)
                    .clickable { nav.popBackStack() },
            )
            Spacer(Modifier.weight(1f))
            Icon(
                painter = painterResource(R.drawable.ic_send),
                contentDescription = "Share",
                tint = Color.White,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = "Settings",
                tint = Color.White,
                modifier = Modifier.size(40.dp),
            )
        }

        // Cover area - full width brown with big profile circle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(ProfileCover),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(110.dp)
                    .background(Color.White, CircleShape)
                    .border(3.dp, Color(0xFF5D4037), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "0",
                    color = ProfileCover,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Name row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Osama bin Mohamm",
                color = TextMain,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_verified),
                    contentDescription = null,
                    tint = CyanBrand,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
                Box(
                    Modifier
                        .background(LevelBadge, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Lv.1",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text("0", color = TextMuted, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(8.dp))

        // ID + copy + country
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "ID:225712899",
                    color = TextBody,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_plus),
                    contentDescription = "Copy ID",
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp).clickable {},
                )
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .padding(vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        Modifier
                            .size(20.dp)
                            .background(Color(0xFFFF9800), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_star),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                    Text("India", color = TextBody, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Menu items
        MenuRow("Moments", "0") { }
        Spacer(Modifier.height(1.dp))
        DividerLine()
        MenuRow("Gift Wall", null) { }
        Spacer(Modifier.height(1.dp))
        DividerLine()
        MenuRow("Signature", null) { }
        Spacer(Modifier.height(1.dp))
        DividerLine()
        MenuRow("BFF", null) { }
        Spacer(Modifier.height(1.dp))
        DividerLine()
        MenuRow("Guard", null) { }
        Spacer(Modifier.height(1.dp))
        DividerLine()
        MenuRow("Stats", null) { }

        Spacer(Modifier.height(20.dp))

        // Signature text
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Text(
                "Signature",
                color = TextMain,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "This person says nothing!",
                color = TextBody,
                fontSize = 14.sp,
            )
        }

        Spacer(Modifier.height(24.dp))

        // BFF section
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "BFF",
                    color = TextMain,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(bffCards, key = { it.title }) { card ->
                    BffCardItem(card)
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // Bottom action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .height(56.dp)
                    .background(PinkGift, RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_gift),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        "Send Gift",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(56.dp)
                    .background(CyanBrand, RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_send),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        "Chat",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun MenuRow(title: String, badge: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        badge?.let {
            Text(it, color = TextMuted, fontSize = 13.sp)
        } ?: Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(18.dp),
        )
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

@Composable
private fun BffCardItem(card: BffCard) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .background(BffCardBg, RoundedCornerShape(16.dp)),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(Gold.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(card.icon),
                contentDescription = null,
                tint = OnBrand,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            card.title,
            color = TextBody,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .size(32.dp)
                .background(Color.White, CircleShape)
                .border(1.dp, Stroke, CircleShape)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_plus),
                contentDescription = null,
                tint = CyanBrand,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
