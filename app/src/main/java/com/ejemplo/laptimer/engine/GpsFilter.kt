package com.ejemplo.laptimer.engine

class GpsFilter {
    private var lastRaw: Long? = null
    private var dayOffset = 0L

    fun filterTime(rawTimeMs: Long): Long {
        val prevRaw = lastRaw
        if (prevRaw != null && rawTimeMs < prevRaw - 43_200_000L) dayOffset += 86_400_000L
        lastRaw = rawTimeMs
        return rawTimeMs + dayOffset
    }

    fun reset() {
        lastRaw = null
        dayOffset = 0L
    }
}
