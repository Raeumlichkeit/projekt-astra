# Projekt Astra 1.1.8-pre.1

Wetteraktualisierung ohne Datenlücke (P1.6). Version Code 18.

## Änderungen

- Herunterziehen lässt die letzte erfolgreiche Wetterantwort, Stundenübersicht und den Astra-Score sichtbar. Neue Daten ersetzen sie erst nach vollständiger Prüfung.
- Ein dezenter Hinweis zeigt den laufenden Abruf. Bei Fehlern bleiben alte Werte mit Datenstand, Abrufalter und einer Wiederholen-Aktion stehen.
- Wetter, Lichtschätzung, Radar, Wolkenraster und GPS werden getrennt behandelt. Ein Fehler entfernt nicht die übrigen Daten. Alte Wetterwerte werden bei einem Standortwechsel ausdrücklich dem bisherigen Ort zugeordnet.
- Die Kartenansicht wird beim Aktualisieren nicht neu geladen. Zoom, Position und Ebenenschalter bleiben erhalten; bei einem neuen Beobachtungsort wird die Karte dorthin zentriert.
- Radar ersetzt ein bestehendes Bild erst, wenn die sichtbaren Ersatzkacheln geladen sind. Ein unvollständiges Wolkenraster ersetzt keine vollständige Ebene. Beide Quellen zeigen ihren eigenen Zeitstand und Fehlerhinweis.
- Vorhersagestunden und Score verwenden feste Zeitstempel. Mitternacht und doppelte Ortszeiten bei der Zeitumstellung verschieben die Zuordnung nicht.
- Hintergrundwechsel beenden laufende Abrufe und Kartentimer. Rückkehr lädt neu, ohne vorhandene Werte zuvor zu löschen; verspätete Antworten früherer Abrufe werden ignoriert.

## Datenschutz und Grenzen

- Keine neue Berechtigung, kein zusätzlicher Anbieter, keine neue dauerhafte Wetter- oder Standortablage. Die letzte Wetterantwort liegt nur im Arbeitsspeicher der geöffneten Ansicht; Tabwechsel oder Prozessende verwerfen sie. Vorhandener Grundkartencache und Datenschutzfreigaben bleiben unverändert.
- Standortkoordinaten für Online-Abfragen bleiben auf 0,01° gerundet. Bis zu einem neuen GPS-Fix gilt der letzte verfügbare Ort; ohne Standort bleibt Berlin ausdrücklich als Demo gekennzeichnet.
- Alte Wetterdaten sind keine aktuelle Messung. Karten- und Wetterstände können voneinander abweichen. Das Wolkenraster ist eine grobe Modellansicht, kein hochauflösendes Satellitenbild; Radarabdeckung hängt vom Anbieter ab.
- Die NASA-Lichtdaten bleiben von 2016 und werden weiterhin als veraltet gekennzeichnet. Werte der externen Lichtkarte werden nicht in den Score übernommen.

## Prüfstand

- 117 JVM-Tests erfolgreich, darunter zwölf neue Tests für Datenbeibehaltung, unabhängige Lichtantworten, Fehler, Standortwechsel, überholte Antworten, Hintergrund/Rückkehr, Mitternacht, Zeitumstellung und unvollständige Antworten.
- 40 Android-Instrumentierungstests erfolgreich auf dem API-37-Emulator. Sieben neue WebView-Tests prüfen verzögerte Antworten, ausgefallene Radar-Metadaten/-Kacheln, Teilraster, Standortwechsel, Schalter/Zoom, Hintergrund und fehlende Netzwerkfreigabe. Zwei Oberflächentests verwenden eine echte Pull-Geste und einen Lifecycle-Wechsel.
- Debug-APK, minifiziertes Release-AAB und Lint erfolgreich: 0 Fehler, 0 Warnungen, 2 bestehende Compose-Optimierungshinweise.
- Live-Abfrage für den öffentlichen Demo-Standort Berlin bestätigt das Epoch-Zeitformat und vollständige Stundenarrays von Open-Meteo. Reproduzierbare Fehler-/Kartentests verwenden lokale Testantworten ohne externe Server.

## Bitte auf weiteren Geräten prüfen

1. Wetter laden und herunterziehen: Score, Stunden und Karte dürfen während des Abrufs nicht verschwinden.
2. Nach erfolgreichem Laden Flugmodus aktivieren und aktualisieren: alte Daten samt Zeitstand müssen bleiben, mit verständlichem Fehlerhinweis. Danach Netz wieder aktivieren und erneut versuchen.
3. Karte zoomen und Ebenen umschalten, dann aktualisieren: die gewählten Einstellungen müssen erhalten bleiben. Radar und Bewölkung dürfen unabhängig fertig werden.
4. Während des Abrufs in den Hintergrund wechseln und zurückkehren. Keine leere Ansicht und kein Überschreiben durch eine alte Antwort.
5. Mit echtem GPS den Standort wechseln sowie kleine Displays/große Schrift prüfen. Diese Geräteprüfungen sind noch offen; Akkutests bleiben niedrig priorisiert.

Die `app-debug.apk` ist mit dem Entwicklungszertifikat signiert und direkt installierbar. Das AAB ist ein technischer, nicht für den Play Store freigegebener Build ohne Upload-Signatur. Prüfsummen: `pre-release-1.1.8-pre.1.sha256`. Als Nächstes folgt P1.7: schnellerer App- und Sternkartenstart.
