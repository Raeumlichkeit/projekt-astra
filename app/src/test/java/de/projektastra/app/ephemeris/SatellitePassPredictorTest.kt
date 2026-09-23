package de.projektastra.app.ephemeris

import de.projektastra.app.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SatellitePassPredictorTest {

    private val issTle = TleParser.parseTle(
        "1 25544U 98067A   26263.51829341  .00016717  00000-0  10270-3 0  9993",
        "2 25544  51.6416 247.4627 0006703 130.5360 325.0288 15.72125391563537",
        "ISS (ZARYA)"
    )

    private val predictor = SatellitePassPredictor()

    @Test
    fun `predicts passes and verifies geometric properties`() {
        val observer = GeoPoint(52.52, 13.405, 34.0) // Berlin
        val startTime = issTle.epochInstant
        val propagator = Sgp4Propagator()
        // Predict over 72 hours (plenty of passes for an orbit with period ~92m and inc 51.6°)
        val passes = predictor.predictPasses(issTle, observer, startTime, durationHours = 72, minElevationDegrees = 10.0)

        assertTrue("Expected multiple passes over 72 hours: ${passes.size}", passes.isNotEmpty())

        passes.forEach { pass ->
            assertEquals("ISS (ZARYA)", pass.satelliteName)
            assertEquals(25544, pass.noradId)
            assertTrue("Rise before max: ${pass.riseTime} vs ${pass.maxElevationTime}",
                pass.riseTime <= pass.maxElevationTime)
            assertTrue("Max before set: ${pass.maxElevationTime} vs ${pass.setTime}",
                pass.maxElevationTime <= pass.setTime)
            assertTrue("Peak elevation above threshold: ${pass.maxElevation}",
                pass.maxElevation >= 10.0)
            assertTrue("Pass duration must be positive and at most 20 minutes: ${pass.durationSeconds}",
                pass.durationSeconds in 1L..1200L)
            val riseAltitude = propagator.propagate(issTle, pass.riseTime, observer)!!.coordinates.altitude
            val setAltitude = propagator.propagate(issTle, pass.setTime, observer)!!.coordinates.altitude
            assertEquals("Rise crosses the 10-degree threshold", 10.0, riseAltitude, 0.2)
            assertEquals("Set crosses the 10-degree threshold", 10.0, setAltitude, 0.2)
            assertTrue("Max magnitude within visual bounds: ${pass.maxMagnitude}",
                pass.maxMagnitude in -4.0..4.0)

            assertTrue("Track points non-empty", pass.trackPoints.isNotEmpty())
            assertTrue("Time markers non-empty", pass.timeMarkers.isNotEmpty())
        }
    }

    @Test
    fun `satellite catalog freshness check`() {
        val now = Instant.parse("2026-09-21T00:00:00Z")
        val freshTle = issTle.copy(epochInstant = now.minusSeconds(2 * 86400L))
        val goodTle = issTle.copy(epochInstant = now.minusSeconds(15 * 86400L))
        val staleTle = issTle.copy(epochInstant = now.minusSeconds(40 * 86400L))

        assertEquals(TleFreshnessStatus.EXCELLENT, SatelliteCatalog.checkFreshness(freshTle, now))
        assertEquals(TleFreshnessStatus.GOOD, SatelliteCatalog.checkFreshness(goodTle, now))
        val staleResult = SatelliteCatalog.checkFreshness(staleTle, now)
        assertTrue(staleResult is TleFreshnessStatus.STALE)
        assertEquals(40, (staleResult as TleFreshnessStatus.STALE).daysOld)
    }
}
