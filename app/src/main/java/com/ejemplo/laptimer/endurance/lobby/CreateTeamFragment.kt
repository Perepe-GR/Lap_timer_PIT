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
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.ejemplo.laptimer.CircuitStore
import com.ejemplo.laptimer.util.ValidationUtils

class CreateTeamFragment : Fragment() {

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
        title.text = "Create Endurance Team"
        title.textSize = 22f
        title.typeface = Typeface.DEFAULT_BOLD
        title.setTextColor(Color.WHITE)
        title.setPadding(0, 0, 0, dp(16))
        root.addView(title)

        val etTeamName = inputField("Team Name (6-15 chars)", "Empuria", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS, 15)
        val etCode = inputField("Team PIN / Code (4-6 digits)", "1234", InputType.TYPE_CLASS_NUMBER, 6)
        root.addView(etTeamName)
        val lpCode = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lpCode.topMargin = dp(12)
        root.addView(etCode, lpCode)

        val lblCircuit = TextView(requireContext()).apply {
            text = "Circuit"
            textSize = 14f
            setTextColor(Color.parseColor("#9AA3AE"))
            setPadding(0, dp(4), 0, dp(4))
        }
        val lpLbl = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lpLbl.topMargin = dp(16)
        root.addView(lblCircuit, lpLbl)

        val circuitContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            background = roundBg(Color.parseColor("#12161B"), 10)
            setPadding(dp(12), dp(4), dp(12), dp(4))
            gravity = Gravity.CENTER_VERTICAL
        }

        val store = CircuitStore(requireContext())
        val circuits = store.list().map { it.name }.toMutableList()
        if (circuits.isEmpty()) circuits.add("Karting Empuriabrava")
        val spCircuit = Spinner(requireContext()).apply {
            background = null
            setPadding(0, 0, 0, 0)
        }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, circuits)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCircuit.adapter = adapter
        circuitContainer.addView(spCircuit, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val arrow = TextView(requireContext()).apply {
            text = "▼"
            textSize = 12f
            setTextColor(Color.parseColor("#9AA3AE"))
            setPadding(dp(8), 0, dp(8), 0)
        }
        circuitContainer.addView(arrow)

        val lpCircuit = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lpCircuit.topMargin = dp(4)
        root.addView(circuitContainer, lpCircuit)

        val btnCreate = TextView(requireContext())
        btnCreate.text = "CREATE TEAM"
        btnCreate.gravity = Gravity.CENTER
        btnCreate.textSize = 16f
        btnCreate.typeface = Typeface.DEFAULT_BOLD
        btnCreate.setTextColor(Color.parseColor("#0B0D10"))
        btnCreate.background = roundBg(Color.parseColor("#C6F432"), 14)
        btnCreate.setOnClickListener {
            val name = etTeamName.text.toString().trim()
            val code = etCode.text.toString().trim()
            val circuitName = circuits.getOrNull(spCircuit.selectedItemPosition) ?: "Circuit 1"

            var valid = true
            val nameRes = ValidationUtils.validateIdentifier(name)
            if (nameRes.isFailure) {
                etTeamName.error = nameRes.exceptionOrNull()?.message
                valid = false
            }
            val pinRes = ValidationUtils.validatePin(code)
            if (pinRes.isFailure) {
                etCode.error = pinRes.exceptionOrNull()?.message
                valid = false
            }
            if (!valid) return@setOnClickListener

            (activity as? EnduranceLobbyActivity)?.viewModel?.createTeam(name, code, circuitName, "")
        }
        val lpBtn = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56))
        lpBtn.topMargin = dp(24)
        root.addView(btnCreate, lpBtn)

        return root
    }
}
