package com.ejemplo.laptimer.engine

class GpsFilter {
    private var lastRaw: Long? = null
    private var dayOffset = 0L
    private var prevLat: Double? = null
    private var prevLon: Double? = null
    private var prevMs: Long? = null
    private var maxSessionKmh = 0.0

    fun filterTime(rawTimeMs: Long): Long {
        val prevRaw = lastRaw
        if (prevRaw != null && rawTimeMs < prevRaw - 43_200_000L) dayOffset += 86_400_000L
        lastRaw = rawTimeMs
        return rawTimeMs + dayOffset
    }

    fun isFixPlausible(lat: Double, lon: Double, speedKmh: Double, timeMs: Long): Boolean {
        // Hard ceiling at 400.0 km/h
        if (speedKmh > 400.0) return false

        // Track max session speed dynamically
        if (speedKmh > maxSessionKmh) {
            maxSessionKmh = speedKmh
        }

        val lastLat = prevLat
        val lastLon = prevLon
        val lastT = prevMs
        if (lastLat != null && lastLon != null && lastT != null) {
            val dtMs = timeMs - lastT
            if (dtMs in 1..400) {
                // Reject extreme spatial teleportation jumps (> 150m in < 400ms)
                val dist = distanceM(lastLat, lastLon, lat, lon)
                if (dist > 150.0) return false
            }
        }
        prevLat = lat
        prevLon = lon
        prevMs = timeMs
        return true
    }

    fun reset() {
        lastRaw = null
        dayOffset = 0L
        prevLat = null
        prevLon = null
        prevMs = null
        maxSessionKmh = 0.0
    }

    private fun distanceM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }
}
