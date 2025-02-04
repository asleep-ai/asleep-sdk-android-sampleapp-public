package ai.asleep.asleep_sdk_android_sampleapp.ui.main

import ai.asleep.asleep_sdk_android_sampleapp.utils.AsleepError

sealed class AsleepState {
    data object STATE_IDLE: AsleepState() {
        override fun toString() = "STATE_IDLE"
    }
    data object STATE_INITIALIZING : AsleepState() {
        override fun toString() = "STATE_INITIALIZING"
    }
    data object STATE_INITIALIZED : AsleepState() {
        override fun toString() = "STATE_INITIALIZED"
    }
    data object STATE_TRACKING_STARTING : AsleepState() {
        override fun toString() = "STATE_TRACKING_STARTING"
    }
    data object STATE_TRACKING_STARTED : AsleepState() {
        override fun toString() = "STATE_TRACKING_STARTED"
    }
    data object STATE_TRACKING_STOPPING : AsleepState() {
        override fun toString() = "STATE_TRACKING_STOPPING"
    }
    data class STATE_ERROR(val errorCode: AsleepError) : AsleepState() {
        override fun toString() = "STATE_ERROR"
    }
}