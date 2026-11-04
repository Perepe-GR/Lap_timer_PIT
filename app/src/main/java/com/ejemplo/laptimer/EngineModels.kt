package com.ejemplo.laptimer

import com.ejemplo.laptimer.engine.GapHandler

/** Línea de paso: segmento a-b perpendicular al rumbo, más la dirección de marcha (dx, dy). */
internal class Gate(
    val ax: Double, val ay: Double,
    val bx: Double, val by: Double,
    val dx: Double, val dy: Double
)

/** Muestra de la vuelta de referencia: tiempo (ms desde el inicio de vuelta) y posición local. */
class RefSample(val t: Long, val x: Double, val y: Double)

/** Color de un sector tipo F1. */
enum class MarkKind { NEUTRAL, PURPLE, GREEN, YELLOW }

enum class GapFlag { NONE, WARNING, INVALID }

class SectorMark(val ms: Long, val kind: MarkKind)

class LapResult(
    val number: Int,
    val lapMs: Long,
    val splits: List<Long>,
    val startMs: Long,
    val endMs: Long,
    val isBest: Boolean,
    val trace: List<RefSample>,
    /** Diferencia con la mejor vuelta anterior (negativo = más rápida); null si era la primera. */
    val deltaMs: Long? = null,
    /** Colores de los sectores de esta vuelta (el último es el que cierra en meta). */
    val marks: List<SectorMark?> = emptyList(),
    /** 0 = vuelta normal, 1 = OUT LAP (desde la salida del PIT), 2 = IN LAP (hasta la entrada al PIT). */
    val kind: Int = 0,
    val gapFlag: GapFlag = GapFlag.NONE
)

class PersistData(val bestLapMs: Long?, val bestSectors: LongArray, val ref: List<RefSample>?)

data class EngineState(
    val running: Boolean,
    val lapNumber: Int,
    val sector: Int,
    val sectorCount: Int,
    val currentLapMs: Long?,
    val lastLapMs: Long?,
    val bestLapMs: Long?,
    val optimalMs: Long?,
    val deltaSec: Double?,
    val speedKmh: Double,
    val gpsTimeMs: Long,
    val marks: List<SectorMark?>,
    val isInvalid: Boolean = false,
    val gapState: GapHandler.GapState = GapHandler.GapState.NONE
)
