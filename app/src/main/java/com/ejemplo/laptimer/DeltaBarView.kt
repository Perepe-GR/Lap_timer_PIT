package com.ejemplo.laptimer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Barra del delta. ratio = delta / (2 % de la mejor vuelta), limitado a -1..+1.
 * Negativo (más rápido) rellena a la izquierda en verde; positivo, a la derecha en rojo.
 */
class DeltaBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1E242B.toInt() }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF9AA3AE.toInt() }
    private var ratio = 0f

    fun setRatio(r: Float) {
        ratio = r.coerceIn(-1f, 1f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val d = resources.displayMetrics.density
        val w = width.toFloat()
        val h = height.toFloat()
        val barH = 16f * d
        val top = (h - barH) / 2f
        val bottom = top + barH
        val r = barH / 2f
        val half = w / 2f

        canvas.drawRoundRect(0f, top, w, bottom, r, r, track)

        if (ratio < 0f) {
            fill.color = ThemeManager.getTheme(context).accent
            canvas.drawRoundRect(half + half * ratio, top, half, bottom, r, r, fill)
        } else if (ratio > 0f) {
            fill.color = ThemeManager.getTheme(context).alert
            canvas.drawRoundRect(half, top, half + half * ratio, bottom, r, r, fill)
        }

        canvas.drawRect(half - 1.5f * d, 0f, half + 1.5f * d, h, tick)
    }
}
