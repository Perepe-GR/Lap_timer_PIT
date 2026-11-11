package com.ejemplo.laptimer.engine

import kotlin.math.sqrt

data class Phase1Metrics(
    val consistencyScore: Float = 100f,
    val smoothnessScore: Float = 0f, // Jerk G/s
    val idealExecutionScore: Float = 100f,
    val theoreticalIdealLapMs: Long = 0L,
    val brakingDeviationMeters: Float = 0f
)

class Phase1PerformanceEngine {

    private val lapTimes = mutableListOf<Long>()
    private val sectorBestMap = mutableMapOf<Int, Long>()
    private val brakingPoints = mutableListOf<Float>()
    private val jerkReadings = mutableListOf<Float>()

    fun recordLap(lapMs: Long, sectors: List<Long>, brakingPointMeters: Float) {
        if (lapMs > 0L) lapTimes.add(lapMs)
        if (brakingPointMeters > 0f) brakingPoints.add(brakingPointMeters)
        sectors.forEachIndexed { idx, ms ->
            if (ms > 0L) {
                val currentBest = sectorBestMap[idx] ?: Long.MAX_VALUE
                if (ms < currentBest) {
                    sectorBestMap[idx] = ms
                }
            }
        }
    }

    fun recordJerk(ax: Float, ay: Float, dtSec: Float) {
        if (dtSec <= 0f) return
        val jerk = sqrt(ax * ax + ay * ay) / dtSec
        jerkReadings.add(jerk)
        if (jerkReadings.size > 200) jerkReadings.removeAt(0)
    }

    fun computeMetrics(bestLapMs: Long): Phase1Metrics {
        val consistency = computeConsistencyScore()
        val smoothness = if (jerkReadings.isNotEmpty()) jerkReadings.average().toFloat() else 0f
        val idealLap = sectorBestMap.values.sum()
        val execution = if (bestLapMs > 0L && idealLap > 0L) {
            (idealLap.toFloat() / bestLapMs.toFloat() * 100f).coerceIn(0f, 100f)
        } else {
            100f
        }
        val brakingDev = computeBrakingDeviation()

        return Phase1Metrics(
            consistencyScore = consistency,
            smoothnessScore = smoothness,
            idealExecutionScore = execution,
            theoreticalIdealLapMs = idealLap,
            brakingDeviationMeters = brakingDev
        )
    }

    private fun computeConsistencyScore(): Float {
        if (lapTimes.size < 2) return 100f
        val mean = lapTimes.average()
        val variance = lapTimes.map { (it - mean) * (it - mean) }.average()
        val stdDev = sqrt(variance)
        if (mean == 0.0) return 100f
        val ratio = (stdDev / mean).toFloat()
        return (100f * (1f - ratio)).coerceIn(0f, 100f)
    }

    private fun computeBrakingDeviation(): Float {
        if (brakingPoints.size < 2) return 0f
        val mean = brakingPoints.average()
        val variance = brakingPoints.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance).toFloat()
    }

    fun reset() {
        lapTimes.clear()
        sectorBestMap.clear()
        brakingPoints.clear()
        jerkReadings.clear()
    }
}
