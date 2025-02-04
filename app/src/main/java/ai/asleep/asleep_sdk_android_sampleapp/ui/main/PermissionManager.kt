package ai.asleep.asleep_sdk_android_sampleapp.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class PermissionManager(private val activity: AppCompatActivity) {
    private val context = activity.applicationContext
    private val batteryOptimizationLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            checkAndRequestPermissions()
        }
    private val micPermissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            checkAndRequestPermissions()
        }
    private val notificationPermissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            checkAndRequestPermissions()
        }

    private val _allPermissionsGranted = MutableLiveData(false)
    val allPermissionsGranted: LiveData<Boolean> = _allPermissionsGranted
    private val _batteryOptimized = MutableLiveData(false)
    val batteryOptimized: LiveData<Boolean> = _batteryOptimized
    private val _micPermission = MutableLiveData(false)
    val micPermission: LiveData<Boolean> = _micPermission
    private val _notificationPermission = MutableLiveData(false)
    val notificationPermission: LiveData<Boolean> = _notificationPermission

    fun checkAndRequestPermissions() {
        when {
            !isBatteryOptimizationIgnored() -> {
                val intent = Intent().apply {
                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    data = Uri.parse("package:${context.packageName}")
                }
                batteryOptimizationLauncher.launch(intent)
            }

            context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                if (shouldShowRequestPermissionRationale(activity, android.Manifest.permission.RECORD_AUDIO)) {
                    showPermissionDialog()
                } else {
                    micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                }
            }

            hasNotificationPermission().not() -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    shouldShowRequestPermissionRationale(activity, android.Manifest.permission.POST_NOTIFICATIONS)
                ) {
                    showPermissionDialog()
                } else {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            else -> {
                checkAllPermissions()
            }
        }
    }

    fun checkAllPermissions() {
        _batteryOptimized.value = isBatteryOptimizationIgnored()
        _micPermission.value =
            context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        _notificationPermission.value = hasNotificationPermission()

        _allPermissionsGranted.value =
            (_batteryOptimized.value == true) && (_micPermission.value == true) && (_notificationPermission.value == true)
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val packageName = context.packageName
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    private fun showPermissionDialog() {
        val dialog = PermissionDialogFragment()
        dialog.show(activity.supportFragmentManager, "PermissionDialogFragment")
    }
}