package com.weatheralarm.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "weather_alarm_settings")

enum class StatusLevel {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}

enum class LocationMode {
    GPS,
    MANUAL_CITY
}

data class AlarmSettings(
    val enabled: Boolean = false,
    val checkHour: Int = 7,
    val checkMinute: Int = 0,
    val earlyHour: Int = 7,
    val earlyMinute: Int = 30,
    val clearHour: Int = 7,
    val clearMinute: Int = 50,
    val forceManualEnabled: Boolean = false,
    val forceHour: Int = 7,
    val forceMinute: Int = 30,
    val locationMode: LocationMode = LocationMode.MANUAL_CITY,
    val cityCode: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationLabel: String = "尚未选择城市",
    val cityQuery: String = "",
    val lastWeatherSummary: String = "尚未检查天气",
    val lastScheduledAlarmEpochMs: Long = 0L,
    val lastIsAdverse: Boolean? = null,
    val lastStatusLevel: StatusLevel = StatusLevel.INFO,
    val lastStatusMessage: String = "请先手动选择中国城市/区县，再打开天气联动闹钟。"
)

class AlarmPreferencesRepository(private val context: Context) {
    private object Keys {
        val enabled = booleanPreferencesKey("enabled")
        val checkHour = intPreferencesKey("check_hour")
        val checkMinute = intPreferencesKey("check_minute")
        val earlyHour = intPreferencesKey("early_hour")
        val earlyMinute = intPreferencesKey("early_minute")
        val rainHour = intPreferencesKey("rain_hour")
        val rainMinute = intPreferencesKey("rain_minute")
        val clearHour = intPreferencesKey("clear_hour")
        val clearMinute = intPreferencesKey("clear_minute")
        val forceManualEnabled = booleanPreferencesKey("force_manual_enabled")
        val forceHour = intPreferencesKey("force_hour")
        val forceMinute = intPreferencesKey("force_minute")
        val locationMode = stringPreferencesKey("location_mode")
        val cityCode = stringPreferencesKey("city_code")
        val latitude = doublePreferencesKey("latitude")
        val longitude = doublePreferencesKey("longitude")
        val locationLabel = stringPreferencesKey("location_label")
        val cityQuery = stringPreferencesKey("city_query")
        val lastWeatherSummary = stringPreferencesKey("last_weather_summary")
        val lastScheduledAlarmEpochMs = longPreferencesKey("last_scheduled_alarm_epoch_ms")
        val lastIsAdverse = stringPreferencesKey("last_is_adverse")
        val lastStatusLevel = stringPreferencesKey("last_status_level")
        val lastStatusMessage = stringPreferencesKey("last_status_message")
    }

    val settingsFlow: Flow<AlarmSettings> = context.dataStore.data.map { prefs ->
        val earlyH = prefs[Keys.earlyHour] ?: prefs[Keys.rainHour] ?: 7
        val earlyM = prefs[Keys.earlyMinute] ?: prefs[Keys.rainMinute] ?: 30
        AlarmSettings(
            enabled = prefs[Keys.enabled] ?: false,
            checkHour = prefs[Keys.checkHour] ?: 7,
            checkMinute = prefs[Keys.checkMinute] ?: 0,
            earlyHour = earlyH,
            earlyMinute = earlyM,
            clearHour = prefs[Keys.clearHour] ?: 7,
            clearMinute = prefs[Keys.clearMinute] ?: 50,
            forceManualEnabled = prefs[Keys.forceManualEnabled] ?: false,
            forceHour = prefs[Keys.forceHour] ?: 7,
            forceMinute = prefs[Keys.forceMinute] ?: 30,
            locationMode = when (prefs[Keys.locationMode]) {
                "GPS" -> LocationMode.GPS
                else -> LocationMode.MANUAL_CITY
            },
            cityCode = prefs[Keys.cityCode],
            latitude = prefs[Keys.latitude],
            longitude = prefs[Keys.longitude],
            locationLabel = prefs[Keys.locationLabel] ?: "尚未选择城市",
            cityQuery = prefs[Keys.cityQuery] ?: "",
            lastWeatherSummary = prefs[Keys.lastWeatherSummary] ?: "尚未检查天气",
            lastScheduledAlarmEpochMs = prefs[Keys.lastScheduledAlarmEpochMs] ?: 0L,
            lastIsAdverse = when (prefs[Keys.lastIsAdverse]) {
                "true" -> true
                "false" -> false
                else -> null
            },
            lastStatusLevel = runCatching {
                StatusLevel.valueOf(prefs[Keys.lastStatusLevel] ?: StatusLevel.INFO.name)
            }.getOrDefault(StatusLevel.INFO),
            lastStatusMessage = prefs[Keys.lastStatusMessage]
                ?: "请先手动选择中国城市/区县，再打开天气联动闹钟。"
        )
    }

    suspend fun current(): AlarmSettings = settingsFlow.first()

    suspend fun setEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.enabled] = enabled }
    }

    suspend fun setCheckTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.checkHour] = hour
            it[Keys.checkMinute] = minute
        }
    }

    suspend fun setEarlyTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.earlyHour] = hour
            it[Keys.earlyMinute] = minute
            it[Keys.rainHour] = hour
            it[Keys.rainMinute] = minute
        }
    }

    suspend fun setClearTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.clearHour] = hour
            it[Keys.clearMinute] = minute
        }
    }

    suspend fun setForceManual(enabled: Boolean) {
        context.dataStore.edit { it[Keys.forceManualEnabled] = enabled }
    }

    suspend fun setForceTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.forceHour] = hour
            it[Keys.forceMinute] = minute
        }
    }

    suspend fun setLocationMode(mode: LocationMode) {
        context.dataStore.edit { it[Keys.locationMode] = mode.name }
    }

    suspend fun setCitySelection(
        cityCode: String,
        label: String,
        cityQuery: String,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        context.dataStore.edit {
            it[Keys.cityCode] = cityCode
            it[Keys.locationLabel] = label
            it[Keys.cityQuery] = cityQuery
            if (latitude != null) it[Keys.latitude] = latitude
            if (longitude != null) it[Keys.longitude] = longitude
        }
    }

    suspend fun setWeatherResult(
        summary: String,
        isAdverse: Boolean?,
        scheduledAlarmEpochMs: Long,
        statusLevel: StatusLevel,
        statusMessage: String
    ) {
        context.dataStore.edit {
            it[Keys.lastWeatherSummary] = summary
            it[Keys.lastScheduledAlarmEpochMs] = scheduledAlarmEpochMs
            it[Keys.lastIsAdverse] = when (isAdverse) {
                true -> "true"
                false -> "false"
                null -> "unknown"
            }
            it[Keys.lastStatusLevel] = statusLevel.name
            it[Keys.lastStatusMessage] = statusMessage
        }
    }

    suspend fun setStatus(level: StatusLevel, message: String) {
        context.dataStore.edit {
            it[Keys.lastStatusLevel] = level.name
            it[Keys.lastStatusMessage] = message
        }
    }
}
