package com.weatheralarm.app.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.weatheralarm.app.R
import com.weatheralarm.app.WeatherAlarmApp

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val label = intent?.getStringExtra(AlarmScheduler.EXTRA_ALARM_LABEL)
            ?: "天气闹钟到点了"
        val ringIntent = Intent(context, AlarmRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmScheduler.EXTRA_ALARM_LABEL, label)
        }
        context.startActivity(ringIntent)

        val fullScreenPending = PendingIntent.getActivity(
            context,
            2001,
            ringIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, WeatherAlarmApp.CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("天气闹钟")
            .setContentText(label)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPending, true)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)

        context.startForegroundService(
            Intent(context, AlarmSoundService::class.java).apply {
                putExtra(AlarmScheduler.EXTRA_ALARM_LABEL, label)
            }
        )
    }

    companion object {
        const val NOTIFICATION_ID = 42
    }
}
