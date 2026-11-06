package com.ejemplo.laptimer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSecurityValidationTest {

    @Test
    fun testValidIdentifiers() {
        assertTrue(ValidationUtils.isValidIdentifier("Torque Team"))
        assertTrue(ValidationUtils.isValidIdentifier("RedBull 1"))
        assertTrue(ValidationUtils.isValidIdentifier("TEAM 12345"))
        assertTrue(ValidationUtils.isValidIdentifier("123456"))
        assertTrue(ValidationUtils.isValidIdentifier("ABCDEFGHIJKLMNO")) // 15 chars
    }

    @Test
    fun testInvalidIdentifiers() {
        assertFalse(ValidationUtils.isValidIdentifier("Short")) // 5 chars
        assertFalse(ValidationUtils.isValidIdentifier("ThisIdentifierIsTooLongForSure")) // > 15 chars
        assertFalse(ValidationUtils.isValidIdentifier("Team#1")) // special char #
        assertFalse(ValidationUtils.isValidIdentifier("Team/Box")) // slash
        assertFalse(ValidationUtils.isValidIdentifier("Team.Room")) // dot
        assertFalse(ValidationUtils.isValidIdentifier(""))
    }

    @Test
    fun testValidPins() {
        assertTrue(ValidationUtils.isValidPin("1234"))
        assertTrue(ValidationUtils.isValidPin("0000"))
        assertTrue(ValidationUtils.isValidPin("123456"))
    }

    @Test
    fun testInvalidPins() {
        assertFalse(ValidationUtils.isValidPin("123")) // 3 digits
        assertFalse(ValidationUtils.isValidPin("1234567")) // 7 digits
        assertFalse(ValidationUtils.isValidPin("12a4")) // letters
        assertFalse(ValidationUtils.isValidPin("12 4")) // space
        assertFalse(ValidationUtils.isValidPin(""))
    }

    @Test
    fun testSecurityUtilsHashAndVerify() {
        val pin = "1234"
        val hashRes = SecurityUtils.hashPin(pin)

        assertTrue(hashRes.salt.isNotBlank())
        assertTrue(hashRes.pinHash.isNotBlank())
        assertEquals(32, hashRes.salt.length) // 16 bytes hex

        // Verification success
        assertTrue(SecurityUtils.verifyPin(pin, hashRes.salt, hashRes.pinHash))

        // Verification fail
        assertFalse(SecurityUtils.verifyPin("9999", hashRes.salt, hashRes.pinHash))
        assertFalse(SecurityUtils.verifyPin("12345", hashRes.salt, hashRes.pinHash))
    }
}
