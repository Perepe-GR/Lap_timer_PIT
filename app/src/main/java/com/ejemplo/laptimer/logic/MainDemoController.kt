package com.ejemplo.laptimer.logic

import com.ejemplo.laptimer.*
import com.ejemplo.laptimer.firebase.SwapRequest
import com.ejemplo.laptimer.session.*
import org.json.JSONArray
import org.json.JSONObject

fun MainActivity.demoStopMs(): Long {
    val set = settings.demoStopSec
    if (set > 0) return set * 1000L
    val min = minPitSec()
    if (min <= 0) return 8_000L
    demoStops++
    val target = if (demoStops % 2 == 1) min * 600L else min * 1400L
    return maxOf(1_000L, target - 19_000L)
}

fun MainActivity.maybeRequestDemoPit() {
    val d = demo ?: return
    if (!demoActive || d.pitBusy) return
    val want = if (settings.isEndurance()) (pendingSwap != null || isFinalLap()) else demoLapsSincePit >= 3
    if (!want) return
    if (settings.isEndurance()) {
        val key = pendingSwap?.id ?: ("final-" + stint.number)
        if (key == demoPitKey) return
        demoPitKey = key
    }
    demoLapsSincePit = 0
    d.requestPit(demoStopMs())
}

fun MainActivity.buildTelemetry(): String? {
    if (!hasFix && !demoActive) return null
    val o = JSONObject()
    val s = stint
    o.put("version", "V2.4.0")
    o.put("t", System.currentTimeMillis())
    o.put("lat", lastLat)
    o.put("lon", lastLon)
    o.put("kmh", Math.round(lastKmh))
    o.put("car", settings.carName)
    o.put("driver", s.driver)
    o.put("zone", when {
        inPit -> "pit"
        inPaddock -> "paddock"
        else -> "track"
    })
    o.put("circuito", circuit?.name ?: "")
    o.put("demo", demoActive)
    o.put("bat", batteryLevel())
    o.put("v", "V2.1")
    o.put("stint", s.number)
    o.put("stintLaps", s.lapsDone)
    o.put("plan", s.plan)
    o.put("stintMs", stintElapsedMs())
    o.put("minMs", s.minMin * 60_000L)
    o.put("maxMs", s.maxMin * 60_000L)
    o.put("mode", settings.raceMode)
    race?.startEpoch?.let { o.put("raceStart", it) }
    o.put("lastLap", isFinalLap())
    o.put("health", stintHealth().name.lowercase())
    o.put("minLaps", s.minLaps)
    o.put("maxLaps", s.maxLaps)
    o.put("planMin", s.planMin)
    o.put("pitMinSec", minPitSec())
    if (inPit) o.put("inPitMs", clockNow() - pitStartAt)
    settings.plannedDriver(s.number + 1)?.let { o.put("next", it) }
    if (pendingSwap?.next?.isBlank() == true) o.put("needDriver", true)
    o.put("pitStops", settings.pitStops)
    o.put("pitFails", settings.pitFails)
    o.put("pitLastMs", settings.pitLastMs)
    o.put("inPit", inPit)
    pendingSwap?.let {
        o.put("swap", it.next)
        o.put("swapProg", it.id.startsWith("prog-"))
    }
    currentAlert?.let { o.put("alert", it) }
    val st = lastState
    if (st != null) {
        o.put("lap", st.lapNumber)
        o.put("sector", st.sector)
        o.put("sectores", st.sectorCount)
        o.put("run", st.running)
        st.currentLapMs?.let { o.put("cur", it) }
        st.lastLapMs?.let { o.put("last", it) }
        st.bestLapMs?.let { o.put("best", it) }
        st.optimalMs?.let { o.put("opt", it) }
        st.deltaSec?.let { o.put("delta", it) }
        val splitsArr = JSONArray()
        for (m in st.marks) {
            if (m != null) splitsArr.put(m.ms)
        }
        o.put("splits", splitsArr)
    }
    return o.toString()
}

fun MainActivity.lapJson(r: LapResult): String {
    val o = JSONObject()
    o.put("version", "V2.1")
    o.put("n", r.number)
    o.put("ms", r.lapMs)
    val secCount = circuit?.sectorCount ?: 3
    val isComplete = r.splits.size >= secCount
    o.put("sectorsComplete", isComplete)
    val secObj = JSONObject()
    if (isComplete) {
        for ((idx, sp) in r.splits.withIndex()) {
            if (idx < secCount) secObj.put((idx + 1).toString(), sp)
        }
    }
    o.put("sectors", secObj)
    val arr = JSONArray()
    for (sp in r.splits) arr.put(sp)
    o.put("s", arr)
    o.put("t", System.currentTimeMillis())
    o.put("circuito", circuit?.name ?: "")
    o.put("d", stint.driver)
    o.put("st", stint.number)
    o.put("k", r.kind)
    o.put("demo", demoActive)
    return o.toString()
}

fun MainActivity.startDemo() {
    if (reconName != null) exitRecon()
    endRace("closed")
    raceEnded = false
    recorder?.end()
    demoActive = true
    resetPitUi()
    val t1 = settings.targetFor(1)
    val driver1 = if (settings.isEndurance()) {
        settings.plannedDriver(1) ?: settings.driverName.ifBlank { "Driver 1" }
    } else {
        settings.driverName.ifBlank { "Driver 1" }
    }
    stint = StintState(
        1, driver1, 0, 0L, t1.planLaps, t1.minMin, t1.maxMin, t1.minLaps, t1.maxLaps, t1.planMin
    )
    demoPitKey = null
    demoLapsSincePit = 0
    demoStops = 0
    lastFinalShown = false
    simNowMs = 0L
    pendingSwap = null
    DemoCircuit.init(this)
    circuit = DemoCircuit.circuit
    engine = buildEngine(DemoCircuit.circuit, false)
    lastState = null
    hz = 0.0
    lastRawMs = null
    resetDisplay()
    val d = DemoSource(android.os.Handler(android.os.Looper.getMainLooper())) { ev, render -> onPosition(ev, render) }
    d.factor = demoFactor
    demo = d
    refreshTop()
    d.start()
    toast("Demo: swipe from top and tap bar to change speed")
}

fun MainActivity.stopDemo() {
    endRace("closed")
    raceEnded = false
    demo?.stop()
    demo = null
    demoActive = false
    resetPitUi()
    pendingSwap = null
    stint = loadStint()
    circuit = store.loadActive()
    engine = circuit?.let { buildEngine(it, true) }
    lastState = null
    lastKmh = 0.0
    hz = 0.0
    lastRawMs = null
    resetDisplay()
    refreshTop()
    renderGps()
    updateLock()
}

fun MainActivity.cycleDemoSpeed() {
    demoFactor = when (demoFactor) {
        1 -> 2
        2 -> 4
        else -> 1
    }
    demo?.factor = demoFactor
    showTopBar()
    toast("Demo ×$demoFactor")
}
