package de.projektastra.app.ephemeris

import de.projektastra.app.GeoPoint
import de.projektastra.app.HorizontalCoordinates
import java.time.Instant
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

internal data class Vector3D(val x: Double, val y: Double, val z: Double) {
    val magnitude: Double get() = sqrt(x * x + y * y + z * z)
    operator fun plus(o: Vector3D) = Vector3D(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vector3D) = Vector3D(x - o.x, y - o.y, z - o.z)
    operator fun times(scale: Double) = Vector3D(x * scale, y * scale, z * scale)
    fun dot(o: Vector3D): Double = x * o.x + y * o.y + z * o.z
    fun normalized(): Vector3D {
        val mag = magnitude
        return if (mag > 0.0) Vector3D(x / mag, y / mag, z / mag) else this
    }
}

internal enum class IlluminationStatus(val label: String) {
    SUNLIT("Voll beleuchtet"),
    PENUMBRA("Halbschatten"),
    ECLIPSED("Im Erdschatten (unsichtbar)")
}

internal data class TopocentricPosition(
    val coordinates: HorizontalCoordinates,
    val rangeKm: Double,
    val rangeRateKmS: Double,
    val altitudeAboveEarthKm: Double,
    val illumination: IlluminationStatus,
    val isObserverDark: Boolean,
    val isVisible: Boolean
)

internal data class SatelliteTrackPoint(
    val time: Instant,
    val position: HorizontalCoordinates,
    val rangeKm: Double,
    val illumination: IlluminationStatus,
    val isVisible: Boolean
)

internal class Sgp4Propagator {

    companion object {
        // WGS-72 Earth Physical Constants
        const val RE_KM = 6378.135
        const val MU = 398600.8
        // WGS-84 Reference Ellipsoid for Topocentric Coordinates
        private const val WGS84_A_KM = 6378.137
        private const val WGS84_F = 1.0 / 298.257223563
        private const val WGS84_E2 = 2.0 * WGS84_F - WGS84_F * WGS84_F
    }

    /** Near-Earth SGP4 only; deep-space TLEs throw and propagate() returns null. */
    fun propagateTeme(tle: TleData, time: Instant): Vector3D {
        val elapsedSeconds = (time.epochSecond - tle.epochInstant.epochSecond).toDouble() +
            (time.nano - tle.epochInstant.nano) / 1e9
        return NearEarthSgp4(tle).position(elapsedSeconds / 60.0)
    }

    fun gmst(time: Instant): Double {
        val jd = 2440587.5 + time.toEpochMilli() / 86400000.0
        val t = (jd - 2451545.0) / 36525.0
        val thetaDeg = 280.46061837 + 360.98564736629 * (jd - 2451545.0) +
            0.000387933 * t * t - (t * t * t) / 38710000.0
        return Math.toRadians((thetaDeg % 360.0 + 360.0) % 360.0)
    }

    fun temeToEcef(teme: Vector3D, gmstRad: Double): Vector3D {
        val cosG = cos(gmstRad)
        val sinG = sin(gmstRad)
        return Vector3D(
            x = teme.x * cosG + teme.y * sinG,
            y = -teme.x * sinG + teme.y * cosG,
            z = teme.z
        )
    }

    fun observerEcef(observer: GeoPoint): Vector3D {
        val latRad = Math.toRadians(observer.latitude)
        val lonRad = Math.toRadians(observer.longitude)
        val altKm = observer.altitudeMeters / 1000.0

        val sinLat = sin(latRad)
        val cosLat = cos(latRad)
        val sinLon = sin(lonRad)
        val cosLon = cos(lonRad)

        val n = WGS84_A_KM / sqrt(1.0 - WGS84_E2 * sinLat * sinLat)
        val x = (n + altKm) * cosLat * cosLon
        val y = (n + altKm) * cosLat * sinLon
        val z = (n * (1.0 - WGS84_E2) + altKm) * sinLat
        return Vector3D(x, y, z)
    }

    fun ecefToTopocentric(satEcef: Vector3D, obsEcef: Vector3D, observer: GeoPoint): Triple<Double, Double, Double> {
        val delta = satEcef - obsEcef
        val latRad = Math.toRadians(observer.latitude)
        val lonRad = Math.toRadians(observer.longitude)

        val sinLat = sin(latRad)
        val cosLat = cos(latRad)
        val sinLon = sin(lonRad)
        val cosLon = cos(lonRad)

        // SEZ: South, East, Zenith
        val s = sinLat * cosLon * delta.x + sinLat * sinLon * delta.y - cosLat * delta.z
        val e = -sinLon * delta.x + cosLon * delta.y
        val z = cosLat * cosLon * delta.x + cosLat * sinLon * delta.y + sinLat * delta.z

        val range = sqrt(s * s + e * e + z * z)
        val alt = Math.toDegrees(asin((z / max(1e-6, range)).coerceIn(-1.0, 1.0)))
        val az = (Math.toDegrees(atan2(e, -s)) + 360.0) % 360.0
        return Triple(az, alt, range)
    }

    fun sunPositionTeme(time: Instant): Vector3D {
        val jd = 2440587.5 + time.toEpochMilli() / 86400000.0
        val n = jd - 2451545.0
        val l = Math.toRadians((280.460 + 0.9856474 * n) % 360.0)
        val g = Math.toRadians((357.528 + 0.9856003 * n) % 360.0)
        val lambda = l + Math.toRadians(1.915 * sin(g) + 0.020 * sin(2.0 * g))
        val eps = Math.toRadians(23.439 - 0.0000004 * n)
        return Vector3D(cos(lambda), cos(eps) * sin(lambda), sin(eps) * sin(lambda)).normalized()
    }

    fun checkIllumination(satTeme: Vector3D, sunDirTeme: Vector3D): IlluminationStatus {
        val s = satTeme.dot(sunDirTeme)
        if (s > 0.0) return IlluminationStatus.SUNLIT

        val distPerp = sqrt(max(0.0, satTeme.dot(satTeme) - s * s))
        val absS = abs(s)
        val rEff = RE_KM + 20.0 // Earth radius with effective atmosphere layer

        val rUmbra = rEff - absS * tan(Math.toRadians(0.264))
        val rPenumbra = rEff + absS * tan(Math.toRadians(0.269))

        return when {
            distPerp <= rUmbra -> IlluminationStatus.ECLIPSED
            distPerp < rPenumbra -> IlluminationStatus.PENUMBRA
            else -> IlluminationStatus.SUNLIT
        }
    }

    fun sunElevationAtObserver(time: Instant, observer: GeoPoint): Double {
        val sunDirTeme = sunPositionTeme(time)
        val gmstVal = gmst(time)
        val sunDirEcef = temeToEcef(sunDirTeme * 149597870.7, gmstVal)
        val obsEcef = observerEcef(observer)
        val (_, alt, _) = ecefToTopocentric(sunDirEcef, obsEcef, observer)
        return alt
    }

    fun propagate(tle: TleData, time: Instant, observer: GeoPoint): TopocentricPosition? {
        val satTeme = runCatching { propagateTeme(tle, time) }.getOrNull() ?: return null
        val gmstVal = gmst(time)
        val satEcef = temeToEcef(satTeme, gmstVal)
        val obsEcef = observerEcef(observer)

        val (az, alt, range) = ecefToTopocentric(satEcef, obsEcef, observer)

        // Radial velocity via finite difference (dt = 0.5s)
        val dt = 0.5
        val nextTime = time.plusMillis((dt * 1000).toLong())
        val nextSatTeme = runCatching { propagateTeme(tle, nextTime) }.getOrNull() ?: return null
        val nextSatEcef = temeToEcef(nextSatTeme, gmst(nextTime))
        val (_, _, nextRange) = ecefToTopocentric(nextSatEcef, obsEcef, observer)
        val rangeRate = (nextRange - range) / dt

        val sunDir = sunPositionTeme(time)
        val illumination = checkIllumination(satTeme, sunDir)
        val sunAlt = sunElevationAtObserver(time, observer)
        val isObserverDark = sunAlt <= -6.0
        val isVisible = alt >= 10.0 && illumination != IlluminationStatus.ECLIPSED && isObserverDark

        val altAboveEarthKm = satTeme.magnitude - RE_KM

        return TopocentricPosition(
            coordinates = HorizontalCoordinates(az, alt),
            rangeKm = range,
            rangeRateKmS = rangeRate,
            altitudeAboveEarthKm = altAboveEarthKm,
            illumination = illumination,
            isObserverDark = isObserverDark,
            isVisible = isVisible
        )
    }

    fun generatePassTrack(
        tle: TleData,
        start: Instant,
        durationMinutes: Int,
        observer: GeoPoint
    ): List<SatelliteTrackPoint> {
        val points = mutableListOf<SatelliteTrackPoint>()
        val totalSeconds = durationMinutes * 60
        var sec = 0
        while (sec <= totalSeconds) {
            val t = start.plusSeconds(sec.toLong())
            val pos = propagate(tle, t, observer)
            if (pos != null) {
                points.add(
                    SatelliteTrackPoint(
                        time = t,
                        position = pos.coordinates,
                        rangeKm = pos.rangeKm,
                        illumination = pos.illumination,
                        isVisible = pos.isVisible
                    )
                )
            }
            sec += 10
        }
        return points
    }
}
