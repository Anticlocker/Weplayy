package com.sundram.weplay.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

fun defaultUsername(id: String): String =
    "wp_" + id.filter { it.isLetterOrDigit() }.takeLast(6).lowercase()

data class UserProfile(
    val name: String,
    val dobMillis: Long,
    val id: String,
    val photoPath: String? = null,
    val provider: String = "google",
    val phone: String? = null,
    val email: String? = null,
    val level: Int = 1,
    val coins: Long = 500,
    val family: String? = null,
    val familyCode: String? = null,
    val frame: String? = null,
    val username: String = defaultUsername(id),
    val bio: String = "",
    val gender: String = "Not specified",
    val usernameChangedAt: Long = 0L,
    val hasPassword: Boolean = false,
    val passwordHash: String? = null,
    val phoneVerified: Boolean = phone != null,
    val emailVerified: Boolean = email != null,
    val linkedGoogle: Boolean = provider == "google",
    val linkedApple: Boolean = false,
    val linkedFacebook: Boolean = false,
    val nickname: String = name,
    val region: String = "",
    val signature: String = "",
    val priceTag: String = "",
    val title: String = "",
    val badge: String = "",
    val qrContactCard: String = "",
)

private const val PASSWORD_ITERATIONS = 120_000

fun hashPassword(password: String): String {
    val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
    val hash = pbkdf2(password, salt, PASSWORD_ITERATIONS)
    return "pbkdf2$PASSWORD_ITERATIONS$" +
        Base64.encodeToString(salt, Base64.NO_WRAP) + "$" +
        Base64.encodeToString(hash, Base64.NO_WRAP)
}

fun verifyPassword(id: String, password: String, stored: String?): Boolean {
    if (stored.isNullOrEmpty()) return false
    if (!stored.startsWith("pbkdf2$")) return legacyHash(id, password) == stored
    val parts = stored.split("$")
    if (parts.size != 4) return false
    val iterations = parts[1].toIntOrNull() ?: return false
    val salt = runCatching { Base64.decode(parts[2], Base64.NO_WRAP) }.getOrNull() ?: return false
    val expected = runCatching { Base64.decode(parts[3], Base64.NO_WRAP) }.getOrNull() ?: return false
    return MessageDigest.isEqual(expected, pbkdf2(password, salt, iterations))
}

private fun pbkdf2(password: String, salt: ByteArray, iterations: Int): ByteArray {
    val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
    return try {
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
    } finally {
        spec.clearPassword()
    }
}

private fun legacyHash(id: String, password: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest("$id:$password".toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

fun ageFromDob(millis: Long): Int {
    if (millis <= 0L) return 0
    val utc = TimeZone.getTimeZone("UTC")
    val dob = Calendar.getInstance(utc).apply { timeInMillis = millis }
    val now = Calendar.getInstance(utc)
    var age = now.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
    if (now.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) age--
    return age
}

fun formatDob(millis: Long): String {
    val fmt = SimpleDateFormat("d MMM yyyy", Locale.US)
    fmt.timeZone = TimeZone.getTimeZone("UTC")
    return fmt.format(Date(millis))
}

class ProfileStore(context: Context) {

    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("weplay_user", Context.MODE_PRIVATE)

    fun load(): UserProfile? {
        val name = prefs.getString("name", null) ?: return null
        val id = prefs.getString("id", null) ?: return null
        val provider = prefs.getString("provider", "google") ?: "google"
        val phone = prefs.getString("phone", null)
        val email = prefs.getString("email", null)
        val passwordHash = prefs.getString("password_hash", null)
        return UserProfile(
            name = name,
            dobMillis = prefs.getLong("dob", 0L),
            id = id,
            photoPath = prefs.getString("photo", null),
            provider = provider,
            phone = phone,
            email = email,
            level = prefs.getInt("level", 1),
            coins = prefs.getLong("coins", 500),
            family = prefs.getString("family", null),
            familyCode = prefs.getString("family_code", null),
            frame = prefs.getString("frame", null),
            username = prefs.getString("username", null) ?: defaultUsername(id),
            bio = prefs.getString("bio", "").orEmpty(),
            gender = prefs.getString("gender", "Not specified") ?: "Not specified",
            usernameChangedAt = prefs.getLong("username_changed_at", 0L),
            hasPassword = prefs.getBoolean("has_password", passwordHash != null),
            passwordHash = passwordHash,
            phoneVerified = prefs.getBoolean("phone_verified", phone != null),
            emailVerified = prefs.getBoolean("email_verified", email != null),
            linkedGoogle = prefs.getBoolean("linked_google", provider == "google"),
            linkedApple = prefs.getBoolean("linked_apple", false),
            linkedFacebook = prefs.getBoolean("linked_facebook", false),
            nickname = prefs.getString("nickname", null) ?: name,
            region = prefs.getString("region", "").orEmpty(),
            signature = prefs.getString("signature", "").orEmpty(),
            priceTag = prefs.getString("price_tag", "").orEmpty(),
            title = prefs.getString("title", "").orEmpty(),
            badge = prefs.getString("badge", "").orEmpty(),
            qrContactCard = prefs.getString("qr_contact_card", "").orEmpty(),
        )
    }

    fun save(profile: UserProfile) {
        prefs.edit()
            .putString("name", profile.name)
            .putLong("dob", profile.dobMillis)
            .putString("id", profile.id)
            .putString("photo", profile.photoPath)
            .putString("provider", profile.provider)
            .putString("phone", profile.phone)
            .putString("email", profile.email)
            .putInt("level", profile.level)
            .putLong("coins", profile.coins)
            .putString("family", profile.family)
            .putString("family_code", profile.familyCode)
            .putString("frame", profile.frame)
            .putString("username", profile.username)
            .putString("bio", profile.bio)
            .putString("gender", profile.gender)
            .putLong("username_changed_at", profile.usernameChangedAt)
            .putBoolean("has_password", profile.hasPassword)
            .putString("password_hash", profile.passwordHash)
            .putBoolean("phone_verified", profile.phoneVerified)
            .putBoolean("email_verified", profile.emailVerified)
            .putBoolean("linked_google", profile.linkedGoogle)
            .putBoolean("linked_apple", profile.linkedApple)
            .putBoolean("linked_facebook", profile.linkedFacebook)
            .putString("nickname", profile.nickname)
            .putString("region", profile.region)
            .putString("signature", profile.signature)
            .putString("price_tag", profile.priceTag)
            .putString("title", profile.title)
            .putString("badge", profile.badge)
            .putString("qr_contact_card", profile.qrContactCard)
            .apply()
    }

    /** Adjust coin balance; used by DM gift flow. Returns new balance. */
    fun adjustCoins(delta: Long): Long {
        val cur = prefs.getLong("coins", 500)
        val next = (cur + delta).coerceAtLeast(0)
        prefs.edit().putLong("coins", next).apply()
        return next
    }

    fun coinBalance(): Long = prefs.getLong("coins", 500)

    /** Save a compressed copy of a chat photo into files/chat_media. */
    fun saveChatPhoto(uri: Uri): String? {
        val dir = File(app.filesDir, "chat_media").apply { mkdirs() }
        val out = File(dir, "chat_${System.currentTimeMillis()}.jpg")
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            app.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            // chat photos: cap longest edge at ~1280px, JPEG q80 — good balance for a demo
            val longest = maxOf(bounds.outWidth, bounds.outHeight)
            var sample = 1
            while (longest > 0 && longest / sample > 1280) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = app.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return null
            FileOutputStream(out).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            }
            bitmap.recycle()
            out.absolutePath
        }.getOrNull()
    }

    /** Save a recorded voice note (OGG/Opus) into files/chat_media. */
    fun saveVoiceNote(input: java.io.InputStream): String? {
        val dir = File(app.filesDir, "chat_media").apply { mkdirs() }
        val ts = System.currentTimeMillis()
        val out = File(dir, "voice_$ts.ogg")
        return runCatching {
            java.io.FileOutputStream(out).use { fos ->
                input.copyTo(fos)
            }
            out.absolutePath
        }.getOrNull()
    }

    fun savePhoto(uri: Uri): String? {
        val dir = File(app.filesDir, "profile").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val out = File(dir, "avatar_${System.currentTimeMillis()}.jpg")
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            app.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            val longest = maxOf(bounds.outWidth, bounds.outHeight)
            var sample = 1
            while (longest > 0 && longest / sample > 720) sample *= 2

            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = app.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return null

            FileOutputStream(out).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, stream)
            }
            bitmap.recycle()
            out.absolutePath
        }.getOrNull()
    }

    fun clear() {
        prefs.edit().clear().apply()
        File(app.filesDir, "profile").deleteRecursively()
    }
}
