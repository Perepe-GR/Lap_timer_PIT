package com.ejemplo.laptimer.session

import android.os.SystemClock
import com.ejemplo.laptimer.*
import com.ejemplo.laptimer.logic.*

private val arrivalWindow = java.util.ArrayDeque<Long>()

fun MainActivity.buildEngine(c: Circuit, withHistory: Boolean): LapEngine {
    val initial = if (withHistory) statsStore.load(c, scopeKey()) else null
    val e = LapEngine(c, initial)
    e.onLap = { r -> handleLap(r) }
    e.onBoundary = { runOnUiThread { onBoundary() } }
    e.onPitIn = { runOnUiThread { enterPit() } }
    e.onPitOut = { runOnUiThread { exitPit() } }
    e.outlapCounts = settings.outlapCounts
    e.inlapCounts = settings.inlapCounts
    e.setInPit(inPit)
    return e
}

fun MainActivity.onPosition(ev: NmeaEvent.Position, render: Boolean) {
    lastLat = ev.lat
    lastLon = ev.lon
    lastKmh = ev.speedKmh
    hasFix = true
    val now = SystemClock.elapsedRealtime()
    if (!demoActive) lastFixAt = now
    val c = ev.courseDeg
    if (c != null && ev.speedKmh > 1.5) lastCourse = c

    if (demoActive) simNowMs = ev.timeMs

    val sNow = stint
    circuit?.let { if (it.hasPaddock && reconName == null) updatePaddock(it.inPaddock(ev.lat, ev.lon)) }
    if (race == null && !raceEnded && ev.speedKmh > 5.0) ensureRace()
    if (sNow.startEpoch == 0L && !inPit && !inPaddock && ev.speedKmh > 5.0) {
        sNow.startEpoch = clockNow()
        saveStint()
    }

    // Stabilized dynamic Hz calculation based on USB arrival window
    arrivalWindow.addLast(now)
    while (arrivalWindow.size > 25) arrivalWindow.removeFirst()
    if (arrivalWindow.size >= 4) {
        val dt = (arrivalWindow.last() - arrivalWindow.first()) / 1000.0
        if (dt > 0.1) {
            val calcHz = (arrivalWindow.size - 1) / dt
            hz = if (hz == 0.0) calcHz else hz * 0.85 + calcHz * 0.15
        }
    }
    lastRawMs = ev.timeMs

    val st = engine?.onFix(ev.lat, ev.lon, ev.speedKmh, ev.timeMs)
    lastState = st

    if (!demoActive && st != null) {
        val circ = circuit
        val rec = recorder
        if (circ != null && rec != null &&
            rec.onSample(circ, st.gpsTimeMs, ev.lat, ev.lon, ev.speedKmh, stint.driver, stint.number, settings.carName)
        ) {
            engine?.resetSession()
        }
    }

    if (!render) return
    runOnUiThread {
        val kmhText = String.format(java.util.Locale.US, "%.0f", ev.speedKmh)
        bindings.txtSpeed.text = kmhText
        bindings.txtSpeedCorner.text = "$kmhText km/h"
        renderGps()
        if (st != null) renderEngine(st)
        updateLock()
        if (!demoActive && (!autoLoadDone || circuit == null)) {
            autoLoad(ev.lat, ev.lon)
        }
    }
}

private var lastAutoLoadCheckMs = 0L

fun MainActivity.autoLoad(lat: Double, lon: Double) {
    if (reconName != null) return
    if (lastState?.running == true) return
    if (Math.abs(lat) < 0.0001 && Math.abs(lon) < 0.0001) return

    val now = SystemClock.elapsedRealtime()
    if (now - lastAutoLoadCheckMs < 10_000L && circuit != null) return
    lastAutoLoadCheckMs = now

    val near = store.nearest(lat, lon, 5000.0) ?: return
    if (near.name != circuit?.name) {
        selectCircuit(near)
        toast("Auto-loaded circuit: ${near.name}")
        autoLoadDone = true
    }
}
