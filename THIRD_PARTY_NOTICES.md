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
- Base map: © OpenStreetMap contributors, used according to the OSM tile usage policy,
  https://operations.osmfoundation.org/policies/tiles/
- Map renderer: Leaflet 1.9.4, BSD-2-Clause, https://leafletjs.com/

## Astronomy event calendar

- Eclipse dates and global visibility regions: NASA Goddard Space Flight Center eclipse catalogs,
  https://eclipse.gsfc.nasa.gov/
- Recurring meteor-shower maxima, radiant coordinates, and ideal zenithal hourly rates:
  International Meteor Organization meteor-shower calendars, https://www.imo.net/resources/calendar/

The app combines these catalog facts with a local, approximate visibility estimate. Meteor-shower
maxima can vary and the calculated score is not an official observing forecast.

## Night-light map

- Earth at Night (2012, VIIRS, Suomi NPP) imagery: NASA Global Imagery Browse Services (GIBS),
  layer `VIIRS_CityLights_2012`, https://earthdata.nasa.gov/gibs

The night-light imagery is used as a visual proxy for artificial light pollution. It is not a
current measurement or a Bortle-class map.
