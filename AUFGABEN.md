# Aufgaben und Prioritäten

Stand: 6. September 2026 · Aktueller Ausbau: Version 1.1.4.

Diese Liste führt offene Arbeit und ausdrücklich abgehakte Ausbauschritte. Bestehende Funktionen stehen auch in der [README](README.md). Die Reihenfolge ist eine Arbeitsplanung, keine automatische Freigabe für Veröffentlichungen oder neue Datenübertragungen.

## P1 – Als Nächstes

### 1. Objektsuche und Nachführen

- [x] Offline-Suche nach Namen, gebräuchlichen Alternativnamen und Katalognummern, beispielsweise Saturn, Andromeda, M31 und HIP-Nummern.
- [x] Ergebnisse mit Objekttyp und aktueller Sichtbarkeit anzeigen; zwischen Andromeda als Sternbild und als Galaxie unterscheiden.
- [x] Suchtreffer und Einträge der Beobachtungsliste direkt in der Sternkarte öffnen und zentrieren.
- [x] Optional ein ausgewähltes Objekt in der manuellen Karte nachführen; Nachführen beim manuellen Verschieben beenden und den Zustand klar anzeigen.
- [x] Auswahl und Suche auch ohne Online-Freigabe nutzbar halten; keine Suchhistorie ohne ausdrückliche Nutzerentscheidung speichern.
- [x] Tests für alternative Namen, leere Ergebnisse, ausgeblendete Deep-Sky-Ebene und Ziele unter dem Gelände-/Horizont ergänzen.

Umgesetzt in Version 1.1.4. Die Suche umfasst die gebündelten Kataloge und Sternbilder mit einem ausdrücklich benannten Referenzstern. Suchtext bleibt nur während der geöffneten Suche im Arbeitsspeicher. Deep-Sky-Treffer schalten ihre Ebene ein; Nachführen aktualisiert alle fünf Sekunden und endet bei manueller Bedienung, einem neuen Ziel oder Wechsel in AR. Die Höhenlage beschreibt keine Wetter-/Tageslicht-Sichtbarkeitsprognose. Reale Kamera-/Sensortests bleiben bei P2.9.

### 2. Zeitsteuerung der Sternkarte

- [ ] Datum und Uhrzeit frei wählen sowie Zeit vorwärts und rückwärts laufen lassen; Pause und Geschwindigkeit anbieten.
- [ ] Simulationszeit deutlich kennzeichnen und jederzeit mit „Zurück zu Jetzt“ zur Live-Ansicht wechseln.
- [ ] Sterne, Planeten, Mond, Milchstraße, Sternbildgrenzen und Objektinformationen auf dieselbe ausgewählte Zeit beziehen.
- [ ] Aus Kalenderereignissen die Karte zum Ereigniszeitpunkt öffnen.
- [ ] Historische oder simulierte Himmelsansichten nicht mit vermeintlich passenden Live-Wetterdaten vermischen; fehlende Vorhersagen ausdrücklich kennzeichnen.
- [ ] AR standardmäßig live lassen; beim Wechsel aus einer Simulation den Zeitwechsel sichtbar machen.
- [ ] Zeitzonen, Sommerzeitwechsel, Tageswechsel und Rückkehr aus dem Hintergrund testen.

### 3. Vollbild und aufgeräumte Kartenbedienung

- [ ] Kartenbedienelemente einklappbar machen und einen leicht beendbaren Vollbildmodus anbieten.
- [ ] Beschriftungsdichte einstellen; überlappende Namen und Beschriftungen hinter Bedienelementen vermeiden.
- [ ] Ausgewählte Objekte und wichtige Orientierungsangaben gegenüber anderen Beschriftungen bevorzugen.
- [ ] Aktuelles Sichtfeld beziehungsweise Zoomstufe anzeigen und eine Ansicht-zurücksetzen-Funktion anbieten.
- [ ] Große Schrift, TalkBack-Beschriftungen, erreichbare Bedienelemente und Rotlichtmodus für die neuen Oberflächen prüfen.

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

## P2 – Danach

### 5. „Was lohnt sich heute Nacht?“

- [ ] Aus vorhandenen Wetter-, Mond-, Dämmerungs- und Objektdaten geeignete Beobachtungszeitfenster berechnen.
- [ ] Ziele nach Höhe über dem lokalen Gelände, Mondabstand und geeigneter Beobachtungszeit sortieren.
- [ ] Empfehlungen für bloßes Auge, Fernglas und Teleskop filtern; Begründungen anzeigen statt nur eines Scores.
- [ ] Ziele aus Empfehlungen direkt zur vorhandenen Beobachtungsliste hinzufügen und auf der Karte öffnen.
- [ ] Datenalter, Prognosegrenzen und fehlende Wetter-/Geländedaten sichtbar machen; keine sichere Sichtbarkeit versprechen.

### 6. Beobachtungstagebuch

- [ ] Objekte als beobachtet markieren; Datum, Notizen und optional eigene Fotos hinzufügen.
- [ ] Beobachtungen ausschließlich lokal speichern; genaue Standortangaben nur optional und bewusst hinzufügen.
- [ ] Einzelne Beobachtungen sowie alle Tagebuchdaten löschbar machen; zugehörige app-eigene Foto-Kopien berücksichtigen.
- [ ] Bewussten Export und Import anbieten, einschließlich Vorschau der enthaltenen Daten und Hinweis auf mögliche Foto-Standortmetadaten.
- [ ] Keine automatische Cloud-Synchronisierung oder Änderung der bestehenden Backup-Ausschlüsse einführen.
- [ ] Importfehler, Größenlimits, Export/Import-Rundlauf und vollständiges Löschen testen.

### 7. Fernglas- und Teleskop-Sichtfeld

- [ ] Sichtfeld als Kreis mit frei eingebbarer Winkelgröße über der Karte anzeigen.
- [ ] Optional lokale Geräteprofile mit Brennweite, Okularbrennweite und scheinbarem Gesichtsfeld anbieten; berechnete Werte als Näherung kennzeichnen.
- [ ] Kartenansicht passend zum Instrument drehen oder spiegeln; Zustand sichtbar machen und einfach zurücksetzen können.
- [ ] Objektwahl, Beschriftungen und Touch-Koordinaten unter Drehung/Spiegelung testen; AR davon getrennt lassen.

### 8. AR-Zielhilfe

- [ ] Für ein ausgewähltes Ziel Richtungspfeile und Winkelabstand zur aktuellen Blickrichtung anzeigen.
- [ ] Ziele hinter dem Gerät und unter dem lokalen Horizont verständlich kennzeichnen.
- [ ] Sensorqualität und Kalibrierungshinweise berücksichtigen; keine exakte Zielerfassung bei unsicherer Ausrichtung behaupten.

### 9. Robustheit und Darstellung

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
- [ ] Wiederkehrende Abhängigkeits-/Sicherheitsprüfungen und einen Umgang mit gemeldeten Schwachstellen festlegen.
- [ ] Organisatorische ISO/IEC-27001-Aufgaben aus dem Sicherheitsmaßnahmenbericht separat bearbeiten, bevor entsprechende Konformitäts- oder Zertifizierungsaussagen erwogen werden. Technische Tests allein sind kein solcher Nachweis.
