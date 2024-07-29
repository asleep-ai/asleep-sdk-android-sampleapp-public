package ai.asleep.asleep_sdk_android_sampleapp.receiver

import ai.asleep.asleep_sdk_android_sampleapp.SampleApplication
import ai.asleep.asleep_sdk_android_sampleapp.ui.autotracking.AutoTrackingActivity
import ai.asleep.asleep_sdk_android_sampleapp.ui.autotracking.AutoTrackingDialogFragment.Companion.AUTO_TRACKING_START_REQUEST_CODE
import ai.asleep.asleep_sdk_android_sampleapp.ui.autotracking.AutoTrackingDialogFragment.Companion.AUTO_TRACKING_STOP_REQUEST_CODE
import ai.asleep.asleep_sdk_android_sampleapp.utils.PreferenceHelper
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Calendar


class AutoTrackingBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(this::class.java.simpleName, "onReceive: ${intent.action}")

        when(intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                // set alarm on boot
            }
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                // request permission
            }
            SampleApplication.ACTION_AUTO_TRACKING -> {
                val requestCode = intent.getIntExtra("AUTO_TRACKING_REQUEST_CODE", -1)

                context.startActivity(Intent(context, AutoTrackingActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("AUTO_TRACKING_REQUEST_CODE", requestCode)
                })

                if (requestCode == AUTO_TRACKING_STOP_REQUEST_CODE) {
                    setNextDayAlarm(context)
                }
            }
        }

    }


    private fun setNextDayAlarm(context: Context) {

        val startHour: Int = PreferenceHelper.getStartHour(context)
        val startMinute: Int = PreferenceHelper.getStartMinute(context)

        val endHour: Int = PreferenceHelper.getEndHour(context)
        val endMinute: Int = PreferenceHelper.getEndMinute(context)

        if (PreferenceHelper.isAutoTrackingEnabled(context)) {
            setExactAlarm(context, startHour, startMinute, AUTO_TRACKING_START_REQUEST_CODE) // start alarm
            setExactAlarm(context, endHour, endMinute, AUTO_TRACKING_STOP_REQUEST_CODE) // end alarm
        }
    }

    private fun setExactAlarm(context: Context, hour: Int, minute: Int, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(context, AutoTrackingBroadcastReceiver::class.java).apply {
            action = SampleApplication.ACTION_AUTO_TRACKING
            putExtra("AUTO_TRACKING_REQUEST_CODE", requestCode)
        }.let { intent ->
            PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // next day
        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // 2 minutes for testing
//        calendar.add(Calendar.MINUTE, 2)
//        if (requestCode == AUTO_TRACKING_START_REQUEST_CODE) {
//            PreferenceHelper.putStartHour(context, calendar.get(Calendar.HOUR_OF_DAY))
//            PreferenceHelper.putStartMinute(context, calendar.get(Calendar.MINUTE))
//        } else {
//            PreferenceHelper.putEndHour(context, calendar.get(Calendar.HOUR_OF_DAY))
//            PreferenceHelper.putEndMinute(context, calendar.get(Calendar.MINUTE))
//        }


        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, alarmIntent)
        }catch (e:SecurityException) {
            e.printStackTrace()
        }
    }

}