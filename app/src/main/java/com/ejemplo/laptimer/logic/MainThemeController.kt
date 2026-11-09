package com.ejemplo.laptimer.logic

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.ejemplo.laptimer.MainActivity
import com.ejemplo.laptimer.R
import com.ejemplo.laptimer.StripesDrawable
import com.ejemplo.laptimer.ThemeManager
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan

fun MainActivity.applyThemeStyles() {
    val theme = ThemeManager.getTheme(this)
    val colAccent = theme.accent
    val colBg = theme.bg

    findViewById<View>(R.id.mainRoot)?.setBackgroundColor(colBg)
    findViewById<View>(R.id.txtBest)?.let { (it as? TextView)?.setTextColor(colAccent) }
    bindings.progLaps.progressTintList = android.content.res.ColorStateList.valueOf(colAccent)

    val btnDrawable = GradientDrawable().apply {
        setColor(colAccent)
        cornerRadius = 16f * resources.displayMetrics.density
    }
    bindings.btnSwapConfirm.background = btnDrawable
    bindings.btnSwapConfirm.setTextColor(colBg)
    bindings.btnPitSwapConfirm.background = btnDrawable
    bindings.btnPitSwapConfirm.setTextColor(colBg)
    bindings.btnSave.background = btnDrawable
    bindings.btnSave.setTextColor(colBg)

    bindings.trackMap.invalidate()
}

fun MainActivity.mmss(ms: Long): String {
    val t = ms / 1000
    return String.format(java.util.Locale.US, "%d:%02d", t / 60, t % 60)
}

fun MainActivity.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

fun MainActivity.toast(msg: String) { /* disabled per UI contract */ }

fun MainActivity.solid(fill: Int, fg: Int, text: String) {
    val g = GradientDrawable()
    g.setColor(fill)
    g.cornerRadius = dp(14).toFloat()
    bindings.bannerPit.background = g
    bindings.bannerPit.setTextColor(fg)
    bindings.bannerPit.text = text

    val g2 = GradientDrawable()
    g2.setColor(fill)
    g2.cornerRadius = dp(14).toFloat()
    bindings.pitBannerPit.background = g2
    bindings.pitBannerPit.setTextColor(fg)
    bindings.pitBannerPit.text = text
    bindings.pitBannerPit.visibility = if (text.isNotBlank()) View.VISIBLE else View.GONE
}

fun MainActivity.applyMsgStyle(bg: String, text: String) {
    val b = bg.lowercase()
    when {
        b == "verde" || b == "green" -> solid(Color.parseColor("#22C55E"), Color.parseColor("#0B0D10"), text)
        b == "rojo" || b == "red" -> solid(Color.parseColor("#EF4444"), Color.WHITE, text)
        b == "azul" || b == "blue" -> solid(Color.parseColor("#2563EB"), Color.WHITE, text)
        b == "blanco" || b == "white" -> solid(Color.parseColor("#F2F4F7"), Color.parseColor("#141413"), text)
        b == "cuadros" || b == "rayas" || b == "finish" || b == "checkered" -> {
            val bgDrawable1 = StripesDrawable(Color.parseColor("#FACC15"), Color.parseColor("#EF4444"), dp(14).toFloat())
            val bgDrawable2 = StripesDrawable(Color.parseColor("#FACC15"), Color.parseColor("#EF4444"), dp(14).toFloat())
            bindings.bannerPit.background = bgDrawable1
            bindings.bannerPit.setTextColor(Color.WHITE)
            bindings.pitBannerPit.background = bgDrawable2
            bindings.pitBannerPit.setTextColor(Color.WHITE)

            val sp = SpannableString(" $text ")
            sp.setSpan(
                BackgroundColorSpan(Color.parseColor("#E6141413")),
                0, sp.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            bindings.bannerPit.text = sp
            bindings.pitBannerPit.text = sp
            bindings.pitBannerPit.visibility = if (text.isNotBlank()) View.VISIBLE else View.GONE
        }
        else -> solid(Color.parseColor("#FACC15"), Color.parseColor("#141413"), text)
    }
}
