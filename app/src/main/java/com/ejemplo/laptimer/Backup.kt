package com.ejemplo.laptimer

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Copia de seguridad en un único zip: circuitos, marcas y referencias, sesiones (con o sin trazas GPS)
 * y ajustes. Al restaurar se añaden los archivos que falten y se sustituyen los que tengan el mismo nombre.
 */
object Backup {
    private val DIRS = listOf("circuits", "stats", "sessions")
    private val PREFS = listOf("laptimer", "laptimer_net")

    fun create(context: Context, includeTraces: Boolean): File {
        val dir = File(context.cacheDir, "export")
        dir.mkdirs()
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        val out = File(dir, "laptimer-copia-$stamp.zip")
        ZipOutputStream(BufferedOutputStream(FileOutputStream(out))).use { zip ->
            for (d in DIRS) {
                val files = File(context.filesDir, d).listFiles() ?: continue
                for (f in files) {
                    if (!f.isFile) continue
                    if (!includeTraces && f.extension == "trc") continue
                    zip.putNextEntry(ZipEntry("files/$d/${f.name}"))
                    f.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
            zip.putNextEntry(ZipEntry("prefs.json"))
            zip.write(prefsToJson(context).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return out
    }

    /** Devuelve cuántos archivos se han restaurado. */
    fun restore(context: Context, uri: Uri): Int {
        var count = 0
        val base = context.filesDir.canonicalFile
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("No se pudo abrir el archivo")
        input.use { raw ->
            ZipInputStream(raw).use { zip ->
                while (true) {
                    val e = zip.nextEntry ?: break
                    if (e.isDirectory) continue
                    val n = e.name
                    if (n == "prefs.json") {
                        applyPrefs(context, zip.readBytes().toString(Charsets.UTF_8))
                        continue
                    }
                    if (!n.startsWith("files/")) continue
                    val rel = n.removePrefix("files/")
                    if (rel.substringBefore('/') !in DIRS) continue
                    val dest = File(base, rel).canonicalFile
                    if (!dest.path.startsWith(base.path + File.separator)) continue
                    dest.parentFile?.mkdirs()
                    FileOutputStream(dest).use { out -> zip.copyTo(out) }
                    count++
                }
            }
        }
        return count
    }

    private fun prefsToJson(context: Context): String {
        val root = JSONObject()
        for (name in PREFS) {
            val o = JSONObject()
            for ((k, v) in context.getSharedPreferences(name, Context.MODE_PRIVATE).all) {
                val e = JSONObject()
                when (v) {
                    is Boolean -> { e.put("t", "b"); e.put("v", v) }
                    is Int -> { e.put("t", "i"); e.put("v", v) }
                    is Long -> { e.put("t", "l"); e.put("v", v) }
                    is Float -> { e.put("t", "f"); e.put("v", v.toDouble()) }
                    is String -> { e.put("t", "s"); e.put("v", v) }
                    else -> continue
                }
                o.put(k, e)
            }
            root.put(name, o)
        }
        return root.toString()
    }

    private fun applyPrefs(context: Context, text: String) {
        val root = JSONObject(text)
        for (name in PREFS) {
            val o = root.optJSONObject(name) ?: continue
            val ed = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit()
            val keys = o.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val e = o.getJSONObject(k)
                when (e.getString("t")) {
                    "b" -> ed.putBoolean(k, e.getBoolean("v"))
                    "i" -> ed.putInt(k, e.getInt("v"))
                    "l" -> ed.putLong(k, e.getLong("v"))
                    "f" -> ed.putFloat(k, e.getDouble("v").toFloat())
                    "s" -> ed.putString(k, e.getString("v"))
                    else -> Unit
                }
            }
            ed.apply()
        }
    }
}
