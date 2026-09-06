package de.projektastra.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SkyAppearanceTest {
    @Test fun defaultsAreNaturalWithNoCameraOverlayOrGrid() {
        val appearance = SkyAppearance.restore()
        assertEquals(MilkyWayMode.NATURAL, appearance.mode)
        assertEquals(1f, appearance.intensity, 0f)
        assertFalse(appearance.showInAr)
        assertFalse(appearance.showGrid)
    }

    @Test fun everyStoredModeRestoresWithoutChangingItsMeaning() {
        MilkyWayMode.entries.forEach { mode ->
            assertEquals(mode, SkyAppearance.restore(mode.name).mode)
        }
    }

    @Test fun absentOrUnknownModesFallBackToNatural() {
        listOf(null, "", "UNKNOWN", "photo", " NATURAL ").forEach { stored ->
            assertEquals(MilkyWayMode.NATURAL, SkyAppearance.restore(stored).mode)
        }
    }

    @Test fun nonFiniteIntensityCannotReachSliderOrRenderer() {
        listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY).forEach { value ->
            assertEquals(1f, SkyAppearance(intensity = value).normalized().intensity, 0f)
            assertEquals(1f, SkyAppearance.restore(intensity = value).intensity, 0f)
        }
    }

    @Test fun intensityIsClampedToTheSupportedRange() {
        assertEquals(0.25f, SkyAppearance.restore(intensity = -100f).intensity, 0f)
        assertEquals(0.25f, SkyAppearance.restore(intensity = 0f).intensity, 0f)
        assertEquals(1.75f, SkyAppearance.restore(intensity = 100f).intensity, 0f)
    }

    @Test fun validIntensityIsPreservedExactly() {
        listOf(0.25f, 0.7f, 1f, 1.4f, 1.75f).forEach { value ->
            assertEquals(value, SkyAppearance.restore(intensity = value).intensity, 0f)
        }
    }

    @Test fun normalizationDoesNotResetIndependentPreferences() {
        val appearance = SkyAppearance(MilkyWayMode.PHOTO, -1f, showInAr = true, showGrid = true).normalized()
        assertEquals(MilkyWayMode.PHOTO, appearance.mode)
        assertEquals(0.25f, appearance.intensity, 0f)
        assertTrue(appearance.showInAr)
        assertTrue(appearance.showGrid)
    }

    @Test fun switchingOffPreservesTheChosenIntensityForLater() {
        val appearance = SkyAppearance.restore("OFF", intensity = 1.25f, showInAr = true, showGrid = true)
        assertEquals(MilkyWayMode.OFF, appearance.mode)
        assertEquals(1.25f, appearance.intensity, 0f)
        assertTrue(appearance.showInAr)
        assertTrue(appearance.showGrid)
        assertEquals(appearance, appearance.normalized().normalized())
    }
}
