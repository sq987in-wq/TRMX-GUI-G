package com.example.data.system

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs

data class DeviceSystemStatus(
    val batteryPercent: Int,
    val isCharging: Boolean,
    val batteryTemperatureCelsius: Float?,
    val thermalStatusText: String,
    val freeStorageBytes: Long,
    val totalStorageBytes: Long,
    val freeStorageFormatted: String,
    val totalStorageFormatted: String,
    val storagePercentUsed: Int,
    val availableRamBytes: Long,
    val totalRamBytes: Long,
    val ramPercentUsed: Int
)

class SystemStatusProvider(private val context: Context) {

    fun getSystemStatus(): DeviceSystemStatus {
        // Battery
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, ifilter)
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 0

        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val rawTemp = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val tempCelsius = if (rawTemp > 0) rawTemp / 10.0f else null

        // Thermal status from PowerManager if available
        var thermalStr = "Normal"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val thermal = powerManager?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE
            thermalStr = when (thermal) {
                PowerManager.THERMAL_STATUS_NONE -> "Nominal"
                PowerManager.THERMAL_STATUS_LIGHT -> "Light Throttling"
                PowerManager.THERMAL_STATUS_MODERATE -> "Moderate"
                PowerManager.THERMAL_STATUS_SEVERE -> "Severe"
                PowerManager.THERMAL_STATUS_CRITICAL -> "Critical"
                PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency"
                PowerManager.THERMAL_STATUS_SHUTDOWN -> "Shutdown"
                else -> "Normal"
            }
        }

        // Storage using StatFs on internal data directory
        val stat = StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val totalStorage = totalBlocks * blockSize
        val freeStorage = availableBlocks * blockSize
        val usedStorage = totalStorage - freeStorage
        val storagePctUsed = if (totalStorage > 0) ((usedStorage.toDouble() / totalStorage) * 100).toInt() else 0

        // Memory / RAM
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)
        val totalRam = memInfo.totalMem
        val availRam = memInfo.availMem
        val usedRam = totalRam - availRam
        val ramPctUsed = if (totalRam > 0) ((usedRam.toDouble() / totalRam) * 100).toInt() else 0

        return DeviceSystemStatus(
            batteryPercent = batteryPct,
            isCharging = isCharging,
            batteryTemperatureCelsius = tempCelsius,
            thermalStatusText = thermalStr,
            freeStorageBytes = freeStorage,
            totalStorageBytes = totalStorage,
            freeStorageFormatted = formatBytes(freeStorage),
            totalStorageFormatted = formatBytes(totalStorage),
            storagePercentUsed = storagePctUsed,
            availableRamBytes = availRam,
            totalRamBytes = totalRam,
            ramPercentUsed = ramPctUsed
        )
    }

    private fun formatBytes(bytes: Long): String {
        val gb = bytes.toDouble() / (1024 * 1024 * 1024)
        return if (gb >= 1.0) {
            String.format("%.1f GB", gb)
        } else {
            val mb = bytes.toDouble() / (1024 * 1024)
            String.format("%.0f MB", mb)
        }
    }
}
