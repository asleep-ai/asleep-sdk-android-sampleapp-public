package ai.asleep.asleep_sdk_android_sampleapp.service

import ai.asleep.asleep_sdk_android_sampleapp.IListener
import ai.asleep.asleep_sdk_android_sampleapp.R
import ai.asleep.asleep_sdk_android_sampleapp.SampleApplication
import ai.asleep.asleep_sdk_android_sampleapp.ui.MainActivity
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
import android.os.RemoteCallbackList
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


const val TAG = ">>>>> AsleepService"
@AndroidEntryPoint
class AsleepService : LifecycleService() {

    @Inject
    lateinit var asleepViewModel: AsleepViewModel

    private val listeners = RemoteCallbackList<IListener>()

    companion object {
        private const val FOREGROUND_SERVICE_ID = 1000
        private const val RECORD_NOTIFICATION_CHANNEL_ID = "12344321"

        const val ACTION_START_TRACKING = "ACTION_START_TRACKING"
        const val ACTION_STOP_TRACKING = "ACTION_STOP_TRACKING"

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
        Log.d(TAG, "onCreate: ")
        createNotificationChannel()
        startForegroundService()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "onStartCommand:")

        if (intent == null || intent.action == null) { // means "user event didn't execute"

            asleepViewModel.continueTracking()

        } else if (intent.action == ACTION_START_TRACKING) {

            val storedUserId = SampleApplication.sharedPref.getString("user_id", null)
            asleepViewModel.startSleepTracking(storedUserId)

        } else if (intent.action == ACTION_STOP_TRACKING) {

            asleepViewModel.stopSleepTracking(this)

        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel =
                NotificationChannel(
                    RECORD_NOTIFICATION_CHANNEL_ID,
                    "Recording In Progress",
                    NotificationManager.IMPORTANCE_HIGH
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
                .setContentTitle("Tracking Sleep")
                .setContentText("Running ForegroundService")
                .setSmallIcon(R.mipmap.ic_sampleapp)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("Tracking Sleep")
                .setContentText("Running ForegroundService")
                .setSmallIcon(R.mipmap.ic_sampleapp)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(FOREGROUND_SERVICE_ID, notification, FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(FOREGROUND_SERVICE_ID, notification)
        }

        Log.d(">>>>> ", "startForeground()")
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return asleepViewModel.binder
    }

    override fun onDestroy() {
        super.onDestroy()

    }
}