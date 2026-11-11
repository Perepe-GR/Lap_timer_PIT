package com.ejemplo.laptimer.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImuSensorManagerTest {

    @Test
    fun testImuTelemetryDataDefaults() {
        val data = ImuTelemetryData()
        assertEquals(0f, data.gLongitudinal, 0.001f)
        assertEquals(0f, data.gLateral, 0.001f)
        assertEquals(0f, data.gVertical, 0.001f)
        assertTrue(data.timestampNanos > 0L)
    }

    @Test
    fun testImuTelemetryDataCustomValues() {
        val data = ImuTelemetryData(
            timestampNanos = 1_000_000_000L,
            gLongitudinal = 0.85f,
            gLateral = -1.20f,
            gVertical = 0.05f
        )
        assertEquals(1_000_000_000L, data.timestampNanos)
        assertEquals(0.85f, data.gLongitudinal, 0.001f)
        assertEquals(-1.20f, data.gLateral, 0.001f)
        assertEquals(0.05f, data.gVertical, 0.001f)
    }
}
