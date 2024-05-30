package ai.asleep.asleep_sdk_android_sampleapp.ui

import ai.asleep.asleep_sdk_android_sampleapp.R
import ai.asleep.asleep_sdk_android_sampleapp.SampleApplication
import ai.asleep.asleep_sdk_android_sampleapp.databinding.ActivityMainBinding
import ai.asleep.asleep_sdk_android_sampleapp.service.AsleepService
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels()

    private val fragmentManager = supportFragmentManager
    private val fragmentTransaction = fragmentManager.beginTransaction()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // webview update시 fgs 죽이는 이슈 재현을 위하여
        val webView = WebView(this)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        val isRunningService = AsleepService.isAsleepServiceRunning(context = this@MainActivity)
        Log.d(this.javaClass.name, "isRunningService: $isRunningService")
        if (!isRunningService) {
            fragmentTransaction.replace(R.id.fragment_container_view, HomeFragment())
            fragmentTransaction.commit()
        } else {
            fragmentTransaction.replace(R.id.fragment_container_view, TrackingFragment())
            fragmentTransaction.commit()

            mainViewModel.bindService()
        }

    }

}