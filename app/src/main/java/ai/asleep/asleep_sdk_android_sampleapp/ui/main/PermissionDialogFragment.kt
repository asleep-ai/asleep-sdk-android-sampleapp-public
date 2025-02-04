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
            val builder = AlertDialog.Builder(activity)
            builder
                .setTitle(getString(R.string.permission_dialog_title))
                .setMessage(getString(R.string.permission_dialog_message))
                .setPositiveButton(R.string.permission_dialog_positive_button) { dialog, id ->
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
}