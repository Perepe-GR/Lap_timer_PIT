package com.ejemplo.laptimer.endurance.lobby

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.ejemplo.laptimer.endurance.sync.PendingPilot

fun dp(context: Context, v: Int): Int = (v * context.resources.displayMetrics.density).toInt()

fun roundBg(color: Int, radiusDp: Int, context: Context): GradientDrawable {
    val g = GradientDrawable()
    g.setColor(color)
    g.cornerRadius = dp(context, radiusDp).toFloat()
    return g
}

fun createPendingRow(
    context: Context,
    pId: String,
    p: PendingPilot,
    teamCode: String,
    viewModel: EnduranceLobbyViewModel
): LinearLayout {
    val row = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = roundBg(Color.parseColor("#12161B"), 10, context)
        setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8))
    }
    val tvName = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    val tvTitle = TextView(context).apply {
        text = p.name.ifBlank { pId }
        textSize = 16f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.WHITE)
    }
    val tvStatus = TextView(context).apply {
        text = "Status: ${p.status.ifBlank { "PENDING" }} · ID: ${p.id.ifBlank { pId }}"
        textSize = 12f
        setTextColor(Color.parseColor("#9AA3AE"))
    }
    tvName.addView(tvTitle)
    tvName.addView(tvStatus)
    row.addView(tvName, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

    val btnAccept = TextView(context).apply {
        text = "ACCEPT"
        gravity = Gravity.CENTER
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.parseColor("#0B0D10"))
        background = roundBg(Color.parseColor("#C6F432"), 8, context)
        setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8))
        setOnClickListener { viewModel.acceptPilot(teamCode, pId, p.name, p.uid) }
    }
    row.addView(btnAccept)

    val btnReject = TextView(context).apply {
        text = "REJECT"
        gravity = Gravity.CENTER
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.WHITE)
        background = roundBg(Color.parseColor("#EF4444"), 8, context)
        setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            leftMargin = dp(context, 8)
        }
        setOnClickListener { viewModel.rejectPilot(teamCode, p.uid.ifBlank { pId }) }
    }
    row.addView(btnReject)
    return row
}
