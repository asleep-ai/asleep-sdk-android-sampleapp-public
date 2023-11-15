package ai.asleep.asleep_sdk_android_sampleapp

import ai.asleep.asleep_sdk_android_sampleapp.databinding.FragmentReportsBinding
import ai.asleep.asleepsdk.Asleep
import ai.asleep.asleepsdk.data.SleepSession
import ai.asleep.asleepsdk.tracking.Reports
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import java.time.LocalDate

class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    private lateinit var callback: OnBackPressedCallback
    private val sharedViewModel: MainViewModel by viewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val fragmentManager = requireActivity().supportFragmentManager
                val transaction = fragmentManager.beginTransaction()
                transaction.replace(R.id.fragment_container_view, HomeFragment())
                transaction.commit()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val reports = Asleep.createReports(sharedViewModel.asleepConfig)
        val today = LocalDate.now()
        reports?.getReports(today.minusDays(7).toString(), today.toString(), "DESC", 0, 20, object : Reports.ReportsListener {
            override fun onSuccess(reports: List<SleepSession>?) {
                Log.d(">>>>> getReports", "onSuccess: ${reports!!.size} sizes - $reports")
                var result = ""
                for (i in reports.indices) {
                    result += "SleepSession[$i]: ${reports[i].sessionId}\n - ${reports[i].sessionStartTime}, ${reports[i].timeInBed}sec. \n\n"
                }
                activity?.runOnUiThread {
                    binding.tvReports.text = result
                }
            }

            override fun onFail(errorCode: Int, detail: String) {
                Log.d(">>>>> getReports", "onFail: $errorCode - $detail")
            }
        })
    }

    override fun onDetach() {
        super.onDetach()
        callback.remove()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}