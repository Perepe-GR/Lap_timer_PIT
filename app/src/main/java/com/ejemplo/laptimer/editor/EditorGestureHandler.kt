package com.ejemplo.laptimer.editor

import android.content.Context
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import com.ejemplo.laptimer.GatePoint
import com.ejemplo.laptimer.bearingDeg

class EditorGestureHandler(
    private val context: Context,
    private val map: MapView,
    private val viewModel: MapEditorViewModel,
    private val dynamic: ArrayList<Overlay>,
    private val density: Float,
    private val onRedraw: () -> Unit,
    private val onUpdateHint: () -> Unit,
    private val onToast: (String) -> Unit
) {
    private var pending: GeoPoint? = null
    private val dialogs = EditorDialogs(context, viewModel, density, onRedraw, onUpdateHint, onToast)
    private val gateRenderer = GateRenderer(context, map, dynamic, density, viewModel, onRedraw, onUpdateHint)

    fun onTap(p: GeoPoint) {
        when (viewModel.mode) {
            EditorMode.FINISH_POS -> { pending = p; viewModel.mode = EditorMode.FINISH_DIR }
            EditorMode.SECTOR_POS -> { pending = p; viewModel.mode = EditorMode.SECTOR_DIR }
            EditorMode.PITIN_POS -> { pending = p; viewModel.mode = EditorMode.PITIN_DIR }
            EditorMode.PITOUT_POS -> { pending = p; viewModel.mode = EditorMode.PITOUT_DIR }
            EditorMode.FINISH_DIR, EditorMode.SECTOR_DIR, EditorMode.PITIN_DIR, EditorMode.PITOUT_DIR -> {
                val a = pending
                if (a != null) {
                    val h = bearingDeg(a.latitude, a.longitude, p.latitude, p.longitude)
                    val gp = GatePoint(a.latitude, a.longitude, h)
                    when (viewModel.mode) {
                        EditorMode.FINISH_DIR -> {
                            if (viewModel.points.isEmpty()) viewModel.points.add(gp) else viewModel.points[0] = gp
                            viewModel.selected = 0
                        }
                        EditorMode.SECTOR_DIR -> {
                            viewModel.points.add(gp)
                            viewModel.selected = viewModel.points.size - 1
                        }
                        EditorMode.PITIN_DIR -> {
                            viewModel.pitIn = gp
                            viewModel.selected = MapEditorViewModel.SEL_PIT_IN
                        }
                        else -> {
                            viewModel.pitOut = gp
                            viewModel.selected = MapEditorViewModel.SEL_PIT_OUT
                        }
                    }
                }
                pending = null
                viewModel.mode = EditorMode.IDLE
            }
            EditorMode.SET_DIR -> {
                val s = viewModel.getSel()
                if (s != null) viewModel.setSel(s.copy(headingDeg = bearingDeg(s.lat, s.lon, p.latitude, p.longitude)))
                viewModel.mode = EditorMode.IDLE
            }
            EditorMode.PADDOCK_ADD -> viewModel.paddock.add(com.ejemplo.laptimer.LatLon(p.latitude, p.longitude))
            EditorMode.IDLE -> viewModel.selected = MapEditorViewModel.SEL_NONE
        }
        onRedraw()
        onUpdateHint()
    }

    fun askWidth() = dialogs.askWidth()

    fun drawGate(
        g: GatePoint,
        label: String,
        accent: Int,
        sel: Int,
        onMoved: (Double, Double) -> Unit,
        onWidth: (Double, Double) -> Unit
    ) = gateRenderer.drawGate(g, label, accent, sel, onMoved, onWidth)
}
