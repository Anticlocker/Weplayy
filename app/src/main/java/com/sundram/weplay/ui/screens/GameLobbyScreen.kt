package com.sundram.weplay.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sundram.weplay.data.UserProfile
import com.sundram.weplay.ui.nav.Routes
import kotlinx.coroutines.launch

// ── WePlay Color Palette ──────────────────────────────────────────────────────
private val WePlayBg        = Color(0xFFFFFFFF)
private val WePlayNavy      = Color(0xFF151C33)
private val WePlaySubText   = Color(0xFF757D8A)
private val CoinPillBg      = Color(0xFFF3F4F6)
private val GoldCoinColor   = Color(0xFFFFB300)
private val OrangePlusColor = Color(0xFFFF6D00)
private val RedBadgeColor   = Color(0xFFFF3B30)
private val GreenActiveDot  = Color(0xFF00E676)

// ── Custom WePlay Stepped Cartouche Shape ─────────────────────────────────────
class WePlayCartoucheShape(
    private val cornerStep: Float = 20f,
    private val notch: Float = 10f,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val c = cornerStep
        val n = notch
        val path = Path().apply {
            moveTo(c, 0f)
            lineTo(w - c, 0f)
            lineTo(w - c, n)
            lineTo(w - n, n)
            lineTo(w - n, c)
            lineTo(w, c)
            lineTo(w, h - c)
            lineTo(w - n, h - c)
            lineTo(w - n, h - n)
            lineTo(w - c, h - n)
            lineTo(w - c, h)
            lineTo(c, h)
            lineTo(c, h - n)
            lineTo(n, h - n)
            lineTo(n, h - c)
            lineTo(0f, h - c)
            lineTo(0f, c)
            lineTo(n, c)
            lineTo(n, n)
            lineTo(c, n)
            close()
        }
        return Outline.Generic(path)
    }
}

private val BannerShape = WePlayCartoucheShape(cornerStep = 24f, notch = 11f)
private val GridCardShape = WePlayCartoucheShape(cornerStep = 18f, notch = 9f)

// ─────────────────────────────────────────────────────────────────────────────
//  1. Top Profile & Navigation Header (Exact Reference)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WePlayTopBar(nav: NavHostController, profile: UserProfile) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // User Profile Avatar with Level Badge
        Box(
            modifier = Modifier.size(46.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFCCBC))
                    .border(1.5.dp, Color(0xFFFFE0B2), CircleShape)
                    .clickable { nav.navigate(Routes.ME) },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val cx = size.width / 2f; val cy = size.height / 2f
                    // Warm portrait background
                    drawCircle(Color(0xFFFFD180), radius = size.minDimension * 0.5f)
                    // Shirt/Collar
                    drawArc(
                        Color(0xFF263238), 0f, 180f, true,
                        topLeft = Offset(cx - size.width * 0.38f, cy + size.height * 0.18f),
                        size = Size(size.width * 0.76f, size.height * 0.46f),
                    )
                    // Head
                    drawCircle(Color(0xFFFFCCBC), radius = size.minDimension * 0.28f, center = Offset(cx, cy - size.height * 0.06f))
                    // Dark Hair
                    drawArc(
                        Color(0xFF3E2723), -175f, 155f, true,
                        topLeft = Offset(cx - size.width * 0.3f, cy - size.height * 0.42f),
                        size = Size(size.width * 0.6f, size.height * 0.5f),
                    )
                    // Beard
                    drawArc(
                        Color(0xFF3E2723), 20f, 140f, false,
                        topLeft = Offset(cx - size.width * 0.14f, cy + size.height * 0.05f),
                        size = Size(size.width * 0.28f, size.height * 0.16f),
                        style = Stroke(size.width * 0.035f, cap = StrokeCap.Round),
                    )
                    // Eyes
                    drawCircle(Color(0xFF212121), radius = size.minDimension * 0.026f, center = Offset(cx - size.width * 0.09f, cy - size.height * 0.07f))
                    drawCircle(Color(0xFF212121), radius = size.minDimension * 0.026f, center = Offset(cx + size.width * 0.09f, cy - size.height * 0.07f))
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        // Coin Capsule: [ 🟡 80 + ]
        Box(
            modifier = Modifier
                .height(32.dp)
                .background(CoinPillBg, RoundedCornerShape(16.dp))
                .clickable { nav.navigate(Routes.TOPUP) }
                .padding(horizontal = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Gold Coin with square hole
                Box(
                    modifier = Modifier
                        .size(19.dp)
                        .background(GoldCoinColor, CircleShape)
                        .border(1.dp, Color(0xFFFFD54F), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val cx = size.width / 2f; val cy = size.height / 2f
                        drawRoundRect(
                            Color(0xFFB78103),
                            topLeft = Offset(cx - size.width * 0.16f, cy - size.height * 0.16f),
                            size = Size(size.width * 0.32f, size.height * 0.32f),
                            cornerRadius = CornerRadius(1.5f),
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${profile.coins}",
                    color = WePlayNavy,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(6.dp))
                // Orange + button
                Box(
                    modifier = Modifier
                        .size(15.dp)
                        .background(OrangePlusColor, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Right side: King & Events
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // King (Lion Badge)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { nav.navigate(Routes.ROOM) },
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFF3E0)),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(26.dp)) {
                        val cx = size.width / 2f; val cy = size.height / 2f
                        // Golden lion mane
                        drawCircle(Color(0xFFFFB300), radius = size.minDimension * 0.44f)
                        // Lion face
                        drawCircle(Color(0xFFFFE082), radius = size.minDimension * 0.3f, center = Offset(cx, cy + size.height * 0.04f))
                        // Crown on lion
                        val crownPath = Path().apply {
                            moveTo(cx - size.width * 0.2f, cy - size.height * 0.18f)
                            lineTo(cx - size.width * 0.28f, cy - size.height * 0.36f)
                            lineTo(cx - size.width * 0.08f, cy - size.height * 0.22f)
                            lineTo(cx, cy - size.height * 0.4f)
                            lineTo(cx + size.width * 0.08f, cy - size.height * 0.22f)
                            lineTo(cx + size.width * 0.28f, cy - size.height * 0.36f)
                            lineTo(cx + size.width * 0.2f, cy - size.height * 0.18f)
                            close()
                        }
                        drawPath(crownPath, Color(0xFFFF6F00))
                        // Eyes
                        drawCircle(Color(0xFF3E2723), radius = size.minDimension * 0.035f, center = Offset(cx - size.width * 0.1f, cy + size.height * 0.04f))
                        drawCircle(Color(0xFF3E2723), radius = size.minDimension * 0.035f, center = Offset(cx + size.width * 0.1f, cy + size.height * 0.04f))
                        // Nose
                        drawCircle(Color(0xFFD84315), radius = size.minDimension * 0.04f, center = Offset(cx, cy + size.height * 0.12f))
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text("King", color = WePlaySubText, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }

            // Events (Cyan Star Badge)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { nav.navigate(Routes.DISCOVER) },
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0F7FA)),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(22.dp)) {
                        val cx = size.width / 2f; val cy = size.height / 2f
                        val starPath = Path().apply {
                            val outerR = size.minDimension * 0.48f
                            val innerR = size.minDimension * 0.21f
                            for (i in 0 until 10) {
                                val angle = Math.toRadians((i * 36.0) - 90.0).toFloat()
                                val r = if (i % 2 == 0) outerR else innerR
                                val x = cx + r * kotlin.math.cos(angle)
                                val y = cy + r * kotlin.math.sin(angle)
                                if (i == 0) moveTo(x, y) else lineTo(x, y)
                            }
                            close()
                        }
                        drawPath(starPath, Color(0xFF00E5FF))
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text("Events", color = WePlaySubText, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  2. Shortcuts: Ranking, Tasks, Friends (Exact Circular Artwork)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WePlayShortcutsRow(nav: NavHostController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        // ── 1. Ranking (Golden crown over purple portrait ring)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { nav.navigate(Routes.RANKING) },
        ) {
            Box(modifier = Modifier.size(62.dp), contentAlignment = Alignment.Center) {
                // Purple/pink gradient ring with portrait
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Color(0xFFCE93D8), Color(0xFF8E24AA))))
                        .border(2.dp, Color(0xFFE1BEE7), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val cx = size.width / 2f; val cy = size.height / 2f
                        // Winner silhouette / podium
                        drawRoundRect(
                            Color(0xFFFFD54F),
                            topLeft = Offset(cx - size.width * 0.16f, cy - size.height * 0.14f),
                            size = Size(size.width * 0.32f, size.height * 0.44f),
                            cornerRadius = CornerRadius(size.width * 0.05f),
                        )
                        // Winner head
                        drawCircle(Color(0xFFFFE0B2), radius = size.minDimension * 0.16f, center = Offset(cx, cy - size.height * 0.16f))
                    }
                }
                // Golden crown pinned on top-left
                Box(modifier = Modifier.align(Alignment.TopStart).offset(x = 1.dp, y = (-2).dp)) {
                    Canvas(Modifier.size(20.dp)) {
                        val cx = size.width / 2f
                        val crown = Path().apply {
                            moveTo(0f, size.height * 0.7f)
                            lineTo(0f, size.height * 0.35f)
                            lineTo(cx * 0.5f, size.height * 0.6f)
                            lineTo(cx, size.height * 0.15f)
                            lineTo(cx * 1.5f, size.height * 0.6f)
                            lineTo(size.width, size.height * 0.35f)
                            lineTo(size.width, size.height * 0.7f)
                            close()
                        }
                        drawPath(crown, Color(0xFFFFD700))
                        drawPath(crown, Color(0xFFC8A000), style = Stroke(1.5f))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("Ranking", color = WePlayNavy, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        // ── 2. Tasks (Cyan circular badge with clipboard & red dot)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { nav.navigate(Routes.ME) },
        ) {
            Box(modifier = Modifier.size(62.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE1F5FE))
                        .border(2.dp, Color(0xFF81D4FA), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(34.dp)) {
                        val cx = size.width / 2f; val cy = size.height / 2f
                        // Clipboard body (white)
                        drawRoundRect(
                            Color.White,
                            topLeft = Offset(cx - size.width * 0.28f, cy - size.height * 0.32f),
                            size = Size(size.width * 0.56f, size.height * 0.68f),
                            cornerRadius = CornerRadius(size.width * 0.08f),
                        )
                        // Dark clip at top
                        drawRoundRect(
                            Color(0xFF0288D1),
                            topLeft = Offset(cx - size.width * 0.18f, cy - size.height * 0.42f),
                            size = Size(size.width * 0.36f, size.height * 0.18f),
                            cornerRadius = CornerRadius(size.width * 0.06f),
                        )
                        // Red checkmark
                        val chk = Path().apply {
                            moveTo(cx - size.width * 0.14f, cy)
                            lineTo(cx - size.width * 0.02f, cy + size.height * 0.14f)
                            lineTo(cx + size.width * 0.18f, cy - size.height * 0.1f)
                        }
                        drawPath(chk, Color(0xFFE53935), style = Stroke(size.width * 0.1f, cap = StrokeCap.Round))
                    }
                }
                // Red notification dot on top-right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 1.dp, y = 2.dp)
                        .size(9.dp)
                        .background(RedBadgeColor, CircleShape)
                        .border(1.5.dp, Color.White, CircleShape),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text("Tasks", color = WePlayNavy, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        // ── 3. Friends (Green/cyan ring with 3D avatar)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { nav.navigate(Routes.MSG) },
        ) {
            Box(modifier = Modifier.size(62.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0F2F1))
                        .border(2.dp, Color(0xFF80CBC4), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(36.dp)) {
                        val cx = size.width / 2f; val cy = size.height / 2f
                        // Turquoise circle
                        drawCircle(Color(0xFF26A69A), radius = size.minDimension * 0.48f)
                        // Friend shirt
                        drawArc(
                            Color(0xFF00796B), 0f, 180f, true,
                            topLeft = Offset(cx - size.width * 0.32f, cy + size.height * 0.15f),
                            size = Size(size.width * 0.64f, size.height * 0.44f),
                        )
                        // Friend face
                        drawCircle(Color(0xFFFFCCBC), radius = size.minDimension * 0.22f, center = Offset(cx, cy - size.height * 0.08f))
                        // Dark hair
                        drawArc(
                            Color(0xFF212121), -180f, 180f, true,
                            topLeft = Offset(cx - size.width * 0.22f, cy - size.height * 0.38f),
                            size = Size(size.width * 0.44f, size.height * 0.4f),
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("Friends", color = WePlayNavy, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  3. Games Header: "Games" + "🚪 Game Room"
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WePlayGamesHeader(nav: NavHostController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
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
            Canvas(Modifier.size(16.dp)) {
                drawRoundRect(
                    Color(0xFF37474F),
                    cornerRadius = CornerRadius(size.width * 0.2f),
                )
                drawRoundRect(
                    Color(0xFFB0BEC5),
                    topLeft = Offset(size.width * 0.2f, size.height * 0.2f),
                    size = Size(size.width * 0.6f, size.height * 0.6f),
                    cornerRadius = CornerRadius(size.width * 0.1f),
                )
            }
            Spacer(Modifier.width(5.dp))
            Text(
                text = "Game Room",
                color = Color(0xFF455A64),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  4. Featured Game Banner with Stepped Cartouche Bevel Frame
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun WePlayFeaturedBanner(
    background: Brush,
    borderBrush: Brush,
    title: String,
    badge: String? = null,
    drawIllustration: DrawScope.(Float, Float) -> Unit,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(106.dp)
            .shadow(6.dp, BannerShape)
            .clip(BannerShape)
            .background(background)
            .clickable(onClick = onClick),
    ) {
        // Decorative stepped cartouche border overlay
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val c = 24f; val n = 11f
            val path = Path().apply {
                moveTo(c, 0f)
                lineTo(w - c, 0f)
                lineTo(w - c, n)
                lineTo(w - n, n)
                lineTo(w - n, c)
                lineTo(w, c)
                lineTo(w, h - c)
                lineTo(w - n, h - c)
                lineTo(w - n, h - n)
                lineTo(w - c, h - n)
                lineTo(w - c, h)
                lineTo(c, h)
                lineTo(c, h - n)
                lineTo(n, h - n)
                lineTo(n, h - c)
                lineTo(0f, h - c)
                lineTo(0f, c)
                lineTo(n, c)
                lineTo(n, n)
                lineTo(c, n)
                close()
            }
            // Metallic golden/tinted outer border
            drawPath(path, borderBrush, style = Stroke(width = 4f))
            // Inner soft highlight
            drawCircle(
                Color.White.copy(alpha = 0.12f),
                radius = w * 0.45f,
                center = Offset(w * 0.8f, h * 0.5f),
            )
        }

        // Top-left "New" tag if present
        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, top = 10.dp)
                    .background(Color(0xFFFFEE58), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
                Text(badge, color = Color(0xFF5D4037), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Title on left
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp, top = if (badge != null) 14.dp else 0.dp)
                .fillMaxWidth(0.52f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 22.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Illustration on right
        Canvas(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(130.dp)
                .padding(end = 4.dp),
        ) {
            drawIllustration(size.width, size.height)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  5. Two-Column Grid Card with Stepped Cartouche Bevel Frame
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun WePlayGridCard(
    background: Brush,
    borderBrush: Brush,
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
            .height(130.dp)
            .shadow(4.dp, GridCardShape)
            .clip(GridCardShape)
            .background(background)
            .clickable(onClick = onClick),
    ) {
        // Decorative stepped cartouche border overlay
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val c = 18f; val n = 9f
            val path = Path().apply {
                moveTo(c, 0f)
                lineTo(w - c, 0f)
                lineTo(w - c, n)
                lineTo(w - n, n)
                lineTo(w - n, c)
                lineTo(w, c)
                lineTo(w, h - c)
                lineTo(w - n, h - c)
                lineTo(w - n, h - n)
                lineTo(w - c, h - n)
                lineTo(w - c, h)
                lineTo(c, h)
                lineTo(c, h - n)
                lineTo(n, h - n)
                lineTo(n, h - c)
                lineTo(0f, h - c)
                lineTo(0f, c)
                lineTo(n, c)
                lineTo(n, n)
                lineTo(c, n)
                close()
            }
            // Metallic border
            drawPath(path, borderBrush, style = Stroke(width = 3.5f))
            // Inner glow
            drawCircle(Color.White.copy(alpha = 0.1f), radius = w * 0.45f, center = Offset(w * 0.8f, 0f))
        }

        // Badge on top-left (e.g. "Free Power-ups", "Popular")
        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 10.dp, top = 8.dp)
                    .background(badgeColor, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(text = badgeText, color = badgeTextColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Title on left
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp, top = if (badgeText != null) 12.dp else 0.dp)
                .fillMaxWidth(0.55f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Illustration on right
        Canvas(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(96.dp)
                .padding(end = 2.dp),
        ) {
            drawIllustration(size.width, size.height)
        }

        // Extra floating badge on bottom-right (e.g. countdown / gift pack)
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

// ═════════════════════════════════════════════════════════════════════════════
//  6. HIGH-FIDELITY GAME ILLUSTRATIONS (Exact Reference Art)
// ═════════════════════════════════════════════════════════════════════════════

// ── 1. Petrol Pump Simulator (Boy attendant with green cap holding fuel nozzle)
private fun DrawScope.drawPetrolPumpSimulator(w: Float, h: Float) {
    val cx = w * 0.62f; val cy = h * 0.52f

    // Fuel dispenser machine on right
    drawRoundRect(
        Color(0xFFECEFF1),
        topLeft = Offset(w * 0.76f, cy - h * 0.35f),
        size = Size(w * 0.22f, h * 0.72f),
        cornerRadius = CornerRadius(w * 0.04f),
    )
    drawRoundRect(
        Color(0xFF263238),
        topLeft = Offset(w * 0.79f, cy - h * 0.25f),
        size = Size(w * 0.16f, h * 0.22f),
        cornerRadius = CornerRadius(w * 0.02f),
    )

    // Attendant boy body (green jacket)
    drawRoundRect(
        Brush.verticalGradient(listOf(Color(0xFF43A047), Color(0xFF2E7D32))),
        topLeft = Offset(cx - w * 0.2f, cy + h * 0.05f),
        size = Size(w * 0.36f, h * 0.44f),
        cornerRadius = CornerRadius(w * 0.08f),
    )
    // White inner shirt
    drawRoundRect(Color.White, topLeft = Offset(cx - w * 0.07f, cy + h * 0.05f), size = Size(w * 0.14f, h * 0.2f), cornerRadius = CornerRadius(w * 0.03f))

    // Head
    drawCircle(Color(0xFFFFCCBC), radius = w * 0.18f, center = Offset(cx, cy - h * 0.14f))

    // Green baseball cap with visor
    drawArc(
        Color(0xFF2E7D32), -180f, 180f, true,
        topLeft = Offset(cx - w * 0.2f, cy - h * 0.4f),
        size = Size(w * 0.4f, h * 0.38f),
    )
    drawRoundRect(
        Color(0xFF1B5E20),
        topLeft = Offset(cx - w * 0.24f, cy - h * 0.22f),
        size = Size(w * 0.48f, h * 0.09f),
        cornerRadius = CornerRadius(w * 0.04f),
    )

    // Smiling face with teeth
    drawCircle(Color(0xFF212121), radius = w * 0.03f, center = Offset(cx - w * 0.07f, cy - h * 0.14f))
    drawCircle(Color(0xFF212121), radius = w * 0.03f, center = Offset(cx + w * 0.07f, cy - h * 0.14f))
    // Big open toothy smile
    drawArc(
        Color(0xFFB71C1C), 0f, 180f, true,
        topLeft = Offset(cx - w * 0.09f, cy - h * 0.08f),
        size = Size(w * 0.18f, h * 0.12f),
    )
    drawRoundRect(Color.White, topLeft = Offset(cx - w * 0.07f, cy - h * 0.08f), size = Size(w * 0.14f, h * 0.05f), cornerRadius = CornerRadius(w * 0.02f))

    // Red fuel nozzle in hand
    drawRoundRect(
        Color(0xFFD32F2F),
        topLeft = Offset(cx - w * 0.38f, cy - h * 0.15f),
        size = Size(w * 0.18f, h * 0.28f),
        cornerRadius = CornerRadius(w * 0.05f),
    )
    // Nozzle spout
    drawLine(Color(0xFF757575), Offset(cx - w * 0.36f, cy - h * 0.14f), Offset(cx - w * 0.48f, cy - h * 0.26f), strokeWidth = w * 0.04f, cap = StrokeCap.Round)
    // Fuel hose
    drawArc(
        Color(0xFF212121), 60f, 160f, false,
        topLeft = Offset(cx - w * 0.38f, cy),
        size = Size(w * 0.35f, h * 0.35f),
        style = Stroke(w * 0.035f),
    )
}

// ── 2. Who's the Spy (Detective in black fedora hat, sunglasses, trench coat)
private fun DrawScope.drawWhosTheSpy(w: Float, h: Float) {
    val cx = w * 0.65f; val cy = h * 0.52f

    // Dark trench coat
    drawRoundRect(
        Brush.verticalGradient(listOf(Color(0xFF37474F), Color(0xFF212121))),
        topLeft = Offset(cx - w * 0.24f, cy + h * 0.06f),
        size = Size(w * 0.48f, h * 0.44f),
        cornerRadius = CornerRadius(w * 0.1f),
    )
    // White collar & tie
    drawRoundRect(Color.White, topLeft = Offset(cx - w * 0.08f, cy + h * 0.06f), size = Size(w * 0.16f, h * 0.14f), cornerRadius = CornerRadius(w * 0.03f))
    drawRoundRect(Color(0xFFD32F2F), topLeft = Offset(cx - w * 0.03f, cy + h * 0.1f), size = Size(w * 0.06f, h * 0.22f), cornerRadius = CornerRadius(w * 0.02f))

    // Head (skin tone)
    drawCircle(Color(0xFFFFCCBC), radius = w * 0.18f, center = Offset(cx, cy - h * 0.12f))

    // Black fedora hat
    drawRoundRect(
        Color(0xFF212121),
        topLeft = Offset(cx - w * 0.34f, cy - h * 0.28f),
        size = Size(w * 0.68f, h * 0.1f),
        cornerRadius = CornerRadius(w * 0.05f),
    )
    drawArc(
        Color(0xFF212121), -180f, 180f, true,
        topLeft = Offset(cx - w * 0.22f, cy - h * 0.48f),
        size = Size(w * 0.44f, h * 0.4f),
    )
    // Hat ribbon band
    drawRoundRect(
        Color(0xFF1565C0),
        topLeft = Offset(cx - w * 0.22f, cy - h * 0.32f),
        size = Size(w * 0.44f, h * 0.05f),
    )

    // Black sunglasses
    drawRoundRect(
        Color(0xFF212121),
        topLeft = Offset(cx - w * 0.18f, cy - h * 0.18f),
        size = Size(w * 0.36f, h * 0.1f),
        cornerRadius = CornerRadius(w * 0.04f),
    )
    drawCircle(Color.White.copy(0.4f), radius = w * 0.02f, center = Offset(cx - w * 0.08f, cy - h * 0.15f))
    drawCircle(Color.White.copy(0.4f), radius = w * 0.02f, center = Offset(cx + w * 0.08f, cy - h * 0.15f))
}

// ── 3. Paint to Hide (Graffiti yellow smiley face badge with paint splashes)
private fun DrawScope.drawPaintToHide(w: Float, h: Float) {
    val cx = w * 0.64f; val cy = h * 0.5f

    // Colorful graffiti paint splatters in background
    drawCircle(Color(0xFF00E5FF).copy(0.7f), radius = w * 0.14f, center = Offset(cx - w * 0.22f, cy + h * 0.18f))
    drawCircle(Color(0xFFE91E63).copy(0.7f), radius = w * 0.12f, center = Offset(cx + w * 0.22f, cy - h * 0.22f))
    drawCircle(Color(0xFF76FF03).copy(0.7f), radius = w * 0.1f, center = Offset(cx + w * 0.2f, cy + h * 0.2f))

    // Giant yellow smiley badge
    drawCircle(
        Brush.radialGradient(listOf(Color(0xFFFFEE58), Color(0xFFFFD600))),
        radius = w * 0.28f, center = Offset(cx, cy),
    )
    drawCircle(Color(0xFFFFAB00), radius = w * 0.28f, center = Offset(cx, cy), style = Stroke(w * 0.035f))

    // Big happy smile
    drawArc(
        Color(0xFF212121), 10f, 160f, false,
        topLeft = Offset(cx - w * 0.15f, cy - h * 0.06f),
        size = Size(w * 0.3f, h * 0.22f),
        style = Stroke(w * 0.045f, cap = StrokeCap.Round),
    )
    // Eyes
    drawCircle(Color(0xFF212121), radius = w * 0.04f, center = Offset(cx - w * 0.09f, cy - h * 0.07f))
    drawCircle(Color(0xFF212121), radius = w * 0.04f, center = Offset(cx + w * 0.09f, cy - h * 0.07f))
}

// ── 4. Trickster's Cafe (Crocodile with sunglasses & bandana holding poker cards)
private fun DrawScope.drawTrickstersCafe(w: Float, h: Float) {
    val cx = w * 0.6f; val cy = h * 0.52f

    // Crocodile green body
    drawRoundRect(
        Color(0xFF43A047),
        topLeft = Offset(cx - w * 0.22f, cy + h * 0.05f),
        size = Size(w * 0.44f, h * 0.42f),
        cornerRadius = CornerRadius(w * 0.1f),
    )
    // Head with snout
    drawCircle(Color(0xFF43A047), radius = w * 0.22f, center = Offset(cx, cy - h * 0.12f))
    drawRoundRect(
        Color(0xFF2E7D32),
        topLeft = Offset(cx - w * 0.3f, cy - h * 0.18f),
        size = Size(w * 0.25f, h * 0.14f),
        cornerRadius = CornerRadius(w * 0.05f),
    )

    // Yellow bandana around neck
    drawRoundRect(
        Color(0xFFFFD54F),
        topLeft = Offset(cx - w * 0.2f, cy + h * 0.02f),
        size = Size(w * 0.4f, h * 0.1f),
        cornerRadius = CornerRadius(w * 0.04f),
    )

    // Cool sunglasses
    drawRoundRect(
        Color(0xFF212121),
        topLeft = Offset(cx - w * 0.18f, cy - h * 0.18f),
        size = Size(w * 0.36f, h * 0.09f),
        cornerRadius = CornerRadius(w * 0.035f),
    )

    // Fanned playing cards in hand (bottom-left)
    drawRoundRect(Color.White, topLeft = Offset(cx - w * 0.36f, cy + h * 0.08f), size = Size(w * 0.16f, h * 0.22f), cornerRadius = CornerRadius(w * 0.03f))
    drawRoundRect(Color(0xFFD32F2F), topLeft = Offset(cx - w * 0.34f, cy + h * 0.1f), size = Size(w * 0.12f, h * 0.1f), cornerRadius = CornerRadius(w * 0.02f))
}

// ── 5. Hide And Seek (Scared toilet character + peeking shadow monster)
private fun DrawScope.drawHideAndSeek(w: Float, h: Float) {
    val cx = w * 0.65f; val cy = h * 0.55f

    // Brick wall in background (grey)
    drawRoundRect(
        Color(0xFF90A4AE),
        topLeft = Offset(cx - w * 0.25f, cy - h * 0.4f),
        size = Size(w * 0.36f, h * 0.5f),
        cornerRadius = CornerRadius(w * 0.04f),
    )
    // Black shadow monster peeking behind wall
    drawCircle(Color(0xFF212121), radius = w * 0.14f, center = Offset(cx - w * 0.18f, cy - h * 0.25f))
    drawCircle(Color(0xFFFF5252), radius = w * 0.025f, center = Offset(cx - w * 0.22f, cy - h * 0.27f))
    drawCircle(Color(0xFFFF5252), radius = w * 0.025f, center = Offset(cx - w * 0.14f, cy - h * 0.27f))

    // Scared cartoon toilet character in foreground
    drawRoundRect(
        Color.White,
        topLeft = Offset(cx - w * 0.16f, cy - h * 0.1f),
        size = Size(w * 0.38f, h * 0.45f),
        cornerRadius = CornerRadius(w * 0.1f),
    )
    // Toilet lid
    drawOval(Color(0xFFECEFF1), topLeft = Offset(cx - w * 0.18f, cy + h * 0.08f), size = Size(w * 0.42f, h * 0.25f))
    // Scared eyes with pupils looking sideways
    drawCircle(Color(0xFF212121), radius = w * 0.04f, center = Offset(cx - w * 0.06f, cy - h * 0.04f))
    drawCircle(Color(0xFF212121), radius = w * 0.04f, center = Offset(cx + w * 0.08f, cy - h * 0.04f))
    drawCircle(Color.White, radius = w * 0.016f, center = Offset(cx - w * 0.04f, cy - h * 0.05f))
    drawCircle(Color.White, radius = w * 0.016f, center = Offset(cx + w * 0.1f, cy - h * 0.05f))
    // Sweat drop
    drawCircle(Color(0xFF40C4FF), radius = w * 0.028f, center = Offset(cx + w * 0.18f, cy - h * 0.1f))
}

// ── 6. Space Werewolf (White cyber astronaut wolf waving hand)
private fun DrawScope.drawSpaceWerewolf(w: Float, h: Float) {
    val cx = w * 0.6f; val cy = h * 0.52f

    // Spacesuit body
    drawRoundRect(
        Brush.radialGradient(listOf(Color(0xFFEDE7F6), Color(0xFFD1C4E9)), center = Offset(cx, cy + h * 0.2f), radius = w * 0.35f),
        topLeft = Offset(cx - w * 0.24f, cy + h * 0.04f),
        size = Size(w * 0.48f, h * 0.44f),
        cornerRadius = CornerRadius(w * 0.12f),
    )
    // Helmet
    drawCircle(Color(0xFFEDE7F6), radius = w * 0.26f, center = Offset(cx, cy - h * 0.1f))

    // Pointed wolf ears on helmet
    val earL = Path().apply {
        moveTo(cx - w * 0.16f, cy - h * 0.3f)
        lineTo(cx - w * 0.26f, cy - h * 0.48f)
        lineTo(cx - w * 0.06f, cy - h * 0.3f)
        close()
    }
    drawPath(earL, Color(0xFFEDE7F6))
    val earR = Path().apply {
        moveTo(cx + w * 0.16f, cy - h * 0.3f)
        lineTo(cx + w * 0.26f, cy - h * 0.48f)
        lineTo(cx + w * 0.06f, cy - h * 0.3f)
        close()
    }
    drawPath(earR, Color(0xFFEDE7F6))

    // Purple visor
    drawRoundRect(
        Brush.verticalGradient(listOf(Color(0xFF7E57C2), Color(0xFF4527A0))),
        topLeft = Offset(cx - w * 0.19f, cy - h * 0.25f),
        size = Size(w * 0.38f, h * 0.25f),
        cornerRadius = CornerRadius(w * 0.09f),
    )
    // Glowing cyan eyes
    drawRoundRect(Color(0xFF00E5FF), topLeft = Offset(cx - w * 0.13f, cy - h * 0.16f), size = Size(w * 0.08f, h * 0.04f), cornerRadius = CornerRadius(w * 0.02f))
    drawRoundRect(Color(0xFF00E5FF), topLeft = Offset(cx + w * 0.05f, cy - h * 0.16f), size = Size(w * 0.08f, h * 0.04f), cornerRadius = CornerRadius(w * 0.02f))

    // Waving white glove
    drawCircle(Color.White, radius = w * 0.09f, center = Offset(cx - w * 0.35f, cy - h * 0.05f))
}

// ── 7. Oh My Card (Fanned colorful playing cards)
private fun DrawScope.drawOhMyCard(w: Float, h: Float) {
    val cx = w * 0.6f; val cy = h * 0.5f

    // 3 Fanned playing cards
    rotate(-18f, Offset(cx - w * 0.1f, cy)) {
        drawRoundRect(Color.White, topLeft = Offset(cx - w * 0.25f, cy - h * 0.3f), size = Size(w * 0.26f, h * 0.5f), cornerRadius = CornerRadius(w * 0.04f))
        drawCircle(Color(0xFFE53935), radius = w * 0.04f, center = Offset(cx - w * 0.12f, cy - h * 0.05f))
    }
    rotate(0f, Offset(cx, cy)) {
        drawRoundRect(Color.White, topLeft = Offset(cx - w * 0.13f, cy - h * 0.32f), size = Size(w * 0.26f, h * 0.52f), cornerRadius = CornerRadius(w * 0.04f))
        drawRoundRect(Color(0xFF43A047), topLeft = Offset(cx - w * 0.07f, cy - h * 0.15f), size = Size(w * 0.14f, h * 0.18f), cornerRadius = CornerRadius(w * 0.02f))
    }
    rotate(18f, Offset(cx + w * 0.1f, cy)) {
        drawRoundRect(Color.White, topLeft = Offset(cx - w * 0.01f, cy - h * 0.3f), size = Size(w * 0.26f, h * 0.5f), cornerRadius = CornerRadius(w * 0.04f))
        drawCircle(Color(0xFFFFB300), radius = w * 0.04f, center = Offset(cx + w * 0.12f, cy - h * 0.05f))
    }
}

// ── 8. Ludo (3D Red king token + white dice)
private fun DrawScope.drawLudoGame(w: Float, h: Float) {
    val cx = w * 0.6f; val cy = h * 0.52f

    // 3D Red king token (left)
    drawCircle(Color(0xFFE53935), radius = w * 0.12f, center = Offset(cx - w * 0.12f, cy - h * 0.18f))
    drawRoundRect(
        Brush.verticalGradient(listOf(Color(0xFFE53935), Color(0xFFB71C1C))),
        topLeft = Offset(cx - w * 0.22f, cy - h * 0.06f),
        size = Size(w * 0.24f, h * 0.35f),
        cornerRadius = CornerRadius(w * 0.06f),
    )
    drawOval(Color(0xFFB71C1C), topLeft = Offset(cx - w * 0.26f, cy + h * 0.24f), size = Size(w * 0.32f, h * 0.14f))

    // 3D White dice (right)
    rotate(15f, Offset(cx + w * 0.16f, cy)) {
        drawRoundRect(
            Color.White,
            topLeft = Offset(cx + w * 0.02f, cy - h * 0.18f),
            size = Size(w * 0.32f, h * 0.32f),
            cornerRadius = CornerRadius(w * 0.06f),
        )
        // Red center pip + black corner pips
        drawCircle(Color(0xFFD32F2F), radius = w * 0.04f, center = Offset(cx + w * 0.18f, cy - h * 0.02f))
        drawCircle(Color(0xFF212121), radius = w * 0.025f, center = Offset(cx + w * 0.08f, cy - h * 0.12f))
        drawCircle(Color(0xFF212121), radius = w * 0.025f, center = Offset(cx + w * 0.28f, cy + h * 0.08f))
    }
}

// ── 9. Guess My Drawing (Cylindrical colored pencil + paint brush)
private fun DrawScope.drawGuessMyDrawing(w: Float, h: Float) {
    val cx = w * 0.6f; val cy = h * 0.5f

    // Angled colorful pencil
    rotate(-35f, Offset(cx, cy)) {
        // Pencil body (yellow)
        drawRoundRect(Color(0xFFFFD54F), topLeft = Offset(cx - w * 0.1f, cy - h * 0.3f), size = Size(w * 0.2f, h * 0.55f), cornerRadius = CornerRadius(w * 0.03f))
        // Pencil tip (wood tone + dark lead)
        val tip = Path().apply {
            moveTo(cx - w * 0.1f, cy + h * 0.25f)
            lineTo(cx + w * 0.1f, cy + h * 0.25f)
            lineTo(cx, cy + h * 0.42f)
            close()
        }
        drawPath(tip, Color(0xFFFFCCBC))
        drawCircle(Color(0xFF212121), radius = w * 0.04f, center = Offset(cx, cy + h * 0.39f))
        // Pink eraser at top
        drawRoundRect(Color(0xFFFF8A80), topLeft = Offset(cx - w * 0.1f, cy - h * 0.4f), size = Size(w * 0.2f, h * 0.12f), cornerRadius = CornerRadius(w * 0.04f))
    }
}

// ── 10. Mic Grab (Hand holding microphone with golden mesh & chest)
private fun DrawScope.drawMicGrabGame(w: Float, h: Float) {
    val cx = w * 0.62f; val cy = h * 0.48f

    // Golden chest in background
    drawRoundRect(Color(0xFFFFD54F), topLeft = Offset(cx - w * 0.15f, cy - h * 0.3f), size = Size(w * 0.36f, h * 0.28f), cornerRadius = CornerRadius(w * 0.05f))

    // Microphone in hand
    drawRoundRect(Color(0xFF212121), topLeft = Offset(cx - w * 0.08f, cy - h * 0.02f), size = Size(w * 0.16f, h * 0.38f), cornerRadius = CornerRadius(w * 0.04f))
    drawCircle(Color(0xFFFFD54F), radius = w * 0.13f, center = Offset(cx, cy - h * 0.08f))

    // Hand holding mic
    drawCircle(Color(0xFFFFCCBC), radius = w * 0.12f, center = Offset(cx, cy + h * 0.28f))
}

// ── 11. Crazy Alpaca (Fluffy cute white alpaca with green collar and bell)
private fun DrawScope.drawCrazyAlpacaGame(w: Float, h: Float) {
    val cx = w * 0.62f; val cy = h * 0.5f

    // Fluffy white head
    drawCircle(Color.White, radius = w * 0.26f, center = Offset(cx, cy - h * 0.08f))
    // Fluffy top puff
    drawCircle(Color.White, radius = w * 0.12f, center = Offset(cx, cy - h * 0.3f))
    drawCircle(Color.White, radius = w * 0.1f, center = Offset(cx - w * 0.12f, cy - h * 0.26f))
    drawCircle(Color.White, radius = w * 0.1f, center = Offset(cx + w * 0.12f, cy - h * 0.26f))

    // Ears
    drawRoundRect(Color(0xFF9E9E9E), topLeft = Offset(cx - w * 0.28f, cy - h * 0.32f), size = Size(w * 0.09f, h * 0.18f), cornerRadius = CornerRadius(w * 0.04f))
    drawRoundRect(Color(0xFF9E9E9E), topLeft = Offset(cx + w * 0.19f, cy - h * 0.32f), size = Size(w * 0.09f, h * 0.18f), cornerRadius = CornerRadius(w * 0.04f))

    // Cute eyes
    drawCircle(Color(0xFF212121), radius = w * 0.045f, center = Offset(cx - w * 0.09f, cy - h * 0.08f))
    drawCircle(Color.White, radius = w * 0.016f, center = Offset(cx - w * 0.075f, cy - h * 0.095f))
    drawCircle(Color(0xFF212121), radius = w * 0.045f, center = Offset(cx + w * 0.09f, cy - h * 0.08f))
    drawCircle(Color.White, radius = w * 0.016f, center = Offset(cx + w * 0.105f, cy - h * 0.095f))

    // Green collar with golden bell
    drawRoundRect(Color(0xFF00E676), topLeft = Offset(cx - w * 0.18f, cy + h * 0.12f), size = Size(w * 0.36f, h * 0.08f), cornerRadius = CornerRadius(w * 0.04f))
    drawCircle(Color(0xFFFFD54F), radius = w * 0.07f, center = Offset(cx, cy + h * 0.22f))
}

// ── 12. Jackaroo (Blue round character with cards)
private fun DrawScope.drawJackaroo(w: Float, h: Float) {
    val cx = w * 0.6f; val cy = h * 0.52f

    // Blue round character
    drawCircle(
        Brush.radialGradient(listOf(Color(0xFF40C4FF), Color(0xFF0288D1))),
        radius = w * 0.28f, center = Offset(cx, cy),
    )
    // Eyes
    drawCircle(Color.White, radius = w * 0.06f, center = Offset(cx - w * 0.09f, cy - h * 0.08f))
    drawCircle(Color(0xFF212121), radius = w * 0.035f, center = Offset(cx - w * 0.08f, cy - h * 0.08f))
    drawCircle(Color.White, radius = w * 0.06f, center = Offset(cx + w * 0.09f, cy - h * 0.08f))
    drawCircle(Color(0xFF212121), radius = w * 0.035f, center = Offset(cx + w * 0.1f, cy - h * 0.08f))

    // Playing cards in front
    drawRoundRect(Color.White, topLeft = Offset(cx - w * 0.3f, cy + h * 0.08f), size = Size(w * 0.16f, h * 0.22f), cornerRadius = CornerRadius(w * 0.03f))
    drawRoundRect(Color(0xFFE53935), topLeft = Offset(cx - w * 0.28f, cy + h * 0.1f), size = Size(w * 0.12f, h * 0.1f), cornerRadius = CornerRadius(w * 0.02f))
}

// ── 13. Survival Challenge (Colorful clown/jester face with party hat)
private fun DrawScope.drawSurvivalChallenge(w: Float, h: Float) {
    val cx = w * 0.62f; val cy = h * 0.52f

    // Clown face (white)
    drawCircle(Color.White, radius = w * 0.22f, center = Offset(cx, cy))
    // Red clown nose
    drawCircle(Color(0xFFD50000), radius = w * 0.07f, center = Offset(cx, cy + h * 0.02f))
    // Party hat
    val hat = Path().apply {
        moveTo(cx - w * 0.18f, cy - h * 0.16f)
        lineTo(cx + w * 0.18f, cy - h * 0.16f)
        lineTo(cx, cy - h * 0.44f)
        close()
    }
    drawPath(hat, Color(0xFF6200EA))
    drawCircle(Color(0xFFFFD54F), radius = w * 0.05f, center = Offset(cx, cy - h * 0.44f))

    // Eyes
    drawCircle(Color(0xFF212121), radius = w * 0.035f, center = Offset(cx - w * 0.08f, cy - h * 0.08f))
    drawCircle(Color(0xFF212121), radius = w * 0.035f, center = Offset(cx + w * 0.08f, cy - h * 0.08f))
}

// ── 14. Video Room (Red vintage TV set with play button)
private fun DrawScope.drawVideoRoom(w: Float, h: Float) {
    val cx = w * 0.6f; val cy = h * 0.52f

    // Antenna
    drawLine(Color(0xFF9E9E9E), Offset(cx, cy - h * 0.26f), Offset(cx - w * 0.16f, cy - h * 0.42f), strokeWidth = w * 0.03f, cap = StrokeCap.Round)
    drawLine(Color(0xFF9E9E9E), Offset(cx, cy - h * 0.26f), Offset(cx + w * 0.16f, cy - h * 0.42f), strokeWidth = w * 0.03f, cap = StrokeCap.Round)

    // Red TV Cabinet
    drawRoundRect(
        Color(0xFFE53935),
        topLeft = Offset(cx - w * 0.26f, cy - h * 0.24f),
        size = Size(w * 0.52f, h * 0.48f),
        cornerRadius = CornerRadius(w * 0.08f),
    )
    // TV Screen (white/glow)
    drawRoundRect(
        Color.White,
        topLeft = Offset(cx - w * 0.2f, cy - h * 0.18f),
        size = Size(w * 0.32f, h * 0.36f),
        cornerRadius = CornerRadius(w * 0.05f),
    )
    // Play triangle on screen
    val play = Path().apply {
        moveTo(cx - w * 0.08f, cy - h * 0.08f)
        lineTo(cx + w * 0.08f, cy)
        lineTo(cx - w * 0.08f, cy + h * 0.08f)
        close()
    }
    drawPath(play, Color(0xFFE53935))
}

// ── 15. Bingo (Bingo card with lottery balls)
private fun DrawScope.drawBingoGame(w: Float, h: Float) {
    val cx = w * 0.6f; val cy = h * 0.5f

    // Bingo card in background
    drawRoundRect(Color(0xFF4A148C), topLeft = Offset(cx - w * 0.28f, cy - h * 0.34f), size = Size(w * 0.36f, h * 0.54f), cornerRadius = CornerRadius(w * 0.05f))
    drawRoundRect(Color(0xFF7C4DFF), topLeft = Offset(cx - w * 0.28f, cy - h * 0.34f), size = Size(w * 0.36f, h * 0.12f), cornerRadius = CornerRadius(w * 0.05f))

    // Golden Lottery Ball in foreground
    drawCircle(
        Brush.radialGradient(listOf(Color(0xFFFFD54F), Color(0xFFFF8F00))),
        radius = w * 0.2f, center = Offset(cx + w * 0.14f, cy + h * 0.12f),
    )
    drawCircle(Color.White, radius = w * 0.08f, center = Offset(cx + w * 0.14f, cy + h * 0.12f))
}

// ─────────────────────────────────────────────────────────────────────────────
//  7. Explore Feed Model & Item (Phase 5)
// ─────────────────────────────────────────────────────────────────────────────
data class ExploreUser(
    val id: String,
    val name: String,
    val gender: String, // "F" or "M"
    val avatarBg: Color,
    val distance: String,
    val statusText: String,
    val isGameRoom: Boolean = false,
    val bio: String,
)

private val exploreUsers = listOf(
    ExploreUser("1", "Madiha", "F", Color(0xFFE1BEE7), "< 194km", "In Voice Room", false, "This is a very mysterious..."),
    ExploreUser("2", "L4ZYK!D!!", "M", Color(0xFFFFCCBC), "< 213km", "In Voice Room", false, "If you are in choice yo..."),
    ExploreUser("3", "Scooby", "F", Color(0xFFFFF9C4), "< 110km", "In Voice Room", false, "Ek flower do batterfl..."),
    ExploreUser("4", "siddhi", "F", Color(0xFFFFCDD2), "", "In Voice Room", false, "Punjabi..."),
    ExploreUser("5", "K O H L I", "M", Color(0xFFC8E6C9), "", "In Voice Room", false, "Gopiyo ka Kanhaaa"),
    ExploreUser("6", "T!wariji", "F", Color(0xFFD1C4E9), "", "In Voice Room", false, "Hope obliterated!"),
    ExploreUser("7", "K!RAN", "F", Color(0xFFB3E5FC), "", "In Voice Room", false, "Silence is the Best Answer for a..."),
    ExploreUser("8", "M!RA", "F", Color(0xFFFFE0B2), "", "In Voice Room", false, "This user made lots of exciting..."),
    ExploreUser("9", "N O R A", "F", Color(0xFFF8BBD0), "", "In Voice Room", false, "F* to cutie hai"),
    ExploreUser("10", "VED!CA", "F", Color(0xFFE0F2F1), "", "In Voice Room", false, "This user made lots of exciting..."),
    ExploreUser("11", "Santyaaa", "M", Color(0xFFD7CCC8), "", "In Voice Room", false, "🔥"),
    ExploreUser("12", "Ashuu", "F", Color(0xFFFFCCBC), "", "In Voice Room", false, "💕"),
    ExploreUser("13", "Lizzie", "F", Color(0xFFC5CAE9), "< 165km", "In Game Room", true, "This user is your age"),
)

@Composable
private fun ExploreFeedItem(user: ExploreUser, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // User Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(user.avatarBg),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val cx = size.width / 2f; val cy = size.height / 2f
                    drawCircle(Color(0xFFFFCCBC), radius = size.minDimension * 0.28f, center = Offset(cx, cy - size.height * 0.05f))
                    drawArc(
                        if (user.gender == "F") Color(0xFF8E24AA) else Color(0xFF1E88E5),
                        -180f, 180f, true,
                        topLeft = Offset(cx - size.width * 0.3f, cy - size.height * 0.38f),
                        size = Size(size.width * 0.6f, size.height * 0.44f),
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            // Name + Gender + Bio
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.name,
                        color = WePlayNavy,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(4.dp))
                    // Gender symbol
                    Text(
                        text = if (user.gender == "F") "♀" else "♂",
                        color = if (user.gender == "F") Color(0xFFE91E63) else Color(0xFF1E88E5),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (user.distance.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = user.distance,
                            color = WePlaySubText,
                            fontSize = 11.sp,
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = user.bio,
                    color = WePlaySubText,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(8.dp))

            // Right Status Badge: Equalizer icon + "In Voice Room" or "In Game Room"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                // Animated equalizer bars icon
                Canvas(Modifier.size(12.dp)) {
                    val w = size.width; val h = size.height
                    drawRoundRect(GreenActiveDot, topLeft = Offset(0f, h * 0.4f), size = Size(w * 0.22f, h * 0.6f), cornerRadius = CornerRadius(1.5f))
                    drawRoundRect(GreenActiveDot, topLeft = Offset(w * 0.38f, 0f), size = Size(w * 0.22f, h), cornerRadius = CornerRadius(1.5f))
                    drawRoundRect(GreenActiveDot, topLeft = Offset(w * 0.76f, h * 0.25f), size = Size(w * 0.22f, h * 0.75f), cornerRadius = CornerRadius(1.5f))
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = user.statusText,
                    color = Color(0xFF2E7D32),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        // Subtle divider line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.6.dp)
                .background(Color(0xFFEEEEEE)),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  8. Complete Games Home Screen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GamesHomeScreen(
    nav: NavHostController,
    pad: PaddingValues,
    profile: UserProfile,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showBackToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 3 }
    }

    Box(modifier = Modifier.fillMaxSize().background(WePlayBg)) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = 14.dp,
                end = 14.dp,
                top = 10.dp,
                bottom = pad.calculateBottomPadding() + 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── Phase 2: Top Profile Bar (Avatar, Coin Pill, King, Events) ────
            item { WePlayTopBar(nav, profile) }

            // ── Shortcuts: Ranking, Tasks, Friends ───────────────────────────
            item { WePlayShortcutsRow(nav) }

            // ── Games Section Header ─────────────────────────────────────────
            item { WePlayGamesHeader(nav) }

            // ── Phase 3: 3 Featured Game Banners (Cartouche frames) ───────────
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 1. Petrol Pump Simulator
                    WePlayFeaturedBanner(
                        background = Brush.linearGradient(listOf(Color(0xFFFFA726), Color(0xFFFB8C00), Color(0xFFEF6C00))),
                        borderBrush = Brush.linearGradient(listOf(Color(0xFFFFEE58), Color(0xFFFFB300), Color(0xFFFF8F00))),
                        title = "Petrol Pump Simulator",
                        drawIllustration = { w, h -> drawPetrolPumpSimulator(w, h) },
                        onClick = {},
                    )

                    // 2. Who's the Spy
                    WePlayFeaturedBanner(
                        background = Brush.linearGradient(listOf(Color(0xFF29B6F6), Color(0xFF0288D1), Color(0xFF01579B))),
                        borderBrush = Brush.linearGradient(listOf(Color(0xFF81D4FA), Color(0xFF29B6F6), Color(0xFF0277BD))),
                        title = "Who's the Spy",
                        drawIllustration = { w, h -> drawWhosTheSpy(w, h) },
                        onClick = { nav.navigate(Routes.SPY) },
                    )

                    // 3. Paint to Hide
                    WePlayFeaturedBanner(
                        background = Brush.linearGradient(listOf(Color(0xFFFFA726), Color(0xFFFF7043), Color(0xFFF4511E))),
                        borderBrush = Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFB300), Color(0xFFFB8C00))),
                        title = "Paint to Hide",
                        badge = "New",
                        drawIllustration = { w, h -> drawPaintToHide(w, h) },
                        onClick = {},
                    )
                }
            }

            // ── Phase 4: Two-Column Game Grid (12 Games in exact reference order) ──
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Row 1: Trickster's Cafe & Hide And Seek
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(1f)) {
                            WePlayGridCard(
                                background = Brush.linearGradient(listOf(Color(0xFF8D6E63), Color(0xFF5D4037))),
                                borderBrush = Brush.linearGradient(listOf(Color(0xFFBCAAA4), Color(0xFF8D6E63))),
                                title = "Trickster's\nCafe",
                                drawIllustration = { w, h -> drawTrickstersCafe(w, h) },
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            WePlayGridCard(
                                background = Brush.linearGradient(listOf(Color(0xFF26A69A), Color(0xFF00796B))),
                                borderBrush = Brush.linearGradient(listOf(Color(0xFF80CBC4), Color(0xFF26A69A))),
                                title = "Hide And\nSeek",
                                drawIllustration = { w, h -> drawHideAndSeek(w, h) },
                            )
                        }
                    }

                    // Row 2: Space Werewolf & Oh My Card
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(1f)) {
                            WePlayGridCard(
                                background = Brush.linearGradient(listOf(Color(0xFF7E57C2), Color(0xFF4527A0))),
                                borderBrush = Brush.linearGradient(listOf(Color(0xFFB39DDB), Color(0xFF7E57C2))),
                                title = "Space\nWerewolf",
                                drawIllustration = { w, h -> drawSpaceWerewolf(w, h) },
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            WePlayGridCard(
                                background = Brush.linearGradient(listOf(Color(0xFF66BB6A), Color(0xFF2E7D32))),
                                borderBrush = Brush.linearGradient(listOf(Color(0xFFA5D6A7), Color(0xFF43A047))),
                                title = "Oh My\nCard",
                                drawIllustration = { w, h -> drawOhMyCard(w, h) },
                            )
                        }
                    }

                    // Row 3: Ludo & Guess My Drawing
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(1f)) {
                            WePlayGridCard(
                                background = Brush.linearGradient(listOf(Color(0xFF5E35B1), Color(0xFF311B92))),
                                borderBrush = Brush.linearGradient(listOf(Color(0xFF9575CD), Color(0xFF5E35B1))),
                                title = "Ludo",
                                drawIllustration = { w, h -> drawLudoGame(w, h) },
                                onClick = { nav.navigate(Routes.LUDO) },
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            WePlayGridCard(
                                background = Brush.linearGradient(listOf(Color(0xFF4DB6AC), Color(0xFF00695C))),
                                borderBrush = Brush.linearGradient(listOf(Color(0xFF80CBC4), Color(0xFF00897B))),
                                title = "Guess My\nDrawing",
                                drawIllustration = { w, h -> drawGuessMyDrawing(w, h) },
                            )
                        }
                    }

                    // Row 4: Mic Grab & Crazy Alpaca
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(1f)) {
                            WePlayGridCard(
                                background = Brush.linearGradient(listOf(Color(0xFFEC407A), Color(0xFFC2185B))),
                                borderBrush = Brush.linearGradient(listOf(Color(0xFFF48FB1), Color(0xFFD81B60))),
                                title = "Mic Grab",
                                drawIllustration = { w, h -> drawMicGrabGame(w, h) },
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            WePlayGridCard(
                                background = Brush.linearGradient(listOf(Color(0xFF8BC34A), Color(0xFF558B2F))),
                                borderBrush = Brush.linearGradient(listOf(Color(0xFFC5E1A5), Color(0xFF689F38))),
                                title = "Crazy\nAlpaca",
                                drawIllustration = { w, h -> drawCrazyAlpacaGame(w, h) },
                            )
                        }
                    }

                    // Row 5: Jackaroo & Survival Challenge
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(1f)) {
                            WePlayGridCard(
                                background = Brush.linearGradient(listOf(Color(0xFFFFA726), Color(0xFFE65100))),
                                borderBrush = Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFB8C00))),
                                title = "Jackaroo",
                                drawIllustration = { w, h -> drawJackaroo(w, h) },
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            WePlayGridCard(
                                background = Brush.linearGradient(listOf(Color(0xFFC62828), Color(0xFF8E0000))),
                                borderBrush = Brush.linearGradient(listOf(Color(0xFFEF9A9A), Color(0xFFD32F2F))),
                                title = "Survival\nChallenge",
                                drawIllustration = { w, h -> drawSurvivalChallenge(w, h) },
                            )
                        }
                    }

                    // Row 6: Video Room & Bingo
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(1f)) {
                            WePlayGridCard(
                                background = Brush.linearGradient(listOf(Color(0xFF7E57C2), Color(0xFF3F51B5))),
                                borderBrush = Brush.linearGradient(listOf(Color(0xFFB39DDB), Color(0xFF5C6BC0))),
                                title = "Video Room",
                                drawIllustration = { w, h -> drawVideoRoom(w, h) },
                                onClick = { nav.navigate(Routes.ROOM) },
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            WePlayGridCard(
                                background = Brush.linearGradient(listOf(Color(0xFF5C6BC0), Color(0xFF283593))),
                                borderBrush = Brush.linearGradient(listOf(Color(0xFF9FA8DA), Color(0xFF3949AB))),
                                title = "Bingo",
                                badgeText = "Free Power-ups",
                                badgeColor = Color(0xFFFFEE58),
                                badgeTextColor = Color(0xFF5D4037),
                                drawIllustration = { w, h -> drawBingoGame(w, h) },
                            )
                        }
                    }
                }
            }

            // ── Phase 5: Explore Feed Section ────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Explore",
                    color = WePlayNavy,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }

            items(exploreUsers, key = { it.id }) { user ->
                ExploreFeedItem(user = user, onClick = { nav.navigate(Routes.ROOM) })
            }
        }

        // ── Floating Gift Pack Badge (Bottom-Right) ──────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = pad.calculateBottomPadding() + 8.dp)
                .shadow(6.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFFFF3E0), Color(0xFFFFE082))))
                .border(1.dp, Color(0xFFFFD54F), RoundedCornerShape(16.dp))
                .padding(horizontal = 7.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Golden Gift Pack icon
                Canvas(Modifier.size(24.dp)) {
                    val cx = size.width / 2f; val cy = size.height / 2f
                    drawRoundRect(Color(0xFFFFB300), topLeft = Offset(cx - size.width * 0.35f, cy - size.height * 0.15f), size = Size(size.width * 0.7f, size.height * 0.55f), cornerRadius = CornerRadius(3f))
                    drawRoundRect(Color(0xFFFFD54F), topLeft = Offset(cx - size.width * 0.38f, cy - size.height * 0.3f), size = Size(size.width * 0.76f, size.height * 0.18f), cornerRadius = CornerRadius(2f))
                    // Red ribbon
                    drawRect(Color(0xFFE53935), topLeft = Offset(cx - size.width * 0.08f, cy - size.height * 0.3f), size = Size(size.width * 0.16f, size.height * 0.7f))
                }
                Text("Gift Pack", color = Color(0xFFE65100), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .background(Color(0xCC000000), RoundedCornerShape(6.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                    Text("959 d 20 h", color = Color(0xFFFFD54F), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ── Floating "Back to top" Button (Bottom-Left when scrolled) ────────
        AnimatedVisibility(
            visible = showBackToTop,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, bottom = pad.calculateBottomPadding() + 10.dp),
        ) {
            Box(
                modifier = Modifier
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFF00CCF9))
                    .clickable { scope.launch { listState.animateScrollToItem(0) } }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("↑", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(3.dp))
                    Text("Back to top", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Backwards compatibility alias for MainActivity ───────────────────────────
@Composable
fun GameLobbyScreen(
    nav: NavHostController,
    pad: PaddingValues,
    profile: UserProfile,
) = GamesHomeScreen(nav, pad, profile)
