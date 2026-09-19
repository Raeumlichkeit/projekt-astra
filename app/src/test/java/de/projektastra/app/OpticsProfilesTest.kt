package de.projektastra.app

import androidx.compose.ui.geometry.Offset
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class OpticsProfilesTest {

    @Test
    fun opticsProfile_magnificationAndTrueFovFormulas() {
        val profile = OpticsProfile(
            name = "8\" Dobson mit 25 mm Okular",
            isBinocular = false,
            telescopeFocalLengthMm = 1200.0,
            telescopeApertureMm = 200.0,
            eyepieceFocalLengthMm = 25.0,
            eyepieceAfovDegrees = 52.0
        )

        // V = 1200 / 25 = 48x
        assertEquals(48.0, profile.magnification!!, 0.001)

        // TFOV = 52 / 48 = 1.0833°
        assertEquals(52.0 / 48.0, profile.effectiveFovDegrees, 0.001)

        // AP = 200 / 48 = 4.166 mm
        assertEquals(200.0 / 48.0, profile.exitPupilMm!!, 0.001)
    }

    @Test
    fun opticsProfile_binocularUsesCustomFovDirectly() {
        val binocular = OpticsProfile(
            name = "10x50 Fernglas",
            isBinocular = true,
            customFovDegrees = 6.5
        )

        assertNull("Binoculars do not compute telescope magnification", binocular.magnification)
        assertEquals(6.5, binocular.effectiveFovDegrees, 0.001)
    }

    @Test
    fun opticsProfile_serializationRoundTripPreservesFields() {
        val profile = OpticsProfile(
            id = "test-profile-1",
            name = "Apo 80/480 mit Nagler 16mm",
            isBinocular = false,
            telescopeFocalLengthMm = 480.0,
            telescopeApertureMm = 80.0,
            eyepieceFocalLengthMm = 16.0,
            eyepieceAfovDegrees = 82.0,
            customFovDegrees = 2.73
        )

        val json = profile.toJsonObject()
        val restored = OpticsProfile.fromJsonObject(json)

        assertEquals(profile.id, restored.id)
        assertEquals(profile.name, restored.name)
        assertEquals(profile.isBinocular, restored.isBinocular)
        assertEquals(profile.telescopeFocalLengthMm, restored.telescopeFocalLengthMm)
        assertEquals(profile.telescopeApertureMm, restored.telescopeApertureMm)
        assertEquals(profile.eyepieceFocalLengthMm, restored.eyepieceFocalLengthMm)
        assertEquals(profile.eyepieceAfovDegrees, restored.eyepieceAfovDegrees)
        assertEquals(profile.customFovDegrees, restored.customFovDegrees, 0.001)
    }

    @Test
    fun opticsSettings_serializationRoundTrip() {
        val settings = OpticsSettings(
            fovCircleEnabled = true,
            currentFovDegrees = 1.25,
            activeProfileId = "prof-42",
            mirrored = true,
            rotationDegrees = 180f,
            rotateLabels = true,
            telradMode = true
        )

        val json = settings.toJsonObject()
        val restored = OpticsSettings.fromJsonObject(json)

        assertEquals(settings.fovCircleEnabled, restored.fovCircleEnabled)
        assertEquals(settings.currentFovDegrees, restored.currentFovDegrees, 0.001)
        assertEquals(settings.activeProfileId, restored.activeProfileId)
        assertEquals(settings.mirrored, restored.mirrored)
        assertEquals(settings.rotationDegrees, restored.rotationDegrees, 0.001f)
        assertEquals(settings.rotateLabels, restored.rotateLabels)
        assertEquals(settings.telradMode, restored.telradMode)
        assertTrue(restored.isCustomized)
    }

    @Test
    fun transformAndInverseTransformScreenPoint_exactRoundTripAcrossAllOrientations() {
        val center = Offset(500f, 400f)
        val testPoints = listOf(
            Offset(500f, 400f), // Center
            Offset(600f, 400f), // Right
            Offset(500f, 300f), // Top
            Offset(420f, 480f), // Diagonal
            Offset(100f, 750f)
        )

        val orientationConfigs = listOf(
            OpticsSettings(mirrored = false, rotationDegrees = 0f),
            OpticsSettings(mirrored = true, rotationDegrees = 0f), // Zenitspiegel
            OpticsSettings(mirrored = false, rotationDegrees = 180f), // Newton-Invertierung
            OpticsSettings(mirrored = false, rotationDegrees = 90f),
            OpticsSettings(mirrored = false, rotationDegrees = 270f),
            OpticsSettings(mirrored = true, rotationDegrees = 90f),
            OpticsSettings(mirrored = true, rotationDegrees = 180f)
        )

        for (settings in orientationConfigs) {
            for (original in testPoints) {
                val transformed = settings.transformScreenPoint(original, center)
                val restored = settings.inverseTransformScreenPoint(transformed, center)

                assertEquals(
                    "X coordinate should match after round-trip under mirrored=${settings.mirrored}, rot=${settings.rotationDegrees}",
                    original.x, restored.x, 0.01f
                )
                assertEquals(
                    "Y coordinate should match after round-trip under mirrored=${settings.mirrored}, rot=${settings.rotationDegrees}",
                    original.y, restored.y, 0.01f
                )
            }
        }
    }

    @Test
    fun transformScreenPoint_specificGeometricExpectedValues() {
        val center = Offset(100f, 100f)
        val point = Offset(150f, 100f) // 50px right of center

        // 1. Mirrored horizontally -> 50px left of center
        val mirroredSettings = OpticsSettings(mirrored = true, rotationDegrees = 0f)
        val mirroredPoint = mirroredSettings.transformScreenPoint(point, center)
        assertEquals(50f, mirroredPoint.x, 0.01f)
        assertEquals(100f, mirroredPoint.y, 0.01f)

        // 2. Rotated 180 degrees -> 50px left of center
        val rotated180Settings = OpticsSettings(mirrored = false, rotationDegrees = 180f)
        val rotatedPoint = rotated180Settings.transformScreenPoint(point, center)
        assertEquals(50f, rotatedPoint.x, 0.01f)
        assertEquals(100f, rotatedPoint.y, 0.01f)

        // 3. Rotated 90 degrees clockwise -> 50px below center
        val rotated90Settings = OpticsSettings(mirrored = false, rotationDegrees = 90f)
        val rot90Point = rotated90Settings.transformScreenPoint(point, center)
        assertEquals(100f, rot90Point.x, 0.01f)
        assertEquals(150f, rot90Point.y, 0.01f)
    }

    @Test
    fun transformPanDelta_mirroredAndRotatedAdjustments() {
        val pan = Offset(10f, 0f) // Moving right

        // Mirrored -> should invert X
        val mirroredSettings = OpticsSettings(mirrored = true, rotationDegrees = 0f)
        val panMirrored = mirroredSettings.transformPanDelta(pan)
        assertEquals(-10f, panMirrored.x, 0.01f)
        assertEquals(0f, panMirrored.y, 0.01f)

        // Rotated 180 -> should invert both X and Y
        val rot180Settings = OpticsSettings(mirrored = false, rotationDegrees = 180f)
        val pan180 = rot180Settings.transformPanDelta(pan)
        assertEquals(-10f, pan180.x, 0.01f)
        assertEquals(0f, pan180.y, 0.01f)
    }
}
