# Pre-Release 1.1.9-pre.13 – OLED Reinstschwarz (#000000) für alle UI-Elemente

**Datum:** 22. September 2026
**Typ:** Pre-Release (Beta-Kanal)
**Version:** `1.1.9-pre.13` (Version Code 31)

## Neue Features & Verbesserungen

### 1. OLED Reinstschwarz (#000000) für die gesamte Benutzeroberfläche
- **Reinstes True-Black auf AMOLED/OLED-Displays:**
  - Bei aktiviertem OLED-Modus werden sämtliche UI-Flächen, Cards, Sheets, Header und Hintergründe auf absolutes True-Black (`#000000`) geschaltet.
  - Subpixel schalten auf AMOLED-Panels vollständig ab (0 mA Stromverbrauch für schwarze Pixel, maximale Erhaltung der Dunkeladaptation für nächtliche Beobachtungen).
- **Dynamische Theme-Palette (`LocalOledMode`):**
  - Jetpack Compose CompositionLocal steuert die Farbpalette der App konsistent und reaktiv.
  - Hintergrund, Surfaces und Cards (`Night`, `NightBlue`, `AstraSurface`, `AstraSurfaceHigh`) wechseln dynamisch auf `Color.Black`.
  - Kartenränder und Trennlinien wechseln auf ein dezentes Grau (`#1E1E1E`), um Container sauber abzugrenzen, ohne Pixel aufzuhellen.
- **Bildschirmhintergründe:**
  - `WeatherScreen`, `EventsScreen`, `ObservationPlanTab` und `SettingsScreen` schalten von Blauverlauf auf reines Schwarz um.
  - `AstraScreenHeader` schaltet Gradient-Overlays transparent.
  - Sternkarte (`sky-viewport` und `SkyCanvas`) schaltet auf reinstes `#000000`.
- **Detail-Sheets & Komponenten:**
  - **Optik-Rechner (`OpticsFovSheet`)**: Reines Schwarz für alle Okular-/Teleskop-Konfigurationskarten.
  - **Mond-Terminator & Relief (`MoonDetailSheet`)**: Reines Schwarz für Kraterkarten, Filter-Chips und Beobachtungstipps.
  - **Star-Hopping (`StarHopSheet`)**: Reines Schwarz für Routenbrowser, Wegpunktkarten und untere Navigationsleiste.

## Testsuite & Verifikation
- **495 bestandene Tests** (100 % Erfolgsquote, 0 Fehler).
- Release-Bundle (AAB) und Debug-APK erfolgreich kompiliert und signiert.
- Alle Datenschutz-Invarianten (`allowBackup="false"`, keine unautorisierten Netzwerkaufrufe) intakt.

## Prüfsummen
- `app-debug.apk`: `50d6c308283f7d51af798a9bcd218bf65e691fd66172b1c42c693776437ec9b0`
- `app-release.aab`: `3cd52b90420855d30cf0548bc9085241a227cfa74aa89c82155a7a0a4e3d8793`
