# PROJECT_MAP.md — LapTimer v2.2.0

## Architecture Overview
The project follows a modular, single-responsibility architecture across clean subpackages:

1. **Core Engine (`engine/`, root)** - GPS filtering, lap detection, sector calculation, reference trace, delta calculation, GPS gap handling, and core models.
2. **Session Logic (`session/`)** - Session lifecycle, GPS binding, recording, event dispatching, pit overlay, and separated renderers.
3. **Application Logic (`logic/`)** - Navigation, UI state, reactive theme management, race targets, demo simulation, and reconnaissance.
4. **Results (`results/`, root)** - Unified `ResultsActivity` with tabs: `HistoryTab`, `RecordsTab`, `StatsTab` (plus deprecated legacy activities).
5. **Endurance Runtime & Sync (`endurance/`)** - Multi-device Endurance lobby flow, pilot session timing, leader dashboard, driver swap dialog, strategy editor, models, and unified Firebase RTDB sync under `/rooms/{roomCode}/`.
6. **Editor (`editor/`)** - Circuit editing, gestures, gate rendering, toolbar, dialogs, and track saving.
7. **Network, Hardware & Firebase (`net/`, `firebase/`, root)** - Network settings, USB auto-authorization, REST client, anonymous auth (`FirebaseAuthManager`), hash utilities, payload serialization, and `database.rules.json`.
8. **UI Components (`ui/`, root)** - View setup, controller wiring, custom views (`TrackMapView`, `DeltaBarView`, `LineChartView`, `PageRenderView`).

## Key Modules by Subpackage

### Core Engine (`engine/`, root)
- `LapEngine.kt` - Orchestrates lap timing, sectors, and live delta integrated with `GapHandler` and `DeltaCalculator`.
- `EngineModels.kt` - Core data structures including `gapState` and `gapFlag`.
- `LapDetector.kt` - Gate crossing detection algorithm for finish, sectors, and pit lane.
- `GpsFilter.kt` - 25Hz GPS time filtering and smoothing.
- `SectorCalculator.kt` - F1-style sector color determination and optimal lap computation.
- `ReferenceLap.kt` - Reference trace matching for live delta.
- `DeltaCalculator.kt` - Live delta time calculation and exponential smoothing.
- `GapHandler.kt` - Hybrid strategy C for GPS signal loss (freeze, warning, and invalidation).

### Session Management (`session/`)
- `SessionLifecycle.kt` - Session start/stop/pause, pit entry/exit, and circuit selection.
- `SessionGpsBinding.kt` - Binds GPS position fixes to `LapEngine` and calculates dynamic, stabilized Hz.
- `SessionPit.kt` - Pit lane overlay, progressive stopwatch timer, yellow warning flashing, green compliance.
- `SessionEvents.kt` - Event handling for driver swaps and pit messages.
- `SessionRendering.kt` - Module reference pointer for session rendering.
- `StatusBarsRenderer.kt` - Status bars rendering (GPS, Net, top bar, bottom bar, stint strip).
- `TelemetryRenderer.kt` - Telemetry rendering (lap times, delta bar, sector chips, display reset).
- `SessionRecording.kt` - Coordinates raw trace recording.

### Application Logic (`logic/`)
- `MainSessionController.kt` - Main session coordinator.
- `MainNavigator.kt` - Navigation routing supporting new Solo, Endurance, Results, and Settings flows.
- `MainPermissionsHandler.kt` - USB/GPS connectivity status and alerts.
- `MainThemeController.kt` & `ThemeManager.kt` - Reactive theme palette application and text styling via StateFlow.
- `MainUiState.kt` - UI state models and stint health status.
- `MainEventBus.kt` - Event dispatching bus between components.
- `MainRaceController.kt` - Sprint and Endurance race target management.
- `MainDemoController.kt` - 25Hz virtual simulator source and telemetry formatting.
- `MainReconController.kt` - Circuit reconnaissance and drafting.
- `ModePageActivity.kt` - Solo mode setup (Track day, Sprint, Long Run).

### Results (`results/`, root)
- `ResultsActivity.kt` - Host activity with 3 tab buttons ("My Sessions" | "Best Laps" | "Stats"), supporting `EXTRA_TAB`.
- `HistoryTab.kt` - Saved sessions list, details click, and deletion dialogs.
- `RecordsTab.kt` - Circuit & driver filter spinners with `LineChartView` best lap evolution.
- `StatsTab.kt` - Overall records, totals, and optimal lap time summary.
- `HistoryActivity.kt` - [DEPRECATED - merged into ResultsActivity].
- `RecordsActivity.kt` - [DEPRECATED - merged into ResultsActivity].

### Endurance Multi-Device Sync & Runtime (`endurance/`)
- **Lobby (`endurance/lobby/`)**:
  - `EnduranceLobbyActivity.kt` - Host Activity managing lobby steps.
  - `RoleSelectFragment.kt` - Role selection ("Team Leader" vs "Driver/Pilot").
  - `CreateTeamFragment.kt` - Leader team creation, circuit selection, and numeric PIN default (`1234`, max 6 digits).
  - `JoinTeamFragment.kt` - Driver team join with team code and numeric PIN validation.
  - `WaitingRoomFragment.kt` - Real-time connected drivers list, PIN display (Leader only), and "Start Session" button.
  - `EnduranceLobbyViewModel.kt` - StateFlow-based ViewModel orchestrating lobby flow with anonymous auth.
- **Pilot Runtime (`endurance/pilot/`)**:
  - `PilotSessionActivity.kt` - Driver timing UI, ON_TRACK/RESTING status cards, GPS loss & offline indicators, and unexpected movement alerts.
  - `PilotSessionViewModel.kt` - ViewModel observing session state, current pilot status, and lap queueing.
  - `PilotEngineBridge.kt` - Bridge connecting local `LapEngine` & GPS fixes to `PilotSessionViewModel`.
- **Leader Runtime (`endurance/leader/`)**:
  - `LeaderDashboardActivity.kt` - Team Leader dashboard UI displaying current driver, team stats, driver list, and "SWAP DRIVER" action.
  - `LeaderDashboardViewModel.kt` - ViewModel managing driver swaps, stint creation, and strategy updates.
  - `PilotChangeDialog.kt` - Modal dialog to perform driver swaps.
  - `StrategyEditorDialog.kt` - Modal dialog to configure and push stint strategy parameters.
- **Sync & Data (`endurance/sync/`)**:
  - `EnduranceModels.kt` & `EnduranceModelMappers.kt` - TeamMeta (with pinHash, salt, leaderUid), MemberInfo, StrategyBase, SessionState, PilotInfo, LapRecord, Stint.
  - `EnduranceSyncClient.kt` & `EnduranceRepository.kt` - REST client with `withContext(Dispatchers.IO)` and anonymous auth token injection (`?auth=<idToken>`).


### Editor (`editor/`)
- `MapEditorActivity.kt` - Osmdroid map editor lifecycle and root layout.
- `MapEditorViewModel.kt` - Circuit points, sector ordering, pit gates, paddock polygon, and undo logic.
- `EditorGestureHandler.kt` - Touch gestures, gate dragging, and width dialogs.
- `EditorToolbar.kt` - Bottom button rows, hints setup, and Mapbox Satellite layer toggle FAB.
- `MapboxTileSourceHelper.kt` - Custom Mapbox Satellite tile source definition and layer toggle helper.
- `TraceOverlayRenderer.kt` - High-performance 25Hz GPS reference trace polyline renderer.
- `GateRenderer.kt` - Gate lines, arrows, markers, and handle overlays.
- `TrackSaver.kt` - Circuit validation and `CircuitStore` persistence.

### Network, Hardware & Data Layer (`net/`, `firebase/`, root)
- `LapTimerApplication.kt` - Main Application class ensuring early FirebaseApp initialization.
- `FirebaseAuthManager.kt` - Prior anonymous Firebase Auth (`signInAnonymously()`) and ID token retrieval.
- `HashUtils.kt` - SHA-256 with salt hashing for secure PIN verification.
- `HomeUiBuilder.kt` - Main menu with 4 primary entries: Solo / Endurance / Results / Settings.
- `UsbSerialManager.kt` - Transparent USB auto-authorization by u-blox VID/PID.
- `ReceptorConfig.kt` - Receptor model configuration (10Hz/25Hz/Auto).
- `NetSettings.kt` & `NetSettingsModel.kt` & `NetSettingsStore.kt` - Preferences, car name, driver name, strategy.
- `FirebaseRestLink.kt` & `FirebaseRestClient.kt` & `FirebasePayload.kt` - REST telemetry and command sync with `?auth=<idToken>`.
- `CircuitStore.kt` & `SessionStore.kt` & `RaceStore.kt` & `StatsStore.kt` - Local JSON & binary trace persistence.

## Project Conventions & Rules
- **UI Contract**: Design system and UI conventions strictly defined in `ui_contract.md`.
- **File Size Rule**: Target <= 200 lines per file (tolerance margin up to 220 lines max).
- **UI Language**: All user interface text must be in **English**.
- **Main Menu Entries**: Solo, Endurance, Results, Settings (plus Demo Mode and Circuits).
- **No Manual USB Permission Dialog**: Transparent auto-authorization via `res/xml/device_filter.xml` (u-blox VID 5446, FTDI, CP210x, CH340, Prolific).
