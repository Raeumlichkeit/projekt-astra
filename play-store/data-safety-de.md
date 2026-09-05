# Entwurf für den Bereich „Datensicherheit“

Dieser Entwurf beschreibt Version 1.1.0. Die Antworten müssen vor jeder Veröffentlichung noch einmal gegen den tatsächlichen App-Code und die aktuellen Google-Play-Fragen geprüft werden.

## Datenerhebung und Weitergabe

- Erhobene Datentypen: ungefährer und genauer Standort
- Zweck: App-Funktionalität – lokaler Himmel, Wetter, Geländeprofil und standortbezogene Ereignisse
- Erhebung: optional; die App ist mit dem deutlich gekennzeichneten Demo-Standort Berlin nutzbar
- Verarbeitung: während der Nutzung, teilweise nur vorübergehend
- Übertragung: ausschließlich verschlüsselt über HTTPS
- Verkauf: nein
- Werbung oder Personalisierung: nein
- Eigenes Benutzerkonto: nein
- Analyse- oder Tracking-SDK: nein

Standortkoordinaten werden für Wetter- und Geländeanfragen an Open-Meteo sowie für die numerische Nachtlichtschätzung an NASA GIBS übertragen. Kartenanbieter erhalten technisch notwendige Karten- und Kachelanfragen. Die App betreibt keinen eigenen Server und speichert Standortdaten nicht dauerhaft.

Objektfavoriten, Beobachtungsereignisse, Erinnerungsvorlauf und Rotlichtmodus bleiben ausschließlich lokal auf dem Gerät. Die jährlichen Meteorschauer-Kalender werden ohne persönliche Kennung direkt von der International Meteor Organization abgerufen.

## Kamera

Die Kamera wird nur für die AR-Vorschau auf dem Gerät verwendet. Bilder und Videoframes werden nicht gespeichert und nicht vom Gerät übertragen. Kameradaten sind daher nicht als off-device erhobene Daten zu deklarieren.

## Löschung

Es existieren weder Konto noch serverseitiges Nutzerprofil. App-Einstellungen können durch Löschen der App-Daten oder Deinstallieren entfernt werden.

## Benachrichtigungen

Die optionale Benachrichtigungsberechtigung dient ausschließlich lokalen Ereigniserinnerungen. Projekt Astra verwendet keinen externen Push-Dienst und überträgt keine Favoriten oder Beobachtungslisten.

## Sicherheit

- Datenübertragung verschlüsselt: ja
- Nutzer können die App ohne Standortfreigabe verwenden: ja
- Unabhängige Sicherheitsprüfung: nein, sofern vor Veröffentlichung keine durchgeführt wird
