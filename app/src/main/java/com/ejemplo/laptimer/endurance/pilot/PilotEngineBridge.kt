package com.ejemplo.laptimer.endurance.pilot

import android.content.Context
import com.ejemplo.laptimer.*
import com.ejemplo.laptimer.endurance.sync.CurrentLap
import com.ejemplo.laptimer.endurance.sync.LapRecord

class PilotEngineBridge(
    private val context: Context,
    private val circuit: Circuit,
    private val onLap: (LapRecord) -> Unit,
    private val onCurrentLap: (CurrentLap) -> Unit,
    private val onGpsFix: (Double, Boolean) -> Unit
) {
    private var engine: LapEngine? = null
    private var lapCount = 0

    fun start() {
        stop()
        val e = LapEngine(circuit)
        e.onLap = { res ->
            lapCount++
            val rec = LapRecord(
                lapNumber = lapCount,
                timeMs = res.lapMs,
                sectorsMs = res.splits,
                valid = !res.isBest || res.lapMs > 10000L,
                recordedAt = System.currentTimeMillis()
            )
            onLap(rec)
        }
        engine = e
    }

    fun onFix(lat: Double, lon: Double, speedKmh: Double, timeMs: Long) {
        val e = engine ?: return
        val st = e.onFix(lat, lon, speedKmh, timeMs)
        onGpsFix(speedKmh, !st.isInvalid)

        val current = CurrentLap(
            startTs = timeMs - (st.currentLapMs ?: 0L),
            sectorsMs = st.marks.mapNotNull { it?.ms },
            gpsValid = !st.isInvalid
        )
        onCurrentLap(current)
    }

    fun stop() {
        engine?.resetSession()
        engine = null
    }
}
