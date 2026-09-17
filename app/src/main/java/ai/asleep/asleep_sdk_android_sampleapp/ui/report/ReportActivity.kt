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
import ai.asleep.asleepsdk.Asleep
import ai.asleep.asleepsdk.data.Report
import ai.asleep.asleepsdk.recorder.RecordingFile
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class ReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportBinding
    private val reportViewModel: ReportViewModel by viewModels()

    // Same path the tracking side records into (see AsleepViewModel.recordingPath).
    private val recordingPath: String by lazy { File(filesDir, "recordings").absolutePath }
    private var mediaPlayer: MediaPlayer? = null
    private var playingPath: String? = null
    private var playingRow: TextView? = null

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
        showRecordings(report.session?.id)
    }

    /* Lists the recording files kept for the shown session and plays one on tap. Only sessions
       recorded on this device (into this branch's recordingPath) have files; anything else just
       shows the empty message. */
    private fun showRecordings(sessionId: String?) {
        stopPlayback()
        binding.layoutRecordings.removeAllViews()

        val files = sessionId?.let {
            runCatching { Asleep.createRecordingFileManager(recordingPath).getAllSegments(it) }
                .getOrDefault(emptyList())
        }.orEmpty().filter { it.filePath != null }

        binding.tvRecordingsEmpty.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
        files.forEach { binding.layoutRecordings.addView(makeRecordingRow(it)) }
    }

    private fun makeRecordingRow(file: RecordingFile): TextView {
        val label = buildString {
            append("#%03d".format(file.segmentIndex))
            file.timestamp?.takeIf { it.length >= 19 }?.let { append("  ${it.substring(11, 19)}") }
            if (file.isSnoringDetected) append("  snoring(%.1f)".format(file.snoreIntensity))
            if (file.isBreathDetected) append("  breath(%.1f)".format(file.breathSeverity))
            append("  %.1fdB".format(file.maxDb))
        }
        return TextView(this).apply {
            text = label
            tag = label
            textSize = 14f
            setPadding(8, 12, 8, 12)
            setOnClickListener { togglePlayback(file.filePath!!, this) }
        }
    }

    private fun togglePlayback(path: String, row: TextView) {
        if (playingPath == path) {
            stopPlayback()
            return
        }
        stopPlayback()
        runCatching {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                start()
                setOnCompletionListener { stopPlayback() }
            }
            playingPath = path
            playingRow = row
            row.text = getString(R.string.recordings_playing_prefix) + row.tag
        }.onFailure {
            stopPlayback()
            Toast.makeText(this, "Playback failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopPlayback() {
        mediaPlayer?.let { player ->
            runCatching { player.stop() }
            player.release()
        }
        mediaPlayer = null
        playingPath = null
        playingRow?.let { it.text = it.tag as String }
        playingRow = null
    }

    override fun onStop() {
        super.onStop()
        stopPlayback()
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