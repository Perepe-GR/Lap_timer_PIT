package com.ejemplo.laptimer

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/** Mejores marcas históricas de un circuito (Long.MAX_VALUE = sin dato). */
class CircuitStats(val signature: String, val bestLapMs: Long?, val bestSectors: LongArray)

class Persisted(val stats: CircuitStats, val ref: List<RefSample>?)

/**
 * Guarda por circuito y por ámbito la mejor vuelta, los mejores sectores y la traza de la mejor
 * vuelta (referencia del delta). Ámbito "" = el coche (todos los pilotos); "p_<piloto>" = un piloto.
 * Si el circuito cambia (firma distinta) los datos se ignoran.
 */
class StatsStore(context: Context) {
    private val dir = File(context.filesDir, "stats").apply { mkdirs() }

    private fun key(name: String) = name.replace(Regex("[^A-Za-z0-9_-]"), "_")
    private fun base(name: String, scope: String) =
        key(name) + (if (scope.isEmpty()) "" else "__" + key(scope))

    private fun jsonFile(name: String, scope: String) = File(dir, base(name, scope) + ".json")
    private fun refFile(name: String, scope: String) = File(dir, base(name, scope) + ".ref")

    fun save(c: Circuit, stats: CircuitStats, ref: List<RefSample>?, scope: String = "") {
        try {
            val o = JSONObject()
            o.put("firma", stats.signature)
            val best = stats.bestLapMs
            if (best != null) o.put("mejor", best)
            val arr = JSONArray()
            for (v in stats.bestSectors) arr.put(if (v == Long.MAX_VALUE) -1L else v)
            o.put("sectores", arr)
            jsonFile(c.name, scope).writeText(o.toString())
            if (ref != null) {
                DataOutputStream(BufferedOutputStream(FileOutputStream(refFile(c.name, scope)))).use { out ->
                    out.writeInt(ref.size)
                    for (s in ref) {
                        out.writeInt(s.t.toInt())
                        out.writeFloat(s.x.toFloat())
                        out.writeFloat(s.y.toFloat())
                    }
                }
            }
        } catch (e: Exception) {
            // sin almacenamiento disponible: se ignora
        }
    }

    fun load(c: Circuit, scope: String = ""): Persisted? {
        try {
            val f = jsonFile(c.name, scope)
            if (!f.exists()) return null
            val o = JSONObject(f.readText())
            if (o.optString("firma") != c.signature()) return null
            val arr = o.getJSONArray("sectores")
            val sec = LongArray(arr.length())
            for (i in 0 until arr.length()) {
                val v = arr.getLong(i)
                sec[i] = if (v < 0) Long.MAX_VALUE else v
            }
            val best: Long? = if (o.has("mejor")) o.getLong("mejor") else null
            val stats = CircuitStats(c.signature(), best, sec)
            var ref: List<RefSample>? = null
            val rf = refFile(c.name, scope)
            if (rf.exists()) {
                DataInputStream(BufferedInputStream(FileInputStream(rf))).use { inp ->
                    val n = inp.readInt()
                    val list = ArrayList<RefSample>(n)
                    for (i in 0 until n) {
                        val t = inp.readInt().toLong()
                        val x = inp.readFloat().toDouble()
                        val y = inp.readFloat().toDouble()
                        list.add(RefSample(t, x, y))
                    }
                    ref = list
                }
            }
            return Persisted(stats, ref)
        } catch (e: Exception) {
            return null
        }
    }

    /** Incorpora una vuelta terminada a las marcas de un ámbito. */
    fun update(c: Circuit, scope: String, r: LapResult) {
        val old = load(c, scope)
        val n = c.sectorCount
        val sectors = LongArray(n) { Long.MAX_VALUE }
        var best: Long? = null
        var ref: List<RefSample>? = null
        if (old != null) {
            for (i in 0 until minOf(n, old.stats.bestSectors.size)) sectors[i] = old.stats.bestSectors[i]
            best = old.stats.bestLapMs
            ref = old.ref
        }
        for (i in r.splits.indices) {
            if (i < n && r.splits[i] < sectors[i]) sectors[i] = r.splits[i]
        }
        val b = best
        if (b == null || r.lapMs < b) {
            best = r.lapMs
            ref = r.trace
        }
        save(c, CircuitStats(c.signature(), best, sectors), ref, scope)
    }

    /** Borra las marcas de un circuito en todos los ámbitos. */
    fun clear(name: String) {
        val k = key(name)
        val files = dir.listFiles() ?: return
        for (f in files) {
            if (f.name == "$k.json" || f.name == "$k.ref" || f.name.startsWith(k + "__")) f.delete()
        }
    }
}
