package com.ejemplo.laptimer.util

object ValidationUtils {
    /**
     * Checks that identifier (Team Name / Room Code) has between 6 and 15 alphanumeric characters with spaces (no special symbols).
     */
    fun validateIdentifier(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.length !in 6..15) return false
        return trimmed.all { it.isLetterOrDigit() || it.isWhitespace() }
    }

    /**
     * Checks that PIN is strictly numeric and between 4 and 6 digits.
     */
    fun validatePin(pin: String): Boolean {
        val trimmed = pin.trim()
        if (trimmed.length !in 4..6) return false
        return trimmed.all { it.isDigit() }
    }
}
