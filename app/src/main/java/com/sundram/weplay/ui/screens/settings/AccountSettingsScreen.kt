package com.sundram.weplay.ui.screens.settings

import android.content.Context
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sundram.weplay.R
import com.sundram.weplay.data.UserProfile
import com.sundram.weplay.data.ageFromDob
import com.sundram.weplay.data.clearLocalAccount
import com.sundram.weplay.data.hashPassword
import com.sundram.weplay.data.verifyPassword
import com.sundram.weplay.ui.components.UserAvatar
import com.sundram.weplay.ui.nav.Routes
import com.sundram.weplay.ui.screens.SubScreen
import com.sundram.weplay.ui.theme.CyanBrand
import com.sundram.weplay.ui.theme.MintGreen
import com.sundram.weplay.ui.theme.PinkGift
import com.sundram.weplay.ui.theme.CardWhite
import com.sundram.weplay.ui.theme.TextMuted
import com.sundram.weplay.ui.theme.WarnOrange
import com.sundram.weplay.ui.theme.TextMain
import com.sundram.weplay.ui.theme.FacebookBlue
import java.util.concurrent.TimeUnit

@Composable
fun AccountSettingsScreen(
    nav: NavHostController,
    profile: UserProfile,
    onSave: (UserProfile) -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    var dialog by remember { mutableStateOf<String?>(null) }
    val verified = profile.phoneVerified || profile.emailVerified
    val linkedCount = listOf(
        profile.linkedGoogle,
        profile.linkedApple,
        profile.linkedFacebook,
    ).count { it }

    SubScreen(nav, "Account", "Profile, sign-in and security") {
        Column(
            Modifier
                .fillMaxWidth()
                .background(CardWhite, RoundedCornerShape(18.dp))
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(profile.name, profile.photoPath, size = 62.dp)
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            profile.name,
                            color = TextMain,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (verified) {
                            Spacer(Modifier.width(5.dp))
                            Icon(
                                painter = painterResource(R.drawable.ic_verified),
                                contentDescription = null,
                                tint = CyanBrand,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                    Text("@${profile.username}", color = CyanBrand, fontSize = 12.sp)
                    Text("ID ${profile.id}", color = TextMuted, fontSize = 11.sp)
                }
            }
            if (profile.bio.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(profile.bio, color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                buildString {
                    append(profile.gender)
                    if (profile.dobMillis > 0L) {
                        append(" · Age ")
                        append(ageFromDob(profile.dobMillis))
                    }
                },
                color = TextMuted,
                fontSize = 11.sp,
            )
        }

        Spacer(Modifier.height(16.dp))
        SettingsGroup("Account management") {
            SettingsRow(
                icon = R.drawable.ic_edit,
                tint = CyanBrand,
                title = "Edit profile",
                subtitle = "Avatar, nickname, ID, QR contact card, gender, birthday, region, signature, price tag, title, badge, level",
                onClick = { nav.navigate(Routes.PROFILE_EDIT) },
            )
            SettingsRowDivider()
            SettingsRow(
                icon = R.drawable.ic_logout,
                tint = PinkGift,
                title = "Delete account",
                subtitle = "Remove all local data permanently",
                showChevron = false,
                onClick = { dialog = "delete" },
            )
            SettingsRowDivider()
            SettingsRow(
                icon = R.drawable.ic_phone,
                tint = MintGreen,
                title = "Phone number",
                subtitle = profile.phone?.let { "+91 $it" } ?: "Add a phone number",
                trailing = { if (profile.phoneVerified) SettingsVerifiedBadge() },
                onClick = { dialog = "phone" },
            )
            SettingsRowDivider()
            LinkedAccountRow(label = "Google", linked = profile.linkedGoogle) {
                toggleLink(
                    context = context,
                    label = "Google",
                    linked = profile.linkedGoogle,
                    linkedCount = linkedCount,
                    onToggle = { onSave(profile.copy(linkedGoogle = !profile.linkedGoogle)) },
                )
            }
            if (profile.phone == null && !profile.linkedGoogle) {
                SettingsRowDivider()
                SettingsRow(
                    icon = R.drawable.ic_mail,
                    tint = TextMuted,
                    title = "Google bound",
                    subtitle = "Bind Google or add a phone number",
                    showChevron = false,
                )
            }
        }
    }

    when (dialog) {
        "username" -> UsernameDialog(
            profile = profile,
            onSave = {
                onSave(it)
                toast(context, "Username updated")
            },
            onDismiss = { dialog = null },
        )

        "phone" -> ContactDialog(
            kind = "phone",
            current = profile.phone,
            onSave = { value ->
                onSave(profile.copy(phone = value, phoneVerified = true))
                toast(context, "Phone number verified")
            },
            onDismiss = { dialog = null },
        )

        "email" -> ContactDialog(
            kind = "email",
            current = profile.email,
            onSave = { value ->
                onSave(profile.copy(email = value, emailVerified = true))
                toast(context, "Email address verified")
            },
            onDismiss = { dialog = null },
        )

        "password" -> PasswordDialog(
            profile = profile,
            onSave = {
                onSave(it)
                toast(context, "Password saved")
            },
            onDismiss = { dialog = null },
        )

        "logout" -> AlertDialog(
            onDismissRequest = { dialog = null },
            containerColor = CardWhite,
            title = { Text("Log out?", color = TextMain, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "You will return to the sign-in screen.",
                    color = TextMuted,
                    fontSize = 13.sp,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        dialog = null
                        onLogout()
                    }
                ) {
                    Text("Log out", color = PinkGift, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { dialog = null }) { Text("Cancel", color = TextMuted) }
            },
        )

        "delete" -> DeleteAccountDialog(
            onDismiss = { dialog = null },
            onConfirm = {
                clearLocalAccount(context)
                onLogout()
            },
        )
    }
}

@Composable
private fun LinkedAccountRow(label: String, linked: Boolean, onClick: () -> Unit) {
    SettingsRow(
        icon = when (label) {
            "Apple" -> R.drawable.ic_star
            "Facebook" -> R.drawable.ic_users
            else -> R.drawable.ic_mail
        },
        tint = when (label) {
            "Apple" -> TextMuted
            "Facebook" -> FacebookBlue
            else -> CyanBrand
        },
        title = label,
        subtitle = if (linked) "Connected to this account" else "Not connected",
        value = if (linked) "Linked" else "Link",
        valueColor = if (linked) MintGreen else CyanBrand,
        showChevron = false,
        onClick = onClick,
    )
}

private fun toggleLink(
    context: Context,
    label: String,
    linked: Boolean,
    linkedCount: Int,
    onToggle: () -> Unit,
) {
    if (linked && linkedCount <= 1) {
        toast(context, "Link another account before removing $label.")
        return
    }
    onToggle()
    toast(context, if (linked) "$label unlinked" else "$label linked")
}

@Composable
private fun UsernameDialog(
    profile: UserProfile,
    onSave: (UserProfile) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(profile.username) }
    val normalized = value.trim().lowercase()
    val valid = normalized.matches(Regex("^[a-z0-9_]{4,20}$"))
    val nextChangeAt = profile.usernameChangedAt + TimeUnit.DAYS.toMillis(30)
    val locked = profile.usernameChangedAt > 0L && System.currentTimeMillis() < nextChangeAt

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        title = { Text("Username", color = TextMain, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Usernames can be changed once every 30 days.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
                Spacer(Modifier.height(12.dp))
                SettingsTextField(
                    value = value,
                    onChange = { value = it.take(20) },
                    placeholder = "your_handle",
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Use 4 to 20 lowercase letters, numbers or underscores.",
                    color = if (valid) TextMuted else WarnOrange,
                    fontSize = 11.sp,
                )
                if (locked) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Next change available on ${formatDate(nextChangeAt)}.",
                        color = WarnOrange,
                        fontSize = 11.sp,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid && !locked && normalized != profile.username,
                onClick = {
                    onSave(
                        profile.copy(
                            username = normalized,
                            usernameChangedAt = System.currentTimeMillis(),
                        )
                    )
                    onDismiss()
                },
            ) {
                Text(
                    "Save",
                    color = if (valid && !locked && normalized != profile.username) {
                        CyanBrand
                    } else {
                        TextMuted
                    },
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
private fun ContactDialog(
    kind: String,
    current: String?,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val isPhone = kind == "phone"
    var step by remember { mutableStateOf(0) }
    var value by remember { mutableStateOf(current.orEmpty()) }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val valid = if (isPhone) {
        value.filter { it.isDigit() }.length == 10
    } else {
        Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        title = {
            Text(
                when {
                    step == 0 && isPhone -> "Phone number"
                    step == 0 -> "Email address"
                    isPhone -> "Verify phone"
                    else -> "Verify email"
                },
                color = TextMain,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column {
                if (step == 0) {
                    SettingsTextField(
                        value = value,
                        onChange = {
                            value = if (isPhone) {
                                it.filter { char -> char.isDigit() }.take(10)
                            } else {
                                it.take(60)
                            }
                            error = null
                        },
                        placeholder = if (isPhone) "9876543210" else "you@example.com",
                        keyboardType = if (isPhone) KeyboardType.Phone else KeyboardType.Email,
                    )
                    if (value.isNotBlank() && !valid) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (isPhone) "Enter a 10-digit phone number." else "Enter a valid email address.",
                            color = WarnOrange,
                            fontSize = 11.sp,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "No message is sent in this local build. A demo verification code is shown " +
                            "in the next step.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                    )
                } else {
                    Text(
                        if (isPhone) "Enter the code for +91 $value." else "Enter the code for $value.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Demo code: 123456",
                        color = CyanBrand,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(10.dp))
                    SettingsTextField(
                        value = code,
                        onChange = {
                            code = it.filter { char -> char.isDigit() }.take(6)
                            error = null
                        },
                        placeholder = "123456",
                        keyboardType = KeyboardType.Number,
                    )
                    if (error != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(error!!, color = WarnOrange, fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            if (step == 0) {
                TextButton(
                    enabled = valid,
                    onClick = {
                        step = 1
                        error = null
                    },
                ) {
                    Text(
                        "Send code",
                        color = if (valid) CyanBrand else TextMuted,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                TextButton(
                    enabled = code.length == 6,
                    onClick = {
                        if (code == "123456") {
                            onSave(value.trim())
                            onDismiss()
                        } else {
                            error = "Incorrect code. Use 123456."
                        }
                    },
                ) {
                    Text(
                        "Verify",
                        color = if (code.length == 6) CyanBrand else TextMuted,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (step == 1) {
                        step = 0
                        code = ""
                        error = null
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text(if (step == 1) "Back" else "Cancel", color = TextMuted)
            }
        },
    )
}

@Composable
private fun PasswordDialog(
    profile: UserProfile,
    onSave: (UserProfile) -> Unit,
    onDismiss: () -> Unit,
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val newValid = newPassword.length >= 6
    val matches = newPassword == confirmation
    val canSave = newValid && matches && (!profile.hasPassword || currentPassword.isNotEmpty())

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        title = {
            Text(
                if (profile.hasPassword) "Change password" else "Set password",
                color = TextMain,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column {
                if (profile.hasPassword) {
                    Text("Current password", color = TextMuted, fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                    SettingsTextField(
                        value = currentPassword,
                        onChange = {
                            currentPassword = it
                            error = null
                        },
                        placeholder = "Current password",
                        password = true,
                        keyboardType = KeyboardType.Password,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Text("New password", color = TextMuted, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                SettingsTextField(
                    value = newPassword,
                    onChange = {
                        newPassword = it
                        error = null
                    },
                    placeholder = "At least 6 characters",
                    password = true,
                    keyboardType = KeyboardType.Password,
                )
                Spacer(Modifier.height(12.dp))
                Text("Confirm new password", color = TextMuted, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                SettingsTextField(
                    value = confirmation,
                    onChange = {
                        confirmation = it
                        error = null
                    },
                    placeholder = "Repeat the password",
                    password = true,
                    keyboardType = KeyboardType.Password,
                )
                if (confirmation.isNotEmpty() && !matches) {
                    Spacer(Modifier.height(6.dp))
                    Text("Passwords do not match.", color = WarnOrange, fontSize = 11.sp)
                }
                if (error != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(error!!, color = WarnOrange, fontSize = 11.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "The password is stored as a local hash on this device. Password sign-in is " +
                        "not connected in this build.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    if (profile.hasPassword &&
                        !verifyPassword(profile.id, currentPassword, profile.passwordHash)
                    ) {
                        error = "Current password is incorrect."
                    } else {
                        onSave(
                            profile.copy(
                                hasPassword = true,
                                passwordHash = hashPassword(newPassword),
                            )
                        )
                        onDismiss()
                    }
                },
            ) {
                Text(
                    "Save",
                    color = if (canSave) CyanBrand else TextMuted,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) }
        },
    )
}

private fun toast(context: Context, text: String) {
    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}
