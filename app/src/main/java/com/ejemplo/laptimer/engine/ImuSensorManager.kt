package com.ejemplo.laptimer.engine

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import java.util.concurrent.atomic.AtomicReference

/**
 * Encapsulates Android sensor callbacks, rotation matrix transforms, gravity compensation,
 * IIR low-pass filtering, zero-calibration (tare), and 25Hz GPS timestamp synchronization.
 */
class ImuSensorManager(
    context: Context,
    cutoffHz: Float = ExponentialIirLowPassFilter.DEFAULT_CUTOFF_HZ,
    private val onTelemetryData: (ImuTelemetryData) -> Unit
) : SensorEventListener {

    companion object {
        private const val GRAVITY_EARTH = 9.80665f
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val rotVectorSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private var sensorThread: HandlerThread? = null
    private var sensorHandler: Handler? = null

    private val filterLongitudinal = ExponentialIirLowPassFilter(cutoffHz)
    private val filterLateral = ExponentialIirLowPassFilter(cutoffHz)
    private val filterVertical = ExponentialIirLowPassFilter(cutoffHz)

    private val rotationMatrix = FloatArray(9)
    private val earthAccel = FloatArray(3)
    private var hasRotationMatrix = false

    private var lastTimestampNanos: Long = 0L

    @Volatile
    var tareLongitudinalG = 0f

    @Volatile
    var tareLateralG = 0f

    @Volatile
    var tareVerticalG = 0f

    private val latestTelemetry = AtomicReference(ImuTelemetryData())

    fun start(): Boolean {
        if (accelSensor == null) return false

        val thread = HandlerThread("ImuSensorThread").apply { start() }
        sensorThread = thread
        val handler = Handler(thread.looper)
        sensorHandler = handler

        val accelRegistered = sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_GAME, handler)
        if (rotVectorSensor != null) {
            sensorManager.registerListener(this, rotVectorSensor, SensorManager.SENSOR_DELAY_GAME, handler)
        }
        return accelRegistered
    }

    fun stop() {
        try {
            sensorManager.unregisterListener(this)
        } catch (_: Exception) {}
        sensorThread?.quitSafely()
        sensorThread = null
        sensorHandler = null
    }

    fun setCutoffFrequency(cutoffHz: Float) {
        filterLongitudinal.cutoffHz = cutoffHz
        filterLateral.cutoffHz = cutoffHz
        filterVertical.cutoffHz = cutoffHz
    }

    fun tare() {
        val current = latestTelemetry.get()
        tareLongitudinalG += current.gLongitudinal
        tareLateralG += current.gLateral
        tareVerticalG += current.gVertical
    }

    fun resetTare() {
        tareLongitudinalG = 0f
        tareLateralG = 0f
        tareVerticalG = 0f
        filterLongitudinal.reset()
        filterLateral.reset()
        filterVertical.reset()
    }

    fun getLatestTelemetry(): ImuTelemetryData = latestTelemetry.get()

    override fun onSensorChanged(event: SensorEvent) {
        val timestamp = event.timestamp
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                hasRotationMatrix = true
            }
            Sensor.TYPE_ACCELEROMETER -> {
                processAccelerometer(event.values, timestamp)
            }
        }
    }

    private fun processAccelerometer(values: FloatArray, timestampNanos: Long) {
        val deltaSec = if (lastTimestampNanos > 0L) {
            (timestampNanos - lastTimestampNanos) / 1_000_000_000.0f
        } else {
            0.02f
        }
        lastTimestampNanos = timestampNanos

        val rawX = values[0]
        val rawY = values[1]
        val rawZ = values[2]

        val linX: Float
        val linY: Float
        val linZ: Float

        if (hasRotationMatrix) {
            earthAccel[0] = rotationMatrix[0] * rawX + rotationMatrix[1] * rawY + rotationMatrix[2] * rawZ
            earthAccel[1] = rotationMatrix[3] * rawX + rotationMatrix[4] * rawY + rotationMatrix[5] * rawZ
            earthAccel[2] = rotationMatrix[6] * rawX + rotationMatrix[7] * rawY + rotationMatrix[8] * rawZ

            linX = earthAccel[0]
            linY = earthAccel[1]
            linZ = earthAccel[2] - GRAVITY_EARTH
        } else {
            linX = rawX
            linY = rawY
            linZ = rawZ - GRAVITY_EARTH
        }

        val rawGLong = (linY / GRAVITY_EARTH)
        val rawGLat = (linX / GRAVITY_EARTH)
        val rawGVert = (linZ / GRAVITY_EARTH)

        val fLong = filterLongitudinal.filter(rawGLong, deltaSec) - tareLongitudinalG
        val fLat = filterLateral.filter(rawGLat, deltaSec) - tareLateralG
        val fVert = filterVertical.filter(rawGVert, deltaSec) - tareVerticalG

        val data = ImuTelemetryData(
            timestampNanos = timestampNanos,
            gLongitudinal = fLong,
            gLateral = fLat,
            gVertical = fVert
        )

        latestTelemetry.set(data)
        onTelemetryData(data)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
