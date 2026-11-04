# Lap Timer · Version 2.2.0

High-precision lap timer for circuit racing with native direct USB u-blox GPS telemetry (25Hz), multi-device endurance team collaboration via unified Firebase RTDB rooms (`/rooms/{roomCode}/`), anonymous authentication (`signInAnonymously`), default numeric PIN (`1234`), and web pit-wall integration.

## Key Features
1. **Core GPS Engine & Direct u-blox Integration**:
   - **Native USB Serial**: Direct connection to u-blox GNSS receivers via USB OTG cable (Vendor ID `5446`). No external Mock GPS apps required.
   - **High-Frequency NMEA Parsing**: Background parsing of `$GPRMC` and `$GPGGA` sentences feeding 25Hz telemetry seamlessly.
   - Dynamic stabilized Hz calculation and config (`ReceptorConfig`: 10Hz, 25Hz, Auto).
   - Hybrid strategy C GPS gap handling (freeze, warning, and lap invalidation).
   - Precision gate crossing detection (finish, sectors, pit lane, paddock).

2. **UI & Theme Customization**:
   - **English UI throughout the application**
   - Consistent navigation headers with back buttons across all submenus and endurance screens.
   - Reactive instant theme switching via `ThemeManager` state flow (F1 Neon Lime, Toyota GR86 Gazoo Racing, Racing Cyan / Gulf GT).
   - F1-style sector timing chips (Purple, Green, Yellow, Neutral).

3. **Pit Stop & Pit Crew Management**:
   - Progressive stopwatch timer starting from 0.0s.
   - Flashing yellow warning overlay during mandatory pit wait time.
   - Instant green compliance indicator when minimum pit stop time is met.
   - Firebase REST telemetry sync with Pit Panel.

4. **Multi-Device Endurance Sync Layer & Runtime (V2.2.0)**:
   - **Unified Room Root (`/rooms/{roomCode}/`)**: Complete migration under a single room architecture.
   - **Anonymous Authentication & Security**: Mandatory prior `signInAnonymously()` execution and `?auth=<idToken>` injection in all REST requests.
   - **Default Numeric PIN (`1234`) & Hashing**: Secure SHA-256 with salt hashing (`pinHash` and `salt`) for room access and member roles (`members/{uid}`).
   - **Simplified Room Creation & Pit-Wall Link Sharing**: Streamlined numeric PIN creation (`1234` default) and instant Pit-Wall strategy link sharing.

5. **Circuit Editor**:
   - Interactive Osmdroid map editor with Mapbox Satellite imagery and Trace overlay.
   - Gate placement for finish, sectors, pit entry/exit, and paddock area.

6. **Race Modes**:
   - Track Day (free practice)
   - Sprint (target laps/duration limit)
   - Resistencia (multi-device team roster, stint strategy, pit rules)
   - Long Run (extended solo practice with stint management)

## Project Rules
- **File Size Rule**: Target <= 200 lines per file (tolerance margin up to 220 lines max).
- **UI Language**: 100% English.
- **USB Auto-Authorization**: VID/PID filtering via `res/xml/device_filter.xml` (u-blox VID 5446, FTDI, CP210x, CH340, Prolific).
- **Architecture**: Modular structure documented in `PROJECT_MAP.md`.
