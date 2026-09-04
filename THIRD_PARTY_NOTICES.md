# Third-party data

## HYG Stellar Database v4.1

The bundled bright-star subset in `app/src/main/assets/hyg_bright_stars.tsv` is derived from the
HYG Stellar Database v4.1 by David Nash / Astronomy Nexus.

- Source: https://codeberg.org/astronexus/hyg
- Archive used for the generated subset: https://github.com/astronexus/HYG-Database
- License: Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0)
- Filter: stars with visual magnitude `mag <= 6.0`
- Retained fields: HIP identifier, display name, J2000 right ascension and declination, visual
  magnitude, distance, spectral type, colour index, and constellation abbreviation.

The generated subset remains available under CC BY-SA 4.0.

## Weather map

- Rain radar tiles and timeline metadata: RainViewer Weather Maps API, free for personal and
  educational use with attribution, https://www.rainviewer.com/api.html
- Cloud-cover values: Open-Meteo, https://open-meteo.com/
- Base map: © OpenStreetMap contributors, used according to the OSM tile usage policy,
  https://operations.osmfoundation.org/policies/tiles/
- Map renderer: Leaflet 1.9.4, BSD-2-Clause, https://leafletjs.com/
