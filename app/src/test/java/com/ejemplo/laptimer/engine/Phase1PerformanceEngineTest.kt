package com.ejemplo.laptimer.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class Phase1PerformanceEngineTest {

    private lateinit var engine: Phase1PerformanceEngine

    @Before
    fun setUp() {
        engine = Phase1PerformanceEngine()
    }

    @Test
    fun testConsistencyScorePerfect() {
        engine.recordLap(60_000L, listOf(20_000L, 20_000L, 20_000L), 100f)
        engine.recordLap(60_000L, listOf(20_000L, 20_000L, 20_000L), 100f)
        val metrics = engine.computeMetrics(60_000L)
        assertEquals(100f, metrics.consistencyScore, 0.01f)
    }

    @Test
    fun testIdealExecutionScore() {
        engine.recordLap(62_000L, listOf(19_000L, 21_000L, 22_000L), 100f)
        engine.recordLap(60_000L, listOf(20_000L, 20_000L, 20_000L), 100f)
        // Best sectors: 19,000 + 20,000 + 20,000 = 59,000 ms ideal
        // Best lap: 60,000 ms
        val metrics = engine.computeMetrics(60_000L)
        assertEquals(59_000L, metrics.theoreticalIdealLapMs)
        val expectedExecution = (59_000f / 60_000f) * 100f
        assertEquals(expectedExecution, metrics.idealExecutionScore, 0.1f)
    }

    @Test
    fun testSmoothnessJerkRecording() {
        engine.recordJerk(0.1f, 0.2f, 0.02f)
        engine.recordJerk(0.2f, 0.3f, 0.02f)
        val metrics = engine.computeMetrics(60_000L)
        assertTrue(metrics.smoothnessScore > 0f)
    }

    @Test
    fun testReset() {
        engine.recordLap(60_000L, listOf(20_000L, 20_000L, 20_000L), 100f)
        engine.reset()
        val metrics = engine.computeMetrics(60_000L)
        assertEquals(100f, metrics.consistencyScore, 0.01f)
        assertEquals(0L, metrics.theoreticalIdealLapMs)
    }
}
