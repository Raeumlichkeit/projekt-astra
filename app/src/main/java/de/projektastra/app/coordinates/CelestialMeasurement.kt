package de.projektastra.app.coordinates

import de.projektastra.app.CelestialObject
import de.projektastra.app.HorizontalCoordinates
import de.projektastra.app.SkyCoordinateFrame
import java.util.Locale
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Equatorial coordinate pair on the celestial sphere (J2000).
 *
 * @property raHours Right ascension in hours, in [0.0, 24.0).
 * @property decDegrees Declination in degrees, in [-90.0, +90.0].
 */
data class EquatorialCoordinates(
    val raHours: Double,
    val decDegrees: Double
) {
    init {
        require(raHours.isFinite()) { "raHours must be finite: $raHours" }
        require(decDegrees.isFinite()) { "decDegrees must be finite: $decDegrees" }
        require(decDegrees in -90.0..90.0) { "decDegrees must be in [-90.0, +90.0]: $decDegrees" }
    }

    val rightAscensionHours: Double get() = raHours
    val declinationDegrees: Double get() = decDegrees

    val raDegrees: Double
        get() = ((raHours * 15.0) % 360.0 + 360.0) % 360.0

    val raRadians: Double
        get() = Math.toRadians(raDegrees)

    val decRadians: Double
        get() = Math.toRadians(decDegrees)

    /**
     * Great-circle angular distance on the celestial sphere to [other] in degrees.
     */
    fun angularDistanceTo(other: EquatorialCoordinates): Double =
        CelestialMeasurement.angularDistance(this, other)

    /**
     * Position angle of [other] relative to this coordinate in degrees [0.0, 360.0).
     */
    fun positionAngleTo(other: EquatorialCoordinates): Double =
        CelestialMeasurement.positionAngle(this, other)

    companion object {
        fun fromDegrees(raDegrees: Double, decDegrees: Double): EquatorialCoordinates {
            val normalizedHours = (((raDegrees % 360.0) + 360.0) % 360.0) / 15.0
            return EquatorialCoordinates(normalizedHours, decDegrees)
        }
    }
}

/**
 * Structured breakdown of angular separation into degrees, arcminutes, and arcseconds.
 */
data class AngularDistance(
    val degrees: Int,
    val arcminutes: Int,
    val arcseconds: Double
) {
    /** Formatted as D° MM' SS" with integer seconds. */
    fun formatStandard(): String = String.format(
        Locale.US, "%d° %02d' %02d\"", degrees, arcminutes, round(arcseconds).toInt()
    )

    /** Formatted as D° MM' SS.S" with fractional seconds. */
    fun formatPrecise(): String = String.format(
        Locale.US, "%d° %02d' %04.1f\"", degrees, arcminutes, arcseconds
    )

    companion object {
        fun fromDegrees(deg: Double): AngularDistance {
            val nonNegative = deg.coerceAtLeast(0.0)
            var d = nonNegative.toInt()
            val remMinutes = (nonNegative - d) * 60.0
            var m = remMinutes.toInt()
            var s = (remMinutes - m) * 60.0

            if (round(s) >= 60.0) {
                s = 0.0
                m += 1
                if (m >= 60) {
                    m = 0
                    d += 1
                }
            }
            return AngularDistance(d, m, s)
        }
    }
}

/**
 * Result of celestial measurement between two equatorial points.
 *
 * Implements the required PROJECT.md interface contract, while exposing
 * decomposition fields for complete E2E test suite compatibility.
 */
data class MeasurementResult(
    val angularDistanceDegrees: Double,
    val formattedDistance: String,
    val positionAngleDegrees: Double,
    val degrees: Int = AngularDistance.fromDegrees(angularDistanceDegrees).degrees,
    val arcMinutes: Int = AngularDistance.fromDegrees(angularDistanceDegrees).arcminutes,
    val arcSeconds: Double = AngularDistance.fromDegrees(angularDistanceDegrees).arcseconds
) {
    val formattedDms: String get() = formattedDistance
}

/**
 * Core astronomical calculation engine for celestial measurements.
 */
object CelestialMeasurement {

    /**
     * Measures the angular distance and position angle from [p1] to [p2].
     */
    fun measure(p1: EquatorialCoordinates, p2: EquatorialCoordinates): MeasurementResult {
        val distDeg = angularDistance(p1, p2)
        val paDeg = positionAngle(p1, p2)
        val details = AngularDistance.fromDegrees(distDeg)
        return MeasurementResult(
            angularDistanceDegrees = distDeg,
            formattedDistance = details.formatStandard(),
            positionAngleDegrees = paDeg,
            degrees = details.degrees,
            arcMinutes = details.arcminutes,
            arcSeconds = details.arcseconds
        )
    }

    /**
     * Great-circle angular distance in degrees using Vincenty's spherical formulation.
     * Guaranteed numerically stable across all angles [0.0, 180.0].
     */
    fun angularDistance(p1: EquatorialCoordinates, p2: EquatorialCoordinates): Double {
        val dAlpha = p2.raRadians - p1.raRadians
        val sinDelta1 = sin(p1.decRadians)
        val cosDelta1 = cos(p1.decRadians)
        val sinDelta2 = sin(p2.decRadians)
        val cosDelta2 = cos(p2.decRadians)
        val sinDAlpha = sin(dAlpha)
        val cosDAlpha = cos(dAlpha)

        val y = hypot(
            cosDelta2 * sinDAlpha,
            cosDelta1 * sinDelta2 - sinDelta1 * cosDelta2 * cosDAlpha
        )
        val x = sinDelta1 * sinDelta2 + cosDelta1 * cosDelta2 * cosDAlpha

        val distRad = atan2(y, x)
        return Math.toDegrees(distRad).coerceIn(0.0, 180.0)
    }

    /**
     * Alternative Haversine formulation for distance calculation and comparison.
     */
    fun angularDistanceHaversine(p1: EquatorialCoordinates, p2: EquatorialCoordinates): Double {
        val dDec = p2.decRadians - p1.decRadians
        val dAlpha = p2.raRadians - p1.raRadians
        val sinHalfDDec = sin(dDec / 2.0)
        val sinHalfDAlpha = sin(dAlpha / 2.0)

        val hav = sinHalfDDec * sinHalfDDec +
            cos(p1.decRadians) * cos(p2.decRadians) * sinHalfDAlpha * sinHalfDAlpha

        val distRad = 2.0 * asin(sqrt(hav.coerceIn(0.0, 1.0)))
        return Math.toDegrees(distRad).coerceIn(0.0, 180.0)
    }

    /**
     * Position angle theta in degrees in [0.0, 360.0) from North through East.
     */
    fun positionAngle(p1: EquatorialCoordinates, p2: EquatorialCoordinates): Double {
        val dAlpha = p2.raRadians - p1.raRadians
        val sinDelta1 = sin(p1.decRadians)
        val cosDelta1 = cos(p1.decRadians)
        val sinDelta2 = sin(p2.decRadians)
        val cosDelta2 = cos(p2.decRadians)

        val y = cosDelta2 * sin(dAlpha)
        val x = cosDelta1 * sinDelta2 - sinDelta1 * cosDelta2 * cos(dAlpha)

        if (abs(x) < 1e-15 && abs(y) < 1e-15) {
            return 0.0
        }

        val angleDeg = Math.toDegrees(atan2(y, x))
        return ((angleDeg % 360.0) + 360.0) % 360.0
    }

    /**
     * Samples [numSegments] points along the great-circle arc between [p1] and [p2] (SLERP).
     */
    fun interpolateGreatCircle(
        p1: EquatorialCoordinates,
        p2: EquatorialCoordinates,
        numSegments: Int = 16
    ): List<EquatorialCoordinates> {
        val distRad = Math.toRadians(angularDistance(p1, p2))
        if (distRad < 1e-9 || numSegments < 1) return listOf(p1, p2)

        val sinD = sin(distRad)
        val v1 = toUnitVector(p1)
        val v2 = toUnitVector(p2)

        return (0..numSegments).map { i ->
            val t = i.toDouble() / numSegments.toDouble()
            val a = sin((1.0 - t) * distRad) / sinD
            val b = sin(t * distRad) / sinD
            val vx = a * v1[0] + b * v2[0]
            val vy = a * v1[1] + b * v2[1]
            val vz = a * v1[2] + b * v2[2]
            fromUnitVector(vx, vy, vz)
        }
    }

    /**
     * Computes the equatorial midpoint on the great circle between [p1] and [p2].
     */
    fun interpolateMidpoint(p1: EquatorialCoordinates, p2: EquatorialCoordinates): EquatorialCoordinates =
        interpolateGreatCircle(p1, p2, numSegments = 2)[1]

    private fun toUnitVector(p: EquatorialCoordinates): DoubleArray {
        val cosDec = cos(p.decRadians)
        return doubleArrayOf(
            cosDec * cos(p.raRadians),
            cosDec * sin(p.raRadians),
            sin(p.decRadians)
        )
    }

    private fun fromUnitVector(x: Double, y: Double, z: Double): EquatorialCoordinates {
        val norm = sqrt(x * x + y * y + z * z).coerceAtLeast(1e-12)
        val raRad = (atan2(y, x) + 2.0 * Math.PI) % (2.0 * Math.PI)
        val decRad = asin((z / norm).coerceIn(-1.0, 1.0))
        return EquatorialCoordinates(
            raHours = Math.toDegrees(raRad) / 15.0,
            decDegrees = Math.toDegrees(decRad)
        )
    }
}

/**
 * Endpoint for celestial measurement.
 */
internal sealed interface MeasurementPoint {
    val equatorial: EquatorialCoordinates
    val displayName: String

    data class ObjectPoint(
        val celestial: CelestialObject,
        override val equatorial: EquatorialCoordinates = EquatorialCoordinates(celestial.raHours, celestial.decDegrees),
        override val displayName: String = celestial.name.ifBlank { celestial.catalogId }
    ) : MeasurementPoint

    data class CoordinatePoint(
        override val equatorial: EquatorialCoordinates,
        val horizontal: HorizontalCoordinates? = null,
        override val displayName: String = String.format(
            Locale.US, "RA %.2fh, Dec %+.1f°", equatorial.raHours, equatorial.decDegrees
        )
    ) : MeasurementPoint
}

/**
 * UI State for the Celestial Measurement Tool.
 */
internal data class CelestialMeasurementState(
    val isActive: Boolean = false,
    val origin: MeasurementPoint? = null,
    val target: MeasurementPoint? = null
) {
    val isComplete: Boolean get() = origin != null && target != null

    val result: MeasurementResult?
        get() = if (origin != null && target != null) {
            CelestialMeasurement.measure(origin.equatorial, target.equatorial)
        } else null

    fun selectPoint(point: MeasurementPoint): CelestialMeasurementState = when {
        origin == null -> copy(origin = point, target = null)
        target == null -> copy(target = point)
        else -> copy(origin = point, target = null) // start fresh measurement
    }

    fun swapDirection(): CelestialMeasurementState =
        if (origin != null && target != null) copy(origin = target, target = origin) else this

    fun reset(): CelestialMeasurementState = CelestialMeasurementState(isActive = true)
    fun close(): CelestialMeasurementState = CelestialMeasurementState(isActive = false)
}

/**
 * Inverts topocentric [HorizontalCoordinates] back to [EquatorialCoordinates] (J2000)
 * using the observer's [SkyCoordinateFrame].
 */
internal fun SkyCoordinateFrame.toEquatorial(horizontal: HorizontalCoordinates): EquatorialCoordinates {
    val azRad = Math.toRadians(horizontal.azimuth)
    val altRad = Math.toRadians(horizontal.altitude)
    val local = doubleArrayOf(cos(altRad) * sin(azRad), cos(altRad) * cos(azRad), sin(altRad))
    val matrix = horizontalToJ2000
    val vx = matrix[0] * local[0] + matrix[3] * local[1] + matrix[6] * local[2]
    val vy = matrix[1] * local[0] + matrix[4] * local[1] + matrix[7] * local[2]
    val vz = matrix[2] * local[0] + matrix[5] * local[1] + matrix[8] * local[2]
    val norm = sqrt(vx * vx + vy * vy + vz * vz).coerceAtLeast(1e-12)
    val raRad = (atan2(vy, vx) + 2.0 * Math.PI) % (2.0 * Math.PI)
    val decRad = asin((vz / norm).coerceIn(-1.0, 1.0))
    return EquatorialCoordinates(
        raHours = Math.toDegrees(raRad) / 15.0,
        decDegrees = Math.toDegrees(decRad)
    )
}
