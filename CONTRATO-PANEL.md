# Contrato Técnico: Comunicación App Android ⇄ Panel Pit Web (`CONTRATO-PANEL.md` - V2.2.0)

Este documento define el protocolo entre la app Android `LapTimer` (V2.2.0) y el panel pit web (Pit Wall): rutas de Firebase Realtime Database, estructura de datos JSON, ciclos de vida, modos de carrera, autenticación anónima y roles. El panel debe implementarse estrictamente según este contrato.

---

## 1. Versión

- **Versión de la app:** `"V2.2.0"` (`versionName` en `app/build.gradle.kts`).
- **Campo de versión:** los payloads `live`, `laps` y `races` incluyen obligatoriamente `"version": "V2.2.0"`.
- **Independencia del panel:** el panel web tiene su propia versión; no tiene por qué coincidir.

---

## 2. Firebase

### 2.1 Raíz única

- **URL de Realtime Database:** `https://laptimer-pit-default-rtdb.europe-west1.firebasedatabase.app` (configurable en `NetSettings.dbUrl`).
- **Raíz única:** todo cuelga de `/rooms/{roomCode}/`. El antiguo nodo `/teams/{teamCode}` queda **eliminado** y no debe usarse.
- **Identificador de sala:** `{roomCode}` es el único nombre para el código de sala en todo el documento.

### 2.2 Autenticación (modo B)

- **Método:** autenticación anónima de Firebase Auth (`signInAnonymously()`), tanto en la app como en el panel web, inicializada tempranamente mediante `LapTimerApplication`.
- **Peticiones REST:** cada petición añade el token de acceso como parámetro `?auth=<idToken>` (obtenido mediante `getIdToken(true)`).
- **Reglas:** las lecturas y escrituras bajo `/rooms/{roomCode}/` exigen `auth != null`. Las escrituras en `inbox` y `cmd` exigen además que el uid tenga rol `crew_chief` en `members`.
- **Control de acceso:** la pertenencia a la sala se valida con el PIN numérico de la sala (por defecto `1234`, sección 3.F). El PIN se valida contra un hash (`pinHash` y `salt`), nunca en texto plano.

### 2.3 Rutas (todas bajo `/rooms/{roomCode}/`)

| Ruta | Escritura | Lectura |
| --- | --- | --- |
| `meta` | App (al crear la sala) | Panel y app |
| `members/{uid}` | App y panel (al unirse) | Panel y app |
| `session` | App | Panel |
| `strategy` | App | Panel |
| `pilots/{pilotId}` | App | Panel |
| `stints/{stintId}` | App | Panel |
| `live/{carId}` | App (periódico) | Panel |
| `laps/{carId}/{lapKey}` | App (al completar vuelta) | Panel |
| `inbox/{carId}/{messageKey}` | Panel (solo crew_chief) | App |
| `cmd/{carId}` | Panel (solo crew_chief) | App |
| `races/{carId}/{raceId}` | App (al terminar la carrera) | Panel |

- **`carId`:** es la clave normalizada del coche, la misma que devuelve `StintTarget.keyOf(carName)` (por ejemplo `"gr86"`).

---

## 3. Estructura de datos (JSON)

### A. Estado en vivo del coche (`live/{carId}`)

- **Ruta:** `/rooms/{roomCode}/live/{carId}` (método HTTP: `PUT`)
- **Campos:**
  - `version` (String): `"V2.2.0"`.
  - `car` (String): nombre del coche visible (p. ej. `"GR86"`).
  - `driver` (String): nombre del piloto actual.
  - `zone` (String): `"track"`, `"pit"` o `"paddock"`.
  - `lat` (Double), `lon` (Double), `kmh` (Int), `lap` (Int), `timeMs` (Long), `bestMs` (Long, nullable), `stint` (Int), `t` (Long).
- **Ejemplo:**

```json
{
  "version": "V2.2.0",
  "car": "GR86",
  "driver": "Driver 1",
  "zone": "track",
  "lat": 41.3851,
  "lon": 2.1734,
  "kmh": 125,
  "lap": 4,
  "timeMs": 74210,
  "bestMs": 72000,
  "stint": 1,
  "t": 1772745600000
}
```

### B. Registro de vuelta (`laps/{carId}/{lapKey}`)

- **Ruta:** `/rooms/{roomCode}/laps/{carId}/{lapKey}`, donde `lapKey` es `"L" + timestamp` (método: `PUT`).
- **Campos:**
  - `version` (String): `"V2.2.0"`.
  - `n` (Int), `ms` (Long), `k` (Int), `d` (String), `st` (Int), `sectorsComplete` (Boolean), `sectors` (Object), `t` (Long).
- **Ejemplo:**

```json
{
  "version": "V2.2.0",
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

### C. Mensaje del pit (`inbox/{carId}/{messageKey}`)

- **Ruta:** `/rooms/{roomCode}/inbox/{carId}/{messageKey}`. Escribe solo el panel con rol `crew_chief`.

### D. Comando / cambio (`cmd/{carId}`)

- **Ruta:** `/rooms/{roomCode}/cmd/{carId}`. Escribe solo el panel con rol `crew_chief`.

### E. Resumen de carrera (`races/{carId}/{raceId}`)

- **Ruta:** `/rooms/{roomCode}/races/{carId}/{raceId}` (método: `PUT`). El campo `version` es `"V2.2.0"`.

### F. Sala y miembros (`meta`, `members`)

- **`meta`** (`/rooms/{roomCode}/meta`):
  - `pinHash` (String): hash SHA-256 con sal del PIN numérico de la sala (por defecto `1234`).
  - `salt` (String): sal usada en el hash.
  - `leaderUid` (String): uid anónimo del crew_chief.
  - `code` (String), `name` (String), `circuitId` (String), `mode` ("endurance").
- **`members/{uid}`** (`/rooms/{roomCode}/members/{uid}`):
  - `role` (String): `"pilot"` o `"crew_chief"`.
  - `name` (String): nombre visible.
  - `t` (Long): timestamp de unión.

---

## 4. Frecuencia y ciclo de vida

- **Telemetría (`live`):** envío periódico según `NetSettings.intervalS` (mínimo 10 s).
- **Vueltas (`laps`):** encoladas localmente (`pendingLaps`, hasta 300 elementos) y enviadas al completarse.

---

## 5. Roles y sincronización en modo Endurance

- **Roles:** dos roles en una sala de Endurance: `pilot` y `crew_chief`.
- **PIN numérico:** PIN por defecto `1234` (4-6 dígitos numéricos).
- **Un solo crew_chief:** validado mediante `meta/leaderUid`.
