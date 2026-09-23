package de.projektastra.app.ephemeris

import de.projektastra.app.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.Instant

class Sgp4PropagatorTest {

    private val propagator = Sgp4Propagator()
    private val issTle = TleParser.parseTle(
        "1 25544U 98067A   26263.51829341  .00016717  00000-0  10270-3 0  9993",
        "2 25544  51.6416 247.4627 0006703 130.5360 325.0288 15.72125391563537",
        "ISS (ZARYA)"
    )

    @Test
    fun `published ISS TEME position matches near Earth branch`() {
        // python-sgp4 2.2 documentation, WGS-72 ISS example at JD 2458827.362605.
        // https://pypi.org/project/sgp4/2.2/
        val tle = TleParser.parseTle(
            "1 25544U 98067A   19343.69339541  .00001764  00000-0  38792-4 0  9991",
            "2 25544  51.6439 211.2001 0007417  17.6667  85.6398 15.50103472202482"
        )
        val pos = propagator.propagateTeme(tle, Instant.parse("2019-12-09T20:42:09.072Z"))
        assertEquals(-6102.44, pos.x, 0.03)
        assertEquals(-986.33, pos.y, 0.03)
        assertEquals(-2820.31, pos.z, 0.03)
    }

    @Test
    fun `bundled Tiangong and Hubble TLEs remain in supported LEO range`() {
        val lines = listOf(
            "1 48274U 21035A   26263.48912037  .00021480  00000-0  14562-3 0  9995" to
                "2 48274  41.4728 185.1245 0005128  84.2154 275.9241 15.60214820295127",
            "1 20580U 90037B   26263.31428519  .00001428  00000-0  48210-4 0  9990" to
                "2 20580  28.4688  72.1945 0002841 112.5482 247.5912 15.09142871784916"
        )
        lines.forEach { (line1, line2) ->
            val tle = TleParser.parseTle(line1, line2)
            assertTrue(!tle.isDeepSpace)
            for (hours in listOf(0L, 6L, 24L)) {
                val radius = propagator.propagateTeme(tle, tle.epochInstant.plusSeconds(hours * 3600)).magnitude
                assertTrue("${tle.satelliteName} at +$hours h: $radius km", radius in 6600.0..7300.0)
            }
        }
    }

    @Test
    fun `Vallado eccentric near Earth vectors match published TEME positions`() {
        // AIAA 2006-6753 SGP4-VER.TLE / tcppver.out, satellite 00005.
        // https://github.com/aholinch/sgp4/blob/master/data/tcppver.out
        val tle = TleParser.parseTle(
            "1 00005U 58002B   00179.78495062  .00000023  00000-0  28098-4 0  4753",
            "2 00005  34.2682 348.7242 1859667 331.7664  19.3264 10.82419157413667"
        )
        val vectors = listOf(
            0L to Vector3D(7022.46529266, -1400.08296755, 0.03995155),
            360L to Vector3D(-7154.03120202, -3783.17682504, -3536.19412294),
            720L to Vector3D(-7134.59340119, 6531.68641334, 3260.27186483),
            1440L to Vector3D(-938.55923943, -6268.18748831, -4294.02924751)
        )
        vectors.forEach { (minutes, expected) ->
            val actual = propagator.propagateTeme(tle, tle.epochInstant.plusSeconds(minutes * 60))
            assertTrue("00005 at +$minutes min: ${(actual - expected).magnitude} km",
                (actual - expected).magnitude < 0.02)
        }
    }

    @Test
    fun `Vallado low eccentricity drag vectors match published TEME positions`() {
        // AIAA 2006-6753 SGP4-VER.TLE / tcppver.out, satellite 06251.
        val tle = TleParser.parseTle(
            "1 06251U 62025E   06176.82412014  .00008885  00000-0  12808-3 0  3985",
            "2 06251  58.0579  54.0425 0030035 139.1568 221.1854 15.56387291  6774"
        )
        val vectors = listOf(
            0L to Vector3D(3988.31022699, 5498.96657235, 0.90055879),
            120L to Vector3D(-3935.69800083, 409.10980837, 5471.33577327),
            720L to Vector3D(3692.60030028, -976.24265255, -5623.36447493),
            1440L to Vector3D(-2777.14682335, -5663.16031708, -2462.54889123)
        )
        vectors.forEach { (minutes, expected) ->
            val actual = propagator.propagateTeme(tle, tle.epochInstant.plusSeconds(minutes * 60))
            assertTrue("06251 at +$minutes min: ${(actual - expected).magnitude} km",
                (actual - expected).magnitude < 0.02)
        }
    }

    @Test
    fun `deep space input is rejected instead of using near Earth equations`() {
        val tle = issTle.copy(meanMotionRevsPerDay = 2.006)
        try {
            propagator.propagateTeme(tle, tle.epochInstant)
            fail("Expected explicit deep-space rejection")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("Deep-space"))
        }
        assertEquals(null, propagator.propagate(tle, tle.epochInstant, GeoPoint(52.52, 13.405, 34.0)))
    }

    @Test
    fun `propagateTeme returns consistent LEO position vector`() {
        val t0 = issTle.epochInstant
        val pos0 = propagator.propagateTeme(issTle, t0)

        // Orbital radius of ISS: ~6780 km (400 km altitude + 6378 km Earth radius)
        val r0 = pos0.magnitude
        assertTrue("ISS orbital radius ($r0 km) must be within 6600..7000 km", r0 in 6600.0..7000.0)

        // Propagate forward by 45 minutes (approx half orbit)
        val tHalf = t0.plusSeconds(45 * 60L)
        val posHalf = propagator.propagateTeme(issTle, tHalf)
        val rHalf = posHalf.magnitude
        assertTrue("Radius after 45 min must stay in LEO bounds: $rHalf", rHalf in 6600.0..7000.0)

        // Dot product between opposite sides of orbit should be strongly negative
        val dot = pos0.dot(posHalf)
        assertTrue("Opposite points on circular orbit should have negative dot product: $dot", dot < 0)
    }

    @Test
    fun `coordinate frame rotation via GMST preserves vector norm`() {
        val vec = Vector3D(4000.0, -3000.0, 5000.0)
        val gmst = propagator.gmst(Instant.parse("2026-09-20T12:00:00Z"))
        val rotated = propagator.temeToEcef(vec, gmst)

        assertEquals("Vector magnitude must be invariant under frame rotation",
            vec.magnitude, rotated.magnitude, 1e-9)
    }

    @Test
    fun `topocentric SEZ coordinates at zenith evaluate to 90 deg elevation`() {
        val observer = GeoPoint(52.52, 13.405, 50.0)
        val obsEcef = propagator.observerEcef(observer)

        // Place a synthetic satellite 400 km directly above the observer along the geodetic zenith vector
        val latRad = Math.toRadians(observer.latitude)
        val lonRad = Math.toRadians(observer.longitude)
        val zenithUnit = Vector3D(kotlin.math.cos(latRad) * kotlin.math.cos(lonRad), kotlin.math.cos(latRad) * kotlin.math.sin(lonRad), kotlin.math.sin(latRad))
        val satEcef = obsEcef + (zenithUnit * 400.0)

        val (az, alt, range) = propagator.ecefToTopocentric(satEcef, obsEcef, observer)

        assertEquals(90.0, alt, 0.01)
        assertEquals(400.0, range, 0.01)
        assertTrue(az in 0.0..360.0)
    }

    @Test
    fun `solar illumination distinguishes sunlit from eclipsed states`() {
        val sunDir = Vector3D(1.0, 0.0, 0.0) // Sun along +X

        // Satellite on day side (+X)
        val daySat = Vector3D(6800.0, 0.0, 0.0)
        assertEquals(IlluminationStatus.SUNLIT, propagator.checkIllumination(daySat, sunDir))

        // Satellite deep in Earth umbra (-X, zero perp distance)
        val nightSat = Vector3D(-6800.0, 0.0, 0.0)
        assertEquals(IlluminationStatus.ECLIPSED, propagator.checkIllumination(nightSat, sunDir))

        // Satellite at high inclination receiving sun while over night side
        val polarHighSat = Vector3D(-6800.0, 0.0, 8000.0)
        assertEquals(IlluminationStatus.SUNLIT, propagator.checkIllumination(polarHighSat, sunDir))
    }

    @Test
    fun `propagate topocentric returns complete physical state`() {
        val observer = GeoPoint(52.52, 13.405, 34.0) // Berlin
        val t = issTle.epochInstant
        val pos = propagator.propagate(issTle, t, observer)

        assertNotNull(pos)
        assertTrue(pos!!.coordinates.azimuth in 0.0..360.0)
        assertTrue(pos.coordinates.altitude in -90.0..90.0)
        assertTrue(pos.rangeKm > 300.0)
        assertTrue("Altitude was ${pos.altitudeAboveEarthKm}", pos.altitudeAboveEarthKm in 340.0..500.0)
    }

    @Test
    fun `generatePassTrack creates sampled track points`() {
        val observer = GeoPoint(52.52, 13.405, 34.0)
        val track = propagator.generatePassTrack(issTle, issTle.epochInstant, durationMinutes = 10, observer = observer)

        // 10 minutes sampled every 10s = 61 points
        assertEquals(61, track.size)
        track.forEach { pt ->
            assertTrue(pt.position.azimuth in 0.0..360.0)
            assertTrue(pt.position.altitude in -90.0..90.0)
            assertTrue(pt.rangeKm > 0.0)
        }
    }
}
