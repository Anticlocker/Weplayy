package com.sundram.weplay.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sundram.weplay.R
import com.sundram.weplay.data.Demo
import com.sundram.weplay.data.UserProfile
import com.sundram.weplay.ui.components.InitialsAvatar
import com.sundram.weplay.ui.components.avatarGradient
import com.sundram.weplay.ui.theme.CyanBrand
import com.sundram.weplay.ui.theme.Gold
import com.sundram.weplay.ui.theme.KtvRed
import com.sundram.weplay.ui.theme.LiveRed
import com.sundram.weplay.ui.theme.PinkGift
import com.sundram.weplay.voiceroom.RoomConnection
import com.sundram.weplay.voiceroom.RoomMemberVo
import com.sundram.weplay.voiceroom.VoiceRoomState
import com.sundram.weplay.voiceroom.VoiceRoomViewModel
import kotlin.math.cos
import kotlin.math.sin

// ─── Room background colors ───────────────────────────────────────────────────
private val RoomBgTop    = Color(0xFF16082E)
private val RoomBgMid    = Color(0xFF1A0E3D)
private val RoomBgLower  = Color(0xFF0D1B4B)
private val RoomBgBottom = Color(0xFF0A1628)
private val ChatPanelBg  = Color(0x99000000)
private val GlowCyan     = Color(0xFF00CCF9)
private val GlowPurple   = Color(0xFF6C2BD9)
private val SeatEmptyBg  = Color(0xFF251840)
private val SeatEmptyStroke = Color(0xFF4A3A6A)
private val InputBarBg   = Color(0xFF0D1428)

/**
 * Live voice room: dark-themed redesign matching the WePlay visual reference.
 * Preserves all existing ViewModel wiring, seat interaction, mic toggle, and chat.
 */
@Composable
fun LiveVoiceRoomScreen(
    nav: NavHostController,
    roomId: String?,
    profile: UserProfile,
) {
    val id = roomId?.takeIf { it.isNotBlank() } ?: Demo.rooms.first().id
    val context = LocalContext.current
    val vm: VoiceRoomViewModel = viewModel(
        factory = VoiceRoomViewModel.factory(
            context = context,
            roomId = id,
            identity = profile.id,
            displayName = profile.name,
        ),
    )
    val state by vm.state.collectAsState()
    val room = Demo.rooms.firstOrNull { it.id == id } ?: Demo.rooms.first()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            vm.onMicrophonePermissionResult(true)
        } else {
            Toast.makeText(context, "Microphone permission is needed to talk.", Toast.LENGTH_SHORT).show()
        }
    }

    // Seat click handler
    val onSeatClick: (Int) -> Unit = { seat ->
        val occupant = state.seatAt(seat)
        when {
            occupant == null -> vm.takeSeat(seat)
            occupant.isSelf  -> vm.leaveSeat()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(RoomBgTop, RoomBgMid, RoomBgLower, RoomBgBottom),
                ),
            ),
    ) {
        // ── Atmospheric glow layer ─────────────────────────────────────────
        RoomAtmosphere()

        // ── Star particles ─────────────────────────────────────────────────
        StarParticles()

        // ── Main layout ────────────────────────────────────────────────────
        Column(Modifier.fillMaxSize()) {

            // Header
            RoomHeader(nav = nav, title = room.title, roomId = id, state = state)

            // Connection error banner
            if (state.error != null) {
                ConnectionBanner(state = state, onRetry = vm::reconnect)
            }

            // Host seat (seat 0) — large, centered, with crown
            HostSeat(
                member = state.seatAt(0),
                onClick = { onSeatClick(0) },
            )

            Spacer(Modifier.height(10.dp))

            // Row of 4 seats (seats 1–4)
            SeatRow(seats = 1..4, state = state, onSeatClick = onSeatClick)

            Spacer(Modifier.height(10.dp))

            // Row of 4 seats (seats 5–8) — typically empty "+" buttons
            SeatRow(seats = 5..8, state = state, onSeatClick = onSeatClick)

            Spacer(Modifier.height(12.dp))

            // Chat panel — expands to fill remaining space
            DarkChatPanel(state = state, modifier = Modifier.weight(1f))

            // Bottom input bar
            VoiceRoomInputBar(
                state = state,
                onToggleMic = {
                    vm.toggleMicrophone {
                        if (vm.usesLiveServer) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                onSend = vm::sendMessage,
            )
        }
    }
}

// ─── Atmospheric glow orbs ────────────────────────────────────────────────────
@Composable
private fun RoomAtmosphere() {
    val infinite = rememberInfiniteTransition(label = "atm")
    val pulse by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue  = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    Canvas(Modifier.fillMaxSize()) {
        // Upper-left purple orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(GlowPurple.copy(alpha = 0.35f * pulse), Color.Transparent),
                center = Offset(size.width * 0.1f, size.height * 0.18f),
                radius = size.width * 0.65f,
            ),
            center = Offset(size.width * 0.1f, size.height * 0.18f),
            radius = size.width * 0.65f,
        )
        // Center-right cyan orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(GlowCyan.copy(alpha = 0.18f * pulse), Color.Transparent),
                center = Offset(size.width * 0.85f, size.height * 0.3f),
                radius = size.width * 0.5f,
            ),
            center = Offset(size.width * 0.85f, size.height * 0.3f),
            radius = size.width * 0.5f,
        )
        // Lower-center blue orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1A3080).copy(alpha = 0.4f), Color.Transparent),
                center = Offset(size.width * 0.5f, size.height * 0.75f),
                radius = size.width * 0.55f,
            ),
            center = Offset(size.width * 0.5f, size.height * 0.75f),
            radius = size.width * 0.55f,
        )
    }
}

// ─── Star particle layer (static, seeded) ─────────────────────────────────────
@Composable
private fun StarParticles() {
    val stars = remember {
        val rng = java.util.Random(42L)
        List(40) {
            Triple(
                rng.nextFloat(), // x fraction
                rng.nextFloat(), // y fraction — concentrate in upper 70%
                rng.nextFloat() * 2.5f + 1f, // radius dp
            )
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        stars.forEach { (xf, yf, r) ->
            drawCircle(
                color = Color.White.copy(alpha = 0.4f + xf * 0.3f),
                radius = r,
                center  = Offset(xf * size.width, yf * size.height * 0.75f),
            )
        }
    }
}

// ─── Room header ──────────────────────────────────────────────────────────────
@Composable
private fun RoomHeader(
    nav: NavHostController,
    title: String,
    roomId: String,
    state: VoiceRoomState,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Back button
        CircleIconButton(icon = R.drawable.ic_back, description = "Back") { nav.popBackStack() }

        Spacer(Modifier.width(10.dp))

        // Room avatar / title thumbnail
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(avatarGradient(title))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                title.take(1).uppercase(),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.width(8.dp))

        // Title + ID stack
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "ID: ${roomId.take(6).uppercase()}",
                color = Color(0xFFB0BAD4),
                fontSize = 11.sp,
            )
        }

        // Share icon
        CircleIconButton(icon = R.drawable.ic_send, description = "Share room") { /* share */ }

        Spacer(Modifier.width(6.dp))

        // Listener count pill
        ListenerPill(state)
    }
}

@Composable
private fun ListenerPill(state: VoiceRoomState) {
    val label = when (state.connection) {
        RoomConnection.CONNECTED, RoomConnection.SIMULATED ->
            "${state.listenerCount}  🔥"
        RoomConnection.CONNECTING, RoomConnection.RECONNECTING -> "…"
        else -> "Offline"
    }
    val pillColor = when (state.connection) {
        RoomConnection.CONNECTED, RoomConnection.SIMULATED -> Color(0xFF1A2650)
        else -> Color(0xFF3A1020)
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(pillColor)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CircleIconButton(icon: Int, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0x33FFFFFF))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}

// ─── Connection error banner ───────────────────────────────────────────────────
@Composable
private fun ConnectionBanner(state: VoiceRoomState, onRetry: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xBB1A0E3D))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                state.transportLabel,
                color = Color(0xFF8899BB),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                state.error.orEmpty(),
                color = Color.White,
                fontSize = 12.sp,
                maxLines = 2,
            )
        }
        if (state.connection == RoomConnection.FAILED ||
            state.connection == RoomConnection.DISCONNECTED
        ) {
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyanBrand)
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Retry", color = Color(0xFF06202B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Host seat ────────────────────────────────────────────────────────────────
@Composable
private fun HostSeat(member: RoomMemberVo?, onClick: () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "hostGlow")
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.3f,
        targetValue  = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(110.dp).clickable(onClick = onClick),
        ) {
            // Outer glow ring
            Box(
                Modifier
                    .size(108.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(CyanBrand.copy(alpha = glowAlpha * 0.5f), Color.Transparent),
                            radius = 160f,
                        ),
                        CircleShape,
                    ),
            )
            // Ring borders — concentric
            Box(
                Modifier
                    .size(102.dp)
                    .border(2.dp, CyanBrand.copy(alpha = glowAlpha * 0.4f), CircleShape),
            )
            Box(
                Modifier
                    .size(92.dp)
                    .border(2.dp, CyanBrand.copy(alpha = glowAlpha * 0.6f), CircleShape),
            )
            Box(
                Modifier
                    .size(82.dp)
                    .border(2.dp, CyanBrand.copy(alpha = 0.85f), CircleShape),
            )

            // Avatar
            if (member != null) {
                InitialsAvatar(name = member.name, size = 76.dp)
            } else {
                // Empty host seat placeholder
                Box(
                    Modifier
                        .size(76.dp)
                        .background(SeatEmptyBg, CircleShape)
                        .border(2.dp, SeatEmptyStroke, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_plus),
                        contentDescription = "Take host seat",
                        tint = Color(0xFF8899BB),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            // Crown badge at top-center
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .size(26.dp)
                    .background(Color(0xFFFFD54A), CircleShape)
                    .border(1.5.dp, Color(0xFFFFC107), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("👑", fontSize = 13.sp, textAlign = TextAlign.Center)
            }

            // Mic-off badge bottom-right
            if (member != null && !member.micOn) {
                MicOffBadge(Modifier.align(Alignment.BottomEnd), 20.dp)
            }
        }

        Spacer(Modifier.height(4.dp))

        // "Host" label pill
        Box(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1A0E3D))
                .border(1.dp, Gold.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 2.dp),
        ) {
            Text(
                "Host",
                color = Gold,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(Modifier.height(3.dp))
        Text(
            member?.name ?: "Open",
            color = if (member != null) Color.White else Color(0xFF8899BB),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ─── Seat row ─────────────────────────────────────────────────────────────────
@Composable
private fun SeatRow(seats: IntRange, state: VoiceRoomState, onSeatClick: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        seats.forEach { seat ->
            SeatView(
                member = state.seatAt(seat),
                onClick = { onSeatClick(seat) },
            )
        }
    }
}

@Composable
private fun SeatView(member: RoomMemberVo?, onClick: () -> Unit) {
    val name = when {
        member == null -> "Open"
        member.isSelf  -> "You"
        else           -> member.name
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(62.dp).clickable(onClick = onClick),
        ) {
            if (member != null) {
                // Speaking ring
                if (member.speaking) {
                    Box(
                        Modifier
                            .size(60.dp)
                            .border(2.dp, CyanBrand, CircleShape),
                    )
                }
                InitialsAvatar(name = member.name, size = 52.dp)
            } else {
                // Empty seat — dark circle with "+"
                Box(
                    Modifier
                        .size(52.dp)
                        .background(SeatEmptyBg, CircleShape)
                        .border(1.5.dp, SeatEmptyStroke, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_plus),
                        contentDescription = "Take seat",
                        tint = Color(0xFF8899BB),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            // Mic-off badge
            if (member != null && !member.micOn) {
                MicOffBadge(Modifier.align(Alignment.BottomEnd), 16.dp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            name,
            color = if (member != null) Color.White else Color(0xFF8899BB),
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MicOffBadge(modifier: Modifier, size: Dp) {
    Box(
        modifier
            .size(size)
            .background(LiveRed, CircleShape)
            .border(1.5.dp, Color(0xFF0A1628), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.ic_mic_off),
            contentDescription = "Muted",
            tint = Color.White,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}

// ─── Translucent chat panel ───────────────────────────────────────────────────
@Composable
private fun DarkChatPanel(state: VoiceRoomState, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val atBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible >= info.totalItemsCount - 1
        }
    }
    val stickToBottom = atBottom
    LaunchedEffect(state.messages.size) {
        val newest = state.messages.lastOrNull()
        if (newest != null && (stickToBottom || newest.isSelf)) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(ChatPanelBg),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.messages) { message ->
                Row(verticalAlignment = Alignment.Top) {
                    if (!message.system) {
                        InitialsAvatar(message.author, size = 28.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Column {
                        Text(
                            if (message.system) "Room" else message.author,
                            color = when {
                                message.system -> Gold
                                message.isSelf -> KtvRed
                                else           -> CyanBrand
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            message.body,
                            color = if (message.system) Color(0xFFB0BAD4) else Color.White,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }
}

// ─── Bottom input bar ─────────────────────────────────────────────────────────
@Composable
private fun VoiceRoomInputBar(
    state: VoiceRoomState,
    onToggleMic: () -> Unit,
    onSend: (String) -> Unit,
) {
    var draft by remember { mutableStateOf("") }

    Row(
        Modifier
            .fillMaxWidth()
            .background(InputBarBg)
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Message input pill
        Box(
            Modifier
                .weight(1f)
                .height(44.dp)
                .background(Color(0xFF1E2A4A), RoundedCornerShape(22.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (draft.isEmpty()) {
                Text("Say something…", color = Color(0xFF8899BB), fontSize = 13.sp)
            }
            BasicTextField(
                value = draft,
                onValueChange = { draft = it },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Mic toggle button
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (state.selfMicOn) CyanBrand else LiveRed)
                .clickable(onClick = onToggleMic),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(if (state.selfMicOn) R.drawable.ic_mic else R.drawable.ic_mic_off),
                contentDescription = if (state.selfMicOn) "Mute microphone" else "Unmute microphone",
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }

        // Gift button (UI only — no real transaction)
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(PinkGift),
            contentAlignment = Alignment.Center,
        ) {
            Text("🎁", fontSize = 18.sp, textAlign = TextAlign.Center)
        }

        // Send button (shows only when draft is not blank)
        if (draft.isNotBlank()) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(CyanBrand)
                    .clickable {
                        onSend(draft)
                        draft = ""
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_send),
                    contentDescription = "Send message",
                    tint = Color(0xFF06202B),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
