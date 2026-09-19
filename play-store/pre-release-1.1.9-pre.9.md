# Pre-Release 1.1.9-pre.9 – Robustheit und Darstellung (P2.12)

**Datum:** 20. September 2026
**Typ:** Pre-Release (Beta-Kanal)
**Version:** `1.1.9-pre.9` (Version Code 27)

## Neue Features & Verbesserungen

### 1. Robustheit & Fehlerzustände
- **DSS2-Himmelsaufnahmen (`SkySurveyImage`):**
  - Benutzerfreundliche Fehler- und Ladezustände bei Timeouts oder Serverausfällen
  - Dedizierter „Erneut versuchen“-Button zum direkten Neuladen der Archivaufnahme
  - Diskrete Ladeanzeige während des Bildabrufs
- **Geländeprofilierung (`TerrainRepository`):**
  - Direkte Wiederholungsoption („Erneut versuchen“) in der Statuszeile, falls das GLO-90-Geländeprofil offline oder vorübergehend nicht abrufbar war
- **Wetter & Karten:**
  - Konsistente Fehlerhinweise und Retry-Buttons über alle Module hinweg verifiziert

### 2. Automatisierte Regressionstests (`SkyMapInteractionTest.kt`)
- 16 neue automatisierte Unittests:
  - **Pinch-to-Zoom & Panning:** Min-/Max-Clamping (25° bis 150° FOV), FOV-proportionale Gestenskalierung, mathematische Behandlung degenerierter Gestenwerte, Horizont-/Zenit-Begrenzung (-90° bis +90°), Azimut-Wrapping über den Nordmeridian (0°/360°)
  - **Optik-Gestentransformation:** Natürliche Wischgesten unter 0°, 90°, 180° (Newton-Invertierung), 270° und horizontaler Spiegelung (Zenitspiegel)
  - **AR- & Kartenwechsel:** Nahtlose Übernahme der Blickrichtung von Sensoren beim Verlassen von AR, Erhalt aktiver Zielauswahl, Freigabe der Zielnachführung beim AR-Eintritt, isolierte Optik-Deaktivierung im AR-Modus, dynamischer Wechsel zwischen sphärischer Perspektive und Zylinderprojektion
  - **Beschriftungskollisionen:** Überlappungsfreie Platzierung dichter Sternhaufen (30+ Sterne in 50×50 px), Prioritätsgarantie für ausgewählte Ziele (`TARGET`), korrekte Platzierung aufrechter Beschriftungen unter Optikdrehung, Einhaltung der Viewport-Padding-Grenzen

### 3. Dokumentierte Geräte-Prüffälle (`device-test-cases.md`)
- Vollständige Testmatrix für echte Geräte:
  - Berechtigungsentzug zur Laufzeit (Standort, Kamera, Benachrichtigungen)
  - App-Unterbrechungen (Anrufe, Home, Sperrbildschirm, Split-Screen, Low-Memory-Trim)
  - Fehlende Sensoren (Kompass/Magnetometer, Gyroskop, Kamera)
  - Vollständiger Offline-Betrieb (Flugmodus, Asset-Kataloge, Fallbacks)
  - Fehlerhafte Netzwerkantworten (Timeouts, HTTP 500)
  - Rendering-Profilierung mit Benchmarks für Frame-Zeiten und Heap-Speicher

## Technische Details
- `app/src/test/java/de/projektastra/app/SkyMapInteractionTest.kt`: 16 Unittests (Gesamtbestand: 179+ Tests)
- `play-store/device-test-cases.md`: Umfassende Prüfmatrix und Dokumentation
- `MainActivity.kt`: Retry-Mechanismus für DSS2-Aufnahmen und Geländeprofil
- `PrivacySecurity.kt`: `onError` und `onFinished` Callbacks in `PrivateWebViews`

## Installation & Test
1. APK oder Bundle herunterladen und installieren
2. Sternkarte manuell verschieben, zoomen und mit zwei Fingern skalieren
3. Himmelskörper antippen → DSS2-Aufnahme aufrufen (bei Offline-Verbindung oder Timeout den Wiederholen-Button prüfen)
4. AR-Modus aktivieren und deaktivieren → Ausrichtung und Zielübernahme prüfen
