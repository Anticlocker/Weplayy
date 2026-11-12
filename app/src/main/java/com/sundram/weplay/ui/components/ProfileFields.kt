package com.sundram.weplay.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sundram.weplay.R
import com.sundram.weplay.data.ProfileStore
import com.sundram.weplay.data.formatDob
import com.sundram.weplay.ui.theme.CardWhite
import com.sundram.weplay.ui.theme.CyanBrand
import com.sundram.weplay.ui.theme.Stroke
import com.sundram.weplay.ui.theme.TextBody
import com.sundram.weplay.ui.theme.TextMain
import com.sundram.weplay.ui.theme.TextMuted
import java.util.Calendar
import java.util.TimeZone

/**
 * Photo chooser wired to the profile store. Returns a click callback that
 * launches the system photo picker and hands back the saved copy path.
 */
@Composable
fun ProfilePhotoPicker(onPicked: (String?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val store = remember { ProfileStore(context) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) onPicked(store.savePhoto(uri))
    }
    return { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}

/** Date-of-birth field row: calendar icon, formatted date, optional trailing chip. */
@Composable
fun DobFieldRow(
    dobMillis: Long,
    trailing: String? = null,
    placeholder: String? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(CardWhite, RoundedCornerShape(14.dp))
            .border(1.dp, Stroke, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_calendar),
            contentDescription = null,
            tint = if (dobMillis > 0L) TextMain else TextMuted,
            modifier = Modifier.size(19.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            if (dobMillis > 0L) formatDob(dobMillis) else (placeholder ?: "Select date"),
            color = if (dobMillis > 0L) TextMain else TextMuted,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(trailing, color = TextBody, fontSize = 12.sp)
        }
    }
}

/** Date picker dialog shared by the profile forms. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DobPickerDialog(
    initialMillis: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = if (initialMillis > 0L) initialMillis else defaultDobMillis(),
        yearRange = 1900..Calendar.getInstance().get(Calendar.YEAR),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let(onConfirm)
                    onDismiss()
                }
            ) { Text("Done", color = CyanBrand, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextBody) }
        },
    ) {
        DatePicker(state = state, showModeToggle = false)
    }
}

/** Default date of birth the picker starts from. */
fun defaultDobMillis(): Long {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.set(2000, Calendar.JANUARY, 1, 0, 0, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
