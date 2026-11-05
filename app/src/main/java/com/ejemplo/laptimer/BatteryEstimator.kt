package com.ejemplo.laptimer

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.SystemClock

/** Real-time battery consumption monitor and Time-to-Empty (TTE) estimator. */
object BatteryEstimator {
    private var initialPct = -1
    private var initialTimeMs = 0L
    private var dischargeRatePerMinute = 0.0 // percentage consumed per minute

    fun update(context: Context): BatteryInfo {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val now = SystemClock.elapsedRealtime()
        if (initialPct < 0 || now - initialTimeMs > 60_000L) {
            if (initialPct != pct) {
                if (initialPct > 0 && pct < initialPct && !charging) {
                    val drop = initialPct - pct
                    val mins = (now - initialTimeMs) / 60000.0
                    if (mins > 0) {
                        val currentRate = drop / mins
                        dischargeRatePerMinute = if (dischargeRatePerMinute == 0.0) currentRate else (dischargeRatePerMinute * 0.7 + currentRate * 0.3)
                    }
                }
                initialPct = pct
                initialTimeMs = now
            }
        }

        val remainingMinutes = if (!charging && dischargeRatePerMinute > 0.0 && pct > 0) {
            (pct / dischargeRatePerMinute).toInt()
        } else {
            -1
        }

        return BatteryInfo(
            percentage = if (pct in 1..100) pct else 100,
            isCharging = charging,
            remainingMinutes = remainingMinutes,
            dischargeRatePerHour = dischargeRatePerMinute * 60.0
        )
    }
}

data class BatteryInfo(
    val percentage: Int,
    val isCharging: Boolean,
    val remainingMinutes: Int,
    val dischargeRatePerHour: Double
)
