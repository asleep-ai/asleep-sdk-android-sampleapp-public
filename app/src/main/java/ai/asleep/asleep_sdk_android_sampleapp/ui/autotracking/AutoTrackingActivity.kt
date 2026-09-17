package ai.asleep.asleep_sdk_android_sampleapp.ui.autotracking

import ai.asleep.asleep_sdk_android_sampleapp.service.AsleepService
import ai.asleep.asleep_sdk_android_sampleapp.ui.autotracking.AutoTrackingDialogFragment.Companion.AUTO_TRACKING_START_REQUEST_CODE
import ai.asleep.asleep_sdk_android_sampleapp.ui.autotracking.AutoTrackingDialogFragment.Companion.AUTO_TRACKING_STOP_REQUEST_CODE
import ai.asleep.asleep_sdk_android_sampleapp.utils.PreferenceHelper
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

/**
 * Launched by the auto-tracking alarm. It only gives the app a visible component from which a
 * microphone-typed Foreground Service may be started, then finishes. AsleepService keeps the
 * session running in its own process, so the app does not have to stay on screen.
 */
class AutoTrackingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(this::class.simpleName, "AutoTrackingActivity onCreate")
        Log.d(this::class.simpleName, "onCreate: MIC GRANTED - ${checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED}")

        val requestCode = intent.getIntExtra("AUTO_TRACKING_REQUEST_CODE", -1)

        when (requestCode) {
            AUTO_TRACKING_START_REQUEST_CODE -> startTrackingService()
            AUTO_TRACKING_STOP_REQUEST_CODE -> stopTrackingService()
            else -> finish()
        }
    }

    private fun startTrackingService() {
        // [Optional] store the start tracking time. This activity runs in the main process, the
        // same one that later reads the value back.
        PreferenceHelper.saveStartTrackingTime(applicationContext, System.currentTimeMillis())

        val intent = Intent(applicationContext, AsleepService::class.java).apply {
            action = AsleepService.ACTION_START_TRACKING
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        finish()
    }

    private fun stopTrackingService() {
        if (!AsleepService.isAsleepServiceRunning(applicationContext)) {
            Log.d(this::class.simpleName, "Don't exist AsleepService!!")
            finish()
            return
        }

        // ACTION_STOP_AUTO_TRACKING makes the service bring MainActivity up with the finished
        // session id once the session is closed, so the user lands on the report.
        val intent = Intent(applicationContext, AsleepService::class.java).apply {
            action = AsleepService.ACTION_STOP_AUTO_TRACKING
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        finish()
    }
}
