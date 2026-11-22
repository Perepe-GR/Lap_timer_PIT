package com.ejemplo.laptimer

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.FtdiSerialDriver
import com.hoho.android.usbserial.driver.ProlificSerialDriver
import com.hoho.android.usbserial.driver.ProbeTable
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class UsbSerialManager(
    private val context: Context,
    private val onData: (ByteArray) -> Unit,
    private val onStatus: (String) -> Unit
) : SerialInputOutputManager.Listener {

    companion object {
        const val ACTION_USB_PERMISSION = "com.ejemplo.laptimer.USB_PERMISSION"
        const val BAUD_RATE = 115200
        private const val TAG = "UsbSerialManager"
    }

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val ioExecutor: ExecutorService = Executors.newCachedThreadPool()
    private var port: UsbSerialPort? = null
    private var ioManager: SerialInputOutputManager? = null

    private val watchdogHandler = Handler(Looper.getMainLooper())
    private var lastDataReceivedMs = System.currentTimeMillis()
    private var watchdogStarted = false

    private val watchdog = object : Runnable {
        override fun run() {
            if (port != null) {
                val idleMs = System.currentTimeMillis() - lastDataReceivedMs
                if (idleMs > 4000L) {
                    Log.w(TAG, "Watchdog: No NMEA data received for ${idleMs}ms. Auto-recovering USB port...")
                    onStatus("Reconnecting USB GPS...")
                    try {
                        openPort()
                    } catch (e: Exception) {
                        Log.e(TAG, "Watchdog auto-reconnect failed: ${e.message}")
                    }
                    lastDataReceivedMs = System.currentTimeMillis()
                }
            } else {
                val driver = findDriver()
                if (driver != null) {
                    Log.i(TAG, "Watchdog: Found disconnected USB driver. Auto-connecting...")
                    openPort()
                }
            }
            watchdogHandler.postDelayed(this, 3000L)
        }
    }

    val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            }
            Log.i(TAG, "BroadcastReceiver onReceive: action=$action, device=${device?.deviceName}")

            if (action == ACTION_USB_PERMISSION) {
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                if (granted) {
                    openPort()
                } else {
                    onStatus("USB permission denied")
                }
            } else if (action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                openPort()
            } else if (action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                onStatus("USB Disconnected")
                disconnect()
            }
        }
    }

    fun findDriver(): UsbSerialDriver? {
        val deviceList = usbManager.deviceList
        if (deviceList.isEmpty()) {
            Log.w(TAG, "findDriver: No USB devices connected.")
        }
        val defaultDriver = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager).firstOrNull()
        if (defaultDriver != null) return defaultDriver

        val customTable = ProbeTable().apply {
            addProduct(0x1546, 0x01a9, CdcAcmSerialDriver::class.java)
            addProduct(0x1546, 0x01a9, FtdiSerialDriver::class.java)
            addProduct(0x1546, 0x01a9, ProlificSerialDriver::class.java)
        }
        return UsbSerialProber(customTable).findAllDrivers(usbManager).firstOrNull()
    }

    fun connect(): Boolean {
        if (!watchdogStarted) {
            watchdogStarted = true
            watchdogHandler.postDelayed(watchdog, 3000L)
        }
        if (port != null) return true
        val driver = findDriver()
        if (driver == null) {
            onStatus("Connect antenna via USB")
            return false
        }
        openPort()
        return isConnected
    }

    fun openPort() {
        if (!watchdogStarted) {
            watchdogStarted = true
            watchdogHandler.postDelayed(watchdog, 3000L)
        }
        val driver = findDriver()
        if (driver == null) {
            onStatus("Antenna not detected")
            return
        }
        val device = driver.device
        if (!usbManager.hasPermission(device)) {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val permissionIntent = PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), flags)
            usbManager.requestPermission(device, permissionIntent)
            onStatus("Requesting USB permission...")
            return
        }

        val connection = usbManager.openDevice(device)
        if (connection == null) {
            onStatus("Could not open USB device")
            return
        }
        if (driver.ports.isEmpty()) {
            onStatus("No serial ports on device")
            connection.close()
            return
        }

        val p = driver.ports[0]
        try {
            p.open(connection)
            p.setParameters(BAUD_RATE, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
        } catch (e: Exception) {
            onStatus("USB error: ${e.message}")
            runCatching { p.close() }
            return
        }

        port = p
        lastDataReceivedMs = System.currentTimeMillis()
        ioManager = SerialInputOutputManager(p, this).also {
            ioExecutor.submit(it)
        }
        onStatus("Waiting for GPS...")
    }

    fun disconnect() {
        ioManager?.stop()
        ioManager = null
        runCatching { port?.close() }
        port = null
    }

    fun shutdown() {
        watchdogHandler.removeCallbacks(watchdog)
        watchdogStarted = false
        disconnect()
        ioExecutor.shutdownNow()
    }

    val isConnected: Boolean
        get() = port != null

    override fun onNewData(data: ByteArray) {
        lastDataReceivedMs = System.currentTimeMillis()
        onData(data)
    }

    override fun onRunError(e: Exception) {
        Log.e(TAG, "onRunError: SerialInputOutputManager run error: ${e.message}", e)
        onStatus("USB Reconnecting...")
        disconnect()
    }
}
