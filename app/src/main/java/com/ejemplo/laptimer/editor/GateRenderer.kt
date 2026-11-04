package com.ejemplo.laptimer.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import com.ejemplo.laptimer.GatePoint
import com.ejemplo.laptimer.LocalFrame

class GateRenderer(
    private val context: Context,
    private val map: MapView,
    private val dynamic: ArrayList<Overlay>,
    private val density: Float,
    private val viewModel: MapEditorViewModel,
    private val onRedraw: () -> Unit,
    private val onUpdateHint: () -> Unit
) {
    fun addLine(a: GeoPoint, b: GeoPoint, color: Int, width: Float) {
        val pl = Polyline(map)
        pl.setPoints(listOf(a, b))
        pl.outlinePaint.color = color
        pl.outlinePaint.strokeWidth = width
        dynamic.add(pl)
        map.overlays.add(pl)
    }

    fun drawGate(
        g: GatePoint,
        label: String,
        accent: Int,
        sel: Int,
        onMoved: (Double, Double) -> Unit,
        onWidth: (Double, Double) -> Unit
    ) {
        val f = LocalFrame(g.lat, g.lon)
        val h = Math.toRadians(g.headingDeg)
        val dx = Math.sin(h)
        val dy = Math.cos(h)
        val isSel = sel == viewModel.selected
        val leftX = -dy * g.leftM
        val leftY = dx * g.leftM
        val rightX = dy * g.rightM
        val rightY = -dx * g.rightM
        val leftEnd = GeoPoint(f.lat(leftY), f.lon(leftX))
        val rightEnd = GeoPoint(f.lat(rightY), f.lon(rightX))
        addLine(leftEnd, rightEnd, accent, 6f * density)
        addLine(
            GeoPoint(g.lat, g.lon),
            GeoPoint(f.lat(30.0 * dy), f.lon(30.0 * dx)),
            if (isSel) Color.WHITE else 0xFFB8C0CC.toInt(), 3f * density
        )

        val m = Marker(map)
        m.position = GeoPoint(g.lat, g.lon)
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        m.icon = labelIcon(label, isSel, accent)
        m.isDraggable = true
        m.setOnMarkerClickListener { _, _ ->
            viewModel.selected = sel
            viewModel.mode = EditorMode.IDLE
            map.post { onRedraw(); onUpdateHint() }
            true
        }
        m.setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
            override fun onMarkerDrag(marker: Marker?) {}
            override fun onMarkerDragStart(marker: Marker?) {}
            override fun onMarkerDragEnd(marker: Marker?) {
                val mp = marker?.position ?: return
                onMoved(mp.latitude, mp.longitude)
                map.post { onRedraw() }
            }
        })
        dynamic.add(m)
        map.overlays.add(m)

        if (isSel) {
            fun handle(pos: GeoPoint, isLeft: Boolean) {
                val hm = Marker(map)
                hm.position = pos
                hm.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                hm.icon = handleIcon(accent)
                hm.isDraggable = true
                hm.setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                    override fun onMarkerDrag(marker: Marker?) {}
                    override fun onMarkerDragStart(marker: Marker?) {}
                    override fun onMarkerDragEnd(marker: Marker?) {
                        val mp = marker?.position ?: return
                        val x = f.x(mp.longitude)
                        val y = f.y(mp.latitude)
                        val d = x * (-dy) + y * dx
                        if (isLeft) onWidth(d.coerceIn(MapEditorViewModel.MIN_WIDTH_M, MapEditorViewModel.MAX_WIDTH_M), g.rightM)
                        else onWidth(g.leftM, (-d).coerceIn(MapEditorViewModel.MIN_WIDTH_M, MapEditorViewModel.MAX_WIDTH_M))
                        map.post { onRedraw(); onUpdateHint() }
                    }
                })
                dynamic.add(hm)
                map.overlays.add(hm)
            }
            handle(leftEnd, true)
            handle(rightEnd, false)
        }
    }

    private fun labelIcon(text: String, isSelected: Boolean, accent: Int): Drawable {
        val s = (36 * density).toInt()
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = if (isSelected) Color.WHITE else accent
        c.drawCircle(s / 2f, s / 2f, s / 2f, p)
        p.color = 0xFF0B0D10.toInt()
        c.drawCircle(s / 2f, s / 2f, s / 2f - 3f * density, p)
        p.color = accent
        p.textSize = s * 0.5f
        p.textAlign = Paint.Align.CENTER
        p.typeface = Typeface.DEFAULT_BOLD
        c.drawText(text, s / 2f, s / 2f + p.textSize * 0.35f, p)
        return BitmapDrawable(context.resources, bmp)
    }

    private fun handleIcon(accent: Int): Drawable {
        val s = (26 * density).toInt()
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = accent
        c.drawCircle(s / 2f, s / 2f, s / 2f, p)
        p.color = Color.WHITE
        c.drawCircle(s / 2f, s / 2f, s / 2f - 4f * density, p)
        return BitmapDrawable(context.resources, bmp)
    }
}
