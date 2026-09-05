# Datensicherheit – Prüfentwurf 1.1.1

Nicht unverändert in Play Console übernehmen: Anbieterrollen und Drittland-/Rechtsgrundlagenprüfung sind noch offen. Verantwortlicher: Jeremy Grez; Support/Datenschutz: jeregrez@gmail.com. Keine ISO-Zertifizierung und keine unabhängige Sicherheitsprüfung behaupten.

- Ungefährer Standort: optional nach separater Online-Freigabe, für Wetter/Wolken und Karten auf 0,01° gerundet.
- Genauer Standort: nur bei zusätzlicher Freigabe des Online-Geländeprofils an Open-Meteo.
- Auch IP-Adresse, Zeit, App-Version und angefragte Kartenbereiche/Objektbilder/Jahreskalender erreichen Dienstleister.
- Open-Meteo nennt Protokolle einschließlich Standort/IP für bis zu 90 Tage. Daher **nicht pauschal „nur flüchtige Verarbeitung“ oder „keine Datenerhebung“ ankreuzen**.
- Für andere Anbieter sind die Aufbewahrung und die Einordnung als Weitergabe/Dienstleister vor Veröffentlichung einzeln zu prüfen.
- Keine Werbung, Analyse-SDKs, eigene Konten oder eigener Backend-Server. HTTPS ist erzwungen.
- Kamera-/Sensordaten, Favoriten und Ereignislisten verlassen die App nicht über deren Netzwerkfunktionen.
- Lokaler OSM-Kartencache: maximal 32 MB, Gültigkeit maximal 30 Tage, anschließend Löschung beim nächsten Start/Abruf. Betrachtete Regionen können aus Kacheln erschlossen werden.
- Online-Freigabe widerrufen und Kartencache löschen unter Info. Alle lokalen Daten über Android-App-Einstellungen löschen.
- Deinstallation/App-Daten-Löschung entfernt keine schon übermittelten Anbieterprotokolle.
- Lokale Benachrichtigungen über Android; kein Push-Dienst.
- App-Cloud-Backups und Geräteübertragung ausgeschlossen.

Details und Empfänger: [Datenschutzentwurf](privacy-policy.html). Vor Release aktuelle [Google-Play-Anleitung](https://support.google.com/googleplay/android-developer/answer/10787469) und den tatsächlichen Release-Code gemeinsam prüfen. IP-basierte Informationen und Anbieterprotokolle nicht übergehen.
