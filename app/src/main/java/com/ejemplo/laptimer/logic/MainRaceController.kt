package com.ejemplo.laptimer.logic

import android.content.Intent
import android.graphics.Color
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.ejemplo.laptimer.*
import com.ejemplo.laptimer.session.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun MainActivity.raceTargetLaps(): Int = if (settings.isSprint()) settings.sprintLaps else 0

fun MainActivity.raceTargetMin(): Int = when {
    settings.isSprint() -> settings.sprintMin
    settings.isEndurance() -> settings.raceMin
    else -> 0
}

fun MainActivity.resumeRace() {
    val cur = raceStore.loadCurrent() ?: return
    val now = System.currentTimeMillis()
    if (cur.laps.isNotEmpty() && now - cur.endEpoch < 30 * 60_000L) {
        race = cur
        raceStartClock = clockNow() - (now - cur.startEpoch)
        raceLapsDone = cur.laps.maxOfOrNull { it.n } ?: 0
    } else if (cur.laps.isNotEmpty()) {
        cur.reason = "closed"
        raceStore.archive(cur)
        link?.uploadRaceNow(cur.id, cur.toJson().toString())
    } else {
        raceStore.clearCurrent()
    }
}

fun MainActivity.ensureRace() {
    if (race != null || raceEnded || reconName != null) return
    val c = circuit ?: return
    val now = System.currentTimeMillis()
    race = RaceRecord(
        SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(now)),
        settings.raceMode, if (demoActive) "DEMO" else c.name, settings.carName, now,
        targetLaps = raceTargetLaps(), targetMin = raceTargetMin()
    )
    raceStartClock = clockNow()
    raceTimeUp = false
    raceLapsDone = 0
}

fun MainActivity.onRaceLap(r: LapResult) {
    if (raceEnded) return
    ensureRace()
    val rc = race ?: return
    rc.laps.add(RaceLap(r.number, r.lapMs, r.kind, stint.driver, stint.number, r.splits))
    rc.endEpoch = System.currentTimeMillis()
    raceStore.saveCurrent(rc)
    if (r.number > raceLapsDone) raceLapsDone = r.number
    updateLapHeader()
    val byLaps = rc.targetLaps > 0 && raceLapsDone >= rc.targetLaps
    if (byLaps || raceTimeUp) endRace("target")
}

fun MainActivity.updateLapHeader() {
    val rc = race
    val n = if (rc != null) raceLapsDone + 1 else (lastState?.lapNumber ?: 1)
    val totalLaps = if (settings.isSprint()) settings.sprintLaps else 0
    val totalMin = if (settings.isSprint()) settings.sprintMin else 0
    val sb = StringBuilder("LAP ").append(n)
    if (totalLaps > 0) sb.append(" / ").append(totalLaps)
    bindings.txtLapLabel.text = sb.toString()
    var frac = -1.0
    if (totalLaps > 0) frac = (n - 1).toDouble() / totalLaps
    if (totalMin > 0 && rc != null) {
        val f = (clockNow() - raceStartClock).toDouble() / (totalMin * 60_000.0)
        frac = if (frac < 0) f else maxOf(frac, f)
    }
    val best = lastState?.bestLapMs
    bindings.txtInfo3.text = sb.toString() + (if (best != null) "  ·  BEST " + fmtMs(best) else "")
    bindings.txtStint3.text = bindings.txtStint.text
    bindings.txtStint3.setTextColor(bindings.txtStint.currentTextColor)
    bindings.txtStint3.visibility = if (settings.isEndurance()) View.VISIBLE else View.GONE
    if (frac < 0) {
        bindings.progLaps.visibility = View.GONE
    } else {
        bindings.progLaps.visibility = View.VISIBLE
        bindings.progLaps.progress = (frac.coerceIn(0.0, 1.0) * 1000).toInt()
    }
}

fun MainActivity.checkRaceTime() {
    val rc = race ?: return
    if (raceEnded || raceTimeUp || rc.targetMin <= 0) return
    if (clockNow() - raceStartClock >= rc.targetMin * 60_000L) {
        raceTimeUp = true
        renderBottom()
    }
}

fun MainActivity.isRaceFinalLap(): Boolean {
    val rc = race ?: return false
    if (raceEnded || reconName != null) return false
    if (raceTimeUp) return true
    return rc.targetLaps > 0 && raceLapsDone == rc.targetLaps - 1
}

fun MainActivity.endRace(reason: String) {
    val rc = race
    race = null
    raceTimeUp = false
    raceLapsDone = 0
    if (rc == null) return
    if (rc.laps.isEmpty()) {
        raceStore.clearCurrent()
        return
    }
    rc.endEpoch = System.currentTimeMillis()
    rc.durMs = clockNow() - raceStartClock
    rc.reason = reason
    raceStore.archive(rc)
    link?.uploadRaceNow(rc.id, rc.toJson().toString())
    if (reason == "target") {
        raceEnded = true
        showRaceFinished(rc)
    }
}

fun MainActivity.showRaceFinished(rc: RaceRecord) {
    val msg = "LAPS: ${rc.counted()}\nBEST: ${fmtMs(rc.bestMs())}\nTIME: ${mmss(rc.durMs)}"
    AlertDialog.Builder(this, R.style.Theme_LapTimer_Dialog)
        .setTitle("RACE FINISHED")
        .setMessage(msg)
        .setPositiveButton("OK", null)
        .setNeutralButton("VIEW SUMMARY") { _, _ -> startActivity(Intent(this, RacesActivity::class.java)) }
        .show()
}

fun MainActivity.showLapSummary(r: LapResult) {
    if (inPit || reconName != null) return
    val title = StringBuilder()
    when (r.kind) {
        1 -> title.append("OUT LAP")
        2 -> title.append("IN LAP")
        else -> title.append("LAP ").append(r.number)
    }
    if (r.kind != 0 && r.number > 0) title.append(" · LAP ").append(r.number)
    if (r.isBest) title.append(" · BEST LAP")
    bindings.txtSumTitle.text = title.toString()
    bindings.txtSumTime.text = fmtMs(r.lapMs)
    val d = r.deltaMs
    if (r.kind != 0) {
        bindings.txtSumDelta.text = "—"
        bindings.txtSumDelta.setTextColor(Color.parseColor("#9AA3AE"))
    } else if (d == null) {
        bindings.txtSumDelta.text = "FIRST LAP"
        bindings.txtSumDelta.setTextColor(Color.parseColor("#9AA3AE"))
    } else {
        bindings.txtSumDelta.text = String.format(Locale.US, "%+.3f", d / 1000.0)
        bindings.txtSumDelta.setTextColor(if (d <= 0L) COL_GREEN else Color.parseColor("#EF4444"))
    }
    renderChips(bindings.chipsRowSum, r.marks, r.marks.size, 20f)
    if (settings.isEndurance()) {
        val s = stint
        val sb = StringBuilder("STINT ").append(s.number).append(" · ")
            .append(s.driver.uppercase(Locale.getDefault())).append(" · LAP ").append(s.lapsDone)
        if (s.plan > 0) sb.append('/').append(s.plan)
        bindings.txtSumStint.text = sb.toString()
        bindings.txtSumStint.visibility = View.VISIBLE
    } else {
        bindings.txtSumStint.visibility = View.GONE
    }
    bindings.lapOverlay.visibility = View.VISIBLE
    bindings.lapOverlay.removeCallbacks(hideLapSummary)
    bindings.lapOverlay.postDelayed(hideLapSummary, settings.lapSummarySec * 1000L)
}

fun MainActivity.hideLapSummaryNow() {
    bindings.lapOverlay.removeCallbacks(hideLapSummary)
    bindings.lapOverlay.visibility = View.GONE
}
