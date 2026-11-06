package com.ejemplo.laptimer

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.SystemClock

/**
 * Real-time battery consumption monitor and Time-to-Empty (TTE) estimator.
 * Measures discharge dynamically using BatteryManager current (microamperes/mAh)
 * and battery percentage tracking over time.
 */
object BatteryEstimator {
    private var lastRecordedPct = -1
    private var lastRecordedTimeMs = 0L
    private var filteredPctDropPerMin = 0.0
    private var filteredCurrentMa = 0.0

    fun update(context: Context): BatteryInfo {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val pct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL ||
                plugged != 0

        val now = SystemClock.elapsedRealtime()
        val validPct = if (pct in 1..100) {
            pct
        } else {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt() else 100
        }

        // Hardware instantaneous discharge current in microamperes (negative or positive depending on device OEM)
        val currentUa = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
        val chargeCounterUah = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) ?: 0

        if (charging) {
            lastRecordedPct = validPct
            lastRecordedTimeMs = now
            return BatteryInfo(
                percentage = validPct,
                isCharging = true,
                remainingMinutes = -1,
                dischargeRatePerMinute = 0.0,
                dischargeCurrentMah = 0.0
            )
        }

        // Evaluate current in mA if supported by kernel / BatteryManager
        if (currentUa != 0 && currentUa != Int.MIN_VALUE && currentUa != Int.MAX_VALUE) {
            val absMa = Math.abs(currentUa) / 1000.0
            if (absMa in 50.0..5000.0) {
                filteredCurrentMa = if (filteredCurrentMa == 0.0) absMa else (filteredCurrentMa * 0.85 + absMa * 0.15)
            }
        }

        // Percentage tracking over elapsed time
        if (lastRecordedPct < 0) {
            lastRecordedPct = validPct
            lastRecordedTimeMs = now
        } else {
            val elapsedMs = now - lastRecordedTimeMs
            if (validPct < lastRecordedPct && elapsedMs >= 15_000L) {
                val drop = lastRecordedPct - validPct
                val mins = elapsedMs / 60000.0
                if (mins > 0) {
                    val rate = drop / mins
                    filteredPctDropPerMin = if (filteredPctDropPerMin == 0.0) rate else (filteredPctDropPerMin * 0.7 + rate * 0.3)
                }
                lastRecordedPct = validPct
                lastRecordedTimeMs = now
            } else if (validPct > lastRecordedPct) {
                lastRecordedPct = validPct
                lastRecordedTimeMs = now
            }
        }

        // Estimate remaining minutes to empty
        var remainingMins = -1
        if (filteredPctDropPerMin > 0.005) {
            remainingMins = (validPct / filteredPctDropPerMin).toInt()
        } else if (filteredCurrentMa > 80.0 && chargeCounterUah > 0) {
            val remainingMah = chargeCounterUah / 1000.0
            remainingMins = ((remainingMah / filteredCurrentMa) * 60.0).toInt()
        } else if (filteredCurrentMa > 80.0) {
            // Assume an average ~3500 mAh battery capacity for modern smartphones
            val estimatedRemainingMah = 3500.0 * (validPct / 100.0)
            remainingMins = ((estimatedRemainingMah / filteredCurrentMa) * 60.0).toInt()
        } else if (filteredPctDropPerMin == 0.0) {
            // Default baseline screen-on GPS racing discharge rate (~18% per hour -> 0.3% / min)
            val fallbackRate = 0.3
            remainingMins = (validPct / fallbackRate).toInt()
        }

        val clampedMins = if (remainingMins > 0) remainingMins.coerceIn(1, 1440) else -1

        return BatteryInfo(
            percentage = validPct,
            isCharging = false,
            remainingMinutes = clampedMins,
            dischargeRatePerMinute = filteredPctDropPerMin,
            dischargeCurrentMah = filteredCurrentMa
        )
    }

    fun formatRemainingTime(info: BatteryInfo): String {
        if (info.isCharging) return "--"
        if (info.remainingMinutes <= 0) return "--"
        val h = info.remainingMinutes / 60
        val m = info.remainingMinutes % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }
}

data class BatteryInfo(
    val percentage: Int,
    val isCharging: Boolean,
    val remainingMinutes: Int,
    val dischargeRatePerMinute: Double,
    val dischargeCurrentMah: Double
)
