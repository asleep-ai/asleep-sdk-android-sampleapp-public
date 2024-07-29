package ai.asleep.asleep_sdk_android_sampleapp.service

import ai.asleep.asleep_sdk_android_sampleapp.BuildConfig
import ai.asleep.asleep_sdk_android_sampleapp.IAsleepService
import ai.asleep.asleep_sdk_android_sampleapp.IListener
import ai.asleep.asleep_sdk_android_sampleapp.data.ErrorCode
import ai.asleep.asleep_sdk_android_sampleapp.utils.PreferenceHelper
import ai.asleep.asleepsdk.Asleep
import ai.asleep.asleepsdk.AsleepErrorCode
import ai.asleep.asleepsdk.data.AsleepConfig
import ai.asleep.asleepsdk.tracking.SleepTrackingManager
import ai.asleep.asleepsdk.tracking.TrackingStatus
import android.app.Application
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject


@HiltViewModel
class AsleepViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Application
) : ViewModel() {
    // Step1: Init the Asleep Track SDK
    private var _userId = MutableLiveData<String?>()
    val userId: LiveData<String?> get() = _userId
    private var _asleepConfig: AsleepConfig? = null

    // Step2~4: SleepTrackingManager
    private var _sleepTrackingManager: SleepTrackingManager? = null
    private var _sessionId: String? = null
    private var _sequence = MutableLiveData<Int?>(null)

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
            apiKey = BuildConfig.ASLEEP_API_KEY,
            userId = userId,
            baseUrl = null,
            callbackUrl = null,
            service = "SampleApp",
            object : Asleep.AsleepConfigListener {
                override fun onSuccess(userId: String?, asleepConfig: AsleepConfig?) {
                    saveUserIdInSharedPreference(userId)
                    _userId.value = userId
                    _asleepConfig = asleepConfig

                    notifyListeners(listeners) { listener ->
                        (listener).onUserIdReceived(userId)
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
            },
            object : Asleep.AsleepLogger {
                override fun d(tag: String, msg: String, throwable: Throwable?) {
                    if (throwable == null) {
                        Log.d(tag, msg)
                    } else {
                        Log.d(tag, "$msg ${throwable.localizedMessage}")
                    }
                }

                override fun e(tag: String, msg: String, throwable: Throwable?) {
                    if (throwable == null) {
                        Log.e(tag, msg)
                    } else {
                        Log.e(tag, "$msg ${throwable.localizedMessage}")
                    }
                }

                override fun i(tag: String, msg: String, throwable: Throwable?) {
                    if (throwable == null) {
                        Log.i(tag, msg)
                    } else {
                        Log.i(tag, "$msg ${throwable.localizedMessage}")
                    }
                }

                override fun w(tag: String, msg: String, throwable: Throwable?) {
                    if (throwable == null) {
                        Log.w(tag, msg)
                    } else {
                        Log.w(tag, "$msg ${throwable.localizedMessage}")
                    }
                }
            })
    }

    fun createSleepTrackingManager() {
        _sleepTrackingManager = Asleep.createSleepTrackingManager(_asleepConfig, object : SleepTrackingManager.TrackingListener {
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
                            (listener).onStopTrackingReceived(_sessionId)
                        }
                    else ->
                        notifyListeners(listeners) { listener ->
                            (listener).onErrorCodeReceived(ErrorCode(errorCode, detail))
                        }
                }
            }
        })
    }

    fun getTrackingStatus(): TrackingStatus? {
        return _sleepTrackingManager?.getTrackingStatus()
    }

    private fun startTracking() {
        _sleepTrackingManager?.startSleepTracking()
    }

    fun stopSleepTracking(context: LifecycleService) {
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
            _asleepConfig = Asleep.getSavedAsleepConfig(applicationContext, BuildConfig.ASLEEP_API_KEY)
            createSleepTrackingManager()
            startTracking()
        }
    }

    private fun saveUserIdInSharedPreference(userId: String?) {
        userId?.let {
            PreferenceHelper.putUserId(applicationContext, it)
        }
    }

    private fun notifyListeners(listeners: RemoteCallbackList<IListener>, onReceive: (IListener) -> Unit) {
        val numListeners = listeners.beginBroadcast()
        for (i in 0 until numListeners) {
            try {
                val listener = listeners.getBroadcastItem(i)
                onReceive(listener)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
        listeners.finishBroadcast()
    }
}