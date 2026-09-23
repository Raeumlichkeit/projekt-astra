# Projekt Astra 1.1.9-pre.4 · Beta

Beobachtungsfenster heute Nacht („Heute Nacht im Überblick“) und ausrüstungsbasierte Beobachtungsempfehlungen („Was lohnt sich heute Nacht?“). Vierter Teil von 1.1.9, Version Code 22. Dieser Pre-Release stammt von `beta`; `main` bleibt unverändert bei `1.1.8-pre.1`.

## Neuerungen

- **Beobachtungsfenster heute Nacht im Überblick (`TonightWindowCalculator`):**
  - Exakte lokale Berechnung des Nachthimmelfensters anhand Sonnenstand und Dämmerungsphasen (Sonnenuntergang, bürgerliche/nautische/astronomische Dämmerung, Sonnenaufgang).
  - Mondstatus: Beleuchtungsanteil in Prozent, Mondphasenbezeichnung sowie Mondstand (Auf- bzw. Untergangsfenster bezogen auf die Beobachtungsnacht).
  - Lokale Seeing- und Wetterintegration: Bei aktiver Internetverbindung wird die stündliche Bewölkungsprognose im Dunkelheitsfenster ausgewertet; bei Offline-Betrieb erfolgt eine rein astronomische Dunkelheitsberechnung mit transparenter Kennzeichnung.
- **Ziel-Empfehlungsengine („Was lohnt sich heute Nacht?“, `TonightTargetEngine`):**
  - Vorselektierte Highlights aus Sonnensystem (Planeten, Mond), markanten Leitsternen und beliebten Deep-Sky-Showpieces (u. a. Plejaden, Orionnebel, Andromeda, Hantelnebel, Ringnebel, Galaxienpaar M81/M82, Doppelsternhaufen h & χ Persei).
  - **Horizonthöhen-Schwellenwert:** Nur Objekte, die in der Nacht mindestens 16° über den Horizont steigen, werden empfohlen (Vermeidung von Dunst und Bodenhindernissen).
  - **Sphärische Mondabstandsberechnung:** Exakter Winkelabstand zum Mond am Kulminationszeitpunkt, um Blendung von lichtschwachen Deep-Sky-Objekten zu vermeiden.
  - **Qualitäts-Score (0–100):** Bewertet Kulminationshöhe, Helligkeit (mag), Mondabstand und Beobachtungskomfort (vor 01:00 Uhr).
  - **Ausrüstungs-Filter:** Filterchips für „Alle“, „Bloßes Auge“, „Fernglas“ und „Teleskop“.
- **Direktaktionen in den Empfehlungskarten:**
  - **„In Karte öffnen“:** Wechselt in die Sternkarte und zentriert das Ziel direkt.
  - **„Merken“:** Favorit mit einem Klick hinzufügen oder entfernen (mit gefülltem Stern-Icon synchron zur Favoritenliste).
- **100 % Privacy First & On-Device:**
  - Sämtliche Berechnungen (Sonnenstand, Mondphase, Kulmination, Ziel-Scores) erfolgen vollständig lokal auf dem Gerät.
  - Es werden keine Standortdaten ins Netz übertragen (Wetterabruf nutzt gerundete 0,01°-Koordinaten).
  - Keine Cloud-Backups, keine Telemetrie.

## Prüfstand

- 138 JVM-Tests erfolgreich (`:app:testDebugUnitTest`), inklusive `TonightRecommendationsTest`, `SkyStartupTest`, `LocationStoreTest` und Katalog-Regressionstests.
- Lint-Prüfung (`:app:lintDebug`) fehlerfrei abgeschlossen (0 Fehler).
- Debug-APK und Release-AAB erfolgreich gebaut.

## APK & Installation

Die `app-debug.apk` ist mit dem Entwicklungszertifikat signiert und direkt installierbar. Das AAB ist ein technischer, nicht für den Play Store freigegebener Build ohne Upload-Signatur. Prüfsummen: `pre-release-1.1.9-pre.4.sha256`.
