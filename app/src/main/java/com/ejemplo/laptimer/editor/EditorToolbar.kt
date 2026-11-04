package com.ejemplo.laptimer.editor

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class EditorToolbar(
    private val activity: MapEditorActivity,
    private val root: ViewGroup,
    private val viewModel: MapEditorViewModel,
    private val gestureHandler: EditorGestureHandler,
    private val hint: TextView
) {
    fun setupUI() {
        val density = activity.resources.displayMetrics.density

        val scroll = HorizontalScrollView(activity)
        scroll.isHorizontalScrollBarEnabled = false
        scroll.setBackgroundColor(0xCC0B0D10.toInt())
        scroll.setPadding((4 * density).toInt(), (6 * density).toInt(), (4 * density).toInt(), (6 * density).toInt())

        val container = LinearLayout(activity)
        container.orientation = LinearLayout.HORIZONTAL
        container.gravity = Gravity.CENTER_VERTICAL
        scroll.addView(container, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        fun addButton(text: String, accent: Boolean = false, action: () -> Unit) {
            val b = TextView(activity)
            b.text = text
            b.textSize = 12f
            b.typeface = Typeface.DEFAULT_BOLD
            b.setTextColor(if (accent) Color.parseColor("#0B0D10") else Color.WHITE)
            val bg = GradientDrawable().apply {
                setColor(if (accent) Color.parseColor("#C6F432") else Color.parseColor("#1E252D"))
                cornerRadius = 6f * density
            }
            b.background = bg
            b.gravity = Gravity.CENTER
            b.isClickable = true
            b.isFocusable = true
            val padH = (10 * density).toInt()
            val padV = (6 * density).toInt()
            b.setPadding(padH, padV, padH, padV)
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins((3 * density).toInt(), 0, (3 * density).toInt(), 0)
            b.layoutParams = lp
            b.setOnClickListener { action() }
            container.addView(b)
        }

        addButton("+ FINISH") { viewModel.mode = EditorMode.FINISH_POS; activity.updateHint() }
        addButton("+ SECTOR") {
            if (viewModel.points.isEmpty()) activity.toast("Place FINISH first")
            else { viewModel.mode = EditorMode.SECTOR_POS; activity.updateHint() }
        }
        addButton("+ PIT IN") { viewModel.mode = EditorMode.PITIN_POS; activity.updateHint() }
        addButton("+ PIT OUT") { viewModel.mode = EditorMode.PITOUT_POS; activity.updateHint() }
        addButton("HEADING") {
            if (viewModel.getSel() == null) activity.toast("Select a line on the map first")
            else { viewModel.mode = EditorMode.SET_DIR; activity.updateHint() }
        }
        addButton("WIDTH") { gestureHandler.askWidth() }
        addButton("DELETE") {
            if (!viewModel.deleteSelected()) activity.toast("Select a line on the map first")
            else { activity.redraw(); activity.updateHint() }
        }

        addButton("+ PADDOCK") { viewModel.mode = EditorMode.PADDOCK_ADD; activity.updateHint() }
        addButton("DONE") { viewModel.mode = EditorMode.IDLE; activity.redraw(); activity.updateHint() }
        addButton("UNDO") { viewModel.undoPaddock(); activity.redraw(); activity.updateHint() }
        addButton("CLEAR PADDOCK") { viewModel.clearPaddock(); activity.redraw(); activity.updateHint() }

        addButton("MY POSITION") {
            if (viewModel.gpsLat.isNaN()) activity.toast("No GPS position available yet")
            else activity.map.controller.animateTo(org.osmdroid.util.GeoPoint(viewModel.gpsLat, viewModel.gpsLon))
        }
        addButton("SAVE", true) { activity.save() }
        addButton("EXIT") { activity.finish() }

        val fabMapToggle = FloatingActionButton(activity).apply {
            size = FloatingActionButton.SIZE_MINI
            setImageResource(android.R.drawable.ic_menu_mapmode)
            contentDescription = "Toggle Map Layer"
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#1E252D"))
            imageTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            elevation = 4f * density
        }

        val fabParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.END or Gravity.BOTTOM
        )
        val marginPx = (14 * density).toInt()
        val bottomMarginPx = (52 * density).toInt()
        fabParams.setMargins(0, 0, marginPx, bottomMarginPx)

        root.addView(fabMapToggle, fabParams)
        root.addView(scroll, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
    }
}
