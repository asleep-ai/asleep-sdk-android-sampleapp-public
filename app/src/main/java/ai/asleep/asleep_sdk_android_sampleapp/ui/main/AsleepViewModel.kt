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
import ai.asleep.asleepsdk.data.ProductInfo
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

    // Asleep.setup() only has to run once per process.
    private var isSetupCompleted = false

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

    /**
     * Registers this device as a product, then hands control to [onSetupComplete].
     *
     * The issued credential is cached by the SDK, so only the first run reaches the server;
     * later launches complete without a network call. Setup is also a gate - initAsleepConfig()
     * must not run until it has completed.
     */
    private fun setupAsleep(onSetupComplete: () -> Unit) {
        if (isSetupCompleted) {
            onSetupComplete()
            return
        }

        Asleep.setup(
            context = applicationContext,
            apiKey = Constants.ASLEEP_API_KEY,
            baseUrl = Constants.BASE_URL,
            callbackUrl = Constants.CALLBACK_URL,
            service = Constants.SERVICE_NAME,
            asleepLogger = SampleAsleepLogger,
            asleepSetupListener = object : Asleep.AsleepSetupListener {
                override fun onComplete() {
                    Log.d(TAG, "setup onComplete")
                    isSetupCompleted = true
                    onSetupComplete()
                }

                override fun onProgress(progress: Int) {
                    Log.d(TAG, "setup onProgress: $progress")
                }

                override fun onFail(errorCode: Int, detail: String) {
                    // 13000 (ERR_PRODUCT_REGISTER_FAILED): transient - retrying setup may succeed.
                    // 13400 (ERR_PRODUCT_REGISTER_REJECTED): permanent - the request itself is
                    // wrong (ProductInfo values or credentials), so retrying is pointless.
                    Log.e(TAG, "setup onFail: $errorCode - $detail")
                    val asleepError = AsleepError(errorCode, detail)
                    _asleepErrorCode.value = asleepError
                    _asleepState.value = AsleepState.STATE_ERROR(asleepError)
                }
            },
            productInfo = ProductInfo(
                model = Constants.PRODUCT_MODEL,
                identifierType = Asleep.ProductIdentifierType.SERIAL,
                identifierValue = PreferenceHelper.getOrCreateProductSerial(applicationContext)
            )
        )
    }

    fun initAsleepConfig() {
        if (_asleepState.value != AsleepState.STATE_IDLE) {
            return
        }

        if (_asleepConfig.value == null) {
            _asleepState.value = AsleepState.STATE_INITIALIZING
            val storedUserId = PreferenceHelper.getAsleepUserId(applicationContext)
            setupAsleep { requestAsleepConfig(storedUserId) }
        } else {
            _asleepState.value = AsleepState.STATE_INITIALIZED
        }
    }

    private fun requestAsleepConfig(storedUserId: String?, onInitialized: (() -> Unit)? = null) {
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
                    onInitialized?.invoke()
                }
            }
        )
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

    /* shouldGoToReport is a plain LiveData, so a re-subscribing Activity would receive the old
       `true` again and auto-navigate. The observer consumes it through this before navigating. */
    fun clearShouldGoToReport() {
        _shouldGoToReport.value = false
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
        setupAsleep {
            requestAsleepConfig(storedUserId) { beginSleepTracking() }
        }
    }
}