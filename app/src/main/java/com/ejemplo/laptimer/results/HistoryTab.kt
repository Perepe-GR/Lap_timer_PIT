package com.ejemplo.laptimer.results

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.ejemplo.laptimer.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryTab(
    private val context: Context,
    private val store: SessionStore
) {
    val view: View
    private val list: ListView
    private val empty: TextView
    private var sessions: List<SessionRecord> = emptyList()

    init {
        val pad = (16 * context.resources.displayMetrics.density).toInt()
        val root = LinearLayout(context)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(0xFF0B0D10.toInt())
        root.setPadding(0, pad / 2, 0, 0)

        empty = TextView(context)
        empty.text = "No saved sessions yet. Sessions are recorded automatically when driving on an active circuit."
        empty.setTextColor(0xFF9AA3AE.toInt())
        empty.setPadding(pad, pad, pad, 0)
        root.addView(empty)

        list = ListView(context)
        root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        view = root

        list.setOnItemClickListener { _, _, pos, _ ->
            val i = Intent(context, SessionActivity::class.java)
            i.putExtra("id", sessions[pos].id)
            context.startActivity(i)
        }
        list.setOnItemLongClickListener { _, _, pos, _ ->
            confirmDelete(sessions[pos])
            true
        }
    }

    fun reload() {
        sessions = store.list()
        val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val lines = sessions.map { s ->
            fmt.format(Date(s.startEpochMs)) + " · " + s.circuitName + driverTag(s) + "\n" +
                s.laps.size + " laps · BEST " + fmtMs(s.bestLap()?.lapMs)
        }
        list.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, lines)
        empty.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun confirmDelete(s: SessionRecord) {
        AlertDialog.Builder(context)
            .setTitle("Delete session")
            .setMessage("Delete session for " + s.circuitName + "?")
            .setPositiveButton("Delete") { _, _ ->
                store.delete(s.id)
                reload()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
