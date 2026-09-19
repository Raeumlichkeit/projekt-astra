# Projekt Astra 1.1.9-pre.6 · Beta

Beobachtungstagebuch mit 100 % lokaler Speicherung, Seeing-Bewertung, Teleskopnotizen, Fotoverknüpfung, Export/Import und vollständiger Datenkontrolle (P2.9). Sechster Teil von 1.1.9, Version Code 24. Dieser Pre-Release stammt von `beta`; `main` bleibt unverändert bei `1.1.8-pre.1`.

## Neuerungen & Funktionen

- **Beobachtungstagebuch (P2.9):**
  - **Beobachtungen protokollieren:** Halte deine Nächte fest mit Datum/Uhrzeit, Notizen, Seeing-Bewertung (1 bis 5 Sterne mit intuitiver Skala) und verwendeter Ausrüstung (Teleskop, Fernglas, Okular).
  - **Direkt aus der Sternkarte:** In der Objekt-Detailansicht (`ObjectDetails`) steht nun der Button **„Beobachten“** zur Verfügung, der ein Beobachtungsformular mit vorausgefülltem Objektnamen, Objekttyp und Katalog-ID öffnet.
  - **Optionale Fotos:** Eigene Beobachtungsfotos können angehängt werden; die Bilder werden in den privaten, isolierten App-Speicher kopiert (`context.filesDir/logbook_photos/`). Thumbnail-Vorschau und Vollbildansicht direkt in der App.
  - **100 % lokaler Datenschutz:** Keine Cloud-Synchronisierung. Standortdaten (GPS-Koordinaten oder Ortsname) sind standardmäßig deaktiviert und werden nur bei bewusster Aktivierung des Nutzers an den jeweiligen Eintrag angehängt.
  - **Löschen mit Dateibereinigung:** Einzelne Einträge können jederzeit bearbeitet oder gelöscht werden; zugehörige lokale Fotodateien werden dabei rückstandsfrei entfernt. Ein Bestätigungsdialog ermöglicht das vollständige Löschen aller Tagebucheinträge und Fotos.
  - **Export & Import (JSON):**
    - Bewusster JSON-Export per Zwischenablage oder Share-Sheet.
    - Robuster Import mit Größenprüfung (maximal 10 MB, bis zu 5.000 Einträge), Vorschau der erkannten Einträge und des Datumszeitraums sowie Wahlmöglichkeit zwischen Zusammenführen (Merge) und Überschreiben.
    - Transparenter Datenschutz- und EXIF-Hinweis beim Export bezüglich potenzieller Kamera-Metadaten in Fotodateien.
  - **Backup-Ausschluss:** Manifest und Datenextraktionsregeln schließen Tagebuch und Fotos strikt von Cloud-Backups und automatischen Übertragungen aus.

## Prüfstand

- 144 JVM-Tests erfolgreich (`:app:testDebugUnitTest`), inklusive Serialisierung, Default-Standort-Nullwerte, Array-/Root-JSON-Parsing, Seeing-Bewertungs-Clamping, Größenlimits und Payload-Validierung.
- Lint-Prüfung (`:app:lintDebug`) fehlerfrei abgeschlossen (0 Fehler).
- Debug-APK und Release-AAB erfolgreich gebaut.

## APK & Installation

Die `app-debug.apk` ist mit dem Entwicklungszertifikat signiert und direkt installierbar. Prüfsummen: `pre-release-1.1.9-pre.6.sha256`.
