package de.projektastra.app.ephemeris

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class JupiterMoonsCalculatorTest {

    @Test
    fun `computed moon offsets remain within physical bounds`() {
        val now = Instant.parse("2026-09-20T12:00:00Z")
        val state = JupiterMoonsCalculator.calculate(now)

        assertEquals(4, state.moons.size)
        assertEquals("Io", state.io.name)
        assertEquals("Europa", state.europa.name)
        assertEquals("Ganymede", state.ganymede.name)
        assertEquals("Callisto", state.callisto.name)

        assertTrue(state.distanceAu in 4.0..6.5)
        assertTrue(state.angularDiameterArcsec in 30.0..55.0)

        state.moons.forEach { moon ->
            assertTrue("${moon.name} offset out of bounds: ${moon.offsetRJ}", moon.offsetRJ in -30.0..30.0)
            assertTrue("${moon.name} z out of bounds: ${moon.zAU}", kotlin.math.abs(moon.zAU) < 0.03)
        }
    }

    @Test
    fun `detects Io eclipse at verified timestamp`() {
        val instant = Instant.parse("2026-09-20T13:45:00Z")
        val jupiter = JupiterMoonsCalculator.calculate(instant)

        assertEquals(JupiterMoonEvent.ECLIPSE, jupiter.io.event)
        assertTrue(jupiter.io.isInEclipse)
        assertTrue(jupiter.activeEvents.any { it.first.name == "Io" && it.second == JupiterMoonEvent.ECLIPSE })
    }

    @Test
    fun `detects Io occultation at verified timestamp`() {
        val instant = Instant.parse("2026-09-20T15:30:00Z")
        val jupiter = JupiterMoonsCalculator.calculate(instant)

        assertEquals(JupiterMoonEvent.OCCULTATION, jupiter.io.event)
        assertTrue(jupiter.io.zAU > 0) // Behind Jupiter
    }

    @Test
    fun `MoonState preserves isInEclipse boolean flag during shadow cone passage`() {
        val instant = Instant.parse("2026-09-20T13:45:00Z")
        val jupiter = JupiterMoonsCalculator.calculate(instant)
        assertTrue(jupiter.io.isInEclipse)
    }

    @Test
    fun `detects Europa shadow transit ingress before moon transit`() {
        val instant = Instant.parse("2026-09-21T01:30:00Z")
        val jupiter = JupiterMoonsCalculator.calculate(instant)

        assertEquals(JupiterMoonEvent.SHADOW_TRANSIT, jupiter.europa.event)
        assertTrue(jupiter.europa.isShadowTransiting)
        assertNotNull(jupiter.europa.shadowOffsetRJ)
        assertTrue(kotlin.math.abs(jupiter.europa.shadowOffsetRJ!!) <= 1.0)
    }

    @Test
    fun `detects Europa transit at verified timestamp`() {
        val instant = Instant.parse("2026-09-21T04:00:00Z")
        val jupiter = JupiterMoonsCalculator.calculate(instant)

        assertEquals(JupiterMoonEvent.TRANSIT, jupiter.europa.event)
        assertTrue(jupiter.europa.zAU < 0) // In front of Jupiter
    }

    @Test
    fun `orbital motion advances over time`() {
        val t0 = Instant.parse("2026-09-20T00:00:00Z")
        val t1 = Instant.parse("2026-09-20T06:00:00Z")

        val state0 = JupiterMoonsCalculator.calculate(t0)
        val state1 = JupiterMoonsCalculator.calculate(t1)

        // Io orbital period is ~1.77 days (42.5h), in 6h it moves significantly
        assertTrue(kotlin.math.abs(state1.io.offsetRJ - state0.io.offsetRJ) > 0.5)
    }
}
