package ai.asleep.asleep_sdk_android_sampleapp.utils

import ai.asleep.asleep_sdk_android_sampleapp.R
import android.content.Context

/*
 * The SDK reports sleep/snoring stages as plain integers. The sample app shows the raw value
 * together with a human readable label; the labels are shared with the iOS sample app.
 */

internal fun getSleepStageLabel(context: Context, stage: Int): String = when (stage) {
    0 -> context.getString(R.string.sleep_stage_label_awake)
    1 -> context.getString(R.string.sleep_stage_label_light)
    2 -> context.getString(R.string.sleep_stage_label_deep)
    3 -> context.getString(R.string.sleep_stage_label_rem)
    else -> context.getString(R.string.sleep_stage_label_unknown)
}

internal fun getSnoringStageLabel(context: Context, stage: Int): String = when (stage) {
    0 -> context.getString(R.string.snoring_stage_label_not_snoring)
    1 -> context.getString(R.string.snoring_stage_label_snoring)
    else -> context.getString(R.string.sleep_stage_label_unknown)
}

internal fun getSleepStageText(context: Context, stage: Int): String =
    context.getString(R.string.tracking_label_current_sleep_stage, stage, getSleepStageLabel(context, stage))

internal fun getSnoringStageText(context: Context, stage: Int): String =
    context.getString(R.string.tracking_label_current_snoring_stage, stage, getSnoringStageLabel(context, stage))
