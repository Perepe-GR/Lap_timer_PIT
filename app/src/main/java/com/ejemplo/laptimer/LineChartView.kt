package com.ejemplo.laptimer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import java.util.Locale

/** Gráfica de líneas simple (hasta varias series sobre un mismo eje X). */
class LineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var xs = DoubleArray(0)
    private var series: List<DoubleArray> = emptyList()
    private var colors = IntArray(0)
    private var zeroLine = false
    private var xUnit = " m"
    private val d = resources.displayMetrics.density
    private val axis = Paint().apply {
        color = 0xFF3A424C.toInt()
        strokeWidth = 1f * d
    }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * d
        strokeJoin = Paint.Join.ROUND
    }
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF9AA3AE.toInt()
        textSize = 11f * d
    }

    fun setData(
        xs: DoubleArray, series: List<DoubleArray>, colors: IntArray, zeroLine: Boolean,
        xUnit: String = " m"
    ) {
        this.xUnit = xUnit
        this.xs = xs
        this.series = series
        this.colors = colors
        this.zeroLine = zeroLine
        invalidate()
    }

    fun clear() {
        xs = DoubleArray(0)
        series = emptyList()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (xs.size < 2 || series.isEmpty()) return
        val left = 44f * d
        val right = width - 8f * d
        val top = 8f * d
        val bottom = height - 18f * d

        var mn = Double.MAX_VALUE
        var mx = -Double.MAX_VALUE
        for (s in series) for (v in s) {
            if (v < mn) mn = v
            if (v > mx) mx = v
        }
        if (zeroLine) {
            mn = minOf(mn, 0.0)
            mx = maxOf(mx, 0.0)
        }
        if (mx - mn < 1e-6) mx = mn + 1.0

        val x0 = xs[0]
        val x1 = xs[xs.size - 1]
        fun px(x: Double) = left + ((x - x0) / (x1 - x0)).toFloat() * (right - left)
        fun py(v: Double) = bottom - ((v - mn) / (mx - mn)).toFloat() * (bottom - top)

        canvas.drawLine(left, top, left, bottom, axis)
        canvas.drawLine(left, bottom, right, bottom, axis)
        if (zeroLine && mn < 0.0 && mx > 0.0) canvas.drawLine(left, py(0.0), right, py(0.0), axis)

        canvas.drawText(String.format(Locale.US, "%.1f", mx), 2f * d, top + txt.textSize, txt)
        canvas.drawText(String.format(Locale.US, "%.1f", mn), 2f * d, bottom, txt)
        txt.textAlign = Paint.Align.RIGHT
        canvas.drawText(String.format(Locale.US, "%.0f%s", x1, xUnit), right, height - 3f * d, txt)
        txt.textAlign = Paint.Align.LEFT

        for ((idx, s) in series.withIndex()) {
            val path = Path()
            for (i in xs.indices) {
                val X = px(xs[i])
                val Y = py(s[i])
                if (i == 0) path.moveTo(X, Y) else path.lineTo(X, Y)
            }
            line.color = colors[idx]
            canvas.drawPath(path, line)
        }
    }
}
