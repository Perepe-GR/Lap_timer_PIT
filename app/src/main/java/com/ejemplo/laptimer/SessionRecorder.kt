package com.ejemplo.laptimer

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Graba sesiones automáticamente: empieza al moverse (más de 5 km/h) y termina tras 5 minutos
 * parado, al cambiar de circuito o al cerrar la app. Solo se guardan las que tienen vueltas.
 */
class SessionRecorder(private val store: SessionStore, private val saveTrace: () -> Boolean) {
    private var rec: SessionRecord? = null
    private var writer: TraceWriter? = null
    private var t0 = 0L
    private var lastLat = Double.NaN
    private var lastLon = Double.NaN
    private var stoppedSince = -1L

    /** Devuelve true si con esta muestra ha empezado una sesión nueva. */
    @Synchronized
    fun onSample(
        circuit: Circuit, gpsT: Long, lat: Double, lon: Double, kmh: Double,
        driver: String, stint: Int, car: String
    ): Boolean {
        var started = false
        if (rec == null) {
            if (kmh < 5.0) return false
            val id = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            rec = SessionRecord(
                id, circuit.name, System.currentTimeMillis(), circuit.sectorCount,
                driver = driver, stint = stint, car = car
            )
            writer = if (saveTrace()) TraceWriter(store.traceFile(id)) else null
            t0 = gpsT
            lastLat = Double.NaN
            lastLon = Double.NaN
            stoppedSince = -1L
            started = true
        }
        val r = rec ?: return started
        val off = gpsT - t0
        if (!lastLat.isNaN()) r.distanceM += distanceM(lastLat, lastLon, lat, lon)
        lastLat = lat
        lastLon = lon
        if (kmh > r.maxSpeedKmh) r.maxSpeedKmh = kmh
        r.durationMs = off
        runCatching { writer?.add(off, lat, lon, kmh.toFloat()) }
        if (kmh < 2.0) {
            if (stoppedSince < 0) stoppedSince = gpsT
            else if (gpsT - stoppedSince > 300_000L) end()
        } else {
            stoppedSince = -1L
        }
        return started
    }

    @Synchronized
    fun onLap(res: LapResult) {
        val r = rec ?: return
        r.laps.add(LapRecord(res.number, res.lapMs, res.splits, res.startMs - t0, res.endMs - t0))
        runCatching { store.save(r) }
    }

    @Synchronized
    fun end() {
        val r = rec ?: return
        runCatching { writer?.close() }
        writer = null
        rec = null
        if (r.laps.isEmpty()) runCatching { store.delete(r.id) } else runCatching { store.save(r) }
    }
}

/** Texto " · Piloto (stint N)" para las listas de sesiones. */
fun driverTag(s: SessionRecord): String {
    if (s.driver.isEmpty()) return ""
    return " · " + s.driver + (if (s.stint > 0) " (stint " + s.stint + ")" else "")
}
