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
 * Black-box diagnostic telemetry logger recording high-frequency GPS, NMEA, IMU, USB errors,
 * and lap/sector event milestones to structured CSV files.
 */
class DebugTelemetryLogger(private val context: Context) {
    companion object {
        private const val TAG = "DebugTelemetryLogger"
    }

    private var telemetryWriter: FileWriter? = null
    private var eventWriter: FileWriter? = null
    private var isRecording = false
    private var sessionTag = ""

    @Synchronized
    fun startSession() {
        if (isRecording) return
        try {
            sessionTag = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val dir = File(context.getExternalFilesDir(null), "debug_logs").apply { mkdirs() }

            val telFile = File(dir, "debug_telemetry_$sessionTag.csv")
            telemetryWriter = FileWriter(telFile, false).apply {
                append("ElapsedRealtimeMs,TimestampUtc,Lat,Lon,Kmh,Sats,Hdop,AccuracyMeters,GLong,GLat,GVert,Source\n")
                flush()
            }

            val eventFile = File(dir, "debug_events_$sessionTag.csv")
            eventWriter = FileWriter(eventFile, false).apply {
                append("ElapsedRealtimeMs,TimestampUtc,Category,Message\n")
                flush()
            }

            isRecording = true
            Log.i(TAG, "Started black-box debug session: $sessionTag")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start debug session: ${e.message}", e)
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
        if (!isRecording || telemetryWriter == null) return
        try {
            val elapsed = SystemClock.elapsedRealtime()
            val utc = System.currentTimeMillis()
            telemetryWriter?.apply {
                append("$elapsed,$utc,$lat,$lon,$kmh,$sats,$hdop,$accuracy,$gLong,$gLat,$gVert,$source\n")
                flush()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing debug telemetry row: ${e.message}")
        }
    }

    @Synchronized
    fun logEvent(category: String, message: String) {
        if (!isRecording || eventWriter == null) return
        try {
            val elapsed = SystemClock.elapsedRealtime()
            val utc = System.currentTimeMillis()
            val cleanMsg = message.replace(",", ";").replace("\n", " ")
            eventWriter?.apply {
                append("$elapsed,$utc,$category,$cleanMsg\n")
                flush()
            }
            Log.w(TAG, "[$category] $message")
        } catch (e: Exception) {
            Log.e(TAG, "Error writing debug event: ${e.message}")
        }
    }

    @Synchronized
    fun stopSession() {
        if (!isRecording) return
        try {
            telemetryWriter?.close()
            eventWriter?.close()
            telemetryWriter = null
            eventWriter = null
            isRecording = false
            Log.i(TAG, "Stopped black-box debug session: $sessionTag")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping debug session: ${e.message}")
        }
    }
}
