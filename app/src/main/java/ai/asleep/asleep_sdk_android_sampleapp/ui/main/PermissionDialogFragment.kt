package ai.asleep.asleep_sdk_android_sampleapp.ui.main

import ai.asleep.asleep_sdk_android_sampleapp.R
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.DialogFragment

class PermissionDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { activity ->
            val messageResId = arguments?.getInt(ARG_MESSAGE_RES_ID)
                ?.takeIf { it != 0 } ?: R.string.permission_dialog_message_mic
            val builder = AlertDialog.Builder(activity)
            builder
                .setTitle(getString(R.string.permission_dialog_title))
                .setMessage(getString(messageResId))
                .setNegativeButton(R.string.permission_dialog_negative_button) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(R.string.permission_dialog_positive_button) { dialog, _ ->
                    val intent = Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.parse("package:${activity.packageName}")
                    }
                    startActivity(intent)
                    dialog.dismiss()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {
        private const val ARG_MESSAGE_RES_ID = "message_res_id"

        fun newInstance(messageResId: Int): PermissionDialogFragment {
            return PermissionDialogFragment().apply {
                arguments = Bundle().apply { putInt(ARG_MESSAGE_RES_ID, messageResId) }
            }
        }
    }
}
