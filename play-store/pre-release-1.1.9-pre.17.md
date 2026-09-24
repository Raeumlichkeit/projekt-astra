# 1.1.9-pre.17 – Querformat und Drehung

24. September 2026 · Version Code 35 · Beta-Testversion

## Änderungen

- Beim Drehen bleibt der gewählte Reiter erhalten. AR-Modus, manuelle Blickrichtung, wichtige Kartenebenen, ausgewähltes Objekt und Nachführen werden für die Activity-Neuerstellung mit einfachen IDs und Schaltern gesichert; Standortkoordinaten werden dafür nicht in den Android-Sitzungszustand kopiert.
- Eine geöffnete Sternsuche behält die Eingabe nach der Drehung. Bei geringer Querformathöhe nutzt die App kompakte Kopfbereiche und eine Symbolnavigation mit vollständigen Screenreader-Beschriftungen. Die Sternkarte gibt dem Himmel mehr Platz.
- Die Datenschutzerklärung verwendet die verfügbare Dialoghöhe; „Schließen“ bleibt außerhalb des scrollbaren Inhalts erreichbar. Im Fotodialog steht „Schließen“ vor dem Bild, das auf die restliche Höhe begrenzt ist.

## Verifikation

- 517 JVM-Tests bestanden, keine Fehler oder übersprungenen Tests.
- 64 Android-Instrumentierungstests auf dem Pixel-9-Pro-XL-Emulator mit Android 17 / API 37 bestanden. Der neue Test dreht die echte MainActivity von Hoch- nach Querformat und zurück, erhält den Wetter-Reiter und prüft die Navigation im Querformat.
- Lint: „No issues found“. Debug-APK, Android-Test-APK und minifiziertes Release-AAB erfolgreich gebaut.
- Manuelle Emulatorprüfung bei 640×360 dp und 200 % Systemschrift: kompakte Symbolnavigation und Kopfbereich angezeigt; der Datenschutzdialog ließ sich öffnen und über die sichtbare Schließen-Aktion wieder verlassen. Die simulierten Display- und Schriftwerte wurden anschließend zurückgesetzt.

## Grenzen

- Ein Emulator ersetzt keine Tests auf echten Geräten mit Kamera, Kompass und unterschiedlichen Displayausschnitten. Der Fotodialog wurde im Code geprüft, aber nicht mit einem echten Logbuchfoto im Querformat bedient. Komplexe Mess- und Star-Hop-Sitzungen sowie ungespeicherte Logbuchentwürfe wurden nicht für eine Drehung migriert.
- Bei 200 % Schrift benötigt die Datenschutzerklärung im kurzen Querformat viel Scrollen. Auf sehr kleinen Geräten und mit eingeblendeter Tastatur sind weitere Bedienungstests sinnvoll.
- Die APK ist debugsigniert und nur für Tests gedacht; keine Play-Store-Freigabe und keine ISO/IEC-27001-Zertifizierung. `main` bleibt unverändert.

## Test-APK

```text
71df27ad7ae3730e9551b848e69936bf9f64dd25447e032e761f9c73a22e6fa7  projekt-astra-1.1.9-pre.17.apk
```
