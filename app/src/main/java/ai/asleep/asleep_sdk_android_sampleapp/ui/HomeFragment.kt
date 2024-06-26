package ai.asleep.asleep_sdk_android_sampleapp.ui

import ai.asleep.asleep_sdk_android_sampleapp.R
import ai.asleep.asleep_sdk_android_sampleapp.SampleApplication
import ai.asleep.asleep_sdk_android_sampleapp.databinding.FragmentHomeBinding
import ai.asleep.asleep_sdk_android_sampleapp.service.RecordService
import ai.asleep.asleep_sdk_android_sampleapp.utils.changeTimeFormat
import ai.asleep.asleep_sdk_android_sampleapp.utils.getCurrentTime
import ai.asleep.asleepsdk.data.Report
import ai.asleep.asleepsdk.data.Stat
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val asleepViewModel: AsleepViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Display the user id (observe user id)
        asleepViewModel.userId.observe(viewLifecycleOwner) { binding.tvId.text = it }

        // If getReport() is successful, display the Report (observe Report)
        asleepViewModel.report.observe(viewLifecycleOwner) { report ->
            if (report != null) {
                displayReport(true)
                displayReportText(report)
            } else {
                displayReport(false)
            }
        }

        binding.apply {
            btnTrackingStart.setOnClickListener {
                if (ActivityCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    asleepViewModel.clearSleepTrackingState()

                    // [Optional] store the start tracking time
                    with(SampleApplication.sharedPref.edit()) {
                        putString("start_tracking_time", getCurrentTime())
                        apply()
                    }

                    // start sleep tracking
                    startTrackingService()
                    moveToTrackingScreen()
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.POST_NOTIFICATIONS), 0)
                    } else {
                        ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.RECORD_AUDIO), 0)
                    }
                }
            }
            btnRefreshReport.setOnClickListener { refreshReport() }
            btnIgnoreBatteryOpt.setOnClickListener { ignoreBatteryOptimizations() }
        }
    }

    private fun displayReport(displayable: Boolean) {
        when(displayable) {
            true -> {
                binding.llHomeReport.visibility = View.VISIBLE
                binding.tvNotice.visibility = View.INVISIBLE
                binding.tvSubNotice.visibility = View.INVISIBLE
            }
            false -> {
                binding.llHomeReport.visibility = View.INVISIBLE
                binding.tvNotice.visibility = View.VISIBLE
                binding.tvSubNotice.visibility = View.VISIBLE
            }
        }
    }

    private fun displayReportText(report: Report) {
        val reportText = getReportText(report)
        val statText = if (report.stat != null) getStatText(report.stat!!) else null

        binding.apply {
            tvSessionId.text = report.session?.id
            tvReport.text = reportText
            tvStat.text = statText?: "is null"
            tvSleepStages.text = report.session?.sleepStages.toString()
            tvBreathStages.text = report.session?.breathStages.toString()
            tvSnoringStages.text = report.session?.snoringStages.toString()
        }
    }

    private fun getReportText(report: Report): String {
        return  "Created Timezone : ${report.session?.createdTimezone}\n" +
                "${getString(R.string.report_time_range)} : ${changeTimeFormat(report.session?.startTime)} ~ ${changeTimeFormat(report.session?.endTime)}\n" +
                "Unexpected Timezone : ${report.session?.unexpectedEndTime}\n" +
                "${getString(R.string.report_session_state)} : ${report.session?.state}\n" +
                "Missing Data Ratio : " +report.missingDataRatio + "\n" +
                "Peculiarities : " +report.peculiarities
    }

    private fun getStatText(stat: Stat): String {
        return "SleepLatency: " + stat.sleepLatency + "\n" +
                "WakeLatency: " + stat.wakeupLatency + "\n" +
                "SleepTime: " + stat.sleepTime + "\n" +
                "WakeTime: " + stat.wakeTime + "\n" +
                "TimeInWake: " + stat.timeInWake + "\n" +
                "TimeInSleep: " + stat.timeInSleep + "\n" +
                "TimeInBed: " + stat.timeInBed + "\n" +
                "TimeInSleepPeriod: " + stat.timeInSleepPeriod + "\n" +
                "TimeInWake: " + stat.timeInRem + "\n" +
                "TimeInLight: " + stat.timeInLight + "\n" +
                "TimeInDeep: " + stat.timeInDeep + "\n" +
                "TimeInStableBreath: " + stat.timeInStableBreath + "\n" +
                "TimeInUnstableBreath: " + stat.timeInUnstableBreath + "\n" +
                "SleepEfficiency: " + stat.sleepEfficiency + "\n" +
                "WakeRatio: " + stat.wakeRatio + "\n" +
                "SleepRatio: " + stat.sleepRatio + "\n" +
                "RemRatio: " + stat.remRatio + "\n" +
                "LightRatio: " + stat.lightRatio + "\n" +
                "DeepRatio: " + stat.deepRatio + "\n" +
                "StableBreathRatio: " + stat.stableBreathRatio + "\n" +
                "UnstableBreathRatio: " + stat.unstableBreathRatio + "\n" +
                "BreathingPattern: " + stat.breathingPattern + "\n" +
                "BreathingIndex: " + stat.breathingIndex + "\n" +
                "SleepCycle: " + stat.sleepCycle + "\n" +
                "SleepCycleCount: " + stat.sleepCycleCount + "\n" +
                "WasoCount: " + stat.wasoCount + "\n" +
                "LongestWaso: " + stat.longestWaso + "\n" +
                "UnstableBreathCount: " + stat.unstableBreathCount + "\n" +
                "LightLatency: " + stat.lightLatency + "\n" +
                "RemLatency: " + stat.remLatency + "\n" +
                "DeepLatency: " + stat.deepLatency + "\n" +
                "SleepIndex: " + stat.sleepIndex + "\n" +
                "TimeInSnoring: " + stat.timeInSnoring + "\n" +
                "TimeInNoSnoring: " + stat.timeInNoSnoring + "\n" +
                "snoringRatio: " + stat.snoringRatio + "\n" +
                "noSnoringRatio: " + stat.noSnoringRatio + "\n" +
                "snoringCount: " + stat.snoringRatio + "\n" +
                "sleepCycleTime: " + stat.sleepCycleTime + "\n"
    }

    private fun refreshReport() = asleepViewModel.getSingleReport()

    @SuppressLint("BatteryLife")
    private fun ignoreBatteryOptimizations() {
        val context: Context = requireContext()
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(context.packageName)) {
            Toast.makeText(requireActivity(), "Battery optimization is already being ignored.", Toast.LENGTH_SHORT).show()
        } else {
            val intent = Intent()
            intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intent.data = Uri.parse("package:${context.packageName}")
            startActivity(intent)
        }
    }

    private fun startTrackingService() {
        val intent = Intent(requireActivity(), RecordService::class.java)
        intent.action = RecordService.ACTION_START_OR_RESUME_SERVICE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireActivity().startForegroundService(intent)
        } else {
            requireActivity().startService(intent)
        }
    }

    private fun moveToTrackingScreen() {
        val fragmentManager = requireActivity().supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container_view, TrackingFragment())
        transaction.commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}