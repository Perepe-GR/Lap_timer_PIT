package com.ejemplo.laptimer.endurance.sync

import android.util.Log
import com.ejemplo.laptimer.firebase.FirebaseAuthManager
import com.ejemplo.laptimer.util.SecurityUtils
import kotlinx.coroutines.Dispatchers
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

    data class TacticalInstruction(
        val text: String = "",
        val bg: String = "amarillo",
        val t: Long = 0L
    )

    private suspend fun getAuthToken(): String? = FirebaseAuthManager.getIdToken()

    suspend fun request(method: String, path: String, body: String? = null, query: String = ""): String = withContext(Dispatchers.IO) {
        val cleanDb = dbUrl.trimEnd('/')
        val token = getAuthToken()
        val authQuery = if (!token.isNullOrBlank()) {
            if (query.isBlank()) "?auth=$token" else "$query&auth=$token"
        } else {
            query
        }
        val url = URL("$cleanDb/$path.json$authQuery")
        Log.d(TAG, "HTTP $method -> $path")
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
            if (code !in 200..299) throw IOException("HTTP $code")
            text
        } catch (e: Exception) {
            Log.e(TAG, "HTTP Request Error [$method $path]: ${e.message}", e)
            throw e
        } finally {
            c.disconnect()
        }
    }

    suspend fun connectOrCreateRoom(code: String, pin: String, role: String, name: String, circuitId: String): Result<TeamMeta> = runCatching {
        Log.i(TAG, "connectOrCreateRoom for code: $code, role: $role")
        val currentUid = FirebaseAuthManager.ensureAuthenticated()
        val cleanCode = code.trim().uppercase()
        val metaRes = fetchTeamMeta(cleanCode)
        val meta = metaRes.getOrNull()

        if (meta == null) {
            val hashResult = SecurityUtils.hashPin(pin)
            val newMeta = TeamMeta(
                code = cleanCode,
                name = name.ifBlank { cleanCode },
                circuitId = circuitId.ifBlank { "Circuit 1" },
                createdBy = currentUid,
                createdAt = System.currentTimeMillis(),
                pinHash = hashResult.pinHash,
                salt = hashResult.salt,
                leaderUid = currentUid,
                mode = "endurance"
            )
            val jsonPayload = JSONObject(newMeta.toMap()).toString()
            request("PUT", "rooms/$cleanCode/meta", jsonPayload)

            // Dual-Write Authentication: Simultaneously write security metadata to /teams/{cleanCode}
            val teamPayload = JSONObject().apply {
                put("name", newMeta.name)
                put("pinHash", newMeta.pinHash)
                put("salt", newMeta.salt)
                put("updatedAt", System.currentTimeMillis())
            }.toString()
            request("PUT", "teams/$cleanCode", teamPayload)

            request("PUT", "rooms/$cleanCode/strategy/leaderUid", JSONObject().put("uid", currentUid).toString())
            request("PUT", "rooms/$cleanCode/session", JSONObject(SessionState("LOBBY", "", 0L, 0L).toMap()).toString())
            registerMember(cleanCode, currentUid, if (role == "CREW_CHIEF") "crew_chief" else "pilot", name).getOrThrow()
            newMeta
        } else {
            val isValidPin = if (meta.pinHash.isNotBlank()) {
                SecurityUtils.verifyPin(pin, meta.salt, meta.pinHash)
            } else {
                meta.code == pin.trim()
            }
            if (!isValidPin) throw SecurityException("Incorrect PIN for existing room")
            registerMember(cleanCode, currentUid, if (role == "CREW_CHIEF") "crew_chief" else "pilot", name).getOrThrow()
            meta
        }
    }

    suspend fun createTeam(code: String, name: String, circuitId: String, leaderUid: String, pin: String): Result<Unit> = runCatching {
        connectOrCreateRoom(code, pin, "CREW_CHIEF", name, circuitId).getOrThrow()
    }

    suspend fun registerMember(code: String, uid: String, role: String, name: String): Result<Unit> = runCatching {
        val member = MemberInfo(role = role, name = name, t = System.currentTimeMillis())
        request("PUT", "rooms/$code/members/$uid", JSONObject(member.toMap()).toString())
    }

    suspend fun fetchTeamMeta(code: String): Result<TeamMeta?> = runCatching {
        val res = request("GET", "rooms/$code/meta")
        if (res.isBlank() || res == "null") return@runCatching null
        val trimmed = res.trim()
        if (trimmed.startsWith("{")) {
            val obj = JSONObject(trimmed)
            val map = mutableMapOf<String, Any?>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                if (!obj.isNull(k)) map[k] = obj.get(k)
            }
            teamMetaFromMap(map)
        } else {
            null
        }
    }

    suspend fun requestJoin(code: String, pilotId: String, pilotName: String, uid: String): Result<Unit> = runCatching {
        val deviceUid = uid.ifBlank { FirebaseAuthManager.ensureAuthenticated() }
        val pending = PendingPilot(id = pilotId, name = pilotName, uid = deviceUid, status = "PENDING", requestedAt = System.currentTimeMillis())
        val json = JSONObject(pending.toMap()).toString()
        request("PUT", "rooms/$code/requests/$deviceUid", json)
        request("PUT", "rooms/$code/pending/$deviceUid", json)
    }

    suspend fun joinTeam(code: String, inputPin: String, pilotId: String, pilotName: String): Result<TeamMeta> = runCatching {
        val currentUid = FirebaseAuthManager.ensureAuthenticated()
        val cleanCode = code.trim().uppercase()
        val metaRes = fetchTeamMeta(cleanCode)
        val meta = metaRes.getOrThrow() ?: throw IOException("Team code not found")

        val isValidPin = if (meta.pinHash.isNotBlank()) {
            SecurityUtils.verifyPin(inputPin, meta.salt, meta.pinHash)
        } else {
            meta.code == inputPin.trim()
        }
        if (!isValidPin) throw SecurityException("Incorrect PIN")

        requestJoin(cleanCode, pilotId, pilotName, currentUid).getOrThrow()
        meta
    }
}
