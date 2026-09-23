# Zusammenarbeit in Projekt Astra

- Unabhängige, ausreichend große Teilaufgaben möglichst an **native, im Subagents-Bereich sichtbare Agents** delegieren. Die verfügbare Parallelität nutzen, soweit sich dadurch Arbeit sinnvoll beschleunigt; keine Scheinaufgaben nur zum Füllen von Plätzen starten.
- Bei größeren Änderungen einen unabhängigen Supervisor/Reviewer für den integrierten Stand einsetzen. Die Hauptinstanz verantwortet Integration, tatsächliche Testergebnisse und Freigabe. Angeforderte, aktive und abgeschlossene Arbeit nicht miteinander verwechseln.
- Jedem Agent klar abgegrenzte Dateien und Aufgaben zuweisen; laufende und fremde Änderungen erhalten. Gradle und Android-Emulator haben jeweils nur einen ausführenden Besitzer, damit sich Builds/Tests nicht gegenseitig stören.
- Betroffene Funktionen im Android-Emulator prüfen. Modell-/JVM-Tests nicht als vollständige UI-Tests ausgeben; fehlende reale Geräte-, Sensor- und Kameratests ausdrücklich benennen.
- Größere funktionale, technische oder sicherheitsrelevante Änderungen gemäß `RELEASE_PROCESS.md` als GitHub-Pre-Release mit Test-APK und SHA-256 bereitstellen. Keine automatische Übernahme von `beta` nach `main`, kein Play-Store-Release und keine ISO-Zertifizierungsaussage daraus ableiten.
- Akkutests haben niedrige Priorität (P3).

Diese Regeln halten die ausdrücklichen Arbeitspräferenzen des Projektinhabers fest. Sie erweitern nicht den Auftrag einer einzelnen Anfrage: Reviews bleiben ohne gesonderten Änderungsauftrag lesend; Veröffentlichung und Tests müssen zum jeweiligen Auftrag passen.
