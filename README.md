# Projekt Astra

Ein Android-MVP für eine sensorgesteuerte Sternkarte und aktuelle astronomische Wetterdaten.

## Bereits umgesetzt

- Sternpositionen aus Rektaszension/Deklination, Beobachterposition und Uhrzeit
- Ausrichtung über Android-Rotationssensor
- AR-Modus mit CameraX-Kamerabild und Stern-Overlay
- Korrektur von magnetisch Nord auf geografisch Nord
- GPS-Standort mit Berlin als klar gekennzeichnetem Demo-Fallback
- Offline-Katalog mit 5.041 HIP-Sternen bis zur visuellen Helligkeit 6,0 (HYG v4.1)
- optional zuschaltbarer Offline-Katalog mit 1.016 Deep-Sky-Objekten (OpenNGC)
- Sternfarben, Horizont sowie erweiterte Linien und Namen markanter Sternbilder
- dynamische Milchstraßenebene, passend zu Uhrzeit, Standort und aktuellem Kartenausschnitt
- lokales 360°-Geländeprofil aus GLO-90-Höhendaten für einen realistisch verdeckten Horizont, mit Offline-Fallback
- Sonne, Mond und alle sieben von der Erde sichtbaren Planeten mit topozentrischen Ephemeriden
- aktuelle Planetenhelligkeit, Phase, Entfernung und berechnete zwölfstündige Bahn
- ausführliche Objektinformationen, Eigenbewegung und berechnete Positionen für zwölf Stunden
- echte DSS2-Himmelsaufnahmen passend zur Koordinate des ausgewählten Objekts
- Live-Wetter von Open-Meteo ohne API-Schlüssel
- interaktive Wetterkarte mit lokal gebündeltem Kartenrenderer, Regenradar und Bewölkung
- Pull-to-Refresh aktualisiert Wetter, Wetterkarte und GPS-Standort
- stündliche 24-Stunden-Zeitleiste für Wolken, Regenwahrscheinlichkeit, Wind und Beobachtungssicht
- aufklappbare Astra-Score-Erklärung mit Einzelabzügen für Bewölkung, Regen, Wind, Sichtweite und Mondlicht
- manuell verschiebbare und zoombare Sternkarte; AR bleibt separat sensorgesteuert
- standortbezogener Himmelskalender für Meteorschauer sowie Sonnen- und Mondfinsternisse
- exakt berechnete lokale Sonnenfinsterniskontakte, Bedeckung und Sonnenhöhe
- lokale Mondfinsternisfilterung mit Kontaktzeiten und Mondhöhe
- lokale Beobachtungseinschätzung aus Radiantenhöhe und ungefährem Mondlicht
- interaktive NASA-VIIRS-Nachtlichtkarte als Orientierung für Lichtverschmutzung
- geführte Kompass-/AR-Kalibrierung mit Android-Sensorgenauigkeit
- Referenztests für Ephemeriden und die lokale Sonnenfinsternis vom 2. August 2027
- Dunkles, für Nachtbeobachtung optimiertes Compose-UI

## Lokal starten

1. Das Verzeichnis in einer aktuellen Version von Android Studio öffnen.
2. Android SDK 37 installieren, falls Android Studio danach fragt.
3. Gradle-Synchronisierung durchführen.
4. Die App auf einem Android-Gerät mit Standort- und Bewegungssensoren starten.

Auf einem Emulator funktioniert die Oberfläche, die automatische Ausrichtung benötigt jedoch simulierte Sensorwerte oder ein echtes Gerät.

## Möglicher weiterer Ausbau

- vollständige IAU-Sternbildgrenzen und optionale Illustrationen
- automatische Online-Aktualisierung der jährlichen IMO-Meteorschauer-Maxima
- numerische Lichtverschmutzungsschätzung für den Astra-Score
- Favoriten, Beobachtungslisten und rechtzeitige Ereignisbenachrichtigungen

## Daten und Datenschutz

Der Standort wird für Himmels- und Ereignisberechnungen lokal verarbeitet, in den Karten lokal markiert und zur Abfrage des standortbezogenen Wetters sowie des Geländehöhenprofils an Open-Meteo übertragen. Es ist kein Tracking oder Benutzerkonto enthalten.

Der abgeleitete Offline-Sternkatalog basiert auf HYG v4.1 und steht unter CC BY-SA 4.0.
Details stehen in `THIRD_PARTY_NOTICES.md`.
