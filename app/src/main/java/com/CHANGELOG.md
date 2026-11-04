# Changelog

All notable changes to this project will be documented in this file.

## [2.1.0] - 2026-03-06

### Added
- Native USB Serial u-blox GNSS receiver integration (25Hz NMEA parsing in background, eliminating all external Mock GPS app dependencies).
- Team Leader Pit-Wall link sharing and copying feature for external pit wall monitors/computers.
- Consistent navigation headers ("BACK" button) across all Endurance submenus and lobby screens.
- Redesigned circuit selector dropdown in the "Create Endurance Team" view with clean labeling ("Circuit") and dark theme consistency.

### Changed
- Simplified team creation form: removed redundancy between team code and random PIN, replacing them with a single manual fixed Team PIN/Code field.
- Refactored race modes into explicit individual categories (`trackday`, `sprint`, `resistencia`, `long_run`).
- Updated telemetry and lap/race payloads with `"version": "V2.1"`, `"zone"` tracking (`track`, `pit`, `paddock`), `sectorsComplete`, and object-indexed `laps` structure.

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
