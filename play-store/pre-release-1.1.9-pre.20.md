# 1.1.9-pre.20 – Milchstraße nach dem Wiederöffnen vollständig

24. September 2026 · Version Code 38 · Beta-Testversion

## Änderung

Nach dem Minimieren konnte Android den `TextureView`-Puffer auf die volle Kartengröße zurücksetzen. Die in pre.19 eingeführte 75-%-Verkleinerung zeichnete dann nur noch ein Rechteck unten links, während Sterne und Linien die ganze Karte füllten. Die Verkleinerung ist entfernt. Der OpenGL-Puffer wird weiterhin bei echten Größenänderungen der Kartenfläche erneuert. Die übrigen Optimierungen aus pre.19 bleiben erhalten.

## Verifikation

- Der Fehler wurde mit pre.19 auf dem Android-17-Emulator als exakt begrenztes Texturrechteck reproduziert. Mit diesem Stand blieb die Milchstraße nach drei Home-/Wiederöffnen-Zyklen in derselben Blickrichtung über die ganze Karte sichtbar.
- Ein neuer Instrumentierungstest simuliert den Android-Puffer-Reset bei 901 × 901 Pixeln und prüft nach dem Hintergrund-/Vordergrundwechsel 25 Bildpunkte einschließlich Ecken und Rändern. Der bestehende Test für echte Größenänderungen sowie der manuell-/AR-Wechseltest bestehen ebenfalls.
- Vollständiger Lauf: 517 JVM-Tests und 69 Instrumentierungstests auf dem Android-17-Emulator bestanden; Android Lint meldet keine Probleme. Debug-Test-APK und minifiziertes Release-Bundle wurden erfolgreich gebaut.

## Grenzen

- Die entfernte Pufferverkleinerung hatte die isolierte Milchstraßen-Ebene beschleunigt; dieser Teil des Geschwindigkeitsgewinns entfällt. Das allgemeine Wisch-Ruckeln bleibt eine offene Aufgabe und muss auf einem schwächeren echten Gerät profiliert werden.
- Die Emulatorprüfung ersetzt keinen Test auf dem betroffenen Handy; echte Kamera- und Sensorfunktionen wurden hier nicht erneut geprüft. Keine neuen Berechtigungen oder Datenübertragungen. Die APK ist debugsigniert und nur für Tests gedacht; keine Play-Store-Freigabe und keine ISO/IEC-27001-Zertifizierung. `main` bleibt unverändert.

## Test-APK

`projekt-astra-1.1.9-pre.20.apk` · SHA-256: `bc2245d5cc2e9be856266fa881a720f36f50f826bb8832fcc4f3949dd03a0450`
