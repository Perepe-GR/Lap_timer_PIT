package com.ejemplo.laptimer.util

import java.security.MessageDigest
import java.security.SecureRandom

/** Data class holding computed SHA-256 hash and salt. */
data class PinHashResult(
    val pinHash: String,
    val salt: String
)

/** Security utility for generating secure salts, computing PIN hashes and verifying credentials. */
object SecurityUtils {

    /**
     * Generates a secure random salt and computes SHA-256(salt + pin).
     */
    fun hashPin(pin: String): PinHashResult {
        val salt = generateSalt()
        val pinHash = sha256(salt, pin.trim())
        return PinHashResult(pinHash = pinHash, salt = salt)
    }

    /**
     * Verifies that the entered PIN matches the stored salt and pinHash.
     */
    fun verifyPin(inputPin: String, storedSalt: String, storedPinHash: String): Boolean {
        if (storedPinHash.isBlank()) return false
        val computedHash = sha256(storedSalt, inputPin.trim())
        return computedHash.equals(storedPinHash, ignoreCase = true)
    }

    fun generateSalt(): String {
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun sha256(salt: String, pin: String): String {
        val input = salt + pin.trim()
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
