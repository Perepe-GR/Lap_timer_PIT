package com.ejemplo.laptimer

import org.json.JSONArray
import org.json.JSONObject

/** Una vuelta de una sesión. Los offsets son milisegundos desde el inicio de la sesión. */
class LapRecord(
    val number: Int,
    val lapMs: Long,
    val splits: List<Long>,
    val startOffsetMs: Long,
    val endOffsetMs: Long
)

class SessionRecord(
    val id: String,
    val circuitName: String,
    val startEpochMs: Long,
    val sectorCount: Int,
    val laps: MutableList<LapRecord> = ArrayList(),
    var maxSpeedKmh: Double = 0.0,
    var distanceM: Double = 0.0,
    var durationMs: Long = 0L,
    val driver: String = "",
    val stint: Int = 0,
    val car: String = ""
) {
    fun bestLap(): LapRecord? = laps.minByOrNull { it.lapMs }

    fun toJson(): String {
        val o = JSONObject()
        o.put("id", id)
        o.put("circuito", circuitName)
        o.put("inicio", startEpochMs)
        o.put("sectores", sectorCount)
        o.put("vmax", maxSpeedKmh)
        o.put("dist", distanceM)
        o.put("dur", durationMs)
        o.put("piloto", driver)
        o.put("stint", stint)
        o.put("coche", car)
        val arr = JSONArray()
        for (l in laps) {
            val lo = JSONObject()
            lo.put("n", l.number)
            lo.put("ms", l.lapMs)
            val sp = JSONArray()
            for (s in l.splits) sp.put(s)
            lo.put("s", sp)
            lo.put("a", l.startOffsetMs)
            lo.put("b", l.endOffsetMs)
            arr.put(lo)
        }
        o.put("vueltas", arr)
        return o.toString()
    }

    companion object {
        fun fromJson(text: String): SessionRecord {
            val o = JSONObject(text)
            val r = SessionRecord(
                o.getString("id"), o.getString("circuito"), o.getLong("inicio"), o.getInt("sectores"),
                driver = o.optString("piloto", ""), stint = o.optInt("stint", 0), car = o.optString("coche", "")
            )
            r.maxSpeedKmh = o.optDouble("vmax", 0.0)
            r.distanceM = o.optDouble("dist", 0.0)
            r.durationMs = o.optLong("dur", 0L)
            val arr = o.getJSONArray("vueltas")
            for (i in 0 until arr.length()) {
                val lo = arr.getJSONObject(i)
                val sp = lo.getJSONArray("s")
                val splits = ArrayList<Long>()
                for (k in 0 until sp.length()) splits.add(sp.getLong(k))
                r.laps.add(LapRecord(lo.getInt("n"), lo.getLong("ms"), splits, lo.getLong("a"), lo.getLong("b")))
            }
            return r
        }
    }
}
