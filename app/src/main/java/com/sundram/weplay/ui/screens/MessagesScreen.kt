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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sundram.weplay.R
import com.sundram.weplay.data.Chat
import com.sundram.weplay.data.DirectoryUser
import com.sundram.weplay.data.MessagingStore
import com.sundram.weplay.data.NotificationKind
import com.sundram.weplay.ui.components.InitialsAvatar
import com.sundram.weplay.ui.components.RoundIconButton
import com.sundram.weplay.ui.nav.Routes
import com.sundram.weplay.ui.components.SectionHeader
import com.sundram.weplay.ui.theme.CyanBrand
import com.sundram.weplay.ui.theme.KtvRed
import com.sundram.weplay.ui.theme.LiveRed
import com.sundram.weplay.ui.theme.PageWhite
import com.sundram.weplay.ui.theme.TextBody
import com.sundram.weplay.ui.theme.TextMain
import com.sundram.weplay.ui.theme.BgSoft
import com.sundram.weplay.ui.theme.CardWhite
import com.sundram.weplay.ui.theme.Stroke
import com.sundram.weplay.ui.theme.MintGreen
import com.sundram.weplay.ui.theme.TextMuted
import com.sundram.weplay.ui.theme.Divider

private enum class MsgTab { CHATS, PEOPLE, ALERTS }

@Composable
fun MessagesScreen(
    nav: NavController,
    pad: PaddingValues,
    store: MessagingStore,
    onUnreadChange: (Int) -> Unit = {},
) {
    var tab by remember { mutableStateOf(MsgTab.CHATS) }
    var version by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var confirmDecline by remember { mutableStateOf<DirectoryUser?>(null) }
    var confirmUnblock by remember { mutableStateOf<DirectoryUser?>(null) }

    fun bump() { version++ }

    // Seed demo data once per screen entry
    LaunchedEffect(Unit) {
        store.seedIfEmpty()
        bump()
    }

    val snapshot = remember(version) {
        object {
            val chats = store.allChats()
            val friends = store.friends()
            val incoming = store.incomingRequests()
            val outgoing = store.outgoingRequests()
            val blockedList = store.blockedUsers()
            val notifs = store.notifications()
            val unreadNotifs = store.unreadNotifications()
            val unreadTotal = store.unreadTotal()
        }
    }
    val chats = snapshot.chats
    val friends = snapshot.friends
    val incoming = snapshot.incoming
    val outgoing = snapshot.outgoing
    val blockedList = snapshot.blockedList
    val notifs = snapshot.notifs
    val unreadNotifs = snapshot.unreadNotifs
    val onlineFriends = remember(snapshot) { friends.filter { it.online } }

    LaunchedEffect(snapshot.unreadTotal) {
        onUnreadChange(snapshot.unreadTotal)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageWhite)
            .padding(pad),
    ) {
        // ---------- header ----------
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text("Message", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextMain)
                Text(
                    when (tab) {
                        MsgTab.CHATS -> "${chats.size} chats"
                        MsgTab.PEOPLE -> "${friends.size} friends"
                        MsgTab.ALERTS -> "$unreadNotifs new alerts"
                    },
                    fontSize = 12.sp, color = TextMuted,
                )
            }
            Spacer(Modifier.width(8.dp))
            Box {
                RoundIconButton(
                    painter = painterResource(R.drawable.ic_bell),
                    onClick = { tab = MsgTab.ALERTS },
                    contentDescription = "Notifications",
                )
                if (unreadNotifs > 0) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(16.dp)
                            .background(KtvRed, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (unreadNotifs > 9) "9+" else "$unreadNotifs",
                            color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        // ---------- search bar ----------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .background(CardWhite, RoundedCornerShape(14.dp))
                .border(1.dp, Stroke, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = TextMain, fontSize = 15.sp),
                cursorBrush = SolidColor(CyanBrand),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { field ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                "Search people by name or username",
                                color = Divider, fontSize = 14.sp,
                            )
                        }
                        field()
                    }
                },
            )
        }

        // ---------- tab chips ----------
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MsgTab.entries.forEach { t ->
                val selected = tab == t
                Box(
                    Modifier
                        .background(
                            if (selected) CyanBrand else CardWhite,
                            RoundedCornerShape(20.dp),
                        )
                        .clickable { tab = t }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val label = when (t) {
                        MsgTab.CHATS -> "Chats"
                        MsgTab.PEOPLE -> "People"
                        MsgTab.ALERTS -> "Alerts" + if (unreadNotifs > 0) " ($unreadNotifs)" else ""
                    }
                    Text(
                        label,
                        color = if (selected) Color.White else TextMain,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // ---------- body ----------
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            when (tab) {
                MsgTab.CHATS -> {
                    if (query.isNotBlank()) {
                        // search mode in chats tab → people results
                        val results = store.search(query)
                        item { SectionHeader("Search results", "${results.size}") }
                        items(results, key = { "s" + it.id }) { u ->
                            UserRow(u = u, rel = store.relationship(u.id)) {
                                nav.navigate("user/${u.id}")
                            }
                        }
                        if (results.isEmpty()) {
                            item { EmptyNote("No one found for \"$query\"") }
                        }
                    } else {
                        if (onlineFriends.isNotEmpty()) {
                            item {
                                SectionHeader("Online now", "${onlineFriends.size}")
                                Spacer(Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    items(onlineFriends, key = { "on" + it.id }) { u ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.clickable {
                                                nav.navigate("user/${u.id}")
                                            },
                                        ) {
                                            Box {
                                                InitialsAvatar(u.name, size = 52.dp)
                                                Box(
                                                    Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .size(12.dp)
                                                        .background(MintGreen, CircleShape),
                                                )
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(u.name, color = TextMain, fontSize = 10.sp, maxLines = 1)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                        if (chats.isEmpty()) {
                            item { EmptyNote("No chats yet. Search people below, add a friend and say hello.") }
                        } else {
                            item { SectionHeader("Recent chats", "${chats.size}") }
                            items(chats, key = { it.userId }) { chat ->
                                ChatRow(
                                    chat = chat,
                                    user = store.userById(chat.userId),
                                    onClick = { nav.navigate("chat/${chat.userId}"); bump() },
                                    onMute = {
                                        store.setMuted(chat.userId, !chat.muted); bump()
                                    },
                                    onDelete = { store.deleteChat(chat.userId); bump() },
                                )
                            }
                        }
                        // quick people strip so new users can be found from Chats tab too
                        val quick = store.allUsers().filter { !store.isMe(it.id) && !store.isBlocked(it.id) }.take(6)
                        item {
                            Spacer(Modifier.height(10.dp))
                            SectionHeader("People", "${quick.size}")
                            Spacer(Modifier.height(6.dp))
                        }
                        items(quick, key = { "q" + it.id }) { u ->
                            UserRow(u = u, rel = store.relationship(u.id)) {
                                nav.navigate("user/${u.id}")
                            }
                        }
                    }
                }

                MsgTab.PEOPLE -> {
                    if (query.isNotBlank()) {
                        val results = store.search(query)
                        item { SectionHeader("Search results", "${results.size}") }
                        items(results, key = { "ps" + it.id }) { u ->
                            UserRow(u = u, rel = store.relationship(u.id)) {
                                nav.navigate("user/${u.id}")
                            }
                        }
                        if (results.isEmpty()) {
                            item { EmptyNote("No one found for \"$query\"") }
                        }
                    } else {
                        if (incoming.isNotEmpty()) {
                            item { SectionHeader("Friend requests", "${incoming.size}") }
                            items(incoming, key = { "in" + it.id }) { u ->
                                RequestRow(
                                    u = u,
                                    onAccept = { store.acceptRequest(u.id); bump() },
                                    onDecline = { confirmDecline = u },
                                    onClick = { nav.navigate("user/${u.id}") },
                                )
                            }
                        }
                        if (outgoing.isNotEmpty()) {
                            item { SectionHeader("Pending", "${outgoing.size}") }
                            items(outgoing, key = { "out" + it.id }) { u ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CardWhite, RoundedCornerShape(14.dp))
                                        .padding(10.dp),
                                ) {
                                    InitialsAvatar(u.name, size = 40.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(u.name, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                        Text("Request sent", color = TextMuted, fontSize = 11.sp)
                                    }
                                    TextButton(onClick = { store.cancelRequest(u.id); bump() }) {
                                        Text("Cancel", color = KtvRed, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        item { SectionHeader("Friends", "${friends.size}") }
                        if (friends.isEmpty()) {
                            item { EmptyNote("No friends yet. Accept a request or search people above.") }
                        } else {
                            items(friends, key = { "f" + it.id }) { u ->
                                UserRow(u = u, rel = store.relationship(u.id)) {
                                    nav.navigate("user/${u.id}")
                                }
                            }
                        }
                        if (blockedList.isNotEmpty()) {
                            item { SectionHeader("Blocked", "${blockedList.size}") }
                            items(blockedList, key = { "b" + it.id }) { u ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CardWhite, RoundedCornerShape(14.dp))
                                        .padding(10.dp),
                                ) {
                                    InitialsAvatar(u.name, size = 40.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(u.name, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                        Text("Blocked", color = TextMuted, fontSize = 11.sp)
                                    }
                                    TextButton(onClick = { confirmUnblock = u }) {
                                        Text("Unblock", color = CyanBrand, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                MsgTab.ALERTS -> {
                    if (notifs.isEmpty()) {
                        item { EmptyNote("No alerts yet. Friend requests and new messages will appear here.") }
                    } else {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Spacer(Modifier.weight(1f))
                                TextButton(
                                    onClick = { store.clearNotifications(); bump() },
                                ) { Text("Clear all", color = TextMuted, fontSize = 12.sp) }
                            }
                        }
                        items(notifs, key = { it.id }) { n ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CardWhite, RoundedCornerShape(14.dp))
                                    .clickable(
                                        enabled = n.userId != null,
                                    ) { n.userId?.let { nav.navigate("user/$it") } }
                                    .padding(12.dp),
                            ) {
                                InitialsAvatar(
                                    store.userById(n.userId ?: "")?.name ?: "?",
                                    size = 36.dp,
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        n.text,
                                        color = if (n.read) TextMuted else TextMain,
                                        fontSize = 13.sp,
                                        maxLines = 2,
                                    )
                                    if (n.kind == NotificationKind.ROOM_INVITE) {
                                        Spacer(Modifier.height(6.dp))
                                        Box(
                                            Modifier
                                                .background(CyanBrand, RoundedCornerShape(10.dp))
                                                .clickable {
                                                    nav.navigate(Routes.roomRoute(n.roomId ?: "r1"))
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                        ) {
                                            Text("Join", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                if (!n.read) {
                                    Box(
                                        Modifier.size(8.dp).background(CyanBrand, CircleShape),
                                    )
                                }
                            }
                        }
                        // open alerts tab reading animation: mark read after short delay
                        item {
                            LaunchedEffect(notifs.size) {
                                if (unreadNotifs > 0) {
                                    kotlinx.coroutines.delay(600)
                                    store.markNotificationsRead()
                                    bump()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ---------- dialogs ----------
    confirmDecline?.let { u ->
        AlertDialog(
            onDismissRequest = { confirmDecline = null },
            title = { Text("Decline request?", color = TextMain) },
            text = { Text("This will decline ${u.name}'s friend request.", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = { store.declineRequest(u.id); confirmDecline = null; bump() }) {
                    Text("Decline", color = KtvRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDecline = null }) { Text("Cancel", color = TextMuted) }
            },
            containerColor = CardWhite,
        )
    }
    confirmUnblock?.let { u ->
        AlertDialog(
            onDismissRequest = { confirmUnblock = null },
            title = { Text("Unblock ${u.name}?", color = TextMain) },
            text = { Text("You will be able to receive requests from them again.", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = { store.unblock(u.id); confirmUnblock = null; bump() }) {
                    Text("Unblock", color = CyanBrand)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmUnblock = null }) { Text("Cancel", color = TextMuted) }
            },
            containerColor = CardWhite,
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ChatRow(
    chat: Chat,
    user: DirectoryUser?,
    onClick: () -> Unit,
    onMute: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { true },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(LiveRed, RoundedCornerShape(14.dp))
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Text("Delete", color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                SwipeToDismissBoxValue.StartToEnd -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(CyanBrand, RoundedCornerShape(14.dp))
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            if (chat.muted) "Unmute" else "Mute",
                            color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                }

                else -> {
                    Box(Modifier.fillMaxSize().background(CardWhite, RoundedCornerShape(14.dp)))
                }
            }
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardWhite, RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                InitialsAvatar(chat.userId, size = 48.dp)
                if (user?.online == true) {
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .size(13.dp)
                            .background(MintGreen, CircleShape)
                            .border(2.dp, CardWhite, CircleShape),
                    )
                }
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (chat.pinned) {
                        Icon(
                            painter = painterResource(R.drawable.ic_pin),
                            contentDescription = "Pinned",
                            tint = TextMuted,
                            modifier = Modifier.size(11.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        user?.name ?: chat.userId,
                        color = TextMain,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (chat.muted) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            painter = painterResource(R.drawable.ic_bell_off),
                            contentDescription = "Muted",
                            tint = TextMuted,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    chat.displayText,
                    color = if (chat.unread > 0) TextMain else TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    relativeTime(chat.lastTs),
                    color = TextMuted, fontSize = 10.sp,
                )
                if (chat.unread > 0) {
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .background(if (chat.muted) TextMuted else KtvRed, RoundedCornerShape(10.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    ) {
                        Text(
                            "${chat.unread}",
                            color = TextMain,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        // swipe trigger — when swipe passes threshold, run action
        LaunchedEffect(dismissState.currentValue) {
            when (dismissState.currentValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    dismissState.reset()
                }

                SwipeToDismissBoxValue.StartToEnd -> {
                    onMute()
                    dismissState.reset()
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun UserRow(u: DirectoryUser, rel: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardWhite, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InitialsAvatar(u.name, size = 44.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                u.name,
                color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                "@${u.id} · Lv ${u.level}" + if (u.online) " · Online" else "",
                color = TextMuted, fontSize = 11.sp, maxLines = 1,
            )
        }
        RelationshipPill(rel)
    }
}

@Composable
private fun RelationshipPill(rel: String) {
    val (label, bg) = when (rel) {
        "friend" -> "Friend" to MintGreen
        "incoming" -> "Respond" to KtvRed
        "outgoing" -> "Sent" to TextMuted
        "blocked" -> "Blocked" to TextMuted
        else -> "Add" to CyanBrand
    }
    Box(
        Modifier
            .background(bg, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RequestRow(
    u: DirectoryUser,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardWhite, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InitialsAvatar(u.name, size = 44.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(u.name, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("Wants to be your friend", color = TextMuted, fontSize = 11.sp)
        }
        Box(
            Modifier
                .background(CyanBrand, RoundedCornerShape(10.dp))
                .clickable(onClick = onAccept)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text("Accept", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier
                .background(PageWhite, RoundedCornerShape(10.dp))
                .clickable(onClick = onDecline)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text("Decline", color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyNote(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(CardWhite, RoundedCornerShape(14.dp))
            .padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = TextMuted, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

fun relativeTime(ts: Long): String {
    if (ts == 0L) return ""
    val diff = System.currentTimeMillis() - ts
    val min = diff / 60_000
    if (min < 1) return "now"
    if (min < 60) return "${min}m"
    val hr = min / 60
    if (hr < 24) return "${hr}h"
    val day = hr / 24
    if (day < 7) return "${day}d"
    return "${day / 7}w"
}
