package com.ejemplo.laptimer

import android.content.Context
import org.json.JSONObject
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Circuito demo cargado desde res/raw/demo_circuit.json (GeoJSON).
 */
object DemoCircuit {
    var NAME = "Karting Empuriabrava"
    lateinit var frame: LocalFrame
        private set
    var LENGTH: Double = 0.0
        private set
    var PIT_LENGTH: Double = 0.0
        private set

    lateinit var circuit: Circuit
        private set

    private val trackCoords = ArrayList<Pair<Double, Double>>() // (x, y)
    private val trackDists = ArrayList<Double>()
    private val pitCoords = ArrayList<Pair<Double, Double>>() // (x, y)
    private val pitDists = ArrayList<Double>()

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        try {
            val jsonStr = context.resources.openRawResource(R.raw.demo_circuit).bufferedReader().use { it.readText() }
            val root = JSONObject(jsonStr)
            val features = root.getJSONArray("features")

            var finishGate: GatePoint? = null
            val sectorGates = ArrayList<GatePoint>()
            var pitInGate: GatePoint? = null
            var pitOutGate: GatePoint? = null

            var firstLat = 42.260149
            var firstLon = 3.11234

            for (i in 0 until features.length()) {
                val f = features.getJSONObject(i)
                val prop = f.getJSONObject("properties")
                val type = prop.optString("type", "")
                val geom = f.getJSONObject("geometry")
                val geomType = geom.getString("type")

                if (geomType == "LineString" && type == "circuit_line") {
                    val coords = geom.getJSONArray("coordinates")
                    trackCoords.clear()
                    trackDists.clear()
                    var cumDist = 0.0
                    for (j in 0 until coords.length()) {
                        val pt = coords.getJSONArray(j)
                        val lon = pt.getDouble(0)
                        val lat = pt.getDouble(1)
                        if (j == 0) {
                            firstLat = lat
                            firstLon = lon
                            frame = LocalFrame(lat, lon)
                        }
                        val x = frame.x(lon)
                        val y = frame.y(lat)
                        trackCoords.add(Pair(x, y))
                        if (j > 0) {
                            val prev = trackCoords[j - 1]
                            cumDist += hypot(x - prev.first, y - prev.second)
                        }
                        trackDists.add(cumDist)
                    }
                    LENGTH = cumDist
                } else if (geomType == "LineString" && type == "pit_lane") {
                    val coords = geom.getJSONArray("coordinates")
                    pitCoords.clear()
                    pitDists.clear()
                    var cumDist = 0.0
                    for (j in 0 until coords.length()) {
                        val pt = coords.getJSONArray(j)
                        val lon = pt.getDouble(0)
                        val lat = pt.getDouble(1)
                        val x = frame.x(lon)
                        val y = frame.y(lat)
                        pitCoords.add(Pair(x, y))
                        if (j > 0) {
                            val prev = pitCoords[j - 1]
                            cumDist += hypot(x - prev.first, y - prev.second)
                        }
                        pitDists.add(cumDist)
                    }
                    PIT_LENGTH = cumDist
                } else if (geomType == "Point") {
                    val coords = geom.getJSONArray("coordinates")
                    val lon = coords.getDouble(0)
                    val lat = coords.getDouble(1)
                    val rumbo = prop.optDouble("rumbo", 0.0)
                    val anchoIzq = prop.optDouble("anchoIzq", 2.0)
                    val anchoDer = prop.optDouble("anchoDer", 2.0)
                    val gp = GatePoint(lat, lon, rumbo, anchoIzq, anchoDer)

                    when (type) {
                        "meta" -> finishGate = gp
                        "sector" -> sectorGates.add(gp)
                        "boxes_entrada" -> pitInGate = gp
                        "boxes_salida" -> pitOutGate = gp
                    }
                }
            }

            if (!::frame.isInitialized) {
                frame = LocalFrame(firstLat, firstLon)
            }

            circuit = Circuit(
                name = NAME,
                finish = finishGate ?: GatePoint(firstLat, firstLon, 0.0, 2.0, 2.0),
                sectors = sectorGates,
                pitIn = pitInGate,
                pitOut = pitOutGate
            )
            initialized = true
        } catch (e: Exception) {
            frame = LocalFrame(42.260149, 3.11234)
            circuit = Circuit(NAME, GatePoint(42.260149, 3.11234, 0.0, 2.0, 2.0), emptyList(), null, null)
            LENGTH = 714.0
            PIT_LENGTH = 120.0
            initialized = true
        }
    }

    private fun wrap(s: Double): Double = if (LENGTH > 0) ((s % LENGTH) + LENGTH) % LENGTH else 0.0
    private fun norm(a: Double): Double = ((a % 360.0) + 360.0) % 360.0

    fun pose(sIn: Double): DoubleArray {
        if (trackCoords.size < 2) return doubleArrayOf(0.0, 0.0, 0.0)
        val s = wrap(sIn)
        var idx = 0
        for (i in 0 until trackDists.size - 1) {
            if (s >= trackDists[i] && s <= trackDists[i + 1]) {
                idx = i
                break
            }
        }
        val d0 = trackDists[idx]
        val d1 = trackDists[idx + 1]
        val span = d1 - d0
        val t = if (span < 1e-6) 0.0 else (s - d0) / span

        val p0 = trackCoords[idx]
        val p1 = trackCoords[idx + 1]
        val x = p0.first + t * (p1.first - p0.first)
        val y = p0.second + t * (p1.second - p0.second)

        val angleRad = atan2(p1.first - p0.first, p1.second - p0.second)
        val heading = norm(Math.toDegrees(angleRad))
        return doubleArrayOf(x, y, heading)
    }

    fun pitPose(uIn: Double): DoubleArray {
        if (pitCoords.size < 2) return pose(uIn)
        val u = uIn.coerceIn(0.0, if (PIT_LENGTH > 0) PIT_LENGTH else 1.0)
        var idx = 0
        for (i in 0 until pitDists.size - 1) {
            if (u >= pitDists[i] && u <= pitDists[i + 1]) {
                idx = i
                break
            }
        }
        val d0 = pitDists[idx]
        val d1 = pitDists[idx + 1]
        val span = d1 - d0
        val t = if (span < 1e-6) 0.0 else (u - d0) / span

        val p0 = pitCoords[idx]
        val p1 = pitCoords[idx + 1]
        val x = p0.first + t * (p1.first - p0.first)
        val y = p0.second + t * (p1.second - p0.second)

        val angleRad = atan2(p1.first - p0.first, p1.second - p0.second)
        val heading = norm(Math.toDegrees(angleRad))
        return doubleArrayOf(x, y, heading)
    }

    fun speedMs(sIn: Double): Double {
        return 15.0
    }
}
