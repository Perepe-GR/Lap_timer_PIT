package com.ejemplo.laptimer.endurance.pilot

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
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

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val teamCode = intent.getStringExtra("teamCode") ?: "EMPB-26"
        val pilotId = intent.getStringExtra("pilotId") ?: "pilot-1"

        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0B0D10"))
            setPadding(dp(20), dp(16), dp(20), dp(16))
        }

        // Top Navigation Header (Back + Title)
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
            text = "Driver Session"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        navHeader.addView(navTitle)
        rootLayout.addView(navHeader)

        txtHeader = TextView(this).apply {
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            text = "Team: $teamCode"
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
            text = "EXIT SESSION"
            gravity = Gravity.CENTER
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#12161B"))
            setPadding(0, dp(14), 0, dp(14))
            setOnClickListener { confirmExit() }
        }
        val lpBtn = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lpBtn.topMargin = dp(24)
        rootLayout.addView(btnExit, lpBtn)

        setContentView(rootLayout)

        viewModel.startSession(teamCode, pilotId)

        observeState()
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                if (state.isCurrentPilot) {
                    rootLayout.setBackgroundColor(Color.parseColor("#16A34A")) // Green when ON_TRACK
                    txtStatus.text = "ON TRACK · YOUR STINT"
                    txtStatus.setTextColor(Color.WHITE)
                } else {
                    rootLayout.setBackgroundColor(Color.parseColor("#C2410C")) // Orange/amber when RESTING
                    txtStatus.text = "RESTING · Awaiting stint (~${state.estimatedStintMinutes} min)"
                    txtStatus.setTextColor(Color.WHITE)
                }

                txtTimer.text = if (state.currentLapMs > 0) String.format(java.util.Locale.US, "%.1f s", state.currentLapMs / 1000.0) else "--:--.--"
                txtBest.text = "BEST: " + (state.bestLapMs?.let { String.format(java.util.Locale.US, "%.2f s", it / 1000.0) } ?: "--:--")

                if (!state.gpsValid) {
                    txtGpsStatus.text = "GPS DISCONNECTED"
                    txtGpsStatus.setTextColor(Color.parseColor("#EF4444"))
                } else {
                    txtGpsStatus.text = "GPS Active · ${state.syncStatus}"
                    txtGpsStatus.setTextColor(Color.parseColor("#C6F432"))
                }

                if (state.unexpectedMovement) {
                    viewModel.dismissUnexpectedMovement()
                }
            }
        }
    }

    private fun confirmExit() {
        AlertDialog.Builder(this)
            .setTitle("Exit Session")
            .setMessage("Are you sure you want to exit this endurance session?")
            .setPositiveButton("Exit") { _, _ -> finish() }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
