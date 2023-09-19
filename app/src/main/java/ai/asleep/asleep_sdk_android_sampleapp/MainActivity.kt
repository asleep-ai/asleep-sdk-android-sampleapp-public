package ai.asleep.asleep_sdk_android_sampleapp

import ai.asleep.asleep_sdk_android_sampleapp.databinding.ActivityMainBinding
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
}