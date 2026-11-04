package com.ejemplo.laptimer.engine

class GapHandler {
    companion object {
        const val GAP_FREEZE_MS = 3000L
        const val GAP_INVALIDATE_MS = 5000L
    }

    private var totalOffsetMs = 0L
    private var lastT = 0L
    private var hasLast = false
    private var currentGapState = GapState.NONE

    enum class GapState { NONE, FREEZE, WARNING, INVALID }

    fun adjust(rawT: Long): Long {
        if (!hasLast) { lastT = rawT; hasLast = true; currentGapState = GapState.NONE; return rawT }
        val gap = rawT - lastT
        when {
            gap >= GAP_INVALIDATE_MS -> {
                totalOffsetMs += gap
                currentGapState = GapState.INVALID
            }
            gap >= GAP_FREEZE_MS -> {
                totalOffsetMs += gap
                currentGapState = GapState.WARNING
            }
            gap > 1000L -> {
                totalOffsetMs += gap
                currentGapState = GapState.FREEZE
            }
            else -> currentGapState = GapState.NONE
        }
        lastT = rawT
        return rawT - totalOffsetMs
    }

    fun state(): GapState = currentGapState

    fun reset() {
        totalOffsetMs = 0
        hasLast = false
        currentGapState = GapState.NONE
    }
}
