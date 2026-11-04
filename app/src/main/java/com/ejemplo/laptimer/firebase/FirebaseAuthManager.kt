package com.ejemplo.laptimer.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/** Handles prior anonymous Firebase Auth and token retrieval for REST calls. */
object FirebaseAuthManager {

    private const val TAG = "FirebaseAuthManager"

    suspend fun ensureAuthenticated(): String {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            Log.i(TAG, "Already authenticated anonymously with Firebase Auth UID: ${user.uid}")
            return user.uid
        }
        Log.i(TAG, "No active user found. Executing signInAnonymously() asynchronously...")
        val result = auth.signInAnonymously().await()
        val uid = result.user?.uid ?: throw IllegalStateException("Firebase Anonymous Auth failed")
        Log.i(TAG, "signInAnonymously() SUCCESS -> Firebase Auth UID: $uid")
        return uid
    }

    suspend fun getIdToken(): String? {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: run {
            ensureAuthenticated()
            auth.currentUser
        } ?: return null

        return try {
            val token = user.getIdToken(false).await().token
            Log.d(TAG, "getIdToken() SUCCESS for UID: ${user.uid}")
            token
        } catch (e: Exception) {
            Log.e(TAG, "getIdToken() FAILED: ${e.message}", e)
            null
        }
    }
}
