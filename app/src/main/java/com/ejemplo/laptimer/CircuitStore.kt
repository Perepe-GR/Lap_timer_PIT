package com.ejemplo.laptimer

import android.content.Context
import org.json.JSONObject
import java.io.File

/** Stores circuits as local JSON files and syncs with global public Firebase repository /circuits. */
class CircuitStore(private val context: Context) {
    private val dir = File(context.filesDir, "circuits").apply { mkdirs() }
    private val prefs = context.getSharedPreferences("laptimer", Context.MODE_PRIVATE)

    private fun fileFor(name: String) =
        File(dir, name.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".json")

    fun save(c: Circuit) {
        saveLocalOnly(c)
        syncCircuitToFirebase(c)
    }

    private fun saveLocalOnly(c: Circuit) {
        fileFor(c.name).writeText(c.toJson())
    }

    private fun syncCircuitToFirebase(c: Circuit) {
        val settings = NetSettings(context)
        val dbUrl = settings.dbUrl.ifBlank { "https://laptimer-pit-default-rtdb.europe-west1.firebasedatabase.app" }
        val circuitKey = c.name.lowercase().replace(Regex("[^a-z0-9]"), "_")
        val payload = JSONObject().apply {
            put("name", c.name)
            put("signature", c.signature())
            put("sectorCount", c.sectorCount)
            put("geojson", c.toJson())
            put("updatedAt", System.currentTimeMillis())
        }.toString()

        Thread {
            try {
                val token = kotlinx.coroutines.runBlocking {
                    try { com.ejemplo.laptimer.firebase.FirebaseAuthManager.getIdToken() } catch (_: Exception) { null }
                }
                val cleanDb = dbUrl.trimEnd('/')
                val authQuery = if (!token.isNullOrBlank()) "?auth=$token" else ""
                val url = java.net.URL("$cleanDb/circuits/$circuitKey.json$authQuery")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "PUT"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                conn.responseCode
                conn.disconnect()
            } catch (_: Exception) {
                // Ignore offline network failures gracefully
            }
        }.start()
    }

    fun syncFromFirebase() {
        val settings = NetSettings(context)
        val dbUrl = settings.dbUrl.ifBlank { "https://laptimer-pit-default-rtdb.europe-west1.firebasedatabase.app" }
        Thread {
            try {
                val token = kotlinx.coroutines.runBlocking {
                    try { com.ejemplo.laptimer.firebase.FirebaseAuthManager.getIdToken() } catch (_: Exception) { null }
                }
                val cleanDb = dbUrl.trimEnd('/')
                val authQuery = if (!token.isNullOrBlank()) "?auth=$token" else ""
                val url = java.net.URL("$cleanDb/circuits.json$authQuery")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val code = conn.responseCode
                if (code in 200..299) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    if (text.isNotBlank() && text != "null" && text.trim().startsWith("{")) {
                        val root = JSONObject(text)
                        val keys = root.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            if (!root.isNull(key)) {
                                val obj = root.getJSONObject(key)
                                val geojsonStr = obj.optString("geojson", "")
                                if (geojsonStr.isNotBlank()) {
                                    runCatching {
                                        val c = Circuit.fromJson(geojsonStr)
                                        saveLocalOnly(c)
                                    }
                                }
                            }
                        }
                    }
                }
                conn.disconnect()
            } catch (_: Exception) {
                // Ignore offline network failures gracefully
            }
        }.start()
    }

    fun delete(name: String) {
        fileFor(name).delete()
    }

    /** Deletes circuit locally, and from global Firebase DB only if executed by an admin/developer app. */
    fun deleteFromFirebase(name: String, isAdmin: Boolean): Boolean {
        delete(name)
        if (!isAdmin) return false

        val settings = NetSettings(context)
        val dbUrl = settings.dbUrl.ifBlank { "https://laptimer-pit-default-rtdb.europe-west1.firebasedatabase.app" }
        val circuitKey = name.lowercase().replace(Regex("[^a-z0-9]"), "_")

        Thread {
            try {
                val token = kotlinx.coroutines.runBlocking {
                    try { com.ejemplo.laptimer.firebase.FirebaseAuthManager.getIdToken() } catch (_: Exception) { null }
                }
                val cleanDb = dbUrl.trimEnd('/')
                val authQuery = if (!token.isNullOrBlank()) "?auth=$token" else ""
                val url = java.net.URL("$cleanDb/circuits/$circuitKey.json$authQuery")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "DELETE"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.responseCode
                conn.disconnect()
            } catch (_: Exception) {
                // Ignore network failures gracefully
            }
        }.start()
        return true
    }

    fun list(): List<Circuit> {
        val files = dir.listFiles() ?: return emptyList()
        return files
            .filter { it.extension == "json" }
            .mapNotNull { f -> runCatching { Circuit.fromJson(f.readText()) }.getOrNull() }
            .sortedBy { it.name.lowercase() }
    }

    /** Nearest saved circuit to (lat, lon) within maxM meters, or null. */
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
