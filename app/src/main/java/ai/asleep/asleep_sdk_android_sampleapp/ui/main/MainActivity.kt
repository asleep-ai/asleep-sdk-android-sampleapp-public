package ai.asleep.asleep_sdk_android_sampleapp.ui.main

import ai.asleep.asleep_sdk_android_sampleapp.BuildConfig
import ai.asleep.asleep_sdk_android_sampleapp.R
import ai.asleep.asleep_sdk_android_sampleapp.databinding.ActivityMainBinding
import ai.asleep.asleep_sdk_android_sampleapp.ui.Constants
import ai.asleep.asleep_sdk_android_sampleapp.ui.Constants.EXTRA_ASLEEP_USER_ID
import ai.asleep.asleep_sdk_android_sampleapp.ui.Constants.EXTRA_FROM_STATE
import ai.asleep.asleep_sdk_android_sampleapp.ui.Constants.EXTRA_SESSION_ID
import ai.asleep.asleep_sdk_android_sampleapp.ui.autotracking.AutoTrackingDialogFragment
import ai.asleep.asleep_sdk_android_sampleapp.ui.report.ReportActivity
import ai.asleep.asleep_sdk_android_sampleapp.utils.PreferenceHelper
import ai.asleep.asleep_sdk_android_sampleapp.utils.formatTimestamp
import ai.asleep.asleep_sdk_android_sampleapp.utils.getSleepStageText
import ai.asleep.asleep_sdk_android_sampleapp.utils.getSnoringStageText
import ai.asleep.asleep_sdk_android_sampleapp.utils.showErrorDialog
import ai.asleep.asleepsdk.Asleep
import ai.asleep.asleepsdk.data.RecordingType
import ai.asleep.asleepsdk.data.Session
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // Audio is uploaded every 30 seconds, so one sequence equals half a minute.
    private val uploadIntervalMinutes = 0.5

    private lateinit var binding: ActivityMainBinding
    private lateinit var permissionManager: PermissionManager

    private val asleepViewModel: AsleepViewModel by viewModels()

    /* The spinner entries are the enum names in declaration order, so the position maps straight
       onto RecordingType.values(). An out-of-range position falls back to ALL, the SDK default. */
    private val selectedRecordingType: RecordingType
        get() = RecordingType.values()
            .getOrElse(binding.spRecordingType.selectedItemPosition) { RecordingType.ALL }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionManager = PermissionManager(this)
        setPermissionObserver()
        permissionManager.checkAllPermissions()

        // Define Main screen by AsleepState
        lifecycleScope.launch {
            asleepViewModel.asleepState.collect { state ->
                when (state) {
                    AsleepState.STATE_IDLE -> {
                        checkRunningService()
                    }
                    AsleepState.STATE_INITIALIZING -> {
                        binding.llButtons.visibility = View.VISIBLE
                        binding.btnControlTracking.text = getString(R.string.button_text_initializing)
                        binding.btnControlTracking.isEnabled = false
                    }
                    AsleepState.STATE_INITIALIZED -> {
                        flushPendingReportNavigation()
                        binding.llButtons.visibility = View.VISIBLE
                        binding.llTrackingInfo.visibility = View.GONE
                        showTrackingDoneIfFinished()
                        binding.btnControlTracking.apply {
                            isEnabled = true
                            text = getString(R.string.button_text_start_tracking)
                            setOnClickListener {
                                if (permissionManager.allPermissionsGranted.value == true) {
                                    asleepViewModel.beginSleepTracking(selectedRecordingType)
                                } else {
                                    permissionManager.checkAndRequestPermissions()
                                }
                            }
                        }
                    }
                    AsleepState.STATE_TRACKING_STARTING, AsleepState.STATE_TRACKING_STOPPING -> {
                        binding.llButtons.visibility = View.GONE
                        binding.llTrackingDone.visibility = View.GONE
                        binding.btnControlTracking.isEnabled = false
                        binding.btnControlTracking.text = getString(R.string.button_text_loading)
                    }
                    AsleepState.STATE_TRACKING_STARTED -> {
                        flushPendingReportNavigation()
                        binding.llButtons.visibility = View.GONE
                        binding.llTrackingDone.visibility = View.GONE
                        binding.llTrackingInfo.visibility = View.VISIBLE
                        showStartTrackingTime()
                        binding.btnControlTracking.apply {
                            isEnabled = true
                            text = getString(R.string.button_text_stop_tracking)
                            setOnClickListener {
                                if (asleepViewModel.isEnoughTrackingTime()) {
                                    asleepViewModel.endSleepTracking()
                                } else {
                                    showInsufficientTimeDialog()
                                }
                            }
                        }
                    }
                    is AsleepState.STATE_ERROR -> {
                        pendingReportState = null
                        binding.btnControlTracking.isEnabled = false
                        showErrorDialog(supportFragmentManager)
                    }
                }
            }
        }

        binding.apply {
            binding.btnGotoReport.setOnClickListener { requestReportNavigation(Constants.StateName.INIT.name) }
            btnAutotracking.setOnClickListener {
                if (permissionManager.allPermissionsGranted.value == true) {
                    val dialog: DialogFragment = AutoTrackingDialogFragment()
                    dialog.show(supportFragmentManager, "AutoTrackingDialogFragment")
                } else {
                    permissionManager.checkAndRequestPermissions()
                }
            }
            tvVersion.text = BuildConfig.VERSION_NAME
        }

        asleepViewModel.asleepUserId.observe(this) { asleepUserId ->
            binding.tvAsleepUserId.text = getString(R.string.status_message_asleep_id, asleepUserId)
        }
        asleepViewModel.sequence.observe(this) { sequence ->
            binding.tvSequence.text = getUploadedSequenceText(sequence)
        }
        asleepViewModel.currentSleepData.observe(this) {
            it?.let { session -> showCurrentSleepData(session) }
        }
        asleepViewModel.warningMessage.observe(this) { warningMessage ->
            binding.tvWarningMessage.text = warningMessage
            binding.llWarning.visibility =
                if (warningMessage.isNullOrBlank()) View.GONE else View.VISIBLE
        }
        asleepViewModel.shouldGoToReport.observe(this) { shouldGoToReport ->
            if (shouldGoToReport) {
                asleepViewModel.clearShouldGoToReport()
                requestReportNavigation(Constants.StateName.TRACKING.name)
            }
        }
    }

    /* Report entry must wait for the SDK to finish initializing: ReportActivity runs its own
       initAsleepConfig, and the SDK silently ignores an init that overlaps one already in
       progress - the report screen would then wait for a callback that never comes. Every entry
       point (button, auto-navigation, service wake-up) funnels through this gate. */
    private var pendingReportState: String? = null

    private fun requestReportNavigation(state: String) {
        when (asleepViewModel.asleepState.value) {
            AsleepState.STATE_INITIALIZED, AsleepState.STATE_TRACKING_STARTED -> gotoReportActivity(state)
            is AsleepState.STATE_ERROR -> Unit  // init failed - the error dialog is already shown
            else -> pendingReportState = state  // queued until STATE_INITIALIZED / STARTED
        }
    }

    private fun flushPendingReportNavigation() {
        pendingReportState?.let {
            pendingReportState = null
            gotoReportActivity(it)
        }
    }

    private fun showInsufficientTimeDialog() {
        val dialog = InsufficientTimeDialogFragment()
        dialog.show(supportFragmentManager, "InsufficientTimeDialogFragment")
    }

    private fun showStartTrackingTime() {
        val startTime = PreferenceHelper.getStartTrackingTime(applicationContext)
        val startTimeText = if (startTime > 0L) formatTimestamp(startTime) else ""
        binding.tvStartTrackingTime.text =
            getString(R.string.tracking_label_start_time, startTimeText)
    }

    /* The sequence is zero based, so sequence 0 already covers the first 30 second upload. */
    private fun getUploadedSequenceText(sequence: Int?): String {
        val elapsedMinutes = uploadIntervalMinutes * ((sequence ?: -1) + 1)
        return getString(
            R.string.tracking_label_uploaded_sequence,
            sequence?.toString() ?: "-",
            String.format(Locale.US, "%.1f", elapsedMinutes)
        )
    }

    /* Stages only arrive once enough audio has been analysed, so each line stays hidden
       until the SDK reports a value. */
    private fun showCurrentSleepData(session: Session) {
        val sleepStage = session.sleepStages?.lastOrNull()
        binding.tvCurrentSleepStage.apply {
            if (sleepStage == null) {
                visibility = View.GONE
            } else {
                text = getSleepStageText(context, sleepStage)
                visibility = View.VISIBLE
            }
        }

        val snoringStage = session.snoringStages?.lastOrNull()
        binding.tvCurrentSnoringStage.apply {
            if (snoringStage == null) {
                visibility = View.GONE
            } else {
                text = getSnoringStageText(context, snoringStage)
                visibility = View.VISIBLE
            }
        }
    }

    /* After a session ends the SDK returns to the initialized state - keep the session id on
       screen until the next tracking starts so it can be copied for a report lookup. */
    private fun showTrackingDoneIfFinished() {
        val sessionId = asleepViewModel.sessionId.value
        if (sessionId.isNullOrEmpty()) {
            binding.llTrackingDone.visibility = View.GONE
        } else {
            binding.tvDoneSessionId.text = getString(R.string.tracking_done_session_id, sessionId)
            binding.llTrackingDone.visibility = View.VISIBLE
        }
    }

    private fun setPermissionObserver() {
        permissionManager.batteryOptimized.observe(this) { batteryOptimized ->
            binding.tvIgnoreBatteryOpt.text = getString(
                R.string.status_message_ignore_battery_optimization,
                batteryOptimized.toString()
            )
        }
        permissionManager.micPermission.observe(this) { micPermission ->
            binding.tvMicPermission.text = getString(
                R.string.status_message_microphone_permission,
                micPermission.toString()
            )
        }
        permissionManager.notificationPermission.observe(this) { notificationPermission ->
            binding.tvNotiPermission.text = getString(
                R.string.status_message_notification_permission,
                notificationPermission.toString()
            )
        }
    }

    private fun gotoReportActivity(state: String) {
        val asleepUserId = asleepViewModel.asleepUserId.value
        asleepUserId?.let { userid ->
            val intent = Intent(this@MainActivity, ReportActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_ASLEEP_USER_ID, userid)
                putExtra(EXTRA_FROM_STATE, state)
                if (state.equals(Constants.StateName.TRACKING.name)) {
                    putExtra(EXTRA_SESSION_ID, asleepViewModel.sessionId.value)
                }
            }
            startActivity(intent)
        }
    }

    private fun checkRunningService() {
        val isRunningService = Asleep.isSleepTrackingAlive(applicationContext)
        if (isRunningService) {
            asleepViewModel.connectSleepTracking()
        } else {
            asleepViewModel.initAsleepConfig()
        }
    }
}