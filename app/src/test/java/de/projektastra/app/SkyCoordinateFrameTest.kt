package de.projektastra.app

import androidx.compose.ui.geometry.Offset
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.Vector
import io.github.cosinekitty.astronomy.horizon
import io.github.cosinekitty.astronomy.rotationEqjEqd
import io.github.cosinekitty.astronomy.rotationGalEqj
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

class SkyCoordinateFrameTest {
    private val instant = Instant.parse("2026-09-05T20:00:00Z")
    private val observer = GeoPoint(52.52, 13.405, 34.0)
    private val time = Time.fromMillisecondsSince1970(instant.toEpochMilli())

    private fun equatorial(raHours: Double, decDegrees: Double): DoubleArray {
        val ra = Math.toRadians(raHours * 15.0)
        val dec = Math.toRadians(decDegrees)
        return doubleArrayOf(cos(dec) * cos(ra), cos(dec) * sin(ra), sin(dec))
    }

    private fun recovered(frame: SkyCoordinateFrame, position: HorizontalCoordinates): DoubleArray {
        val azimuth = Math.toRadians(position.azimuth)
        val altitude = Math.toRadians(position.altitude)
        val local = doubleArrayOf(cos(altitude) * sin(azimuth), cos(altitude) * cos(azimuth), sin(altitude))
        val matrix = frame.horizontalToJ2000
        return DoubleArray(3) { row -> (0..2).sumOf { column -> matrix[column * 3 + row] * local[column] } }
    }

    // NASA equatorial all-sky orientation: RA decreases from left to right, north is at the top.
    private fun textureCoordinates(vector: DoubleArray): Pair<Double, Double> {
        val ra = atan2(vector[1], vector[0])
        val dec = asin((vector[2] / sqrt(vector.sumOf { it * it })).coerceIn(-1.0, 1.0))
        val u = 0.5 - ra / (2.0 * Math.PI)
        return u - floor(u) to 0.5 - dec / Math.PI
    }

    @Test fun catalogueProjectionMatchesPrecessionAndGeometricHorizon() {
        val frame = SkyCoordinateFrame(observer, instant)
        val j2000ToDate = rotationEqjEqd(time)
        val place = Observer(observer.latitude, observer.longitude, observer.altitudeMeters)
        listOf(0.0 to 0.0, 6.7525 to -16.7161, 17.7611 to -29.0078, 23.99 to 89.99).forEach { (ra, dec) ->
            val xyz = equatorial(ra, dec)
            val ofDate = j2000ToDate.rotate(Vector(xyz[0], xyz[1], xyz[2], time)).toEquatorial()
            val expected = horizon(time, place, ofDate.ra, ofDate.dec, Refraction.None)
            val actual = frame.horizontal(ra, dec)
            assertEquals(expected.azimuth, actual.azimuth, 1e-8)
            assertEquals(expected.altitude, actual.altitude, 1e-8)
        }
    }

    @Test fun gpuColumnMajorMatrixInvertsCatalogueProjectionAcrossDatesAndLatitudes() {
        listOf("2000-01-01T12:00:00Z", "2026-09-05T20:00:00Z", "2050-06-21T00:00:00Z").forEach { date ->
            listOf(-90.0, -33.86, 0.0, 52.52, 90.0).forEach { latitude ->
                val frame = SkyCoordinateFrame(GeoPoint(latitude, 151.21, 30.0), Instant.parse(date))
                listOf(0.0, 6.0, 12.0, 18.0, 23.999).forEach { ra ->
                    listOf(-90.0, -29.0078, 0.0, 27.12825, 90.0).forEach { dec ->
                        val expected = equatorial(ra, dec)
                        val actual = recovered(frame, frame.horizontal(ra, dec))
                        (0..2).forEach { axis -> assertEquals("$date / $latitude / RA $ra / Dec $dec / axis $axis", expected[axis], actual[axis], 1e-7) }
                    }
                }
            }
        }
    }

    @Test fun matrixColumnsAreOrthonormalAndRightHanded() {
        val matrix = SkyCoordinateFrame(observer, instant).horizontalToJ2000
        for (a in 0..2) for (b in 0..2) {
            val dot = (0..2).sumOf { row -> matrix[a * 3 + row].toDouble() * matrix[b * 3 + row] }
            assertEquals(if (a == b) 1.0 else 0.0, dot, 1e-7)
        }
        val eastCrossNorth = doubleArrayOf(
            matrix[1].toDouble() * matrix[5] - matrix[2].toDouble() * matrix[4],
            matrix[2].toDouble() * matrix[3] - matrix[0].toDouble() * matrix[5],
            matrix[0].toDouble() * matrix[4] - matrix[1].toDouble() * matrix[3]
        )
        (0..2).forEach { axis -> assertEquals(matrix[6 + axis].toDouble(), eastCrossNorth[axis], 1e-7) }
    }

    @Test fun textureReferenceMeridiansAndPolesHaveTheDocumentedOrientation() {
        val frame = SkyCoordinateFrame(observer, instant)
        listOf(0.0 to 0.5, 6.0 to 0.25, 12.0 to 0.0, 18.0 to 0.75).forEach { (ra, expectedU) ->
            val (u, v) = textureCoordinates(recovered(frame, frame.horizontal(ra, 0.0)))
            val wrappedDistance = kotlin.math.abs(u - expectedU).let { minOf(it, 1.0 - it) }
            assertTrue("RA $ra has reversed texture orientation", wrappedDistance < 1e-7)
            assertEquals(0.5, v, 1e-7)
        }
        assertEquals(0.0, textureCoordinates(recovered(frame, frame.horizontal(0.0, 90.0))).second, 1e-7)
        assertEquals(1.0, textureCoordinates(recovered(frame, frame.horizontal(0.0, -90.0))).second, 1e-7)
    }

    @Test fun galacticCenterAndNorthPoleRegisterWithTheMilkyWayReferenceFrame() {
        val frame = SkyCoordinateFrame(observer, instant)
        val rotation = rotationGalEqj()
        val galacticCenter = rotation.rotate(Vector(1.0, 0.0, 0.0, time)).toEquatorial()
        val northGalacticPole = rotation.rotate(Vector(0.0, 0.0, 1.0, time)).toEquatorial()
        // Astronomy Engine 2.1.19 transforms the IAU 1958 frame, not the later rounded
        // Hipparcos pole constants. Reference values from its published GAL->EQJ matrix.
        assertEquals(266.40593246, galacticCenter.ra * 15.0, 1e-5)
        assertEquals(-28.93388502, galacticCenter.dec, 1e-5)
        assertEquals(192.85893059, northGalacticPole.ra * 15.0, 1e-5)
        assertEquals(27.12840040, northGalacticPole.dec, 1e-5)
        val (u, v) = textureCoordinates(recovered(frame, frame.horizontal(galacticCenter.ra, galacticCenter.dec)))
        assertEquals(0.759986, u, 1e-5)
        assertEquals(0.660744, v, 1e-5)
    }

    @Test fun textureCoordinatesRemainFiniteAtEveryViewportEdgeAndAcrossZenith() {
        val frame = SkyCoordinateFrame(observer, instant)
        listOf(-90.0, 0.0, 80.0, 90.0).forEach { altitude ->
            listOf(25.0, 95.0, 150.0).forEach { fov ->
                val projection = SkyProjection(359.0, altitude, 400f, 800f, fov, perspective = true)
                for (x in 0..4) for (y in 0..8) {
                    val horizontal = projection.coordinates(Offset(x * 100f, y * 100f))!!
                    val (u, v) = textureCoordinates(recovered(frame, horizontal))
                    assertTrue(u.isFinite() && u >= 0.0 && u < 1.0)
                    assertTrue(v.isFinite() && v in 0.0..1.0)
                }
            }
        }
    }
}
