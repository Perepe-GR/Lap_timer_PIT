# Changelog

All notable changes to this project will be documented in this file.

## [2.3.0] - 2026-11-06

### Added
- Complete hybrid architecture integration: Android Native (Kotlin), Firebase Realtime Database, and Pit Wall Web Dashboard (`pit-panel/index.html`).
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
