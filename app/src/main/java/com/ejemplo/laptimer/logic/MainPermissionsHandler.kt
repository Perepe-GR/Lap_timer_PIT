package com.ejemplo.laptimer.logic

import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.os.BatteryManager
import android.os.SystemClock
import android.view.View
import com.ejemplo.laptimer.MainActivity
import com.ejemplo.laptimer.NmeaEvent
import com.ejemplo.laptimer.BatteryEstimator
import com.ejemplo.laptimer.session.*

fun MainActivity.connectGps() {
    if (demoActive) return
    val src = settings.gpsSource
    if (src == "internal" || src == "racebox" || src == "dragy" || src == "garmin" || src == "external_bt") {
        gpsLocationManager.connect(this)
        return
    }
    usbSerialManager.connect()
}

fun MainActivity.disconnectGps() {
    gpsLocationManager.disconnect()
    usbSerialManager.disconnect()
}

fun MainActivity.onNewData(data: ByteArray) {
    lastDataAt = SystemClock.elapsedRealtime()
    val text = String(data, Charsets.US_ASCII)
    synchronized(lineBuffer) {
        lineBuffer.append(text)
        var idx = lineBuffer.indexOf("\n")
        while (idx >= 0) {
            val line = lineBuffer.substring(0, idx).trim()
            lineBuffer.delete(0, idx + 1)
            if (line.isNotEmpty()) handleSentence(line)
            idx = lineBuffer.indexOf("\n")
        }
    }
}

fun MainActivity.handleSentence(line: String) {
    when (val ev = parser.parse(line)) {
        is NmeaEvent.Position -> if (!demoActive) onPosition(ev, true)
        is NmeaEvent.Fix -> runOnUiThread {
            sats = ev.sats
            hdop = ev.hdop
            if (ev.alt != null) altitude = ev.alt
            renderGps()
        }
        null -> Unit
    }
}

fun MainActivity.isCharging(): Boolean {
    val i = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val st = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val plugged = i?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
    return st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL || plugged != 0
}

fun MainActivity.batteryLevel(): Int {
    val b = batteryPct()
    return if (b in 1..100) b else 100
}

fun MainActivity.batteryPct(): Int {
    val bm = getSystemService(android.content.Context.BATTERY_SERVICE) as BatteryManager
    return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
}

fun MainActivity.computeAlert(): Pair<String, Boolean>? {
    val now = SystemClock.elapsedRealtime()
    if (!demoActive) {
        val internalGps = settings.gpsSource == "internal"
        val activeConnected = if (internalGps) (hasFix || now - lastDataAt < 10_000L) else (usbSerialManager.isConnected && now - lastDataAt < 5_000L)
        if (!activeConnected) {
            engine?.invalidateCurrentLap()
            return Pair("No GPS signal", true)
        }
        if (!internalGps && now - lastFixAt > 5_000L) {
            engine?.invalidateCurrentLap()
            return Pair("No GPS signal", true)
        }
    }
    val s = stint
    val el = stintElapsedMs()
    val started = s.startEpoch > 0L
    val endurance = settings.isEndurance()
    if (endurance && s.maxMin > 0 && started && el >= s.maxMin * 60_000L) {
        return Pair("STINT: TIME LIMIT EXCEEDED", true)
    }
    if (endurance && s.maxLaps > 0 && s.lapsDone > s.maxLaps) {
        return Pair("STINT: LAP LIMIT EXCEEDED", true)
    }
    if (now < pitFailUntil) return Pair(pitFailText, true)

    val batInfo = BatteryEstimator.update(this)
    // Never show battery alerts if charging / connected to power
    if (!batInfo.isCharging) {
        val remStr = if (batInfo.remainingMinutes > 0) " (${BatteryEstimator.formatRemainingTime(batInfo)})" else ""
        if (batInfo.remainingMinutes in 1..20) {
            return Pair("CRITICAL BATTERY: ${batInfo.percentage}%$remStr", true)
        }
        if (batInfo.remainingMinutes in 21..35) {
            return Pair("LOW BATTERY: ${batInfo.percentage}%$remStr", false)
        }
        if (batInfo.percentage <= settings.batCrit) {
            return Pair("CRITICAL BATTERY: ${batInfo.percentage}%$remStr", true)
        }
    }

    if (endurance && s.maxMin > 0 && started && el >= (s.maxMin - 5) * 60_000L) {
        val left = ((s.maxMin * 60_000L) - el + 59_999L) / 60_000L
        return Pair("STINT: $left MIN REMAINING FOR MAX", false)
    }
    if (endurance && s.plan > 0 && s.lapsDone >= s.plan) {
        return Pair("STINT: PLANNED LAPS COMPLETED", false)
    }

    if (!batInfo.isCharging && batInfo.percentage <= settings.batWarn) {
        val remStr = if (batInfo.remainingMinutes > 0) " (${BatteryEstimator.formatRemainingTime(batInfo)})" else ""
        return Pair("LOW BATTERY: ${batInfo.percentage}%$remStr", false)
    }
    return null
}

fun MainActivity.updateAlerts() {
    val a = computeAlert()
    currentAlert = a?.first
    if (a == null) {
        bindings.alertStrip.visibility = View.GONE
        return
    }
    bindings.alertStrip.typeface = Typeface.MONOSPACE
    bindings.alertStrip.text = a.first
    bindings.alertStrip.setBackgroundColor(if (a.second) Color.parseColor("#EF4444") else Color.parseColor("#F5A524"))
    bindings.alertStrip.setTextColor(if (a.second) Color.WHITE else Color.parseColor("#141413"))
    bindings.alertStrip.visibility = View.VISIBLE
}

fun MainActivity.locked(): Boolean = !demoActive && settings.lockKmh > 0 && lastKmh > settings.lockKmh

fun MainActivity.swapLocked(): Boolean = settings.lockKmh > 0 && lastKmh > settings.lockKmh

fun MainActivity.updateLock() {
    bindings.btnHome.alpha = if (locked()) 0.4f else 1f
    val sa = if (swapLocked()) 0.4f else 1f
    bindings.btnSwapConfirm.alpha = sa
    bindings.btnPitSwapConfirm.alpha = sa
}

fun MainActivity.checkUnlocked(): Boolean {
    if (locked()) {
        toast("Locked: speed exceeds ${settings.lockKmh} km/h")
        return false
    }
    return true
}
