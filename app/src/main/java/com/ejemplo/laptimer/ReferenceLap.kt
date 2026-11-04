package com.ejemplo.laptimer

import kotlin.math.hypot

/**
 * Vuelta de referencia (la mejor). Dada una posición, devuelve en qué instante de la
 * vuelta pasó la referencia por el punto más cercano de su trazado.
 */
class ReferenceLap(samples: List<RefSample>) {
    private val n = samples.size
    private val xs = DoubleArray(n) { samples[it].x }
    private val ys = DoubleArray(n) { samples[it].y }
    private val ts = LongArray(n) { samples[it].t }
    private val cum = DoubleArray(n) // distancia acumulada a lo largo de la traza (m)
    private var last = 0
    private var misses = 0

    init {
        for (i in 1 until n) {
            cum[i] = cum[i - 1] + hypot(xs[i] - xs[i - 1], ys[i] - ys[i - 1])
        }
    }

    fun reset() {
        last = 0
        misses = 0
    }

    fun timeAt(x: Double, y: Double): Double? {
        if (n < 2) return null

        var lo = last
        var hi = last
        if (misses > 50) {
            // Perdido (trompo, salida de pista): búsqueda global
            lo = 0
            hi = n - 2
        } else {
            // Ventana: 15 m hacia atrás y 80 m hacia delante de la última posición
            while (lo > 0 && cum[last] - cum[lo - 1] < 15.0) lo--
            while (hi < n - 2 && cum[hi + 1] - cum[last] < 80.0) hi++
        }
        if (hi > n - 2) hi = n - 2

        var bestD = Double.MAX_VALUE
        var bestT = 0.0
        var bestIdx = last
        for (i in lo..hi) {
            val sx = xs[i + 1] - xs[i]
            val sy = ys[i + 1] - ys[i]
            val len2 = sx * sx + sy * sy
            val u = if (len2 < 1e-9) 0.0
            else (((x - xs[i]) * sx + (y - ys[i]) * sy) / len2).coerceIn(0.0, 1.0)
            val px = xs[i] + u * sx
            val py = ys[i] + u * sy
            val d = hypot(x - px, y - py)
            if (d < bestD) {
                bestD = d
                bestT = ts[i] + u * (ts[i + 1] - ts[i])
                bestIdx = i
            }
        }

        if (bestD > 40.0) {
            misses++
            return null
        }
        misses = 0
        last = bestIdx
        return bestT
    }
}
