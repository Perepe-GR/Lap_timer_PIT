package com.ejemplo.laptimer.engine

/**
 * Timestamped, gravity-compensated, filtered G-force metrics in vehicle frame.
 * @param timestampNanos SystemClock.elapsedRealtimeNanos() timestamp.
 * @param gLongitudinal Acceleration (+) / Braking (-) in Gs along vehicle motion vector.
 * @param gLateral Cornering Left (-) / Cornering Right (+) in Gs.
 * @param gVertical Vertical bounce / Kerb impacts in Gs (0.0G = static gravity compensated).
 */
data class ImuTelemetryData(
    val timestampNanos: Long = System.currentTimeMillis() * 1_000_000L,
    val gLongitudinal: Float = 0f,
    val gLateral: Float = 0f,
    val gVertical: Float = 0f
)
