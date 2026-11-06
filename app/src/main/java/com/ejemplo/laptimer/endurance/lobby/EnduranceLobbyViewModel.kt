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
    val step: Int = STEP_SETUP,
    val role: String = "PILOT",
    val mode: String = "JOIN",
    val teamCode: String = "",
    val pin: String = "",
    val teamName: String = "",
    val circuitId: String = "",
    val myPilotId: String = "",
    val currentPilotId: String = "",
    val pilots: Map<String, PilotData> = emptyMap(),
    val pendingPilots: Map<String, PendingPilot> = emptyMap(),
    val sessionState: String = "LOBBY",
    val error: String? = null,
    val navigateToSession: String? = null
) {
    companion object {
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
        val isSessionActive = settings.sessionActive
        if (isSessionActive && activeRoom.isNotBlank()) {
            Log.i("EnduranceLobbyVM", "Restoring active pilot session for room: $activeRoom")
            _uiState.value = _uiState.value.copy(
                teamCode = activeRoom,
                role = "PILOT",
                step = LobbyUiState.STEP_WAITING,
                myPilotId = settings.deviceUid
            )
            startObserving(activeRoom)
        } else {
            viewModelScope.launch {
                val globalRoom = settings.ensureRoom()
                val globalPin = settings.pin
                val pilotName = settings.driverName
                val currentUid = try { FirebaseAuthManager.ensureAuthenticated() } catch (_: Exception) { "pilot-" + System.currentTimeMillis() }
                joinTeam(globalRoom, globalPin, currentUid, pilotName)
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
                    myPilotId = uid, pin = inputPin.trim(), step = LobbyUiState.STEP_WAITING, error = null
                )
                startObserving(cleanCode)
            } else {
                val err = joinRes.exceptionOrNull()
                _uiState.value = _uiState.value.copy(pin = "", error = "Connection error or timeout: ${err?.message}")
            }
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
            syncClient.observeCurrentPilotId(code).collect { curId ->
                _uiState.value = _uiState.value.copy(currentPilotId = curId)
            }
        }
        viewModelScope.launch {
            syncClient.observePendingPilots(code).collect { pending ->
                _uiState.value = _uiState.value.copy(pendingPilots = pending)
                val myId = _uiState.value.myPilotId
                if (_uiState.value.step == LobbyUiState.STEP_WAITING && myId.isNotBlank()) {
                    val myReq = pending[myId]
                    val hadReq = _uiState.value.pendingPilots.containsKey(myId)
                    if (myReq?.status == "REJECTED" || (hadReq && myReq == null)) {
                        settings.clearEnduranceSession()
                        _uiState.value = LobbyUiState(
                            step = LobbyUiState.STEP_SETUP,
                            error = "You have been removed from the team by the Crew Chief."
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
                    _uiState.value = _uiState.value.copy(navigateToSession = "PILOT")
                }
            }
        }
    }

    fun consumeError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
