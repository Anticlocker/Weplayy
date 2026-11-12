package com.sundram.weplay.ui.screens.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sundram.weplay.R
import com.sundram.weplay.data.MessagingStore
import com.sundram.weplay.data.SavedCard
import com.sundram.weplay.data.UserProfile
import com.sundram.weplay.data.WalletStore
import com.sundram.weplay.ui.components.PrimaryButton
import com.sundram.weplay.ui.nav.Routes
import com.sundram.weplay.ui.screens.SubScreen
import com.sundram.weplay.ui.theme.CyanBrand
import com.sundram.weplay.ui.theme.CyanDeep
import com.sundram.weplay.ui.theme.MintGreen
import com.sundram.weplay.ui.theme.PinkGift
import com.sundram.weplay.ui.theme.CardWhite
import com.sundram.weplay.ui.theme.TextMuted
import com.sundram.weplay.ui.theme.WarnOrange
import com.sundram.weplay.ui.theme.Gold
import com.sundram.weplay.ui.theme.TextMain
import com.sundram.weplay.ui.theme.OnBrand

@Composable
fun WalletSettingsScreen(nav: NavHostController, profile: UserProfile) {
    val context = LocalContext.current
    val store = remember { WalletStore(context) }
    val topUps = store.topUps(profile.id)
    val giftCount = giftEntries(context, profile).size
    val cards = store.cards(profile.id)

    SubScreen(nav, "Wallet & Payments", "Coins, cards and transactions") {
        BoxBalance(profile.coins)
        Spacer(Modifier.height(16.dp))

        SettingsGroup {
            SettingsRow(
                icon = R.drawable.ic_plus,
                tint = Gold,
                title = "Top up coins",
                subtitle = "Add coins to your wallet",
                onClick = { nav.navigate(Routes.TOPUP) },
            )
            SettingsRowDivider()
            SettingsRow(
                icon = R.drawable.ic_coin,
                tint = CyanBrand,
                title = "Transaction history",
                subtitle = "Purchases and gift activity",
                value = (topUps.size + giftCount).toString(),
                onClick = { nav.navigate(Routes.WALLET_TRANSACTIONS) },
            )
            SettingsRowDivider()
            SettingsRow(
                icon = R.drawable.ic_bag,
                tint = MintGreen,
                title = "Payment methods",
                subtitle = "Cards saved on this device",
                value = if (cards.isEmpty()) "None" else cards.size.toString(),
                onClick = { nav.navigate(Routes.PAYMENT_METHODS) },
            )
            SettingsRowDivider()
            SettingsRow(
                icon = R.drawable.ic_crown,
                tint = PinkGift,
                title = "Host earnings",
                subtitle = "Withdrawals and payout history",
                value = "Phase 5",
                onClick = { nav.navigate(Routes.HOST_EARNINGS) },
            )
        }

        SettingsGroup("Summary") {
            SettingsRow(
                icon = R.drawable.ic_coin,
                tint = Gold,
                title = "Coins purchased",
                subtitle = "Across ${topUps.size} top-up${if (topUps.size == 1) "" else "s"}",
                value = "%,d".format(topUps.sumOf { it.coins }),
                valueColor = Gold,
                showChevron = false,
            )
            SettingsRowDivider()
            SettingsRow(
                icon = R.drawable.ic_info,
                tint = TextMuted,
                title = "Demo amount spent",
                subtitle = "No real money was charged",
                value = "₹%,d".format(topUps.sumOf { it.price }),
                showChevron = false,
            )
        }

        Text(
            "Payments are simulated in this build. Google Play Billing and real payment gateways " +
                "are not connected.",
            color = TextMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

@Composable
private fun BoxBalance(coins: Long) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(CyanBrand, CyanDeep)),
                RoundedCornerShape(18.dp),
            )
            .padding(18.dp),
    ) {
        Text(
            "COIN BALANCE",
            color = OnBrand.copy(alpha = 0.7f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "%,d".format(coins),
                color = OnBrand,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.ic_coin),
                contentDescription = null,
                tint = OnBrand,
                modifier = Modifier.size(30.dp),
            )
        }
        Text(
            "Local demo balance for rooms, gifts and games",
            color = OnBrand.copy(alpha = 0.7f),
            fontSize = 11.sp,
        )
    }
}

@Composable
fun TransactionsScreen(nav: NavHostController, profile: UserProfile) {
    val context = LocalContext.current
    val store = remember { WalletStore(context) }
    var filter by remember { mutableStateOf("all") }
    val purchases = store.topUps(profile.id).map {
        WalletEntry(
            kind = "purchase",
            title = "Coin top-up",
            detail = "%,d coins added".format(it.coins),
            coins = it.coins,
            price = it.price,
            at = it.at,
        )
    }
    val giftEvents = giftEntries(context, profile)
    val entries = (purchases + giftEvents).sortedByDescending { it.at }
    val filtered = when (filter) {
        "purchases" -> entries.filter { it.kind == "purchase" }
        "gifts" -> entries.filter { it.kind == "gift" }
        else -> entries
    }

    SubScreen(nav, "Transaction history", "Purchases and gift activity") {
        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            SettingsFilterPill("All", filter == "all") { filter = "all" }
            SettingsFilterPill("Purchases", filter == "purchases") { filter = "purchases" }
            SettingsFilterPill("Gifts", filter == "gifts") { filter = "gifts" }
        }
        Spacer(Modifier.height(14.dp))

        if (filtered.isEmpty()) {
            SettingsEmptyState(
                title = "No transactions yet",
                body = "Coin top-ups and gift activity will appear here.",
            )
        } else {
            SettingsGroup {
                filtered.forEachIndexed { index, entry ->
                    TransactionRow(entry)
                    if (index != filtered.lastIndex) SettingsRowDivider()
                }
            }
        }

        Text(
            "Gift activity appears after gifts are sent or received in a room. Top-up purchases " +
                "are recorded by the local demo checkout.",
            color = TextMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

private fun giftEntries(context: Context, profile: UserProfile): List<WalletEntry> {
    val messaging = MessagingStore(context) { profile }
    return messaging.giftLog().map {
        WalletEntry(
            kind = "gift",
            title = "Gift sent",
            detail = "${it.giftName} to ${messaging.userById(it.targetId)?.name ?: it.targetId}",
            coins = -it.coins,
            price = null,
            at = it.at,
        )
    }
}

private data class WalletEntry(
    val kind: String,
    val title: String,
    val detail: String,
    val coins: Long,
    val price: Int?,
    val at: Long,
)

@Composable
private fun TransactionRow(entry: WalletEntry) {
    val tint = if (entry.kind == "gift") PinkGift else Gold
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier
                .size(38.dp)
                .background(tint.copy(alpha = 0.14f), RoundedCornerShape(11.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(
                    if (entry.kind == "gift") R.drawable.ic_gift else R.drawable.ic_coin
                ),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.title, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(entry.detail, color = TextMuted, fontSize = 11.sp)
            Text(formatStamp(entry.at), color = TextMuted, fontSize = 10.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            if (entry.coins != 0L) {
                Text(
                    (if (entry.coins > 0) "+%,d" else "%,d").format(entry.coins),
                    color = if (entry.coins > 0) MintGreen else PinkGift,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (entry.price != null) {
                Text("₹%,d".format(entry.price), color = TextMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun PaymentMethodsScreen(nav: NavHostController, profile: UserProfile) {
    val context = LocalContext.current
    val store = remember { WalletStore(context) }
    var cards by remember { mutableStateOf(store.cards(profile.id)) }
    var showAdd by remember { mutableStateOf(false) }

    SubScreen(nav, "Payment methods", "Cards saved on this device") {
        PrimaryButton("Add payment method") { showAdd = true }
        Spacer(Modifier.height(16.dp))

        if (cards.isEmpty()) {
            SettingsEmptyState(
                title = "No saved cards",
                body = "Add a card to keep a local demo payment method ready for top-ups.",
            )
        } else {
            SettingsGroup("Saved cards") {
                cards.forEachIndexed { index, card ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            Modifier
                                .size(38.dp)
                                .background(CyanBrand.copy(alpha = 0.14f), RoundedCornerShape(11.dp)),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_bag),
                                contentDescription = null,
                                tint = CyanBrand,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${card.brand} •••• ${card.last4}",
                                color = TextMain,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Text("Expires ${card.expiry}", color = TextMuted, fontSize = 11.sp)
                        }
                        TextButton(
                            onClick = {
                                store.removeCard(profile.id, card)
                                cards = store.cards(profile.id)
                                Toast.makeText(context, "Card removed", Toast.LENGTH_SHORT).show()
                            },
                        ) {
                            Text("Remove", color = PinkGift, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (index != cards.lastIndex) SettingsRowDivider()
                }
            }
        }

        Text(
            "Only the card brand, last four digits and expiry are stored. No payment is processed " +
                "and card details are never uploaded.",
            color = TextMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }

    if (showAdd) {
        AddCardDialog(
            onDismiss = { showAdd = false },
            onSave = { card ->
                store.addCard(profile.id, card)
                cards = store.cards(profile.id)
                Toast.makeText(context, "Card saved", Toast.LENGTH_SHORT).show()
            },
        )
    }
}

@Composable
private fun AddCardDialog(
    onDismiss: () -> Unit,
    onSave: (SavedCard) -> Unit,
) {
    var number by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    val digits = number.filter { it.isDigit() }
    val expiryValid = expiry.matches(Regex("(0[1-9]|1[0-2])/\\d{2}"))
    val valid = digits.length in 13..19 && expiryValid && cvv.length in 3..4
    val brand = when {
        digits.startsWith("4") -> "Visa"
        digits.startsWith("5") -> "Mastercard"
        digits.startsWith("3") -> "Amex"
        else -> "Card"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        title = { Text("Add payment method", color = TextMain, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Card number", color = TextMuted, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                SettingsTextField(
                    value = number,
                    onChange = { number = it.filter { char -> char.isDigit() }.take(19) },
                    placeholder = "4242424242424242",
                    keyboardType = KeyboardType.Number,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Expiry", color = TextMuted, fontSize = 11.sp)
                        Spacer(Modifier.height(6.dp))
                        SettingsTextField(
                            value = expiry,
                            onChange = { raw ->
                                val clean = raw.filter { char -> char.isDigit() }.take(4)
                                expiry = if (clean.length >= 3) {
                                    clean.take(2) + "/" + clean.drop(2)
                                } else {
                                    clean
                                }
                            },
                            placeholder = "MM/YY",
                            keyboardType = KeyboardType.Number,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text("CVV", color = TextMuted, fontSize = 11.sp)
                        Spacer(Modifier.height(6.dp))
                        SettingsTextField(
                            value = cvv,
                            onChange = { cvv = it.filter { char -> char.isDigit() }.take(4) },
                            placeholder = "123",
                            password = true,
                            keyboardType = KeyboardType.Number,
                        )
                    }
                }
                if (number.isNotBlank() && !valid) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Enter a valid card number, expiry and CVV.",
                        color = WarnOrange,
                        fontSize = 11.sp,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Demo only. The full number and CVV are discarded; only the brand, last four " +
                        "digits and expiry are saved.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(SavedCard(brand = brand, last4 = digits.takeLast(4), expiry = expiry))
                    onDismiss()
                },
            ) {
                Text(
                    "Save card",
                    color = if (valid) CyanBrand else TextMuted,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) }
        },
    )
}

@Composable
fun HostEarningsScreen(nav: NavHostController) {
    SubScreen(nav, "Host earnings", "Withdrawals and payouts") {
        Column(
            Modifier
                .fillMaxWidth()
                .background(CardWhite, RoundedCornerShape(18.dp))
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Available balance",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                )
                SettingsValueBadge("Phase 5", Gold)
            }
            Spacer(Modifier.height(6.dp))
            Text("₹0.00", color = TextMain, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "Host earnings will appear here once payouts are connected.",
                color = TextMuted,
                fontSize = 11.sp,
            )
        }

        Spacer(Modifier.height(16.dp))
        SettingsGroup("Payout summary") {
            SettingsRow(
                icon = R.drawable.ic_coin,
                tint = Gold,
                title = "Available",
                subtitle = "Ready to withdraw",
                value = "₹0",
                showChevron = false,
            )
            SettingsRowDivider()
            SettingsRow(
                icon = R.drawable.ic_calendar,
                tint = CyanBrand,
                title = "Pending",
                subtitle = "Clearing from recent gifts",
                value = "₹0",
                showChevron = false,
            )
            SettingsRowDivider()
            SettingsRow(
                icon = R.drawable.ic_verified,
                tint = MintGreen,
                title = "Withdrawn",
                subtitle = "Lifetime payouts",
                value = "₹0",
                showChevron = false,
            )
        }

        PrimaryButton("Request payout", enabled = false) {}
        Spacer(Modifier.height(12.dp))
        SettingsEmptyState(
            title = "No payout history",
            body = "Host earnings, withdrawal requests and payout history are planned for Phase 5. " +
                "No host account is connected in this build.",
        )
    }
}
