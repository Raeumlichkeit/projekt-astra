# 1.1.9-pre.23 – Optionale Satelliten, Kalender-Widget und Karten-Performance

24. September 2026 · Version Code 41 · Beta-Testversion

## Änderungen

- Satellitenbahnen und ihre Beschriftungen sind standardmäßig ausgeschaltet. In der ausgeklappten Kartenbedienung lassen sie sich über „Satelliten“ einschalten; dieselbe Einstellung gibt es unter „Optik & FOV“. Die Auswahl bleibt nach einem Neustart erhalten. Im ausgeschalteten Zustand werden auch die zugehörigen Bahnberechnungen ausgesetzt.
- Die Sternkarte berechnet die Farben der Katalogsterne nicht mehr bei jedem Schwenk neu. Der Milchstraßen-Renderer übernimmt nach einer aufwendigen Initialisierung den neuesten angeforderten Blickwinkel und verarbeitet Wiederholungen auf seinem Render-Thread. Die Texturauflösung bleibt unverändert.
- Die Test-APK enthält erstmals optimierten, nicht debuggbaren Release-Code mit R8. Sie wird weiterhin ausdrücklich mit dem bisherigen Testschlüssel signiert und lässt sich über pre.22 installieren. Die kleinere Datei und der optimierte Code sind keine Zusage für eine bestimmte Bildrate.
- Das Startbildschirm-Widget zeigt den nächsten Eintrag aus Astras Himmelskalender, bei ausreichend großer Mindesthöhe auch einen zweiten. Ein Tipp auf den Termin öffnet den Kalender. Es werden die bereits lokal verfügbaren astronomischen Kalenderdaten verwendet, nicht persönliche Android-Kalender.
- Auf Android 12 und neuer nutzt das Widget die systemseitige Farbpalette mit heller und dunkler Variante. Ältere Android-Versionen erhalten passende Ersatzfarben. Die Berechnung läuft außerhalb des Broadcast-Hauptthreads; bei großer Schrift bleibt die kompakte Ansicht lesbar.

## Verifikation

- 521 JVM-Tests bestanden; Android Lint ohne Fehler oder Warnungen. Optimierte, testsignierte Release-APK gebaut, Versionscode und Signatur geprüft und als Update über die pre.22-APK im Android-17-Emulator installiert.
- Ein vollständiger Lauf mit 80 Instrumentierungstests bestand vor der abschließenden Renderer-Korrektur. Danach bestanden erneut alle 12 betroffenen Rendering-Tests, einschließlich des neuen Tests mit echter Compose-Einbettung, Canvas-Überlagerung und mehreren Blickrichtungswechseln ohne Bitmap-Lesen zwischen den Frames. Diese Läufe beziehen sich ausdrücklich auf unterschiedliche Zwischenstände.
- Im optimierten Build manuell geprüft: Satelliten an/aus und gespeicherte Auswahl nach Neustart, bewegte Milchstraße beim Schwenken, Minimieren/Wiederöffnen ohne fehlende Texturflächen sowie Kalenderaufruf bei laufender und zuvor geschlossener App.
- Widget im tatsächlichen Emulator-Launcher geprüft: sichtbarer Draconiden-Termin, Öffnen des Himmelskalenders durch Antippen sowie Wechsel zwischen hellen und dunklen Systemfarben. Automatisierte Layout-Tests prüfen zusätzlich kleine Größen und große Schrift.
- Ein unabhängiger nativer Subagent hat den integrierten Stand überprüft; die Hauptinstanz hat Builds, Emulatorprüfungen und Freigabe verantwortet.

## Grenzen

- Das verbleibende Ruckeln auf dem betroffenen Handy ist noch nicht abschließend überprüft. Die Emulator-Messung fällt günstiger aus, ersetzt aber keinen Vergleich auf echter Hardware. Bitte gerade das Schwenken der Milchstraße mit dieser optimierten APK erneut testen.
- Kamera, reale Sensoren, GPS und die Widget-Farbübernahme auf anderen Herstellern/Launchern wurden nicht auf echten Geräten getestet. Die konkrete Systempalette und Darstellung hängen von Android-Version und Launcher ab.
- Ohne verfügbare Wetterdaten zeigt das Widget weiterhin „—“ statt eines erfundenen Scores. Gespeicherter Standort und Offline-/Datenschutzeinstellungen bleiben maßgeblich; das Widget fragt keinen neuen Hintergrundstandort ab.
- Keine neuen Berechtigungen oder zusätzlichen Netzwerkzugriffe für diese Funktionen. Testsignierte Vorabversion, kein Play-Store-Produktivstand und keine ISO/IEC-27001-Zertifizierung. Veröffentlichung von `beta`; `main` bleibt unverändert.

## Test-APK

`projekt-astra-1.1.9-pre.23.apk` · 8.528.707 Bytes

SHA-256: `ba9977ec034f196c47373c7cd5844e26869961a5ba8a65b6d44e730ba6a5bf18`
