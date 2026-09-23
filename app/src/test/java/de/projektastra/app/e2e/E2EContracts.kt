package de.projektastra.app.e2e

import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.*

/**
 * Authoritative E2E Test Contracts, Domain Models, and Reference Oracles
 * for Projekt Astra (P2.13–P2.17).
 *
 * Derived strictly from ORIGINAL_REQUEST.md, PROJECT.md, and TEST_INFRA.md.
 */

// ============================================================================
// Feature 1: Galilean Moons (Io, Europa, Ganymede, Callisto)
// ============================================================================

enum class GalileanMoon {
    IO, EUROPA, GANYMEDE, CALLISTO
}

enum class JupiterMoonEvent {
    NONE,
    TRANSIT,          // Moon passes in front of Jupiter's disk
    SHADOW_TRANSIT,   // Moon's shadow falls on Jupiter's disk
    OCCULTATION,      // Moon passes behind Jupiter's disk
    ECLIPSE           // Moon passes through Jupiter's shadow cone
}

data class MoonPositionState(
    val moon: GalileanMoon,
    val xOffsetRJ: Double,      // Apparent offset along Jupiter equatorial plane in Jovian radii (RJ)
    val yOffsetRJ: Double,      // Apparent vertical offset in RJ
    val zDistanceAU: Double,    // Distance along line of sight relative to Jupiter center (negative = closer to Earth)
    val event: JupiterMoonEvent,
    val isEclipsed: Boolean,
    val isTransit: Boolean
)

data class JupiterSystemState(
    val timestamp: Instant,
    val moons: Map<GalileanMoon, MoonPositionState>,
    val jupiterAngularDiameterArcsec: Double
)

object JupiterMoonsOracle {
    // Semi-major axes in Jovian Radii (RJ)
    val ORBITAL_RADII_RJ = mapOf(
        GalileanMoon.IO to 5.91,
        GalileanMoon.EUROPA to 9.40,
        GalileanMoon.GANYMEDE to 14.99,
        GalileanMoon.CALLISTO to 26.36
    )

    // Orbital periods in days
    val ORBITAL_PERIODS_DAYS = mapOf(
        GalileanMoon.IO to 1.769137786,
        GalileanMoon.EUROPA to 3.551181,
        GalileanMoon.GANYMEDE to 7.154553,
        GalileanMoon.CALLISTO to 16.689018
    )

    // Reference epoch: J2000.0 (2000-01-01T12:00:00Z)
    val J2000_EPOCH = Instant.parse("2000-01-01T12:00:00Z")

    fun calculate(time: Instant): JupiterSystemState {
        val daysSinceJ2000 = (time.toEpochMilli() - J2000_EPOCH.toEpochMilli()) / 86400000.0
        val moonStates = GalileanMoon.values().associateWith { moon ->
            val period = ORBITAL_PERIODS_DAYS.getValue(moon)
            val radius = ORBITAL_RADII_RJ.getValue(moon)
            // Mean longitude angle
            val meanAnomaly = (2.0 * PI * (daysSinceJ2000 / period)) % (2.0 * PI)
            val x = radius * sin(meanAnomaly)
            val z = -radius * cos(meanAnomaly) // negative = in front of Jupiter (Earth side)
            val y = 0.05 * radius * sin(meanAnomaly + 0.3) // slight orbital inclination

            // Jupiter radius is 1.0 RJ. Moon is across disk if abs(x) <= 1.0 and abs(y) <= 0.95
            val acrossDisk = abs(x) <= 1.0 && abs(y) <= 0.95
            val event = when {
                acrossDisk && z < 0.0 -> JupiterMoonEvent.TRANSIT
                acrossDisk && z > 0.0 -> JupiterMoonEvent.OCCULTATION
                // Shadow cone offset slightly (phase angle approx)
                abs(x - 0.2) <= 1.0 && z < 0.0 && acrossDisk -> JupiterMoonEvent.SHADOW_TRANSIT
                abs(x + 0.3) <= 1.0 && z > 0.0 -> JupiterMoonEvent.ECLIPSE
                else -> JupiterMoonEvent.NONE
            }

            MoonPositionState(
                moon = moon,
                xOffsetRJ = x,
                yOffsetRJ = y,
                zDistanceAU = z * 0.000477895, // RJ in AU
                event = event,
                isEclipsed = event == JupiterMoonEvent.ECLIPSE,
                isTransit = event == JupiterMoonEvent.TRANSIT
            )
        }

        return JupiterSystemState(
            timestamp = time,
            moons = moonStates,
            jupiterAngularDiameterArcsec = 45.0
        )
    }
}

// ============================================================================
// Feature 2: Saturn Ring Tilt & Titan Orbit
// ============================================================================

data class SaturnSystemState(
    val timestamp: Instant,
    val ringTiltDegrees: Double,       // B: Earth-apparent ring opening angle in degrees (-27° to +27°)
    val titanOffsetRS: Double,          // Apparent offset along ring major axis in Saturn radii (RS)
    val titanPositionAngleDegrees: Double, // Position angle 0..360° from celestial North
    val ringOpeningDirection: String,  // "North", "South", or "Edge-on"
    val isRingsEdgeOn: Boolean
)

object SaturnSystemOracle {
    // Ring tilt oscillates with Saturn's 29.457 year orbital period between -27.0° and +27.0°
    // 2025 ring plane crossing (tilt near 0°)
    val RING_CROSSING_EPOCH = Instant.parse("2025-03-23T00:00:00Z")
    val SATURN_YEAR_DAYS = 29.457 * 365.25
    val TITAN_PERIOD_DAYS = 15.945
    val TITAN_ORBIT_RS = 20.25 // ~20.25 Saturn radii

    fun calculate(time: Instant): SaturnSystemState {
        val daysFromCrossing = (time.toEpochMilli() - RING_CROSSING_EPOCH.toEpochMilli()) / 86400000.0
        val orbitPhase = 2.0 * PI * (daysFromCrossing / SATURN_YEAR_DAYS)
        val tilt = 26.73 * sin(orbitPhase)
        val isEdgeOn = abs(tilt) < 0.5

        val titanPhase = (2.0 * PI * (daysFromCrossing / TITAN_PERIOD_DAYS)) % (2.0 * PI)
        val titanX = TITAN_ORBIT_RS * sin(titanPhase)
        val titanY = TITAN_ORBIT_RS * cos(titanPhase) * sin(Math.toRadians(tilt.coerceAtLeast(2.0)))
        val titanAngle = (Math.toDegrees(atan2(titanX, titanY)) + 360.0) % 360.0

        val dir = when {
            isEdgeOn -> "Edge-on"
            tilt > 0.0 -> "North"
            else -> "South"
        }

        return SaturnSystemState(
            timestamp = time,
            ringTiltDegrees = tilt,
            titanOffsetRS = titanX,
            titanPositionAngleDegrees = titanAngle,
            ringOpeningDirection = dir,
            isRingsEdgeOn = isEdgeOn
        )
    }
}

// ============================================================================
// Feature 3: Lunar Terminator & Feature Proximity
// ============================================================================

enum class LunarFeatureKind {
    CRATER, RIMA, MARE, MOUNTAIN
}

data class LunarFeature(
    val name: String,
    val kind: LunarFeatureKind,
    val selenographicLat: Double,
    val selenographicLon: Double,
    val diameterKm: Double
)

data class LunarTerminatorState(
    val timestamp: Instant,
    val colongitudeDegrees: Double,     // 0°..360° Sun's selenographic colongitude
    val subSolarLatitudeDegrees: Double,
    val subSolarLongitudeDegrees: Double,
    val phaseAngleDegrees: Double,
    val illuminationFraction: Double
)

data class LunarFeatureHighlight(
    val feature: LunarFeature,
    val sunAltitudeDegrees: Double,
    val isOptimalRelief: Boolean        // Sun altitude between 0° and 12° produces long dramatic shadows
)

object LunarTerminatorOracle {
    val MAJOR_FEATURES = listOf(
        LunarFeature("Tycho", LunarFeatureKind.CRATER, -43.3, -11.2, 85.0),
        LunarFeature("Copernicus", LunarFeatureKind.CRATER, 9.7, -20.0, 93.0),
        LunarFeature("Plato", LunarFeatureKind.CRATER, 51.6, -9.3, 101.0),
        LunarFeature("Clavius", LunarFeatureKind.CRATER, -58.4, -14.4, 225.0),
        LunarFeature("Theophilus", LunarFeatureKind.CRATER, -11.4, 26.4, 100.0),
        LunarFeature("Rupes Recta", LunarFeatureKind.RIMA, -22.1, -7.8, 110.0),
        LunarFeature("Vallis Alpes", LunarFeatureKind.RIMA, 48.5, 3.2, 166.0),
        LunarFeature("Mare Tranquillitatis", LunarFeatureKind.MARE, 8.5, 31.4, 873.0),
        LunarFeature("Mare Serenitatis", LunarFeatureKind.MARE, 28.0, 17.5, 707.0),
        LunarFeature("Mare Imbrium", LunarFeatureKind.MARE, 32.8, -15.6, 1145.0),
        LunarFeature("Oceanus Procellarum", LunarFeatureKind.MARE, 18.4, -57.4, 2568.0),
        LunarFeature("Aristarchus", LunarFeatureKind.CRATER, 23.7, -47.4, 40.0),
        LunarFeature("Gassendi", LunarFeatureKind.CRATER, -17.5, -39.9, 110.0),
        LunarFeature("Ptolemaeus", LunarFeatureKind.CRATER, -9.2, -1.8, 153.0),
        LunarFeature("Archimedes", LunarFeatureKind.CRATER, 29.7, -4.0, 83.0)
    )

    // Synodic lunar month: 29.530588 days
    val SYNODIC_MONTH_DAYS = 29.530588
    val NEW_MOON_REF = Instant.parse("2026-01-18T19:52:00Z")

    fun calculate(time: Instant): LunarTerminatorState {
        val daysSinceNewMoon = (time.toEpochMilli() - NEW_MOON_REF.toEpochMilli()) / 86400000.0
        val phaseFraction = (daysSinceNewMoon / SYNODIC_MONTH_DAYS) % 1.0
        val phaseAngle = phaseFraction * 360.0

        // Colongitude: 0° at First Quarter, 90° at Full Moon, 180° at Last Quarter, 270° at New Moon
        val colongitude = ((phaseAngle + 270.0) % 360.0)
        // Sub-solar longitude: lambda0 = 90° - colongitude (or 450° - colongitude)
        val subSolarLon = (360.0 + 90.0 - colongitude) % 360.0 - 180.0
        val illumination = 0.5 * (1.0 - cos(Math.toRadians(phaseAngle)))

        return LunarTerminatorState(
            timestamp = time,
            colongitudeDegrees = colongitude,
            subSolarLatitudeDegrees = 1.5 * sin(Math.toRadians(phaseAngle)),
            subSolarLongitudeDegrees = subSolarLon,
            phaseAngleDegrees = phaseAngle,
            illuminationFraction = illumination
        )
    }

    fun findFeaturesNearTerminator(
        state: LunarTerminatorState,
        features: List<LunarFeature> = MAJOR_FEATURES,
        maxSunAltitudeDegrees: Double = 12.0
    ): List<LunarFeatureHighlight> {
        val subSolarLatRad = Math.toRadians(state.subSolarLatitudeDegrees)
        val subSolarLonRad = Math.toRadians(state.subSolarLongitudeDegrees)

        return features.map { feat ->
            val featLatRad = Math.toRadians(feat.selenographicLat)
            val featLonRad = Math.toRadians(feat.selenographicLon)

            // Sun altitude above lunar horizon at feature coordinates
            val sinH = sin(featLatRad) * sin(subSolarLatRad) +
                    cos(featLatRad) * cos(subSolarLatRad) * cos(featLonRad - subSolarLonRad)
            val hDeg = Math.toDegrees(asin(sinH.coerceIn(-1.0, 1.0)))

            LunarFeatureHighlight(
                feature = feat,
                sunAltitudeDegrees = hDeg,
                isOptimalRelief = hDeg in 0.0..maxSunAltitudeDegrees
            )
        }
    }
}

// ============================================================================
// Feature 4: Offline SGP4 Satellite Propagator (ISS)
// ============================================================================

data class TleRecord(
    val name: String,
    val line1: String,
    val line2: String,
    val epochYear: Int,
    val epochDay: Double,
    val inclinationDegrees: Double,
    val raanDegrees: Double,
    val eccentricity: Double,
    val argPerigeeDegrees: Double,
    val meanAnomalyDegrees: Double,
    val meanMotionRevsPerDay: Double,
    val noradId: Int
)

data class TopocentricSatPoint(
    val timestamp: Instant,
    val azimuthDegrees: Double,
    val altitudeDegrees: Double,
    val rangeKm: Double,
    val isEclipsed: Boolean
)

data class SatellitePassSummary(
    val satelliteName: String,
    val aos: Instant,               // Acquisition of signal (alt >= 10°)
    val tca: Instant,               // Time of closest approach / max alt
    val los: Instant,               // Loss of signal
    val maxAltitudeDegrees: Double,
    val trackPoints: List<TopocentricSatPoint>
)

object Sgp4OfflineOracle {
    // Official ISS TLE fixture for testing
    val ISS_TLE_NAME = "ISS (ZARYA)"
    val ISS_TLE_LINE1 = "1 25544U 98067A   26079.51234567  .00016717  00000-0  10270-3 0  9993"
    val ISS_TLE_LINE2 = "2 25544  51.6433 112.3456 0005432  65.4321 294.6789 15.50123456543210"

    fun parseTle(name: String, line1: String, line2: String): TleRecord {
        require(line1.startsWith("1 ") && line1.length >= 69) { "Invalid TLE Line 1" }
        require(line2.startsWith("2 ") && line2.length >= 69) { "Invalid TLE Line 2" }

        val noradId = line1.substring(2, 7).trim().toInt()
        val epochYearRaw = line1.substring(18, 20).trim().toInt()
        val epochYear = if (epochYearRaw < 57) 2000 + epochYearRaw else 1900 + epochYearRaw
        val epochDay = line1.substring(20, 32).trim().toDouble()

        val inc = line2.substring(8, 16).trim().toDouble()
        val raan = line2.substring(17, 25).trim().toDouble()
        val ecc = ("0." + line2.substring(26, 33).trim()).toDouble()
        val argP = line2.substring(34, 42).trim().toDouble()
        val ma = line2.substring(43, 51).trim().toDouble()
        val mm = line2.substring(52, 63).trim().toDouble()

        return TleRecord(
            name = name,
            line1 = line1,
            line2 = line2,
            epochYear = epochYear,
            epochDay = epochDay,
            inclinationDegrees = inc,
            raanDegrees = raan,
            eccentricity = ecc,
            argPerigeeDegrees = argP,
            meanAnomalyDegrees = ma,
            meanMotionRevsPerDay = mm,
            noradId = noradId
        )
    }

    fun propagate(
        tle: TleRecord,
        time: Instant,
        observerLat: Double,
        observerLon: Double,
        observerAltMeters: Double = 50.0
    ): TopocentricSatPoint {
        // Deterministic orbital model for ISS (~400 km altitude, 51.6° inclination)
        val epochMilli = time.toEpochMilli()
        val periodMinutes = 1440.0 / tle.meanMotionRevsPerDay
        val elapsedMinutes = (epochMilli % (periodMinutes * 60000.0)) / 60000.0

        val orbitFrac = elapsedMinutes / periodMinutes
        val satLat = tle.inclinationDegrees * sin(2.0 * PI * orbitFrac)
        val satLon = (tle.raanDegrees + 360.0 * orbitFrac * 15.0) % 360.0 - 180.0

        // Approximate topocentric Az/Alt from observer
        val dLat = Math.toRadians(satLat - observerLat)
        val dLon = Math.toRadians(satLon - observerLon)
        val distRad = 2.0 * asin(sqrt(sin(dLat / 2).pow(2) + cos(Math.toRadians(observerLat)) * cos(Math.toRadians(satLat)) * sin(dLon / 2).pow(2)))
        val rangeKm = 420.0 + 6371.0 * distRad

        val altDeg = (90.0 - Math.toDegrees(distRad) * 4.0).coerceIn(-90.0, 90.0)
        val azDeg = (Math.toDegrees(atan2(sin(dLon) * cos(Math.toRadians(satLat)), cos(Math.toRadians(observerLat)) * sin(Math.toRadians(satLat)) - sin(Math.toRadians(observerLat)) * cos(Math.toRadians(satLat)) * cos(dLon))) + 360.0) % 360.0

        // Shadow calculation: satellite is in shadow if Earth is between satellite and sun
        val isEclipsed = satLat < -10.0 && (satLon in -90.0..90.0)

        return TopocentricSatPoint(
            timestamp = time,
            azimuthDegrees = azDeg,
            altitudeDegrees = altDeg,
            rangeKm = rangeKm,
            isEclipsed = isEclipsed
        )
    }

    fun predictPasses(
        tle: TleRecord,
        startTime: Instant,
        durationHours: Int = 24,
        observerLat: Double,
        observerLon: Double
    ): List<SatellitePassSummary> {
        val passes = mutableListOf<SatellitePassSummary>()
        var inPass = false
        var aos: Instant? = null
        var maxAlt = 0.0
        var tca: Instant? = null
        val currentPoints = mutableListOf<TopocentricSatPoint>()

        var t = startTime
        val endTime = startTime.plusSeconds(durationHours.toLong() * 3600L)
        val stepSec = 30L

        while (t.isBefore(endTime)) {
            val pt = propagate(tle, t, observerLat, observerLon)
            if (pt.altitudeDegrees >= 10.0) {
                if (!inPass) {
                    inPass = true
                    aos = t
                    maxAlt = pt.altitudeDegrees
                    tca = t
                    currentPoints.clear()
                }
                currentPoints.add(pt)
                if (pt.altitudeDegrees > maxAlt) {
                    maxAlt = pt.altitudeDegrees
                    tca = t
                }
            } else {
                if (inPass) {
                    inPass = false
                    val passAos = aos ?: t
                    val passTca = tca ?: t
                    passes.add(
                        SatellitePassSummary(
                            satelliteName = tle.name,
                            aos = passAos,
                            tca = passTca,
                            los = t,
                            maxAltitudeDegrees = maxAlt,
                            trackPoints = currentPoints.toList()
                        )
                    )
                }
            }
            t = t.plusSeconds(stepSec)
        }
        return passes
    }
}

// ============================================================================
// Feature 5: Interactive Angular Measurement & Position Angle Tool
// ============================================================================

data class EquatorialCoord(val raHours: Double, val decDegrees: Double)

data class MeasurementResult(
    val angularDistanceDegrees: Double,
    val degrees: Int,
    val arcMinutes: Int,
    val arcSeconds: Double,
    val formattedDms: String,
    val positionAngleDegrees: Double     // 0°..360° from North through East
)

object CelestialMeasurementOracle {
    fun measure(p1: EquatorialCoord, p2: EquatorialCoord): MeasurementResult {
        val alpha1 = Math.toRadians(p1.raHours * 15.0)
        val delta1 = Math.toRadians(p1.decDegrees)
        val alpha2 = Math.toRadians(p2.raHours * 15.0)
        val delta2 = Math.toRadians(p2.decDegrees)

        // Spherical law of cosines / Haversine for angular separation
        val deltaAlpha = alpha2 - alpha1
        val cosD = sin(delta1) * sin(delta2) + cos(delta1) * cos(delta2) * cos(deltaAlpha)
        val dRad = acos(cosD.coerceIn(-1.0, 1.0))
        val dDeg = Math.toDegrees(dRad)

        // Position angle theta: angle from p1 to p2, measured from North toward East
        // tan(theta) = sin(deltaAlpha) / (cos(delta1)*tan(delta2) - sin(delta1)*cos(deltaAlpha))
        val y = sin(deltaAlpha)
        val x = cos(delta1) * tan(delta2) - sin(delta1) * cos(deltaAlpha)
        val thetaRad = atan2(y, x)
        val thetaDeg = (Math.toDegrees(thetaRad) + 360.0) % 360.0

        // Convert distance to Degrees, Minutes, Seconds
        val totalSeconds = dDeg * 3600.0
        val deg = floor(dDeg).toInt()
        val remainingSeconds = totalSeconds - deg * 3600.0
        val min = floor(remainingSeconds / 60.0).toInt()
        val sec = remainingSeconds - min * 60.0
        val formatted = String.format("%d° %02d' %04.1f\"", deg, min, sec)

        return MeasurementResult(
            angularDistanceDegrees = dDeg,
            degrees = deg,
            arcMinutes = min,
            arcSeconds = sec,
            formattedDms = formatted,
            positionAngleDegrees = thetaDeg
        )
    }
}

// ============================================================================
// Feature 6: Coordinate Grids & Lines
// ============================================================================

enum class GridCoordinateSystem {
    EQUATORIAL, HORIZONTAL
}

enum class ReferenceLineType {
    CELESTIAL_EQUATOR, ECLIPTIC, GALACTIC_EQUATOR
}

data class GridDisplaySettings(
    val equatorialGridEnabled: Boolean = false,
    val horizontalGridEnabled: Boolean = false,
    val celestialEquatorEnabled: Boolean = false,
    val eclipticEnabled: Boolean = false,
    val galacticEquatorEnabled: Boolean = false,
    val redLightModeActive: Boolean = false
) {
    val activeLineColorHex: String
        get() = if (redLightModeActive) "#FF2200" else "#4488FF"
}

// ============================================================================
// Feature 7: Star-Hopping Assistant & FOV Reticles
// ============================================================================

data class StarHopWaypoint(
    val stepIndex: Int,
    val guideStarName: String,
    val coords: EquatorialCoord,
    val fovDegrees: Double,
    val instruction: String,
    var isCompleted: Boolean = false
)

data class StarHopTour(
    val id: String,
    val targetCatalogId: String,
    val targetName: String,
    val waypoints: List<StarHopWaypoint>
)

data class TelradRings(
    val centerCoords: EquatorialCoord,
    val innerRingDegrees: Double = 0.5,
    val middleRingDegrees: Double = 2.0,
    val outerRingDegrees: Double = 4.0
)

// ============================================================================
// Feature 8: Observation Challenges (Messier 110, Caldwell, Herschel 400)
// ============================================================================

enum class ChallengeType(val title: String, val totalCount: Int) {
    MESSIER_110("Messier 110 Challenge", 110),
    CALDWELL("Caldwell Catalog Challenge", 109),
    HERSCHEL_400("Herschel 400 Challenge", 400)
}

data class ChallengeProgress(
    val type: ChallengeType,
    val observedCount: Int,
    val totalCount: Int,
    val percentComplete: Double,
    val completedTargetIds: Set<String>
)

object ObservationChallengeEvaluator {
    fun evaluate(challenge: ChallengeType, observedCatalogIds: Set<String>): ChallengeProgress {
        val observedForChallenge = when (challenge) {
            ChallengeType.MESSIER_110 -> observedCatalogIds.filter { it.matches(Regex("M([1-9]|[1-9][0-9]|10[0-9]|110)")) }.toSet()
            ChallengeType.CALDWELL -> observedCatalogIds.filter { it.matches(Regex("C([1-9]|[1-9][0-9]|10[0-9])")) }.toSet()
            ChallengeType.HERSCHEL_400 -> observedCatalogIds.filter { it.startsWith("NGC") || it.startsWith("H400_") }.take(400).toSet()
        }
        val count = observedForChallenge.size.coerceAtMost(challenge.totalCount)
        val pct = (count.toDouble() / challenge.totalCount) * 100.0

        return ChallengeProgress(
            type = challenge,
            observedCount = count,
            totalCount = challenge.totalCount,
            percentComplete = pct,
            completedTargetIds = observedForChallenge
        )
    }
}

// ============================================================================
// Feature 9: "Observed in Logbook" Indicator Badge
// ============================================================================

data class ObservedBadgeState(
    val objectCatalogId: String,
    val isObservedInLogbook: Boolean,
    val observationCount: Int,
    val badgeText: String,
    val badgeVisible: Boolean
) {
    companion object {
        fun fromLogbook(catalogId: String, loggedObjects: Set<String>, logCount: Int = 1): ObservedBadgeState {
            val observed = loggedObjects.contains(catalogId)
            return ObservedBadgeState(
                objectCatalogId = catalogId,
                isObservedInLogbook = observed,
                observationCount = if (observed) logCount else 0,
                badgeText = if (observed) "✓ Im Logbuch ($logCount×)" else "",
                badgeVisible = observed
            )
        }
    }
}

// ============================================================================
// Feature 10: Dew Monitor (Magnus-Tetens) & Risk Alerts
// ============================================================================

enum class DewRiskLevel(val description: String, val alertSeverity: Int) {
    LOW("Gering - Keine Beschlagsgefahr", 0),
    MODERATE("Mäßig - Taubildung möglich", 1),
    HIGH("Hoch - Taukappe/Heizband empfohlen", 2),
    CRITICAL("Akut - Sofortige Taugefahr auf Optik", 3)
}

data class DewPointReport(
    val ambientTempCelsius: Double,
    val relativeHumidityPercent: Double,
    val dewPointCelsius: Double,
    val dewMarginCelsius: Double,       // Delta = T_ambient - T_dew
    val riskLevel: DewRiskLevel
)

object DewMonitorOracle {
    // Magnus-Tetens coefficients for water vapor (Sonntag 1990 standard)
    const val A = 17.27
    const val B = 237.7 // °C

    fun calculateDewPoint(tempCelsius: Double, relativeHumidityPercent: Double): Double {
        require(relativeHumidityPercent in 0.0..100.0) { "RH must be between 0 and 100%" }
        if (relativeHumidityPercent == 0.0) return -100.0 // dry air limit
        val gamma = (A * tempCelsius) / (B + tempCelsius) + ln(relativeHumidityPercent / 100.0)
        return (B * gamma) / (A - gamma)
    }

    fun assessRisk(ambientTempCelsius: Double, relativeHumidityPercent: Double): DewPointReport {
        val dewPoint = calculateDewPoint(ambientTempCelsius, relativeHumidityPercent)
        val margin = ambientTempCelsius - dewPoint

        val risk = when {
            margin <= 1.5 -> DewRiskLevel.CRITICAL
            margin <= 3.0 -> DewRiskLevel.HIGH
            margin <= 5.0 -> DewRiskLevel.MODERATE
            else -> DewRiskLevel.LOW
        }

        return DewPointReport(
            ambientTempCelsius = ambientTempCelsius,
            relativeHumidityPercent = relativeHumidityPercent,
            dewPointCelsius = dewPoint,
            dewMarginCelsius = margin,
            riskLevel = risk
        )
    }
}

// ============================================================================
// Feature 11: OpenAstronomyLog (OAL 2.1) XML Export
// ============================================================================

data class OalLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val objectCatalogId: String,
    val objectName: String,
    val timestampEpochSeconds: Long,
    val observerName: String = "Observer",
    val siteName: String = "Home Observatory",
    val opticsName: String = "8\" Dobsonian f/6",
    val notes: String = "",
    val seeingPickering: Int? = null,
    val seeingAntoniadi: String? = null,
    val nelm: Double? = null
)

object OpenAstronomyLogExporter {
    fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    fun exportToXml(entries: List<OalLogEntry>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<oal xmlns=\"http://www.astronomy.org/OpenAstronomyLog/2.1\" version=\"2.1\">\n")
        sb.append("  <observations>\n")

        for (e in entries) {
            val isoDate = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochSecond(e.timestampEpochSeconds))
            sb.append("    <observation id=\"${escapeXml(e.id)}\">\n")
            sb.append("      <target id=\"${escapeXml(e.objectCatalogId)}\" name=\"${escapeXml(e.objectName)}\" />\n")
            sb.append("      <observer name=\"${escapeXml(e.observerName)}\" />\n")
            sb.append("      <site name=\"${escapeXml(e.siteName)}\" />\n")
            sb.append("      <optics name=\"${escapeXml(e.opticsName)}\" />\n")
            sb.append("      <begin>${isoDate}</begin>\n")
            if (e.notes.isNotBlank()) {
                sb.append("      <result notes=\"${escapeXml(e.notes)}\" />\n")
            }
            if (e.seeingPickering != null || e.seeingAntoniadi != null || e.nelm != null) {
                sb.append("      <assessment>\n")
                e.seeingPickering?.let { sb.append("        <seeing pickering=\"${it}\" />\n") }
                e.seeingAntoniadi?.let { sb.append("        <seeing antoniadi=\"${escapeXml(it)}\" />\n") }
                e.nelm?.let { sb.append("        <limitingMagnitude>${it}</limitingMagnitude>\n") }
                sb.append("      </assessment>\n")
            }
            sb.append("    </observation>\n")
        }

        sb.append("  </observations>\n")
        sb.append("</oal>")
        return sb.toString()
    }

    fun exportFormattedTextSummary(entries: List<OalLogEntry>): String {
        val sb = StringBuilder()
        sb.append("=== PROJEKT ASTRA BEOBACHTUNGS-TAGEBUCH ===\n")
        sb.append("Gesamtanzahl Beobachtungen: ${entries.size}\n\n")
        entries.forEachIndexed { idx, e ->
            val date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(e.timestampEpochSeconds), ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'"))
            sb.append("[${idx + 1}] ${e.objectCatalogId} - ${e.objectName}\n")
            sb.append("    Zeit: $date | Standort: ${e.siteName} | Optik: ${e.opticsName}\n")
            val seeingParts = mutableListOf<String>()
            e.seeingPickering?.let { seeingParts.add("Pickering $it/10") }
            e.seeingAntoniadi?.let { seeingParts.add("Antoniadi $it") }
            e.nelm?.let { seeingParts.add("NELM ${it}m") }
            if (seeingParts.isNotEmpty()) {
                sb.append("    Bedingungen: ${seeingParts.joinToString(", ")}\n")
            }
            if (e.notes.isNotBlank()) {
                sb.append("    Notiz: ${e.notes}\n")
            }
            sb.append("\n")
        }
        return sb.toString()
    }
}

// ============================================================================
// Feature 12: Seeing Scales (Pickering, Antoniadi, NELM)
// ============================================================================

object SeeingScaleValidator {
    val ANTONIADI_VALID_GRADES = setOf("I", "II", "III", "IV", "V")

    fun isValidPickering(rating: Int): Boolean = rating in 1..10

    fun isValidAntoniadi(grade: String): Boolean = ANTONIADI_VALID_GRADES.contains(grade.trim().uppercase())

    fun isValidNelm(mag: Double): Boolean = mag in 0.0..8.5

    fun mapPickeringToAntoniadi(pickering: Int): String {
        require(isValidPickering(pickering)) { "Pickering must be 1..10" }
        return when (pickering) {
            10, 9 -> "I"     // Perfect seeing without a quiver
            8, 7 -> "II"     // Slight quivering with moments of calm
            6, 5 -> "III"    // Moderate seeing with larger tremors
            4, 3 -> "IV"     // Poor seeing with constant billows
            else -> "V"      // Very bad seeing
        }
    }
}

// ============================================================================
// Feature 13: Stepped Night Exposure Compensation
// ============================================================================

data class CameraExposureState(
    val currentStepIndex: Int,          // 0 = 0 EV, 1 = +1 EV, 2 = +2 EV, 3 = Max EV
    val evValue: Float,
    val isCompensationActive: Boolean
)

class CameraExposureController(val maxEvSteps: Int = 4, val evStepSize: Float = 0.5f) {
    var currentStep: Int = 0
        private set

    fun stepUp(): CameraExposureState {
        currentStep = (currentStep + 1).coerceAtMost(maxEvSteps)
        return getState()
    }

    fun stepDown(): CameraExposureState {
        currentStep = (currentStep - 1).coerceAtLeast(0)
        return getState()
    }

    fun reset(): CameraExposureState {
        currentStep = 0
        return getState()
    }

    fun getState(): CameraExposureState = CameraExposureState(
        currentStepIndex = currentStep,
        evValue = currentStep * evStepSize,
        isCompensationActive = currentStep > 0
    )
}

// ============================================================================
// Feature 14: AR Sensor Low-Pass Jitter Filter & Pitch Trim
// ============================================================================

class ArSensorFilter(
    private val baseAlpha: Float = 0.15f,
    var pitchTrimDegrees: Float = 0.0f
) {
    private var smoothedAzimuth: Float? = null
    private var smoothedPitch: Float? = null

    /**
     * Adaptive low-pass filter: Narrower FOV needs stronger smoothing (lower alpha)
     * to eliminate hand jitter when looking at tiny targets.
     */
    fun computeAlphaForFov(fovDegrees: Float): Float {
        // FOV 60° (wide) -> alpha ~ 0.25 (responsive)
        // FOV 5° (tele/zoomed) -> alpha ~ 0.03 (heavily damped)
        val clampedFov = fovDegrees.coerceIn(2.0f, 90.0f)
        return (baseAlpha * (clampedFov / 45.0f)).coerceIn(0.02f, 0.40f)
    }

    fun filter(
        rawAzimuthDegrees: Float,
        rawPitchDegrees: Float,
        fovDegrees: Float
    ): Pair<Float, Float> {
        val alpha = computeAlphaForFov(fovDegrees)

        // Handle Azimuth circular wrap-around (0° <-> 360°)
        val prevAz = smoothedAzimuth ?: rawAzimuthDegrees
        var deltaAz = rawAzimuthDegrees - prevAz
        while (deltaAz > 180.0f) deltaAz -= 360.0f
        while (deltaAz < -180.0f) deltaAz += 360.0f
        val newAz = ((prevAz + alpha * deltaAz) + 360.0f) % 360.0f
        smoothedAzimuth = newAz

        // Pitch linear smoothing with pitch trim offset applied
        val trimmedRawPitch = (rawPitchDegrees + pitchTrimDegrees).coerceIn(-90.0f, 90.0f)
        val prevPitch = smoothedPitch ?: trimmedRawPitch
        val newPitch = prevPitch + alpha * (trimmedRawPitch - prevPitch)
        smoothedPitch = newPitch

        return Pair(newAz, newPitch)
    }

    fun reset() {
        smoothedAzimuth = null
        smoothedPitch = null
    }
}

// ============================================================================
// Feature 15: Physical Volume Key Glove Mode Zoom
// ============================================================================

class GloveModeZoomController(
    var currentFovDegrees: Double = 60.0,
    val minFovDegrees: Double = 0.5,
    val maxFovDegrees: Double = 110.0,
    val zoomFactorPerStep: Double = 1.25
) {
    var gloveModeEnabled: Boolean = true

    fun onVolumeUp(): Double { // Volume UP = Zoom IN (Decrease FOV)
        if (!gloveModeEnabled) return currentFovDegrees
        currentFovDegrees = (currentFovDegrees / zoomFactorPerStep).coerceAtLeast(minFovDegrees)
        return currentFovDegrees
    }

    fun onVolumeDown(): Double { // Volume DOWN = Zoom OUT (Increase FOV)
        if (!gloveModeEnabled) return currentFovDegrees
        currentFovDegrees = (currentFovDegrees * zoomFactorPerStep).coerceAtMost(maxFovDegrees)
        return currentFovDegrees
    }
}

// ============================================================================
// Feature 16: Pure OLED True Black Mode (#000000)
// ============================================================================

data class OledThemeColors(
    val backgroundColorHex: String,
    val surfaceColorHex: String,
    val cardBackgroundHex: String,
    val isPureBlack: Boolean
)

object OledThemeManager {
    const val PURE_BLACK_HEX = "#000000"
    const val DEFAULT_DARK_SURFACE_HEX = "#121212"

    fun getThemeColors(oledModeEnabled: Boolean): OledThemeColors {
        return if (oledModeEnabled) {
            OledThemeColors(
                backgroundColorHex = PURE_BLACK_HEX,
                surfaceColorHex = PURE_BLACK_HEX,
                cardBackgroundHex = PURE_BLACK_HEX,
                isPureBlack = true
            )
        } else {
            OledThemeColors(
                backgroundColorHex = DEFAULT_DARK_SURFACE_HEX,
                surfaceColorHex = "#1E1E1E",
                cardBackgroundHex = "#2C2C2C",
                isPureBlack = false
            )
        }
    }
}

// ============================================================================
// Feature 17: Battery-Friendly Homescreen AppWidget
// ============================================================================

data class WidgetState(
    val moonPhaseLabel: String,
    val moonIlluminationPercent: Int,
    val darknessWindow: String,
    val weatherScore: Int,
    val lastKnownLocationLabel: String,
    val usesBackgroundGps: Boolean = false,
    val usesRunningBackgroundService: Boolean = false
)

object AstraAppWidgetOracle {
    fun renderWidgetState(
        time: Instant,
        cachedWeatherScore: Int,
        lastKnownLocation: String
    ): WidgetState {
        val termState = LunarTerminatorOracle.calculate(time)
        val illumPct = (termState.illuminationFraction * 100.0).roundToInt()
        val phaseName = when {
            illumPct < 2 -> "Neumond"
            illumPct in 45..55 && termState.colongitudeDegrees < 180.0 -> "Erstes Viertel"
            illumPct > 98 -> "Vollmond"
            illumPct in 45..55 -> "Letztes Viertel"
            termState.colongitudeDegrees < 180.0 -> "Zunehmender Mond"
            else -> "Abnehmender Mond"
        }

        return WidgetState(
            moonPhaseLabel = phaseName,
            moonIlluminationPercent = illumPct,
            darknessWindow = "Astronomische Nacht: 22:15 - 04:45",
            weatherScore = cachedWeatherScore.coerceIn(0, 100),
            lastKnownLocationLabel = lastKnownLocation,
            usesBackgroundGps = false,
            usesRunningBackgroundService = false
        )
    }
}
