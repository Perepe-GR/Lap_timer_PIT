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
    private var exec: ScheduledExecutorService? = null
    private val pendingLaps = ConcurrentLinkedQueue<String>()
    private var lastKey: String? = null
    private var lastCmdId: String? = null
    private var lastInstTime = 0L
    private var first = true
    private var purged = false

    private val client: FirebaseRestClient get() = FirebaseRestClient(settings.dbUrl, settings.room)

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
        if (!settings.enabled || settings.dbUrl.isBlank() || settings.room.isBlank()) {
            onStatus(NetStatus.OFF)
            return
        }
        first = true
        purged = false
        lastKey = null
        lastCmdId = null
        lastInstTime = 0L
        val e = Executors.newSingleThreadScheduledExecutor()
        exec = e
        val period = settings.intervalS.toLong().coerceAtLeast(10L)
        e.scheduleWithFixedDelay({ cycle() }, 0L, period, TimeUnit.SECONDS)
    }

    fun stop() {
        exec?.shutdownNow()
        exec = null
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

    private fun cycle() {
        try {
            val car = settings.carKey()
            val t = telemetry()
            if (t != null) request("PUT", "live/$car", t, "")
            while (true) {
                val lap = pendingLaps.peek() ?: break
                val key = "L" + JSONObject(lap).optLong("t", System.currentTimeMillis())
                request("PUT", "laps/$car/$key", lap, "")
                pendingLaps.poll()
            }
            fetchMessages(car)
            fetchInstructions()
            fetchCommand(car)
            if (!first && !purged) purgeInbox(car)
            onStatus(NetStatus.OK)
        } catch (_: Exception) {
            onStatus(NetStatus.ERROR)
        }
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
            val room = settings.room.ifBlank { settings.ensureRoom() }
            val txt = request("GET", "rooms/$room/instructions", null, "").trim()
            if (txt.isNotEmpty() && txt != "null") {
                val obj = JSONObject(txt)
                val text = obj.optString("text", "").ifBlank { obj.optString("message", "") }
                val bg = obj.optString("bg", "amarillo")
                val flag = obj.optString("flag", "")
                val t = obj.optLong("t", obj.optLong("timestamp", System.currentTimeMillis()))
                if (text.isNotBlank() && (lastInstTime == 0L || t > lastInstTime)) {
                    lastInstTime = t
                    onMessages(listOf(PitMessage("inst-$t", text, bg, flag)))
                }
            }
        } catch (_: Exception) {}
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
        if (txt.isEmpty() || txt == "null") {
            if (lastCmdId != null) {
                lastCmdId = null
                onCommand(null)
            }
            return
        }
        val o = JSONObject(txt)
        val id = o.optString("id", "")
        val conf = o.optBoolean("conf", false)
        val key = if (conf) "$id!" else id
        if (key != lastCmdId) {
            lastCmdId = key
            onCommand(
                SwapRequest(
                    id, o.optString("next", ""), o.optInt("laps", 0),
                    o.optInt("min", 0), o.optInt("max", 0), o.optInt("pit", 0), conf
                )
            )
        }
    }
}
