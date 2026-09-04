# Projekt Astra

Ein Android-MVP für eine sensorgesteuerte Sternkarte und aktuelle astronomische Wetterdaten.

## Bereits umgesetzt

- Sternpositionen aus Rektaszension/Deklination, Beobachterposition und Uhrzeit
- Ausrichtung über Android-Rotationssensor
- GPS-Standort mit Berlin als klar gekennzeichnetem Demo-Fallback
- Offline-Katalog mit zwölf hellen Sternen
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
- Sternbilder und Horizontlinie
- interaktive Wetterkarte mit Wolken- und Niederschlags-Layern
- Kompasskalibrierung und magnetische Deklination
- Tests mit Referenz-Ephemeriden

## Daten und Datenschutz

Der Standort wird für die Himmelsberechnung lokal verarbeitet und zur Abfrage des standortbezogenen Wetters an Open-Meteo übertragen. Es ist noch kein Tracking oder Benutzerkonto enthalten.
