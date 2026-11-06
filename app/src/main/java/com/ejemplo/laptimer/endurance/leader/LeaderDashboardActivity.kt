package com.ejemplo.laptimer.endurance.leader

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ejemplo.laptimer.NetSettings
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LeaderDashboardActivity : AppCompatActivity() {

    private val viewModel: LeaderDashboardViewModel by viewModels()

    private lateinit var rootLayout: LinearLayout
    private lateinit var txtHeader: TextView
    private lateinit var txtCurrentPilot: TextView
    private lateinit var pilotsList: ListView
    private var teamCode = "EMPB-26"

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        teamCode = intent.getStringExtra("teamCode") ?: "EMPB-26"

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
            text = "Team Leader Dashboard"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        navHeader.addView(navTitle)
        rootLayout.addView(navHeader)

        // Dashboard Info Row
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        txtHeader = TextView(this).apply {
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#9AA3AE"))
        }
        headerRow.addView(txtHeader, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val btnStrat = TextView(this).apply {
            text = "STRATEGY"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#0B0D10"))
            setBackgroundColor(Color.parseColor("#C6F432"))
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setOnClickListener {
                StrategyEditorDialog(this@LeaderDashboardActivity, viewModel.uiState.value.strategy) { updated ->
                    viewModel.updateStrategy(teamCode, updated)
                }.show()
            }
        }
        headerRow.addView(btnStrat)
        rootLayout.addView(headerRow)

        // Current Pilot Card
        val curCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#12161B"))
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        txtCurrentPilot = TextView(this).apply {
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        curCard.addView(txtCurrentPilot, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val btnSwap = TextView(this).apply {
            text = "SWAP DRIVER"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#C2410C"))
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setOnClickListener {
                val state = viewModel.uiState.value
                PilotChangeDialog(this@LeaderDashboardActivity, state.currentPilotId, state.pilots) { newPilotId ->
                    viewModel.changePilot(teamCode, newPilotId)
                }.show()
            }
        }
        curCard.addView(btnSwap)

        val lpCard = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(16)
            bottomMargin = dp(16)
        }
        rootLayout.addView(curCard, lpCard)

        // Drivers List
        pilotsList = ListView(this)
        rootLayout.addView(pilotsList, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        // Footer Finish Button
        val btnFinish = TextView(this).apply {
            text = "FINISH SESSION"
            gravity = Gravity.CENTER
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#EF4444"))
            setPadding(0, dp(14), 0, dp(14))
            setOnClickListener { confirmFinish() }
        }
        val lpFin = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(16)
        }
        rootLayout.addView(btnFinish, lpFin)

        setContentView(rootLayout)

        viewModel.start(teamCode)
        observeState()
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                txtHeader.text = "Team: ${state.teamCode}"
                txtCurrentPilot.text = "On Track: ${state.currentPilotName.uppercase()}"

                val items = state.pilots.values.map { p ->
                    val curMark = if (p.info.id == state.currentPilotId) " [ON TRACK]" else ""
                    "${p.info.name}$curMark  ·  Status: ${p.info.status}  ·  Laps: ${p.laps.size}"
                }
                pilotsList.adapter = ArrayAdapter(this@LeaderDashboardActivity, android.R.layout.simple_list_item_1, items)

                state.unexpectedPilotName?.let { unkName ->
                    viewModel.dismissUnexpectedPilot()
                }
            }
        }
    }

    private fun confirmFinish() {
        AlertDialog.Builder(this)
            .setTitle("Finish Endurance Session")
            .setMessage("Are you sure you want to finish the endurance session for the entire team?")
            .setPositiveButton("Finish Session") { _, _ ->
                NetSettings(this).clearEnduranceSession()
                viewModel.finishSession(teamCode)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
