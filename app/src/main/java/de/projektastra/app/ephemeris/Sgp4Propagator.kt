package de.projektastra.app.ephemeris

import de.projektastra.app.GeoPoint
import de.projektastra.app.HorizontalCoordinates
import java.time.Instant
import kotlin.math.PI
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
        const val XKE = 0.0743669161
        const val VKM = 7.90536574
        const val CK2 = 5.413080e-4   // 0.5 * J2
        const val CK4 = 0.62098875e-6 // -3/8 * J4
        const val A30 = 2.53881e-6    // -J3
        const val S0 = 1.0 + 78.0 / RE_KM
        const val Q0 = 1.0 + 120.0 / RE_KM
        const val QOMS2T = 1.8802893e-9

        // WGS-84 Reference Ellipsoid for Topocentric Coordinates
        private const val WGS84_A_KM = 6378.137
        private const val WGS84_F = 1.0 / 298.257223563
        private const val WGS84_E2 = 2.0 * WGS84_F - WGS84_F * WGS84_F
    }

    private data class Sgp4Init(
        val a00: Double,
        val n00: Double,
        val e0: Double,
        val i0: Double,
        val omega0: Double,
        val raan0: Double,
        val m0: Double,
        val bstar: Double,
        val mDot: Double,
        val omegaDot: Double,
        val raanDot: Double,
        val c1: Double,
        val c4: Double,
        val c5: Double,
        val tsi: Double,
        val eta: Double,
        val beta02: Double,
        val theta2: Double,
        val sinI0: Double,
        val cosI0: Double
    )

    private fun initModel(tle: TleData): Sgp4Init {
        val i0 = Math.toRadians(tle.inclinationDegrees)
        val raan0 = Math.toRadians(tle.raanDegrees)
        val e0 = tle.eccentricity
        val omega0 = Math.toRadians(tle.argumentOfPerigeeDegrees)
        val m0 = Math.toRadians(tle.meanAnomalyDegrees)
        val n0 = tle.meanMotionRadPerMin
        val bstar = tle.bstarDrag

        val a1 = Math.cbrt(XKE / n0).let { it * it }
        val cosI0 = cos(i0)
        val sinI0 = sin(i0)
        val theta2 = cosI0 * cosI0
        val x3thm1 = 3.0 * theta2 - 1.0
        val beta02 = 1.0 - e0 * e0
        val beta0 = sqrt(max(1e-12, beta02))
        val beta03 = beta0 * beta02

        val del1 = 1.5 * CK2 * x3thm1 / (a1 * a1 * beta03)
        val a0 = a1 * (1.0 - del1 * (1.0 / 3.0 + del1 * (1.0 + 134.0 / 81.0 * del1)))
        val del0 = 1.5 * CK2 * x3thm1 / (a0 * a0 * beta03)
        val n00 = n0 / (1.0 + del0)
        val a00 = a0 / (1.0 - del0)

        val rPerigee = a00 * (1.0 - e0)
        val hPerigee = (rPerigee - 1.0) * RE_KM
        val (s, qoms2t) = if (hPerigee < 156.0) {
            val sAdjust = if (hPerigee <= 98.0) 1.0 + 20.0 / RE_KM else 1.0 + (hPerigee - 78.0) / RE_KM
            val qAdjust = Math.pow((Q0 - sAdjust), 4.0)
            sAdjust to qAdjust
        } else {
            S0 to QOMS2T
        }

        val pinvsq = 1.0 / (a00 * a00 * beta02 * beta02)
        val tsi = 1.0 / (a00 - s)
        val eta = a00 * e0 * tsi
        val eta2 = eta * eta
        val eeta = e0 * eta
        val psisq = abs(1.0 - eta2)
        val coef = qoms2t * Math.pow(tsi, 4.0)
        val coef1 = coef / Math.pow(psisq, 3.5)

        val c2 = coef1 * n00 * (a00 * (1.0 + 1.5 * eta2 + eeta * (4.0 + eta2)) +
            0.75 * CK2 * tsi / psisq * x3thm1 * (8.0 + 24.0 * eta2 + 3.0 * eta2 * eta2))
        val c1 = bstar * c2

        val c4 = 2.0 * n00 * coef1 * a00 * beta02 * (eta * (2.0 + 0.5 * eta2) +
            e0 * (0.5 + 2.0 * eta2) - 2.0 * CK2 * tsi / (a00 * psisq) *
            (3.0 * x3thm1 * (1.0 - 2.0 * eeta + eta2 * (1.5 - 0.5 * eeta)) +
                0.75 * (1.0 - theta2) * (2.0 * eta2 - eeta * (1.0 + eta2)) * cos(2.0 * omega0)))

        val c5 = 2.0 * coef1 * a00 * beta02 * (1.0 + 2.75 * (eta2 + eeta) + eeta * eta2)

        // Secular rates
        val temp1 = 1.5 * CK2 * pinvsq * n00
        val temp2 = 0.5 * temp1 * CK2 * pinvsq
        val temp3 = -0.46875 * CK4 * pinvsq * pinvsq * n00
        val mDot = n00 + 0.5 * temp1 * beta0 * x3thm1 + 0.0625 * temp2 * beta0 *
            (13.0 - 78.0 * theta2 + 137.0 * theta2 * theta2)
        val omegaDot = -0.5 * temp1 * (1.0 - 5.0 * theta2) + 0.0625 * temp2 *
            (7.0 - 114.0 * theta2 + 395.0 * theta2 * theta2) + temp3 * (3.0 - 36.0 * theta2 + 49.0 * theta2 * theta2)
        val xhdot1 = -temp1 * cosI0
        val raanDot = xhdot1 + (0.5 * temp2 * (4.0 - 19.0 * theta2) + 2.0 * temp3 * (3.0 - 7.0 * theta2)) * cosI0

        return Sgp4Init(
            a00 = a00,
            n00 = n00,
            e0 = e0,
            i0 = i0,
            omega0 = omega0,
            raan0 = raan0,
            m0 = m0,
            bstar = bstar,
            mDot = mDot,
            omegaDot = omegaDot,
            raanDot = raanDot,
            c1 = c1,
            c4 = c4,
            c5 = c5,
            tsi = tsi,
            eta = eta,
            beta02 = beta02,
            theta2 = theta2,
            sinI0 = sinI0,
            cosI0 = cosI0
        )
    }

    fun propagateTeme(tle: TleData, time: Instant): Vector3D {
        val model = initModel(tle)
        val deltaMin = (time.toEpochMilli() - tle.epochInstant.toEpochMilli()) / 60000.0

        val mdf = model.m0 + model.mDot * deltaMin
        val omegadf = model.omega0 + model.omegaDot * deltaMin
        val xnoddf = model.raan0 + model.raanDot * deltaMin
        val tsq = deltaMin * deltaMin

        val m = mdf + model.c1 * tsq
        val omega = omegadf
        val xnode = xnoddf - (1.5 * model.n00 * CK2 * model.cosI0 / (model.a00 * model.a00 * model.beta02)) * model.c1 * tsq

        val a = model.a00 * Math.pow(1.0 - model.c1 * deltaMin, 2.0)
        val e = (model.e0 - model.bstar * model.c4 * deltaMin - model.bstar * model.c5 * tsq).coerceIn(1e-6, 0.999)
        val beta2 = 1.0 - e * e

        // Solve Kepler's Equation M = E - e*sin(E)
        var uM = (m % (2.0 * PI) + 2.0 * PI) % (2.0 * PI)
        var epw = uM
        for (iter in 0 until 12) {
            val f = epw - e * sin(epw) - uM
            val fDot = 1.0 - e * cos(epw)
            val delta = f / fDot
            epw -= delta
            if (abs(delta) < 1e-11) break
        }

        val sinE = sin(epw)
        val cosE = cos(epw)
        val sinV = (sqrt(beta2) * sinE) / (1.0 - e * cosE)
        val cosV = (cosE - e) / (1.0 - e * cosE)
        val v = atan2(sinV, cosV)
        val u = omega + v
        val r = a * (1.0 - e * cosE)

        // Short-period perturbations
        val x2u = 2.0 * u
        val sin2u = sin(x2u)
        val cos2u = cos(x2u)
        val rk = r * (1.0 - 1.5 * CK2 * sqrt(beta2) / (a * a * beta2 * beta2) * (3.0 * model.theta2 - 1.0)) +
            0.5 * CK2 / (a * beta2 * beta2) * (1.0 - model.theta2) * cos2u
        val uk = u - 0.25 * CK2 / (a * a * beta2 * beta2) * (7.0 * model.theta2 - 1.0) * sin2u
        val xnodek = xnode + 1.5 * CK2 * model.cosI0 / (a * a * beta2 * beta2) * sin2u
        val xinck = model.i0 + 1.5 * CK2 * model.cosI0 * model.sinI0 / (a * a * beta2 * beta2) * cos2u

        // Unit orientation vectors in TEME
        val sinUk = sin(uk)
        val cosUk = cos(uk)
        val sinNode = sin(xnodek)
        val cosNode = cos(xnodek)
        val sinInc = sin(xinck)
        val cosInc = cos(xinck)

        val mx = -sinNode * cosInc
        val my = cosNode * cosInc
        val mz = sinInc

        val ux = mx * sinUk + cosNode * cosUk
        val uy = my * sinUk + sinNode * cosUk
        val uz = mz * sinUk

        val posKm = rk * RE_KM
        return Vector3D(ux * posKm, uy * posKm, uz * posKm)
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
        val nextSatTeme = propagateTeme(tle, nextTime)
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
