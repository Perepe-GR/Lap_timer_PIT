package com.ejemplo.laptimer

import android.content.Context
import com.ejemplo.laptimer.net.NetSettingsStore
import com.ejemplo.laptimer.net.StintTarget
import com.ejemplo.laptimer.util.ValidationUtils
import org.json.JSONArray

/** Network, session and stint status settings, stored locally on the device. */
class NetSettings(context: Context) {
    private val store = NetSettingsStore(context)
    private val p get() = store.prefs

    // ---- Network ----
    var enabled: Boolean get() = p.getBoolean("enabled", false); set(v) = store.putB("enabled", v)
    var dbUrl: String get() = p.getString("db", "https://laptimer-pit-default-rtdb.europe-west1.firebasedatabase.app") ?: "https://laptimer-pit-default-rtdb.europe-west1.firebasedatabase.app"; set(v) = store.putS("db", v.trim().trimEnd('/'))
    var room: String get() = p.getString("room", "") ?: ""; set(v) = store.putS("room", v.trim().uppercase())
    var pin: String get() = p.getString("pin", "1234") ?: "1234"; set(v) = store.putS("pin", v.trim())
    var pinHash: String get() = p.getString("pinHash", "") ?: ""; set(v) = store.putS("pinHash", v)
    var salt: String get() = p.getString("salt", "") ?: ""; set(v) = store.putS("salt", v)
    var intervalS: Int get() = p.getInt("interval", 1); set(v) = store.putI("interval", v.coerceAtLeast(1))
    var panelUrl: String get() = p.getString("panel", "https://user.github.io/pit-panel/") ?: "https://user.github.io/pit-panel/"; set(v) = store.putS("panel", v.trim())
    var saveTrace: Boolean get() = p.getBoolean("trace", true); set(v) = store.putB("trace", v)

    // ---- Identity ----
    var carName: String get() = p.getString("carName", "Car 1") ?: "Car 1"; set(v) = store.putS("carName", v.trim().ifEmpty { "Car 1" })
    var driverName: String get() = p.getString("driver", "Driver 1") ?: "Driver 1"; set(v) = store.putS("driver", v.trim().ifEmpty { "Driver 1" })
    fun carKey(): String = StintTarget.keyOf(carName)

    // ---- Endurance Session Persistence ----
    var activeRoomCode: String get() = p.getString("activeRoomCode", "") ?: ""; set(v) = store.putS("activeRoomCode", v.trim().uppercase())
    var userRole: String get() = p.getString("userRole", "") ?: ""; set(v) = store.putS("userRole", v)
    var deviceUid: String
        get() {
            var uid = p.getString("deviceUid", "") ?: ""
            if (uid.isBlank()) {
                val model = android.os.Build.MODEL?.replace(Regex("[^a-zA-Z0-9]"), "_") ?: "device"
                uid = "dev_${model}_${java.util.UUID.randomUUID().toString().substring(0, 8)}"
                store.putS("deviceUid", uid)
            }
            return uid
        }
        set(v) = store.putS("deviceUid", v)
    var sessionActive: Boolean get() = p.getBoolean("sessionActive", false); set(v) = store.putB("sessionActive", v)

    fun clearEnduranceSession() {
        activeRoomCode = ""
        userRole = ""
        deviceUid = ""
        sessionActive = false
    }

    fun saveEnduranceSession(roomCode: String, role: String, uid: String) {
        activeRoomCode = roomCode
        room = roomCode
        userRole = role
        deviceUid = uid
        sessionActive = true
    }

    // ---- Session Settings ----
    var lockKmh: Int get() = p.getInt("lockKmh", 10); set(v) = store.putI("lockKmh", v.coerceAtLeast(0))
    var rangePct: Float get() = p.getFloat("rangePct", 2f); set(v) = store.putF("rangePct", v.coerceIn(0.5f, 10f))
    var msgSectors: Int get() = p.getInt("msgSectors", 2); set(v) = store.putI("msgSectors", v.coerceIn(1, 6))
    var msgSec: Int get() = p.getInt("msgSec", 15); set(v) = store.putI("msgSec", v.coerceIn(5, 120))
    var batWarn: Int get() = p.getInt("batWarn", 20); set(v) = store.putI("batWarn", v.coerceIn(1, 90))
    var batCrit: Int get() = p.getInt("batCrit", 10); set(v) = store.putI("batCrit", v.coerceIn(1, 90))

    // ---- Race Mode ----
    var raceMode: String get() = p.getString("raceMode", "trackday") ?: "trackday"; set(v) = store.putS("raceMode", v)
    fun isEndurance(): Boolean = raceMode == "resistencia"
    fun isLongRun(): Boolean = raceMode == "long_run"
    fun isSprint(): Boolean = raceMode == "sprint"
    fun isTrackDay(): Boolean = raceMode == "trackday"

    var sprintLaps: Int get() = p.getInt("sprintLaps", 0); set(v) = store.putI("sprintLaps", v.coerceAtLeast(0))
    var sprintMin: Int get() = p.getInt("sprintMin", 0); set(v) = store.putI("sprintMin", v.coerceAtLeast(0))
    var outlapCounts: Boolean get() = p.getBoolean("outlapCounts", true); set(v) = store.putB("outlapCounts", v)
    var inlapCounts: Boolean get() = p.getBoolean("inlapCounts", true); set(v) = store.putB("inlapCounts", v)

    fun raceModeLabel(): String = when (raceMode) {
        "sprint" -> "Sprint"
        "resistencia" -> "Endurance"
        "long_run" -> "Long Run"
        else -> "Track day"
    }

    var gpsSource: String get() = p.getString("gpsSource", "usb") ?: "usb"; set(v) = store.putS("gpsSource", v)
    fun gpsSourceLabel(): String = when (gpsSource) {
        "internal" -> "Internal GPS (Phone / Tablet)"
        "racebox" -> "RaceBox (BLE / Mock)"
        "dragy" -> "Dragy (10Hz / 25Hz)"
        "garmin" -> "Garmin (Catalyst / GLO)"
        "external_bt" -> "External Bluetooth GPS (NMEA)"
        else -> "USB u-blox Antenna (25 Hz)"
    }

    var lapSummarySec: Int get() = p.getInt("lapSummarySec", 4); set(v) = store.putI("lapSummarySec", v.coerceIn(3, 6))
    var pitMinSec: Int get() = p.getInt("pitMinSec", 0); set(v) = store.putI("pitMinSec", v.coerceAtLeast(0))

    var rosterText: String get() = p.getString("roster", "") ?: ""; set(v) = store.putS("roster", v)
    fun roster(): List<String> = rosterText.split('\n').map { it.trim() }.filter { it.isNotEmpty() }

    var pitStops: Int get() = p.getInt("pitStops", 0); set(v) = store.putI("pitStops", v)
    var pitFails: Int get() = p.getInt("pitFails", 0); set(v) = store.putI("pitFails", v)

    fun pitLog(): List<String> = (p.getString("pitLog", "") ?: "").split('\n').filter { it.isNotBlank() }

    fun addPitLog(stintNo: Int, durMs: Long, minSec: Int, ok: Boolean) {
        val l = pitLog().toMutableList()
        l.add("$stintNo|$durMs|$minSec|${if (ok) 1 else 0}|${System.currentTimeMillis()}")
        store.putS("pitLog", l.takeLast(60).joinToString("\n"))
    }

    var pitLastMs: Long get() = p.getLong("pitLastMs", 0L); set(v) = store.putL("pitLastMs", v)
    var refScope: String get() = p.getString("refScope", "coche") ?: "coche"; set(v) = store.putS("refScope", v)

    var defPlanLaps: Int get() = p.getInt("defPlan", 0); set(v) = store.putI("defPlan", v.coerceAtLeast(0))
    var defMinMin: Int get() = p.getInt("defMin", 0); set(v) = store.putI("defMin", v.coerceAtLeast(0))
    var defMaxMin: Int get() = p.getInt("defMax", 0); set(v) = store.putI("defMax", v.coerceAtLeast(0))

    var stintNumber: Int get() = p.getInt("stintNumber", 1); set(v) = store.putI("stintNumber", v)
    var stintLapsDone: Int get() = p.getInt("stintLaps", 0); set(v) = store.putI("stintLaps", v)
    var stintStartEpoch: Long get() = p.getLong("stintStart", 0L); set(v) = store.putL("stintStart", v)

    var stintPlan: Int get() = p.getInt("stintPlan", -1); set(v) = store.putI("stintPlan", v)
    var stintMin: Int get() = p.getInt("stintMin", -1); set(v) = store.putI("stintMin", v)
    var stintMax: Int get() = p.getInt("stintMax", -1); set(v) = store.putI("stintMax", v)
    var stintMinLaps: Int get() = p.getInt("stintMinLaps", -1); set(v) = store.putI("stintMinLaps", v)
    var stintMaxLaps: Int get() = p.getInt("stintMaxLaps", -1); set(v) = store.putI("stintMaxLaps", v)
    var stintPlanMin: Int get() = p.getInt("stintPlanMin", -1); set(v) = store.putI("stintPlanMin", v)

    var raceMin: Int get() = p.getInt("raceMin", 0); set(v) = store.putI("raceMin", v.coerceAtLeast(0))
    var demoStopSec: Int get() = p.getInt("demoStopSec", 0); set(v) = store.putI("demoStopSec", v.coerceAtLeast(0))

    var strategyJson: String get() = p.getString("strategy", "[]") ?: "[]"; set(v) = store.putS("strategy", v)

    fun strategy(): List<StintTarget> {
        return try {
            val arr = JSONArray(strategyJson)
            (0 until arr.length()).map { StintTarget.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveStrategy(list: List<StintTarget>) {
        val arr = JSONArray()
        for (t in list) arr.put(t.toJson())
        strategyJson = arr.toString()
    }

    fun targetFor(n: Int): StintTarget {
        val list = strategy()
        if (n >= 1 && n <= list.size) return list[n - 1]
        return StintTarget(minMin = defMinMin, maxMin = defMaxMin, planLaps = defPlanLaps)
    }

    fun plannedDriver(n: Int): String? {
        val list = strategy()
        if (n >= 1 && n <= list.size && list[n - 1].driver.isNotBlank()) return list[n - 1].driver
        return null
    }

    fun resetStint() {
        stintNumber = 1
        stintLapsDone = 0
        stintStartEpoch = 0L
        stintPlan = -1
        stintMin = -1
        stintMax = -1
        stintMinLaps = -1
        stintMaxLaps = -1
        stintPlanMin = -1
        pitStops = 0
        pitFails = 0
        pitLastMs = 0L
        store.putS("pitLog", "")
    }

    fun ensureRoom(): String {
        var r = room
        if (r.isBlank() || (r.length == 10 && !ValidationUtils.isValidIdentifier(r))) {
            r = "TORQUE TEAM"
            room = r
        }
        return r
    }

    companion object {
        fun keyOf(s: String): String = StintTarget.keyOf(s)
    }
}
