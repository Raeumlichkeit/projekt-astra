# Projekt Astra 1.1.7-pre.1

P1.5: Lichtverschmutzungskarte mit Umkreis, Ortsvergleich und verständlichem Datenstand. Version Code 16.

## Änderungen

- Offizielle deutsche LightPollutionMap.app-Einbettung, bewusst ladbar nach eigenem Anbieter-/Standorthinweis. Die vorhandene Astra-Karte bleibt verfügbar.
- Kilometermaßstab, Standorttaste, Umkreis von 10/25/50 km und begrenzter Karten-/Zoombereich.
- Nachtlicht, punktuelle Raster-Klassen und Ortsvergleich getrennt schaltbar. Vergleich zeigt Entfernung und Differenz zum Beobachtungsort.
- Vergrößerte Karte mit Schließen-Taste und Android-Zurück; Ausschnitt und Vergleich bleiben erhalten.
- Sichtbarer Datenstand 2016, Datenalter und Quellen. Neuladen aktualisiert den Abruf, nicht das Beobachtungsjahr.
- Teilweise geladene Karten bleiben bei Fehlern und Wiederholungen sichtbar. Erneuter Abruf und Cache-Löschen direkt an der Karte.
- Transparente Pixel und nicht abgedeckte Polarorte ergeben „Keine Daten“. Gerundete Koordinaten und kein Speichern von Vergleichsorten.
- Rotlichtdarstellung auch im Kartenfenster; Bedienfelder und Legende sind bei kleiner Fläche scrollbar.

## Einordnung und bekannte Grenzen

Die externe LightPollutionMap.app-Ansicht wird live vom Anbieter geliefert. Laut dessen [Datenbeschreibung](https://lightpollutionmap.app/about-data/) ist 2025 die letzte abgeschlossene Jahresschicht, 2026 eine vorläufige Schätzung (Prüfstand 11. September 2026). Diese Werte werden nicht ausgelesen oder in Astras Score übernommen. Die Freigabe gilt nur für die geöffnete Ansicht. Wetter-/Adresssuch-/Uploadfunktionen zusätzlicher Website-Anbieter bleiben gesperrt; entsprechende Website-Abschnitte können deshalb Fehlermeldungen anzeigen. Änderungen der Website können die Einbettung beeinflussen. Im Rotlichtmodus gehen die normalen Farbabstufungen der Kartenlegende verloren; für den Vergleich zusätzlich die numerischen Ortsdetails beachten.

Der Astra-Lichtindex wird aus der Helligkeit des dargestellten NASA-VIIRS-Bildes abgeleitet. Bortle- und mag/arcsec²-Angaben sind unvalidierte Näherungen, keine physikalische Umrechnung oder SQM-Messung. Wetter, Mond und Lichtglocken aus der Umgebung sind darin nicht modelliert. Eine kalibrierte flächendeckende Himmelshelligkeitskarte bleibt offen.

NASA-Bilder stammen weiterhin von 2016. Mehr Zoom vergrößert das vorhandene Raster und liefert keine neuere oder genauere Messung. Karten erfordern die bestehende Online-Freigabe. Nur öffentliche OSM-Grundkarten liegen im privaten löschbaren Cache; Nachtlicht und Vergleiche bleiben im Arbeitsspeicher.

## Durchgeführte Prüfungen

- 104 JVM-Tests und 30 Android-Instrumentierungstests erfolgreich, darunter Raster-Sampling, Rundung, Cache-Generationen, Teilfehler/Wiederholung, externe URL-Regeln, fehlende Freigabe und iframe-CSP in Chromium.
- Der neue Layouttest lädt das iframe zunächst ohne Höhe und prüft anschließend drei Größen einschließlich des tatsächlichen inneren Viewports. Damit ist der im Live-Test gefundene unsichtbare Kartenbereich abgesichert.
- Lint: 0 Fehler, 0 Warnungen, 2 bereits bestehende Hinweise zur Compose-Zustandsoptimierung. Debug-APK und minifiziertes Release-AAB erfolgreich gebaut.
- Android-17/API-37-Emulator mit Demo-Berlin: echte NASA/OSM-Kacheln, Ortsvergleich und erhaltener Astra-Kartenausschnitt; externe farbige Karte und Bortle-/Helligkeitsdetails, Android-Zurück, Hintergrund/Rückkehr, Rotlicht und Fehlermeldung bei abgeschaltetem Netz visuell geprüft.

Die automatisierten WebView-Tests verwenden lokale Fixtures. Der ergänzende Live-Test ist eine Stichprobe, kein vollständiger Netzwerk-/Datenschutzaudit und keine Zusage, dass jede Funktion der fremden Website verfügbar ist.

## Auf weiteren Geräten testen

1. Kalender öffnen, Lichtkarte vergrößern, verschieben und zoomen. Mit Taste und Android-Zurück schließen und den erhaltenen Ausschnitt prüfen.
2. Umkreis wechseln, „Ort vergleichen“ aktivieren und einen Ort antippen. „Raster-Klassen“ ein-/ausschalten, danach Nachtlicht ausblenden und wieder aktivieren.
3. Netzwerk während des Ladens unterbrechen und „Neu laden“ verwenden. Vorhandene Kacheln sollen sichtbar bleiben; fehlende Ebenen werden gemeldet. Nach Wiederherstellung erneut laden.
4. Den Kartencache löschen und Karte erneut öffnen. Favoriten und Beobachtungslisten müssen erhalten bleiben.
5. Rotlicht, Hoch-/Querformat, große Android-Schrift und Zurück-Geste prüfen. Karte bei laufendem Abruf in den Hintergrund schicken und wieder öffnen.
6. Mit deaktivierten Online-Daten starten: Die App soll die Karte nicht abrufen. Ohne Standortfreigabe muss der Demo-Standort Berlin erkennbar sein.
7. „Externe Karte laden“ erst nach Lesen des Anbieterhinweises auswählen. Farbige Karte und Standortdetails prüfen, schließen, bei unterbrochenem Netzwerk erneut laden und in die Astra-Karte zurückkehren. Rotlicht und Rückkehr aus dem Hintergrund auch dort prüfen.

Bitte Gerätemodell, Android-Version und Schritte zum Wiederholen angeben. Reale Geräteprüfungen einschließlich Android 9 stehen aus; Akkutests bleiben niedrig priorisiert.

## Installation

Die angehängte `app-debug.apk` ist eine mit dem Entwicklungszertifikat signierte, direkt installierbare Testversion. Das AAB ist ein technischer Build und nicht direkt installierbar; die Play-Store-Freigabe erfolgt separat. Die Datei `pre-release-1.1.7.sha256` enthält die SHA-256-Prüfsummen beider Dateien.

Nächste Aufgaben: P1.6 (Wetterdaten während Aktualisierungen sichtbar lassen) und P1.7 (schnellerer App-/Kartenstart).
