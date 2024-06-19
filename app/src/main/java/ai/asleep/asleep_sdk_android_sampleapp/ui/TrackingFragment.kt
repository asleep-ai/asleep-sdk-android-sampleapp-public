package ai.asleep.asleep_sdk_android_sampleapp.ui

import ai.asleep.asleep_sdk_android_sampleapp.R
import ai.asleep.asleep_sdk_android_sampleapp.SampleApplication
import ai.asleep.asleep_sdk_android_sampleapp.databinding.FragmentTrackingBinding
import ai.asleep.asleep_sdk_android_sampleapp.service.AsleepService
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TrackingFragment : Fragment() {

    private val TAG = this.javaClass.name

    private var _binding: FragmentTrackingBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModels()



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrackingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Display the user id
        mainViewModel.userId.observe(viewLifecycleOwner) { userId ->
            binding.tvId.text = userId
        }

        // If sequence is called back in onUpload, update sequence on display (observe sequence)
        mainViewModel.sequence.observe(viewLifecycleOwner) {
            binding.tvSequence.text = getSequenceText(it)
        }

        binding.btnTrackingStop.setOnClickListener {
            if (mainViewModel.isTracking == MainViewModel.TrackingState.STATE_TRACKING_STARTED) {
                mainViewModel.isTracking = MainViewModel.TrackingState.STATE_TRACKING_STOPPING
                moveToHomeScreen()
                stopSleepTracking()
            } else {
                Log.d(TAG, "Start tracking has not been initiated yet.")
            }
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
        mainViewModel.unbindService()
        val intent = Intent(requireActivity(), AsleepService::class.java)
        intent.action = AsleepService.ACTION_STOP_TRACKING
        requireActivity().startService(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}