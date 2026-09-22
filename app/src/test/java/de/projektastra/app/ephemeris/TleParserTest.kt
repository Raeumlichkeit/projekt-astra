package de.projektastra.app.ephemeris

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class TleParserTest {

    private val line1 = "1 25544U 98067A   26263.51829341  .00016717  00000-0  10270-3 0  9997"
    private val line2 = "2 25544  51.6416 247.4627 0006703 130.5360 325.0288 15.72125391563537"

    @Test
    fun `parses valid ISS TLE successfully`() {
        val tle = TleParser.parseTle(line1, line2, "ISS (ZARYA)")

        assertEquals("ISS (ZARYA)", tle.satelliteName)
        assertEquals(25544, tle.noradCatalogNumber)
        assertEquals('U', tle.classification)
        assertEquals("98067A", tle.internationalDesignator)
        assertEquals(2026, tle.epochYear)
        assertEquals(263.51829341, tle.epochDayOfYear, 1e-6)
        assertEquals(0.00010270, tle.bstarDrag, 1e-9)
        assertEquals(51.6416, tle.inclinationDegrees, 1e-4)
        assertEquals(247.4627, tle.raanDegrees, 1e-4)
        assertEquals(0.0006703, tle.eccentricity, 1e-7)
        assertEquals(130.5360, tle.argumentOfPerigeeDegrees, 1e-4)
        assertEquals(325.0288, tle.meanAnomalyDegrees, 1e-4)
        assertEquals(15.72125391, tle.meanMotionRevsPerDay, 1e-6)
        assertEquals(56353, tle.revolutionNumberAtEpoch)
        assertFalse(tle.isDeepSpace)
    }

    @Test
    fun `validates checksum accurately`() {
        assertTrue(TleParser.validateChecksum(line1))
        assertTrue(TleParser.validateChecksum(line2))

        // Corrupted digit (expected checksum is 7, replace with 8)
        val corruptedLine = line1.substring(0, 68) + "8"
        assertFalse(TleParser.validateChecksum(corruptedLine))
    }

    @Test
    fun `parses assumed decimal exponent properly`() {
        assertEquals(0.000038706, TleParser.parseDecimalWithExponent(" 38706-4"), 1e-11)
        assertEquals(-0.0000012345, TleParser.parseDecimalWithExponent("-12345-5"), 1e-12)
        assertEquals(0.0, TleParser.parseDecimalWithExponent(" 00000-0"), 1e-12)
        assertEquals(0.00010270, TleParser.parseDecimalWithExponent(" 10270-3"), 1e-10)
    }

    @Test
    fun `converts epoch day of year to UTC instant`() {
        val tle = TleParser.parseTle(line1, line2)
        val epochZoned = tle.epochInstant.atZone(ZoneOffset.UTC)

        assertEquals(2026, epochZoned.year)
        assertEquals(9, epochZoned.monthValue) // Day 263 of 2026 is in September (263 - 31 - 28 - 31 - 30 - 31 - 30 - 31 - 31 = Sept 20)
        assertEquals(20, epochZoned.dayOfMonth)
        assertEquals(12, epochZoned.hour) // 0.51829341 * 24 = 12.438...
    }

    @Test
    fun `parses multiple TLEs from multi-line text`() {
        val text = listOf(
            "ISS (ZARYA)",
            line1,
            line2,
            "TIANGONG (CSS)",
            "1 48274U 21035A   26263.48912037  .00021480  00000-0  14562-3 0  9998",
            "2 48274  41.4728 185.1245 0005128  84.2154 275.9241 15.60214820295123"
        )
        val list = TleParser.parseMultiple(text)

        assertEquals(2, list.size)
        assertEquals(25544, list[0].noradCatalogNumber)
        assertEquals("ISS (ZARYA)", list[0].satelliteName)
        assertEquals(48274, list[1].noradCatalogNumber)
        assertEquals("TIANGONG (CSS)", list[1].satelliteName)
    }
}
