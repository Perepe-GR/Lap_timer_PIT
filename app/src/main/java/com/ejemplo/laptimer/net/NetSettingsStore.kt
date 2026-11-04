package com.ejemplo.laptimer.net

import android.content.Context

class NetSettingsStore(context: Context) {
    val prefs = context.getSharedPreferences("laptimer_net", Context.MODE_PRIVATE)

    fun putS(k: String, v: String) = prefs.edit().putString(k, v).apply()
    fun putI(k: String, v: Int) = prefs.edit().putInt(k, v).apply()
    fun putB(k: String, v: Boolean) = prefs.edit().putBoolean(k, v).apply()
    fun putF(k: String, v: Float) = prefs.edit().putFloat(k, v).apply()
    fun putL(k: String, v: Long) = prefs.edit().putLong(k, v).apply()
}
