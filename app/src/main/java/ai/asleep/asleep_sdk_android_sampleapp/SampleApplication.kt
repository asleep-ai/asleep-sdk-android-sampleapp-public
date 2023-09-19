package ai.asleep.asleep_sdk_android_sampleapp

import ai.asleep.asleepsdk.Asleep
import ai.asleep.asleepsdk.data.AsleepConfig
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SampleApplication : Application() {

    companion object {
        lateinit var sharedPref: SharedPreferences

        // for Asleep SDK
        private var _asleepConfig: AsleepConfig? = null
        val asleepConfig: AsleepConfig? get() = _asleepConfig

        private var _userId: String? = null
        val userId: String? get() = _userId

//        fun setUserId(userId: String?) { _userId = userId }
    }

    override fun onCreate() {
        super.onCreate()
        sharedPref = applicationContext.getSharedPreferences(
            "preference_key", Context.MODE_PRIVATE)
        _userId = sharedPref.getString("user_id", null)
        initAsleepConfig()
    }

    private fun initAsleepConfig() {
        Asleep.initAsleepConfig(
            context = applicationContext,
            apiKey = BuildConfig.ASLEEP_API_KEY,
            userId = _userId,
            baseUrl = null,
            callbackUrl = null,
            service = "SampleApp",
            object : Asleep.AsleepConfigListener {
                override fun onSuccess(userId: String?, asleepConfig: AsleepConfig?) {
                    _asleepConfig = asleepConfig

                    with (sharedPref.edit()) {
                        putString("user_id", userId)
                        apply()
                    }
                    _userId = userId

                    Log.d(">>>> AsleepConfigListener", "onSuccess: userId - $userId")
                }
                override fun onFail(errorCode: Int, detail: String) {
                    Log.d(">>>> AsleepConfigListener", "onFail: $errorCode - $detail")
                }
            })
    }
}