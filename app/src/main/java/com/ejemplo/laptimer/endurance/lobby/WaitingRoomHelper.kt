package com.ejemplo.laptimer.endurance.lobby

import android.content.Context
import android.graphics.drawable.GradientDrawable

fun dp(context: Context, v: Int): Int = (v * context.resources.displayMetrics.density).toInt()

fun roundBg(color: Int, radiusDp: Int, context: Context): GradientDrawable {
    val g = GradientDrawable()
    g.setColor(color)
    g.cornerRadius = dp(context, radiusDp).toFloat()
    return g
}
