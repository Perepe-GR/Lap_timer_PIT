package com.ejemplo.laptimer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/** Trazado de una vuelta coloreado por velocidad (rojo = lento, verde = rápido). */
class TraceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var xs = DoubleArray(0)
    private var ys = DoubleArray(0)
    private var vs = DoubleArray(0)
    private val d = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 4f * d
        strokeCap = Paint.Cap.ROUND
    }

    fun setData(x: DoubleArray, y: DoubleArray, v: DoubleArray) {
        xs = x
        ys = y
        vs = v
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val n = xs.size
        if (n < 2) return
        var minX = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE
        var minV = Double.MAX_VALUE
        var maxV = -Double.MAX_VALUE
        for (i in 0 until n) {
            if (xs[i] < minX) minX = xs[i]
            if (xs[i] > maxX) maxX = xs[i]
            if (ys[i] < minY) minY = ys[i]
            if (ys[i] > maxY) maxY = ys[i]
            if (vs[i] < minV) minV = vs[i]
            if (vs[i] > maxV) maxV = vs[i]
        }
        val m = 16f * d
        val w = (maxX - minX).coerceAtLeast(1.0)
        val h = (maxY - minY).coerceAtLeast(1.0)
        val scale = minOf((width - 2 * m) / w, (height - 2 * m) / h).toFloat()
        val offX = (width - (w * scale).toFloat()) / 2f
        val offY = (height - (h * scale).toFloat()) / 2f
        fun sx(x: Double) = offX + ((x - minX) * scale).toFloat()
        fun sy(y: Double) = height - (offY + ((y - minY) * scale).toFloat())

        val range = (maxV - minV).coerceAtLeast(1.0)
        val hsv = FloatArray(3)
        for (i in 0 until n - 1) {
            val t = ((vs[i] - minV) / range).toFloat()
            hsv[0] = 120f * t
            hsv[1] = 0.9f
            hsv[2] = 1f
            paint.color = Color.HSVToColor(hsv)
            canvas.drawLine(sx(xs[i]), sy(ys[i]), sx(xs[i + 1]), sy(ys[i + 1]), paint)
        }
    }
}
