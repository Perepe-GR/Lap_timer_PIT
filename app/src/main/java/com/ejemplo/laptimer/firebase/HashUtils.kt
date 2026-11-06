package com.ejemplo.laptimer.firebase

import com.ejemplo.laptimer.util.SecurityUtils

/** Utility delegating to SecurityUtils for generating salts and SHA-256 pin hashes. */
object HashUtils {

    fun generateSalt(): String = SecurityUtils.generateSalt()

    fun sha256(salt: String, pin: String): String = SecurityUtils.sha256(salt, pin)
}
