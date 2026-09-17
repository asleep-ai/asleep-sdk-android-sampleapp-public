package ai.asleep.asleep_sdk_android_sampleapp.service

import ai.asleep.asleep_sdk_android_sampleapp.IAsleepService
import ai.asleep.asleep_sdk_android_sampleapp.IListener
import ai.asleep.asleep_sdk_android_sampleapp.data.CurrentSleepData
import ai.asleep.asleep_sdk_android_sampleapp.data.ErrorCode
import ai.asleep.asleep_sdk_android_sampleapp.ui.Constants
import ai.asleep.asleep_sdk_android_sampleapp.utils.PreferenceHelper
import ai.asleep.asleep_sdk_android_sampleapp.utils.SampleAsleepLogger
import ai.asleep.asleepsdk.Asleep
import ai.asleep.asleepsdk.AsleepErrorCode
import ai.asleep.asleepsdk.data.AsleepConfig
import ai.asleep.asleepsdk.data.Session
import ai.asleep.asleepsdk.tracking.SleepTrackingManager
import ai.asleep.asleepsdk.tracking.TrackingStatus
import android.app.Application
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Service-side ViewModel. It lives in the `:RecordingService` process and owns everything the SDK
 * needs: `initAsleepConfig()`, the `SleepTrackingManager` and its `TrackingListener`, and the
 * `requestAnalysis()` polling.
 *
 * The UI runs in another process and cannot share this state in memory, so results are pushed back
 * over the [IListener] AIDL callbacks held in a [RemoteCallbackList].
 */
@HiltViewModel
class AsleepViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Application
) : ViewModel() {

    // Step1: Init the Asleep Track SDK
    private var _userId = MutableLiveData<String?>()
    private var _asleepConfig: AsleepConfig? = null

    // Step2~4: SleepTrackingManager
    private var _sleepTrackingManager: SleepTrackingManager? = null
    private var _sessionId: String? = null
    private var _sequence = MutableLiveData<Int?>(null)
    private var _analyzedSeq: Int? = null // The seq that succeeded by receiving a success callback from requestAnalysis()

    var isReporting = false
    val reportingSessionId = MutableLiveData<String>()

    private val listeners = RemoteCallbackList<IListener>()
    val binder: IAsleepService.Stub = object : IAsleepService.Stub() {
        @Throws(RemoteException::class)
        override fun registerListener(listener: IListener?) {
            listeners.register(listener)
        }

        @Throws(RemoteException::class)
        override fun unregisterListener(listener: IListener?) {
            listeners.unregister(listener)
        }
    }

    fun startSleepTracking(userId: String?) {
        Asleep.initAsleepConfig(
            context = applicationContext,
            apiKey = Constants.ASLEEP_API_KEY,
            userId = userId,
            baseUrl = Constants.BASE_URL,
            callbackUrl = Constants.CALLBACK_URL,
            service = Constants.SERVICE_NAME,
            asleepLogger = SampleAsleepLogger,
            asleepConfigListener = object : Asleep.AsleepConfigListener {
                override fun onSuccess(userId: String?, asleepConfig: AsleepConfig?) {
                    saveUserIdInSharedPreference(userId)
                    _userId.value = userId
                    _asleepConfig = asleepConfig

                    notifyListeners(listeners) { listener ->
                        listener.onUserIdReceived(userId)
                    }

                    createSleepTrackingManager()
                    startTracking()
                }

                override fun onFail(errorCode: Int, detail: String) {
                    Log.d("initAsleepConfig", "onFail: $errorCode - $detail")
                    notifyListeners(listeners) { listener ->
                        listener.onErrorCodeReceived(ErrorCode(errorCode, detail))
                    }
                }
            }
        )
    }

    fun createSleepTrackingManager() {
        _sleepTrackingManager = Asleep.createSleepTrackingManager(
            _asleepConfig,
            object : SleepTrackingManager.TrackingListener {
                // onCreate() carries no sessionId - read it from getTrackingStatus().
                override fun onCreate() {
                    _sessionId = getTrackingStatus()?.sessionId

                    notifyListeners(listeners) { listener ->
                        listener.onSessionIdReceived(_sessionId)
                    }
                }

                override fun onUpload(sequence: Int) {
                    _sequence.value = sequence

                    notifyListeners(listeners) { listener ->
                        listener.onSequenceReceived(sequence)
                    }

                    if (sequence > 10 && (sequence % 10 == 1 || sequence - (_analyzedSeq ?: 0) > 10)) {
                        requestAnalysis(sequence)
                    }
                }

                override fun onClose(sessionId: String) {
                    _sessionId = sessionId

                    notifyListeners(listeners) { listener ->
                        listener.onStopTrackingReceived(_sessionId)
                    }

                    if (isReporting) {
                        _sessionId?.let {
                            reportingSessionId.value = it
                        }
                    }
                }

                override fun onFail(errorCode: Int, detail: String) {
                    when (errorCode) {

                        /*
                        * Even if an error occurs during the termination process,
                        * the foreground service (FGS) must be stopped, so the StopTracking message is sent.
                        */
                        AsleepErrorCode.ERR_CLOSE_SERVER_ERROR,
                        AsleepErrorCode.ERR_CLOSE_FAILED,
                        AsleepErrorCode.ERR_CLOSE_FORBIDDEN,
                        AsleepErrorCode.ERR_CLOSE_UNAUTHORIZED,
                        AsleepErrorCode.ERR_CLOSE_BAD_REQUEST,
                        AsleepErrorCode.ERR_CLOSE_NOT_FOUND ->
                            notifyListeners(listeners) { listener ->
                                listener.onStopTrackingReceived(_sessionId)
                            }

                        else ->
                            notifyListeners(listeners) { listener ->
                                listener.onErrorCodeReceived(ErrorCode(errorCode, detail))
                            }
                    }
                }
            }
        )
    }

    fun getTrackingStatus(): TrackingStatus? {
        return _sleepTrackingManager?.getTrackingStatus()
    }

    private fun startTracking() {
        _sleepTrackingManager?.startSleepTracking()
    }

    fun stopSleepTracking() {
        if (_sleepTrackingManager?.getTrackingStatus()?.sessionId != null) {
            _sleepTrackingManager?.stopSleepTracking()
        }
    }

    /*
     * If you want to continue from the previous sleep tracking,
     * You need to reinitialize 'asleepConfig' and 'sleepTrackingManager'
     * and call the startSleepTracking()
     */
    fun continueTracking() {
        if (_asleepConfig == null && Asleep.hasUnfinishedSession(applicationContext)) { // Conditions for continuing
            _asleepConfig = Asleep.getSavedAsleepConfig(applicationContext, Constants.ASLEEP_API_KEY)
            createSleepTrackingManager()
            startTracking()
        }
    }

    /*
     * Interim results are polled through SleepTrackingManager.requestAnalysis().
     * Asleep.getCurrentSleepData() talks to the SDK's own Foreground Service, so it cannot be used
     * in this configuration.
     */
    private fun requestAnalysis(seq: Int) {
        _sleepTrackingManager?.requestAnalysis(
            analysisListener = object : SleepTrackingManager.AnalysisListener {
                override fun onSuccess(session: Session) {
                    _analyzedSeq = seq

                    val currentSleepData = CurrentSleepData(
                        lastSleepStage = session.sleepStages?.lastOrNull(),
                        lastSnoringStage = session.snoringStages?.lastOrNull()
                    )
                    notifyListeners(listeners) { listener ->
                        listener.onCurrentSleepDataReceived(currentSleepData)
                    }
                }

                override fun onFail(errorCode: Int, detail: String) {
                    notifyListeners(listeners) { listener ->
                        listener.onErrorCodeReceived(ErrorCode(errorCode, detail))
                    }
                }
            }
        )
    }

    private fun saveUserIdInSharedPreference(userId: String?) {
        userId?.let {
            PreferenceHelper.putAsleepUserId(applicationContext, it)
        }
    }

    private fun notifyListeners(listeners: RemoteCallbackList<IListener>, onReceive: (IListener) -> Unit) {
        val numListeners = listeners.beginBroadcast()
        for (i in 0 until numListeners) {
            try {
                val listener = listeners.getBroadcastItem(i)
                onReceive(listener)
            } catch (e: RemoteException) {
                Log.e(this::class.java.simpleName, "Failed to notify a listener", e)
            }
        }
        listeners.finishBroadcast()
    }
}
