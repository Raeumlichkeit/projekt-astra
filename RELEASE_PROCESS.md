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

Der aktuelle Entwicklungsstand der Zeitsteuerung ist als `1.1.5-pre.1` vorgesehen. Ein Pre-Release ist kein Play-Store-Produktivstand; Datenschutz-, Signatur- und Store-Prüfungen bleiben vor der stabilen Veröffentlichung erforderlich.
