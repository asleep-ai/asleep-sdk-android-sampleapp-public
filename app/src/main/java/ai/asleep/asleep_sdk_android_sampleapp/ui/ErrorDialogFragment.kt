package ai.asleep.asleep_sdk_android_sampleapp.ui

import ai.asleep.asleep_sdk_android_sampleapp.R
import ai.asleep.asleep_sdk_android_sampleapp.ui.main.AsleepViewModel
import ai.asleep.asleep_sdk_android_sampleapp.ui.main.MainActivity
import ai.asleep.asleep_sdk_android_sampleapp.ui.report.ReportActivity
import ai.asleep.asleep_sdk_android_sampleapp.ui.report.ReportViewModel
import ai.asleep.asleep_sdk_android_sampleapp.utils.getDebugMessage
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels

class ErrorDialogFragment : DialogFragment() {

    private val asleepViewModel: AsleepViewModel by activityViewModels()
    private val reportViewModel: ReportViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false // Prevent dialog dismissal on outside click
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { activity ->
            val builder = AlertDialog.Builder(activity)
            when (activity) {
                is MainActivity -> {
                    asleepViewModel.asleepErrorCode.value?.let {
                        builder
                            .setTitle("${it.code}: ${it.message}")
                            .setMessage(getDebugMessage(it))
                            .setPositiveButton(getString(R.string.dialog_confirm_button)) { dialog, id ->
                                asleepViewModel.clearAsleepError()
                                dialog.dismiss()
                            }
                    } ?: { builder.setTitle("Unknown Issue") }
                }
                is ReportActivity -> {
                    reportViewModel.asleepErrorCode.value?.let {
                        builder
                            .setTitle("${it.code}: ${it.message}")
                            .setMessage(getDebugMessage(it))
                            .setPositiveButton(getString(R.string.dialog_confirm_button)) { dialog, id ->
                                reportViewModel.clearAsleepError()
                                dialog.dismiss()
                            }
                    } ?: { builder.setTitle("Unknown Issue") }
                }
            }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}