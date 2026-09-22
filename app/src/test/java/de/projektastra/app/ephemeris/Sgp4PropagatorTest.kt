package de.projektastra.app.ephemeris

import de.projektastra.app.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
