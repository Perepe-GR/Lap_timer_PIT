package com.ejemplo.laptimer.net

import org.json.JSONObject
import java.security.SecureRandom

data class StintTarget(
    val driver: String = "",
    val minMin: Int = 0,
    val minLaps: Int = 0,
    val maxMin: Int = 0,
    val maxLaps: Int = 0,
    val planLaps: Int = 0,
    val planMin: Int = 0,
    val pitSec: Int = 0
) {
    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("driver", driver)
        o.put("minMin", minMin)
        o.put("minLaps", minLaps)
        o.put("maxMin", maxMin)
        o.put("maxLaps", maxLaps)
        o.put("planLaps", planLaps)
        o.put("planMin", planMin)
        o.put("pitSec", pitSec)
        return o
    }

    companion object {
        fun fromJson(o: JSONObject) = StintTarget(
            o.optString("driver", ""),
            o.optInt("minMin", 0), o.optInt("minLaps", 0),
            o.optInt("maxMin", 0), o.optInt("maxLaps", 0),
            o.optInt("planLaps", 0), o.optInt("planMin", 0),
            o.optInt("pitSec", 0)
        )

        fun newCode(): String {
            val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            val rnd = SecureRandom()
            val sb = StringBuilder()
            for (i in 0 until 10) sb.append(alphabet[rnd.nextInt(alphabet.length)])
            return sb.toString()
        }

        fun keyOf(s: String): String {
            val k = s.trim().lowercase().replace(Regex("[^a-z0-9_-]"), "-").trim('-')
            return if (k.isEmpty()) "x" else k
        }
    }
}
