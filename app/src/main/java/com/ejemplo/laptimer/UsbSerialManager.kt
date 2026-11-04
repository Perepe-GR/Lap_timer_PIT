package com.ejemplo.laptimer

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
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

    val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            }
            Log.i(TAG, "BroadcastReceiver onReceive: action=$action, device=${device?.deviceName} (VID:${device?.vendorId}, PID:${device?.productId})")

            if (action == ACTION_USB_PERMISSION) {
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                if (granted) {
                    Log.i(TAG, "USB permission GRANTED by user for device: ${device?.deviceName}")
                    openPort()
                } else {
                    Log.e(TAG, "USB permission DENIED by user for device: ${device?.deviceName}")
                    onStatus("USB permission denied")
                }
            } else if (action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                Log.i(TAG, "USB device attached")
                openPort()
            } else if (action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                Log.i(TAG, "USB device detached")
                onStatus("USB Disconnected")
                disconnect()
            }
        }
    }

    fun findDriver(): UsbSerialDriver? {
        val deviceList = usbManager.deviceList
        if (deviceList.isEmpty()) {
            Log.w(TAG, "findDriver: No USB devices connected to system bus.")
        } else {
            for ((name, dev) in deviceList) {
                Log.i(TAG, "Connected USB Bus Device: name=$name, vendorId=${dev.vendorId} (0x${Integer.toHexString(dev.vendorId)}), productId=${dev.productId} (0x${Integer.toHexString(dev.productId)})")
            }
        }

        val defaultDriver = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager).firstOrNull()
        if (defaultDriver != null) {
            Log.i(TAG, "findDriver: Found driver via default prober -> ${defaultDriver.javaClass.simpleName} (Device: ${defaultDriver.device.deviceName})")
            return defaultDriver
        }

        // Custom Prober for u-blox (Vendor ID: 0x1546 / 5446, Product ID: 0x01a9 / 425)
        val customTable = ProbeTable().apply {
            addProduct(0x1546, 0x01a9, CdcAcmSerialDriver::class.java)
            addProduct(0x1546, 0x01a9, FtdiSerialDriver::class.java)
            addProduct(0x1546, 0x01a9, ProlificSerialDriver::class.java)
        }
        val customDriver = UsbSerialProber(customTable).findAllDrivers(usbManager).firstOrNull()
        if (customDriver != null) {
            Log.i(TAG, "findDriver: Found driver via custom u-blox prober (0x1546:0x01a9) -> ${customDriver.javaClass.simpleName}")
        } else {
            Log.w(TAG, "findDriver: No driver found via default or custom u-blox prober for 0x1546:0x01a9")
        }
        return customDriver
    }

    fun connect(): Boolean {
        if (port != null) {
            Log.i(TAG, "connect: Port already connected.")
            return true
        }
        val driver = findDriver()
        if (driver == null) {
            Log.w(TAG, "connect: No driver found. Please check OTG cable and u-blox connection.")
            onStatus("Connect antenna via USB")
            return false
        }
        openPort()
        return isConnected
    }

    fun openPort() {
        val driver = findDriver()
        if (driver == null) {
            Log.w(TAG, "openPort: Antenna not detected (driver is null)")
            onStatus("Antenna not detected")
            return
        }
        val device = driver.device
        val hasPerm = usbManager.hasPermission(device)
        Log.i(TAG, "openPort: Device found -> ${device.deviceName}, Vendor: ${device.vendorId}, Product: ${device.productId}, HasPermission: $hasPerm")

        if (!hasPerm) {
            Log.i(TAG, "openPort: Requesting USB permission for device...")
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
            Log.e(TAG, "openPort: FAILED to open USB device connection (usbManager.openDevice returned null).")
            onStatus("Could not open USB device")
            return
        }
        Log.i(TAG, "openPort: UsbDeviceConnection opened successfully (serialNumber: ${connection.serial})")

        if (driver.ports.isEmpty()) {
            Log.e(TAG, "openPort: Driver has no ports available!")
            onStatus("No serial ports on device")
            connection.close()
            return
        }

        val p = driver.ports[0]
        try {
            p.open(connection)
            p.setParameters(BAUD_RATE, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            Log.i(TAG, "openPort: Port opened successfully and parameters set to $BAUD_RATE baud, 8 data bits, 1 stop bit, no parity (Endpoint: 0x80 IN)")
        } catch (e: Exception) {
            Log.e(TAG, "openPort: Exception during p.open() or setParameters(): ${e.message}", e)
            onStatus("USB error: ${e.message}")
            runCatching { p.close() }
            return
        }

        port = p
        ioManager = SerialInputOutputManager(p, this).also {
            ioExecutor.submit(it)
            Log.i(TAG, "openPort: SerialInputOutputManager started on background thread.")
        }
        onStatus("Waiting for GPS...")
    }

    fun disconnect() {
        Log.i(TAG, "disconnect: Disconnecting USB serial port.")
        ioManager?.stop()
        ioManager = null
        runCatching { port?.close() }
        port = null
    }

    fun shutdown() {
        Log.i(TAG, "shutdown: Shutting down UsbSerialManager.")
        disconnect()
        ioExecutor.shutdownNow()
    }

    val isConnected: Boolean
        get() = port != null

    override fun onNewData(data: ByteArray) {
        val nmeaSnippet = try {
            String(data, Charsets.US_ASCII).take(120).replace("\r", "\\r").replace("\n", "\\n")
        } catch (_: Exception) {
            ""
        }
        Log.d(TAG, "onNewData: Received ${data.size} raw bytes | NMEA preview: $nmeaSnippet")
        onData(data)
    }

    override fun onRunError(e: Exception) {
        Log.e(TAG, "onRunError: SerialInputOutputManager run error: ${e.message}", e)
        onStatus("USB Disconnected")
        disconnect()
    }
}
