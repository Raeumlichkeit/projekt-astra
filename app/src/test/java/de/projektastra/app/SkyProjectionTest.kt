package de.projektastra.app

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.*
import org.junit.Test

class SkyProjectionTest {
    private val projection = SkyProjection(0.0, 0.0, 400f, 200f, 40.0)
    private fun sky(azimuth: Double, altitude: Double = 0.0) = HorizontalCoordinates(azimuth, altitude)

    private fun assertPoint(x: Float, y: Float, actual: Offset) {
        assertEquals(x, actual.x, 0.001f)
        assertEquals(y, actual.y, 0.001f)
    }

    @Test fun opticsMarginKeepsGeometryUntilAfterRotation() {
        val rotatedViewport = SkyProjection(0.0, 0.0, 200f, 300f, 40.0,
            perspective = true, clipPadding = 200f)
        val outside = rotatedViewport.coordinates(Offset(250f, 150f))!!
        val point = rotatedViewport.point(outside)!!
        assertEquals(250f, point.x, 0.01f)
        assertFalse(rotatedViewport.contains(point)) // The final screen rectangle remains unchanged.
        assertTrue(rotatedViewport.segments(sky(0.0), outside).isNotEmpty())
    }

    @Test fun visiblePortionSurvivesAtEveryEdge() {
        val cases = listOf(
            sky(-30.0) to Offset(0f, 100f),
            sky(30.0) to Offset(400f, 100f),
            sky(0.0, 20.0) to Offset(200f, 0f),
            sky(0.0, -20.0) to Offset(200f, 200f)
        )
        cases.forEach { (outside, edge) ->
            val segment = projection.segments(sky(0.0), outside).single()
            assertPoint(200f, 100f, segment.start)
            assertPoint(edge.x, edge.y, segment.end)
        }
    }

    @Test fun twoInvisibleEndpointsCanCrossTheWholeView() {
        assertNull(projection.point(sky(-30.0)))
        assertNull(projection.point(sky(30.0)))
        val horizontal = projection.segments(sky(-30.0), sky(30.0)).single()
        assertPoint(0f, 100f, horizontal.start)
        assertPoint(400f, 100f, horizontal.end)
        val vertical = projection.segments(sky(0.0, 20.0), sky(0.0, -20.0)).single()
        assertPoint(200f, 0f, vertical.start)
        assertPoint(200f, 200f, vertical.end)
    }

    @Test fun outsideParallelAndDegenerateSegmentsAreHandled() {
        assertTrue(projection.segments(sky(30.0, 5.0), sky(40.0, -5.0)).isEmpty())
        assertTrue(projection.segments(sky(-10.0, 20.0), sky(10.0, 20.0)).isEmpty())
        assertTrue(projection.segments(sky(30.0), sky(30.0)).isEmpty())
        val point = projection.segments(sky(0.0), sky(0.0)).single()
        assertPoint(200f, 100f, point.start)
        assertEquals(point.start, point.end)
    }

    @Test fun northCrossingUsesTheShortArc() {
        val segment = projection.segments(sky(359.0), sky(1.0)).single()
        assertPoint(190f, 100f, segment.start)
        assertPoint(210f, 100f, segment.end)
        val reverse = projection.segments(sky(1.0), sky(359.0)).single()
        assertEquals(segment.start, reverse.end)
        assertEquals(segment.end, reverse.start)
    }

    @Test fun backsideCrossingDoesNotDrawADiagonalAcrossTheMap() {
        assertTrue(projection.segments(sky(170.0), sky(190.0)).isEmpty())
        assertTrue(projection.segments(sky(190.0), sky(170.0)).isEmpty())
        val south = SkyProjection(180.0, 0.0, 400f, 200f, 40.0)
        assertTrue(south.segments(sky(359.0), sky(1.0)).isEmpty())
    }

    @Test fun glowAndPartlyVisibleObjectsKeepTheirOffscreenMargin() {
        assertNull(projection.point(sky(20.5)))
        assertPoint(405f, 100f, projection.point(sky(20.5), padding = 8f)!!)
        assertTrue(projection.segments(sky(-30.0, 11.0), sky(30.0, 11.0)).isEmpty())
        val glow = projection.segments(sky(-30.0, 11.0), sky(30.0, 11.0), padding = 38f).single()
        assertPoint(-38f, -10f, glow.start)
        assertPoint(438f, -10f, glow.end)
    }

    @Test fun cornersAndReversedLinesAreClippedSymmetrically() {
        val from = sky(-40.0, 20.0)
        val to = sky(40.0, -20.0)
        val forward = projection.segments(from, to).single()
        assertPoint(0f, 0f, forward.start)
        assertPoint(400f, 200f, forward.end)
        val backward = projection.segments(to, from).single()
        assertEquals(forward.start, backward.end)
        assertEquals(forward.end, backward.start)
    }

    @Test fun panZoomAndAspectRatioKeepEdgesContinuous() {
        listOf(400f to 800f, 800f to 400f).forEach { (width, height) ->
            listOf(25.0, 95.0, 150.0).forEach { fov ->
                listOf(0.0, 179.0, 359.0, 720.0, -360.0).forEach { azimuth ->
                    val view = SkyProjection(azimuth, 30.0, width, height, fov)
                    val line = view.segments(sky(azimuth - fov / 2 - 1, 30.0),
                        sky(azimuth + fov / 2 + 1, 30.0)).single()
                    assertPoint(0f, height / 2, line.start)
                    assertPoint(width, height / 2, line.end)
                    assertPoint(width / 2, height / 2, view.point(sky(azimuth, 30.0))!!)
                }
            }
        }
    }

    @Test fun altitudeOrientationIsUnchanged() {
        assertPoint(200f, 50f, projection.point(sky(0.0, 5.0))!!)
        assertPoint(200f, 150f, projection.point(sky(0.0, -5.0))!!)
        val lookingUp = SkyProjection(0.0, 5.0, 400f, 200f, 40.0)
        assertPoint(200f, 150f, lookingUp.point(sky(0.0))!!)
    }

    @Test fun invalidViewsAndCoordinatesReturnNothing() {
        listOf(
            SkyProjection(0.0, 0.0, 0f, 200f, 40.0),
            SkyProjection(0.0, 0.0, 400f, Float.NaN, 40.0),
            SkyProjection(Double.NaN, 0.0, 400f, 200f, 40.0),
            SkyProjection(0.0, 0.0, 400f, 200f, 0.0)
        ).forEach {
            assertNull(it.point(sky(0.0)))
            assertTrue(it.segments(sky(-10.0), sky(10.0)).isEmpty())
        }
        assertNull(projection.point(sky(Double.NaN)))
        assertNull(projection.point(sky(0.0), -1f))
        assertTrue(projection.segments(sky(0.0), sky(0.0, Double.POSITIVE_INFINITY)).isEmpty())
        assertTrue(projection.segments(sky(0.0), sky(1.0), Float.NaN).isEmpty())
    }

    @Test fun sphericalMapContinuesBeyondZenith() {
        val view = SkyProjection(0.0, 80.0, 400f, 800f, 95.0, true)
        val beyondZenith = view.point(sky(180.0, 85.0))!!
        assertPoint(200f, beyondZenith.y, beyondZenith)
        assertTrue(beyondZenith.y < 400f)
        assertNotNull(view.point(sky(0.0, 90.0)))
        assertNull(view.point(sky(180.0, -80.0)))
    }

    @Test fun everySphericalViewportPointRoundTripsWithoutBlankRegions() {
        listOf(-90.0, -40.0, 0.0, 57.0, 90.0).forEach { altitude ->
            listOf(25.0, 95.0, 150.0).forEach { fov ->
                listOf(400f to 800f, 800f to 400f).forEach { (width, height) ->
                    val view = SkyProjection(359.0, altitude, width, height, fov, true)
                    for (x in 0..10) for (y in 0..10) {
                        val pixel = Offset(x * width / 10, y * height / 10)
                        val coordinates = view.coordinates(pixel)!!
                        assertTrue(coordinates.altitude in -90.0..90.0)
                        assertTrue(coordinates.azimuth in 0.0..<360.0)
                        val projected = view.point(coordinates, 0.01f)!!
                        assertPoint(pixel.x, pixel.y, projected)
                    }
                }
            }
        }
    }

    @Test fun sphericalLinesClipAcrossEdgesAndTheCameraPlane() {
        val view = SkyProjection(0.0, 0.0, 400f, 200f, 40.0, true)
        val crossing = view.segments(sky(-30.0), sky(30.0)).single()
        assertPoint(0f, 100f, crossing.start)
        assertPoint(400f, 100f, crossing.end)
        val partlyBehind = view.segments(sky(0.0), sky(100.0)).single()
        assertPoint(200f, 100f, partlyBehind.start)
        assertPoint(400f, 100f, partlyBehind.end)
        assertTrue(view.segments(sky(170.0), sky(190.0)).isEmpty())
    }

    @Test fun sphericalGroundPolygonsClipToTheViewport() {
        val up = SkyProjection(0.0, 90.0, 400f, 800f, 95.0, true)
        val down = SkyProjection(0.0, -90.0, 400f, 800f, 95.0, true)
        var groundArea = 0.0
        (0 until 120).forEach { step ->
            val triangle = listOf(sky(step * 3.0), sky((step + 1) * 3.0), sky(0.0, -90.0))
            assertTrue(up.polygon(triangle).isEmpty())
            val polygon = down.polygon(triangle)
            polygon.forEach {
                assertTrue(it.x in -0.01f..400.01f && it.y in -0.01f..800.01f)
            }
            if (polygon.isNotEmpty()) groundArea += kotlin.math.abs(
                (polygon + polygon.first()).zipWithNext().sumOf { (a, b) -> (a.x * b.y - b.x * a.y).toDouble() }) / 2
        }
        assertEquals(400.0 * 800.0, groundArea, 0.1)
    }

    @Test fun wideFovStereographicPreservesShapesWithoutExtremeGnomonicDistortion() {
        val view = SkyProjection(0.0, 0.0, 1000f, 1000f, 150.0, true)
        val p0 = view.point(sky(0.0, 0.0))!!
        val p10 = view.point(sky(10.0, 0.0))!!
        val distCenter = p10.x - p0.x

        val p65 = view.point(sky(65.0, 0.0))!!
        val p75 = view.point(sky(75.0, 0.0))!!
        val distEdge = p75.x - p65.x

        val distortionRatio = distEdge / distCenter
        // Gnomonic would produce ~9x magnification stretch at 75°, stereographic is only ~1.49x
        assertTrue("Stereographic distortion ratio should be gentle (< 1.7x), was $distortionRatio",
            distortionRatio in 1.3..1.7)
    }

    @Test fun stereographicConformalIsometryMaintainsEqualRadialAndTangentialScale() {
        val view = SkyProjection(0.0, 0.0, 1000f, 1000f, 150.0, true)
        val center = view.point(sky(50.0, 0.0))!!
        val dAz = view.point(sky(52.0, 0.0))!!
        val dAlt = view.point(sky(50.0, 2.0))!!

        val deltaX = kotlin.math.hypot((dAz.x - center.x).toDouble(), (dAz.y - center.y).toDouble())
        val deltaY = kotlin.math.hypot((dAlt.x - center.x).toDouble(), (dAlt.y - center.y).toDouble())
        val scaleRatio = deltaX / deltaY
        // Conformal projection preserves aspect ratios locally (deltaX == deltaY within 2%)
        assertEquals(1.0, scaleRatio, 0.02)
    }

    @Test fun sphericalHorizonSeparatesSkyAndGroundCorrectly() {
        val width = 1000f
        val height = 2000f
        val fov = 95.0
        for (alt in listOf(0.0, 35.0, -35.0, 60.0, -60.0)) {
            val v = SkyProjection(180.0, alt, width, height, fov, true)
            val pts = (0..160).mapNotNull { step ->
                val relAz = -120.0 + step * (240.0 / 160.0)
                val sampleAz = ((180.0 + relAz) % 360.0 + 360.0) % 360.0
                v.point(HorizontalCoordinates(sampleAz, 0.0), padding = 20f)
            }
            assertTrue("pts should not be empty for alt=$alt", pts.isNotEmpty())
            val horizonMinY = pts.minOf { it.y }
            val horizonMaxY = pts.maxOf { it.y }
            val hCenter = v.point(HorizontalCoordinates(180.0, 0.0))!!
            // When looking up (alt > 0), horizon curve is strictly in the lower half of the screen
            if (alt > 0) {
                assertTrue("Horizon should be in lower half for alt > 0, was minY=$horizonMinY", horizonMinY > height / 2)
            } else if (alt < 0) {
                // When looking down (alt < 0), horizon curve is in the upper half of the screen
                assertTrue("Horizon should be in upper half for alt < 0, was maxY=$horizonMaxY", horizonMaxY < height / 2)
            } else {
                assertEquals("Horizon center should be at center of screen for alt=0", height / 2, hCenter.y, 1f)
            }
        }
    }

    @Test fun eyepieceNarrowFovIsValidAndProjectsProperly() {
        val narrow = SkyProjection(180.0, 45.0, 1080f, 1920f, 0.5, perspective = true)
        val centerPoint = narrow.point(HorizontalCoordinates(180.0, 45.0))
        assertNotNull(centerPoint)
        assertEquals(540f, centerPoint!!.x, 0.5f)
        assertEquals(960f, centerPoint.y, 0.5f)

        // Point outside the 0.5° FOV should return null
        val outsidePoint = narrow.point(HorizontalCoordinates(182.0, 45.0))
        assertNull(outsidePoint)
    }

    @Test fun preparedPositionsMatchExistingProjectionAcrossPanZoomAndAr() {
        val positions = listOf(
            sky(0.0, 0.0), sky(13.4, 30.0), sky(89.0, -40.0),
            sky(179.0, 80.0), sky(270.0, -85.0), sky(359.9, 89.0),
            sky(-30.0, 10.0), sky(720.0, -20.0)
        )
        val prepared = positions.map(::PreparedSkyPosition)
        for (perspective in listOf(false, true)) {
            for (fov in listOf(0.5, 25.0, 95.0, 150.0)) {
                for (azimuth in listOf(0.0, 11.0, 179.0, 359.0)) {
                    for (altitude in listOf(-80.0, 0.0, 39.0, 89.0)) {
                        val view = SkyProjection(azimuth, altitude, 901f, 1201f, fov, perspective)
                        positions.zip(prepared).forEach { (raw, cached) ->
                            val expected = view.point(raw, padding = 2f)
                            val actual = view.point(cached, padding = 2f)
                            if (expected == null) assertNull(actual) else {
                                assertNotNull(actual)
                                assertEquals(expected.x, actual!!.x, 0.002f)
                                assertEquals(expected.y, actual.y, 0.002f)
                            }
                        }
                    }
                }
            }
        }
        assertNull(SkyProjection(0.0, 0.0, 901f, 1201f, 95.0, true)
            .point(PreparedSkyPosition(sky(Double.NaN))))
    }
}
