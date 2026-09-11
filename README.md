# WeatherClock（天气闹钟）

安卓应用：按当地天气自动调整起床闹钟。

## 功能

- 到设定时间请求天气，按是否影响出行决定响铃时刻
- 雨/雪/雾/霾/沙尘/冰雹等 → 早响铃；晴/多云/阴 → 正常响铃
- 网络失败时按早响铃时间兜底
- 支持手动强制响铃时间
- 使用中国天气网城市区号表手动选城
- 天气接口：`http://t.weather.sojson.com/api/weather/city/{区号}`

## 构建

```bash
# 需要 JDK 17 + Android SDK
export JAVA_HOME=...
export ANDROID_HOME=...
./gradlew assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

## 说明

- `local.properties` 仅本机使用，不入库
- 城市区号数据来自 `app/src/main/assets/china_city_codes.json`
