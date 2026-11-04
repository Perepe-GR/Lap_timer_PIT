package com.ejemplo.laptimer.endurance.leader

import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import com.ejemplo.laptimer.endurance.sync.PilotData

class PilotChangeDialog(
    private val context: Context,
    private val currentPilotId: String,
    private val pilots: Map<String, PilotData>,
    private val onConfirm: (String) -> Unit
) {
    fun show() {
        val availablePilots = pilots.filter { it.key != currentPilotId }.values.toList()
        if (availablePilots.isEmpty()) {
            AlertDialog.Builder(context)
                .setTitle("Driver Swap")
                .setMessage("No other drivers registered in team yet.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val pad = (16 * context.resources.displayMetrics.density).toInt()
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        val listView = ListView(context)
        val items = availablePilots.map { p ->
            "${p.info.name}  ·  Status: ${p.info.status}"
        }
        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_single_choice, items)
        listView.adapter = adapter
        listView.choiceMode = ListView.CHOICE_MODE_SINGLE
        root.addView(listView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (200 * context.resources.displayMetrics.density).toInt()))

        var selectedIndex = -1
        listView.setOnItemClickListener { _, _, position, _ ->
            selectedIndex = position
        }

        AlertDialog.Builder(context)
            .setTitle("Select New Driver")
            .setView(root)
            .setPositiveButton("Confirm Swap") { _, _ ->
                if (selectedIndex in availablePilots.indices) {
                    val p = availablePilots[selectedIndex]
                    if (p.info.status == "ON_TRACK") {
                        AlertDialog.Builder(context)
                            .setTitle("Confirm Driver")
                            .setMessage("${p.info.name} is currently marked ON_TRACK. Perform swap now?")
                            .setPositiveButton("Swap Now") { _, _ -> onConfirm(p.info.id) }
                            .setNegativeButton("Cancel", null)
                            .show()
                    } else {
                        onConfirm(p.info.id)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
