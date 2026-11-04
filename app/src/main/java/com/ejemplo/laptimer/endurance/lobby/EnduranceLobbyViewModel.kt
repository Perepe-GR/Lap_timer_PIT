package com.ejemplo.laptimer.endurance.lobby

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ejemplo.laptimer.NetSettings
import com.ejemplo.laptimer.endurance.sync.*
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
    val pilots: Map<String, PilotData> = emptyMap(),
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
            val cleanCode = code.trim().uppercase()
            val metaRes = syncClient.fetchTeamMeta(cleanCode)
            val existingMeta = metaRes.getOrNull()
            if (existingMeta != null && existingMeta.leaderUid.isNotBlank() && existingMeta.leaderUid != leaderUid) {
                _uiState.value = _uiState.value.copy(error = "Only one Crew Chief is allowed per team session.")
                return@launch
            }
            val pin = cleanCode
            val res = syncClient.createTeam(cleanCode, name, circuitId, leaderUid, pin)
            if (res.isSuccess) {
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
                _uiState.value = _uiState.value.copy(error = "Failed to create team: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun joinTeam(code: String, inputPin: String, pilotId: String, pilotName: String) {
        viewModelScope.launch {
            val cleanCode = code.trim().uppercase()
            val metaRes = syncClient.fetchTeamMeta(cleanCode)
            val meta = metaRes.getOrNull()
            if (meta == null) {
                _uiState.value = _uiState.value.copy(error = "Team code not found")
                return@launch
            }
            val isValidPin = if (meta.pinHash.isNotBlank()) {
                com.ejemplo.laptimer.firebase.HashUtils.sha256(meta.salt, inputPin.trim()) == meta.pinHash
            } else {
                meta.code == inputPin.trim()
            }
            if (!isValidPin) {
                _uiState.value = _uiState.value.copy(error = "Incorrect PIN")
                return@launch
            }
            val joinRes = syncClient.joinTeam(cleanCode, pilotId, pilotName)
            if (joinRes.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    teamCode = cleanCode,
                    teamName = meta.name,
                    circuitId = meta.circuitId,
                    pin = inputPin.trim(),
                    step = LobbyUiState.STEP_WAITING,
                    error = null
                )
                startObserving(cleanCode)
            } else {
                _uiState.value = _uiState.value.copy(error = "Failed to join team: ${joinRes.exceptionOrNull()?.message}")
            }
        }
    }

    fun startSession() {
        val code = _uiState.value.teamCode
        viewModelScope.launch {
            syncClient.setSessionState(code, "RACING")
        }
    }

    private fun startObserving(code: String) {
        viewModelScope.launch {
            syncClient.observePilots(code).collect { pilots ->
                _uiState.value = _uiState.value.copy(pilots = pilots)
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
