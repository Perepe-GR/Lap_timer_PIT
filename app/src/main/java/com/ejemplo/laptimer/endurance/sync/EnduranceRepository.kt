package com.ejemplo.laptimer.endurance.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject
import java.io.File

class EnduranceRepository(
    private val context: Context,
    private val syncClient: EnduranceSyncClient = EnduranceSyncClient()
) {
    private val prefs = context.getSharedPreferences("endurance_repo", Context.MODE_PRIVATE)
    private val queueDir = File(context.filesDir, "endurance_queue").apply { mkdirs() }

    fun saveStrategyLocal(strategy: StrategyBase) {
        val json = JSONObject(strategy.toMap()).toString()
        prefs.edit().putString("strategy_base", json).apply()
    }

    fun loadStrategyLocal(): StrategyBase? {
        val json = prefs.getString("strategy_base", null) ?: return null
        return try {
            val obj = JSONObject(json)
            val map = mutableMapOf<String, Any?>()
            val keys = obj.keys()
            while (keys.hasNext()) { val k = keys.next(); map[k] = obj.get(k) }
            strategyBaseFromMap(map)
        } catch (e: Exception) {
            null
        }
    }

    fun enqueueLap(lap: LapRecord) {
        try {
            val file = File(queueDir, "lap_${lap.lapNumber}_${System.currentTimeMillis()}.json")
            val json = JSONObject(lap.toMap()).toString()
            file.writeText(json)
        } catch (e: Exception) {
            // Queue fallback
        }
    }

    suspend fun flushQueue(code: String, pilotId: String) {
        val files = queueDir.listFiles() ?: return
        for (f in files.sortedBy { it.name }) {
            try {
                val json = f.readText()
                val obj = JSONObject(json)
                val map = mutableMapOf<String, Any?>()
                val keys = obj.keys()
                while (keys.hasNext()) { val k = keys.next(); map[k] = obj.get(k) }
                val lap = lapRecordFromMap(map)
                val res = syncClient.pushLap(code, pilotId, lap)
                if (res.isSuccess) {
                    f.delete()
                }
            } catch (e: Exception) {
                // Keep file for retry
            }
        }
    }

    fun scheduleConnectedSync(code: String, pilotId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val data = Data.Builder()
            .putString("teamCode", code)
            .putString("pilotId", pilotId)
            .build()

        val request = OneTimeWorkRequestBuilder<TelemetrySyncWorker>()
            .setConstraints(constraints)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "telemetry_sync_$code",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun observeOnline(): Flow<Boolean> = flow {
        while (true) {
            emit(true)
            kotlinx.coroutines.delay(10000L)
        }
    }

    suspend fun startSync(code: String, pilotId: String) {
        flushQueue(code, pilotId)
        scheduleConnectedSync(code, pilotId)
    }
}
