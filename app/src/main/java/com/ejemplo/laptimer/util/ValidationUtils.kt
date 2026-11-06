package com.ejemplo.laptimer.util

object ValidationUtils {

    private val identifierPattern = Regex("^[a-zA-Z0-9 ]{6,15}$")
    private val pinPattern = Regex("^[0-9]{4,6}$")

    fun validateIdentifier(identifier: String): Result<Unit> {
        val trimmed = identifier.trim()
        return when {
            trimmed.length !in 6..15 ->
                Result.failure(IllegalArgumentException("El identificador debe tener entre 6 y 15 caracteres."))
            !identifierPattern.matches(trimmed) ->
                Result.failure(IllegalArgumentException("El identificador solo puede contener letras, números y espacios (sin símbolos)."))
            else -> Result.success(Unit)
        }
    }

    fun validatePin(pin: String): Result<Unit> {
        val trimmed = pin.trim()
        return when {
            !pinPattern.matches(trimmed) ->
                Result.failure(IllegalArgumentException("El PIN debe ser exclusivamente numérico y tener entre 4 y 6 dígitos."))
            else -> Result.success(Unit)
        }
    }

    fun isValidIdentifier(identifier: String): Boolean = validateIdentifier(identifier).isSuccess
    fun isValidPin(pin: String): Boolean = validatePin(pin).isSuccess
}
