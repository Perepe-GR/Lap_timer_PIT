package com.ejemplo.laptimer

sealed class NmeaEvent {
    data class Position(
        val lat: Double,
        val lon: Double,
        val speedKmh: Double,
        val courseDeg: Double?,
        val timeMs: Long
    ) : NmeaEvent()

    data class Fix(val sats: Int, val hdop: Double, val alt: Double?) : NmeaEvent()
}

/**
 * Parsea sentencias NMEA (RMC y GGA) del receptor u-blox.
 * Coordenadas en formato ddmm.mmmm (lat) y dddmm.mmmm (lon).
 */
class NmeaParser {

    fun parse(sentence: String): NmeaEvent? {
        if (sentence.isEmpty() || sentence[0] != '$') return null

        val star = sentence.indexOf('*')
        val body = if (star >= 0) sentence.substring(1, star) else sentence.substring(1)

        if (star >= 0 && sentence.length >= star + 3) {
            val expected = sentence.substring(star + 1, star + 3).toIntOrNull(16)
            if (expected != null && checksum(body) != expected) return null
        }

        val f = body.split(',')
        val type = f[0]
        return when {
            type.endsWith("RMC") -> parseRmc(f)
            type.endsWith("GGA") -> parseGga(f)
            else -> null
        }
    }

    private fun parseRmc(f: List<String>): NmeaEvent? {
        if (f.size < 8) return null
        if (f[2] != "A") return null // A = posición válida

        val lat = parseCoord(f[3], 2, f[4]) ?: return null
        val lon = parseCoord(f[5], 3, f[6]) ?: return null
        val knots = f[7].toDoubleOrNull() ?: 0.0
        val course = f.getOrNull(8)?.toDoubleOrNull() // vacío si está parado
        val timeMs = parseTime(f[1]) ?: return null

        return NmeaEvent.Position(
            lat = lat,
            lon = lon,
            speedKmh = knots * 1.852,
            courseDeg = course,
            timeMs = timeMs
        )
    }

    private fun parseGga(f: List<String>): NmeaEvent? {
        if (f.size < 10) return null
        val sats = f[7].toIntOrNull() ?: return null
        val hdop = f[8].toDoubleOrNull() ?: return null
        val alt = f[9].toDoubleOrNull()
        return NmeaEvent.Fix(sats, hdop, alt)
    }

    private fun parseCoord(value: String, degDigits: Int, hemi: String): Double? {
        if (value.length <= degDigits) return null
        val deg = value.substring(0, degDigits).toIntOrNull() ?: return null
        val min = value.substring(degDigits).toDoubleOrNull() ?: return null
        var result = deg + min / 60.0
        if (hemi == "S" || hemi == "W") result = -result
        return result
    }

    /** "212946.68" -> milisegundos desde medianoche UTC. */
    private fun parseTime(t: String): Long? {
        if (t.length < 6) return null
        val h = t.substring(0, 2).toIntOrNull() ?: return null
        val m = t.substring(2, 4).toIntOrNull() ?: return null
        val s = t.substring(4).toDoubleOrNull() ?: return null
        return ((h * 3600 + m * 60) * 1000L) + Math.round(s * 1000)
    }

    private fun checksum(s: String): Int {
        var c = 0
        for (ch in s) c = c xor ch.code
        return c
    }
}
