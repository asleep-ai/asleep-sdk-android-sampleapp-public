package ai.asleep.asleep_sdk_android_sampleapp.ui.main

import ai.asleep.asleep_sdk_android_sampleapp.R
import ai.asleep.asleep_sdk_android_sampleapp.ui.Constants
import ai.asleep.asleep_sdk_android_sampleapp.ui.Constants.MIN_TRACKING_MINUTES
import ai.asleep.asleep_sdk_android_sampleapp.utils.AsleepError
import ai.asleep.asleep_sdk_android_sampleapp.utils.PreferenceHelper
import ai.asleep.asleep_sdk_android_sampleapp.utils.SampleAsleepLogger
import ai.asleep.asleep_sdk_android_sampleapp.utils.PreferenceHelper.Companion.getStartTrackingTime
import ai.asleep.asleep_sdk_android_sampleapp.utils.getCurrentTime
import ai.asleep.asleep_sdk_android_sampleapp.utils.isWarning
import ai.asleep.asleepsdk.Asleep
import ai.asleep.asleepsdk.data.AsleepConfig
import ai.asleep.asleepsdk.data.RecordingType
import ai.asleep.asleepsdk.data.Session
import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import javax.inject.Inject
import kotlin.math.abs

private const val TAG = "AsleepViewModel"

@HiltViewModel
class AsleepViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Application
) : ViewModel() {

    // Step 1: Init the Asleep Track SDK
    private var _asleepUserId = MutableLiveData<String?>(null)
    val asleepUserId: LiveData<String?> get() = _asleepUserId

    private var _asleepConfig = MutableLiveData<AsleepConfig?>(null)

    /**
     * Directory the SDK writes the recordings into.
     *
     * Only the segments the server flagged are kept, and the filtering runs after the session is
     * closed - so the files are in place once onComplete() has been delivered.
     */
    private val recordingPath: String by lazy {
        File(applicationContext.filesDir, "recordings").apply { mkdirs() }.absolutePath
    }

    // Step 2~4: Tracking
    private var _sessionId = MutableStateFlow<String?>(null)
    val sessionId: StateFlow<String?> get() = _sessionId
    private var _sequence = MutableLiveData<Int?>(null)
    val sequence: LiveData<Int?> get() = _sequence
    private var _currentSleepData = MutableLiveData<Session?>(null)
    val currentSleepData: LiveData<Session?> = _currentSleepData

    /**
     * CompletableAsleepTrackingListener adds onComplete() on top of the regular callbacks.
     *
     * onFinish() only means the session was closed - the server is still analysing it. The final
     * report, and the recordings that survive the filtering, arrive in onComplete(). Passing a
     * recordingPath requires this listener: the SDK rejects the plain AsleepTrackingListener.
     */
    private val completableAsleepTrackingListener = object : Asleep.CompletableAsleepTrackingListener {
        override fun onStart(sessionId: String) {
            _sessionId.value = sessionId
            _asleepState.value = AsleepState.STATE_TRACKING_STARTED
        }

        override fun onPerform(sequence: Int) {
            _sequence.postValue(sequence)
        }

        override fun onFinish(sessionId: String?) {
            _sessionId.value = sessionId

            if (asleepState.value is AsleepState.STATE_ERROR) {
                // Exit(Finish) due to Error
            } else {
                // The session is closed, but the report is not ready yet - wait for onComplete().
                _asleepState.value = AsleepState.STATE_IDLE
            }
        }

        override fun onComplete(session: Session?) {
            Log.d(TAG, "onComplete: session=${session?.id} state=${session?.state}")
            session?.let { _currentSleepData.postValue(it) }

            logRecordingFiles(session?.id ?: _sessionId.value)

            if (asleepState.value is AsleepState.STATE_ERROR) {
                return
            }
            _shouldGoToReport.postValue(enoughTrackingTime)
        }

        override fun onFail(errorCode: Int, detail: String) {
            handleErrorOrWarning(AsleepError(errorCode, detail))
        }
    }

    private var _asleepErrorCode = MutableLiveData<AsleepError?>(null)
    val asleepErrorCode: LiveData<AsleepError?> get() = _asleepErrorCode
    private var _warningMessage = MutableLiveData("")
    val warningMessage: LiveData<String> get() = _warningMessage

    // state
    private var _asleepState = MutableStateFlow<AsleepState>(AsleepState.STATE_IDLE)
    val asleepState: StateFlow<AsleepState> get() = _asleepState

    // go to report
    private var enoughTrackingTime: Boolean = false
    private var _shouldGoToReport = MutableLiveData(false)
    val shouldGoToReport: LiveData<Boolean> = _shouldGoToReport

    fun clearAsleepError() {
        _asleepErrorCode.value = null
        if (Asleep.isSleepTrackingAlive(applicationContext)) {
            _asleepState.value = AsleepState.STATE_TRACKING_STARTED
        } else if (_asleepConfig.value != null) {
            _asleepState.value = AsleepState.STATE_INITIALIZED
        } else {
            _asleepState.value = AsleepState.STATE_IDLE
        }
    }

    fun initAsleepConfig() {
        if (_asleepState.value != AsleepState.STATE_IDLE) {
            return
        }

        if (_asleepConfig.value == null) {
            _asleepState.value = AsleepState.STATE_INITIALIZING
            val storedUserId = PreferenceHelper.getAsleepUserId(applicationContext)
            Asleep.initAsleepConfig(
                context = applicationContext,
                apiKey = Constants.ASLEEP_API_KEY,
                userId = storedUserId,
                baseUrl = Constants.BASE_URL,
                callbackUrl = Constants.CALLBACK_URL,
                service = Constants.SERVICE_NAME,
                asleepLogger = SampleAsleepLogger,
                asleepConfigListener = object : Asleep.AsleepConfigListener {
                    override fun onFail(errorCode: Int, detail: String) {
                        _asleepErrorCode.value = AsleepError(errorCode, detail)
                        _asleepState.value = AsleepState.STATE_ERROR(AsleepError(errorCode, detail))
                    }

                    override fun onSuccess(userId: String?, asleepConfig: AsleepConfig?) {
                        _asleepConfig.value = asleepConfig
                        _asleepUserId.value = userId
                        userId?.let { PreferenceHelper.putAsleepUserId(applicationContext, it) }
                        _asleepState.value = AsleepState.STATE_INITIALIZED
                    }
                }
            )
        } else {
            _asleepState.value = AsleepState.STATE_INITIALIZED
        }
    }

    fun beginSleepTracking(recordingType: RecordingType = RecordingType.ALL) {
        if (_asleepState.value == AsleepState.STATE_INITIALIZED) {
            _asleepState.value = AsleepState.STATE_TRACKING_STARTING
            _asleepConfig.value?.let {
                Asleep.beginSleepTracking(
                    asleepConfig = it,
                    completableAsleepTrackingListener = completableAsleepTrackingListener,
                    notificationTitle = applicationContext.getString(R.string.app_name),
                    notificationText = "",
                    notificationIcon = R.mipmap.ic_sampleapp,
                    notificationClass = MainActivity::class.java,
                    recordingPath = recordingPath,
                    recordingType = recordingType
                )
            }
            PreferenceHelper.saveStartTrackingTime(applicationContext, System.currentTimeMillis())
        }
    }

    fun endSleepTracking() {
        if (Asleep.isSleepTrackingAlive(applicationContext)) {
            _asleepState.value = AsleepState.STATE_TRACKING_STOPPING
            Asleep.endSleepTracking()
        }
    }

    /* shouldGoToReport is a plain LiveData, so a re-subscribing Activity would receive the old
       `true` again and auto-navigate. The observer consumes it through this before navigating. */
    fun clearShouldGoToReport() {
        _shouldGoToReport.value = false
    }

    fun connectSleepTracking() {
        Asleep.connectSleepTracking(completableAsleepTrackingListener)
        _asleepUserId.value = PreferenceHelper.getAsleepUserId(applicationContext)
        _asleepState.value = AsleepState.STATE_TRACKING_STARTED
    }

    /**
     * Lists what the SDK kept for the finished session.
     *
     * Reading the segment index touches the filesystem, and onComplete() is delivered on the main
     * looper, so the lookup runs on its own thread.
     */
    private fun logRecordingFiles(sessionId: String?) {
        if (sessionId == null) {
            Log.w(TAG, "No session id - skipping the recording lookup")
            return
        }

        Thread {
            val recordingFileManager = Asleep.createRecordingFileManager(recordingPath)
            val segments = recordingFileManager.getAllSegments(sessionId)
            Log.d(TAG, "Recordings for $sessionId: ${segments.size} segment(s) kept")
            segments.forEach {
                Log.d(
                    TAG,
                    "seq=${it.segmentIndex} snoring=${it.isSnoringDetected} " +
                        "breath=${it.isBreathDetected} maxDb=${it.maxDb} path=${it.filePath}"
                )
            }
            Log.d(TAG, "Stored sessions: ${recordingFileManager.getSessions()}")
        }.start()
    }

    fun handleErrorOrWarning(asleepError: AsleepError) {
        val code = asleepError.code
        val message = asleepError.message
        if (isWarning(code)) {
            // Keep the log free of a leading blank line so the warning box starts at its header.
            val existingMessage = _warningMessage.value
            val newLine = "${getCurrentTime()} $code - $message"
            _warningMessage.postValue(
                if (existingMessage.isNullOrEmpty()) newLine else "$existingMessage\n$newLine"
            )
        } else {
            _asleepErrorCode.postValue(asleepError)
            _asleepState.value = AsleepState.STATE_ERROR(asleepError)
        }
    }

    fun isEnoughTrackingTime(): Boolean {
        val startTime = getStartTrackingTime(applicationContext)
        val timeDifferenceInMinutes = abs(startTime - System.currentTimeMillis()) / (60 * 1000)
        enoughTrackingTime = timeDifferenceInMinutes >= MIN_TRACKING_MINUTES
        return enoughTrackingTime
    }

    // call beginTracking() in initAsleepConfig()'s onSuccess callback
    fun beginAutoSleepTracking(storedUserId: String?) {
        Asleep.initAsleepConfig(
            context = applicationContext,
            apiKey = Constants.ASLEEP_API_KEY,
            userId = storedUserId,
            baseUrl = Constants.BASE_URL,
            callbackUrl = Constants.CALLBACK_URL,
            service = Constants.SERVICE_NAME,
                asleepLogger = SampleAsleepLogger,
            asleepConfigListener = object : Asleep.AsleepConfigListener {
                override fun onFail(errorCode: Int, detail: String) {
                    _asleepErrorCode.value = AsleepError(errorCode, detail)
                    _asleepState.value = AsleepState.STATE_ERROR(AsleepError(errorCode, detail))
                }

                override fun onSuccess(userId: String?, asleepConfig: AsleepConfig?) {
                    _asleepConfig.value = asleepConfig
                    _asleepUserId.value = userId
                    userId?.let { PreferenceHelper.putAsleepUserId(applicationContext, it) }
                    _asleepState.value = AsleepState.STATE_INITIALIZED
                    beginSleepTracking()
                }
            }
        )
    }
}
