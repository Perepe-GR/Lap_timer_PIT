# Instructions for Copilot & AI Agents

## Project Overview
Android app (Kotlin, XML views, no Compose) for high-precision lap timing and telemetry, version 1.2.
Supports external USB 25 Hz u-blox GPS receiver (`usb-serial-for-android`), internal GPS, RaceBox, Dragy, and Garmin.
Horizontal screen layout for vehicle dashboard mounting.

## Language
UI text in **English**. Documentation/comments in English or Catalan/Spanish.

## Subpackage Architecture (`app/src/main/java/com/ejemplo/laptimer`)
- `engine/`: `LapEngine.kt`, `LapDetector.kt`, `GpsFilter.kt`, `SectorCalculator.kt`, `ReferenceLap.kt`, `EngineModels.kt`
- `session/`: `SessionLifecycle.kt`, `SessionGpsBinding.kt`, `SessionPit.kt`, `SessionEvents.kt`, `SessionRendering.kt`, `SessionRecording.kt`, `SessionStore.kt`, `SessionRecord.kt`, `Trace.kt`
- `logic/`: `MainSessionController.kt`, `MainNavigator.kt`, `MainPermissionsHandler.kt`, `MainThemeController.kt`, `MainUiState.kt`, `MainEventBus.kt`, `MainRaceController.kt`, `MainDemoController.kt`, `MainReconController.kt`
- `editor/`: `MapEditorActivity.kt`, `MapEditorViewModel.kt`, `EditorGestureHandler.kt`, `EditorToolbar.kt`, `GateRenderer.kt`, `EditorDialogs.kt`, `TrackSaver.kt`
- `net/` & `firebase/`: `NetSettings.kt`, `NetSettingsModel.kt`, `NetSettingsStore.kt`, `FirebaseRestLink.kt`, `FirebaseRestClient.kt`, `FirebasePayload.kt`
- `ui/` & root: `MainActivity.kt`, `MainViewSetup.kt`, `MainControllerWiring.kt`, `HomeActivity.kt`, `HomeUiBuilder.kt`, `CircuitsActivity.kt`, `ModePageActivity.kt`, `EnduranceActivity.kt`, `StrategyActivity.kt`, `BackupActivity.kt`, `RacesActivity.kt`, `HistoryActivity.kt`, `AnalysisActivity.kt`

## Key Rules
- **File Size Rule**: Target <= 200 lines per file (tolerance margin up to 220 lines max). Split files whenever technically sensible.
- **UI Text**: All user-facing UI text MUST be in **English**.
- **USB Authorization**: Auto-authorized via `res/xml/device_filter.xml` (u-blox VID 5446, FTDI, CP210x, CH340, Prolific). No manual `requestPermission()` popups.
- **Demo Mode**: Starts 5 meters before pit exit on `pit_lane` trajectory without triggering false positive initial pit overlay.
- Keep `minSdk 24`, `applicationId`, and dependencies clean. REST connection for Firebase (no heavy SDK).
