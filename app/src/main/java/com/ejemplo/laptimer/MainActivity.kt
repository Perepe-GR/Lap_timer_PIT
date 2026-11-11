package com.ejemplo.laptimer

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.ejemplo.laptimer.engine.*
import com.ejemplo.laptimer.firebase.NetStatus
import com.ejemplo.laptimer.firebase.SwapRequest
import com.ejemplo.laptimer.logic.*
import com.ejemplo.laptimer.session.*
import com.ejemplo.laptimer.ui.*

/** Pantalla de sesión: Coordinador de ciclo de vida que gestiona USB, GPS, Bindings y lógica de sesión. */
class MainActivity : AppCompatActivity() {

    internal lateinit var bindings: MainActivityBindings
    internal lateinit var usbSerialManager: UsbSerialManager
    internal lateinit var gpsLocationManager: GpsLocationManager
    internal lateinit var imuSensorManager: ImuSensorManager
    @Volatile internal var lastImuData = com.ejemplo.laptimer.engine.ImuTelemetryData()
    internal val phase1Engine = com.ejemplo.laptimer.engine.Phase1PerformanceEngine()

    internal lateinit var raceStore: RaceStore
    internal var race: RaceRecord? = null
    internal var raceStartClock = 0L
    internal var raceEnded = false
    internal var raceTimeUp = false
    internal var raceLapsDone = 0

    internal val parser = NmeaParser()
    internal val lineBuffer = StringBuilder()

    internal lateinit var store: CircuitStore
    internal lateinit var statsStore: StatsStore
    internal lateinit var sessionStore: SessionStore
    internal lateinit var settings: NetSettings
    @Volatile internal var recorder: SessionRecorder? = null
    @Volatile internal var link: FirebaseRestLink? = null

    @Volatile internal var circuit: Circuit? = null
    @Volatile internal var engine: LapEngine? = null
    @Volatile internal var lastState: EngineState? = null
    internal var stint = StintState(1, "Driver 1", 0, 0L, 0, 0, 0)

    @Volatile internal var hasFix = false
    @Volatile internal var lastLat = 0.0
    @Volatile internal var lastLon = 0.0
    @Volatile internal var lastKmh = 0.0
    @Volatile internal var lastCourse: Double? = null
    @Volatile internal var lastDataAt = 0L
    internal var lastAccuracyMeters: Float? = null
    @Volatile internal var lastFixAt = 0L
    @Volatile internal var currentAlert: String? = null
    internal var hz = 0.0
    internal var lastRawMs: Long? = null
    internal var sats = 0
    internal var hdop = 0.0
    internal var altitude = 0.0
    internal var autoLoadDone = false

    @Volatile internal var demoActive = false
    internal var demo: DemoSource? = null
    internal var demoFactor = 1

    internal var reconName: String? = null
    internal var draftFinish: GatePoint? = null
    internal val draftSectors = mutableListOf<GatePoint>()

    internal var netStatus = NetStatus.OFF

    internal var deltaSum = 0.0
    internal var deltaCount = 0
    internal var deltaShownAt = 0L

    internal var viewIndex = 0
    internal var downX = 0f
    internal var downY = 0f
    internal var tracking = false
    internal val hideTop = Runnable { bindings.topBar.visibility = View.GONE }

    internal val activeMsgs = ArrayList<ActiveMsg>()
    internal val prune = Runnable { pruneMsgs() }
    @Volatile internal var pendingSwap: SwapRequest? = null

    @Volatile internal var simNowMs = 0L
    internal fun clockNow(): Long = if (demoActive) simNowMs else System.currentTimeMillis()

    internal var demoLapsSincePit = 0
    internal var demoPitKey: String? = null
    internal var demoStops = 0
    internal var lastFinalShown = false

    internal var inPit = false
    internal var inPaddock = false
    internal var paddockDismissed = false
    internal var pitStartAt = 0L
    internal var pitBlinkOn = false
    internal var pitSecOverride = 0
    internal var pitFailUntil = 0L
    internal var pitFailText = ""
    internal val pitBlink = object : Runnable {
        override fun run() {
            if (!inPit && !inPaddock) return
            updatePitOverlay()
            bindings.pitOverlay.postDelayed(this, 100L)
        }
    }
    internal val hideLapSummary = Runnable { bindings.lapOverlay.visibility = View.GONE }

    internal val COL_GREEN get() = ThemeManager.getTheme(this).accent
    internal val COL_RED get() = ThemeManager.getTheme(this).alert

    internal val tickHandler = Handler(Looper.getMainLooper())
    internal val tick = object : Runnable {
        override fun run() {
            updateStintStrip(); updateAlerts(); pruneMsgs()
            if (inPit && clockNow() - pitStartAt > 900_000L) exitPit()
            ensureProgrammedSwap(); maybeRequestDemoPit(); checkRaceTime(); updateLapHeader()
            val fin = (isFinalLap() || isRaceFinalLap()) && !inPit
            if (fin != lastFinalShown) { lastFinalShown = fin; renderBottom() }
            tickHandler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupWindowAndDisplay()
        bindings = MainActivityBindings(this)
        setupGpsManagers()

        store = CircuitStore(this); statsStore = StatsStore(this)
        sessionStore = SessionStore(this); raceStore = RaceStore(this)
        settings = NetSettings(this)
        recorder = SessionRecorder(sessionStore) { settings.saveTrace }
        stint = loadStint(); resumeRace()

        circuit = store.loadActive()
        engine = circuit?.let { buildEngine(it, true) }

        link = FirebaseRestLink(
            settings, { buildTelemetry() },
            { msgs -> runOnUiThread { showPitMessages(msgs) } },
            { req -> runOnUiThread { onPanelCommand(req) } },
            { st -> runOnUiThread { netStatus = st; renderNet() } }
        )

        setupClickListeners()
        resetDisplay(); applyThemeStyles(); refreshTop(); renderNet(); renderBottom()
        ThemeManager.onThemeChanged = { runOnUiThread { applyThemeStyles() } }

        intent.getStringExtra("recon")?.let { startRecon(it) }
        if (intent.getBooleanExtra("demo", false)) startDemo()
        if (intent.getBooleanExtra("pitlaneActive", false)) {
            enterPit()
        }

        showTopBar()
    }

    override fun onResume() {
        super.onResume()
        applyThemeStyles()
        if (!demoActive) { stint = loadStint(); syncActiveCircuit() }
        engine?.outlapCounts = settings.outlapCounts
        engine?.inlapCounts = settings.inlapCounts
        connectGps(); link?.start(); renderNet()
        tickHandler.post(tick)
    }

    override fun onPause() {
        tickHandler.removeCallbacks(tick)
        link?.stop()
        if (demoActive) stopDemo()
        if (hasFix && !demoActive) store.saveLastPosition(lastLat, lastLon)
        disconnectGps()
        super.onPause()
    }

    override fun onDestroy() {
        if (isFinishing) endRace("closed")
        demo?.stop(); recorder?.end()
        unregisterReceiver(usbSerialManager.permissionReceiver)
        usbSerialManager.shutdown()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                connectGps()
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> { downX = ev.rawX; downY = ev.rawY; tracking = true }
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_CANCEL -> tracking = false
            MotionEvent.ACTION_UP -> { if (tracking) { tracking = false; handleSwipe(downX, downY, ev.rawX, ev.rawY) } }
        }
        return super.dispatchTouchEvent(ev)
    }
}
