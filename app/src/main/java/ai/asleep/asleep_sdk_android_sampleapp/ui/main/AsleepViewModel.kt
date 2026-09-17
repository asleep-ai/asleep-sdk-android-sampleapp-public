package ai.asleep.asleep_sdk_android_sampleapp.ui.main

import ai.asleep.asleep_sdk_android_sampleapp.IAsleepService
import ai.asleep.asleep_sdk_android_sampleapp.IListener
import ai.asleep.asleep_sdk_android_sampleapp.data.CurrentSleepData
import ai.asleep.asleep_sdk_android_sampleapp.data.ErrorCode
import ai.asleep.asleep_sdk_android_sampleapp.service.AsleepService
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
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

/**
 * UI-side ViewModel.
 *
 * The tracking session runs in the `:RecordingService` process, so this ViewModel never touches
 * `SleepTrackingManager`. It starts and stops the session with intents to [AsleepService] and
 * receives every update over the [IListener] AIDL callbacks while bound to that service.
 *
 * `initAsleepConfig()` is still called here, in the UI process, only to obtain and display the
 * Asleep user id and to enable the start button; the service initializes its own config in its own
 * process.
 */
@HiltViewModel
class AsleepViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Application
) : ViewModel() {

    // Step 1: Init the Asleep Track SDK
    // Seeded from preferences so that a freshly started UI process already knows the user id when
    // the service is the one that created it.
    private var _asleepUserId = MutableLiveData(PreferenceHelper.getAsleepUserId(applicationContext))
    val asleepUserId: LiveData<String?> get() = _asleepUserId

    private var _asleepConfig = MutableLiveData<AsleepConfig?>(null)

    // Step 2~4: Tracking - all of it happens in the service process
    private var _sessionId = MutableStateFlow<String?>(null)
    val sessionId: StateFlow<String?> get() = _sessionId
    private var _sequence = MutableLiveData<Int?>(null)
    val sequence: LiveData<Int?> get() = _sequence
    private var _currentSleepData = MutableLiveData<CurrentSleepData?>(null)
    val currentSleepData: LiveData<CurrentSleepData?> = _currentSleepData

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

    private var asleepService: IAsleepService? = null
    private var isBound = false

    /*
     * AIDL callbacks arrive on a binder thread while the service is inside its broadcast loop.
     * Every handler therefore only hands the work to the main thread and returns immediately,
     * so the service process is never blocked on the UI process.
     */
    private val listener = object : IListener.Stub() {
        override fun onUserIdReceived(userId: String?) {
            viewModelScope.launch(Dispatchers.Main) {
                Log.d(TAG, "onUserIdReceived userId: $userId")
                _asleepUserId.value = userId
                userId?.let { PreferenceHelper.putAsleepUserId(applicationContext, it) }
            }
        }

        override fun onSessionIdReceived(sessionId: String?) { // start tracking
            viewModelScope.launch(Dispatchers.Main) {
                Log.d(TAG, "onSessionIdReceived sessionId: $sessionId")
                _sessionId.value = sessionId
                _asleepState.value = AsleepState.STATE_TRACKING_STARTED
            }
        }

        override fun onSequenceReceived(sequence: Int) {
            viewModelScope.launch(Dispatchers.Main) {
                _sequence.value = sequence
            }
        }

        override fun onCurrentSleepDataReceived(currentSleepData: CurrentSleepData?) {
            viewModelScope.launch(Dispatchers.Main) {
                _currentSleepData.value = currentSleepData
            }
        }

        override fun onErrorCodeReceived(errorCode: ErrorCode?) {
            errorCode ?: return
            viewModelScope.launch(Dispatchers.Main) {
                Log.d(TAG, "onErrorCodeReceived errorCode: $errorCode")
                handleErrorOrWarning(AsleepError(errorCode.code, errorCode.message))
            }
        }

        override fun onStopTrackingReceived(sessionId: String?) {
            viewModelScope.launch(Dispatchers.Main) {
                Log.d(TAG, "onStopTrackingReceived sessionId: $sessionId")
                _sessionId.value = sessionId
                unbindService()

                if (_asleepState.value is AsleepState.STATE_ERROR) {
                    // Exit(Finish) due to Error
                } else {
                    // Successful Finish
                    _shouldGoToReport.value = enoughTrackingTime
                    _asleepState.value = AsleepState.STATE_IDLE
                }
            }
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            asleepService = IAsleepService.Stub.asInterface(service)
            try {
                asleepService?.registerListener(listener)
                isBound = true
            } catch (e: RemoteException) {
                Log.e(TAG, "Failed to register the listener", e)
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            try {
                asleepService?.unregisterListener(listener)
            } catch (e: RemoteException) {
                Log.e(TAG, "Failed to unregister the listener", e)
            }
            isBound = false
            asleepService = null
        }
    }

    fun bindService() {
        if (!isBound) {
            Intent(applicationContext, AsleepService::class.java).also { intent ->
                applicationContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    fun unbindService() {
        if (isBound) {
            try {
                asleepService?.unregisterListener(listener)
            } catch (e: RemoteException) {
                Log.e(TAG, "Failed to unregister the listener", e)
            }
            applicationContext.unbindService(connection)
            isBound = false
            asleepService = null
        }
    }

    /**
     * Called on entry: the session may already be running in the other process.
     */
    fun connectRunningService() {
        _asleepState.value = AsleepState.STATE_TRACKING_STARTED
        bindService()
    }

    fun isTrackingAlive(): Boolean = AsleepService.isAsleepServiceRunning(applicationContext)

    fun clearAsleepError() {
        _asleepErrorCode.value = null
        if (isTrackingAlive()) {
            _asleepState.value = AsleepState.STATE_TRACKING_STARTED
        } else if (_asleepConfig.value != null) {
            _asleepState.value = AsleepState.STATE_INITIALIZED
        } else {
            _asleepState.value = AsleepState.STATE_IDLE
        }
    }

    /* shouldGoToReport is a plain LiveData, so a re-subscribing Activity would receive the old
       `true` again and auto-navigate. The observer consumes it through this before navigating. */
    fun clearShouldGoToReport() {
        _shouldGoToReport.value = false
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

    /**
     * Starts the Foreground Service and binds to it. The session itself is created in the service
     * process; the state machine here advances when `onSessionIdReceived` comes back.
     */
    fun startSleepTracking() {
        if (_asleepState.value == AsleepState.STATE_INITIALIZED) {
            _asleepState.value = AsleepState.STATE_TRACKING_STARTING

            // [Optional] store the start tracking time. Written and read in the UI process only.
            PreferenceHelper.saveStartTrackingTime(applicationContext, System.currentTimeMillis())

            val intent = Intent(applicationContext, AsleepService::class.java).apply {
                action = AsleepService.ACTION_START_TRACKING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(intent)
            } else {
                applicationContext.startService(intent)
            }
            bindService()
        }
    }

    fun stopSleepTracking() {
        if (isTrackingAlive()) {
            _asleepState.value = AsleepState.STATE_TRACKING_STOPPING

            val intent = Intent(applicationContext, AsleepService::class.java).apply {
                action = AsleepService.ACTION_STOP_TRACKING
            }
            applicationContext.startService(intent)
        }
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

    override fun onCleared() {
        super.onCleared()
        unbindService()
    }

    companion object {
        private const val TAG = "AsleepViewModel"
    }
}
