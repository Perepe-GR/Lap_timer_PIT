package com.ejemplo.laptimer

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Detalle de una sesión: tabla de vueltas y acciones (analizar, exportar, borrar). */
class SessionActivity : AppCompatActivity() {

    private lateinit var store: SessionStore
    private lateinit var rec: SessionRecord

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = SessionStore(this)
        val id = intent.getStringExtra("id")
        val r = if (id != null) store.get(id) else null
        if (r == null) {
            finish()
            return
        }
        rec = r

        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(0xFF0B0D10.toInt())
        root.setPadding(pad, pad, pad, pad)

        val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val head = TextView(this)
        head.setTextColor(Color.WHITE)
        head.textSize = 15f
        head.typeface = Typeface.DEFAULT_BOLD
        head.text = fmt.format(Date(rec.startEpochMs)) + " · " + rec.circuitName + driverTag(rec) + "\n" +
            rec.laps.size + " laps · BEST " + fmtMs(rec.bestLap()?.lapMs) +
            " · Vmax " + String.format(Locale.US, "%.0f", rec.maxSpeedKmh) + " km/h · " +
            String.format(Locale.US, "%.1f", rec.distanceM / 1000.0) + " km · " +
            (rec.durationMs / 60_000) + " min"
        root.addView(head)

        val best = rec.bestLap()
        val lines = rec.laps.map { l ->
            val star = if (best != null && l.number == best.number) "★ " else "   "
            star + "V" + l.number + "   " + fmtMs(l.lapMs) + "   " +
                l.splits.joinToString("  ") { String.format(Locale.US, "%.2f", it / 1000.0) }
        }
        val list = ListView(this)
        list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, lines)
        root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        fun addButton(text: String, action: () -> Unit) {
            val b = Button(this)
            b.text = text
            b.setOnClickListener { action() }
            bar.addView(b, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        addButton("ANALIZAR") {
            if (!store.traceFile(rec.id).exists()) {
                toast("Esta sesión no guardó la traza GPS")
            } else {
                val i = Intent(this, AnalysisActivity::class.java)
                i.putExtra("id", rec.id)
                startActivity(i)
            }
        }
        addButton("CSV") {
            Export.share(this, "sesion-" + rec.id + ".csv", Export.lapsCsv(rec), "text/csv")
        }
        addButton("GPX") {
            val tr = TraceReader.read(store.traceFile(rec.id))
            if (tr == null) {
                toast("Esta sesión no guardó la traza GPS")
            } else {
                Export.share(this, "sesion-" + rec.id + ".gpx", Export.gpx(rec, tr), "application/gpx+xml")
            }
        }
        addButton("BORRAR") {
            AlertDialog.Builder(this)
                .setTitle("Borrar sesión")
                .setMessage("¿Borrar esta sesión y su traza?")
                .setPositiveButton("Borrar") { _, _ ->
                    store.delete(rec.id)
                    finish()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
        root.addView(bar)
        setContentView(root)
    }

    private fun toast(msg: String) { /* disabled per UI contract */ }
}
