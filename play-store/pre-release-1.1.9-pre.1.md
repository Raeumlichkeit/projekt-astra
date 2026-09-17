# Projekt Astra 1.1.9-pre.1 · Beta

Gemeinsamer Katalogcache und wiederholbare Ladefehler. Erster Teil von P1.7, Version Code 19. Dieser Pre-Release stammt von `beta`; `main` bleibt unverändert bei `1.1.8-pre.1`.

## Änderungen

- Sternkarte und Beobachtungsliste verwenden denselben Katalogcache im Arbeitsspeicher. HYG-Sterne, OpenNGC-Objekte, IAU-Sternbildgrenzen und der statische Suchindex werden außerhalb des UI-Threads aufgebaut und bei weiteren Tabwechseln wiederverwendet.
- Fertige Ladestufen bleiben bei Abbruch oder einem Fehler in einer späteren Stufe erhalten. Gleichzeitige Aufrufe laden dieselben Daten nicht mehrfach.
- Ladefehler werden nicht mehr als erfolgreiche Ersatz- oder Leerkataloge gespeichert. **Katalog erneut laden** wiederholt nur die noch fehlenden Stufen. Die kleine Ersatz-Sternkarte dient während des Ladens und bei Fehlern nur der Anzeige.
- Die Beobachtungsliste zeigt den Ladezustand und zählt gespeicherte Vormerkungen auch dann korrekt, wenn deren Namen noch nicht aufgelöst sind. Ein Ladefehler entfernt keine Favoriten.
- Die Suche trennt den statischen Katalog von den zeit- und ortsabhängigen Sonnensystemzielen. Planeten bleiben auch während des Katalogaufbaus suchbar; ein vorgemerktes Objekt wird erst nach Laden der Objektkataloge aufgelöst.

## Datenschutz und Grenzen

- Keine neue Abhängigkeit, Berechtigung, Datenquelle oder dauerhafte Ablage. Der neue Cache enthält ausschließlich mitgelieferte, ortsunabhängige Katalogdaten und deren Suchindex; keinen Standort, Suchtext oder aktuelle Planetenpositionen. Er wird mit dem App-Prozess verworfen.
- Vorhandene Datenschutzfreigaben und die Speicherung von Favoriten bleiben unverändert. Die NASA-Lichtdaten bleiben von 2016 und werden weiterhin als veraltet gekennzeichnet; Daten der externen Lichtkarte werden nicht in den Astra-Score übernommen.
- P1.7 ist noch nicht abgeschlossen: Textur-/GPU-Start, Priorisierung des ersten sichtbaren Kartenrahmens, Gelände, Low-Memory-Verhalten und Messungen auf leistungsschwachen echten Geräten stehen aus. Dieser Stand verspricht keine bestimmte Startzeit oder bereits nachgewiesene Beschleunigung.

## Prüfstand

- 124 JVM-Tests erfolgreich, darunter Cache-Wiederverwendung, paralleles Laden, Teilfehler, Wiederholen, Abbruch, ungültige IAU-Daten und die getrennte Suche nach beweglichen Objekten.
- 43 Android-Instrumentierungstests erfolgreich auf dem API-37-Emulator. Neue Tests prüfen die vollständigen mitgelieferten Kataloge, Fehler und Wiederholen aller drei echten Loader sowie die Beobachtungsliste mit Ladefehler, Retry, erhaltenem Favoriten und erneutem Öffnen ohne erneutes Parsen.
- Debug-APK, minifiziertes Release-AAB und Lint erfolgreich: 0 Fehler, 0 Warnungen, 2 bestehende Compose-Optimierungshinweise.

## Bitte auf weiteren Geräten prüfen

1. App vollständig schließen und erneut öffnen: Ladehinweis und anschließend vollständige Sternkarte kontrollieren; währenddessen bedienen und nach einem Planeten suchen.
2. Zwischen Sternkarte und Beobachtungsliste mehrfach wechseln. Sterne, Deep-Sky-Objekte und Favoriten müssen erhalten beziehungsweise nach dem Laden wieder aufgelöst sein.
3. Einen nicht zur kleinen Ersatzkarte gehörenden Stern wie den Polarstern vormerken. Nach einem App-Neustart in der Liste öffnen und zur Sternkarte springen.
4. Während des Ladens den Tab wechseln oder die App in den Hintergrund schicken und zurückkehren. Suche und Katalog dürfen nicht dauerhaft unvollständig bleiben.
5. Flugmodus, große Schrift und ein schwächeres Handy prüfen. Auffällige Startzeiten bitte mit Gerätemodell und Android-Version melden; vergleichbare Kalt-/Warmstartmessungen sind noch offen. Akkutests bleiben niedrig priorisiert.

Die `app-debug.apk` ist mit dem Entwicklungszertifikat signiert und direkt installierbar. Das AAB ist ein technischer, nicht für den Play Store freigegebener Build ohne Upload-Signatur. Prüfsummen: `pre-release-1.1.9-pre.1.sha256`.
