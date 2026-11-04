package com.ejemplo.laptimer

import kotlin.math.cos
import kotlin.math.sin

internal class Pt(val x: Float, val y: Float, val s: Int)

internal fun kindColor(k: MarkKind?, colPurple: Int, colGreen: Int, colYellow: Int, colNeutral: Int): Int = when (k) {
    MarkKind.PURPLE -> colPurple
    MarkKind.GREEN -> colGreen
    MarkKind.YELLOW -> colYellow
    else -> colNeutral
}

internal fun gateEnds(g: GatePoint, f: LocalFrame): FloatArray {
    val x = f.x(g.lon)
    val y = f.y(g.lat)
    val h = Math.toRadians(g.headingDeg)
    val lx = sin(h - Math.PI / 2)
    val ly = cos(h - Math.PI / 2)
    return floatArrayOf(
        (x + lx * g.leftM).toFloat(), (y + ly * g.leftM).toFloat(),
        (x - lx * g.rightM).toFloat(), (y - ly * g.rightM).toFloat()
    )
}
