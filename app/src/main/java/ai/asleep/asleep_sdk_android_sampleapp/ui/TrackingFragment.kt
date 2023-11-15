package ai.asleep.asleep_sdk_android_sampleapp.ui

import ai.asleep.asleep_sdk_android_sampleapp.HomeFragment
import ai.asleep.asleep_sdk_android_sampleapp.R
import ai.asleep.asleep_sdk_android_sampleapp.service.RecordService
import ai.asleep.asleep_sdk_android_sampleapp.databinding.FragmentTrackingBinding
import ai.asleep.asleepsdk.AsleepErrorCode
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TrackingFragment : Fragment() {

    private var _binding: FragmentTrackingBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrackingBinding.inflate(inflater, container, false)
        val view = binding.root

        val intent = Intent(requireActivity(), RecordService::class.java)
        requireActivity().startForegroundService(intent)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.errorCodeLiveData.observe(viewLifecycleOwner) { errorCode ->
            if (errorCode != null) {
                Toast.makeText(requireActivity(), ">>>>>> Error : $errorCode", Toast.LENGTH_SHORT).show()
                if (errorCode == AsleepErrorCode.ERR_UPLOAD_FORBIDDEN || errorCode == AsleepErrorCode.ERR_UPLOAD_NOT_FOUND) {
                    moveToHomeScreen()
                    stopSleepTracking()
                }
            }
        }
        sharedViewModel.sequenceLiveData.observe(viewLifecycleOwner) { seq ->
            var text = String.format(resources.getString(R.string.tracking_sequence))
            text += if (seq == null) {
                "-" + " (0.0 min.)"
            } else {
                if (sharedViewModel.isDeveloperModeOn) {
                    "$seq ${String.format(resources.getString(R.string.tracking_minute_elapsed), (seq + 1) * 0.5 * 10)}"
                } else {
                    "$seq ${String.format(resources.getString(R.string.tracking_minute_elapsed), (seq + 1) * 0.5)}"
                }
            }
            binding.tvSequence.text = text
        }

        sharedViewModel.arrayList.observe(viewLifecycleOwner) { list ->
            val last: String? = if (list.isNotEmpty()) list.last() else null
            val secondLast: String? = if (list.size >= 2) list[list.size - 2] else null

            if (secondLast != null) { binding.tvErr1.text = secondLast }
            if (last != null) { binding.tvErr2.text = last }
        }

        binding.btnTrackingStop.setOnClickListener {
            moveToHomeScreen()
            stopSleepTracking()
        }

        val startTimeText = "${String.format(resources.getString(R.string.tracking_start_time))} : ${sharedViewModel.startTrackingTime}"
        binding.tvStartTime.text = startTimeText

        val idText = if (sharedViewModel.isDeveloperModeOn) {
            "user Id: " + sharedViewModel.developerModeUserId
        } else {
            "user Id: " + sharedViewModel.userId
        }
        binding.tvId.text = idText

        binding.switchDeveloperMode.apply {
            isChecked = sharedViewModel.isDeveloperModeOn
            isClickable = false
        }

        binding.tvGuide.text = if (sharedViewModel.isDeveloperModeOn) {
            String.format(resources.getString(R.string.developer_mode_tracking_guidance_message))
        } else {
            String.format(resources.getString(R.string.tracking_guidance_message))
        }
    }

    private fun moveToHomeScreen() {
        val fragmentManager = requireActivity().supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container_view, HomeFragment())
        transaction.commit()
    }

    private fun stopSleepTracking() {
        val intent = Intent(requireActivity(), RecordService::class.java)
        requireActivity().stopService(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}