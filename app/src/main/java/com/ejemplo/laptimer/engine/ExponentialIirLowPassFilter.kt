package com.ejemplo.laptimer.engine

/**
 * Single-pole Exponential Infinite Impulse Response (IIR / EMA) Low-Pass Filter.
 * Formula: y[k] = alpha * x[k] + (1 - alpha) * y[k-1]
 * where alpha = dt / (RC + dt), RC = 1 / (2 * PI * cutoffHz)
 */
class ExponentialIirLowPassFilter(
    initialCutoffHz: Float = DEFAULT_CUTOFF_HZ
) {
    companion object {
        const val DEFAULT_CUTOFF_HZ = 3.0f
        const val MIN_CUTOFF_HZ = 1.0f
        const val MAX_CUTOFF_HZ = 10.0f
    }

    @Volatile
    var cutoffHz: Float = initialCutoffHz.coerceIn(MIN_CUTOFF_HZ, MAX_CUTOFF_HZ)
        set(value) {
            field = value.coerceIn(MIN_CUTOFF_HZ, MAX_CUTOFF_HZ)
        }

    private var initialized = false
    private var filteredValue = 0f

    @Synchronized
    fun filter(newValue: Float, deltaSec: Float): Float {
        if (!initialized || deltaSec <= 0f) {
            filteredValue = newValue
            initialized = true
            return filteredValue
        }

        val rc = (1.0f / (2.0f * Math.PI.toFloat() * cutoffHz))
        val alpha = (deltaSec / (rc + deltaSec)).coerceIn(0.001f, 1.0f)
        filteredValue = alpha * newValue + (1.0f - alpha) * filteredValue
        return filteredValue
    }

    @Synchronized
    fun reset(initialValue: Float = 0f) {
        filteredValue = initialValue
        initialized = false
    }

    @Synchronized
    fun getFilteredValue(): Float = filteredValue
}
