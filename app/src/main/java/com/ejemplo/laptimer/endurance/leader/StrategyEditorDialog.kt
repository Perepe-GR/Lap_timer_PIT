package com.ejemplo.laptimer.endurance.leader

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.ejemplo.laptimer.endurance.sync.StrategyBase

class StrategyEditorDialog(
    private val context: Context,
    private val currentStrategy: StrategyBase,
    private val onSave: (StrategyBase) -> Unit
) {
    private fun dp(v: Int): Int = (v * context.resources.displayMetrics.density).toInt()

    private fun field(hint: String, initVal: String, inputType: Int = InputType.TYPE_CLASS_NUMBER): EditText {
        val e = EditText(context)
        e.hint = hint
        e.setText(initVal)
        e.inputType = inputType
        e.setHintTextColor(Color.parseColor("#9AA3AE"))
        e.setTextColor(Color.WHITE)
        e.textSize = 15f
        val g = GradientDrawable().apply {
            setColor(Color.parseColor("#12161B"))
            cornerRadius = dp(8).toFloat()
        }
        e.background = g
        e.setPadding(dp(12), dp(10), dp(12), dp(10))
        return e
    }

    fun show() {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }

        val banner = TextView(context).apply {
            text = "Strategy updates apply to all team drivers immediately."
            textSize = 12f
            setTextColor(Color.parseColor("#C6F432"))
            setPadding(0, 0, 0, dp(12))
        }
        root.addView(banner)

        val etStint = field("Stint Minutes (10-120)", currentStrategy.stintMinutes.toString())
        val etPitWindow = field("Pit Window Laps (0 = open)", currentStrategy.pitWindowLaps.toString())
        val etFuel = field("Fuel per Lap (liters)", currentStrategy.fuelPerLap.toString(), InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        val etNotes = field("Strategy Notes", currentStrategy.notes, InputType.TYPE_CLASS_TEXT)

        root.addView(etStint)
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) }
        root.addView(etPitWindow, lp)
        root.addView(etFuel, lp)
        root.addView(etNotes, lp)

        AlertDialog.Builder(context)
            .setTitle("Endurance Strategy Editor")
            .setView(root)
            .setPositiveButton("Save Strategy") { _, _ ->
                val stint = etStint.text.toString().toIntOrNull() ?: 45
                val window = etPitWindow.text.toString().toIntOrNull() ?: 0
                val fuel = etFuel.text.toString().toDoubleOrNull() ?: 1.0
                val notes = etNotes.text.toString().trim()
                val updated = StrategyBase(stint, window, fuel, notes, System.currentTimeMillis())
                onSave(updated)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
