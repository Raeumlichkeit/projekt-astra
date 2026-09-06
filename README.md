# Projekt Astra

Ein Android-MVP für eine sensorgesteuerte Sternkarte und aktuelle astronomische Wetterdaten.

## Bereits umgesetzt

- Sternpositionen aus Rektaszension/Deklination, Beobachterposition und Uhrzeit
- Ausrichtung über Android-Rotationssensor
- AR-Modus mit CameraX-Kamerabild und Stern-Overlay
- Korrektur von magnetisch Nord auf geografisch Nord
- GPS-Standort mit Berlin als klar gekennzeichnetem Demo-Fallback
- Offline-Katalog mit 5.041 HIP-Sternen bis zur visuellen Helligkeit 6,0 (HYG v4.1)
- optional zuschaltbarer Offline-Katalog mit 1.016 Deep-Sky-Objekten (OpenNGC)
- Sternfarben, Horizont sowie erweiterte Linien und Namen markanter Sternbilder
- vollständige Grenzen aller 88 IAU-Sternbilder sowie optionale, dezente Illustrationen
- dynamische Milchstraßenebene, passend zu Uhrzeit, Standort und aktuellem Kartenausschnitt
- lokales 360°-Geländeprofil aus GLO-90-Höhendaten für einen realistisch verdeckten Horizont, mit Offline-Fallback
- Sonne, Mond und alle sieben von der Erde sichtbaren Planeten mit topozentrischen Ephemeriden
- aktuelle Planetenhelligkeit, Phase, Entfernung und berechnete zwölfstündige Bahn
- ausführliche Objektinformationen, Eigenbewegung und berechnete Positionen für zwölf Stunden
- echte DSS2-Himmelsaufnahmen passend zur Koordinate des ausgewählten Objekts
- Live-Wetter von Open-Meteo ohne API-Schlüssel
- interaktive Wetterkarte mit lokal gebündeltem Kartenrenderer, Regenradar und Bewölkung
- Pull-to-Refresh aktualisiert Wetter, Wetterkarte und GPS-Standort
- stündliche 24-Stunden-Zeitleiste für Wolken, Regenwahrscheinlichkeit, Wind und Beobachtungssicht
- aufklappbare Astra-Score-Erklärung mit Einzelabzügen für Bewölkung, Regen, Wind, Sichtweite, Mondlicht und Lichtverschmutzung
- manuell verschiebbare und zoombare Sternkarte; AR bleibt separat sensorgesteuert
- sphärische Perspektive der manuellen Karte ohne leere Zenitbereiche; randgenaues Clipping von Sternbildlinien, IAU-Grenzen und Milchstraße (1.1.2)
- lesbare Randbeschriftungen und durchgängiger Geländehorizont; gemeinsame Himmelszeit mit Aktualisierung alle fünf Sekunden im Vordergrund
- standortbezogener Himmelskalender für Meteorschauer sowie Sonnen- und Mondfinsternisse
- automatische Online-Aktualisierung der jährlichen IMO-Meteorschauer-Maxima mit Offline-Fallback
- exakt berechnete lokale Sonnenfinsterniskontakte, Bedeckung und Sonnenhöhe
- lokale Mondfinsternisfilterung mit Kontaktzeiten und Mondhöhe
- lokale Beobachtungseinschätzung aus Radiantenhöhe und ungefährem Mondlicht
- interaktive NASA-VIIRS-Nachtlichtkarte als Orientierung für Lichtverschmutzung
- numerischer VIIRS-Lichtindex, geschätzte Bortle-Klasse und Einbezug in den Astra-Score
- lokale Favoriten und Beobachtungslisten mit einstellbaren Ereigniserinnerungen
- geführte Kompass-/AR-Kalibrierung mit Android-Sensorgenauigkeit
- globaler, dauerhaft gespeicherter Rotlichtmodus mit Schnellschalter in der Sternkarte
- adaptives und monochromes App-Icon sowie Android-12+-Startbildschirm
- Android-17-konformer Location Button für freiwilligen, sitzungsbasierten Standortzugriff
- für Google Play vorbereiteter Release-Build, Store-Texte, Grafiken und Datenschutzunterlagen
- Referenztests für Ephemeriden und die lokale Sonnenfinsternis vom 2. August 2027
- Dunkles, für Nachtbeobachtung optimiertes Compose-UI

## Offene Aufgaben

Die priorisierte [Aufgabenliste](AUFGABEN.md) enthält die nächsten Ausbauschritte: Objektsuche und Nachführen, Zeitsteuerung, Vollbildmodus, Nachtplanung, Beobachtungstagebuch, Instrumenten-Sichtfeld und AR-Zielhilfe. Akkutests haben ausdrücklich niedrige Priorität (P3). Veröffentlichung und Sicherheitsfreigaben werden separat geführt.

## Lokal starten

1. Das Verzeichnis in einer aktuellen Version von Android Studio öffnen.
2. Android SDK 37 installieren, falls Android Studio danach fragt.
3. Gradle-Synchronisierung durchführen.
4. Die App auf einem Android-Gerät mit Standort- und Bewegungssensoren starten.

Auf einem Emulator funktioniert die Oberfläche, die automatische Ausrichtung benötigt jedoch simulierte Sensorwerte oder ein echtes Gerät.

## Google-Play-Release

Die vorbereiteten Store-Texte, Grafiken, Datenschutzangaben und die vollständige Veröffentlichungsliste liegen unter `play-store/`. Ein Release-Bundle lässt sich auch ohne Schlüssel zur technischen Prüfung mit `gradlew bundleRelease` erstellen. Für den Upload muss einmalig ein privater Upload-Key eingerichtet werden; die Anleitung steht in `play-store/release-checklist.md`.

## Daten und Datenschutz

Version 1.1.1 startet offline. GPS bleibt für Himmels- und Ereignisberechnungen lokal. Nach separater Online-Freigabe verwenden Wetter/Karten einen auf 0,01° gerundeten Ort; das genaue Geländeprofil benötigt eine zusätzliche Freigabe unter **Info**. Anbieter erhalten IP-Adresse, Anfragezeit und Ressourcenparameter. Open-Meteo nennt eine Protokollaufbewahrung von bis zu 90 Tagen. Standortantworten bleiben in der App im Arbeitsspeicher. Öffentliche OSM-Kacheln liegen separat im privaten, löschbaren Cache (32 MB, Gültigkeit maximal 30 Tage, Bereinigung beim nächsten Start/Abruf). Kacheln können betrachtete Regionen erkennen lassen.

Favoriten, Beobachtungslisten und Erinnerungseinstellungen bleiben lokal. WebViews verwenden keine Cookies, keinen DOM-Speicher und keinen Browser-Diskcache; alte WebView-Daten werden beim Update entfernt. Android-App-Backups und Geräteübertragung sind ausdrücklich ausgeschlossen. Standortlistener und App-Netzwerkzugriffe enden beim Verlassen des Vordergrunds. Details: [Datenschutzerklärung](play-store/privacy-policy.html).

## Sicherheitsstand und Prüfungen

- [Ursprünglicher Prüfbericht (1.1.0)](SECURITY_REVIEW.md)
- [Maßnahmen, Nachweise und offene Release-Punkte (1.1.1)](SECURITY_REMEDIATION.md)
- Kein Nachweis einer ISO/IEC-27001-Zertifizierung: Die Norm betrifft auch das organisatorische Informationssicherheitsmanagement, nicht nur App-Code.
- IMO-PDFs: isolierter Android-Prozess ohne App-Daten/Internet, 4-MB-Eingangslimit, höchstens 64 Seiten/131.072 Textzeichen, 20-Sekunden-Abbruch.
- Python-Updater: gepinntes, hashgeprüftes pypdf 6.17.0, Prozess-/Größenlimits, Linux-Speicherlimit; getrennte CI-Jobs für PDF-Verarbeitung und schreibberechtigte JSON-Veröffentlichung.
- Gradle-Distribution mit SHA-256, Abhängigkeits-Lockfile und Verifikationsmetadaten. Neuaufnahme weiterer Checksummen immer prüfen; nicht blind regenerieren.

Prüfen: `gradlew testDebugUnitTest connectedDebugAndroidTest lint bundleRelease`. Instrumentierung nur auf einem Testemulator ausführen: Androids Testinstallation kann App-Testdaten entfernen. Python: virtuelle Umgebung mit Python >=3.11, `pip install --require-hashes --only-binary=:all: -r scripts/requirements-imo.txt`, dann `python -m unittest discover -s scripts -p 'test_*.py'`.

`gradlew verifyPlayRelease` blockiert fehlenden Upload-Key, fehlende Bundle-Signatur oder eine offene Datenschutzfreigabe. Ein erfolgreiches normales `bundleRelease` bedeutet ohne lokale Schlüsselkonfiguration **nicht**, dass das Bundle signiert oder veröffentlichungsfertig ist.

Der abgeleitete Offline-Sternkatalog basiert auf HYG v4.1 und steht unter CC BY-SA 4.0.
Details stehen in `THIRD_PARTY_NOTICES.md`.
