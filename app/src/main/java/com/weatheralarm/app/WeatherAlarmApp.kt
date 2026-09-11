package com.weatheralarm.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.weatheralarm.app.data.AlarmPreferencesRepository

class WeatherAlarmApp : Application() {
    lateinit var preferencesRepository: AlarmPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        preferencesRepository = AlarmPreferencesRepository(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALARM,
                getString(R.string.channel_alarm),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "天气闹钟响铃通知"
                setBypassDnd(true)
                enableVibration(true)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_STATUS,
                getString(R.string.channel_status),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "天气检查与闹钟调度状态"
            }
        )
    }

    companion object {
        const val CHANNEL_ALARM = "alarm_channel"
        const val CHANNEL_STATUS = "status_channel"
    }
}
