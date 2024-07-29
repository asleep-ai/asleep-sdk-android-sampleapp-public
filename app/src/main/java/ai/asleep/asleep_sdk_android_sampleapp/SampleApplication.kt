package ai.asleep.asleep_sdk_android_sampleapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SampleApplication : Application() {

    companion object {
        private lateinit var instance: SampleApplication

        val ACTION_AUTO_TRACKING: String by lazy {
            instance.packageName + ".ACTION_AUTO_TRACKING"
        }

    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}