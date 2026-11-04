package com.ejemplo.laptimer

import android.content.Intent
import android.net.Uri
import android.text.InputType
import androidx.activity.result.contract.ActivityResultContracts
import java.net.URLEncoder
import java.util.Locale

/** General Settings: screen, telemetry, Pit Crew, Firebase, and backups. */
class SessionSettingsActivity : PageActivity() {
    override val pageTitle: String = "Settings & Configuration"

    private val restoreLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) doRestore(uri)
        }

    override fun build() {
        val settings = NetSettings(this)

        val cPit = card("Telemetry & Pit Crew")
        val swEnabled = check(cPit, "Enable telemetry with Pit Wall", settings.enabled)
        val etRoom = textField(
            cPit, "Room Code (10 characters)", settings.ensureRoom(), "",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        )
        addButton(cPit, button("GENERATE NEW CODE") { etRoom.setText(NetSettings.newCode()) })
        // Car Name, Current Driver and Pit Web Panel URL removed from settings UI (fixed by project default)
        val etInt = numField(cPit, "Telemetry Interval (seconds; min 10)", settings.intervalS.toString())
        addButton(cPit, button("SHARE TELEMETRY LINK") {
            shareLink(settings.panelUrl, settings.dbUrl, etRoom.text.toString())
        })

        val c1 = card("Display & Driving Speed Lock")
        val etLock = numField(c1, "Touch Lock speed threshold (km/h; 0 = disabled)", settings.lockKmh.toString())
        val etRange = numField(
            c1, "Delta Bar Range (% relative to best lap)",
            String.format(Locale.US, "%.1f", settings.rangePct), true
        )
        val etSum = numField(c1, "Lap Summary Display Duration (seconds)", settings.lapSummarySec.toString())

        val cGps = card("Position / GPS Receiver Source")
        label(cGps, "Select GPS receiver source:")
        val gpsOpts = listOf(
            "usb" to "External USB u-blox GPS Antenna (25 Hz) [Recommended]",
            "internal" to "Internal Device / Android GPS",
            "racebox" to "RaceBox (Mini / Micro / Pro)",
            "dragy" to "Dragy (10Hz / 25Hz)",
            "garmin" to "Garmin (Catalyst / GLO)",
            "external_bt" to "Other External Bluetooth GPS (NMEA / Mock)"
        )
        val gpsChoice = choice(cGps, gpsOpts, settings.gpsSource)

        val cTheme = card("Color Theme & Palette")
        label(cTheme, "Select UI color palette:")
        val themeOpts = ThemeManager.THEMES.map { it.id to "${it.name} · ${it.description}" }
        val currentThemeId = ThemeManager.getTheme(this).id
        val themeChoice = choice(cTheme, themeOpts, currentThemeId)

        val c3 = card("Delta Reference & Out/In Lap Rules")
        val scope = choice(
            c3,
            listOf("coche" to "Global Car Reference", "piloto" to "Individual Driver Reference"),
            if (settings.refScope == "piloto") "piloto" else "coche"
        )
        val cbOut = check(c3, "OUT LAP (pit exit to finish) counts as numbered lap", settings.outlapCounts)
        val cbIn = check(c3, "IN LAP (finish to pit entry) counts as numbered lap", settings.inlapCounts)

        val c2 = card("Battery Alerts")
        val etWarn = newNum(settings.batWarn.toString())
        val etCrit = newNum(settings.batCrit.toString())
        rowOf(c2, block("Low Battery Alert (%)", etWarn), block("Critical Battery Alert (%)", etCrit))

        val c4 = card("Demo Simulator")
        val etDemo = numField(c4, "Demo pit stop stop duration (seconds; 0 = auto)", settings.demoStopSec.toString())

        val c5 = card("Backup & Data")
        val swTrace = check(c5, "Save full GPS trace in session files", settings.saveTrace)
        rowOf(
            c5,
            button("EXPORT BACKUP") { makeBackup(swTrace.isChecked) },
            button("RESTORE BACKUP") { restoreLauncher.launch(arrayOf("*/*")) }
        )

        addFooter(
            button("CANCEL") { finish() },
            button("SAVE SETTINGS", true) {
                settings.enabled = swEnabled.isChecked
                settings.room = etRoom.text.toString()
                // carName, driverName and panelUrl retained in model but removed from settings UI
                settings.intervalS = etInt.text.toString().toIntOrNull() ?: 10

                settings.lockKmh = etLock.text.toString().toIntOrNull() ?: 10
                settings.rangePct = etRange.text.toString().toFloatOrNull() ?: 2f
                settings.lapSummarySec = etSum.text.toString().toIntOrNull() ?: 4
                settings.batWarn = etWarn.text.toString().toIntOrNull() ?: 20
                settings.batCrit = etCrit.text.toString().toIntOrNull() ?: 10
                settings.refScope = scope()
                settings.outlapCounts = cbOut.isChecked
                settings.inlapCounts = cbIn.isChecked
                settings.demoStopSec = etDemo.text.toString().toIntOrNull() ?: 0
                settings.saveTrace = swTrace.isChecked
                settings.gpsSource = gpsChoice()

                val chosenTheme = themeChoice()
                if (chosenTheme.isNotBlank()) {
                    ThemeManager.setTheme(this@SessionSettingsActivity, chosenTheme)
                }

                toast("Settings saved successfully")
                finish()
            }
        )
    }

    private fun shareLink(panel: String, db: String, room: String) {
        if (panel.isBlank() || db.isBlank()) {
            toast("Firebase URL or Pit Panel URL missing")
            return
        }
        val sep = if (panel.contains("?")) "&" else "?"
        val url = panel + sep + "db=" + URLEncoder.encode(db, "UTF-8") + "&room=" + room
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        startActivity(Intent.createChooser(i, "Share Pit Panel Link"))
    }

    private fun makeBackup(traces: Boolean) {
        try {
            val f = Backup.create(this, traces)
            Export.shareFile(this, f, "application/zip")
        } catch (e: Exception) {
            toast("Error creating backup: ${e.message}")
        }
    }

    private fun doRestore(uri: Uri) {
        try {
            val n = Backup.restore(this, uri)
            toast("Backup restored successfully ($n files)")
        } catch (e: Exception) {
            toast("Error restoring backup: ${e.message}")
        }
    }
}
