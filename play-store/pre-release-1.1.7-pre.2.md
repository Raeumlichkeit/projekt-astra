# Projekt Astra 1.1.7-pre.2

Kleine Hinweiskorrektur zur Lichtverschmutzung. Version Code 17.

- Direkt am Astra-Score steht jetzt sichtbar „Veraltete Lichtdaten · NASA 2016“, unabhängig von der ausgeklappten Berechnungserklärung.
- Auch die interne Lichtkarte warnt über ihren Bedienelementen, einschließlich Vollbild. Neue Beleuchtung ist nicht erfasst; Neuladen ändert den Datenstand nicht.
- Datenjahr auch an der Wetter-Lichtkachel und in der Score-Aufschlüsselung ergänzt.
- Score-Formel, Datenquellen und Online-Freigaben bleiben unverändert. Die externe LightPollutionMap.app bleibt eine separate Ansicht; keine automatische oder manuelle Übernahme ihrer Werte eingeführt.

## Prüfstand

- 105 JVM-Tests und 31 Android-Instrumentierungstests erfolgreich (API-37-Emulator).
- Neuer Test: NASA-2016-Warnung ist bei 320 × 480 dp ohne aufgeklappte Details oder Scrollen sichtbar, auch ohne Netzwerkfreigabe.
- Neuer Test: Warntext in der Score-Aufschlüsselung nennt den veralteten Datenstand; der Lichtabzug bleibt unverändert bei maximal 20 Punkten.
- Debug-APK, minifiziertes Release-AAB und Lint erfolgreich; 0 Lint-Fehler, 0 Warnungen, 2 bestehende Hinweise zur Compose-Zustandsoptimierung.

## Auf Geräten prüfen

1. Wetter öffnen: Hinweis im Astra-Score lesen, ohne die Berechnungserklärung zu öffnen. Die Warnung betrifft die Lichtverschmutzungsdaten, nicht das aktuelle Wetter.
2. Kalender → interne Astra-Karte öffnen und vergrößern. Warnung „NASA 2016“ oberhalb der Bedienelemente prüfen.
3. Große Schrift, kleines Display und Rotlicht prüfen: Hinweise und Kartenbedienung müssen erreichbar bleiben.

Die bisherigen [Grenzen und Geräte-Testfälle](pre-release-1.1.7.md) gelten weiter. Die `app-debug.apk` ist mit dem Entwicklungszertifikat signiert und direkt installierbar. Das AAB ist nur ein technischer Build; keine Play-Store-Freigabe. Prüfsummen: `pre-release-1.1.7-pre.2.sha256`.
