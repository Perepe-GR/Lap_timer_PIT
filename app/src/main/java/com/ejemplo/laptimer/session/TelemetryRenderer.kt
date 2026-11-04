package com.ejemplo.laptimer.session

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.widget.TextViewCompat
import com.ejemplo.laptimer.*
import com.ejemplo.laptimer.logic.*
import java.util.Locale

fun MainActivity.renderEngine(st: EngineState) {
    if (st.isInvalid) {
        bindings.txtLap.text = "Invalid Lap"; bindings.txtLap3.text = "Invalid Lap"
    } else {
        bindings.txtLap.text = fmtMs(st.currentLapMs); bindings.txtLap3.text = fmtMs(st.currentLapMs)
    }
    circuit?.let {
        if (bindings.trackMap.circuit !== it) bindings.trackMap.setCircuit(it)
        bindings.trackMap.onPosition(lastLat, lastLon, (st.sector - 1).coerceAtLeast(0), st.running)
    }
    bindings.txtLast.text = fmtMs(st.lastLapMs); bindings.txtBest.text = fmtMs(st.bestLapMs); bindings.txtOpt.text = fmtMs(st.optimalMs)
    renderChips(bindings.chipsRow, st.marks, st.sectorCount, 12f)
    renderChips(bindings.chipsRow2, st.marks, st.sectorCount, 24f)
    renderChips(bindings.chipsRow3, st.marks, st.sectorCount, 20f)

    bindings.txtRangeLabel.text = String.format(Locale.US, "±%.1f %% BEST LAP", settings.rangePct)
    val best = st.bestLapMs
    val range = if (best != null) best * settings.rangePct / 100.0 / 1000.0 else null
    if (range != null) {
        bindings.txtRangeNeg.text = String.format(Locale.US, "-%.2f s", range)
        bindings.txtRangePos.text = String.format(Locale.US, "+%.2f s", range)
    } else {
        bindings.txtRangeNeg.text = String.format(Locale.US, "-%.1f %%", settings.rangePct)
        bindings.txtRangePos.text = String.format(Locale.US, "+%.1f %%", settings.rangePct)
    }

    if (!st.isInvalid) {
        st.deltaSec?.let { deltaSum += it; deltaCount++ }
        val now = SystemClock.elapsedRealtime()
        if (now - deltaShownAt >= 200L) {
            deltaShownAt = now
            val avg = if (deltaCount > 0) deltaSum / deltaCount else null
            deltaSum = 0.0; deltaCount = 0
            renderDelta(avg, range)
        }
    } else {
        renderDelta(null, null)
    }

    if (reconName == null) {
        val name = circuit?.name ?: ""
        val prefix = if (demoActive) "DEMO ×$demoFactor · " else ""
        bindings.txtTop.text = if (st.running) "${prefix}LAP ${st.lapNumber} · S${st.sector}/${st.sectorCount} · $name" else "${prefix}$name · Cross FINISH to start"
    }
}

fun MainActivity.renderDelta(d: Double?, range: Double?) {
    if (d != null && range != null && range > 0.0) {
        val text = String.format(Locale.US, "%+.3f", d)
        val color = if (d <= 0.0) COL_GREEN else Color.parseColor("#EF4444")
        val ratio = (d / range).toFloat()
        bindings.txtDelta.text = text; bindings.txtDelta2.text = text; bindings.txtDelta3.text = text
        bindings.txtDelta.setTextColor(color); bindings.txtDelta2.setTextColor(color); bindings.txtDelta3.setTextColor(color)
        bindings.deltaBar.setRatio(ratio); bindings.deltaBar2.setRatio(ratio)
    } else {
        bindings.txtDelta.text = "—.—"; bindings.txtDelta2.text = "—.—"; bindings.txtDelta3.text = "—.—"
        bindings.txtDelta.setTextColor(COL_GREEN); bindings.txtDelta2.setTextColor(COL_GREEN); bindings.txtDelta3.setTextColor(COL_GREEN)
        bindings.deltaBar.setRatio(0f); bindings.deltaBar2.setRatio(0f)
    }
}

fun MainActivity.renderChips(row: LinearLayout, marks: List<SectorMark?>, count: Int, textSp: Float) {
    if ((row.tag as? Int) != count) {
        row.removeAllViews()
        for (i in 0 until count) {
            val tv = AppCompatTextView(this)
            tv.typeface = Typeface.DEFAULT_BOLD; tv.gravity = Gravity.CENTER; tv.maxLines = 1
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(tv, 8, textSp.toInt(), 1, TypedValue.COMPLEX_UNIT_SP)
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            if (i > 0) lp.marginStart = dp(6)
            row.addView(tv, lp)
        }
        row.tag = count
    }
    for (i in 0 until count) {
        val tv = row.getChildAt(i) as TextView
        val m = marks.getOrNull(i)
        val bg = GradientDrawable(); bg.cornerRadius = dp(8).toFloat()
        if (m == null) {
            bg.setColor(Color.parseColor("#1E242B"))
            tv.setTextColor(Color.parseColor("#9AA3AE"))
            tv.text = "S${i + 1} --"
        } else {
            val (fill, fg) = when (m.kind) {
                MarkKind.NEUTRAL -> Pair(Color.parseColor("#1E242B"), Color.WHITE)
                MarkKind.PURPLE -> Pair(Color.parseColor("#A855F7"), Color.WHITE)
                MarkKind.GREEN -> Pair(COL_GREEN, Color.parseColor("#0B0D10"))
                MarkKind.YELLOW -> Pair(Color.parseColor("#FACC15"), Color.parseColor("#0B0D10"))
            }
            bg.setColor(fill); tv.setTextColor(fg)
            tv.text = "S${i + 1} " + String.format(Locale.US, "%.2f", m.ms / 1000.0)
        }
        tv.background = bg
    }
}

fun MainActivity.resetDisplay() {
    bindings.txtLap.text = "--:--.---"; bindings.txtLast.text = "--:--.---"
    bindings.txtBest.text = "--:--.---"; bindings.txtOpt.text = "--:--.---"
    deltaSum = 0.0; deltaCount = 0; renderDelta(null, null)
    bindings.txtRangeLabel.text = String.format(Locale.US, "±%.1f %% BEST LAP", settings.rangePct)
    bindings.txtRangeNeg.text = String.format(Locale.US, "-%.1f %%", settings.rangePct)
    bindings.txtRangePos.text = String.format(Locale.US, "+%.1f %%", settings.rangePct)
    val n = circuit?.sectorCount ?: 3
    renderChips(bindings.chipsRow, emptyList(), n, 12f)
    renderChips(bindings.chipsRow2, emptyList(), n, 24f)
    renderChips(bindings.chipsRow3, emptyList(), n, 20f)
    bindings.txtLap3.text = "--:--.---"
    updateStintStrip()
}
