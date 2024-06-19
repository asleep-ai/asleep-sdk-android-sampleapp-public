package ai.asleep.asleep_sdk_android_sampleapp.ui

import ai.asleep.asleep_sdk_android_sampleapp.R
import ai.asleep.asleep_sdk_android_sampleapp.SampleApplication
import ai.asleep.asleep_sdk_android_sampleapp.databinding.ActivityMainBinding
import ai.asleep.asleep_sdk_android_sampleapp.service.RecordService
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val fragmentManager = supportFragmentManager
    private val fragmentTransaction = fragmentManager.beginTransaction()
    private val asleepViewModel: AsleepViewModel by viewModels()

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

        asleepViewModel.getSingleReport()
    }

    private fun initAsleepConfig() {
        val storedUserId = SampleApplication.sharedPref.getString("user_id", null)
        asleepViewModel.initAsleepConfig(storedUserId)
    }
}