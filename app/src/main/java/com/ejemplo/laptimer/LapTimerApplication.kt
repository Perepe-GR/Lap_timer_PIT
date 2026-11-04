package com.ejemplo.laptimer

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/** Main Application class to guarantee early FirebaseApp initialization. */
class LapTimerApplication : Application() {

    companion object {
        private const val TAG = "LapTimerApplication"
    }

    override fun onCreate() {
        super.onCreate()
        initFirebase()
    }

    private fun initFirebase() {
        if (FirebaseApp.getApps(this).isEmpty()) {
            try {
                FirebaseApp.initializeApp(this)
                Log.i(TAG, "FirebaseApp initialized successfully with default configuration.")
            } catch (e: Exception) {
                Log.w(TAG, "Default Firebase initialization failed (${e.message}), initializing programmatically with explicit credentials.")
                val options = FirebaseOptions.Builder()
                    .setApiKey("AIzaSyAapqxMCk3CYkccwhPCL_5h4pWjxtKozJg")
                    .setApplicationId("1:401753783898:android:672eac3228892eae3aa263")
                    .setDatabaseUrl("https://laptimer-pit-default-rtdb.europe-west1.firebasedatabase.app")
                    .setProjectId("laptimer-pit")
                    .setGcmSenderId("401753783898")
                    .setStorageBucket("laptimer-pit.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.i(TAG, "FirebaseApp programmatically initialized with project credentials.")
            }
        } else {
            Log.i(TAG, "FirebaseApp is already initialized.")
        }
    }
}
