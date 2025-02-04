package ai.asleep.asleep_sdk_android_sampleapp.ui.main

import ai.asleep.asleep_sdk_android_sampleapp.R
import ai.asleep.asleep_sdk_android_sampleapp.ui.Constants
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels

class InsufficientTimeDialogFragment : DialogFragment() {

    private val asleepViewModel: AsleepViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false // Prevent dialog dismissal on outside click
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { activity ->
            val builder = AlertDialog.Builder(activity)
            builder
                .setTitle(getString(R.string.insufficient_time_dialog_title))
                .setMessage(getString(R.string.insufficient_time_dialog_message, Constants.MIN_TRACKING_MINUTES))
                .setNegativeButton(getString(R.string.insufficient_time_dialog_negative_button)) { dialog, id ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.insufficient_time_dialog_positive_button)) { dialog, id ->
                    asleepViewModel.endSleepTracking()
                    dialog.dismiss()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}