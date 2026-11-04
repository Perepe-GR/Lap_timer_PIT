package com.ejemplo.laptimer

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sqrt

/** Marco plano local (metros) centrado en un punto. Válido a escala de circuito. */
class LocalFrame(lat0: Double, private val lon0: Double) {
    private val latRef = lat0
    private val kx = 111_320.0 * cos(Math.toRadians(lat0))
    private val ky = 110_574.0

    fun x(lon: Double): Double = (lon - lon0) * kx
    fun y(lat: Double): Double = (lat - latRef) * ky
    fun lon(x: Double): Double = lon0 + x / kx
    fun lat(y: Double): Double = latRef + y / ky
}

fun distanceM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val f = LocalFrame(lat1, lon1)
    val dx = f.x(lon2)
    val dy = f.y(lat2)
    return sqrt(dx * dx + dy * dy)
}

/** Rumbo en grados (0 = norte, sentido horario) del punto 1 al punto 2. */
fun bearingDeg(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val f = LocalFrame(lat1, lon1)
    val d = Math.toDegrees(atan2(f.x(lon2), f.y(lat2)))
    return (d + 360.0) % 360.0
}

fun fmtMs(ms: Long?): String {
    if (ms == null) return "--:--.---"
    val min = ms / 60_000
    val sec = (ms % 60_000) / 1000
    val milli = ms % 1000
    return String.format(java.util.Locale.US, "%d:%02d.%03d", min, sec, milli)
}
