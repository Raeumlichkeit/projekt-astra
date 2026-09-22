# Project: Projekt Astra — Astronomical & Observation-Practical Expansion (P2.13–P2.17)

## Architecture
Projekt Astra is an offline-first, privacy-focused Android astronomy application built with Jetpack Compose, Kotlin Coroutines, and the `io.github.cosinekitty:astronomy:2.1.19` library.

### Module & Package Boundaries
- `de.projektastra.app`:
  - **Ephemeris & Dynamics**: `SolarSystemCatalog`, `JupiterMoonsCalculator`, `SaturnSystemCalculator`, `LunarTerminatorCalculator`, `Sgp4Propagator`.
  - **Coordinate Frames & Projections**: `SkyCoordinateFrame`, `SkyProjection`, `CelestialMeasurement`, `SkyGridRenderer`, `StarHopCatalog`.
  - **Observation & Planning**: `ObservationLogbook`, `ObservationChallenges`, `DewMonitor`, `OpenAstronomyLogExport`, `TonightRecommendations`.
  - **Camera, Sensors & Hardware**: `CameraPreview`, `SensorFilter`, `AstraTheme` (OLED mode), `MainActivity` key event handling (Glove mode).
  - **AppWidget Subsystem**: `AstraAppWidgetProvider`, `AstraWidgetUpdater` (using Android SDK `RemoteViews`, zero background GPS, zero background services).
  - **Security & Privacy**: `PrivacySecurity`, `NetworkPolicy`, `LocationStore` (strictly enforced, `allowBackup="false"`).

### Data Flow
1. **Ephemeris**: UTC Time + Observer Location $\to$ `AstronomyEngine` / `Sgp4Propagator` $\to$ Topocentric & Projected Coordinates $\to$ `SkyCanvas` & `ObjectDetails`.
2. **Finding Aids**: Screen Touches $\to$ `SkyProjection.coordinates` $\to$ `CelestialMeasurement` $\to$ Invariant Overlays.
3. **Logbook & Challenges**: User Logs $\to$ `ObservationLogbookStore` $\to$ Challenge Evaluator $\to$ Progress Bars & Map/Search Observed Badges.
4. **Environment**: Open-Meteo Temperature & Humidity $\to$ `DewMonitor` (Magnus-Tetens) $\to$ Warning Alert in Weather & Plan screens.
5. **Widget**: System broadcast / App update trigger $\to$ `TonightWindowCalculator` + cached weather + `LocationStore` $\to$ `RemoteViews` on Android Launcher.

---

## Feature Inventory

Every feature identified in the Survey phase is mapped to an implementation milestone. No feature is unassigned.

| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Galilean Jupiter Moons | Real-time calculation of Io, Europa, Ganymede, Callisto state vectors relative to Jupiter; event detection (Transits, Occultations, Eclipses, Shadow Transits); diagram in ObjectDetails and zoomed star map rendering | M1 | P2.13 / ORIGINAL_REQUEST §R1 |
| 2 | Saturn Ring Tilt & Titan Orbit | Saturn ring opening/tilt angle calculation & visualization; Titan analytical Keplerian orbit position relative to Saturn | M1 | P2.13 / ORIGINAL_REQUEST §R1 |
| 3 | Lunar Terminator & Feature Relief | Phase-accurate lunar terminator curve calculation; prioritized highlighting of craters, rilles, and maria in optimal low-sun relief along terminator | M1 | P2.13 / ORIGINAL_REQUEST §R1 |
| 4 | Offline SGP4 Satellite Propagator & ISS Tracks | Pure Kotlin SGP4 propagation from bundled/imported TLEs; visible pass predictions; sky trajectory with time markers on map & AR; 100% offline, zero background GPS | M1 | P2.13 / ORIGINAL_REQUEST §R1 |
| 5 | Interactive Angular Measurement Tool | Interactive measurement of angular separation ($D^\circ M' S''$) and position angle ($\theta \in [0^\circ, 360^\circ)$) between two celestial objects; invariant under zoom, rotation, mirroring | M2 | P2.14 / ORIGINAL_REQUEST §R2 |
| 6 | Coordinate Grids & Reference Lines | True Equatorial (RA/Dec) and Horizontal (Az/Alt) grids; independent toggles for Celestial Equator, Ecliptic, and Galactic Equator; full red-light mode support | M2 | P2.14 / ORIGINAL_REQUEST §R2 |
| 7 | Interactive Star-Hopping Assistant | Navigational hops from bright guide stars to faint DSOs; checkable waypoints; active Telrad ($0.5^\circ, 2^\circ, 4^\circ$) and eyepiece FOV reticles centered on active waypoint | M2 | P2.14 / ORIGINAL_REQUEST §R2 |
| 8 | Observation Challenges & Sync | Standardized challenges for Messier (110), Caldwell (109), and Herschel 400; real-time progress bars ("X von N beobachtet") synchronized to logbook | M3 | P2.15 / ORIGINAL_REQUEST §R3 |
| 9 | "Observed in Logbook" Indicator Badge | Subtle visual indicator/badge for logged objects across Star Map (`SkyCanvas`), Search (`SkySearchSheet`), and Observation Plan (`ObservationPlanScreen`) | M3 | P2.15 / ORIGINAL_REQUEST §R3 |
| 10 | Dew Point & Condensation Monitor | Local Magnus-Tetens calculation from ambient temp & relative humidity (Open-Meteo); 4-stage risk alert (Gering, Mäßig, Hoch, Akut) for telescope optics | M3 | P2.15 / ORIGINAL_REQUEST §R3 |
| 11 | OpenAstronomyLog (OAL 2.1) XML Export | Export logbook entries to valid OpenAstronomyLog XML schema with proper entity escaping; printable/red-light formatted text summary | M3 | P2.15 / ORIGINAL_REQUEST §R3 |
| 12 | Standard Astronomical Seeing Scales | Support for Pickering (1–10), Antoniadi (I–V), and naked-eye limiting magnitude (NELM) in logbook entries and export | M3 | P2.15 / ORIGINAL_REQUEST §R3 |
| 13 | Stepped Night Exposure Compensation | Camera2 AE exposure compensation stepping (+1 EV, +2 EV, max EV) via CameraX in AR mode to brighten faint horizons | M4 | P2.16 / ORIGINAL_REQUEST §R4 |
| 14 | AR Sensor Low-Pass Jitter Filter & Pitch Trim | Adaptive/configurable low-pass filter ($\alpha(FOV)$) smoothing orientation jitter at narrow FOVs; manual pitch trim offset ($\pm 15^\circ$) for leveling/magnetic offset | M4 | P2.16 / ORIGINAL_REQUEST §R4 |
| 15 | Physical Volume Key Glove Mode Zoom | Interception of `KEYCODE_VOLUME_UP/DOWN` in `MainActivity` on star map tab for stepped zoom when wearing winter gloves | M4 | P2.17 / ORIGINAL_REQUEST §R4 |
| 16 | Pure OLED True Black Mode (`#000000`) | Ultra-dark UI theme replacing dark grays and gradients with pure `#000000` for zero-luminance OLED pixels and maximal dark adaptation | M4 | P2.17 / ORIGINAL_REQUEST §R4 |
| 17 | Battery-Friendly Homescreen AppWidget | Android SDK `AppWidgetProvider` + `RemoteViews` displaying Moon phase, darkness window, and weather score without background GPS or running service | M5 | P2.17 / ORIGINAL_REQUEST §R4 |
| 18 | E2E Testing Suite & Quality Verification | Full 4-tier requirement-driven E2E test suite (Tiers 1–4) + Phase 2 adversarial coverage hardening (Tier 5); `./gradlew test` passes 100% | M6 | ORIGINAL_REQUEST §Verification |
| 19 | Project Documentation Updates | Update AUFGABEN.md (marking P2.13–P2.17 checkboxes completed) and README.md with newly implemented capabilities | M6 | ORIGINAL_REQUEST §Verification |

---

## Milestones

| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Solar System Dynamics & Moon (P2.13) | Galilean moons (positions & events), Saturn rings & Titan, Lunar terminator & features, Offline SGP4 satellite propagation & ISS tracks | Survey complete | DONE (Unit & E2E tests passing, verified by Auditor CLEAN, Challengers APPROVE, Reviewers APPROVE) |
| M2 | Celestial Measurement & Navigation Aids (P2.14) | Angular distance & position angle tool, Equatorial & Horizontal grids, Reference lines (Equator, Ecliptic, Galactic), Star-hopping assistant, Optics invariance | M1 (coordinates) | DONE (CelestialMeasurement, SkyGridRenderer, StarHopCatalog, StarHopHud, StarHopSheet, tests passing) |
| M3 | Observation Practice & Logbook (P2.15) | Messier 110/Caldwell/Herschel 400 challenges with sync progress, Logbook indicator across Map/Search/Plan, Dew Monitor (Magnus-Tetens), OAL XML & formatted text export, Seeing scales | None (independent data store) | DONE (ObservationChallenges, DewMonitor, OpenAstronomyLogExport, Seeing scales, tests passing) |
| M4 | Camera, AR & Hardware Usability (P2.16 & P2.17) | Camera2 AE exposure compensation, AR sensor low-pass filtering & pitch trim, Glove mode volume key zoom, OLED pure black mode | None (UI & hardware layer) | DONE (CameraExposureController, SensorFilter, GloveModeZoomController, OledThemeManager, tests passing) |
| M5 | Battery-Friendly Homescreen AppWidget (P2.17) | Android `AppWidgetProvider` + `RemoteViews` for moon phase, darkness window, weather score, zero background GPS/service | M1, M3 (calculations) | DONE (AstraAppWidgetProvider, AstraWidgetUpdater, widget layout, tests passing) |
| M6 | Final Milestone & Verification | Pass 100% of E2E test suite (Tiers 1–4), update AUFGABEN.md and README.md | M1, M2, M3, M4, M5, E2E Track | DONE (gradlew test 100% pass, AUFGABEN.md & README.md updated to 1.1.9-pre.12) |

---

## Interface Contracts

### 1. Solar System & Moons ↔ UI / Details
- `JupiterMoonsCalculator`:
  - `fun calculate(time: Instant): JupiterSystemState`
  - `data class MoonState(val name: String, val offsetRJ: Double, val zAU: Double, val event: JupiterMoonEvent, val shadowOffsetRJ: Double? = null, val isShadowTransiting: Boolean = false, val isInEclipse: Boolean = false)`
  - `enum class JupiterMoonEvent { NONE, TRANSIT, SHADOW_TRANSIT, OCCULTATION, ECLIPSE }`
- `SaturnSystemCalculator`:
  - `fun calculate(time: Instant): SaturnSystemState`
  - `data class SaturnSystemState(val ringTiltDegrees: Double, val titanOffsetRS: Double, val titanPositionAngle: Double)`
- `LunarTerminatorCalculator`:
  - `fun calculateTerminator(time: Instant): LunarTerminatorState`
  - `fun featuresNearTerminator(terminator: LunarTerminatorState, features: List<LunarFeature>): List<LunarFeatureHighlight>`
- `Sgp4Propagator`:
  - `fun propagate(tle: TleData, time: Instant, observer: GeoPoint): TopocentricPosition?`
  - `fun generatePassTrack(tle: TleData, start: Instant, durationMinutes: Int, observer: GeoPoint): List<SatelliteTrackPoint>`

### 2. Celestial Measurement ↔ SkyCanvas
- `CelestialMeasurement`:
  - `fun measure(p1: EquatorialCoordinates, p2: EquatorialCoordinates): MeasurementResult`
  - `data class MeasurementResult(val angularDistanceDegrees: Double, val formattedDistance: String, val positionAngleDegrees: Double)`
- `SkyGridRenderer`:
  - `fun generateEquatorialGrid(stepHours: Double, stepDegrees: Double): List<SkyGridLine>`
  - `fun generateHorizontalGrid(stepAzDegrees: Double, stepAltDegrees: Double): List<SkyGridLine>`
  - `fun generateReferenceLines(): ReferenceLines`

### 3. Star-Hopping ↔ SkyCanvas / Sheet
- `StarHopEngine`:
  - `data class StarHopRoute(val id: String, val targetName: String, val guideStar: String, val steps: List<StarHopStep>)`
  - `data class StarHopStep(val stepIndex: Int, val description: String, val centerCoords: EquatorialCoordinates, val fovDegrees: Double, val isCompleted: Boolean)`

### 4. Logbook & Challenges ↔ Observation Plan / Map
- `ObservationChallengeRegistry`:
  - `fun evaluateProgress(challenge: ChallengeType, entries: List<ObservationLogEntry>): ChallengeProgress`
  - `data class ChallengeProgress(val type: ChallengeType, val completedCount: Int, val totalCount: Int, val targets: List<ChallengeTargetStatus>)`
- `ObservationLogbookStore`:
  - `fun exportOalXml(entries: List<ObservationLogEntry>): String`
  - `fun exportFormattedText(entries: List<ObservationLogEntry>): String`
- `DewMonitor`:
  - `fun calculate(tempCelsius: Double, relativeHumidityPercent: Double): DewPointResult`
  - `enum class DewRiskLevel { LOW, MODERATE, HIGH, CRITICAL }`

### 5. Camera & Sensors ↔ AR Mode
- `SensorFilter`:
  - `fun filterOrientation(currentAzimuth: Float, targetAzimuth: Float, currentPitch: Float, targetPitch: Float, fovDegrees: Float, pitchTrim: Float): Pair<Float, Float>`
- `CameraNightControls`:
  - `fun applyExposureCompensation(camera: Camera, step: Int): Boolean`

---

## Code Layout

```
app/src/main/java/de/projektastra/app/
├── ephemeris/
│   ├── JupiterMoonsCalculator.kt
│   ├── SaturnSystemCalculator.kt
│   ├── LunarTerminatorCalculator.kt
│   ├── Sgp4Propagator.kt
│   └── SatelliteCatalog.kt
├── coordinates/
│   ├── CelestialMeasurement.kt
│   ├── SkyGridRenderer.kt
│   └── StarHopCatalog.kt
├── observation/
│   ├── ObservationChallenges.kt
│   ├── DewMonitor.kt
│   └── OpenAstronomyLogExport.kt
├── hardware/
│   ├── SensorFilter.kt
│   └── CameraExposureController.kt
├── widget/
│   ├── AstraAppWidgetProvider.kt
│   └── AstraWidgetUpdater.kt
├── MainActivity.kt (integration points)
├── SkyCanvas.kt / SkyProjection.kt / SkyCoordinateFrame.kt
├── ObservationLogbook.kt / TonightRecommendations.kt / WeatherData.kt
└── OpticsProfiles.kt / PrivacySecurity.kt
```

## Security & Verification Rules
1. **allowBackup="false"**: Must be strictly preserved in `AndroidManifest.xml`.
2. **Zero Unauthorized Traffic**: Only permitted HTTPS endpoints in `NetworkPolicy` (`api.open-meteo.com`). SGP4, ephemeris, widget, challenges must be 100% offline.
3. **No Background GPS / Service**: Location updates on demand only; rounded to 0.01°.
4. **Hermetic Build**: Do not alter `app/build.gradle.kts` dependency locks or add external Maven libraries; implement math and widgets with pure Kotlin and Android standard SDK.
5. **Full Unit Test Pass**: `./gradlew test` must pass 100% at every milestone.
