package ai.asleep.asleep_sdk_android_sampleapp.utils

import ai.asleep.asleepsdk.Asleep
import android.util.Log

/**
 * Forwards the SDK's internal logs to Logcat.
 *
 * Pass this to `initAsleepConfig(asleepLogger = ...)`. Without a logger the SDK's internal
 * diagnostics (e.g. why a call was ignored) are invisible in a release AAR, which makes
 * intermittent issues in the field very hard to trace.
 */
object SampleAsleepLogger : Asleep.AsleepLogger {
    override fun d(tag: String, msg: String, throwable: Throwable?) {
        Log.d(tag, msg, throwable)
    }

    override fun e(tag: String, msg: String, throwable: Throwable?) {
        Log.e(tag, msg, throwable)
    }

    override fun i(tag: String, msg: String, throwable: Throwable?) {
        Log.i(tag, msg, throwable)
    }

    override fun w(tag: String, msg: String, throwable: Throwable?) {
        Log.w(tag, msg, throwable)
    }
}
