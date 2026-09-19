# Aufgaben und Prioritäten

Stand: 19. September 2026 · Aktueller Ausbau: Version 1.1.9-pre.7.

Diese Liste führt offene Arbeit und ausdrücklich abgehakte Ausbauschritte. Bestehende Funktionen stehen auch in der [README](README.md). Die Reihenfolge ist eine Arbeitsplanung, keine automatische Freigabe für Veröffentlichungen oder neue Datenübertragungen.

**Release-Regel:** Jede größere funktionale, technische oder sicherheitsrelevante Änderung wird zuerst als eigener GitHub-Pre-Release mit Test-APK beziehungsweise Bundle veröffentlicht. Der verbindliche Ablauf steht in [RELEASE_PROCESS.md](RELEASE_PROCESS.md). Nach den Tests auf mehreren Geräten und der Korrektur der Rückmeldungen folgt erst der reguläre Release. Kleine Dokumentations- oder rein interne Teständerungen dürfen gesammelt werden.

## P1 – Als Nächstes

### 1. Objektsuche und Nachführen

- [x] Offline-Suche nach Namen, gebräuchlichen Alternativnamen und Katalognummern, beispielsweise Saturn, Andromeda, M31 und HIP-Nummern.
- [x] Ergebnisse mit Objekttyp und aktueller Sichtbarkeit anzeigen; zwischen Andromeda als Sternbild und als Galaxie unterscheiden.
- [x] Suchtreffer und Einträge der Beobachtungsliste direkt in der Sternkarte öffnen und zentrieren.
- [x] Optional ein ausgewähltes Objekt in der manuellen Karte nachführen; Nachführen beim manuellen Verschieben beenden und den Zustand klar anzeigen.
- [x] Auswahl und Suche auch ohne Online-Freigabe nutzbar halten; keine Suchhistorie ohne ausdrückliche Nutzerentscheidung speichern.
- [x] Tests für alternative Namen, leere Ergebnisse, ausgeblendete Deep-Sky-Ebene und Ziele unter dem Gelände-/Horizont ergänzen.

Umgesetzt in Version 1.1.4. Die Suche umfasst die gebündelten Kataloge und Sternbilder mit einem ausdrücklich benannten Referenzstern. Suchtext bleibt nur während der geöffneten Suche im Arbeitsspeicher. Deep-Sky-Treffer schalten ihre Ebene ein; Nachführen aktualisiert alle fünf Sekunden und endet bei manueller Bedienung, einem neuen Ziel oder Wechsel in AR. Die Höhenlage beschreibt keine Wetter-/Tageslicht-Sichtbarkeitsprognose. Reale Kamera-/Sensortests bleiben bei P2.12.

### 2. Zeitsteuerung der Sternkarte

- [x] Datum und Uhrzeit frei wählen sowie Zeit vorwärts und rückwärts laufen lassen; Pause und Geschwindigkeit anbieten.
- [x] Simulationszeit deutlich kennzeichnen und jederzeit mit „Zurück zu Jetzt“ zur Live-Ansicht wechseln.
- [x] Sterne, Planeten, Mond, Milchstraße, Sternbildgrenzen und Objektinformationen auf dieselbe ausgewählte Zeit beziehen.
- [x] Aus Kalenderereignissen die Karte zum Ereigniszeitpunkt öffnen.
- [x] Historische oder simulierte Himmelsansichten nicht mit vermeintlich passenden Live-Wetterdaten vermischen; fehlende Vorhersagen ausdrücklich kennzeichnen.
- [x] AR standardmäßig live lassen; beim Wechsel aus einer Simulation den Zeitwechsel sichtbar machen.
- [x] Zeitzonen, Sommerzeitwechsel, Tageswechsel und Rückkehr aus dem Hintergrund testen.

Umgesetzt in Version 1.1.5. Die Simulation läuft nur im Vordergrund, pausiert bei Hintergrund-/Tabwechsel und verwendet eine monotone verstrichene Zeit. Eingaben gelten für 1900–2100; Sommerzeitlücken werden zurückgewiesen, doppelte Ortszeiten können als früherer oder späterer Zeitpunkt ausgewählt werden. Wetter bleibt auf aktuelle Vorhersagedaten bezogen und wird in der Simulation ausdrücklich gekennzeichnet.

### 3. Vollbild und aufgeräumte Kartenbedienung

- [x] Kartenbedienelemente einklappbar machen und einen leicht beendbaren Vollbildmodus anbieten.
- [x] Beschriftungsdichte einstellen; überlappende Namen und Beschriftungen hinter Bedienelementen vermeiden.
- [x] Ausgewählte Objekte und wichtige Orientierungsangaben gegenüber anderen Beschriftungen bevorzugen.
- [x] Aktuelles Sichtfeld beziehungsweise Zoomstufe anzeigen und eine Ansicht-zurücksetzen-Funktion anbieten.
- [ ] Große Schrift, TalkBack-Beschriftungen, erreichbare Bedienelemente und Rotlichtmodus für die neuen Oberflächen prüfen.

Implementiert in 1.1.6-pre.1. Bedienung und Objektbereich liegen außerhalb des Himmels und erhalten begrenzte, scrollbar bleibende Flächen. Das Vollbild endet mit der sichtbaren Taste oder Android-Zurück. Beschriftungen berücksichtigen Priorität, Schriftgröße, Kartenränder und Gelände. Reset zeigt Süden / 35° Höhe / 95° Sichtfeld und beendet das Nachführen. Das AR-Sichtfeld berücksichtigt Größenänderungen des tatsächlichen Kameraausschnitts. Gerätefeedback wird anhand der [Pre-Release-Testhinweise](play-store/pre-release-1.1.6.md) gesammelt.

### 4. Visuelle Überarbeitung und realistischerer Sternenhimmel

- [x] Die normale Sternkarte optisch überarbeiten: natürlicherer Himmel, feinere Helligkeitsabstufungen und weniger schematischer Gesamteindruck.
- [x] Das bisherige einfache Milchstraßenband durch eine strukturierte Darstellung mit Dunkelwolken, unterschiedlichen Sternendichten und erkennbarem galaktischem Zentrum ersetzen.
- [x] Eine geeignete astronomisch referenzierte Himmels-/Milchstraßentextur auswählen; Lizenz, Quellenangabe, Koordinatensystem und Offline-Bündelung prüfen. Keine beliebige Landschaftsaufnahme oder unregistrierte Illustration als positionsgenaue Himmelskarte verwenden.
- [x] Milchstraßenstruktur passend zu Standort, Datum und Uhrzeit auf die Himmelskugel projizieren; bei Verschieben, Zoomen und späterer Zeitsteuerung korrekt mitführen. Doppelte Sterne aus Textur und Objektkatalog vermeiden.
- [x] Sternfarben, Größen und Leuchten dezenter und anhand der Kataloghelligkeiten abstimmen; Auswahl und Objektinformationen unabhängig von der visuellen Darstellung erhalten.
- [x] Helligkeit beziehungsweise Kontrast der Milchstraße einstellbar machen. Eine verstärkte Astrofoto-Darstellung von einer natürlich-dezenten Ansicht klar unterscheiden; sie ist keine Zusage der tatsächlichen Sichtbarkeit vor Ort.
- [x] Gelände-Verdeckung, Rotlichtmodus und gut lesbare Beschriftungen erhalten. AR-Steuerung unverändert lassen und dort eine dezente, separat schaltbare Milchstraßenüberlagerung vorsehen.
- [x] Koordinaten- und GPU-Vergleichstests an bekannten Himmelspositionen sowie an RA-Naht, Kartenrändern und Zenit ergänzen; Horizont, Rotlicht, Texturübergänge, Darstellungsmodi und Speichergrenze im Android-17-Emulator prüfen.

Umgesetzt in Version 1.1.3. Die Textur basiert auf NASA/Gaia-Sterndaten; sie ist keine Original-Fotoaufnahme. Kontraständerungen sind Darstellungsstile, keine Sichtbarkeitsprognose. Herkunft und Reproduktion: [Milchstraßen-Asset](scripts/MILKY_WAY_ASSET.md).

- [ ] Nachprüfung auf echten Geräten, besonders Android 9/API 28, kleineren Grafikchips und wenig Arbeitsspeicher: Zoom-/Bildqualität, erste Ladezeit, Drehung und schnelles Öffnen/Schließen. Akkutests bleiben ausdrücklich P3.

### 5. Lichtverschmutzungskarte verbessern

- [x] Kilometermaßstab und Legende für den Astra-Bildlichtindex sowie ausdrücklich unvalidierte Bortle-Einordnung anzeigen; Helligkeitsnäherung am Beobachtungsort erläutern.
- [x] Gerundeten Standortmarker, Umkreis von 10/25/50 km und Zoom-/Kartengrenzen ergänzen; Beobachtungsorte mit Entfernung und Bildlichtdifferenz vergleichbar machen.
- [x] Künstliches Nachtlicht, punktuelle Raster-Klassen und Beobachtungsplatzvergleich getrennt schaltbar machen.
- [x] Datenjahr, Datenalter, Kachelquelle und Unsicherheit verständlich anzeigen; die historischen Daten von 2016 ausdrücklich kennzeichnen.
- [x] Veraltete NASA-Lichtdaten von 2016 direkt am Astra-Score und über der internen Karte kennzeichnen, ohne erst Details öffnen zu müssen (1.1.7-pre.2). Berechnung unverändert lassen.
- [x] Offline-/Fehlerzustand, Cache-Löschung und erneutes Laden mit automatisierten Android-Fixtures testen; Lichtkarten erhalten nur den gerundeten GPS-Startort und benötigen Online-Freigabe.
- [x] Astra-Karte bei Rotlicht, kleiner Fläche, großer Schrift, Mercator-Grenzen und teilweisen Kachelfehlern im Emulator prüfen.
- [x] Offizielle LightPollutionMap.app-Einbettung nach gesonderter Ladefreigabe ergänzen; Empfänger und Einschränkungen erläutern, Standortdetails innerhalb der Anbieterkarte belassen.

Implementierung in 1.1.7-pre.1: Vergrößerbare Karte mit erhaltenem Ausschnitt, begrenzten parallelen Wiederholungen und direkter Cache-Löschung. Transparente Pixel und Orte außerhalb der Mercator-Abdeckung ergeben keine Dunkelheitswertung. Beobachtungs- und Vergleichsort werden auf 0,01° gerundet; Vergleiche bleiben im Arbeitsspeicher. Die vorhandene Score-Skala bleibt erhalten.

Die zusätzliche Anbieterkarte verwendet eine getrennte, flüchtige WebView mit eingeschränkten Netzwerkzielen und ohne Geräteberechtigungen. Nur ihr GPS-Startort wird gerundet; selbst angetippte Kartenpunkte werden durch die Website ausgewertet. Sie ändert nicht den Astra-Score und ersetzt nicht die eigene Karte.

- [ ] Beide Lichtkarten auf älteren realen Geräten, mit TalkBack und gedrosselter/wechselnder Mobilfunkverbindung nachprüfen. Externe Zusatzfunktionen außerhalb der Lichtkarte sind bewusst nicht vollständig freigegeben.

- [ ] Für Astras eigene Karte und Score eine wissenschaftlich kalibrierte, flächendeckende Himmelshelligkeits-/Bortle-Datenquelle mit passenden Nachnutzungsrechten ergänzen. Das bisherige NASA-Kartenbild erlaubt nur einen relativen Bildlichtvergleich; seine Bortle-/Helligkeitsnäherung ist nicht validiert. Die externe Einbettung ist keine Freigabe zum Übernehmen ihrer internen API-Daten.

### 6. Wetteraktualisierung ohne Datenlücke

- [x] Beim Pull-to-Refresh die letzte erfolgreiche Wetterantwort, Zeitleiste, Astra-Score-Daten und Kartenebenen sichtbar lassen, bis neue Daten vollständig vorliegen.
- [x] Ladezustand als dezente Aktualisierungsanzeige über den alten Daten zeigen; keine leere Ansicht und kein Zurücksetzen auf „Idle“ während des Abrufs.
- [x] Bei einem Fehler alte Daten mit Datenalter und Wiederholen-Aktion anzeigen; nur beim ersten Abruf einen leeren Ladezustand verwenden.
- [x] Wetter, Regenradar, Bewölkung und Standortaktualisierung getrennt behandeln, damit ein Fehler nicht alle vorhandenen Ebenen entfernt.
- [x] Tests für Pull-to-Refresh, langsame Antwort, Fehler, Standortwechsel, Offlinebetrieb und App-Hintergrund während des Abrufs ergänzen.

Umgesetzt in `1.1.8-pre.1`. Wetter und zugehöriger Ort werden atomar ersetzt; alte Antworten können neuere Anfragen nicht überschreiben. Stunden und Score verwenden feste Zeitstempel des Datensatzes. Radar tauscht das Bild erst nach erfolgreichem Laden der sichtbaren Ersatzkacheln aus, Bewölkung erst nach einem vollständigen 49-Punkte-Raster. Fehler und Zeitstände beider Ebenen werden getrennt angezeigt. Standortabruf bleibt unabhängig; ohne neuen GPS-Fix gilt der letzte verfügbare Ort. Alle Wetter-/Standortdaten bleiben nur im Arbeitsspeicher dieser Ansicht, nicht über Tabwechsel oder einen Prozessneustart hinweg. Hintergrundabrufe werden abgebrochen und bei Rückkehr neu begonnen.

- [ ] Auf einem weiteren realen Gerät mit schwachem Mobilfunk, echtem GPS-Standortwechsel und großer Schrift gegenprüfen; Akkutests bleiben P3.

### 7. Schneller App- und Sternkartenstart

- [x] Kataloge, Sternbildgrenzen, Geländeprofil und Suchindex aus dem UI-Thread verlagern oder gestuft laden; der erste Kartenrahmen soll ohne vollständigen Deep-Sky-Aufbau erscheinen.
- [x] HYG-/OpenNGC-/IAU-Daten pro Prozess zwischenspeichern und bei wiederholtem Tabwechsel nicht erneut parsen (Sternkarte und Beobachtungsliste, `beta` / `1.1.9-pre.1`).

Teilstand auf `beta` (`1.1.9-pre.1`): Sternkarte und Beobachtungsliste teilen denselben prozessweiten Katalogcache. Kataloge und ortsunabhängiger Suchindex werden gestuft außerhalb des UI-Threads geladen. Fertige Stufen bleiben beim Tabwechsel erhalten; Suchtext, Standort und aktuelle Planetenpositionen gehören nicht in diesen Cache. Loader-Fehler werden durchgereicht und fehlgeschlagene Stufen bleiben wiederholbar. Die kleine Ersatz-Sternkarte dient nur der Anzeige, nicht als vermeintlich vollständiger Cache. Vorgemerkte Objektziele warten auf die Objektkataloge; die Beobachtungsliste verwechselt noch nicht aufgelöste Favoriten nicht mit einer leeren Liste und löscht keine Vormerkungen. Dies garantiert noch keinen ersten gezeichneten Kartenrahmen vor den optionalen Ebenen; Textur/GPU, Gelände, Low-Memory-Verhalten und Referenzgeräte-Messungen bleiben offen.

- [x] Milchstraßen-Textur, GPU-Kontext und optionale Ebenen nach dem ersten sichtbaren Kartenrahmen priorisiert laden; Fehler müssen die Basiskarte nutzbar lassen.
- [ ] Kaltstart, Warmstart, erster sichtbarer Kartenrahmen und Interaktion auf einem leistungsschwachen Referenzgerät messen und Zielwerte dokumentieren.
- [ ] Speicherbudget, GC-Pausen, Textur-Upload, Rotation, Prozesswiederherstellung und App-Start ohne Netzwerk prüfen; keine personenbezogenen Daten in Performance-Logs schreiben.
- [x] Regressionstests für schnelle Kartenanzeige, wiederholtes Öffnen/Schließen, Karten-/Wetterwechsel und Low-Memory-Verhalten ergänzen. Akkutests bleiben P3.

## P2 – Danach

### 8. „Was lohnt sich heute Nacht?“

- [x] Aus vorhandenen Wetter-, Mond-, Dämmerungs- und Objektdaten geeignete Beobachtungszeitfenster berechnen.
- [x] Ziele nach Höhe über dem lokalen Gelände, Mondabstand und geeigneter Beobachtungszeit sortieren.
- [x] Empfehlungen für bloßes Auge, Fernglas und Teleskop filtern; Begründungen anzeigen statt nur eines Scores.
- [x] Ziele aus Empfehlungen direkt zur vorhandenen Beobachtungsliste hinzufügen und auf der Karte öffnen.
- [x] Datenalter, Prognosegrenzen und fehlende Wetter-/Geländedaten sichtbar machen; keine sichere Sichtbarkeit versprechen.

Umgesetzt in Version 1.1.9-pre.4 (`TonightWindowCalculator`, `TonightTargetEngine`, `ObservationPlanScreen`). Die Berechnungen laufen 100 % lokal auf dem Gerät. Empfehlungen berücksichtigen die Mindesthöhe (>= 16°), den sphärischen Mondabstand, Helligkeit und optimale Kulminationszeiten. Filter für Bloßes Auge, Fernglas und Teleskop mit nachvollziehbarer astronomischer Begründung. Direkte Aktionen zum Zentrieren in der Karte und Hinzufügen/Entfernen aus Favoriten.

### 9. Beobachtungstagebuch

- [x] Objekte als beobachtet markieren; Datum, Notizen und optional eigene Fotos hinzufügen.
- [x] Beobachtungen ausschließlich lokal speichern; genaue Standortangaben nur optional und bewusst hinzufügen.
- [x] Einzelne Beobachtungen sowie alle Tagebuchdaten löschbar machen; zugehörige app-eigene Foto-Kopien berücksichtigen.
- [x] Bewussten Export und Import anbieten, einschließlich Vorschau der enthaltenen Daten und Hinweis auf mögliche Foto-Standortmetadaten.
- [x] Keine automatische Cloud-Synchronisierung oder Änderung der bestehenden Backup-Ausschlüsse einführen.
- [x] Importfehler, Größenlimits, Export/Import-Rundlauf und vollständiges Löschen testen.

Umgesetzt in Version 1.1.9-pre.6 (`ObservationLogbook.kt`, `ObservationLogbookStore`, `ObservationLogEntryDialog`, `LogbookExportImportDialog`, `FullPhotoDialog`). Beobachtungen werden 100 % lokal in einer privaten JSON-Datei in `context.filesDir` abgelegt. Eigene Fotos werden in den isolierten Ordner `logbook_photos` kopiert und beim Löschen eines Eintrags automatisch bereinigt. Standortdaten sind standardmäßig deaktiviert und werden nur bei bewusstem Opt-in hinzugefügt. Vollständiges Löschen aller Einträge und lokaler Fotos mit Bestätigungsdialog. Export und Import via JSON mit Größenbegrenzung (10 MB, 5.000 Einträge), Vorschau und EXIF-Datenschutzhinweis. Cloud-Backup und Datenübertragung bleiben strikt ausgeschlossen (`allowBackup="false"`).

### 10. Fernglas- und Teleskop-Sichtfeld

- [x] Sichtfeld als Kreis mit frei eingebbarer Winkelgröße über der Karte anzeigen (inkl. Telrad-Sucher 0,5° / 2,0° / 4,0°).
- [x] Optional lokale Geräteprofile mit Brennweite, Okularbrennweite und scheinbarem Gesichtsfeld anbieten; berechnete Werte als Näherung kennzeichnen.
- [x] Kartenansicht passend zum Instrument drehen oder spiegeln; Zustand sichtbar machen und einfach zurücksetzen können.
- [x] Objektwahl, Beschriftungen und Touch-Koordinaten unter Drehung/Spiegelung testen; AR davon getrennt lassen.

Umgesetzt in Version 1.1.9-pre.7 (`OpticsProfiles.kt`, `OpticsProfilesTest.kt`, `OpticsFovSheet.kt`, `MainActivity.kt`). Frei skalierbarer Sichtfeldkreis (0,1° bis 30,0°) und Telrad-Sucher (0,5° / 2° / 4° Kreise mit Fadenkreuz). Geräteprofile für Ferngläser (10×50, 8×42) und Teleskope (8" Dobson 25 mm / 10 mm Plössl) sowie eigene Profile mit Brennweite, Öffnung, Okularbrennweite und scheinbarem Gesichtsfeld (AFOV). Automatische Kennzeichnung von Vergrößerung, wahrem Gesichtsfeld und Austrittspupille als Näherungswerte. Drehung um 0°, 90°, 180° (Newton-Invertierung), 270° und horizontale Spiegelung (Zenitspiegel) mit wählbarer Beschriftungsausrichtung (aufrecht oder mitrotierend). Millimetergenaue Objektauswahl per inverser Koordinatentransformation und an Wischgesten angepasste Panning-Deltas. Dezentes Status-Badge über der Karte mit 1-Klick-Reset („Standard“). AR bleibt strikt unbeeinflusst und alle Einstellungen werden 100 % lokal ohne Cloud-Sync gespeichert.

### 11. AR-Zielhilfe

- [ ] Für ein ausgewähltes Ziel Richtungspfeile und Winkelabstand zur aktuellen Blickrichtung anzeigen.
- [ ] Ziele hinter dem Gerät und unter dem lokalen Horizont verständlich kennzeichnen.
- [ ] Sensorqualität und Kalibrierungshinweise berücksichtigen; keine exakte Zielerfassung bei unsicherer Ausrichtung behaupten.

### 12. Robustheit und Darstellung

- [ ] Auf echten Geräten AR-Ausrichtung, Kameraüberlagerung, Drehung, Zoom und Touch-Auswahl prüfen, auch auf unterstützten älteren Android-Versionen.
- [ ] Rendering beim Schwenken profilieren; Framezeiten, kurzzeitige Hänger und Speichernutzung mit und ohne Deep Sky/IAU-Grenzen vergleichen.
- [ ] Automatisierte Regressionstests um Pinch-Zoom, AR-/Kartenwechsel und Beschriftungskollisionen erweitern.
- [ ] Berechtigungsentzug, App-Unterbrechungen, fehlende Sensoren, Offline-Betrieb und fehlerhafte Netzwerkantworten als Geräte-Testfälle dokumentieren.
- [ ] Verständliche Fehler- und Wiederholen-Zustände für Wetter, Karten und Objektbilder prüfen.

## P3 – Niedrige Priorität

- [ ] **Akkutests (ausdrücklich Low Prio):** Verbrauch bei normaler Sternkarte, AR/Kamera, GPS und Wetterkarte über längere Sitzungen messen.
- [ ] **Akkutests (Low Prio):** Prüfen, ob nach Bildschirm-Aus und Verlassen der App unnötige Aktivität bestehen bleibt; gegebenenfalls Energiesparoptionen ableiten.

Akkutests stehen hinter den Funktions- und Bedienungsverbesserungen. Unabhängig davon bleiben korrekte Berechtigungen sowie das Beenden von Standort- und Netzwerkzugriffen im Hintergrund Teil der Sicherheitsprüfung.

## Vor öffentlicher Veröffentlichung – separat abarbeiten

Diese Punkte sind nicht von der Umsetzung aller optionalen Funktionen abhängig. Maßgeblich bleiben die [Release-Checkliste](play-store/release-checklist.md) und die offenen Punkte im [Sicherheitsmaßnahmenbericht](SECURITY_REMEDIATION.md).

- [ ] Upload-Key geschützt einrichten und sichern; signiertes Bundle und Release-Prüfung verifizieren.
- [ ] Dokumentierte Datenschutzprüfung abschließen, freigegebene Datenschutzseite öffentlich bereitstellen und Datensicherheitsangaben mit dem tatsächlichen Release abgleichen.
- [ ] Store-Texte, Screenshots und Versionsangaben auf den freizugebenden Stand bringen; aktuelle Play-Console-Anforderungen vor dem Upload prüfen.
- [ ] Funktionale Geräteprüfung und Store-Prelaunch-Bericht abschließen; Akkulaufzeitmessungen bleiben niedrig priorisiert.
- [ ] Jeden größeren Änderungsstand zuerst als GitHub-Pre-Release auf mindestens einem zusätzlichen Gerät testen und die Freigabe dokumentieren; Ablauf siehe [RELEASE_PROCESS.md](RELEASE_PROCESS.md).
- [ ] Wiederkehrende Abhängigkeits-/Sicherheitsprüfungen und einen Umgang mit gemeldeten Schwachstellen festlegen.
- [ ] Organisatorische ISO/IEC-27001-Aufgaben aus dem Sicherheitsmaßnahmenbericht separat bearbeiten, bevor entsprechende Konformitäts- oder Zertifizierungsaussagen erwogen werden. Technische Tests allein sind kein solcher Nachweis.
