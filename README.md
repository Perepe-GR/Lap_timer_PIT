# Lap Timer · Version 2.4.0

High-precision lap timing, telemetry, and endurance racing management system for circuit motorsport. Integrates native 25Hz direct USB u-blox GNSS hardware, an Android application (Kotlin), a Firebase Realtime Database backend with offline persistence, and a real-time responsive Pit Wall Web Dashboard (`index.html`) for the Crew Chief.

---

## 1. General Architecture & Ecosystem

LapTimer is engineered as a hybrid distributed racing ecosystem composed of three synchronized layers:

```
┌─────────────────────────────────────────────────────────────┐
│                   LapTimer Ecosystem v2.4.0                 │
└─────────────────────────────────────────────────────────────┘
                               │
       ┌───────────────────────┼───────────────────────┐
       ▼                       ▼                       ▼
┌──────────────┐      ┌─────────────────┐     ┌────────────────┐
│ Android App  │◄────►│ Firebase RTDB   │◄───►│  Pit Wall Web  │
│ (Pilot/Solo) │      │  Cloud Backend  │     │  (Crew Chief)  │
└──────────────┘      └─────────────────┘     └────────────────┘
       ▲                       ▲
       │                       │
┌──────────────┐      ┌─────────────────┐
│ u-blox GNSS  │      │ Offline Cache   │
│ 25Hz USB OTG │      │ (Persistence)   │
└──────────────┘      └─────────────────┘
```

1. **Native Android Application (`app`)**:
   - **Languages & Frameworks**: Kotlin, Coroutines, StateFlow, Android Jetpack (ViewModel, WorkManager), Osmdroid.
   - **High-Frequency GPS Engine**: Direct USB Serial OTG communication with u-blox GNSS receivers (Vendor ID `5446`) at 25Hz. Native background NMEA parser (`$GPRMC`, `$GPGGA`) with dynamic stabilized Hz calculation, hybrid Strategy C gap handling (freeze, warning, lap invalidation), and sub-meter gate crossing interpolation.
   - **Solo Mode**: Track Day (free practice), Sprint (target laps/duration), and Long Run (stint pacing and tire conservation).
   - **Endurance Mode**: Multi-device team coordination. Drivers operate dedicated cockpit units displaying live delta (±2%), F1-style sector chips, stint countdowns, pit lane timing overlays, and incoming pit wall instructions.
2. **Backend Realtime Layer (Firebase Realtime Database & Auth)**:
   - Central cloud hub at `https://laptimer-pit-default-rtdb.europe-west1.firebasedatabase.app`.
   - Unified room model (`/rooms/{teamName}/...`) and legacy compatibility paths (`/teams/{teamName}/...`).
   - Anonymous authentication (`signInAnonymously()`) with automatic token refresh (`?auth=<idToken>`) for REST endpoints and native WebSockets.
3. **Pit Wall Web Dashboard (`pit-panel/index.html`)**:
   - Responsive telemetry and strategy monitoring console tailored for Crew Chiefs and pit engineers.
   - Built with modern HTML5, Tailwind CSS, and the Firebase JavaScript SDK.
   - Operates on any browser (tablets, laptops, pit-wall monitors) with zero local installation.

---

## 2. Authentication, Access Control & Security

LapTimer implements a robust security and access control mechanism ensuring multi-tenant isolation, anti-tampering, and race-condition immunity:

### 2.1 Team Access & Identifier Validation
- **Team Identifier (`teamName`)**: Validated via strict regular expression:
  ```regex
  ^[a-zA-Z0-9 ]{6,15}$
  ```
  Enforces an alphanumeric string between 6 and 15 characters (allowing internal single spaces), blocking SQL/NoSQL injection tokens, path traversal symbols (`/`, `.`, `#`, `$`, `[`, `]`), and non-printable characters.
- **Access PIN (`pin`)**: Validated via strict numeric regular expression:
  ```regex
  ^[0-9]{4,6}$
  ```
  Requires a strictly numeric PIN of 4 to 6 digits (default `1234`).

### 2.2 Cryptographic PIN Hashing (`pinHash` & `salt`)
Credentials are never stored or transmitted in plain text:
- **Salt Generation**: 16 cryptographically secure random bytes generated via `java.security.SecureRandom`, hex-encoded into a 32-character string.
- **Hash Computation**: Computed using `SHA-256(salt + pin.trim())`.
- **Verification**: Evaluated via constant-time string comparison in `SecurityUtils.verifyPin()`. Stored under `/rooms/{teamName}/meta/pinHash` and `/rooms/{teamName}/meta/salt`.

### 2.3 Firebase Security Rules & Concurrency Control (`deviceUid`)
Firebase Realtime Database Security Rules protect room nodes from unauthorized reads and writes:
- **Unique Device Keying (`deviceUid`)**: Driver join requests are keyed strictly by their authenticated Firebase UID (`/rooms/{teamName}/requests/{deviceUid}`). This eliminates race conditions, overwrites, or ID collisions when multiple drivers attempt to join simultaneously:
  ```json
  "requests": {
    "$deviceUid": {
      ".write": "auth != null && (auth.uid == $deviceUid || root.child('rooms').child($roomCode).child('meta').child('leaderUid').val() == auth.uid)",
      ".validate": "newData.hasChildren(['id', 'name', 'uid', 'status']) && newData.child('uid').val() == $deviceUid"
    }
  }
  ```
- **Role Isolation**: Only the team leader (`leaderUid`) can update tactical instructions (`/rooms/{roomCode}/instructions`), alter stint strategies, or approve driver requests.
- **Team Privacy**: Node reads under `/teams` require active membership in `/teams/{teamCode}/members/{uid}`.

---

## 3. Resilience & Offline Handling

Circuit racing often involves severe cellular shadow zones (paddock concrete garages, remote forest sections, mountain tracks). LapTimer delivers total operational resilience:

1. **Database Offline Persistence**:
   - Initialized at application startup in `LapTimerApplication`:
     ```kotlin
     FirebaseDatabase.getInstance("https://laptimer-pit-default-rtdb.europe-west1.firebasedatabase.app")
         .setPersistenceEnabled(true)
     ```
   - Automatically caches remote state, subscriptions, and pending writes to on-device persistent disk storage.
2. **Local Queueing & Shadow Zone Buffering**:
   - Lap records are enqueued locally in `ConcurrentLinkedQueue<String>` (`pendingLaps`, buffer capacity up to 300 laps).
   - If an internet dropout occurs while on track, timing and delta operations proceed uninterrupted.
   - `TelemetrySyncWorker` (Jetpack WorkManager) periodically checks network availability and guarantees background queue flushing once connectivity is restored.
3. **Pit Wall Connection Recovery**:
   - The web dashboard continuously monitors the `.info/connected` Firebase state node.
   - Seamlessly transitions UI between **En Línea** (pulsing green indicator) and **Sin Conexión (Offline)** (red badge).
   - Automatically re-subscribes listeners and synchronizes pending driver requests and circuit data upon link restoration.

---

## 4. Pit Wall Web Dashboard (`pit-panel/index.html`)

The Crew Chief dashboard provides comprehensive pit-lane oversight and real-time interaction:

- **Crew Chief Authentication**:
  - Validates team credentials against `/rooms/{teamName}/meta` and provides automatic fallback to `/teams/{teamName}`.
  - Supports multiple concurrent Crew Chief sessions across multiple devices without session preemption.
- **Real-Time Driver Request Listener**:
  - Subscribes to `/rooms/{teamName}/requests` and `/rooms/{teamName}/pending`.
  - Dynamically renders pending driver requests showing Driver Name and Device ID.
  - Provides instant **Aceptar** (`ACCEPTED`) and **Rechazar** (`REJECTED`) controls. Approving a driver transitions their status to `READY` under `/rooms/{teamName}/pilots/{driverId}/info`.
- **Tactical Pit-to-Car Messaging**:
  - Live tactical instruction dispatch (`/rooms/{teamName}/instructions`) transmits pit orders (e.g., *"Box this lap"*, *"Conserve battery"*, *"Target +0.5s"*) directly to the driver's cockpit display with sub-second latency.
- **Global Circuit Repository**:
  - Real-time catalog synchronized from `/circuits`. Displays active circuit names and sector configurations available across the ecosystem.

---

## 5. Technical Specifications (v2.4.0)

| Specification | Value |
|---|---|
| **App Version** | `v2.4.0` (`versionName = "2.4.0"`, `versionCode = 125`) |
| **Android Target** | compileSdk 34, minSdk 24, targetSdk 34 |
| **JDK & Language** | Java 17, Kotlin 1.9.0 |
| **GPS Telemetry Rate** | 25 Hz (native u-blox USB OTG) / 10 Hz fallback / Internal GPS |
| **Supported GNSS Chips** | u-blox NEO-M8N, NEO-M9N, ZED-F9P (VID `5446`, FTDI, CP210x, CH340) |
| **Backend Database** | Firebase Realtime Database (BOM 33.1.0) |
| **Web Stack** | HTML5, Tailwind CSS, Firebase JS SDK 9.22.0 |
| **Offline Engine** | Disk Persistence (`setPersistenceEnabled`), WorkManager Queue |
| **Security Standard** | SHA-256 with 16-byte random salt, strict regex validation |
| **Architecture Rules** | Target <= 200 lines per Kotlin file (max 220 tolerance) |
| **UI Language** | 100% English across the native mobile application |
