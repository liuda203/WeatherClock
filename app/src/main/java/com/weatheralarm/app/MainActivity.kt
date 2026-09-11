package com.weatheralarm.app

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.weatheralarm.app.data.StatusLevel
import com.weatheralarm.app.ui.MainScreen
import com.weatheralarm.app.ui.MainViewModel
import com.weatheralarm.app.ui.theme.WeatherAlarmTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(application as WeatherAlarmApp)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WeatherAlarmTheme {
                val state by viewModel.uiState.collectAsState()
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { result ->
                    val granted = result.values.any { it }
                    if (granted) {
                        viewModel.refreshLocation()
                    } else {
                        viewModel.setMessage(
                            "未授予定位权限：可改用「手动选城」继续使用",
                            StatusLevel.WARNING
                        )
                    }
                }

                LaunchedEffect(Unit) {
                    ensureExactAlarmPermission()
                    requestRuntimePermissions(permissionLauncher::launch)
                }

                MainScreen(
                    state = state,
                    onToggleEnabled = viewModel::setEnabled,
                    onPickCheckTime = viewModel::setCheckTime,
                    onPickEarlyTime = viewModel::setEarlyTime,
                    onPickClearTime = viewModel::setClearTime,
                    onToggleForceManual = viewModel::setForceManual,
                    onPickForceTime = viewModel::setForceTime,
                    onLocationModeChange = viewModel::setLocationMode,
                    onCityQueryChange = viewModel::onCityQueryChange,
                    onSelectCity = viewModel::selectCity,
                    onRefreshLocation = {
                        requestRuntimePermissions(permissionLauncher::launch)
                        viewModel.refreshLocation()
                    },
                    onTestWeatherNow = viewModel::runWeatherCheckNow,
                    onOpenExactAlarmSettings = { openExactAlarmSettings() },
                    onOpenBatterySettings = { openBatterySettings() }
                )
            }
        }
    }

    private fun requestRuntimePermissions(launcher: (Array<String>) -> Unit) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        val needRequest = permissions.any {
            ContextCompat.checkSelfPermission(this, it) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (needRequest) {
            launcher(permissions.toTypedArray())
        }
    }

    private fun ensureExactAlarmPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val alarmManager = getSystemService(AlarmManager::class.java)
        if (!alarmManager.canScheduleExactAlarms()) {
            viewModel.setMessage(
                "请允许“精确闹钟”权限，否则到点可能不响",
                StatusLevel.WARNING
            )
        }
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        }
    }

    private fun openBatterySettings() {
        startActivity(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        )
    }
}
