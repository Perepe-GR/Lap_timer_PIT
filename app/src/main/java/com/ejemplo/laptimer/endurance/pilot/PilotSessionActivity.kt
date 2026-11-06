package com.ejemplo.laptimer.endurance.pilot

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ejemplo.laptimer.NetSettings
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PilotSessionActivity : AppCompatActivity() {

    private val viewModel: PilotSessionViewModel by viewModels()

    private lateinit var rootLayout: LinearLayout
    private lateinit var txtHeader: TextView
    private lateinit var txtStatus: TextView
    private lateinit var txtTimer: TextView
    private lateinit var txtBest: TextView
    private lateinit var txtGpsStatus: TextView
    private lateinit var instructionBanner: LinearLayout
    private lateinit var txtInstruction: TextView

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settings = NetSettings(this)
        val teamCode = intent.getStringExtra("teamCode") ?: settings.ensureRoom()
        val pilotId = intent.getStringExtra("pilotId") ?: settings.deviceUid.ifBlank { "pilot-1" }

        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0B0D10"))
            setPadding(dp(20), dp(16), dp(20), dp(16))
        }

        val navHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(12))
        }
        val back = TextView(this).apply {
            text = "‹  BACK"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#C6F432"))
            setPadding(0, dp(6), dp(16), dp(6))
            setOnClickListener { finish() }
        }
        navHeader.addView(back)
        val navTitle = TextView(this).apply {
            text = "Driver Session Cockpit"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        navHeader.addView(navTitle)
        rootLayout.addView(navHeader)

        instructionBanner = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#FACC15"))
            setPadding(dp(16), dp(12), dp(16), dp(12))
            visibility = View.GONE
        }
        txtInstruction = TextView(this).apply {
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#141413"))
        }
        instructionBanner.addView(txtInstruction, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val btnCloseBanner = TextView(this).apply {
            text = "✕"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#141413"))
            setPadding(dp(10), 0, 0, 0)
            setOnClickListener {
                instructionBanner.visibility = View.GONE
                viewModel.dismissInstruction()
            }
        }
        instructionBanner.addView(btnCloseBanner)
        rootLayout.addView(instructionBanner, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(16) })

        txtHeader = TextView(this).apply {
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            text = "Team: $teamCode · Driver: ${settings.driverName}"
        }
        rootLayout.addView(txtHeader)

        txtStatus = TextView(this).apply {
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(16))
        }
        rootLayout.addView(txtStatus)

        txtTimer = TextView(this).apply {
            textSize = 48f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        rootLayout.addView(txtTimer)

        txtBest = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.parseColor("#9AA3AE"))
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(16))
        }
        rootLayout.addView(txtBest)

        txtGpsStatus = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
        }
        rootLayout.addView(txtGpsStatus)

        val btnExit = TextView(this).apply {
            text = "FINISH SESSION"
            gravity = Gravity.CENTER
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#EF4444"))
            setPadding(0, dp(14), 0, dp(14))
            setOnClickListener { confirmExit(teamCode, pilotId) }
        }
        rootLayout.addView(btnExit, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(24) })

        setContentView(rootLayout)
        viewModel.startSession(teamCode, pilotId)
        observeState(teamCode, pilotId)
    }

    private fun observeState(teamCode: String, pilotId: String) {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                if (state.ejected) {
                    Toast.makeText(this@PilotSessionActivity, "You have been removed from the team by the Crew Chief.", Toast.LENGTH_LONG).show()
                    finish()
                    return@collectLatest
                }

                if (state.pilotName.isNotBlank()) {
                    txtHeader.text = "Team: $teamCode · Driver: ${state.pilotName}"
                }

                if (state.isCurrentPilot) {
                    rootLayout.setBackgroundColor(Color.parseColor("#16A34A"))
                    txtStatus.text = "ON TRACK · DRIVING (YOUR STINT)"
                } else {
                    rootLayout.setBackgroundColor(Color.parseColor("#C2410C"))
                    txtStatus.text = "RESTING · Awaiting stint (~${state.estimatedStintMinutes} min)"
                }
                txtStatus.setTextColor(Color.WHITE)

                txtTimer.text = if (state.currentLapMs > 0) String.format(java.util.Locale.US, "%.1f s", state.currentLapMs / 1000.0) else "--:--.--"
                txtBest.text = "BEST: " + (state.bestLapMs?.let { String.format(java.util.Locale.US, "%.2f s", it / 1000.0) } ?: "--:--")

                if (!state.gpsValid) {
                    txtGpsStatus.text = "GPS DISCONNECTED"
                    txtGpsStatus.setTextColor(Color.parseColor("#EF4444"))
                } else {
                    txtGpsStatus.text = "GPS Active · ${state.syncStatus}"
                    txtGpsStatus.setTextColor(Color.parseColor("#C6F432"))
                }

                val instr = state.tacticalInstruction
                if (instr != null && instr.text.isNotBlank()) {
                    instructionBanner.visibility = View.VISIBLE
                    txtInstruction.text = "PIT: ${instr.text}"
                    val bgCol = when (instr.bg) {
                        "rojo" -> Color.parseColor("#EF4444")
                        "verde" -> Color.parseColor("#22C55E")
                        "azul" -> Color.parseColor("#2563EB")
                        "blanco" -> Color.parseColor("#F2F4F7")
                        else -> Color.parseColor("#FACC15")
                    }
                    val fgCol = if (instr.bg == "rojo" || instr.bg == "azul") Color.WHITE else Color.parseColor("#141413")
                    instructionBanner.setBackgroundColor(bgCol)
                    txtInstruction.setTextColor(fgCol)
                } else {
                    instructionBanner.visibility = View.GONE
                }

                if (state.unexpectedMovement) {
                    viewModel.dismissUnexpectedMovement()
                }
            }
        }
    }

    private fun confirmExit(teamCode: String, pilotId: String) {
        AlertDialog.Builder(this)
            .setTitle("Finish Session")
            .setMessage("Are you sure you want to finish and exit this endurance session?")
            .setPositiveButton("Finish Session") { _, _ ->
                viewModel.leaveSession(teamCode, pilotId) {
                    finish()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
