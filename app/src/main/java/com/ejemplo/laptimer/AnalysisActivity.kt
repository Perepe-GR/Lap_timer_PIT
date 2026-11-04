package com.ejemplo.laptimer

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Análisis post-sesión: compara dos vueltas (A y B) con la velocidad por distancia,
 * el delta de B respecto a A por distancia y el trazado coloreado por velocidad.
 */
class AnalysisActivity : AppCompatActivity() {

    private lateinit var rec: SessionRecord
    private lateinit var trace: Trace
    private lateinit var spA: Spinner
    private lateinit var spB: Spinner
    private lateinit var traceView: TraceView
    private lateinit var chartSpeed: LineChartView
    private lateinit var chartDelta: LineChartView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = SessionStore(this)
        val id = intent.getStringExtra("id")
        val r = if (id != null) store.get(id) else null
        val tr = if (id != null) TraceReader.read(store.traceFile(id)) else null
        if (r == null || tr == null || r.laps.isEmpty()) {
            Toast.makeText(this, "No hay datos para analizar", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        rec = r
        trace = tr

        val d = resources.displayMetrics.density
        val pad = (12 * d).toInt()

        fun label(t: String): TextView {
            val v = TextView(this)
            v.text = t
            v.textSize = 12f
            v.setTextColor(0xFF9AA3AE.toInt())
            v.typeface = Typeface.DEFAULT_BOLD
            return v
        }

        val root = LinearLayout(this)
        root.orientation = LinearLayout.HORIZONTAL
        root.setBackgroundColor(0xFF0B0D10.toInt())
        root.setPadding(pad, pad, pad, pad)

        // Izquierda: trazado
        val left = LinearLayout(this)
        left.orientation = LinearLayout.VERTICAL
        left.addView(label("TRAZADO DE LA LAP A (rojo = lento, verde = rápido)"))
        traceView = TraceView(this)
        left.addView(traceView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(left, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))

        // Derecha: selectores y gráficas
        val right = LinearLayout(this)
        right.orientation = LinearLayout.VERTICAL

        val labels = rec.laps.map { "LAP " + it.number + "  " + fmtMs(it.lapMs) }
        fun makeSpinner(): Spinner {
            val sp = Spinner(this)
            val ad = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
            ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            sp.adapter = ad
            return sp
        }
        spA = makeSpinner()
        spB = makeSpinner()
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.addView(label("A (verde) "))
        row.addView(spA, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(label("  B (blanca) "))
        row.addView(spB, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        right.addView(row)

        right.addView(label("VELOCIDAD (km/h) POR DISTANCIA"))
        chartSpeed = LineChartView(this)
        right.addView(chartSpeed, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        right.addView(label("DELTA DE B RESPECTO A (s): positivo = B más lenta"))
        chartDelta = LineChartView(this)
        right.addView(chartDelta, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.4f)
        lp.marginStart = pad
        root.addView(right, lp)
        setContentView(root)

        // Selección inicial: A = mejor vuelta, B = última (o la segunda mejor si coinciden)
        val bestIdx = rec.laps.indices.minByOrNull { rec.laps[it].lapMs } ?: 0
        var bIdx = rec.laps.size - 1
        if (bIdx == bestIdx && rec.laps.size > 1) bIdx = rec.laps.indices.filter { it != bestIdx }.minByOrNull { rec.laps[it].lapMs } ?: 0
        spA.setSelection(bestIdx)
        spB.setSelection(bIdx)

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                compute()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spA.onItemSelectedListener = listener
        spB.onItemSelectedListener = listener
        compute()
    }

    private fun compute() {
        val la = rec.laps[spA.selectedItemPosition]
        val lb = rec.laps[spB.selectedItemPosition]
        val a = Analysis.slice(trace, la.startOffsetMs, la.endOffsetMs)
        val b = Analysis.slice(trace, lb.startOffsetMs, lb.endOffsetMs)
        if (a == null || b == null) {
            chartSpeed.clear()
            chartDelta.clear()
            Toast.makeText(this, "No hay traza suficiente para esa lap", Toast.LENGTH_SHORT).show()
            return
        }
        traceView.setData(a.x, a.y, a.speed)

        val dmax = minOf(a.dist[a.dist.size - 1], b.dist[b.dist.size - 1])
        val n = (dmax / 2.0).toInt() + 1
        val grid = DoubleArray(n) { it * 2.0 }
        val sa = DoubleArray(n) { Analysis.interp(a.dist, a.speed, grid[it]) }
        val sb = DoubleArray(n) { Analysis.interp(b.dist, b.speed, grid[it]) }
        val dl = DoubleArray(n) {
            Analysis.interp(b.dist, b.time, grid[it]) - Analysis.interp(a.dist, a.time, grid[it])
        }
        chartSpeed.setData(grid, listOf(sa, sb), intArrayOf(0xFFC6F432.toInt(), Color.WHITE), false)
        chartDelta.setData(grid, listOf(dl), intArrayOf(0xFFF5A524.toInt()), true)
    }
}
