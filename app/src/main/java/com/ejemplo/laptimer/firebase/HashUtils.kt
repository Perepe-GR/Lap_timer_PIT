package com.ejemplo.laptimer.firebase

import java.security.MessageDigest
import java.util.UUID

/** Utility for generating salts and SHA-256 pin hashes according to CONTRATO-PANEL.md V2.1.9. */
object HashUtils {

    fun generateSalt(): String = UUID.randomUUID().toString().replace("-", "").take(16)

    fun sha256(salt: String, pin: String): String {
        val input = salt + pin
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
