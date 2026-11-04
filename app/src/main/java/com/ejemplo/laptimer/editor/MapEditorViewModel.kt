package com.ejemplo.laptimer.editor

import com.ejemplo.laptimer.GatePoint
import com.ejemplo.laptimer.LatLon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.osmdroid.util.GeoPoint

enum class EditorMode {
    IDLE, FINISH_POS, FINISH_DIR, SECTOR_POS, SECTOR_DIR, PITIN_POS, PITIN_DIR, PITOUT_POS, PITOUT_DIR, SET_DIR, PADDOCK_ADD
}

class MapEditorViewModel {
    companion object {
        const val SEL_NONE = -1
        const val SEL_PIT_IN = -2
        const val SEL_PIT_OUT = -3
        const val MIN_WIDTH_M = 2.0
        const val MAX_WIDTH_M = 150.0
        val COL_FINISH = 0xFFC6F432.toInt()
        val COL_SECTOR = 0xFFF5A524.toInt()
        val COL_PIT = 0xFF38BDF8.toInt()
        val COL_PADDOCK = 0xFFE879F9.toInt()
    }

    var name = ""
    val points = ArrayList<GatePoint>()
    var pitIn: GatePoint? = null
    var pitOut: GatePoint? = null
    val paddock = ArrayList<LatLon>()
    var selected = SEL_NONE
    var mode = EditorMode.IDLE
    var gpsLat = Double.NaN
    var gpsLon = Double.NaN

    private val _referenceTrace = MutableStateFlow<List<GeoPoint>>(emptyList())
    val referenceTrace: StateFlow<List<GeoPoint>> = _referenceTrace.asStateFlow()

    fun setReferenceTrace(trace: List<GeoPoint>) {
        _referenceTrace.value = trace
    }

    fun getSel(): GatePoint? = when {
        selected >= 0 && selected < points.size -> points[selected]
        selected == SEL_PIT_IN -> pitIn
        selected == SEL_PIT_OUT -> pitOut
        else -> null
    }

    fun setSel(g: GatePoint) {
        when {
            selected >= 0 && selected < points.size -> points[selected] = g
            selected == SEL_PIT_IN -> pitIn = g
            selected == SEL_PIT_OUT -> pitOut = g
        }
    }

    fun selName(): String = when {
        selected == 0 -> "FINISH"
        selected > 0 -> "Sector $selected"
        selected == SEL_PIT_IN -> "PIT IN"
        selected == SEL_PIT_OUT -> "PIT OUT"
        else -> ""
    }

    fun deleteSelected(): Boolean {
        when {
            selected >= 0 && selected < points.size -> points.removeAt(selected)
            selected == SEL_PIT_IN -> pitIn = null
            selected == SEL_PIT_OUT -> pitOut = null
            else -> return false
        }
        selected = SEL_NONE
        return true
    }

    fun undoPaddock() {
        if (paddock.isNotEmpty()) paddock.removeAt(paddock.size - 1)
    }

    fun clearPaddock() {
        paddock.clear()
        mode = EditorMode.IDLE
    }
}
