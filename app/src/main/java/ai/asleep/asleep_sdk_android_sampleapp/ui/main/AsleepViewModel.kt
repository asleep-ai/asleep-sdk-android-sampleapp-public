package ai.asleep.asleep_sdk_android_sampleapp.ui.main

import ai.asleep.asleep_sdk_android_sampleapp.R
import ai.asleep.asleep_sdk_android_sampleapp.ui.Constants
import ai.asleep.asleep_sdk_android_sampleapp.ui.Constants.MIN_TRACKING_MINUTES
import ai.asleep.asleep_sdk_android_sampleapp.utils.AsleepError
import ai.asleep.asleep_sdk_android_sampleapp.utils.PreferenceHelper
import ai.asleep.asleep_sdk_android_sampleapp.utils.PreferenceHelper.Companion.getStartTrackingTime
import ai.asleep.asleep_sdk_android_sampleapp.utils.getCurrentTime
import ai.asleep.asleep_sdk_android_sampleapp.utils.isWarning
import ai.asleep.asleepsdk.Asleep
import ai.asleep.asleepsdk.data.AsleepConfig
import ai.asleep.asleepsdk.data.Session
import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class AsleepViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Application
) : ViewModel() {

    // Step 1: Init the Asleep Track SDK
    private var _asleepUserId = MutableLiveData<String?>(null)
    val asleepUserId: LiveData<String?> get() = _asleepUserId

    private var _asleepConfig = MutableLiveData<AsleepConfig?>(null)

    // Step 2~4: Tracking
    private var _sessionId = MutableStateFlow<String?>(null)
    val sessionId: StateFlow<String?> get() = _sessionId
    private var _sequence = MutableLiveData<Int?>(null)
    val sequence: LiveData<Int?> get() = _sequence
    private var _currentSleepData = MutableLiveData<Session?>(null)
    val currentSleepData: LiveData<Session?> = _currentSleepData
    private val asleepTrackingListener = object: Asleep.AsleepTrackingListener {
        override fun onStart(sessionId: String) {
            _asleepState.value = AsleepState.STATE_TRACKING_STARTED
        }

        override fun onPerform(sequence: Int) {
            _sequence.postValue(sequence)
            if (sequence > 10 && (sequence % 10 == 1 || sequence - (_analyzedSeq ?: 0) > 10)) {
                getCurrentSleepData(sequence)
            }
        }

        override fun onFinish(sessionId: String?) {
            _sessionId.value = sessionId

            if (asleepState.value is AsleepState.STATE_ERROR) {
                // Exit(Finish) due to Error
            } else {
                // Successful Finish
                if (enoughTrackingTime) {
                    _shouldGoToReport.postValue(true)
                } else {
                    _shouldGoToReport.postValue(false)
                }
                _asleepState.value = AsleepState.STATE_IDLE
            }
        }

        override fun onFail(errorCode: Int, detail: String) {
            handleErrorOrWarning(AsleepError(errorCode, detail))
        }
    }
    private var _analyzedSeq: Int? = null // The seq that succeeded by receiving a success callback from getCurrentSleepData()

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

    fun beginSleepTracking() {
        if (_asleepState.value == AsleepState.STATE_INITIALIZED) {
            _asleepState.value = AsleepState.STATE_TRACKING_STARTING
            _asleepConfig.value?.let {
                Asleep.beginSleepTracking(
                    asleepConfig = it,
                    asleepTrackingListener = asleepTrackingListener,
                    notificationTitle = applicationContext.getString(R.string.app_name),
                    notificationText = "",
                    notificationIcon = R.mipmap.ic_sampleapp,
                    notificationClass = MainActivity::class.java
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

    fun connectSleepTracking() {
        Asleep.connectSleepTracking(asleepTrackingListener)
        _asleepUserId.value = PreferenceHelper.getAsleepUserId(applicationContext)
        _asleepState.value = AsleepState.STATE_TRACKING_STARTED
    }

    private fun getCurrentSleepData(seq: Int) {
        Asleep.getCurrentSleepData(
            asleepSleepDataListener = object : Asleep.AsleepSleepDataListener {
                override fun onFail(errorCode: Int, detail: String) {
                    handleErrorOrWarning(AsleepError(errorCode, detail))
                }
                override fun onSleepDataReceived(session: Session) {
                    _currentSleepData.postValue(session)
                    _analyzedSeq = seq
                }
            }
        )
    }

    fun handleErrorOrWarning(asleepError: AsleepError) {
        val code = asleepError.code
        val message = asleepError.message
        if (isWarning(code)) {
            val existingMessage = _warningMessage.value
            _warningMessage.postValue("$existingMessage\n${getCurrentTime()} $code - $message")
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