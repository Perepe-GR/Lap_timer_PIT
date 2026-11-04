package com.ejemplo.laptimer.engine

import com.ejemplo.laptimer.MarkKind

class SectorCalculator(private val n: Int, private val histBest: LongArray, private val sessBest: LongArray) {
    fun kindFor(i: Int, ms: Long): MarkKind {
        val h = histBest[i]
        val s = sessBest[i]
        val hasH = h != Long.MAX_VALUE
        val hasS = s != Long.MAX_VALUE
        if (!hasH && !hasS) return MarkKind.NEUTRAL
        val beatsH = !hasH || ms < h
        val beatsS = !hasS || ms < s
        return when {
            beatsH && beatsS -> MarkKind.PURPLE
            hasS && ms < s -> MarkKind.GREEN
            else -> MarkKind.YELLOW
        }
    }

    fun optimal(): Long? {
        var sum = 0L
        for (i in 0 until n) {
            val m = minOf(histBest[i], sessBest[i])
            if (m == Long.MAX_VALUE) return null
            sum += m
        }
        return sum
    }
}
