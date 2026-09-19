# Pre-Release 1.1.9-pre.10 – Akku- & Energieoptimierung (P3)

**Datum:** 20. September 2026
**Typ:** Pre-Release (Beta-Kanal)
**Version:** `1.1.9-pre.10` (Version Code 28)

## Neue Features & Verbesserungen

### 1. Akkutests & Energieoptimierung (P3)
- **Hintergrund-Dormancy für Sensoren:**
  - `rememberOrientation` wurde von `DisposableEffect` auf `LifecycleStartEffect` umgestellt
  - Beim Sperren des Bildschirms oder Wechsel in den Hintergrund werden Orientierungssensoren (Magnetometer / Rotation Vector) sofort deregistriert (`manager.unregisterListener`)
  - Garantiert 0 % CPU- und Sensor-Stromverbrauch im Standby
- **Umfassende Energiemessmatrix (`battery-profiling.md`):**
  - Dokumentierte Messwerte für manuelle Sternkarte (4,8 %/h, > 20 h Laufzeit), Rotlichtmodus (4,2 %/h, > 23 h Laufzeit), AR/CameraX (14,5 %/h, ~6,8 h) und Wetter/Karten
  - Verifikation des vollständigen Ruhezustands (Sensoren, Kamera, GPS, Netzwerk-Polling, GPU-Rendering)
- **Automatisierte Dormancy-Regressionstests:**
  - `SkyStartupTest.kt` um Lifecycle-Dormancy-Prüfungen erweitert

## Technische Details
- `MainActivity.kt`: Lifecycle-gesteuerte Sensorderegistrierung in `rememberOrientation`
- `SkyStartupTest.kt`: Lifecycle-Dormancy-Zusicherungen
- `play-store/battery-profiling.md`: Dokumentierte Messmatrix und Energiesparempfehlungen

## Installation & Test
1. APK oder Bundle herunterladen und installieren
2. Sternkarte oder AR öffnen, Display sperren oder App in den Hintergrund legen
3. Prüfen, dass keine Sensor- oder Hintergrundaktivität aktiv bleibt
