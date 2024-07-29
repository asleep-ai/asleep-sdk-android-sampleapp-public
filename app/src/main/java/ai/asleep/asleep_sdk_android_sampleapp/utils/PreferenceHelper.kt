package ai.asleep.asleep_sdk_android_sampleapp.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceHelper private constructor() {

    companion object {
        private const val PREF_NAME = "time_prefs"
        private const val START_TIME_HOUR_KEY = "start_time_hour"
        private const val START_TIME_MINUTE_KEY = "start_time_minute"
        private const val END_TIME_HOUR_KEY = "end_time_hour"
        private const val END_TIME_MINUTE_KEY = "end_time_minute"
        private const val ENABLE_TRACKING_KEY = "enable_tracking"
        private const val USER_ID = "user_id"
        private const val START_TRACKING_TIME = "start_tracking_time"

        private fun getSharedPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }

        fun putStartHour(context: Context, hour: Int) {
            getSharedPreferences(context).edit().putInt(START_TIME_HOUR_KEY, hour).apply()
        }

        fun getStartHour(context: Context): Int {
            return getSharedPreferences(context).getInt(START_TIME_HOUR_KEY, 23) // 기본값 9시
        }

        fun putStartMinute(context: Context, minute: Int) {
            getSharedPreferences(context).edit().putInt(START_TIME_MINUTE_KEY, minute).apply()
        }

        fun getStartMinute(context: Context): Int {
            return getSharedPreferences(context).getInt(START_TIME_MINUTE_KEY, 30) // 기본값 0분
        }

        fun putEndHour(context: Context, hour: Int) {
            getSharedPreferences(context).edit().putInt(END_TIME_HOUR_KEY, hour).apply()
        }

        fun getEndHour(context: Context): Int {
            return getSharedPreferences(context).getInt(END_TIME_HOUR_KEY, 7) // 기본값 17시
        }

        fun putEndMinute(context: Context, minute: Int) {
            getSharedPreferences(context).edit().putInt(END_TIME_MINUTE_KEY, minute).apply()
        }

        fun getEndMinute(context: Context): Int {
            return getSharedPreferences(context).getInt(END_TIME_MINUTE_KEY, 0) // 기본값 0분
        }

        fun putAutoTrackingEnabled(context: Context, isEnabled: Boolean) {
            getSharedPreferences(context).edit().putBoolean(ENABLE_TRACKING_KEY, isEnabled).apply()
        }

        fun isAutoTrackingEnabled(context: Context): Boolean {
            return getSharedPreferences(context).getBoolean(ENABLE_TRACKING_KEY, false) // 기본값 false
        }


        fun putUserId(context: Context, userId: String) {
            getSharedPreferences(context).edit().putString(USER_ID, userId).apply()
        }

        fun getUserId(context: Context): String {
            return getSharedPreferences(context).getString(USER_ID, "")?:""
        }

        fun putStartTrackingTime(context: Context, startTrackingTime: String) {
            getSharedPreferences(context).edit().putString(START_TRACKING_TIME, startTrackingTime).apply()
        }

        fun getStartTrackingTime(context: Context): String {
            return getSharedPreferences(context).getString(START_TRACKING_TIME, "")?:""
        }
    }
}