package com.ejemplo.laptimer.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class GapHandlerTest {

    @Test
    fun testNormalGaps() {
        val handler = GapHandler()
        val t1 = handler.adjust(1000L)
        assertEquals(1000L, t1)
        assertEquals(GapHandler.GapState.NONE, handler.state())

        val t2 = handler.adjust(2000L) // 1s gap
        assertEquals(2000L, t2)
        assertEquals(GapHandler.GapState.NONE, handler.state())
    }

    @Test
    fun testFreezeGap() {
        val handler = GapHandler()
        handler.adjust(1000L)
        val t2 = handler.adjust(2500L) // 1.5s gap (> 1s, <= 3s)
        assertEquals(1000L, t2)
        assertEquals(GapHandler.GapState.FREEZE, handler.state())
    }

    @Test
    fun testWarningGap() {
        val handler = GapHandler()
        handler.adjust(1000L)
        val t2 = handler.adjust(4500L) // 3.5s gap (3s - 5s)
        assertEquals(1000L, t2)
        assertEquals(GapHandler.GapState.WARNING, handler.state())
    }

    @Test
    fun testInvalidateGap() {
        val handler = GapHandler()
        handler.adjust(1000L)
        val t2 = handler.adjust(7000L) // 6s gap (>= 5s)
        assertEquals(1000L, t2)
        assertEquals(GapHandler.GapState.INVALID, handler.state())
    }

    @Test
    fun testReset() {
        val handler = GapHandler()
        handler.adjust(1000L)
        handler.adjust(7000L)
        handler.reset()
        val t = handler.adjust(10000L)
        assertEquals(10000L, t)
        assertEquals(GapHandler.GapState.NONE, handler.state())
    }
}
