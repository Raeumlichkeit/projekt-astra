# Sicherheits- und Datenschutzprüfung – Projekt Astra

Prüfdatum: 5. September 2026. Geprüft: Version 1.1.0 / Code 9, Commit `72cf296e208707f98646d80271eeab33dba94ff9`.

Nachtrag: Maßnahmen und Tests für Version 1.1.1 stehen in [SECURITY_REMEDIATION.md](SECURITY_REMEDIATION.md). Dieser Bericht dokumentiert bewusst den Ausgangsstand; Quellzeilen können sich inzwischen verschoben haben.

## Ergebnis

Der geprüfte Stand erhält aus dieser Prüfung keine pauschale Sicherheits- oder Datenschutzfreigabe. Insbesondere ist die Aussage, personenbezogene Daten würden außerhalb des Gerätes nicht gespeichert, nicht belegbar: Open-Meteo nennt ausdrücklich Serverprotokolle mit möglichen Standortdaten und eine Löschung nach 90 Tagen. Im vorhandenen Emulator wurden außerdem dauerhafte WebView-Dateien mit numerischen Standortparametern gefunden.

Eine Konformität oder Zertifizierung nach DIN EN ISO/IEC 27001 wurde nicht festgestellt. Dieser Bericht ist eine technische Bestandsaufnahme mit einer begrenzten ISMS-Lückenbewertung. Er ist kein Zertifizierungsaudit und kein Nachweis vollständiger DSGVO-Konformität. Ein erfolgreicher Build und Android-Lint ohne Befunde reichen hierfür nicht aus.

Es wurden keine Produktdateien, Berechtigungen, Nutzerdaten oder GitHub-Einstellungen verändert. Nur dieser lokale Bericht wurde hinzugefügt. Es erfolgte kein Upload des Quellcodes oder gespeicherter Nutzerdaten an externe Scanner.

## Konkrete Befunde und Abhilfen

Die Prioritäten bezeichnen die Bearbeitungsreihenfolge für dieses Projekt; sie sind keine CVSS-Bewertung.

### 1. Hohe Priorität: Standortübertragung und externe Aufbewahrung

Beleg: [MainActivity.kt](<C:/Users/Jeremy/Documents/Chati/Projekt Astra/app/src/main/java/de/projektastra/app/MainActivity.kt:2842>) sendet Wetteranfragen mit der vollständigen verfügbaren GPS-Genauigkeit. Die Geländeanfrage übermittelt an Zeile 2631 sowie 2638–2641 den Mittelpunkt und umliegende Koordinaten mit fünf Nachkommastellen. Sie startet bereits in der Sternkarte nach Standortfreigabe. Der Berechtigungshinweis in Zeile 730 beschreibt die Verwendung als lokal.

Open-Meteo erklärt für seine freie API, dass IP-Adressen verarbeitet werden können und Serverprotokolle geografische Koordinaten enthalten können; diese Protokolle werden nach 90 Tagen gelöscht. Das belegt eine mögliche externe Speicherung, aber keinen unberechtigten Zugriff oder eingetretenen Datenschutzvorfall. Die App kann diese Protokolle beim lokalen Löschen nicht entfernen. [Anbieterbedingungen](https://open-meteo.com/en/terms)

Abhilfe: Übertragung und Aufbewahrung vor der Nutzung verständlich erklären. Wetterkoordinaten auf die benötigte räumliche Genauigkeit reduzieren. Für das genauere Geländeprofil eine klare Entscheidung über die Online-Nutzung anbieten oder lokale Geländedaten verwenden. Wenn keine externen Standortprotokolle akzeptabel sind, muss die Datenversorgung technisch entsprechend geändert werden; HTTPS allein verhindert Serverprotokolle nicht.

### 2. Hohe Priorität: Standortspuren in lokalem Karten-Cache und Store-Material

Beleg: [MainActivity.kt](<C:/Users/Jeremy/Documents/Chati/Projekt Astra/app/src/main/java/de/projektastra/app/MainActivity.kt:1553>) und Zeile 1899 aktivieren DOM-Speicher und den normalen WebView-Datenträgercache. Beim Schließen wird die WebView zerstört, ihr persistenter Speicher aber nicht gelöscht. Die Wetterkarte lädt Standortparameter über JavaScript nach.

Die ausschließlich lesende Prüfung des bereits benutzten Debug-Emulators fand unter `cache`/`app_webview` jeweils zwei Dateien mit `latitude=` beziehungsweise `longitude=` gefolgt von einer Ziffer, vier Dateien mit OSM-Kachelpfaden und neun mit dem RainViewer-Kachelhost. Der WebView-HTTP-Cache belegte ungefähr 724 KiB. Es existieren auch Cookie-Datenbank-, Local-Storage- und Session-Storage-Dateien. Deren Existenz beweist keine Tracking-Cookies; deren Inhalte wurden nicht vollständig ausgewertet. Die Festplattenreste können aus bisherigen App-Sitzungen und älteren installierten Builds stammen. Die aktuelle Implementierung räumt sie nicht gezielt auf.

Die versionierten [Wetter-Screenshots](<C:/Users/Jeremy/Documents/Chati/Projekt Astra/play-store/screenshots/02-wetter.png>) und [Kalender-Screenshots](<C:/Users/Jeremy/Documents/Chati/Projekt Astra/play-store/screenshots/03-kalender.png>) zeigen zudem einen konkreten Teststandort. Ob dies ein realer privater Standort ist, wurde nicht ermittelt. Eine Veröffentlichung des Repositorys oder der Store-Bilder würde diese Darstellung weitergeben.

Abhilfe: Aufbewahrung und Löschung für Standortantworten, Karten und WebView-Speicher ausdrücklich festlegen; unnötige Cookies/DOM-Speicherung deaktivieren und vorhandene Altbestände bei einer Umstellung berücksichtigen. OSMs Kachel-Cachevorgaben bei der Lösung beachten; eine pauschale Abschaltung aller Kachel-Caches ist nicht automatisch geeignet. Store-Bilder mit einem bewusst gewählten Demo-Ort neu erstellen. Falls vorhandene Bilder sensible Orte zeigen, auch deren Git-Historie und bereits geteilte Kopien prüfen, bevor etwas veröffentlicht wird. [OSM-Kachelrichtlinie](https://operations.osmfoundation.org/policies/tiles/)

### 3. Mittlere Priorität: Backup-Regeln decken nicht alle Speicherbereiche ausdrücklich ab

Beleg: [data_extraction_rules.xml](<C:/Users/Jeremy/Documents/Chati/Projekt Astra/app/src/main/res/xml/data_extraction_rules.xml:3>) schließt in beiden Abschnitten ausschließlich `root` aus. Android behandelt `file`, `database`, `sharedpref` und die gerätegeschützten Bereiche separat. Die AOSP-Implementierung durchläuft diese Bereiche einzeln; ihr Ausschlussvergleich beim Sichern vergleicht den konkreten Pfad. Ein Ausschluss des Wurzelpfades ersetzt daher keine ausdrücklichen Ausschlüsse dieser getrennten Bereiche.

`allowBackup=false` und `fullBackupContent=false` sind bereits gesetzt und schützen gegen reguläre Cloud-Sicherungen. Laut Android können Hersteller bei Gerätewechsel-Übertragungen `allowBackup=false` jedoch anders behandeln. Favoriten in SharedPreferences sind deshalb nicht durch die XML-Regel vollständig abgesichert. Ein tatsächlicher Gerätewechsel wurde nicht durchgeführt. Die beobachtete WorkManager-Datenbank liegt dagegen im ausgeschlossenen `no_backup`-Bereich.

Abhilfe: Für Cloud-Backup und Gerätewechsel alle verwendeten Datenbereiche explizit ausschließen und auf unterstützten Geräten testen. [Android-Backup-Dokumentation](https://developer.android.com/identity/data/autobackup), [AOSP BackupAgent](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/app/backup/BackupAgent.java)

### 4. Mittlere Priorität: Standortlistener endet nicht ausdrücklich beim Verlassen der App

Beleg: [MainActivity.kt](<C:/Users/Jeremy/Documents/Chati/Projekt Astra/app/src/main/java/de/projektastra/app/MainActivity.kt:411>) registriert den Listener in `DisposableEffect` und entfernt ihn erst beim Verlassen der Komposition. Das Drücken der Home-Taste beendet die Komposition nicht zwangsläufig. Da Android 9 unterstützt wird, sind dort mit der normalen Standortberechtigung gedrosselte Hintergrundmeldungen möglich. Die Datenschutzaussage „kein Standortzugriff im Hintergrund“ ist daher nicht für alle unterstützten Geräte durch den Code abgesichert.

Abhilfe: Registrierung an den sichtbaren Lifecycle koppeln, beispielsweise mit `LifecycleStartEffect` und `onStopOrDispose`; auf Android 9 und einer aktuellen Version prüfen. Keine Hintergrund-Standortberechtigung ist vorhanden – das ist positiv. Ein tatsächlicher Hintergrund-Callback wurde in dieser Prüfung nicht ausgelöst. [Compose-Lifecycle](https://developer.android.com/topic/libraries/architecture/lifecycle), [Android-Hintergrundstandortlimits](https://developer.android.com/about/versions/oreo/background-location-limits)

### 5. Mittlere Priorität: PDF-Abhängigkeit des Updaters und unbeschränkte PDF-Verarbeitung

Beleg: [IMO-Workflow](<C:/Users/Jeremy/Documents/Chati/Projekt Astra/.github/workflows/update-imo-calendar.yml:19>) installiert `pypdf==6.0.0`. Für diese Version sind unter anderem Speicher-/CPU-Erschöpfung bei der Textextraktion durch präparierte PDF-Schriften dokumentiert. Die konkret geprüften Meldungen sind [GHSA-fwg2-594c-jp42](https://github.com/py-pdf/pypdf/security/advisories/GHSA-fwg2-594c-jp42) und [GHSA-fp3f-mc75-235c](https://github.com/py-pdf/pypdf/security/advisories/GHSA-fp3f-mc75-235c). Dafür sind mindestens 6.15.0 erforderlich; zum Prüfzeitpunkt war 6.17.0 verfügbar und ohne Treffer in der ergänzenden OSV-Abfrage.

In [AstraFeatures.kt](<C:/Users/Jeremy/Documents/Chati/Projekt Astra/app/src/main/java/de/projektastra/app/AstraFeatures.kt:305>) wird zusätzlich die komplette Online-PDF-Antwort ohne Größenlimit in den Speicher gelesen und im App-Prozess verarbeitet. Verbindungs- und Lesetimeouts existieren, aber kein Download-, Seiten- oder wirksames Rechenzeitlimit für die PDF-Auswertung. Das Python-Skript hat eine ähnliche Begrenzungslücke.

Die URLs sind feste HTTPS-Adressen der IMO. Ein Angriff setzt beispielsweise manipulierte vorgelagerte Inhalte voraus; ein ungewöhnlich großes oder fehlerhaftes Dokument genügt aber für Verfügbarkeitsprobleme. Eine personenbezogene Datenweitergabe oder Codeausführung wurde hier nicht nachgewiesen.

Abhilfe: pypdf aktualisieren und Parser-Regressionstests durchführen; Downloads begrenzen, Workflow-Laufzeit begrenzen und PDF-Verarbeitung isolieren beziehungsweise validierte, kleine Datenformate bereitstellen. [PDFBox-Sicherheitshinweise](https://pdfbox.apache.org/security.html)

### 6. Mittlere Priorität: Datenschutzerklärung noch nicht veröffentlichungsfertig

Beleg: [privacy-policy.html](<C:/Users/Jeremy/Documents/Chati/Projekt Astra/play-store/privacy-policy.html:27>) behauptet für IMO-Abrufe, keine persönlichen Daten würden übertragen. Auch dort sieht der Anbieter mindestens die öffentliche IP-Adresse und technische Anfrageinformationen. Eine API ohne persönliche Kennung ist nicht gleichbedeutend mit einer Anfrage ohne personenbezogene Daten. Anbieteraufbewahrung und Karten-Cache werden nicht ausreichend beschrieben. In Zeile 42 ist der Verantwortlichen-/Kontaktbereich noch ein Platzhalter.

Abhilfe: Tatsächliche Empfänger, technische Metadaten, lokale Speicherung, Anbieteraufbewahrung, Löschgrenzen, Verantwortlichenkontakt und zutreffende Datenschutzinformationen vervollständigen. Die rechtliche Grundlage und Veröffentlichungspflichten müssen für den tatsächlichen Betrieb bestimmt werden. Standortdaten und Online-Kennungen können personenbezogene Daten sein; Zweckbindung, Datenminimierung und Speicherbegrenzung sind eigenständige Anforderungen. [DSGVO, insbesondere Artikel 4, 5 und 13](https://eur-lex.europa.eu/eli/reg/2016/679/oj?locale=de)

### 7. Ergänzende Härtung: WebView-Navigation und Lieferkette

Die Karten besitzen keine Navigationsfreigabeliste. Ein Klick auf einen Quellenlink öffnet weitere Seiten in derselben JavaScript-fähigen WebView; zusätzliche Cookies und Inhalte werden damit möglich. Native JavaScript-Brücken sind nicht vorhanden; Datei-/Content-Zugriff und gemischte HTTP-Inhalte sind bei den Karten gesperrt. Externe Links sollten kontrolliert im Browser geöffnet und Kartenressourcen auf die benötigten Ziele eingeschränkt werden.

Der Workflow verwendet veränderliche Action-Tags `@v4`/`@v5` und einen schreibberechtigten GitHub-Token während Installation und Dokumentverarbeitung. Gradles Distributionsprüfsumme, Abhängigkeits-Verifikationsmetadaten und Lockfiles fehlen. Das sind Härtungslücken, kein Nachweis einer Kompromittierung. Abhilfe: Action-Commit-SHAs, minimale Job-Rechte, getrennte Datenprüfung/Veröffentlichung und Paket-/Distributionsverifikation. [GitHub Actions Sicherheit](https://docs.github.com/en/actions/reference/security/secure-use), [Gradle-Verifikation](https://docs.gradle.org/current/userguide/dependency_verification.html)

### 8. Release-Nachweis: vorhandenes Bundle ist unsigniert

Die Prüfung mit `jarsigner -verify` meldet für das vorhandene `app-release.aab`: `jar is unsigned`. Eine lokale `keystore.properties` existiert nicht. [app/build.gradle.kts](<C:/Users/Jeremy/Documents/Chati/Projekt Astra/app/build.gradle.kts:34>) aktiviert die Release-Signierung nur bei vorhandener Konfiguration.

Die frühere Aussage, das Bundle sei bereits signiert, war falsch. Ein erfolgreicher Task namens `signReleaseBundle` war kein Signaturnachweis. Vor einem Play-Upload ist eine echte Upload-Signierung nötig. Es wurden weder Schlüssel angelegt noch Geheimnisse ausgegeben.

## Tatsächliche Datenablage

| Daten | Beobachteter oder im Code belegter Ort | Bewertung |
|---|---|---|
| Aktueller GPS-Punkt | App-Arbeitsspeicher; zusätzlich Standortparameter in Online-Anfragen | Kein ausdrücklich gespeicherter GPS-Verlauf in SharedPreferences gefunden; Anbieter-/Kartenreste beachten |
| Wetter-/Wolkenanfragen und Kartenausschnitte | Open-Meteo bzw. Kartenanbieter; WebView-Datenträgercache | Externe Log-Aufbewahrung teils bestätigt, lokale Reste nachgewiesen |
| Favoriten, gespeicherte Ereignisse, Erinnerungsvorlauf | Private SharedPreferences `astra_observations` | Für die gewünschte Planungsfunktion vorgesehene lokale Speicherung; Backup-Regeln nachbessern |
| Erinnerungsaufträge | Private WorkManager-Datenbank unter `no_backup`; danach Android-Benachrichtigungen | Ereignistitel/-zeit gespeichert, keine GPS-Felder im WorkManager-Auftrag |
| IMO-Katalog und Abrufzeit | Private SharedPreferences `astra_imo` | Öffentliche Astronomiedaten und Abrufmetadaten |
| Rotlichtmodus | Private SharedPreferences `astra_settings` | Beabsichtigte lokale Einstellung |
| AR-Kamerabilder | CameraX-Vorschau | Kein Datei-/Uploadpfad für Bilder im geprüften Code gefunden |
| Store-Screenshots | Git-Arbeitsverzeichnis und Versionshistorie | Sichtbare Standortdarstellung; Demo-Daten vor Veröffentlichung sicherstellen |

Private SharedPreferences sind nicht automatisch eine Sicherheitslücke: Sie liegen im Android-App-Sandboxbereich. Eine zusätzliche Verschlüsselung ist risikobasiert zu beurteilen. Das Problem hier sind unter anderem Umfang, Aufbewahrung und unerwartete Übertragungswege.

## Einordnung DIN EN ISO/IEC 27001

DIN Media führt die deutsche Ausgabe **DIN EN ISO/IEC 27001:2024-01**, basierend auf ISO/IEC 27001:2022, mit Änderung **A1:2024-11**. Die Norm behandelt das Informationssicherheits-Managementsystem einer Organisation einschließlich festgelegtem Geltungsbereich und Risikomanagement. Eine einzelne Android-App kann diese organisatorischen Nachweise nicht ersetzen. [DIN Media](https://www.dinmedia.de/de/norm/din-en-iso-iec-27001/370680635), [ISO](https://www.iso.org/standard/27001)

| Für eine belastbare ISMS-Bewertung benötigter Nachweis | Im geprüften Projekt vorgefunden |
|---|---|
| Festgelegter Geltungsbereich, Verantwortlichkeiten, Sicherheitsziele | Kein dokumentierter Nachweis gefunden |
| Risikobewertung, Behandlung und Freigabe verbleibender Risiken | Kein bestehender Prozess-/Freigabenachweis; dieser Bericht liefert lediglich Eingaben |
| Auswahl/Begründung anwendbarer Maßnahmen, Erklärung zur Anwendbarkeit | Nicht vorgelegt |
| Lieferantenbewertung, Aufbewahrung, Zugriffs- und Schlüsselverwaltung | Teilweise technische Einstellungen und Quellenangaben; organisatorische Nachweise fehlen |
| Sichere Entwicklung und Schwachstellenbehandlung | Git, funktionale Tests und Lint vorhanden; Sicherheitslücken und fehlende Verifikation siehe oben |
| Vorfallbehandlung, Überprüfung der Wirksamkeit, interne Audits und Verbesserung | Keine entsprechenden Nachweise im Repository gefunden |

„Nicht gefunden“ bedeutet nicht, dass solche Nachweise außerhalb des Projekts nicht existieren. Der vollständige lizenzierte Normtext, organisatorische Unterlagen und ein Zertifikat wurden nicht vorgelegt; eine vollständige Prüfung aller Normforderungen wurde deshalb nicht durchgeführt.

## Prüfmethoden und Grenzen

- Relevante Kotlin-, HTML-, Manifest-, Backup-, Netzwerk-, Gradle-, Workflow- und Datenschutzdateien gelesen; zusammengeführtes Release-Manifest geprüft.
- Vorhandenen Emulator-App-Speicher nur gelesen. Ergebnisse auf Pfade, Trefferzahlen und Datenarten begrenzt; konkrete Koordinaten, Cookie-Werte oder persönliche Listen nicht in diesen Bericht kopiert.
- Öffentliche Anbieterinformationen und Primärquellen abgeglichen. 167 unterschiedliche aufgelöste Maven-Koordinaten einschließlich Constraints/Plattformmodulen gegen OSV geprüft: keine gelisteten Treffer für die exakten Versionen. Abweichend davon hat das separate Python-Werkzeug pypdf 6.0.0 dokumentierte Treffer.
- Begrenzte Suche nach bekannten Geheimnissignaturen im versionierten Quelltext sowie sensiblen Dateinamen in der Git-Historie: keine entsprechenden Schlüssel-/Tokenfunde. Keine vollständige historische Inhaltsprüfung aller Git-Objekte.
- Gradle-Wrapper-JAR stimmt mit einer offiziellen früheren Wrapper-Prüfsumme überein; kein Manipulationsnachweis. Bestehender Lint-Bericht meldet keine Befunde; die vorhandenen zehn Unit-Tests sind überwiegend funktionale Astronomietests und kein Sicherheitstestprogramm.
- Keine aktiven Angriffstests, kein TLS-Mitschnitt, kein Gerätewechsel-/Android-9-Laufzeittest, keine Kontrolle fremder Serverprotokolle, keine Prüfung organisatorischer Prozesse oder tatsächlicher GitHub-/Play-Console-Zugriffsrechte.

Empfohlene Reihenfolge: Standort-/Aufbewahrungskonzept und Datenschutzhinweise festlegen, Cache-/Backup-/Lifecycle-Lücken schließen, PDF-Verarbeitung und Build-Lieferkette härten, Demo-Store-Bilder und Signierung herstellen, anschließend die geänderten Kontrollen gezielt erneut prüfen.
