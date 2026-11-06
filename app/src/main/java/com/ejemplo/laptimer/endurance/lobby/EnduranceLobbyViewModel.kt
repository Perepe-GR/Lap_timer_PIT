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
    val step: Int = STEP_ROLE_SELECT,
    val role: String = "PILOT",
    val mode: String = "JOIN",
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

    init {
        val activeRoom = settings.activeRoomCode
        val activeRole = settings.userRole
        val isSessionActive = settings.sessionActive
        if (isSessionActive && activeRoom.isNotBlank()) {
            Log.i("EnduranceLobbyVM", "Restoring active session for room: $activeRoom, role: $activeRole")
            _uiState.value = _uiState.value.copy(
                teamCode = activeRoom,
                role = activeRole.ifBlank { "PILOT" },
                step = LobbyUiState.STEP_WAITING,
                myPilotId = settings.deviceUid
            )
            startObserving(activeRoom)
        }
    }

    fun selectRole(role: String) {
        val normalizedRole = if (role == "LEADER" || role == "CREW_CHIEF") "CREW_CHIEF" else "PILOT"
        val mode = if (normalizedRole == "CREW_CHIEF") "CREATE" else "JOIN"
        _uiState.value = _uiState.value.copy(role = normalizedRole, mode = mode, step = LobbyUiState.STEP_SETUP, error = null)
    }

    fun goBackToRoleSelect() {
        _uiState.value = _uiState.value.copy(step = LobbyUiState.STEP_ROLE_SELECT, error = null)
    }

    fun createTeam(teamName: String, pin: String, circuitId: String, leaderUid: String) {
        viewModelScope.launch {
            val cleanCode = teamName.trim().uppercase()
            val currentUid = try {
                FirebaseAuthManager.ensureAuthenticated()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(pin = "", error = "Authentication error: ${e.message}")
                return@launch
            }

            val metaRes = syncClient.fetchTeamMeta(cleanCode)
            val existingMeta = metaRes.getOrNull()
            if (existingMeta != null && existingMeta.leaderUid.isNotBlank() && existingMeta.leaderUid != currentUid) {
                _uiState.value = _uiState.value.copy(pin = "", error = "Only one Crew Chief allowed per room.")
                return@launch
            }

            val res = syncClient.createTeam(cleanCode, teamName, circuitId, currentUid, pin)
            if (res.isSuccess) {
                settings.saveEnduranceSession(cleanCode, "CREW_CHIEF", currentUid)
                _uiState.value = _uiState.value.copy(
                    teamCode = cleanCode, teamName = teamName, circuitId = circuitId, pin = pin,
                    step = LobbyUiState.STEP_WAITING, error = null
                )
                startObserving(cleanCode)
            } else {
                val err = res.exceptionOrNull()
                _uiState.value = _uiState.value.copy(pin = "", error = "Connection error or timeout: ${err?.message}")
            }
        }
    }

    fun joinTeam(code: String, inputPin: String, pilotId: String, pilotName: String) {
        viewModelScope.launch {
            val cleanCode = code.trim().uppercase()
            val metaRes = syncClient.fetchTeamMeta(cleanCode)
            val meta = metaRes.getOrNull()
            if (meta == null) {
                _uiState.value = _uiState.value.copy(pin = "", error = "Incorrect PIN or room not found")
                return@launch
            }
            val isValidPin = if (meta.pinHash.isNotBlank()) {
                SecurityUtils.verifyPin(inputPin, meta.salt, meta.pinHash)
            } else {
                meta.code == inputPin.trim()
            }
            if (!isValidPin) {
                _uiState.value = _uiState.value.copy(pin = "", error = "Incorrect PIN or room not found")
                return@launch
            }
            val joinRes = syncClient.joinTeam(cleanCode, inputPin, pilotId, pilotName)
            if (joinRes.isSuccess) {
                val uid = pilotId.ifBlank { FirebaseAuthManager.ensureAuthenticated() }
                settings.saveEnduranceSession(cleanCode, "PILOT", uid)
                _uiState.value = _uiState.value.copy(
                    teamCode = cleanCode, teamName = meta.name, circuitId = meta.circuitId,
                    myPilotId = pilotId, pin = inputPin.trim(), step = LobbyUiState.STEP_WAITING, error = null
                )
                startObserving(cleanCode)
            } else {
                val err = joinRes.exceptionOrNull()
                _uiState.value = _uiState.value.copy(pin = "", error = "Connection error or timeout: ${err?.message}")
            }
        }
    }

    fun acceptPilot(code: String, pilotId: String, pilotName: String, uid: String) {
        viewModelScope.launch {
            runCatching { syncClient.acceptPilot(code, pilotId, pilotName, uid) }
                .onFailure { e -> _uiState.value = _uiState.value.copy(error = "Error accepting pilot: ${e.message}") }
        }
    }

    fun rejectPilot(code: String, uid: String) {
        viewModelScope.launch {
            runCatching { syncClient.rejectPilot(code, uid) }
                .onFailure { e -> _uiState.value = _uiState.value.copy(error = "Error rejecting pilot: ${e.message}") }
        }
    }

    fun startSession() {
        val code = _uiState.value.teamCode
        viewModelScope.launch {
            runCatching { syncClient.setSessionState(code, "RACING") }
                .onFailure { e -> _uiState.value = _uiState.value.copy(error = "Error starting session: ${e.message}") }
        }
    }

    fun leaveRoom() {
        settings.clearEnduranceSession()
        _uiState.value = LobbyUiState()
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
                        settings.clearEnduranceSession()
                        _uiState.value = _uiState.value.copy(
                            step = LobbyUiState.STEP_SETUP, pin = "",
                            error = "Your access request has been rejected by the team."
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
