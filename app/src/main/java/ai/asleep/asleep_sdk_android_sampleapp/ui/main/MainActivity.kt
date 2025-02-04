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
import ai.asleep.asleep_sdk_android_sampleapp.utils.showErrorDialog
import ai.asleep.asleepsdk.Asleep
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

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var permissionManager: PermissionManager

    private val asleepViewModel: AsleepViewModel by viewModels()

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
                        binding.btnControlTracking.text = "No user id"
                        binding.btnControlTracking.isEnabled = false
                    }
                    AsleepState.STATE_INITIALIZED -> {
                        binding.llButtons.visibility = View.VISIBLE
                        binding.llTrackingInfo.visibility = View.GONE
                        binding.btnControlTracking.apply {
                            isEnabled = true
                            text = getString(R.string.button_text_start_tracking)
                            setOnClickListener {
                                if (permissionManager.allPermissionsGranted.value == true) {
                                    asleepViewModel.beginSleepTracking()
                                } else {
                                    permissionManager.checkAndRequestPermissions()
                                }
                            }
                        }
                    }
                    AsleepState.STATE_TRACKING_STARTING, AsleepState.STATE_TRACKING_STOPPING -> {
                        binding.llButtons.visibility = View.GONE
                        binding.btnControlTracking.isEnabled = false
                        binding.btnControlTracking.text = "LOADING"
                    }
                    AsleepState.STATE_TRACKING_STARTED -> {
                        binding.llButtons.visibility = View.GONE
                        binding.llTrackingInfo.visibility = View.VISIBLE
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
                        binding.btnControlTracking.isEnabled = false
                        showErrorDialog(supportFragmentManager)
                    }
                }
            }
        }

        binding.apply {
            binding.btnGotoReport.setOnClickListener { gotoReportActivity(Constants.StateName.INIT.name) }
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
            val sequenceText = "${getString(R.string.tracking_label_uploaded_sequence)} $sequence"
            binding.tvSequence.text = sequenceText
        }
        asleepViewModel.currentSleepData.observe(this) {
            it?.let { binding.tvCurrentSleepData.text = getCurrentSleepDataText(it) }
        }
        asleepViewModel.warningMessage.observe(this) { warningMessage ->
            binding.tvWarningMessage.text = warningMessage
        }
        asleepViewModel.shouldGoToReport.observe(this) { shouldGoToReport ->
            if (shouldGoToReport) { gotoReportActivity(Constants.StateName.TRACKING.name) }
        }
    }

    private fun showInsufficientTimeDialog() {
        val dialog = InsufficientTimeDialogFragment()
        dialog.show(supportFragmentManager, "InsufficientTimeDialogFragment")
    }

    private fun getCurrentSleepDataText(session: Session): String {
        var currentStagesText = ""
        session.sleepStages?.let {
            if (it.isNotEmpty()) {
                currentStagesText += "Current Sleep Stage: ${it.last()}\n"
            } else {
                currentStagesText += "Current Stages: checkable if sequence is 10+"
            }
        }
        session.snoringStages?.let {
            if (it.isNotEmpty()) {
                currentStagesText += "Current Snoring Stage: ${it.last()}\n"
            }
        }
        return currentStagesText
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