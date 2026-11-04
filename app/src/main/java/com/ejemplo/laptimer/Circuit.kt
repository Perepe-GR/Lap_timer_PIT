package com.ejemplo.laptimer

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Ancho por defecto (m) de cada lado de una línea de paso, medido desde el punto marcado. */
const val DEFAULT_HALF_WIDTH_M = 15.0

/**
 * Punto de paso (meta, sector o boxes) con el rumbo de marcha en ese punto.
 * La línea es perpendicular al rumbo y se extiende leftM metros a la izquierda y rightM a la derecha
 * del punto (mirando en el sentido de marcha).
 */
data class GatePoint(
    val lat: Double,
    val lon: Double,
    val headingDeg: Double,
    val leftM: Double = DEFAULT_HALF_WIDTH_M,
    val rightM: Double = DEFAULT_HALF_WIDTH_M
)

/** Vértice de la zona PADDOCK (data class para que la igualdad entre circuitos funcione). */
data class LatLon(val lat: Double, val lon: Double)

/**
 * Un circuito: meta + puntos de sector, y opcionalmente el carril de PIT (línea de entrada y de salida) y una
 * zona PADDOCK (polígono, por si los boxes se quedan pequeños y desvían al paddock).
 * n puntos de sector = n + 1 sectores (el último termina en la meta).
 */
data class Circuit(
    val name: String,
    val finish: GatePoint,
    val sectors: List<GatePoint>,
    val pitIn: GatePoint? = null,
    val pitOut: GatePoint? = null,
    val paddock: List<LatLon> = emptyList()
) {
    /** Hay zona PADDOCK definida (mínimo 3 vértices). */
    val hasPaddock: Boolean get() = paddock.size >= 3

    /** true si (lat, lon) está dentro de la zona PADDOCK (polígono pequeño: basta con grados). */
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

    /** Hay carril de boxes definido (necesita entrada y salida). */
    val hasPitLane: Boolean get() = pitIn != null && pitOut != null

    /**
     * Firma de la geometría de meta y sectores (sin anchos ni boxes): sirve para saber si las marcas
     * guardadas siguen valiendo. Es la misma que en la v0.7, así que cambiar el ancho de una línea
     * o añadir boxes NO borra los récords.
     */
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

/** Guarda los circuitos como archivos JSON en el almacenamiento privado de la app. */
class CircuitStore(context: Context) {
    private val dir = File(context.filesDir, "circuits").apply { mkdirs() }
    private val prefs = context.getSharedPreferences("laptimer", Context.MODE_PRIVATE)

    private fun fileFor(name: String) =
        File(dir, name.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".json")

    fun save(c: Circuit) {
        fileFor(c.name).writeText(c.toJson())
    }

    fun delete(name: String) {
        fileFor(name).delete()
    }

    fun list(): List<Circuit> {
        val files = dir.listFiles() ?: return emptyList()
        return files
            .filter { it.extension == "json" }
            .mapNotNull { f -> runCatching { Circuit.fromJson(f.readText()) }.getOrNull() }
            .sortedBy { it.name.lowercase() }
    }

    /** Circuito guardado más cercano a (lat, lon) dentro de maxM metros, o null. */
    fun nearest(lat: Double, lon: Double, maxM: Double): Circuit? {
        var best: Circuit? = null
        var bestD = maxM
        for (c in list()) {
            val d = distanceM(lat, lon, c.finish.lat, c.finish.lon)
            if (d < bestD) {
                bestD = d
                best = c
            }
        }
        return best
    }

    fun saveLastPosition(lat: Double, lon: Double) {
        prefs.edit().putString("lastLat", lat.toString()).putString("lastLon", lon.toString()).apply()
    }

    fun lastPosition(): Pair<Double, Double>? {
        val a = prefs.getString("lastLat", null)?.toDoubleOrNull() ?: return null
        val b = prefs.getString("lastLon", null)?.toDoubleOrNull() ?: return null
        return Pair(a, b)
    }

    var activeName: String?
        get() = prefs.getString("activo", null)
        set(value) {
            prefs.edit().putString("activo", value).apply()
        }

    fun loadActive(): Circuit? {
        val n = activeName ?: return null
        return list().firstOrNull { it.name == n }
    }
}
