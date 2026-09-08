package de.projektastra.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.atan
import kotlin.math.tan

class CameraFieldOfViewTest {
    private fun fov(dx: Double, dy: Double) = cameraHorizontalFovDegrees(
        dx, dy, 6.4, 4.8, 4000, 3000, 4.0
    )!!

    @Test fun portraitAndLandscapeUseTheActualSensorAxis() {
        assertEquals(Math.toDegrees(2 * atan(4.8 / 8.0)), fov(0.0, 3000.0), 0.00001)
        assertEquals(Math.toDegrees(2 * atan(6.4 / 8.0)), fov(4000.0, 0.0), 0.00001)
        assertTrue(fov(4000.0, 0.0) > fov(0.0, 3000.0))
    }

    @Test fun tallerFillCenterViewportNarrowsTheDisplayedHorizontalField() {
        val uncropped = fov(0.0, 3000.0)
        val cropped = fov(0.0, 1800.0)
        assertEquals(0.6, tan(Math.toRadians(cropped / 2)) / tan(Math.toRadians(uncropped / 2)), 0.00001)
        assertTrue(cropped < uncropped)
    }

    @Test fun rotationOrMirroringDoesNotChangeTheVisibleField() {
        assertEquals(fov(0.0, 2400.0), fov(0.0, -2400.0), 0.00001)
        assertEquals(fov(2400.0, 0.0), fov(-2400.0, 0.0), 0.00001)
    }

    @Test fun invalidOrNotYetAvailableMetadataIsRejected() {
        assertNull(cameraHorizontalFovDegrees(0.0, 0.0, 6.4, 4.8, 4000, 3000, 4.0))
        assertNull(cameraHorizontalFovDegrees(Double.NaN, 1.0, 6.4, 4.8, 4000, 3000, 4.0))
        assertNull(cameraHorizontalFovDegrees(3000.0, 0.0, 6.4, 4.8, 0, 3000, 4.0))
        assertNull(cameraHorizontalFovDegrees(3000.0, 0.0, 6.4, 4.8, 4000, 3000, 0.0))
        assertNull(cameraHorizontalFovDegrees(3000.0, 0.0, Double.POSITIVE_INFINITY, 4.8, 4000, 3000, 4.0))
    }
}
