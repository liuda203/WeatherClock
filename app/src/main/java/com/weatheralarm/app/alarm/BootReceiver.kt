package com.weatheralarm.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.weatheralarm.app.WeatherAlarmApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as WeatherAlarmApp
                val settings = app.preferencesRepository.current()
                AlarmScheduler.scheduleWeatherCheck(context.applicationContext, settings)
            } finally {
                pending.finish()
            }
        }
    }
}
