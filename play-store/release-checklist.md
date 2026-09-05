# Release-Checkliste für Google Play

## Im Projekt erledigt

- `targetSdk 37` und `compileSdk 37`
- eindeutige Application-ID `de.projektastra.app`
- Version 1.0.0 / Version Code 8
- Android App Bundle konfiguriert
- R8-Minifizierung und Ressourcenverkleinerung für Release
- optionale lokale Upload-Key-Konfiguration ohne Geheimnisse im Repository
- ausschließlich HTTPS; Klartextverkehr ist blockiert
- adaptive, runde und monochrome Launcher-Icons
- Android-12+-Splashscreen
- Android-17-Location-Button und nur sitzungsbasierter Vordergrundstandort
- keine Hintergrundstandortberechtigung
- Kamera und Sensoren als optionale Hardwaremerkmale
- Datenschutztext in der App
- Store-Texte, Datensicherheitsentwurf und Grafikpaket
- Debug- und Release-Tests, Lint und Emulatorprüfung

## Vom Entwickler in Play Console zu erledigen

1. Google-Play-Entwicklerkonto anlegen beziehungsweise Identität bestätigen.
2. Prüfen, ob die Application-ID dauerhaft `de.projektastra.app` bleiben soll. Paketnamen können nach dem ersten Upload nicht wiederverwendet werden.
3. Einen privaten Upload-Key erzeugen, sicher sichern und `keystore.properties.example` lokal als `keystore.properties` ausfüllen.
4. Das signierte `app-release.aab` erzeugen und Play App Signing aktivieren.
5. Support-E-Mail und eine öffentlich erreichbare, nicht editierbare Datenschutz-URL eintragen.
6. Store-Texte und Grafiken aus diesem Verzeichnis hochladen.
7. Datensicherheitsformular anhand `data-safety-de.md` ausfüllen.
8. Angaben zu App-Zugriff, Werbung, Zielgruppe, Nachrichten-Apps, Gesundheitsfunktionen und Inhaltsbewertung beantworten. Vorschlag: keine Anmeldung, keine Werbung, nicht speziell für Kinder, Kategorie Bildung.
9. Interne Prüfung starten und den automatischen Pre-Launch-Report bearbeiten.
10. Falls das persönliche Entwicklerkonto nach dem 13. November 2023 erstellt wurde: geschlossenen Test mit mindestens 12 dauerhaft angemeldeten Testern über 14 Tage durchführen und anschließend Produktionszugang beantragen.

## Upload-Key erzeugen

```powershell
keytool -genkeypair -v -keystore upload-key.jks -alias upload -keyalg RSA -keysize 4096 -validity 10000
Copy-Item keystore.properties.example keystore.properties
```

Danach Passwörter und Dateipfad nur in der ignorierten Datei `keystore.properties` eintragen.

## Signiertes Bundle bauen

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat clean bundleRelease
```

Ausgabe: `app/build/outputs/bundle/release/app-release.aab`
