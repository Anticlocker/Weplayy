package com.sundram.weplay.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sundram.weplay.R
import com.sundram.weplay.data.ChatMessage
import com.sundram.weplay.data.MessagingStore
import com.sundram.weplay.ui.components.InitialsAvatar
import com.sundram.weplay.ui.components.RoundIconButton
import com.sundram.weplay.ui.theme.CyanBrand
import com.sundram.weplay.ui.theme.Gold
import com.sundram.weplay.ui.theme.KtvRed
import com.sundram.weplay.ui.theme.PageWhite
import com.sundram.weplay.ui.theme.Stroke
import com.sundram.weplay.ui.theme.TextBody
import com.sundram.weplay.ui.theme.TextMain
import com.sundram.weplay.ui.theme.BgSoft
import com.sundram.weplay.ui.theme.CardWhite
import com.sundram.weplay.ui.theme.ChipBg
import com.sundram.weplay.ui.theme.TextMuted
import com.sundram.weplay.ui.theme.Divider
import com.sundram.weplay.ui.theme.WallBlue
import com.sundram.weplay.ui.theme.WallMint
import com.sundram.weplay.ui.theme.WallPurple
import kotlinx.coroutines.delay

import com.sundram.weplay.ui.util.formatDayLabel
import com.sundram.weplay.ui.util.formatMessageTime

private val REPORT_REASONS = listOf(
    "Harassment",
    "Inappropriate content",
    "Fake profile",
    "Other",
)

private sealed interface ChatItem {
    data class Msg(val m: ChatMessage) : ChatItem
    data class DateSep(val label: String) : ChatItem
}

private fun buildChatItems(messages: List<ChatMessage>): List<ChatItem> {
    val out = mutableListOf<ChatItem>()
    var lastDay: String? = null
    for (m in messages) {
        val day = formatDayLabel(m.ts)
        if (day != lastDay) {
            out += ChatItem.DateSep(day)
            lastDay = day
        }
        out += ChatItem.Msg(m)
    }
    return out
}

private enum class RecorderState { IDLE, RECORDING }

private object VoiceRecorder {
    private var recorder: android.media.MediaRecorder? = null
    private var startTs: Long = 0L
    private var outputFile: java.io.File? = null

    fun start(context: android.content.Context) {
        cancel()
        outputFile = java.io.File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            .apply { parentFile?.mkdirs() }
        val r = android.media.MediaRecorder()
        try {
            r.setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(64_000)
            r.setAudioSamplingRate(44_100)
            r.setOutputFile(outputFile?.absolutePath)
            r.prepare()
            r.start()
        } catch (t: Throwable) {
            runCatching { r.release() }
            outputFile?.delete()
            outputFile = null
            throw t
        }
        recorder = r
        startTs = System.currentTimeMillis()
    }

    fun stop(): Pair<String?, Long> {
        val r = recorder ?: return null to 0L
        val f = outputFile
        val duration = System.currentTimeMillis() - startTs
        runCatching { r.stop() }
        runCatching { r.release() }
        recorder = null
        outputFile = null
        return (if (f?.exists() == true) f.absolutePath else null) to duration
    }

    fun cancel() {
        val r = recorder
        if (r != null) {
            runCatching { r.stop() }
            runCatching { r.release() }
            recorder = null
        }
        outputFile?.delete()
        outputFile = null
        startTs = 0L
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    nav: NavController,
    userId: String,
    store: MessagingStore,
    profile: com.sundram.weplay.data.UserProfile? = null,
    onSaveCoins: (Long) -> Unit = {},
) {
    var version by remember { mutableStateOf(0) }
    var input by remember(userId) { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmBlock by remember { mutableStateOf(false) }
    var confirmReport by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf(REPORT_REASONS.first()) }
    var showGiftPicker by remember { mutableStateOf(false) }
    var showPhotoPicker by remember { mutableStateOf(false) }
    var coinBalance by remember(userId) { mutableStateOf(profile?.coins ?: 500L) }
    LaunchedEffect(profile?.coins) {
        profile?.coins?.let { coinBalance = it }
    }
    var recordingState by remember { mutableStateOf(RecorderState.IDLE) }
    val context = androidx.compose.ui.platform.LocalContext.current

    fun bump() { version++ }

    fun sendInput() {
        val text = input.trim()
        if (text.isEmpty()) return
        if (store.sendMessage(userId, text) != null) {
            input = ""
            store.saveDraft(userId, "")
            bump()
        }
    }

    fun startRecording() {
        recordingState = RecorderState.RECORDING
        runCatching { VoiceRecorder.start(context) }.onFailure {
            recordingState = RecorderState.IDLE
        }
    }

    fun stopRecording() {
        val (path, durationMs) = VoiceRecorder.stop()
        recordingState = RecorderState.IDLE
        if (path != null && durationMs >= 1000) {
            store.sendMedia(userId, com.sundram.weplay.data.MsgType.VOICE, path, durationMs)
            bump()
        }
    }

    val micPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) startRecording()
    }

    fun requestMicAndRecord() {
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.RECORD_AUDIO,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) startRecording() else micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
    }

    val currentRecordingState by androidx.compose.runtime.rememberUpdatedState(recordingState)
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            if (currentRecordingState == RecorderState.RECORDING) VoiceRecorder.stop()
        }
    }

    val user = store.userById(userId)
    val chat = remember(version, userId) { if (userId.isNotEmpty()) store.openChat(userId) else null }
    val messages = chat?.messages ?: emptyList()
    val chatItems = buildChatItems(messages)
    val blocked = remember(version, userId) { store.isBlocked(userId) }
    val listState = rememberLazyListState()
    val atBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible >= info.totalItemsCount - 1
        }
    }

    val stickToBottom = atBottom
    LaunchedEffect(chatItems.size) {
        if (chatItems.isNotEmpty() && (stickToBottom || messages.lastOrNull()?.fromMe == true)) {
            listState.animateScrollToItem(chatItems.lastIndex)
        }
    }

    LaunchedEffect(userId) {
        while (true) {
            delay(1000)
            if (store.flushDueReplies()) bump()
        }
    }

    DisposableEffect(Unit) {
        onDispose { VoiceRecorder.cancel() }
    }

    // restore draft
    LaunchedEffect(userId) {
        input = (store.chat(userId)?.draft ?: "")
    }

    val bg = wallBrush(chat?.wallpaper ?: 0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
    ) {
        // ---------- header ----------
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(CardWhite.copy(alpha = 0.92f))
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            RoundIconButton(
                painter = painterResource(R.drawable.ic_back),
                onClick = { nav.popBackStack() },
                contentDescription = "Back",
                bg = Color.Transparent,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable { nav.navigate("user/$userId") },
            ) {
                InitialsAvatar(user?.name ?: userId, size = 40.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        user?.name ?: userId,
                        color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        when {
                            blocked -> "Blocked"
                            user?.online == true -> "Online now"
                            else -> "Offline"
                        },
                        color = TextMuted, fontSize = 11.sp,
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            Box {
                RoundIconButton(
                    painter = painterResource(R.drawable.ic_settings),
                    onClick = { menuOpen = true },
                    contentDescription = "Chat menu",
                    bg = Color.Transparent,
                )
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    containerColor = CardWhite,
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (chat?.pinned == true) "Unpin chat" else "Pin chat",
                                color = TextMain,
                            )
                        },
                        onClick = {
                            store.setPinned(userId, chat?.pinned != true)
                            menuOpen = false; bump()
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (chat?.muted == true) "Unmute" else "Mute notifications",
                                color = TextMain,
                            )
                        },
                        onClick = {
                            store.setMuted(userId, chat?.muted != true)
                            menuOpen = false; bump()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Change wallpaper", color = TextMain) },
                        onClick = { store.cycleWallpaper(userId); menuOpen = false; bump() },
                    )
                    DropdownMenuItem(
                        text = { Text("View profile", color = TextMain) },
                        onClick = { menuOpen = false; nav.navigate("user/$userId") },
                    )
                    DropdownMenuItem(
                        text = { Text("Clear chat", color = TextMain) },
                        onClick = { menuOpen = false; confirmClear = true },
                    )
                    DropdownMenuItem(
                        text = { Text("Block", color = KtvRed) },
                        onClick = { menuOpen = false; confirmBlock = true },
                    )
                    DropdownMenuItem(
                        text = { Text("Report", color = KtvRed) },
                        onClick = { menuOpen = false; confirmReport = true },
                    )
                }
            }
        }

        // ---------- messages ----------
        if (blocked) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "You blocked ${user?.name ?: userId}",
                        color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Unblock them to chat again.",
                        color = TextMuted, fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                    Box(
                        Modifier
                            .background(CyanBrand, RoundedCornerShape(12.dp))
                            .clickable {
                                store.unblock(userId); bump()
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Text("Unblock", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 30.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "Say hello to ${user?.name ?: userId}",
                                color = TextMuted, fontSize = 13.sp,
                            )
                        }
                    }
                }
                items(chatItems, key = {
                    when (it) {
                        is ChatItem.Msg -> "m" + it.m.id
                        is ChatItem.DateSep -> "d" + it.label
                    }
                }) { item ->
                    when (item) {
                        is ChatItem.DateSep -> {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Box(
                                    Modifier
                                        .background(BgSoft, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Text(item.label, color = TextMuted, fontSize = 11.sp)
                                }
                            }
                        }

                        is ChatItem.Msg -> MessageBubble(
                            m = item.m,
                            onDelete = {
                                store.deleteMessageForMe(userId, item.m.id)
                                bump()
                            },
                            onRecall = {
                                store.recallMessage(userId, item.m.id)
                                bump()
                            },
                        )
                    }
                }
                if (store.isTyping(userId)) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${user?.name ?: userId} is typing…",
                                color = TextMuted, fontSize = 11.sp,
                            )
                        }
                    }
                }
            }

            // ---------- input ----------
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardWhite.copy(alpha = 0.92f))
                    .imePadding()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(BgSoft, RoundedCornerShape(22.dp))
                        .border(1.dp, Color.Transparent, RoundedCornerShape(22.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it; store.saveDraft(userId, it) },
                        singleLine = false,
                        textStyle = TextStyle(color = TextMain, fontSize = 15.sp),
                        cursorBrush = SolidColor(CyanBrand),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = { sendInput() },
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { field ->
                            Box {
                                if (input.isEmpty()) {
                                    Text("Message...", color = Divider, fontSize = 14.sp)
                                }
                                field()
                            }
                        },
                    )
                }
                Spacer(Modifier.width(4.dp))
                IconButtonRow(icon = R.drawable.ic_gift, desc = "Send gift") { showGiftPicker = true }
                Spacer(Modifier.width(4.dp))
                IconButtonRow(icon = R.drawable.ic_camera, desc = "Send photo") { showPhotoPicker = true }
                Spacer(Modifier.width(4.dp))
                val isRecording = recordingState == RecorderState.RECORDING
                if (input.isBlank()) {
                    IconButtonRow(
                        icon = R.drawable.ic_mic,
                        desc = if (isRecording) "Stop voice note" else "Voice note",
                    ) {
                        if (isRecording) stopRecording() else requestMicAndRecord()
                    }
                } else {
                    Spacer(Modifier.width(4.dp))
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(CyanBrand)
                            .clickable { sendInput() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_send),
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }

    // ---------- bottom sheets ----------
    if (showGiftPicker) {
        GiftPickerSheet(
            balance = coinBalance,
            onDismiss = { showGiftPicker = false },
            onSelect = { gift ->
                val ok = store.sendGift(userId, gift.id) { newBal ->
                    coinBalance = newBal
                    onSaveCoins(newBal)
                }
                if (ok) bump()
                showGiftPicker = false
            },
        )
    }
    if (showPhotoPicker) {
        PhotoPickerSheet(
            onDismiss = { showPhotoPicker = false },
            onPick = { path ->
                store.sendMedia(userId, com.sundram.weplay.data.MsgType.IMAGE, path)
                bump()
                showPhotoPicker = false
            },
        )
    }

    // ---------- dialogs ----------
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear chat?", color = TextMain) },
            text = { Text("All messages in this chat will be deleted.", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = { store.clearChat(userId); confirmClear = false; bump() }) {
                    Text("Clear", color = KtvRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel", color = TextMuted) }
            },
            containerColor = CardWhite,
        )
    }

    if (confirmBlock) {
        AlertDialog(
            onDismissRequest = { confirmBlock = false },
            title = { Text("Block ${user?.name ?: userId}?", color = TextMain) },
            text = {
                Text(
                    "They will not be able to message you or see your profile. " +
                        "You will be removed from each other's friends.",
                    color = TextMuted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    store.block(userId)
                    confirmBlock = false
                    nav.popBackStack()
                }) { Text("Block", color = KtvRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmBlock = false }) { Text("Cancel", color = TextMuted) }
            },
            containerColor = CardWhite,
        )
    }

    if (confirmReport) {
        AlertDialog(
            onDismissRequest = { confirmReport = false },
            title = { Text("Report ${user?.name ?: userId}", color = TextMain) },
            text = {
                Column {
                    Text("Pick a reason. The user will also be blocked.", color = TextMuted, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    REPORT_REASONS.forEach { r ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reportReason = r }
                                .padding(vertical = 6.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(18.dp)
                                    .border(
                                        1.dp,
                                        if (reportReason == r) CyanBrand else TextMuted,
                                        CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (reportReason == r) {
                                    Box(Modifier.size(10.dp).background(CyanBrand, CircleShape))
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(r, color = TextMain, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    store.report(userId, reportReason)
                    confirmReport = false
                    nav.popBackStack()
                }) { Text("Report & Block", color = KtvRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReport = false }) { Text("Cancel", color = TextMuted) }
            },
            containerColor = CardWhite,
        )
    }
}

@ExperimentalFoundationApi
@Composable
private fun MessageBubble(
    m: ChatMessage,
    onDelete: () -> Unit = {},
    onRecall: () -> Unit = {},
) {
    var showMenu by remember { mutableStateOf(false) }
    val fromMe = m.fromMe
    val withinRecallWindow = System.currentTimeMillis() - m.ts <= 120_000L

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromMe) Arrangement.End else Arrangement.Start,
    ) {
        Box {
            Box(
                Modifier
                    .background(
                        if (fromMe) CyanBrand else BgSoft,
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (fromMe) 16.dp else 4.dp,
                            bottomEnd = if (fromMe) 4.dp else 16.dp,
                        ),
                    )
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { showMenu = true },
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                // long-press menu
                if (showMenu) {
                    DropdownMenu(
                        expanded = true,
                        onDismissRequest = { showMenu = false },
                        containerColor = CardWhite,
                    ) {
                        if (fromMe && withinRecallWindow && !m.recalled) {
                            DropdownMenuItem(
                                text = { Text("Recall message", color = TextMain) },
                                onClick = { showMenu = false; onRecall() },
                            )
                        }
                        if (!m.recalled) {
                            DropdownMenuItem(
                                text = { Text("Delete for me", color = TextMain) },
                                onClick = { showMenu = false; onDelete() },
                            )
                        }
                    }
                }
                when {
                    m.recalled -> {
                        Text(
                            "Message recalled",
                            color = if (fromMe) Color.White.copy(alpha = 0.75f) else TextMuted,
                            fontSize = 12.sp,
                        )
                    }

                    m.type == com.sundram.weplay.data.MsgType.GIFT -> {
                        val gift = com.sundram.weplay.data.DemoGifts.firstOrNull {
                            it.id == m.giftId
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(gift?.icon ?: R.drawable.ic_gift),
                                contentDescription = null,
                                tint = Color(gift?.tint ?: 0xFF50DCA2),
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(
                                    gift?.name ?: "Gift",
                                    color = if (fromMe) Color.White else TextMain,
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "${m.giftCost} coins",
                                    color = if (fromMe) Color.White.copy(alpha = 0.75f) else TextMuted,
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    }

                    m.type == com.sundram.weplay.data.MsgType.IMAGE && m.mediaPath != null -> {
                        val path = m.mediaPath
                        val bitmap = remember(path) {
                            runCatching { android.graphics.BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(width = 180.dp, height = 120.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            )
                        } else {
                            Text("Photo", color = if (fromMe) Color.White else TextMain, fontSize = 13.sp)
                        }
                    }

                    m.type == com.sundram.weplay.data.MsgType.VOICE && m.mediaPath != null -> {
                        VoiceNoteRow(m = m, fromMe = fromMe)
                    }

                    else -> {
                        Text(
                            m.text,
                            color = if (fromMe) Color.White else TextMain,
                            fontSize = 14.sp,
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        (if (m.type != com.sundram.weplay.data.MsgType.TEXT) "🎤 " else "") +
                            formatMessageTime(m.ts),
                        color = if (fromMe) Color.White.copy(alpha = 0.7f) else Divider,
                        fontSize = 10.sp,
                    )
                    if (fromMe) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(
                                if (m.read) R.drawable.ic_check_all else R.drawable.ic_check,
                            ),
                            contentDescription = if (m.read) "Read" else "Sent",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IconButtonRow(@androidx.annotation.DrawableRes icon: Int, desc: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(BgSoft)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = desc,
            tint = Color.White,
            modifier = Modifier.size(19.dp),
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun GiftPickerSheet(
    balance: Long,
    onDismiss: () -> Unit,
    onSelect: (com.sundram.weplay.data.GiftItem) -> Unit,
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        scrimColor = Color.Black.copy(alpha = 0.6f),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Send a gift", color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_coin),
                        contentDescription = null, tint = Gold, modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("$balance", color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(com.sundram.weplay.data.DemoGifts.size) { idx ->
                    val gift = com.sundram.weplay.data.DemoGifts[idx]
                    val afford = balance >= gift.cost
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(PageWhite, RoundedCornerShape(14.dp))
                            .clickable(enabled = afford) { onSelect(gift) }
                            .padding(12.dp),
                    ) {
                        Icon(
                            painter = painterResource(gift.icon),
                            contentDescription = null,
                            tint = Color(gift.tint),
                            modifier = Modifier.size(34.dp),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            gift.name,
                            color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${gift.cost}c",
                            color = if (afford) Gold else Divider,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Gifts are sent instantly. Coins leave your wallet.", color = TextMuted, fontSize = 11.sp)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun PhotoPickerSheet(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { com.sundram.weplay.data.ProfileStore(context) }
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent(),
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            val path = store.saveChatPhoto(uri)
            if (path != null) onPick(path)
        }
    }
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        scrimColor = Color.Black.copy(alpha = 0.6f),
    ) {
        // just trigger system picker immediately
        LaunchedEffect(Unit) {
            launcher.launch("image/*")
        }
        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("Opening gallery…", color = TextMain)
        }
    }
}

@Composable
private fun VoiceNoteRow(m: ChatMessage, fromMe: Boolean) {
    var playing by remember { mutableStateOf(false) }
    var player by remember { mutableStateOf<android.media.MediaPlayer?>(null) }

    androidx.compose.runtime.DisposableEffect(m.id) {
        onDispose {
            player?.runCatching { stop() }
            player?.release()
            player = null
            playing = false
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(34.dp)
                .background(CyanBrand.copy(alpha = 0.15f), CircleShape)
                .clickable {
                    if (playing) {
                        player?.runCatching { stop() }
                        player?.release()
                        player = null
                        playing = false
                    } else {
                        val path = m.mediaPath ?: return@clickable
                        val next = android.media.MediaPlayer()
                        val started = runCatching {
                            next.setDataSource(path)
                            next.prepare()
                            next.setOnCompletionListener {
                                playing = false
                                next.release()
                                player = null
                            }
                            next.start()
                        }.isSuccess
                        if (started) {
                            player = next
                            playing = true
                        } else {
                            next.release()
                            playing = false
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play),
                contentDescription = if (playing) "Pause" else "Play",
                tint = CyanBrand,
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "${(m.durationMs / 1000).toInt()}s",
            color = if (fromMe) Color.White else TextMain,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun wallBrush(w: Int): Brush = when (w) {
    1 -> Brush.verticalGradient(listOf(WallBlue, PageWhite))
    2 -> Brush.verticalGradient(listOf(WallPurple, PageWhite))
    3 -> Brush.verticalGradient(listOf(WallMint, PageWhite))
    else -> Brush.verticalGradient(listOf(PageWhite, BgSoft))
}
