# Changelog

All notable changes to this project will be documented in this file.

## [2.6.0] - 2026-11-09

### Added
- Phase 1 Gamified Performance Metrics Engine (`Phase1PerformanceEngine.kt`):
  - Consistency Score (%), Smoothness Score (Jerk G/s), Ideal Execution Score (%), and Braking Point Consistency ($\sigma_{S_{brake}}$).
- 5th Display Layout (`viewGForce`):
  - Motorsport G-G Friction Circle Diagram (Lateral vs Longitudinal acceleration) and Vertical Z acceleration side bar with kerb/bounce progress bar.
  - Updated swipe navigation cycler (`MainNavigator.kt`) to support 5 display layouts (`% 5`).
- Settings Toggle for Gamified Metrics:
  - Added preference toggle in `SessionSettingsActivity` and `PitSettingsActivity` to enable/disable performance calculations at runtime to conserve CPU/battery.
- Pit Wall Web Dashboard Enhancements:
  - Interactive collapsible Session Lap History drawer on clicking any car in the live timing table (expanding up to 50 lines before scrolling).
  - Separate login tabs for "Connect to Team" and "Create New Team".
  - Persistent expanded history state across live telemetry refreshes (eliminating the 500ms flicker).
- High-Performance Dual-Background Network Architecture:
  - Decoupled 500ms high-frequency telemetry push executor from 1-second pit message/instruction pulling executor in `FirebaseRestLink`.

### Changed
- Upgraded major version to 2.6.0 (`versionCode = 170`, `versionName = "2.6.0"`, build number reset to `0`) across `build.gradle.kts`, `version.properties`, `Races.kt`, `HomeUiBuilder.kt`, and `MainDemoController.kt`.

## [2.5.0] - 2026-11-09

### Added
- Fourth Display Layout (`viewGpsSpeed`):
  - Added dedicated GPS & Speed Telemetry diagnostic view with giant speed display in KM/H, refresh rate in Hz, satellite count, HDOP precision, altitude, and fix status.
  - Updated swipe navigation cycler (`MainNavigator.kt`) to support 4 display layouts (`% 4`).
- Web Pit Wall Dashboard Session Persistence:
  - Implemented 24-hour persistent device session memory (`localStorage`) and Crew Chief logout controls (`🚪 Logout`).
- Dynamic Circuit Sector Rendering on Web Panel:
  - Replaced hardcoded 3-sector assumption with dynamic circuit sector count from telemetry, rendering `--.--` fallbacks for uncompleted sectors.
- Robustness & Edge Case Enhancements:
  - Automatic exponential backoff retry mechanism (up to 3 attempts) for transient REST network failures in `FirebaseRestClient`.
  - Disk write safety (`runCatching`) in `SessionStore` and `SessionRecorder`.
  - Persistent disk queue for unsynced telemetry laps in `FirebaseRestLink`.
  - Plausible speed ceiling (400 km/h) and spatial jump filter (>150m in <400ms) in `GpsFilter`.
  - 3-second pit gate debounce cooldown in `LapDetector` to prevent gate oscillation.
  - 24-hour automatic purge of inactive offline devices from Firebase rooms (`/devices/` and `/pilots/`).
  - Uncoupled FirebaseRestLink message polling from `settings.enabled` state so pit instructions and flags (`GREEN`, `YELLOW`, `RED`, `BLUE`, `FINISH`) always arrive instantly on all driving screens.

### Changed
- Upgraded major version to 2.5.0 (`versionCode = 160`, `versionName = "2.5.0"`, build number reset to `0`) across `build.gradle.kts`, `version.properties`, `Races.kt`, `HomeUiBuilder.kt`, and `MainDemoController.kt`.

## [2.4.0] - 2026-11-08

### Added
- Official Data Contract Migration (`/rooms/{roomCode}/devices/{deviceUid}/`):
  - Created isolated, atomic hardware device routes under `/rooms/{roomCode}/devices/{deviceUid}/` storing `deviceName`, `status` ("driving", "pit", "offline"), and `lastSeenTs`.
- Unique Installation Device Identity (`deviceUid`):
  - Enforced unique, installation-persistent device identity (`device-{MODEL}-{UUID}`) in `NetSettings.kt` and ViewModels (`EnduranceLobbyViewModel`, `PilotSessionViewModel`), eliminating device UID collisions across emulators and multiple hardware units.
- Real-Time Firebase Presence & `onDisconnect()` Synchronization:
  - Integrated `FirebaseDatabase` `.onDisconnect().setValue("offline")` for instant server-side presence updates upon sudden network drops or airplane mode activations.
  - Added atomic device status updates via `updateDeviceStatusAtomic` on `Dispatchers.IO`.
- Web Dashboard Presence Integration (`pit-panel/index.html`):
  - Updated JS listeners for `/rooms/{teamName}/devices` and `/pilots`.
  - Added dynamic status light indicators (Green/Emerald for Online/Driving, Yellow for Pit, Red/Grey for Offline/Disconnected or stale `lastSeenTs`).
- Endurance Cockpit UI Refinements (`PilotSessionActivity.kt`):
  - Completely removed top tactical message banner from standby cockpit view.
  - Implemented dynamic countdown timer with green background (`#16A34A`) counting down stint time to kart entry (holding at `00:00:00` at zero).
  - Prominent "I'M IN THE CAR / READY TO DRIVE" button launching driving layout (`MainActivity`) with pitlane banner active (`pitlaneActive = true`).
  - Out-of-car exit procedure with "I'VE EXITED THE VEHICLE" confirmation button transitioning screen to red resting/finished state (`#EF4444`).
- Multi-GPS Source Fixes (`MainPermissionsHandler.kt` & `GpsLocationManager.kt`):
  - Fixed internal phone GPS and external Bluetooth GPS sources (RaceBox, Dragy, Garmin) to prevent false "No GPS signal" alerts and lap invalidations caused by USB serial checks.
  - Added resilient provider selection (`GPS_PROVIDER`, `NETWORK_PROVIDER`) with safe exception fallback.
- Robust JSON Safe Parsing (`EnduranceSyncExtensions.kt`):
  - Protected all network response JSON parsers with inner `try/catch` and `optJSONObject` to handle non-JSON / HTML gateway error payloads gracefully.

### Changed
- Upgraded version to 2.4.0 (`versionCode = 125`, `versionName = "2.4.0"`) in `build.gradle.kts`, `version.properties`, `Races.kt`, `HomeUiBuilder.kt`, and `MainDemoController.kt`.

## [2.3.0] - 2026-11-06

### Added
- Complete hybrid architecture integration: Android Native (Kotlin), Firebase Realtime Database, and Pit Wall Web Dashboard (`pit-panel/index.html`).
- Refactored Battery Monitoring & Time-to-Empty (`BatteryEstimator.kt`, `StatusBarsRenderer.kt`, `MainPermissionsHandler.kt`):
  - Dynamic discharge rate calculation (% per minute and mA instantaneous draw via `BatteryManager`).
  - Time-to-Empty display beside the battery icon: `"4h 20m remaining"` or `"--"` when plugged in / charging.
  - Charging-state suppression: completely suppresses battery warning/critical alerts if the device is connected to external power.
  - Strict Monospace typography and 100% English wording across status bar and alert strip.
- Refactored Settings screen (`SessionSettingsActivity` and `PitSettingsActivity`) and introduced `SettingsViewModel`:
  - Replaced legacy 10-character random codes with validated Team/Session Name (6-15 chars, alphanumeric with spaces) and numeric Access PIN (4-6 digits).
  - Pre-save validation via `ValidationUtils` with field-level inline error cues.
  - Secure cryptographic PIN hashing (`SecurityUtils.hashPin`) producing `pinHash` and `salt`.
  - Automatic Firebase team synchronization to `/teams/{teamName}` with `pinHash` and `salt` (never plain-text credentials).
- Pit Wall Web Dashboard built with Tailwind CSS: Crew Chief authentication supporting concurrent sessions, real-time driver request management (Aceptar/Rechazar), tactical instruction broadcasting to cockpit displays, and global circuit repository viewer.
- Strict input validation in `ValidationUtils`: regex validation for team identifiers (`^[a-zA-Z0-9 ]{6,15}$`) and numeric PINs (`^[0-9]{4,6}$`).
- Secure cryptographic PIN hashing in `SecurityUtils`: 16-byte cryptographically secure random salt and SHA-256 hash generation and verification (`pinHash` and `salt`).
- Robust request synchronization via `deviceUid` under `/rooms/{teamName}/requests/{deviceUid}` and `/pending/{deviceUid}` to prevent duplicate requests and race conditions between joining drivers.
- Comprehensive Firebase Security Rules protecting `/teams`, `/rooms/{teamName}`, `/requests/{deviceUid}`, and `/instructions`.
- Full offline resilience via `setPersistenceEnabled(true)` in `LapTimerApplication` and background queue synchronization with `TelemetrySyncWorker`.

### Changed
- Upgraded version to 2.3.0 (`versionCode = 70`, `versionName = "2.3.0"`) in `build.gradle.kts`, `version.properties`, `Races.kt`, and `MainDemoController.kt`.
- Cleaned build configuration by removing redundant empty `app/build.gradle`.

## [2.2.0] - 2025-03-08
