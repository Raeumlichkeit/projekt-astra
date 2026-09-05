package de.projektastra.app

import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.localSolarEclipsesAfter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

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

    @Test
    fun `solar system ephemerides provide all major visible bodies`() {
        val observer = GeoPoint(52.52, 13.405, 34.0)
        val objects = SolarSystemCatalog.at(observer, Instant.parse("2026-09-05T20:00:00Z"))

        assertEquals(9, objects.size)
        assertTrue(objects.all { it.raHours in 0.0..24.0 })
        assertTrue(objects.all { it.decDegrees in -90.0..90.0 })
        assertTrue(objects.all { (it.distanceAu ?: 0.0) > 0.0 })
    }

    @Test
    fun `milky way projection forms a complete valid galactic band`() {
        val band = MilkyWayModel.horizontalBand(
            GeoPoint(52.52, 13.405, 34.0),
            Instant.parse("2026-09-05T20:00:00Z")
        )

        assertEquals(121, band.size)
        assertTrue(band.all { it.azimuth in 0.0..360.0 })
        assertTrue(band.all { it.altitude in -90.0..90.0 })
    }

    @Test
    fun `terrain profile interpolates across north without a seam`() {
        val profile = TerrainProfile(
            samples = listOf(
                TerrainSample(0.0, 4.0),
                TerrainSample(90.0, 8.0),
                TerrainSample(180.0, 2.0),
                TerrainSample(270.0, 6.0)
            ),
            observerElevationMeters = 100.0
        )

        assertEquals(5.0, profile.altitudeAt(315.0), 0.001)
        assertEquals(5.0, profile.altitudeAt(-45.0), 0.001)
        assertEquals(4.0, profile.altitudeAt(360.0), 0.001)
    }

    @Test
    fun `next Berlin solar eclipse agrees with NASA 2027 event date`() {
        val eclipse = localSolarEclipsesAfter(
            Time(2027, 1, 1, 0, 0, 0.0),
            Observer(52.52, 13.405, 34.0)
        ).first()
        val peakDate = Instant.ofEpochMilli(eclipse.peak.time.toMillisecondsSince1970())
            .atZone(ZoneOffset.UTC).toLocalDate()

        assertEquals("2027-08-02", peakDate.toString())
        assertTrue(eclipse.obscuration in 0.0..1.0)
        assertTrue(eclipse.partialBegin.time < eclipse.peak.time)
        assertTrue(eclipse.peak.time < eclipse.partialEnd.time)
    }

    @Test
    fun `IAU B1875 precession produces valid J2000 coordinates`() {
        val point = IauBoundaryCatalog.precessB1875ToJ2000(22.86667, 34.5)

        assertTrue(point.raHours in 0.0..24.0)
        assertTrue(point.decDegrees in -90.0..90.0)
        assertTrue(kotlin.math.abs(point.raHours - 22.86667) < 0.2)
    }

    @Test
    fun `IAU catalogue contains all 88 constellations including both Serpens regions`() {
        val text = java.io.File("src/main/assets/iau_constellation_boundaries_b1875.dat").readText()
        val boundaries = IauBoundaryCatalog.parse(text)

        assertEquals(89, boundaries.size)
        assertEquals(88, boundaries.map { it.abbreviation }.toSet().size)
        assertEquals(2, boundaries.count { it.abbreviation == "SER" })
    }

    @Test
    fun `VIIRS brightness estimate increases Bortle class and score penalty`() {
        val dark = LightPollutionRepository.estimateFromLuminance(2.0)
        val bright = LightPollutionRepository.estimateFromLuminance(250.0)
        val observer = GeoPoint(52.52, 13.405, 34.0)
        val instant = Instant.parse("2026-09-05T20:00:00Z")
        val darkScore = AstraScoreCalculator.calculate(0, 0, 5.0, 20_000.0, observer, instant, dark).score
        val brightScore = AstraScoreCalculator.calculate(0, 0, 5.0, 20_000.0, observer, instant, bright).score

        assertTrue(bright.index > dark.index)
        assertTrue(bright.bortleClass > dark.bortleClass)
        assertTrue(brightScore < darkScore)
    }

    @Test
    fun `bundled IMO JSON parser reads both annual calendars`() {
        val json = java.io.File("src/main/assets/imo_meteor_showers.json").readText()
        val snapshot = MeteorCalendarRepository.parse(json, online = false)

        assertEquals(22, snapshot.showers.size)
        assertTrue(snapshot.showers.any { it.year == 2026 && it.code == "PER" && it.zhr == 100 })
        assertTrue(snapshot.showers.any { it.year == 2027 && it.code == "PER" && it.zhr == 110 })
    }
}
