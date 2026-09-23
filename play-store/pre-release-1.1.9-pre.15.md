# 1.1.9-pre.15 – Review-Korrekturen für Datenschutz, Logbuch und Sternkarte

23. September 2026 · Version Code 33 · Beta-Testversion

## Änderungen

- Logbuch-Schreibvorgänge sind atomisch; beschädigte gespeicherte Daten werden nicht still als leeres Logbuch überschrieben. JSON-/Fotogrößen, Zeitpunkte, Dateinamen und Zielpfade werden geprüft. Importierte JSON-Dateien können keine vorhandenen lokalen Fotos beanspruchen.
- Fotoentfernung im Editor wird erst beim erfolgreichen Speichern übernommen. Abbrechen und fehlgeschlagenes Speichern erhalten die ursprünglichen Daten. Eine reine Notizänderung ersetzt keine historischen Koordinaten durch den heutigen Standort.
- Standort-Merken ist standardmäßig aus. Frühere Koordinaten ohne ausdrückliche Auswahl werden bereinigt; „Standort vergessen“ beendet die laufende Nutzung und aktualisiert das Widget. GPS-Berechtigung und dauerhafte Speicherung sind getrennte Entscheidungen.
- Einheitliche Projektion für AR-Zielhilfe und Sterne; passende Milchstraßen-Drehung/-Spiegelung bei Optikprofilen; weniger Rand-Clipping und korrigierte Gelände-Verdeckung. Enge Sichtfelder bleiben beim Verschieben und Lautstärketasten-Zoom erhalten.
- Satellitenpass-Vorhersage außerhalb des UI-Threads und minutenweise statt bei jedem Zeit-Tick. Near-Earth-SGP4 mit Drag- und periodischen Termen aus satellite.js 6.0.0; gegen Vallado-Referenzvektoren geprüft. Lizenztext ist in Quelle und APK enthalten.
- Jupiter-Mondpositionen berücksichtigen die Lichtlaufzeit. Helle Sommernächte und Polartage erhalten kein erfundenes astronomisches Dunkelheitsfenster; Widget und Empfehlungen behandeln dies ausdrücklich.
- Doppelte Testmodelle, ungenutzte Helfer und direkte Abhängigkeitsdeklarationen reduziert. Der bisher indirekte Android-Test-Runner wird ausdrücklich deklariert, damit Instrumentation weiterhin funktioniert.

## Verifikation

- 515 JVM-Tests bestanden, keine übersprungenen Tests. Darunter veröffentlichte SGP4-Vektoren, Jupiter-Geometrie, Dunkelheitsfenster/Polargebiete und Optik-/AR-Projektion.
- 11 Python-Tests zum IMO-Kalender und dessen Eingabe-/Fehlerbehandlung bestanden.
- Debug-APK, Android-Test-APK und minifiziertes Release-AAB erfolgreich gebaut. Lint: 0 Fehler, 17 Warnungen, 2 Hinweise. Warnungen betreffen verfügbare Dependency-Updates und bestehende Widget-/Style-Hinweise; der nun explizite Test-Runner ergänzt einen Versionshinweis, ohne seine bisher aufgelöste Version zu ändern.
- Negativtest `verifyPlayRelease` stoppt wie vorgesehen mit Exit-Code 1: Upload-Key fehlt. Die rechtliche Datenschutzfreigabe bleibt ebenfalls offen.
- **58/58 Android-Instrumentierungstests bestanden** auf Pixel-9-Pro-XL-Emulator, Android 17 / API 37 (vollständiger finaler Durchlauf: 173,704 Sekunden). Darunter neun Speichersicherheits- und drei reale Logbuch-UI-Regressionsfälle sowie Katalog-Retry, Gelände-/GPU-/Optikdarstellung, Karten-/Wetteraktualisierung und bestehende Sicherheitsprüfungen. Die ersten Läufe deckten Fehler in den neuen Test-Fixtures auf (Scroll-Bedienung, Activity-Result-Owner und offscreen Dialogtitel); diese wurden korrigiert, nicht übersprungen.
- App anschließend separat offline mit „Berlin Demo“ gestartet und die Sternkarte mit Milchstraße, Beschriftungen und Horizont visuell geprüft. Kein Ersatz für reale Kamera-/Sensortests.

Die als `e2e` bezeichneten JVM-Vertragstests sind nicht mit vollständigen App-Bedienungstests gleichzusetzen. Die Android-Prüfungen bedienen getrennt echte UI-/Dateispeicherpfade und verwenden isolierte Testdaten. Die Referenz für die kreisförmige SGP4-Testbahn wurde zusätzlich unabhängig mit Python `sgp4` 2.24 / WGS-72 berechnet.

## Grenzen und Testhinweise

- Test-APK mit Entwicklungs-/Debugsignatur; nicht für Play Store freigegeben. Kein neuer Upload-Key, keine ISO/IEC-27001-Zertifizierung und keine abschließende rechtliche Datenschutzfreigabe. Das AAB ist ein technisches Build-Artefakt, kein Freigabenachweis.
- SGP4 unterstützt erdnahe Bahnen unter 225 Minuten Umlaufzeit; Deep-Space-/SDP4-Bahnen werden ausdrücklich abgelehnt. Veraltete TLEs können trotz korrekter Berechnung zu falschen Überflugprognosen führen.
- Die interne Lichtkarte und ihr Score verwenden weiterhin historische NASA-Daten von 2016 und bleiben ausdrücklich als veraltet/unvalidiert gekennzeichnet.
- Reale Kamera-/Kompassgenauigkeit, OEM-Verhalten, ältere Android-Versionen und schwache GPUs sind mit diesem Emulator nicht vollständig abgedeckt. Bitte Standort-Merken/Vergessen, Logbuch-Abbrechen/Import, Optikprofile, enge Zoomstufen und AR auf weiteren Geräten prüfen. Akkutests bleiben niedrig priorisiert.
- Manuelle Logbuch-Exporte sind unverschlüsselt; ausgewählte Fotos können EXIF-Daten enthalten. Die aktualisierte Datenschutzerklärung beschreibt Speicherung und Grenzen.

Der Korrekturstand wird auf `beta` veröffentlicht. `main` bleibt beim zuvor zusammengeführten `1.1.9-pre.14`-Stand. Die SHA-256-Prüfsumme der Test-APK wird als eigenes Release-Asset bereitgestellt:

```text
463200a4f84e737c67e4c5c0531b7c684204cc9f3658758193cc21d282077ca9  projekt-astra-1.1.9-pre.15.apk
```
