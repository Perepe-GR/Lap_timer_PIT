package com.ejemplo.laptimer.engine

import com.ejemplo.laptimer.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class LapDetector(private val circuit: Circuit) {
    internal val frame = LocalFrame(circuit.finish.lat, circuit.finish.lon)
    internal val finishGate = makeGate(circuit.finish)
    internal val pitInGate: Gate? = if (circuit.hasPitLane) circuit.pitIn?.let { makeGate(it) } else null
    internal val pitOutGate: Gate? = if (circuit.hasPitLane) circuit.pitOut?.let { makeGate(it) } else null
    internal val sequence: List<Gate> = circuit.sectors.map { makeGate(it) } + finishGate
    internal val n = sequence.size

    internal fun makeGate(p: GatePoint): Gate {
        val x = frame.x(p.lon)
        val y = frame.y(p.lat)
        val h = Math.toRadians(p.headingDeg)
        val dx = sin(h)
        val dy = cos(h)
        val lx = -dy * p.leftM
        val ly = dx * p.leftM
        val rx = dy * p.rightM
        val ry = -dx * p.rightM
        return Gate(x + lx, y + ly, x + rx, y + ry, dx, dy)
    }

    internal fun crossing(g: Gate, x1: Double, y1: Double, x2: Double, y2: Double): Double? {
        val rx = x2 - x1
        val ry = y2 - y1
        val sx = g.bx - g.ax
        val sy = g.by - g.ay
        val den = rx * sy - ry * sx
        if (abs(den) < 1e-9) return null
        val t = ((g.ax - x1) * sy - (g.ay - y1) * sx) / den
        val u = ((g.ax - x1) * ry - (g.ay - y1) * rx) / den
        if (t < 0.0 || t > 1.0 || u < 0.0 || u > 1.0) return null
        if (rx * g.dx + ry * g.dy <= 0.0) return null
        return t
    }

    private var lastPitCrossingMs = 0L

    fun checkPitCrossing(inPit: Boolean, x1: Double, y1: Double, x2: Double, y2: Double, nowMs: Long = System.currentTimeMillis()): Pair<Boolean, Double>? {
        if (nowMs - lastPitCrossingMs < 3000L) return null
        val pin = pitInGate ?: return null
        val pout = pitOutGate ?: return null
        if (!inPit) {
            val fIn = crossing(pin, x1, y1, x2, y2)
            if (fIn != null) {
                lastPitCrossingMs = nowMs
                return Pair(true, fIn)
            }
        } else {
            val fOut = crossing(pout, x1, y1, x2, y2)
            if (fOut != null) {
                lastPitCrossingMs = nowMs
                return Pair(false, fOut)
            }
        }
        return null
    }

    fun checkGateCrossing(running: Boolean, expected: Int, x1: Double, y1: Double, x2: Double, y2: Double): Pair<Int, Double>? {
        val gate = if (running) sequence[expected] else finishGate
        val f = crossing(gate, x1, y1, x2, y2)
        if (f != null) return Pair(if (running) expected else -1, f)
        if (running && expected < n - 1) {
            val f2 = crossing(finishGate, x1, y1, x2, y2)
            if (f2 != null) return Pair(-1, f2)
        }
        return null
    }
}
