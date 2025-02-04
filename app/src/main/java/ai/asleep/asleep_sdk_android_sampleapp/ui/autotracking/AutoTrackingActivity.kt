package ai.asleep.asleep_sdk_android_sampleapp.ui.autotracking

import ai.asleep.asleep_sdk_android_sampleapp.ui.autotracking.AutoTrackingDialogFragment.Companion.AUTO_TRACKING_START_REQUEST_CODE
import ai.asleep.asleep_sdk_android_sampleapp.ui.autotracking.AutoTrackingDialogFragment.Companion.AUTO_TRACKING_STOP_REQUEST_CODE
import ai.asleep.asleep_sdk_android_sampleapp.ui.main.AsleepViewModel
import ai.asleep.asleep_sdk_android_sampleapp.utils.PreferenceHelper
import ai.asleep.asleepsdk.Asleep
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AutoTrackingActivity: AppCompatActivity() {

    private val asleepViewModel: AsleepViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(this::class.simpleName, "AutoTrackingActivity onCreate")
        Log.d(this::class.simpleName, "onCreate: MIC GRANTED - ${checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED}")

        val requestCode = intent.getIntExtra("AUTO_TRACKING_REQUEST_CODE", -1)

        when (requestCode) {
            AUTO_TRACKING_START_REQUEST_CODE -> startTrackingService()
            AUTO_TRACKING_STOP_REQUEST_CODE -> stopTrackingService()
        }
    }

    private fun startTrackingService() {
        val storedUserId = PreferenceHelper.getAsleepUserId(applicationContext)
        if (!Asleep.isSleepTrackingAlive(applicationContext)) {
            PreferenceHelper.saveStartTrackingTime(this, System.currentTimeMillis())
            asleepViewModel.beginAutoSleepTracking(storedUserId)
        } else {
            asleepViewModel.connectSleepTracking()
        }

        finish()
    }

    private fun stopTrackingService() {
        val isRunningService = Asleep.isSleepTrackingAlive(applicationContext)
        if (!isRunningService) {
            Log.d(this::class.simpleName, "Don't exist AsleepService!!")
            finish()
            return
        }
        asleepViewModel.endSleepTracking()
        finish()
    }
}
