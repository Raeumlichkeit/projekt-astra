package de.projektastra.app.ephemeris

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.math.abs

class LunarTerminatorTest {

    @Test
    fun `terminator colongitude and subsolar coordinates across key lunar phases`() {
        // First Quarter: ~2026-09-18T22:00:00Z
        val firstQuarter = LunarTerminatorCalculator.calculateTerminator(Instant.parse("2026-09-18T22:00:00Z"))
        assertTrue("Colongitude near 0° or 360°: ${firstQuarter.colongitude}",
            firstQuarter.colongitude in 355.0..360.0 || firstQuarter.colongitude in 0.0..10.0)
        assertTrue("Subsolar lon near 90°: ${firstQuarter.subSolarLon}", firstQuarter.subSolarLon in 80.0..95.0)
        assertTrue("Phase fraction near 0.5: ${firstQuarter.phaseFraction}", firstQuarter.phaseFraction in 0.45..0.55)

        // Full Moon: ~2026-09-26T16:00:00Z
        val fullMoon = LunarTerminatorCalculator.calculateTerminator(Instant.parse("2026-09-26T16:00:00Z"))
        assertTrue("Colongitude near 90°: ${fullMoon.colongitude}", fullMoon.colongitude in 80.0..100.0)
        assertTrue("Subsolar lon near 0°: ${fullMoon.subSolarLon}", abs(fullMoon.subSolarLon) < 10.0)
        assertTrue("Phase fraction near 1.0: ${fullMoon.phaseFraction}", fullMoon.phaseFraction >= 0.98)

        // Last Quarter: ~2026-10-03T14:00:00Z
        val lastQuarter = LunarTerminatorCalculator.calculateTerminator(Instant.parse("2026-10-03T14:00:00Z"))
        assertTrue("Colongitude near 180°: ${lastQuarter.colongitude}", lastQuarter.colongitude in 170.0..190.0)
        assertTrue("Subsolar lon near -90°: ${lastQuarter.subSolarLon}", lastQuarter.subSolarLon in -95.0..-80.0)
        assertTrue("Phase fraction near 0.5: ${lastQuarter.phaseFraction}", lastQuarter.phaseFraction in 0.45..0.55)

        // New Moon: ~2026-09-11T14:00:00Z
        val newMoon = LunarTerminatorCalculator.calculateTerminator(Instant.parse("2026-09-11T14:00:00Z"))
        assertTrue("Colongitude near 270°: ${newMoon.colongitude}", newMoon.colongitude in 260.0..280.0)
        assertTrue("Phase fraction near 0.0: ${newMoon.phaseFraction}", newMoon.phaseFraction <= 0.03)
    }

    @Test
    fun `libration remains within physical lunar boundaries`() {
        val base = Instant.parse("2026-01-01T00:00:00Z")
        for (month in 0..11) {
            val t = base.plusSeconds(month * 30L * 86400L)
            val state = LunarTerminatorCalculator.calculateTerminator(t)
            assertTrue("Sub-Earth lon out of range: ${state.subEarthLon}", state.subEarthLon in -9.0..9.0)
            assertTrue("Sub-Earth lat out of range: ${state.subEarthLat}", state.subEarthLat in -7.5..7.5)
            assertTrue("Distance km out of range: ${state.moonDistanceKm}", state.moonDistanceKm in 350000.0..410000.0)
        }
    }

    @Test
    fun `solar elevation and terminator equation validation`() {
        val state = LunarTerminatorCalculator.calculateTerminator(Instant.parse("2026-09-18T22:00:00Z"))

        // Zenith test: sub-solar point must have sun elevation = 90°
        val zenithElev = state.sunElevationDegrees(state.subSolarLon, state.subSolarLat)
        assertEquals(90.0, zenithElev, 0.01)

        // Anti-solar test: opposite point must have sun elevation = -90°
        val antiSolarLon = LunarTerminatorState.normalizeLongitude(state.subSolarLon + 180.0)
        val antiSolarElev = state.sunElevationDegrees(antiSolarLon, -state.subSolarLat)
        assertEquals(-90.0, antiSolarElev, 0.01)

        // Points on morning terminator must have h_sun == 0.0°
        for (lat in listOf(-60.0, -30.0, 0.0, 30.0, 60.0)) {
            val mTermLon = state.morningTerminatorLon(lat)
            val elev = state.sunElevationDegrees(mTermLon, lat)
            assertEquals("Elevation on morning terminator at lat $lat must be 0°", 0.0, elev, 0.1)

            val eTermLon = state.eveningTerminatorLon(lat)
            val elevE = state.sunElevationDegrees(eTermLon, lat)
            assertEquals("Elevation on evening terminator at lat $lat must be 0°", 0.0, elevE, 0.1)
        }
    }

    @Test
    fun `lunar feature catalog integrity`() {
        val catalog = LunarFeatureCatalog.allFeatures
        assertTrue("Catalog must contain at least 65 features (actual: ${catalog.size})", catalog.size >= 65)

        val ids = mutableSetOf<String>()
        val types = mutableSetOf<LunarFeatureType>()

        catalog.forEach { feature ->
            assertTrue("Non-blank ID required", feature.id.isNotBlank())
            assertTrue("Unique ID: ${feature.id}", ids.add(feature.id))
            assertTrue("Valid lat [-90, 90]: ${feature.selenographicLat}", feature.selenographicLat in -90.0..90.0)
            assertTrue("Valid lon [-180, 180]: ${feature.selenographicLon}", feature.selenographicLon in -180.0..180.0)
            assertTrue("Positive diameter: ${feature.diameterKm}", feature.diameterKm > 0.0)
            assertTrue("Non-blank name", feature.name.isNotBlank())
            types.add(feature.type)
        }

        assertEquals("All 6 lunar feature types must be present", 6, types.size)
    }

    @Test
    fun `optimal relief and highlighting logic near terminator`() {
        // First Quarter: Central features (Rupes Recta, Ptolemaeus) are near sunrise terminator
        val state = LunarTerminatorCalculator.calculateTerminator(Instant.parse("2026-09-19T06:00:00Z"))
        val highlights = LunarTerminatorCalculator.featuresNearTerminator(state)

        val rupesRecta = highlights.firstOrNull { it.feature.id == "rupes_recta" }
        val mareCrisium = highlights.firstOrNull { it.feature.id == "mare_crisium" }

        assertTrue("Rupes Recta should be evaluated", rupesRecta != null)
        assertTrue("Rupes Recta must be in optimal relief or near terminator: sunElev=${rupesRecta?.sunElevationDegrees}",
            rupesRecta!!.sunElevationDegrees in -5.0..15.0)

        assertTrue("Mare Crisium should be evaluated", mareCrisium != null)
        // In first quarter, Mare Crisium (lon +59°) has high sun (> 40°) so inOptimalRelief must be false
        assertTrue("Mare Crisium sun elevation should be high: ${mareCrisium!!.sunElevationDegrees}",
            mareCrisium.sunElevationDegrees > 30.0)
        assertFalse("Mare Crisium must NOT be in optimal low-sun relief", mareCrisium.inOptimalRelief)
    }

    @Test
    fun `disc orthographic projection bounds`() {
        val state = LunarTerminatorCalculator.calculateTerminator(Instant.parse("2026-09-20T00:00:00Z"))
        LunarFeatureCatalog.allFeatures.forEach { feature ->
            val vis = state.earthVisibility(feature.selenographicLon, feature.selenographicLat)
            if (vis.isVisibleFromEarth) {
                val radiusSq = vis.diskX * vis.diskX + vis.diskY * vis.diskY
                assertTrue("Projected feature ${feature.name} must lie on unit disc: $radiusSq", radiusSq <= 1.0001)
            }
        }
    }

    @Test
    fun `visible terminator path generates valid polyline`() {
        val state = LunarTerminatorCalculator.calculateTerminator(Instant.parse("2026-09-19T00:00:00Z"))
        val path = LunarTerminatorCalculator.generateVisibleTerminatorPath(state, stepDegrees = 5.0)

        assertTrue("Polyline points should be generated", path.isNotEmpty())
        path.forEach { (x, y) ->
            val rSq = x * x + y * y
            assertTrue("Terminator point ($x, $y) must be on disc", rSq <= 1.001f)
        }
    }
}
