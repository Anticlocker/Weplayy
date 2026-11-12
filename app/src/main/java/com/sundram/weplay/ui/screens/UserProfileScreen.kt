package com.sundram.weplay.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.navigation.NavController
import com.sundram.weplay.R
import com.sundram.weplay.data.MessagingStore
import com.sundram.weplay.ui.components.InitialsAvatar
import com.sundram.weplay.ui.components.RoundIconButton
import com.sundram.weplay.ui.components.SectionHeader
import com.sundram.weplay.ui.theme.CardWhite
import com.sundram.weplay.ui.theme.CyanBrand
import com.sundram.weplay.ui.theme.MintGreen
import com.sundram.weplay.ui.theme.KtvRed
import com.sundram.weplay.ui.theme.TextMain
import com.sundram.weplay.ui.theme.BgSoft
import com.sundram.weplay.ui.theme.PageWhite
import com.sundram.weplay.ui.theme.TextMuted

@Composable
fun UserProfileScreen(nav: NavController, userId: String, store: MessagingStore) {
    var version by remember { mutableStateOf(0) }
    var confirmBlock by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    var confirmUnblock by remember { mutableStateOf(false) }

    fun bump() { version++ }

    val user = run { version; store.userById(userId) }
    val rel = run { version; store.relationship(userId) }
    val isMe = userId == store.meId()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageWhite)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(
                painter = painterResource(R.drawable.ic_back),
                onClick = { nav.popBackStack() },
                contentDescription = "Back",
                bg = Color.Transparent,
            )
            Spacer(Modifier.width(8.dp))
            Text("Profile", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextMain)
        }

        if (user == null) {
            Spacer(Modifier.height(30.dp))
            Text("User not found.", color = TextMuted, fontSize = 14.sp)
            return@Column
        }

        Spacer(Modifier.height(20.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            InitialsAvatar(user.name, size = 110.dp)
            Spacer(Modifier.height(12.dp))
            Text(user.name, color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("@${user.id}", color = CyanBrand, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .background(BgSoft, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text("Lv ${user.level}", color = TextMain, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    if (user.online) "● Online" else "○ Offline",
                    color = if (user.online) MintGreen else TextMuted, fontSize = 11.sp,
                )
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .background(BgSoft, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Column {
                    SectionHeader("Bio")
                    Spacer(Modifier.height(6.dp))
                    Text(user.bio, color = TextMain, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        if (!isMe) {
            // primary CTA row
            when (rel) {
                "blocked" -> {
                    ActionButton("Unblock", CyanBrand) { confirmUnblock = true }
                }
                "friend" -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f)) {
                            ActionButton("Message", CyanBrand) { nav.navigate("chat/${user.id}") }
                        }
                        Box(Modifier.weight(1f)) {
                            ActionButton("Remove friend", BgSoft) { confirmRemove = true }
                        }
                    }
                }
                "incoming" -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f)) {
                            ActionButton("Accept", CyanBrand) {
                                store.acceptRequest(user.id); bump()
                            }
                        }
                        Box(Modifier.weight(1f)) {
                            ActionButton("Decline", BgSoft) {
                                store.declineRequest(user.id); bump()
                            }
                        }
                    }
                }
                "outgoing" -> {
                    ActionButton("Request sent — tap to cancel", BgSoft) {
                        store.cancelRequest(user.id); bump()
                    }
                }
                else -> {
                    ActionButton("Add friend", CyanBrand) {
                        store.sendRequest(user.id); bump()
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) {
                    ActionButton("Message", BgSoft) {
                        // messaging a non-friend requires a request first
                        if (rel != "friend") {
                            store.sendRequest(user.id)
                        }
                        nav.navigate("chat/${user.id}")
                    }
                }
                Box(Modifier.weight(1f)) {
                    ActionButton("Block", BgSoft) { confirmBlock = true }
                }
            }
        } else {
            Spacer(Modifier.height(4.dp))
            Text("This is you.", color = TextMuted, fontSize = 12.sp)
        }
    }

    // dialogs ------------------------------------------------------------
    if (user != null && confirmBlock) {
        AlertDialog(
            onDismissRequest = { confirmBlock = false },
            title = { Text("Block ${user.name}?", color = TextMain) },
            text = { Text("You will no longer receive messages or requests from them.", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = { store.block(user.id); confirmBlock = false; nav.popBackStack() }) {
                    Text("Block", color = KtvRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmBlock = false }) { Text("Cancel", color = TextMuted) }
            },
            containerColor = CardWhite,
        )
    }
    if (user != null && confirmUnblock) {
        AlertDialog(
            onDismissRequest = { confirmUnblock = false },
            title = { Text("Unblock ${user.name}?", color = TextMain) },
            confirmButton = {
                TextButton(onClick = { store.unblock(user.id); confirmUnblock = false; bump() }) {
                    Text("Unblock", color = CyanBrand)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmUnblock = false }) { Text("Cancel", color = TextMuted) }
            },
            containerColor = CardWhite,
        )
    }
    if (user != null && confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Remove ${user.name}?", color = TextMain) },
            text = { Text("They will be removed from your friends list.", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = { store.removeFriend(user.id); confirmRemove = false; bump() }) {
                    Text("Remove", color = KtvRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) { Text("Cancel", color = TextMuted) }
            },
            containerColor = CardWhite,
        )
    }
}

@Composable
private fun ActionButton(label: String, bg: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(bg, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (bg == CyanBrand) Color.White else TextMain,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
