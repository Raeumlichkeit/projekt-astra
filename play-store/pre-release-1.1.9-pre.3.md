# Projekt Astra 1.1.9-pre.3 · Beta

Dauerhaftes lokales Merken des Beobachtungsstandorts mit Opt-out-Möglichkeit und Zurücksetzen auf die Berlin-Demo. Dritter Teil von 1.1.9, Version Code 21. Dieser Pre-Release stammt von `beta`; `main` bleibt unverändert bei `1.1.8-pre.1`.

## Änderungen

- **Lokaler Standort-Speicher (`LocationStore`):** Nach einmaliger Freigabe des Standorts über den Standort-Knopf wird der Beobachtungsort in den privaten App-Einstellungen (`astra_settings`) auf dem Gerät gespeichert.
- **Sofortiger Sternkarten-Start mit eigenem Himmel:** Beim nächsten App-Start lädt Astra sofort den gespeicherten Standort. Die Sternkarte richtet sich direkt auf den echten Horizont und Nachthimmel des Nutzers aus, ohne dass der Freigabeknopf bei jedem Kaltstart erneut gedrückt werden muss.
- **Opt-Out direkt in der Sternkarte:**
  - Ist ein Standort hinterlegt oder aktiv, wird der große Freigabebutton ausgeblendet.
  - In der Kartenbedienung wird der Status („Lokal gespeichert“ bzw. „GPS aktiv“) angezeigt.
  - Ein Klick auf **„Auf Demo zurücksetzen“** löscht die gespeicherten Koordinaten sofort und stellt den Himmel wieder auf die Berlin-Demo um.
- **Datenschutzkontrolle unter „Info“:**
  - In den Einstellungen unter „Deine Daten“ gibt es einen neuen Schalter **„Standort für die Sternkarte merken“**.
  - Beim Deaktivieren wird der gespeicherte Ort rückstandsfrei vom Gerät gelöscht.
- **100 % Datenschutz & Sicherheit:**
  - Koordinaten werden ausschließlich auf dem Gerät gespeichert und verlassen dieses niemals.
  - Durch `allowBackup="false"` und `data_extraction_rules.xml` sind Cloud-Backups und Gerätetransfers vollständig unterbunden.
  - Externe Wetter- und Kartenabrufe bleiben wie bisher auf 0,01° (~1 km) gerundet.

## Prüfstand

- 132 JVM-Tests erfolgreich (`:app:testDebugUnitTest`), inklusive Koordinaten-Parsing, Validierung, Bereichsprüfung, fehlenden Höhenwerten und Deaktivierungslogik.
- Lint-Prüfung (`:app:lintDebug`) fehlerfrei abgeschlossen (0 Fehler).
- Debug-APK und Release-AAB erfolgreich gebaut.

## APK & Installation

Die `app-debug.apk` ist mit dem Entwicklungszertifikat signiert und direkt installierbar. Das AAB ist ein technischer, nicht für den Play Store freigegebener Build ohne Upload-Signatur. Prüfsummen: `pre-release-1.1.9-pre.3.sha256`.
