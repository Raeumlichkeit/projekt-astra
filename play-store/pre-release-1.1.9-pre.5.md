# Projekt Astra 1.1.9-pre.5 · Beta

Katalog-ID-Synchronisierung, robusteres Objektöffnen, optimierter Tag-/Nacht-Umschaltschwellenwert und verbesserte Teleskopfilterung. Fünfter Teil von 1.1.9, Version Code 23. Dieser Pre-Release stammt von `beta`; `main` bleibt unverändert bei `1.1.8-pre.1`.

## Neuerungen & Korrekturen

- **Synchronisierung der Deep-Sky-Katalog-IDs:**
  - Die kuratierten Empfehlungen verwenden nun exakt die offiziellen Katalog-IDs aus dem gebündelten OpenNGC-Katalog (`M 31 · NGC 224`, `M 45 · Mel022`, `M 42 · NGC 1976`, `M 13 · NGC 6205`, `M 44 · NGC 2632`, `M 57 · NGC 6720`, `M 27 · NGC 6853`, `M 81 · NGC 3031`).
  - **Behebt Favoriten-Warnung:** Vorgemerkte Empfehlungen lösen keine Warnung mehr aus („Einige vorgemerkte Objekte sind im aktuellen Katalog nicht verfügbar“), sondern werden nahtlos in der Favoritenliste mit aktuellen Koordinaten dargestellt.
- **Defensives Zentrieren in der Sternkarte (`SkyScreen`):**
  - Beim Klick auf **„In Karte öffnen“** sucht Astra nun flexibel nach vollständiger Katalog-ID, Teilkomponenten (z. B. `NGC 224` in `M 31 · NGC 224`) oder der Messier-Nummer. Ziele werden dadurch in jedem Fall zuverlässig zentriert.
- **Optimierter Nacht-Planungsschwellenwert:**
  - Der Schwellenwert in `TonightWindowCalculator` wurde von `hour < 12` auf `hour < 6` angepasst. Wer die App morgens oder mittags öffnet, erhält nun stets die Vorhersage für die **kommende Nacht** (inklusive passender Open-Meteo-Wetterprognose), anstatt der vergangenen Nacht.
- **Ausrüstungsfilter „Teleskop“:**
  - Teleskopbeobachter erhalten nun neben speziellen Deep-Sky-Nebel- und Kugelsternhaufenzielen auch alle Planeten (insbesondere den Mars mit Polkappen) sowie den Mond als lohnende Ziele angezeigt.
- **Mond-Detailverbesserung:**
  - Bei der Empfehlungskarte für den Mond wird die irreführende Angabe „Mondabstand: 0°“ unterdrückt und eine bereinigte Beschreibung angezeigt.
- **100 % Privacy & On-Device:**
  - Alle Berechnungen laufen weiterhin vollständig lokal auf dem Gerät.

## Prüfstand

- 140 JVM-Tests erfolgreich (`:app:testDebugUnitTest`), inklusive der neuen Tests für Vormittagsplanung, Teleskopfilterung, Mondabstand und Katalog-ID-Konsistenz.
- Lint-Prüfung (`:app:lintDebug`) fehlerfrei abgeschlossen (0 Fehler).
- Debug-APK und Release-AAB erfolgreich gebaut.

## APK & Installation

Die `app-debug.apk` ist mit dem Entwicklungszertifikat signiert und direkt installierbar. Prüfsummen: `pre-release-1.1.9-pre.5.sha256`.
