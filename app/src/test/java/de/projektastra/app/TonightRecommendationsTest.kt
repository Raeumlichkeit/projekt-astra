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
        assertTrue(window.hasAstronomicalDarkness)
        assertTrue(window.darknessEnd.isAfter(window.darknessStart))
        assertTrue(SolarSystemCatalog.horizontal(Body.Sun, berlin, window.darknessStart).altitude <= -18.0)
        assertTrue(SolarSystemCatalog.horizontal(Body.Sun, berlin, window.darknessEnd).altitude <= -18.0)
        assertTrue(window.nightDateText.isNotEmpty())
        assertTrue("SunsetText should have HH:mm format", window.sunsetText.contains(":"))
        assertTrue("SunrisesText should have HH:mm format", window.sunrisesText.contains(":"))
        assertTrue("DarknessText should contain time range", window.darknessText.contains("–") || window.darknessText.contains("Dunkelheit"))
        assertTrue("MoonPhasePercent between 0 and 100", window.moonPhasePercent in 0..100)
        assertTrue("MoonPhaseName should not be empty", window.moonPhaseName.isNotEmpty())
        assertTrue("Offline notice when weather is null", window.weatherNotice.contains("offline", ignoreCase = true))
    }

    @Test
    fun berlinMidsummer_hasNoAstronomicalWindowEvenWithClearWeather() {
        val instant = Instant.parse("2026-06-21T20:00:00Z")
        val weather = WeatherSnapshot(
            temperature = 18.0,
            cloudCover = 0,
            windSpeed = 1.0,
            visibility = 20000.0,
            updatedAt = "21.06. 22:00 CEST",
            forecast = listOf(HourlyForecast("23:00", 1, 0, 0, 1.0, 20000.0, instant.plusSeconds(3600))),
            observedAt = instant,
            fetchedAt = instant
        )
        val window = TonightWindowCalculator.calculate(berlin, instant, weather, zone)

        assertFalse(window.hasAstronomicalDarkness)
        assertEquals(window.darknessStart, window.darknessEnd)
        assertTrue(window.darknessText.contains("Keine astronomische Dunkelheit"))
        assertFalse(window.bestWindowSummary.contains("Optimale"))
        assertTrue(TonightTargetEngine.evaluate(berlin, window, instant).isEmpty())
    }

    @Test
    fun polarSummer_hasNoWindowOrFalseRecommendations() {
        val observer = GeoPoint(89.0, 15.0, 0.0)
        val instant = Instant.parse("2026-06-21T21:00:00Z")
        val window = TonightWindowCalculator.calculate(observer, instant, zone = ZoneId.of("Arctic/Longyearbyen"))

        assertFalse(window.hasAstronomicalDarkness)
        assertTrue(window.sunsetText.contains("Polartag"))
        assertTrue(window.sunrisesText.contains("Polartag"))
        assertTrue(TonightTargetEngine.evaluate(observer, window, instant).isEmpty())
    }

    @Test
    fun polarWinter_hasFullAstronomicalWindowAndNoInventedSunEvents() {
        val observer = GeoPoint(89.0, 15.0, 0.0)
        val instant = Instant.parse("2026-12-21T21:00:00Z")
        val arcticZone = ZoneId.of("Arctic/Longyearbyen")
        val window = TonightWindowCalculator.calculate(observer, instant, zone = arcticZone)

        assertTrue(window.hasAstronomicalDarkness)
        assertEquals(18 * 3600L, window.darknessEnd.epochSecond - window.darknessStart.epochSecond)
        assertTrue(SolarSystemCatalog.horizontal(Body.Sun, observer, window.darknessStart).altitude <= -18.0)
        assertTrue(SolarSystemCatalog.horizontal(Body.Sun, observer, window.darknessEnd).altitude <= -18.0)
        assertTrue(window.darknessText.contains("durchgehend"))
        assertTrue(window.sunsetText.contains("Polarnacht"))
        assertTrue(window.sunrisesText.contains("Polarnacht"))
    }

    @Test
    fun moonPhaseDistinguishesWaxingAndWaning() {
        val waxing = TonightWindowCalculator.calculate(berlin, Instant.parse("2026-01-25T20:00:00Z"), zone = zone)
        val waning = TonightWindowCalculator.calculate(berlin, Instant.parse("2026-02-09T20:00:00Z"), zone = zone)

        assertTrue(waxing.moonPhaseName.startsWith("Zunehm") || waxing.moonPhaseName == "Erstes Viertel")
        assertTrue(waning.moonPhaseName.startsWith("Abnehm") || waning.moonPhaseName == "Letztes Viertel")
    }

    @Test
    fun localSixAmBoundarySelectsCorrectNightAcrossDstChange() {
        val before = TonightWindowCalculator.calculate(berlin, Instant.parse("2026-10-25T04:30:00Z"), zone = zone)
        val after = TonightWindowCalculator.calculate(berlin, Instant.parse("2026-10-25T05:30:00Z"), zone = zone)

        assertTrue(before.nightDateText.contains("24. Oktober"))
        assertTrue(after.nightDateText.contains("25. Oktober"))
        assertTrue(before.hasAstronomicalDarkness)
        assertTrue(after.hasAstronomicalDarkness)
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
                "Telescope filter should allow telescope, binocular, planets, or moon targets",
                target.equipment == ObservationEquipment.TELESCOPE ||
                    target.equipment == ObservationEquipment.BINOCULARS ||
                    target.objectData.objectType == CelestialType.PLANET ||
                    target.objectData.objectType == CelestialType.MOON
            )
        }

        // Verify Moon target has suppressed moonSeparationDegrees
        val moonTarget = allTargets.firstOrNull { it.objectData.solarBody == Body.Moon }
        if (moonTarget != null) {
            assertEquals(-1.0, moonTarget.moonSeparationDegrees, 0.001)
            assertFalse("Moon reason should not mention 0° moon separation", moonTarget.reason.contains("0°"))
        }
    }

    @Test
    fun tonightWindowCalculator_morningPlanningSelectsUpcomingNight() {
        // At 08:30 AM local time, user is planning the coming evening (today), not yesterday
        val morningInstant = Instant.parse("2026-10-15T06:30:00Z") // 08:30 CEST in Berlin
        val window = TonightWindowCalculator.calculate(
            observer = berlin,
            now = morningInstant,
            weather = null,
            zone = zone
        )
        assertTrue(
            "Window for morning planning should select 15. Oktober",
            window.nightDateText.contains("15. Oktober")
        )
    }

    @Test
    fun tonightRecommendations_curatedCatalogIdsMatchStandardCatalogs() {
        val testInstant = Instant.parse("2026-10-15T20:00:00Z")
        val window = TonightWindowCalculator.calculate(
            observer = berlin,
            now = testInstant,
            weather = null,
            zone = zone
        )
        val targets = TonightTargetEngine.evaluate(
            observer = berlin,
            window = window,
            now = testInstant,
            equipmentFilter = ObservationEquipment.ALL,
            zone = zone
        )
        for (t in targets) {
            val id = t.objectData.catalogId
            assertTrue("catalogId should be valid: $id", id.isNotEmpty())
            if (t.objectData.solarBody != null) {
                assertTrue("Solar body catalogId: $id", id.startsWith("Astronomy Engine · "))
            } else if (id.startsWith("HIP ")) {
                assertTrue("HIP ID format: $id", id.removePrefix("HIP ").toIntOrNull() != null)
            } else {
                // Deep sky objects should use exact catalog naming (e.g. M 31 · NGC 224, NGC 869)
                assertTrue("Deep sky catalogId format: $id", id.startsWith("M ") || id.startsWith("NGC "))
            }
        }
    }
}

