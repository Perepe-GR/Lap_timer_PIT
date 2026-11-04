package com.ejemplo.laptimer

import android.os.Handler
import java.util.Random
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Piloto virtual: genera posiciones a 25 Hz sobre el circuito demo, con ritmo distinto en cada
 * vuelta (±1-2 %) y algo de ruido de GPS. Permite acelerar la simulación (x1, x2, x4).
 * Arranca 5 metros antes de la salida de boxes (pit exit) sobre la trayectoria del pit_lane,
 * incorporándose de forma fluida a la pista principal sin disparar el estado de boxes al inicio.
 */
class DemoSource(
    private val handler: Handler,
    private val sink: (NmeaEvent.Position, Boolean) -> Unit
) {
    @Volatile
    var factor = 1
    private var running = false
    private var sTotal = 20.0
    private var lapIdx = -1
    private var pace = 1.0
    private var clockMs = 12 * 3_600_000L
    private val rnd = Random(7)

    @Volatile private var pitReq = false
    @Volatile private var pitPhase = 3 // Start accelerating out near pit exit
    private var stopSimMs = 10_000L
    private var stoppedMs = 0L
    private var pitDist = (DemoCircuit.PIT_LENGTH - 5.0).coerceAtLeast(0.0)
    private var pitV = 10.0
    private var rampV = 99.0
    private var mergeT = 0.0
    private var mergeStartX = 0.0
    private var mergeStartY = 0.0
    private var mergeStartHeading = 0.0

    val pitBusy: Boolean get() = pitReq || pitPhase != 0
    val inPitLane: Boolean get() = pitPhase == 1 || pitPhase == 2

    fun requestPit(stopMs: Long) {
        stopSimMs = stopMs
        pitReq = true
    }

    fun releasePit() {
        if (pitPhase == 2) stoppedMs = stopSimMs
    }

    private companion object {
        const val PIT_V = 8.0
        const val BOX_STOP_U = 60.0
    }

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            val f = factor
            for (i in 0 until f) step(i == f - 1)
            handler.postDelayed(this, 40)
        }
    }

    fun start() {
        running = true
        handler.post(tick)
    }

    fun stop() {
        running = false
        handler.removeCallbacks(tick)
    }

    private fun step(render: Boolean) {
        val idx = floor(sTotal / DemoCircuit.LENGTH).toInt()
        if (idx != lapIdx) {
            lapIdx = idx
            pace = 1.0 + (rnd.nextGaussian() * 0.012).coerceIn(-0.03, 0.03)
        }
        val lapBase = floor(sTotal / DemoCircuit.LENGTH) * DemoCircuit.LENGTH
        val local = sTotal - lapBase
        var v = DemoCircuit.speedMs(sTotal) * pace

        var inPitTrack = false
        var x = 0.0
        var y = 0.0
        var course = 0.0

        when (pitPhase) {
            0 -> {
                if (pitReq && local >= 685.0 && local < 710.0) {
                    pitPhase = 1
                    pitDist = 0.0
                    pitV = v
                    inPitTrack = true
                } else if (rampV < 90.0) {
                    rampV += 3.0 * 0.04
                    if (rampV >= v) rampV = 99.0 else v = rampV
                }
            }
            1 -> {
                inPitTrack = true
                val d = BOX_STOP_U - pitDist
                val vStop = sqrt(2.0 * 3.0 * max(d, 0.0))
                val target = min(PIT_V, vStop)
                pitV = if (pitV > target) max(target, pitV - 4.0 * 0.04) else min(target, pitV + 2.0 * 0.04)
                v = pitV
                pitDist += v * 0.04
                if (d <= 0.2 || pitDist >= BOX_STOP_U) {
                    v = 0.0
                    pitV = 0.0
                    stoppedMs = 0L
                    pitPhase = 2
                    pitDist = BOX_STOP_U
                }
            }
            2 -> {
                inPitTrack = true
                v = 0.0
                stoppedMs += 40L
                if (stoppedMs >= stopSimMs) {
                    pitV = 0.0
                    pitPhase = 3
                }
            }
            3 -> {
                inPitTrack = true
                pitV = min(PIT_V, pitV + 2.5 * 0.04)
                v = pitV
                pitDist += v * 0.04
                if (pitDist >= DemoCircuit.PIT_LENGTH) {
                    pitPhase = 4 // Smooth merge / tweening phase
                    mergeT = 0.0
                    val pEnd = DemoCircuit.pitPose(DemoCircuit.PIT_LENGTH)
                    mergeStartX = pEnd[0]
                    mergeStartY = pEnd[1]
                    mergeStartHeading = pEnd[2]
                    inPitTrack = false
                }
            }
            4 -> {
                // Smooth interpolation between pit lane exit and main circuit trace
                mergeT += 0.04 / 1.5 // 1.5 seconds transition
                v = 10.0
                if (mergeT >= 1.0) {
                    pitPhase = 0
                    pitReq = false
                    rampV = pitV
                    sTotal = lapBase + DemoCircuit.LENGTH + 20.0
                } else {
                    val pTarget = DemoCircuit.pose(lapBase + DemoCircuit.LENGTH + 20.0)
                    x = (1.0 - mergeT) * mergeStartX + mergeT * pTarget[0]
                    y = (1.0 - mergeT) * mergeStartY + mergeT * pTarget[1]
                    course = ((mergeStartHeading * (1.0 - mergeT) + pTarget[2] * mergeT) % 360.0 + 360.0) % 360.0
                }
            }
        }

        if (pitPhase != 4) {
            if (!inPitTrack) {
                sTotal += v * 0.04
            } else {
                sTotal += (v * 0.5) * 0.04
            }
            clockMs += 40

            val p = if (inPitTrack) DemoCircuit.pitPose(pitDist) else DemoCircuit.pose(sTotal)
            x = p[0] + rnd.nextGaussian() * 0.3
            y = p[1] + rnd.nextGaussian() * 0.3
            course = (((p[2] + rnd.nextGaussian()) % 360.0) + 360.0) % 360.0
        } else {
            clockMs += 40
            x += rnd.nextGaussian() * 0.3
            y += rnd.nextGaussian() * 0.3
        }

        val ev = NmeaEvent.Position(
            lat = DemoCircuit.frame.lat(y),
            lon = DemoCircuit.frame.lon(x),
            speedKmh = v * 3.6 + rnd.nextGaussian() * 0.3,
            courseDeg = course,
            timeMs = clockMs
        )
        sink(ev, render)
    }
}
