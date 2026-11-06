package com.ejemplo.laptimer

import android.content.Intent
import android.net.Uri
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.ejemplo.laptimer.editor.MapEditorActivity

/** Circuits management: select, create, edit, delete, import, export. */
class CircuitsActivity : PageActivity() {
    override val pageTitle: String = "Circuits"

    private lateinit var store: CircuitStore
    private lateinit var statsStore: StatsStore

    private val importLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) importCircuit(uri)
        }

    override fun build() {
        store = CircuitStore(this)
        statsStore = StatsStore(this)
        store.syncFromFirebase()
        val active = store.loadActive()

        val c0 = card("Active Circuit")
        note(c0, active?.name ?: "None")
        if (active != null) {
            val boxes = if (active.hasPitLane) " · with PIT" else ""
            note(c0, "${active.sectorCount} sectors$boxes").setTextColor(colMuted)
        }

        val list = store.list()
        val c1 = card("Saved Circuits")
        if (list.isEmpty()) {
            note(c1, "No circuits saved.")
        } else {
            for (c in list) {
                val row = LinearLayout(this)
                row.orientation = LinearLayout.HORIZONTAL
                row.gravity = Gravity.CENTER_VERTICAL
                row.setPadding(0, dp(8), 0, 0)
                val isActive = c.name == active?.name
                val name = TextView(this)
                name.text = c.name + (if (c.hasPitLane) "  · PIT" else "")
                name.textSize = 16f
                name.setTextColor(if (isActive) colLime else colText)
                row.addView(name, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                if (!isActive) {
                    row.addView(button("SELECT") {
                        store.activeName = c.name
                        toast("Active circuit: ${c.name}")
                        rebuild()
                    })
                }
                val del = button("DELETE") { confirmDelete(c) }
                val dlp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                dlp.marginStart = dp(8)
                row.addView(del, dlp)
                c1.addView(row)
            }
        }

        val c2 = card("New Circuit")
        val etName = textField(c2, "Circuit Name", "", "Circuit name")
        rowOf(
            c2,
            button("ON MAP", true) { newCircuit(etName.text.toString().trim(), true) },
            button("BY RECONNAISSANCE") { newCircuit(etName.text.toString().trim(), false) }
        )

        val c3 = card("Active Circuit Actions")
        addButton(c3, button("EDIT ON MAP (gate width, PIT & PADDOCK)") { editOnMap() })
        addButton(c3, button("EXPORT") { exportCircuit() })
        addButton(c3, button("IMPORT CIRCUIT") { importLauncher.launch(arrayOf("*/*")) })
    }

    override fun onRestart() {
        super.onRestart()
        rebuild()
    }

    private fun newCircuit(name: String, onMap: Boolean) {
        if (name.isEmpty()) {
            toast("Enter a circuit name")
            return
        }
        if (onMap) {
            openMapEditor(name, true)
        } else {
            val i = Intent(this, MainActivity::class.java)
            i.putExtra("recon", name)
            startActivity(i)
        }
    }

    private fun openMapEditor(name: String, isNew: Boolean) {
        val i = Intent(this, MapEditorActivity::class.java)
        i.putExtra("name", name)
        i.putExtra("new", isNew)
        val pos = store.lastPosition()
        if (pos != null) {
            i.putExtra("lat", pos.first)
            i.putExtra("lon", pos.second)
        }
        startActivity(i)
    }

    private fun editOnMap() {
        val c = store.loadActive()
        if (c == null) {
            toast("No active circuit")
            return
        }
        openMapEditor(c.name, false)
    }

    private fun confirmDelete(c: Circuit) {
        AlertDialog.Builder(this)
            .setTitle("Delete «${c.name}»")
            .setMessage("This will also delete reference marks and records. Saved session logs will not be affected.")
            .setPositiveButton("Delete") { _, _ ->
                store.delete(c.name)
                statsStore.clear(c.name)
                if (store.activeName == c.name) store.activeName = null
                toast("Circuit deleted")
                rebuild()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun exportCircuit() {
        val c = store.loadActive()
        if (c == null) {
            toast("No active circuit")
            return
        }
        Export.share(this, c.name + ".json", c.toJson(), "application/json")
    }

    private fun importCircuit(uri: Uri) {
        try {
            val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: throw IllegalStateException("Empty file")
            val c = Circuit.fromJson(text)
            store.save(c)
            store.activeName = c.name
            toast("Circuit imported: ${c.name}")
            rebuild()
        } catch (e: Exception) {
            toast("Could not import circuit: ${e.message}")
        }
    }
}
