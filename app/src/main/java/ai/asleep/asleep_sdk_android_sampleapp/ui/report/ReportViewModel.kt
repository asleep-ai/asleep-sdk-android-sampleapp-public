package ai.asleep.asleep_sdk_android_sampleapp.ui.report

import ai.asleep.asleep_sdk_android_sampleapp.ui.Constants
import ai.asleep.asleep_sdk_android_sampleapp.utils.AsleepError
import ai.asleep.asleep_sdk_android_sampleapp.utils.getOneWeekAgoDateString
import ai.asleep.asleep_sdk_android_sampleapp.utils.getTodayString
import ai.asleep.asleepsdk.Asleep
import ai.asleep.asleepsdk.data.AsleepConfig
import ai.asleep.asleepsdk.data.Report
import ai.asleep.asleepsdk.data.SleepSession
import ai.asleep.asleepsdk.tracking.Reports
import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

private const val TAG = "ReportViewModel"

@HiltViewModel
class ReportViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Application
) : ViewModel() {

    // Config and user ID received on the MainActivity
    private var _asleepConfig = MutableLiveData<AsleepConfig?>(null)
    private var _asleepUserId = MutableLiveData<String?>(null)
    val asleepUserId: LiveData<String?> get() = _asleepUserId

    // Step5~7: Report
    private var _reports: Reports? = null
    private var _reportList = MutableLiveData<List<SleepSession>?>(null)
    private var _latestSessionId = MutableLiveData<String?>(null)

    private var _asleepErrorCode = MutableLiveData<AsleepError?>(null)
    val asleepErrorCode: LiveData<AsleepError?> get() = _asleepErrorCode

    // Current Report
    private var currentIndex = -1
    private var _currentReport = MutableLiveData<Report?>()
    val currentReport: LiveData<Report?> get() = _currentReport

    fun clearAsleepError() {
        _asleepErrorCode.value = null
    }

    fun updateLastestSessionId(sessionId: String?) {
        sessionId?.let {
            _latestSessionId.value = it
        } ?: {
            Toast.makeText(applicationContext, "No Session ID", Toast.LENGTH_SHORT).show()
        }
    }

    fun getLatestReport() {
        if (_reports == null) {
            _reports = Asleep.createReports(_asleepConfig.value)
        }

        _latestSessionId.value?.let { sessionId ->
            _reports?.getReport(sessionId, object : Reports.ReportListener {
                override fun onFail(errorCode: Int, detail: String) {
                    _asleepErrorCode.value = AsleepError(errorCode, detail)
                }

                override fun onSuccess(report: Report?) {
                    Log.d(TAG, "onSuccess: $report")
                    _currentReport.value = report
                    getLatestReportInList()
                }
            })
        } ?: run {
            Toast.makeText(applicationContext, "Can't get a latest report", Toast.LENGTH_SHORT).show()
        }
    }

    fun getReport(sessionId: String) {
        _reports?.getReport(sessionId, object : Reports.ReportListener {
            override fun onFail(errorCode: Int, detail: String) {
                _asleepErrorCode.value = AsleepError(errorCode, detail)
            }

            override fun onSuccess(report: Report?) {
                _currentReport.value = report
            }
        })
    }

    fun getLatestReportInList() {
        _reports = Asleep.createReports(_asleepConfig.value)
        _reports?.getReports(
            fromDate = getOneWeekAgoDateString(),
            toDate = getTodayString(),
            reportsListener = object : Reports.ReportsListener {
                override fun onFail(errorCode: Int, detail: String) {
                    _asleepErrorCode.value = AsleepError(errorCode, detail)
                }

                override fun onSuccess(reports: List<SleepSession>?) {
                    Log.d(TAG, "onSuccess: getReportList")
                    _reportList.value = reports
                    _reportList.value?.let { list ->
                        if (list.isNotEmpty()) {
                            currentIndex = 0
                            list[currentIndex].sessionId?.let {
                                getReport(it)
                            } ?: run {
                                Toast.makeText(applicationContext, "No Current Report ID", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        )
    }

    fun initAsleepConfig(userId: String) {
        Asleep.initAsleepConfig(
            context = applicationContext,
            apiKey = Constants.ASLEEP_API_KEY,
            userId = userId,
            baseUrl = Constants.BASE_URL,
            callbackUrl = Constants.CALLBACK_URL,
            service = Constants.SERVICE_NAME,
            asleepConfigListener = object : Asleep.AsleepConfigListener {
                override fun onFail(errorCode: Int, detail: String) {
                    _asleepErrorCode.value = AsleepError(errorCode, detail)
                }

                override fun onSuccess(userId: String?, asleepConfig: AsleepConfig?) {
                    Log.d(TAG, "onSuccess: initConfig")
                    _asleepConfig.value = asleepConfig
                    _asleepUserId.value = userId
                }
            }
        )
    }

    fun getPreviousReport() {
        _reportList.value?.let { list ->
            if (list.isNotEmpty()) {
                currentIndex = if (currentIndex + 1 >= list.lastIndex) list.lastIndex else currentIndex + 1
                updateCurrentReport(list)
            }
        }
    }

    fun getNextReport() {
        _reportList.value?.let { list ->
            if (list.isNotEmpty()) {
                currentIndex = if (currentIndex - 1 < 0) 0 else currentIndex - 1
                updateCurrentReport(list)
            }
        }
    }

    private fun updateCurrentReport(list: List<SleepSession>) {
        val currentSessionId = list[currentIndex].sessionId
        currentSessionId?.let {
            getReport(it)
        } ?: run {
            _asleepErrorCode.value = AsleepError(0, "current session id is null")
        }
    }
}