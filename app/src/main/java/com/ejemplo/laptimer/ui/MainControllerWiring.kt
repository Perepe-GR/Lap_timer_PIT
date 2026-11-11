package com.ejemplo.laptimer.ui

import android.content.IntentFilter
import android.os.SystemClock
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import com.ejemplo.laptimer.GpsLocationManager
import com.ejemplo.laptimer.MainActivity
import com.ejemplo.laptimer.NmeaEvent
import com.ejemplo.laptimer.UsbSerialManager
import com.ejemplo.laptimer.engine.ImuSensorManager
import com.ejemplo.laptimer.logic.*
import com.ejemplo.laptimer.session.*

fun MainActivity.setupGpsManagers() {
    usbSerialManager = UsbSerialManager(
        this,
        { data -> onNewData(data) },
        { status -> bindings.txtGps.text = status }
    )
    gpsLocationManager = GpsLocationManager(
        this,
        { loc ->
            lastDataAt = SystemClock.elapsedRealtime()
            val speedKmh = if (loc.hasSpeed()) loc.speed * 3.6 else 0.0
            val course = if (loc.hasBearing()) loc.bearing.toDouble() else null
            val time = if (loc.time > 0L) loc.time else System.currentTimeMillis()
            if (loc.hasAccuracy()) {
                lastAccuracyMeters = loc.accuracy
            }
            val ev = NmeaEvent.Position(loc.latitude, loc.longitude, speedKmh, course, time)
            onPosition(ev, true)
        },
        { status -> bindings.txtGps.text = status }
    )
    imuSensorManager = ImuSensorManager(this) { imu ->
        lastImuData = imu
        runOnUiThread { renderImuTelemetry(imu) }
    }
    imuSensorManager.start()
    ContextCompat.registerReceiver(
        this,
        usbSerialManager.permissionReceiver,
        IntentFilter(UsbSerialManager.ACTION_USB_PERMISSION),
        ContextCompat.RECEIVER_NOT_EXPORTED
    )
}

fun MainActivity.setupClickListeners() {
    bindings.btnHome.setOnClickListener { askExit() }
    bindings.btnMarkFinish.setOnClickListener { markFinish() }
    bindings.btnMarkSector.setOnClickListener { markSector() }
    bindings.btnSave.setOnClickListener { saveCircuit() }
    bindings.btnCancel.setOnClickListener { exitRecon() }
    bindings.btnSwapConfirm.setOnClickListener { confirmSwap() }
    bindings.btnPitSwapConfirm.setOnClickListener { confirmSwap() }
    bindings.lapOverlay.setOnClickListener { hideLapSummaryNow() }
    bindings.pitOverlay.setOnLongClickListener {
        if (inPit) exitPit()
        if (inPaddock) {
            paddockDismissed = true
            inPaddock = false
            bindings.pitOverlay.removeCallbacks(pitBlink)
            bindings.pitOverlay.visibility = View.GONE
        }
        swapDismissed = true
        pendingSwap = null
        toast("Pit screen closed")
        true
    }
    bindings.txtTop.setOnClickListener { if (demoActive) cycleDemoSpeed() }

    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            askExit()
        }
    })
}
