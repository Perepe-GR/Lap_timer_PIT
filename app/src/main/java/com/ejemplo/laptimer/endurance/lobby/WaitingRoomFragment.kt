package com.ejemplo.laptimer.endurance.lobby

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WaitingRoomFragment : Fragment() {

    private fun dp(v: Int): Int = (v * requireContext().resources.displayMetrics.density).toInt()

    private lateinit var txtInfo: TextView
    private lateinit var pilotList: ListView
    private lateinit var btnRetry: TextView
    private lateinit var txtTimeout: TextView

    private var timeoutSeconds = 30
    private var timerJob: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0B0D10"))
            setPadding(dp(20), dp(16), dp(20), dp(16))
        }

        txtInfo = TextView(requireContext()).apply {
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        root.addView(txtInfo)

        txtTimeout = TextView(requireContext()).apply {
            textSize = 14f
            setTextColor(Color.parseColor("#9AA3AE"))
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(4))
        }
        root.addView(txtTimeout)

        btnRetry = TextView(requireContext()).apply {
            text = "Retry Status Verification"
            gravity = Gravity.CENTER
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#0B0D10"))
            background = roundBg(Color.parseColor("#C6F432"), 10, requireContext())
            setPadding(0, dp(12), 0, dp(12))
            visibility = View.GONE
            setOnClickListener { resetTimeout() }
        }
        root.addView(btnRetry, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(12) })

        val lbl = TextView(requireContext()).apply {
            text = "Connected Team Drivers:"
            textSize = 14f
            setTextColor(Color.parseColor("#9AA3AE"))
        }
        root.addView(lbl, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) })

        pilotList = ListView(requireContext())
        root.addView(pilotList, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        observeState()
        startTimeoutTimer()
        return root
    }

    private fun startTimeoutTimer() {
        timerJob?.cancel()
        timeoutSeconds = 30
        timerJob = viewLifecycleOwner.lifecycleScope.launch {
            while (timeoutSeconds > 0) {
                delay(1000L)
                timeoutSeconds--
                txtTimeout.text = "Waiting for Crew Chief approval... (${timeoutSeconds}s)"
            }
            txtTimeout.text = "Connection timeout reached. Server response delayed."
            btnRetry.visibility = View.VISIBLE
        }
    }

    private fun resetTimeout() {
        btnRetry.visibility = View.GONE
        txtTimeout.text = "Rechecking status..."
        startTimeoutTimer()
    }

    private fun observeState() {
        val vm = (activity as? EnduranceLobbyActivity)?.viewModel ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            vm.uiState.collectLatest { state ->
                val tName = state.teamName.ifBlank { state.teamCode }
                val teamDisplay = if (tName.equals(state.teamCode, true)) tName else "$tName (${state.teamCode})"
                val circuitDisplay = state.circuitId.ifBlank { "Default Circuit" }
                txtInfo.text = "Team: $teamDisplay · Circuit: $circuitDisplay"

                val lines = state.pilots.values.map { p ->
                    val pilotName = p.info.name.ifBlank { "Pilot" }
                    val isDriving = state.currentPilotId == p.info.id || state.currentPilotId == p.info.name
                    val statusText = if (isDriving) "DRIVING" else p.info.status.ifBlank { "READY" }
                    "$pilotName (ID: ${p.info.id})  ·  Status: $statusText"
                }
                if (lines.isEmpty()) {
                    pilotList.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, listOf("Waiting for Crew Chief approval..."))
                } else {
                    pilotList.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, lines)
                }
            }
        }
    }

    override fun onDestroyView() {
        timerJob?.cancel()
        super.onDestroyView()
    }
}
