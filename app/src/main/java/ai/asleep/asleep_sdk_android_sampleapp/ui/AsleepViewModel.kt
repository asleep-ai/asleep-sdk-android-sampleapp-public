package ai.asleep.asleep_sdk_android_sampleapp.ui

import ai.asleep.asleep_sdk_android_sampleapp.BuildConfig
import ai.asleep.asleep_sdk_android_sampleapp.SampleApplication
import ai.asleep.asleepsdk.Asleep
import ai.asleep.asleepsdk.data.AsleepConfig
import ai.asleep.asleepsdk.data.Report
import ai.asleep.asleepsdk.tracking.Reports
import ai.asleep.asleepsdk.tracking.SleepTrackingManager
import ai.asleep.asleepsdk.tracking.TrackingStatus
import android.app.Application
import android.util.Log
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
    val sequence: LiveData<Int?> get() = _sequence

    // Step5~7: Report
    private var _reports: Reports? = null
    private var _report = MutableLiveData<Report?>()
    val report: LiveData<Report?> get() = _report

    fun initAsleepConfig(userId: String?) {
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
                }
                override fun onFail(errorCode: Int, detail: String) {
                    Log.d(">>>> AsleepConfigListener", "onFail: $errorCode - $detail")
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
            }

            override fun onUpload(sequence: Int) {
                _sequence.value = sequence
            }

            override fun onClose(sessionId: String) {
                _sessionId = sessionId
                getSingleReport() // [Optional] display the report immediately after the tracking ends
            }

            override fun onFail(errorCode: Int, detail: String) {}
        })
    }

    fun getTrackingStatus(): TrackingStatus? {
        return _sleepTrackingManager?.getTrackingStatus()
    }

    fun startSleepTracking() {
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
            _asleepConfig = Asleep.getSavedAsleepConfig(applicationContext, BuildConfig.ASLEEP_API_KEY)
            createSleepTrackingManager()
            startSleepTracking()
        }
    }

    fun createReports() {
        _reports = Asleep.createReports(_asleepConfig)
    }

    fun getSingleReport() {
        createReports()
        _sessionId?.let { sessionId ->
            _reports?.getReport(sessionId, object : Reports.ReportListener {
                override fun onSuccess(report: Report?) {
                    Log.d("getReport", "onSuccess: $report")
                    _report.value = report
                }

                override fun onFail(errorCode: Int, detail: String) {
                    Log.d("getReport", "onFail: $errorCode : $detail")
                }
            })
        }
    }

    // [Optional] This was added because it's unnatural if previous tracking data remained after start tracking
    fun clearSleepTrackingState() {
        _sessionId = ""
        _sequence.value = null
        _report.value = null
    }

    private fun saveUserIdInSharedPreference(userId: String?) {
        with(SampleApplication.sharedPref.edit()) {
            putString("user_id", userId)
            apply()
        }
        Log.d(">>>> AsleepConfigListener", "onSuccess: userId - $userId")
    }
}