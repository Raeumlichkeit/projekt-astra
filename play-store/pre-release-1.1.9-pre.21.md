# 1.1.9-pre.21 – Zenit-Horizont und Satellitennamen

24. September 2026 · Version Code 39 · Beta-Testversion

## Änderung

Bei fast senkrechtem Blick nach oben und 150° horizontalem Sichtfeld zeigte die Karte am oberen Rand erneut einen scharf begrenzten Bereich. Anders als beim Rechteckfehler aus pre.19/20 war dies der zweite, tatsächlich sichtbare Horizontbogen: Die Karte deckte Boden bislang nur unter dem unteren Bogen ab. Der vollständige Horizontkonturpfad deckt jetzt beide Bodenzonen ab und berücksichtigt Geländeprofil sowie gedrehte oder gespiegelte Optik.

Satellitenpassagen tragen nun „SAT“, den Satellitennamen und „max.“ mit der Uhrzeit der größten Höhe. Die optionale Ekliptiklinie heißt „EKLIPTIK“; eine goldene Linie durch die Sonne ist also nicht automatisch eine Satellitenbahn. Beschriftungen werden nur oberhalb des Horizonts und mit Kollisionsprüfung platziert.

## Verifikation

- Den oberen Horizontbogen mit pre.20 auf dem Android-17-Emulator bei 90° Blickhöhe und 150° Sichtfeld reproduziert. Die pre.21-Test-APK zeigt nach einem Home-/Wiederöffnen-Zyklus dort korrekt Boden statt Sternhimmel; Satellitennamen und die zugeschaltete Ekliptikbeschriftung wurden in der Karte visuell geprüft.
- Neue Instrumentierungstests decken Zenit, Randbereiche, Blick nach unten und erhöhtes Gelände ab; bestehende Tests für gedrehte Optik und verdeckte Objekte bleiben erhalten.
- Vollständiger Lauf: 517 JVM-Tests und 73 Instrumentierungstests auf dem Android-17-Emulator bestanden; Android Lint meldet keine Probleme. Debug-Test-APK und minifiziertes Release-Bundle wurden erfolgreich gebaut.

## Grenzen

- Nur die ersten drei enthaltenen Satelliten werden derzeit vorhergesagt. Auch zukünftige Passagen der nächsten zwölf Stunden bleiben eingeblendet; ein eigener Ein-/Aus-Schalter ist noch offen. Bei engem Sichtfeld oder dichten Objekten kann die Kollisionsprüfung Namen ausblenden.
- TLE-Bahnen sind Näherungen und müssen auf dem echten Gerät mit aktueller Uhrzeit und Standort gegengeprüft werden. Kamerasensoren und GPU-Leistung eines echten Handys wurden hier nicht getestet.
- Die APK ist debugsigniert und nur für Tests gedacht. Keine neuen Berechtigungen oder Datenübertragungen; keine Play-Store-Freigabe und keine ISO/IEC-27001-Zertifizierung. `main` bleibt unverändert.

## Test-APK

`projekt-astra-1.1.9-pre.21.apk` · SHA-256: `82bd559e2827f0104c9390dddd25dcf299df1659a3ae089fb8337e22d140aa85`
