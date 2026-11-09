package com.ejemplo.laptimer.session

import android.graphics.Color
import android.view.View
import com.ejemplo.laptimer.*
import com.ejemplo.laptimer.firebase.*
import com.ejemplo.laptimer.logic.*
import com.ejemplo.laptimer.net.*
import java.util.Locale

var lastProposedSwapStint = 0
var swapDismissed = false
var pitSwapHandled = false

fun MainActivity.ensureProgrammedSwap() {
    if (!settings.isEndurance()) return
    val cur = stint
    val target = settings.targetFor(cur.number)
    val timeMet = target.maxMin > 0 && stintElapsedMs() >= target.maxMin * 60_000L
    val lapsMet = target.planLaps > 0 && cur.lapsDone >= target.planLaps
    val finalLap = isFinalLap()
    if (!timeMet && !lapsMet && !finalLap) return

    if (pitSwapHandled || swapDismissed) return
    pitSwapHandled = true
    lastProposedSwapStint = cur.number
    val nextDriver = settings.plannedDriver(cur.number + 1) ?: settings.roster().firstOrNull() ?: "Driver 2"
    val next = settings.targetFor(cur.number + 1)
    if (pendingSwap == null) {
        pendingSwap = SwapRequest(
            id = "prog-" + cur.number,
            next = nextDriver,
            laps = next.planLaps,
            minMin = next.minMin,
            maxMin = next.maxMin,
            pitSec = next.pitSec
        )
    }
}

fun MainActivity.updatePitOverlay() {
    if (bindings.pitOverlay.visibility != View.VISIBLE) return
    val inPad = inPaddock && !inPit
    bindings.txtPitTitle.text = if (inPad) "PADDOCK ZONE" else "PIT LANE"

    val dur = if (inPad) 0L else clockNow() - pitStartAt
    val min = minPitSec()
    val ok = min <= 0 || dur >= min * 1000L

    // Background color: Green if minimum compliance met, flashing YELLOW (pampalluga) if waiting for min pit time
    val colBg = when {
        inPad -> Color.parseColor("#7E22CE")
        ok -> Color.parseColor("#16A34A") // Solid green when min pit time met
        else -> if (pitBlinkOn) Color.parseColor("#FACC15") else Color.parseColor("#CA8A04") // Flashing warning yellow!
    }
    bindings.pitOverlay.setBackgroundColor(colBg)

    val textColor = if (!inPad && !ok) Color.parseColor("#0B0D10") else Color.WHITE
    bindings.txtPitTitle.setTextColor(textColor)
    bindings.txtPitCount.setTextColor(textColor)

    val seconds = dur / 1000.0
    bindings.txtPitCount.text = String.format(Locale.US, "%.1f s", seconds)

    if (min > 0) {
        bindings.txtPitSub.visibility = View.VISIBLE
        bindings.txtPitSub.text = if (ok) "MINIMUM PIT TIME MET ($min s)" else "MANDATORY MINIMUM: $min s"
        bindings.txtPitSub.setTextColor(if (ok) Color.WHITE else Color.parseColor("#141413"))
    } else {
        bindings.txtPitSub.visibility = View.GONE
    }
    val sw = pendingSwap
    if (sw != null && sw.next.isNotBlank()) {
        bindings.pitSwapBox.visibility = View.VISIBLE
        val plan = if (sw.laps > 0) "${sw.laps} laps" else "No lap plan"
        val mn = if (sw.minMin > 0) "${sw.minMin} min" else "no min"
        val mx = if (sw.maxMin > 0) "${sw.maxMin} min" else "no max"
        bindings.txtPitSwap.text = "NEXT: ${sw.next.uppercase()}\n$plan · $mn / $mx"
        bindings.txtPitSwap.setTextColor(textColor)
        bindings.btnPitSwapConfirm.visibility = View.VISIBLE
        bindings.btnPitSwapConfirm.isEnabled = !swapLocked()
    } else {
        bindings.pitSwapBox.visibility = View.GONE
        bindings.btnPitSwapConfirm.visibility = View.GONE
    }
}
