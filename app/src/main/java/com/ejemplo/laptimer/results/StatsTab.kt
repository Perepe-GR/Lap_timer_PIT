package com.ejemplo.laptimer.results

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.ejemplo.laptimer.*

class StatsTab(
    private val context: Context,
    private val store: SessionStore
) {
    val view: View
    private val txtStats: TextView

    init {
        val pad = (16 * context.resources.displayMetrics.density).toInt()
        val root = LinearLayout(context)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(0xFF0B0D10.toInt())
        root.setPadding(pad, pad, pad, pad)

        txtStats = TextView(context)
        txtStats.setTextColor(Color.WHITE)
        txtStats.textSize = 15f
        txtStats.typeface = Typeface.MONOSPACE
        root.addView(txtStats)
        view = root

        reload()
    }

    fun reload() {
        val list = store.list()
        if (list.isEmpty()) {
            txtStats.text = "No overall statistics available yet. Complete sessions to view records and optimal laps."
            txtStats.setTextColor(0xFF9AA3AE.toInt())
            return
        }
        val circuits = list.map { it.circuitName }.distinct().sorted()
        val sb = StringBuilder()
        sb.append("OVERALL STATISTICS\n")
        sb.append("Total Sessions: ").append(list.size).append("\n")
        sb.append("Total Laps: ").append(list.sumOf { it.laps.size }).append("\n\n")

        for (c in circuits) {
            val cSessions = list.filter { it.circuitName == c }
            val stats = computeRecordsStats(cSessions, null)
            sb.append("=== ").append(c).append(" ===\n")
            sb.append(stats.infoText).append("\n\n")
        }
        txtStats.text = sb.toString().trimEnd()
        txtStats.setTextColor(Color.WHITE)
    }
}
