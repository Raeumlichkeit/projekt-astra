# Milchstraßenhintergrund: Herkunft und Reproduktion

`app/src/main/assets/milkyway_gaia_2020.jpg` ist die unverändert gebündelte,
bereits auf 3840 × 1920 Pixel verkleinerte JPEG-Fassung von NASAs
[Deep Star Maps 2020](https://svs.gsfc.nasa.gov/4851/). Verwendet wird ausdrücklich
der **Milchstraßenhintergrund ohne helle Hipparcos-/Tycho-Sterne**. Die interaktiv
gezeichneten Vordergrundsterne der App werden dadurch nicht doppelt abgebildet.

Das Hintergrundbild ist eine Visualisierung gemessener Sternpositionen,
Helligkeiten und Farben, keine fotografische Panoramaaufnahme und kein
KI-generiertes Bild. Feine Sternfelder, Dunkelwolken, galaktisches Zentrum und
Magellansche Wolken sind Bestandteil der Quelldaten. Die Darstellung ist keine
photometrische Messung und keine Vorhersage der Sichtbarkeit mit bloßem Auge.

## Quellen und Nutzungsbedingungen

- Wissenschaftliche Beschreibung, Projektion und Originaldateien:
  <https://svs.gsfc.nasa.gov/4851/>
- Verwendete JPEG-Fassung einschließlich Dateihistorie und Lizenz:
  <https://commons.wikimedia.org/wiki/File:Deep_Star_Maps_2020_%E2%80%93_Milkyway_2020_64k.jpg>
- NASA-SVS-Nutzungsbedingungen:
  <https://svs.gsfc.nasa.gov/help/>
- Allgemeine NASA-Medienrichtlinien:
  <https://www.nasa.gov/nasa-brand-center/images-and-media/>

NASA SVS erklärt seine Inhalte grundsätzlich als gemeinfrei, soweit nicht
abweichend gekennzeichnet. Die Dateiseite der verwendeten Wikimedia-JPEG-Fassung
gestattet Weitergabe, Bearbeitung und kommerzielle Nutzung mit Namensnennung
(„Attribution only“, keine behauptete CC-BY-Versionsnummer). Die App nennt NASA
und ESA als Datenquellen; daraus folgt keine Unterstützung oder Freigabe der App
durch diese Organisationen. Keine NASA-Logos verwenden.

Herkunftsnachweis:

> NASA/Goddard Space Flight Center Scientific Visualization Studio (Ernie Wright).
> Gaia DR2: ESA/Gaia/DPAC. JPEG conversion provided via Wikimedia Commons
> (PantheraLeo1359531).

## Orientierung und Farbraum

Das NASA-Original nutzt eine Plate-carrée-Projektion in ICRF/J2000-geozentrischen
Äquatorialkoordinaten. Die Mitte liegt bei RA 0h; RA nimmt nach links zu.
Für normierte Texturkoordinaten gilt daher:

```text
u = fract(0.5 - RA_in_Grad / 360)
v = 0.5 - Deklination_in_Grad / 180
```

Oben liegt der nördliche, unten der südliche Himmelspol. Die horizontale Naht
liegt bei RA 12h. Bei der lokalen Projektion muss dasselbe Koordinatensystem wie
bei den Vordergrundsternen verwendet werden. Die Textur niemals automatisch
spiegeln, drehen oder auf 4096 Pixel strecken. Die galaktischen NASA-Varianten
sind andere Dateien; diese werden hier nicht verwendet.

Die Wikimedia-Quelldatei ist als sRGB dokumentiert. Ihr eingebettetes ICC-Profil
bleibt erhalten. Die EXR-zu-JPEG-Konvertierung erfolgte bereits upstream; deren
genaue Tone-Mapping-Parameter sind nicht dokumentiert. Astra nimmt **keine**
erneute Farbkorrektur, Interpolation, Kompression oder Pixelbearbeitung vor.
Der Renderer darf für den gewählten Kartenmodus die Anzeigehelligkeit anpassen;
das unveränderte Asset bleibt davon getrennt. Eine beliebige HDR-Konvertierung
würde nicht zwangsläufig dieselben Pixel reproduzieren.

## Prüfen und reproduzieren

Benötigt wird Python mit Pillow (hier geprüft mit Pillow 12.3.0); dies ist nur
ein Entwicklungswerkzeug, keine neue Android-Abhängigkeit. Bereits enthaltene
Datei offline prüfen:

```powershell
python scripts/prepare_milky_way.py --check
```

Gepinnte Quelle über HTTPS erneut laden und bytegleich bündeln:

```powershell
python scripts/prepare_milky_way.py --download
```

Alternativ eine schon heruntergeladene Quelldatei verwenden:

```powershell
python scripts/prepare_milky_way.py --source app/build/milkyway-source.jpg
```

Größe: **1.465.425 Byte**, Maße: **3840 × 1920 Pixel**, RGB/JPEG.

SHA-256:

```text
6eb620fff7e4742323b5e291a45844cdaff131c7755b3c8a386574db3febb6dc
```

Der Download ist auf HTTPS-Bildhosts von Wikimedia und 5 MB begrenzt. Änderungen
an Quelle oder bereits vorhandenem Asset führen zu einem Prüffehler statt zum
stillen Überschreiben. Metadaten stehen daneben in
`milkyway_gaia_2020.metadata.json`. Quelle und fertiges Asset müssen denselben
SHA-256-Wert besitzen. Es gibt keinen Abruf dieses Bildes durch die App und
keinen automatischen Download beim Gradle-Build.

Die direkten NASA-Bilddownloads lieferten bei der Einbindung am 06.09.2026
HTTP 403. Deshalb wird die mit NASA-Herkunft dokumentierte Wikimedia-JPEG-Fassung
verwendet. Falls diese Thumbnail-Datei upstream einmal verändert wird, muss
eine neue Fassung vor Aktualisierung des Hashes erneut geprüft werden.
