package com.ejemplo.laptimer.endurance.pilot

import android.content.Intent
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
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ejemplo.laptimer.MainActivity
import com.ejemplo.laptimer.NetSettings
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PilotSessionActivity : AppCompatActivity() {

    private val viewModel: PilotSessionViewModel by viewModels()

    private lateinit var rootLayout: LinearLayout
    private lateinit var txtHeader: TextView
    private lateinit var txtStatus: TextView
    private lateinit var txtCountdown: TextView
    private lateinit var btnInCar: TextView
    private lateinit var btnExitVehicle: TextView
    private lateinit var txtGpsStatus: TextView

    private var isInOutMode = false

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settings = NetSettings(this)
        val teamCode = intent.getStringExtra("teamCode") ?: settings.ensureRoom()
        val pilotId = intent.getStringExtra("pilotId") ?: settings.deviceUid.ifBlank { "pilot-1" }

        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#16A34A")) // Green background for standby / countdown
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
            setTextColor(Color.WHITE)
            setPadding(0, dp(6), dp(16), dp(6))
            setOnClickListener { finish() }
        }
        navHeader.addView(back)
        val navTitle = TextView(this).apply {
            text = "Driver Cockpit Standby"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        navHeader.addView(navTitle)
        rootLayout.addView(navHeader)

        txtHeader = TextView(this).apply {
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            text = "Team: $teamCode · Driver: ${settings.driverName}"
        }
        rootLayout.addView(txtHeader)

        txtStatus = TextView(this).apply {
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(0, dp(6), dp(12), dp(6))
            text = "Awaiting relay · Time to kart"
        }
        rootLayout.addView(txtStatus)

        // Dynamic countdown timer view with green background
        txtCountdown = TextView(this).apply {
            textSize = 42f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#16A34A"))
            setPadding(dp(12), dp(16), dp(12), dp(16))
        }
        rootLayout.addView(txtCountdown, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(20) })

        // Large Confirmation Button "I'm in the car / Ready to drive"
        btnInCar = TextView(this).apply {
            text = "I'M IN THE CAR / READY TO DRIVE"
            gravity = Gravity.CENTER
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#0B0D10"))
            setBackgroundColor(Color.parseColor("#C6F432"))
            setPadding(0, dp(16), 0, dp(16))
            setOnClickListener {
                val intent = Intent(this@PilotSessionActivity, MainActivity::class.java).apply {
                    putExtra("pitlaneActive", true)
                }
                startActivity(intent)
                finish()
            }
        }
        rootLayout.addView(btnInCar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(16) })

        // Button for outgoing pilot to confirm exiting the vehicle
        btnExitVehicle = TextView(this).apply {
            text = "I'VE EXITED THE VEHICLE / OUT OF CAR"
            gravity = Gravity.CENTER
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#991B1B"))
            setPadding(0, dp(14), 0, dp(14))
            setOnClickListener {
                isInOutMode = true
                rootLayout.setBackgroundColor(Color.parseColor("#EF4444")) // Red resting/finished state
                txtCountdown.visibility = View.GONE
                btnInCar.visibility = View.GONE
                btnExitVehicle.visibility = View.GONE
                txtStatus.text = "RESTING · FINISHED (OUT OF CAR)"
                viewModel.leaveSession(teamCode, pilotId) {}
            }
        }
        rootLayout.addView(btnExitVehicle, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(16) })

        txtGpsStatus = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        }
        rootLayout.addView(txtGpsStatus)

        setContentView(rootLayout)
        viewModel.startSession(teamCode, pilotId)
        observeState(teamCode, pilotId)
    }

    private fun observeState(teamCode: String, pilotId: String) {
        lifecycleScope.launch {
            launch {
                viewModel.remainingSeconds.collectLatest { secs ->
                    val hrs = secs / 3600
                    val min = (secs % 3600) / 60
                    val sec = secs % 60
                    txtCountdown.text = if (hrs > 0) {
                        String.format(java.util.Locale.US, "%02d:%02d:%02d", hrs, min, sec)
                    } else {
                        String.format(java.util.Locale.US, "%02d:%02d", min, sec)
                    }
                }
            }
            launch {
                viewModel.uiState.collectLatest { state ->
                    if (state.ejected && !isInOutMode) {
                        Toast.makeText(this@PilotSessionActivity, "You have been removed from the team by the Crew Chief.", Toast.LENGTH_LONG).show()
                        finish()
                        return@collectLatest
                    }

                    if (state.pilotName.isNotBlank()) {
                        txtHeader.text = "Team: $teamCode · Driver: ${state.pilotName}"
                    }

                    if (!isInOutMode) {
                        if (state.isCurrentPilot) {
                            txtStatus.text = "YOUR STINT · TIME TO DRIVE!"
                        } else {
                            txtStatus.text = "RESTING · Awaiting relay (~${state.estimatedStintMinutes} min)"
                        }
                    }

                    if (!state.gpsValid) {
                        txtGpsStatus.text = "GPS DISCONNECTED"
                    } else {
                        txtGpsStatus.text = "GPS Active · ${state.syncStatus}"
                    }
                }
            }
        }
    }
}
