package ai.asleep.asleep_sdk_android_sampleapp.ui

import ai.asleep.asleep_sdk_android_sampleapp.BuildConfig
import ai.asleep.asleep_sdk_android_sampleapp.IAsleepService
import ai.asleep.asleep_sdk_android_sampleapp.IListener
import ai.asleep.asleep_sdk_android_sampleapp.SampleApplication
import ai.asleep.asleep_sdk_android_sampleapp.data.ErrorCode
import ai.asleep.asleep_sdk_android_sampleapp.service.AsleepService
import ai.asleep.asleepsdk.Asleep
import ai.asleep.asleepsdk.data.AsleepConfig
import ai.asleep.asleepsdk.data.Report
import ai.asleep.asleepsdk.tracking.Reports
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Application
) : ViewModel() {
    // Step1: Init the Asleep Track SDK
    private var _userId = MutableLiveData<String?>(SampleApplication.sharedPref.getString("user_id", null))
    val userId: LiveData<String?> get() = _userId

    private var _asleepConfig: AsleepConfig? = null

    private var _sessionId = MutableLiveData<String?>(null)
    val sessionId: LiveData<String?> get() = _sessionId

    private var _sequence = MutableLiveData<Int?>(null)
    val sequence: LiveData<Int?> get() = _sequence

    // Step5~7: Report
    private var _reports: Reports? = null
    private var _report = MutableLiveData<Report?>()
    val report: LiveData<Report?> get() = _report

    private var asleepService: IAsleepService? = null
    private var isBound = false

    var isReporting = false

    var isTracking = TrackingState.STATE_TRACKING_STOPPED
    enum class TrackingState { STATE_TRACKING_STOPPED, STATE_TRACKING_STARTING, STATE_TRACKING_STARTED, STATE_TRACKING_STOPPING}

    private val listener = object : IListener.Stub() {
        override fun onUserIdReceived(userId: String) {
            viewModelScope.launch {
                withContext(Dispatchers.Main) {
                    Log.d("IListener", "onUserIdReceived userId: $userId")
                }
                _userId.value = userId
                saveUserIdInSharedPreference(userId)
            }
        }

        override fun onSessionIdReceived(sessionId: String) { // start tracking
            viewModelScope.launch {
                withContext(Dispatchers.Main) {
                    Log.d("IListener", "onSessionIdReceived sessionId: $sessionId")
                }
                _sessionId.value = sessionId
                isTracking = TrackingState.STATE_TRACKING_STARTED
            }
        }

        override fun onSequenceReceived(sequence: Int) {
            viewModelScope.launch {
                withContext(Dispatchers.Main) {
                    Log.d("IListener", "onSequenceReceived sequence: $sequence")
                }
                _sequence.value = sequence
            }
        }

        override fun onErrorCodeReceived(errorCode: ErrorCode) {
            viewModelScope.launch {
                withContext(Dispatchers.Main) {
                    Log.d("IListener", "onErrorCodeReceived errorCode: $errorCode")
                    Toast.makeText(applicationContext, "${errorCode.code} ${errorCode.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onStopTrackingReceived(sessionId: String) {
            viewModelScope.launch {
                withContext(Dispatchers.Main) {
                    Log.d("IListener", "onStopTrackingReceived sessionId: $sessionId")
                }
                _sessionId.value = sessionId
                isTracking = TrackingState.STATE_TRACKING_STOPPED

                Intent(applicationContext, AsleepService::class.java).also { intent ->
                    applicationContext.stopService(intent)
                }

                // request report
                if(!isReporting) {
                    isReporting = true
                    delay(4000)
                    getSingleReport(sessionId)
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
                e.printStackTrace()
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            try {
                asleepService?.unregisterListener(listener)
                isBound = false
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
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
            applicationContext.unbindService(connection)
            isBound = false
        }
    }

    fun getSingleReport(sessionId: String) {
        if (isTracking == TrackingState.STATE_TRACKING_STOPPED) {
            userId.value?.let {
                Asleep.initAsleepConfig(
                    context = applicationContext,
                    apiKey = BuildConfig.ASLEEP_API_KEY,
                    userId = userId.value,
                    baseUrl = null,
                    callbackUrl = null,
                    service = "SampleApp",
                    object : Asleep.AsleepConfigListener {
                        override fun onSuccess(userId: String?, asleepConfig: AsleepConfig?) {
                            _asleepConfig = asleepConfig

                            // create reports
                            _reports = Asleep.createReports(_asleepConfig)

                            // get single report
                            _reports?.getReport(sessionId, object : Reports.ReportListener {
                                override fun onSuccess(report: Report?) {
                                    Log.d("getReport", "onSuccess: $report")
                                    _report.value = report
                                    isReporting = false
                                }

                                override fun onFail(errorCode: Int, detail: String) {
                                    Log.d("getReport", "onFail: $errorCode : $detail")
                                    isReporting = false
                                }
                            })

                        }

                        override fun onFail(errorCode: Int, detail: String) {
                            Log.d("initAsleepConfig", "onFail: $errorCode - $detail")
                            isReporting = false

                        }
                    }
                )
            }
        }
    }
    fun clearSleepTrackingState() {
        _sessionId.value = null
        _sequence.value = null
        _report.value = null
    }

    private fun saveUserIdInSharedPreference(userId: String?) {
        with(SampleApplication.sharedPref.edit()) {
            putString("user_id", userId)
            apply()
        }
    }

    override fun onCleared() {
        super.onCleared()

        unbindService()
    }
}