package com.ejemplo.laptimer.session

import com.ejemplo.laptimer.*
import com.ejemplo.laptimer.logic.*

fun MainActivity.handleLap(r: LapResult) {
    link?.enqueueLap(lapJson(r))
    val s = stint
    if (r.number > 0) s.lapsDone++
    if (s.planMin > 0 && s.planHitLaps == 0 && stintElapsedMs() >= s.planMin * 60_000L) s.planHitLaps = s.lapsDone
    saveStint()
    runOnUiThread {
        showLapSummary(r)
        bindings.trackMap.onLapDone(r.kind, r.marks)
        onRaceLap(r)
    }
    if (demoActive) {
        if (r.number > 0) demoLapsSincePit++
        return
    }
    if (r.kind != 0) return
    recorder?.onLap(r)
    val c = circuit
    if (c != null) {
        statsStore.update(c, "", r)
        statsStore.update(c, "p_" + NetSettings.keyOf(s.driver), r)
    }
}
