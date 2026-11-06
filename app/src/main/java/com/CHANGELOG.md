# Changelog

All notable changes to this project will be documented in this file.

## [2.3.0] - 2026-11-06

### Added
- Hybrid Android/Web architecture documentation and Pit Wall Web Dashboard (`pit-panel/index.html`) with Tailwind CSS and live Firebase listeners.
- Settings screen and `SettingsViewModel` updated to enforce v2.3.0 security standards:
  - Eliminated legacy 10-char random room codes in favor of Team/Session Name (6-15 chars, alphanumeric) and numeric Access PIN (4-6 digits).
  - Synchronous pre-save validation via `ValidationUtils` with field error highlighting.
  - Salt and SHA-256 PIN hashing via `SecurityUtils`.
  - Secure `/teams/{teamName}` Firebase synchronization storing only `pinHash` and `salt`.
- Strict team name (`^[a-zA-Z0-9 ]{6,15}$`) and numeric PIN (`^[0-9]{4,6}$`) validation with SHA-256 + salt security.
- Driver request synchronization with unique `deviceUid` keys to eliminate concurrency conditions and race conditions.
- Offline persistence configuration (`setPersistenceEnabled(true)`) and automatic background queue recovery.

### Changed
- Version updated to 2.3.0 (`versionCode = 70`, `versionName = "2.3.0"`).

## [2.1.0] - 2026-03-06
