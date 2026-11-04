package com.ejemplo.laptimer.endurance.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class EnduranceModelsTest {

    @Test
    fun testTeamMetaRoundTrip() {
        val original = TeamMeta("EMPB-26", "Empuriabrava Racing", "circ_1", "user_1", 1700000000L)
        val map = original.toMap()
        val restored = teamMetaFromMap(map)
        assertEquals(original, restored)
    }

    @Test
    fun testStrategyBaseRoundTrip() {
        val original = StrategyBase(45, 12, 1.25, "Box on lap 12", 1700000000L)
        val map = original.toMap()
        val restored = strategyBaseFromMap(map)
        assertEquals(original, restored)
    }

    @Test
    fun testStrategyCurrentRoundTrip() {
        val original = StrategyCurrent(50, 15, 1.30, "Pace yourself", "leader_1", 1700000000L)
        val map = original.toMap()
        val restored = strategyCurrentFromMap(map)
        assertEquals(original, restored)
    }

    @Test
    fun testSessionStateRoundTrip() {
        val original = SessionState("RACING", "pilot_1", 1700000000L, 0L)
        val map = original.toMap()
        val restored = sessionStateFromMap(map)
        assertEquals(original, restored)
    }

    @Test
    fun testPilotInfoRoundTrip() {
        val original = PilotInfo("pilot_1", "Marc Marquez", "ON_TRACK", 1700000000L)
        val map = original.toMap()
        val restored = pilotInfoFromMap(map)
        assertEquals(original, restored)
    }

    @Test
    fun testLapRecordRoundTrip() {
        val original = LapRecord(1, 48500L, listOf(15200L, 16300L, 17000L), true, false, 1700000000L)
        val map = original.toMap()
        val restored = lapRecordFromMap(map)
        assertEquals(original, restored)
    }

    @Test
    fun testStintRoundTrip() {
        val original = Stint("pilot_1", 1700000000L, 1700002700L, 35)
        val map = original.toMap()
        val restored = stintFromMap(map)
        assertEquals(original, restored)
    }
}
