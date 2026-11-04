package com.ejemplo.laptimer.endurance.leader

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

data class LeaderUiState(
    val teamName: String = "",
    val teamCode: String = "",
    val circuitId: String = "",
    val currentPilotId: String = "",
    val currentPilotName: String = "None",
    val pilots: Map<String, PilotData> = emptyMap(),
    val strategy: StrategyBase = StrategyBase(),
    val sessionState: String = "RACING",
    val unexpectedPilotName: String? = null
)

class LeaderDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = NetSettings(application)
    private val syncClient = EnduranceSyncClient(settings.dbUrl)

    private val _uiState = MutableStateFlow(LeaderUiState())
    val uiState: StateFlow<LeaderUiState> = _uiState.asStateFlow()

    private var syncJob: Job? = null

    fun start(teamCode: String) {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            launch {
                syncClient.observePilots(teamCode).collect { map ->
                    val curId = _uiState.value.currentPilotId
                    val curName = map[curId]?.info?.name ?: "None"
                    _uiState.value = _uiState.value.copy(pilots = map, currentPilotName = curName)
                }
            }
            launch {
                syncClient.observeSessionState(teamCode).collect { st ->
                    val curName = _uiState.value.pilots[st.currentPilotId]?.info?.name ?: "None"
                    _uiState.value = _uiState.value.copy(
                        sessionState = st.state,
                        currentPilotId = st.currentPilotId,
                        currentPilotName = curName
                    )
                }
            }
            launch {
                syncClient.observeStrategy(teamCode).collect { strat ->
                    _uiState.value = _uiState.value.copy(strategy = strat)
                }
            }
        }
    }

    fun changePilot(teamCode: String, newPilotId: String) {
        viewModelScope.launch {
            syncClient.setCurrentPilot(teamCode, newPilotId)
            val stint = Stint(pilotId = newPilotId, startTs = System.currentTimeMillis())
            syncClient.pushStint(teamCode, stint)
        }
    }

    fun updateStrategy(teamCode: String, strategy: StrategyBase) {
        viewModelScope.launch {
            syncClient.updateStrategy(teamCode, strategy)
        }
    }

    fun finishSession(teamCode: String) {
        viewModelScope.launch {
            syncClient.setSessionState(teamCode, "FINISHED")
        }
    }

    fun dismissUnexpectedPilot() {
        _uiState.value = _uiState.value.copy(unexpectedPilotName = null)
    }

    override fun onCleared() {
        syncJob?.cancel()
        super.onCleared()
    }
}
