# Projekt Astra 1.1.9-pre.7 · Beta

Fernglas- und Teleskop-Sichtfeld (FOV), Telrad-Sucher, anpassbare Geräteprofile sowie Orientierungsausrichtung mit Drehung und Spiegelung (P2.10). Siebter Teil von 1.1.9, Version Code 25. Dieser Pre-Release stammt von `beta`; `main` bleibt unverändert bei `1.1.8-pre.1`.

## Neuerungen & Funktionen

- **Fernglas- und Teleskop-Sichtfeld (P2.10):**
  - **Sichtfeld-Kreis (FOV Overlay):** Frei skalierbarer Sichtfeldkreis über der Sternkarte mit exakter Winkelgröße (0,1° bis 30,0°). Passend zu Ferngläsern und Okularen zentriert im Sichtfeld der Karte.
  - **Telrad-Sucher:** Optionaler Telrad-Modus mit drei konzentrischen Zielkreisen (0,5°, 2,0° und 4,0°) und feiner Fadenkreuz-Markierung zum präzisen Aufsuchen von Deep-Sky-Objekten und Orientierungssternen.
  - **Geräteprofile:**
    - Standardprofile für beliebte Optiken vorkonfiguriert: 10×50 Fernglas (6,5° FOV), 8×42 Fernglas (7,5° FOV), 8" Dobson mit 25 mm Okular (1,08° FOV, 48×) und 8" Dobson mit 10 mm Okular (0,43° FOV, 120×).
    - Eigene Teleskop- und Fernglasprofile anlegen, bearbeiten und speichern mit Teleskop-Brennweite, Öffnung, Okularbrennweite und scheinbarem Gesichtsfeld (AFOV).
    - Automatische Berechnung von Vergrößerung ($V = F / f$), wahrem Gesichtsfeld ($TFOV \approx AFOV / V$) und Austrittspupille ($AP = D / V$) mit Kennzeichnung als Näherungswerte.
  - **Karten-Ausrichtung passend zum Instrument:**
    - Drehung um 0°, 90°, 180° (Newton-Invertierung) oder 270° zur perfekten Deckungsgleichheit mit dem Blick durch den Okularauszug.
    - Horizontale Spiegelung (Zenitspiegel / Amici-Prisma-Korrektur).
    - Wählbare Beschriftungsausrichtung: Standardmäßig bleiben Stern- und Objektnamen aufrecht lesbar; optional können Beschriftungen zusammen mit der Optik mitrotieren.
  - **Exakte Touch-Präzision und intuitive Gesten:**
    - Mathematisch exakte inverse Koordinatentransformation für Fingertipps: Sterne und Deep-Sky-Objekte werden auch unter beliebig gedrehter oder gespiegelter Ansicht pixelgenau ausgewählt.
    - Transformierte Wischgesten (Pan Delta), sodass sich die Karte unter dem Finger immer in die natürliche Richtung bewegt.
  - **Status-Badge & 1-Klick-Reset:**
    - Bei aktiver Drehung, Spiegelung oder FOV-Modus erscheint ein dezentes Badge über der Sternkarte mit aktuellem Profilnamen, Gradanzeige und direktem 1-Klick-Zurücksetzen („Standard“).
  - **Strikte AR-Trennung:**
    - Kamera-Vorschau, AR-Tracking und Gerätesensorik bleiben vollständig unbeeinflusst und frei von optischer Verzerrung.
  - **100 % lokaler Datenschutz:**
    - Sämtliche Optik-Einstellungen und Geräteprofile werden ausschließlich lokal auf dem Gerät gespeichert (`SharedPreferences`).

## Prüfstand

- 150 JVM-Tests erfolgreich (`:app:testDebugUnitTest`), inklusive optischer Formeln, FOV-Berechnungen, JSON-Serialisierung, mathematischer Transformations-Roundtrips und Pan-Delta-Invertierung.
- Lint-Prüfung (`:app:lintDebug`) fehlerfrei abgeschlossen (0 Fehler).
- Debug-APK und Release-AAB erfolgreich gebaut.

## APK & Installation

Die `app-debug.apk` ist mit dem Entwicklungszertifikat signiert und direkt installierbar. Prüfsummen: `pre-release-1.1.9-pre.7.sha256`.
