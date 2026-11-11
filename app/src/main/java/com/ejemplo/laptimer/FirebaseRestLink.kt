package com.ejemplo.laptimer

import com.ejemplo.laptimer.firebase.FirebaseAuthManager
import com.ejemplo.laptimer.firebase.FirebaseRestClient
import com.ejemplo.laptimer.firebase.NetStatus
import com.ejemplo.laptimer.firebase.PitMessage
import com.ejemplo.laptimer.firebase.SwapRequest
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

private const val INBOX_KEEP = 20

class FirebaseRestLink(
    private val settings: NetSettings,
    private val telemetry: () -> String?,
    private val onMessages: (List<PitMessage>) -> Unit,
    private val onCommand: (SwapRequest?) -> Unit,
    private val onStatus: (NetStatus) -> Unit
) {
    private var telemetryExec: ScheduledExecutorService? = null
    private var pitExec: ScheduledExecutorService? = null
    private val pendingLaps = ConcurrentLinkedQueue<String>()
    private var lastKey: String? = null
    private var lastCmdId: String? = null
    private var lastInstTime = 0L
    private var lastInstFingerprint: String? = null
    private var first = true
    private var purged = false
    private var firstInstruction = true

    private val client: FirebaseRestClient get() = FirebaseRestClient(settings.dbUrl, settings.activeRoomCode.ifBlank { settings.room.ifBlank { settings.ensureRoom() } })

    private fun request(method: String, path: String, body: String?, query: String): String {
        val token = runBlocking {
            try { FirebaseAuthManager.getIdToken() } catch (_: Exception) { null }
        }
        return client.request(method, path, body, query, token)
    }

    fun enqueueLap(json: String) {
        pendingLaps.add(json)
        while (pendingLaps.size > 300) pendingLaps.poll()
    }

    fun start() {
        stop()
        val roomCode = settings.activeRoomCode.ifBlank { settings.room.ifBlank { settings.ensureRoom() } }
        if (settings.dbUrl.isBlank() || roomCode.isBlank()) {
            onStatus(NetStatus.OFF)
            return
        }
        first = true
        purged = false
        firstInstruction = true
        lastKey = null
        lastCmdId = null
        lastInstTime = 0L

        val telExec = Executors.newSingleThreadScheduledExecutor()
        telemetryExec = telExec
        telExec.scheduleWithFixedDelay({ uploadTelemetryCycle() }, 0L, 500L, TimeUnit.MILLISECONDS)

        val pExec = Executors.newSingleThreadScheduledExecutor()
        pitExec = pExec
        pExec.scheduleWithFixedDelay({ pitPollCycle() }, 0L, 1L, TimeUnit.SECONDS)
    }

    fun stop() {
        telemetryExec?.shutdownNow()
        telemetryExec = null
        pitExec?.shutdownNow()
        pitExec = null
    }

    fun uploadRaceNow(id: String, json: String) {
        if (!settings.enabled || settings.dbUrl.isBlank() || settings.room.isBlank()) return
        val car = settings.carKey()
        Thread {
            try { request("PUT", "races/$car/$id", json, "") } catch (_: Exception) {}
        }.start()
    }

    fun clearCommandNow() {
        val car = settings.carKey()
        Thread {
            try { request("PUT", "cmd/$car", "null", "") } catch (_: Exception) {}
        }.start()
    }

    private fun uploadTelemetryCycle() {
        try {
            val car = settings.carKey()
            if (settings.enabled) {
                val t = telemetry()
                if (t != null) request("PUT", "live/$car", t, "")
                while (true) {
                    val lap = pendingLaps.peek() ?: break
                    val key = "L" + JSONObject(lap).optLong("t", System.currentTimeMillis())
                    request("PUT", "laps/$car/$key", lap, "")
                    pendingLaps.poll()
                }
                onStatus(NetStatus.OK)
            } else {
                onStatus(NetStatus.OFF)
            }
        } catch (_: Exception) {
            onStatus(NetStatus.ERROR)
        }
    }

    private fun pitPollCycle() {
        try {
            val car = settings.carKey()
            fetchMessages(car)
            fetchInstructions()
            fetchCommand(car)
            if (settings.enabled && !first && !purged) purgeInbox(car)
        } catch (_: Exception) {}
    }

    private fun fetchMessages(car: String) {
        val txt = request("GET", "inbox/$car", null, "?orderBy=%22%24key%22&limitToLast=5").trim()
        val keys: List<String>
        var o: JSONObject? = null
        if (txt.isEmpty() || txt == "null") {
            keys = emptyList()
        } else {
            val obj = JSONObject(txt)
            o = obj
            val list = ArrayList<String>()
            val it = obj.keys()
            while (it.hasNext()) list.add(it.next())
            list.sort()
            keys = list
        }
        if (first) {
            lastKey = keys.lastOrNull()
            first = false
            return
        }
        val seen = lastKey
        val fresh = keys.filter { seen == null || it > seen }
        if (fresh.isEmpty() || o == null) return
        lastKey = fresh.last()
        val msgs = ArrayList<PitMessage>()
        for (k in fresh) {
            val m = o.getJSONObject(k)
            val text = m.optString("text", m.optString("message", ""))
            val bg = m.optString("bg", "amarillo")
            val flag = m.optString("flag", "")
            if (text.isNotBlank()) msgs.add(PitMessage(k, text, bg, flag))
        }
        if (msgs.isNotEmpty()) onMessages(msgs)
    }

    private fun fetchInstructions() {
        try {
            val room = settings.activeRoomCode.ifBlank { settings.room.ifBlank { settings.ensureRoom() } }
            val txt = request("GET", "rooms/$room/instructions", null, "").trim()
            android.util.Log.d("FirebaseRestLink", "Fetched instructions raw: $txt")
            if (txt.isNotEmpty() && txt != "null" && txt.startsWith("{")) {
                val obj = JSONObject(txt)
                val text = obj.optString("text", "").ifBlank { obj.optString("message", "") }
                val bg = obj.optString("bg", "amarillo")
                val flag = obj.optString("flag", "")
                val targetCar = obj.optString("targetCar", "ALL").ifBlank { obj.optString("carId", "ALL") }
                val t = obj.optLong("t", obj.optLong("timestamp", 0L))

                val carKey = settings.carKey().uppercase()
                val carName = settings.carName.uppercase()
                val driverName = settings.driverName.uppercase()
                val deviceUid = settings.deviceUid.uppercase()
                val target = targetCar.uppercase().trim()

                val isForUs = target == "ALL" ||
                              target == "FOR_ALL" ||
                              target == carKey ||
                              target == carName ||
                              target == driverName ||
                              target == deviceUid ||
                              carName.contains(target) ||
                              driverName.contains(target) ||
                              target.contains(carName)

                android.util.Log.i("FirebaseRestLink", "Instruction target: '$target', App carName: '$carName', driver: '$driverName', deviceUid: '$deviceUid' -> isForUs: $isForUs")

                val fingerprint = "$t:$text:$bg:$flag:$targetCar"
                if (firstInstruction) {
                    lastInstFingerprint = fingerprint
                    firstInstruction = false
                    return
                }
                if (isForUs && text.isNotBlank() && fingerprint != lastInstFingerprint) {
                    lastInstFingerprint = fingerprint
                    val msgId = if (t > 0L) "inst-$t" else "inst-${fingerprint.hashCode()}"
                    onMessages(listOf(PitMessage(msgId, text, bg, flag)))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRestLink", "Error fetching instructions: ${e.message}", e)
        }
    }

    private fun purgeInbox(car: String) {
        purged = true
        val txt = request("GET", "inbox/$car", null, "?shallow=true").trim()
        if (txt.isEmpty() || txt == "null") return
        val obj = JSONObject(txt)
        val keys = ArrayList<String>()
        val it = obj.keys()
        while (it.hasNext()) keys.add(it.next())
        if (keys.size <= INBOX_KEEP) return
        keys.sort()
        val old = keys.subList(0, keys.size - INBOX_KEEP).take(60)
        for (k in old) request("PUT", "inbox/$car/$k", "null", "")
    }

    private fun fetchCommand(car: String) {
        val txt = request("GET", "cmd/$car", null, "").trim()
        if (txt.isEmpty() || txt == "null") return
        try {
            val m = JSONObject(txt)
            val id = m.optString("id", "")
            if (id.isBlank() || id == lastCmdId) return
            lastCmdId = id
            val next = m.optString("next", m.optString("driver", "Driver 2"))
            val laps = m.optInt("laps", 0)
            val minMin = m.optInt("minMin", 0)
            val maxMin = m.optInt("maxMin", 0)
            val pitSec = m.optInt("pitSec", 0)
            val confirmed = m.optBoolean("confirmed", false)
            val req = SwapRequest(id, next, laps, minMin, maxMin, pitSec, confirmed)
            onCommand(req)
        } catch (_: Exception) {}
    }
}
