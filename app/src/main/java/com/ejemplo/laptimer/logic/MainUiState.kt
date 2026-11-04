package com.ejemplo.laptimer.logic

import com.ejemplo.laptimer.MainActivity
import com.ejemplo.laptimer.NetSettings
import com.ejemplo.laptimer.firebase.SwapRequest

internal class ActiveMsg(val text: String, val bg: String, var remaining: Int, val at: Long)

internal class StintState(
    var number: Int,
    var driver: String,
    var lapsDone: Int,
    var startEpoch: Long,
    var plan: Int,
    var minMin: Int,
    var maxMin: Int,
    var minLaps: Int = 0,
    var maxLaps: Int = 0,
    var planMin: Int = 0,
    var planHitLaps: Int = 0
)

internal enum class StintHealth { NONE, RED, GREEN, PURPLE }

internal fun MainActivity.loadStint(): StintState {
    val n = settings.stintNumber
    val t = settings.targetFor(n)
    fun pick(v: Int, d: Int) = if (v >= 0) v else d
    return StintState(
        n,
        settings.driverName,
        settings.stintLapsDone,
        settings.stintStartEpoch,
        pick(settings.stintPlan, t.planLaps),
        pick(settings.stintMin, t.minMin),
        pick(settings.stintMax, t.maxMin),
        pick(settings.stintMinLaps, t.minLaps),
        pick(settings.stintMaxLaps, t.maxLaps),
        pick(settings.stintPlanMin, t.planMin)
    )
}

fun MainActivity.saveStint() {
    if (demoActive) return
    val s = stint
    settings.stintNumber = s.number
    settings.stintLapsDone = s.lapsDone
    settings.stintStartEpoch = s.startEpoch
    settings.stintPlan = s.plan
    settings.stintMin = s.minMin
    settings.stintMax = s.maxMin
    settings.stintMinLaps = s.minLaps
    settings.stintMaxLaps = s.maxLaps
    settings.stintPlanMin = s.planMin
    settings.driverName = s.driver
}

fun MainActivity.stintElapsedMs(): Long {
    val s = stint
    return if (s.startEpoch > 0L) clockNow() - s.startEpoch else 0L
}

internal fun MainActivity.stintHealth(): StintHealth {
    val s = stint
    val el = stintElapsedMs()
    val hasLimits = s.minMin > 0 || s.minLaps > 0 || s.maxMin > 0 || s.maxLaps > 0
    val tooShort = (s.minMin > 0 && el < s.minMin * 60_000L) || (s.minLaps > 0 && s.lapsDone < s.minLaps)
    val tooLong = (s.maxMin > 0 && el > s.maxMin * 60_000L) || (s.maxLaps > 0 && s.lapsDone > s.maxLaps)
    val onTarget = (s.plan > 0 && s.lapsDone == s.plan) ||
        (s.planMin > 0 && s.planHitLaps > 0 && s.lapsDone == s.planHitLaps)
    return when {
        tooShort || tooLong -> StintHealth.RED
        onTarget -> StintHealth.PURPLE
        hasLimits -> StintHealth.GREEN
        else -> StintHealth.NONE
    }
}

fun MainActivity.isFinalLap(): Boolean {
    if (!settings.isEndurance() || reconName != null) return false
    val s = stint
    if (s.plan > 0 && s.lapsDone == s.plan - 1) return true
    if (s.planMin > 0 && s.planHitLaps == 0 && s.startEpoch > 0L) {
        val ref = lastState?.lastLapMs ?: lastState?.bestLapMs ?: 0L
        if (ref > 0L && stintElapsedMs() + ref >= s.planMin * 60_000L) return true
    }
    return false
}

fun MainActivity.scopeKey(): String =
    if (settings.refScope == "piloto") "p_" + NetSettings.keyOf(stint.driver) else ""
