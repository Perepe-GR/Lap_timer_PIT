package com.ejemplo.laptimer.endurance.sync

fun TeamMeta.toMap(): Map<String, Any?> = mapOf(
    "code" to code,
    "name" to name,
    "circuitId" to circuitId,
    "createdBy" to createdBy,
    "createdAt" to createdAt,
    "pinHash" to pinHash,
    "salt" to salt,
    "leaderUid" to leaderUid,
    "mode" to mode
)

fun teamMetaFromMap(map: Map<String, Any?>): TeamMeta = TeamMeta(
    code = map["code"]?.toString() ?: "",
    name = map["name"]?.toString() ?: "",
    circuitId = map["circuitId"]?.toString() ?: "",
    createdBy = map["createdBy"]?.toString() ?: "",
    createdAt = (map["createdAt"] as? Number)?.toLong() ?: (map["createdAt"] as? String)?.toLongOrNull() ?: 0L,
    pinHash = map["pinHash"]?.toString() ?: "",
    salt = map["salt"]?.toString() ?: "",
    leaderUid = map["leaderUid"]?.toString() ?: "",
    mode = map["mode"]?.toString() ?: "endurance"
)

fun MemberInfo.toMap(): Map<String, Any?> = mapOf(
    "role" to role,
    "name" to name,
    "t" to t
)

fun PendingPilot.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "uid" to uid,
    "status" to status,
    "requestedAt" to requestedAt
)

fun pendingPilotFromMap(map: Map<String, Any?>): PendingPilot = PendingPilot(
    id = map["id"]?.toString() ?: "",
    name = map["name"]?.toString() ?: "",
    uid = map["uid"]?.toString() ?: "",
    status = map["status"]?.toString() ?: "PENDING",
    requestedAt = (map["requestedAt"] as? Number)?.toLong() ?: (map["requestedAt"] as? String)?.toLongOrNull() ?: 0L
)

fun StrategyBase.toMap(): Map<String, Any?> = mapOf(
    "stintMinutes" to stintMinutes,
    "pitWindowLaps" to pitWindowLaps,
    "fuelPerLap" to fuelPerLap,
    "notes" to notes,
    "updatedAt" to updatedAt
)

fun strategyBaseFromMap(map: Map<String, Any?>): StrategyBase = StrategyBase(
    stintMinutes = (map["stintMinutes"] as? Number)?.toInt() ?: 0,
    pitWindowLaps = (map["pitWindowLaps"] as? Number)?.toInt() ?: 0,
    fuelPerLap = (map["fuelPerLap"] as? Number)?.toDouble() ?: 0.0,
    notes = map["notes"] as? String ?: "",
    updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: 0L
)

fun StrategyCurrent.toMap(): Map<String, Any?> = mapOf(
    "stintMinutes" to stintMinutes,
    "pitWindowLaps" to pitWindowLaps,
    "fuelPerLap" to fuelPerLap,
    "notes" to notes,
    "updatedBy" to updatedBy,
    "updatedAt" to updatedAt
)

fun strategyCurrentFromMap(map: Map<String, Any?>): StrategyCurrent = StrategyCurrent(
    stintMinutes = (map["stintMinutes"] as? Number)?.toInt() ?: 0,
    pitWindowLaps = (map["pitWindowLaps"] as? Number)?.toInt() ?: 0,
    fuelPerLap = (map["fuelPerLap"] as? Number)?.toDouble() ?: 0.0,
    notes = map["notes"] as? String ?: "",
    updatedBy = map["updatedBy"] as? String ?: "",
    updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: 0L
)

fun SessionState.toMap(): Map<String, Any?> = mapOf(
    "state" to state,
    "currentPilotId" to currentPilotId,
    "startedAt" to startedAt,
    "finishedAt" to finishedAt
)

fun sessionStateFromMap(map: Map<String, Any?>): SessionState = SessionState(
    state = map["state"] as? String ?: "LOBBY",
    currentPilotId = map["currentPilotId"] as? String ?: "",
    startedAt = (map["startedAt"] as? Number)?.toLong() ?: 0L,
    finishedAt = (map["finishedAt"] as? Number)?.toLong() ?: 0L
)

fun PilotInfo.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "status" to status,
    "lastSeenTs" to lastSeenTs
)

fun pilotInfoFromMap(map: Map<String, Any?>): PilotInfo = PilotInfo(
    id = map["id"]?.toString() ?: "",
    name = map["name"]?.toString() ?: "",
    status = map["status"] as? String ?: "READY",
    lastSeenTs = (map["lastSeenTs"] as? Number)?.toLong() ?: 0L
)

fun LapRecord.toMap(): Map<String, Any?> = mapOf(
    "lapNumber" to lapNumber,
    "timeMs" to timeMs,
    "sectorsMs" to sectorsMs,
    "valid" to valid,
    "gapFlag" to gapFlag,
    "recordedAt" to recordedAt
)

@Suppress("UNCHECKED_CAST")
fun lapRecordFromMap(map: Map<String, Any?>): LapRecord = LapRecord(
    lapNumber = (map["lapNumber"] as? Number)?.toInt() ?: 0,
    timeMs = (map["timeMs"] as? Number)?.toLong() ?: 0L,
    sectorsMs = (map["sectorsMs"] as? List<*>)?.mapNotNull { (it as? Number)?.toLong() } ?: emptyList(),
    valid = map["valid"] as? Boolean ?: true,
    gapFlag = map["gapFlag"] as? Boolean ?: false,
    recordedAt = (map["recordedAt"] as? Number)?.toLong() ?: 0L
)

fun Stint.toMap(): Map<String, Any?> = mapOf(
    "pilotId" to pilotId,
    "startTs" to startTs,
    "endTs" to endTs,
    "laps" to laps
)

fun stintFromMap(map: Map<String, Any?>): Stint = Stint(
    pilotId = map["pilotId"] as? String ?: "",
    startTs = (map["startTs"] as? Number)?.toLong() ?: 0L,
    endTs = (map["endTs"] as? Number)?.toLong() ?: 0L,
    laps = (map["laps"] as? Number)?.toInt() ?: 0
)
