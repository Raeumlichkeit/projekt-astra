# Original User Request

## 2026-09-19T23:50:54Z

Implementiere die offenen astronomischen und beobachtungspraktischen Funktionsblöcke aus AUFGABEN.md für Projekt Astra unter Wahrung der bestehenden Offline- und Datenschutzarchitektur mit einem vollständigen Multi-Agenten-Team (Full Team).

Working directory: c:\Users\Jeremy\Documents\Chati\Projekt Astra
Integrity mode: development

## Requirements

### R1. Dynamische Sonnensystem-Körper & Monddetails (P2.13)
Echtzeit-Positionen der vier Galileischen Jupitermonde (Io, Europa, Ganymed, Kallisto) inklusive Schattentransiten und Verfinsterungen auf der Sternkarte und in den Objektdetails darstellen. Visualisierung des Saturn-Ringöffnungswinkels und Titans Orbitposition bereitstellen. Eine phasengenaue Mond-Detailansicht mit Kratern, Rillen und Meeresbeschriftungen entlang des aktuellen Terminators anbieten sowie sichtbare Überflüge der ISS und heller Satelliten rein lokal auf Basis von TLE-Bahnelementen (SGP4) mit Zeitmarken auf Karte und im AR-Modus einblenden.

### R2. Aufsuchhilfen & Himmelsvermessung (P2.14)
Ein interaktives Messwerkzeug zur Ermittlung des exakten Winkelabstands und Positionswinkels zwischen zwei beliebigen Himmelsobjekten bereitstellen. Zuschaltbare äquatoriale (RA/Dec) und horizontale (Az/Alt) Koordinatengitter sowie getrennt einblendbare Referenzlinien für Himmelsäquator, Ekliptik und galaktischen Äquator mit voller Rotlicht-Unterstützung implementieren. Einen interaktiven Star-Hopping-Assistenten mit abhakbaren Zwischenstationen von markanten Leitsternen zu lichtschwachen Deep-Sky-Zielen anbieten.

### R3. Beobachtungspraxis & erweiterte Planung (P2.15)
Standardisierte Beobachtungs-Challenges (Messier 110, Caldwell, Herschel 400) mit visuellem Fortschrittsbalken integrieren und den Status „Im Logbuch beobachtet“ dezent in Karte, Suche und Beobachtungsplan kennzeichnen. Eine lokale Taupunkt- und Beschlagswarnung (Dew Monitor) anhand von Umgebungstemperatur und Luftfeuchtigkeit bereitstellen. Den Tagebuchexport um das OpenAstronomyLog-XML-Format (OAL) sowie druck-/rotlichtoptimierte Textzusammenfassungen und astronomische Seeing-Skalen (Pickering, Antoniadi, visuelle Grenzgröße) erweitern.

### R4. Kamera-, AR- & Feld-Usability (P2.16 & P2.17)
Eine gestufte Nacht-Belichtungskorrektur (Camera2 AE Exposure Compensation) und eine zuschaltbare Tiefpassfilterung gegen Sensorzittern im AR-Modus bereitstellen. Physische Tastenbedienung (Lautstärketasten für Sternkarten-Zoom) für die Bedienung mit Handschuhen, einen reinen OLED-Reinstschwarz-Modus zur maximalen Dunkeladaption sowie ein akkuschonendes Homescreen-Widget (Android Glance / AppWidget) für Mondphase, Dunkelheitsfenster und Wetter-Score auf Basis des letzten bekannten Orts (ohne Hintergrund-GPS) ergänzen.

## Verification Resources
- Vorhandene Gradle-Testsuite mit JUnit- und Architekturtests (`./gradlew test`)
- Bestehende astronomische Berechnungsmodelle in `TonightRecommendations.kt`, `SkyCoordinateFrame.kt`, `SkyProjection.kt` und `OpticsProfiles.kt`
- Dokumentierte Geräte-Testfälle in `play-store/device-test-cases.md` und Akku-Benchmarks in `play-store/battery-profiling.md`

## Acceptance Criteria

### Sonnensystem & Mond (P2.13)
- [ ] Jupitermonde Io, Europa, Ganymed und Kallisto werden mit relativen Koordinaten zu Jupiter berechnet und bei Ereignissen (Transit, Okkultation, Verfinsterung) visuell und textuell gekennzeichnet.
- [ ] Saturns Ringneigungswinkel wird astronomisch korrekt berechnet und dargestellt; Titan wird auf seiner Bahn angezeigt.
- [ ] Der Mondterminator wird phasengenau berechnet und markante Mondformationen (Krater, Meere) in Terminatornähe werden priorisiert hervorgehoben.
- [ ] ISS- und Satelliten-Bahnberechnungen erfolgen mit SGP4 rein lokal ohne permanente Hintergrundortung oder Serverzwang; Satellitenpfade werden auf Karte und AR mit Zeitstempeln dargestellt.
- [ ] Automatisierte Unit-Tests für Mondterminator, Jupitermond-Konfigurationen und Satellitenpropagator laufen erfolgreich durch.

### Aufsuchhilfen & Koordinaten (P2.14)
- [ ] Winkelabstand zweier gewählter Punkte wird exakt in Grad, Bogenminuten und Bogensekunden berechnet und der Positionswinkel angegeben.
- [ ] Äquatoriales und horizontales Gitter sowie Referenzlinien (Ekliptik, Himmelsäquator, galaktischer Äquator) lassen sich separat ein-/ausschalten und unterstützen den Rotlichtmodus.
- [ ] Star-Hopping-Assistent führt schrittweise mit Telrad- und Okulargesichtsfeldern vom Startstern zum Zielobjekt.
- [ ] Gesten, Zoom und Bildrotation/Spiegelung verzerren die Mess- und Gitterlinien nicht; Testabdeckung vorhanden.

### Beobachtungsplanung & Tagebuch (P2.15)
- [ ] Challenges für Messier (110 Objekte), Caldwell und Herschel 400 zeigen den Echtzeit-Fortschritt („X von N beobachtet“) synchron zum Tagebuch.
- [ ] Bereits im Logbuch erfasste Objekte tragen eine Kennzeichnung in Sternkarte, Suche und Beobachtungsplan.
- [ ] Taupunkt wird aus Open-Meteo-Werten lokal berechnet und warnt bei Temperaturannäherung vor Kondensat/Taubeschlag.
- [ ] Tagebuchexport erzeugt valides OpenAstronomyLog-XML (OAL) und formatierte Textzusammenfassungen; Seeing-Skalen (Pickering 1–10, Antoniadi) werden im Tagebuch unterstützt.
- [ ] Unit-Tests validieren OAL-Serialisierung, Taupunktformel und Challenge-Filter.

### Kamera, AR & Feld-Usability (P2.16 & P2.17)
- [ ] Belichtungskorrektur hellt das Camera2-Nachtbild in Stufen auf, ohne die App-Stabilität zu gefährden.
- [ ] Tiefpass-Filterung glättet Sensor-Jitter im AR-Modus bei engen Sichtfeldern.
- [ ] Lautstärketasten zoomen im Handschuh-Modus stufenweise in die Sternkarte hinein bzw. heraus.
- [ ] OLED-Reinstschwarz-Modus schaltet Grautöne auf `#000000` um.
- [ ] Homescreen-Widget zeigt Mondphase und Wetter-Score ohne laufenden Hintergrund-Dienst oder Hintergrund-GPS.

### Code-Qualität & Build-Integrität
- [ ] `./gradlew test` baut fehlerfrei durch und alle bestehenden sowie neuen Unit-Tests bestehen zu 100 %.
- [ ] Striktes Beibehalten von `allowBackup="false"` und Verzicht auf unverschlüsselte oder unautorisierte Netzwerkübertragungen gemäß `SECURITY_REMEDIATION.md`.
- [ ] `AUFGABEN.md` und `README.md` werden entsprechend der umgesetzten Punkte aktualisiert.
