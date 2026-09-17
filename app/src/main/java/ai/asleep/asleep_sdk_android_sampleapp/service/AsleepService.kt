package ai.asleep.asleep_sdk_android_sampleapp.service

import ai.asleep.asleep_sdk_android_sampleapp.R
import ai.asleep.asleep_sdk_android_sampleapp.ui.Constants.EXTRA_REPORTING_SESSION_ID
import ai.asleep.asleep_sdk_android_sampleapp.ui.main.MainActivity
import ai.asleep.asleep_sdk_android_sampleapp.utils.PreferenceHelper
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground Service that hosts the `SleepTrackingManager` session.
 *
 * `Asleep.beginSleepTracking()` on the default branch runs the session inside the SDK's own
 * Foreground Service. Driving `SleepTrackingManager` directly means the app has to supply that
 * service itself, and this branch mirrors the SDK's arrangement: the service runs in a dedicated
 * `:RecordingService` process, so recording survives even when the UI process is reclaimed.
 *
 * Because of that process split, the UI cannot read [AsleepViewModel] directly - it binds to this
 * service and receives updates over the `IListener` AIDL callbacks.
 */
@AndroidEntryPoint
class AsleepService : LifecycleService() {

    @Inject
    lateinit var asleepViewModel: AsleepViewModel

    companion object {
        private const val TAG = "AsleepService"

        private const val FOREGROUND_SERVICE_ID = 1000
        private const val RECORD_NOTIFICATION_CHANNEL_ID = "12344321"

        const val ACTION_START_TRACKING = "ACTION_START_TRACKING"
        const val ACTION_STOP_TRACKING = "ACTION_STOP_TRACKING"
        const val ACTION_STOP_AUTO_TRACKING = "ACTION_STOP_AUTO_TRACKING"

        /**
         * The service lives in another process, so this is how the UI finds out whether a session
         * is already running when the user re-enters the app.
         */
        fun isAsleepServiceRunning(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (AsleepService::class.java.name == service.service.className) {
                    return true
                }
            }
            return false
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AsleepService onCreate: ")
        createNotificationChannel()
        startForegroundService()

        asleepViewModel.reportingSessionId.observe(this) {
            Log.d(TAG, "reportingSessionId : $it")
            stopSelf()

            // Auto tracking stopped while the app was not on screen: bring the UI up so the user
            // lands on the report.
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(EXTRA_REPORTING_SESSION_ID, it)
            }
            startActivity(intent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "onStartCommand: ${intent?.action}")

        if (intent == null || intent.action == null) { // means "user event didn't execute"

            asleepViewModel.continueTracking()

        } else if (intent.action == ACTION_START_TRACKING) {

            val storedUserId = PreferenceHelper.getAsleepUserId(applicationContext)
            asleepViewModel.startSleepTracking(storedUserId)

        } else if (intent.action == ACTION_STOP_TRACKING) {

            asleepViewModel.stopSleepTracking()
            stopSelf()

        } else if (intent.action == ACTION_STOP_AUTO_TRACKING) {

            asleepViewModel.isReporting = true
            asleepViewModel.stopSleepTracking()

        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel =
                NotificationChannel(
                    RECORD_NOTIFICATION_CHANNEL_ID,
                    getString(R.string.notification_channel_tracking_name),
                    NotificationManager.IMPORTANCE_LOW
                )
            notificationChannel.setSound(null, null)
            NotificationManagerCompat.from(applicationContext)
                .createNotificationChannel(notificationChannel)
        }
    }

    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            }
        )

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, RECORD_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_tracking_title))
                .setContentText(getString(R.string.notification_tracking_text))
                .setSmallIcon(R.mipmap.ic_sampleapp)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle(getString(R.string.notification_tracking_title))
                .setContentText(getString(R.string.notification_tracking_text))
                .setSmallIcon(R.mipmap.ic_sampleapp)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        }

        // The session records audio, so the service has to declare the microphone type.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(FOREGROUND_SERVICE_ID, notification, FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(FOREGROUND_SERVICE_ID, notification)
        }

        Log.d(TAG, "startForeground()")
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return asleepViewModel.binder
    }
}
