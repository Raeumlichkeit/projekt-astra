# 1.1.9-pre.16 – Lesbareres Widget und Lint-Bereinigung

23. September 2026 · Version Code 34 · Beta-Testversion

## Änderungen

- Widget-Texte aus dem Layout in Stringressourcen ausgelagert; Schrift mindestens 11sp und Layout ohne verschachtelte Gewichte. Bei großer Systemschrift entfallen redundante Beschriftungen; vollständige Informationen bleiben über Accessibility verfügbar.
- Nachtzeiten stehen als kompakter Bereich `HH:mm–HH:mm` im Widget, damit die Uhrzeiten auf 180dp Breite nicht hinter einem abgeschnittenen Präfix verschwinden. Fehlende astronomische Dunkelheit ergibt kein erfundenes Zeitintervall.
- Fehlender Wetter-Cache ergibt „—“ statt des bisher angenommenen Werts 80/100. Vorhandene Werte bleiben begrenzt auf 0–100; im Großschrift-Kompaktmodus kennzeichnet ein Wolkensymbol den Wetterwert. Kein zusätzlicher Netzwerk- oder Hintergrund-GPS-Zugriff.
- Drei vorhandene SharedPreferences-Schreibstellen nutzen die Kotlin-Erweiterung mit unverändert asynchronem `apply`-Verhalten. Kamera-Sichtfeld und Simulationsgeschwindigkeit verwenden die passenden numerischen Compose-Zustände.
- Gezielte Updates: [AndroidX Core KTX 1.19.1](https://developer.android.com/jetpack/androidx/releases/core#1.19.1), [WorkManager KTX 2.12.0](https://developer.android.com/jetpack/androidx/releases/work#2.12.0), [Android Test Runner 1.7.0](https://developer.android.com/jetpack/androidx/releases/test#runner-1.7.0). Die offiziellen POM-/AAR-Metadaten passen zu minSdk 28, compileSdk 37 und AGP 9.4. Keine pauschalen Dependency-Upgrades.
- Dependency-Lock und SHA-256-Verifikationsmetadaten aktualisiert. Die zehn neuen Artefakt-/Metadatenprüfsummen wurden mit direkten HTTPS-Abrufen von Google Maven abgeglichen; die normale Gradle-Verifikation bleibt aktiv.

## Verifikation

- **Lint: „No issues found“** – die zuvor gemeldeten 17 Warnungen und zwei Hinweise sind beseitigt, ohne Baseline oder Unterdrückungen hinzuzufügen.
- **517 JVM-Tests bestanden**, keine Fehler und keine übersprungenen Tests.
- Debug-APK, Android-Test-APK und minifiziertes Release-AAB erfolgreich mit normaler Dependency-Verifikation gebaut.
- **63/63 Android-Instrumentierungstests bestanden** auf Pixel-9-Pro-XL-Emulator, Android 17 / API 37 (177,369 Sekunden), einschließlich der fünf neuen Widget-/WorkManager-Prüfungen.
- Die beiden synthetischen Widget-Renderings bei 180×110dp und Schriftfaktor 1/2 wurden zusätzlich visuell geprüft: Nachtzeiten vollständig sichtbar, keine überlappenden Textfelder. Bei großer Schrift bleiben lange Bezeichnungen abgekürzt; der vollständige Inhalt bleibt für Accessibility verfügbar.
- Berechtigungen der fertigen APK gegen `1.1.9-pre.15` verglichen: unverändert. minSdk 28 und targetSdk 37 bleiben erhalten.

Zusätzliche Android-Prüfungen decken echtes RemoteViews-Layout bei 180×110dp und Schriftfaktor 1/2, vollständige sichtbare Nachtzeiten ohne Ellipsis, fehlenden Wetter-Cache sowie das Planen/Ersetzen/Abbrechen einer zukünftigen Erinnerung über Astras echten WorkManager-Pfad ab. Test-Erinnerungen verwenden eigene IDs und werden gezielt abgebrochen; keine Benachrichtigungen werden verschickt.

## Grenzen

- Emulatorprüfungen ersetzen keine Tests auf echten Geräten, unterschiedlichen Launchern oder mit realer Kamera/Kompass. Akkutests bleiben P3.
- Widget-Wetterwerte stammen weiterhin aus dem lokalen letzten Wetterabruf, nicht aus einer eigenständigen Live-Abfrage.
- Debugsignierte Test-APK, keine Play-Store-Freigabe und keine ISO/IEC-27001-Zertifizierung. Upload-Key und rechtliche Datenschutzfreigabe bleiben offen.
- `main` bleibt unverändert; der Pre-Release gehört zu `beta`.

## Test-APK

```text
171c0fc933565cba81788e6ca718405cdb7c8d5e8d1708054adffafd3d42e496  projekt-astra-1.1.9-pre.16.apk
```
