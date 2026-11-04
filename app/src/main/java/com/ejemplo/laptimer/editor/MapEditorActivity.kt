package com.ejemplo.laptimer.editor

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import com.ejemplo.laptimer.CircuitStore
import java.io.File
import java.util.Locale

class MapEditorActivity : AppCompatActivity() {

    internal lateinit var map: MapView
    internal lateinit var hint: TextView
    internal lateinit var store: CircuitStore
    internal lateinit var viewModel: MapEditorViewModel
    internal lateinit var gestureHandler: EditorGestureHandler
    internal lateinit var traceRenderer: TraceOverlayRenderer
    private val dynamic = ArrayList<Overlay>()
    private val density get() = resources.displayMetrics.density

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cfg = Configuration.getInstance()
        cfg.load(applicationContext, getSharedPreferences("osm", MODE_PRIVATE))
        cfg.userAgentValue = packageName
        cfg.osmdroidBasePath = File(cacheDir, "osmdroid")
        cfg.osmdroidTileCache = File(cacheDir, "osmdroid/tiles")

        store = CircuitStore(this)
        viewModel = MapEditorViewModel()
        viewModel.name = intent.getStringExtra("name") ?: "Circuit"
        viewModel.gpsLat = intent.getDoubleExtra("lat", Double.NaN)
        viewModel.gpsLon = intent.getDoubleExtra("lon", Double.NaN)

        if (!intent.getBooleanExtra("new", false)) {
            store.list().firstOrNull { it.name == viewModel.name }?.let { c ->
                viewModel.points.add(c.finish); viewModel.points.addAll(c.sectors)
                viewModel.pitIn = c.pitIn; viewModel.pitOut = c.pitOut; viewModel.paddock.addAll(c.paddock)
            }
        }

        val root = FrameLayout(this)
        map = MapView(this).apply {
            setTileSource(MapboxTileSourceHelper.getSatelliteSource())
            setMultiTouchControls(true)
            maxZoomLevel = 22.0
        }
        root.addView(map, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        traceRenderer = TraceOverlayRenderer(map)
        lifecycleScope.launch {
            viewModel.referenceTrace.collectLatest { trace -> traceRenderer.renderTrace(trace, lifecycleScope) }
        }

        hint = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(0xEE0B0D10.toInt())
                cornerRadius = 8f * density
            }
            val padH = (14 * density).toInt()
            val padV = (6 * density).toInt()
            setPadding(padH, padV, padH, padV)
            gravity = Gravity.CENTER
        }
        val hintParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL
        )
        hintParams.topMargin = (10 * density).toInt()
        root.addView(hint, hintParams)

        gestureHandler = EditorGestureHandler(
            this, map, viewModel, dynamic, density,
            { redraw() }, { updateHint() }, { msg -> toast(msg) }
        )

        EditorToolbar(this, root, viewModel, gestureHandler, hint).setupUI()
        setContentView(root)

        map.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean { gestureHandler.onTap(p); return true }
            override fun longPressHelper(p: GeoPoint): Boolean = false
        }))

        initMapPosition(); redraw(); updateHint()
    }

    override fun onResume() { super.onResume(); map.onResume() }
    override fun onPause() { map.onPause(); super.onPause() }

    internal fun toast(msg: String) { /* disabled per UI contract */ }

    private fun initMapPosition() {
        val targetZoom = 18.0
        when {
            viewModel.points.isNotEmpty() -> {
                map.controller.setZoom(targetZoom)
                map.controller.setCenter(GeoPoint(viewModel.points[0].lat, viewModel.points[0].lon))
            }
            !viewModel.gpsLat.isNaN() -> {
                map.controller.setZoom(targetZoom)
                map.controller.setCenter(GeoPoint(viewModel.gpsLat, viewModel.gpsLon))
            }
            else -> {
                map.controller.setZoom(6.0)
                map.controller.setCenter(GeoPoint(40.4, -3.7))
            }
        }
    }

    internal fun updateHint() {
        hint.text = when (viewModel.mode) {
            EditorMode.IDLE -> {
                val g = viewModel.getSel()
                if (g != null) String.format(Locale.US, "«%s» · %s · left %.0f m · right %.0f m", viewModel.name, viewModel.selName(), g.leftM, g.rightM)
                else "«${viewModel.name}» · Tap + FINISH, + SECTOR, + PIT IN / PIT OUT, or + PADDOCK."
            }
            EditorMode.FINISH_POS -> "Tap map to place the FINISH line."
            EditorMode.SECTOR_POS -> "Tap map to place sector line."
            EditorMode.PITIN_POS -> "Tap map to place PIT IN line."
            EditorMode.PITOUT_POS -> "Tap map to place PIT OUT line."
            EditorMode.PADDOCK_ADD -> "PADDOCK: tap map to place vertices (${viewModel.paddock.size} added)."
            else -> "Now tap a point in driving direction."
        }
    }

    internal fun redraw() {
        map.overlays.removeAll(dynamic); dynamic.clear()

        for (i in viewModel.points.indices) {
            val idx = i
            gestureHandler.drawGate(
                viewModel.points[idx], if (idx == 0) "M" else idx.toString(),
                if (idx == 0) MapEditorViewModel.COL_FINISH else MapEditorViewModel.COL_SECTOR, idx,
                { lat, lon -> if (idx in viewModel.points.indices) viewModel.points[idx] = viewModel.points[idx].copy(lat = lat, lon = lon) },
                { l, r -> if (idx in viewModel.points.indices) viewModel.points[idx] = viewModel.points[idx].copy(leftM = l, rightM = r) }
            )
        }
        viewModel.pitIn?.let { g ->
            gestureHandler.drawGate(g, "E", MapEditorViewModel.COL_PIT, MapEditorViewModel.SEL_PIT_IN,
                { lat, lon -> viewModel.pitIn = viewModel.pitIn?.copy(lat = lat, lon = lon) },
                { l, r -> viewModel.pitIn = viewModel.pitIn?.copy(leftM = l, rightM = r) }
            )
        }
        viewModel.pitOut?.let { g ->
            gestureHandler.drawGate(g, "S", MapEditorViewModel.COL_PIT, MapEditorViewModel.SEL_PIT_OUT,
                { lat, lon -> viewModel.pitOut = viewModel.pitOut?.copy(lat = lat, lon = lon) },
                { l, r -> viewModel.pitOut = viewModel.pitOut?.copy(leftM = l, rightM = r) }
            )
        }

        if (viewModel.paddock.size >= 2) {
            val poly = Polygon(map).apply {
                points = viewModel.paddock.map { GeoPoint(it.lat, it.lon) }
                fillPaint.color = 0x33E879F9
                outlinePaint.color = MapEditorViewModel.COL_PADDOCK
                outlinePaint.strokeWidth = 3f * density
            }
            dynamic.add(poly); map.overlays.add(poly)
        }
        for (i in viewModel.paddock.indices) {
            val idx = i; val m = Marker(map)
            m.position = GeoPoint(viewModel.paddock[idx].lat, viewModel.paddock[idx].lon)
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER); m.isDraggable = true
            m.setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                override fun onMarkerDrag(marker: Marker?) {}
                override fun onMarkerDragStart(marker: Marker?) {}
                override fun onMarkerDragEnd(marker: Marker?) {
                    val mp = marker?.position ?: return
                    if (idx in viewModel.paddock.indices) viewModel.paddock[idx] = com.ejemplo.laptimer.LatLon(mp.latitude, mp.longitude)
                    map.post { redraw() }
                }
            })
            dynamic.add(m); map.overlays.add(m)
        }
        map.invalidate()
    }

    internal fun save() {
        if (viewModel.points.isEmpty()) { toast("Place at least the FINISH line"); return }
        if ((viewModel.pitIn == null) != (viewModel.pitOut == null)) { toast("PIT lane requires both PIT IN and PIT OUT lines"); return }
        if (viewModel.paddock.size in 1..2) { toast("PADDOCK area requires at least 3 points"); return }
        val success = TrackSaver.saveCircuit(this, store, viewModel.name, viewModel.points, viewModel.pitIn, viewModel.pitOut, viewModel.paddock)
        if (success) { toast("Circuit saved successfully"); finish() } else { toast("Error saving circuit") }
    }
}
