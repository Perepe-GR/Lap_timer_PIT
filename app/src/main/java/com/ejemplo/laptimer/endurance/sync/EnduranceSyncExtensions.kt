package com.ejemplo.laptimer.endurance.sync

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject

/** Extension functions for EnduranceSyncClient providing real-time observers and lobby actions. */

fun EnduranceSyncClient.observePendingPilots(code: String): Flow<Map<String, PendingPilot>> = flow {
    while (true) {
        runCatching {
            val resReq = request("GET", "rooms/$code/requests")
            val resPending = request("GET", "rooms/$code/pending")
            val text = if (resReq.isNotBlank() && resReq != "null" && resReq != "{}") resReq else resPending
            if (text.isNotBlank() && text != "null") {
                val trimmed = text.trim()
                if (trimmed.startsWith("{")) {
                    val result = mutableMapOf<String, PendingPilot>()
                    val root = JSONObject(trimmed)
                    val keys = root.keys()
                    while (keys.hasNext()) {
                        val pId = keys.next()
                        if (!root.isNull(pId)) {
                            val pObj = root.getJSONObject(pId)
                            val map = mutableMapOf<String, Any?>()
                            val ik = pObj.keys()
                            while (ik.hasNext()) {
                                val k = ik.next()
                                if (!pObj.isNull(k)) map[k] = pObj.get(k)
                            }
                            result[pId] = pendingPilotFromMap(map)
                        }
                    }
                    emit(result)
                }
            } else {
                emit(emptyMap())
            }
        }
        delay(3000L)
    }
}

fun EnduranceSyncClient.observePilots(code: String): Flow<Map<String, PilotData>> = flow {
    while (true) {
        runCatching {
            val res = request("GET", "rooms/$code/pilots")
            if (res.isNotBlank() && res != "null") {
                val trimmed = res.trim()
                if (trimmed.startsWith("{")) {
                    val result = mutableMapOf<String, PilotData>()
                    val root = JSONObject(trimmed)
                    val keys = root.keys()
                    while (keys.hasNext()) {
                        val pId = keys.next()
                        if (!root.isNull(pId)) {
                            val pObj = root.getJSONObject(pId)
                            val infoMap = mutableMapOf<String, Any?>()
                            if (pObj.has("info") && !pObj.isNull("info")) {
                                val infoObj = pObj.getJSONObject("info")
                                val ik = infoObj.keys()
                                while (ik.hasNext()) {
                                    val k = ik.next()
                                    if (!infoObj.isNull(k)) infoMap[k] = infoObj.get(k)
                                }
                            }
                            result[pId] = PilotData(info = pilotInfoFromMap(infoMap))
                        }
                    }
                    emit(result)
                }
            }
        }
        delay(3000L)
    }
}

fun EnduranceSyncClient.observeSessionState(code: String): Flow<SessionState> = flow {
    while (true) {
        runCatching {
            val res = request("GET", "rooms/$code/session")
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
                    emit(sessionStateFromMap(map))
                }
            }
        }
        delay(3000L)
    }
}

fun EnduranceSyncClient.observeCurrentPilotId(code: String): Flow<String> = flow {
    while (true) {
        runCatching {
            val res = request("GET", "rooms/$code/session/currentPilotId")
            if (res.isNotBlank() && res != "null") {
                val cleaned = res.trim().removeSurrounding("\"")
                emit(cleaned)
            } else {
                emit("")
            }
        }
        delay(3000L)
    }
}

suspend fun EnduranceSyncClient.acceptPilot(code: String, pilotId: String, pilotName: String, uid: String): Result<Unit> = runCatching {
    val info = PilotInfo(pilotId, pilotName, "READY", System.currentTimeMillis())
    request("PUT", "rooms/$code/pilots/$pilotId/info", JSONObject(info.toMap()).toString())
    val member = MemberInfo(role = "pilot", name = pilotName, t = System.currentTimeMillis())
    request("PUT", "rooms/$code/members/$uid", JSONObject(member.toMap()).toString())
    val updateObj = mapOf("status" to "ACCEPTED")
    request("PUT", "rooms/$code/requests/$uid", JSONObject(updateObj).toString())
    request("PUT", "rooms/$code/pending/$uid", JSONObject(updateObj).toString())
}

suspend fun EnduranceSyncClient.rejectPilot(code: String, uid: String): Result<Unit> = runCatching {
    val updateObj = mapOf("status" to "REJECTED")
    request("PUT", "rooms/$code/requests/$uid", JSONObject(updateObj).toString())
    request("PUT", "rooms/$code/pending/$uid", JSONObject(updateObj).toString())
}

suspend fun EnduranceSyncClient.setSessionState(code: String, state: String): Result<Unit> = runCatching {
    request("PUT", "rooms/$code/session/state", JSONObject.quote(state))
}
