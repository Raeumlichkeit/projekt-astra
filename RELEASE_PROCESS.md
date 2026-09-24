# Release-Ablauf

Größere Änderungen werden zunächst als GitHub-Pre-Release bereitgestellt, damit sie auf mehreren Geräten getestet werden können. Eine stabile Veröffentlichung erfolgt erst nach dokumentiertem Feedback und der Freigabe durch Jeremy Grez.

## Was gilt als größere Änderung?

- neue oder deutlich geänderte Nutzerfunktionen
- Änderungen an Datenmodell, Berechtigungen, Standort-/Netzwerkverhalten oder Datenschutz
- relevante Fehlerkorrekturen, die Karten-, Wetter-, AR- oder Kalenderfunktionen beeinflussen
- Performance-, Speicher- oder Sicherheitsänderungen mit möglichem Geräteverhalten

Reine Dokumentationsänderungen, interne Testanpassungen und unveränderte Formatierungen dürfen gesammelt werden.

## Ablauf

1. `versionCode` erhöhen und eine passende Vorabversion festlegen, zum Beispiel `1.1.5-pre.1` für den ersten Testzyklus der nächsten Version.
2. Unit-, Instrumentations-, Lint- und Build-Prüfungen ausführen; bekannte Einschränkungen in den Release-Hinweisen nennen.
3. APK beziehungsweise Bundle und eine SHA-256-Prüfsumme erzeugen.
4. GitHub-Tag `v<version>-pre.<n>` erstellen und als **Pre-release** veröffentlichen. Die Release-Notiz enthält Testumfang, bekannte Einschränkungen und die Prüfsumme.
5. Den Pre-Release auf mindestens einem weiteren Android-Gerät testen und Rückmeldungen sowie Blocker dokumentieren.
6. Nach Korrektur und Freigabe den stabilen Release ohne `-pre.<n>` veröffentlichen. Der stabile Release erhält einen neuen, höheren `versionCode`.

Der Beta-Stand ist `1.1.9-pre.23` (Version Code 41), mit optionalen Satelliten, einem Kalender-Widget mit Systemfarben und entlastetem Karten-Rendering. Dieser Pre-Release wird von `beta` erstellt; `main` bleibt beim zuvor zusammengeführten Stand `1.1.9-pre.14` (Version Code 32). Die [Release-Hinweise](play-store/pre-release-1.1.9-pre.23.md) dokumentieren Tests und Grenzen. Ein Pre-Release ist kein Play-Store-Produktivstand; Datenschutz-, Signatur- und Store-Prüfungen bleiben vor der stabilen Veröffentlichung erforderlich.

Für realistische Performance-Gerätetests ab pre.23: `./gradlew.bat :app:assembleRelease -PastraTestApk=true`. Das baut optimierten, nicht debuggbaren Release-Code mit R8 und signiert die APK ausdrücklich mit dem bisherigen Debug-Testschlüssel. Die APK liegt unter `app/build/outputs/apk/release/app-release.apk`. Vor Upload die Signaturgleichheit zum vorherigen Pre-Release, Versionscode, SHA-256 und den installierten optimierten Build im Emulator prüfen. Der Flag verändert keine gespeicherten Signaturdateien; ohne ihn bleibt die reguläre Release-Signierung erhalten. `verifyPlayRelease` lehnt den Testsignatur-Flag ab.
