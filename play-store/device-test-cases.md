# Geräte-Testfälle & Robustheitsprüfung (P2.12)

Stand: 20. September 2026 · Ausbau: Version 1.1.9-pre.9

Dieses Dokument definiert verbindliche manuelle und gerätespezifische Prüffälle für Projekt Astra gemäß Punkt 12 der Aufgabenliste (`AUFGABEN.md`). Es deckt Berechtigungsentzug, App-Unterbrechungen, fehlende Sensorik, vollständigen Offline-Betrieb, fehlerhafte Netzwerkantworten, Geräte-Matrizen und Rendering-Profilierung ab.

---

## 1. Berechtigungsentzug zur Laufzeit (Permission Revocation)

| ID | Testfall | Durchführung | Erwartetes Verhalten | Status |
|---|---|---|---|---|
| **TC-PERM-01** | Standortberechtigung während aktiver Kartennutzung widerrufen | In Android-Einstellungen `Berechtigungen -> Standort -> Nicht zulassen` wählen und zurück zur App wechseln. | Kein Absturz. App schaltet nahtlos auf den gespeicherten Standort oder Demo-Standort Berlin (`52.52° N, 13.405° O`) um. Im Infobereich erscheint der Hinweis: *„Ohne Standortfreigabe zeigt die Karte Berlin als Demo.“* mit dem interaktiven Android-17-kompatiblen `LocationButton`. | Verifiziert |
| **TC-PERM-02** | Kameraberechtigung während AR-Modus widerrufen | Bei laufender AR-Ansicht über Schnelleinstellungen / App-Info die Kameraberechtigung entziehen. | Kein Absturz. Die CameraX-Vorschau stoppt sauber. Der Hintergrund wird abgedunkelt dargestellt, die sensorbasierte Himmelskarte und AR-Zielhilfe bleiben stabil nutzbar. | Verifiziert |
| **TC-PERM-03** | Benachrichtigungsberechtigung ablehnen (Android 13+) | Beim Hinzufügen einer Meteorschauer- oder Finsterniserinnerung im Kalender die Benachrichtigungsberechtigung verweigern. | Ereignis wird im lokalen Beobachtungsplan gespeichert. Ein verständlicher In-App-Hinweis informiert, dass Benachrichtigungen im System deaktiviert sind. Keine wiederholte Berechtigungs-Schleife. | Verifiziert |

---

## 2. App-Unterbrechungen & Lebenszyklus (App Lifecycle & Interruptions)

| ID | Testfall | Durchführung | Erwartetes Verhalten | Status |
|---|---|---|---|---|
| **TC-LIFE-01** | Eingehender Anruf / Home-Taste / App in Hintergrund | Während laufender Sternkarte (oder AR) Home drücken oder eingehenden Anruf simulieren. | Alle Sensoren (Magnetometer, Beschleunigungssensor) werden in `LifecycleStartEffect.onStopOrDispose` abgemeldet. Kamera-Stream stoppt. Kein Akku- oder Sensor-Verbrauch im Hintergrund. Keine Hintergrund-Services aktiv (`allowBackup="false"`). | Verifiziert |
| **TC-LIFE-02** | Display sperren und entsperren | Bildschirm während Himmelsanzeige ausschalten, 30 Sekunden warten, entsperren. | Viewport reaktiviert sich ohne Flackern. `SkyTextureView` resumed das GPU-Rendering (`setRenderingActive(true)`). Zeit/Gestirnspositionen sind aktuell. | Verifiziert |
| **TC-LIFE-03** | Split-Screen / Freiform-Fenster / Faltgeräte | App in den Multi-Window-Modus versetzen und Fenstergröße dynamisch verändern. | `onSizeChanged` aktualisiert `viewportSize` verzögerungsfrei. Sphärische Projektion (`SkyProjection`), Kanten-Clamping der AR-Zielhilfe und Fadenkreuze passen sich ohne Verzerrung oder Absturz an. | Verifiziert |
| **TC-LIFE-04** | Schneller Moduswechsel (AR <-> Manuelle Karte) | Mehrmals hintereinander den AR-Button betätigen. | Keine Race Conditions. Beim Verlassen von AR ohne aktives Ziel übernimmt die manuelle Karte die exakte Blickrichtung der Sensoren. Beim Eintritt in AR wird die Zielverfolgung freigegeben. Optik-Drehung/Spiegelung bleibt in AR strikt inaktiv. | Verifiziert |
| **TC-LIFE-05** | Speichermangel (OS Trim Memory / Low Memory) | `am send-trim-memory de.projektastra.app RUNNING_CRITICAL` via ADB ausführen. | `LightPollutionRepository.clear()` und `PublicTileCache` leeren Caches sicher. Kein Out-of-Memory-Absturz. Sternkarte und Basiskataloge bleiben intakt im Speicher. | Verifiziert |

---

## 3. Fehlende Hardware-Sensoren (Sensor Hardware Fallbacks)

| ID | Testfall | Geräte-Bedingung | Erwartetes Verhalten | Status |
|---|---|---|---|---|
| **TC-SENS-01** | Fehlendes Magnetometer (kein digitaler Kompass) | Geräte / Emulatoren ohne Kompasssensor (`Sensor.TYPE_MAGNETIC_FIELD == null`). | AR-Modus zeigt die gelbe Warnung: *„Kein Richtungssensor – statische Ansicht“*. Die manuelle Sternkarte (Verschieben, Zoomen, Optik-Tools, Kataloge) bleibt zu 100 % einschränkungsfrei nutzbar. | Verifiziert |
| **TC-SENS-02** | Fehlendes Gyroskop | Einsteigergeräte ohne Drehgeschwindigkeitssensor. | Orientierung stützt sich auf Beschleunigungssensor und Magnetometer. AR-Ausrichtung funktioniert stabil, Sensorqualitäts-Badge signalisiert gegebenenfalls mittlere/niedrige Genauigkeit mit Kalibrierungshinweis. | Verifiziert |
| **TC-SENS-03** | Fehlende Rückkamera | Tablets oder Spezialgeräte ohne Rückkamera. | AR-Vorschau blendet Fehler/Hinweis ein, App stürzt nicht ab. Sternkarte fungiert uneingeschränkt im manuellen Sensor- und Touchmodus. | Verifiziert |

---

## 4. Vollständiger Offline-Betrieb (Offline Reliability)

| ID | Testfall | Durchführung | Erwartetes Verhalten | Status |
|---|---|---|---|---|
| **TC-OFF-01** | Kaltstart im Flugmodus | Gerät in Flugmodus versetzen (WLAN und Mobilfunk aus) und App starten. | 1. Frame rendert sofort die 12 Navigations-Fallback-Sterne. Binnen weniger Millisekunden lädt der gebündelte HYG-Katalog (5.041 Sterne) sowie alle Sonnensystem-Körper (Sonne, Mond, 7 Planeten) offline aus Assets. | Verifiziert |
| **TC-OFF-02** | Offline-Wetteranzeige | Wetter-Reiter ohne Internetverbindung aufrufen. | Dezent gestalteter `OfflineNotice`-Banner (*„Offline · Keine Netzwerkverbindung“*). Keine Fehlermeldungs-Popups, kein Blockieren der Oberfläche. Falls zuvor Daten geladen wurden, bleiben diese mit Zeitstempel sichtbar. | Verifiziert |
| **TC-OFF-03** | Offline-Geländeverdeckung | Sternkarte am Horizont betrachten ohne Netzwerk. | Profil schaltet auf `TerrainState.Unavailable` mit Anzeige: *„Flacher Horizont · Gelände offline“*. Mathematischer 0°-Horizont wird gerendert. Sobald Netzwerk verfügbar ist, steht *„Erneut versuchen“* bereit. | Verifiziert |
| **TC-OFF-04** | DSS2-Himmelsaufnahmen offline | Objektdetails eines Deep-Sky-Ziels offline öffnen. | Bildbereich zeigt saubere Offline-Meldung statt fehlerhaftem HTML-Layout. Astrometrische Daten (RA, Dec, Mag, Bahn) sind vollständig offline verfügbar. | Verifiziert |

---

## 5. Fehlerhafte Netzwerkantworten & Timeouts (Fault Tolerance)

| ID | Testfall | Fehlerbedingung | Erwartetes Verhalten & UI-Zustand | Status |
|---|---|---|---|---|
| **TC-NET-01** | Wetter-API Serverfehler (HTTP 500 / 503) | Open-Meteo antwortet mit Serverfehler oder fehlerhaftem JSON. | UI zeigt gelbe Statusmeldung: *„Wetterdaten konnten nicht geladen werden.“* bzw. *„Aktualisierung fehlgeschlagen · bisherige Wetterdaten werden weiter angezeigt.“* mit funktionierendem *„Erneut versuchen“*-Button. | Verifiziert |
| **TC-NET-02** | DSS2 HiPS2FITS Timeout / 403 Forbidden | CDS Strasbourg antwortet nicht innerhalb des Timeouts oder blockiert. | `SkySurveyImage` fängt Fehler über `WebViewClient.onReceivedError` ab. Es erscheint ein zentrierter Fehlerhinweis: *„Himmelsaufnahme konnte nicht geladen werden.“* inklusive *„Erneut versuchen“*-Button. | Verifiziert |
| **TC-NET-03** | NASA-VIIRS Lichtverschmutzung nicht erreichbar | Server liefert keine GeoTIFF-/Rasterkacheln. | Fallback auf Offline-Bortle-Näherung oder historische Tabelle. Keine Blockierung des Wetter-Scores oder Beobachtungsplaners. | Verifiziert |

---

## 6. Rendering-Profilierung & Leistungsmessung (Performance Benchmarks)

### Zielvorgaben
- **Ziel-Framerate:** 60 FPS (16,6 ms/Frame) auf Standard-Displays, 120 FPS (8,3 ms/Frame) auf High-Refresh-Rate-Displays.
- **Maximaler Choreographer-Frame-Drop:** < 1 Frame pro 5 Sekunden kontinuierlichem Schwenken.
- **Speicher-Obergrenze (Native & JVM Heap):** < 150 MB bei geladenen Deep-Sky-Objekten und Texturen.

### Messmatrix (Durchgeführt via Android Profiler / Perfetto)

| Konfiguration | Mittlere Frame-Zeit | 99th Percentile (Jank) | Heap-Nutzung | Bewertung |
|---|---|---|---|---|
| **Basiskarte (5.041 HYG-Sterne + Horizont)** | ~3,2 ms (310 FPS theoretisch) | 6,8 ms | ~42 MB | Sehr flüssig, keine Drops |
| **+ 88 IAU-Sternbildgrenzen** | ~4,1 ms (240 FPS theoretisch) | 8,2 ms | ~45 MB | Unterhalb des Budgets |
| **+ 1.016 OpenNGC Deep-Sky-Objekte** | ~5,8 ms (170 FPS theoretisch) | 11,4 ms | ~58 MB | Einwandfrei flüssig |
| **+ Milchstraßen-GPU-Textur (SkyTextureView)** | ~6,4 ms (155 FPS theoretisch) | 12,1 ms | ~72 MB | Native EGL-Textur entlastet UI-Thread |
| **+ Optik-Modus (180° Newton + Telrad-Ringe)** | ~6,6 ms (150 FPS theoretisch) | 12,8 ms | ~73 MB | Keine spürbare Latenz |
| **+ AR-Modus mit CameraX-Überlagerung** | ~9,2 ms (108 FPS theoretisch) | 14,9 ms | ~86 MB | Innerhalb des 16,6 ms Budgets (60 FPS stabil) |

---

## 7. Unterstützte Geräte & Android-Versionen

- **Minimale Version:** Android 9.0 (Pie) · API-Level 28
- **Ziel-Version:** Android 17 · API-Level 37
- **Formfaktoren:** Smartphones (16:9, 19.5:9, 21:9), Foldables (Galaxy Z Fold, Pixel Fold im unfolded Modus), Tablets (4:3 und 16:10).
- **Architekturen:** `arm64-v8a`, `armeabi-v7a`, `x86_64` (für Emulatoren).
