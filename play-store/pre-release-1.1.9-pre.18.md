# 1.1.9-pre.18 – Flüssigeres Verschieben der Milchstraße

24. September 2026 · Version Code 36 · Beta-Testversion

## Änderungen

- Die manuelle Sternkarte verarbeitet jedes empfangene Wisch- und Zoom-Delta gegen den aktuellen Blickwinkel. Mehrere Touch-Ereignisse vor einer Compose-Neuzeichnung überschreiben sich nicht mehr; das Lösen einer aktiven Ziel-Nachführung übernimmt deren Blickrichtung einmalig.
- Die Tipp-Erkennung der Karte wird beim Schwenken nicht mehr für jeden neuen Blickwinkel neu gestartet. Die perspektivische Sternprojektion vermeidet eine unnötige Winkel-Normalisierung pro Objekt.
- Die Milchstraßen-Textur nutzt die bilineare GPU-Abtastung außerhalb der schmalen RA-Naht. Azimut-/Höhenbasis und Brennweite werden für die manuelle Projektion einmal pro Bild statt pro Bildpunkt berechnet. RA-Naht, AR-Ansicht und Optik-Drehung/-Spiegelung bleiben unverändert.

## Verifikation

- 517 JVM-Tests bestanden; keine Fehler oder übersprungenen Tests.
- 66 Android-Instrumentierungstests auf dem Pixel-9-Pro-XL-Emulator mit Android 17 / API 37 bestanden, darunter schnelle Wischfolgen und 80 unmittelbar aufeinanderfolgende Textur-Updates mit Prüfung der letzten Blickrichtung.
- Lint: „No issues found“. Debug-APK, Android-Test-APK und minifiziertes Release-AAB gebaut.
- Manuelle Emulatorprüfung mit wiederholten horizontalen Wischbewegungen: Milchstraße, Sterne und Horizont bleiben sichtbar; die Textur ist nach dem Schwenk zur letzten Blickrichtung ausgerichtet.

## Grenzen

- Die getrennten Renderpfade für Sterne (Compose) und Milchstraße (TextureView/OpenGL) können auf langsamen Geräten während eines laufenden Schwenks weiterhin kurz auseinanderlaufen. Die Tests beweisen den korrekten Endzustand, nicht durchgehend 60 Bilder pro Sekunde. Ein Test auf einem schwächeren echten Gerät bleibt nötig.
- Kamera- und Sensorverhalten wurden in diesem Schritt nicht auf einem echten Gerät geprüft. Keine neuen Berechtigungen oder Datenübertragungen.
- Die APK ist debugsigniert und nur für Tests gedacht; keine Play-Store-Freigabe und keine ISO/IEC-27001-Zertifizierung. `main` bleibt unverändert.

## Test-APK

```text
a4b46026519ce52904e8c81f5ef3454ce2d246bb7a4e70402c0198c7d055bb99  projekt-astra-1.1.9-pre.18.apk
```
