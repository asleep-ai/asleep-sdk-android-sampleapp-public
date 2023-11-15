package ai.asleep.asleep_sdk_android_sampleapp

import ai.asleep.asleep_sdk_android_sampleapp.databinding.ActivityMainBinding
import ai.asleep.asleepsdk.Asleep
import ai.asleep.asleepsdk.data.AsleepConfig
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Observer
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val fragmentManager = supportFragmentManager
    private val fragmentTransaction = fragmentManager.beginTransaction()
    private val sharedViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        if(savedInstanceState == null) {
            val isRunningService = RecordService.isRecordServiceRunning(context = this@MainActivity)
            if (!isRunningService) {
                initAsleepConfig()
                fragmentTransaction.replace(R.id.fragment_container_view, HomeFragment())
                fragmentTransaction.commit()
            } else {
                fragmentTransaction.replace(R.id.fragment_container_view, TrackingFragment())
                fragmentTransaction.commit()
            }
        }

        val sessionIdObserver = Observer<String> { sessionId ->
            if (sessionId != "") { sharedViewModel.getReport() }
        }
        sharedViewModel.sessionIdLiveData.observe(this, sessionIdObserver)
    }

    private fun initAsleepConfig() {
        val storedUserId = SampleApplication.sharedPref.getString("user_id", null)

        Asleep.initAsleepConfig(
            context = applicationContext,
            apiKey = BuildConfig.ASLEEP_API_KEY,
            userId = storedUserId,
            baseUrl = null,
            callbackUrl = null,
            service = "SampleApp",
            object : Asleep.AsleepConfigListener {
                override fun onSuccess(userId: String?, asleepConfig: AsleepConfig?) {
                    with (SampleApplication.sharedPref.edit()) {
                        putString("user_id", userId)
                        apply()
                    }
                    sharedViewModel.setUserId(userId)
                    sharedViewModel.setAsleepConfig(asleepConfig)

                    Log.d(">>>> AsleepConfigListener", "onSuccess: userId - $userId")
                }
                override fun onFail(errorCode: Int, detail: String) {
                    Log.d(">>>> AsleepConfigListener", "onFail: $errorCode - $detail")
                }
            })
    }
}