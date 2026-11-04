package com.ejemplo.laptimer

import android.content.Intent
import android.text.InputType
import java.net.URLEncoder

/** Pit crew and telemetry settings. */
class PitSettingsActivity : PageActivity() {
    override val pageTitle: String = "Pit Crew & Telemetry"

    override fun build() {
        val settings = NetSettings(this)

        val c1 = card("Pit Connection")
        val swEnabled = check(c1, "Enable telemetry with Pit Wall", settings.enabled)
        val etRoom = textField(
            c1, "Room Code (10 characters)", settings.ensureRoom(), "",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        )
        addButton(c1, button("GENERATE NEW CODE") { etRoom.setText(NetSettings.newCode()) })
        val etInt = numField(c1, "Telemetry Interval (seconds; min 10)", settings.intervalS.toString())

        val c3 = card("Data & Traces")
        val swTrace = check(c3, "Save full GPS trace in session files", settings.saveTrace)

        fun saveAll() {
            settings.enabled = swEnabled.isChecked
            settings.room = etRoom.text.toString()
            settings.intervalS = etInt.text.toString().toIntOrNull() ?: 10
            settings.saveTrace = swTrace.isChecked
        }

        addFooter(
            button("CANCEL") { finish() },
            button("SHARE LINK") {
                saveAll()
                shareLink(settings)
            },
            button("SAVE", true) {
                saveAll()
                toast("Settings saved")
                finish()
            }
        )
    }

    private fun shareLink(settings: NetSettings) {
        val panel = settings.panelUrl
        val sep = if (panel.contains("?")) "&" else "?"
        val url = panel + sep + "db=" + URLEncoder.encode(settings.dbUrl, "UTF-8") + "&room=" + settings.room
        val i = Intent(Intent.ACTION_SEND)
        i.type = "text/plain"
        i.putExtra(Intent.EXTRA_TEXT, url)
        startActivity(Intent.createChooser(i, "Share Pit Panel Link"))
    }
}
