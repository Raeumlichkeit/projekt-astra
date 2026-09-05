# Third-party data

## HYG Stellar Database v4.1

The bundled bright-star subset in `app/src/main/assets/hyg_bright_stars.tsv` is derived from the
HYG Stellar Database v4.1 by David Nash / Astronomy Nexus.

- Source: https://codeberg.org/astronexus/hyg
- Archive used for the generated subset: https://github.com/astronexus/HYG-Database
- License: Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0)
- Filter: stars with a HIP identifier and visual magnitude `mag <= 6.0`
- Retained fields: HIP identifier, display name, J2000 right ascension and declination, visual
  magnitude, distance, spectral type, colour index, constellation abbreviation, proper motion,
  radial velocity, absolute magnitude, and luminosity.

The generated subset remains available under CC BY-SA 4.0.

## OpenNGC

The optional deep-sky subset in `app/src/main/assets/openngc_deep_sky.tsv` is derived from
OpenNGC by Mattia Verga.

- Source: https://github.com/mattiaverga/OpenNGC
- License: Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0)
- Filter: Messier objects plus named, visually bright, or large NGC/IC and addendum objects
- Retained fields: identifiers, J2000 coordinates, object type, magnitude, constellation,
  angular size, position angle, redshift, and Messier identifier.

The generated subset remains available under CC BY-SA 4.0.

## Sky survey images

- DSS2 colour survey cutouts: CDS HiPS2FITS service, Observatoire astronomique de Strasbourg,
  https://alasky.u-strasbg.fr/hips-image-services/hips2fits

## Weather map

- Rain radar tiles and timeline metadata: RainViewer Weather Maps API, free for personal and
  educational use with attribution, https://www.rainviewer.com/api.html
- Cloud-cover values: Open-Meteo, https://open-meteo.com/
- Local horizon elevations: Open-Meteo Elevation API using the 90-metre Copernicus GLO-90
  digital elevation model, https://open-meteo.com/en/docs/elevation-api
- Open-Meteo data license: Creative Commons Attribution 4.0 (CC BY 4.0) for
  non-commercial use, https://open-meteo.com/en/licence
- Base map: © OpenStreetMap contributors, used according to the OSM tile usage policy,
  https://operations.osmfoundation.org/policies/tiles/
- Map renderer: Leaflet 1.9.4, BSD-2-Clause, https://leafletjs.com/

## Astronomy event calendar

- Eclipse dates and global visibility regions: NASA Goddard Space Flight Center eclipse catalogs,
  https://eclipse.gsfc.nasa.gov/
- Recurring meteor-shower maxima, radiant coordinates, and ideal zenithal hourly rates:
  International Meteor Organization meteor-shower calendars, https://www.imo.net/resources/calendar/

Solar-system positions and exact local eclipse circumstances are calculated with Astronomy Engine.
Meteor-shower maxima can vary and the calculated score is not an official observing forecast.

The bundled JSON is generated from the official annual IMO PDF calendars by
`scripts/update_imo_calendar.py`. A scheduled GitHub workflow refreshes the current and following
year; the app retrieves that generated JSON and retains a local offline fallback.

## IAU constellation boundaries

- Catalogue VI/49, `constbnd.dat`: Davenhall A. C. and Leggett S. K., Catalogue of
  Constellation Boundary Data, CDS/VizieR.
- Source: https://cdsarc.cds.unistra.fr/viz-bin/cat/VI/49
- The catalogue is based on the official constellation delimitations by Eugène Delporte (1930).
- The bundled B1875 points are precessed to J2000 in the app and used to render all 88 boundaries.

## Astronomy Engine 2.1.19

- Source: https://github.com/cosinekitty/astronomy
- License: MIT
- Used for topocentric Sun, Moon and planet positions, illumination, constellation lookup,
  galactic-coordinate rotation, and local solar/lunar eclipse calculations.
- Full license text: `LICENSES/Astronomy-Engine-MIT.txt`

## Night-light map and numerical estimate

- Black Marble Nighttime Lights (annual 2016, VIIRS, Suomi NPP) imagery: NASA Global Imagery
  Browse Services (GIBS), layer `VIIRS_Night_Lights`, https://earthdata.nasa.gov/gibs

Rendered pixel luminance near the observer is converted into an explicitly labelled 0–100 proxy,
estimated Bortle class and approximate sky brightness. These values are not an SQM measurement.
