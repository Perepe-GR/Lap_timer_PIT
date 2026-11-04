package com.ejemplo.laptimer.session

import android.os.SystemClock
import android.view.View
import com.ejemplo.laptimer.*
import com.ejemplo.laptimer.firebase.*
import com.ejemplo.laptimer.logic.*
import com.ejemplo.laptimer.net.*

fun MainActivity.syncActiveCircuit() {
    val active = store.loadActive()
    if (active != null && active != circuit) selectCircuit(active)
}

fun MainActivity.selectCircuit(c: Circuit) {
    endRace("closed")
    raceEnded = false
    recorder?.end()
    resetPitUi()
    circuit = c
    engine = buildEngine(c, true)
    lastState = null
    store.activeName = c.name
    resetDisplay()
    refreshTop()
}

fun MainActivity.resetPitUi() {
    inPaddock = false
    inPit = false
    pitSecOverride = 0
    pitSwapHandled = false
    swapDismissed = false
    bindings.pitOverlay.removeCallbacks(pitBlink)
    bindings.pitOverlay.visibility = View.GONE
}

fun MainActivity.minPitSec(): Int {
    if (!settings.isEndurance()) return 0
    if (pitSecOverride > 0) return pitSecOverride
    val sw = pendingSwap
    return if (sw != null && sw.pitSec > 0) sw.pitSec else settings.pitMinSec
}

fun MainActivity.updatePaddock(inside: Boolean) {
    if (!inside) paddockDismissed = false
    val now = inside && !paddockDismissed
    if (now == inPaddock) return
    inPaddock = now
    runOnUiThread {
        if (now) {
            if (circuit?.hasPitLane == true) engine?.setInPit(true)
            showPaddockOverlay()
        } else if (!inPit) {
            bindings.pitOverlay.removeCallbacks(pitBlink)
            bindings.pitOverlay.visibility = View.GONE
        }
    }
}

fun MainActivity.showPaddockOverlay() {
    if (reconName != null) return
    hideLapSummaryNow()
    bindings.pitOverlay.visibility = View.VISIBLE
    bindings.pitOverlay.removeCallbacks(pitBlink)
    pitBlink.run()
}

fun MainActivity.enterPit() {
    if (reconName != null || inPit) return
    if (demoActive && demo?.inPitLane != true) return
    inPit = true
    pitStartAt = clockNow()
    pitBlinkOn = false
    pitSwapHandled = false
    swapDismissed = false
    hideLapSummaryNow()
    bindings.pitOverlay.visibility = View.VISIBLE
    bindings.pitOverlay.removeCallbacks(pitBlink)
    pitBlink.run()
    ensureProgrammedSwap()
    val sw = pendingSwap
    if (sw != null && sw.confirmed && settings.isEndurance()) applySwap(sw)
}

fun MainActivity.exitPit() {
    if (stint.startEpoch == 0L) {
        stint.startEpoch = clockNow()
        saveStint()
    }
    if (!inPit) return
    val dur = clockNow() - pitStartAt
    val min = minPitSec()
    inPit = false
    pitSecOverride = 0
    pitSwapHandled = false
    swapDismissed = false
    bindings.pitOverlay.removeCallbacks(pitBlink)
    bindings.pitOverlay.visibility = View.GONE
    if (inPaddock) showPaddockOverlay()
    val ok = !(min > 0 && dur < min * 1000L)
    if (!demoActive) {
        settings.pitStops = settings.pitStops + 1
        settings.pitLastMs = dur
        if (!ok) settings.pitFails = settings.pitFails + 1
        settings.addPitLog(stint.number, dur, min, ok)
    }
    if (!ok) {
        pitFailText = "PIT STOP INCUMPLIDO: ${dur / 1000} s DE $min s"
        pitFailUntil = SystemClock.elapsedRealtime() + 15_000L
        updateAlerts()
    }
    renderBottom()
}
