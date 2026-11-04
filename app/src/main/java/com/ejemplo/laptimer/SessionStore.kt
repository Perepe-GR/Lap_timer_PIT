package com.ejemplo.laptimer

import android.content.Context
import java.io.File

class SessionStore(context: Context) {
    private val dir = File(context.filesDir, "sessions").apply { mkdirs() }

    private fun jsonFile(id: String) = File(dir, "$id.json")
    fun traceFile(id: String) = File(dir, "$id.trc")

    fun save(r: SessionRecord) {
        try {
            jsonFile(r.id).writeText(r.toJson())
        } catch (e: Exception) {
            // sin almacenamiento: se ignora
        }
    }

    fun delete(id: String) {
        jsonFile(id).delete()
        traceFile(id).delete()
    }

    fun get(id: String): SessionRecord? =
        runCatching { SessionRecord.fromJson(jsonFile(id).readText()) }.getOrNull()

    fun list(): List<SessionRecord> {
        val files = dir.listFiles() ?: return emptyList()
        return files
            .filter { it.extension == "json" }
            .mapNotNull { f -> runCatching { SessionRecord.fromJson(f.readText()) }.getOrNull() }
            .sortedByDescending { it.startEpochMs }
    }
}
