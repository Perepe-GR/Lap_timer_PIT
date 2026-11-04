package com.ejemplo.laptimer.endurance.lobby

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class RoleSelectFragment : Fragment() {

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun roundBg(color: Int, radiusDp: Int, strokeColor: Int = 0, strokeDp: Int = 0): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(color)
        g.cornerRadius = dp(radiusDp).toFloat()
        if (strokeDp > 0) g.setStroke(dp(strokeDp), strokeColor)
        return g
    }

    private fun roleCard(title: String, desc: String, primary: Boolean, action: () -> Unit): LinearLayout {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            background = roundBg(
                Color.parseColor("#12161B"),
                16,
                if (primary) Color.parseColor("#C6F432") else Color.parseColor("#1E242B"),
                2
            )
            setPadding(dp(20), dp(16), dp(20), dp(16))
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }

        val t = TextView(requireContext()).apply {
            text = title
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (primary) Color.parseColor("#C6F432") else Color.WHITE)
        }
        card.addView(t)

        val d = TextView(requireContext()).apply {
            text = desc
            textSize = 13f
            setTextColor(Color.parseColor("#9AA3AE"))
            setPadding(0, dp(4), 0, 0)
        }
        card.addView(d)

        return card
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0B0D10"))
            setPadding(dp(24), dp(20), dp(24), dp(20))
        }

        val title = TextView(requireContext()).apply {
            text = "Select Endurance Role"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        root.addView(title)

        val sub = TextView(requireContext()).apply {
            text = "Strict 2-role management: 1 Crew Chief per team session."
            textSize = 13f
            setTextColor(Color.parseColor("#9AA3AE"))
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(20))
        }
        root.addView(sub)

        val cardChief = roleCard(
            "Crew Chief (Team Leader)",
            "Creates the team session, manages stint strategy, driver swaps, and the Pit-Wall panel.",
            true
        ) {
            (activity as? EnduranceLobbyActivity)?.viewModel?.selectRole("CREW_CHIEF")
        }
        val lpChief = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        root.addView(cardChief, lpChief)

        val cardPilot = roleCard(
            "Driver / Pilot",
            "Joins the team session, receives stint targets, and syncs live lap timing telemetry.",
            false
        ) {
            (activity as? EnduranceLobbyActivity)?.viewModel?.selectRole("PILOT")
        }
        val lpPilot = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(14)
        }
        root.addView(cardPilot, lpPilot)

        return root
    }
}
