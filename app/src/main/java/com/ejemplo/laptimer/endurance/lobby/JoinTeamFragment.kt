package com.ejemplo.laptimer.endurance.lobby

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.ejemplo.laptimer.util.ValidationUtils

class JoinTeamFragment : Fragment() {

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun roundBg(color: Int, radiusDp: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(color)
        g.cornerRadius = dp(radiusDp).toFloat()
        return g
    }

    private fun inputField(hintText: String, defaultVal: String = "", inputType: Int = InputType.TYPE_CLASS_TEXT, maxLength: Int = 30): EditText {
        val e = EditText(requireContext())
        e.hint = hintText
        e.setText(defaultVal)
        e.inputType = inputType
        e.filters = arrayOf(InputFilter.LengthFilter(maxLength))
        e.setHintTextColor(Color.parseColor("#9AA3AE"))
        e.setTextColor(Color.WHITE)
        e.textSize = 16f
        e.background = roundBg(Color.parseColor("#12161B"), 10)
        e.setPadding(dp(16), dp(12), dp(16), dp(12))
        return e
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = LinearLayout(requireContext())
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#0B0D10"))
        root.setPadding(dp(20), dp(16), dp(20), dp(16))

        val title = TextView(requireContext())
        title.text = "Join Endurance Team"
        title.textSize = 22f
        title.typeface = Typeface.DEFAULT_BOLD
        title.setTextColor(Color.WHITE)
        title.setPadding(0, 0, 0, dp(16))
        root.addView(title)

        val etCode = inputField("Team Code (6-15 chars)", "", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS, 15)
        val etPin = inputField("PIN (4-6 digits)", "", InputType.TYPE_CLASS_NUMBER, 6)
        val etPilotName = inputField("Driver Name (6-15 chars)", "", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS, 15)

        root.addView(etCode)
        val lpPin = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lpPin.topMargin = dp(12)
        root.addView(etPin, lpPin)
        val lpPilot = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lpPilot.topMargin = dp(12)
        root.addView(etPilotName, lpPilot)

        val btnJoin = TextView(requireContext())
        btnJoin.text = "JOIN TEAM"
        btnJoin.gravity = Gravity.CENTER
        btnJoin.textSize = 16f
        btnJoin.typeface = Typeface.DEFAULT_BOLD
        btnJoin.setTextColor(Color.parseColor("#0B0D10"))
        btnJoin.background = roundBg(Color.parseColor("#C6F432"), 14)
        btnJoin.setOnClickListener {
            val code = etCode.text.toString().trim()
            val pin = etPin.text.toString().trim()
            val name = etPilotName.text.toString().trim()

            var valid = true
            if (!ValidationUtils.validateIdentifier(code)) {
                etCode.error = "Team Code must be 6-15 alphanumeric characters."
                valid = false
            }
            if (!ValidationUtils.validatePin(pin)) {
                etPin.error = "PIN must be strictly numeric (4-6 digits)."
                valid = false
            }
            if (!ValidationUtils.validateIdentifier(name)) {
                etPilotName.error = "Driver Name must be 6-15 alphanumeric characters with spaces."
                valid = false
            }
            if (!valid) return@setOnClickListener

            val pilotId = "pilot-" + System.currentTimeMillis()
            (activity as? EnduranceLobbyActivity)?.viewModel?.joinTeam(code, pin, pilotId, name)
        }
        val lpBtn = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56))
        lpBtn.topMargin = dp(20)
        root.addView(btnJoin, lpBtn)

        return root
    }
}
