# Projekt Astra

Projekt Astra ist eine persönliche Android-App für die manuelle Sternkarte, eine live ausgerichtete AR-Ansicht, astronomisches Wetter und Beobachtungsplanung.

**Beta-Stand:** `1.1.9-pre.22` · Version Code `40` · Android 9 / API 28 bis Android 17 / API 37<br>
Dieser Korrekturstand liegt auf `beta`; `main` enthält den zuvor zusammengeführten Stand `1.1.9-pre.14`. Zum Testen: [v1.1.9-pre.22 öffnen](https://github.com/Raeumlichkeit/projekt-astra/releases/tag/v1.1.9-pre.22).

![Sternkarte](play-store/screenshots/01-sternenkarte.png)

## Funktionen

| Bereich | Enthalten |
| --- | --- |
| Sternkarte | Manuell verschieben und zoomen, sphärische Projektion, Sternfarben, Horizont, 88 IAU-Sternbildgrenzen, äquatoriale/horizontale Gitter, Referenzlinien (Äquator, Ekliptik, Galaktisch), Optik & FOV-Kreis/Telrad mit Drehung und Spiegelung, Handschuh-Modus (Lautstärketasten) |
| Sonnensystem & Mond | Galileische Jupitermonde (Io, Europa, Ganymed, Kallisto) mit Transit/Verfinsterungs-Ereignissen, Saturn Ringsystem & Titan, Mondterminator-Detailrelief & Kraterkatalog |
| Satelliten & ISS | Lokale Near-Earth-SGP4-Vorhersage für ISS und helle Satelliten mit benannten Passpfaden und Zeiten der größten Höhe (ohne Cloud/Tracking); benötigt zeitnahe TLEs, keine Deep-Space-/SDP4-Bahnen |
| Aufsuchhilfen | Interaktives Winkelabstand- und Positionsmesswerkzeug, geführter Star-Hopping-Assistent mit Telrad- und Okularfeldern |
| Beobachtungsplan | „Heute Nacht im Überblick“ (Dunkelheit, Mond & Seeing), Dew Monitor (Magnus-Tetens Taupunktwarnung), Beobachtungs-Challenges (Messier 110, Caldwell, Herschel 400), „Im Logbuch beobachtet“-Statusbadges |
| Tagebuch | Beobachtungstagebuch mit Fotos, Pickering 1–10, Antoniadi I–V, NELM fst, JSON-Backup, OAL 2.1 XML Standard-Export & Rotlicht-Textzusammenfassung |
| AR & Kamera | CameraX-Kamerabild mit Nacht-Belichtungskorrektur (AE EV Stepping), adaptiver Tiefpass-Sensorfilterung, manueller ±15° Pitch-Trimmung und AR-Zielhilfe |
| Widget & Design | Passives Homescreen-Widget (Mondphase, Dunkelheitsfenster, Wetter-Score ohne Hintergrund-GPS), OLED Reinstschwarz (#000000) & Rotlichtmodus |

## Neu in 1.1.9-pre.22 · Standortknopf und Schwenken

Im Hochformat bleibt der Android-17-Standortknopf fest oberhalb der Sternkarte; im kompakten Querformat öffnet „Standort verwenden“ den nativen Knopf in einem Dialog. Er überlagert die Karte beim Scrollen nicht mehr; die übrigen Bedienelemente bleiben scrollbar. Beim Schwenken werden Sternrichtungen einmal vorab berechnet statt für Tausende Sterne pro Bild erneut trigonometrisch projiziert. Wiederholte Updates mit unverändertem Himmelszustand lösen keinen zusätzlichen GPU-Frame aus; der Milchstraßen-Shader spart zwei redundante Normalisierungen pro Bildpunkt. Das verringert Rechenarbeit, garantiert aber noch keine ruckelfreie Darstellung auf jedem Handy. [Tests und Grenzen](play-store/pre-release-1.1.9-pre.22.md).

## Neu in 1.1.9-pre.21 · Horizont und benannte Satellitenbahnen

Beim Blick fast zum Zenit mit sehr weitem Sichtfeld ist der Horizont im Hochformat oben und unten sichtbar. Die obere Bodenzone zeigte bisher irrtümlich Sternhimmel; die Maske deckt nun beide Seiten ab. Sichtbare Satellitenbahnen tragen „SAT“, den Namen und die Zeit ihrer größten Höhe. Die optionale Ekliptiklinie erhält eine eigene Kennzeichnung, damit sie nicht mit einer Satellitenbahn verwechselt wird. [Tests und Grenzen](play-store/pre-release-1.1.9-pre.21.md).

## Neu in 1.1.9-pre.20 · Milchstraße nach dem Wiederöffnen vollständig

Android kann nach dem Minimieren den `TextureView`-Puffer erneut auf die volle Kartenfläche setzen. Die in pre.19 eingeführte 75-%-Pufferverkleinerung führte dann dazu, dass die Milchstraße nur noch als Rechteck unten links erschien. Diese Verkleinerung ist zurückgenommen; echte Größenwechsel der Kartenfläche werden weiterhin korrekt behandelt. Die übrigen Sternkarten-Optimierungen bleiben. Der [Pre-Release](play-store/pre-release-1.1.9-pre.20.md) dokumentiert Tests und die Performance-Grenze.

## Neu in 1.1.9-pre.19 · Weiteres Entlasten der Sternkarte

In pre.19 wurde die diffuse Milchstraßen-Fotoebene versuchsweise mit einem kleineren GPU-Puffer gezeichnet; diese Maßnahme ist seit pre.20 zurückgenommen. Das frühzeitige Auslassen sicher unter dem Horizont liegender Sterne bleibt. Der pre.19-Emulatortest zeigte einen Gewinn, aber kein durchgehend flüssiges Schwenken. Die goldene Diagonale mit Punkten ist eine vorausberechnete Satellitenbahn, die untere goldene Kurve der Horizont. Historischer Testumfang: [Pre-Release-Hinweise](play-store/pre-release-1.1.9-pre.19.md).

## Neu in 1.1.9-pre.18 · Ruhigeres Verschieben der Sternkarte

Schnelle Wischbewegungen gehen nicht mehr zwischen zwei Neuzeichnungen verloren. Die Milchstraßen-Textur nutzt die GPU-Filterung außerhalb der RA-Naht und berechnet ihre Blickrichtungsbasis nur einmal pro Bild; Sternpositionen sparen eine unnötige Winkel-Normalisierung. Das senkt die Renderarbeit, ersetzt aber keinen Test auf einem schwächeren echten Gerät. Details und Grenzen stehen in den [Pre-Release-Hinweisen](play-store/pre-release-1.1.9-pre.18.md).

## Neu in 1.1.9-pre.17 · Querformat

Beim Drehen bleibt der gewählte Reiter erhalten. Die manuelle Sternkarte behält ihre Blickrichtung; AR bleibt aktiviert und richtet sich nach den Sensoren neu aus. Suche und wichtige geöffnete Ansichten bleiben erhalten. Bei geringer Querformathöhe nutzen die Hauptseiten kürzere Kopfbereiche und eine für Screenreader beschriftete Symbolnavigation. Datenschutz- und Fotodialog halten die Schließen-Aktion erreichbar. Testumfang und Grenzen stehen in den [Pre-Release-Hinweisen](play-store/pre-release-1.1.9-pre.17.md).

## Neu in 1.1.9-pre.16 · Widget und Wartung

Das Homescreen-Widget erhält besser lesbare Schrift, Stringressourcen und ein vereinfachtes Layout. Ohne gespeicherten Wetterwert zeigt es „—“ statt eines erfundenen Scores. Die gemeldeten Updates für AndroidX Core, WorkManager und den Android-Test-Runner sowie die Kotlin-Wartungshinweise sind gezielt bearbeitet. Testumfang und Prüfstatus: [Pre-Release-Hinweise](play-store/pre-release-1.1.9-pre.16.md).

## Neu in 1.1.9-pre.15 · Fehlerkorrekturen nach dem Review

- **Logbuch und Datenschutz:** Atomisches Speichern schützt vor abgebrochenen Schreibvorgängen. Ungültige Importpfade und Zeitpunkte werden abgefangen; fremde JSON-Dateien können keine vorhandenen Fotos übernehmen. Abbrechen nach Fotoentfernung und reine Notizänderungen erhalten Fotos beziehungsweise historische Koordinaten.
- **Standort:** Dauerhaftes Merken ist ausdrücklich freiwillig. Eine GPS-Berechtigung allein speichert keinen Standort; „Standort vergessen“ stoppt die laufende Nutzung bis zur nächsten bewussten Aktivierung und aktualisiert das Widget.
- **Sternkarte:** Sternpositionen, AR-Zielhilfe und Milchstraße nutzen zueinander passende Projektionen. Optik-Drehung/-Spiegelung, enge Sichtfelder und Gelände-Verdeckung wurden korrigiert.
- **Astronomie:** Referenzgeprüfter Near-Earth-SGP4-Port, Lichtlaufzeit für Jupitermonde und ehrliche Dunkelheitsangaben bei Polartag und hellen Sommernächten. Satellitenpass-Berechnungen laufen außerhalb des UI-Threads.
- **Wartbarkeit:** Doppelte Testmodelle und ungenutzte Hilfen reduziert. Unit-/Modelltests im Ordner `e2e` sind **keine vollständigen Bedienungstests**; reale Android-UI- und Speichertests werden separat ausgeführt.

Testumfang und verbleibende Grenzen: [Pre-Release-Hinweise](play-store/pre-release-1.1.9-pre.15.md). Eine Vorabversion ist keine Play-Store- oder ISO-Freigabe.

## Neu in 1.1.9-pre.12 · Beta

- **Dynamische Sonnensystem-Körper & Mondterminator (P2.13):**
  - **Galileische Jupitermonde:** Echtzeitberechnung der Bahnen von Io, Europa, Ganymed und Kallisto; Erkennung von Schattentransiten, Okkultationen und Verfinsterungen mit schematischem Diagramm und Detail-Badges.
  - **Saturn Ringsystem & Titan:** Ringsystem-Neigungswinkel und Titan-Orbitposition.
  - **Mondterminator & Kraterrelief:** Colongitude $C_0$, optische Libration, selenographischer Morgenterminator und interaktiver `MoonDetailSheet` mit hervorgehobenen Kratern am Terminator.
  - **Lokale SGP4-Satellitenpass-Vorhersage:** Reine Offline-Berechnung von ISS- und Satellitenüberflügen aus gebündelten TLEs (`satellites_bright.tle`) ohne Hintergrund-GPS.
- **Himmelsvermessung, Koordinatengitter & Star-Hopping (P2.14):**
  - **Winkelabstandsmessung:** Exakte Berechnung des Abstands (Grad, Bogenminuten, Bogensekunden) und Positionswinkels zwischen beliebigen Sternen und Himmelskoordinaten.
  - **Astronomische Koordinatengitter:** Äquatoriales (RA/Dec) und horizontales (Az/Alt) Raster sowie Referenzlinien für Himmelsäquator, Ekliptik und galaktischen Äquator mit Rotlicht-Unterstützung.
  - **Star-Hopping-Assistent:** Schritt-für-Schritt-Routen von Orientierungssternen zu Deep-Sky-Objekten mit interaktivem HUD und Zentrier-Option.
- **Beobachtungspraxis, Dew Monitor & OAL XML Export (P2.15):**
  - **Beobachtungs-Challenges:** Messier 110, Caldwell 109 und Herschel 400 mit automatischem Fortschritt aus dem lokalen Logbuch.
  - **„Im Logbuch beobachtet“-Badges:** Dezente Statusanzeigen auf der Sternkarte, in der Objektsuche und im Beobachtungsplan.
  - **Dew Monitor (Taupunkt- & Beschlagswarnung):** 4-stufige Risikobewertung (Gering, Mäßig, Hoch, Akut) nach der Sonntag (1990) Magnus-Tetens-Formel.
  - **OAL 2.1 XML & Rotlicht-Textexport:** Vollständiger Export nach dem OpenAstronomyLog 2.1 XML-Standard sowie formatierte Textzusammenfassungen.
  - **Standard-Seeing-Skalen:** Pickering (1–10), Antoniadi (I–V) und freie Grenzgröße (NELM / fst).
- **Kamera-AR-Nachtoptimierung, Hardware & Feld-Usability (P2.16 & P2.17):**
  - **AR-Nachtbelichtung:** Gestufte Camera2/CameraX AE Belichtungskorrektur (+0 bis +3 EV) für dunkle Himmelsansichten.
  - **Sensorfilterung & Pitch-Trimm:** Adaptive Tiefpass-Alpha-Dämpfung bei engem FOV, zirkulare 360°-Azimut-Glättung und ±15° manueller Höhen-Offset.
  - **Handschuh-Modus (Volume-Key-Zoom):** Zoom per Lautstärketasten (+/-) auf dem Sternkarten-Tab bei frostigen Beobachtungsnächten.
  - **OLED Reinstschwarz (#000000):** Vollständig tiefschwarzes UI-Farbschema zur maximalen Akkuschonung und Dunkeladaption.
  - **Passives Homescreen-Widget:** `AstraAppWidgetProvider` mit Mondphase, Dunkelheitsfenster und gecachtem Wetter-Score ohne Hintergrund-GPS oder Hintergrunddienste.

- **Winkeltreue stereografische Projektion (Entzerrung bei weitem FOV):**
  - **Natürliche Sternfeldansicht beim Herauszoomen:** Umstellung der manuellen Sternkarten-Perspektive (`SkyProjection.kt`) von gnomonischer Rektilinearprojektion auf winkeltreue (konforme) stereografische Projektion. Beseitigt extreme Dehnungen und Verzerrungen an Bildrändern und Ecken bei weitem Sichtfeld (bis 150° FOV). Sternbilder behalten ihre Form ohne Verzerrung.
  - **Pixelgenaue Shader-Anpassung:** Der OpenGL-Fragment-Shader (`milky_way.frag`) der Milchstraße berechnet den Kamerastrahl nun ebenfalls per inverser stereografischer Projektion, sodass Milchstraßenstrukturen, Sterne und Sternbildlinien auf subpixel-genau übereinstimmen.
  - **Rotlichtmodus für Optik & FOV:** Vollständige Einbindung des Rotlichtmodus in das Optik-Status-Badge, den Optik-Einstellungs-Sheet (`OpticsFovSheet.kt`) und die Darstellung des Sichtfeldkreises auf der Sternkarte.
  - **Neue Unit-Tests:** 185 bestandene Unit-Tests (+2 neue mathematische Verifikationstests für stereografische Konformität und Entzerrung). Testhinweise: [1.1.9-pre.11](play-store/pre-release-1.1.9-pre.11.md).

## Neu in 1.1.9-pre.10 · Beta

- **Akku- & Energieoptimierung (P3):**
  - **Hintergrund-Dormancy für Sensoren:** Umstellung der Sensoregistrierung in `rememberOrientation` auf `LifecycleStartEffect` (beim Ausschalten des Bildschirms oder Wechsel in den Hintergrund werden Orientierungssensoren sofort deregistriert, 0 % Standby-Last).
  - **Energiemessmatrix & Laufzeitdokumentation:** Detaillierte Messungen in `play-store/battery-profiling.md` für OLED-Dunkelmodus (4,8 %/h, > 20 h Laufzeit), Rotlichtmodus (4,2 %/h, > 23 h Laufzeit) und AR-Kamera (14,5 %/h, ~6,8 h).
  - **Automatisierte Dormancy-Tests:** Lifecycle-Vertragstests in `SkyStartupTest.kt` verifiziert. Testhinweise: [1.1.9-pre.10](play-store/pre-release-1.1.9-pre.10.md).

## Neu in 1.1.9-pre.9 · Beta

- **Robustheit & Darstellung (P2.12):**
  - **Fehler- und Ladezustände für DSS2-Himmelsaufnahmen:** Ladeindikator beim Abruf sowie verständliche Fehlermeldung mit „Erneut versuchen“-Button bei Timeouts oder Serverfehlern.
  - **Geländeprofil-Retry:** Direkter Wiederholungs-Button bei fehlgeschlagener oder offline gebliebener GLO-90-Geländeprofilierung.
  - **Automatisierte Regressionstests:** 16 neue Tests in `SkyMapInteractionTest.kt` für Pinch-to-Zoom (25° bis 150°), Panning-Präzision, Orientierungstransformation unter Optik-Modi (Newton, Zenitspiegel), AR-/Kartenwechsel mit Sensorübernahme und kollisionsfreie Beschriftungsplatzierung dichter Sternhaufen.
  - **Dokumentierte Geräte-Testfälle:** Vollständige Testmatrix in `play-store/device-test-cases.md` für Berechtigungsentzug, App-Unterbrechungen, Sensor-Fallbacks, Offline-Betrieb und Rendering-Profilierung (60/120 FPS Benchmark). Testhinweise: [1.1.9-pre.9](play-store/pre-release-1.1.9-pre.9.md).

## Neu in 1.1.9-pre.8 · Beta

- **AR-Zielhilfe (P2.11):**
  - **Richtungspfeile & Winkelabstand:** Für jedes ausgewählte Himmelsobjekt wird der Winkelabstand zur aktuellen Blickrichtung berechnet und als Gradangabe angezeigt.
  - **Zielkreuz-Markierung:** Pulsierendes Fadenkreuz wenn sich das Ziel im Sichtfeld befindet.
  - **Kantenzeiger:** Kreisförmige Pfeile am Bildschirmrand mit Richtungsangabe und Gradabstand für Off-Screen-Objekte.
  - **Status-Banner:** Oberer Informationsbalken mit Zielname, Objekttyp, Entfernung und Schließen-Button.
  - **Hinter-dem-Gerät-Erkennung:** Ziele > 90° von der Blickrichtung werden als „Umdrehen" mit spezieller Farbgebung markiert.
  - **Horizont- und Geländeerkennung:** Objekte unter dem lokalen Horizont oder Geländeprofil werden als orange Warnung angezeigt.
  - **Sensorqualität:** Anzeige der Sensorgenauigkeit (Hoch/Mittel/Niedrig) mit Kalibrierungshinweis bei unzuverlässiger Ausrichtung. Testhinweise: [1.1.9-pre.8](play-store/pre-release-1.1.9-pre.8.md).

## Neu in 1.1.9-pre.7 · Beta

- **Fernglas- und Teleskop-Sichtfeld & Orientierung (P2.10):**
  - **Sichtfeld-Kreis (FOV Overlay):** Frei skalierbarer Kreis (0,1° bis 30,0°) mit Winkelgrößenbeschriftung und Zentrierfadenkreuz.
  - **Telrad-Sucher:** Spezieller 3-Ring-Telrad-Modus mit 0,5°, 2,0° und 4,0° Ringen und klassischem Teilstrichkreuz.
  - **Geräteprofile:** Vorkonfigurierte Profile (10×50 Fernglas, 8×42 Fernglas, 8" Dobson mit 25 mm und 10 mm Plössl) sowie eigene Profile mit Brennweite, Öffnung, Okularbrennweite und scheinbarem Gesichtsfeld. Automatische Kennzeichnung von Vergrößerung ($V = F / f$), wahrem Gesichtsfeld ($TFOV \approx AFOV / V$) und Austrittspupille als Näherungswerte.
  - **Drehung & Spiegelung:** Bildorientierung um 0°, 90°, 180° (Newton-Spiegeltelekop) oder 270° drehen sowie horizontal spiegeln (Zenitspiegel). Stern- und Objektnamen bleiben standardmäßig aufrecht lesbar (oder können optional mitrotieren).
  - **Exakte Touch-Präzision & Wischgesten:** Inverse Koordinatentransformation sorgt dafür, dass getippte Sterne auch unter Spiegelung/Drehung absolut millimetergenau getroffen werden; Wischgesten bewegen die Karte stets natürlich unter dem Finger.
  - **Status-Badge:** Schneller Überblick über aktive Modifikationen mit 1-Klick-Zurücksetzen („Standard“). AR bleibt strikt unbeeinflusst. Testhinweise: [1.1.9-pre.7](play-store/pre-release-1.1.9-pre.7.md).

## Neu in 1.1.9-pre.6 · Beta

- **Beobachtungstagebuch (P2.9):**
  - **Beobachtungen protokollieren:** Eigene Beobachtungen erfassen mit Datum, Notizen, Seeing-Rating (1 bis 5 Sterne) und Ausrüstung (Teleskop/Fernglas/Okulare).
  - **Direkt aus der Sternkarte:** In den Objektdetails kann direkt per Klick auf „Beobachten“ ein neuer Eintrag für das ausgewählte Objekt angelegt werden.
  - **Optionale eigene Fotos:** Bilder werden in das isolierte App-Verzeichnis kopiert und können im Tagebuch als Thumbnail oder im Vollbild betrachtet werden.
  - **100 % lokaler Datenschutz:** Standortdaten sind optional und standardmäßig deaktiviert (Opt-in). Keine Cloud-Synchronisierung, Ausschluss von automatischen Backups.
  - **Löschen & Dateibereinigung:** Einzelne Einträge bearbeiten und löschen (inkl. Foto-Bereinigung) sowie vollständiges Löschen mit Sicherheitsabfrage.
  - **JSON-Export & -Import:** Mit Datenbegrenzung (10 MB / 5.000 Einträge), Vorschau, Zusammenführen/Ersetzen und EXIF-Datenschutzhinweis. Testhinweise: [1.1.9-pre.6](play-store/pre-release-1.1.9-pre.6.md).

## Neu in 1.1.9-pre.4 · Beta

Im Reiter **Plan** („Beobachtungsplaner“) gibt es zwei neue, lokal berechnete Bereiche:
- **Beobachtungsfenster heute Nacht („Heute Nacht im Überblick“):**
  - Zeigt das exakte lokale Fenster der astronomischen Dunkelheit bzw. nautischen Dämmerung, Sonnenunter- und -aufgangszeiten.
  - Mondstatus mit Beleuchtungsgrad in Prozent, Mondphase und Auf-/Untergangszeiten.
  - Bedingungen: Bei aktiver Internetverbindung wird die stündliche Bewölkungsvorhersage ausgewertet („Optimale Bedingungen ab 22:00 Uhr bei klarem Himmel“); bei Offline-Betrieb wird transparent die rein astronomische Dunkelheit ausgewiesen.
- **Empfehlungsengine („Was lohnt sich heute Nacht?“):**
  - Berechnet für ausgewählte Himmelshighlights (Planeten, Doppelsterne, Nebel, Galaxien, Sternhaufen) den Verlauf über die Nacht.
  - Schließt Objekte unter 16° Horizonthöhe strikt aus.
  - Berücksichtigt den sphärischen Winkelabstand zum Mond, um blendfreie Beobachtungen zu gewährleisten.
  - Filterchips für **Alle**, **Bloßes Auge**, **Fernglas** und **Teleskop**.
  - Schnellaktionen direkt in der Karte: **In Karte öffnen** (zentriert das Objekt sofort im Sternkarten-Tab) und **Merken** (Favoriten-Stern).
  - 100 % Privacy First: Berechnungen laufen vollständig lokal auf dem Gerät. Testhinweise: [1.1.9-pre.4](play-store/pre-release-1.1.9-pre.4.md).

## Dauerhafter Standort seit 1.1.9-pre.3 · Beta

Nach einmaliger Standortfreigabe merkt sich Astra den Beobachtungsort lokal auf dem Gerät (`LocationStore` in den privaten Einstellungen `astra_settings`). Bei jedem weiteren Start lädt die Sternkarte direkt diesen Ort und zeigt sofort deinen passenden Nachthimmel – ohne dass der Freigabeknopf erneut gedrückt werden muss.

Volle Kontrolle und Opt-Out:
- **Sternkarte:** Bei hinterlegtem Standort wird der große Standortbutton ausgeblendet und stattdessen in der Kartenbedienung die Aktion **Auf Demo zurücksetzen** angeboten. Ein Klick löscht die gespeicherten Koordinaten sofort und kehrt zur Berlin-Demo zurück.
- **Info / Deine Daten:** Ein neuer Schalter **Standort für die Sternkarte merken** erlaubt das dauerhafte Deaktivieren; beim Ausschalten werden gespeicherte Koordinaten rückstandsfrei entfernt.
- **100 % Datenschutz:** Kein Cloud-Backup (`allowBackup="false"`, `data_extraction_rules.xml`), Koordinaten verlassen das Gerät niemals. Testhinweise: [1.1.9-pre.3](play-store/pre-release-1.1.9-pre.3.md).

## Basiskartenstart seit 1.1.9-pre.2 · Beta

Die Sternkarte priorisiert das Zeichnen des ersten Basiskarten-Rahmens: Horizont, Planeten, Navigationssterne und Nachthimmelhintergrund erscheinen ohne Wartezeit auf den schwereren GPU- und Milchstraßentextur-Ladevorgang. Erst nach dem ersten sichtbaren Rahmen wird der OpenGL ES-Shader gestartet und die 3840-Pixel-Gaia-Milchstraße im Hintergrund dekodiert.

Schlägt die GPU-Initialisierung oder das Texturdekodieren auf leistungsschwachen Geräten fehl, bleibt die Basiskarte mit dunklem Nachthimmelhintergrund vollständig nutzbar. Unter **Ebenen & Namen** steht die Aktion **Textur erneut laden** zur Verfügung. Bei geringem Gerätespeicher oder knappem freiem Heap skaliert der Textur-Loader die Bildgröße automatisch herunter, um GC-Pausen und Speicherüberläufe zu verhindern; bei Speicherdruck des Systems (`onTrimMemory`) werden flüchtige Gelände- und Lichtcaches geleert. Der gemeinsame Katalogcache aus 1.1.9-pre.1 bleibt aktiv. Testhinweise: [1.1.9-pre.2](play-store/pre-release-1.1.9-pre.2.md).

## Wetteraktualisierung seit 1.1.8-pre.1

Beim Herunterziehen bleiben Wetterwerte, Astra-Score, Stundenübersicht und Karten sichtbar, bis Ersatzdaten vorliegen. Eine kleine Statusanzeige kennzeichnet den Abruf. Bei einem Fehler bleiben die bisherigen Werte mit Datenstand und Wiederholen-Aktion erhalten.

Radar und Bewölkung aktualisieren sich unabhängig, ohne die Kartenansicht neu aufzubauen. Zoom und Ebenenschalter bleiben erhalten; Ersatz-Radarkacheln werden vor dem Austausch geladen. Beide Ebenen zeigen ihren eigenen Datenstand und Fehlerstatus. Ein Standortwechsel mischt alte Wetterdaten nicht mit dem neuen Ort; noch angezeigte Daten des bisherigen Orts sind gekennzeichnet.

Die Stundenübersicht verwendet feste Zeitpunkte, auch über Mitternacht und Zeitumstellungen hinweg. Im Hintergrund werden Abrufe abgebrochen und veraltete Rückmeldungen verworfen. Wetter- und Standortdaten bleiben nur während der geöffneten Ansicht im Arbeitsspeicher; es gibt keinen neuen dauerhaften Wettercache. Testhinweise: [1.1.8-pre.1](play-store/pre-release-1.1.8-pre.1.md).

## Hinweise auf alte Lichtdaten seit 1.1.7-pre.2

Ein sichtbarer Hinweis direkt am Astra-Score und oberhalb der internen Lichtkarte kennzeichnet die NASA-Lichtdaten von **2016 als veraltet**. Der Hinweis bleibt bei eingeklappter Score-Erklärung und im Karten-Vollbild sichtbar. Neuladen liefert keine neueren Beobachtungen; heutige Beleuchtung kann abweichen. Die Score-Berechnung bleibt unverändert, die Werte der externen Karte werden weiterhin nicht übernommen. Testhinweise: [1.1.7-pre.2](play-store/pre-release-1.1.7-pre.2.md).

## Lichtkarten seit 1.1.7-pre.1

Im Kalender lässt sich zusätzlich die offizielle deutsche Einbettung von [LightPollutionMap.app](https://lightpollutionmap.app/de/) öffnen. Vor dem Laden steht ein eigener Hinweis zu den externen Anbietern und dem gerundeten Startort. Die externe Ansicht benötigt die bewusste Aktion **Externe Karte laden**; sie erhält keinen GPS-, Kamera- oder Dateizugriff. Ihre Modellwerte bleiben innerhalb der Anbieterkarte und werden nicht in den Astra-Score übernommen. Zusätzliche Website-Funktionen wie Wetter, Adresssuche und Foto-Upload sind nicht freigeschaltet.

Die Lichtverschmutzungskarte im Kalender bietet einen Kilometermaßstab, einen Umkreis von 10, 25 oder 50 km und getrennte Schalter für Nachtlicht, Raster-Klassen und Ortsvergleich. Ein angetippter Vergleichsort wird dem gerundeten Beobachtungsort gegenübergestellt. Mit **Karte vergrößern** erhält sie mehr Platz; Zoom und Vergleich bleiben beim Öffnen und Schließen erhalten.

Die Legende zeigt den Bildlichtindex, Quellen und das Alter der NASA-Daten von **2016**. Ein neuer Kachelabruf ist keine neue Satellitenbeobachtung. Die Bortle- und Helligkeitsangaben sind unvalidierte Näherungen aus dem dargestellten Kartenbild, keine kalibrierten Strahldichten, SQM-Messungen oder Zusagen eines dunklen Himmels. Eine wissenschaftlich kalibrierte Himmelshelligkeitskarte bleibt ein eigener weiterer Schritt.

Fehlende oder transparente Pixel werden als fehlende Daten behandelt. Bei Ladefehlern bleibt bereits geladenes Kartenmaterial sichtbar; **Neu laden** versucht den Abruf erneut. Den privaten Grundkartencache kannst du direkt an der Karte löschen. Online-Freigabe, Standort-Rundung und Rotlichtmodus gelten auch für die vergrößerte Ansicht; Vergleichsorte werden nicht gespeichert.

## Sternkartenbedienung seit 1.1.6

Die Sternkarte hat eine eigene Kartenfläche und eine einklappbare Bedienung. Die Vollbildtaste blendet Navigation und Kopfbereich aus; dieselbe Taste oder Androids Zurück-Geste beendet den Modus. **Ansicht zurücksetzen** stellt Süden, 35° Höhe und ein horizontales Sichtfeld von 95° wieder her und beendet das Nachführen.

Das Bedienfeld enthält Suche, Zeitsteuerung, Ebenen, Deep Sky, AR, Ausrichten, Zoom und Rotlicht. Richtung, Sichtfeld sowie Live- oder Simulationszeit bleiben sichtbar. Bedienung und Objektinformationen liegen außerhalb der gezeichneten Himmelsfläche und bleiben bei großer Schrift scrollbar.

Unter **Ebenen & Namen** stehen die Beschriftungsdichten „Wenige“, „Normal“ und „Viele“ zur Verfügung. Ein Layout mit Prioritäten, Randkürzung und Kollisionsprüfung hält ausgewählte Ziele und Orientierungspunkte lesbar. Die Einstellung wird nur lokal gespeichert. Das AR-Sichtfeld folgt dem tatsächlichen CameraX-Ausschnitt, wenn sich die Kartenfläche durch Vollbild oder Bedienung verändert.

Die Zeitsteuerung aus 1.1.5 bleibt verfügbar. Sie bezieht Sternkarte, Planeten, Milchstraße, Sternbildgrenzen und Objektinformationen auf denselben Zeitpunkt; AR bleibt live. Wetter und Wetterkarte sind aktuelle Daten und werden bei Simulationen entsprechend gekennzeichnet.

## Sternkarte bedienen

1. In der normalen Karte mit einem Finger wischen und mit zwei Fingern zoomen. Eine manuelle Bewegung beendet das Nachführen.
2. Das Reglersymbol öffnet die Kartenbedienung. Dort lassen sich Ebenen, Beschriftungen, Deep Sky, Zeit und Ausrichtung einstellen.
3. Mit **Vollbild** erhält die Himmelsfläche mehr Platz. Androids Zurück-Geste oder die Vollbildtaste führt sicher zurück.
4. Ein Objekt antippen, suchen oder aus dem Plan öffnen, um Infos, Zentrieren, Favorit und Nachführen zu verwenden.
5. **AR** nutzt Kamera und Sensoren mit Live-Zeit. Die manuelle Karte bleibt davon getrennt und lässt sich jederzeit wieder öffnen.

Ohne Standortfreigabe zeigt Astra Berlin als deutlich markierten Demo-Standort. Für astronomische Berechnungen wird der Standort lokal verarbeitet.

## Lokal bauen und testen

Voraussetzungen:

- Android Studio mit Android SDK 37
- JDK 17, vorzugsweise die von Android Studio mitgelieferte Runtime
- Android-Gerät oder Emulator ab API 28; Kamera, Kompass und GPS sind optionale Hardware

Debug-APK bauen und auf einem angeschlossenen Gerät installieren:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Die wichtigsten Prüfungen:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lint
.\gradlew.bat :app:assembleDebugAndroidTest
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb install -r app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
adb shell am instrument -w de.projektastra.app.test/androidx.test.runner.AndroidJUnitRunner
```

Die Oberfläche läuft im Emulator. Für echte AR-Ausrichtung, Kameraausschnitt, Kompass und GPS sind Tests auf realen Geräten erforderlich. Die aktuellen Gerätetests stehen in [play-store/pre-release-1.1.9-pre.1.md](play-store/pre-release-1.1.9-pre.1.md).

Der dokumentierte Prüfstand steht in den [Pre-Release-Testhinweisen](play-store/pre-release-1.1.9-pre.1.md). WebView-Tests verwenden lokale Testkacheln für reproduzierbare Lade-, Fehler- und Vergleichsfälle; die Prüfung echter Anbieter und weiterer Geräte ergänzt diese Tests.

## Release und Pre-Releases

Jede größere funktionale, technische oder sicherheitsrelevante Änderung wird zuerst als eigener GitHub-Pre-Release mit installierbarer APK, Bundle und Prüfsumme veröffentlicht. Erst nach Tests auf mehreren Geräten, dokumentiertem Feedback und behobenen Blockern folgt ein stabiler Release. Kleine Dokumentations- und rein interne Teständerungen dürfen gesammelt werden. Der vollständige Ablauf steht in [RELEASE_PROCESS.md](RELEASE_PROCESS.md).

Für einen technischen Release-Build:

```powershell
.\gradlew.bat :app:bundleRelease
```

Für eine Veröffentlichung im Play Store müssen zusätzlich ein außerhalb des Repositories gesicherter Upload-Key, eine signierte Bundle-Prüfung, die Datenschutzfreigabe und die [Release-Checkliste](play-store/release-checklist.md) erledigt sein. `bundleRelease` allein ist keine Store-Freigabe.

## Roadmap

Die vollständige, priorisierte Liste steht in [AUFGABEN.md](AUFGABEN.md). Die ursprünglichen Kernstufen P1 (Suche, Zeitsteuerung, Vollbild, visuelle Überarbeitung, Lichtkarte, Wetteraktualisierung, Startzeit), P2 (Beobachtungsplan, Tagebuch, Sichtfeld/Telrad, AR-Zielhilfe, Robustheit) und P3 (Akkutests) sind im aktuellen Beta-Zweig weitgehend umgesetzt.

Als nächste Ausbaustufen im Backlog sind geplant:
- **P2.13 Dynamische Sonnensystem-Körper & Monddetails:** Galileische Jupitermonde, Saturn-Ringe und Terminator-Mondkarte.
- **P2.14 Aufsuchhilfen & Himmelsvermessung:** Interaktives Winkelabstand-Messwerkzeug, Koordinatengitter und Star-Hopping-Assistent.
- **P2.15 Beobachtungspraxis & erweiterte Planung:** Messier-/Caldwell-Challenges mit Fortschrittsbalken, Taupunkt-Warnung und OAL-Export.
- **P2.16 Kamera- & AR-Nachtoptimierung:** Camera2-Nachtbelichtungsanpassung und Sensor-Dämpfung.
- **P2.17 Winter- & Feld-Usability:** Physische Tastenbedienung (Lautstärketasten für Zoom) und akkuschonendes Homescreen-Widget.

## Daten und Datenschutz

- Der Start, die Sternkarte, Ephemeriden, Suchindex, Favoriten und Beobachtungslisten funktionieren offline.
- GPS wird für Himmels- und Ereignisberechnungen lokal verwendet. Erst nach gesonderter Online-Freigabe erhält der Wetter-/Kartenanbieter einen auf 0,01° gerundeten Ort; das genaue Geländeprofil hat eine weitere Freigabe.
- Kamera- und Bewegungssensoren werden lokal verarbeitet. Es werden keine Kameraaufnahmen gespeichert oder hochgeladen.
- Es gibt kein Konto, keine automatische Cloud-Synchronisierung, keine Suchhistorie und keine automatische Geräte- oder Android-App-Sicherung.
- WebViews deaktivieren Cookies, DOM-Speicher und Browser-Cache. OSM-Kacheln der Astra-Karte liegen in einem privaten, löschbaren Cache. Die freiwillig eingebettete Anbieterkarte hat eigene Empfänger; ihr Hinweis steht vor dem Laden und in der Datenschutzerklärung.
- Die externen Dienste und Lösch-/Aufbewahrungshinweise stehen in der [Datenschutzerklärung](play-store/privacy-policy.html) und in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

Die Anwendung besitzt keinen Nachweis einer ISO/IEC-27001-Zertifizierung. Diese Norm umfasst neben technischen Kontrollen auch ein organisatorisches Informationssicherheitsmanagement. Prüfstatus und offene Maßnahmen stehen in [SECURITY_REVIEW.md](SECURITY_REVIEW.md) und [SECURITY_REMEDIATION.md](SECURITY_REMEDIATION.md).

## Quellen und Lizenzen

Kataloge und Darstellungen stammen unter anderem aus HYG v4.1, OpenNGC, NASA/Gaia, NASA VIIRS, DSS2, Open-Meteo, RainViewer, OpenStreetMap und Astronomy Engine. Die einzelnen Nutzungsbedingungen, Quellenangaben und mitgelieferten Lizenztexte stehen in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md), [LICENSES](LICENSES) und [scripts/MILKY_WAY_ASSET.md](scripts/MILKY_WAY_ASSET.md).

Rückmeldungen zum Pre-Release bitte mit Gerätemodell, Android-Version, aktivem Modus (Karte/AR/Rotlicht) und reproduzierbaren Schritten melden. Das Projekt ist zunächst für den persönlichen Gebrauch gedacht.
