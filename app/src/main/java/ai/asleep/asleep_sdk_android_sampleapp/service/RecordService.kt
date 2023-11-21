package ai.asleep.asleep_sdk_android_sampleapp.service

import ai.asleep.asleep_sdk_android_sampleapp.R
import ai.asleep.asleep_sdk_android_sampleapp.ui.MainActivity
import ai.asleep.asleep_sdk_android_sampleapp.ui.MainViewModel
import ai.asleep.asleepsdk.Asleep
import ai.asleep.asleepsdk.tracking.SleepTrackingManager
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RecordService : Service() {

    private var sleepTrackingManager: SleepTrackingManager? = null

    @Inject
    lateinit var viewModel: MainViewModel


    companion object {
        private const val FOREGROUND_SERVICE_ID = 1000
        private const val RECORD_NOTIFICATION_CHANNEL_ID = "12344321"

        fun isRecordServiceRunning(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (RecordService::class.java.name == service.service.className) {
                    return true
                }
            }
            return false
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundService()
        createSleepTrackingManager()
        sleepTrackingManager?.startSleepTracking()
        viewModel.setSequence(null)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sleepTrackingManager?.stopSleepTracking()
    }

    private fun createNotificationChannel() {
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

        val notification = Notification.Builder(this, RECORD_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Tracking Sleep")
            .setContentText("Running ForegroundService")
            .setSmallIcon(R.mipmap.ic_sampleapp)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(FOREGROUND_SERVICE_ID, notification)

        Log.d(">>>>> ", "startForeground()")
    }

    private fun createSleepTrackingManager() {
        val asleepConfig = if (viewModel.isDeveloperModeOn) {
            viewModel.developerModeAsleepConfig
        } else {
            viewModel.asleepConfig
        }

        sleepTrackingManager = Asleep.createSleepTrackingManager(asleepConfig, object : SleepTrackingManager.TrackingListener {
            override fun onCreate() {
                Log.d(">>>>> sleepTrackingManager - ", "onCreate: start tracking")
                Log.d(">>>>> RecordService", "onCreate) TrackingStatus.sessionId: ${sleepTrackingManager?.getTrackingStatus()?.sessionId}")
            }

            override fun onUpload(sequence: Int) {
                Log.d(">>>>> sleepTrackingManager - ", "onUpload: $sequence")
                viewModel.setSequence(sequence)
            }

            override fun onClose(sessionId: String) {
                Log.d(">>>>> sleepTrackingManager - ", "onClose: $sessionId")
                Log.d(">>>>> RecordService", "onClose) TrackingStatus.sessionId: ${sleepTrackingManager?.getTrackingStatus()?.sessionId}")
                viewModel.sessionIdLiveData.postValue(sessionId)
            }

            override fun onFail(errorCode: Int, detail: String) {
                Log.d(">>>>> sleepTrackingManager - ", "onFail: $errorCode - $detail")
                viewModel.setErrorData(errorCode, detail)
            }
        })

        Log.d(">>>>> RecordService", "before create) TrackingStatus.sessionId: ${sleepTrackingManager?.getTrackingStatus()?.sessionId}")
    }

//    private fun requestAnalysis() {
//        sleepTrackingManager?.requestAnalysis(object : SleepTrackingManager.AnalysisListener {
//            override fun onSuccess(session: Session) {
//                Log.d(">>>>> requestAnalysis - ", "${session.toString()}")
//            }
//
//            override fun onFail(errorCode: Int, detail: String) {
//                Log.d(">>>>> requestAnalysis - ", "onFail: $errorCode - $detail")
//            }
//        })
//    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }
}