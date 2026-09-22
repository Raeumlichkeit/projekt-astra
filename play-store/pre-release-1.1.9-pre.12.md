# Pre-Release 1.1.9-pre.12 – Ephemeriden, Koordinatensysteme, Beobachtungspraxis & Usability (P2.13–P2.17)

**Datum:** 22. September 2026
**Typ:** Pre-Release (Beta-Kanal)
**Version:** `1.1.9-pre.12` (Version Code 30)

## Neue Features & Verbesserungen

### 1. Erweiterte Sonnensystem-Ephemeriden & Mondterminator (P2.13)
- **Galileische Jupitermonde:**
  - Präzise In-Plane-Orbitberechnung der 4 Monde (Io, Europa, Ganymed, Kallisto) in Einheiten des Jupiter-Radius ($R_J$).
  - Erkennung und Anzeige von Mondtransiten, Schattentransiten auf der Jupiterscheibe, Okkultationen und Verfinsterungen im Schattenkonus.
  - Interaktives Miniatur-Diagramm bei starkem Zoom ($\le 45^\circ$) und Infokarten in den Objektdetails.
- **Saturn-Ringsystem & Titan:**
  - Berechnung der geozentrischen Ringöffnung/Ringneigung $B$ (0° bis 28°) nach dem Meeus-Algorithmus.
  - Ovaldarstellung des A-/B-Rings und Titan-Positionswinkel.
- **Mondterminator & Kraterrelief:**
  - Genaue Colongitude ($C_0$), Sub-Solar-Länge und optische Libration ($L, B$).
  - Berechnete Sonnenhöhe an 40 markanten Mondkratern und Gebirgen mit automatischer Filterung nach optimalen plastischen Schattenwürfen ($\pm 15^\circ$ um den Terminator).
  - Mond-Detail-Sheet (`MoonDetailSheet.kt`) mit visueller Suchfunktion nach Relief-Qualität.
- **Offline-Satellitenvorhersage (SGP4):**
  - Autonome Orbitpropagation von ISS, Hubble und Tiangong aus gebündelten TLE-Bahnelementen ohne Netzwerkzugriff.
  - Anzeige des Überflugspfads mit Live-Positionspunkt auf der Sternkarte.

### 2. Koordinatensysteme, Winkelmessung & Star-Hopping (P2.14)
- **Interaktive sphärische Winkelmessung:**
  - Punkt-zu-Punkt-Messung zwischen Gestirnen und freien Himmelspositionen auf der Sternkarte.
  - Numerisch stabile Vincenty-Großkreisabstandsberechnung und Positionswinkel ($0^\circ$ bis $360^\circ$).
  - SLERP-Bogen und Mess-Badge direkt auf der Sternkarte gerendert.
- **Astronomische Himmelsgitter (`SkyGridRenderer`):**
  - Echtes äquatoriales Gitter (Rektaszensions-Stundenkreise, Deklinationsparallelen) und horizontales Gitter (Azimut/Höhe) mit adaptivem LOD je nach Sichtfeld.
  - 3 fundamentale Referenzlinien: Himmelsäquator, Ekliptik und Galaktischer Äquator.
- **Interaktiver Star-Hopping-Assistent:**
  - Schritt-für-Schritt-Routenführung von auffälligen Leitsternen zu lichtschwachen Deep-Sky-Objekten (z. B. M31, M13, M57).
  - Gestrichelte Routenpfade, farblich abgestimmte Wegpunktringe, Zentrierfunktion bis 0,5° Okular-FOV und HUD-Schrittnavigation.

### 3. Beobachtungspraxis & erweiterte Planung (P2.15)
- **Standardisierte Beobachtungs-Challenges:**
  - Vollständiger Messier-Katalog (110 Objekte), Caldwell-Katalog (109) und Herschel 400 mit Synchronisation zum Beobachtungstagebuch und visuellen Fortschrittsbalken.
  - Dezent grünes Status-Badge „✓ Im Logbuch“ auf der Sternkarte, in der Suche und im Beobachtungsplan.
- **Taupunkt- & Beschlagswarnung (Dew Monitor):**
  - Berechnung des Taupunkts nach der Magnus-Tetens-Formel aus Open-Meteo-Temperatur und Feuchtigkeit.
  - 4-stufiges Warnsystem (Gering, Mäßig, Hoch, Akut) zur rechtzeitigen Aktivierung von Taukappenheizungen.
- **OpenAstronomyLog (OAL 2.1 XML) & Standard-Skalen:**
  - Vollständiger OAL 2.1 XML-Export für Astro-Software (SkyTools, Cartes du Ciel, Deepsky).
  - Integration von Pickering-Seeing (1–10), Antoniadi-Skala (I–V) und Grenzgröße (f.s.t. / NELM).

### 4. Usability & Feldoptimierung (P2.16 & P2.17)
- **AR-Nachtbelichtung & Sensor-Tiefpass:**
  - Stufenweise Belichtungskorrektur (+1 bis +3 EV) über Camera2 AE Exposure Compensation für extrem dunkle Horizonte.
  - Einstellbare Kompass-/Gyrosensor-Dämpfung gegen Handzittern bei engem Sichtfeld sowie manuelle Pitch-Trimmung ($\pm 15^\circ$).
- **Handschuh-Modus (Volume-Key-Zoom):**
  - Zoomen der Sternkarte über die physischen Lautstärketasten (Volume +/-) bei eisigen Nächten mit Handschuhen.
- **OLED-Reinschwarz-Modus:**
  - Absolutes `#000000` auf allen Flächen ohne graue Zwischenebenen für maximale Dunkeladaption und Energieersparnis.
- **Passives Homescreen-Widget:**
  - Android-Widget mit Anzeige von Mondphase, Beleuchtungsgrad, astronomischer Nacht und Wetter-Score.
  - Strikt passiv: **0 % Hintergrund-GPS**, **0 Hintergrunddienste**, Aktualisierung nur bei App-Nutzung.

### 5. Fehlerkorrekturen
- Geländeverlauf/Horizont im manuellen Stereografiemodus: Invertierte/falsche Himmels- und Bodenflächenzuordnung behoben.
- Okular-FOV-Grenzen: Untere Grenze auf 0,5° (30 Bogenminuten) für hochvergrößernde Okulare und Star-Hop-Wegpunkte erweitert.

## Testsuite & Verifikation
- **495 bestandene Tests** (100 % Erfolgsquote, 0 Fehler) über alle Testklassen hinweg (Unit-, E2E- und Mathematiktests).
- Android Lint ohne Fehler abgeschlossen.
- Alle Datenschutz-Invarianten (`allowBackup="false"`, keine unautorisierten Netzwerkaufrufe) intakt.

## Prüfsummen
- `app-debug.apk`: `6aae9b7648f638ad9cdbea36be9de30f2da15c348e3c7b1b2b03fa2fa0f57c8d`
- `app-release.aab`: `aab68475c9b8478c82d3fa2267b33bc7d4113c9fd5a611cfa916c9db40c6332d`
