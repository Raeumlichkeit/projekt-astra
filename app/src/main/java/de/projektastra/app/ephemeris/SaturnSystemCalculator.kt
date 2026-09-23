package de.projektastra.app.ephemeris

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.Vector
import io.github.cosinekitty.astronomy.geoVector
import io.github.cosinekitty.astronomy.illumination
import io.github.cosinekitty.astronomy.rotationAxis
import java.time.Instant
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal data class SaturnSystemState(
    val ringTiltDegrees: Double,         // Ring opening angle B as seen from Earth (deg)
    val titanOffsetRS: Double,           // Distance of Titan from Saturn in Saturn equatorial radii (RS)
    val titanPositionAngle: Double,      // Position angle on the sky (deg, North through East)
    val ringPositionAngle: Double = 0.0, // Celestial position angle of ring major axis (deg)
    val titanXRS: Double = 0.0,          // Offset in ring plane major axis (RS)
    val titanYRS: Double = 0.0,          // Offset in ring plane minor axis (RS)
    val distanceAu: Double = 0.0
)

internal object SaturnSystemCalculator {
    private const val RS_EQUATORIAL_KM = 60268.0
    private const val TITAN_SEMI_MAJOR_KM = 1221870.0
    private const val TITAN_A_RS = TITAN_SEMI_MAJOR_KM / RS_EQUATORIAL_KM // ~20.274
    private const val TITAN_ECCENTRICITY = 0.0288
    private const val TITAN_DAILY_MOTION_DEG = 22.5769768 // deg/day
    private const val TITAN_L0_DEG = 9.64 // Mean longitude at J2000.0 epoch
    private const val TITAN_PERIAPSIS_DEG = 180.5

    fun calculate(time: Instant): SaturnSystemState {
        val astroTime = Time.fromMillisecondsSince1970(time.toEpochMilli())

        // 1. Ring opening angle B from Astronomy Engine (IAU convention: B > 0 North face, B < 0 South face)
        val illuminationInfo = illumination(Body.Saturn, astroTime)
        val ringTilt = -illuminationInfo.ringTilt

        // 2. Saturn distance & line-of-sight vector
        val rSat = geoVector(Body.Saturn, astroTime, Aberration.Corrected)
        val delta = rSat.length()
        val eLos = rSat.div(delta)

        // 3. Saturn orientation & ring plane normal
        val axisInfo = rotationAxis(Body.Saturn, astroTime)
        val ringNormal = axisInfo.north

        // 4. Titan analytical Keplerian orbit calculation
        val d = astroTime.tt
        val meanLongitude = ((TITAN_L0_DEG + TITAN_DAILY_MOTION_DEG * d) % 360.0 + 360.0) % 360.0
        val meanAnomalyDeg = ((meanLongitude - TITAN_PERIAPSIS_DEG) % 360.0 + 360.0) % 360.0
        val meanAnomalyRad = Math.toRadians(meanAnomalyDeg)

        val equationOfCenterDeg = 2.0 * TITAN_ECCENTRICITY * sin(meanAnomalyRad) * (180.0 / PI)
        val trueLongitudeDeg = ((meanLongitude + equationOfCenterDeg) % 360.0 + 360.0) % 360.0
        val trueAnomalyRad = Math.toRadians(trueLongitudeDeg)

        val rRS = TITAN_A_RS * (1.0 - TITAN_ECCENTRICITY * cos(meanAnomalyRad))

        val bRad = Math.toRadians(ringTilt)
        val xRS = rRS * sin(trueAnomalyRad)
        val yRS = rRS * cos(trueAnomalyRad) * sin(bRad)
        val offsetRS = sqrt(xRS * xRS + yRS * yRS)

        // 5. Position angle in sky plane (Celestial North through East)
        val zEarth = Vector(0.0, 0.0, 1.0, astroTime)
        val northProj = zEarth.minus(scale(eLos, zEarth.dot(eLos)))
        val northLen = northProj.length()
        val uNorth = if (northLen > 1e-6) northProj.div(northLen) else Vector(0.0, 1.0, 0.0, astroTime)
        val uEast = cross(uNorth, eLos)

        val ringNormProj = ringNormal.minus(scale(eLos, ringNormal.dot(eLos)))
        val ringNormLen = ringNormProj.length()
        val uRingPole = if (ringNormLen > 1e-6) ringNormProj.div(ringNormLen) else uNorth
        val uRingMajor = cross(uRingPole, eLos)
        val ringPa = ((atan2(uRingMajor.dot(uEast), uRingMajor.dot(uNorth)) * 180.0 / PI) + 360.0) % 360.0

        val dEast = xRS * uRingMajor.dot(uEast) + yRS * uRingPole.dot(uEast)
        val dNorth = xRS * uRingMajor.dot(uNorth) + yRS * uRingPole.dot(uNorth)
        val titanPa = ((atan2(dEast, dNorth) * 180.0 / PI) + 360.0) % 360.0

        return SaturnSystemState(
            ringTiltDegrees = ringTilt,
            titanOffsetRS = offsetRS,
            titanPositionAngle = titanPa,
            ringPositionAngle = ringPa,
            titanXRS = xRS,
            titanYRS = yRS,
            distanceAu = delta
        )
    }

    private fun cross(a: Vector, b: Vector): Vector {
        return Vector(
            a.y * b.z - a.z * b.y,
            a.z * b.x - a.x * b.z,
            a.x * b.y - a.y * b.x,
            a.t
        )
    }

    private fun scale(v: Vector, s: Double): Vector = Vector(v.x * s, v.y * s, v.z * s, v.t)
}
