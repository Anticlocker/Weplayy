package com.sundram.weplay.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sundram.weplay.R
import com.sundram.weplay.data.BugReport
import com.sundram.weplay.data.FaqItems
import com.sundram.weplay.data.LegalDocuments
import com.sundram.weplay.data.SupportStore
import com.sundram.weplay.data.UserProfile
import com.sundram.weplay.ui.screens.SubScreen
import com.sundram.weplay.ui.theme.CyanBrand
import com.sundram.weplay.ui.theme.MintGreen
import com.sundram.weplay.ui.theme.CardWhite
import com.sundram.weplay.ui.theme.TextMuted
import com.sundram.weplay.ui.theme.Gold
import com.sundram.weplay.ui.theme.TextMain

private val BugReportCategories = listOf(
    "App crash",
    "Wrong information",
    "Payment or coins",
    "Voice room issue",
    "Something else",
)

@Composable
fun SupportSettingsScreen(nav: NavHostController, profile: UserProfile) {
    val context = LocalContext.current
    val store = remember { SupportStore(context) }
    var reports by remember { mutableStateOf(store.reports(profile.id)) }
    var showNew by remember { mutableStateOf(false) }
    var showCategory by remember { mutableStateOf(false) }
    var faq by remember { mutableStateOf<Pair<String, String>?>(null) }
    var legal by remember { mutableStateOf<Int?>(null) }
    var category by remember { mutableStateOf(BugReportCategories.first()) }
    var message by remember { mutableStateOf("") }

    SubScreen(nav, "Support & Legal", "Help center, policies and bug reports") {
        SettingsGroup("Help center") {
            FaqItems.forEachIndexed { index, item ->
                SettingsRow(
                    icon = R.drawable.ic_comment,
                    tint = if (index % 2 == 0) CyanBrand else MintGreen,
                    title = item.question,
                    onClick = { faq = item.question to item.answer },
                    showChevron = false,
                )
                if (index != FaqItems.lastIndex) SettingsRowDivider()
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingsGroup("Policies") {
            LegalDocuments.forEachIndexed { index, doc ->
                SettingsRow(
                    icon = R.drawable.ic_info,
                    tint = CyanBrand,
                    title = doc.title,
                    value = "View",
                    onClick = { legal = index },
                )
                if (index != LegalDocuments.lastIndex) SettingsRowDivider()
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingsGroup("Bug reports") {
            SettingsRow(
                icon = R.drawable.ic_settings,
                tint = Gold,
                title = "Report a problem",
                subtitle = "Tell us what went wrong and where",
                onClick = { showNew = true },
            )
        }

        if (reports.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            SettingsGroup("Your bug reports") {
                reports.forEachIndexed { index, report ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Row {
                            Text(
                                report.category,
                                color = TextMain,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            SettingsValueBadge("Submitted")
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            report.message,
                            color = TextMuted,
                            fontSize = 12.sp,
                            maxLines = 2,
                        )
                        Text(
                            "${report.id} · ${formatStamp(report.at)}",
                            color = TextMuted,
                            fontSize = 10.sp,
                        )
                    }
                    if (index != reports.lastIndex) SettingsRowDivider()
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            "Support data stays on this device. No moderation service is connected in this build.",
            color = TextMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
        )
    }

    faq?.let { (question, answer) ->
        SettingsInfoDialog(
            title = question,
            body = answer,
            onDismiss = { faq = null },
        )
    }

    legal?.let { index ->
        val doc = LegalDocuments[index]
        AlertDialog(
            onDismissRequest = { legal = null },
            containerColor = CardWhite,
            title = { Text(doc.title, color = TextMain, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(doc.updated, color = TextMuted, fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    doc.sections.forEach { section ->
                        Text(section.heading, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(3.dp))
                        Text(section.body, color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp)
                        Spacer(Modifier.height(10.dp))
                    }
                    Text("Full documents ship with the signed build.", color = TextMuted, fontSize = 10.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { legal = null }) { Text("Close", color = CyanBrand) }
            },
        )
    }

    if (showCategory) {
        SettingsChoiceDialog(
            title = "Category",
            options = BugReportCategories.map { SettingsChoice(it, it) },
            selected = category,
            onSelect = { category = it },
            onDismiss = { showCategory = false },
        )
    }

    if (showNew) {
        AlertDialog(
            onDismissRequest = { showNew = false },
            containerColor = CardWhite,
            title = { Text("Report a problem", color = TextMain, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Category", color = TextMuted, fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(CardWhite, RoundedCornerShape(12.dp))
                            .clickable { showCategory = true }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                    ) {
                        Text(
                            category,
                            color = TextMain,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Text("Change", color = CyanBrand, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("What happened?", color = TextMuted, fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                    SettingsTextField(
                        value = message,
                        onChange = { message = it.take(400) },
                        placeholder = "Describe the problem",
                        singleLine = false,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Reports are saved locally and are not uploaded.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = message.isNotBlank(),
                    onClick = {
                        val report = BugReport(
                            id = "BG-" + (100000..999999).random(),
                            category = category,
                            message = message.trim(),
                            imagePath = null,
                            at = System.currentTimeMillis(),
                        )
                        store.add(profile.id, report)
                        reports = store.reports(profile.id)
                        message = ""
                        category = BugReportCategories.first()
                        showNew = false
                        Toast.makeText(context, "Bug report submitted", Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Text("Submit", color = CyanBrand, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNew = false }) { Text("Cancel", color = TextMuted) }
            },
        )
    }
}
