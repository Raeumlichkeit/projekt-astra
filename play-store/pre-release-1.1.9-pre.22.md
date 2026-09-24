# 1.1.9-pre.22 – Standortknopf und Schwenk-Optimierung

24. September 2026 · Version Code 40 · Beta-Testversion

## Änderung

Der Android-17-Standortknopf lag nach dem Aufklappen der Kartenbedienung über der Sternkarte. Android zeichnet ihn als eigene, oben liegende `SurfaceView`, die nicht vom Compose-Scrollbereich abgeschnitten wird. Im Hochformat sitzt der native Berechtigungsknopf jetzt in einem festen Bereich oberhalb der Karte; nur die übrigen Bedienelemente scrollen. Im kompakten Querformat öffnet ein schlanker „Standort verwenden“-Eintrag den nativen Knopf in einem eigenen Dialog, damit die Kartenbedienung nutzbar bleibt. Beim Einklappen beziehungsweise Schließen verschwindet der Knopf.

Beim manuellen Schwenken werden die Richtungsvektoren der Katalogsterne einmal für den gewählten Ort und Zeitpunkt vorberechnet. Pro Bild entfallen damit die wiederholten trigonometrischen Berechnungen für mehrere Tausend Sterne. Wiederholte Updates mit unverändertem Himmelszustand lösen keinen zusätzlichen GPU-Frame aus; bei Resume oder Größenänderung wird weiterhin neu gezeichnet. Der Milchstraßen-Shader spart zwei redundante Normalisierungen pro Bildpunkt. Die Genauigkeit der Sternpositionen und der volle Texturpuffer bleiben erhalten.

## Verifikation

- Den Button-Überlauf mit pre.21 auf dem Android-17-Emulator reproduziert. In pre.22 bei aufgeklappter, gescrollter und wieder eingeklappter Kartenbedienung visuell geprüft: Der Knopf überdeckt die Karte nicht mehr. Im Querformat sind die übrigen Bedienelemente scrollbar; der Standort-Dialog wurde geöffnet und ohne zurückbleibende Schaltfläche geschlossen.
- Zwei API-37-Instrumentierungstests prüfen den Button im Hochformat sprachunabhängig mit Bildpixeln unterhalb der Panelkante sowie den Querformat-Auslöser, das Dialog-Öffnen und „Abbrechen“. Projektionstests vergleichen den vorberechneten Pfad mit dem bisherigen manuellen und AR-Pfad über Pan, Zoom und Azimut-Übergänge. GPU-Tests prüfen die Milchstraßen-Registrierung einschließlich Polnähe und das Ausbleiben überflüssiger Frames.
- Vollständiger Lauf: 518 JVM-Tests und 76 Instrumentierungstests auf dem Android-17-Emulator bestanden; Android Lint ohne Fehler oder Warnungen. Debug-Test-APK und minifiziertes Release-Bundle gebaut.

## Grenzen

- Die geringere Rechenarbeit ist verifiziert; eine durchgehend ruckelfreie Darstellung auf dem betroffenen Handy ist damit noch nicht nachgewiesen. Der Emulator eignet sich nicht als verlässliche Framerate-Messung für dessen GPU. Ein Vergleich auf dem echten Gerät und bei Bedarf ein Frame-Trace für Stern-Overlay und Milchstraßen-Textur stehen aus.
- Kamera, Sensoren, Standortfreigabe und Grafikleistung wurden nicht auf einem echten Handy getestet.
- Die APK ist debugsigniert und nur für Tests gedacht. Keine neuen Berechtigungen oder Datenübertragungen; keine Play-Store-Freigabe und keine ISO/IEC-27001-Zertifizierung. `main` bleibt unverändert.

## Test-APK

`projekt-astra-1.1.9-pre.22.apk` · SHA-256: `16a22a697b5a05cc6bfc6fac5ffb91da7de258d0428e897b7339a982e1663d9c`
