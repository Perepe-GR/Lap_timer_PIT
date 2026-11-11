package com.ejemplo.laptimer

import android.content.Intent
import android.net.Uri
import android.text.InputType
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.ejemplo.laptimer.util.ValidationUtils
import java.net.URLEncoder
import java.util.Locale

/** General Settings: screen, telemetry, Pit Crew, Firebase, and backups. */
class SessionSettingsActivity : PageActivity() {
    override val pageTitle: String = "Settings & Configuration"

    private val viewModel: SettingsViewModel by viewModels()

    private val restoreLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) doRestore(uri)
        }

    override fun build() {
        val settings = viewModel.settings
        val currentUiState = viewModel.uiState.value

        val cDriverTeam = card("Driver & Team Configuration")
        val etTeamName = textField(
            cDriverTeam, "Team Session Name (Firebase room: /rooms/{teamName})",
            currentUiState.teamName, "e.g. TORQUE TEAM",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        )
        val etPin = textField(
            cDriverTeam, "PIN Access Code (Security credential)",
            currentUiState.pin, "••••",
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        )
        val etCarName = textField(
            cDriverTeam, "Car Name / Number (Telemetry identifier)",
            settings.carName, "Car 1",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        )
        val etDriverName = textField(
            cDriverTeam, "Driver Name (Active driver / Roster)",
            settings.driverName, "Driver 1",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        )

        val cPit = card("Telemetry & Pit Crew")
        val swEnabled = check(cPit, "Enable telemetry with Pit Wall", currentUiState.enabled)
        val etInt = numField(cPit, "Telemetry Interval (seconds; min 1)", currentUiState.intervalS.toString())

        addButton(cPit, button("SHARE TELEMETRY LINK") {
            val teamName = etTeamName.text.toString().trim()
            shareLink(settings.panelUrl, settings.dbUrl, teamName)
        })

        val c1 = card("Display & Driving Speed Lock")
        val etLock = numField(c1, "Touch Lock speed threshold (km/h; 0 = disabled)", settings.lockKmh.toString())
        val etRange = numField(
            c1, "Delta Bar Range (% relative to best lap)",
            String.format(Locale.US, "%.1f", settings.rangePct), true
        )
        val etSum = numField(c1, "Lap Summary Display Duration (seconds)", settings.lapSummarySec.toString())
        val swGamified = check(c1, "Gamified Performance Metrics", settings.gamifiedMetricsEnabled)

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
        label(cTheme, "Select UI color palette (applied instantly):")
        val themeOpts = ThemeManager.THEMES.map { it.id to "${it.name} · ${it.description}" }
        val currentThemeId = ThemeManager.getTheme(this).id
        choice(cTheme, themeOpts, currentThemeId) { chosenTheme ->
            if (chosenTheme.isNotBlank()) {
                ThemeManager.setTheme(this@SessionSettingsActivity, chosenTheme)
            }
        }

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
        val etDemo = numField(c4, "Demo pit stop duration (seconds; 0 = auto)", settings.demoStopSec.toString())

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
                val inputTeam = etTeamName.text.toString().trim()
                val inputPin = etPin.text.toString().trim()

                viewModel.onTeamNameChanged(inputTeam)
                viewModel.onPinChanged(inputPin)
                viewModel.onEnabledChanged(swEnabled.isChecked)
                viewModel.onIntervalChanged(etInt.text.toString().toIntOrNull() ?: 10)

                viewModel.validateAndSave(
                    onSuccess = {
                        settings.carName = etCarName.text.toString().trim()
                        settings.driverName = etDriverName.text.toString().trim()
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
                        settings.gamifiedMetricsEnabled = swGamified.isChecked
                        settings.gpsSource = gpsChoice()

                        runOnUiThread {
                            Toast.makeText(this@SessionSettingsActivity, "Settings saved successfully", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    },
                    onError = { errorMsg ->
                        runOnUiThread {
                            val idRes = ValidationUtils.validateIdentifier(inputTeam)
                            if (idRes.isFailure) {
                                etTeamName.error = idRes.exceptionOrNull()?.message
                            }
                            val pinRes = ValidationUtils.validatePin(inputPin)
                            if (pinRes.isFailure) {
                                etPin.error = pinRes.exceptionOrNull()?.message
                            }
                            Toast.makeText(this@SessionSettingsActivity, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
        )
    }

    private fun shareLink(panel: String, db: String, room: String) {
        if (panel.isBlank() || db.isBlank() || room.isBlank()) {
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
        } catch (_: Exception) {
            // ignore
        }
    }

    private fun doRestore(uri: Uri) {
        try {
            Backup.restore(this, uri)
        } catch (_: Exception) {
            // ignore
        }
    }
}
