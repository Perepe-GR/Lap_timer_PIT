package com.ejemplo.laptimer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import java.util.Arrays
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Custom View for rendering motorsport Friction Circle / G-G Diagram (Lateral vs Longitudinal Acceleration)
 * with a directional session peak G-force dotted polygon envelope.
 */
class GForceCircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#334155")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#475569")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#94A3B8")
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C6F432")
        style = Paint.Style.FILL
    }

    private val dotGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#40C6F432")
        style = Paint.Style.FILL
    }

    private val peakRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EF4444")
        style = Paint.Style.STROKE
        strokeWidth = 4f
        pathEffect = DashPathEffect(floatArrayOf(12f, 12f), 0f)
    }

    @Volatile
    var gLat = 0f

    @Volatile
    var gLong = 0f

    var maxG = 1.5f

    private val numSectors = 36
    private val sectorMaxGs = FloatArray(numSectors) { 0f }

    fun updateGForce(latG: Float, longG: Float) {
        gLat = latG
        gLong = longG

        val r = sqrt(latG * latG + longG * longG)
        if (r > 0.05f && r < maxG * 2f) {
            val angle = atan2(longG, latG)
            var sectorIdx = ((angle + Math.PI.toFloat()) / (2f * Math.PI.toFloat()) * numSectors).toInt()
            sectorIdx = sectorIdx.coerceIn(0, numSectors - 1)
            if (r > sectorMaxGs[sectorIdx]) {
                sectorMaxGs[sectorIdx] = r.coerceAtMost(maxG * 1.5f)
            }
        }
        postInvalidateOnAnimation()
    }

    fun resetPeak() {
        Arrays.fill(sectorMaxGs, 0f)
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = (min(width, height) / 2f) * 0.85f

        // Draw concentric G-rings (0.5G, 1.0G, 1.5G)
        val ring1 = maxRadius * (0.5f / maxG)
        val ring2 = maxRadius * (1.0f / maxG)
        val ring3 = maxRadius

        canvas.drawCircle(cx, cy, ring1, ringPaint)
        canvas.drawCircle(cx, cy, ring2, ringPaint)
        canvas.drawCircle(cx, cy, ring3, ringPaint)

        // Draw Directional Peak Dotted Polygon Envelope
        val path = Path()
        var hasPoints = false
        for (i in 0 until numSectors) {
            val maxR = sectorMaxGs[i]
            if (maxR > 0f) {
                val theta = (i.toFloat() / numSectors) * 2f * Math.PI.toFloat() - Math.PI.toFloat()
                val latDir = cos(theta)
                val longDir = sin(theta)
                val px = cx + (latDir * maxR / maxG) * maxRadius
                val py = cy - (longDir * maxR / maxG) * maxRadius

                if (!hasPoints) {
                    path.moveTo(px, py)
                    hasPoints = true
                } else {
                    path.lineTo(px, py)
                }
            }
        }
        if (hasPoints) {
            path.close()
            canvas.drawPath(path, peakRingPaint)
        }

        // Draw Crosshair Axes
        canvas.drawLine(cx - maxRadius, cy, cx + maxRadius, cy, axisPaint)
        canvas.drawLine(cx, cy - maxRadius, cx, cy + maxRadius, axisPaint)

        // Ring Labels
        canvas.drawText("0.5G", cx + ring1 + 10f, cy - 6f, labelPaint)
        canvas.drawText("1.0G", cx + ring2 + 10f, cy - 6f, labelPaint)
        canvas.drawText("1.5G", cx + ring3 - 30f, cy - 6f, labelPaint)

        // Calculate G-force dot position
        // gLat (+ = Right, - = Left), gLong (+ = Accel [Up], - = Brake [Down])
        val px = cx + (gLat / maxG) * maxRadius
        val py = cy - (gLong / maxG) * maxRadius

        // Draw Glow and Dot
        canvas.drawCircle(px, py, 24f, dotGlowPaint)
        canvas.drawCircle(px, py, 12f, dotPaint)
    }
}
