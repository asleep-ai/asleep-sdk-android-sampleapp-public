package ai.asleep.asleep_sdk_android_sampleapp

import ai.asleep.asleep_sdk_android_sampleapp.databinding.FragmentTrackingBinding
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TrackingFragment : Fragment() {

    private var _binding: FragmentTrackingBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: MainViewModel by viewModels()

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
            Toast.makeText(requireActivity(), ">>>>>> Error : $errorCode", Toast.LENGTH_SHORT).show()
            moveToHomeScreen()
            stopSleepTracking()
        }
        sharedViewModel.sequenceLiveData.observe(viewLifecycleOwner) { seq ->
            var text = String.format(resources.getString(R.string.tracking_sequence))
            if (seq == null) {
                text += "-" + " (0.0 min.)"
            } else {
                text += "$seq ${String.format(resources.getString(R.string.tracking_minute_elapsed), (seq + 1) *0.5)}"
            }
            binding.tvSequence.text = text
        }

        binding.btnTrackingStop.setOnClickListener {
            moveToHomeScreen()
            stopSleepTracking()
        }

        val startTimeText = "${String.format(resources.getString(R.string.tracking_start_time))} : ${sharedViewModel.startTrackingTime}"
        binding.tvStartTime.text = startTimeText

        val idText = "user Id: " + SampleApplication.userId
        binding.tvId.text = idText
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