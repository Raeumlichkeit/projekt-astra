# Pre-Release 1.1.9-pre.8 – AR-Zielhilfe

**Datum:** 2026-09-19
**Typ:** Pre-Release (Beta-Kanal)

## Neue Features

### AR-Zielhilfe
- Richtungspfeile und Winkelabstand für ausgewählte Himmelsobjekte relativ zur aktuellen Blickrichtung
- Pulsierende Zielkreuz-Markierung wenn sich das Objekt im Sichtfeld befindet
- Kreisförmige Kantenzeiger mit Gradangabe für Objekte außerhalb des Bildschirms
- Status-Banner mit Zielname, Typ und Entfernungsangabe
- Verständliche Hinweise für Ziele hinter dem Gerät (> 90° von der Blickrichtung)
- Warnung bei Zielen unter dem lokalen Horizont oder Geländeprofil
- Sensorqualitäts-Anzeige (Hoch / Mittel / Niedrig) mit Kalibrierungshinweisen
- Keine falsche Genauigkeit bei unsicherer Sensorausrichtung
- Smooth Edge-Clamping: Off-Screen-Pfeile bleiben am Bildschirmrand sichtbar

## Technische Details
- `ArTargetGuidance.kt`: Haversine-Distanzberechnung, 3D-Kamerarelativvektor, Bildschirmkanten-Clamping, Winkel-Normalisierung
- `ArTargetGuidanceUi.kt`: Compose-Overlay mit Reticle, Edge-Pointer und Status-Banner
- 7 neue Testmethoden (Gesamtstand: 163+ Tests)
- 100 % lokal, keine Cloud-Kommunikation

## Bekannte Einschränkungen
- AR-Genauigkeit hängt von der Qualität des Magnetometers und Beschleunigungssensors ab
- Bei stark magnetischen Umgebungen kann die Richtungsanzeige abweichen
- Geländeprofil-Erkennung nur verfügbar wenn zuvor geladen

## Installation
1. APK herunterladen und installieren (Play Protect ggf. bestätigen)
2. Beliebiges Himmelsobjekt antippen → AR-Zielhilfe erscheint automatisch
3. Gerät in die angezeigte Richtung schwenken bis das Zielkreuz pulsiert
