package com.ejemplo.laptimer.endurance.lobby

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ejemplo.laptimer.NetSettings
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.net.URLEncoder

class WaitingRoomFragment : Fragment() {

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun roundBg(color: Int, radiusDp: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(color)
        g.cornerRadius = dp(radiusDp).toFloat()
        return g
    }

    private lateinit var txtInfo: TextView
    private lateinit var txtPin: TextView
    private lateinit var pilotList: ListView
    private lateinit var btnStart: TextView
    private lateinit var btnCopyLink: TextView
    private lateinit var btnShareLink: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = LinearLayout(requireContext())
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#0B0D10"))
        root.setPadding(dp(20), dp(16), dp(20), dp(16))

        txtInfo = TextView(requireContext())
        txtInfo.textSize = 20f
        txtInfo.typeface = Typeface.DEFAULT_BOLD
        txtInfo.setTextColor(Color.WHITE)
        root.addView(txtInfo)

        txtPin = TextView(requireContext())
        txtPin.textSize = 26f
        txtPin.typeface = Typeface.DEFAULT_BOLD
        txtPin.setTextColor(Color.parseColor("#C6F432"))
        txtPin.setPadding(0, dp(6), 0, dp(12))
        root.addView(txtPin)

        // Leader Pit-Wall Management Link Actions
        btnCopyLink = TextView(requireContext()).apply {
            text = "COPY PIT-WALL LINK"
            gravity = Gravity.CENTER
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = roundBg(Color.parseColor("#12161B"), 10)
            setPadding(0, dp(10), 0, dp(10))
            visibility = View.GONE
            setOnClickListener {
                val url = getPitWallUrl()
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Pit-Wall Link", url)
                clipboard.setPrimaryClip(clip)
            }
        }
        val lpCopy = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lpCopy.bottomMargin = dp(8)
        root.addView(btnCopyLink, lpCopy)

        btnShareLink = TextView(requireContext()).apply {
            text = "SHARE PIT-WALL LINK"
            gravity = Gravity.CENTER
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = roundBg(Color.parseColor("#12161B"), 10)
            setPadding(0, dp(10), 0, dp(10))
            visibility = View.GONE
            setOnClickListener {
                val url = getPitWallUrl()
                val i = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, url)
                }
                startActivity(Intent.createChooser(i, "Share Pit-Wall Link"))
            }
        }
        val lpShare = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lpShare.bottomMargin = dp(12)
        root.addView(btnShareLink, lpShare)

        val lbl = TextView(requireContext())
        lbl.text = "Connected Drivers / Pilots:"
        lbl.textSize = 14f
        lbl.setTextColor(Color.parseColor("#9AA3AE"))
        root.addView(lbl)

        pilotList = ListView(requireContext())
        root.addView(pilotList, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        btnStart = TextView(requireContext())
        btnStart.text = "START ENDURANCE SESSION"
        btnStart.gravity = Gravity.CENTER
        btnStart.textSize = 16f
        btnStart.typeface = Typeface.DEFAULT_BOLD
        btnStart.setTextColor(Color.parseColor("#0B0D10"))
        btnStart.background = roundBg(Color.parseColor("#C6F432"), 14)
        btnStart.visibility = View.GONE
        btnStart.setOnClickListener {
            (activity as? EnduranceLobbyActivity)?.viewModel?.startSession()
        }
        val lpBtn = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56))
        lpBtn.topMargin = dp(16)
        root.addView(btnStart, lpBtn)

        observeState()
        return root
    }

    private fun getPitWallUrl(): String {
        val vm = (activity as? EnduranceLobbyActivity)?.viewModel ?: return ""
        val state = vm.uiState.value
        val settings = NetSettings(requireContext())
        val panel = settings.panelUrl.ifBlank { "https://user.github.io/pit-panel/" }
        val db = settings.dbUrl.ifBlank { "https://laptimer-default-rtdb.firebaseio.com" }
        val sep = if (panel.contains("?")) "&" else "?"
        return panel + sep + "db=" + URLEncoder.encode(db, "UTF-8") + "&room=" + state.teamCode
    }

    private fun observeState() {
        val vm = (activity as? EnduranceLobbyActivity)?.viewModel ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            vm.uiState.collectLatest { state ->
                txtInfo.text = "Team: ${state.teamName} (${state.teamCode}) · Circuit: ${state.circuitId}"
                if (state.role == "LEADER") {
                    txtPin.text = "Verbal PIN: ${state.pin}"
                    txtPin.visibility = View.VISIBLE
                    btnStart.visibility = View.VISIBLE
                    btnCopyLink.visibility = View.VISIBLE
                    btnShareLink.visibility = View.VISIBLE
                } else {
                    txtPin.visibility = View.GONE
                    btnStart.visibility = View.GONE
                    btnCopyLink.visibility = View.GONE
                    btnShareLink.visibility = View.GONE
                }

                val lines = state.pilots.values.map { p ->
                    "${p.info.name}  ·  Status: ${p.info.status}"
                }
                pilotList.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, lines)
            }
        }
    }
}
