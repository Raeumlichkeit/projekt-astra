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
}
