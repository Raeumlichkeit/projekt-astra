# E2E Test Infra: Projekt Astra (P2.13–P2.17)

## Test Philosophy
- Opaque-box, requirement-driven derived strictly from `ORIGINAL_REQUEST.md` and `AUFGABEN.md`.
- Zero dependency on internal implementation design details; tests interact through defined astronomical calculation interfaces, public models, and Android components.
- Strict offline and security preservation: verify `allowBackup="false"`, no unauthorized network requests, zero background GPS.
- Methodology: Category-Partition + Boundary Value Analysis (BVA) + Pairwise Combinatorial Testing + Real-World Workload Testing.

## Feature Inventory & Test Coverage Mapping
| # | Feature | Requirement Source | Tier 1 (Coverage) | Tier 2 (BVA) | Tier 3 (Pairwise) | Tier 4 (Scenario) |
|---|---------|--------------------|:-----------------:|:------------:|:-----------------:|:-----------------:|
| 1 | Galilean Moons (Io, Europa, Ganymede, Callisto) | ORIGINAL_REQUEST §R1 | ≥ 5 | ≥ 5 | ✓ | ✓ |
| 2 | Saturn Ring Tilt & Titan Orbit | ORIGINAL_REQUEST §R1 | ≥ 5 | ≥ 5 | ✓ | ✓ |
| 3 | Lunar Terminator & Feature Proximity | ORIGINAL_REQUEST §R1 | ≥ 5 | ≥ 5 | ✓ | ✓ |
| 4 | Offline SGP4 Satellite Propagator (ISS) | ORIGINAL_REQUEST §R1 | ≥ 5 | ≥ 5 | ✓ | ✓ |
| 5 | Angular Distance & Position Angle Tool | ORIGINAL_REQUEST §R2 | ≥ 5 | ≥ 5 | ✓ | ✓ |
| 6 | Coordinate Grids (RA/Dec, Az/Alt) & Lines | ORIGINAL_REQUEST §R2 | ≥ 5 | ≥ 5 | ✓ | ✓ |
| 7 | Star-Hopping Assistant & FOV Reticles | ORIGINAL_REQUEST §R2 | ≥ 5 | ≥ 5 | ✓ | ✓ |
| 8 | Observation Challenges (M110, Caldwell, Herschel 400) | ORIGINAL_REQUEST §R3 | ≥ 5 | ≥ 5 | ✓ | ✓ |
| 9 | "Observed in Logbook" Indicator Badge | ORIGINAL_REQUEST §R3 | ≥ 5 | ≥ 5 | ✓ | ✓ |
| 10 | Dew Monitor (Magnus-Tetens) & Risk Alerts | ORIGINAL_REQUEST §R3 | ≥ 5 | ≥ 5 | ✓ | ✓ |
| 11 | OpenAstronomyLog (OAL 2.1) XML Export | ORIGINAL_REQUEST §R3 | ≥ 5 | ≥ 5 | ✓ | ✓ |
| 12 | Seeing Scales (Pickering, Antoniadi, NELM) | ORIGINAL_REQUEST §R3 | ≥ 5 | ≥ 5 | ✓ | ✓ |
| 13 | Stepped Night Exposure Compensation | ORIGINAL_REQUEST §R4 | ≥ 5 | ≥ 5 | ✓ | ✓ |
| 14 | AR Sensor Low-Pass Jitter Filter & Pitch Trim | ORIGINAL_REQUEST §R4 | ≥ 5 | ≥ 5 | ✓ | ✓ |
| 15 | Physical Volume Key Glove Mode Zoom | ORIGINAL_REQUEST §R4 | ≥ 5 | ≥ 5 | ✓ | ✓ |
| 16 | Pure OLED True Black Mode (`#000000`) | ORIGINAL_REQUEST §R4 | ≥ 5 | ≥ 5 | ✓ | ✓ |
| 17 | Battery-Friendly Homescreen AppWidget | ORIGINAL_REQUEST §R4 | ≥ 5 | ≥ 5 | ✓ | ✓ |

## Test Architecture
- **Test Runner**: `.\gradlew.bat test` (or `.\gradlew.bat test --tests "de.projektastra.app.*"`)
- **Pass/Fail Semantics**: All tests must complete with exit code 0, 0 failures, 0 errors, 0 skipped.
- **Directory Layout**: `app/src/test/java/de/projektastra/app/`
- **Tiers Structure**:
  - `Tier 1`: `AstronomicalEphemerisTest.kt`, `CelestialMeasurementUnitTest.kt`, `DewPointCalculationTest.kt`, `Sgp4PropagatorUnitTest.kt`.
  - `Tier 2`: `BoundaryCornerCasesTest.kt`, `OpenAstronomyLogSerializationTest.kt`, `ObservationChallengesUnitTest.kt`.
  - `Tier 3`: `CrossFeaturePairwiseTest.kt`, `OpticsInvarianceIntegrationTest.kt`, `SecurityAndOfflineEnforcementTest.kt`.
  - `Tier 4`: `RealWorldObservationScenariosTest.kt`.

## Real-World Application Scenarios (Tier 4)
| # | Scenario | Features Exercised | Target Complexity |
|---|----------|--------------------|-------------------|
| 1 | Winter Sub-Zero Dobsonian Observation | Glove Mode Volume Zoom, Pure OLED Mode, Optics 180° Inversion, Star-Hopping to M42 with Telrad, Dew Warning | High |
| 2 | High-Power Planetary & Lunar Night | Galilean Moons Transit/Shadow, Saturn Ring Tilt & Titan, Terminator Low-Sun Craters, Pickering 1–10 Seeing in Logbook | High |
| 3 | Satellite Pass Tracking in AR Mode | SGP4 ISS Pass Prediction, Time Markers on AR Sky Track, Adaptive Low-Pass Damping against Tremor, Night Exposure EV+2 | High |
| 4 | Deep-Sky Marathon & Challenge Logging | Messier 110 & Caldwell Progress Bars, Map & Search Observed Indicators, OAL XML Export, NELM / Antoniadi Scales | High |
| 5 | Homescreen Quick Assessment & Planning | AppWidget Moon Phase, Darkness Window, Weather Score, Offline Location Fallback (No GPS Background Drain) | Medium |

## Coverage Thresholds
- Tier 1: ≥ 5 test cases per feature (85+ test cases)
- Tier 2: ≥ 5 test cases per feature (85+ test cases)
- Tier 3: ≥ 17 pairwise cross-feature integration test cases
- Tier 4: ≥ 5 realistic multi-feature end-to-end user application workflows
- **Total Minimum Threshold**: ~192+ comprehensive test cases across Tiers 1–4.
