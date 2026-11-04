package com.ejemplo.laptimer

import androidx.appcompat.app.AlertDialog
import java.util.Locale

/** Saved race summaries: lap times, driver, and stint records. */
class RacesActivity : PageActivity() {
    override val pageTitle: String = "Race Summaries"

    private lateinit var races: RaceStore

    override fun build() {
        races = RaceStore(this)
        val list = races.list()
        if (list.isEmpty()) {
            val c = card()
            note(c, "No race summaries saved yet. Summaries are automatically created upon race completion or session close.")
            return
        }
        val df = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        for (r in list) {
            val title = df.format(java.util.Date(r.startEpoch)) + " · " + r.circuit
            val c = card(title)
            val mode = when (r.mode) {
                "sprint" -> "SPRINT"
                "resistencia" -> "ENDURANCE"
                else -> "TRACK DAY"
            }
            val end = if (r.reason == "target") "Completed by target" else "Session closed"
            note(
                c,
                mode + " · LAPS " + r.counted() + " · BEST " + fmtMs(r.bestMs()) +
                    " · TIME " + mmssOf(r.durMs) + "\n" + end
            )
            val detail = note(c, lapLines(r))
            detail.visibility = android.view.View.GONE
            detail.typeface = android.graphics.Typeface.MONOSPACE
            addButton(c, button("SHOW / HIDE LAPS") {
                detail.visibility = if (detail.visibility == android.view.View.GONE) android.view.View.VISIBLE
                else android.view.View.GONE
            })
            addButton(c, button("DELETE") {
                AlertDialog.Builder(this)
                    .setTitle("Delete this race summary?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        races.delete(r.id)
                        rebuild()
                    }
                    .show()
            })
        }
    }

    private fun lapLines(r: RaceRecord): String {
        val sb = StringBuilder()
        val best = r.bestMs()
        for (l in r.laps) {
            val tag = when (l.kind) {
                1 -> "OUT"
                2 -> "IN "
                else -> if (l.n > 0) String.format(Locale.US, "%3d", l.n) else "   "
            }
            sb.append("LAP ").append(tag).append("  ").append(fmtMs(l.ms))
            if (l.kind == 0 && best != null && l.ms == best) sb.append("  BEST")
            if (l.driver.isNotBlank()) sb.append("  ").append(l.driver)
            if (l.stint > 0) sb.append(" S").append(l.stint)
            sb.append('\n')
        }
        return sb.toString().trimEnd()
    }
}
