# Changelog

All notable changes to this project will be documented in this file.

## [2.2.0] - 2025-03-08

### Added
- Unified Firebase RTDB architecture under `/rooms/{roomCode}/...`.
- Mandatory anonymous authentication (`signInAnonymously()`) and REST token injection (`?auth=<idToken>`).
- Secure SHA-256 with salt PIN hashing (`pinHash`, `salt`) and member roles registration under `members/{uid}` (`crew_chief` vs `pilot`).
- Simplified numeric PIN (`1234` default) for Endurance room creation with restricted numeric keyboard and 6-digit max limit.
- Early `FirebaseApp` initialization in `LapTimerApplication` and built-in programmatic fallback credentials.

## [2.0.0] - 2025-03-05

### Added
- Multi-device Endurance mode: each pilot has their own app, coordinated by a Team Leader.
- Real-time synchronization via Firebase Realtime Database with offline persistence and queue flushing.
- Team lobby flow with short team code and 4-digit verbal PIN authorization.
- Team Leader Dashboard with live driver swap actions and strategy editor.
- Hybrid strategy C GPS gap handling (freeze, warning, and lap invalidation on signal loss).
- Automatic USB auto-authorization via u-blox VID/PID filtering and background reconnection.
- Reactive theme switching (instant palette updates across all open views via StateFlow).
- Results view: unified History + Records + Stats into 3 tabs (`ResultsActivity`).
- Receptor configuration support (`ReceptorConfig`: 10Hz, 25Hz, Auto).
- i18n: 100% of user-facing UI text in English.

### Changed
- Main menu streamlined to 4 entries: Solo / Endurance / Results / Settings.
- Refactored `SessionRendering.kt` into `StatusBarsRenderer.kt` and `TelemetryRenderer.kt`.
- Refactored `LapEngine.kt` by extracting `DeltaCalculator` and `GapHandler`.
- Updated GPS loss banner text to "No GPS signal".

### Removed
- `HistoryActivity.kt` (merged into `ResultsActivity`).
- `RecordsActivity.kt` (merged into `ResultsActivity`).

### Fixed
- Bug: Theme changes did not apply instantly without restart.
- Bug: USB reconnection required manual prompt authorization while driving.
- Bug: GPS signal loss corrupted lap timing data.

## [1.1.0] - 2025-02-15
- Initial high-precision lap timing engine, Osmdroid circuit map editor, and Firebase REST telemetry sync.
