package com.sundram.weplay.ui.screens.settings

import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sundram.weplay.R
import com.sundram.weplay.ui.theme.CyanBrand
import com.sundram.weplay.ui.theme.MintGreen
import com.sundram.weplay.ui.theme.PinkGift
import com.sundram.weplay.ui.theme.CardWhite
import com.sundram.weplay.ui.theme.Stroke
import com.sundram.weplay.ui.theme.TextMuted
import com.sundram.weplay.ui.theme.TextMain
import com.sundram.weplay.ui.theme.OnBrand
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SettingsChoice(
    val value: String,
    val label: String,
    val description: String? = null,
)

@Composable
fun SettingsGroup(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (title != null) {
        Text(
            title,
            color = TextMain,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 2.dp, bottom = 7.dp),
        )
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(CardWhite, RoundedCornerShape(16.dp))
            .padding(vertical = 2.dp),
        content = content,
    )
    Spacer(Modifier.height(16.dp))
}

@Composable
fun SettingsRow(
    icon: Int,
    tint: Color,
    title: String,
    subtitle: String? = null,
    value: String? = null,
    valueColor: Color = TextMuted,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = onClick != null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && onClick != null) { onClick?.invoke() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIcon(icon, tint, enabled)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = if (enabled) TextMain else TextMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    color = if (enabled) TextMuted else TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(6.dp))
            trailing()
        }
        if (value != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                value,
                color = if (enabled) valueColor else TextMuted,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
        if (showChevron) {
            Spacer(Modifier.width(4.dp))
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
fun SettingsSwitchRow(
    icon: Int,
    tint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIcon(icon, tint, enabled)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = if (enabled) TextMain else TextMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                subtitle,
                color = if (enabled) TextMuted else TextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OnBrand,
                checkedTrackColor = CyanBrand,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = CardWhite,
                uncheckedBorderColor = Stroke,
            ),
        )
    }
}

@Composable
private fun SettingsIcon(icon: Int, tint: Color, enabled: Boolean) {
    Box(
        Modifier
            .size(34.dp)
            .background(
                tint.copy(alpha = if (enabled) 0.14f else 0.06f),
                RoundedCornerShape(10.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (enabled) tint else TextMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
fun SettingsVerifiedBadge() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_verified),
            contentDescription = null,
            tint = CyanBrand,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(3.dp))
        Text(
            "Verified",
            color = CyanBrand,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun SettingsChoiceDialog(
    title: String,
    options: List<SettingsChoice>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        title = { Text(title, color = TextMain, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(option.value)
                                onDismiss()
                            }
                            .padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = option.value == selected,
                            onClick = {
                                onSelect(option.value)
                                onDismiss()
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = CyanBrand,
                                unselectedColor = TextMuted,
                            ),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                option.label,
                                color = TextMain,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            if (option.description != null) {
                                Text(
                                    option.description,
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = CyanBrand) }
        },
    )
}

@Composable
fun SettingsInfoDialog(
    title: String,
    body: String,
    confirmLabel: String = "Close",
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        title = { Text(title, color = TextMain, fontWeight = FontWeight.Bold) },
        text = {
            Text(body, color = TextMuted, fontSize = 13.sp, lineHeight = 20.sp)
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(confirmLabel, color = CyanBrand) }
        },
    )
}

@Composable
fun SettingsTextField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = singleLine,
        textStyle = TextStyle(color = TextMain, fontSize = 15.sp),
        cursorBrush = SolidColor(CyanBrand),
        visualTransformation = if (password) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = if (singleLine) ImeAction.Done else ImeAction.Default,
        ),
        modifier = modifier
            .fillMaxWidth()
            .background(CardWhite, RoundedCornerShape(12.dp))
            .border(1.dp, Stroke, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        decorationBox = { field ->
            Box {
                if (value.isEmpty()) {
                    Text(placeholder, color = TextMuted, fontSize = 15.sp)
                }
                field()
            }
        },
    )
}

@Composable
fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    var confirmation by remember { mutableStateOf("") }
    val canDelete = confirmation.trim() == "DELETE"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        title = { Text("Delete account?", color = TextMain, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "This permanently removes your profile, wallet history, settings, blocked users " +
                        "and local reports from this device. It cannot be undone.",
                    color = TextMuted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "Type DELETE to confirm",
                    color = PinkGift,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                SettingsTextField(
                    value = confirmation,
                    onChange = { confirmation = it.take(10) },
                    placeholder = "DELETE",
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canDelete,
                onClick = {
                    onDismiss()
                    onConfirm()
                },
            ) {
                Text(
                    "Delete account",
                    color = if (canDelete) PinkGift else TextMuted,
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
fun SettingsEmptyState(title: String, body: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(CardWhite, RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            title,
            color = TextMain,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            body,
            color = TextMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp,
        )
    }
}

@Composable
fun SettingsFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        label,
        color = if (selected) OnBrand else TextMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(
                if (selected) CyanBrand else CardWhite,
                RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
fun SettingsValueBadge(text: String, color: Color = MintGreen) {
    Text(
        text,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(7.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

fun appVersion(context: Context): String = runCatching {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        info.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        info.versionCode.toLong()
    }
    "${info.versionName} ($code)"
}.getOrDefault("0.1.0 (1)")

fun formatStamp(millis: Long): String =
    SimpleDateFormat("d MMM yyyy, h:mm a", Locale.US).format(Date(millis))

fun formatDate(millis: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.US).format(Date(millis))

fun formatTime(minuteOfDay: Int): String {
    val hour = (minuteOfDay / 60).coerceIn(0, 23)
    val minute = minuteOfDay % 60
    val displayHour = when {
        hour % 12 == 0 -> 12
        else -> hour % 12
    }
    val suffix = if (hour < 12) "AM" else "PM"
    return "%d:%02d %s".format(Locale.US, displayHour, minute, suffix)
}

fun formatBytes(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "%.1f KB".format(Locale.US, bytes / 1024.0)
    bytes < 1024L * 1024L * 1024L -> "%.1f MB".format(Locale.US, bytes / (1024.0 * 1024.0))
    else -> "%.2f GB".format(Locale.US, bytes / (1024.0 * 1024.0 * 1024.0))
}

fun directorySize(directory: File?): Long = runCatching {
    directory?.walkBottomUp()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
}.getOrDefault(0L)

@Composable
fun SettingsRowDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 60.dp, end = 14.dp)
            .height(1.dp)
            .background(Stroke.copy(alpha = 0.45f))
    )
}

@Composable
fun SettingsInlineActions(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}
