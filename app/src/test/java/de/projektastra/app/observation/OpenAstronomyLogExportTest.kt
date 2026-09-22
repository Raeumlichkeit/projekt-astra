package de.projektastra.app.observation

import de.projektastra.app.ObservationLogEntry
import org.junit.Assert.*
import org.junit.Test

class OpenAstronomyLogExportTest {

    @Test
    fun testXmlEscaping() {
        val raw = "M31 & <Andromeda> \"Galaxy\" 'test'"
        val escaped = OpenAstronomyLogExport.escapeXml(raw)
        assertEquals("M31 &amp; &lt;Andromeda&gt; &quot;Galaxy&quot; &apos;test&apos;", escaped)
    }

    @Test
    fun testOalXmlStructure() {
        val entry = ObservationLogEntry(
            id = "test-obs-1",
            objectCatalogId = "M42",
            objectName = "Orionnebel",
            timestampEpochSeconds = 1700000000L,
            notes = "Trapezsterne & Nebelfilter UHC",
            equipment = "8\" Dobsonian f/6",
            locationName = "Sternwarte Berlin",
            seeingPickering = 8,
            seeingAntoniadi = "II",
            nelm = 5.8
        )

        val xml = OpenAstronomyLogExport.exportToXml(listOf(entry))

        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
        assertTrue(xml.contains("<oal xmlns=\"http://www.astronomy.org/OpenAstronomyLog/2.1\" version=\"2.1\">"))
        assertTrue(xml.contains("<observation id=\"test-obs-1\">"))
        assertTrue(xml.contains("<target id=\"M42\" name=\"Orionnebel\" />"))
        assertTrue(xml.contains("<site name=\"Sternwarte Berlin\" />"))
        assertTrue(xml.contains("<optics name=\"8&quot; Dobsonian f/6\" />"))
        assertTrue(xml.contains("<result notes=\"Trapezsterne &amp; Nebelfilter UHC\" />"))
        assertTrue(xml.contains("<seeing pickering=\"8\" />"))
        assertTrue(xml.contains("<seeing antoniadi=\"II\" />"))
        assertTrue(xml.contains("<limitingMagnitude>5.8</limitingMagnitude>"))
        assertTrue(xml.endsWith("</oal>"))
    }

    @Test
    fun testFormattedTextSummary() {
        val entry = ObservationLogEntry(
            id = "test-obs-2",
            objectCatalogId = "M13",
            objectName = "Herkuleshaufen",
            timestampEpochSeconds = 1700000000L,
            notes = "Bis ins Zentrum aufgelöst",
            equipment = "10\" Dobson",
            locationName = "Rhön",
            seeingPickering = 7,
            seeingAntoniadi = "II",
            nelm = 6.3
        )

        val summary = OpenAstronomyLogExport.exportFormattedTextSummary(listOf(entry))

        assertTrue(summary.contains("=== PROJEKT ASTRA BEOBACHTUNGS-TAGEBUCH ==="))
        assertTrue(summary.contains("Gesamtanzahl Beobachtungen: 1"))
        assertTrue(summary.contains("[1] M13 - Herkuleshaufen"))
        assertTrue(summary.contains("Standort: Rhön"))
        assertTrue(summary.contains("Optik: 10\" Dobson"))
        assertTrue(summary.contains("Pickering 7/10"))
        assertTrue(summary.contains("Antoniadi II"))
        assertTrue(summary.contains("NELM 6.3m"))
        assertTrue(summary.contains("Notiz: Bis ins Zentrum aufgelöst"))
    }

    @Test
    fun testSeeingScaleValidator() {
        assertTrue(SeeingScaleValidator.isValidPickering(1))
        assertTrue(SeeingScaleValidator.isValidPickering(10))
        assertFalse(SeeingScaleValidator.isValidPickering(0))
        assertFalse(SeeingScaleValidator.isValidPickering(11))

        assertTrue(SeeingScaleValidator.isValidAntoniadi("I"))
        assertTrue(SeeingScaleValidator.isValidAntoniadi("v"))
        assertFalse(SeeingScaleValidator.isValidAntoniadi("VI"))

        assertTrue(SeeingScaleValidator.isValidNelm(6.5))
        assertFalse(SeeingScaleValidator.isValidNelm(-1.0))
        assertFalse(SeeingScaleValidator.isValidNelm(9.5))

        assertEquals("I", SeeingScaleValidator.mapPickeringToAntoniadi(10))
        assertEquals("I", SeeingScaleValidator.mapPickeringToAntoniadi(9))
        assertEquals("II", SeeingScaleValidator.mapPickeringToAntoniadi(8))
        assertEquals("II", SeeingScaleValidator.mapPickeringToAntoniadi(7))
        assertEquals("III", SeeingScaleValidator.mapPickeringToAntoniadi(6))
        assertEquals("III", SeeingScaleValidator.mapPickeringToAntoniadi(5))
        assertEquals("IV", SeeingScaleValidator.mapPickeringToAntoniadi(4))
        assertEquals("IV", SeeingScaleValidator.mapPickeringToAntoniadi(3))
        assertEquals("V", SeeingScaleValidator.mapPickeringToAntoniadi(2))
        assertEquals("V", SeeingScaleValidator.mapPickeringToAntoniadi(1))
    }
}
