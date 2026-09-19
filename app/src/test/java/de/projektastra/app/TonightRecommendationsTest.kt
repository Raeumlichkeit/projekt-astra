package de.projektastra.app

import io.github.cosinekitty.astronomy.Body
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class TonightRecommendationsTest {

    private val berlin = GeoPoint(52.52, 13.405, 34.0)
    private val zone = ZoneId.of("Europe/Berlin")

    @Test
    fun angularDistanceDegrees_computesExpectedAngles() {
        // Same coordinates -> 0 deg
        val distZero = TonightTargetEngine.angularDistanceDegrees(12.0, 45.0, 12.0, 45.0)
        assertEquals(0.0, distZero, 0.001)

        // 90 degrees away along equator
        val dist90 = TonightTargetEngine.angularDistanceDegrees(0.0, 0.0, 6.0, 0.0)
        assertEquals(90.0, dist90, 0.001)

        // Opposite poles -> 180 deg
        val distPoles = TonightTargetEngine.angularDistanceDegrees(0.0, 90.0, 0.0, -90.0)
        assertEquals(180.0, distPoles, 0.001)
    }

    @Test
    fun tonightWindowCalculator_calculatesSunAndMoonWindows() {
        // Test autumn evening: 2026-10-15 20:00 UTC
        val testInstant = Instant.parse("2026-10-15T20:00:00Z")
        val window = TonightWindowCalculator.calculate(
            observer = berlin,
            now = testInstant,
            weather = null,
            zone = zone
        )

        assertNotNull(window.nightDateText)
        assertTrue(window.nightDateText.isNotEmpty())
        assertTrue("SunsetText should have HH:mm format", window.sunsetText.contains(":"))
        assertTrue("SunrisesText should have HH:mm format", window.sunrisesText.contains(":"))
        assertTrue("DarknessText should contain time range", window.darknessText.contains("–") || window.darknessText.contains("Dunkelheit"))
        assertTrue("MoonPhasePercent between 0 and 100", window.moonPhasePercent in 0..100)
        assertTrue("MoonPhaseName should not be empty", window.moonPhaseName.isNotEmpty())
        assertTrue("Offline notice when weather is null", window.weatherNotice.contains("Offline") || window.weatherNotice.contains("astronomisch"))
    }

    @Test
    fun tonightWindowCalculator_evaluatesWeatherConditions() {
        val testInstant = Instant.parse("2026-10-15T20:00:00Z")
        val hourlyForecast = listOf(
            HourlyForecast(
                time = "22:00",
                hoursFromNow = 0,
                cloudCover = 15,
                rainProbability = 0,
                windSpeed = 5.0,
                visibility = 20000.0,
                instant = testInstant
            ),
            HourlyForecast(
                time = "23:00",
                hoursFromNow = 1,
                cloudCover = 20,
                rainProbability = 0,
                windSpeed = 4.0,
                visibility = 20000.0,
                instant = testInstant.plusSeconds(3600)
            )
        )
        val weatherSnapshot = WeatherSnapshot(
            temperature = 10.0,
            cloudCover = 15,
            windSpeed = 5.0,
            visibility = 20000.0,
            updatedAt = "15.10. 22:00 CEST",
            forecast = hourlyForecast,
            observedAt = testInstant,
            fetchedAt = testInstant
        )

        val window = TonightWindowCalculator.calculate(
            observer = berlin,
            now = testInstant,
            weather = weatherSnapshot,
            zone = zone
        )

        assertTrue(
            "Should reflect good conditions from weather",
            window.bestWindowSummary.contains("Optimale") ||
                window.bestWindowSummary.contains("Gute") ||
                window.bestWindowSummary.contains("Sicht") ||
                window.bestWindowSummary.contains("Klar")
        )
        assertTrue("Weather notice should include cloud percentage", window.weatherNotice.contains("%"))
    }

    @Test
    fun tonightTargetEngine_evaluatesAndFiltersCandidates() {
        val testInstant = Instant.parse("2026-10-15T20:00:00Z")
        val window = TonightWindowCalculator.calculate(
            observer = berlin,
            now = testInstant,
            weather = null,
            zone = zone
        )

        // Evaluate all targets
        val allTargets = TonightTargetEngine.evaluate(
            observer = berlin,
            window = window,
            now = testInstant,
            equipmentFilter = ObservationEquipment.ALL,
            zone = zone
        )

        assertFalse("Curated list should find visible targets", allTargets.isEmpty())

        for (target in allTargets) {
            // Must strictly obey minimum altitude threshold of 16 degrees
            assertTrue(
                "Peak altitude must be >= 16.0° for ${target.objectData.name}, got ${target.peakAltitudeDegrees}",
                target.peakAltitudeDegrees >= 16.0
            )
            // Quality score in reasonable range
            assertTrue(
                "Score must be between 5 and 99, got ${target.qualityScore}",
                target.qualityScore in 5..99
            )
            // Reason and highlight title should be populated
            assertTrue(target.highlightTitle.isNotEmpty())
            assertTrue(target.reason.isNotEmpty())
        }

        // Verify sorted descending by quality score
        for (i in 0 until allTargets.size - 1) {
            assertTrue(
                "Targets should be sorted descending by quality score",
                allTargets[i].qualityScore >= allTargets[i + 1].qualityScore
            )
        }

        // Evaluate equipment filtering
        val nakedEyeOnly = TonightTargetEngine.evaluate(
            observer = berlin,
            window = window,
            now = testInstant,
            equipmentFilter = ObservationEquipment.NAKED_EYE,
            zone = zone
        )
        for (target in nakedEyeOnly) {
            assertEquals(ObservationEquipment.NAKED_EYE, target.equipment)
        }

        val binoculars = TonightTargetEngine.evaluate(
            observer = berlin,
            window = window,
            now = testInstant,
            equipmentFilter = ObservationEquipment.BINOCULARS,
            zone = zone
        )
        for (target in binoculars) {
            assertTrue(
                target.equipment == ObservationEquipment.BINOCULARS ||
                    target.equipment == ObservationEquipment.NAKED_EYE
            )
        }

        val telescopes = TonightTargetEngine.evaluate(
            observer = berlin,
            window = window,
            now = testInstant,
            equipmentFilter = ObservationEquipment.TELESCOPE,
            zone = zone
        )
        for (target in telescopes) {
            assertTrue(
                target.equipment == ObservationEquipment.TELESCOPE ||
                    target.equipment == ObservationEquipment.BINOCULARS
            )
        }
    }
}
