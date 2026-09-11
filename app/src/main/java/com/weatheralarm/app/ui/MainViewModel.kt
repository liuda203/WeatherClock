package com.weatheralarm.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.weatheralarm.app.WeatherAlarmApp
import com.weatheralarm.app.alarm.AlarmScheduler
import com.weatheralarm.app.alarm.WeatherAlarmCoordinator
import com.weatheralarm.app.data.ChinaCityCatalog
import com.weatheralarm.app.data.CityRepository
import com.weatheralarm.app.data.CitySuggestion
import com.weatheralarm.app.data.LocationMode
import com.weatheralarm.app.data.StatusLevel
import com.weatheralarm.app.util.LocationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MainUiState(
    val settings: com.weatheralarm.app.data.AlarmSettings = com.weatheralarm.app.data.AlarmSettings(),
    val message: String = "请先手动选择中国城市/区县，再打开天气联动闹钟。",
    val messageLevel: StatusLevel = StatusLevel.INFO,
    val canExactAlarm: Boolean = true,
    val isBusy: Boolean = false,
    val cityQueryInput: String = "",
    val citySuggestions: List<CitySuggestion> = emptyList(),
    val isSearchingCity: Boolean = false
)

class MainViewModel(
    private val app: WeatherAlarmApp
) : ViewModel() {
    private val prefs = app.preferencesRepository
    private val cityRepository = CityRepository(app)
    private var searchJob: Job? = null

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // 预加载城市表
            runCatching { ChinaCityCatalog.all(app) }
            prefs.settingsFlow.collect { settings ->
                _uiState.update {
                    it.copy(
                        settings = settings,
                        message = settings.lastStatusMessage,
                        messageLevel = settings.lastStatusLevel,
                        canExactAlarm = AlarmScheduler.canScheduleExactAlarms(app),
                        cityQueryInput = if (it.cityQueryInput.isBlank()) {
                            settings.cityQuery
                        } else {
                            it.cityQueryInput
                        }
                    )
                }
            }
        }
    }

    fun setMessage(message: String, level: StatusLevel = StatusLevel.INFO) {
        _uiState.update { it.copy(message = message, messageLevel = level) }
        viewModelScope.launch { prefs.setStatus(level, message) }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && prefs.current().cityCode.isNullOrBlank() && !prefs.current().forceManualEnabled) {
                setMessage("请先选择城市区号，再开启联动闹钟", StatusLevel.ERROR)
                return@launch
            }
            prefs.setEnabled(enabled)
            val settings = prefs.current().copy(enabled = enabled)
            AlarmScheduler.scheduleWeatherCheck(app, settings)
            if (enabled) {
                setMessage(
                    "已开启：将在 ${fmt(settings.checkHour, settings.checkMinute)} 检查天气",
                    StatusLevel.SUCCESS
                )
            } else {
                AlarmScheduler.cancelAll(app)
                setMessage("已关闭天气闹钟", StatusLevel.INFO)
            }
        }
    }

    fun setCheckTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            prefs.setCheckTime(hour, minute)
            rescheduleIfNeeded()
        }
    }

    fun setEarlyTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            prefs.setEarlyTime(hour, minute)
            rescheduleIfNeeded()
        }
    }

    fun setClearTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            prefs.setClearTime(hour, minute)
            rescheduleIfNeeded()
        }
    }

    fun setForceManual(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setForceManual(enabled)
            val settings = prefs.current()
            if (enabled && settings.enabled) {
                WeatherAlarmCoordinator.evaluate(app, scheduleRing = true)
            }
            setMessage(
                if (enabled) {
                    "已开启手动强制响铃：${fmt(settings.forceHour, settings.forceMinute)}（忽略天气）"
                } else {
                    "已关闭手动强制，恢复天气联动"
                },
                if (enabled) StatusLevel.WARNING else StatusLevel.INFO
            )
            rescheduleIfNeeded()
        }
    }

    fun setForceTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            prefs.setForceTime(hour, minute)
            val settings = prefs.current()
            if (settings.forceManualEnabled && settings.enabled) {
                WeatherAlarmCoordinator.evaluate(app, scheduleRing = true)
            }
            setMessage("强制响铃时间已设为 ${fmt(hour, minute)}", StatusLevel.WARNING)
        }
    }

    fun setLocationMode(mode: LocationMode) {
        viewModelScope.launch {
            prefs.setLocationMode(mode)
            setMessage(
                if (mode == LocationMode.MANUAL_CITY) {
                    "已切换为手动选城（中国省市区县区号表）"
                } else {
                    "已切换为 GPS 辅助匹配区号（仍建议手动确认城市）"
                },
                StatusLevel.INFO
            )
        }
    }

    fun onCityQueryChange(query: String) {
        _uiState.update { it.copy(cityQueryInput = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(200)
            val q = query.trim()
            if (q.isEmpty()) {
                _uiState.update { it.copy(citySuggestions = emptyList(), isSearchingCity = false) }
                return@launch
            }
            _uiState.update { it.copy(isSearchingCity = true) }
            try {
                val results = cityRepository.searchCities(q)
                _uiState.update {
                    it.copy(citySuggestions = results, isSearchingCity = false)
                }
                if (results.isEmpty()) {
                    setMessage("未找到匹配的中国城市/区县，请换省市区关键词", StatusLevel.WARNING)
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isSearchingCity = false, citySuggestions = emptyList()) }
                setMessage("城市搜索失败：${t.message ?: "未知错误"}", StatusLevel.ERROR)
            }
        }
    }

    fun selectCity(city: CitySuggestion) {
        viewModelScope.launch {
            prefs.setLocationMode(LocationMode.MANUAL_CITY)
            prefs.setCitySelection(
                cityCode = city.code,
                label = city.displayName,
                cityQuery = city.name
            )
            _uiState.update {
                it.copy(
                    cityQueryInput = city.name,
                    citySuggestions = emptyList()
                )
            }
            setMessage("已选择：${city.displayName}", StatusLevel.SUCCESS)
        }
    }

    fun refreshLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            try {
                prefs.setLocationMode(LocationMode.GPS)
                val location = LocationHelper.resolveLocation(app)
                if (location == null) {
                    setMessage(
                        "定位失败：请打开定位权限，或直接手动选城",
                        StatusLevel.ERROR
                    )
                    return@launch
                }
                val matched = ChinaCityCatalog.resolveFromAddress(
                    context = app,
                    provinceHint = location.province,
                    cityHint = location.city,
                    districtHint = location.district,
                    fallbackLabel = location.label
                )
                if (matched == null) {
                    setMessage(
                        "定位成功（${location.label}），但未匹配到区号表，请手动搜索选择城市",
                        StatusLevel.WARNING
                    )
                } else {
                    prefs.setCitySelection(
                        cityCode = matched.code,
                        label = "GPS匹配：${matched.display}（${matched.code}）",
                        cityQuery = matched.name,
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                    _uiState.update { it.copy(cityQueryInput = matched.name) }
                    setMessage("已匹配区号：${matched.display}（${matched.code}）", StatusLevel.SUCCESS)
                }
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    fun runWeatherCheckNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            try {
                val outcome = WeatherAlarmCoordinator.evaluate(
                    context = app,
                    scheduleRing = true
                )
                _uiState.update {
                    it.copy(
                        message = outcome.statusMessage,
                        messageLevel = outcome.statusLevel
                    )
                }
                val settings = prefs.current()
                if (settings.enabled) {
                    AlarmScheduler.scheduleWeatherCheck(app, settings)
                }
            } catch (t: Throwable) {
                setMessage("执行失败：${t.message ?: "未知错误"}", StatusLevel.ERROR)
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    private suspend fun rescheduleIfNeeded() {
        val settings = prefs.current()
        if (settings.enabled) {
            AlarmScheduler.scheduleWeatherCheck(app, settings)
            setMessage(
                "设置已更新，下次检查时间 ${fmt(settings.checkHour, settings.checkMinute)}",
                StatusLevel.INFO
            )
        }
    }

    private fun fmt(hour: Int, minute: Int): String =
        "%02d:%02d".format(hour, minute)

    companion object {
        fun formatEpoch(epochMs: Long): String {
            if (epochMs <= 0L) return "尚未安排"
            return SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(epochMs))
        }
    }

    class Factory(private val app: WeatherAlarmApp) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(app) as T
        }
    }
}
