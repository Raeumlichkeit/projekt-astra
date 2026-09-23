package de.projektastra.app.ephemeris

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.math.abs

class SaturnSystemCalculatorTest {

    @Test
    fun `ring opening angle agrees with astronomical illumination`() {
        val instant = Instant.parse("2026-09-20T00:00:00Z")
        val saturn = SaturnSystemCalculator.calculate(instant)

        assertEquals(-7.96, saturn.ringTiltDegrees, 0.1)
        assertTrue(saturn.distanceAu in 8.0..11.0)
    }

    @Test
    fun `ring opening angle reflects IAU convention with northern maximum positive`() {
        val instant2017 = Instant.parse("2017-07-01T00:00:00Z")
        val saturn2017 = SaturnSystemCalculator.calculate(instant2017)

        assertEquals(26.67, saturn2017.ringTiltDegrees, 0.1)
        assertTrue(saturn2017.ringTiltDegrees > 0.0)
    }

    @Test
    fun `Titan orbital elongation bounds`() {
        val start = Instant.parse("2026-09-20T00:00:00Z")
        for (day in 0..16) {
            val state = SaturnSystemCalculator.calculate(start.plusSeconds(day * 86400L))
            assertTrue(
                "Titan offset out of expected range at day $day: ${state.titanOffsetRS}",
                state.titanOffsetRS in 2.0..21.0
            )
            assertTrue(
                "Position angle out of bounds [0, 360): ${state.titanPositionAngle}",
                state.titanPositionAngle in 0.0..360.0
            )
        }
    }

    @Test
    fun `Titan greatest elongation position angles`() {
        // Day +4: 2026-09-24T00:00:00Z -> Eastern elongation (~90°)
        val eastern = SaturnSystemCalculator.calculate(Instant.parse("2026-09-24T00:00:00Z"))
        assertTrue("Expected Titan eastern offset near 20 RS: ${eastern.titanOffsetRS}", eastern.titanOffsetRS > 18.0)
        assertTrue("Expected PA near 90 deg: ${eastern.titanPositionAngle}", abs(eastern.titanPositionAngle - 90.6) < 15.0)

        // Day +12: 2026-10-02T00:00:00Z -> Western elongation (~270°)
        val western = SaturnSystemCalculator.calculate(Instant.parse("2026-10-02T00:00:00Z"))
        assertTrue("Expected Titan western offset near 20 RS: ${western.titanOffsetRS}", western.titanOffsetRS > 18.0)
        assertTrue("Expected PA near 270 deg: ${western.titanPositionAngle}", abs(western.titanPositionAngle - 271.5) < 15.0)
    }

    @Test
    fun `Titan 15_95 day orbital periodicity`() {
        val t0 = Instant.parse("2026-09-20T00:00:00Z")
        // 15.94542 days = 15 days, 22 hours, 41 minutes = 1,377,684 seconds
        val periodSeconds = (15.94542 * 86400).toLong()
        val t1 = t0.plusSeconds(periodSeconds)

        val s0 = SaturnSystemCalculator.calculate(t0)
        val s1 = SaturnSystemCalculator.calculate(t1)

        assertEquals(s0.titanOffsetRS, s1.titanOffsetRS, 0.5)
        val paDiff = abs(s0.titanPositionAngle - s1.titanPositionAngle).let { if (it > 180.0) 360.0 - it else it }
        assertTrue("Titan PA diff after one period too large: $paDiff", paDiff < 5.0)
    }
}
