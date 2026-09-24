# 1.1.9-pre.19 – Weniger Arbeit beim Schwenken

24. September 2026 · Version Code 37 · Beta-Testversion

## Änderungen

- Die diffuse Milchstraßen-Fotoebene nutzt in der manuellen Sternkarte bei größeren Ansichten einen auf 75 % der Kantenlänge (höchstens 1600 Pixel an der längsten Seite) verkleinerten GPU-Puffer. Die Ansicht skaliert diese Ebene auf die Bildschirmgröße; scharfe Sterne, Linien und Texte bleiben in ihrer bisherigen Auflösung. AR behält den vollen Puffer.
- Beim manuellen Schwenken entfallen Projektionen von Objekten mehr als 5° unter dem opaken Horizont. AR und Objektauswahl bleiben unverändert. Die Sternprojektion berechnet Sinus und Kosinus der Höhe nur einmal pro Punkt.
- Die auffällige goldene Linie mit Punkten auf der Sternkarte ist eine **vorausberechnete Satellitenbahn mit Zeitmarken**, kein Fehler der Milchstraßen-Textur. Sie kann auch einen Überflug später am Tag zeigen. Die goldene Kurve am unteren Rand ist der Horizont. Eine verständlichere Kennzeichnung und ein Schalter für Satellitenbahnen stehen noch auf der Aufgabenliste.

## Verifikation

- 517 JVM-Tests bestanden, keine Fehler oder übersprungenen Tests.
- 68 Android-Instrumentierungstests auf dem Pixel-9-Pro-XL-Emulator mit Android 17 / API 37 bestanden; darunter Textur-Größenwechsel, manuell–AR–manuell, Kartenränder, Horizont und Wischgesten.
- Android-Lint: „No Issues Found“. Debug-APK, Android-Test-APK und minifiziertes Release-AAB gebaut.
- Manuelle Emulatorprüfung in Hoch- und Querformat: Sterne, Satellitenbahn, Horizont und Milchstraße bleiben nach dem Schwenken sichtbar; im Querformat reicht die Textur bis an die Ränder. In einem isolierten Zwei-Sekunden-Test der Fotoebene wurden zuvor 63–65 und danach 83–92 fertig gerenderte Bilder gemessen. Bei identischen Serien von je zehn Wischgesten sank der Median der gesamten UI-Frames im Emulator von 150 ms (alter Stand, zwei Durchläufe) auf 129 ms (neuer Stand, zwei Durchläufe).

## Grenzen

- Das Schwenken ist **noch nicht durchgehend flüssig**: Die meisten UI-Frames im Emulator überschritten weiterhin das Bildzeitbudget. Die zwei Renderpfade können während des Wischens kurz auseinanderlaufen. Die Messwerte sind Emulatorwerte, keine Zusage für andere Geräte.
- Die verkleinerte diffuse Fotoebene kann feine Milchstraßenstrukturen etwas weicher wirken lassen. Ein kurzer schwarzer Übergangs-Frame bei Größen- oder AR-Wechsel wurde durch die stabilen Endbildtests nicht ausgeschlossen.
- Ein Test auf einem schwächeren echten Gerät sowie echte Kamera-, Sensor- und Rotationstests bleiben nötig. Keine neuen Berechtigungen oder Datenübertragungen. Die APK ist debugsigniert und nur für Tests gedacht; keine Play-Store-Freigabe und keine ISO/IEC-27001-Zertifizierung. `main` bleibt unverändert.

## Test-APK

```text
ce6f016503e1250ea26da7f29da3e6eea5b2ee875c8cb89892d6f8467fd50003  projekt-astra-1.1.9-pre.19.apk
```
