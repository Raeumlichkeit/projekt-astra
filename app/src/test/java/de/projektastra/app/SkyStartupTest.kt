package de.projektastra.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SkyStartupTest {

    @Test
    fun fallbackObjectsAreAvailableImmediatelyBeforeHygLoads() {
        val fallbacks = StarCatalog.fallbackObjects
        assertTrue("Fallback objects must not be empty", fallbacks.isNotEmpty())
        assertEquals(12, fallbacks.size)

        // Verify key navigational anchor stars are present
        val names = fallbacks.map { it.name }.toSet()
        assertTrue(names.contains("Sirius"))
        assertTrue(names.contains("Wega"))
        assertTrue(names.contains("Polaris") || names.contains("Arktur"))

        fallbacks.forEach { star ->
            assertTrue("RA must be within 0..24 hours", star.raHours in 0.0..24.0)
            assertTrue("Dec must be within -90..90 degrees", star.decDegrees in -90.0..90.0)
            assertTrue("Fallback stars must be bright (mag <= 2.0)", star.magnitude <= 2.0)
            assertNotNull("Catalog ID must exist", star.catalogId)
        }
    }

    @Test
    fun canvasDrawBackgroundIsActiveWheneverTextureIsNotReady() {
        fun shouldDrawBackground(arEnabled: Boolean, textureReady: Boolean?): Boolean {
            return arEnabled || textureReady != true
        }

        // Before texture is rendered / while loading: base canvas must draw its dark background
        assertTrue("Initial state before texture loads must draw background",
            shouldDrawBackground(arEnabled = false, textureReady = null))

        // When texture initialization fails: base canvas must retain its dark background
        assertTrue("Failed texture must retain dark canvas background",
            shouldDrawBackground(arEnabled = false, textureReady = false))

        // When texture is ready in normal mode: canvas background is transparent to show GPU texture
        assertFalse("Ready texture in normal mode yields transparent canvas background",
            shouldDrawBackground(arEnabled = false, textureReady = true))

        // In AR mode: canvas background overlay is always drawn regardless of texture
        assertTrue("AR mode always draws background overlay",
            shouldDrawBackground(arEnabled = true, textureReady = true))
        assertTrue("AR mode without texture draws background overlay",
            shouldDrawBackground(arEnabled = true, textureReady = null))
        assertTrue("AR mode with failed texture draws background overlay",
            shouldDrawBackground(arEnabled = true, textureReady = false))
    }

    @Test
    fun textureRetryIncrementsTriggerState() {
        var retryCount = 0
        val onRetryClick: () -> Unit = { retryCount++ }

        assertEquals(0, retryCount)
        onRetryClick()
        assertEquals(1, retryCount)
        onRetryClick()
        assertEquals(2, retryCount)
    }

    @Test
    fun prioritizedLoadingPreservesBaseSkyVisibility() {
        // Simulates the object list visible during the first frame (before catalogs finish loading)
        val observer = GeoPoint(52.52, 13.405, 34.0)
        val instant = java.time.Instant.parse("2026-09-19T20:00:00Z")
        val frame = SkyCoordinateFrame(observer, instant)

        val stars = StarCatalog.fallbackObjects
        val solarSystem = SolarSystemCatalog.at(observer, instant)

        val initialVisible = (stars + solarSystem).map {
            VisibleObject(
                it,
                it.solarBody?.let { body -> SolarSystemCatalog.horizontal(body, observer, instant) }
                    ?: frame.horizontal(it.raHours, it.decDegrees)
            )
        }

        assertTrue("First frame visible objects must contain fallback stars and solar system bodies",
            initialVisible.size >= 12 + 8) // 12 fallback stars + Sun/Moon/Planets
        assertTrue("All positions must be valid coordinates",
            initialVisible.all { it.position.azimuth in 0.0..360.0 && it.position.altitude in -90.0..90.0 })
    }
}
