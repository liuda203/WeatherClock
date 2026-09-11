package com.weatheralarm.app.alarm

import android.content.Context
import com.weatheralarm.app.WeatherAlarmApp
import com.weatheralarm.app.data.AlarmSettings
import com.weatheralarm.app.data.ChinaCityCatalog
import com.weatheralarm.app.data.LocationMode
import com.weatheralarm.app.data.StatusLevel
import com.weatheralarm.app.data.WeatherRepository
import com.weatheralarm.app.util.LocationHelper

data class ScheduleOutcome(
    val summary: String,
    val statusMessage: String,
    val statusLevel: StatusLevel,
    val isAdverse: Boolean?,
    val scheduledAlarmEpochMs: Long,
    val ringHour: Int,
    val ringMinute: Int
)

/**
 * 统一的天气检查 + 闹钟决策，供界面预览与后台定时检查共用。
 */
object WeatherAlarmCoordinator {

    suspend fun evaluate(
        context: Context,
        scheduleRing: Boolean
    ): ScheduleOutcome {
        val app = context.applicationContext as WeatherAlarmApp
        val prefs = app.preferencesRepository
        var settings = prefs.current()

        if (settings.forceManualEnabled) {
            val hour = settings.forceHour
            val minute = settings.forceMinute
            val label = "手动强制响铃 ${fmt(hour, minute)}"
            val triggerAt = if (scheduleRing && settings.enabled) {
                AlarmScheduler.scheduleRingAlarm(context, hour, minute, label)
            } else {
                0L
            }
            val summary = "已启用手动强制响铃，忽略天气判定"
            val message = if (scheduleRing && settings.enabled) {
                "已按手动时间安排：$label"
            } else {
                "手动强制模式预览：$label（请打开总开关以真正安排）"
            }
            prefs.setWeatherResult(
                summary = summary,
                isAdverse = null,
                scheduledAlarmEpochMs = triggerAt,
                statusLevel = StatusLevel.WARNING,
                statusMessage = message
            )
            return ScheduleOutcome(
                summary = summary,
                statusMessage = message,
                statusLevel = StatusLevel.WARNING,
                isAdverse = null,
                scheduledAlarmEpochMs = triggerAt,
                ringHour = hour,
                ringMinute = minute
            )
        }

        settings = ensureCityCode(context, settings)
        val cityCode = settings.cityCode
        if (cityCode.isNullOrBlank()) {
            return scheduleFallbackEarly(
                context = context,
                settings = settings,
                scheduleRing = scheduleRing,
                reason = "未选择城市区号：请在「手动选城」中按省市区选择"
            )
        }

        return try {
            val weather = WeatherRepository().fetchCurrentWeather(cityCode)
            val useEarly = weather.isAdverse
            val hour = if (useEarly) settings.earlyHour else settings.clearHour
            val minute = if (useEarly) settings.earlyMinute else settings.clearMinute
            val label = if (useEarly) {
                "影响出行，早响铃 ${fmt(hour, minute)}（${weather.conditionLabel}）"
            } else {
                "出行尚可，响铃 ${fmt(hour, minute)}（${weather.conditionLabel}）"
            }
            val triggerAt = if (scheduleRing && settings.enabled) {
                AlarmScheduler.scheduleRingAlarm(context, hour, minute, label)
            } else {
                0L
            }
            val summary = buildString {
                append(weather.summary)
                append(" · ")
                append(settings.locationLabel)
                if (scheduleRing && settings.enabled) {
                    append(" · 已安排 $label")
                } else {
                    append(" · 仅预览")
                }
            }
            val message = if (scheduleRing && settings.enabled) {
                "天气获取成功，已安排：$label"
            } else {
                "天气获取成功：$label（总开关未开则不会真正响铃）"
            }
            val level = if (useEarly) StatusLevel.WARNING else StatusLevel.SUCCESS
            prefs.setWeatherResult(
                summary = summary,
                isAdverse = useEarly,
                scheduledAlarmEpochMs = triggerAt,
                statusLevel = level,
                statusMessage = message
            )
            ScheduleOutcome(
                summary = summary,
                statusMessage = message,
                statusLevel = level,
                isAdverse = useEarly,
                scheduledAlarmEpochMs = triggerAt,
                ringHour = hour,
                ringMinute = minute
            )
        } catch (t: Throwable) {
            scheduleFallbackEarly(
                context = context,
                settings = settings,
                scheduleRing = scheduleRing,
                reason = "天气获取失败：${t.message ?: "网络异常"}，已按早响铃时间兜底"
            )
        }
    }

    private suspend fun ensureCityCode(
        context: Context,
        settings: AlarmSettings
    ): AlarmSettings {
        val app = context.applicationContext as WeatherAlarmApp
        val prefs = app.preferencesRepository
        if (!settings.cityCode.isNullOrBlank()) return settings
        if (settings.locationMode != LocationMode.GPS) return settings

        val location = LocationHelper.resolveLocation(context) ?: return settings
        val matched = ChinaCityCatalog.resolveFromAddress(
            context = context,
            provinceHint = location.province,
            cityHint = location.city,
            districtHint = location.district,
            fallbackLabel = location.label
        ) ?: return settings

        prefs.setCitySelection(
            cityCode = matched.code,
            label = "GPS匹配：${matched.display}",
            cityQuery = matched.name,
            latitude = location.latitude,
            longitude = location.longitude
        )
        return prefs.current()
    }

    private suspend fun scheduleFallbackEarly(
        context: Context,
        settings: AlarmSettings,
        scheduleRing: Boolean,
        reason: String
    ): ScheduleOutcome {
        val app = context.applicationContext as WeatherAlarmApp
        val hour = settings.earlyHour
        val minute = settings.earlyMinute
        val label = "兜底早响铃 ${fmt(hour, minute)}"
        val triggerAt = if (scheduleRing && settings.enabled) {
            AlarmScheduler.scheduleRingAlarm(context, hour, minute, label)
        } else {
            0L
        }
        val summary = "$reason · $label"
        val message = if (scheduleRing && settings.enabled) {
            "$reason。已安排 $label，避免闹钟缺失。"
        } else {
            "$reason。预览将使用 $label。"
        }
        app.preferencesRepository.setWeatherResult(
            summary = summary,
            isAdverse = true,
            scheduledAlarmEpochMs = triggerAt,
            statusLevel = StatusLevel.ERROR,
            statusMessage = message
        )
        return ScheduleOutcome(
            summary = summary,
            statusMessage = message,
            statusLevel = StatusLevel.ERROR,
            isAdverse = true,
            scheduledAlarmEpochMs = triggerAt,
            ringHour = hour,
            ringMinute = minute
        )
    }

    private fun fmt(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)
}
