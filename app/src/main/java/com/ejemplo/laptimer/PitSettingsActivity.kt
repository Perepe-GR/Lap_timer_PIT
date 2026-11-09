package com.ejemplo.laptimer

import android.content.Intent
import android.text.InputType
import android.widget.Toast
import androidx.activity.viewModels
import com.ejemplo.laptimer.util.ValidationUtils
import java.net.URLEncoder

/** Pit crew and telemetry settings. */
class PitSettingsActivity : PageActivity() {
    override val pageTitle: String = "Pit Crew & Telemetry"

    private val viewModel: SettingsViewModel by viewModels()

    override fun build() {
        val settings = viewModel.settings
        val currentUiState = viewModel.uiState.value

        val c1 = card("Pit Connection")
        val swEnabled = check(c1, "Enable telemetry with Pit Wall", currentUiState.enabled)
        val etTeamName = textField(
            c1, "Team / Session Name (6-15 chars, alphanumeric)",
            currentUiState.teamName, "Ej. TORQUE TEAM",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        )
        val etPin = textField(
            c1, "Access PIN (4-6 digits)",
            currentUiState.pin, "••••",
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        )
        val etInt = numField(c1, "Telemetry Interval (seconds; min 1)", currentUiState.intervalS.toString())

        val c3 = card("Data & Traces")
        val swTrace = check(c3, "Save full GPS trace in session files", settings.saveTrace)

        addFooter(
            button("CANCEL") { finish() },
            button("SHARE LINK") {
                val teamName = etTeamName.text.toString().trim()
                shareLink(settings.panelUrl, settings.dbUrl, teamName)
            },
            button("SAVE", true) {
                val inputTeam = etTeamName.text.toString().trim()
                val inputPin = etPin.text.toString().trim()

                viewModel.onTeamNameChanged(inputTeam)
                viewModel.onPinChanged(inputPin)
                viewModel.onEnabledChanged(swEnabled.isChecked)
                viewModel.onIntervalChanged(etInt.text.toString().toIntOrNull() ?: 10)

                viewModel.validateAndSave(
                    onSuccess = {
                        settings.saveTrace = swTrace.isChecked
                        runOnUiThread {
                            Toast.makeText(this@PitSettingsActivity, "Settings saved successfully", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(this@PitSettingsActivity, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
        )
    }

    private fun shareLink(panel: String, dbUrl: String, room: String) {
        if (panel.isBlank() || dbUrl.isBlank() || room.isBlank()) return
        val sep = if (panel.contains("?")) "&" else "?"
        val url = panel + sep + "db=" + URLEncoder.encode(dbUrl, "UTF-8") + "&room=" + room
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        startActivity(Intent.createChooser(i, "Share Pit Panel Link"))
    }
}
