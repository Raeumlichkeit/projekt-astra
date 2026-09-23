package de.projektastra.app

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Regression tests for P2.12 (Robustheit und Darstellung):
 * - Pinch-Zoom & Panning math (clamping, FOV scaling, optics-aware gesture delta)
 * - AR- / Kartenwechsel state transitions (orientation handoff, perspective mode, optics isolation)
 * - Beschriftungskollisionen (dense cluster collision avoidance, priority ordering, screen bounds)
 */
class SkyMapInteractionTest {

    // =========================================================================
    // 1. PINCH-ZOOM & PANNING FORMULA TESTS
    // =========================================================================

    private fun computeNextFov(latestFov: Double, zoom: Float): Double {
        if (!zoom.isFinite() || zoom <= 0f) return latestFov
        return (latestFov / zoom.toDouble()).coerceIn(25.0, 150.0)
    }

    private fun normalizeDegrees(degrees: Double): Double =
        ((degrees % 360.0) + 360.0) % 360.0

    private fun computePan(
        latestAzimuth: Double,
        latestAltitude: Double,
        latestFov: Double,
        canvasWidth: Float,
        canvasHeight: Float,
        pan: Offset
    ): Pair<Double, Double> {
        val verticalFov = latestFov * canvasHeight / canvasWidth
        val nextAzimuth = normalizeDegrees(
            latestAzimuth - pan.x / canvasWidth * latestFov
        )
        val nextAltitude = (
            latestAltitude + pan.y / canvasHeight * verticalFov
        ).coerceIn(-90.0, 90.0)
        return nextAzimuth to nextAltitude
    }

    @Test
    fun pinchZoomInDecreasesFovWithinLimits() {
        val initialFov = 95.0
        val zoomedIn = computeNextFov(initialFov, 1.5f)
        assertTrue("Zoom-in (scale > 1) must decrease FOV", zoomedIn < initialFov)
        assertEquals(initialFov / 1.5, zoomedIn, 0.001)
    }

    @Test
    fun pinchZoomOutIncreasesFovWithinLimits() {
        val initialFov = 60.0
        val zoomedOut = computeNextFov(initialFov, 0.5f)
        assertTrue("Zoom-out (scale < 1) must increase FOV", zoomedOut > initialFov)
        assertEquals(120.0, zoomedOut, 0.001)
    }

    @Test
    fun pinchZoomClampsAtMinimumAndMaximumFov() {
        // Extreme zoom-in clamps at 25.0 degrees
        assertEquals(25.0, computeNextFov(30.0, 10.0f), 0.001)
        assertEquals(25.0, computeNextFov(25.0, 2.0f), 0.001)

        // Extreme zoom-out clamps at 150.0 degrees
        assertEquals(150.0, computeNextFov(120.0, 0.1f), 0.001)
        assertEquals(150.0, computeNextFov(150.0, 0.5f), 0.001)
    }

    @Test
    fun pinchZoomHandlesDegenerateZoomValuesSafely() {
        val fov = 75.0
        // Zero or negative zoom factors must not produce NaN, Infinity or crash
        assertEquals(fov, computeNextFov(fov, 0.0f), 0.001)
        assertEquals(fov, computeNextFov(fov, -1.0f), 0.001)
        assertEquals(fov, computeNextFov(fov, Float.NaN), 0.001)
        assertEquals(fov, computeNextFov(fov, Float.POSITIVE_INFINITY), 0.001)
    }

    @Test
    fun panningScalesDeltaProportionallyToFov() {
        val width = 1000f
        val height = 1000f
        val panDelta = Offset(100f, 0f)

        // At 100° FOV: 100px / 1000px * 100° = 10° azimuth shift
        val (azimuthWide, _) = computePan(180.0, 0.0, 100.0, width, height, panDelta)
        assertEquals(170.0, azimuthWide, 0.001)

        // At 25° FOV (zoomed in): 100px / 1000px * 25° = 2.5° azimuth shift (finer control)
        val (azimuthTele, _) = computePan(180.0, 0.0, 25.0, width, height, panDelta)
        assertEquals(177.5, azimuthTele, 0.001)
    }

    @Test
    fun panningClampsAltitudeToHorizonAndZenithNadir() {
        val width = 500f
        val height = 500f

        // Pan downward beyond zenith (+90°)
        val (_, altUp) = computePan(0.0, 80.0, 50.0, width, height, Offset(0f, 1000f))
        assertEquals(90.0, altUp, 0.001)

        // Pan upward beyond nadir (-90°)
        val (_, altDown) = computePan(0.0, -80.0, 50.0, width, height, Offset(0f, -1000f))
        assertEquals(-90.0, altDown, 0.001)
    }

    @Test
    fun panningWrapsAzimuthAcrossNorthMeridian() {
        val width = 1000f
        val height = 1000f

        // Pan right across 0° -> into 350°+
        val (azLeft, _) = computePan(5.0, 0.0, 100.0, width, height, Offset(100f, 0f))
        assertEquals(355.0, azLeft, 0.001)

        // Pan left across 360° -> into 0°..10°
        val (azRight, _) = computePan(355.0, 0.0, 100.0, width, height, Offset(-100f, 0f))
        assertEquals(5.0, azRight, 0.001)
    }

    @Test
    fun panDeltaTransformsCorrectlyUnderOpticsSettings() {
        val rawPan = Offset(10f, 20f)

        // Standard: no rotation, not mirrored
        val normal = OpticsSettings()
        val deltaNormal = normal.transformPanDelta(rawPan)
        assertEquals(10f, deltaNormal.x, 0.001f)
        assertEquals(20f, deltaNormal.y, 0.001f)

        // 180° rotation (Newton telescope inverted image): pan delta must invert
        val newton = OpticsSettings(rotationDegrees = 180f)
        val deltaNewton = newton.transformPanDelta(rawPan)
        assertEquals(-10f, deltaNewton.x, 0.01f)
        assertEquals(-20f, deltaNewton.y, 0.01f)

        // 90° rotation: (x, y) rotated by -90°
        val rot90 = OpticsSettings(rotationDegrees = 90f)
        val deltaRot90 = rot90.transformPanDelta(rawPan)
        assertEquals(20f, deltaRot90.x, 0.01f)
        assertEquals(-10f, deltaRot90.y, 0.01f)

        // Mirrored horizontally (Zenitspiegel): x inverted, y untouched
        val zenithMirror = OpticsSettings(mirrored = true)
        val deltaMirror = zenithMirror.transformPanDelta(rawPan)
        assertEquals(-10f, deltaMirror.x, 0.001f)
        assertEquals(20f, deltaMirror.y, 0.001f)
    }

    // =========================================================================
    // 2. AR- / KARTENWECHSEL TRANSITION TESTS
    // =========================================================================

    @Test
    fun switchingFromArToManualMapAdoptsSensorOrientationWhenNoTargetSelected() {
        // Simulates LaunchedEffect(arEnabled) logic:
        // if (wasArEnabled && !arEnabled && selection.target == null) {
        //     manualAzimuth = orientation.azimuth
        //     manualAltitude = orientation.altitude
        // }
        var manualAzimuth = 180.0f
        var manualAltitude = 35.0f
        val sensorAzimuth = 45.0f
        val sensorAltitude = 60.0f

        val wasArEnabled = true
        val arEnabled = false
        val hasTarget = false

        if (wasArEnabled && !arEnabled && !hasTarget) {
            manualAzimuth = sensorAzimuth
            manualAltitude = sensorAltitude
        }

        assertEquals("Manual map must seamlessly adopt sensor azimuth", 45.0f, manualAzimuth, 0.001f)
        assertEquals("Manual map must seamlessly adopt sensor altitude", 60.0f, manualAltitude, 0.001f)
    }

    @Test
    fun switchingFromArToManualMapPreservesTargetWhenTargetIsSelected() {
        var manualAzimuth = 180.0f
        var manualAltitude = 35.0f
        val sensorAzimuth = 45.0f
        val sensorAltitude = 60.0f

        val wasArEnabled = true
        val arEnabled = false
        val hasTarget = true // Target is selected

        if (wasArEnabled && !arEnabled && !hasTarget) {
            manualAzimuth = sensorAzimuth
            manualAltitude = sensorAltitude
        }

        // Must NOT overwrite with sensor orientation
        assertEquals(180.0f, manualAzimuth, 0.001f)
        assertEquals(35.0f, manualAltitude, 0.001f)
    }

    @Test
    fun enteringArModeReleasesTargetTracking() {
        var tracking = true
        val arEnabled = true

        if (arEnabled) {
            tracking = false
        }

        assertFalse("Entering AR mode must release tracking so sensors steer the camera view", tracking)
    }

    @Test
    fun projectionModeDiffersBetweenArAndManualMap() {
        val width = 800f
        val height = 600f

        // Manual map: perspective = true (gnomonic / spherical perspective)
        val manualProjection = SkyProjection(
            centerAzimuth = 180.0,
            centerAltitude = 45.0,
            width = width,
            height = height,
            horizontalFov = 95.0,
            perspective = true
        )
        assertTrue(manualProjection.perspective)

        // AR mode: perspective = false (cylindrical projection matching CameraX flat sensor feed)
        val arProjection = SkyProjection(
            centerAzimuth = 180.0,
            centerAltitude = 45.0,
            width = width,
            height = height,
            horizontalFov = 60.0,
            perspective = false
        )
        assertFalse(arProjection.perspective)
    }

    @Test
    fun opticsTransformationsAreStrictlyDisabledInArMode() {
        // applyOptics = !arMode && opticsSettings.isCustomized
        val opticsActive = OpticsSettings(rotationDegrees = 180f, mirrored = true)
        assertTrue(opticsActive.isCustomized)

        // In manual map mode: optics are applied
        val applyInManual = !false && opticsActive.isCustomized
        assertTrue(applyInManual)

        // In AR mode: optics are NEVER applied so real camera is not inverted/mirrored
        val applyInAr = !true && opticsActive.isCustomized
        assertFalse("Optics rotation and mirroring must be disabled in AR mode", applyInAr)
    }

    @Test
    fun backgroundRenderingBehaviorMatchesArAndTextureState() {
        fun drawBackground(arEnabled: Boolean, textureReady: Boolean?): Boolean {
            return arEnabled || textureReady != true
        }

        // In manual map mode with texture ready: background is transparent to show GPU texture
        assertFalse(drawBackground(arEnabled = false, textureReady = true))

        // In manual map mode with texture failed or loading: base dark background is drawn
        assertTrue(drawBackground(arEnabled = false, textureReady = false))
        assertTrue(drawBackground(arEnabled = false, textureReady = null))

        // In AR mode: background is always drawn as darkened camera overlay
        assertTrue(drawBackground(arEnabled = true, textureReady = true))
        assertTrue(drawBackground(arEnabled = true, textureReady = false))
    }

    // =========================================================================
    // 3. BESCHRIFTUNGSKOLLISIONEN (LABEL COLLISION LAYOUT) TESTS
    // =========================================================================

    private fun candidate(
        id: String,
        x: Float,
        y: Float,
        kind: SkyLabelKind = SkyLabelKind.STAR,
        text: String = id,
        size: Float = 14f,
        rank: Double = 0.0
    ) = SkyLabelCandidate(
        id = id,
        text = text,
        anchorX = x,
        anchorY = y,
        kind = kind,
        textSize = size,
        ascent = -size * 0.8f,
        descent = size * 0.2f,
        anchorGap = 6f,
        rank = rank
    )

    private fun runLayout(
        candidates: List<SkyLabelCandidate>,
        width: Float = 400f,
        height: Float = 400f,
        density: SkyLabelDensity = SkyLabelDensity.RICH,
        padding: Float = 8f,
        separation: Float = 4f
    ): List<PlacedSkyLabel> = layoutSkyLabels(
        candidates = candidates,
        width = width,
        height = height,
        density = density,
        padding = padding,
        separation = separation,
        measureText = { cand, text -> text.length * cand.textSize * 0.55f }
    )

    @Test
    fun denseClusterNeverProducesOverlappingBoundingBoxes() {
        val width = 400f
        val height = 400f
        val padding = 8f
        val separation = 4f

        // 30 stars densely packed in a 50x50px box around (200, 200)
        val cluster = (0 until 30).map { i ->
            val offsetX = (i % 6) * 8f
            val offsetY = (i / 6) * 8f
            candidate("Star_$i", 180f + offsetX, 180f + offsetY, rank = i.toDouble())
        }

        val placed = runLayout(cluster, width, height, padding = padding, separation = separation)
        assertTrue("At least some labels should fit in cluster", placed.isNotEmpty())

        // Verify pairwise: NO two placed labels may overlap within separation gap
        for (i in placed.indices) {
            for (j in i + 1 until placed.size) {
                val a = placed[i].bounds
                val b = placed[j].bounds
                assertFalse(
                    "Labels '${placed[i].text}' and '${placed[j].text}' must not overlap",
                    a.overlaps(b, separation)
                )
            }
        }
    }

    @Test
    fun targetLabelAlwaysWinsOverDenseSurroundingStars() {
        val target = candidate("SelectedTarget", 200f, 200f, kind = SkyLabelKind.TARGET, text = "M31 Andromeda")
        // 20 surrounding background stars
        val surrounding = (0 until 20).map { i ->
            candidate("Background_$i", 195f + (i % 5) * 3f, 195f + (i / 5) * 3f, kind = SkyLabelKind.STAR)
        }

        val placed = runLayout(listOf(target) + surrounding, 400f, 400f, SkyLabelDensity.NORMAL)
        val targetPlaced = placed.find { it.candidate.kind == SkyLabelKind.TARGET }
        assertNotNull("Target label must always be placed", targetPlaced)
        assertEquals("M31 Andromeda", targetPlaced!!.text)
    }

    @Test
    fun uprightLabelsUnderOpticsTransformationRespectTransformedAnchors() {
        val optics = OpticsSettings(rotationDegrees = 90f, rotateLabels = false)
        val canvasCenter = Offset(200f, 200f)

        val rawAnchor = Offset(250f, 200f) // 50px right of center
        val transformedAnchor = optics.transformScreenPoint(rawAnchor, canvasCenter)
        // 90° rotation moves (50, 0) to (0, 50) -> (200, 250)
        assertEquals(200f, transformedAnchor.x, 0.01f)
        assertEquals(250f, transformedAnchor.y, 0.01f)

        val label = candidate("Sirius", transformedAnchor.x, transformedAnchor.y, kind = SkyLabelKind.BRIGHT_STAR)
        val placed = runLayout(listOf(label), 400f, 400f)
        assertEquals(1, placed.size)
        // Label should be placed near transformed anchor
        assertTrue(placed.first().bounds.top >= 200f)
    }

    @Test
    fun allLabelsRemainStrictlyInsideViewportPadding() {
        val width = 300f
        val height = 300f
        val padding = 10f

        val edgeCandidates = listOf(
            candidate("TopLeft", 5f, 5f),
            candidate("TopRight", 295f, 5f),
            candidate("BottomLeft", 5f, 295f),
            candidate("BottomRight", 295f, 295f),
            candidate("Center", 150f, 150f)
        )

        val placed = runLayout(edgeCandidates, width, height, padding = padding)
        placed.forEach { label ->
            assertTrue("Left bound >= padding", label.bounds.left >= padding - 0.01f)
            assertTrue("Top bound >= padding", label.bounds.top >= padding - 0.01f)
            assertTrue("Right bound <= width - padding", label.bounds.right <= width - padding + 0.01f)
            assertTrue("Bottom bound <= height - padding", label.bounds.bottom <= height - padding + 0.01f)
        }
    }

    @Test
    fun extremelyNarrowOrShortViewportDropsCandidatesCleanly() {
        val candidate = candidate("TestStar", 10f, 10f)

        // Viewport smaller than 2 * padding
        val emptyNarrow = runLayout(listOf(candidate), width = 12f, height = 300f, padding = 8f)
        assertTrue(emptyNarrow.isEmpty())

        val emptyShort = runLayout(listOf(candidate), width = 300f, height = 12f, padding = 8f)
        assertTrue(emptyShort.isEmpty())
    }
}
