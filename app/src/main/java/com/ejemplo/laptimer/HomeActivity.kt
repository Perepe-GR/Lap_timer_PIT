package com.ejemplo.laptimer

import android.content.Context
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.hoho.android.usbserial.driver.UsbSerialProber

/** Menú de inicio: se abre antes de las pantallas de vueltas. */
class HomeActivity : AppCompatActivity() {

    internal lateinit var store: CircuitStore
    internal lateinit var statsStore: StatsStore
    internal lateinit var settings: NetSettings
    internal lateinit var txtCircuit: TextView
    internal lateinit var txtAntenna: TextView
    internal lateinit var txtPit: TextView
    internal lateinit var txtMode: TextView

    internal val muted get() = ThemeManager.getTheme(this).muted
    internal val lime get() = ThemeManager.getTheme(this).accent
    internal val colBg get() = ThemeManager.getTheme(this).bg
    internal val colCard get() = ThemeManager.getTheme(this).cardBg
    internal val colText get() = ThemeManager.getTheme(this).text

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        store = CircuitStore(this)
        statsStore = StatsStore(this)
        settings = NetSettings(this)
        setContentView(buildUi())
    }

    override fun onResume() {
        super.onResume()
        setContentView(buildUi())
        refreshStatus()
    }

    internal fun refreshStatus() {
        val c = store.loadActive()
        txtCircuit.text = "Circuito activo: " + (c?.name ?: "ninguno")
        if (settings.gpsSource == "usb") {
            val usb = getSystemService(Context.USB_SERVICE) as UsbManager
            val has = UsbSerialProber.getDefaultProber().findAllDrivers(usb).isNotEmpty()
            txtAntenna.text = "GPS: Antena USB (" + (if (has) "detectada" else "no detectada") + ")"
            txtAntenna.setTextColor(if (has) lime else muted)
        } else {
            txtAntenna.text = "GPS: " + settings.gpsSourceLabel()
            txtAntenna.setTextColor(lime)
        }
        txtPit.text = if (settings.enabled) {
            "Pit crew: activado · sala " + settings.room
        } else {
            "Pit crew: desactivado"
        }
        txtMode.text = "Modo: " + settings.raceModeLabel()
        txtMode.setTextColor(if (settings.isEndurance()) lime else muted)
    }
}
