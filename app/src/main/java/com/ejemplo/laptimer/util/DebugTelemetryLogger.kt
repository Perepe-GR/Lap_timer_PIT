package com.ejemplo.laptimer.util

import android.content.Context
import android.os.SystemClock
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Black-box diagnostic telemetry logger recording high-frequency GPS, NMEA, and IMU data to CSV.
 */
class DebugTelemetryLogger(private val context: Context) {
    companion object {
        private const val TAG = "DebugTelemetryLogger"
    }

    private var fileWriter: FileWriter? = null
    private var isRecording = false
    private var logFileName = ""

    @Synchronized
    fun startSession() {
        if (isRecording) return
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            logFileName = "debug_telemetry_$timestamp.csv"
            val dir = File(context.getExternalFilesDir(null), "debug_logs").apply { mkdirs() }
            val file = File(dir, logFileName)
            fileWriter = FileWriter(file, false).apply {
                append("ElapsedRealtimeMs,TimestampUtc,Lat,Lon,Kmh,Sats,Hdop,AccuracyMeters,GLong,GLat,GVert,Source\n")
                flush()
            }
            isRecording = true
            Log.i(TAG, "Started debug telemetry log: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start debug log: ${e.message}", e)
        }
    }

    @Synchronized
    fun logRow(
        lat: Double,
        lon: Double,
        kmh: Double,
        sats: Int,
        hdop: Double,
        accuracy: Float,
        gLong: Float,
        gLat: Float,
        gVert: Float,
        source: String
    ) {
        if (!isRecording || fileWriter == null) return
        try {
            val elapsed = SystemClock.elapsedRealtime()
            val utc = System.currentTimeMillis()
            fileWriter?.apply {
                append("$elapsed,$utc,$lat,$lon,$kmh,$sats,$hdop,$accuracy,$gLong,$gLat,$gVert,$source\n")
                flush()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing debug row: ${e.message}")
        }
    }

    @Synchronized
    fun stopSession(): String? {
        if (!isRecording) return null
        try {
            fileWriter?.close()
            fileWriter = null
            isRecording = false
            val dir = File(context.getExternalFilesDir(null), "debug_logs")
            val file = File(dir, logFileName)
            Log.i(TAG, "Stopped debug telemetry log: ${file.absolutePath}")
            return file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping debug log: ${e.message}")
            return null
        }
    }
}
