package com.ejemplo.laptimer.endurance.lobby

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ejemplo.laptimer.NetSettings
import com.ejemplo.laptimer.endurance.sync.*
import com.ejemplo.laptimer.firebase.FirebaseAuthManager
import com.ejemplo.laptimer.util.SecurityUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LobbyUiState(
    val step: Int = STEP_ROLE_SELECT, // STEP_ROLE_SELECT, STEP_SETUP, STEP_WAITING
    val role: String = "PILOT", // CREW_CHIEF or PILOT
    val mode: String = "JOIN", // CREATE or JOIN
    val teamCode: String = "",
    val pin: String = "",
    val teamName: String = "",
    val circuitId: String = "",
    val myPilotId: String = "",
    val pilots: Map<String, PilotData> = emptyMap(),
    val pendingPilots: Map<String, PendingPilot> = emptyMap(),
    val sessionState: String = "LOBBY",
    val error: String? = null,
    val navigateToSession: String? = null
) {
    companion object {
        const val STEP_ROLE_SELECT = 0
        const val STEP_SETUP = 1
        const val STEP_WAITING = 2
    }
}

class EnduranceLobbyViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = NetSettings(application)
    private val syncClient = EnduranceSyncClient(settings.dbUrl)

    private val _uiState = MutableStateFlow(LobbyUiState())
    val uiState: StateFlow<LobbyUiState> = _uiState.asStateFlow()

    fun selectRole(role: String) {
        val normalizedRole = if (role == "LEADER" || role == "CREW_CHIEF") "CREW_CHIEF" else "PILOT"
        val mode = if (normalizedRole == "CREW_CHIEF") "CREATE" else "JOIN"
        _uiState.value = _uiState.value.copy(
            role = normalizedRole,
            mode = mode,
            step = LobbyUiState.STEP_SETUP,
            error = null
        )
    }

    fun goBackToRoleSelect() {
        _uiState.value = _uiState.value.copy(step = LobbyUiState.STEP_ROLE_SELECT, error = null)
    }

    fun createTeam(code: String, name: String, circuitId: String, leaderUid: String) {
        viewModelScope.launch {
            Log.i("EnduranceLobbyViewModel", "createTeam invoked for code: $code")
            val cleanCode = code.trim().uppercase()
            val currentUid = try {
                FirebaseAuthManager.ensureAuthenticated()
            } catch (e: Exception) {
                Log.e("EnduranceLobbyViewModel", "Firebase Auth failed prior to createTeam: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    pin = "",
                    error = "Error de conexión con el servicio de autenticación: ${e.message}"
                )
                return@launch
            }

            val metaRes = syncClient.fetchTeamMeta(cleanCode)
            val existingMeta = metaRes.getOrNull()
            if (existingMeta != null && existingMeta.leaderUid.isNotBlank() && existingMeta.leaderUid != currentUid) {
                Log.w("EnduranceLobbyViewModel", "Crew Chief collision: room $cleanCode already owned by ${existingMeta.leaderUid}")
                _uiState.value = _uiState.value.copy(
                    pin = "",
                    error = "Solo se permite un Cap de Equipo (Crew Chief) por sala."
                )
                return@launch
            }

            val pin = cleanCode
            val res = syncClient.createTeam(cleanCode, name, circuitId, currentUid, pin)
            if (res.isSuccess) {
                Log.i("EnduranceLobbyViewModel", "createTeam Result SUCCESS for $cleanCode")
                _uiState.value = _uiState.value.copy(
                    teamCode = cleanCode,
                    teamName = name,
                    circuitId = circuitId,
                    pin = pin,
                    step = LobbyUiState.STEP_WAITING,
                    error = null
                )
                startObserving(cleanCode)
            } else {
                val err = res.exceptionOrNull()
                Log.e("EnduranceLobbyViewModel", "createTeam Result FAILURE: ${err?.message}", err)
                _uiState.value = _uiState.value.copy(
                    pin = "",
                    error = "Error de conexión o tiempo de espera agotado: ${err?.message}"
                )
            }
        }
    }

    fun joinTeam(code: String, inputPin: String, pilotId: String, pilotName: String) {
        viewModelScope.launch {
            val cleanCode = code.trim().uppercase()
            val metaRes = syncClient.fetchTeamMeta(cleanCode)
            val meta = metaRes.getOrNull()
            if (meta == null) {
                _uiState.value = _uiState.value.copy(
                    pin = "",
                    error = "PIN incorrecto o sala no encontrada"
                )
                return@launch
            }
            val isValidPin = if (meta.pinHash.isNotBlank()) {
                SecurityUtils.verifyPin(inputPin, meta.salt, meta.pinHash)
            } else {
                meta.code == inputPin.trim()
            }
            if (!isValidPin) {
                _uiState.value = _uiState.value.copy(
                    pin = "",
                    error = "PIN incorrecto o sala no encontrada"
                )
                return@launch
            }
            val joinRes = syncClient.joinTeam(cleanCode, inputPin, pilotId, pilotName)
            if (joinRes.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    teamCode = cleanCode,
                    teamName = meta.name,
                    circuitId = meta.circuitId,
                    myPilotId = pilotId,
                    pin = inputPin.trim(),
                    step = LobbyUiState.STEP_WAITING,
                    error = null
                )
                startObserving(cleanCode)
            } else {
                val err = joinRes.exceptionOrNull()
                _uiState.value = _uiState.value.copy(
                    pin = "",
                    error = "Error de conexión o tiempo de espera agotado: ${err?.message}"
                )
            }
        }
    }

    fun acceptPilot(code: String, pilotId: String, pilotName: String, uid: String) {
        viewModelScope.launch {
            try {
                syncClient.acceptPilot(code, pilotId, pilotName, uid)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Error al aceptar piloto: ${e.message}")
            }
        }
    }

    fun rejectPilot(code: String, uid: String) {
        viewModelScope.launch {
            try {
                syncClient.rejectPilot(code, uid)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Error al rechazar piloto: ${e.message}")
            }
        }
    }

    fun startSession() {
        val code = _uiState.value.teamCode
        viewModelScope.launch {
            try {
                syncClient.setSessionState(code, "RACING")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Error al iniciar sesión: ${e.message}")
            }
        }
    }

    private fun startObserving(code: String) {
        viewModelScope.launch {
            syncClient.observePilots(code).collect { pilots ->
                _uiState.value = _uiState.value.copy(pilots = pilots)
            }
        }
        viewModelScope.launch {
            syncClient.observePendingPilots(code).collect { pending ->
                _uiState.value = _uiState.value.copy(pendingPilots = pending)
                val myId = _uiState.value.myPilotId
                if (_uiState.value.role == "PILOT" && _uiState.value.step == LobbyUiState.STEP_WAITING && myId.isNotBlank()) {
                    val myReq = pending[myId]
                    if (myReq?.status == "REJECTED") {
                        _uiState.value = _uiState.value.copy(
                            step = LobbyUiState.STEP_SETUP,
                            pin = "",
                            error = "Tu solicitud ha sido rechazada por el equipo."
                        )
                    } else if (myReq?.status == "ACCEPTED") {
                        _uiState.value = _uiState.value.copy(navigateToSession = "PILOT")
                    }
                }
            }
        }
        viewModelScope.launch {
            syncClient.observeSessionState(code).collect { sessionState ->
                _uiState.value = _uiState.value.copy(sessionState = sessionState.state)
                if (sessionState.state == "RACING") {
                    _uiState.value = _uiState.value.copy(navigateToSession = _uiState.value.role)
                }
            }
        }
    }

    fun consumeError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
