package com.example.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build

data class DeviceTelemetry(
    val deviceModel: String,
    val batteryPercent: Int,
    val isCharging: Boolean,
    val wifiSignalDbm: Int,
    val wifiSignalLevel: Int, // 0 to 4 bars
    val wifiSsid: String,
    val wifiLinkSpeedMbps: Int,
    val volumePercent: Int,
    val isMuted: Boolean
)

object SystemStatsProvider {

    fun getTelemetry(context: Context): DeviceTelemetry {
        val (battery, charging) = getBatteryInfo(context)
        val (rssi, level, ssid, linkSpeed) = getWifiInfo(context)
        val (volume, muted) = getVolumeInfo(context)
        val model = getDeviceModelName()

        return DeviceTelemetry(
            deviceModel = model,
            batteryPercent = battery,
            isCharging = charging,
            wifiSignalDbm = rssi,
            wifiSignalLevel = level,
            wifiSsid = ssid,
            wifiLinkSpeedMbps = linkSpeed,
            volumePercent = volume,
            isMuted = muted
        )
    }

    fun getBatteryInfo(context: Context): Pair<Int, Boolean> {
        return try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val intent = context.registerReceiver(null, filter)
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val percent = if (level >= 0 && scale > 0) {
                ((level.toFloat() / scale.toFloat()) * 100).toInt().coerceIn(0, 100)
            } else {
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
            }
            Pair(percent, isCharging)
        } catch (_: Exception) {
            Pair(85, false)
        }
    }

    fun getWifiInfo(context: Context): WifiDetails {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val wifiInfo: WifiInfo? = wifiManager?.connectionInfo
            val rssi = wifiInfo?.rssi ?: -52

            val level = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && wifiManager != null) {
                wifiManager.calculateSignalLevel(rssi).coerceIn(0, 4)
            } else {
                @Suppress("DEPRECATION")
                WifiManager.calculateSignalLevel(rssi, 5).coerceIn(0, 4)
            }

            var ssid = wifiInfo?.ssid?.trim('\"') ?: "SoundMesh-Net"
            if (ssid.isEmpty() || ssid == "<unknown ssid>") {
                ssid = "SoundMesh Wi-Fi"
            }
            val speed = wifiInfo?.linkSpeed?.takeIf { it > 0 } ?: 433

            WifiDetails(rssi = rssi, level = level, ssid = ssid, linkSpeedMbps = speed)
        } catch (_: Exception) {
            WifiDetails(rssi = -52, level = 4, ssid = "SoundMesh-Net", linkSpeedMbps = 433)
        }
    }

    fun getVolumeInfo(context: Context): Pair<Int, Boolean> {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val current = am?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 10
            val max = am?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
            val pct = if (max > 0) ((current.toFloat() / max) * 100).toInt() else 80
            val isMuted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am?.isStreamMute(AudioManager.STREAM_MUSIC) ?: false
            } else {
                current == 0
            }
            Pair(pct, isMuted)
        } catch (_: Exception) {
            Pair(80, false)
        }
    }

    fun getDeviceModelName(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }
    }

    data class WifiDetails(
        val rssi: Int,
        val level: Int,
        val ssid: String,
        val linkSpeedMbps: Int
    )
}
