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
    val intervalS: Int = 1,
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
        _uiState.value = _uiState.value.copy(intervalS = interval.coerceAtLeast(1))
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
            nameErr = idResult.exceptionOrNull()?.message ?: "Invalid identifier"
            hasError = true
        }
        if (pinResult.isFailure) {
            pinErr = pinResult.exceptionOrNull()?.message ?: "Invalid PIN"
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

                // Persist locally FIRST (always saved regardless of network state)
                settings.enabled = current.enabled
                settings.room = teamName
                settings.activeRoomCode = teamName
                settings.pin = rawPin
                settings.pinHash = hashResult.pinHash
                settings.salt = hashResult.salt
                settings.intervalS = current.intervalS

                // Synchronize to Firebase if telemetry/pitwall integration is enabled,
                // catching any network or Firebase exceptions so local save is never blocked.
                var firebaseError: String? = null
                if (current.enabled) {
                    try {
                        syncTeamToFirebase(teamName, hashResult)
                    } catch (e: Exception) {
                        firebaseError = e.message
                    }
                }

                _uiState.value = _uiState.value.copy(isLoading = false, saveSuccess = true)
                if (firebaseError != null) {
                    onError("Settings saved locally. Firebase sync warning: $firebaseError")
                } else {
                    onSuccess()
                }
            } catch (e: Exception) {
                val err = "Error saving settings: ${e.message}"
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = err)
                onError(err)
            }
        }
    }

    private suspend fun syncTeamToFirebase(teamName: String, hashResult: PinHashResult) = withContext(Dispatchers.IO) {
        val uid = runCatching { FirebaseAuthManager.ensureAuthenticated() }.getOrDefault("anon")
        var token = FirebaseAuthManager.getIdToken()
        val dbUrl = settings.dbUrl.trimEnd('/')

        val teamPayload = JSONObject().apply {
            put("name", teamName)
            put("pinHash", hashResult.pinHash)
            put("salt", hashResult.salt)
            put("updatedAt", System.currentTimeMillis())
        }.toString()

        val metaPayload = JSONObject().apply {
            put("code", teamName)
            put("name", teamName)
            put("circuitId", "Circuit 1")
            put("createdBy", uid)
            put("createdAt", System.currentTimeMillis())
            put("pinHash", hashResult.pinHash)
            put("salt", hashResult.salt)
            put("leaderUid", uid)
            put("mode", "endurance")
            put("updatedAt", System.currentTimeMillis())
        }.toString()

        fun putEndpoint(path: String, payload: String): Int {
            val authQuery = if (!token.isNullOrBlank()) "?auth=$token" else ""
            val url = URL("$dbUrl/$path.json$authQuery")
            val conn = url.openConnection() as HttpURLConnection
            return try {
                conn.requestMethod = "PUT"
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                android.util.Log.i("SettingsViewModel", "PUT $path -> HTTP $code")
                code
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "PUT $path FAILED: ${e.message}", e)
                -1
            } finally {
                try { conn.disconnect() } catch (_: Exception) {}
            }
        }

        // Write to /rooms/{teamName}/meta with automatic token refresh on 401
        var codeRooms = putEndpoint("rooms/$teamName/meta", metaPayload)
        if (codeRooms == 401) {
            token = FirebaseAuthManager.getIdToken()
            codeRooms = putEndpoint("rooms/$teamName/meta", metaPayload)
        }

        // Write to /teams/{teamName} with automatic token refresh on 401
        var codeTeams = putEndpoint("teams/$teamName", teamPayload)
        if (codeTeams == 401) {
            token = FirebaseAuthManager.getIdToken()
            codeTeams = putEndpoint("teams/$teamName", teamPayload)
        }

        if (codeRooms !in 200..299 && codeRooms != 401 && codeRooms != 403) {
            throw IOException("HTTP $codeRooms for rooms meta")
        }
        if (codeTeams !in 200..299 && codeTeams != 401 && codeTeams != 403) {
            throw IOException("HTTP $codeTeams for teams registry")
        }
    }
}
