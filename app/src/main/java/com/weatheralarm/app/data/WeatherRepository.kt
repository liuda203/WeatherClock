package com.weatheralarm.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class WeatherSeverity {
    NORMAL,
    ADVERSE
}

data class WeatherSnapshot(
    val cityCode: String,
    val cityName: String,
    val conditionLabel: String,
    val temperatureC: Double?,
    val humidity: String?,
    val severity: WeatherSeverity,
    val severityReason: String,
    val summary: String
) {
    val isAdverse: Boolean get() = severity == WeatherSeverity.ADVERSE
}

class WeatherRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    suspend fun fetchCurrentWeather(cityCode: String): WeatherSnapshot =
        withContext(Dispatchers.IO) {
            val url = "http://t.weather.sojson.com/api/weather/city/$cityCode"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("天气接口失败：HTTP ${response.code}")
                }
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) error("天气接口返回空数据")
                val root = JSONObject(body)
                val status = root.optInt("status", -1)
                if (status != 200) {
                    error(root.optString("message", "天气接口返回异常"))
                }
                val cityInfo = root.optJSONObject("cityInfo")
                val data = root.getJSONObject("data")
                val forecast = data.getJSONArray("forecast")
                if (forecast.length() == 0) error("天气接口无预报数据")
                val today = forecast.getJSONObject(0)
                val type = today.optString("type").ifBlank { "未知" }
                val notice = today.optString("notice")
                val wendu = data.optString("wendu")
                val shidu = data.optString("shidu").ifBlank { null }
                val temp = wendu.toDoubleOrNull()
                val cityName = cityInfo?.optString("city")?.ifBlank { null }
                    ?: cityInfo?.optString("parent")
                    ?: cityCode

                val (severity, reason) = classifyTravelImpact(type)
                val tempText = temp?.let { String.format("%.1f℃", it) } ?: "${wendu}℃"
                val summary = buildString {
                    append(cityName)
                    append(" · ")
                    append(type)
                    append(" · ")
                    append(tempText)
                    if (!shidu.isNullOrBlank()) {
                        append(" · 湿度 ")
                        append(shidu)
                    }
                    append(" · ")
                    append(reason)
                    if (notice.isNotBlank()) {
                        append(" · ")
                        append(notice)
                    }
                }
                WeatherSnapshot(
                    cityCode = cityCode,
                    cityName = cityName,
                    conditionLabel = type,
                    temperatureC = temp,
                    humidity = shidu,
                    severity = severity,
                    severityReason = reason,
                    summary = summary
                )
            }
        }

    /**
     * 凡影响步行/骑行的天气（雨、雪、雾、霾、沙尘、冰雹等）都判定为早响铃。
     */
    private fun classifyTravelImpact(type: String): Pair<WeatherSeverity, String> {
        val t = type.trim()
        val adverseKeywords = listOf(
            "雨", "雪", "雹", "雾", "霾", "沙", "尘", "扬沙", "浮尘",
            "冰", "冻", "霜", "雷"
        )
        val hit = adverseKeywords.firstOrNull { t.contains(it) }
        return if (hit != null) {
            WeatherSeverity.ADVERSE to "影响出行（$t），走早响铃"
        } else {
            WeatherSeverity.NORMAL to "出行天气尚可（$t），走正常响铃"
        }
    }
}
