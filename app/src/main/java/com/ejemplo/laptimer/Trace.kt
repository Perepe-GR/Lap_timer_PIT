package com.ejemplo.laptimer

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/** Traza GPS de una sesión: tiempo (ms desde el inicio), posición y velocidad. */
class Trace(val t: IntArray, val lat: DoubleArray, val lon: DoubleArray, val v: FloatArray) {
    val size: Int get() = t.size
}

/** Escribe la traza en binario compacto: 24 bytes por muestra (unos 2 MB por hora a 25 Hz). */
class TraceWriter(file: File) {
    private val out = DataOutputStream(BufferedOutputStream(FileOutputStream(file, true), 1 shl 16))
    private var count = 0

    fun add(offsetMs: Long, lat: Double, lon: Double, kmh: Float) {
        try {
            out.writeInt(offsetMs.toInt())
            out.writeDouble(lat)
            out.writeDouble(lon)
            out.writeFloat(kmh)
            count++
            if (count % 250 == 0) out.flush()
        } catch (e: Exception) {
            // se ignora
        }
    }

    fun close() {
        try {
            out.flush()
            out.close()
        } catch (e: Exception) {
            // se ignora
        }
    }
}

object TraceReader {
    fun read(file: File): Trace? {
        if (!file.exists()) return null
        val n = (file.length() / 24).toInt()
        if (n < 2) return null
        val t = IntArray(n)
        val la = DoubleArray(n)
        val lo = DoubleArray(n)
        val v = FloatArray(n)
        try {
            DataInputStream(BufferedInputStream(FileInputStream(file), 1 shl 16)).use { inp ->
                for (i in 0 until n) {
                    t[i] = inp.readInt()
                    la[i] = inp.readDouble()
                    lo[i] = inp.readDouble()
                    v[i] = inp.readFloat()
                }
            }
        } catch (e: Exception) {
            return null
        }
        return Trace(t, la, lo, v)
    }
}
