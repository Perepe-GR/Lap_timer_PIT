package com.ejemplo.laptimer.endurance.lobby

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ejemplo.laptimer.NetSettings
import com.ejemplo.laptimer.endurance.leader.LeaderDashboardActivity
import com.ejemplo.laptimer.endurance.pilot.PilotSessionActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EnduranceLobbyActivity : AppCompatActivity() {

    val viewModel: EnduranceLobbyViewModel by viewModels()
    private lateinit var container: FrameLayout
    private var currentStep = -1

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settings = NetSettings(this)
        if (settings.sessionActive && settings.activeRoomCode.isNotBlank()) {
            val intent = if (settings.userRole == "CREW_CHIEF") {
                Intent(this, LeaderDashboardActivity::class.java)
            } else {
                Intent(this, PilotSessionActivity::class.java)
            }
            intent.putExtra("teamCode", settings.activeRoomCode)
            startActivity(intent)
            finish()
            return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0B0D10"))
            setPadding(dp(16), dp(10), dp(16), dp(10))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val back = TextView(this).apply {
            text = "‹  BACK"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#C6F432"))
            setPadding(0, dp(6), dp(16), dp(6))
            setOnClickListener {
                val step = viewModel.uiState.value.step
                if (step == LobbyUiState.STEP_SETUP) {
                    viewModel.goBackToRoleSelect()
                } else {
                    finish()
                }
            }
        }
        header.addView(back)
        val title = TextView(this).apply {
            text = "Endurance Lobby"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        header.addView(title)
        root.addView(header)

        container = FrameLayout(this).apply {
            id = android.view.View.generateViewId()
        }
        root.addView(container, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        setContentView(root)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val step = viewModel.uiState.value.step
                if (step == LobbyUiState.STEP_SETUP) {
                    viewModel.goBackToRoleSelect()
                } else {
                    finish()
                }
            }
        })

        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                state.error?.let {
                    viewModel.consumeError()
                }

                if (state.step != currentStep) {
                    currentStep = state.step
                    val fragment = when (state.step) {
                        LobbyUiState.STEP_ROLE_SELECT -> RoleSelectFragment()
                        LobbyUiState.STEP_SETUP -> if (state.mode == "CREATE") CreateTeamFragment() else JoinTeamFragment()
                        else -> WaitingRoomFragment()
                    }
                    supportFragmentManager.beginTransaction()
                        .replace(container.id, fragment)
                        .commit()
                }

                state.navigateToSession?.let { targetRole ->
                    val intent = if (targetRole == "LEADER" || targetRole == "CREW_CHIEF") {
                        Intent(this@EnduranceLobbyActivity, LeaderDashboardActivity::class.java)
                    } else {
                        Intent(this@EnduranceLobbyActivity, PilotSessionActivity::class.java)
                    }
                    intent.putExtra("teamCode", state.teamCode)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}
