package com.ejemplo.laptimer.editor

import android.content.Context
import com.ejemplo.laptimer.Circuit
import com.ejemplo.laptimer.CircuitStore
import com.ejemplo.laptimer.GatePoint
import com.ejemplo.laptimer.LatLon

object TrackSaver {
    fun saveCircuit(
        context: Context,
        store: CircuitStore,
        name: String,
        points: List<GatePoint>,
        pitIn: GatePoint?,
        pitOut: GatePoint?,
        paddock: List<LatLon>
    ): Boolean {
        if (points.isEmpty()) return false
        if ((pitIn == null) != (pitOut == null)) return false
        if (paddock.size in 1..2) return false
        val c = Circuit(name, points[0], points.drop(1), pitIn, pitOut, ArrayList(paddock))
        store.save(c)
        store.activeName = name
        return true
    }
}
