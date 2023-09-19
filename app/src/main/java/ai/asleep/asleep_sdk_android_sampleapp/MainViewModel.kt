package ai.asleep.asleep_sdk_android_sampleapp

import ai.asleep.asleepsdk.Asleep
import ai.asleep.asleepsdk.data.Report
import ai.asleep.asleepsdk.tracking.Reports
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

    var sessionIdLiveData =  MutableLiveData ("")

    private val _reportLiveData = MutableLiveData<Report?>()
    val reportLiveData: LiveData<Report?>
        get() = _reportLiveData

    private val _errorCodeLiveData = MutableLiveData<Int>()
    val errorCodeLiveData: LiveData<Int>
        get() = _errorCodeLiveData

    private var _errorDetail: String = ""
    val errorDetail: String
        get() = _errorDetail

    private var _startTrackingTime: String = ""
    val startTrackingTime: String
        get() = _startTrackingTime

    private val _sequenceLiveData = MutableLiveData<Int?>()
    val sequenceLiveData: LiveData<Int?>
        get() = _sequenceLiveData

    fun getReport() {
        val asleepConfig = SampleApplication.asleepConfig
        val reports = Asleep.createReports(asleepConfig)
        reports?.getReport(sessionIdLiveData.value!!, object : Reports.ReportListener {
            override fun onSuccess(report: Report?) {
                Log.d(">>>>> getReport", "onSuccess: $report")
                _reportLiveData.postValue(report)
            }

            override fun onFail(errorCode: Int, detail: String) {
                Log.d(">>>>> getReport", "onFail: $errorCode - $detail")
                _errorDetail = detail
                _errorCodeLiveData.postValue(errorCode)
            }
        })
    }

    fun setErrorData(errorCode: Int, errorDetail: String) {
        _errorDetail = errorDetail
        _errorCodeLiveData.postValue(errorCode)
    }

    fun setSequence(sequence: Int?) {
        _sequenceLiveData.postValue(sequence)
    }

    fun setStartTrackingTime() {
        val systemZone = ZoneId.systemDefault()
        val zonedDateTime = ZonedDateTime.now(systemZone)
        _startTrackingTime = zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }
}