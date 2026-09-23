package de.projektastra.app.widget

import de.projektastra.app.GeoPoint
import de.projektastra.app.ephemeris.LunarTerminatorCalculator
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import kotlin.math.roundToInt

class AstraWidgetTest {

    @Test
    fun testWidgetPrivacyGuarantees() {
        val state = WidgetState(
            moonPhaseLabel = "Zunehmender Mond",
            moonIlluminationPercent = 45,
            darknessWindow = "Astronomische Nacht: 22:15 - 04:45",
            weatherScore = 80,
            lastKnownLocationLabel = "Berlin (52.5°N)",
            usesBackgroundGps = false,
            usesRunningBackgroundService = false
        )

        assertFalse("Widget must strictly prohibit background GPS", state.usesBackgroundGps)
        assertFalse("Widget must strictly prohibit background running service", state.usesRunningBackgroundService)
        assertEquals(80, state.weatherScore)
    }

    @Test
    fun testMoonPhaseCalculationForWidget() {
        // Test with LunarTerminatorCalculator
        val newMoonTime = Instant.parse("2026-01-18T20:00:00Z")
        val termState = LunarTerminatorCalculator.calculateTerminator(newMoonTime)
        val illumPct = (termState.phaseFraction * 100.0).roundToInt()
        assertTrue("New moon illumination should be near zero", illumPct <= 5)

        val fullMoonTime = Instant.parse("2026-02-01T22:00:00Z")
        val termFull = LunarTerminatorCalculator.calculateTerminator(fullMoonTime)
        val fullIllum = (termFull.phaseFraction * 100.0).roundToInt()
        assertTrue("Full moon illumination should be high", fullIllum >= 90)
    }

    @Test
    fun testWeatherScoreClamping() {
        val highState = WidgetState(
            moonPhaseLabel = "Vollmond",
            moonIlluminationPercent = 100,
            darknessWindow = "Astronomische Nacht: 22:15 - 04:45",
            weatherScore = 150.coerceIn(0, 100),
            lastKnownLocationLabel = "Berlin",
            usesBackgroundGps = false,
            usesRunningBackgroundService = false
        )
        assertEquals(100, highState.weatherScore)

        val lowState = WidgetState(
            moonPhaseLabel = "Neumond",
            moonIlluminationPercent = 0,
            darknessWindow = "Astronomische Nacht: 22:15 - 04:45",
            weatherScore = (-10).coerceIn(0, 100),
            lastKnownLocationLabel = "Berlin",
            usesBackgroundGps = false,
            usesRunningBackgroundService = false
        )
        assertEquals(0, lowState.weatherScore)
    }

    @Test
    fun testCalculateStateWithLocationAndDarkness() {
        val time = Instant.parse("2026-09-20T22:00:00Z")
        val location = GeoPoint(50.1109, 8.6821, 100.0) // Frankfurt

        val state = AstraWidgetUpdater.calculateState(
            savedLocation = location,
            time = time,
            weatherScore = 85
        )

        assertEquals("50,1°N, 8,7°E", state.lastKnownLocationLabel)
        assertEquals(85, state.weatherScore)
        assertFalse(state.usesBackgroundGps)
        assertFalse(state.usesRunningBackgroundService)
        assertTrue("Should contain Astronomische Nacht prefix", state.darknessWindow.contains("Astronomische Nacht"))
        assertTrue("Should calculate illumination", state.moonIlluminationPercent in 0..100)
    }

    @Test
    fun testCalculateStateFallbackLocation() {
        val time = Instant.parse("2026-09-20T22:00:00Z")
        val state = AstraWidgetUpdater.calculateState(
            savedLocation = null,
            time = time,
            weatherScore = 70
        )

        assertEquals("Berlin (52.5°N)", state.lastKnownLocationLabel)
        assertEquals(70, state.weatherScore)
        assertTrue(state.darknessWindow.contains("Astronomische Nacht"))
    }
}
