package ai.asleep.asleep_sdk_android_sampleapp.ui

import ai.asleep.asleep_sdk_android_sampleapp.R
import ai.asleep.asleep_sdk_android_sampleapp.databinding.ActivityMainBinding
import ai.asleep.asleep_sdk_android_sampleapp.service.AsleepService
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.webkit.WebView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Reproduce the issue where the foreground service is killed during WebView update
        val webView = WebView(this)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        val reportingSessionId = intent.getStringExtra("reportingSessionId")
        if (!reportingSessionId.isNullOrEmpty()) {
            keepScreenOn()
            val fragmentManager = supportFragmentManager
            val fragmentTransaction = fragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.fragment_container_view, HomeFragment())
            fragmentTransaction.commit()

            mainViewModel.getSingleReport(reportingSessionId)
        } else {
            val isRunningService = AsleepService.isAsleepServiceRunning(applicationContext)
            Log.d(this.javaClass.name, "isRunningService: $isRunningService")
            if (isRunningService) {
                mainViewModel.changeTrackingState(MainViewModel.TrackingState.STATE_TRACKING_STARTED)
                mainViewModel.bindService()
            }
        }
        mainViewModel.trackingState.observe(this) {
            Log.d(this.javaClass.simpleName, "trackingState: ${it.name}")
            val fragmentManager = supportFragmentManager
            val fragmentTransaction = fragmentManager.beginTransaction()
            when (it) {
                MainViewModel.TrackingState.STATE_TRACKING_STARTED, MainViewModel.TrackingState.STATE_TRACKING_STARTING -> {
                    fragmentTransaction.replace(R.id.fragment_container_view, TrackingFragment())
                    fragmentTransaction.commit()
                }

                MainViewModel.TrackingState.STATE_TRACKING_STOPPED, MainViewModel.TrackingState.STATE_TRACKING_STOPPING -> {
                    fragmentTransaction.replace(R.id.fragment_container_view, HomeFragment())
                    fragmentTransaction.commit()
                }

                else -> {}
            }
        }

        // permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), 0)
        }
    }

    private fun keepScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true)
            setShowWhenLocked(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun releaseScreenOn() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseScreenOn()
    }
}