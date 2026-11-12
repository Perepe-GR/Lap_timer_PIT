package com.ejemplo.laptimer

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Una vuelta del resumen de carrera.
 * kind: 0 = vuelta normal, 1 = OUT LAP, 2 = IN LAP. n = 0 si esa vuelta no cuenta como numerada.
 */
class RaceLap(val n: Int, val ms: Long, val kind: Int, val driver: String, val stint: Int, val splits: List<Long> = emptyList())

/** Resumen de una carrera (o sesión): se guarda en la app y se sube a Firebase para verlo en el panel. */
class RaceRecord(
    val id: String,
    val mode: String,
    val circuit: String,
    val car: String,
    val startEpoch: Long,
    var endEpoch: Long = 0L,
    /** "target" = se cumplió la meta de vueltas o tiempo; "closed" = se cerró la sesión. */
    var reason: String = "",
    val targetLaps: Int = 0,
    val targetMin: Int = 0,
    val laps: MutableList<RaceLap> = ArrayList(),
    /** Duración de la carrera en el reloj de la app. */
    var durMs: Long = 0L
) {
    /** Solo las vueltas normales (kind == 0) pueden ser BEST. Out/In laps (kind 1, 2) nunca cuentan para bestMs. */
    fun bestMs(): Long? = laps.filter { it.kind == 0 }.minOfOrNull { it.ms }

    fun counted(): Int = laps.count { it.n > 0 }

    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("version", "V2.6.0")
        o.put("id", id)
        o.put("mode", mode)
        o.put("circuit", circuit)
        o.put("car", car)
        o.put("start", startEpoch)
        o.put("end", endEpoch)
        o.put("dur", durMs)
        o.put("reason", reason)
        o.put("tLaps", targetLaps)
        o.put("tMin", targetMin)
        val lapsObj = JSONObject()
        for (l in laps) {
            val lapObj = JSONObject()
            lapObj.put("version", "V2.6.0")
            lapObj.put("n", l.n)
            lapObj.put("ms", l.ms)
            lapObj.put("k", l.kind)
            lapObj.put("d", l.driver)
            lapObj.put("st", l.stint)
            val isComplete = l.splits.size >= 3
            lapObj.put("sectorsComplete", isComplete)
            val secObj = JSONObject()
            if (isComplete) {
                for ((idx, sp) in l.splits.withIndex()) {
                    if (idx < 3) secObj.put((idx + 1).toString(), sp)
                }
            }
            lapObj.put("sectors", secObj)
            lapObj.put("t", startEpoch + l.ms)
            lapsObj.put(l.n.toString(), lapObj)
        }
        o.put("laps", lapsObj)
        return o
    }

    companion object {
        fun fromJson(o: JSONObject): RaceRecord {
            val r = RaceRecord(
                o.optString("id"), o.optString("mode"), o.optString("circuit"), o.optString("car"),
                o.optLong("start"), o.optLong("end"), o.optString("reason"),
                o.optInt("tLaps"), o.optInt("tMin")
            )
            r.durMs = o.optLong("dur")
            val lapsObj = o.optJSONObject("laps")
            if (lapsObj != null) {
                val keys = lapsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val lo = lapsObj.getJSONObject(k)
                    val splits = ArrayList<Long>()
                    val secObj = lo.optJSONObject("sectors")
                    if (secObj != null) {
                        val sks = secObj.keys()
                        val sortedSks = sks.asSequence().toList().sortedBy { it.toIntOrNull() ?: 0 }
                        for (sk in sortedSks) {
                            splits.add(secObj.getLong(sk))
                        }
                    }
                    r.laps.add(RaceLap(lo.optInt("n"), lo.optLong("ms"), lo.optInt("k"), lo.optString("d"), lo.optInt("st"), splits))
                }
            } else {
                val arr = o.optJSONArray("laps") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val a = arr.getJSONArray(i)
                    r.laps.add(RaceLap(a.optInt(0), a.optLong(1), a.optInt(2), a.optString(3), a.optInt(4)))
                }
            }
            return r
        }
    }
}

/** Carreras terminadas (una por archivo) y la carrera en curso (current.json). */
class RaceStore(context: Context) {
    private val dir = File(context.filesDir, "races").apply { mkdirs() }
    private val currentFile = File(dir, "current.json.tmp")

    fun loadCurrent(): RaceRecord? = try {
        if (currentFile.exists()) RaceRecord.fromJson(JSONObject(currentFile.readText())) else null
    } catch (e: Exception) {
        null
    }

    fun saveCurrent(r: RaceRecord) {
        try {
            currentFile.writeText(r.toJson().toString())
        } catch (e: Exception) {
            // sin espacio o sin permisos
        }
    }

    fun archive(r: RaceRecord) {
        try {
            File(dir, r.id + ".json").writeText(r.toJson().toString())
        } catch (e: Exception) {
            // ignorar
        }
        currentFile.delete()
    }

    fun clearCurrent() {
        currentFile.delete()
    }

    fun list(): List<RaceRecord> {
        val files = dir.listFiles { f -> f.name.endsWith(".json") } ?: return emptyList()
        return files.mapNotNull {
            try {
                RaceRecord.fromJson(JSONObject(it.readText()))
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.startEpoch }
    }

    fun delete(id: String) {
        File(dir, "$id.json").delete()
    }
}
