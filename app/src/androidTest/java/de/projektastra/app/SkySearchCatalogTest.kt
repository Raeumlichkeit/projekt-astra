package de.projektastra.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class SkySearchCatalogTest {
    @Test fun bundledCatalogResolvesAliasesRegionsAndMovingBodiesOffline() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val stars = StarCatalog.load(context)
        val deepSky = DeepSkyCatalog.load(context)
        val planets = SolarSystemCatalog.at(GeoPoint(52.52, 13.405, 34.0), Instant.parse("2026-09-06T20:00:00Z"))
        assertTrue("Full star catalog must load", stars.size > 5000)
        assertTrue("Full deep sky catalog must load", deepSky.size > 1000)
        val regions = constellationSearchTargets(stars)
        val index = SkySearchIndex((stars + deepSky + planets).map { SkySearchTarget(it) } + regions)
        // The Saturn Nebula is also a valid partial match; the exact planet name must lead.
        assertEquals("Saturn", index.search("saturn").first().name)
        assertEquals("HIP 91262", index.search("Wega").single().id)
        assertEquals("HIP 11767", index.search("Polarstern").single().id)
        assertEquals("M 31", index.search("NGC224").single().objectData.messierId)
        assertEquals("M 45", index.search("Plejaden").single().objectData.messierId)
        assertTrue(index.search("Andromeda").any { it.typeLabel == "Galaxie" })
        assertTrue(index.search("Andromeda").any { it.typeLabel == "Sternbild" })
        assertEquals(88, regions.count { !it.asterism })
        assertEquals("Sternmuster", index.search("Großer Wagen").single().typeLabel)
    }
}
