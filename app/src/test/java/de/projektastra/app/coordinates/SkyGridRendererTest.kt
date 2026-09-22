package de.projektastra.app.coordinates

import de.projektastra.app.GeoPoint
import de.projektastra.app.SkyCoordinateFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SkyGridRendererTest {

    private val observer = GeoPoint(52.52, 13.405, 34.0) // Berlin
    private val instant = Instant.parse("2026-09-20T21:00:00Z")
    private val frame = SkyCoordinateFrame(observer, instant)

    @Test
    fun testEquatorialGridLineCountsAndRanges() {
        val grid = SkyGridRenderer.generateEquatorialGrid(stepHours = 2.0, stepDegrees = 20.0)
        val raLines = grid.filter { it.type == GridLineType.RA_HOUR }
        val decLines = grid.filter { it.type == GridLineType.DEC_PARALLEL }

        assertEquals("24h / 2h step should yield 12 RA lines", 12, raLines.size)
        // Dec from -80 to +80 with step 20: -80, -60, -40, -20, 0, 20, 40, 60, 80 = 9
        assertEquals("Dec from -80° to +80° with step 20° should yield 9 parallels", 9, decLines.size)

        // Verify primary colures (0h, 6h, 12h, 18h)
        val colures = raLines.filter { it.isPrimary }
        assertEquals("Colures at 0h, 6h, 12h, 18h should be primary", 4, colures.size)

        // Verify points span
        raLines.forEach { line ->
            assertTrue(line.equatorialPoints.isNotEmpty())
            val minDec = line.equatorialPoints.minOf { it.decDegrees }
            val maxDec = line.equatorialPoints.maxOf { it.decDegrees }
            assertEquals(-80.0, minDec, 1e-4)
            assertEquals(80.0, maxDec, 1e-4)
        }
    }

    @Test
    fun testHorizontalGridLineCountsAndRanges() {
        val grid = SkyGridRenderer.generateHorizontalGrid(stepAzDegrees = 30.0, stepAltDegrees = 20.0)
        val azLines = grid.filter { it.type == GridLineType.AZIMUTH }
        val altLines = grid.filter { it.type == GridLineType.ALTITUDE }

        assertEquals("360° / 30° should yield 12 Azimuth lines", 12, azLines.size)
        assertEquals("Altitude from 0° to 80° step 20° should yield 5 circles", 5, altLines.size)

        val cardinals = azLines.filter { it.isPrimary }
        assertEquals("N, E, S, W should be primary", 4, cardinals.size)
    }

    @Test
    fun testCelestialEquatorCharacteristics() {
        val refs = SkyGridRenderer.generateReferenceLines()
        val eq = refs.celestialEquator
        assertEquals("Dec must be 0.0", 0.0, eq.coordinateValue, 1e-6)
        assertTrue(eq.equatorialPoints.isNotEmpty())
        eq.equatorialPoints.forEach { pt ->
            assertEquals("Every point on celestial equator must have dec = 0°", 0.0, pt.decDegrees, 1e-6)
            assertTrue("RA must be within [0, 24]", pt.raHours in 0.0..24.0001)
        }
    }

    @Test
    fun testEclipticSolsticesAndEquinoxes() {
        val refs = SkyGridRenderer.generateReferenceLines(sampleStepDegrees = 0.5)
        val ecl = refs.ecliptic

        val vernal = ecl.equatorialPoints.minByOrNull { pt -> Math.abs(pt.raHours - 0.0) }!!
        assertEquals("Vernal Equinox Dec must be ~0°", 0.0, vernal.decDegrees, 0.1)

        val summer = ecl.equatorialPoints.maxByOrNull { it.decDegrees }!!
        assertEquals("Summer Solstice RA must be ~6h", 6.0, summer.raHours, 0.1)
        assertEquals("Summer Solstice Dec must be +23.439°", SkyGridRenderer.ECLIPTIC_OBLIQUITY_DEGREES, summer.decDegrees, 0.05)

        val winter = ecl.equatorialPoints.minByOrNull { it.decDegrees }!!
        assertEquals("Winter Solstice RA must be ~18h", 18.0, winter.raHours, 0.1)
        assertEquals("Winter Solstice Dec must be -23.439°", -SkyGridRenderer.ECLIPTIC_OBLIQUITY_DEGREES, winter.decDegrees, 0.05)
    }

    @Test
    fun testGalacticEquatorExtremaAndCenter() {
        val refs = SkyGridRenderer.generateReferenceLines(sampleStepDegrees = 0.5)
        val gal = refs.galacticEquator

        val maxDecPt = gal.equatorialPoints.maxByOrNull { it.decDegrees }!!
        val expectedMaxDec = 90.0 - SkyGridRenderer.GALACTIC_POLE_DEC_DEGREES // ~62.87°
        assertEquals("Max declination of Galactic Equator", expectedMaxDec, maxDecPt.decDegrees, 0.1)

        val galCenterPt = gal.equatorialPoints.minByOrNull { pt ->
            val dRa = Math.abs(pt.raHours - (266.405 / 15.0))
            val dDec = Math.abs(pt.decDegrees - (-28.936))
            dRa * 15.0 + dDec
        }!!
        assertEquals("Galactic center Dec ~ -28.9°", -28.936, galCenterPt.decDegrees, 0.2)
        assertEquals("Galactic center RA ~ 17.76h", 266.405 / 15.0, galCenterPt.raHours, 0.1)
    }

    @Test
    fun testAdaptiveLodBoundaries() {
        val lodWide = SkyGridRenderer.getEquatorialLod(80.0)
        assertEquals(2.0, lodWide.stepHoursOrAzDegrees, 1e-4)
        assertEquals(20.0, lodWide.stepDecOrAltDegrees, 1e-4)

        val lodStandard = SkyGridRenderer.getEquatorialLod(50.0)
        assertEquals(1.0, lodStandard.stepHoursOrAzDegrees, 1e-4)
        assertEquals(10.0, lodStandard.stepDecOrAltDegrees, 1e-4)

        val lodBinocular = SkyGridRenderer.getEquatorialLod(20.0)
        assertEquals(0.5, lodBinocular.stepHoursOrAzDegrees, 1e-4)
        assertEquals(5.0, lodBinocular.stepDecOrAltDegrees, 1e-4)

        val lodTelescope = SkyGridRenderer.getEquatorialLod(10.0)
        assertEquals(1.0 / 6.0, lodTelescope.stepHoursOrAzDegrees, 1e-4)
        assertEquals(2.0, lodTelescope.stepDecOrAltDegrees, 1e-4)

        val lodEyepiece = SkyGridRenderer.getEquatorialLod(2.0)
        assertEquals(1.0 / 30.0, lodEyepiece.stepHoursOrAzDegrees, 1e-4)
        assertEquals(0.5, lodEyepiece.stepDecOrAltDegrees, 1e-4)
    }

    @Test
    fun testToHorizontalConversionIntegrity() {
        val grid = SkyGridRenderer.generateEquatorialGrid()
        val converted = SkyGridRenderer.toHorizontal(grid, frame)

        assertEquals(grid.size, converted.size)
        converted.forEach { line ->
            assertEquals(line.equatorialPoints.size, line.horizontalPoints.size)
            line.horizontalPoints.forEach { hp ->
                assertTrue("Azimuth must be in [0, 360]", hp.azimuth in 0.0..360.0001)
                assertTrue("Altitude must be in [-90, 90]", hp.altitude in -90.0001..90.0001)
            }
        }
    }

    @Test
    fun testDifferentiatedStylesInRedLightMode() {
        val eqStyle = SkyGridRenderer.getLineStyle(GridLineType.CELESTIAL_EQUATOR, true, redLightMode = true)
        val eclStyle = SkyGridRenderer.getLineStyle(GridLineType.ECLIPTIC, true, redLightMode = true)
        val galStyle = SkyGridRenderer.getLineStyle(GridLineType.GALACTIC_EQUATOR, true, redLightMode = true)

        assertNotNull(eqStyle.pathEffect)
        assertNotNull(eclStyle.pathEffect)
        assertNotNull(galStyle.pathEffect)
        // Dash patterns must be distinct
        assertTrue("Dash patterns must differ", eqStyle.pathEffect != eclStyle.pathEffect)
        assertTrue("Dash patterns must differ", eqStyle.pathEffect != galStyle.pathEffect)
        assertTrue("Dash patterns must differ", eclStyle.pathEffect != galStyle.pathEffect)
    }
}
