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
- Sternfarben, Horizont und Linien markanter Sternbilder
- ausführliche Objektinformationen, Eigenbewegung und berechnete Positionen für zwölf Stunden
- echte DSS2-Himmelsaufnahmen passend zur Koordinate des ausgewählten Objekts
- Live-Wetter von Open-Meteo ohne API-Schlüssel
- interaktive Wetterkarte mit lokal gebündeltem Kartenrenderer, Regenradar und Bewölkung
- Pull-to-Refresh aktualisiert Wetter, Wetterkarte und GPS-Standort
- manuell verschiebbare und zoombare Sternkarte; AR bleibt separat sensorgesteuert
- standortbezogener Himmelskalender für Meteorschauer sowie Sonnen- und Mondfinsternisse
- lokale Beobachtungseinschätzung aus Radiantenhöhe und ungefährem Mondlicht
- interaktive NASA-VIIRS-Nachtlichtkarte als Orientierung für Lichtverschmutzung
- Dunkles, für Nachtbeobachtung optimiertes Compose-UI

## Lokal starten

1. Das Verzeichnis in einer aktuellen Version von Android Studio öffnen.
2. Android SDK 37 installieren, falls Android Studio danach fragt.
3. Gradle-Synchronisierung durchführen.
4. Die App auf einem Android-Gerät mit Standort- und Bewegungssensoren starten.

Auf einem Emulator funktioniert die Oberfläche, die automatische Ausrichtung benötigt jedoch simulierte Sensorwerte oder ein echtes Gerät.

## Geplanter nächster Ausbau

- Planeten, Sonne und Mond mit Ephemeriden
- weitere Sternbilder, Sternbildnamen und Illustrationen
- Vorhersage-Zeitleiste für die Bewölkungsfläche
- online aktualisierbarer Ereigniskatalog und exakte lokale Finsterniskontakte
- nutzergeführte Kompass- und AR-Feinkalibrierung
- Tests mit Referenz-Ephemeriden

## Daten und Datenschutz

Der Standort wird für Himmels- und Ereignisberechnungen lokal verarbeitet, in den Karten lokal markiert und zur Abfrage des standortbezogenen Wetters an Open-Meteo übertragen. Es ist kein Tracking oder Benutzerkonto enthalten.

Der abgeleitete Offline-Sternkatalog basiert auf HYG v4.1 und steht unter CC BY-SA 4.0.
Details stehen in `THIRD_PARTY_NOTICES.md`.
