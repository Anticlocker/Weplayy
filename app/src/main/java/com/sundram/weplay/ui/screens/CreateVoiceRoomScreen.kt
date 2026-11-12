package com.sundram.weplay.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sundram.weplay.R
import com.sundram.weplay.ui.theme.CyanBrand
import com.sundram.weplay.ui.theme.Gold
import com.sundram.weplay.ui.theme.KtvRed
import com.sundram.weplay.ui.theme.PageWhite
import com.sundram.weplay.ui.theme.Stroke
import com.sundram.weplay.ui.theme.TextBody
import com.sundram.weplay.ui.theme.TextMain
import com.sundram.weplay.ui.theme.TextMuted
import com.sundram.weplay.ui.theme.WarnOrange

private val NewBadge = Color(0xFFFF5959)
private val ModeUnselectedBg = Color(0xFFF5F5F9)
private val ModeSelectedBg = Color(0xFFE3F2FD)
private val TagBg = Color(0xFFF5F5F9)

private data class RoomMode(
    val label: String,
    val icon: @Composable (Modifier) -> Unit,
    val new: Boolean = false,
)

private val roomModes = listOf(
    RoomMode("Normal", { drawGradCap(it) }, new = false),
    RoomMode("Acquaint", { drawHearts(it) }, new = false),
    RoomMode("Auction", { drawGavel(it) }, new = false),
    RoomMode("Video", { drawTv(it) }, new = false),
    RoomMode("Games", { drawGamepad(it) }, new = true),
)

private data class RoomTag(
    val label: String,
)

private val roomTags = listOf(
    RoomTag("Friends"),
    RoomTag("Music"),
    RoomTag("Game"),
    RoomTag("Chats"),
)

/** Create Voice Room screen — UI only, no backend logic. */
@Composable
fun CreateVoiceRoomScreen(
    nav: NavHostController,
) {
    var roomTitle by remember { mutableStateOf("") }
    var passwordEnabled by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf(0) }
    var selectedTag by remember { mutableStateOf(-1) }

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
                .padding(top = 8.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = "Close",
                tint = TextMain,
                modifier = Modifier
                    .size(40.dp)
                    .clickable { nav.popBackStack() },
            )
            Spacer(Modifier.weight(1f))
            Text(
                "Create Voice Room",
                color = TextMain,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(40.dp))
        }

        Spacer(Modifier.height(20.dp))

        // Room title input
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(Color(0xFFF5F6FA), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.TextField(
                value = roomTitle,
                onValueChange = { roomTitle = it },
                placeholder = { Text("Enter room title", color = TextMuted) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                textStyle = TextStyle(
                    color = TextMain,
                    fontSize = 15.sp,
                ),
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
        }

        Spacer(Modifier.height(24.dp))

        // Password toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Password",
                color = TextMain,
                fontSize = 15.sp,
            )
            Spacer(Modifier.weight(1f))
            Switch(
                checked = passwordEnabled,
                onCheckedChange = { passwordEnabled = it },
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    uncheckedThumbColor = Color(0xFFD1D5DB),
                    uncheckedTrackColor = Color(0xFFE5E7EB),
                    checkedThumbColor = CyanBrand,
                    checkedTrackColor = CyanBrand.copy(alpha = 0.5f),
                ),
            )
        }

        Spacer(Modifier.height(24.dp))

        // Room mode section
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Text(
                "Room mode",
                color = TextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                roomModes.forEachIndexed { index, mode ->
                    ModeChip(
                        mode = mode,
                        selected = selectedMode == index,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedMode = index },
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Select Room Tag section
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Text(
                "Select Room Tag",
                color = TextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                roomTags.forEachIndexed { index, tag ->
                    TagChip(
                        tag = tag,
                        selected = selectedTag == index,
                        onClick = { selectedTag = index },
                    )
                }
            }
        }

        Spacer(Modifier.height(30.dp))

        // Advanced link
        TextButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Create Advanced Voice Room",
                    color = Gold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = Gold,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(Modifier.height(30.dp))

        // Confirm button
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(CyanBrand, RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Confirm",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ModeChip(
    mode: RoomMode,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .height(110.dp)
            .background(
                if (selected) ModeSelectedBg else ModeUnselectedBg,
                RoundedCornerShape(14.dp),
            )
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) CyanBrand else Stroke,
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            mode.icon(Modifier.size(24.dp))
            if (mode.new) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(20.dp)
                        .background(NewBadge, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "NEW",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            mode.label,
            color = if (selected) CyanBrand else TextBody,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

/** Simple vector icons drawn on Canvas so no new drawable assets are needed. */
@Composable
private fun drawGradCap(modifier: Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            drawRoundRect(
                CyanBrand,
                topLeft = Offset(cx - w * 0.35f, cy - h * 0.35f),
                size = Size(w * 0.7f, h * 0.25f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.04f, w * 0.04f),
            )
            drawRoundRect(
                Gold,
                topLeft = Offset(cx - w * 0.06f, cy - h * 0.1f),
                size = Size(w * 0.12f, h * 0.35f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f, w * 0.06f),
            )
            drawCircle(CyanBrand, radius = w * 0.04f, center = Offset(cx, cy - h * 0.28f))
        }
    }
}

@Composable
private fun drawHearts(modifier: Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val r = w * 0.18f
            drawCircle(KtvRed, radius = r, center = Offset(cx - r * 0.55f, cy - r * 0.2f))
            drawCircle(KtvRed, radius = r, center = Offset(cx + r * 0.55f, cy - r * 0.2f))
            val heartPath = Path().apply {
                moveTo(cx - r * 0.7f, cy + r * 0.1f)
                cubicTo(cx - r * 0.9f, cy - r * 0.2f, cx - r * 0.6f, cy - r * 0.6f, cx, cy + r * 0.1f)
                cubicTo(cx + r * 0.6f, cy - r * 0.6f, cx + r * 0.9f, cy - r * 0.2f, cx + r * 0.7f, cy + r * 0.1f)
                close()
            }
            drawPath(heartPath, KtvRed)
        }
    }
}

@Composable
private fun drawGavel(modifier: Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            drawRoundRect(
                Color(0xFF8B5A2B),
                topLeft = Offset(cx - w * 0.1f, cy - h * 0.3f),
                size = Size(w * 0.2f, h * 0.5f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.1f, w * 0.1f),
            )
            drawRoundRect(
                Color(0xFF5D4037),
                topLeft = Offset(cx - w * 0.25f, cy + h * 0.1f),
                size = Size(w * 0.5f, h * 0.2f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f, w * 0.05f),
            )
        }
    }
}

@Composable
private fun drawTv(modifier: Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            drawRoundRect(
                WarnOrange,
                topLeft = Offset(cx - w * 0.3f, cy - h * 0.3f),
                size = Size(w * 0.6f, h * 0.6f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f, w * 0.06f),
            )
            drawRoundRect(
                WarnOrange,
                topLeft = Offset(cx - w * 0.08f, cy + h * 0.25f),
                size = Size(w * 0.16f, h * 0.15f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.04f, w * 0.04f),
            )
        }
    }
}

@Composable
private fun drawGamepad(modifier: Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            drawRoundRect(
                CyanBrand,
                topLeft = Offset(cx - w * 0.35f, cy - h * 0.25f),
                size = Size(w * 0.7f, h * 0.5f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.15f, w * 0.15f),
            )
            drawCircle(Color.White.copy(alpha = 0.2f), radius = w * 0.06f, center = Offset(cx + w * 0.15f, cy - h * 0.1f))
            drawCircle(Color.White.copy(alpha = 0.2f), radius = w * 0.06f, center = Offset(cx + w * 0.15f, cy + h * 0.1f))
            drawCircle(Color.White.copy(alpha = 0.2f), radius = w * 0.06f, center = Offset(cx - w * 0.15f, cy - h * 0.1f))
            drawCircle(Color.White.copy(alpha = 0.2f), radius = w * 0.06f, center = Offset(cx - w * 0.15f, cy + h * 0.1f))
        }
    }
}

@Composable
private fun TagChip(
    tag: RoomTag,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .clickable(onClick = onClick)
            .background(
                if (selected) Color.White else TagBg,
                RoundedCornerShape(20.dp),
            )
            .border(
                if (selected) 1.dp else 0.dp,
                if (selected) CyanBrand else Stroke,
                RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            tag.label,
            color = if (selected) CyanBrand else TextBody,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
