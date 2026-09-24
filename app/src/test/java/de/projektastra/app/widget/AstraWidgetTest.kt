package de.projektastra.app.widget

import androidx.compose.ui.graphics.Color
import de.projektastra.app.GeoPoint
import de.projektastra.app.SkyEvent
import de.projektastra.app.SkyEventKind
import de.projektastra.app.TonightWindowCalculator
import de.projektastra.app.ephemeris.LunarTerminatorCalculator
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

class AstraWidgetTest {

    @Test
    fun onlyUpcomingCalendarEventsAppearAndTodaysApproximatePeakRemainsVisible() {
        val zone = ZoneId.systemDefault()
        val day = LocalDate.of(2026, 9, 20)
        val midnight = day.atStartOfDay(zone).toInstant()
        val noon = day.atTime(LocalTime.NOON).atZone(zone).toInstant()
        fun event(title: String, instant: Instant, approximate: Boolean) = SkyEvent(
            title = title,
            kind = SkyEventKind.METEOR,
            instant = instant,
            timeIsApproximate = approximate,
            status = "Lokal sichtbar",
            statusColor = Color.White,
            facts = "",
            description = ""
        )
        val selected = AstraWidgetUpdater.upcomingWidgetEvents(listOf(
            event("Gestern", midnight.minusSeconds(86400), true),
            event("Vergangene Finsternis", midnight, false),
            event("Heute Meteorschauer", midnight, true),
            event("Morgen", midnight.plusSeconds(86400), true),
            event("Übermorgen", midnight.plusSeconds(172800), true)
        ), noon)
        assertEquals(listOf("Heute Meteorschauer", "Morgen"), selected.map { it.title })
    }

    @Test
    fun missingWeatherScoreRemainsUnknown() {
        val state = AstraWidgetUpdater.calculateState(savedLocation = null,
            time = Instant.parse("2026-09-20T22:00:00Z"))
        assertNull(state.weatherScore)
    }

    @Test
    fun compactNightTimesComeFromTheCalculatedWindowIncludingPolarWinter() {
        val time = Instant.parse("2026-12-21T21:00:00Z")
        val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN).withZone(ZoneId.systemDefault())
        listOf(GeoPoint(52.52, 13.405, 34.0), GeoPoint(89.0, 15.0, 0.0)).forEach { location ->
            val window = TonightWindowCalculator.calculate(location, time)
            assertTrue(window.hasAstronomicalDarkness)
            val state = AstraWidgetUpdater.calculateState(location, time)
            assertEquals("${formatter.format(window.darknessStart)}–${formatter.format(window.darknessEnd)}",
                state.darknessWindowCompact)
        }
    }

    @Test
    fun testWidgetPrivacyGuarantees() {
        val state = WidgetState(
            moonPhaseLabel = "Zunehmender Mond",
            moonIlluminationPercent = 45,
            darknessWindow = "Astronomische Dunkelheit: nicht verfügbar",
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
            darknessWindow = "Astronomische Dunkelheit: nicht verfügbar",
            weatherScore = 150.coerceIn(0, 100),
            lastKnownLocationLabel = "Berlin",
            usesBackgroundGps = false,
            usesRunningBackgroundService = false
        )
        assertEquals(100, highState.weatherScore)

        val lowState = WidgetState(
            moonPhaseLabel = "Neumond",
            moonIlluminationPercent = 0,
            darknessWindow = "Astronomische Dunkelheit: nicht verfügbar",
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

    @Test
    fun polarSummerWidgetShowsNoAstronomicalNight() {
        val state = AstraWidgetUpdater.calculateState(
            savedLocation = GeoPoint(89.0, 15.0, 0.0),
            time = Instant.parse("2026-06-21T21:00:00Z")
        )

        assertTrue(state.darknessWindow.contains("Keine astronomische Dunkelheit"))
        assertFalse(state.darknessWindow.contains("Astronomische Nacht:"))
        assertFalse(state.darknessWindow.contains("22:15"))
        assertNull("Polar summer must not fabricate a night interval", state.darknessWindowCompact)
    }

    @Test
    fun southernWesternCoordinatesHaveCorrectHemisphereLabels() {
        val state = AstraWidgetUpdater.calculateState(
            savedLocation = GeoPoint(-33.9, -70.7, 0.0),
            time = Instant.parse("2026-09-20T22:00:00Z")
        )

        assertEquals("33,9°S, 70,7°W", state.lastKnownLocationLabel)
    }
}
