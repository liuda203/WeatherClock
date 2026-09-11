package com.weatheralarm.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.weatheralarm.app.WeatherAlarmApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WeatherCheckReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        scope.launch {
            try {
                val app = context.applicationContext as WeatherAlarmApp
                val settings = app.preferencesRepository.current()
                if (settings.enabled) {
                    WeatherAlarmCoordinator.evaluate(
                        context = context.applicationContext,
                        scheduleRing = true
                    )
                }
            } catch (t: Throwable) {
                Log.e(TAG, "天气检查失败", t)
                // 二次兜底：即便协调器异常，也尽量安排早响铃
                runCatching {
                    val app = context.applicationContext as WeatherAlarmApp
                    val settings = app.preferencesRepository.current()
                    if (settings.enabled) {
                        val label =
                            "异常兜底早响铃 ${"%02d".format(settings.earlyHour)}:${"%02d".format(settings.earlyMinute)}"
                        val triggerAt = AlarmScheduler.scheduleRingAlarm(
                            context.applicationContext,
                            settings.earlyHour,
                            settings.earlyMinute,
                            label
                        )
                        app.preferencesRepository.setWeatherResult(
                            summary = "天气检查异常：${t.message ?: "未知错误"} · $label",
                            isAdverse = true,
                            scheduledAlarmEpochMs = triggerAt,
                            statusLevel = com.weatheralarm.app.data.StatusLevel.ERROR,
                            statusMessage = "天气检查异常，已按恶劣天气时间安排闹钟，避免不响。"
                        )
                    }
                }
            } finally {
                val app = context.applicationContext as WeatherAlarmApp
                val settings = app.preferencesRepository.current()
                AlarmScheduler.scheduleWeatherCheck(context.applicationContext, settings)
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "WeatherCheckReceiver"
    }
}
