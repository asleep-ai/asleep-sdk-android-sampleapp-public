package ai.asleep.asleep_sdk_android_sampleapp.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceHelper private constructor() {

    companion object {
        private const val PREF_NAME = "sampleapp_prefs"
        private const val START_TIME_KEY = "start_time"

        private const val ASLEEP_PREF_NAME = "asleep_prefs"
        private const val ASLEEP_USER_ID_KEY = "user_id"

        private const val AUTO_TRACKING_PREF_NAME = "time_prefs"
        private const val START_TIME_HOUR_KEY = "start_time_hour"
        private const val START_TIME_MINUTE_KEY = "start_time_minute"
        private const val END_TIME_HOUR_KEY = "end_time_hour"
        private const val END_TIME_MINUTE_KEY = "end_time_minute"
        private const val ENABLE_TRACKING_KEY = "enable_tracking"

        fun getStartTrackingTime(context: Context): Long {
            return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getLong(START_TIME_KEY, 0L)
        }

        fun saveStartTrackingTime(context: Context, time: Long) {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
                .putLong(START_TIME_KEY, time).apply()
        }

        private fun getAsleepPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences(ASLEEP_PREF_NAME, Context.MODE_PRIVATE)
        }

        fun putAsleepUserId(context: Context, userId: String) {
            getAsleepPreferences(context).edit().putString(ASLEEP_USER_ID_KEY, userId).apply()
        }

        fun getAsleepUserId(context: Context): String? {
            return getAsleepPreferences(context).getString(ASLEEP_USER_ID_KEY, null)
        }

        private fun getAutoTrackingPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences(AUTO_TRACKING_PREF_NAME, Context.MODE_PRIVATE)
        }

        fun putStartHour(context: Context, hour: Int) {
            getAutoTrackingPreferences(context).edit().putInt(START_TIME_HOUR_KEY, hour).apply()
        }

        fun getStartHour(context: Context): Int {
            return getAutoTrackingPreferences(context).getInt(START_TIME_HOUR_KEY, 23) // 기본값 9시
        }

        fun putStartMinute(context: Context, minute: Int) {
            getAutoTrackingPreferences(context).edit().putInt(START_TIME_MINUTE_KEY, minute).apply()
        }

        fun getStartMinute(context: Context): Int {
            return getAutoTrackingPreferences(context).getInt(START_TIME_MINUTE_KEY, 30) // 기본값 0분
        }

        fun putEndHour(context: Context, hour: Int) {
            getAutoTrackingPreferences(context).edit().putInt(END_TIME_HOUR_KEY, hour).apply()
        }

        fun getEndHour(context: Context): Int {
            return getAutoTrackingPreferences(context).getInt(END_TIME_HOUR_KEY, 7) // 기본값 17시
        }

        fun putEndMinute(context: Context, minute: Int) {
            getAutoTrackingPreferences(context).edit().putInt(END_TIME_MINUTE_KEY, minute).apply()
        }

        fun getEndMinute(context: Context): Int {
            return getAutoTrackingPreferences(context).getInt(END_TIME_MINUTE_KEY, 0) // 기본값 0분
        }

        fun putAutoTrackingEnabled(context: Context, isEnabled: Boolean) {
            getAutoTrackingPreferences(context).edit().putBoolean(ENABLE_TRACKING_KEY, isEnabled).apply()
        }

        fun isAutoTrackingEnabled(context: Context): Boolean {
            return getAutoTrackingPreferences(context).getBoolean(ENABLE_TRACKING_KEY, false) // 기본값 false
        }
    }
}