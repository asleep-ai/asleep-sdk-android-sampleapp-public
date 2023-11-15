package ai.asleep.asleep_sdk_android_sampleapp

import ai.asleep.asleep_sdk_android_sampleapp.databinding.FragmentHomeBinding
import ai.asleep.asleep_sdk_android_sampleapp.ui.MainViewModel
import ai.asleep.asleep_sdk_android_sampleapp.ui.TrackingFragment
import ai.asleep.asleep_sdk_android_sampleapp.utils.changeTimeFormat
import ai.asleep.asleepsdk.Asleep
import ai.asleep.asleepsdk.data.AsleepConfig
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
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragmentManager = requireActivity().supportFragmentManager
        val transaction = fragmentManager.beginTransaction()

        sharedViewModel.errorCodeLiveData.observe(viewLifecycleOwner) { errorCode ->
            if (errorCode != null) {
                displayReport(false)
                val noticeText = resources.getString(R.string.notice_tracking_terminated)
                val errorText = getErrorText(errorCode)
                binding.tvNotice.text = noticeText
                binding.tvSubNotice.text = errorText
            }
        }
        sharedViewModel.reportLiveData.observe(viewLifecycleOwner) { report ->
            if (report != null) {
                displayReport(true)
                val reportText =
                    "Created Timezone : ${report.session?.createdTimezone}\n" +
                            "${getString(R.string.report_time_range)} : ${changeTimeFormat(report.session?.startTime)} ~ ${changeTimeFormat(report.session?.endTime)}\n" +
                            "Unexpected Timezone : ${report.session?.unexpectedEndTime}\n" +
                            "${getString(R.string.report_session_state)} : ${report.session?.state}\n" +
                            "Missing Data Ratio : " +report.missingDataRatio + "\n" +
                            "Peculiarities : " +report.peculiarities
                val statText = if (report.stat != null) getStatText(report.stat!!) else null
                binding.apply {
                    tvSessionId.text = report.session?.id
                    tvReport.text = reportText
                    tvStat.text = statText?: "is null"
                    tvSleepStages.text = report.session?.sleepStages.toString()
                    tvBreathStages.text = report.session?.breathStages.toString()
                }
            }
            else if(sharedViewModel.errorCodeLiveData.value == null) {
                displayReport(false)
                binding.tvNotice.text = resources.getString(R.string.notice_no_report)
                binding.tvSubNotice.text = ""
            }
        }

        binding.apply {
            btnTrackingStart.setOnClickListener {
                if (ActivityCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    sharedViewModel.setStartTrackingTime()
                    sharedViewModel.setErrorData(null, null)
                    sharedViewModel.setReport(null)
                    sharedViewModel.sessionIdLiveData.value = ""

                    if (sharedViewModel.isDeveloperModeOn) {
                        mockInitAsleepConfig()
                    } else {
                        transaction.replace(R.id.fragment_container_view, TrackingFragment())
                        transaction.commit()
                    }
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
            val idText = if (sharedViewModel.isDeveloperModeOn) {
                "user Id: " + sharedViewModel.developerModeUserId
            } else {
                "user Id: " + sharedViewModel.userId
            }
            tvId.text = idText

            switchDeveloperMode.isChecked = sharedViewModel.isDeveloperModeOn
            switchDeveloperMode.setOnCheckedChangeListener { buttonView, isChecked ->
                Asleep.DeveloperMode.isOn = isChecked
                sharedViewModel.setIsDeveloperModeOn(isChecked)
                val text = if (isChecked) {
                    "user Id: " + sharedViewModel.developerModeUserId
                } else {
                    "user Id: " + sharedViewModel.userId
                }
                tvId.text = text
            }
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

    private fun getErrorText(errorCode: Int?): String {
        return errorCode.toString() + ": " + sharedViewModel.errorDetail
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
                "DeepLatency: " + stat.deepLatency
    }

    private fun refreshReport() {
        val sessionId: String = sharedViewModel.sessionIdLiveData.value ?: ""
        if (sessionId == "") {
            Toast.makeText(requireActivity(), "Refresh error: No session ID!", Toast.LENGTH_SHORT).show()
        } else {
            if(sharedViewModel.userId == null) {
                Toast.makeText(requireActivity(), "Report can't get Report without an user ID.", Toast.LENGTH_SHORT).show()
            } else {
                sharedViewModel.getReport()
            }
        }
    }

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

    private fun mockInitAsleepConfig() {
        Asleep.initAsleepConfig(
            context = requireContext(),
            apiKey = BuildConfig.ASLEEP_API_KEY,
            userId = null,
            baseUrl = null,
            callbackUrl = null,
            service = "SampleAppDeveloperMode",
            object : Asleep.AsleepConfigListener {
                override fun onSuccess(userId: String?, asleepConfig: AsleepConfig?) {
                    sharedViewModel.setDeveloperModeUserId(userId)
                    sharedViewModel.setDeveloperModeAsleepConfig(asleepConfig)
                    val fragmentManager = requireActivity().supportFragmentManager
                    val transaction = fragmentManager.beginTransaction()
                    transaction.replace(R.id.fragment_container_view, TrackingFragment())
                    transaction.commit()
                    Log.d(">>>> AsleepConfigListener", "onSuccess: Developer Id - $userId")
                }
                override fun onFail(errorCode: Int, detail: String) {
                    Log.d(">>>> AsleepConfigListener", "onFail: DeveloperId $errorCode - $detail")
                }
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}