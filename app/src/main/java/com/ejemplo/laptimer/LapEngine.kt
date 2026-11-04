package com.ejemplo.laptimer

import com.ejemplo.laptimer.engine.DeltaCalculator
import com.ejemplo.laptimer.engine.GapHandler
import com.ejemplo.laptimer.engine.GpsFilter
import com.ejemplo.laptimer.engine.LapDetector
import com.ejemplo.laptimer.engine.SectorCalculator

/** Motor de cronometraje: detecta meta/sectores, traza de referencia y delta en vivo. */
class LapEngine(private val circuit: Circuit, initial: Persisted? = null) {

    companion object {
        const val MIN_LAP_MS = 10_000L
        const val MAX_GAP_MS = 1_000L
    }

    private val detector = LapDetector(circuit)
    private val gpsFilter = GpsFilter()
    private val deltaCalculator = DeltaCalculator()
    private val gapHandler = GapHandler()
    private val n = detector.n

    private var hasPrev = false
    private var prevX = 0.0
    private var prevY = 0.0
    private var prevT = 0L

    private var lapStart: Double? = null
    private var lapInvalid = false
    private var expected = 0
    private var splitStart = 0.0
    private val currentSplits = ArrayList<Long>()
    private val trace = ArrayList<RefSample>()

    private var lapsCompleted = 0
    private var lastLapMs: Long? = null
    private var bestLapMs: Long? = null
    private val histBest = LongArray(n) { Long.MAX_VALUE }
    private val sessBest = LongArray(n) { Long.MAX_VALUE }
    private var marks = arrayOfNulls<SectorMark>(n)
    private var prevMarks = arrayOfNulls<SectorMark>(n)
    private var reference: ReferenceLap? = null
    private var refSamples: List<RefSample>? = null
    private var inPit = false
    private var special = 0
    private var currentLapHasGapWarning = false
    private var currentLapHasGapInvalid = false

    private val sectorCalculator = SectorCalculator(n, histBest, sessBest)

    var onLap: ((LapResult) -> Unit)? = null
    @Volatile var outlapCounts = false
    @Volatile var inlapCounts = false
    var onBoundary: (() -> Unit)? = null
    var onPitIn: (() -> Unit)? = null
    var onPitOut: (() -> Unit)? = null

    init {
        if (initial != null) {
            val s = initial.stats
            if (s.bestSectors.size == n) for (i in 0 until n) histBest[i] = s.bestSectors[i]
            bestLapMs = s.bestLapMs
            val r = initial.ref
            if (r != null && r.size >= 2) { refSamples = r; reference = ReferenceLap(r) }
        }
    }

    @Synchronized fun isRunning(): Boolean = lapStart != null
    @Synchronized fun setInPit(v: Boolean) { inPit = v }

    @Synchronized
    fun invalidateCurrentLap() {
        if (lapStart != null) lapInvalid = true
    }

    @Synchronized
    fun resetSession() {
        special = 0; lapStart = null; lapInvalid = false; expected = 0; lapsCompleted = 0; lastLapMs = null
        for (i in 0 until n) sessBest[i] = Long.MAX_VALUE
        marks = arrayOfNulls(n); prevMarks = arrayOfNulls(n)
        hasPrev = false; inPit = false
        currentLapHasGapWarning = false; currentLapHasGapInvalid = false
        trace.clear(); currentSplits.clear()
        reference?.reset(); gpsFilter.reset(); deltaCalculator.reset(); gapHandler.reset()
    }

    @Synchronized
    fun persistData(): PersistData = PersistData(bestLapMs, LongArray(n) { minOf(histBest[it], sessBest[it]) }, refSamples)

    @Synchronized
    fun onFix(lat: Double, lon: Double, speedKmh: Double, rawTimeMs: Long): EngineState {
        val rawT = gpsFilter.filterTime(rawTimeMs)
        val t = gapHandler.adjust(rawT)
        val gapState = gapHandler.state()
        if (gapState == GapHandler.GapState.INVALID) {
            invalidateCurrentLap()
            currentLapHasGapInvalid = true
        } else if (gapState == GapHandler.GapState.WARNING) {
            currentLapHasGapWarning = true
        }

        val x = detector.frame.x(lon)
        val y = detector.frame.y(lat)

        if (hasPrev && t > prevT && t - prevT <= MAX_GAP_MS) {
            processSegment(prevX, prevY, prevT.toDouble(), x, y, t.toDouble())
        }
        hasPrev = true; prevX = x; prevY = y; prevT = t

        var current: Long? = null
        var delta: Double? = null
        val start = lapStart
        if (start != null) {
            val rel = t - start
            current = Math.round(rel)
            if (rel > 0.0) trace.add(RefSample(Math.round(rel), x, y))
            if (special == 0) {
                delta = deltaCalculator.calculate(rel, x, y, reference)
            }
        }

        val shown = if (marks.any { it != null }) marks else prevMarks
        return EngineState(
            running = lapStart != null, lapNumber = lapsCompleted + 1, sector = minOf(expected + 1, n),
            sectorCount = n, currentLapMs = current, lastLapMs = lastLapMs, bestLapMs = bestLapMs,
            optimalMs = sectorCalculator.optimal(), deltaSec = delta, speedKmh = speedKmh, gpsTimeMs = t,
            marks = shown.toList(), isInvalid = lapInvalid, gapState = gapState
        )
    }

    private fun processSegment(x1: Double, y1: Double, t1: Double, x2: Double, y2: Double, t2: Double) {
        detector.checkPitCrossing(inPit, x1, y1, x2, y2)?.let { (entering, f) ->
            val tc = t1 + (t2 - t1) * f
            val xc = x1 + (x2 - x1) * f
            val yc = y1 + (y2 - y1) * f
            if (entering) { inPit = true; endInLap(tc, xc, yc); onPitIn?.invoke() }
            else { inPit = false; startOutLap(tc, xc, yc); onPitOut?.invoke() }
        }

        detector.checkGateCrossing(lapStart != null, expected, x1, y1, x2, y2)?.let { (gateIdx, f) ->
            val tc = t1 + (t2 - t1) * f
            val xc = x1 + (x2 - x1) * f
            val yc = y1 + (y2 - y1) * f
            if (gateIdx >= 0) onGate(tc, xc, yc) else startLap(tc, xc, yc)
        }
    }

    private fun startLap(tc: Double, xc: Double, yc: Double) {
        special = 0; lapStart = tc; lapInvalid = false; expected = 0; splitStart = tc
        currentLapHasGapWarning = false; currentLapHasGapInvalid = false
        currentSplits.clear(); trace.clear(); trace.add(RefSample(0L, xc, yc))
        prevMarks = marks; marks = arrayOfNulls(n); reference?.reset(); deltaCalculator.reset()
    }

    private fun startOutLap(tc: Double, xc: Double, yc: Double) { startLap(tc, xc, yc); special = 1 }

    private fun endInLap(tc: Double, xc: Double, yc: Double) {
        val start = lapStart
        if (start != null && special == 0) {
            val lap = Math.round(tc - start)
            if (lap >= MIN_LAP_MS) {
                val splits = ArrayList(currentSplits)
                splits.add(Math.round(tc - splitStart))
                val mk = marks.toMutableList()
                if (expected < mk.size) mk[expected] = SectorMark(splits.last(), MarkKind.NEUTRAL)
                if (inlapCounts) lapsCompleted++
                val gapFlag = when {
                    currentLapHasGapInvalid || lapInvalid -> GapFlag.INVALID
                    currentLapHasGapWarning -> GapFlag.WARNING
                    else -> GapFlag.NONE
                }
                onLap?.invoke(LapResult(if (inlapCounts) lapsCompleted else 0, lap, splits, Math.round(start), Math.round(tc), false, emptyList(), null, mk, 2, gapFlag))
            }
        }
        lapStart = null; lapInvalid = false; special = 0; expected = 0; currentSplits.clear(); trace.clear()
        deltaCalculator.reset()
    }

    private fun onGate(tc: Double, xc: Double, yc: Double) {
        val idx = expected
        val split = Math.round(tc - splitStart)
        currentSplits.add(split)
        marks[idx] = SectorMark(split, sectorCalculator.kindFor(idx, split))
        splitStart = tc; expected++; onBoundary?.invoke()
        if (expected < n) return

        val start = lapStart ?: return
        val lap = Math.round(tc - start)
        val gapFlag = when {
            currentLapHasGapInvalid || lapInvalid -> GapFlag.INVALID
            currentLapHasGapWarning -> GapFlag.WARNING
            else -> GapFlag.NONE
        }
        if (special == 1) {
            if (lap >= MIN_LAP_MS) {
                if (outlapCounts) lapsCompleted++
                if (marks.isNotEmpty() && currentSplits.isNotEmpty()) marks[0] = SectorMark(currentSplits[0], MarkKind.NEUTRAL)
                onLap?.invoke(LapResult(if (outlapCounts) lapsCompleted else 0, lap, ArrayList(currentSplits), Math.round(start), Math.round(tc), false, emptyList(), null, marks.toList(), 1, gapFlag))
            }
            startLap(tc, xc, yc)
            return
        }
        if (lap >= MIN_LAP_MS) {
            lastLapMs = lap; lapsCompleted++
            for (i in currentSplits.indices) { if (currentSplits[i] < sessBest[i]) sessBest[i] = currentSplits[i] }
            trace.add(RefSample(lap, xc, yc))
            val copy = ArrayList(trace)
            val best = bestLapMs
            val deltaMs = if (best == null) null else lap - best
            val isBest = !lapInvalid && !currentLapHasGapInvalid && (best == null || lap < best)
            if (isBest) { bestLapMs = lap; refSamples = copy; reference = ReferenceLap(copy) }
            onLap?.invoke(LapResult(lapsCompleted, lap, ArrayList(currentSplits), Math.round(start), Math.round(tc), isBest, copy, deltaMs, marks.toList(), 0, gapFlag))
        }
        startLap(tc, xc, yc)
    }
}
