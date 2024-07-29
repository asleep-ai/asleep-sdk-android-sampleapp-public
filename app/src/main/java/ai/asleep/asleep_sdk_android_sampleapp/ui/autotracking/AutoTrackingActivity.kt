package ai.asleep.asleep_sdk_android_sampleapp.ui.autotracking

import ai.asleep.asleep_sdk_android_sampleapp.service.AsleepService
import ai.asleep.asleep_sdk_android_sampleapp.ui.autotracking.AutoTrackingDialogFragment.Companion.AUTO_TRACKING_START_REQUEST_CODE
import ai.asleep.asleep_sdk_android_sampleapp.ui.autotracking.AutoTrackingDialogFragment.Companion.AUTO_TRACKING_STOP_REQUEST_CODE
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AutoTrackingActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(this::class.simpleName, "AutoTrackingActivity onCreate")

        val requestCode = intent.getIntExtra("AUTO_TRACKING_REQUEST_CODE", -1)

        when (requestCode) {
            AUTO_TRACKING_START_REQUEST_CODE -> startTrackingService()
            AUTO_TRACKING_STOP_REQUEST_CODE -> stopTrackingService()
        }
    }

    private fun startTrackingService() {
        val intent = Intent(applicationContext, AsleepService::class.java)
        intent.action = AsleepService.ACTION_START_TRACKING
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        finish()
    }

    private fun stopTrackingService() {
        val isRunningService = AsleepService.isAsleepServiceRunning(applicationContext)
        if (!isRunningService) {
            Log.d(this::class.simpleName, "Don't exist AsleepService!!")
            finish()
            return
        }

        val intent = Intent(applicationContext, AsleepService::class.java)
        intent.action = AsleepService.ACTION_STOP_AUTO_TRACKING
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        finish()
    }
}