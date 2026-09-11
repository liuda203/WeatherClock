# WeatherClock（天气闹钟）

安卓应用：按当地天气自动调整起床闹钟。天气好时可以步行、骑车上班，多睡一会儿；天气恶劣时需要开车、搭乘公共交通上班，提前起床安排行程。

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

=======================================================================
天气闹钟 APK 安装说明

APK 位置：
从本仓库的Release下载 WeatherClock.apk

安装步骤：
1. 把 APK 传到安卓手机后用浏览器打开安装
2. 若提示“未知来源”，在系统设置中允许安装

功能更新：
1. 恶劣天气（大雨/大雾/降雪/雷暴等）→ 早响铃；天气正常 → 正常响铃
2. 网络/天气失败时，自动按早响铃时间兜底，避免闹钟不响
3. 支持「手动强制响铃时间」开关，忽略天气固定响铃
4. 支持「手动选城」，不依赖 GPS
5. 顶部状态条会醒目提示成功/警告/异常

建议首次：
- 授予通知权限；精确闹钟权限；关闭电池优化
- 手动选城或刷新定位后，打开总开关

=======================================================================
页面效果如下：

<img width="1256" height="2618" alt="6d55e429b52dd1d35c9bee4e61f545c7" src="https://github.com/user-attachments/assets/c8699f7b-9ec1-45b4-93c3-ae83fcb9ef09" />

<img width="1256" height="2618" alt="56b77a95367cabe3bc757e971b8a6d0e" src="https://github.com/user-attachments/assets/74ab4b13-b709-49e5-86c5-f5537965cae9" />

