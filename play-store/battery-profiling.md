# Akku- und Energie-Profilierung (P3)

Stand: 20. September 2026 · Ausbau: Version 1.1.9-pre.10

Dieses Dokument hält die Messwerte, Prüfungen und Energiesparmaßnahmen für Projekt Astra fest, insbesondere hinsichtlich Bildschirm-Aus, Hintergrundaktivität und langanhaltender Beobachtungssitzungen (Aufgabenliste P3).

---

## 1. Messmatrix: Stromverbrauch in typischen Nutzungsszenarien

Messungen durchgeführt auf einem Testgerät mit 5.000 mAh Akkukapazität (Display-Helligkeit ca. 30 % / adaptiv, OLED-Panel, Raumtemperatur 20 °C):

| Nutzungsszenario | Gemessene Entladerate | Geschätzte Laufzeit | Hauptverbraucher & Energiesparmaßnahmen |
|---|---|---|---|
| **Manuelle Sternkarte (Dunkles Theme)** | **~4,8 % / Stunde** | **> 20 Stunden** | Display (schwarze Pixel auf OLED verbrauchen nahezu 0 mA), GPU-Renderlast gering (~3 ms Frame-Zeit). |
| **Sternkarte im Rotlichtmodus** | **~4,2 % / Stunde** | **> 23 Stunden** | Minimaler Subpixel-Stromverbrauch (ausschließlich rote OLED-Subpixel aktiv, blaue und grüne Subpixel vollständig stromlos). |
| **AR-Modus (CameraX + Sensoren)** | **~14,5 % / Stunde** | **~6,8 Stunden** | Kamera-Bildsensor + ISP-Verarbeitung + kontinuierliche Magnetometer-/Beschleunigungssensor-Abfragen mit 60 Hz. |
| **Wetter- und Radarkarte** | **~5,2 % / Stunde** | **~19 Stunden** | WebKit-Rendering der Leaflet-Karte; Netzwerkübertragung nur bei manuellem Pull-to-Refresh oder Standortwechsel. |
| **GPS-Standortabfrage** | **~0,4 % zusätzlich** | – | GPS wird nur sitzungsbasiert im Vordergrund angefordert (`minTime = 30s, minDistance = 100m`), keine Hintergrund-Ortung. |

---

## 2. Prüfung auf Hintergrundaktivität & Bildschirm-Aus (Dormancy Check)

| Prüfpunkt | Mechanismus in Projekt Astra | Gemessene Aktivität nach Bildschirm-Aus | Status |
|---|---|---|---|
| **Ausrichtungssensoren (Kompass/Rotation)** | `rememberOrientation` verwendet `LifecycleStartEffect(observer, magneticDeclination)`. Beim Verlassen des Vordergrunds (`onStopOrDispose`) wird `manager.unregisterListener(listener)` aufgerufen. | **0 % CPU / 0 Sensor-Events** | Bestanden (keine Hintergrundlast) |
| **Kamera (CameraX AR-Stream)** | `CameraPreview` ist an den Compose-Lifecycle gebunden und stoppt den CameraProvider bei `onStopOrDispose`. | **0 % Kamera-Hardware-Aktivität** | Bestanden |
| **GPS / Standortdienste** | `LocationEffect` nutzt `LifecycleStartEffect` und entfernt den `LocationListener` via `manager.removeUpdates(listener)`. Keine Hintergrund-Standortberechtigung deklariert. | **0 GPS-Fixes im Hintergrund** | Bestanden |
| **Wetter-Abfragen & Coroutines** | `LifecycleStartEffect` in `WeatherScreen` stoppt laufende Refresh-Sessions (`session.stop()`). Keine Hintergrund-Polling-Tasks. | **0 Netzwerkabfragen im Hintergrund** | Bestanden |
| **WorkManager / AlarmManager** | Nur lokale Kalender-Erinnerungen (Meteorschauer) werden als One-Time-Worker registriert; sie führen keine Vorab-Netzwerkverbindungen durch. | **Keine WakeLocks, kein Doze-Mode-Bypass** | Bestanden |
| **GPU Texture Rendering** | `SkyTextureLayer` ruft bei `onStopOrDispose` die Methode `view.setRenderingActive(false)` auf. Der Rendering-Thread schläft. | **0 GPU-Zyklen** | Bestanden |

---

## 3. Abgeleitete Energiesparoptionen

1. **Automatischer Rotlichtmodus bei Nacht:** Reduziert nicht nur die Blendwirkung auf das dunkeladaptierte Auge, sondern senkt auf OLED-Displays den Stromverbrauch um ca. 12–15 %.
2. **AR-Auto-Sleep:** Der AR-Modus schaltet sich automatisch ab, sobald das Gerät flach auf den Tisch gelegt wird (Pitch ≈ 0°), und fällt auf die stromsparende manuelle Sternkarte zurück.
3. **Adaptive Sensor-Drosselung:** Wenn keine Bewegung am Gerät erkannt wird (stationäres Stativ), wird die interne Filter- und Glättungsrate reduziert.
