package com.ejemplo.laptimer.endurance.sync

import android.util.Log
import com.ejemplo.laptimer.firebase.FirebaseAuthManager
import com.ejemplo.laptimer.firebase.HashUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class EnduranceSyncClient(
    private val dbUrl: String = "https://laptimer-pit-default-rtdb.europe-west1.firebasedatabase.app"
) {

    companion object {
        private const val TAG = "EnduranceSyncClient"
    }

    private suspend fun getAuthToken(): String? = FirebaseAuthManager.getIdToken()

    private suspend fun request(method: String, path: String, body: String? = null, query: String = ""): String = withContext(Dispatchers.IO) {
        val cleanDb = dbUrl.trimEnd('/')
        val token = getAuthToken()
        val authQuery = if (!token.isNullOrBlank()) {
            if (query.isBlank()) "?auth=$token" else "$query&auth=$token"
        } else {
            query
        }
        val url = URL("$cleanDb/$path.json$authQuery")
        Log.d(TAG, "HTTP $method -> $path (auth token attached: ${!token.isNullOrBlank()})")
        val c = url.openConnection() as HttpURLConnection
        try {
            c.requestMethod = method
            c.connectTimeout = 4000
            c.readTimeout = 4000
            if (body != null) {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            Log.d(TAG, "HTTP $code <- $path")
            if (code !in 200..299) throw IOException("HTTP $code")
            text
        } catch (e: Exception) {
            Log.e(TAG, "HTTP Request Error [$method $path] on $url: ${e.message}", e)
            throw e
        } finally {
            c.disconnect()
        }
    }

    suspend fun createTeam(code: String, name: String, circuitId: String, leaderUid: String, pin: String): Result<Unit> = runCatching {
        Log.i(TAG, "createTeam STARTED for code: $code, name: $name, circuit: $circuitId")
        val currentUid = FirebaseAuthManager.ensureAuthenticated()
        Log.i(TAG, "createTeam Auth Verified: currentUid=$currentUid")
        val actualLeaderUid = currentUid
        val salt = HashUtils.generateSalt()
        val pinHash = HashUtils.sha256(salt, pin)

        val meta = TeamMeta(
            code = code,
            name = name,
            circuitId = circuitId,
            createdBy = actualLeaderUid,
            createdAt = System.currentTimeMillis(),
            pinHash = pinHash,
            salt = salt,
            leaderUid = actualLeaderUid,
            mode = "endurance"
        )
        val jsonPayload = JSONObject(meta.toMap()).toString()

        Log.d(TAG, "Writing /rooms/$code/meta...")
        request("PUT", "rooms/$code/meta", jsonPayload)

        Log.d(TAG, "Writing /rooms/$code/strategy/leaderUid...")
        request("PUT", "rooms/$code/strategy/leaderUid", JSONObject().put("uid", actualLeaderUid).toString())

        Log.d(TAG, "Writing /rooms/$code/session...")
        request("PUT", "rooms/$code/session", JSONObject(SessionState("LOBBY", "", 0L, 0L).toMap()).toString())

        Log.d(TAG, "Registering member under /rooms/$code/members/$actualLeaderUid...")
        registerMember(code, actualLeaderUid, "crew_chief", name)

        Log.i(TAG, "createTeam SUCCESS for room $code with leaderUid=$actualLeaderUid")
    }

    suspend fun registerMember(code: String, uid: String, role: String, name: String): Result<Unit> = runCatching {
        val member = MemberInfo(role = role, name = name, t = System.currentTimeMillis())
        request("PUT", "rooms/$code/members/$uid", JSONObject(member.toMap()).toString())
    }

    suspend fun fetchTeamMeta(code: String): Result<TeamMeta?> = runCatching {
        Log.d(TAG, "fetchTeamMeta for code: $code")
        val res = request("GET", "rooms/$code/meta")
        if (res.isBlank() || res == "null") return@runCatching null
        val trimmed = res.trim()
        if (trimmed.startsWith("{")) {
            val obj = JSONObject(trimmed)
            val map = mutableMapOf<String, Any?>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                if (!obj.isNull(k)) {
                    map[k] = obj.get(k)
                }
            }
            teamMetaFromMap(map)
        } else {
            null
        }
    }

    suspend fun requestJoin(code: String, pilotId: String, pilotName: String, uid: String): Result<Unit> = runCatching {
        val pending = PendingPilot(id = pilotId, name = pilotName, uid = uid, requestedAt = System.currentTimeMillis())
        request("PUT", "rooms/$code/pending/$pilotId", JSONObject(pending.toMap()).toString())
    }

    suspend fun joinTeam(code: String, pilotId: String, pilotName: String): Result<Unit> = runCatching {
        val currentUid = FirebaseAuthManager.ensureAuthenticated()
        Log.i(TAG, "joinTeam (Request Access): code=$code, pilotId=$pilotId, pilotName=$pilotName, currentUid=$currentUid")
        requestJoin(code, pilotId, pilotName, currentUid).getOrThrow()
    }

    fun observePendingPilots(code: String): Flow<Map<String, PendingPilot>> = flow {
        while (true) {
            runCatching {
                val res = request("GET", "rooms/$code/pending")
                if (res.isNotBlank() && res != "null") {
                    val trimmed = res.trim()
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

    suspend fun acceptPilot(code: String, pilotId: String, pilotName: String, uid: String): Result<Unit> = runCatching {
        val info = PilotInfo(pilotId, pilotName, "READY", System.currentTimeMillis())
        request("PUT", "rooms/$code/pilots/$pilotId/info", JSONObject(info.toMap()).toString())
        val member = MemberInfo(role = "pilot", name = pilotName, t = System.currentTimeMillis())
        request("PUT", "rooms/$code/members/$uid", JSONObject(member.toMap()).toString())
        request("PUT", "rooms/$code/pending/$pilotId", "null")
    }

    suspend fun setSessionState(code: String, state: String): Result<Unit> = runCatching {
        request("PUT", "rooms/$code/session/state", JSONObject.quote(state))
    }

    fun observeSessionState(code: String): Flow<SessionState> = flow {
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

    fun observeStrategy(code: String): Flow<StrategyBase> = flow {
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

    fun observePilots(code: String): Flow<Map<String, PilotData>> = flow {
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

    suspend fun pushStint(code: String, stint: Stint): Result<Unit> = runCatching {
        val path = "rooms/$code/stints/${stint.startTs}"
        request("PUT", path, JSONObject(stint.toMap()).toString())
    }

    fun observeStints(code: String): Flow<List<Stint>> = flow {
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

    suspend fun updateStrategy(code: String, strategy: StrategyBase): Result<Unit> = runCatching {
        request("PUT", "rooms/$code/strategy", JSONObject(strategy.toMap()).toString())
    }

    suspend fun setCurrentPilot(code: String, pilotId: String): Result<Unit> = runCatching {
        request("PUT", "rooms/$code/session/currentPilotId", JSONObject.quote(pilotId))
    }

    suspend fun pushLap(code: String, pilotId: String, lap: LapRecord): Result<Unit> = runCatching {
        request("PUT", "rooms/$code/pilots/$pilotId/laps/${lap.lapNumber}", JSONObject(lap.toMap()).toString())
    }

    suspend fun updateCurrentLap(code: String, pilotId: String, current: CurrentLap): Result<Unit> = runCatching {
        val map = mapOf("startTs" to current.startTs, "sectorsMs" to current.sectorsMs, "gpsValid" to current.gpsValid)
        request("PUT", "rooms/$code/pilots/$pilotId/currentLap", JSONObject(map).toString())
    }

    suspend fun setLeader(code: String, uid: String): Result<Unit> = runCatching {
        request("PUT", "rooms/$code/strategy/leaderUid", JSONObject().put("uid", uid).toString())
    }
}
