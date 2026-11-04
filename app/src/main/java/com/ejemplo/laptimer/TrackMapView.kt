package com.ejemplo.laptimer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import kotlin.math.hypot
import kotlin.math.min

/**
 * Tercera vista: el trazado del circuito dibujado en grande con tu posición en tiempo real.
 * El trazado sale de tu propia lap (la última lap normal completada; mientras no haya ninguna, de lo que llevas
 * recorrido) y se colorea por sectores con los colores F1 de la última lap. Dibuja las líneas de FINISH, sectores,
 * PIT IN / PIT OUT y la zona PADDOCK, todo en el mismo plano local en metros.
 */
class TrackMapView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    var circuit: Circuit? = null
        private set
    private var frame: LocalFrame? = null
    private val cur = ArrayList<Pt>()
    private var outline: List<Pt> = emptyList()
    private var kinds: List<MarkKind?> = emptyList()
    private var pos: Pt? = null
    private var lastInvalidate = 0L

    private val d = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }

    private val colNeutral = Color.parseColor("#6B7683")
    private val colPurple = Color.parseColor("#A855F7")
    private val colGreen get() = ThemeManager.getTheme(context).accent
    private val colYellow = Color.parseColor("#FACC15")

    fun setCircuit(c: Circuit?) {
        circuit = c
        frame = c?.let { LocalFrame(it.finish.lat, it.finish.lon) }
        cur.clear()
        outline = emptyList()
        kinds = emptyList()
        pos = null
        invalidate()
    }

    /** Posición actual; sector es el índice (0..) del sector en curso. Solo se traza si hay una lap en marcha. */
    fun onPosition(lat: Double, lon: Double, sector: Int, running: Boolean) {
        val f = frame ?: return
        val p = Pt(f.x(lon).toFloat(), f.y(lat).toFloat(), sector)
        pos = p
        if (running) {
            val last = cur.lastOrNull()
            if (last == null || hypot(p.x - last.x, p.y - last.y) > 2.5f) {
                cur.add(p)
                if (cur.size > 6000) cur.subList(0, 1000).clear()
            }
        }
        val now = System.currentTimeMillis()
        if (now - lastInvalidate > 100L) {
            lastInvalidate = now
            postInvalidate()
        }
    }

    /** Al cerrar una lap: la lap normal pasa a ser el trazado dibujado; una OUT LAP/IN LAP solo si aún no hay trazado. */
    fun onLapDone(kind: Int, marks: List<SectorMark?>) {
        if (cur.size >= 20 && (kind == 0 || outline.isEmpty())) {
            outline = ArrayList(cur)
            kinds = marks.map { it?.kind }
        }
        cur.clear()
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val c = circuit
        val f = frame
        if (c == null || f == null) return

        val xs = ArrayList<Float>()
        val ys = ArrayList<Float>()
        fun add(x: Float, y: Float) { xs.add(x); ys.add(y) }
        for (p in outline) add(p.x, p.y)
        for (p in cur) add(p.x, p.y)
        pos?.let { add(it.x, it.y) }
        val gates = ArrayList<Pair<String, FloatArray>>()
        gates.add("F" to gateEnds(c.finish, f))
        for ((i, s) in c.sectors.withIndex()) gates.add("S${i + 1}" to gateEnds(s, f))
        c.pitIn?.let { gates.add("IN" to gateEnds(it, f)) }
        c.pitOut?.let { gates.add("OUT" to gateEnds(it, f)) }
        for ((_, e) in gates) { add(e[0], e[1]); add(e[2], e[3]) }
        for (v in c.paddock) add(f.x(v.lon).toFloat(), f.y(v.lat).toFloat())
        if (xs.isEmpty()) return

        val padX = 28f * d
        val padTop = 78f * d
        val padBottom = 74f * d
        val w = width - 2 * padX
        val h = height - padTop - padBottom
        if (w <= 10f || h <= 10f) return
        val minX = xs.minOrNull() ?: 0f
        val maxX = xs.maxOrNull() ?: 0f
        val minY = ys.minOrNull() ?: 0f
        val maxY = ys.maxOrNull() ?: 0f
        val spanX = maxOf(maxX - minX, 30f)
        val spanY = maxOf(maxY - minY, 30f)
        val k = min(w / spanX, h / spanY)
        val offX = padX + (w - spanX * k) / 2f
        val offY = padTop + (h - spanY * k) / 2f
        fun sx(x: Float) = offX + (x - minX) * k
        fun sy(y: Float) = offY + (maxY - y) * k

        if (c.hasPaddock) {
            val path = Path()
            for ((i, v) in c.paddock.withIndex()) {
                val px = sx(f.x(v.lon).toFloat())
                val py = sy(f.y(v.lat).toFloat())
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(46, 250, 204, 21)
            canvas.drawPath(path, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f * d
            paint.color = Color.parseColor("#FACC15")
            canvas.drawPath(path, paint)
            text.textSize = 11f * d
            text.color = Color.parseColor("#FACC15")
            val v0 = c.paddock[0]
            canvas.drawText("PADDOCK", sx(f.x(v0.lon).toFloat()), sy(f.y(v0.lat).toFloat()) - 6f * d, text)
        }

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 7f * d
        if (outline.size >= 2) {
            for (i in 1 until outline.size) {
                val a = outline[i - 1]
                val b = outline[i]
                paint.color = kindColor(kinds.getOrNull(b.s), colPurple, colGreen, colYellow, colNeutral)
                canvas.drawLine(sx(a.x), sy(a.y), sx(b.x), sy(b.y), paint)
            }
        }
        if (cur.size >= 2) {
            paint.strokeWidth = (if (outline.size >= 2) 2.5f else 5f) * d
            paint.color = if (outline.size >= 2) Color.WHITE else Color.parseColor("#C9D1DA")
            for (i in 1 until cur.size) {
                canvas.drawLine(sx(cur[i - 1].x), sy(cur[i - 1].y), sx(cur[i].x), sy(cur[i].y), paint)
            }
        }
        if (outline.size < 2 && cur.size < 2) {
            text.textSize = 14f * d
            text.color = Color.parseColor("#9AA3AE")
            canvas.drawText("El trazado aparece al completar una lap", width / 2f, height / 2f, text)
        }

        for ((name, e) in gates) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = (if (name == "F") 4f else 3f) * d
            val col = when (name) {
                "F" -> Color.WHITE
                "IN", "OUT" -> Color.parseColor("#38BDF8")
                else -> Color.parseColor("#F5A524")
            }
            paint.color = col
            canvas.drawLine(sx(e[0]), sy(e[1]), sx(e[2]), sy(e[3]), paint)
            text.textSize = 11f * d
            text.color = col
            canvas.drawText(name, sx(e[0]), sy(e[1]) - 5f * d, text)
        }

        pos?.let {
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#0B0D10")
            canvas.drawCircle(sx(it.x), sy(it.y), 13f * d, paint)
            paint.color = Color.parseColor("#C6F432")
            canvas.drawCircle(sx(it.x), sy(it.y), 9f * d, paint)
        }
    }
}
