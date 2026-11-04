package com.ejemplo.laptimer.editor

import android.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

/** Renders high-frequency 25Hz GPS reference trace efficiently on MapView. */
class TraceOverlayRenderer(private val map: MapView) {

    private var tracePolyline: Polyline? = null
    private val density = map.context.resources.displayMetrics.density

    fun renderTrace(trace: List<GeoPoint>, scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            if (trace.isEmpty()) {
                withContext(Dispatchers.Main) {
                    removeTrace()
                }
                return@launch
            }

            val polyline = tracePolyline ?: Polyline(map).apply {
                outlinePaint.color = Color.parseColor("#CC00E5FF")
                outlinePaint.strokeWidth = 3.0f * density
                infoWindow = null
            }

            polyline.setPoints(trace)

            withContext(Dispatchers.Main) {
                if (tracePolyline == null) {
                    tracePolyline = polyline
                    map.overlays.add(0, polyline)
                }
                map.invalidate()
            }
        }
    }

    private fun removeTrace() {
        tracePolyline?.let { map.overlays.remove(it) }
        tracePolyline = null
        map.invalidate()
    }
}
