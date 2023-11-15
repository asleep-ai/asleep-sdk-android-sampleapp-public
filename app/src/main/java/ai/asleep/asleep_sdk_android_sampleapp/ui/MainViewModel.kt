package ai.asleep.asleep_sdk_android_sampleapp.ui

import ai.asleep.asleepsdk.Asleep
import ai.asleep.asleepsdk.AsleepErrorCode
import ai.asleep.asleepsdk.data.AsleepConfig
import ai.asleep.asleepsdk.data.Report
import ai.asleep.asleepsdk.tracking.Reports
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

    private var _userId: String? = null
    val userId: String? get() = _userId

    private var _asleepConfig: AsleepConfig? = null
    val asleepConfig: AsleepConfig? get() = _asleepConfig

    var sessionIdLiveData =  MutableLiveData ("")

    private val _reportLiveData = MutableLiveData<Report?>()
    val reportLiveData: LiveData<Report?>
        get() = _reportLiveData

    private val _errorCodeLiveData = MutableLiveData<Int?>()
    val errorCodeLiveData: LiveData<Int?>
        get() = _errorCodeLiveData

    private var _errorDetail: String? = null
    val errorDetail: String?
        get() = _errorDetail

    private var _startTrackingTime: String = ""
    val startTrackingTime: String
        get() = _startTrackingTime

    private val _sequenceLiveData = MutableLiveData<Int?>()
    val sequenceLiveData: LiveData<Int?>
        get() = _sequenceLiveData

    private val _arrayList = MutableLiveData<ArrayList<String>>()
    val arrayList: LiveData<ArrayList<String>>
        get() = _arrayList

    private var _isDeveloperModeOn: Boolean = false
    val isDeveloperModeOn: Boolean
        get() = _isDeveloperModeOn

    private var _developerModeUserId: String? = ""
    val developerModeUserId: String?
        get() = _developerModeUserId

    private var _developerModeAsleepConfig: AsleepConfig? = null
    val developerModeAsleepConfig: AsleepConfig?
        get() = _developerModeAsleepConfig

    init {
        _arrayList.value = ArrayList()
    }

    fun setUserId(userId: String?) { _userId = userId }

    fun setAsleepConfig(asleepConfig: AsleepConfig?) { _asleepConfig = asleepConfig }

    fun getReport() {
        val reports = if (isDeveloperModeOn) {
            Asleep.createReports(_developerModeAsleepConfig)
        } else {
            Asleep.createReports(_asleepConfig)
        }
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

    fun setReport(report: Report?) {
        _reportLiveData.value = report
    }

    fun setErrorData(errorCode: Int?, errorDetail: String?) {
        _errorDetail = errorDetail
        _errorCodeLiveData.postValue(errorCode)

        if (errorCode == AsleepErrorCode.ERR_AUDIO_SILENCED || errorCode == AsleepErrorCode.ERR_AUDIO_UNSILENCED) {
            val tmpList: ArrayList<String> = _arrayList.value!!
            val errorText: String = if (errorCode == AsleepErrorCode.ERR_AUDIO_SILENCED) {
                "ERR_AUDIO_SILENCED"
            } else {
                "ERR_AUDIO_UNSILENCED"
            }
            tmpList.add(getCurrentDateTime() + "\n" + errorText)
            _arrayList.postValue(tmpList)
        }
    }

    fun setSequence(sequence: Int?) {
        _sequenceLiveData.postValue(sequence)
    }

    fun setIsDeveloperModeOn(isDeveloperModeOn: Boolean) {
        _isDeveloperModeOn = isDeveloperModeOn
    }

    fun setDeveloperModeUserId(developerModeUserId: String?) {
        _developerModeUserId = developerModeUserId
    }

    fun setDeveloperModeAsleepConfig(developerModeAsleepConfig: AsleepConfig?) {
        _developerModeAsleepConfig = developerModeAsleepConfig
    }

    fun setStartTrackingTime() {
        val systemZone = ZoneId.systemDefault()
        val zonedDateTime = ZonedDateTime.now(systemZone)
        _startTrackingTime = zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }

    private fun getCurrentDateTime(): String {
        val currentDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        return currentDateTime.format(formatter)
    }
}