package ai.asleep.asleep_sdk_android_sampleapp.utils

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun parseToDate(time: String): Date? {
    return try {
        // SimpleDateFormat supports XXX (ISO 8601 time zone) on minSdk 24+.
        val inputFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
        inputFormatter.parse(time)
    } catch (e: java.text.ParseException) {
        // Consider logging the error when parsing fails.
         Log.e("TimeUtils", "Failed to parse time: $time", e)
        null
    }
}

internal fun changeTimeFormat(time: String?): String? {
    if (time == null) return null
    
    return parseToDate(time)?.let { date ->
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date)
    }
}

internal fun getTimeOnly(time: String): String {
    return parseToDate(time)?.let { date ->
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(date)
    } ?: ""
}

internal fun getDateOnly(time: String): String {
    return parseToDate(time)?.let { date ->
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
    } ?: ""
}

internal fun formatTimestamp(timeInMillis: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timeInMillis))
}

internal fun getCurrentTime(): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return dateFormat.format(Date())
}

internal fun getTodayString(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}

internal fun getOneWeekAgoDateString(): String {
    val calendar = java.util.Calendar.getInstance()
    calendar.add(java.util.Calendar.DAY_OF_YEAR, -7)
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
}