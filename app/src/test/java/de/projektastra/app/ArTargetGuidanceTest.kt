package de.projektastra.app

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArTargetGuidanceTest {

    @Test
    fun angularDistance_exactPoints() {
        // Identical point -> 0.0°
        assertEquals(0.0, ArTargetGuidanceCalculator.angularDistance(180.0, 45.0, 180.0, 45.0), 1e-4)

        // Same azimuth, 30° difference in altitude
        assertEquals(30.0, ArTargetGuidanceCalculator.angularDistance(180.0, 10.0, 180.0, 40.0), 1e-4)

        // Equator/horizon, 90° difference in azimuth
        assertEquals(90.0, ArTargetGuidanceCalculator.angularDistance(0.0, 0.0, 90.0, 0.0), 1e-4)

        // Opposite points on the celestial sphere
        assertEquals(180.0, ArTargetGuidanceCalculator.angularDistance(0.0, 0.0, 180.0, 0.0), 1e-4)
        assertEquals(180.0, ArTargetGuidanceCalculator.angularDistance(90.0, 90.0, 90.0, -90.0), 1e-4)
    }

    @Test
    fun deltaDegrees_wrapsProperly() {
        assertEquals(0.0, ArTargetGuidanceCalculator.deltaDegrees(0.0), 1e-4)
        assertEquals(45.0, ArTargetGuidanceCalculator.deltaDegrees(45.0), 1e-4)
        assertEquals(-45.0, ArTargetGuidanceCalculator.deltaDegrees(-45.0), 1e-4)
        assertEquals(10.0, ArTargetGuidanceCalculator.deltaDegrees(370.0), 1e-4)
        assertEquals(-10.0, ArTargetGuidanceCalculator.deltaDegrees(-370.0), 1e-4)
        assertEquals(180.0, ArTargetGuidanceCalculator.deltaDegrees(180.0), 1e-4)
        assertEquals(180.0, ArTargetGuidanceCalculator.deltaDegrees(-180.0), 1e-4)
    }

    @Test
    fun relativeCameraVector_directions() {
        // Pointing at azimuth 180°, altitude 0°
        // Target straight ahead: az 180°, alt 0° -> depth > 0, right = 0, up = 0
        val vStraight = ArTargetGuidanceCalculator.relativeCameraVector(180.0, 0.0, 180.0, 0.0)
        assertEquals(1.0, vStraight.depth, 1e-4)
        assertEquals(0.0, vStraight.right, 1e-4)
        assertEquals(0.0, vStraight.up, 1e-4)

        // Target to the right: az 270°, alt 0° -> depth = 0, right = 1, up = 0
        val vRight = ArTargetGuidanceCalculator.relativeCameraVector(180.0, 0.0, 270.0, 0.0)
        assertEquals(0.0, vRight.depth, 1e-4)
        assertEquals(1.0, vRight.right, 1e-4)
        assertEquals(0.0, vRight.up, 1e-4)

        // Target to the left: az 90°, alt 0° -> depth = 0, right = -1, up = 0
        val vLeft = ArTargetGuidanceCalculator.relativeCameraVector(180.0, 0.0, 90.0, 0.0)
        assertEquals(0.0, vLeft.depth, 1e-4)
        assertEquals(-1.0, vLeft.right, 1e-4)
        assertEquals(0.0, vLeft.up, 1e-4)

        // Target directly above: az 180°, alt 90° -> depth = 0, right = 0, up = 1
        val vUp = ArTargetGuidanceCalculator.relativeCameraVector(180.0, 0.0, 180.0, 90.0)
        assertEquals(0.0, vUp.depth, 1e-4)
        assertEquals(0.0, vUp.right, 1e-4)
        assertEquals(1.0, vUp.up, 1e-4)

        // Target directly behind: az 0°, alt 0° -> depth = -1, right = 0, up = 0
        val vBehind = ArTargetGuidanceCalculator.relativeCameraVector(180.0, 0.0, 0.0, 0.0)
        assertEquals(-1.0, vBehind.depth, 1e-4)
        assertEquals(0.0, vBehind.right, 1e-4)
        assertEquals(0.0, vBehind.up, 1e-4)
    }

    @Test
    fun arrowAngle_cardinalDirections() {
        // Up (right = 0, up = 1) -> 0°
        assertEquals(0f, ArTargetGuidanceCalculator.arrowAngle(0.0, 1.0), 0.1f)

        // Right (right = 1, up = 0) -> 90°
        assertEquals(90f, ArTargetGuidanceCalculator.arrowAngle(1.0, 0.0), 0.1f)

        // Down (right = 0, up = -1) -> 180°
        assertEquals(180f, ArTargetGuidanceCalculator.arrowAngle(0.0, -1.0), 0.1f)

        // Left (right = -1, up = 0) -> 270°
        assertEquals(270f, ArTargetGuidanceCalculator.arrowAngle(-1.0, 0.0), 0.1f)
    }

    @Test
    fun edgePosition_clamping() {
        val width = 1000f
        val height = 2000f
        val padding = 50f

        // Pointing straight up -> top edge, horizontal center
        val posUp = ArTargetGuidanceCalculator.edgePosition(0.0, 1.0, width, height, padding)
        assertEquals(500f, posUp.x, 0.1f)
        assertEquals(padding, posUp.y, 0.1f)

        // Pointing straight down -> bottom edge, horizontal center
        val posDown = ArTargetGuidanceCalculator.edgePosition(0.0, -1.0, width, height, padding)
        assertEquals(500f, posDown.x, 0.1f)
        assertEquals(height - padding, posDown.y, 0.1f)

        // Pointing straight right -> right edge, vertical center
        val posRight = ArTargetGuidanceCalculator.edgePosition(1.0, 0.0, width, height, padding)
        assertEquals(width - padding, posRight.x, 0.1f)
        assertEquals(1000f, posRight.y, 0.1f)

        // Pointing straight left -> left edge, vertical center
        val posLeft = ArTargetGuidanceCalculator.edgePosition(-1.0, 0.0, width, height, padding)
        assertEquals(padding, posLeft.x, 0.1f)
        assertEquals(1000f, posLeft.y, 0.1f)
    }

    @Test
    fun calculateGuidance_inViewTarget() {
        val guidance = ArTargetGuidanceCalculator.calculateGuidance(
            targetName = "Jupiter",
            targetTypeLabel = "Planet",
            targetPosition = HorizontalCoordinates(180.0, 30.0),
            currentAzimuth = 180.0,
            currentAltitude = 30.0,
            horizontalFov = 60.0,
            width = 1080f,
            height = 1920f,
            terrainProfile = null,
            sensorAccuracy = android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
            sensorAvailable = true
        )

        assertEquals("Jupiter", guidance.targetName)
        assertEquals(0.0, guidance.angularDistanceDegrees, 1e-4)
        assertFalse(guidance.isBehind)
        assertFalse(guidance.isBelowHorizon)
        assertFalse(guidance.isBelowTerrain)
        val screenPos = guidance.screenPosition
        assertNotNull(screenPos)
        assertEquals(540f, screenPos?.x ?: 0f, 1f)
        assertEquals(960f, screenPos?.y ?: 0f, 1f)
        assertEquals(ArSensorQuality.HIGH, guidance.sensorQuality)
        assertTrue(guidance.sensorQuality.isAccurate)
    }

    @Test
    fun calculateGuidance_behindTarget() {
        val guidance = ArTargetGuidanceCalculator.calculateGuidance(
            targetName = "Polarstern",
            targetTypeLabel = "Stern",
            targetPosition = HorizontalCoordinates(0.0, 52.0),
            currentAzimuth = 180.0,
            currentAltitude = 30.0,
            horizontalFov = 60.0,
            width = 1080f,
            height = 1920f,
            terrainProfile = null,
            sensorAccuracy = android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_LOW,
            sensorAvailable = true
        )

        assertTrue(guidance.isBehind)
        assertFalse(guidance.isInView)
        assertNull(guidance.screenPosition)
        assertTrue(guidance.angularDistanceDegrees > 90.0)
        assertEquals(ArSensorQuality.LOW, guidance.sensorQuality)
        assertFalse(guidance.sensorQuality.isAccurate)
        assertTrue(guidance.statusText.contains("Umdrehen"))
    }

    @Test
    fun calculateGuidance_belowHorizonAndTerrain() {
        // Below mathematical horizon
        val guidanceBelowHorizon = ArTargetGuidanceCalculator.calculateGuidance(
            targetName = "Sonne",
            targetTypeLabel = "Sonne",
            targetPosition = HorizontalCoordinates(180.0, -15.0),
            currentAzimuth = 180.0,
            currentAltitude = 10.0,
            horizontalFov = 60.0,
            width = 1080f,
            height = 1920f,
            terrainProfile = null,
            sensorAccuracy = android.hardware.SensorManager.SENSOR_STATUS_UNRELIABLE,
            sensorAvailable = true
        )
        assertTrue(guidanceBelowHorizon.isBelowHorizon)
        assertTrue(guidanceBelowHorizon.statusText.contains("Unter Horizont"))
        assertEquals(ArSensorQuality.UNRELIABLE, guidanceBelowHorizon.sensorQuality)

        // Above 0° horizon, but behind terrain mountain of 12° altitude
        val terrain = TerrainProfile(
            samples = List(36) { index -> TerrainSample(index * 10.0, 12.0) },
            observerElevationMeters = 500.0
        )
        val guidanceBehindMountain = ArTargetGuidanceCalculator.calculateGuidance(
            targetName = "Sirius",
            targetTypeLabel = "Stern",
            targetPosition = HorizontalCoordinates(180.0, 5.0),
            currentAzimuth = 180.0,
            currentAltitude = 20.0,
            horizontalFov = 60.0,
            width = 1080f,
            height = 1920f,
            terrainProfile = terrain,
            sensorAccuracy = android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM,
            sensorAvailable = true
        )
        assertFalse(guidanceBehindMountain.isBelowHorizon)
        assertTrue(guidanceBehindMountain.isBelowTerrain)
        assertTrue(guidanceBehindMountain.statusText.contains("Hinter Gelände"))
        assertEquals(ArSensorQuality.MEDIUM, guidanceBehindMountain.sensorQuality)
    }
}
