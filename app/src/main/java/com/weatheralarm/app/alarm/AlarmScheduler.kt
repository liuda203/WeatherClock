package com.weatheralarm.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.weatheralarm.app.data.AlarmSettings
import java.util.Calendar

object AlarmScheduler {
    const val ACTION_WEATHER_CHECK = "com.weatheralarm.app.action.WEATHER_CHECK"
    const val ACTION_RING = "com.weatheralarm.app.action.RING"
    const val EXTRA_ALARM_LABEL = "extra_alarm_label"

    private const val REQUEST_WEATHER_CHECK = 1001
    private const val REQUEST_RING = 1002

    fun scheduleWeatherCheck(context: Context, settings: AlarmSettings) {
        if (!settings.enabled) {
            cancelAll(context)
            return
        }
        val triggerAt = nextTriggerMillis(settings.checkHour, settings.checkMinute)
        val intent = Intent(context, WeatherCheckReceiver::class.java).apply {
            action = ACTION_WEATHER_CHECK
        }
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_WEATHER_CHECK,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactAlarm(context, triggerAt, pending)
    }

    fun scheduleRingAlarm(context: Context, hour: Int, minute: Int, label: String): Long {
        val triggerAt = nextTriggerMillis(hour, minute)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_RING
            putExtra(EXTRA_ALARM_LABEL, label)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_RING,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactAlarm(context, triggerAt, pending)
        return triggerAt
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val weatherIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_WEATHER_CHECK,
            Intent(context, WeatherCheckReceiver::class.java).apply { action = ACTION_WEATHER_CHECK },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val ringIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_RING,
            Intent(context, AlarmReceiver::class.java).apply { action = ACTION_RING },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(weatherIntent)
        alarmManager.cancel(ringIntent)
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun setExactAlarm(context: Context, triggerAt: Long, pending: PendingIntent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pending
            )
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return calendar.timeInMillis
    }
}
