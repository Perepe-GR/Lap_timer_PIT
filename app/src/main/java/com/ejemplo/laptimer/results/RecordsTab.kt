package com.ejemplo.laptimer.results

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import com.ejemplo.laptimer.*

class RecordsTab(
    private val context: Context,
    private val store: SessionStore
) {
    val view: View
    private var sessions: List<SessionRecord> = emptyList()
    private var circuits: List<String> = emptyList()
    private var drivers: List<String> = emptyList()
    private val spCircuit: Spinner
    private val spScope: Spinner
    private val txtInfo: TextView
    private val chart: LineChartView

    init {
        val theme = ThemeManager.getTheme(context)
        val pad = (14 * context.resources.displayMetrics.density).toInt()
        val root = LinearLayout(context)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(theme.bg)
        root.setPadding(pad, pad / 2, pad, pad)

        sessions = store.list()
        circuits = sessions.map { it.circuitName }.distinct().sorted()

        if (circuits.isEmpty()) {
            val t = TextView(context)
            t.text = "No saved sessions yet. Records are computed from real session logs."
            t.setTextColor(theme.muted)
            t.textSize = 14f
            t.setPadding(0, pad, 0, 0)
            root.addView(t)
            view = root
            spCircuit = Spinner(context)
            spScope = Spinner(context)
            txtInfo = TextView(context)
            chart = LineChartView(context)
        } else {
            spCircuit = Spinner(context)
            spCircuit.adapter = adapter(circuits)
            spScope = Spinner(context)
            val row = LinearLayout(context)
            row.orientation = LinearLayout.HORIZONTAL
            row.addView(spCircuit, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(spScope, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            root.addView(row)

            txtInfo = TextView(context)
            txtInfo.setTextColor(theme.text)
            txtInfo.textSize = 14f
            txtInfo.setPadding(0, pad / 2, 0, pad / 2)
            root.addView(txtInfo)

            val lbl = TextView(context)
            lbl.text = "BEST LAP PER SESSION (s): green = each session, white = best so far"
            lbl.textSize = 12f
            lbl.typeface = Typeface.DEFAULT_BOLD
            lbl.setTextColor(theme.muted)
            root.addView(lbl)

            chart = LineChartView(context)
            root.addView(chart, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            view = root

            spCircuit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    loadScopes()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            spScope.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    refresh()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            loadScopes()
        }
    }

    fun reload() {
        sessions = store.list()
        circuits = sessions.map { it.circuitName }.distinct().sorted()
        if (circuits.isNotEmpty()) {
            spCircuit.adapter = adapter(circuits)
            loadScopes()
        }
    }

    private fun adapter(items: List<String>): ArrayAdapter<String> {
        val ad = ArrayAdapter(context, android.R.layout.simple_spinner_item, items)
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        return ad
    }

    private fun loadScopes() {
        if (circuits.isEmpty()) return
        val c = circuits[spCircuit.selectedItemPosition.coerceIn(0, circuits.size - 1)]
        drivers = sessions.filter { it.circuitName == c && it.driver.isNotEmpty() }
            .map { it.driver }.distinct().sorted()
        val labels = ArrayList<String>()
        labels.add("Car (all drivers)")
        for (d in drivers) labels.add("Driver: $d")
        spScope.adapter = adapter(labels)
        spScope.setSelection(0)
        refresh()
    }

    private fun refresh() {
        val theme = ThemeManager.getTheme(context)
        if (circuits.isEmpty()) return
        val c = circuits[spCircuit.selectedItemPosition.coerceIn(0, circuits.size - 1)]
        val idx = spScope.selectedItemPosition
        val driver: String? = if (idx > 0 && idx - 1 < drivers.size) drivers[idx - 1] else null
        val list = sessions.filter { it.circuitName == c && (driver == null || it.driver == driver) }
            .sortedBy { it.startEpochMs }
        if (list.isEmpty()) {
            txtInfo.text = "No sessions for this selection."
            chart.clear()
            return
        }
        val stats = computeRecordsStats(list, driver)
        txtInfo.text = stats.infoText

        if (stats.perSession.size < 2) {
            chart.clear()
            txtInfo.text = txtInfo.text.toString() + "\n(At least 2 sessions needed for chart)"
            return
        }
        val xs = DoubleArray(stats.perSession.size) { (it + 1).toDouble() }
        chart.setData(
            xs,
            listOf(stats.perSession.toDoubleArray(), stats.soFar.toDoubleArray()),
            intArrayOf(theme.accent, theme.text),
            false,
            " ses."
        )
    }
}
