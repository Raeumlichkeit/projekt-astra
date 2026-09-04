# Projekt Astra

Ein Android-MVP für eine sensorgesteuerte Sternkarte und aktuelle astronomische Wetterdaten.

## Bereits umgesetzt

- Sternpositionen aus Rektaszension/Deklination, Beobachterposition und Uhrzeit
- Ausrichtung über Android-Rotationssensor
- AR-Modus mit CameraX-Kamerabild und Stern-Overlay
- Korrektur von magnetisch Nord auf geografisch Nord
- GPS-Standort mit Berlin als klar gekennzeichnetem Demo-Fallback
- Offline-Katalog mit 5.070 Sternen bis zur visuellen Helligkeit 6,0 (HYG v4.1)
- Sternfarben, Horizont und Linien markanter Sternbilder
- Objektinformationen und berechnete Positionen für die nächsten zwölf Stunden
- Live-Wetter von Open-Meteo ohne API-Schlüssel
- Dunkles, für Nachtbeobachtung optimiertes Compose-UI

## Lokal starten

1. Das Verzeichnis in einer aktuellen Version von Android Studio öffnen.
2. Android SDK 37 installieren, falls Android Studio danach fragt.
3. Gradle-Synchronisierung durchführen.
4. Die App auf einem Android-Gerät mit Standort- und Bewegungssensoren starten.

Auf einem Emulator funktioniert die Oberfläche, die automatische Ausrichtung benötigt jedoch simulierte Sensorwerte oder ein echtes Gerät.

## Geplanter nächster Ausbau

- größerer, lizenzierter Stern- und Deep-Sky-Katalog
- Planeten, Sonne und Mond mit Ephemeriden
- weitere Sternbilder, Sternbildnamen und Illustrationen
- interaktive Wetterkarte mit Wolken- und Niederschlags-Layern
- nutzergeführte Kompass- und AR-Feinkalibrierung
- Tests mit Referenz-Ephemeriden

## Daten und Datenschutz

Der Standort wird für die Himmelsberechnung lokal verarbeitet und zur Abfrage des standortbezogenen Wetters an Open-Meteo übertragen. Es ist noch kein Tracking oder Benutzerkonto enthalten.

Der abgeleitete Offline-Sternkatalog basiert auf HYG v4.1 und steht unter CC BY-SA 4.0.
Details stehen in `THIRD_PARTY_NOTICES.md`.
