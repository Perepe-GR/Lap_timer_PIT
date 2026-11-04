package com.ejemplo.laptimer.engine

import com.ejemplo.laptimer.ReferenceLap

class DeltaCalculator {
    private var deltaSmooth: Double? = null

    fun calculate(rel: Double, x: Double, y: Double, ref: ReferenceLap?): Double? {
        if (ref == null) return null
        val refT = ref.timeAt(x, y) ?: return null
        val raw = rel - refT
        val s = deltaSmooth?.let { it * 0.7 + raw * 0.3 } ?: raw
        deltaSmooth = s
        return s / 1000.0
    }

    fun reset() {
        deltaSmooth = null
    }
}
