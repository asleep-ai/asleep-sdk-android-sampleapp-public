package ai.asleep.asleep_sdk_android_sampleapp.ui.report

import ai.asleep.asleep_sdk_android_sampleapp.R
import ai.asleep.asleep_sdk_android_sampleapp.databinding.ActivityReportBinding
import ai.asleep.asleep_sdk_android_sampleapp.ui.Constants
import ai.asleep.asleep_sdk_android_sampleapp.ui.Constants.EXTRA_ASLEEP_USER_ID
import ai.asleep.asleep_sdk_android_sampleapp.ui.Constants.EXTRA_FROM_STATE
import ai.asleep.asleep_sdk_android_sampleapp.ui.Constants.EXTRA_SESSION_ID
import ai.asleep.asleep_sdk_android_sampleapp.utils.changeTimeFormat
import ai.asleep.asleep_sdk_android_sampleapp.utils.getDateOnly
import ai.asleep.asleep_sdk_android_sampleapp.utils.getTimeOnly
import ai.asleep.asleep_sdk_android_sampleapp.utils.showErrorDialog
import ai.asleep.asleepsdk.data.Report
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportBinding
    private val reportViewModel: ReportViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val latestSessionId = intent.getStringExtra(EXTRA_SESSION_ID)
        latestSessionId?.let { reportViewModel.updateLastestSessionId(it) }
        val userId = intent.getStringExtra(EXTRA_ASLEEP_USER_ID)
        initAsleepConfig(userId)

        val fromActivityName = intent.getStringExtra(EXTRA_FROM_STATE)
        reportViewModel.asleepUserId.observe(this) { asleepUserId ->
            asleepUserId?.let {
                if (fromActivityName.equals(Constants.StateName.INIT.name)) {
                    reportViewModel.getLatestReportInList()
                } else if (fromActivityName.equals(Constants.StateName.TRACKING.name)) {
                    reportViewModel.getLatestReport()
                }
            }
        }

        reportViewModel.currentReport.observe(this) { currentReport ->
            currentReport?.let {
                showCurrentReport(currentReport)
            }
        }

        reportViewModel.isLoading.observe(this) { isLoading ->
            binding.progressLoading.visibility =
                if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
        }

        reportViewModel.asleepErrorCode.observe(this) { errorCode ->
            errorCode?.let { showErrorDialog(supportFragmentManager) }
        }

        binding.btnPrev.setOnClickListener { reportViewModel.showOlderReport() }
        binding.btnNext.setOnClickListener { reportViewModel.showNewerReport() }
        binding.btnClose.setOnClickListener { finish() }
    }

    private fun initAsleepConfig(asleepUserId: String?) {
        asleepUserId?.let {
            reportViewModel.initAsleepConfig(asleepUserId)
        } ?: run {
            Toast.makeText(applicationContext, "User id NULL", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showCurrentReport(report: Report) {
        setCurrentReportDate(report)

        val reportText = getReportText(report)
        binding.tvSessionId.text = report.session?.id
        binding.tvReport.text = reportText

        sleepStageItem(report)
        snoringStageItem(report)
    }

    private fun setCurrentReportDate(report: Report) {
        report.session?.let { session ->
            session.endTime?.let { endTime ->
                binding.tvSessionEndDate.text = getDateOnly(endTime)
            }
        }
    }

    /* Time Range is the analysed range, Measured is the range actually recorded on the device.
       They differ when a session is closed with an explicit measurement window. */
    private fun getReportText(report: Report): String {
        val notAvailable = getString(R.string.report_value_not_available)
        val measuredStart = changeTimeFormat(report.session?.measurementStartTime) ?: notAvailable
        val measuredEnd = changeTimeFormat(report.session?.measurementEndTime) ?: notAvailable
        return  "Time Range : ${changeTimeFormat(report.session?.startTime)} ~ ${changeTimeFormat(report.session?.endTime)}\n" +
                "Measured : $measuredStart ~ $measuredEnd\n" +
                "Unexpected End Time : ${changeTimeFormat(report.session?.unexpectedEndTime)}\n" +
                "Session State : ${report.session?.state}\n" +
                "Missing Data Ratio : ${String.format(java.util.Locale.US, "%.1f%%", report.missingDataRatio * 100)}\n" +
                "Peculiarities : ${report.peculiarities}"
    }

    private fun sleepStageItem(report: Report) {
        report.session?.let { session ->
            val stages = session.sleepStages
            val awakeSlice = makeSlice(stages, 0, mainColor = ContextCompat.getColor(this, R.color.sleep_stage_awake), otherColor = ContextCompat.getColor(this, R.color.transparent))
            val remSlice = makeSlice(stages, 3, mainColor = ContextCompat.getColor(this, R.color.sleep_stage_rem), otherColor = ContextCompat.getColor(this, R.color.transparent))
            val lightSlice = makeSlice(stages, 1, mainColor = ContextCompat.getColor(this, R.color.sleep_stage_light), otherColor = ContextCompat.getColor(this, R.color.transparent))
            val deepSlice = makeSlice(stages, 2, mainColor = ContextCompat.getColor(this, R.color.sleep_stage_deep), otherColor = ContextCompat.getColor(this, R.color.transparent))

            binding.viewSleepStages.apply {
                setStackedBarData(0, awakeSlice)
                setStackedBarData(1, remSlice)
                setStackedBarData(2, lightSlice)
                setStackedBarData(3, deepSlice)

                setOnStartWidthListener(0) {}
                setOnEndWidthListener(3) {}

                setStartTime(getTimeOnly(session.startTime))
                setEndTime(session.endTime?.let { getTimeOnly(it) } ?: "end time is null")
            }
        }
    }

    private fun snoringStageItem(report: Report) {
        report.session?.let { session ->
            val snoringStages = session.snoringStages
            val snoringValue = 1
            val snoringSlices = makeSlice(
                stages = snoringStages,
                targetValue = snoringValue,
                mainColor = ContextCompat.getColor(this, R.color.snoring_stage_snoring),
                otherColor = ContextCompat.getColor(this, R.color.snoring_stage_not_snoring)
            )
            binding.viewSnoringStages.slices = snoringSlices

            binding.tvSnoringStages.text = report.stat?.let {
                "${getString(R.string.report_label_snoring_ratio)} ${String.format(java.util.Locale.US, "%.1f%%", (it.snoringRatio ?: 0.0f) * 100)}"
            } ?: getString(R.string.report_msg_snoring_ratio_cannot_checked)
        }
    }
}