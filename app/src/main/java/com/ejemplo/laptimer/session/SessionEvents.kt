package com.ejemplo.laptimer.session

import android.os.SystemClock
import com.ejemplo.laptimer.MainActivity
import com.ejemplo.laptimer.firebase.PitMessage
import com.ejemplo.laptimer.firebase.SwapRequest
import com.ejemplo.laptimer.logic.*

fun MainActivity.showPitMessages(list: List<PitMessage>) {
    if (list.isEmpty()) return
    val now = SystemClock.elapsedRealtime()
    for (m in list) {
        val sectors = settings.msgSectors.coerceIn(1, 10)
        activeMsgs.removeAll { it.text == m.text }
        activeMsgs.add(ActiveMsg(m.text, m.bg, sectors, now))
    }
    renderBottom()
}

fun MainActivity.onBoundary() {
    if (activeMsgs.isEmpty()) return
    for (m in activeMsgs) m.remaining--
    activeMsgs.removeAll { it.remaining <= 0 }
    renderBottom()
}

fun MainActivity.pruneMsgs() {
    if (activeMsgs.isEmpty()) return
    val now = SystemClock.elapsedRealtime()
    val timeoutMs = (settings.msgSec * 1000L).coerceIn(5_000L, 120_000L)
    val sizeBefore = activeMsgs.size
    activeMsgs.removeAll { now - it.at > timeoutMs }
    if (activeMsgs.size != sizeBefore) {
        renderBottom()
    }
}

fun MainActivity.confirmSwap() {
    var req = pendingSwap
    if (req == null || req.next.isBlank()) {
        val roster = settings.roster()
        val currentDriver = stint.driver
        val nextDriver = roster.firstOrNull { !it.equals(currentDriver, true) }
            ?: roster.firstOrNull()
            ?: "Driver 2"
        val nextTarget = settings.targetFor(stint.number + 1)
        req = SwapRequest(
            id = "manual-" + System.currentTimeMillis(),
            next = nextDriver,
            laps = nextTarget.planLaps,
            minMin = nextTarget.minMin,
            maxMin = nextTarget.maxMin,
            pitSec = nextTarget.pitSec,
            confirmed = true
        )
        pendingSwap = req
    }
    if (swapLocked()) {
        toast("Locked: speed exceeds ${settings.lockKmh} km/h")
        return
    }
    applySwap(req)
}

fun MainActivity.applySwap(req: SwapRequest) {
    recorder?.end()
    val n = stint.number + 1
    pitSwapHandled = true
    swapDismissed = true
    val t = settings.targetFor(n)
    val plan = if (req.laps > 0) req.laps else t.planLaps
    val mn = if (req.minMin > 0) req.minMin else t.minMin
    val mx = if (req.maxMin > 0) req.maxMin else t.maxMin
    stint = StintState(n, req.next.ifEmpty { "Driver" }, 0, 0L, plan, mn, mx, t.minLaps, t.maxLaps, t.planMin)
    saveStint()
    val c = circuit
    if (!demoActive && c != null && settings.refScope == "piloto") {
        engine = buildEngine(c, true)
        lastState = null
        resetDisplay()
    } else {
        engine?.resetSession()
    }
    if (inPit && req.pitSec > 0) pitSecOverride = req.pitSec
    pendingSwap = null
    if (!req.id.startsWith("prog-")) link?.clearCommandNow()
    lastFinalShown = false
    if (demoActive && inPit) {
        demo?.releasePit()
        demoPitKey = null
    }
    renderBottom()
    updateStintStrip()
    updatePitOverlay()
    toast("Driver swap completed: driving ${stint.driver}")
}

fun MainActivity.onPanelCommand(req: SwapRequest?) {
    pendingSwap = req
    if (req != null && req.confirmed && inPit) {
        applySwap(req)
        return
    }
    renderBottom()
    updatePitOverlay()
}
