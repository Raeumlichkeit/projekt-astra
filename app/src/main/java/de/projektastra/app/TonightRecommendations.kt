package de.projektastra.app

import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.illumination
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal enum class ObservationEquipment(val label: String) {
    ALL("Alle"),
    NAKED_EYE("Bloßes Auge"),
    BINOCULARS("Fernglas"),
    TELESCOPE("Teleskop")
}

internal data class TonightWindow(
    val nightDateText: String,
    val sunsetText: String,
    val darknessStart: Instant,
    val darknessEnd: Instant,
    val darknessText: String,
    val sunrisesText: String,
    val moonPhaseName: String,
    val moonPhasePercent: Int,
    val moonSummary: String,
    val bestWindowSummary: String,
    val weatherNotice: String
)

internal data class TonightTarget(
    val objectData: CelestialObject,
    val equipment: ObservationEquipment,
    val peakInstant: Instant,
    val peakAltitudeDegrees: Double,
    val currentAltitudeDegrees: Double,
    val moonSeparationDegrees: Double,
    val qualityScore: Int,
    val highlightTitle: String,
    val reason: String
)

internal object TonightWindowCalculator {

    fun calculate(
        observer: GeoPoint,
        now: Instant,
        weather: WeatherSnapshot? = null,
        zone: ZoneId = ZoneId.systemDefault()
    ): TonightWindow {
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN).withZone(zone)
        val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN).withZone(zone)

        val localNow = now.atZone(zone)
        val nightDate = if (localNow.hour < 6) localNow.toLocalDate().minusDays(1) else localNow.toLocalDate()
        val nightDateText = dateFormatter.format(nightDate.atTime(20, 0).atZone(zone))

        // Sample Sun altitude from 15:00 on nightDate to 09:00 on the following day (108 steps of 10 min)
        val sampleStart = nightDate.atTime(15, 0).atZone(zone).toInstant()
        val sampleEnd = nightDate.plusDays(1).atTime(9, 0).atZone(zone).toInstant()
        val stepSeconds = 600L

        var sunsetInstant: Instant? = null
        var duskNauticalInstant: Instant? = null
        var duskAstroInstant: Instant? = null
        var dawnAstroInstant: Instant? = null
        var dawnNauticalInstant: Instant? = null
        var sunriseInstant: Instant? = null

        var currentInstant = sampleStart
        var prevSunAlt = SolarSystemCatalog.horizontal(Body.Sun, observer, currentInstant).altitude

        while (!currentInstant.isAfter(sampleEnd)) {
            val nextInstant = currentInstant.plusSeconds(stepSeconds)
            val nextSunAlt = SolarSystemCatalog.horizontal(Body.Sun, observer, nextInstant).altitude

            // Sunset: Sun sinks below 0°
            if (prevSunAlt > 0.0 && nextSunAlt <= 0.0 && sunsetInstant == null) {
                sunsetInstant = nextInstant
            }
            // Nautical dusk: Sun sinks below -12°
            if (prevSunAlt > -12.0 && nextSunAlt <= -12.0 && duskNauticalInstant == null) {
                duskNauticalInstant = nextInstant
            }
            // Astronomical dusk: Sun sinks below -18°
            if (prevSunAlt > -18.0 && nextSunAlt <= -18.0 && duskAstroInstant == null) {
                duskAstroInstant = nextInstant
            }
            // Astronomical dawn: Sun rises above -18°
            if (prevSunAlt <= -18.0 && nextSunAlt > -18.0) {
                dawnAstroInstant = nextInstant
            }
            // Nautical dawn: Sun rises above -12°
            if (prevSunAlt <= -12.0 && nextSunAlt > -12.0) {
                dawnNauticalInstant = nextInstant
            }
            // Sunrise: Sun rises above 0°
            if (prevSunAlt <= 0.0 && nextSunAlt > 0.0 && sunriseInstant == null) {
                sunriseInstant = nextInstant
            }

            prevSunAlt = nextSunAlt
            currentInstant = nextInstant
        }

        val fallbackStart = nightDate.atTime(21, 30).atZone(zone).toInstant()
        val fallbackEnd = nightDate.plusDays(1).atTime(5, 0).atZone(zone).toInstant()

        val darknessStart = duskAstroInstant ?: duskNauticalInstant ?: sunsetInstant ?: fallbackStart
        val darknessEnd = dawnAstroInstant ?: dawnNauticalInstant ?: sunriseInstant ?: fallbackEnd

        val sunsetText = sunsetInstant?.let { timeFormatter.format(it) } ?: "ca. 19:30"
        val sunriseText = sunriseInstant?.let { timeFormatter.format(it) } ?: "ca. 06:30"
        val darknessText = "${timeFormatter.format(darknessStart)} – ${timeFormatter.format(darknessEnd)} Uhr"

        // Moon calculations
        val midpoint = Instant.ofEpochMilli((darknessStart.toEpochMilli() + darknessEnd.toEpochMilli()) / 2)
        val moonIllum = runCatching { illumination(Body.Moon, midpoint.toAstroTime()) }.getOrNull()
        val moonFraction = moonIllum?.phaseFraction ?: 0.5
        val moonPercent = (moonFraction * 100.0).roundToInt()

        val phaseName = when {
            moonFraction < 0.04 -> "Neumond"
            moonFraction < 0.35 -> "Zunehmende Sichel"
            moonFraction < 0.65 -> "Halbmond"
            moonFraction < 0.96 -> "Dreiviertelmond"
            else -> "Vollmond"
        }

        val moonAltStart = SolarSystemCatalog.horizontal(Body.Moon, observer, darknessStart).altitude
        val moonAltMid = SolarSystemCatalog.horizontal(Body.Moon, observer, midpoint).altitude
        val moonAltEnd = SolarSystemCatalog.horizontal(Body.Moon, observer, darknessEnd).altitude

        val moonSummary = when {
            moonPercent <= 5 -> "Neumond · Kein störendes Mondlicht"
            moonAltStart <= 0 && moonAltMid <= 0 && moonAltEnd <= 0 ->
                "$phaseName ($moonPercent %) · Unter dem Horizont (dunkler Himmel)"
            moonAltStart > 0 && moonAltEnd <= 0 ->
                "$phaseName ($moonPercent %) · Geht in der Nacht unter"
            moonAltStart <= 0 && moonAltEnd > 0 ->
                "$phaseName ($moonPercent %) · Geht in der Nacht auf"
            else ->
                "$phaseName ($moonPercent %) · Steht am Nachthimmel"
        }

        // Weather integration & Window summary
        val weatherNotice: String
        val bestWindowSummary: String

        if (weather != null && weather.forecast.isNotEmpty()) {
            val nightForecasts = weather.forecast.filter {
                val epoch = it.instant.epochSecond
                epoch >= darknessStart.epochSecond - 3600 && epoch <= darknessEnd.epochSecond + 3600
            }
            if (nightForecasts.isNotEmpty()) {
                val avgClouds = nightForecasts.map { it.cloudCover }.average().roundToInt()
                val minClouds = nightForecasts.minOf { it.cloudCover }
                val maxRain = nightForecasts.maxOf { it.rainProbability }
                weatherNotice = "Prognose: ca. $avgClouds % Bewölkung (Minimum $minClouds %) · Regenrisiko $maxRain %"
                val clearHours = nightForecasts.filter { it.cloudCover <= 30 && it.rainProbability <= 25 }
                bestWindowSummary = if (clearHours.isNotEmpty()) {
                    val firstHour = clearHours.first().time
                    val lastHour = clearHours.last().time
                    if (avgClouds <= 20) {
                        "Optimale Bedingungen ab ca. $firstHour bis $lastHour Uhr bei klarem Himmel"
                    } else {
                        "Gute Sicht ab ca. $firstHour bis $lastHour Uhr bei aufgelockerter Bewölkung"
                    }
                } else {
                    "Astronomische Dunkelheit $darknessText · Bewölkung beachten"
                }
            } else {
                weatherNotice = "Wetterdaten liegen außerhalb des Nachtfensters"
                bestWindowSummary = "Astronomische Dunkelheit: $darknessText"
            }
        } else {
            weatherNotice = "Wetter offline · Reine astronomische Dunkelheit ohne Bewölkungsprognose"
            bestWindowSummary = "Astronomische Dunkelheit: $darknessText"
        }

        return TonightWindow(
            nightDateText = nightDateText,
            sunsetText = sunsetText,
            darknessStart = darknessStart,
            darknessEnd = darknessEnd,
            darknessText = darknessText,
            sunrisesText = sunriseText,
            moonPhaseName = phaseName,
            moonPhasePercent = moonPercent,
            moonSummary = moonSummary,
            bestWindowSummary = bestWindowSummary,
            weatherNotice = weatherNotice
        )
    }
}

internal object TonightTargetEngine {

    fun angularDistanceDegrees(ra1Hours: Double, dec1Deg: Double, ra2Hours: Double, dec2Deg: Double): Double {
        val alpha1 = Math.toRadians(ra1Hours * 15.0)
        val delta1 = Math.toRadians(dec1Deg)
        val alpha2 = Math.toRadians(ra2Hours * 15.0)
        val delta2 = Math.toRadians(dec2Deg)
        val cosD = sin(delta1) * sin(delta2) + cos(delta1) * cos(delta2) * cos(alpha1 - alpha2)
        return Math.toDegrees(acos(cosD.coerceIn(-1.0, 1.0)))
    }

    private data class HighlightCandidate(
        val name: String,
        val catalogId: String,
        val raHours: Double,
        val decDegrees: Double,
        val magnitude: Double,
        val objectType: CelestialType,
        val constellation: String,
        val defaultEquipment: ObservationEquipment,
        val highlightTitle: String,
        val observationNotes: String,
        val solarBody: Body? = null
    )

    private val curatedCandidates = listOf(
        HighlightCandidate(
            name = "Saturn",
            catalogId = "Astronomy Engine · Saturn",
            raHours = 0.0, decDegrees = 0.0, magnitude = 0.7,
            objectType = CelestialType.PLANET,
            constellation = "Wassermann",
            defaultEquipment = ObservationEquipment.TELESCOPE,
            highlightTitle = "Saturn & Ringsystem",
            observationNotes = "Der faszinierendste Planet im Fernrohr: Seine Ringe und der hellste Mond Titan sind lohnende Höhepunkte.",
            solarBody = Body.Saturn
        ),
        HighlightCandidate(
            name = "Jupiter",
            catalogId = "Astronomy Engine · Jupiter",
            raHours = 0.0, decDegrees = 0.0, magnitude = -2.4,
            objectType = CelestialType.PLANET,
            constellation = "Stier",
            defaultEquipment = ObservationEquipment.BINOCULARS,
            highlightTitle = "Jupiter & 4 Monde",
            observationNotes = "Hellster Planet der Nacht. Schon im einfachen Fernglas zeigen sich die vier Galileischen Monde Io, Europa, Ganymed und Kallisto.",
            solarBody = Body.Jupiter
        ),
        HighlightCandidate(
            name = "Mars",
            catalogId = "Astronomy Engine · Mars",
            raHours = 0.0, decDegrees = 0.0, magnitude = 0.4,
            objectType = CelestialType.PLANET,
            constellation = "Zwillinge",
            defaultEquipment = ObservationEquipment.TELESCOPE,
            highlightTitle = "Mars – Der Rote Planet",
            observationNotes = "Mit bloßem Auge auffällig rötlich-orange leuchtend; im Teleskop zeigen sich Polkappen und dunkle Oberflächenstrukturen.",
            solarBody = Body.Mars
        ),
        HighlightCandidate(
            name = "Mond",
            catalogId = "Astronomy Engine · Moon",
            raHours = 0.0, decDegrees = 0.0, magnitude = -12.0,
            objectType = CelestialType.MOON,
            constellation = "",
            defaultEquipment = ObservationEquipment.BINOCULARS,
            highlightTitle = "Mondkrater am Terminator",
            observationNotes = "An der Tag-Nacht-Grenze werfen Kraterränder und Gebirgsmassive eindrucksvolle Schatten.",
            solarBody = Body.Moon
        ),
        HighlightCandidate(
            name = "Andromedagalaxie",
            catalogId = "M 31 · NGC 224",
            raHours = 0.712, decDegrees = 41.27, magnitude = 3.4,
            objectType = CelestialType.GALAXY,
            constellation = "Andromeda",
            defaultEquipment = ObservationEquipment.BINOCULARS,
            highlightTitle = "Andromeda-Galaxie (M31)",
            observationNotes = "Mit 2,5 Millionen Lichtjahren das am weitesten entfernte Objekt für das bloße Auge; im Fernglas als ovaler Lichtschimmer sichtbar."
        ),
        HighlightCandidate(
            name = "Plejaden",
            catalogId = "M 45 · Mel022",
            raHours = 3.79, decDegrees = 24.11, magnitude = 1.6,
            objectType = CelestialType.OPEN_CLUSTER,
            constellation = "Stier",
            defaultEquipment = ObservationEquipment.BINOCULARS,
            highlightTitle = "Plejaden / Siebengestirn (M45)",
            observationNotes = "Der bekannteste offene Sternhaufen. Für das bloße Auge als feine Gruppe und im Fernglas als funkelndes Diamantenfeld spektakulär."
        ),
        HighlightCandidate(
            name = "Orionnebel",
            catalogId = "M 42 · NGC 1976",
            raHours = 5.59, decDegrees = -5.39, magnitude = 4.0,
            objectType = CelestialType.NEBULA,
            constellation = "Orion",
            defaultEquipment = ObservationEquipment.BINOCULARS,
            highlightTitle = "Großer Orionnebel (M42)",
            observationNotes = "Sternenwiege aus leuchtendem Gas und Staub. Im Fernglas zart leuchtend, im Teleskop mit Trapezsternen im Zentrum."
        ),
        HighlightCandidate(
            name = "Herkuleshaufen",
            catalogId = "M 13 · NGC 6205",
            raHours = 16.695, decDegrees = 36.46, magnitude = 5.8,
            objectType = CelestialType.GLOBULAR_CLUSTER,
            constellation = "Herkules",
            defaultEquipment = ObservationEquipment.BINOCULARS,
            highlightTitle = "Großer Kugelsternhaufen (M13)",
            observationNotes = "Über 300.000 Sterne auf engem Raum. Im Fernglas als kugeliger Nebelball, im Teleskop am Rand in Einzelsterne aufgelöst."
        ),
        HighlightCandidate(
            name = "Praesepe",
            catalogId = "M 44 · NGC 2632",
            raHours = 8.67, decDegrees = 19.67, magnitude = 3.7,
            objectType = CelestialType.OPEN_CLUSTER,
            constellation = "Krebs",
            defaultEquipment = ObservationEquipment.BINOCULARS,
            highlightTitle = "Krippe / Praesepe (M44)",
            observationNotes = "Prächtiger offener Sternhaufen im Krebs, ideal für das Fernglas mit weitem Blickfeld."
        ),
        HighlightCandidate(
            name = "Doppelsternhaufen",
            catalogId = "NGC 869",
            raHours = 2.33, decDegrees = 57.15, magnitude = 3.7,
            objectType = CelestialType.OPEN_CLUSTER,
            constellation = "Perseus",
            defaultEquipment = ObservationEquipment.BINOCULARS,
            highlightTitle = "Doppelsternhaufen h & χ Persei",
            observationNotes = "Zwei dicht benachbarte, funkelnde Sternhaufen; eines der lohnendsten Ziele für jedes Fernglas."
        ),
        HighlightCandidate(
            name = "Ringnebel",
            catalogId = "M 57 · NGC 6720",
            raHours = 18.89, decDegrees = 33.03, magnitude = 8.8,
            objectType = CelestialType.PLANETARY_NEBULA,
            constellation = "Leier",
            defaultEquipment = ObservationEquipment.TELESCOPE,
            highlightTitle = "Ringnebel (M57)",
            observationNotes = "Überrest eines sonnenähnlichen Sterns; im Teleskop unverkennbar als zarter Rauchring geformt."
        ),
        HighlightCandidate(
            name = "Hantelnebel",
            catalogId = "M 27 · NGC 6853",
            raHours = 19.99, decDegrees = 22.72, magnitude = 7.4,
            objectType = CelestialType.PLANETARY_NEBULA,
            constellation = "Füchschen",
            defaultEquipment = ObservationEquipment.BINOCULARS,
            highlightTitle = "Hantelnebel (M27)",
            observationNotes = "Hellster planetarischer Nebel am Himmel, im Fernglas als sanduhrförmiger Nebel erkennbar."
        ),
        HighlightCandidate(
            name = "Bodes Galaxie",
            catalogId = "M 81 · NGC 3031",
            raHours = 9.93, decDegrees = 69.07, magnitude = 6.9,
            objectType = CelestialType.GALAXY,
            constellation = "Großer Bär",
            defaultEquipment = ObservationEquipment.BINOCULARS,
            highlightTitle = "Galaxienpaar M81 & M82",
            observationNotes = "Zwei nahe beieinander liegende Galaxien im Großen Bären, die gleichzeitig ins Fernglasfeld passen."
        ),
        HighlightCandidate(
            name = "Albireo",
            catalogId = "HIP 95947",
            raHours = 19.51, decDegrees = 27.96, magnitude = 3.1,
            objectType = CelestialType.STAR,
            constellation = "Schwan",
            defaultEquipment = ObservationEquipment.BINOCULARS,
            highlightTitle = "Albireo – Goldblauer Doppelstern",
            observationNotes = "Kopfstern des Schwans: Ein goldgelber und ein saphirblauer Stern bilden ein farbenprächtiges Doppelsternpaar."
        ),
        HighlightCandidate(
            name = "Wega",
            catalogId = "HIP 91262",
            raHours = 18.62, decDegrees = 38.78, magnitude = 0.03,
            objectType = CelestialType.STAR,
            constellation = "Leier",
            defaultEquipment = ObservationEquipment.NAKED_EYE,
            highlightTitle = "Wega in der Leier",
            observationNotes = "Strahlend weiß-blauer Hauptstern des Sommerdreiecks und Referenzstern für astronomische Helligkeiten."
        ),
        HighlightCandidate(
            name = "Arktur",
            catalogId = "HIP 69673",
            raHours = 14.26, decDegrees = 19.18, magnitude = -0.05,
            objectType = CelestialType.STAR,
            constellation = "Bärenhüter",
            defaultEquipment = ObservationEquipment.NAKED_EYE,
            highlightTitle = "Arktur – Roter Riese",
            observationNotes = "Hellster Stern des Nordhimmels mit markanter orange-goldener Glut."
        ),
        HighlightCandidate(
            name = "Sirius",
            catalogId = "HIP 32349",
            raHours = 6.75, decDegrees = -16.72, magnitude = -1.46,
            objectType = CelestialType.STAR,
            constellation = "Großer Hund",
            defaultEquipment = ObservationEquipment.NAKED_EYE,
            highlightTitle = "Sirius – Hellster Fixstern",
            observationNotes = "Der hellste Stern am gesamten irdischen Nachthimmel, der im Winterhimmel spektakulär funkelt."
        )
    )

    fun evaluate(
        observer: GeoPoint,
        window: TonightWindow,
        now: Instant,
        equipmentFilter: ObservationEquipment = ObservationEquipment.ALL,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<TonightTarget> {
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN).withZone(zone)
        val sampleStart = window.darknessStart
        val sampleEnd = window.darknessEnd
        val stepMinutes = 20L

        // Generate sample instants across the night
        val sampleInstants = mutableListOf<Instant>()
        var cur = sampleStart
        while (!cur.isAfter(sampleEnd)) {
            sampleInstants.add(cur)
            cur = cur.plusSeconds(stepMinutes * 60)
        }
        if (sampleInstants.isEmpty()) sampleInstants.add(now)

        val results = mutableListOf<TonightTarget>()

        for (candidate in curatedCandidates) {
            // Build CelestialObject
            val baseObj = if (candidate.solarBody != null) {
                SolarSystemCatalog.at(observer, now).firstOrNull { it.solarBody == candidate.solarBody }
            } else {
                CelestialObject(
                    name = candidate.name,
                    catalogId = candidate.catalogId,
                    raHours = candidate.raHours,
                    decDegrees = candidate.decDegrees,
                    magnitude = candidate.magnitude,
                    distanceLightYears = 0.0,
                    spectralClass = "",
                    constellation = candidate.constellation,
                    objectType = candidate.objectType,
                    astronomyDescription = candidate.observationNotes
                )
            } ?: continue

            // Sample altitudes
            var maxAlt = -90.0
            var peakTime = sampleStart

            for (instant in sampleInstants) {
                val coords = coordinatesAt(baseObj, observer, instant)
                if (coords.altitude > maxAlt) {
                    maxAlt = coords.altitude
                    peakTime = instant
                }
            }

            // Reject objects that remain too low in the sky (less than 16° above horizon)
            if (maxAlt < 16.0) continue

            val currentCoords = coordinatesAt(baseObj, observer, now)

            // Calculate moon separation at peak time
            val moonPos = SolarSystemCatalog.horizontal(Body.Moon, observer, peakTime)
            val moonEq = runCatching {
                io.github.cosinekitty.astronomy.equator(
                    Body.Moon,
                    peakTime.toAstroTime(),
                    observer.toAstroObserver(),
                    io.github.cosinekitty.astronomy.EquatorEpoch.OfDate,
                    io.github.cosinekitty.astronomy.Aberration.Corrected
                )
            }.getOrNull()

            val isMoon = baseObj.solarBody == Body.Moon
            val moonSep = if (isMoon) {
                -1.0
            } else if (moonEq != null) {
                angularDistanceDegrees(baseObj.raHours, baseObj.decDegrees, moonEq.ra, moonEq.dec)
            } else {
                75.0
            }

            // Quality score calculation (0..100)
            var score = 0
            // 1. Altitude: higher is better (less atmospheric extinction)
            score += ((maxAlt / 90.0) * 45.0).roundToInt().coerceIn(0, 45)

            // 2. Moon influence
            if (isMoon) {
                score += 30 // Target is the Moon itself
            } else if (window.moonPhasePercent <= 15 || moonPos.altitude <= 0.0) {
                score += 30 // Dark moonless sky
            } else {
                if (baseObj.solarBody != null) {
                    score += 25 // Planets tolerate moonlight very well
                } else {
                    if (moonSep >= 60.0) score += 20
                    else if (moonSep >= 35.0) score += 10
                    else score -= 15 // Washed out by glare
                }
            }

            // 3. Brightness
            val magScore = ((10.0 - baseObj.magnitude).coerceAtLeast(0.0) * 2.0).roundToInt().coerceIn(0, 20)
            score += magScore

            // 4. Evening bonus (prioritize convenient observing before 01:00 AM)
            val peakHour = peakTime.atZone(zone).hour
            if (peakHour in 20..24 || peakHour == 0) score += 5

            val finalScore = score.coerceIn(5, 99)

            val peakTimeText = timeFormatter.format(peakTime)
            val reason = if (isMoon) {
                "Höchster Stand um $peakTimeText Uhr auf ${maxAlt.roundToInt()}° Höhe. ${candidate.observationNotes}"
            } else {
                val moonText = when {
                    moonPos.altitude <= 0.0 -> "Mond unter dem Horizont"
                    moonSep >= 60.0 -> "Mondabstand ${moonSep.roundToInt()}° (ungestört)"
                    moonSep >= 35.0 -> "Mondabstand ${moonSep.roundToInt()}°"
                    else -> "Mondnah (${moonSep.roundToInt()}°)"
                }
                "Höchster Stand um $peakTimeText Uhr auf ${maxAlt.roundToInt()}° Höhe · $moonText. ${candidate.observationNotes}"
            }

            results.add(
                TonightTarget(
                    objectData = baseObj,
                    equipment = candidate.defaultEquipment,
                    peakInstant = peakTime,
                    peakAltitudeDegrees = maxAlt,
                    currentAltitudeDegrees = currentCoords.altitude,
                    moonSeparationDegrees = moonSep,
                    qualityScore = finalScore,
                    highlightTitle = candidate.highlightTitle,
                    reason = reason
                )
            )
        }

        // Sort descending by score
        val sorted = results.sortedByDescending { it.qualityScore }

        // Filter by equipment
        return when (equipmentFilter) {
            ObservationEquipment.ALL -> sorted
            ObservationEquipment.NAKED_EYE -> sorted.filter { it.equipment == ObservationEquipment.NAKED_EYE }
            ObservationEquipment.BINOCULARS -> sorted.filter {
                it.equipment == ObservationEquipment.BINOCULARS || it.equipment == ObservationEquipment.NAKED_EYE
            }
            ObservationEquipment.TELESCOPE -> sorted.filter {
                it.equipment == ObservationEquipment.TELESCOPE ||
                    it.equipment == ObservationEquipment.BINOCULARS ||
                    it.objectData.objectType == CelestialType.PLANET ||
                    it.objectData.objectType == CelestialType.MOON
            }
        }
    }
}

private fun Instant.toAstroTime() = io.github.cosinekitty.astronomy.Time.fromMillisecondsSince1970(toEpochMilli())
private fun GeoPoint.toAstroObserver() = io.github.cosinekitty.astronomy.Observer(latitude, longitude, altitudeMeters)

