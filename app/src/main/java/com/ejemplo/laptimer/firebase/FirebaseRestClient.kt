package com.ejemplo.laptimer.firebase

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class FirebaseRestClient(private val dbUrl: String, private val room: String) {
    fun request(method: String, path: String, body: String?, query: String, authToken: String? = null): String {
        val authParam = if (!authToken.isNullOrBlank()) {
            if (query.isBlank()) "?auth=$authToken" else "$query&auth=$authToken"
        } else {
            query
        }
        val cleanPath = if (path.endsWith(".json") || path.contains(".json?")) path else "$path.json"
        val url = URL("$dbUrl/rooms/$room/$cleanPath$authParam")
        val c = url.openConnection() as HttpURLConnection
        try {
            c.requestMethod = method
            c.connectTimeout = 6000
            c.readTimeout = 6000
            if (body != null) {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw IOException("HTTP $code")
            return text
        } finally {
            c.disconnect()
        }
    }
}
