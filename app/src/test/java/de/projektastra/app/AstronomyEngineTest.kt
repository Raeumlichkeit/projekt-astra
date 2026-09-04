package de.projektastra.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class AstronomyEngineTest {
    @Test
    fun `computed coordinates remain in valid ranges`() {
        val sirius = CelestialObject("Sirius", "HIP 32349", 6.7525, -16.7161, -1.46, 8.6, "A1V")
        val result = AstronomyEngine.horizontalCoordinates(
            sirius,
            GeoPoint(52.52, 13.405, 34.0),
            Instant.parse("2026-01-15T22:00:00Z")
        )

        assertTrue(result.azimuth in 0.0..360.0)
        assertTrue(result.altitude in -90.0..90.0)
    }
}

