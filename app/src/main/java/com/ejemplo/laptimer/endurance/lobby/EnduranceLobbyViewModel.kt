package com.ejemplo.laptimer.endurance.lobby

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ejemplo.laptimer.NetSettings
import com.ejemplo.laptimer.endurance.sync.*
import com.ejemplo.laptimer.firebase.FirebaseAuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class LobbyUiState(
    val step: Int = STEP_WAITING,
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
    private val enduranceRepository = EnduranceRepository(application)

    private val _uiState = MutableStateFlow(LobbyUiState())
    val uiState: StateFlow<LobbyUiState> = _uiState.asStateFlow()

    init {
        // Strict real-time validation: always enter waiting room and listen to Firebase request status
        val globalRoom = settings.ensureRoom()
        val pilotName = settings.driverName
        val currentUid = settings.deviceUid

        _uiState.value = _uiState.value.copy(
            teamCode = globalRoom,
            teamName = globalRoom,
            myPilotId = currentUid,
            step = LobbyUiState.STEP_WAITING
        )

        enduranceRepository.setupDevicePresence(globalRoom, currentUid, settings.carName, "driving")

        viewModelScope.launch {
            runCatching {
                syncClient.requestJoin(globalRoom, currentUid, pilotName, currentUid)
            }
            startObserving(globalRoom, currentUid)
        }
    }

    fun leaveRoom() {
        settings.clearEnduranceSession()
        _uiState.value = LobbyUiState()
    }

    private fun startObserving(code: String, myId: String) {
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
                if (_uiState.value.step == LobbyUiState.STEP_WAITING && myId.isNotBlank()) {
                    val myReq = pending[myId]
                    val status = myReq?.status ?: ""
                    when {
                        status == "ACCEPTED" -> {
                            settings.saveEnduranceSession(code, "PILOT", myId)
                            _uiState.value = _uiState.value.copy(navigateToSession = "PILOT")
                        }
                        status == "REJECTED" || (pending.isNotEmpty() && myReq == null) -> {
                            settings.clearEnduranceSession()
                            _uiState.value = LobbyUiState(
                                step = LobbyUiState.STEP_SETUP,
                                error = "Access rejected or removed by Crew Chief."
                            )
                        }
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
