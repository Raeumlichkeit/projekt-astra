package de.projektastra.app.coordinates

import de.projektastra.app.GeoPoint
import de.projektastra.app.SkyCoordinateFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class CelestialMeasurementTest {

    @Test
    fun testKnownDoubleStar_MizarAndAlcor() {
        val mizar = EquatorialCoordinates(13.39875, 54.92528)
        val alcor = EquatorialCoordinates(13.42042, 54.98806)

        val result = CelestialMeasurement.measure(mizar, alcor)

        // Expected distance is ~11.8 arcminutes (0.1968°)
        assertEquals(0.1968, result.angularDistanceDegrees, 0.005)
        assertEquals(0, result.degrees)
        assertEquals(11, result.arcMinutes)
        assertEquals(71.4, result.positionAngleDegrees, 1.5)
        assertTrue(result.formattedDistance.contains("0° 11'"))
    }

    @Test
    fun testConstellationPair_BetelgeuseAndRigel() {
        val betelgeuse = EquatorialCoordinates(5.9195, 7.4071)
        val rigel = EquatorialCoordinates(5.2423, -8.2016)

        val result = CelestialMeasurement.measure(betelgeuse, rigel)

        // Expected distance: ~18.604° (18° 36' 15")
        assertEquals(18.604, result.angularDistanceDegrees, 0.05)
        assertEquals(18, result.degrees)
        assertEquals(36, result.arcMinutes)
        // Position angle from Betelgeuse (North) to Rigel (South-West) ~ 213.2°
        assertEquals(213.2, result.positionAngleDegrees, 1.0)
    }

    @Test
    fun testCardinalDirections_FromEquator() {
        val origin = EquatorialCoordinates(0.0, 0.0)

        // Due North
        val targetNorth = EquatorialCoordinates(0.0, 90.0)
        val resNorth = CelestialMeasurement.measure(origin, targetNorth)
        assertEquals(90.0, resNorth.angularDistanceDegrees, 1e-6)
        assertEquals(0.0, resNorth.positionAngleDegrees, 1e-6)

        // Due East (RA increases eastward)
        val targetEast = EquatorialCoordinates(6.0, 0.0) // 6h = 90° East
        val resEast = CelestialMeasurement.measure(origin, targetEast)
        assertEquals(90.0, resEast.angularDistanceDegrees, 1e-6)
        assertEquals(90.0, resEast.positionAngleDegrees, 1e-6)

        // Due South
        val targetSouth = EquatorialCoordinates(0.0, -90.0)
        val resSouth = CelestialMeasurement.measure(origin, targetSouth)
        assertEquals(90.0, resSouth.angularDistanceDegrees, 1e-6)
        assertEquals(180.0, resSouth.positionAngleDegrees, 1e-6)

        // Due West (RA decreases / 18h = 270° East)
        val targetWest = EquatorialCoordinates(18.0, 0.0)
        val resWest = CelestialMeasurement.measure(origin, targetWest)
        assertEquals(90.0, resWest.angularDistanceDegrees, 1e-6)
        assertEquals(270.0, resWest.positionAngleDegrees, 1e-6)
    }

    @Test
    fun testBoundary_IdenticalCoordinates() {
        val p = EquatorialCoordinates(12.345, 45.678)
        val result = CelestialMeasurement.measure(p, p)

        assertEquals(0.0, result.angularDistanceDegrees, 1e-12)
        assertEquals(0, result.degrees)
        assertEquals(0, result.arcMinutes)
        assertEquals(0.0, result.arcSeconds, 1e-6)
        assertEquals("0° 00' 00\"", result.formattedDistance)
        assertEquals(0.0, result.positionAngleDegrees, 1e-12)
    }

    @Test
    fun testBoundary_ExactAntipodalPoints() {
        val p1 = EquatorialCoordinates(2.0, 30.0)
        val p2 = EquatorialCoordinates(14.0, -30.0) // RA + 12h, Dec = -Dec

        val result = CelestialMeasurement.measure(p1, p2)
        assertEquals(180.0, result.angularDistanceDegrees, 1e-6)
        assertEquals(180, result.degrees)
        assertEquals(0, result.arcMinutes)
    }

    @Test
    fun testBoundary_RightAscensionWrapAround() {
        val p1 = EquatorialCoordinates(23.9, 0.0)
        val p2 = EquatorialCoordinates(0.1, 0.0)

        val result = CelestialMeasurement.measure(p1, p2)
        // Separation across 24h/0h boundary: (0.1 - (-0.1)) hours = 0.2h = 3.0°
        assertEquals(3.0, result.angularDistanceDegrees, 1e-6)
        assertEquals(90.0, result.positionAngleDegrees, 1e-6)
    }

    @Test
    fun testBoundary_SubArcsecondSeparation() {
        // 0.0001° = 0.36 arcseconds
        val p1 = EquatorialCoordinates(10.0, 0.0)
        val p2 = EquatorialCoordinates.fromDegrees(150.0001, 0.0)

        val result = CelestialMeasurement.measure(p1, p2)
        assertEquals(0.0001, result.angularDistanceDegrees, 1e-6)
        assertEquals(0, result.degrees)
        assertEquals(0, result.arcMinutes)
        assertEquals(0.36, result.arcSeconds, 0.01)
    }

    @Test
    fun testDmsFormatting_CarryOverAvoids60Seconds() {
        // 1.999999°: seconds round to 60 -> should roll over to 2° 00' 00"
        val dms = AngularDistance.fromDegrees(1.999999)
        assertEquals(2, dms.degrees)
        assertEquals(0, dms.arcminutes)
        assertEquals("2° 00' 00\"", dms.formatStandard())

        // 0.999999° -> 1° 00' 00"
        val dms2 = AngularDistance.fromDegrees(0.999999)
        assertEquals(1, dms2.degrees)
        assertEquals(0, dms2.arcminutes)
        assertEquals("1° 00' 00\"", dms2.formatStandard())
    }

    @Test
    fun testConsistency_VincentyVsHaversine() {
        val p1 = EquatorialCoordinates(3.5, 25.0)
        val p2 = EquatorialCoordinates(8.2, -15.0)

        val vincenty = CelestialMeasurement.angularDistance(p1, p2)
        val haversine = CelestialMeasurement.angularDistanceHaversine(p1, p2)

        assertEquals(vincenty, haversine, 1e-10)
    }

    @Test
    fun testInterpolateGreatCircle_MidpointConsistency() {
        val p1 = EquatorialCoordinates(5.9195, 7.4071)
        val p2 = EquatorialCoordinates(5.2423, -8.2016)

        val fullDist = CelestialMeasurement.angularDistance(p1, p2)
        val midpoint = CelestialMeasurement.interpolateMidpoint(p1, p2)

        val d1 = CelestialMeasurement.angularDistance(p1, midpoint)
        val d2 = CelestialMeasurement.angularDistance(midpoint, p2)

        assertEquals(fullDist / 2.0, d1, 1e-6)
        assertEquals(fullDist / 2.0, d2, 1e-6)

        val segments = CelestialMeasurement.interpolateGreatCircle(p1, p2, numSegments = 8)
        assertEquals(9, segments.size)
        assertEquals(p1.raHours, segments.first().raHours, 1e-6)
        assertEquals(p1.decDegrees, segments.first().decDegrees, 1e-6)
        assertEquals(p2.raHours, segments.last().raHours, 1e-6)
        assertEquals(p2.decDegrees, segments.last().decDegrees, 1e-6)
    }

    @Test
    fun testToEquatorialInverseRoundTrip() {
        val observer = GeoPoint(52.52, 13.405, 34.0)
        val instant = Instant.parse("2026-09-20T21:00:00Z")
        val frame = SkyCoordinateFrame(observer, instant)

        val originalEq = EquatorialCoordinates(18.61564, 38.783692) // Vega
        val horiz = frame.horizontal(originalEq.raHours, originalEq.decDegrees)
        val recoveredEq = frame.toEquatorial(horiz)

        assertEquals(originalEq.raHours, recoveredEq.raHours, 1e-5)
        assertEquals(originalEq.decDegrees, recoveredEq.decDegrees, 1e-5)
    }

    @Test
    fun testCelestialMeasurementStateProgression() {
        var state = CelestialMeasurementState(isActive = true)
        assertFalse(state.isComplete)

        val p1 = MeasurementPoint.CoordinatePoint(EquatorialCoordinates(0.0, 0.0))
        val p2 = MeasurementPoint.CoordinatePoint(EquatorialCoordinates(6.0, 0.0))

        state = state.selectPoint(p1)
        assertFalse(state.isComplete)
        assertEquals(p1, state.origin)

        state = state.selectPoint(p2)
        assertTrue(state.isComplete)
        assertNotNull(state.result)
        assertEquals(90.0, state.result!!.angularDistanceDegrees, 1e-6)
        assertEquals(90.0, state.result!!.positionAngleDegrees, 1e-6)

        // Swap direction: distance remains 90°, PA flips by 180° -> 270°
        state = state.swapDirection()
        assertEquals(90.0, state.result!!.angularDistanceDegrees, 1e-6)
        assertEquals(270.0, state.result!!.positionAngleDegrees, 1e-6)

        // Reset
        state = state.reset()
        assertTrue(state.isActive)
        assertFalse(state.isComplete)

        // Close
        state = state.close()
        assertFalse(state.isActive)
    }
}
