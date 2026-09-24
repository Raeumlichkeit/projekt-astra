# Datensicherheit – Prüfentwurf 1.1.9-pre.18

Nicht unverändert in Play Console übernehmen: Anbieterrollen und Drittland-/Rechtsgrundlagenprüfung sind noch offen. Verantwortlicher: Jeremy Grez; Support/Datenschutz: jeregrez@gmail.com. Keine ISO-Zertifizierung und keine unabhängige Sicherheitsprüfung behaupten.

- Ungefährer Standort: optional nach separater Online-Freigabe, für Wetter/Wolken und Karten auf 0,01° gerundet.
- Externe Lichtkarte: erst nach zusätzlicher bewusster Ladeaktion in der Ansicht. Empfänger LightPollutionMap.app samt Karten-API, jsDelivr und optional OSM/CARTO/Esri. Startort gerundet, sonst Berlin-Demo; angefragte Kartenbereiche und IP-Adresse sind personenbezogen auswertbar. Anbieterfristen offen. Keine Übernahme der Modellwerte in den Astra-Score; kein GPS-/Kamera-/Dateizugriff der Website.
- Genauer Standort: nur bei zusätzlicher Freigabe des Online-Geländeprofils an Open-Meteo.
- Auch IP-Adresse, Zeit, App-Version und angefragte Kartenbereiche/Objektbilder/Jahreskalender erreichen Dienstleister. Die externe Lichtkarte sendet statt Astras Versionskennung den Browser-User-Agent mit Browser-/Android- und gegebenenfalls Geräteangaben.
- Open-Meteo nennt Protokolle einschließlich Standort/IP für bis zu 90 Tage. Daher **nicht pauschal „nur flüchtige Verarbeitung“ oder „keine Datenerhebung“ ankreuzen**.
- Für andere Anbieter sind die Aufbewahrung und die Einordnung als Weitergabe/Dienstleister vor Veröffentlichung einzeln zu prüfen.
- Keine Werbung, Analyse-SDKs, eigene Konten oder eigener Backend-Server. HTTPS ist erzwungen.
- Kamera-/Sensordaten, Favoriten und Ereignislisten verlassen die App nicht über deren Netzwerkfunktionen.
- Standort-Merken ist standardmäßig aus und unabhängig von der GPS-Berechtigung. Nach ausdrücklichem Einschalten werden die letzten genauen Koordinaten und die Höhe im privaten App-Speicher gehalten; Abschalten/Zurücksetzen entfernt sie. Alte automatisch gespeicherte Koordinaten ohne gesetzte Merken-Auswahl werden beim Laden entfernt.
- Beobachtungstagebuch: Objekt, Zeitpunkt, Notizen, Ausrüstung und Bewertungen; optional bewusst angehängte Ortsdaten und ausgewählte Fotokopien. Fotos können EXIF/GPS-Metadaten enthalten. Keine automatischen Uploads. Löschung im Logbuch; manuelle JSON/XML/Text-Exporte enthalten gegebenenfalls genaue Orte und Notizen im Klartext, jedoch keine Fotodateien.
- Widget: letzter Wetter-Score lokal gespeichert, Standort aus der separat freigegebenen Standort-Speicherung oder Berlin-Demo. Keine Hintergrund-Ortung. Auf dem Homescreen sichtbare Ortsangaben mit berücksichtigen.
- Lokaler OSM-Kartencache: maximal 32 MB, Gültigkeit maximal 30 Tage, anschließend Löschung beim nächsten Start/Abruf. Betrachtete Regionen können aus Kacheln erschlossen werden.
- Online-Freigabe widerrufen und Kartencache löschen unter Info. Alle lokalen Daten über Android-App-Einstellungen löschen.
- Deinstallation/App-Daten-Löschung entfernt keine schon übermittelten Anbieterprotokolle.
- Lokale Benachrichtigungen über Android; kein Push-Dienst.
- App-Cloud-Backups und Geräteübertragung ausgeschlossen.

Details und Empfänger: [Datenschutzentwurf](privacy-policy.html). Vor Release aktuelle [Google-Play-Anleitung](https://support.google.com/googleplay/android-developer/answer/10787469) und den tatsächlichen Release-Code gemeinsam prüfen. IP-basierte Informationen und Anbieterprotokolle nicht übergehen.
