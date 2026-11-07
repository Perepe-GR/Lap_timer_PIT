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

    fun setupDevicePresence(roomCode: String, deviceUid: String, deviceName: String, initialStatus: String = "driving") {
        try {
            val dbUrl = "https://laptimer-pit-default-rtdb.europe-west1.firebasedatabase.app"
            val database = com.google.firebase.database.FirebaseDatabase.getInstance(dbUrl)
            
            val deviceRef = database.getReference("rooms/$roomCode/devices/$deviceUid")
            val initialData = mapOf(
                "deviceName" to deviceName,
                "status" to initialStatus,
                "lastSeenTs" to System.currentTimeMillis()
            )
            deviceRef.setValue(initialData)

            val statusRef = database.getReference("rooms/$roomCode/devices/$deviceUid/status")
            statusRef.onDisconnect().setValue("offline")

            val tsRef = database.getReference("rooms/$roomCode/devices/$deviceUid/lastSeenTs")
            tsRef.onDisconnect().setValue(System.currentTimeMillis())
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun updateDeviceStatusAtomic(roomCode: String, deviceUid: String, status: String) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val dbUrl = "https://laptimer-pit-default-rtdb.europe-west1.firebasedatabase.app"
                val database = com.google.firebase.database.FirebaseDatabase.getInstance(dbUrl)
                val updates = mapOf(
                    "rooms/$roomCode/devices/$deviceUid/status" to status,
                    "rooms/$roomCode/devices/$deviceUid/lastSeenTs" to System.currentTimeMillis()
                )
                database.reference.updateChildren(updates)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
