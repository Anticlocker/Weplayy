package com.sundram.weplay.ui.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun formatMessageTime(ts: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = ts }
    return "%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
}

fun formatDayLabel(ts: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.US).format(Date(ts))
