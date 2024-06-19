package ai.asleep.asleep_sdk_android_sampleapp.ui

import ai.asleep.asleep_sdk_android_sampleapp.R
import ai.asleep.asleep_sdk_android_sampleapp.SampleApplication
import ai.asleep.asleep_sdk_android_sampleapp.databinding.FragmentTrackingBinding
import ai.asleep.asleep_sdk_android_sampleapp.service.RecordService
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TrackingFragment : Fragment() {

    private var _binding: FragmentTrackingBinding? = null
    private val binding get() = _binding!!
    private val asleepViewModel: AsleepViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrackingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Display the user id (observe user id)
        asleepViewModel.userId.observe(viewLifecycleOwner) { binding.tvId.text = it }

        // If sequence is called back in onUpload, update sequence on display (observe sequence)
        asleepViewModel.sequence.observe(viewLifecycleOwner) {
            binding.tvSequence.text = getSequenceText(it)
        }

        binding.btnTrackingStop.setOnClickListener {
            moveToHomeScreen()
            stopSleepTracking()
        }

        binding.tvStartTime.text = SampleApplication.sharedPref.getString("start_tracking_time", null)
        binding.tvGuide.text = String.format(resources.getString(R.string.tracking_guidance_message))
    }

    private fun getSequenceText(sequence: Int?): String {
        var text = String.format(resources.getString(R.string.tracking_sequence))
        text += if (sequence == null) {
            "-" + " (0.0 min.)"
        } else {
            "$sequence ${String.format(resources.getString(R.string.tracking_minute_elapsed), (sequence + 1) * 0.5)}"
        }
        return text
    }

    private fun moveToHomeScreen() {
        val fragmentManager = requireActivity().supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container_view, HomeFragment())
        transaction.commit()
    }

    private fun stopSleepTracking() {
        val intent = Intent(requireActivity(), RecordService::class.java)
        intent.action = RecordService.ACTION_STOP_SERVICE
        requireActivity().startService(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}