package com.ejemplo.laptimer.firebase

import android.util.Log
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class FirebaseRestClient(private val dbUrl: String, private val room: String) {
    companion object {
        private const val TAG = "FirebaseRestClient"
        private const val MAX_RETRIES = 3
    }

    fun request(method: String, path: String, body: String?, query: String, authToken: String? = null): String {
        val authParam = if (!authToken.isNullOrBlank()) {
            if (query.isBlank()) "?auth=$authToken" else "$query&auth=$authToken"
        } else {
            query
        }
        val cleanPath = if (path.endsWith(".json") || path.contains(".json?")) path else "$path.json"
        val fullPath = if (cleanPath.startsWith("rooms/") || cleanPath.startsWith("teams/") || cleanPath.startsWith("circuits/")) {
            cleanPath
        } else {
            "rooms/$room/$cleanPath"
        }
        val url = URL("${dbUrl.trimEnd('/')}/$fullPath$authParam")

        var lastException: Exception? = null

        for (attempt in 1..MAX_RETRIES) {
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
                if (code in 200..299) {
                    return text
                } else if (code in 500..599 && attempt < MAX_RETRIES) {
                    Log.w(TAG, "Server error HTTP $code on attempt $attempt, retrying...")
                    try { Thread.sleep(attempt * 1000L) } catch (_: Exception) {}
                    continue
                } else {
                    throw IOException("HTTP $code")
                }
            } catch (e: IOException) {
                lastException = e
                if (attempt < MAX_RETRIES) {
                    Log.w(TAG, "Network IOException on attempt $attempt (${e.message}), retrying with exponential backoff...")
                    try { Thread.sleep(attempt * 1000L) } catch (_: Exception) {}
                } else {
                    throw e
                }
            } finally {
                try { c.disconnect() } catch (_: Exception) {}
            }
        }
        throw lastException ?: IOException("Request failed after $MAX_RETRIES attempts")
    }
}
