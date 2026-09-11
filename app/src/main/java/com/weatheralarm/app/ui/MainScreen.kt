package com.weatheralarm.app.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weatheralarm.app.data.CitySuggestion
import com.weatheralarm.app.data.LocationMode
import com.weatheralarm.app.data.StatusLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: MainUiState,
    onToggleEnabled: (Boolean) -> Unit,
    onPickCheckTime: (Int, Int) -> Unit,
    onPickEarlyTime: (Int, Int) -> Unit,
    onPickClearTime: (Int, Int) -> Unit,
    onToggleForceManual: (Boolean) -> Unit,
    onPickForceTime: (Int, Int) -> Unit,
    onLocationModeChange: (LocationMode) -> Unit,
    onCityQueryChange: (String) -> Unit,
    onSelectCity: (CitySuggestion) -> Unit,
    onRefreshLocation: () -> Unit,
    onTestWeatherNow: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    val settings = state.settings
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("天气闹钟", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0B6E99),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFE8F6FF), Color(0xFFF4FAFD), Color.White)
                    )
                )
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusBanner(level = state.messageLevel, message = state.message)

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("启用天气联动闹钟", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "到检查时间自动查天气，再决定响铃时刻",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = settings.enabled,
                        onCheckedChange = onToggleEnabled
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("手动强制响铃时间", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "开启后忽略天气，固定按设定时间响铃",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = settings.forceManualEnabled,
                        onCheckedChange = onToggleForceManual
                    )
                }
            }

            if (settings.forceManualEnabled) {
                TimeRow(
                    title = "强制响铃时间",
                    subtitle = "手动模式生效中",
                    hour = settings.forceHour,
                    minute = settings.forceMinute,
                    onPick = onPickForceTime
                )
            }

            InfoCard(
                icon = { Icon(Icons.Outlined.LocationOn, contentDescription = null) },
                title = "当前位置",
                body = settings.locationLabel
            )
            InfoCard(
                icon = { Icon(Icons.Outlined.Cloud, contentDescription = null) },
                title = "最近天气结果",
                body = settings.lastWeatherSummary,
                emphasize = settings.lastStatusLevel == StatusLevel.ERROR ||
                    settings.lastStatusLevel == StatusLevel.WARNING
            )
            InfoCard(
                icon = { Icon(Icons.Outlined.NotificationsActive, contentDescription = null) },
                title = "已安排响铃",
                body = MainViewModel.formatEpoch(settings.lastScheduledAlarmEpochMs)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("城市区号（中国天气网）", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "请按省市区县选择，例如：江苏 · 南通 · 通州区",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.locationMode == LocationMode.MANUAL_CITY,
                            onClick = { onLocationModeChange(LocationMode.MANUAL_CITY) },
                            label = { Text("手动选城") }
                        )
                        FilterChip(
                            selected = settings.locationMode == LocationMode.GPS,
                            onClick = { onLocationModeChange(LocationMode.GPS) },
                            label = { Text("GPS 辅助") }
                        )
                    }
                    OutlinedTextField(
                        value = state.cityQueryInput,
                        onValueChange = onCityQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("搜索省/市/区县（如：通州区、南通、上海）") },
                        supportingText = {
                            Text(
                                when {
                                    state.isSearchingCity -> "本地区号表搜索中..."
                                    settings.cityCode != null -> "当前区号：${settings.cityCode}"
                                    else -> "结果会标注省份，避免同名区县选错"
                                }
                            )
                        }
                    )
                    state.citySuggestions.forEach { city ->
                        Text(
                            text = city.displayName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectCity(city) }
                                .padding(vertical = 10.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            TimeRow(
                title = "天气检查时间",
                subtitle = "建议设为起床前，例如 07:00",
                hour = settings.checkHour,
                minute = settings.checkMinute,
                onPick = onPickCheckTime
            )
            TimeRow(
                title = "影响出行时早响铃",
                subtitle = "小雨/中雨/雪/雾/霾等",
                hour = settings.earlyHour,
                minute = settings.earlyMinute,
                onPick = onPickEarlyTime
            )
            TimeRow(
                title = "正常天气响铃",
                subtitle = "晴/多云/阴等不影响步行骑行",
                hour = settings.clearHour,
                minute = settings.clearMinute,
                onPick = onPickClearTime
            )

            Button(
                onClick = onRefreshLocation,
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.isBusy) "处理中..." else "刷新 GPS 定位")
            }
            Button(
                onClick = onTestWeatherNow,
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("立即获取天气并安排/预览闹钟")
            }

            if (!state.canExactAlarm) {
                OutlinedButton(
                    onClick = onOpenExactAlarmSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("去开启精确闹钟权限")
                }
            }
            OutlinedButton(
                onClick = onOpenBatterySettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("去关闭电池优化（提高准时率）")
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "规则：凡雨、雪、雾、霾、沙尘、冰雹等影响步行/骑行的天气 → 早响铃；晴/多云/阴 → 正常响铃。天气接口失败时按早响铃兜底。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
private fun StatusBanner(level: StatusLevel, message: String) {
    val (bg, fg, title) = when (level) {
        StatusLevel.ERROR -> Triple(Color(0xFFFFE2E0), Color(0xFF8B1E1E), "异常")
        StatusLevel.WARNING -> Triple(Color(0xFFFFF1D6), Color(0xFF8A5A00), "注意")
        StatusLevel.SUCCESS -> Triple(Color(0xFFE3F6EA), Color(0xFF1B5E3B), "成功")
        StatusLevel.INFO -> Triple(Color(0xFFE8F3FF), Color(0xFF0B4F78), "提示")
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (level == StatusLevel.ERROR || level == StatusLevel.WARNING) {
                Icon(
                    Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = fg
                )
            }
            Column {
                Text(
                    text = title,
                    color = fg,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = message,
                    color = fg,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoCard(
    icon: @Composable () -> Unit,
    title: String,
    body: String,
    emphasize: Boolean = false
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (emphasize) Color(0xFFFFF8E8) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            icon()
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun TimeRow(
    title: String,
    subtitle: String,
    hour: Int,
    minute: Int,
    onPick: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null)
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            OutlinedButton(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, h, m -> onPick(h, m) },
                        hour,
                        minute,
                        true
                    ).show()
                }
            ) {
                Text("%02d:%02d".format(hour, minute))
            }
        }
    }
}
