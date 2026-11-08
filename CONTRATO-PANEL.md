# Contrato Técnico: Comunicación App Android ⇄ Panel Pit Web (`CONTRATO-PANEL.md` - V2.4.0)

Este documento define el protocolo entre la app Android `LapTimer` (V2.4.0) y el panel pit web (Pit Wall): rutas de Firebase Realtime Database, estructura de datos JSON, ciclos de vida, sincronización offline, autenticación anónima, validaciones estrictas y roles. El panel debe implementarse estrictamente según este contrato.

---

## 1. Versión

- **Versión de la app:** `"V2.4.0"` (`versionName` en `app/build.gradle.kts`).
- **Campo de versión:** los payloads `live`, `laps` y `races` incluyen obligatoriamente `"version": "V2.4.0"`.
- **Independencia del panel:** el panel web (`pit-panel/index.html`) opera sobre este contrato de sincronización.

---

## 2. Firebase

### 2.1 Raíz y compatibilidad

- **URL de Realtime Database:** `https://laptimer-pit-default-rtdb.europe-west1.firebasedatabase.app` (configurable en `NetSettings.dbUrl`).
- **Raíz principal:** `/rooms/{roomCode}/` (donde `roomCode` coincide con el identificador del equipo en mayúsculas).
- **Compatibilidad histórica:** el panel web soporta autenticación dual contra `/rooms/{roomCode}/meta` y fallback a `/teams/{teamCode}` para admitir múltiples sesiones concurrentes de Crew Chief.

### 2.2 Autenticación y Seguridad

- **Método:** autenticación anónima de Firebase Auth (`signInAnonymously()`), tanto en la app como en el panel web, inicializada tempranamente mediante `LapTimerApplication`.
- **Peticiones REST:** cada petición añade el token de acceso como parámetro `?auth=<idToken>` (obtenido mediante `getIdToken(true)`).
- **Validación de Identificador de Equipo (`ValidationUtils`):**
  - Regex estricta: `^[a-zA-Z0-9 ]{6,15}$` (6 a 15 caracteres alfanuméricos y espacios, sin caracteres especiales ni símbolos).
- **Validación de PIN (`ValidationUtils`):**
  - Regex estricta: `^[0-9]{4,6}$` (4 a 6 dígitos numéricos).
- **Criptografía (`SecurityUtils`):**
  - Sal aleatoria de 16 bytes generada con `SecureRandom`.
  - Hash seguro calculado con `SHA-256(salt + pin.trim())` almacenado en `meta/pinHash` y `meta/salt`.

### 2.3 Rutas y Permisos

| Ruta | Escritura | Lectura | Propósito |
| --- | --- | --- | --- |
| `meta` | App (al crear la sala) | Panel y app | Metadatos de la sala, `leaderUid`, `pinHash` y `salt` |
| `devices/{deviceUid}` | App / Servidor | Panel y app | Estado en tiempo real del hardware (`deviceName`, `status`, `lastSeenTs`) |
| `members/{uid}` | App y panel (al unirse) | Panel y app | Miembros activos con rol (`pilot` o `crew_chief`) |
| `requests/{deviceUid}` | App (piloto) / Panel (jefe) | Panel y app | Solicitudes de acceso indexadas por `deviceUid` |
| `pending/{deviceUid}` | App (piloto) / Panel (jefe) | Panel y app | Cola de solicitudes pendientes de aprobación |
| `instructions` | Panel (crew_chief) | App (piloto) | Instrucciones tácticas en directo para el cockpit |
| `session` | App | Panel | Estado de la sesión (tiempos, banderas) |
| `strategy` | App / Panel | Panel y app | Configuración de stints y paradas en boxes |
| `pilots/{pilotId}` | App / Panel | Panel y app | Estado del piloto (`READY`, `ON_TRACK`, `RESTING`) |
| `stints/{stintId}` | App | Panel | Historial y planificación de stints |
| `live/{carId}` | App (periódico) | Panel | Telemetría en vivo del vehículo |
| `laps/{carId}/{lapKey}` | App (vuelta hecha) | Panel | Vueltas completadas con parciales de sector |
| `inbox/{carId}/{messageKey}` | Panel (crew_chief) | App | Mensajes dirigidos a un coche específico |
| `cmd/{carId}` | Panel (crew_chief) | App | Comandos de cambio de piloto o parada |
| `races/{carId}/{raceId}` | App (fin carrera) | Panel | Resumen general de la carrera |

---

## 3. Estructura de Datos (JSON)

### A. Solicitudes de Pilotos (`requests/{deviceUid}`)

Para evitar condiciones de carrera y duplicidades entre múltiples dispositivos, cada solicitud se indexa por el `deviceUid` único del dispositivo:
- **Ruta:** `/rooms/{roomCode}/requests/{deviceUid}`
- **Campos:**
  - `id` (String): ID de piloto.
  - `name` (String): Nombre del piloto.
  - `uid` (String): `deviceUid` autenticado.
  - `status` (String): `"PENDING"`, `"ACCEPTED"` o `"REJECTED"`.
  - `requestedAt` (Long): Timestamp en milisegundos.
- **Ejemplo:**
```json
{
  "id": "driver_42",
  "name": "Carlos Sainz",
  "uid": "q8A7s6D5f4G3h2J1k",
  "status": "PENDING",
  "requestedAt": 1772745600000
}
```

### B. Instrucciones Tácticas del Pit (`instructions`)

- **Ruta:** `/rooms/{roomCode}/instructions` (método: `PUT` / `set`)
- **Campos:**
  - `text` / `message` (String): Mensaje táctico para la cabina (ej. `"Box esta vuelta / Ahorrar neumáticos"`).
  - `timestamp` / `t` (Long): Timestamp del envío.

### C. Estado en Vivo del Coche (`live/{carId}`)

- **Ruta:** `/rooms/{roomCode}/live/{carId}`
- **Campos:**
  - `version` (String): `"V2.4.0"`.
  - `car` (String): Nombre del coche (ej. `"GR86"`).
  - `driver` (String): Nombre del piloto actual.
  - `zone` (String): `"track"`, `"pit"` o `"paddock"`.
  - `lat` (Double), `lon` (Double), `kmh` (Int), `lap` (Int), `timeMs` (Long), `bestMs` (Long, nullable), `bat` (Int, porcentaje batería 0-100), `stint` (Int), `t` (Long).
- **Ejemplo:**
```json
{
  "version": "V2.4.0",
  "car": "GR86",
  "driver": "Driver 1",
  "zone": "track",
  "lat": 41.3851,
  "lon": 2.1734,
  "kmh": 125,
  "lap": 4,
  "timeMs": 74210,
  "bestMs": 72000,
  "bat": 88,
  "stint": 1,
  "t": 1772745600000
}
```

### D. Registro de Vuelta (`laps/{carId}/{lapKey}`)

- **Ruta:** `/rooms/{roomCode}/laps/{carId}/{lapKey}` (`lapKey` = `"L" + timestamp`)
- **Campos:**
  - `version` (String): `"V2.4.0"`.
  - `n` (Int), `ms` (Long), `k` (Int), `d` (String), `st` (Int), `sectorsComplete` (Boolean), `sectors` (Object), `t` (Long).
- **Ejemplo:**
```json
{
  "version": "V2.4.0",
  "n": 3,
  "ms": 72450,
  "k": 0,
  "d": "Driver 1",
  "st": 1,
  "sectorsComplete": true,
  "sectors": { "1": 24100, "2": 29800, "3": 18550 },
  "t": 1772745672450
}
```

---

## 4. Resiliencia y Comportamiento Offline

1. **Persistencia Local (`setPersistenceEnabled(true)`):** Habilitada globalmente en `LapTimerApplication`. Las consultas y escrituras se confirman en disco local antes de enviarse al socket.
2. **Cola de Vueltas (`pendingLaps`):** Encolado thread-safe en memoria (hasta 300 vueltas).
3. **Flushing Automático:** Sincronización transparente en segundo plano mediante `TelemetrySyncWorker` cuando el dispositivo recupera conexión de datos tras atravesar zonas de sombra de cobertura.
4. **Panel Web (.info/connected):** El panel pit detecta caídas y reconexiones en tiempo real actualizando dinámicamente el badge de estado y rearmando los listeners sin requerir recargar la página.
