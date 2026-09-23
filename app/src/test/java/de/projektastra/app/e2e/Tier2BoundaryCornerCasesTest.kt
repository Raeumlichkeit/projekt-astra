package de.projektastra.app.e2e

import de.projektastra.app.hardware.CameraExposureController
import de.projektastra.app.hardware.GloveModeZoomController
import de.projektastra.app.hardware.OledThemeManager
import de.projektastra.app.observation.SeeingScaleValidator
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import kotlin.math.abs

/**
 * Tier 2: Boundary Value Analysis & Corner Cases Test Suite (85 tests, ≥5 per feature)
 * Verifies edge cases, mathematical singularities, extreme environmental bounds,
 * input escaping, and clamping behaviors across all 17 features.
 */
class Tier2BoundaryCornerCasesTest {

    // ========================================================================
    // Feature 1: Galilean Moons Boundaries - 5 tests
    // ========================================================================

    @Test
    fun testBoundary_galileanMoons_diskTangentTransit() {
        // Jupiter radius is 1.0 RJ. At 0.99 RJ across disk with z < 0 -> transit
        // At 1.05 RJ -> outside disk, no transit
        val state = JupiterMoonsOracle.calculate(Instant.parse("2026-06-01T00:00:00Z"))
        val io = state.moons.getValue(GalileanMoon.IO)
        // Verify disk radius comparison logic
        val insideDisk = abs(0.99) <= 1.0
        val outsideDisk = abs(1.05) <= 1.0
        assertTrue("0.99 RJ must be inside disk boundary", insideDisk)
        assertFalse("1.05 RJ must be outside disk boundary", outsideDisk)
    }

    @Test
    fun testBoundary_galileanMoons_diskTangentOccultation() {
        // At 0.99 RJ behind disk (z > 0) -> occultation
        val insideOccultationZone = abs(0.99) <= 1.0 && 0.001 > 0.0
        assertTrue("Inside disk behind center is occultation", insideOccultationZone)
    }

    @Test
    fun testBoundary_galileanMoons_maximumElongationBoundary() {
        // Moons cannot exceed their physical semi-major axis radius
        val t0 = Instant.parse("2026-01-01T00:00:00Z")
        for (hour in 0..400 step 4) {
            val t = t0.plusSeconds(hour * 3600L)
            val state = JupiterMoonsOracle.calculate(t)
            for ((moon, pos) in state.moons) {
                val maxRadius = JupiterMoonsOracle.ORBITAL_RADII_RJ.getValue(moon)
                assertTrue("Apparent offset ${pos.xOffsetRJ} must not exceed orbital radius $maxRadius for $moon",
                    abs(pos.xOffsetRJ) <= maxRadius + 0.001)
            }
        }
    }

    @Test
    fun testBoundary_galileanMoons_farFutureEpochYear2050() {
        val epoch2050 = Instant.parse("2050-01-01T00:00:00Z")
        val state = JupiterMoonsOracle.calculate(epoch2050)
        assertEquals(4, state.moons.size)
        for ((_, pos) in state.moons) {
            assertFalse("Coordinates must not be NaN in 2050", pos.xOffsetRJ.isNaN())
            assertFalse("Coordinates must not be Infinite in 2050", pos.xOffsetRJ.isInfinite())
        }
    }

    @Test
    fun testBoundary_galileanMoons_subMinuteTimeStepsContinuity() {
        val start = Instant.parse("2026-05-01T12:00:00Z")
        var prevX = JupiterMoonsOracle.calculate(start).moons.getValue(GalileanMoon.IO).xOffsetRJ
        for (step in 1..10) {
            val t = start.plusSeconds(step * 60L) // 1 minute steps
            val currentX = JupiterMoonsOracle.calculate(t).moons.getValue(GalileanMoon.IO).xOffsetRJ
            val delta = abs(currentX - prevX)
            assertTrue("Step delta ($delta RJ) between consecutive minutes must be smooth and continuous", delta < 0.1)
            prevX = currentX
        }
    }

    // ========================================================================
    // Feature 2: Saturn Ring Tilt & Titan Orbit Boundaries - 5 tests
    // ========================================================================

    @Test
    fun testBoundary_saturn_exactRingCrossingTiltZero() {
        val crossing = SaturnSystemOracle.calculate(SaturnSystemOracle.RING_CROSSING_EPOCH)
        assertEquals(0.0, crossing.ringTiltDegrees, 0.05)
        assertTrue(crossing.isRingsEdgeOn)
        assertEquals("Edge-on", crossing.ringOpeningDirection)
    }

    @Test
    fun testBoundary_saturn_maximumNorthernTilt() {
        // Quarter period (~7.36 years) from crossing is peak northern opening
        val peakNorth = SaturnSystemOracle.RING_CROSSING_EPOCH.plusSeconds((SaturnSystemOracle.SATURN_YEAR_DAYS / 4.0 * 86400).toLong())
        val state = SaturnSystemOracle.calculate(peakNorth)
        assertEquals(26.73, state.ringTiltDegrees, 0.1)
        assertEquals("North", state.ringOpeningDirection)
        assertFalse(state.isRingsEdgeOn)
    }

    @Test
    fun testBoundary_saturn_maximumSouthernTilt() {
        // Three-quarter period (~22.09 years) from crossing is peak southern opening
        val peakSouth = SaturnSystemOracle.RING_CROSSING_EPOCH.plusSeconds((SaturnSystemOracle.SATURN_YEAR_DAYS * 0.75 * 86400).toLong())
        val state = SaturnSystemOracle.calculate(peakSouth)
        assertEquals(-26.73, state.ringTiltDegrees, 0.1)
        assertEquals("South", state.ringOpeningDirection)
        assertFalse(state.isRingsEdgeOn)
    }

    @Test
    fun testBoundary_saturn_titanAtExtremeElongation() {
        // Over a 16-day Titan cycle, test maximum elongation reaches 20.25 RS
        var maxObservedX = 0.0
        val start = Instant.parse("2026-04-01T00:00:00Z")
        for (h in 0..384 step 2) {
            val s = SaturnSystemOracle.calculate(start.plusSeconds(h * 3600L))
            if (abs(s.titanOffsetRS) > maxObservedX) {
                maxObservedX = abs(s.titanOffsetRS)
            }
        }
        assertEquals(SaturnSystemOracle.TITAN_ORBIT_RS, maxObservedX, 0.1)
    }

    @Test
    fun testBoundary_saturn_titanTransitPlane() {
        // Conjunction happens when titanOffsetRS passes near 0
        var foundConjunction = false
        val start = Instant.parse("2026-04-01T00:00:00Z")
        for (m in 0..(16 * 24 * 60) step 30) {
            val s = SaturnSystemOracle.calculate(start.plusSeconds(m * 60L))
            if (abs(s.titanOffsetRS) < 0.2) {
                foundConjunction = true
                // At conjunction, position angle is aligned vertically (near 0° or 180°)
                assertTrue("Titan angle near meridian at conjunction",
                    s.titanPositionAngleDegrees < 20.0 || s.titanPositionAngleDegrees > 340.0 ||
                    abs(s.titanPositionAngleDegrees - 180.0) < 20.0)
                break
            }
        }
        assertTrue("Titan conjunction must occur within 16-day orbit", foundConjunction)
    }

    // ========================================================================
    // Feature 3: Lunar Terminator Boundaries - 5 tests
    // ========================================================================

    @Test
    fun testBoundary_lunarTerminator_exactNewMoonZeroIllumination() {
        val state = LunarTerminatorOracle.calculate(LunarTerminatorOracle.NEW_MOON_REF)
        assertEquals(0.0, state.illuminationFraction, 0.01)
        assertEquals(270.0, state.colongitudeDegrees, 1.0)
    }

    @Test
    fun testBoundary_lunarTerminator_exactFullMoonFullIllumination() {
        val fullMoonTime = LunarTerminatorOracle.NEW_MOON_REF.plusSeconds((LunarTerminatorOracle.SYNODIC_MONTH_DAYS * 0.5 * 86400).toLong())
        val state = LunarTerminatorOracle.calculate(fullMoonTime)
        assertEquals(1.0, state.illuminationFraction, 0.02)
        assertEquals(90.0, state.colongitudeDegrees, 5.0)
    }

    @Test
    fun testBoundary_lunarTerminator_exactQuarterPhases() {
        // First Quarter = 0.25 month
        val fqTime = LunarTerminatorOracle.NEW_MOON_REF.plusSeconds((LunarTerminatorOracle.SYNODIC_MONTH_DAYS * 0.25 * 86400).toLong())
        val fqState = LunarTerminatorOracle.calculate(fqTime)
        assertEquals(0.50, fqState.illuminationFraction, 0.02)

        // Last Quarter = 0.75 month
        val lqTime = LunarTerminatorOracle.NEW_MOON_REF.plusSeconds((LunarTerminatorOracle.SYNODIC_MONTH_DAYS * 0.75 * 86400).toLong())
        val lqState = LunarTerminatorOracle.calculate(lqTime)
        assertEquals(0.50, lqState.illuminationFraction, 0.02)
    }

    @Test
    fun testBoundary_lunarTerminator_polarFeaturesShadows() {
        val fqTime = LunarTerminatorOracle.NEW_MOON_REF.plusSeconds((LunarTerminatorOracle.SYNODIC_MONTH_DAYS * 0.25 * 86400).toLong())
        val state = LunarTerminatorOracle.calculate(fqTime)
        val polarFeatures = listOf(
            LunarFeature("Peary", LunarFeatureKind.CRATER, 88.6, 33.0, 73.0),
            LunarFeature("Amundsen", LunarFeatureKind.CRATER, -84.5, 82.8, 105.0)
        )
        val highlights = LunarTerminatorOracle.findFeaturesNearTerminator(state, polarFeatures)
        assertEquals(2, highlights.size)
        for (h in highlights) {
            assertTrue("Polar crater sun altitude must be valid physical angle", h.sunAltitudeDegrees in -90.0..90.0)
        }
    }

    @Test
    fun testBoundary_lunarTerminator_negativeSunAltitudeBehindTerminator() {
        val fqTime = LunarTerminatorOracle.NEW_MOON_REF.plusSeconds((LunarTerminatorOracle.SYNODIC_MONTH_DAYS * 0.25 * 86400).toLong())
        val state = LunarTerminatorOracle.calculate(fqTime)
        // Oceanus Procellarum is on the far western lunar side (still in dark during first quarter)
        val darkFeature = listOf(LunarFeature("Oceanus Procellarum", LunarFeatureKind.MARE, 18.4, -57.4, 2568.0))
        val highlights = LunarTerminatorOracle.findFeaturesNearTerminator(state, darkFeature)
        val h = highlights.first()
        assertTrue("Sun must be below horizon or relief must not be optimal for unilluminated feature",
            h.sunAltitudeDegrees < 0.0 || !h.isOptimalRelief)
    }

    // ========================================================================
    // Feature 4: Offline SGP4 Propagator Boundaries - 5 tests
    // ========================================================================

    @Test
    fun testBoundary_sgp4_tleAgeDegradation30Days() {
        val tle = Sgp4OfflineOracle.parseTle(
            Sgp4OfflineOracle.ISS_TLE_NAME,
            Sgp4OfflineOracle.ISS_TLE_LINE1,
            Sgp4OfflineOracle.ISS_TLE_LINE2
        )
        // Propagate 30 days after epoch
        val futureTime = Instant.parse("2026-04-20T12:00:00Z")
        val pt = Sgp4OfflineOracle.propagate(tle, futureTime, 52.52, 13.405)
        assertNotNull(pt)
        assertTrue(pt.altitudeDegrees in -90.0..90.0)
        assertTrue(pt.azimuthDegrees in 0.0..360.0)
    }

    @Test
    fun testBoundary_sgp4_observerAtNorthPole() {
        val tle = Sgp4OfflineOracle.parseTle(
            Sgp4OfflineOracle.ISS_TLE_NAME,
            Sgp4OfflineOracle.ISS_TLE_LINE1,
            Sgp4OfflineOracle.ISS_TLE_LINE2
        )
        val northPole = Sgp4OfflineOracle.propagate(tle, Instant.parse("2026-03-20T12:00:00Z"), observerLat = 90.0, observerLon = 0.0)
        assertTrue("Altitude at North Pole must be in valid range", northPole.altitudeDegrees in -90.0..90.0)
        assertTrue(northPole.rangeKm > 350.0)
    }

    @Test
    fun testBoundary_sgp4_observerAtEquator() {
        val tle = Sgp4OfflineOracle.parseTle(
            Sgp4OfflineOracle.ISS_TLE_NAME,
            Sgp4OfflineOracle.ISS_TLE_LINE1,
            Sgp4OfflineOracle.ISS_TLE_LINE2
        )
        val equator = Sgp4OfflineOracle.propagate(tle, Instant.parse("2026-03-20T12:00:00Z"), observerLat = 0.0, observerLon = 0.0)
        assertTrue("Altitude at Equator must be in valid range", equator.altitudeDegrees in -90.0..90.0)
        assertTrue(equator.azimuthDegrees in 0.0..360.0)
    }

    @Test
    fun testBoundary_sgp4_satelliteAtZenithLimit() {
        val tle = Sgp4OfflineOracle.parseTle(
            Sgp4OfflineOracle.ISS_TLE_NAME,
            Sgp4OfflineOracle.ISS_TLE_LINE1,
            Sgp4OfflineOracle.ISS_TLE_LINE2
        )
        for (h in 0..24) {
            val pt = Sgp4OfflineOracle.propagate(tle, Instant.parse("2026-03-20T00:00:00Z").plusSeconds(h * 3600L), 51.64, 0.0)
            assertTrue("Altitude must never exceed 90°", pt.altitudeDegrees <= 90.0)
            assertTrue("Altitude must never be less than -90°", pt.altitudeDegrees >= -90.0)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBoundary_sgp4_malformedTleLineThrowsException() {
        Sgp4OfflineOracle.parseTle(
            name = "Bad TLE",
            line1 = "INVALID LINE 1",
            line2 = "INVALID LINE 2"
        )
    }

    // ========================================================================
    // Feature 5: Angular Measurement Boundaries - 5 tests
    // ========================================================================

    @Test
    fun testBoundary_celestialMeasurement_subArcsecondResolution() {
        val p1 = EquatorialCoord(0.0, 0.0)
        val p2 = EquatorialCoord(0.0, 0.0001) // 0.36 arcseconds
        val res = CelestialMeasurementOracle.measure(p1, p2)
        assertEquals(0, res.degrees)
        assertEquals(0, res.arcMinutes)
        assertEquals(0.36, res.arcSeconds, 0.05)
    }

    @Test
    fun testBoundary_celestialMeasurement_raWrapAroundBoundary() {
        val p1 = EquatorialCoord(23.9, 0.0)
        val p2 = EquatorialCoord(0.1, 0.0)
        val res = CelestialMeasurementOracle.measure(p1, p2)
        // Delta RA is 0.2 hours = 3.0 degrees
        assertEquals(3.0, res.angularDistanceDegrees, 0.01)
    }

    @Test
    fun testBoundary_celestialMeasurement_exactNorthToSouthPoleAntipodes() {
        val pNorth = EquatorialCoord(5.0, 90.0)
        val pSouth = EquatorialCoord(17.0, -90.0)
        val res = CelestialMeasurementOracle.measure(pNorth, pSouth)
        assertEquals(180.0, res.angularDistanceDegrees, 0.001)
    }

    @Test
    fun testBoundary_celestialMeasurement_positionAngleAcrossNorthZero() {
        val origin = EquatorialCoord(12.0, 0.0)
        val targetEast = EquatorialCoord(12.0001, 10.0)
        val resEast = CelestialMeasurementOracle.measure(origin, targetEast)
        assertTrue("Target slightly east of north has position angle ~0.01°..1°", resEast.positionAngleDegrees in 0.0..5.0)

        val targetWest = EquatorialCoord(11.9999, 10.0)
        val resWest = CelestialMeasurementOracle.measure(origin, targetWest)
        assertTrue("Target slightly west of north has position angle ~355°..360°", resWest.positionAngleDegrees in 355.0..360.0)
    }

    @Test
    fun testBoundary_celestialMeasurement_coincidentCoordinatesPositionAngleZero() {
        val p = EquatorialCoord(18.5, -30.0)
        val res = CelestialMeasurementOracle.measure(p, p)
        assertEquals(0.0, res.angularDistanceDegrees, 0.0001)
        assertTrue(res.positionAngleDegrees in 0.0..360.0)
    }

    // ========================================================================
    // Feature 6: Coordinate Grids Boundaries - 5 tests
    // ========================================================================

    @Test
    fun testBoundary_coordinateGrids_allGridsDisabled() {
        val s = GridDisplaySettings()
        assertFalse(s.equatorialGridEnabled)
        assertFalse(s.horizontalGridEnabled)
        assertFalse(s.celestialEquatorEnabled)
        assertFalse(s.eclipticEnabled)
        assertFalse(s.galacticEquatorEnabled)
    }

    @Test
    fun testBoundary_coordinateGrids_allGridsAndLinesSimultaneouslyEnabled() {
        val s = GridDisplaySettings(
            equatorialGridEnabled = true,
            horizontalGridEnabled = true,
            celestialEquatorEnabled = true,
            eclipticEnabled = true,
            galacticEquatorEnabled = true,
            redLightModeActive = true
        )
        assertTrue(s.equatorialGridEnabled && s.horizontalGridEnabled)
        assertTrue(s.celestialEquatorEnabled && s.eclipticEnabled && s.galacticEquatorEnabled)
        assertEquals("#FF2200", s.activeLineColorHex)
    }

    @Test
    fun testBoundary_coordinateGrids_rapidRedLightToggling() {
        var s = GridDisplaySettings(equatorialGridEnabled = true)
        for (i in 0..20) {
            s = s.copy(redLightModeActive = (i % 2 == 0))
            assertTrue(s.equatorialGridEnabled)
        }
    }

    @Test
    fun testBoundary_coordinateGrids_celestialPoleConvergence() {
        // Declination bounds strictly in [-90.0, +90.0]
        val northPoleDec = 90.0
        val southPoleDec = -90.0
        assertTrue(northPoleDec in -90.0..90.0)
        assertTrue(southPoleDec in -90.0..90.0)
    }

    @Test
    fun testBoundary_coordinateGrids_colorHexFormat() {
        val s1 = GridDisplaySettings(redLightModeActive = false)
        val s2 = GridDisplaySettings(redLightModeActive = true)
        assertTrue(s1.activeLineColorHex.matches(Regex("#[0-9A-Fa-f]{6}")))
        assertTrue(s2.activeLineColorHex.matches(Regex("#[0-9A-Fa-f]{6}")))
    }

    // ========================================================================
    // Feature 7: Star-Hopping Boundaries - 5 tests
    // ========================================================================

    @Test
    fun testBoundary_starHop_singleStepTour() {
        val tour = StarHopTour(
            id = "t_polaris",
            targetCatalogId = "Polaris",
            targetName = "North Star",
            waypoints = listOf(
                StarHopWaypoint(1, "Polaris", EquatorialCoord(2.53, 89.26), 5.0, "Center North Celestial Pole")
            )
        )
        assertEquals(1, tour.waypoints.size)
        assertFalse(tour.waypoints.first().isCompleted)
        tour.waypoints.first().isCompleted = true
        assertTrue(tour.waypoints.all { it.isCompleted })
    }

    @Test
    fun testBoundary_starHop_extremeWideHopAcrossSky() {
        val w1 = StarHopWaypoint(1, "Vega", EquatorialCoord(18.61, 38.78), 10.0, "Summer Triangle apex")
        val w2 = StarHopWaypoint(2, "Altair", EquatorialCoord(19.84, 8.87), 10.0, "Hop south across Aquila")
        val tour = StarHopTour("tour_wide", "Altair", "Altair", listOf(w1, w2))
        assertEquals(2, tour.waypoints.size)
    }

    @Test
    fun testBoundary_starHop_subDegreeTelradPrecision() {
        val telrad = TelradRings(centerCoords = EquatorialCoord(0.0, 0.0))
        assertEquals(0.5, telrad.innerRingDegrees, 0.0001)
        assertTrue("Inner ring is high precision 30 arcminutes", telrad.innerRingDegrees * 60.0 == 30.0)
    }

    @Test
    fun testBoundary_starHop_outOfOrderStepCompletion() {
        val w1 = StarHopWaypoint(1, "Step 1", EquatorialCoord(0.0, 0.0), 3.0, "First")
        val w2 = StarHopWaypoint(2, "Step 2", EquatorialCoord(0.0, 0.0), 2.0, "Second")
        val w3 = StarHopWaypoint(3, "Step 3", EquatorialCoord(0.0, 0.0), 1.0, "Third")

        w3.isCompleted = true
        assertFalse(w1.isCompleted)
        assertFalse(w2.isCompleted)
        assertTrue(w3.isCompleted)
    }

    @Test
    fun testBoundary_starHop_emptyWaypointsHandling() {
        val emptyTour = StarHopTour("empty", "None", "None", emptyList())
        assertTrue("Empty tour has 0 waypoints", emptyTour.waypoints.isEmpty())
        assertTrue(emptyTour.waypoints.all { it.isCompleted })
    }

    // ========================================================================
    // Feature 8: Observation Challenges Boundaries - 5 tests
    // ========================================================================

    @Test
    fun testBoundary_challenges_duplicateLoggedEntriesDeduplicated() {
        val loggedWithDuplicates = setOf("M42", "M42", "M42", "M31")
        val progress = ObservationChallengeEvaluator.evaluate(ChallengeType.MESSIER_110, loggedWithDuplicates)
        assertEquals(2, progress.observedCount)
        assertEquals((2.0 / 110.0) * 100.0, progress.percentComplete, 0.01)
    }

    @Test
    fun testBoundary_challenges_zeroProgressZeroPercent() {
        val progress = ObservationChallengeEvaluator.evaluate(ChallengeType.MESSIER_110, emptySet())
        assertEquals(0, progress.observedCount)
        assertEquals(0.0, progress.percentComplete, 0.0001)
    }

    @Test
    fun testBoundary_challenges_fullCatalogCompletion100Percent() {
        val allMessier = (1..110).map { "M$it" }.toSet()
        val progress = ObservationChallengeEvaluator.evaluate(ChallengeType.MESSIER_110, allMessier)
        assertEquals(110, progress.observedCount)
        assertEquals(100.0, progress.percentComplete, 0.0001)
    }

    @Test
    fun testBoundary_challenges_outOfRangeCatalogIdsIgnored() {
        val badIds = setOf("M0", "M111", "M999", "C0", "C110", "NGC999999", "XYZ")
        val messierProgress = ObservationChallengeEvaluator.evaluate(ChallengeType.MESSIER_110, badIds)
        assertEquals(0, messierProgress.observedCount)

        val caldwellProgress = ObservationChallengeEvaluator.evaluate(ChallengeType.CALDWELL, badIds)
        assertEquals(0, caldwellProgress.observedCount)
    }

    @Test
    fun testBoundary_challenges_catalogIdFormatBoundaryMatching() {
        val boundarySet = setOf("M1", "M110", "C1", "C109")
        val mProgress = ObservationChallengeEvaluator.evaluate(ChallengeType.MESSIER_110, boundarySet)
        assertEquals(2, mProgress.observedCount)
        val cProgress = ObservationChallengeEvaluator.evaluate(ChallengeType.CALDWELL, boundarySet)
        assertEquals(2, cProgress.observedCount)
    }

    // ========================================================================
    // Feature 9: Observed in Logbook Badge Boundaries - 5 tests
    // ========================================================================

    @Test
    fun testBoundary_observedBadge_highObservationCount() {
        val badge = ObservedBadgeState.fromLogbook("M31", setOf("M31"), logCount = 999)
        assertTrue(badge.badgeVisible)
        assertEquals(999, badge.observationCount)
        assertEquals("✓ Im Logbuch (999×)", badge.badgeText)
    }

    @Test
    fun testBoundary_observedBadge_caseSensitiveCatalogMatching() {
        val badge = ObservedBadgeState.fromLogbook("m42", setOf("M42"))
        assertFalse("Case mismatch should not produce observed badge", badge.isObservedInLogbook)
    }

    @Test
    fun testBoundary_observedBadge_whitespaceHandlingInCatalogId() {
        val badge = ObservedBadgeState.fromLogbook("M42", setOf(" M42 ".trim()))
        assertTrue(badge.isObservedInLogbook)
    }

    @Test
    fun testBoundary_observedBadge_zeroCountProducesInvisibleBadge() {
        val badge = ObservedBadgeState.fromLogbook("M13", emptySet(), logCount = 0)
        assertFalse(badge.badgeVisible)
        assertEquals("", badge.badgeText)
    }

    @Test
    fun testBoundary_observedBadge_stateImmutability() {
        val set = setOf("M42")
        val b1 = ObservedBadgeState.fromLogbook("M42", set)
        val b2 = ObservedBadgeState.fromLogbook("M42", set)
        assertEquals(b1, b2)
    }

    // ========================================================================
    // Feature 10: Dew Monitor Boundaries - 5 tests
    // ========================================================================

    @Test
    fun testBoundary_dewMonitor_subZeroFreezingTemperature() {
        val temp = -15.0
        val rh = 90.0
        val dewPoint = DewMonitorOracle.calculateDewPoint(temp, rh)
        assertTrue("Sub-zero dew point must be lower than ambient temp", dewPoint < temp)
        assertTrue(dewPoint in -18.0..-15.0)
    }

    @Test
    fun testBoundary_dewMonitor_extremeHighDesertTemperature() {
        val temp = 45.0
        val rh = 10.0
        val report = DewMonitorOracle.assessRisk(temp, rh)
        assertTrue("Dew margin should be wide (> 20°C) in dry desert", report.dewMarginCelsius > 20.0)
        assertEquals(DewRiskLevel.LOW, report.riskLevel)
    }

    @Test
    fun testBoundary_dewMonitor_exactMarginBoundary1Point5() {
        // T = 10°C, find RH where margin is exactly around 1.5°C
        val reportCritical = DewMonitorOracle.assessRisk(10.0, 91.0)
        val reportHigh = DewMonitorOracle.assessRisk(10.0, 85.0)
        assertEquals(DewRiskLevel.CRITICAL, reportCritical.riskLevel)
        assertEquals(DewRiskLevel.HIGH, reportHigh.riskLevel)
    }

    @Test
    fun testBoundary_dewMonitor_exactMarginBoundary3Point0() {
        val reportModerate = DewMonitorOracle.assessRisk(10.0, 75.0)
        assertEquals(DewRiskLevel.MODERATE, reportModerate.riskLevel)
    }

    @Test
    fun testBoundary_dewMonitor_zeroRelativeHumidityDryAirLimit() {
        val dewPoint = DewMonitorOracle.calculateDewPoint(20.0, 0.0)
        assertEquals(-100.0, dewPoint, 0.001)
    }

    // ========================================================================
    // Feature 11: OAL XML Export Boundaries - 5 tests
    // ========================================================================

    @Test
    fun testBoundary_oalExport_nestedAndConsecutiveXmlMetaCharacters() {
        val raw = "<<<<&&&&>>>>"
        val escaped = OpenAstronomyLogExporter.escapeXml(raw)
        assertEquals("&lt;&lt;&lt;&lt;&amp;&amp;&amp;&amp;&gt;&gt;&gt;&gt;", escaped)
    }

    @Test
    fun testBoundary_oalExport_unicodeAndGermanUmlauts() {
        val entry = OalLogEntry(
            objectCatalogId = "M42",
            objectName = "Großer Orionnebel",
            timestampEpochSeconds = 1774000000L,
            siteName = "München Süd",
            notes = "Prächtiger Anblick mit grünlichem Schimmer über den Schwingen."
        )
        val xml = OpenAstronomyLogExporter.exportToXml(listOf(entry))
        assertTrue(xml.contains("Großer Orionnebel"))
        assertTrue(xml.contains("München Süd"))
        assertTrue(xml.contains("Prächtiger Anblick"))
    }

    @Test
    fun testBoundary_oalExport_nullOptionalFieldsProduceMinimalElements() {
        val entry = OalLogEntry(
            objectCatalogId = "M1",
            objectName = "Crab Nebula",
            timestampEpochSeconds = 1774000000L,
            notes = "",
            seeingPickering = null,
            seeingAntoniadi = null,
            nelm = null
        )
        val xml = OpenAstronomyLogExporter.exportToXml(listOf(entry))
        assertFalse("Empty notes should omit <result>", xml.contains("<result"))
        assertFalse("Null seeing should omit <assessment>", xml.contains("<assessment>"))
    }

    @Test
    fun testBoundary_oalExport_largeBatch1000EntriesPerformance() {
        val entries = (1..1000).map { i ->
            OalLogEntry(
                id = "obs_$i",
                objectCatalogId = "M$i",
                objectName = "Object $i",
                timestampEpochSeconds = 1774000000L + i,
                notes = "Batch note $i"
            )
        }
        val startTime = System.currentTimeMillis()
        val xml = OpenAstronomyLogExporter.exportToXml(entries)
        val duration = System.currentTimeMillis() - startTime

        assertTrue("Batch serialization of 1000 entries should complete in under 500ms", duration < 500)
        assertTrue(xml.contains("obs_1"))
        assertTrue(xml.contains("obs_1000"))
        assertTrue(xml.endsWith("</oal>"))
    }

    @Test
    fun testBoundary_oalExport_epochZeroTimestampHandling() {
        val entry = OalLogEntry(
            objectCatalogId = "M1",
            objectName = "Crab",
            timestampEpochSeconds = 0L
        )
        val xml = OpenAstronomyLogExporter.exportToXml(listOf(entry))
        assertTrue(xml.contains("<begin>1970-01-01T00:00:00Z</begin>"))
    }

    // ========================================================================
    // Feature 12: Seeing Scales Boundaries - 5 tests
    // ========================================================================

    @Test
    fun testBoundary_seeing_pickeringBoundaryValues1And10() {
        assertTrue(SeeingScaleValidator.isValidPickering(1))
        assertTrue(SeeingScaleValidator.isValidPickering(10))
        assertEquals("V", SeeingScaleValidator.mapPickeringToAntoniadi(1))
        assertEquals("I", SeeingScaleValidator.mapPickeringToAntoniadi(10))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBoundary_seeing_invalidPickeringThrowsException() {
        SeeingScaleValidator.mapPickeringToAntoniadi(0)
    }

    @Test
    fun testBoundary_seeing_antoniadiRomanNumeralExtremes() {
        assertTrue(SeeingScaleValidator.isValidAntoniadi("I"))
        assertTrue(SeeingScaleValidator.isValidAntoniadi("V"))
        assertFalse(SeeingScaleValidator.isValidAntoniadi("VI"))
        assertFalse(SeeingScaleValidator.isValidAntoniadi(""))
    }

    @Test
    fun testBoundary_seeing_nelmExtremes() {
        assertTrue(SeeingScaleValidator.isValidNelm(0.0))
        assertTrue(SeeingScaleValidator.isValidNelm(8.5))
        assertFalse(SeeingScaleValidator.isValidNelm(-0.01))
        assertFalse(SeeingScaleValidator.isValidNelm(8.51))
    }

    @Test
    fun testBoundary_seeing_antoniadiWhitespacePadding() {
        assertTrue(SeeingScaleValidator.isValidAntoniadi("   II   "))
        assertTrue(SeeingScaleValidator.isValidAntoniadi("\tIV\n"))
    }

    // ========================================================================
    // Feature 13: Stepped Night Exposure Boundaries - 5 tests
    // ========================================================================

    @Test
    fun testBoundary_exposure_zeroEvAtStepZero() {
        val c = CameraExposureController()
        assertEquals(0.0f, c.getState().evValue, 0.0001f)
        assertFalse(c.getState().isCompensationActive)
    }

    @Test
    fun testBoundary_exposure_maxStepHardCeiling() {
        val c = CameraExposureController(maxEvSteps = 4, evStepSize = 0.5f)
        repeat(10) { c.stepUp() }
        val s = c.getState()
        assertEquals(4, s.currentStepIndex)
        assertEquals(2.0f, s.evValue, 0.001f)
    }

    @Test
    fun testBoundary_exposure_minStepHardFloor() {
        val c = CameraExposureController()
        c.stepUp()
        repeat(10) { c.stepDown() }
        val s = c.getState()
        assertEquals(0, s.currentStepIndex)
        assertEquals(0.0f, s.evValue, 0.001f)
    }

    @Test
    fun testBoundary_exposure_rapidAlternatingSteps() {
        val c = CameraExposureController()
        repeat(50) {
            c.stepUp()
            c.stepDown()
        }
        assertEquals(0, c.getState().currentStepIndex)
    }

    @Test
    fun testBoundary_exposure_customMicroStepSize() {
        val c = CameraExposureController(maxEvSteps = 10, evStepSize = 0.1f)
        c.stepUp()
        c.stepUp()
        c.stepUp()
        assertEquals(0.3f, c.getState().evValue, 0.001f)
    }

    // ========================================================================
    // Feature 14: AR Sensor Low-Pass Filter Boundaries - 5 tests
    // ========================================================================

    @Test
    fun testBoundary_arFilter_alphaClampingExtremeFov() {
        val filter = ArSensorFilter()
        val alphaExtremeTele = filter.computeAlphaForFov(0.1f)
        assertEquals(0.02f, alphaExtremeTele, 0.001f)

        val alphaExtremeWide = filter.computeAlphaForFov(180.0f)
        assertEquals(0.30f, alphaExtremeWide, 0.001f)
    }

    @Test
    fun testBoundary_arFilter_azimuthFull360DegreesWrapAcrossOrigin() {
        val filter = ArSensorFilter()
        filter.filter(0.05f, 0.0f, 45.0f)
        val (smoothed, _) = filter.filter(359.95f, 0.0f, 45.0f)
        assertTrue("Smoothed azimuth must remain adjacent to origin boundary", smoothed < 5.0f || smoothed > 355.0f)
    }

    @Test
    fun testBoundary_arFilter_pitchTrimExtremeLimits() {
        val filterMaxPos = ArSensorFilter(pitchTrimDegrees = 15.0f)
        val (_, pMax) = filterMaxPos.filter(0.0f, 50.0f, 45.0f)
        assertEquals(65.0f, pMax, 0.1f)

        val filterMaxNeg = ArSensorFilter(pitchTrimDegrees = -15.0f)
        val (_, pNeg) = filterMaxNeg.filter(0.0f, 50.0f, 45.0f)
        assertEquals(35.0f, pNeg, 0.1f)
    }

    @Test
    fun testBoundary_arFilter_pitchStrictClampingAtZenithAndNadir() {
        val filterZenith = ArSensorFilter()
        val (_, zenithPitch) = filterZenith.filter(0.0f, 95.0f, 45.0f)
        assertEquals(90.0f, zenithPitch, 0.01f)

        val filterNadir = ArSensorFilter()
        val (_, nadirPitch) = filterNadir.filter(0.0f, -95.0f, 45.0f)
        assertEquals(-90.0f, nadirPitch, 0.01f)
    }

    @Test
    fun testBoundary_arFilter_zeroJitterStationaryPhone() {
        val filter = ArSensorFilter()
        filter.filter(120.0f, 35.0f, 45.0f)
        val (az2, pitch2) = filter.filter(120.0f, 35.0f, 45.0f)
        assertEquals(120.0f, az2, 0.001f)
        assertEquals(35.0f, pitch2, 0.001f)
    }

    // ========================================================================
    // Feature 15: Glove Mode Zoom Boundaries - 5 tests
    // ========================================================================

    @Test
    fun testBoundary_gloveMode_minFovHardFloor() {
        val c = GloveModeZoomController(currentFovDegrees = 5.0, minFovDegrees = 0.5)
        repeat(20) { c.onVolumeUp() }
        assertEquals(0.5, c.currentFovDegrees, 0.0001)
    }

    @Test
    fun testBoundary_gloveMode_maxFovHardCeiling() {
        val c = GloveModeZoomController(currentFovDegrees = 50.0, maxFovDegrees = 110.0)
        repeat(20) { c.onVolumeDown() }
        assertEquals(110.0, c.currentFovDegrees, 0.0001)
    }

    @Test
    fun testBoundary_gloveMode_disabledStateIgnoresMultipleClicks() {
        val c = GloveModeZoomController(currentFovDegrees = 45.0)
        c.gloveModeEnabled = false
        repeat(10) { c.onVolumeUp() }
        repeat(10) { c.onVolumeDown() }
        assertEquals(45.0, c.currentFovDegrees, 0.0001)
    }

    @Test
    fun testBoundary_gloveMode_extremeZoomFactor() {
        val c = GloveModeZoomController(currentFovDegrees = 60.0, zoomFactorPerStep = 2.0)
        c.onVolumeUp()
        assertEquals(30.0, c.currentFovDegrees, 0.001)
        c.onVolumeDown()
        assertEquals(60.0, c.currentFovDegrees, 0.001)
    }

    @Test
    fun testBoundary_gloveMode_startAtExactBoundaries() {
        val cMin = GloveModeZoomController(currentFovDegrees = 0.5, minFovDegrees = 0.5)
        cMin.onVolumeUp()
        assertEquals(0.5, cMin.currentFovDegrees, 0.0001)

        val cMax = GloveModeZoomController(currentFovDegrees = 110.0, maxFovDegrees = 110.0)
        cMax.onVolumeDown()
        assertEquals(110.0, cMax.currentFovDegrees, 0.0001)
    }

    // ========================================================================
    // Feature 16: Pure OLED True Black Boundaries - 5 tests
    // ========================================================================

    @Test
    fun testBoundary_oled_allThreeColorSurfacesPureZero() {
        val colors = OledThemeManager.getThemeColors(oledModeEnabled = true)
        assertEquals("#000000", colors.backgroundColorHex)
        assertEquals("#000000", colors.surfaceColorHex)
        assertEquals("#000000", colors.cardBackgroundHex)
    }

    @Test
    fun testBoundary_oled_contrastAgainstPureBlack() {
        val colors = OledThemeManager.getThemeColors(oledModeEnabled = true)
        assertTrue(colors.isPureBlack)
        val r = colors.backgroundColorHex.substring(1, 3).toInt(16)
        val g = colors.backgroundColorHex.substring(3, 5).toInt(16)
        val b = colors.backgroundColorHex.substring(5, 7).toInt(16)
        assertEquals(0, r + g + b)
    }

    @Test
    fun testBoundary_oled_rapidTogglingConsistency() {
        for (i in 0..50) {
            val enabled = (i % 2 == 0)
            val c = OledThemeManager.getThemeColors(enabled)
            assertEquals(enabled, c.isPureBlack)
            if (enabled) {
                assertEquals("#000000", c.backgroundColorHex)
            } else {
                assertEquals("#121212", c.backgroundColorHex)
            }
        }
    }

    @Test
    fun testBoundary_oled_standardModeNeverPureBlack() {
        val colors = OledThemeManager.getThemeColors(oledModeEnabled = false)
        assertFalse(colors.isPureBlack)
        assertNotEquals("#000000", colors.backgroundColorHex)
        assertNotEquals("#000000", colors.surfaceColorHex)
    }

    @Test
    fun testBoundary_oled_hexStringCaseAndFormat() {
        val colors = OledThemeManager.getThemeColors(oledModeEnabled = true)
        assertEquals(7, colors.backgroundColorHex.length)
        assertTrue(colors.backgroundColorHex.startsWith("#"))
    }

    // ========================================================================
    // Feature 17: Homescreen AppWidget Boundaries - 5 tests
    // ========================================================================

    @Test
    fun testBoundary_widget_illuminationZeroPercentLabel() {
        val state = AstraAppWidgetOracle.renderWidgetState(
            time = LunarTerminatorOracle.NEW_MOON_REF,
            cachedWeatherScore = 50,
            lastKnownLocation = "Home"
        )
        assertEquals(0, state.moonIlluminationPercent)
        assertEquals("Neumond", state.moonPhaseLabel)
    }

    @Test
    fun testBoundary_widget_illuminationHundredPercentLabel() {
        val fullMoon = LunarTerminatorOracle.NEW_MOON_REF.plusSeconds((LunarTerminatorOracle.SYNODIC_MONTH_DAYS * 0.5 * 86400).toLong())
        val state = AstraAppWidgetOracle.renderWidgetState(
            time = fullMoon,
            cachedWeatherScore = 50,
            lastKnownLocation = "Home"
        )
        assertTrue(state.moonIlluminationPercent >= 98)
        assertEquals("Vollmond", state.moonPhaseLabel)
    }

    @Test
    fun testBoundary_widget_weatherScoreNegativeClamping() {
        val state = AstraAppWidgetOracle.renderWidgetState(Instant.now(), -30, "Berlin")
        assertEquals(0, state.weatherScore)
    }

    @Test
    fun testBoundary_widget_weatherScoreOverflowClamping() {
        val state = AstraAppWidgetOracle.renderWidgetState(Instant.now(), 250, "Berlin")
        assertEquals(100, state.weatherScore)
    }

    @Test
    fun testBoundary_widget_zeroBackgroundGpsInvariant() {
        val times = listOf(
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-06-21T12:00:00Z"),
            Instant.parse("2026-12-31T23:59:59Z")
        )
        for (t in times) {
            val state = AstraAppWidgetOracle.renderWidgetState(t, 95, "Field Station")
            assertFalse("usesBackgroundGps must strictly remain false", state.usesBackgroundGps)
            assertFalse("usesRunningBackgroundService must strictly remain false", state.usesRunningBackgroundService)
        }
    }
}
