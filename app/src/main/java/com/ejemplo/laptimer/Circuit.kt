package com.ejemplo.laptimer

import org.json.JSONArray
import org.json.JSONObject

/** Default half width (m) for a crossing line measured from center point. */
const val DEFAULT_HALF_WIDTH_M = 15.0

/**
 * Crossing gate point (finish, sector or pit lane) with heading direction.
 * Line is perpendicular to heading and extends leftM to left and rightM to right.
 */
data class GatePoint(
    val lat: Double,
    val lon: Double,
    val headingDeg: Double,
    val leftM: Double = DEFAULT_HALF_WIDTH_M,
    val rightM: Double = DEFAULT_HALF_WIDTH_M
)

/** Vertex coordinates of PADDOCK polygon area. */
data class LatLon(val lat: Double, val lon: Double)

/**
 * Circuit model: finish gate + sector points, optional pit lane and paddock area polygon.
 */
data class Circuit(
    val name: String,
    val finish: GatePoint,
    val sectors: List<GatePoint>,
    val pitIn: GatePoint? = null,
    val pitOut: GatePoint? = null,
    val paddock: List<LatLon> = emptyList()
) {
    /** True if paddock area is defined (minimum 3 vertices). */
    val hasPaddock: Boolean get() = paddock.size >= 3

    /** Returns true if (lat, lon) is inside paddock polygon. */
    fun inPaddock(lat: Double, lon: Double): Boolean {
        if (!hasPaddock) return false
        var inside = false
        var j = paddock.size - 1
        for (i in paddock.indices) {
            val a = paddock[i]
            val b = paddock[j]
            if ((a.lat > lat) != (b.lat > lat) &&
                lon < (b.lon - a.lon) * (lat - a.lat) / (b.lat - a.lat) + a.lon
            ) inside = !inside
            j = i
        }
        return inside
    }

    val sectorCount: Int get() = sectors.size + 1

    /** True if pit lane is defined (needs both pitIn and pitOut). */
    val hasPitLane: Boolean get() = pitIn != null && pitOut != null

    /** Geometry signature for finish and sectors. */
    fun signature(): String = legacyJson().hashCode().toString()

    private fun legacyJson(): String {
        val o = JSONObject()
        o.put("nombre", name)
        o.put("meta", legacyPoint(finish))
        val arr = JSONArray()
        for (p in sectors) arr.put(legacyPoint(p))
        o.put("sectores", arr)
        return o.toString(2)
    }

    fun toJson(): String {
        val o = JSONObject()
        o.put("nombre", name)
        o.put("meta", pointToJson(finish))
        val arr = JSONArray()
        for (p in sectors) arr.put(pointToJson(p))
        o.put("sectores", arr)
        pitIn?.let { o.put("boxesEntrada", pointToJson(it)) }
        pitOut?.let { o.put("boxesSalida", pointToJson(it)) }
        if (hasPaddock) {
            val pa = JSONArray()
            for (v in paddock) pa.put(JSONArray().put(v.lat).put(v.lon))
            o.put("paddock", pa)
        }
        return o.toString(2)
    }

    companion object {
        private fun legacyPoint(p: GatePoint): JSONObject {
            val o = JSONObject()
            o.put("lat", p.lat)
            o.put("lon", p.lon)
            o.put("rumbo", p.headingDeg)
            return o
        }

        private fun pointToJson(p: GatePoint): JSONObject {
            val o = legacyPoint(p)
            o.put("anchoIzq", p.leftM)
            o.put("anchoDer", p.rightM)
            return o
        }

        private fun pointFromJson(o: JSONObject): GatePoint =
            GatePoint(
                o.getDouble("lat"), o.getDouble("lon"), o.getDouble("rumbo"),
                o.optDouble("anchoIzq", DEFAULT_HALF_WIDTH_M),
                o.optDouble("anchoDer", DEFAULT_HALF_WIDTH_M)
            )

        fun fromJson(text: String): Circuit {
            val o = JSONObject(text)
            val arr = o.optJSONArray("sectores") ?: JSONArray()
            val list = ArrayList<GatePoint>()
            for (i in 0 until arr.length()) list.add(pointFromJson(arr.getJSONObject(i)))
            val pin = o.optJSONObject("boxesEntrada")?.let { pointFromJson(it) }
            val pout = o.optJSONObject("boxesSalida")?.let { pointFromJson(it) }
            val pad = ArrayList<LatLon>()
            val pa = o.optJSONArray("paddock")
            if (pa != null) {
                for (i in 0 until pa.length()) {
                    val v = pa.getJSONArray(i)
                    pad.add(LatLon(v.getDouble(0), v.getDouble(1)))
                }
            }
            return Circuit(o.getString("nombre"), pointFromJson(o.getJSONObject("meta")), list, pin, pout, pad)
        }
    }
}
