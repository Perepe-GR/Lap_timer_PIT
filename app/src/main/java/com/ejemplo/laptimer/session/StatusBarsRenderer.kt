package com.ejemplo.laptimer.session

import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import com.ejemplo.laptimer.*
import com.ejemplo.laptimer.firebase.NetStatus
import com.ejemplo.laptimer.logic.*
import java.util.Locale

fun MainActivity.renderGps() {
    val batInfo = BatteryEstimator.update(this)
    val batText = BatteryEstimator.formatRemainingTime(batInfo)

    val accStr = lastAccuracyMeters?.let { acc ->
        if (acc < 1.0f) String.format(Locale.US, " · ± %.0f cm", acc * 100f)
        else String.format(Locale.US, " · ± %.1f m", acc)
    } ?: ""

    val gpsStr = when {
        demoActive -> String.format(Locale.US, "DEMO ×%d · Simulated GPS %.0f Hz", demoFactor, hz)
        !hasFix -> "No GPS signal"
        else -> String.format(Locale.US, "GPS %.0f Hz · %d sat · HDOP %.1f%s", hz, sats, hdop, accStr)
    }
    bindings.txtGps.typeface = Typeface.MONOSPACE
    bindings.txtGps.text = "$gpsStr  ·  $batText"

    bindings.txtGpsSpeedBig.text = String.format(Locale.US, "%.0f", lastKmh)
    bindings.txtGpsDetailFreq.text = String.format(Locale.US, "Refresh Rate: %.1f Hz", hz)
    bindings.txtGpsDetailSats.text = String.format(Locale.US, "Satellites: %d · HDOP: %.1f", sats, hdop)
    val accDetail = lastAccuracyMeters?.let { acc ->
        if (acc < 1.0f) String.format(Locale.US, " · Accuracy: %.0f cm", acc * 100f)
        else String.format(Locale.US, " · Accuracy: %.1f m", acc)
    } ?: ""
    bindings.txtGpsDetailAlt.text = String.format(Locale.US, "Altitude: %.1f m%s", altitude, accDetail)
}

fun MainActivity.renderImuTelemetry(data: com.ejemplo.laptimer.engine.ImuTelemetryData) {
    if (viewIndex != 4) return
    bindings.gForceCircleView.updateGForce(data.gLateral, data.gLongitudinal)
    bindings.txtGXYValue.text = String.format(Locale.US, "LAT: %+.2f G · LONG: %+.2f G", data.gLateral, data.gLongitudinal)
    bindings.txtGZValue.text = String.format(Locale.US, "%+.2fG", data.gVertical)

    val progressZ = ((data.gVertical.coerceIn(-1.0f, 1.0f) + 1.0f) * 50.0f).toInt().coerceIn(0, 100)
    bindings.progGVertical.progress = progressZ
}

fun MainActivity.renderNet() {
    when {
        !settings.enabled -> { bindings.txtNet.text = "PIT: Disabled"; bindings.txtNet.setTextColor(Color.parseColor("#9AA3AE")) }
        netStatus == NetStatus.OK -> { bindings.txtNet.text = "PIT: Connected"; bindings.txtNet.setTextColor(COL_GREEN) }
        netStatus == NetStatus.ERROR -> { bindings.txtNet.text = "PIT: Offline"; bindings.txtNet.setTextColor(Color.parseColor("#EF4444")) }
        else -> { bindings.txtNet.text = "PIT: Connecting…"; bindings.txtNet.setTextColor(Color.parseColor("#9AA3AE")) }
    }
}

fun MainActivity.renderBottom() {
    val recon = reconName; val msg = activeMsgs.lastOrNull(); val sw = pendingSwap
    when {
        recon != null -> {
            bindings.bottomBar.visibility = View.VISIBLE
            bindings.reconBar.visibility = View.VISIBLE; bindings.bannerPit.visibility = View.GONE; bindings.swapBar.visibility = View.GONE
            bindings.pitBannerPit.visibility = View.GONE
        }
        msg != null -> {
            bindings.bottomBar.visibility = View.VISIBLE
            bindings.reconBar.visibility = View.GONE; bindings.bannerPit.visibility = View.VISIBLE; bindings.swapBar.visibility = View.GONE
            applyMsgStyle(msg.bg, msg.text)
        }
        sw != null -> {
            bindings.bottomBar.visibility = View.VISIBLE
            bindings.reconBar.visibility = View.GONE; bindings.bannerPit.visibility = View.GONE; bindings.swapBar.visibility = View.VISIBLE
            bindings.pitBannerPit.visibility = View.GONE
            val d = sw.next.ifEmpty { "driver" }
            bindings.txtSwap.text = "SWAP: Enter PIT to hand over to $d"
        }
        else -> {
            bindings.bottomBar.visibility = View.GONE
            bindings.reconBar.visibility = View.GONE; bindings.bannerPit.visibility = View.GONE; bindings.swapBar.visibility = View.GONE
            bindings.pitBannerPit.visibility = View.GONE
        }
    }
}

fun MainActivity.refreshTop() {
    val name = reconName
    bindings.txtTop.text = when {
        name != null -> "Recon «$name» · FINISH ${if (draftFinish != null) "✔" else "—"} · sectors ${draftSectors.size}"
        demoActive -> "DEMO ×%d · tap to change speed"
        circuit == null -> "No active track · go to HOME > CIRCUITS"
        else -> "${circuit?.name} · Cross FINISH to start"
    }
}

fun MainActivity.updateStintStrip() {
    if (!settings.isEndurance()) {
        bindings.stintBar.visibility = View.GONE; bindings.txtSpeedCorner.visibility = View.VISIBLE
        return
    }
    bindings.stintBar.visibility = View.VISIBLE; bindings.txtSpeedCorner.visibility = View.GONE
    val s = stint; val el = stintElapsedMs()
    val sb = StringBuilder("STINT ").append(s.number).append(" · ").append(s.driver.uppercase(Locale.getDefault())).append(" · ").append(mmss(el))
    if (s.maxMin > 0) sb.append('/').append(mmss(s.maxMin * 60_000L))
    sb.append(" · LAP ").append(s.lapsDone)
    if (s.plan > 0) sb.append('/').append(s.plan)
    bindings.txtStint.text = sb.toString()
    val color = when (stintHealth()) {
        StintHealth.RED -> COL_RED; StintHealth.GREEN -> COL_GREEN
        StintHealth.PURPLE -> Color.parseColor("#A855F7"); StintHealth.NONE -> Color.parseColor("#9AA3AE")
    }
    bindings.txtStint.setTextColor(color)
}
