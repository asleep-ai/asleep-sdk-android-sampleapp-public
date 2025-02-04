package ai.asleep.asleep_sdk_android_sampleapp.ui

import ai.asleep.asleep_sdk_android_sampleapp.BuildConfig

object Constants {
    val BASE_URL: String? = null
    val CALLBACK_URL: String? = null
    const val ASLEEP_API_KEY = BuildConfig.ASLEEP_API_KEY
    const val SERVICE_NAME = "AsleepSampleApp"

    const val MIN_TRACKING_MINUTES = 5

    // intent extra names
    const val EXTRA_ASLEEP_USER_ID = "ASLEEP_USER_ID"
    const val EXTRA_SESSION_ID = "SESSION_ID"
    const val EXTRA_FROM_STATE = "FROM_STATE"

    enum class StateName {
        INIT, TRACKING
    }
}