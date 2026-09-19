# Pre-Release 1.1.9-pre.11 – Stereografische Projektion & Optik-Rotlichtmodus

**Datum:** 20. September 2026
**Typ:** Pre-Release (Beta-Kanal)
**Version:** `1.1.9-pre.11` (Version Code 29)

## Neue Features & Verbesserungen

### 1. Winkeltreue stereografische Projektion für weites FOV (Karten-Zoom)
- **Natürliche Sternfeldansicht beim Herauszoomen:**
  - Die manuelle Sternkarten-Perspektive (`SkyProjection.kt`) wurde von gnomonischer Rektilinearprojektion auf winkeltreue (konforme) stereografische Projektion umgestellt.
  - Verhindert die unnatürliche geometrische Dehnung am Bildrand und in den Bildschirmecken bei weitem Sichtfeld (bis 150° FOV).
  - Sternbild-Asterismen behalten ihre charakteristischen Formen auch am Rand ohne Verzerrung (konforme Eigenschaft: lokales Seitenverhältnis bleibt 1:1).
  - Sanfter, natürlicher Verzerrungsanstieg ($1 + \tan^2(\theta/2) \approx 1,49\times$ bei 75° Bildrand statt extremer $9\times$ bis $70\times$ Dehnung bei Gnomonik).
- **Pixelgenaue Shader-Ausrichtung der Milchstraße:**
  - Der OpenGL-Fragment-Shader (`milky_way.frag`) nutzt nun dieselbe inverse stereografische Projektion zur Richtungsvektor-Berechnung für jedes Bildschirmfragment.
  - Exakte Deckungsgleichheit zwischen den auf dem Compose-Canvas gerenderten Sternen/Sternbildern und dem Gaia-Milchstraßenhintergrund.
- **Sichtfeldkreis-Radius-Formel:**
  - `fovDiameterToRadiusPx` an die stereografische Projektion angepasst ($r = F \tan(\theta/4)$).

### 2. Rotlichtmodus-Integration für Optik & FOV
- **Optik-Status-Badge:**
  - Hintergrund, Rand, Text und Icon passen sich bei aktiviertem Rotlichtmodus an tiefe Rottöne (Astra-Rot) an.
- **Optik & Sichtfeld Sheet (`OpticsFovSheet.kt`):**
  - Theme-Provider `LocalOpticsColors` unterstützt `redLightMode` für Sheet-Hintergrund, Karten, Schieberegler, Chips, Text und den Profilerstellungsdialog.
- **Sichtfeldkreis & Telrad:**
  - FOV-Kreis und Beschriftung wechseln im Rotlichtmodus auf Dunkelrot zur Schonung der Dunkeladaption.

### 3. Testsuite & Verifikation
- 185 bestandene Unit-Tests in 22 Testklassen (inklusive 2 neuer Verifikationstests für stereografische Konformität und Entzerrung).
- Android-Lint fehlerfrei abgeschlossen.
- Korrigierte Testzahlen in `AUFGABEN.md` (P2.11 hat 8 Tests, P2.12 hat 19 Tests).

## Technische Details
- `SkyProjection.kt`: Stereografische Vorwärts- und Rückwärtsprojektion mit robuster Ebenen-Clipping-Geometrie.
- `milky_way.frag`: Stereografischer Strahlengang im Fragment-Shader.
- `OpticsFovSheet.kt`: Dynamische Farbpalette mit `LocalOpticsColors` unter Rotlichtmodus.
- `MainActivity.kt`: Rotlicht-Unterstützung für Optik-Status-Badge und FOV-Kreise.
- `SkyProjectionTest.kt`: 16 Tests für sphärische Projektion, Edge-Clipping und Konformität.

## Installation & Test
1. APK oder Bundle herunterladen und installieren.
2. In der Sternkarte weit herauszoomen (bis 150° FOV) und die natürliche Form der Sternbilder am Bildrand prüfen.
3. Milchstraßen-Ausrichtung an Sternen verifizieren.
4. Rotlichtmodus einschalten und Optik-Badge sowie Optik-Sheet öffnen.
