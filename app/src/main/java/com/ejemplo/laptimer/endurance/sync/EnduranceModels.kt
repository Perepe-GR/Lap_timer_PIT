package com.ejemplo.laptimer.endurance.sync

import androidx.annotation.Keep

@Keep
data class TeamMeta(
    val code: String = "",
    val name: String = "",
    val circuitId: String = "",
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val pinHash: String = "",
    val salt: String = "",
    val leaderUid: String = "",
    val mode: String = "endurance"
)

@Keep
data class MemberInfo(
    val role: String = "pilot", // "pilot" or "crew_chief"
    val name: String = "",
    val t: Long = System.currentTimeMillis()
)

@Keep
data class PendingPilot(
    val id: String = "",
    val name: String = "",
    val uid: String = "",
    val status: String = "PENDING", // PENDING, ACCEPTED, REJECTED
    val requestedAt: Long = 0L
)

@Keep
data class StrategyBase(
    val stintMinutes: Int = 0,
    val pitWindowLaps: Int = 0,
    val fuelPerLap: Double = 0.0,
    val notes: String = "",
    val updatedAt: Long = 0L
)

@Keep
data class StrategyCurrent(
    val stintMinutes: Int = 0,
    val pitWindowLaps: Int = 0,
    val fuelPerLap: Double = 0.0,
    val notes: String = "",
    val updatedBy: String = "",
    val updatedAt: Long = 0L
)

@Keep
data class SessionState(
    val state: String = "LOBBY", // LOBBY, RACING, PIT, FINISHED
    val currentPilotId: String = "",
    val startedAt: Long = 0L,
    val finishedAt: Long = 0L
)

@Keep
data class PilotInfo(
    val id: String = "",
    val name: String = "",
    val status: String = "READY", // ON_TRACK, RESTING, READY
    val lastSeenTs: Long = 0L
)

@Keep
data class LapRecord(
    val lapNumber: Int = 0,
    val timeMs: Long = 0L,
    val sectorsMs: List<Long> = emptyList(),
    val valid: Boolean = true,
    val gapFlag: Boolean = false,
    val recordedAt: Long = 0L
)

@Keep
data class CurrentLap(
    val startTs: Long = 0L,
    val sectorsMs: List<Long> = emptyList(),
    val gpsValid: Boolean = true
)

@Keep
data class PilotData(
    val info: PilotInfo = PilotInfo(),
    val currentLap: CurrentLap? = null,
    val laps: List<LapRecord> = emptyList()
)

@Keep
data class Stint(
    val pilotId: String = "",
    val startTs: Long = 0L,
    val endTs: Long = 0L,
    val laps: Int = 0
)
