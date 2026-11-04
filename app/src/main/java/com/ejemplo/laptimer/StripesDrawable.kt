package com.ejemplo.laptimer

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable

/**
 * Fondo de "pista deslizante": amarillo con 4 rayas rojas verticales
 * (9 franjas iguales: amarillo, rojo, amarillo, rojo... amarillo), con esquinas redondeadas.
 */
class StripesDrawable(
    private val base: Int,
    private val stripe: Int,
    private val radius: Float
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    override fun draw(canvas: Canvas) {
        val b = bounds
        val l = b.left.toFloat()
        val t = b.top.toFloat()
        val r = b.right.toFloat()
        val bt = b.bottom.toFloat()
        path.reset()
        path.addRoundRect(RectF(l, t, r, bt), radius, radius, Path.Direction.CW)
        val saved = canvas.save()
        canvas.clipPath(path)
        paint.color = base
        canvas.drawRect(l, t, r, bt, paint)
        val seg = (r - l) / 9f
        paint.color = stripe
        for (i in 0 until 4) {
            val x = l + seg * (1 + 2 * i)
            canvas.drawRect(x, t, x + seg, bt, paint)
        }
        canvas.restoreToCount(saved)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
