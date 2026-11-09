# Instructions for Copilot & AI Agents

## Project Overview
Android app (Kotlin, XML views, no Compose) for high-precision lap timing, telemetry, and endurance racing management system, version 2.5.0.
Integrates native external 25 Hz u-blox USB OTG GPS receivers (`usb-serial-for-android`), internal GPS, RaceBox, Dragy, and Garmin.
Features a horizontal cockpit display layout for vehicle mounting and a responsive real-time Web Pit Wall Dashboard (`pit-panel/index.html`) using HTML5, Tailwind CSS, and Firebase Realtime Database.

## Language Guidelines
- **User Interface (UI)**: All user-facing UI text MUST strictly be in **English**.
- **Documentation & Comments**: English, Catalan, or Spanish.

## Subpackage Architecture (`app/src/main/java/com/ejemplo/laptimer`)
- `engine/`: `LapEngine.kt`, `LapDetector.kt`, `GpsFilter.kt`, `SectorCalculator.kt`, `ReferenceLap.kt`, `DeltaCalculator.kt`, `GapHandler.kt`, `EngineModels.kt`
- `session/`: `SessionLifecycle.kt`, `SessionGpsBinding.kt`, `SessionPit.kt`, `SessionEvents.kt`, `SessionRendering.kt`, `StatusBarsRenderer.kt`, `TelemetryRenderer.kt`, `SessionRecording.kt`, `SessionStore.kt`, `Trace.kt`
- `logic/`: `MainSessionController.kt`, `MainNavigator.kt`, `MainPermissionsHandler.kt`, `MainThemeController.kt`, `MainUiState.kt`, `MainEventBus.kt`, `MainRaceController.kt`, `MainDemoController.kt`, `MainReconController.kt`, `ModePageActivity.kt`
- `endurance/`: Multi-device endurance lobby (`lobby/`), driver runtime (`pilot/`), leader dashboard (`leader/`), and real-time synchronization under `/rooms/{roomCode}/` (`sync/`)
- `editor/`: `MapEditorActivity.kt`, `MapEditorViewModel.kt`, `EditorGestureHandler.kt`, `EditorToolbar.kt`, `GateRenderer.kt`, `EditorDialogs.kt`, `TrackSaver.kt`
- `net/` & `firebase/`: `NetSettings.kt`, `NetSettingsModel.kt`, `NetSettingsStore.kt`, `FirebaseRestLink.kt`, `FirebaseRestClient.kt`, `FirebasePayload.kt`, `FirebaseAuthManager.kt`
- `util/`: `ValidationUtils.kt` (strict team name/PIN regex validation), `SecurityUtils.kt` (SHA-256 + salt PIN hashing)
- `results/`: `ResultsActivity.kt` (unified tabs for history, records, and stats)
- `ui/` & root: `MainActivity.kt`, `MainViewSetup.kt`, `MainControllerWiring.kt`, `HomeActivity.kt`, `HomeUiBuilder.kt`, `CircuitsActivity.kt`, `SessionSettingsActivity.kt`, `PitSettingsActivity.kt`, `SettingsViewModel.kt`

## Key Rules & Project Conventions
- **File Size Rule**: Target <= 200 lines per file (tolerance margin up to 220 lines max). Split files whenever technically sensible.
- **UI Contract**: Respect dynamic design tokens (`ColorBackground`, `ColorSurface`, `ColorPrimary`, etc.), Monospace typography for telemetry numbers, and strict button rules defined in `ui_contract.md`.
- **USB Authorization**: Auto-authorized transparently via `res/xml/device_filter.xml` (u-blox VID 5446, FTDI, CP210x, CH340, Prolific). Do NOT trigger manual `requestPermission()` popups.
- **Demo Mode**: Starts 5 meters before pit exit on `pit_lane` trajectory without triggering a false-positive initial pit overlay.
- **Database & Offline Resilience**: Firebase Realtime Database REST/WebSocket client with local disk persistence (`setPersistenceEnabled(true)`), anonymous authentication (`signInAnonymously()`), unique installation `deviceUid` keys to prevent race conditions, and `TelemetrySyncWorker` background queue flushing.
- Keep `minSdk 24`, `compileSdk 34`, `applicationId`, Java 17 / Kotlin 1.9 compatibility, and dependencies clean.

---

## Directiva d'Investigació i Benchmarking Open-Source (GitHub Expert Mode)

Per implementar noves funcionalitats, corregir bugs de sincronització o dissenyar components de la Pit Wall web:

1. **Benchmarking de Repositoris Experts a GitHub:**
    - Abans de desenvolupar des de zero o aplicar solucions trivials, investiga com ho han resolt desenvolupadors experts en projectes d'alta matriu de motorsport i telemetria (ex: SimHub Web dashboards, RaceCapture/TrackMetrix, OpenPilot, ChronoGPS, dashboards de telemetria en temps real).
    - Analitza les seves distribucions d'interfície (UX/UI d'alta densitat d'informació), selecció i optimització de visualització de dades, i gestió de reconexions.

2. **Còpia i Adopció de Patrons Avançats:**
    - Adapta els millors patrons visuals i estructurals d'aquests repositoris (estil de taulers de boxes, control de stints, selectors de pilots, grafs de telemetria i estat de connexió), mantenint sempre els tokens de disseny de `DISENO.md` i `ui_contract.md`.
    - Copia i aplica architectures reactives de synchronization per evitar condicions de cursa (race conditions), bucles de renderitzat o eliminacions involuntàries d'estat a la Pit Wall web.

3. **Arquitectura d'Estat Robust:**
    - Estudia la gestió d'estat optimista (optimistic updates), bloquejos temporals de UI i updates atòmics multipath utilitzats en projectes professionals en lloc de sobrescriure rutes directes de la base de dades.