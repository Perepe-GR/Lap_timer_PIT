package com.ejemplo.laptimer.endurance.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Background WorkManager Worker to automatically flush local offline telemetry & lap queues
 * upon network reconnection without blocking the main UI thread.
 */
class TelemetrySyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "TelemetrySyncWorker"
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting offline telemetry queue sync work...")
        val code = inputData.getString("teamCode") ?: ""
        val pilotId = inputData.getString("pilotId") ?: ""

        val repo = EnduranceRepository(applicationContext)
        return try {
            repo.flushQueue(code, pilotId)
            Log.i(TAG, "Offline telemetry queue sync completed successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "TelemetrySyncWorker failed: ${e.message}", e)
            Result.retry()
        }
    }
}
