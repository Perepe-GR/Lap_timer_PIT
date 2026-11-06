package com.ejemplo.laptimer

import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryEstimatorTest {

    @Test
    fun testFormatRemainingTimeWhenCharging() {
        val info = BatteryInfo(
            percentage = 85,
            isCharging = true,
            remainingMinutes = -1,
            dischargeRatePerMinute = 0.0,
            dischargeCurrentMah = 0.0
        )
        val formatted = BatteryEstimator.formatRemainingTime(info)
        assertEquals("--", formatted)
    }

    @Test
    fun testFormatRemainingTimeWithHoursAndMinutes() {
        val info = BatteryInfo(
            percentage = 65,
            isCharging = false,
            remainingMinutes = 260, // 4h 20m
            dischargeRatePerMinute = 0.25,
            dischargeCurrentMah = 450.0
        )
        val formatted = BatteryEstimator.formatRemainingTime(info)
        assertEquals("4h 20m", formatted)
    }

    @Test
    fun testFormatRemainingTimeWithOnlyMinutes() {
        val info = BatteryInfo(
            percentage = 15,
            isCharging = false,
            remainingMinutes = 45,
            dischargeRatePerMinute = 0.33,
            dischargeCurrentMah = 600.0
        )
        val formatted = BatteryEstimator.formatRemainingTime(info)
        assertEquals("45m", formatted)
    }

    @Test
    fun testFormatRemainingTimeWhenZeroOrNegative() {
        val info = BatteryInfo(
            percentage = 0,
            isCharging = false,
            remainingMinutes = 0,
            dischargeRatePerMinute = 0.0,
            dischargeCurrentMah = 0.0
        )
        val formatted = BatteryEstimator.formatRemainingTime(info)
        assertEquals("--", formatted)
    }
}
