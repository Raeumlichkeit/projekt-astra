# Projekt Astra 1.1.9-pre.2 · Beta

Priorisiertes Laden der Basiskarte vor GPU/Texturladen, Entkopplung von GL-Fehlern, Textur-Wiederholen und Speicherresilienz. Zweiter Teil von P1.7, Version Code 20. Dieser Pre-Release stammt von `beta`; `main` bleibt unverändert bei `1.1.8-pre.1`.

## Änderungen

- **Priorisierter Basiskartenstart:** Der erste sichtbare Sternkarten-Rahmen mit Horizont, Himmelsrichtungen, Navigationssternen, Sonne, Mond und Planeten wird sofort gezeichnet, ohne auf den Start des OpenGL ES-Kontexts oder das Dekodieren der 3840-Pixel-Milchstraßentextur zu warten.
- **Entkoppeltes Laden von GPU & Milchstraße:** Erst nach dem ersten gezeichneten Frame wird die `SkyTextureLayer` eingehängt und das Dekodieren sowie der Textur-Upload auf dem Hintergrundthread angestoßen.
- **Absicherung bei Textur- und GPU-Fehlern:** Schlägt die Initialisierung der GPU oder das Laden der Textur fehl, behält die Basiskarte ihren dunklen Nachthimmelhintergrund und bleibt uneingeschränkt interaktiv und nutzbar.
- **Wiederholbares Texturladen:** Im Sheet „Himmel & Ebenen“ wird bei fehlgeschlagener Textur eine Aktion **Textur erneut laden** angeboten, die den GL-Worker gezielt reaktiviert.
- **Heap- und Low-Ram-Schutz:** Vor dem Dekodieren prüft der Worker den freien Systemspeicher (`Runtime.getRuntime()`), um bei knappem Heap automatisch eine stärkere Skalierungsstufe zu wählen und OutOfMemory-Fehler sowie GC-Ruckler zu vermeiden.
- **System-Speicherdruck (`onTrimMemory` / `onLowMemory`):** Bei kritischem Speicherdruck des Systems werden flüchtige Caches für Gelände- und Lichtkartenfreigaben im Hintergrund bereinigt.

## Datenschutz und Grenzen

- Keine neue Abhängigkeit, Berechtigung, Datenquelle oder dauerhafte Datenspeicherung. Der gemeinsame Katalogcache im Arbeitsspeicher aus 1.1.9-pre.1 bleibt unverändert prozesslokal.
- Die historische NASA-Lichtkarte von 2016 bleibt als veraltet gekennzeichnet; externe Kartendaten werden nicht übernommen.
- Dieser Stand optimiert die Priorisierung und Stabilität, garantiert jedoch keine absolute Kaltstartzeit. Endgültige Messwerte auf realen Referenzgeräten stehen noch aus.

## Prüfstand

- 128 JVM-Tests erfolgreich, darunter Basiskarten-Start, Fallback-Sterne, Hintergrundlogik, Retry-Zähler und Katalogcache.
- Android-Instrumentierungstests für EGL/TextureView, einschließlich Neu-Rendern bei `view.retry()` und AR-Alpha-Überlagerung.
- Debug-APK, minifiziertes Release-AAB (`minifyReleaseWithR8`) und Lint erfolgreich: 0 Fehler, 0 Warnungen.

## Bitte auf weiteren Geräten prüfen

1. App frisch starten (Kaltstart): Die Basiskarte mit Horizont und ersten Sternen muss unmittelbar erscheinen, die Milchstraße sich kurz danach weich einblenden.
2. Sternkarte während des Ladens bewegen und zoomen: Keine Blockade des UI-Threads durch den Textur-Upload.
3. Gerät mehrfach drehen (Hoch-/Querformat): Die Karte muss stabil neu ausgerichtet werden, ohne dass der EGL-Worker hängen bleibt.
4. Sheet „Himmel & Ebenen“ öffnen und Milchstraßen-Stile („Aus“, „Natürlich“, „Verstärkt“) wechseln.
5. Verhalten bei wenig RAM und im Flugmodus kontrollieren.

Die `app-debug.apk` ist mit dem Entwicklungszertifikat signiert und direkt installierbar. Das AAB ist ein technischer, nicht für den Play Store freigegebener Build ohne Upload-Signatur. Prüfsummen: `pre-release-1.1.9-pre.2.sha256`.
