package ai.asleep.asleep_sdk_android_sampleapp.utils

import ai.asleep.asleep_sdk_android_sampleapp.ui.ErrorDialogFragment
import ai.asleep.asleepsdk.AsleepErrorCode
import androidx.fragment.app.FragmentManager

data class AsleepError(val code: Int, val message: String)

internal fun showErrorDialog(fragmentManager: FragmentManager) {
    val dialog = ErrorDialogFragment()
    dialog.show(fragmentManager, "ErrorDialogFragment")
}

internal fun isWarning(errorCode: Int): Boolean {
    return errorCode in setOf(
        AsleepErrorCode.ERR_AUDIO_SILENCED,
        AsleepErrorCode.ERR_AUDIO_UNSILENCED,
        AsleepErrorCode.ERR_UPLOAD_FAILED,
    )
}

internal fun getDebugMessage(errorCode: AsleepError): String {
    if (isNetworkError(errorCode.message)) {
        return "Please check your network connection."
    }
    if (isMethodFormatInvalid(errorCode.message)) {
        return "Please check the method format, including argument values and types."
    }

    return when (errorCode.code) {
        AsleepErrorCode.ERR_MIC_PERMISSION -> "The app does not have microphone access permission."
        AsleepErrorCode.ERR_AUDIO -> "Another app is using the microphone, or there is an issue with the microphone settings."
        AsleepErrorCode.ERR_INVALID_URL -> "Please check the URL format."
        AsleepErrorCode.ERR_COMMON_EXPIRED -> "The API rate limit has been exceeded, or the plan has expired."
        AsleepErrorCode.ERR_UPLOAD_FORBIDDEN -> "initAsleepConfig() was performed elsewhere with the same ID during the tracking."
        AsleepErrorCode.ERR_UPLOAD_NOT_FOUND, AsleepErrorCode.ERR_CLOSE_NOT_FOUND -> "The session has already ended."
        else -> ""
    }
}

private fun isNetworkError(errorMessage: String): Boolean {
    val regex = Regex(".*(network.*error|error.*network).*", RegexOption.IGNORE_CASE)
    return regex.matches(errorMessage)
}

private fun isMethodFormatInvalid(errorMessage: String): Boolean {
    return errorMessage.contains("method not allowed", ignoreCase = true)
}
