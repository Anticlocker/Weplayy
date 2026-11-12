package com.sundram.weplay.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sundram.weplay.R
import com.sundram.weplay.data.UserProfile
import com.sundram.weplay.ui.nav.Routes

// ── WePlay Exact Color Palette ────────────────────────────────────────────────
private val WePlayBg         = Color(0xFFFFFFFF)
private val WePlayNavy       = Color(0xFF151C33)
private val WePlaySubText    = Color(0xFF6B7280)
private val CoinPillBg       = Color(0xFFF5F6F8)
private val GoldCoinColor    = Color(0xFFFFB300)
private val OrangePlusColor  = Color(0xFFFF6D00)
private val RedDotColor      = Color(0xFFFF3B30)

// ── Card Rounded Corner Radius ────────────────────────────────────────────────
private val CardCorner = RoundedCornerShape(20.dp)
private val SmallCardCorner = RoundedCornerShape(18.dp)

// ─────────────────────────────────────────────────────────────────────────────
//  1. Top Bar: Profile Avatar, Coin Pill, Duelo, Events
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WePlayTopBar(nav: NavHostController, profile: UserProfile) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // User Profile Avatar (circular photo with subtle outline)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFFE0E7FF))
                .clickable { nav.navigate(Routes.ME) },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                // Warm photo background
                drawCircle(Color(0xFF64B5F6), radius = size.width * 0.5f, center = Offset(cx, cy))
                // Head
                drawCircle(Color(0xFFFFCCBC), radius = size.width * 0.32f, center = Offset(cx, cy - size.height * 0.05f))
                // Hair
                drawArc(
                    Color(0xFF3E2723), -160f, 140f, true,
                    topLeft = Offset(cx - size.width * 0.34f, cy - size.height * 0.44f),
                    size = Size(size.width * 0.68f, size.height * 0.55f),
                )
                // Beard
                drawArc(
                    Color(0xFF3E2723), 20f, 140f, false,
                    topLeft = Offset(cx - size.width * 0.16f, cy + size.height * 0.06f),
                    size = Size(size.width * 0.32f, size.height * 0.18f),
                    style = Stroke(size.width * 0.04f, cap = StrokeCap.Round),
                )
                // Eyes
                drawCircle(Color(0xFF212121), radius = size.width * 0.035f, center = Offset(cx - size.width * 0.1f, cy - size.height * 0.08f))
                drawCircle(Color(0xFF212121), radius = size.width * 0.035f, center = Offset(cx + size.width * 0.1f, cy - size.height * 0.08f))
                // Blue collar shirt
                drawArc(
                    Color(0xFF1565C0), 0f, 180f, true,
                    topLeft = Offset(cx - size.width * 0.36f, cy + size.height * 0.2f),
                    size = Size(size.width * 0.72f, size.height * 0.4f),
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        // Coin Capsule: [ CoinIcon  Amount  + ]
        Box(
            modifier = Modifier
                .height(34.dp)
                .background(CoinPillBg, RoundedCornerShape(17.dp))
                .clickable { nav.navigate(Routes.TOPUP) }
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Gold Lucky Coin
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(GoldCoinColor, CircleShape)
                        .border(1.dp, Color(0xFFFFD54F), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .background(Color(0xFFB78103)),
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${profile.coins}",
                    color = WePlayNavy,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(6.dp))
                // Orange + button
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(OrangePlusColor, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Right side: Duelo & Events
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Duelo (Game Duel icon with red notification dot)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { nav.navigate(Routes.ROOM) },
            ) {
                Box(modifier = Modifier.size(36.dp)) {
                    // Game app tile
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF1E3A8A), Color(0xFF0284C7)))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("⚔️", fontSize = 16.sp)
                    }
                    // Red notification dot
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(7.dp)
                            .background(RedDotColor, CircleShape)
                            .border(1.dp, Color.White, CircleShape),
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Duelo",
                    color = WePlaySubText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
            }

            // Events (Green Star badge)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { nav.navigate(Routes.DISCOVER) },
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0xFFE8F5E9), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("⭐", fontSize = 20.sp)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Events",
                    color = WePlaySubText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  2. Quick Actions: Ranking, Tasks, Friends (Exact WePlay Icons)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WePlayShortcutsRow(nav: NavHostController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        // 1. Ranking
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { nav.navigate(Routes.RANKING) },
        ) {
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Gold ring with podium illustration
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF263238)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🏆", fontSize = 20.sp)
                }
                // Gold Crown attached to top-left
                Text(
                    "👑",
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 1.dp, y = (-2).dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Ranking",
                color = WePlayNavy,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        // 2. Tasks (Purple-to-pink folded gift pouch with red dot)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { nav.navigate(Routes.ME) },
        ) {
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Pouch shape
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF8E24AA), Color(0xFFE91E63)))),
                    contentAlignment = Alignment.Center,
                ) {
                    // White folded flap
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .width(28.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                            .background(Color.White.copy(alpha = 0.9f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(4.dp).background(Color(0xFFE91E63), CircleShape))
                    }
                }
                // Red notification badge dot
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(8.dp)
                        .background(RedDotColor, CircleShape)
                        .border(1.dp, Color.White, CircleShape),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Tasks",
                color = WePlayNavy,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        // 3. Friends (Stylized avatar icon)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { nav.navigate(Routes.MSG) },
        ) {
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Dual tone avatar: blue hair + peach skin
                Canvas(Modifier.size(46.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    // Warm peach face
                    drawCircle(Color(0xFFFFCC80), radius = size.width * 0.44f, center = Offset(cx, cy))
                    // Gradient blue hair top half
                    drawArc(
                        Brush.verticalGradient(listOf(Color(0xFF42A5F5), Color(0xFF1E88E5))),
                        -180f, 180f, true,
                        topLeft = Offset(0f, 0f),
                        size = Size(size.width, size.height * 0.7f),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Friends",
                color = WePlayNavy,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  3. Games Header: "Games" + "🚪 Game Rooms"
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WePlayGamesHeader(nav: NavHostController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Games",
            color = WePlayNavy,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { nav.navigate(Routes.ROOM) }
                .padding(vertical = 4.dp, horizontal = 2.dp),
        ) {
            Text("🚪", fontSize = 14.sp)
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Game Rooms",
                color = Color(0xFF2E384D),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  4. Large Featured Game Banner (Who's the Spy, Liar's Party)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun WePlayFeaturedBanner(
    background: Brush,
    title: String,
    drawIllustration: DrawScope.(Float, Float) -> Unit,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(98.dp)
            .shadow(4.dp, CardCorner)
            .clip(CardCorner)
            .background(background)
            .clickable(onClick = onClick),
    ) {
        // Diagonal dynamic backdrop glow
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val lightPath = Path().apply {
                moveTo(w * 0.45f, 0f)
                lineTo(w, 0f)
                lineTo(w, h)
                lineTo(w * 0.35f, h)
                close()
            }
            drawPath(lightPath, Color.White.copy(alpha = 0.08f))
        }

        // Title on left
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 18.dp)
                .fillMaxWidth(0.55f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 22.sp,
            )
        }

        // Illustration on right
        Canvas(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(110.dp)
                .padding(end = 4.dp),
        ) {
            drawIllustration(size.width, size.height)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  5. Two-Column Grid Game Card (Crazy Alpaca, Space Werewolf, Bingo, etc.)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun WePlayGridCard(
    background: Brush,
    title: String,
    badgeText: String? = null,
    badgeColor: Color = Color(0xFFFFB300),
    badgeTextColor: Color = Color(0xFF5D4037),
    drawIllustration: DrawScope.(Float, Float) -> Unit,
    extraBadge: @Composable (() -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .height(116.dp)
            .shadow(3.dp, SmallCardCorner)
            .clip(SmallCardCorner)
            .background(background)
            .clickable(onClick = onClick),
    ) {
        // Badge on top-left (e.g. 🔥 Popular, New)
        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 8.dp)
                    .background(badgeColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = badgeText,
                    color = badgeTextColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Title
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp, top = if (badgeText != null) 12.dp else 0.dp)
                .fillMaxWidth(0.52f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp,
            )
        }

        // Illustration
        Canvas(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(76.dp)
                .padding(end = 4.dp),
        ) {
            drawIllustration(size.width, size.height)
        }

        // Extra Badge at bottom right
        if (extraBadge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 6.dp, bottom = 6.dp),
            ) {
                extraBadge()
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  6. Game Illustrations Matching Exact WePlay Screenshot
// ─────────────────────────────────────────────────────────────────────────────

// Who's the Spy: Two Spy Girls
private fun DrawScope.drawWhoIsTheSpyCharacters(w: Float, h: Float) {
    val cx = w * 0.5f; val cy = h * 0.5f
    // Girl 1 with silver-pink hair
    drawCircle(Color(0xFFFFCDD2), radius = w * 0.18f, center = Offset(cx - w * 0.16f, cy - h * 0.08f))
    drawArc(Color(0xFFE1BEE7), -180f, 180f, true, topLeft = Offset(cx - w * 0.36f, cy - h * 0.32f), size = Size(w * 0.4f, h * 0.44f))
    drawCircle(Color(0xFF212121), radius = w * 0.03f, center = Offset(cx - w * 0.22f, cy - h * 0.08f))
    drawCircle(Color(0xFF212121), radius = w * 0.03f, center = Offset(cx - w * 0.1f, cy - h * 0.08f))

    // Girl 2 with heart sunglasses & blue hair
    drawCircle(Color(0xFFFFE0B2), radius = w * 0.2f, center = Offset(cx + w * 0.18f, cy + h * 0.02f))
    drawArc(Color(0xFF29B6F6), -180f, 180f, true, topLeft = Offset(cx - w * 0.04f, cy - h * 0.22f), size = Size(w * 0.44f, h * 0.44f))
    // Pink heart sunglasses
    drawRoundRect(Color(0xFFFF4081), topLeft = Offset(cx + w * 0.06f, cy - h * 0.04f), size = Size(w * 0.26f, h * 0.1f), cornerRadius = CornerRadius(w * 0.04f))
    drawCircle(Color.White, radius = w * 0.02f, center = Offset(cx + w * 0.12f, cy - h * 0.02f))
    drawCircle(Color.White, radius = w * 0.02f, center = Offset(cx + w * 0.24f, cy - h * 0.02f))
}

// Liar's Party: Animal Mafia Card Table (Crocodile, Cat, Poodle, Eagle)
private fun DrawScope.drawLiarsPartyCharacters(w: Float, h: Float) {
    val cx = w * 0.5f; val cy = h * 0.55f
    // Wooden Poker Table Arc
    drawOval(Color(0xFFD7CCC8), topLeft = Offset(0f, cy + h * 0.05f), size = Size(w, h * 0.4f))
    drawOval(Color(0xFFBCAAA4), topLeft = Offset(0f, cy + h * 0.05f), size = Size(w, h * 0.4f), style = Stroke(w * 0.03f))

    // Green Crocodile (center left) waving
    drawRoundRect(Color(0xFF43A047), topLeft = Offset(cx - w * 0.32f, cy - h * 0.35f), size = Size(w * 0.28f, h * 0.35f), cornerRadius = CornerRadius(w * 0.08f))
    drawCircle(Color(0xFFFFD54F), radius = w * 0.04f, center = Offset(cx - w * 0.22f, cy - h * 0.25f))
    // Snout
    drawRoundRect(Color(0xFF2E7D32), topLeft = Offset(cx - w * 0.38f, cy - h * 0.26f), size = Size(w * 0.2f, h * 0.14f), cornerRadius = CornerRadius(w * 0.04f))

    // White Cat Girl (center)
    drawCircle(Color(0xFFFFF9C4), radius = w * 0.14f, center = Offset(cx - w * 0.02f, cy - h * 0.15f))
    drawCircle(Color(0xFF212121), radius = w * 0.025f, center = Offset(cx - w * 0.06f, cy - h * 0.16f))
    drawCircle(Color(0xFF212121), radius = w * 0.025f, center = Offset(cx + w * 0.02f, cy - h * 0.16f))

    // Poodle & Eagle (right side)
    drawCircle(Color(0xFFFFE0B2), radius = w * 0.13f, center = Offset(cx + w * 0.2f, cy - h * 0.18f))
    drawCircle(Color(0xFFEEEEEE), radius = w * 0.13f, center = Offset(cx + w * 0.38f, cy - h * 0.25f))
}

// Crazy Alpaca: Cute Fluffy White Alpaca with Blue Collar
private fun DrawScope.drawCrazyAlpaca(w: Float, h: Float) {
    val cx = w * 0.52f; val cy = h * 0.48f
    // Fluffy white alpaca head
    drawCircle(Color.White, radius = w * 0.32f, center = Offset(cx, cy))
    // Ears
    drawRoundRect(Color(0xFF424242), topLeft = Offset(cx - w * 0.3f, cy - h * 0.38f), size = Size(w * 0.14f, h * 0.24f), cornerRadius = CornerRadius(w * 0.06f))
    drawRoundRect(Color(0xFF424242), topLeft = Offset(cx + w * 0.16f, cy - h * 0.38f), size = Size(w * 0.14f, h * 0.24f), cornerRadius = CornerRadius(w * 0.06f))
    // Big comical eyes
    drawCircle(Color(0xFF212121), radius = w * 0.045f, center = Offset(cx - w * 0.12f, cy - h * 0.06f))
    drawCircle(Color.White, radius = w * 0.015f, center = Offset(cx - w * 0.10f, cy - h * 0.08f))
    drawCircle(Color(0xFF212121), radius = w * 0.045f, center = Offset(cx + w * 0.12f, cy - h * 0.06f))
    drawCircle(Color.White, radius = w * 0.015f, center = Offset(cx + w * 0.14f, cy - h * 0.08f))
    // Blue collar with golden bell
    drawRoundRect(Color(0xFF00E5FF), topLeft = Offset(cx - w * 0.24f, cy + h * 0.2f), size = Size(w * 0.48f, h * 0.1f), cornerRadius = CornerRadius(w * 0.04f))
    drawCircle(Color(0xFFFFD54F), radius = w * 0.07f, center = Offset(cx, cy + h * 0.3f))
}

// Space Werewolf: White Astronaut Cyber-Wolf
private fun DrawScope.drawSpaceWerewolf(w: Float, h: Float) {
    val cx = w * 0.52f; val cy = h * 0.5f
    // White space suit
    drawRoundRect(Color(0xFFEDE7F6), topLeft = Offset(cx - w * 0.26f, cy + h * 0.04f), size = Size(w * 0.52f, h * 0.42f), cornerRadius = CornerRadius(w * 0.12f))
    // Helmet dome with ears
    drawCircle(Color(0xFFEDE7F6), radius = w * 0.26f, center = Offset(cx, cy - h * 0.08f))
    // Visor
    drawRoundRect(Color(0xFF7E57C2), topLeft = Offset(cx - w * 0.2f, cy - h * 0.18f), size = Size(w * 0.4f, h * 0.2f), cornerRadius = CornerRadius(w * 0.08f))
    // Glowing cyan eyes
    drawRoundRect(Color(0xFF00E5FF), topLeft = Offset(cx - w * 0.14f, cy - h * 0.12f), size = Size(w * 0.08f, h * 0.04f), cornerRadius = CornerRadius(w * 0.02f))
    drawRoundRect(Color(0xFF00E5FF), topLeft = Offset(cx + w * 0.06f, cy - h * 0.12f), size = Size(w * 0.08f, h * 0.04f), cornerRadius = CornerRadius(w * 0.02f))
    // Waving white glove
    drawCircle(Color.White, radius = w * 0.08f, center = Offset(cx - w * 0.35f, cy - h * 0.05f))
}

// Bingo: Bingo Grid & Golden Lottery Balls (52, G)
private fun DrawScope.drawBingo(w: Float, h: Float) {
    val cx = w * 0.5f; val cy = h * 0.5f
    // Purple bingo grid card
    drawRoundRect(Color(0xFF311B92), topLeft = Offset(cx - w * 0.35f, cy - h * 0.38f), size = Size(w * 0.45f, h * 0.55f), cornerRadius = CornerRadius(w * 0.06f))
    drawRoundRect(Color(0xFF7C4DFF), topLeft = Offset(cx - w * 0.32f, cy - h * 0.35f), size = Size(w * 0.39f, h * 0.12f), cornerRadius = CornerRadius(w * 0.03f))

    // Golden Lottery Ball #52 with Crown
    drawCircle(Color(0xFFFFB300), radius = w * 0.24f, center = Offset(cx + w * 0.12f, cy + h * 0.15f))
    drawCircle(Color(0xFFFFD54F), radius = w * 0.24f, center = Offset(cx + w * 0.12f, cy + h * 0.15f), style = Stroke(w * 0.03f))
    // Small crown on ball
    drawCircle(Color(0xFFFFE082), radius = w * 0.07f, center = Offset(cx + w * 0.12f, cy - h * 0.08f))

    // Green lottery ball
    drawCircle(Color(0xFF00E676), radius = w * 0.12f, center = Offset(cx + w * 0.24f, cy - h * 0.25f))
}

// Mic Grab: Golden Chest + Hand Holding Mic
private fun DrawScope.drawMicGrab(w: Float, h: Float) {
    val cx = w * 0.55f; val cy = h * 0.45f
    // Golden treasure chest
    drawRoundRect(Color(0xFFFFB300), topLeft = Offset(cx - w * 0.2f, cy - h * 0.38f), size = Size(w * 0.42f, h * 0.32f), cornerRadius = CornerRadius(w * 0.06f))
    drawRoundRect(Color(0xFFFFE082), topLeft = Offset(cx - w * 0.22f, cy - h * 0.42f), size = Size(w * 0.46f, h * 0.12f), cornerRadius = CornerRadius(w * 0.04f))

    // Hand holding microphone
    drawRoundRect(Color(0xFF212121), topLeft = Offset(cx - w * 0.12f, cy - h * 0.05f), size = Size(w * 0.24f, h * 0.45f), cornerRadius = CornerRadius(w * 0.12f))
    drawRoundRect(Color(0xFF9E9E9E), topLeft = Offset(cx - w * 0.09f, cy - h * 0.02f), size = Size(w * 0.18f, h * 0.22f), cornerRadius = CornerRadius(w * 0.09f))
    drawCircle(Color(0xFFFFCCBC), radius = w * 0.12f, center = Offset(cx, cy + h * 0.35f))
}

// Pool: Billiard Balls (8-Ball, Red Ball, White Cue Ball)
private fun DrawScope.drawPool(w: Float, h: Float) {
    val cx = w * 0.52f; val cy = h * 0.5f
    // Black 8-ball
    drawCircle(Color(0xFF212121), radius = w * 0.18f, center = Offset(cx - w * 0.12f, cy - h * 0.15f))
    drawCircle(Color.White, radius = w * 0.08f, center = Offset(cx - w * 0.12f, cy - h * 0.15f))

    // Red striped ball
    drawCircle(Color(0xFFD32F2F), radius = w * 0.2f, center = Offset(cx + w * 0.18f, cy - h * 0.02f))
    drawCircle(Color.White, radius = w * 0.09f, center = Offset(cx + w * 0.18f, cy - h * 0.02f))

    // White cue ball
    drawCircle(Color.White, radius = w * 0.22f, center = Offset(cx - w * 0.05f, cy + h * 0.22f))
    drawCircle(Color(0xFFB0BEC5), radius = w * 0.22f, center = Offset(cx - w * 0.05f, cy + h * 0.22f), style = Stroke(w * 0.025f))
}

// Spin: Glass Bottle Roulette
private fun DrawScope.drawSpinBottle(w: Float, h: Float) {
    val cx = w * 0.5f; val cy = h * 0.5f
    // Wooden wheel roulette
    drawCircle(Color(0xFFFFCC80), radius = w * 0.36f, center = Offset(cx + w * 0.08f, cy + h * 0.15f))
    drawCircle(Color(0xFFFFB74D), radius = w * 0.36f, center = Offset(cx + w * 0.08f, cy + h * 0.15f), style = Stroke(w * 0.04f))

    // Green glass bottle angled
    rotate(-38f, Offset(cx, cy)) {
        drawRoundRect(Color(0xFF43A047), topLeft = Offset(cx - w * 0.12f, cy - h * 0.1f), size = Size(w * 0.24f, h * 0.44f), cornerRadius = CornerRadius(w * 0.08f))
        drawRoundRect(Color(0xFF2E7D32), topLeft = Offset(cx - w * 0.06f, cy - h * 0.35f), size = Size(w * 0.12f, h * 0.3f), cornerRadius = CornerRadius(w * 0.04f))
        // Bottle label
        drawRoundRect(Color(0xFFFFF9C4), topLeft = Offset(cx - w * 0.11f, cy + h * 0.02f), size = Size(w * 0.22f, h * 0.18f), cornerRadius = CornerRadius(w * 0.02f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  7. Main Screen Layout: GamesHomeScreen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GamesHomeScreen(
    nav: NavHostController,
    pad: PaddingValues,
    profile: UserProfile,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WePlayBg)
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 10.dp,
            bottom = pad.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ── 1. Top Bar (Avatar, Coin Capsule, Duelo, Events) ───────────────
        item { WePlayTopBar(nav, profile) }

        // ── 2. Quick Actions (Ranking, Tasks, Friends) ─────────────────────
        item { WePlayShortcutsRow(nav) }

        // ── 3. Games Section Header ────────────────────────────────────────
        item { WePlayGamesHeader(nav) }

        // ── 4. Wide Feature Banners (Who's the Spy & Liar's Party) ─────────
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Banner 1: Who's the Spy
                WePlayFeaturedBanner(
                    background = Brush.linearGradient(listOf(Color(0xFF1E88E5), Color(0xFF1565C0))),
                    title = "Who's the Spy",
                    drawIllustration = { w, h -> drawWhoIsTheSpyCharacters(w, h) },
                    onClick = { nav.navigate(Routes.SPY) },
                )

                // Banner 2: Liar's Party
                WePlayFeaturedBanner(
                    background = Brush.linearGradient(listOf(Color(0xFF8D4E3A), Color(0xFF6D3828))),
                    title = "Liar's Party",
                    drawIllustration = { w, h -> drawLiarsPartyCharacters(w, h) },
                    onClick = {},
                )
            }
        }

        // ── 5. Two-Column Games Grid ───────────────────────────────────────
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Row 1: Crazy Alpaca & Space Werewolf
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        WePlayGridCard(
                            background = Brush.linearGradient(listOf(Color(0xFF8BC34A), Color(0xFF689F38))),
                            title = "Crazy\nAlpaca",
                            badgeText = "🔥 Popular",
                            badgeColor = Color(0xFFFFD54F),
                            badgeTextColor = Color(0xFFBF360C),
                            drawIllustration = { w, h -> drawCrazyAlpaca(w, h) },
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        WePlayGridCard(
                            background = Brush.linearGradient(listOf(Color(0xFF42A5F5), Color(0xFF1E88E5))),
                            title = "Space\nWerewolf",
                            drawIllustration = { w, h -> drawSpaceWerewolf(w, h) },
                        )
                    }
                }

                // Row 2: Bingo & Mic Grab
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        WePlayGridCard(
                            background = Brush.linearGradient(listOf(Color(0xFF9575CD), Color(0xFF673AB7))),
                            title = "Bingo",
                            badgeText = "New",
                            badgeColor = Color(0xFFFFEE58),
                            badgeTextColor = Color(0xFF5D4037),
                            drawIllustration = { w, h -> drawBingo(w, h) },
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        WePlayGridCard(
                            background = Brush.linearGradient(listOf(Color(0xFFFF80AB), Color(0xFFF06292))),
                            title = "Mic Grab",
                            drawIllustration = { w, h -> drawMicGrab(w, h) },
                            extraBadge = {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xCC000000), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = "1d : 20h",
                                        color = Color(0xFFFFD54F),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            },
                        )
                    }
                }

                // Row 3: Pool & Spin
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        WePlayGridCard(
                            background = Brush.linearGradient(listOf(Color(0xFF6D4C41), Color(0xFF4E342E))),
                            title = "Pool",
                            drawIllustration = { w, h -> drawPool(w, h) },
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        WePlayGridCard(
                            background = Brush.linearGradient(listOf(Color(0xFF7986CB), Color(0xFF3F51B5))),
                            title = "Spin",
                            drawIllustration = { w, h -> drawSpinBottle(w, h) },
                        )
                    }
                }
            }
        }
    }
}

// ── Backwards compatibility with MainActivity ────────────────────────────────
@Composable
fun GameLobbyScreen(
    nav: NavHostController,
    pad: PaddingValues,
    profile: UserProfile,
) = GamesHomeScreen(nav, pad, profile)
