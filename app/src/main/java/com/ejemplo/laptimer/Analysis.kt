package com.ejemplo.laptimer

/** Una vuelta extraída de la traza: distancia acumulada (m), tiempo (s), velocidad (km/h) y posición local. */
class LapTrace(
    val dist: DoubleArray,
    val time: DoubleArray,
    val speed: DoubleArray,
    val x: DoubleArray,
    val y: DoubleArray
)

object Analysis {
    fun slice(tr: Trace, startMs: Long, endMs: Long): LapTrace? {
        var i0 = -1
        var i1 = -1
        for (i in 0 until tr.size) {
            val t = tr.t[i]
            if (i0 < 0 && t >= startMs) i0 = i
            if (t <= endMs) i1 = i
        }
        if (i0 < 0 || i1 - i0 < 10) return null
        val n = i1 - i0 + 1
        val f = LocalFrame(tr.lat[i0], tr.lon[i0])
        val dist = DoubleArray(n)
        val time = DoubleArray(n)
        val speed = DoubleArray(n)
        val x = DoubleArray(n)
        val y = DoubleArray(n)
        for (k in 0 until n) {
            val i = i0 + k
            x[k] = f.x(tr.lon[i])
            y[k] = f.y(tr.lat[i])
            time[k] = (tr.t[i] - startMs) / 1000.0
            speed[k] = tr.v[i].toDouble()
            if (k > 0) dist[k] = dist[k - 1] + Math.hypot(x[k] - x[k - 1], y[k] - y[k - 1])
        }
        return LapTrace(dist, time, speed, x, y)
    }

    /** Interpolación lineal; xs debe ser no decreciente. */
    fun interp(xs: DoubleArray, ys: DoubleArray, x: Double): Double {
        if (x <= xs[0]) return ys[0]
        val last = xs.size - 1
        if (x >= xs[last]) return ys[last]
        var lo = 0
        var hi = last
        while (hi - lo > 1) {
            val mid = (lo + hi) ushr 1
            if (xs[mid] <= x) lo = mid else hi = mid
        }
        val d = xs[hi] - xs[lo]
        return if (d < 1e-9) ys[lo] else ys[lo] + (ys[hi] - ys[lo]) * (x - xs[lo]) / d
    }
}
