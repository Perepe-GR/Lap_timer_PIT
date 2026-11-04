package com.ejemplo.laptimer.endurance.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class EnduranceRepositoryTest {

    @Test
    fun testStrategySerialization() {
        val strategy = StrategyBase(40, 10, 1.2, "Strategy test", 1700000000L)
        val map = strategy.toMap()
        val restored = strategyBaseFromMap(map)
        assertEquals(strategy.stintMinutes, restored.stintMinutes)
        assertEquals(strategy.pitWindowLaps, restored.pitWindowLaps)
        assertEquals(strategy.notes, restored.notes)
    }

    @Test
    fun testLapRecordValidation() {
        val lap = LapRecord(5, 47200L, listOf(15000L, 16100L, 16100L), true, false, 1700000000L)
        assertEquals(5, lap.lapNumber)
        assertEquals(true, lap.valid)
        assertEquals(47200L, lap.timeMs)
    }
}
