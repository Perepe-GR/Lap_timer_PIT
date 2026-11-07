package com.ejemplo.laptimer.results

import android.content.Context
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
        val theme = ThemeManager.getTheme(context)
        val pad = (14 * context.resources.displayMetrics.density).toInt()
        val root = LinearLayout(context)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(theme.bg)
        root.setPadding(pad, pad / 2, pad, pad)

        txtStats = TextView(context)
        txtStats.setTextColor(theme.text)
        txtStats.textSize = 14f
        txtStats.typeface = Typeface.MONOSPACE
        root.addView(txtStats)
        view = root

        reload()
    }

    fun reload() {
        val theme = ThemeManager.getTheme(context)
        val list = store.list()
        if (list.isEmpty()) {
            txtStats.text = "No overall statistics available yet. Complete sessions to view records and optimal laps."
            txtStats.setTextColor(theme.muted)
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
        txtStats.setTextColor(theme.text)
    }
}
