package com.ejemplo.laptimer.endurance.pilot

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ejemplo.laptimer.NetSettings
import com.ejemplo.laptimer.endurance.sync.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PilotUiState(
    val status: String = "READY",
    val isCurrentPilot: Boolean = false,
    val currentLapMs: Long = 0L,
    val bestLapMs: Long? = null,
    val laps: List<LapRecord> = emptyList(),
    val gpsValid: Boolean = true,
    val syncStatus: String = "Online",
    val unexpectedMovement: Boolean = false,
    val estimatedStintMinutes: Int = 30,
    val tacticalInstruction: EnduranceSyncClient.TacticalInstruction? = null,
    val ejected: Boolean = false,
    val pilotName: String = "",
    val pilotId: String = ""
)

class PilotSessionViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = NetSettings(application)
    private val syncClient = EnduranceSyncClient(settings.dbUrl)

    private val _uiState = MutableStateFlow(PilotUiState())
    val uiState: StateFlow<PilotUiState> = _uiState.asStateFlow()

    private var syncJob: Job? = null
    private var highSpeedCount = 0

    fun startSession(teamCode: String, myPilotId: String) {
        syncJob?.cancel()
        _uiState.value = _uiState.value.copy(pilotId = myPilotId, pilotName = settings.driverName)
        syncJob = viewModelScope.launch {
            launch {
                syncClient.observeSessionState(teamCode).collect { state ->
                    val isCur = state.currentPilotId == myPilotId
                    val newStatus = if (isCur) "ON_TRACK" else "RESTING"
                    _uiState.value = _uiState.value.copy(isCurrentPilot = isCur, status = newStatus)
                }
            }
            launch {
                syncClient.observeStrategy(teamCode).collect { strat ->
                    if (strat.stintMinutes > 0) {
                        _uiState.value = _uiState.value.copy(estimatedStintMinutes = strat.stintMinutes)
                    }
                }
            }
            launch {
                val carKey = settings.carKey()
                syncClient.observeInstructions(teamCode, carKey).collect { instr ->
                    _uiState.value = _uiState.value.copy(tacticalInstruction = instr)
                }
            }
            launch {
                syncClient.observePendingPilots(teamCode).collect { pending ->
                    val req = pending[myPilotId]
                    val hadReq = pending.isNotEmpty()
                    if ((req == null && hadReq) || req?.status == "REJECTED") {
                        settings.clearEnduranceSession()
                        _uiState.value = _uiState.value.copy(ejected = true)
                    }
                }
            }
        }
    }

    fun leaveSession(teamCode: String, myPilotId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            runCatching { syncClient.removePilotPresence(teamCode, myPilotId) }
            settings.clearEnduranceSession()
            onComplete()
        }
    }

    fun onGpsFix(speedKmh: Double, valid: Boolean) {
        _uiState.value = _uiState.value.copy(gpsValid = valid)
        if (!uiState.value.isCurrentPilot && speedKmh > 15.0) {
            highSpeedCount++
            if (highSpeedCount >= 5) {
                _uiState.value = _uiState.value.copy(unexpectedMovement = true)
            }
        } else {
            highSpeedCount = 0
        }
    }

    fun dismissUnexpectedMovement() {
        highSpeedCount = 0
        _uiState.value = _uiState.value.copy(unexpectedMovement = false)
    }

    fun dismissInstruction() {
        _uiState.value = _uiState.value.copy(tacticalInstruction = null)
    }

    fun onLapCompleted(teamCode: String, myPilotId: String, lap: LapRecord) {
        val updatedLaps = _uiState.value.laps + lap
        val best = updatedLaps.filter { it.valid }.minOfOrNull { it.timeMs }
        _uiState.value = _uiState.value.copy(laps = updatedLaps, bestLapMs = best)
        viewModelScope.launch {
            syncClient.pushLap(teamCode, myPilotId, lap)
        }
    }

    fun updateCurrentLap(teamCode: String, myPilotId: String, current: CurrentLap) {
        viewModelScope.launch {
            syncClient.updateCurrentLap(teamCode, myPilotId, current)
        }
    }

    override fun onCleared() {
        syncJob?.cancel()
        super.onCleared()
    }
}
