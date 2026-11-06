package com.ejemplo.laptimer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ejemplo.laptimer.firebase.FirebaseAuthManager
import com.ejemplo.laptimer.util.PinHashResult
import com.ejemplo.laptimer.util.SecurityUtils
import com.ejemplo.laptimer.util.ValidationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class SettingsUiState(
    val teamName: String = "",
    val pin: String = "",
    val enabled: Boolean = false,
    val intervalS: Int = 10,
    val isLoading: Boolean = false,
    val teamNameError: String? = null,
    val pinError: String? = null,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    val settings = NetSettings(application)

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            teamName = settings.room.ifBlank { settings.ensureRoom() },
            pin = settings.pin,
            enabled = settings.enabled,
            intervalS = settings.intervalS
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onTeamNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(teamName = name, teamNameError = null, errorMessage = null)
    }

    fun onPinChanged(pin: String) {
        _uiState.value = _uiState.value.copy(pin = pin, pinError = null, errorMessage = null)
    }

    fun onEnabledChanged(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(enabled = enabled)
    }

    fun onIntervalChanged(interval: Int) {
        _uiState.value = _uiState.value.copy(intervalS = interval.coerceAtLeast(10))
    }

    fun validateAndSave(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val current = _uiState.value
        val rawTeamName = current.teamName.trim()
        val rawPin = current.pin.trim()

        val idResult = ValidationUtils.validateIdentifier(rawTeamName)
        val pinResult = ValidationUtils.validatePin(rawPin)

        var hasError = false
        var nameErr: String? = null
        var pinErr: String? = null

        if (idResult.isFailure) {
            nameErr = idResult.exceptionOrNull()?.message ?: "Identificador inválido"
            hasError = true
        }
        if (pinResult.isFailure) {
            pinErr = pinResult.exceptionOrNull()?.message ?: "PIN inválido"
            hasError = true
        }

        if (hasError) {
            _uiState.value = _uiState.value.copy(
                teamNameError = nameErr,
                pinError = pinErr
            )
            val combinedMsg = listOfNotNull(nameErr, pinErr).joinToString(" | ")
            onError(combinedMsg)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val teamName = rawTeamName.uppercase()
                val hashResult: PinHashResult = SecurityUtils.hashPin(rawPin)

                // Persist locally
                settings.enabled = current.enabled
                settings.room = teamName
                settings.pin = rawPin
                settings.pinHash = hashResult.pinHash
                settings.salt = hashResult.salt
                settings.intervalS = current.intervalS

                // Sincronizar en Firebase si está habilitado
                if (current.enabled) {
                    syncTeamToFirebase(teamName, hashResult)
                }

                _uiState.value = _uiState.value.copy(isLoading = false, saveSuccess = true)
                onSuccess()
            } catch (e: Exception) {
                val err = "Error al guardar en Firebase: ${e.message}"
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = err)
                onError(err)
            }
        }
    }

    private suspend fun syncTeamToFirebase(teamName: String, hashResult: PinHashResult) = withContext(Dispatchers.IO) {
        val token = FirebaseAuthManager.getIdToken()
        val authQuery = if (!token.isNullOrBlank()) "?auth=$token" else ""
        val dbUrl = settings.dbUrl.trimEnd('/')

        val teamPayload = JSONObject().apply {
            put("name", teamName)
            put("pinHash", hashResult.pinHash)
            put("salt", hashResult.salt)
            put("updatedAt", System.currentTimeMillis())
        }

        val url = URL("$dbUrl/teams/$teamName.json$authQuery")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "PATCH"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(teamPayload.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            if (code !in 200..299) {
                throw IOException("HTTP $code")
            }
        } finally {
            conn.disconnect()
        }
    }
}
