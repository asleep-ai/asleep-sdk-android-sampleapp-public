package ai.asleep.asleep_sdk_android_sampleapp.ui.autotracking

import ai.asleep.asleep_sdk_android_sampleapp.R
import ai.asleep.asleep_sdk_android_sampleapp.SampleApplication
import ai.asleep.asleep_sdk_android_sampleapp.receiver.AutoTrackingBroadcastReceiver
import ai.asleep.asleep_sdk_android_sampleapp.utils.PreferenceHelper
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.DialogFragment
import java.util.Calendar

class AutoTrackingDialogFragment : DialogFragment() {

    private lateinit var startTimePicker: TimePicker
    private lateinit var endTimePicker: TimePicker
    private lateinit var enableTrackingSwitch: SwitchCompat

    companion object {
        const val AUTO_TRACKING_START_REQUEST_CODE = 1001
        const val AUTO_TRACKING_STOP_REQUEST_CODE = 1002
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Settings.canDrawOverlays(context)) {
                Toast.makeText(context, "Overlay permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Overlay permission not granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_auto_tracking, container, false)

        val btnClose: ImageButton = view.findViewById(R.id.btn_close)
        btnClose.setOnClickListener { dismiss() }

        startTimePicker = view.findViewById(R.id.start_time_picker)
        endTimePicker = view.findViewById(R.id.end_time_picker)
        enableTrackingSwitch = view.findViewById(R.id.switch_enable_tracking)
        val btnSave: Button = view.findViewById(R.id.btn_save)

        loadSavedTime()

        btnSave.setOnClickListener {
            if (!Settings.canDrawOverlays(context)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context?.packageName}"))
                overlayPermissionLauncher.launch(intent)
            } else {
                saveTime()
            }
        }
        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        requestExactAlarmPermission()
    }

    private fun loadSavedTime() {
        val startHour = PreferenceHelper.getStartHour(requireContext())
        val startMinute = PreferenceHelper.getStartMinute(requireContext())
        val endHour = PreferenceHelper.getEndHour(requireContext())
        val endMinute = PreferenceHelper.getEndMinute(requireContext())
        val isTrackingEnabled = PreferenceHelper.isAutoTrackingEnabled(requireContext())

        startTimePicker.hour = startHour
        startTimePicker.minute = startMinute
        endTimePicker.hour = endHour
        endTimePicker.minute = endMinute
        enableTrackingSwitch.isChecked = isTrackingEnabled
    }

    private fun saveTime() {
        val startHour = startTimePicker.hour
        val startMinute = startTimePicker.minute
        val endHour = endTimePicker.hour
        val endMinute = endTimePicker.minute
        val isTrackingEnabled = enableTrackingSwitch.isChecked

        PreferenceHelper.putStartHour(requireContext(), startHour)
        PreferenceHelper.putStartMinute(requireContext(), startMinute)
        PreferenceHelper.putEndHour(requireContext(), endHour)
        PreferenceHelper.putEndMinute(requireContext(), endMinute)
        PreferenceHelper.putAutoTrackingEnabled(requireContext(), isTrackingEnabled)

        cancelAlarm(AUTO_TRACKING_START_REQUEST_CODE)
        cancelAlarm(AUTO_TRACKING_STOP_REQUEST_CODE)

        if (isTrackingEnabled) {
            Log.d(this.tag, "$startHour:$startMinute ~ $endHour:$endMinute")
            setExactAlarm(startHour, startMinute, AUTO_TRACKING_START_REQUEST_CODE) // start alarm
            setExactAlarm(endHour, endMinute, AUTO_TRACKING_STOP_REQUEST_CODE) // end alarm
        }
        dismiss()
    }

    private fun setExactAlarm(hour: Int, minute: Int, requestCode: Int) {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(requireContext(), AutoTrackingBroadcastReceiver::class.java).apply {
            action = SampleApplication.ACTION_AUTO_TRACKING
            putExtra("AUTO_TRACKING_REQUEST_CODE", requestCode)
        }.let { intent ->
            PendingIntent.getBroadcast(requireContext(), requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                alarmIntent
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun cancelAlarm(requestCode: Int) {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(requireContext(), AutoTrackingBroadcastReceiver::class.java).apply {
            action = SampleApplication.ACTION_AUTO_TRACKING
            putExtra("AUTO_TRACKING_REQUEST_CODE", requestCode)
        }.let { intent ->
            PendingIntent.getBroadcast(requireContext(), requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        alarmManager.cancel(alarmIntent)
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = requireContext().getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context?.packageName))
                startActivity(intent)
            }
        }
    }
}
