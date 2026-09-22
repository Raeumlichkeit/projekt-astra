package de.projektastra.app.hardware

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class CameraAndHardwareTest {

    @Test
    fun testCameraExposureController_stepping() {
        val controller = CameraExposureController(maxEvSteps = 4, evStepSize = 0.5f)
        assertEquals(0, controller.currentStep)
        assertFalse(controller.getState().isCompensationActive)
        assertEquals(0.0f, controller.getState().evValue, 0.001f)

        // Step up
        val s1 = controller.stepUp()
        assertEquals(1, s1.currentStepIndex)
        assertEquals(0.5f, s1.evValue, 0.001f)
        assertTrue(s1.isCompensationActive)

        controller.stepUp()
        controller.stepUp()
        val s4 = controller.stepUp()
        assertEquals(4, s4.currentStepIndex)
        assertEquals(2.0f, s4.evValue, 0.001f)

        // Cannot step past max
        val sOver = controller.stepUp()
        assertEquals(4, sOver.currentStepIndex)

        // Step down
        val s3 = controller.stepDown()
        assertEquals(3, s3.currentStepIndex)
        assertEquals(1.5f, s3.evValue, 0.001f)

        // Reset
        val reset = controller.reset()
        assertEquals(0, reset.currentStepIndex)
        assertEquals(0.0f, reset.evValue, 0.001f)
        assertFalse(reset.isCompensationActive)
    }

    @Test
    fun testArSensorFilter_fovAdaptiveAlpha() {
        val filter = ArSensorFilter(baseAlpha = 0.15f)
        val alphaWide = filter.computeAlphaForFov(90.0f)
        val alphaNormal = filter.computeAlphaForFov(45.0f)
        val alphaTele = filter.computeAlphaForFov(5.0f)

        assertTrue("Wide FOV should have higher alpha (more responsive)", alphaWide > alphaNormal)
        assertTrue("Tele/zoomed FOV should have lower alpha (heavier damping)", alphaTele < alphaNormal)
        assertTrue(alphaTele in 0.02f..0.05f)
        assertTrue(alphaNormal in 0.14f..0.16f)
    }

    @Test
    fun testArSensorFilter_azimuthWrapAround() {
        val filter = ArSensorFilter(baseAlpha = 0.5f)
        // Initial point at 358°
        val (az1, _) = filter.filter(358.0f, 20.0f, 45.0f)
        assertEquals(358.0f, az1, 0.01f)

        // Next point steps across 0° to 2° (difference is +4°, not -356°)
        val (az2, _) = filter.filter(2.0f, 20.0f, 45.0f)
        // With alpha = ~0.15 at 45°:
        // delta is +4.0°, so newAz should smoothly advance past 358° towards 360°/0°
        assertTrue("Azimuth must smooth over wrap-around", az2 > 358.0f || az2 < 2.0f)
    }

    @Test
    fun testArSensorFilter_pitchTrimClamping() {
        val filter = ArSensorFilter(pitchTrimDegrees = 10.0f)
        val (_, pitch1) = filter.filter(180.0f, 30.0f, 45.0f)
        assertEquals(40.0f, pitch1, 0.01f)

        // Clamp trim at ±15°
        filter.reset()
        filter.pitchTrimDegrees = 25.0f // over 15°
        val (_, pitchClamped) = filter.filter(180.0f, 30.0f, 45.0f)
        assertEquals(45.0f, pitchClamped, 0.01f) // clamped to +15° -> 30 + 15 = 45
    }

    @Test
    fun testGloveModeZoomController_zoomInAndOut() {
        val controller = GloveModeZoomController(
            currentFovDegrees = 60.0,
            minFovDegrees = 1.0,
            maxFovDegrees = 100.0,
            zoomFactorPerStep = 1.25
        )

        // Volume Up zooms in (decreases FOV)
        val fovIn = controller.onVolumeUp()
        assertEquals(48.0, fovIn, 0.01)

        // Volume Down zooms out (increases FOV)
        val fovOut = controller.onVolumeDown()
        assertEquals(60.0, fovOut, 0.01)

        // Disabled glove mode ignores keys
        controller.gloveModeEnabled = false
        val unchanged = controller.onVolumeUp()
        assertEquals(60.0, unchanged, 0.01)
    }

    @Test
    fun testOledThemeManager_pureBlackColors() {
        val oled = OledThemeManager.getThemeColors(oledModeEnabled = true)
        assertTrue(oled.isPureBlack)
        assertEquals("#000000", oled.backgroundColorHex)
        assertEquals("#000000", oled.surfaceColorHex)
        assertEquals("#000000", oled.cardBackgroundHex)

        val standard = OledThemeManager.getThemeColors(oledModeEnabled = false)
        assertFalse(standard.isPureBlack)
        assertNotEquals("#000000", standard.surfaceColorHex)
    }
}
