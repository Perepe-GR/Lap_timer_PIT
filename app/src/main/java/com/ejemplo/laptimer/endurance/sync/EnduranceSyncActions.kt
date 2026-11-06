package com.ejemplo.laptimer.endurance.sync

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject

suspend fun EnduranceSyncClient.pushStint(code: String, stint: Stint): Result<Unit> = runCatching {
    val path = "rooms/$code/stints/${stint.startTs}"
    request("PUT", path, JSONObject(stint.toMap()).toString())
}

fun EnduranceSyncClient.observeStints(code: String): Flow<List<Stint>> = flow {
    while (true) {
        runCatching {
            val res = request("GET", "rooms/$code/stints")
            if (res.isNotBlank() && res != "null") {
                val trimmed = res.trim()
                if (trimmed.startsWith("{")) {
                    val result = mutableListOf<Stint>()
                    val root = JSONObject(trimmed)
                    val keys = root.keys()
                    while (keys.hasNext()) {
                        val sKey = keys.next()
                        if (!root.isNull(sKey)) {
                            val sObj = root.getJSONObject(sKey)
                            val map = mutableMapOf<String, Any?>()
                            val kIter = sObj.keys()
                            while (kIter.hasNext()) {
                                val k = kIter.next()
                                if (!sObj.isNull(k)) map[k] = sObj.get(k)
                            }
                            result.add(stintFromMap(map))
                        }
                    }
                    emit(result)
                }
            }
        }
        delay(5000L)
    }
}

fun EnduranceSyncClient.observeStrategy(code: String): Flow<StrategyBase> = flow {
    while (true) {
        runCatching {
            val res = request("GET", "rooms/$code/strategy")
            if (res.isNotBlank() && res != "null") {
                val trimmed = res.trim()
                if (trimmed.startsWith("{")) {
                    val obj = JSONObject(trimmed)
                    val map = mutableMapOf<String, Any?>()
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        if (!obj.isNull(k)) map[k] = obj.get(k)
                    }
                    emit(strategyBaseFromMap(map))
                }
            }
        }
        delay(5000L)
    }
}

fun EnduranceSyncClient.observeInstructions(code: String, carKey: String): Flow<EnduranceSyncClient.TacticalInstruction?> = flow {
    while (true) {
        runCatching {
            val res = request("GET", "rooms/$code/instructions")
            if (res.isNotBlank() && res != "null") {
                val trimmed = res.trim()
                if (trimmed.startsWith("{")) {
                    val obj = JSONObject(trimmed)
                    val textVal = obj.optString("text", "").ifBlank { obj.optString("message", "") }
                    val bgVal = obj.optString("bg", "amarillo")
                    val flagVal = obj.optString("flag", "")
                    val tVal = obj.optLong("t", obj.optLong("timestamp", 0L))
                    if (textVal.isNotBlank()) {
                        emit(EnduranceSyncClient.TacticalInstruction(
                            text = textVal,
                            bg = bgVal,
                            flag = flagVal,
                            t = tVal
                        ))
                    } else {
                        emit(null)
                    }
                } else {
                    emit(null)
                }
            } else {
                emit(null)
            }
        }
        delay(3000L)
    }
}

suspend fun EnduranceSyncClient.updateStrategy(code: String, strategy: StrategyBase): Result<Unit> = runCatching {
    request("PUT", "rooms/$code/strategy", JSONObject(strategy.toMap()).toString())
}

suspend fun EnduranceSyncClient.setCurrentPilot(code: String, pilotId: String): Result<Unit> = runCatching {
    request("PUT", "rooms/$code/session/currentPilotId", JSONObject.quote(pilotId))
}

suspend fun EnduranceSyncClient.pushLap(code: String, pilotId: String, lap: LapRecord): Result<Unit> = runCatching {
    request("PUT", "rooms/$code/pilots/$pilotId/laps/${lap.lapNumber}", JSONObject(lap.toMap()).toString())
}

suspend fun EnduranceSyncClient.updateCurrentLap(code: String, pilotId: String, current: CurrentLap): Result<Unit> = runCatching {
    val map = mapOf("startTs" to current.startTs, "sectorsMs" to current.sectorsMs, "gpsValid" to current.gpsValid)
    request("PUT", "rooms/$code/pilots/$pilotId/currentLap", JSONObject(map).toString())
}

suspend fun EnduranceSyncClient.setLeader(code: String, uid: String): Result<Unit> = runCatching {
    request("PUT", "rooms/$code/strategy/leaderUid", JSONObject().put("uid", uid).toString())
}
