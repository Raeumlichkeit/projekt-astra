# Sicherheitsmaßnahmen – Version 1.1.1 / Code 10

Stand: 6. September 2026. Ausgangspunkt: Commit `72cf296e208707f98646d80271eeab33dba94ff9`, Version 1.1.0. Der [ursprüngliche Prüfbericht](SECURITY_REVIEW.md) bleibt als Befundhistorie erhalten; seine Zeilenverweise beziehen sich auf diesen Ausgangsstand.

## Ergebnis und Grenzen

Die identifizierten technischen Risiken wurden reduziert, nicht sämtliche denkbaren Risiken ausgeschlossen. Keine ISO/IEC-27001-Zertifizierung, kein vollständiger Penetrationstest und keine abschließende rechtliche Datenschutzfreigabe. Eine App allein kann keinen organisatorischen ISMS-Nachweis ersetzen. Die [ISO beschreibt ISO/IEC 27001 als Anforderungen an ein Informationssicherheitsmanagementsystem](https://www.iso.org/standard/27001).

Verantwortlicher/Support nach Nutzervorgabe: Jeremy Grez, jeregrez@gmail.com. Die App bleibt bis zur rechtlichen Freigabe und Einrichtung des Upload-Keys eine private Testversion; es wurde nichts im Play Store veröffentlicht.

## Nachverfolgung der Befunde

| Befund aus 1.1.0 | Maßnahme in 1.1.1 | Verbleibende Grenze |
| --- | --- | --- |
| Genaue Standortabfragen ohne gesonderte Online-Auswahl | Offline-Start, widerrufbare Online-Freigabe; Wetter/Karten auf 0,01° gerundet; genaue Geländepunkte nur nach zweitem Schalter | Gerundete Orte sind weiterhin Standortdaten; IP/Anfrageprotokolle bei Anbietern bleiben möglich |
| WebView-Cache enthält Standortparameter | Keine Cookies/DOM-/Browser-Diskcaches; alle externen Ressourcen durch native HTTPS-Freigabeliste; alte WebView-Daten werden vor der ersten neuen WebView entfernt | OSM-Kachelbilder lassen betrachtete Regionen erkennen; keine Anonymisierungsbehauptung |
| Unvollständige Backup-Ausschlüsse | Cloud- und Geräteübertragung ausdrücklich für root, file, database, sharedpref, external und device-geschützte Speicherbereiche ausgeschlossen | Kein Schutz gegen Root-Zugriff, manuelle Exporte oder kompromittierte Geräte; Hersteller-Backupimplementierungen nicht flächendeckend getestet |
| GPS-Listener bleibt bei gestoppter Activity aktiv | LifecycleStartEffect mit onStopOrDispose; Netzwerkfreigabe und Repository-Caches bei onStop zurückgesetzt | Emulatorprüfung auf Android 17, kein vollständiger Gerätetest für alle unterstützten Android-Versionen |
| Unbeschränkte PDF-Verarbeitung/verwundbares pypdf 6.0.0 | pypdf 6.17.0 mit Wheel-Hash; isolierter Android-Service (eigene UID, nicht exportiert, keine App-Daten/Netzberechtigung), Größen-/Seiten-/Text-/Zeitlimits | Android-PDFBox-Port bleibt 2.0.27.0; Isolation ist eine Risikominderung, keine Garantie gegen Parser- oder Betriebssystemlücken |
| Unzutreffende/unvollständige Datenschutzaussagen | Offline lesbarer Text mit Empfängern, IP/Metadaten, Open-Meteo-Frist, Cache-/Löschregeln und Kontakt; separate technische Freigaben | Rechtliche Prüfung von Anbieterrollen, Rechtsgrundlagen und Drittlandtransfers vor öffentlichem Release offen |
| WebView-Navigation und Lieferkette | Nur lokale HTML-Dokumente in der internen Origin; CSP/Script-Nonce; externe Links nach Benutzeraktion im Browser; keine Redirects im nativen Downloader; Action-SHAs und getrennte CI-Rechte; Wrapper-Hash, Dependency-Lock/Verifikation | HTTPS und Prüfsummen beweisen keine Fehlerfreiheit der Quelle; Erstaufnahme der Maven-Hashes basiert auf dem aufgelösten Paketbestand |
| Unsigned Release wurde als signiert verstanden | `verifyPlayRelease` stoppt ohne Upload-Key/Signatur und bei offener Datenschutzfreigabe; offizieller Gradle-Wrapper liefert Fehlercodes korrekt zurück | Upload-Key ist weiterhin nicht eingerichtet, AAB unsigniert; keine Passwörter erzeugt oder veröffentlicht |

## Daten und Aufbewahrung

- Genaue GPS-Werte: Arbeitsspeicher für lokalen Himmel/Kalender; kein Standortverlauf in App-Einstellungen. Für Gelände nur mit zusätzlicher Freigabe an Open-Meteo.
- Wetter/Gelände/Radar/Nachtlicht/Objektbilder: keine Speicherung durch den App-Downloader. WebViews erhalten Antworten mit `Cache-Control: no-store`; native Verbindungscaches sind abgeschaltet.
- Öffentliche OSM-Bildkacheln: eigener privater `no_backup/public_map_tiles`-Cache, maximal 32 MB; Antwort-Cache-Control wird berücksichtigt, Gültigkeit höchstens 30 Tage. Abgelaufenes wird beim nächsten Start/Abruf gelöscht. Manuelle Löschung unter Info; laufende ältere Abrufe dürfen einen geleerten Cache nicht erneut befüllen. Keine rohen URL-/GPS-Dateinamen, aber Kacheln bleiben geographisch zuordenbar.
- Favoriten, Ereignisse, Erinnerungsvorlauf, Rotlicht-/Online-Auswahl und öffentliche IMO-Kalender: private App-Einstellungen; lokale Android-Erinnerungsaufträge in WorkManager. Bis Änderung, App-Daten-Löschung oder Deinstallation. Kein Cloud-Backup.
- Anbieter: IP, Zeit und angefragte Ressource; [Open-Meteo nennt bis zu 90 Tage einschließlich Geo-/IP-Protokollen](https://open-meteo.com/en/terms). Keine pauschale flüchtige Verarbeitung behaupten. Andere Anbieterfristen nicht vollständig verifiziert.
- Store-Screenshots: neue Aufnahmen verwenden ausdrücklich den Demo-Standort Berlin. Historische Git-Versionen werden dabei nicht gelöscht. Vor einer öffentlichen Freigabe des Repositorys muss entschieden werden, ob vorhandene Standortdarstellungen in der Historie entfernt werden sollen. Kein Force-Push/keine Historienumschreibung ohne ausdrückliche Freigabe.

## Prüfungen und reproduzierbare Nachweise

Die generierten Logs liegen im ignorierten `app/build/`, nicht als dauerhaftes Telemetriearchiv im Repository.

- 16 JVM-Tests: 10 Astronomiereferenztests, 6 Sicherheits-/Validierungstests. HTTPS-Quellen, Host-/Pfadumgehungen, Offline-/Vordergrund-/Geländesperre, Rundung, Größenlimit, PDF-Textlimit, ungültige Kalenderwerte.
- 6 Android-Instrumentierungstests bestanden: WebView-Einstellungen, private Service-Isolation und gültige PDF-Auswertung, fehlerhafte PDFs, Cache-Löschung ohne Favoritenverlust, isolierte Testfixture für Altcache-Migration, lokales HTML ohne Online-Freigabe. Berichte: `app/build/outputs/androidTest-results/connected/`.
- 5 Python-Tests: Schema, ungültige Koordinaten, Magic-/Größenlimit, ungültiges PDF, gültiges Leer-PDF. Beide offiziellen Kalender 2026/2027 zusätzlich über HTTPS geladen, im Kindprozess verarbeitet und als 22 validierte Schauer eingelesen (`app/build/security/imo-validated.json`).
- Debug-APK, minifiziertes Release-AAB und Lint gebaut. Strikte normale Gradle-Läufe nach Erzeugung der Verifikationsmetadaten durchgeführt; keine Deaktivierung der Prüfung. `lint-results-debug.txt`: „No issues found“ – kein Nachweis allgemeiner Sicherheit.
- Negativtest `verifyPlayRelease`: Exit-Code 1 und explizite Meldung zum fehlenden Upload-Key. Das normale `bundleRelease` ist kein Signaturnachweis.
- Offizieller Gradle-9.7.1-Wrapper neu erzeugt: JAR-SHA-256 `7a9ce74cff467ca1bf60a4fcd9f05185acceda4d0f382434d393e17864262c5d`, mit [Gradles veröffentlichter Prüfsumme](https://services.gradle.org/distributions/gradle-9.7.1-wrapper.jar.sha256) abgeglichen. Distribution-SHA-256 ebenfalls fest in den Wrapper-Einstellungen.
- Wetterkarte mit Regenradar/Bewölkung im Emulator visuell geprüft. Scan nach Kartenabruf in `app_webview`, `cache`, `code_cache`, `no_backup` und `shared_prefs`: 0 Dateien mit `latitude=[0-9-]` oder `longitude=[0-9-]`; öffentliche OSM-Bilder im separaten privaten Cache vorhanden. Das ist eine gezielte Stichprobe, kein forensischer Beweis, dass niemals Standortinformationen gespeichert werden.
- Kartencache über den Info-Schalter gelöscht und anschließend leer vorgefunden. Online-Schalter deaktiviert: Wetter zeigt den Offline-Hinweis; Datenschutzerklärung inklusive Verantwortlichem/Kontakt funktioniert weiterhin ohne Online-Freigabe. Kalender/Nachtlichtkarte mit Demo-Standort geprüft.
- Kein vollständiger Netzwerk-Mitschnitt, kein TLS-Pinning-/MITM-Penetrationstest, keine unabhängige Zertifizierungsprüfung. Nach der Arbeitsunterbrechung reagierte der Android-Testemulator auch auf Systembefehle nicht; ein Kaltstart des Emulators behob dies. App-Testdaten wurden dabei nicht nochmals gelöscht.

Die Android-Testinstallation kann die App im Emulator deinstallieren und dortige Testdaten zurücksetzen. Sie wurde nicht auf einem persönlichen Telefon ausgeführt. Die Migrationsroutine selbst wird separat auf Erhalt anderer Dateien/Favoriten getestet.

## Organisatorische Aufgaben vor ISO-/öffentlicher Release-Aussage

Die folgenden Punkte sind **offen**, nicht als erledigte Auditnachweise zu verstehen. Verantwortung: Projektbetreiber; bei öffentlicher Veröffentlichung fachkundige Datenschutz-/ISMS-Prüfung hinzuziehen.

1. ISMS-Geltungsbereich, Informationswerte, Verantwortungen, dokumentierte Risikobewertung und Behandlung festlegen; Anwendbarkeitserklärung und Genehmigung verbleibender Risiken erarbeiten.
2. Dienstleister-/Datenschutzprüfung abschließen, tatsächliche Aufbewahrungsfristen und gegebenenfalls Verträge/Transfergrundlagen dokumentieren; Datenschutzseite öffentlich hosten und Play-Datensicherheitsangaben gegen den Release prüfen.
3. Upload-Key lokal anlegen, geschützt sichern, Zugriff und Wiederherstellung testen. Repository-/Play-Zugänge mit starker Anmeldung/MFA und minimalen Rechten betreiben. Keine Schlüssel oder Passwörter in Chat/Repository.
4. Wiederkehrenden Schwachstellen-/Dependency-Review und Reaktionsprozess festlegen. Parserisolation nicht als Ersatz für Updates ansehen. Neue Dependency-Hashes bewusst prüfen und im Versionsverlauf begründen.
5. Vorfallbehandlung, sichere Meldewege, Lösch-/Wiederherstellungstests, Änderungsfreigaben und interne Audits mit realen Nachweisen betreiben. Managementbewertung und gegebenenfalls externe Zertifizierung planen.
6. Reale Geräte einschließlich unterstützter älterer Android-Versionen, Lebenszykluswechsel, Berechtigungsentzug und fehlerhafte Netzverbindungen testen. Store-Prelaunch-Bericht bearbeiten; Git-Historie und öffentliche Artefakte prüfen.

Referenzen: [ISO/IEC 27001](https://www.iso.org/standard/27001), [Android Lifecycle](https://developer.android.com/topic/libraries/architecture/lifecycle), [isolierte Android-Services](https://developer.android.com/guide/topics/manifest/service-element), [PDFBox-Sicherheit](https://pdfbox.apache.org/security.html), [OSM-Kachelrichtlinie](https://operations.osmfoundation.org/policies/tiles/), [GitHub Actions Sicherheit](https://docs.github.com/en/actions/reference/security/secure-use), [Gradle Dependency Verification](https://docs.gradle.org/current/userguide/dependency_verification.html).
