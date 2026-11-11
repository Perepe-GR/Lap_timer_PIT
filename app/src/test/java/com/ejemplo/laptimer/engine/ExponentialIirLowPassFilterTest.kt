package com.ejemplo.laptimer.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class ExponentialIirLowPassFilterTest {

    private lateinit var filter: ExponentialIirLowPassFilter

    @Before
    fun setUp() {
        filter = ExponentialIirLowPassFilter(initialCutoffHz = 3.0f)
    }

    @Test
    fun testInitialValueInitialization() {
        val result = filter.filter(1.5f, 0.02f)
        assertEquals(1.5f, result, 0.001f)
    }

    @Test
    fun testHighFrequencyNoiseFiltering() {
        filter.filter(0.0f, 0.02f)

        val dt = 0.02f
        var maxFilteredNoise = 0f

        for (i in 1..100) {
            val noise = 0.5f + if (i % 2 == 0) 1.0f else -1.0f
            val filtered = filter.filter(noise, dt)
            if (i > 20) {
                val dev = abs(filtered - 0.5f)
                if (dev > maxFilteredNoise) maxFilteredNoise = dev
            }
        }

        assertTrue("High frequency noise should be attenuated below 0.3G, was $maxFilteredNoise", maxFilteredNoise < 0.3f)
    }

    @Test
    fun testStepResponseConvergence() {
        filter.filter(0.0f, 0.02f)

        val dt = 0.02f
        var lastFiltered = 0.0f

        repeat(100) {
            lastFiltered = filter.filter(2.0f, dt)
        }

        assertEquals(2.0f, lastFiltered, 0.05f)
    }

    @Test
    fun testCutoffFrequencyRangeCoercion() {
        filter.cutoffHz = 0.5f
        assertEquals(ExponentialIirLowPassFilter.MIN_CUTOFF_HZ, filter.cutoffHz, 0.001f)

        filter.cutoffHz = 20.0f
        assertEquals(ExponentialIirLowPassFilter.MAX_CUTOFF_HZ, filter.cutoffHz, 0.001f)
    }

    @Test
    fun testReset() {
        filter.filter(5.0f, 0.02f)
        filter.reset(0.0f)
        assertEquals(0.0f, filter.getFilteredValue(), 0.001f)

        val newStart = filter.filter(1.0f, 0.02f)
        assertEquals(1.0f, newStart, 0.001f)
    }
}
