# Release-Checkliste für Google Play

Die funktionale Ausbauplanung und Testprioritäten stehen in der [Aufgabenliste](../AUFGABEN.md). Akkutests haben niedrige Priorität; funktionale Geräte- und Sicherheitsprüfungen werden davon getrennt behandelt.

## Verbindlicher Pre-Release-Ablauf

Jede größere funktionale, technische oder sicherheitsrelevante Änderung erhält zuerst einen eigenen GitHub-Pre-Release. Dieser enthält die testbare APK beziehungsweise das Bundle und kurze Testhinweise. Erst nach Tests auf mehreren Geräten, dokumentiertem Feedback und behobenen Blockern wird daraus ein regulärer Release. Kleine Dokumentations- und rein interne Teständerungen dürfen gesammelt werden. Der vollständige Ablauf steht in [RELEASE_PROCESS.md](../RELEASE_PROCESS.md).

## Im Projekt erledigt

- `targetSdk 37` und `compileSdk 37`
- eindeutige Application-ID `de.projektastra.app`
- Version 1.1.5 / Version Code 14
- Android App Bundle konfiguriert
- R8-Minifizierung und Ressourcenverkleinerung für Release
- optionale lokale Upload-Key-Konfiguration ohne Geheimnisse im Repository
- ausschließlich HTTPS; Klartextverkehr ist blockiert
- adaptive, runde und monochrome Launcher-Icons
- Android-12+-Splashscreen
- Android-17-Location-Button und nur sitzungsbasierter Vordergrundstandort
- keine Hintergrundstandortberechtigung
- optionale Benachrichtigungsberechtigung nur für lokale Ereigniserinnerungen
- WorkManager-Aufträge werden beim Entfernen eines Ereignisses wieder gelöscht
- Kamera und Sensoren als optionale Hardwaremerkmale
- offline lesbarer Datenschutztext und getrennte Online-/Geländefreigabe in der App
- Verantwortlicher Jeremy Grez, Support/Datenschutz jeregrez@gmail.com
- Store-Texte, Datensicherheitsentwurf und Grafikpaket
- Debug- und Release-Tests, Lint und Emulatorprüfung

## Vom Entwickler in Play Console zu erledigen

1. Google-Play-Entwicklerkonto anlegen beziehungsweise Identität bestätigen.
2. Prüfen, ob die Application-ID dauerhaft `de.projektastra.app` bleiben soll. Paketnamen können nach dem ersten Upload nicht wiederverwendet werden.
3. Einen privaten Upload-Key erzeugen, sicher sichern und `keystore.properties.example` lokal als `keystore.properties` ausfüllen.
4. Das signierte `app-release.aab` erzeugen und Play App Signing aktivieren.
5. Support-E-Mail jeregrez@gmail.com und eine öffentlich erreichbare, nicht editierbare Datenschutz-URL eintragen. Anbieterrollen, Rechtsgrundlagen, Drittlandübermittlungen und Löschfristen rechtlich prüfen; erst anschließend den Prüfmarker `LEGAL_REVIEW_PENDING` im freigegebenen Text entfernen.
6. Store-Texte und Grafiken aus diesem Verzeichnis hochladen.
7. Datensicherheitsformular anhand `data-safety-de.md` ausfüllen und Standort-, Karten- sowie Benachrichtigungsnutzung gegen die aktuelle Play-Console-Abfrage prüfen.
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
.\gradlew.bat verifyPlayRelease
```

Ausgabe: `app/build/outputs/bundle/release/app-release.aab`

Ohne `keystore.properties` bleibt `bundleRelease` technisch baubar, aber das Bundle ist unsigniert. `verifyPlayRelease` stoppt dann ausdrücklich. Passwörter niemals in Shell-Argumente, Git oder Chat schreiben. Anschließend die Signatur mit `jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab` prüfen; Signaturblock allein ersetzt keine kryptografische Prüfung. Den Upload-Key verschlüsselt außerhalb des Repositories sichern und Wiederherstellung testen.

Vor öffentlichem GitHub-/Store-Release alte Git-Versionen von Screenshots auf Standortdarstellungen prüfen. Neue Demo-Screenshots entfernen alte Bilder nicht aus der Historie; keine Historienumschreibung ohne ausdrückliche Freigabe. Der technische Sicherheitsstand ist keine ISO-27001-Zertifizierung; siehe `SECURITY_REMEDIATION.md`.
