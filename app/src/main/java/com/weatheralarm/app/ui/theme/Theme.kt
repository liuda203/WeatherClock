package com.weatheralarm.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B6E99),
    onPrimary = Color.White,
    secondary = Color(0xFF1F7A8C),
    background = Color(0xFFF4FAFD),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF123044),
    onSurface = Color(0xFF123044)
)

@Composable
fun WeatherAlarmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
