package de.projektastra.app

import org.junit.Assert.*
import org.junit.Test

class SkySearchTest {
    private val vega = CelestialObject("Vega", "HIP 91262", 18.6156, 38.7837, 0.03, 25.0, "A0V")
    private val galaxy = vega.copy(name = "Andromeda Galaxy", catalogId = "M 31 · NGC 224",
        objectType = CelestialType.GALAXY, messierId = "M 31")
    private val region = SkySearchTarget(vega, "Andromeda", listOf("And"), "constellation:And")
    private val index = SkySearchIndex(listOf(SkySearchTarget(vega), SkySearchTarget(galaxy), region,
        SkySearchTarget(galaxy.copy(name = "M310 test", catalogId = "M 310", messierId = "M 310"))))

    @Test fun germanAliasFindsCatalogStar() {
        assertEquals("HIP 91262", index.search("  WEGA ").single().id)
        assertEquals(index.search("Wega"), index.search("Vega"))
    }

    @Test fun catalogSpacingAndCaseAreIgnoredWithoutPrefixCollisions() {
        for (query in listOf("M31", "m 31", "NGC224", "ngc 224"))
            assertEquals(galaxy.catalogId, index.search(query).single().id)
        assertEquals(vega.catalogId, index.search("hip91262").single().id)
        assertTrue(index.search("HIP 912620").isEmpty())
    }

    @Test fun constellationAndGalaxyStayDistinct() {
        val matches = index.search("Andromeda")
        assertEquals(setOf("Sternbild", "Galaxie"), matches.map { it.typeLabel }.toSet())
        assertEquals(2, matches.map { it.id }.distinct().size)
    }

    @Test fun blankAndUnknownQueriesDoNotReturnHistoryOrArbitraryTargets() {
        listOf("", "  ", "---", "kein solches Objekt").forEach { assertTrue(index.search(it).isEmpty()) }
        assertTrue(index.search("Vega", 0).isEmpty())
    }

    @Test fun unicodeAndUmlautsNormalizeForRegionAliases() {
        val targets = SkySearchIndex(listOf(SkySearchTarget(vega, "Großer Bär", listOf("Ursa Major"), "constellation:UMa")))
        assertEquals("Großer Bär", targets.search("grosser bar").single().name)
        assertEquals("Großer Bär", targets.search("ursa major").single().name)
    }

    @Test fun namesAndBayerAliasesAreBothIndexed() {
        val target = SkySearchTarget(vega.copy(searchAliases = listOf("Alp Lyr")))
        assertEquals(target, SkySearchIndex(listOf(target)).search("alp lyr").single())
    }

    @Test fun deepSkySearchIsIndependentOfRenderedLayer() {
        val result = index.search("M31").single()
        assertTrue(result.needsDeepSky)
        assertFalse(SkySearchTarget(vega).needsDeepSky)
        assertFalse(region.needsDeepSky)
    }

    @Test fun terrainBlocksAboveZeroTargetsAndBelowHorizonStaysExplicit() {
        val hill = TerrainProfile(listOf(TerrainSample(0.0, 12.0)), 100.0)
        assertEquals(TargetVisibility.BELOW, targetVisibility(HorizontalCoordinates(0.0, -2.0), hill))
        assertEquals(TargetVisibility.BELOW, targetVisibility(HorizontalCoordinates(0.0, 0.0), null))
        assertEquals(TargetVisibility.TERRAIN, targetVisibility(HorizontalCoordinates(270.0, 7.0), hill))
        assertEquals(TargetVisibility.ABOVE, targetVisibility(HorizontalCoordinates(270.0, 13.0), hill))
        assertEquals(TargetVisibility.ABOVE, targetVisibility(HorizontalCoordinates(270.0, 7.0), null))
    }

    @Test fun selectionChangeAndGestureReleaseTrackingButKeepTarget() {
        val following = SkyTargetSelection().select(SkySearchTarget(galaxy)).follow()
        assertTrue(following.tracking)
        assertEquals(following.target, following.release().target)
        assertFalse(following.release().tracking)
        assertFalse(following.select(SkySearchTarget(vega)).tracking)
        assertFalse(SkyTargetSelection().follow().tracking)
    }
}
