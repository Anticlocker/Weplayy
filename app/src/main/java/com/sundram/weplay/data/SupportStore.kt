package com.sundram.weplay.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

data class BugReport(
    val id: String,
    val category: String,
    val message: String,
    val imagePath: String?,
    val at: Long,
)

data class FaqItem(
    val question: String,
    val answer: String,
)

data class LegalSection(
    val heading: String,
    val body: String,
)

data class LegalDocument(
    val key: String,
    val title: String,
    val updated: String,
    val sections: List<LegalSection>,
)

val FaqItems = listOf(
    FaqItem(
        "How do I join a voice room?",
        "Open the Voice Room tab, pick a room and tap Join. Rooms, speakers and listeners are " +
            "simulated on this device in the current local build.",
    ),
    FaqItem(
        "How do coins work?",
        "Coins are a local demo balance used for gifts and room features. You can add coins from " +
            "Me > Top up coins. No real payment is processed.",
    ),
    FaqItem(
        "How do I block or report someone?",
        "Go to Settings > Privacy & Safety. Blocked users can be managed there, and every report " +
            "you submit is listed in Report history.",
    ),
    FaqItem(
        "Why do I not receive push notifications?",
        "This build does not connect to a push service. Notification toggles and quiet hours are " +
            "saved locally so the behaviour is ready when delivery is connected.",
    ),
    FaqItem(
        "How do I delete my account?",
        "Open Settings > Account > Delete account. You will be asked to type DELETE. This removes " +
            "your profile, wallet history, settings and local reports from this device.",
    ),
)

val LegalDocuments = listOf(
    LegalDocument(
        key = "terms",
        title = "Terms of Service",
        updated = "Last updated 5 November 2026",
        sections = listOf(
            LegalSection(
                "Local demo",
                "WePlay is a local demo application. Voice rooms, players, games, gifts and payments " +
                    "are simulated on this device. No real-money transaction takes place.",
            ),
            LegalSection(
                "Your account",
                "You are responsible for the profile you create and for keeping access to this " +
                    "device. Do not use the app to impersonate another person or to break the law.",
            ),
            LegalSection(
                "Acceptable use",
                "Harassment, hate speech, threats, doxxing, spam, scams and illegal content are not " +
                    "allowed. Accounts that violate these rules may be removed.",
            ),
            LegalSection(
                "Changes",
                "These terms may change as the app moves from a local demo to a connected service. " +
                    "The latest version will be shown on this screen.",
            ),
        ),
    ),
    LegalDocument(
        key = "privacy",
        title = "Privacy Policy",
        updated = "Last updated 5 November 2026",
        sections = listOf(
            LegalSection(
                "What we store",
                "Your name, username, birthday, bio, gender, profile photo, contact details, wallet " +
                    "history and settings are stored in this app's private storage on your device.",
            ),
            LegalSection(
                "What leaves the device",
                "Nothing. This build has no backend, no analytics and no network calls for account " +
                    "or wallet data.",
            ),
            LegalSection(
                "Permissions",
                "Microphone access is used only by the microphone test. Photo access is used only " +
                    "when you choose an avatar or attach a screenshot to a bug report.",
            ),
            LegalSection(
                "Deleting your data",
                "Logging out removes the active profile from this device. Deleting your account " +
                    "removes the profile, wallet history, settings, blocked users and reports.",
            ),
        ),
    ),
    LegalDocument(
        key = "guidelines",
        title = "Community Guidelines",
        updated = "Last updated 5 November 2026",
        sections = listOf(
            LegalSection(
                "Respect people",
                "Do not harass, threaten, bully or target other people. Disagreements are fine; " +
                    "abuse is not.",
            ),
            LegalSection(
                "No hate or exploitation",
                "Hate speech, sexual content involving minors, doxxing and content that encourages " +
                    "self-harm are not allowed.",
            ),
            LegalSection(
                "Keep rooms usable",
                "Do not spam, scream into the microphone, play loud unrelated audio or share scam " +
                    "links in rooms and messages.",
            ),
            LegalSection(
                "Report problems",
                "Use Settings > Privacy & Safety to block a user or file a report. Reports are kept " +
                    "in Report history on this device.",
            ),
        ),
    ),
    LegalDocument(
        key = "licenses",
        title = "Open Source Licenses",
        updated = "Last updated 5 November 2026",
        sections = listOf(
            LegalSection(
                "AndroidX and Jetpack Compose",
                "Copyright The Android Open Source Project. Licensed under the Apache License 2.0.",
            ),
            LegalSection(
                "Kotlin standard library",
                "Copyright JetBrains s.r.o. Licensed under the Apache License 2.0.",
            ),
            LegalSection(
                "Material 3",
                "Copyright The Android Open Source Project. Licensed under the Apache License 2.0.",
            ),
            LegalSection(
                "Full license text",
                "The Apache License 2.0 is available at apache.org/licenses/LICENSE-2.0. This app " +
                    "does not include code or assets copied from the WePlay APK.",
            ),
        ),
    ),
)

fun legalDocument(key: String): LegalDocument? =
    LegalDocuments.firstOrNull { it.key == key } ?: LegalDocuments.firstOrNull()

class SupportStore(context: Context) {

    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("weplay_support", Context.MODE_PRIVATE)

    fun reports(ownerId: String): List<BugReport> {
        val raw = prefs.getString("reports_$ownerId", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                BugReport(
                    id = item.optString("id"),
                    category = item.optString("category"),
                    message = item.optString("message"),
                    imagePath = if (item.isNull("image")) null else item.optString("image"),
                    at = item.optLong("at"),
                )
            }
        }.getOrDefault(emptyList())
    }

    fun add(ownerId: String, report: BugReport) {
        val next = (listOf(report) + reports(ownerId)).take(20)
        val array = JSONArray()
        next.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("category", item.category)
                    .put("message", item.message)
                    .put("image", item.imagePath)
                    .put("at", item.at)
            )
        }
        prefs.edit().putString("reports_$ownerId", array.toString()).apply()
    }

    fun saveAttachment(uri: Uri): String? {
        val dir = File(app.filesDir, "support").apply { mkdirs() }
        val out = File(dir, "bug_${System.currentTimeMillis()}.jpg")
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            app.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            val longest = maxOf(bounds.outWidth, bounds.outHeight)
            var sample = 1
            while (longest > 0 && longest / sample > 1280) sample *= 2

            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = app.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null

            FileOutputStream(out).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            }
            bitmap.recycle()
            out.absolutePath
        }.getOrNull()
    }

    fun clear() {
        prefs.edit().clear().apply()
        File(app.filesDir, "support").deleteRecursively()
    }
}
