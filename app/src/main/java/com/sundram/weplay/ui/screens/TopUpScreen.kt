package com.sundram.weplay.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sundram.weplay.R
import com.sundram.weplay.data.CoinPack
import com.sundram.weplay.data.DemoCoinPacks
import com.sundram.weplay.data.TopUpRecord
import com.sundram.weplay.data.TopUpStore
import com.sundram.weplay.data.UserProfile
import com.sundram.weplay.ui.components.NoteBox
import com.sundram.weplay.ui.components.TagPill
import com.sundram.weplay.ui.theme.CyanBrand
import com.sundram.weplay.ui.theme.CyanDeep
import com.sundram.weplay.ui.theme.MintGreen
import com.sundram.weplay.ui.theme.CardWhite
import com.sundram.weplay.ui.theme.Stroke
import com.sundram.weplay.ui.theme.TextMuted
import com.sundram.weplay.ui.theme.WarnOrange
import com.sundram.weplay.ui.theme.Gold
import com.sundram.weplay.ui.theme.TextMain
import com.sundram.weplay.ui.theme.OnBrand
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val stampFormat = SimpleDateFormat("d MMM · h:mm a", Locale.US)

@Composable
fun TopUpScreen(nav: NavHostController, profile: UserProfile, onSave: (UserProfile) -> Unit) {
    val context = LocalContext.current
    val store = remember { TopUpStore(context) }
    var history by remember { mutableStateOf(store.history(profile.id)) }
    var selected by remember { mutableStateOf<CoinPack?>(null) }
    var purchased by remember { mutableStateOf<TopUpRecord?>(null) }
    var balanceAfter by remember { mutableStateOf(profile.coins) }

    SubScreen(nav, "Top up", "Coins for rooms, gifts and games") {
        BalanceCard(profile.coins)

        Spacer(Modifier.height(20.dp))
        Text("Choose a pack", color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))

        DemoCoinPacks.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { pack ->
                    PackTile(
                        pack = pack,
                        modifier = Modifier.weight(1f),
                    ) { selected = pack }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(8.dp))
        Text("Recent top-ups", color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        if (history.isEmpty()) {
            Text(
                "No top-ups yet. Your purchases will show up here.",
                color = TextMuted,
                fontSize = 12.sp,
            )
        } else {
            history.take(5).forEach { record ->
                HistoryRow(record)
            }
        }

        Spacer(Modifier.height(18.dp))
        NoteBox(
            "Payments are not connected yet. Packs are credited locally for testing, " +
                "so no real money is charged."
        )
    }

    val pack = selected
    if (pack != null) {
        AlertDialog(
            onDismissRequest = { selected = null },
            containerColor = CardWhite,
            title = { Text("Confirm top up", color = TextMain, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "%,d coins".format(pack.coins),
                        color = TextMain,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (pack.bonus > 0) {
                        Text(
                            "+ %,d bonus coins".format(pack.bonus),
                            color = MintGreen,
                            fontSize = 13.sp,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Total %,d coins".format(pack.total),
                        color = TextMain,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "₹%,d".format(pack.price),
                        color = CyanBrand,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Demo purchase. No real payment is made and no gateway is connected.",
                        color = WarnOrange,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val record = TopUpRecord(
                            packId = pack.id,
                            coins = pack.total,
                            price = pack.price,
                            at = System.currentTimeMillis(),
                        )
                        store.add(profile.id, record)
                        history = store.history(profile.id)
                        balanceAfter = profile.coins + pack.total
                        onSave(profile.copy(coins = balanceAfter))
                        selected = null
                        purchased = record
                    }
                ) {
                    Text(
                        "Pay ₹%,d".format(pack.price),
                        color = CyanBrand,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { selected = null }) { Text("Cancel", color = TextMuted) }
            },
        )
    }

    val record = purchased
    if (record != null) {
        AlertDialog(
            onDismissRequest = { purchased = null },
            containerColor = CardWhite,
            title = { Text("Coins added", color = TextMain, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "+%,d coins".format(record.coins),
                        color = MintGreen,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "New balance: %,d coins".format(balanceAfter),
                        color = TextMain,
                        fontSize = 13.sp,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { purchased = null }) {
                    Text("Done", color = CyanBrand, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

@Composable
private fun BalanceCard(coins: Long) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(CyanBrand, CyanDeep)),
                RoundedCornerShape(18.dp),
            )
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "COIN BALANCE",
                    color = OnBrand.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "%,d".format(coins),
                    color = OnBrand,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    "Ready for rooms, gifts and games",
                    color = OnBrand.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                )
            }
            Box(
                Modifier
                    .size(54.dp)
                    .background(OnBrand.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_coin),
                    contentDescription = null,
                    tint = OnBrand,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
    }
}

@Composable
private fun PackTile(pack: CoinPack, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .background(CardWhite, RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (pack.tag != null) CyanBrand.copy(alpha = 0.55f) else Stroke,
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_coin),
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(21.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "%,d".format(pack.coins),
                color = TextMain,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            pack.tag?.let { TagPill(it, OnBrand, Gold) }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            if (pack.bonus > 0) "+%,d bonus".format(pack.bonus) else "No bonus",
            color = if (pack.bonus > 0) MintGreen else TextMuted,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "₹%,d".format(pack.price),
                color = CyanBrand,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Text("Tap to buy", color = TextMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun HistoryRow(record: TopUpRecord) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .background(Gold.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_coin),
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "+%,d coins".format(record.coins),
                color = TextMain,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stampFormat.format(Date(record.at)),
                color = TextMuted,
                fontSize = 10.sp,
            )
        }
        Text("₹%,d".format(record.price), color = TextMuted, fontSize = 11.sp)
    }
}
