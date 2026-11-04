package com.ejemplo.laptimer.editor

import android.content.Context
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import java.util.Locale

class EditorDialogs(
    private val context: Context,
    private val viewModel: MapEditorViewModel,
    private val density: Float,
    private val onRedraw: () -> Unit,
    private val onUpdateHint: () -> Unit,
    private val onToast: (String) -> Unit
) {
    fun askWidth() {
        val g = viewModel.getSel()
        if (g == null) {
            onToast("Select a line on the map first")
            return
        }
        val box = LinearLayout(context)
        box.orientation = LinearLayout.VERTICAL
        val pad = (16 * density).toInt()
        box.setPadding(pad, pad / 2, pad, 0)

        fun label(t: String): TextView {
            val v = TextView(context)
            v.text = t
            v.textSize = 12f
            v.setPadding(0, (8 * density).toInt(), 0, 0)
            return v
        }

        fun field(v: Double): EditText {
            val e = EditText(context)
            e.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            e.setText(String.format(Locale.US, "%.1f", v))
            return e
        }

        val etL = field(g.leftM)
        val etR = field(g.rightM)
        box.addView(label("Width to left of point, facing direction of travel (m)"))
        box.addView(etL)
        box.addView(label("Width to right of point (m)"))
        box.addView(etR)
        AlertDialog.Builder(context)
            .setTitle("Line Width · ${viewModel.selName()}")
            .setView(box)
            .setPositiveButton("Apply") { _, _ ->
                val l = (etL.text.toString().toDoubleOrNull() ?: g.leftM).coerceIn(MapEditorViewModel.MIN_WIDTH_M, MapEditorViewModel.MAX_WIDTH_M)
                val r = (etR.text.toString().toDoubleOrNull() ?: g.rightM).coerceIn(MapEditorViewModel.MIN_WIDTH_M, MapEditorViewModel.MAX_WIDTH_M)
                viewModel.setSel(g.copy(leftM = l, rightM = r))
                onRedraw()
                onUpdateHint()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
