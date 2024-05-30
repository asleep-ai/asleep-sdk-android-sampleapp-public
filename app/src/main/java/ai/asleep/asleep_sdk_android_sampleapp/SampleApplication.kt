package ai.asleep.asleep_sdk_android_sampleapp

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SampleApplication : Application() {

    companion object {
        lateinit var sharedPref: SharedPreferences
    }

    override fun onCreate() {
        super.onCreate()
        sharedPref = applicationContext.getSharedPreferences(
            "preference_key", Context.MODE_PRIVATE)
    }
}