package de.projektastra.app.ephemeris

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.Vector
import io.github.cosinekitty.astronomy.geoMoon
import io.github.cosinekitty.astronomy.geoVector
import io.github.cosinekitty.astronomy.illumination
import io.github.cosinekitty.astronomy.libration
import io.github.cosinekitty.astronomy.rotationAxis
import java.time.Instant
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal object LunarTerminatorCalculator {

    fun calculateTerminator(time: Instant): LunarTerminatorState {
        val astroTime = Time.fromMillisecondsSince1970(time.toEpochMilli())
        val librationInfo = libration(astroTime)
        val illumInfo = illumination(Body.Moon, astroTime)
        val axisInfo = rotationAxis(Body.Moon, astroTime)

        // Moon-fixed selenographic basis in ICRF/J2000
        val alpha0 = Math.toRadians(axisInfo.ra * 15.0)
        val delta0 = Math.toRadians(axisInfo.dec)
        val spinW = Math.toRadians(axisInfo.spin)

        val northZ = axisInfo.north
        val nodeU = Vector(-sin(alpha0), cos(alpha0), 0.0, astroTime)
        val v = Vector(-sin(delta0) * cos(alpha0), -sin(delta0) * sin(alpha0), cos(delta0), astroTime)

        val primeX = Vector(
            cos(spinW) * nodeU.x + sin(spinW) * v.x,
            cos(spinW) * nodeU.y + sin(spinW) * v.y,
            cos(spinW) * nodeU.z + sin(spinW) * v.z,
            astroTime
        )
        val eastY = Vector(
            -sin(spinW) * nodeU.x + cos(spinW) * v.x,
            -sin(spinW) * nodeU.y + cos(spinW) * v.y,
            -sin(spinW) * nodeU.z + cos(spinW) * v.z,
            astroTime
        )

        // Moon to Sun vector
        val rSun = geoVector(Body.Sun, astroTime, Aberration.Corrected)
        val rMoon = geoMoon(astroTime)
        val rMtoS = Vector(rSun.x - rMoon.x, rSun.y - rMoon.y, rSun.z - rMoon.z, astroTime)

        // Selenographic projection of Sun
        val sx = rMtoS.x * primeX.x + rMtoS.y * primeX.y + rMtoS.z * primeX.z
        val sy = rMtoS.x * eastY.x + rMtoS.y * eastY.y + rMtoS.z * eastY.z
        val sz = rMtoS.x * northZ.x + rMtoS.y * northZ.y + rMtoS.z * northZ.z
        val distMS = sqrt(sx * sx + sy * sy + sz * sz)

        val subSolarLon = Math.toDegrees(atan2(sy, sx))
        val subSolarLat = Math.toDegrees(asin((sz / distMS).coerceIn(-1.0, 1.0)))

        var colongitude = (90.0 - subSolarLon) % 360.0
        if (colongitude < 0.0) colongitude += 360.0

        return LunarTerminatorState(
            instant = time,
            subSolarLon = subSolarLon,
            subSolarLat = subSolarLat,
            colongitude = colongitude,
            subEarthLon = librationInfo.elon,
            subEarthLat = librationInfo.elat,
            moonDistanceKm = librationInfo.distanceKm,
            moonDiameterDeg = librationInfo.diamDeg,
            phaseFraction = illumInfo.phaseFraction,
            phaseAngleDegrees = illumInfo.phaseAngle,
            axisPositionAngle = axisInfo.ra * 15.0
        )
    }

    fun featuresNearTerminator(
        terminator: LunarTerminatorState,
        features: List<LunarFeature> = LunarFeatureCatalog.allFeatures
    ): List<LunarFeatureHighlight> {
        return features.map { feature ->
            val hSun = terminator.sunElevationDegrees(feature.selenographicLon, feature.selenographicLat)
            val earthVis = terminator.earthVisibility(feature.selenographicLon, feature.selenographicLat)

            val mTerm = terminator.morningTerminatorLon(feature.selenographicLat)
            val eTerm = terminator.eveningTerminatorLon(feature.selenographicLat)

            val distM = abs(LunarTerminatorState.normalizeLongitude(feature.selenographicLon - mTerm)) *
                cos(Math.toRadians(feature.selenographicLat))
            val distE = abs(LunarTerminatorState.normalizeLongitude(feature.selenographicLon - eTerm)) *
                cos(Math.toRadians(feature.selenographicLat))

            val (eventType, distTerm) = if (distM <= distE) {
                (if (hSun in -1.5..15.0) TerminatorEventType.SUNRISE else if (hSun > 15.0) TerminatorEventType.DAYLIGHT else TerminatorEventType.NIGHT) to distM
            } else {
                (if (hSun in -1.5..15.0) TerminatorEventType.SUNSET else if (hSun > 15.0) TerminatorEventType.DAYLIGHT else TerminatorEventType.NIGHT) to distE
            }

            val inOptimal = hSun in 0.0..12.0 && earthVis.isVisibleFromEarth
            val reliefScore = if (hSun in 0.0..12.0) {
                (1.0 - abs(hSun - 4.5) / 7.5).coerceIn(0.0, 1.0)
            } else 0.0

            LunarFeatureHighlight(
                feature = feature,
                sunElevationDegrees = hSun,
                eventType = eventType,
                inOptimalRelief = inOptimal,
                reliefScore = reliefScore,
                distanceToTerminatorDegrees = distTerm,
                isVisibleFromEarth = earthVis.isVisibleFromEarth,
                diskX = earthVis.diskX,
                diskY = earthVis.diskY
            )
        }.sortedWith(
            compareByDescending<LunarFeatureHighlight> { it.inOptimalRelief }
                .thenByDescending { it.reliefScore }
                .thenBy { it.distanceToTerminatorDegrees }
        )
    }

    /**
     * Generates a polyline of the visible terminator curve across the apparent lunar disc.
     * Step in latitude from -88° to +88°.
     */
    fun generateVisibleTerminatorPath(
        terminator: LunarTerminatorState,
        stepDegrees: Double = 2.0
    ): List<Pair<Float, Float>> {
        val points = mutableListOf<Pair<Float, Float>>()
        var lat = -88.0
        while (lat <= 88.0) {
            val lonM = terminator.morningTerminatorLon(lat)
            val visM = terminator.earthVisibility(lonM, lat)
            if (visM.isVisibleFromEarth) {
                points.add(visM.diskX.toFloat() to visM.diskY.toFloat())
            } else {
                val lonE = terminator.eveningTerminatorLon(lat)
                val visE = terminator.earthVisibility(lonE, lat)
                if (visE.isVisibleFromEarth) {
                    points.add(visE.diskX.toFloat() to visE.diskY.toFloat())
                }
            }
            lat += stepDegrees
        }
        return points
    }
}
