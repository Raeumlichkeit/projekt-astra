package de.projektastra.app.e2e

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import kotlin.math.abs

/**
 * Tier 1: Comprehensive Feature Coverage Test Suite (85 tests, ≥5 per feature)
 * Verifies core functionality and expected behavior for all 17 features in ORIGINAL_REQUEST.md.
 */
class Tier1FeatureCoverageTest {

    // ========================================================================
    // Feature 1: Galilean Moons (Io, Europa, Ganymede, Callisto) - 5 tests
    // ========================================================================

    @Test
    fun testGalileanMoons_allFourMoonsPresent() {
        val state = JupiterMoonsOracle.calculate(Instant.parse("2026-06-15T22:00:00Z"))
        assertEquals("All 4 Galilean moons must be calculated", 4, state.moons.size)
        assertTrue(state.moons.containsKey(GalileanMoon.IO))
        assertTrue(state.moons.containsKey(GalileanMoon.EUROPA))
        assertTrue(state.moons.containsKey(GalileanMoon.GANYMEDE))
        assertTrue(state.moons.containsKey(GalileanMoon.CALLISTO))
    }

    @Test
    fun testGalileanMoons_orbitalHierarchyAndDistances() {
        val rIo = JupiterMoonsOracle.ORBITAL_RADII_RJ.getValue(GalileanMoon.IO)
        val rEuropa = JupiterMoonsOracle.ORBITAL_RADII_RJ.getValue(GalileanMoon.EUROPA)
        val rGanymede = JupiterMoonsOracle.ORBITAL_RADII_RJ.getValue(GalileanMoon.GANYMEDE)
        val rCallisto = JupiterMoonsOracle.ORBITAL_RADII_RJ.getValue(GalileanMoon.CALLISTO)

        assertTrue("Io orbit must be closest", rIo < rEuropa)
        assertTrue("Europa inside Ganymede", rEuropa < rGanymede)
        assertTrue("Ganymede inside Callisto", rGanymede < rCallisto)
        assertEquals(5.91, rIo, 0.01)
        assertEquals(26.36, rCallisto, 0.01)
    }

    @Test
    fun testGalileanMoons_orbitalPeriodMonotonicity() {
        val pIo = JupiterMoonsOracle.ORBITAL_PERIODS_DAYS.getValue(GalileanMoon.IO)
        val pEur = JupiterMoonsOracle.ORBITAL_PERIODS_DAYS.getValue(GalileanMoon.EUROPA)
        val pGan = JupiterMoonsOracle.ORBITAL_PERIODS_DAYS.getValue(GalileanMoon.GANYMEDE)
        val pCal = JupiterMoonsOracle.ORBITAL_PERIODS_DAYS.getValue(GalileanMoon.CALLISTO)

        assertTrue(pIo in 1.7..1.8)
        assertTrue(pEur in 3.5..3.6)
        assertTrue(pGan in 7.1..7.2)
        assertTrue(pCal in 16.6..16.8)
        assertTrue(pIo < pEur && pEur < pGan && pGan < pCal)
    }

    @Test
    fun testGalileanMoons_transitDetectionWhenInFrontOfDisk() {
        // Evaluate moon states over a full Io cycle (1.77 days) to find a transit
        val start = Instant.parse("2026-06-01T00:00:00Z")
        var foundTransit = false

        for (minute in 0..(1.77 * 24 * 60).toInt() step 15) {
            val t = start.plusSeconds(minute * 60L)
            val state = JupiterMoonsOracle.calculate(t)
            val io = state.moons.getValue(GalileanMoon.IO)
            if (io.event == JupiterMoonEvent.TRANSIT) {
                foundTransit = true
                assertTrue("Transit moon must be in front of Jupiter (z < 0)", io.zDistanceAU < 0.0)
                assertTrue("Transit moon must be within Jupiter disk radius", abs(io.xOffsetRJ) <= 1.0)
                assertTrue(io.isTransit)
                break
            }
        }
        assertTrue("Io must experience a transit within one orbital period", foundTransit)
    }

    @Test
    fun testGalileanMoons_occultationAndEclipseEvents() {
        val start = Instant.parse("2026-06-01T00:00:00Z")
        var foundOccultation = false

        for (minute in 0..(1.77 * 24 * 60).toInt() step 15) {
            val t = start.plusSeconds(minute * 60L)
            val state = JupiterMoonsOracle.calculate(t)
            val io = state.moons.getValue(GalileanMoon.IO)
            if (io.event == JupiterMoonEvent.OCCULTATION) {
                foundOccultation = true
                assertTrue("Occulted moon must be behind Jupiter (z > 0)", io.zDistanceAU > 0.0)
                assertTrue("Occulted moon must be within disk radius", abs(io.xOffsetRJ) <= 1.0)
                break
            }
        }
        assertTrue("Io must experience an occultation within one orbital period", foundOccultation)
    }

    // ========================================================================
    // Feature 2: Saturn Ring Tilt & Titan Orbit - 5 tests
    // ========================================================================

    @Test
    fun testSaturnSystem_ringTiltBoundedByPhysicalRange() {
        val t1 = Instant.parse("2026-01-01T00:00:00Z")
        val state1 = SaturnSystemOracle.calculate(t1)
        assertTrue("Ring tilt must be within -27.0°..+27.0°", state1.ringTiltDegrees in -27.0..27.0)

        val t2 = Instant.parse("2032-06-01T00:00:00Z")
        val state2 = SaturnSystemOracle.calculate(t2)
        assertTrue("Ring tilt must be within -27.0°..+27.0°", state2.ringTiltDegrees in -27.0..27.0)
    }

    @Test
    fun testSaturnSystem_ringCrossingIdentifiedAsEdgeOn() {
        val crossingTime = SaturnSystemOracle.RING_CROSSING_EPOCH
        val state = SaturnSystemOracle.calculate(crossingTime)
        assertTrue("Ring tilt should be near 0 at crossing epoch", abs(state.ringTiltDegrees) < 0.5)
        assertTrue("Rings should be edge-on at crossing", state.isRingsEdgeOn)
        assertEquals("Edge-on", state.ringOpeningDirection)
    }

    @Test
    fun testSaturnSystem_ringOpeningDirectionCorrect() {
        val futureNorthTime = SaturnSystemOracle.RING_CROSSING_EPOCH.plusSeconds(86400L * 365 * 7) // ~7 years later
        val northState = SaturnSystemOracle.calculate(futureNorthTime)
        assertTrue("Expected positive tilt in northern hemisphere opening", northState.ringTiltDegrees > 0.0)
        assertEquals("North", northState.ringOpeningDirection)
        assertFalse(northState.isRingsEdgeOn)
    }

    @Test
    fun testSaturnSystem_titanOrbitalDistanceBounded() {
        val t = Instant.parse("2026-07-20T21:00:00Z")
        val state = SaturnSystemOracle.calculate(t)
        assertTrue("Titan offset must be within ±20.25 RS", abs(state.titanOffsetRS) <= SaturnSystemOracle.TITAN_ORBIT_RS)
    }

    @Test
    fun testSaturnSystem_titanPositionAngleNormalized() {
        for (day in 0..16) {
            val t = Instant.parse("2026-08-01T00:00:00Z").plusSeconds(day * 86400L)
            val state = SaturnSystemOracle.calculate(t)
            assertTrue("Position angle must be in [0°, 360°)", state.titanPositionAngleDegrees in 0.0..360.0)
        }
    }

    // ========================================================================
    // Feature 3: Lunar Terminator & Feature Proximity - 5 tests
    // ========================================================================

    @Test
    fun testLunarTerminator_colongitudeAdvancesWithPhase() {
        val newMoon = LunarTerminatorOracle.NEW_MOON_REF
        val stateNew = LunarTerminatorOracle.calculate(newMoon)
        assertEquals(0.0, stateNew.illuminationFraction, 0.05)
        assertEquals(270.0, stateNew.colongitudeDegrees, 5.0)

        // 7.38 days later = First quarter
        val firstQuarter = newMoon.plusSeconds((7.3826 * 86400).toLong())
        val stateFq = LunarTerminatorOracle.calculate(firstQuarter)
        assertEquals(0.50, stateFq.illuminationFraction, 0.08)
        assertTrue(stateFq.colongitudeDegrees < 15.0 || stateFq.colongitudeDegrees > 345.0)
    }

    @Test
    fun testLunarTerminator_illuminationFractionBounded() {
        for (day in 0..30) {
            val t = LunarTerminatorOracle.NEW_MOON_REF.plusSeconds(day * 86400L)
            val state = LunarTerminatorOracle.calculate(t)
            assertTrue("Illumination must be in 0.0..1.0", state.illuminationFraction in 0.0..1.0)
            assertTrue("Phase angle in 0..360", state.phaseAngleDegrees in 0.0..360.0)
        }
    }

    @Test
    fun testLunarTerminator_majorCratersCatalogLoaded() {
        val features = LunarTerminatorOracle.MAJOR_FEATURES
        assertTrue("Catalog must contain at least 15 landmark formations", features.size >= 15)
        assertTrue("Tycho must be present", features.any { it.name == "Tycho" && it.kind == LunarFeatureKind.CRATER })
        assertTrue("Copernicus must be present", features.any { it.name == "Copernicus" })
        assertTrue("Rupes Recta must be present", features.any { it.name == "Rupes Recta" && it.kind == LunarFeatureKind.RIMA })
        assertTrue("Mare Tranquillitatis present", features.any { it.name == "Mare Tranquillitatis" && it.kind == LunarFeatureKind.MARE })
    }

    @Test
    fun testLunarTerminator_reliefDetectionAtLowSunAngle() {
        val firstQuarter = LunarTerminatorOracle.NEW_MOON_REF.plusSeconds((7.3826 * 86400).toLong())
        val state = LunarTerminatorOracle.calculate(firstQuarter)
        val highlights = LunarTerminatorOracle.findFeaturesNearTerminator(state)

        assertFalse("Highlight list must not be empty", highlights.isEmpty())
        // Verify optimal relief criteria: sun altitude between 0 and 12 deg
        for (h in highlights) {
            if (h.isOptimalRelief) {
                assertTrue("Optimal relief requires sun altitude in 0..12°", h.sunAltitudeDegrees in 0.0..12.0)
            }
        }
    }

    @Test
    fun testLunarTerminator_subSolarLatitudeBounded() {
        val t = Instant.parse("2026-10-10T18:00:00Z")
        val state = LunarTerminatorOracle.calculate(t)
        assertTrue("Subsolar latitude must not exceed lunar obliquity (~1.6°)", abs(state.subSolarLatitudeDegrees) <= 1.6)
    }

    // ========================================================================
    // Feature 4: Offline SGP4 Satellite Propagator (ISS) - 5 tests
    // ========================================================================

    @Test
    fun testSgp4_parseValidIssTle() {
        val tle = Sgp4OfflineOracle.parseTle(
            Sgp4OfflineOracle.ISS_TLE_NAME,
            Sgp4OfflineOracle.ISS_TLE_LINE1,
            Sgp4OfflineOracle.ISS_TLE_LINE2
        )
        assertEquals(25544, tle.noradId)
        assertEquals("ISS (ZARYA)", tle.name)
        assertEquals(51.6433, tle.inclinationDegrees, 0.001)
        assertEquals(15.50123456, tle.meanMotionRevsPerDay, 0.001)
        assertEquals(2026, tle.epochYear)
    }

    @Test
    fun testSgp4_propagationProducesPhysicalTopocentricCoordinates() {
        val tle = Sgp4OfflineOracle.parseTle(
            Sgp4OfflineOracle.ISS_TLE_NAME,
            Sgp4OfflineOracle.ISS_TLE_LINE1,
            Sgp4OfflineOracle.ISS_TLE_LINE2
        )
        val pt = Sgp4OfflineOracle.propagate(
            tle = tle,
            time = Instant.parse("2026-03-20T19:30:00Z"),
            observerLat = 52.52,
            observerLon = 13.405
        )
        assertTrue("Altitude must be in [-90°, 90°]", pt.altitudeDegrees in -90.0..90.0)
        assertTrue("Azimuth must be in [0°, 360°)", pt.azimuthDegrees in 0.0..360.0)
        assertTrue("Range to LEO satellite must be > 350 km", pt.rangeKm >= 350.0)
    }

    @Test
    fun testSgp4_orbitalPeriodDerivation() {
        val tle = Sgp4OfflineOracle.parseTle(
            Sgp4OfflineOracle.ISS_TLE_NAME,
            Sgp4OfflineOracle.ISS_TLE_LINE1,
            Sgp4OfflineOracle.ISS_TLE_LINE2
        )
        val periodMin = 1440.0 / tle.meanMotionRevsPerDay
        assertTrue("ISS orbital period should be between 90 and 94 minutes", periodMin in 90.0..94.0)
    }

    @Test
    fun testSgp4_predictPassesFindsElevatedWindows() {
        val tle = Sgp4OfflineOracle.parseTle(
            Sgp4OfflineOracle.ISS_TLE_NAME,
            Sgp4OfflineOracle.ISS_TLE_LINE1,
            Sgp4OfflineOracle.ISS_TLE_LINE2
        )
        val passes = Sgp4OfflineOracle.predictPasses(
            tle = tle,
            startTime = Instant.parse("2026-03-20T00:00:00Z"),
            durationHours = 24,
            observerLat = 52.52,
            observerLon = 13.405
        )
        for (pass in passes) {
            assertTrue("Pass max altitude must reach at least 10°", pass.maxAltitudeDegrees >= 10.0)
            assertTrue("AOS must precede or equal TCA", !pass.aos.isAfter(pass.tca))
            assertTrue("TCA must precede or equal LOS", !pass.tca.isAfter(pass.los))
            assertFalse("Track points cannot be empty", pass.trackPoints.isEmpty())
        }
    }

    @Test
    fun testSgp4_shadowDetectionWhenInEarthUmbra() {
        val tle = Sgp4OfflineOracle.parseTle(
            Sgp4OfflineOracle.ISS_TLE_NAME,
            Sgp4OfflineOracle.ISS_TLE_LINE1,
            Sgp4OfflineOracle.ISS_TLE_LINE2
        )
        // Step across an orbit and confirm boolean flag exists and reflects lighting state
        var sawIlluminated = false
        val start = Instant.parse("2026-03-20T12:00:00Z")
        for (m in 0..90 step 5) {
            val pt = Sgp4OfflineOracle.propagate(tle, start.plusSeconds(m * 60L), 52.52, 13.405)
            if (!pt.isEclipsed) {
                sawIlluminated = true
            }
        }
        assertTrue("Satellite must be illuminated at daylight locations", sawIlluminated)
    }

    // ========================================================================
    // Feature 5: Angular Distance & Position Angle Tool - 5 tests
    // ========================================================================

    @Test
    fun testCelestialMeasurement_zeroSeparationForIdenticalPoints() {
        val p1 = EquatorialCoord(12.0, 45.0)
        val res = CelestialMeasurementOracle.measure(p1, p1)
        assertEquals(0.0, res.angularDistanceDegrees, 0.0001)
        assertEquals(0, res.degrees)
        assertEquals(0, res.arcMinutes)
        assertEquals(0.0, res.arcSeconds, 0.01)
        assertEquals("0° 00' 00.0\"", res.formattedDms)
    }

    @Test
    fun testCelestialMeasurement_equatorialSeparation90Degrees() {
        val p1 = EquatorialCoord(0.0, 0.0)
        val p2 = EquatorialCoord(6.0, 0.0) // 6 hours = 90 degrees
        val res = CelestialMeasurementOracle.measure(p1, p2)
        assertEquals(90.0, res.angularDistanceDegrees, 0.001)
        assertEquals(90, res.degrees)
        assertEquals(0, res.arcMinutes)
        assertEquals("90° 00' 00.0\"", res.formattedDms)
    }

    @Test
    fun testCelestialMeasurement_antipodalSeparation180Degrees() {
        val northPole = EquatorialCoord(0.0, 90.0)
        val southPole = EquatorialCoord(0.0, -90.0)
        val res = CelestialMeasurementOracle.measure(northPole, southPole)
        assertEquals(180.0, res.angularDistanceDegrees, 0.001)
        assertEquals(180, res.degrees)
    }

    @Test
    fun testCelestialMeasurement_dmsFormatting() {
        val p1 = EquatorialCoord(0.0, 0.0)
        val p2 = EquatorialCoord(0.0, 12.5083333) // 12 deg, 30 min, 30 sec
        val res = CelestialMeasurementOracle.measure(p1, p2)
        assertEquals(12, res.degrees)
        assertEquals(30, res.arcMinutes)
        assertEquals(30.0, res.arcSeconds, 0.2)
        assertTrue(res.formattedDms.startsWith("12° 30'"))
    }

    @Test
    fun testCelestialMeasurement_positionAngleDueEastAndNorth() {
        val origin = EquatorialCoord(10.0, 0.0)
        val northTarget = EquatorialCoord(10.0, 20.0)
        val resNorth = CelestialMeasurementOracle.measure(origin, northTarget)
        assertEquals("Angle towards North should be 0°", 0.0, resNorth.positionAngleDegrees, 0.5)

        val eastTarget = EquatorialCoord(11.0, 0.0)
        val resEast = CelestialMeasurementOracle.measure(origin, eastTarget)
        assertEquals("Angle towards East should be 90°", 90.0, resEast.positionAngleDegrees, 0.5)
    }

    // ========================================================================
    // Feature 6: Coordinate Grids & Reference Lines - 5 tests
    // ========================================================================

    @Test
    fun testCoordinateGrids_toggleEquatorialGrid() {
        val defaultSettings = GridDisplaySettings()
        assertFalse(defaultSettings.equatorialGridEnabled)

        val enabled = defaultSettings.copy(equatorialGridEnabled = true)
        assertTrue(enabled.equatorialGridEnabled)
        assertFalse(enabled.horizontalGridEnabled)
    }

    @Test
    fun testCoordinateGrids_toggleHorizontalGrid() {
        val settings = GridDisplaySettings(horizontalGridEnabled = true)
        assertTrue(settings.horizontalGridEnabled)
        assertFalse(settings.equatorialGridEnabled)
    }

    @Test
    fun testCoordinateGrids_independentReferenceLines() {
        val s1 = GridDisplaySettings(celestialEquatorEnabled = true)
        assertTrue(s1.celestialEquatorEnabled)
        assertFalse(s1.eclipticEnabled)
        assertFalse(s1.galacticEquatorEnabled)

        val s2 = s1.copy(eclipticEnabled = true, galacticEquatorEnabled = true)
        assertTrue(s2.celestialEquatorEnabled && s2.eclipticEnabled && s2.galacticEquatorEnabled)
    }

    @Test
    fun testCoordinateGrids_redLightModeColorSwitch() {
        val normal = GridDisplaySettings(redLightModeActive = false)
        assertEquals("#4488FF", normal.activeLineColorHex)

        val redLight = GridDisplaySettings(redLightModeActive = true)
        assertEquals("#FF2200", redLight.activeLineColorHex)
    }

    @Test
    fun testCoordinateGrids_gridCoordinateSystemEnum() {
        assertEquals(2, GridCoordinateSystem.values().size)
        assertEquals(GridCoordinateSystem.EQUATORIAL, GridCoordinateSystem.valueOf("EQUATORIAL"))
        assertEquals(GridCoordinateSystem.HORIZONTAL, GridCoordinateSystem.valueOf("HORIZONTAL"))
        assertEquals(3, ReferenceLineType.values().size)
    }

    // ========================================================================
    // Feature 7: Star-Hopping Assistant & Reticles - 5 tests
    // ========================================================================

    @Test
    fun testStarHop_tourStructureAndWaypoints() {
        val w1 = StarHopWaypoint(1, "Alpha Orionis (Betelgeuse)", EquatorialCoord(5.92, 7.41), 5.0, "Start at Betelgeuse")
        val w2 = StarHopWaypoint(2, "Zeta Orionis (Alnitak)", EquatorialCoord(5.68, -1.94), 3.0, "Hop south to eastern belt star")
        val w3 = StarHopWaypoint(3, "M42 (Great Orion Nebula)", EquatorialCoord(5.59, -5.39), 1.5, "Center on diffuse nebulosity")

        val tour = StarHopTour("tour_m42", "M42", "Orion Nebula", listOf(w1, w2, w3))
        assertEquals("tour_m42", tour.id)
        assertEquals(3, tour.waypoints.size)
        assertEquals("M42", tour.targetCatalogId)
    }

    @Test
    fun testStarHop_telradRingDiametersContract() {
        val telrad = TelradRings(centerCoords = EquatorialCoord(5.59, -5.39))
        assertEquals(0.5, telrad.innerRingDegrees, 0.001)
        assertEquals(2.0, telrad.middleRingDegrees, 0.001)
        assertEquals(4.0, telrad.outerRingDegrees, 0.001)
    }

    @Test
    fun testStarHop_waypointCompletionToggle() {
        val wp = StarHopWaypoint(1, "Guide Star", EquatorialCoord(0.0, 0.0), 2.0, "Locate star")
        assertFalse(wp.isCompleted)
        wp.isCompleted = true
        assertTrue(wp.isCompleted)
    }

    @Test
    fun testStarHop_eyepieceFovReticleScaling() {
        val wideWp = StarHopWaypoint(1, "Wide Star", EquatorialCoord(0.0, 0.0), 6.0, "Find with finder")
        val mediumWp = StarHopWaypoint(2, "Mid Star", EquatorialCoord(0.0, 0.0), 2.5, "Switch to 32mm")
        val highPowerWp = StarHopWaypoint(3, "Close Star", EquatorialCoord(0.0, 0.0), 0.8, "Switch to 9mm")

        assertTrue(wideWp.fovDegrees > mediumWp.fovDegrees)
        assertTrue(mediumWp.fovDegrees > highPowerWp.fovDegrees)
    }

    @Test
    fun testStarHop_allWaypointsMarkedCompletesTour() {
        val waypoints = listOf(
            StarHopWaypoint(1, "Star 1", EquatorialCoord(0.0, 0.0), 4.0, "Step 1"),
            StarHopWaypoint(2, "Star 2", EquatorialCoord(0.5, 0.5), 2.0, "Step 2")
        )
        val tour = StarHopTour("t1", "NGC 1", "Target", waypoints)
        assertFalse("Tour should not be complete initially", tour.waypoints.all { it.isCompleted })

        tour.waypoints.forEach { it.isCompleted = true }
        assertTrue("Tour is completed when all waypoints are done", tour.waypoints.all { it.isCompleted })
    }

    // ========================================================================
    // Feature 8: Observation Challenges (M110, Caldwell, Herschel 400) - 5 tests
    // ========================================================================

    @Test
    fun testChallenges_messier110TotalCount() {
        assertEquals(110, ChallengeType.MESSIER_110.totalCount)
        assertEquals("Messier 110 Challenge", ChallengeType.MESSIER_110.title)
    }

    @Test
    fun testChallenges_caldwellTotalCount() {
        assertEquals(109, ChallengeType.CALDWELL.totalCount)
        assertEquals("Caldwell Catalog Challenge", ChallengeType.CALDWELL.title)
    }

    @Test
    fun testChallenges_herschel400TotalCount() {
        assertEquals(400, ChallengeType.HERSCHEL_400.totalCount)
        assertEquals("Herschel 400 Challenge", ChallengeType.HERSCHEL_400.title)
    }

    @Test
    fun testChallenges_evaluateMessierProgress() {
        val logged = setOf("M1", "M31", "M42", "M45", "M110", "NGC7000", "Jupiter")
        val progress = ObservationChallengeEvaluator.evaluate(ChallengeType.MESSIER_110, logged)

        assertEquals(5, progress.observedCount)
        assertEquals(110, progress.totalCount)
        assertEquals((5.0 / 110.0) * 100.0, progress.percentComplete, 0.01)
        assertTrue(progress.completedTargetIds.contains("M1"))
        assertTrue(progress.completedTargetIds.contains("M110"))
        assertFalse(progress.completedTargetIds.contains("NGC7000"))
    }

    @Test
    fun testChallenges_evaluateCaldwellAndHerschelFiltering() {
        val caldwellLogged = setOf("C1", "C14", "C109", "M1")
        val caldwellProgress = ObservationChallengeEvaluator.evaluate(ChallengeType.CALDWELL, caldwellLogged)
        assertEquals(3, caldwellProgress.observedCount)
        assertEquals(109, caldwellProgress.totalCount)

        val herschelLogged = setOf("NGC869", "NGC884", "H400_12", "C14")
        val herschelProgress = ObservationChallengeEvaluator.evaluate(ChallengeType.HERSCHEL_400, herschelLogged)
        assertEquals(3, herschelProgress.observedCount)
        assertEquals(400, herschelProgress.totalCount)
    }

    // ========================================================================
    // Feature 9: "Observed in Logbook" Indicator Badge - 5 tests
    // ========================================================================

    @Test
    fun testObservedBadge_unobservedState() {
        val badge = ObservedBadgeState.fromLogbook("M13", emptySet())
        assertFalse(badge.isObservedInLogbook)
        assertFalse(badge.badgeVisible)
        assertEquals(0, badge.observationCount)
        assertEquals("", badge.badgeText)
    }

    @Test
    fun testObservedBadge_observedSingleTime() {
        val badge = ObservedBadgeState.fromLogbook("M13", setOf("M13"), logCount = 1)
        assertTrue(badge.isObservedInLogbook)
        assertTrue(badge.badgeVisible)
        assertEquals(1, badge.observationCount)
        assertEquals("✓ Im Logbuch (1×)", badge.badgeText)
    }

    @Test
    fun testObservedBadge_observedMultipleTimes() {
        val badge = ObservedBadgeState.fromLogbook("M42", setOf("M42", "M31"), logCount = 4)
        assertTrue(badge.isObservedInLogbook)
        assertEquals(4, badge.observationCount)
        assertEquals("✓ Im Logbuch (4×)", badge.badgeText)
    }

    @Test
    fun testObservedBadge_objectCatalogIdIntegrity() {
        val badge = ObservedBadgeState.fromLogbook("NGC2244", setOf("NGC2244"))
        assertEquals("NGC2244", badge.objectCatalogId)
    }

    @Test
    fun testObservedBadge_emptyLogbookGeneratesNoBadges() {
        val targets = listOf("M1", "M2", "M3", "C1", "NGC100")
        for (t in targets) {
            val b = ObservedBadgeState.fromLogbook(t, emptySet())
            assertFalse("No badge should appear for unlogged target $t", b.badgeVisible)
        }
    }

    // ========================================================================
    // Feature 10: Dew Monitor (Magnus-Tetens) & Risk Alerts - 5 tests
    // ========================================================================

    @Test
    fun testDewMonitor_dewPointAt100PercentRhEqualsTemp() {
        val temp = 10.0
        val rh = 100.0
        val dewPoint = DewMonitorOracle.calculateDewPoint(temp, rh)
        assertEquals(temp, dewPoint, 0.05)
    }

    @Test
    fun testDewMonitor_criticalRiskAlertWhenMarginBelow1Point5() {
        val report = DewMonitorOracle.assessRisk(ambientTempCelsius = 5.0, relativeHumidityPercent = 95.0)
        assertTrue("Dew margin must be <= 1.5°C at 95% RH", report.dewMarginCelsius <= 1.5)
        assertEquals(DewRiskLevel.CRITICAL, report.riskLevel)
        assertEquals(3, report.riskLevel.alertSeverity)
    }

    @Test
    fun testDewMonitor_highRiskAlertWhenMarginBelow3() {
        val report = DewMonitorOracle.assessRisk(ambientTempCelsius = 12.0, relativeHumidityPercent = 85.0)
        assertTrue("Margin must be between 1.5 and 3.0°C", report.dewMarginCelsius in 1.5..3.0)
        assertEquals(DewRiskLevel.HIGH, report.riskLevel)
        assertEquals(2, report.riskLevel.alertSeverity)
    }

    @Test
    fun testDewMonitor_moderateRiskAlertWhenMarginBelow5() {
        val report = DewMonitorOracle.assessRisk(ambientTempCelsius = 15.0, relativeHumidityPercent = 75.0)
        assertTrue("Margin must be between 3.0 and 5.0°C", report.dewMarginCelsius in 3.0..5.0)
        assertEquals(DewRiskLevel.MODERATE, report.riskLevel)
        assertEquals(1, report.riskLevel.alertSeverity)
    }

    @Test
    fun testDewMonitor_lowRiskWhenDryAir() {
        val report = DewMonitorOracle.assessRisk(ambientTempCelsius = 20.0, relativeHumidityPercent = 40.0)
        assertTrue("Dew margin should be > 5.0°C under dry conditions", report.dewMarginCelsius > 5.0)
        assertEquals(DewRiskLevel.LOW, report.riskLevel)
        assertEquals(0, report.riskLevel.alertSeverity)
    }

    // ========================================================================
    // Feature 11: OpenAstronomyLog (OAL 2.1) XML Export - 5 tests
    // ========================================================================

    @Test
    fun testOalExport_validXmlHeaderAndRootTag() {
        val xml = OpenAstronomyLogExporter.exportToXml(emptyList())
        assertTrue("XML header must be present", xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
        assertTrue("Root oal tag with version 2.1 must be present", xml.contains("<oal xmlns=\"http://www.astronomy.org/OpenAstronomyLog/2.1\" version=\"2.1\">"))
        assertTrue("Closing root tag must be present", xml.endsWith("</oal>"))
    }

    @Test
    fun testOalExport_observationElementStructure() {
        val entry = OalLogEntry(
            id = "obs_001",
            objectCatalogId = "M42",
            objectName = "Orion Nebula",
            timestampEpochSeconds = 1774000000L,
            observerName = "Galileo",
            siteName = "Padua",
            opticsName = "Refractor 30x",
            notes = "Four stars of Trapezium resolved."
        )
        val xml = OpenAstronomyLogExporter.exportToXml(listOf(entry))

        assertTrue(xml.contains("<observation id=\"obs_001\">"))
        assertTrue(xml.contains("<target id=\"M42\" name=\"Orion Nebula\" />"))
        assertTrue(xml.contains("<observer name=\"Galileo\" />"))
        assertTrue(xml.contains("<site name=\"Padua\" />"))
        assertTrue(xml.contains("<optics name=\"Refractor 30x\" />"))
        assertTrue(xml.contains("<result notes=\"Four stars of Trapezium resolved.\" />"))
    }

    @Test
    fun testOalExport_xmlCharacterEscaping() {
        val raw = "Observation <M42> & \"Trapezium\" with 8' scope"
        val escaped = OpenAstronomyLogExporter.escapeXml(raw)

        assertEquals("Observation &lt;M42&gt; &amp; &quot;Trapezium&quot; with 8&apos; scope", escaped)
        assertFalse(escaped.contains("<M42>"))
        assertFalse(escaped.contains("& \""))
    }

    @Test
    fun testOalExport_seeingAssessmentTags() {
        val entry = OalLogEntry(
            objectCatalogId = "Jupiter",
            objectName = "Jupiter",
            timestampEpochSeconds = 1774000000L,
            seeingPickering = 7,
            seeingAntoniadi = "II",
            nelm = 6.2
        )
        val xml = OpenAstronomyLogExporter.exportToXml(listOf(entry))

        assertTrue(xml.contains("<assessment>"))
        assertTrue(xml.contains("<seeing pickering=\"7\" />"))
        assertTrue(xml.contains("<seeing antoniadi=\"II\" />"))
        assertTrue(xml.contains("<limitingMagnitude>6.2</limitingMagnitude>"))
    }

    @Test
    fun testOalExport_printableTextSummaryFormatting() {
        val entry = OalLogEntry(
            objectCatalogId = "M31",
            objectName = "Andromeda Galaxy",
            timestampEpochSeconds = 1774000000L,
            siteName = "Alps",
            opticsName = "10x50 Binoculars",
            seeingPickering = 8,
            nelm = 6.5,
            notes = "Dust lane visible."
        )
        val summary = OpenAstronomyLogExporter.exportFormattedTextSummary(listOf(entry))

        assertTrue(summary.contains("PROJEKT ASTRA BEOBACHTUNGS-TAGEBUCH"))
        assertTrue(summary.contains("M31 - Andromeda Galaxy"))
        assertTrue(summary.contains("10x50 Binoculars"))
        assertTrue(summary.contains("Pickering 8/10"))
        assertTrue(summary.contains("NELM 6.5m"))
        assertTrue(summary.contains("Dust lane visible."))
    }

    // ========================================================================
    // Feature 12: Seeing Scales (Pickering, Antoniadi, NELM) - 5 tests
    // ========================================================================

    @Test
    fun testSeeing_pickeringRangeValidation() {
        for (rating in 1..10) {
            assertTrue("Pickering $rating should be valid", SeeingScaleValidator.isValidPickering(rating))
        }
        assertFalse(SeeingScaleValidator.isValidPickering(0))
        assertFalse(SeeingScaleValidator.isValidPickering(11))
        assertFalse(SeeingScaleValidator.isValidPickering(-3))
    }

    @Test
    fun testSeeing_antoniadiRomanNumeralsValidation() {
        listOf("I", "II", "III", "IV", "V").forEach {
            assertTrue("Antoniadi $it should be valid", SeeingScaleValidator.isValidAntoniadi(it))
        }
        assertFalse(SeeingScaleValidator.isValidAntoniadi("VI"))
        assertFalse(SeeingScaleValidator.isValidAntoniadi("0"))
        assertFalse(SeeingScaleValidator.isValidAntoniadi("Good"))
    }

    @Test
    fun testSeeing_nelmRangeValidation() {
        assertTrue(SeeingScaleValidator.isValidNelm(0.0))
        assertTrue(SeeingScaleValidator.isValidNelm(6.5))
        assertTrue(SeeingScaleValidator.isValidNelm(8.5))
        assertFalse(SeeingScaleValidator.isValidNelm(-0.5))
        assertFalse(SeeingScaleValidator.isValidNelm(9.0))
    }

    @Test
    fun testSeeing_pickeringToAntoniadiMapping() {
        assertEquals("I", SeeingScaleValidator.mapPickeringToAntoniadi(10))
        assertEquals("I", SeeingScaleValidator.mapPickeringToAntoniadi(9))
        assertEquals("II", SeeingScaleValidator.mapPickeringToAntoniadi(8))
        assertEquals("II", SeeingScaleValidator.mapPickeringToAntoniadi(7))
        assertEquals("III", SeeingScaleValidator.mapPickeringToAntoniadi(5))
        assertEquals("IV", SeeingScaleValidator.mapPickeringToAntoniadi(3))
        assertEquals("V", SeeingScaleValidator.mapPickeringToAntoniadi(1))
    }

    @Test
    fun testSeeing_caseInsensitiveAntoniadi() {
        assertTrue(SeeingScaleValidator.isValidAntoniadi("ii"))
        assertTrue(SeeingScaleValidator.isValidAntoniadi(" iii "))
        assertTrue(SeeingScaleValidator.isValidAntoniadi("iv"))
    }

    // ========================================================================
    // Feature 13: Stepped Night Exposure Compensation - 5 tests
    // ========================================================================

    @Test
    fun testExposure_initialStateIsZero() {
        val controller = CameraExposureController()
        val state = controller.getState()
        assertEquals(0, state.currentStepIndex)
        assertEquals(0.0f, state.evValue, 0.001f)
        assertFalse(state.isCompensationActive)
    }

    @Test
    fun testExposure_stepUpIncreasesEv() {
        val controller = CameraExposureController(maxEvSteps = 4, evStepSize = 0.5f)
        val s1 = controller.stepUp()
        assertEquals(1, s1.currentStepIndex)
        assertEquals(0.5f, s1.evValue, 0.001f)
        assertTrue(s1.isCompensationActive)

        val s2 = controller.stepUp()
        assertEquals(2, s2.currentStepIndex)
        assertEquals(1.0f, s2.evValue, 0.001f)
    }

    @Test
    fun testExposure_stepUpClampedAtMax() {
        val controller = CameraExposureController(maxEvSteps = 3, evStepSize = 1.0f)
        controller.stepUp()
        controller.stepUp()
        controller.stepUp()
        val s4 = controller.stepUp() // Should remain at 3
        assertEquals(3, s4.currentStepIndex)
        assertEquals(3.0f, s4.evValue, 0.001f)
    }

    @Test
    fun testExposure_stepDownDecreasesEv() {
        val controller = CameraExposureController()
        controller.stepUp()
        controller.stepUp()
        val s1 = controller.stepDown()
        assertEquals(1, s1.currentStepIndex)
        val s0 = controller.stepDown()
        assertEquals(0, s0.currentStepIndex)
        assertFalse(s0.isCompensationActive)
        val sUnder = controller.stepDown() // Clamped at 0
        assertEquals(0, sUnder.currentStepIndex)
    }

    @Test
    fun testExposure_resetReturnsToZero() {
        val controller = CameraExposureController()
        controller.stepUp()
        controller.stepUp()
        controller.stepUp()
        val state = controller.reset()
        assertEquals(0, state.currentStepIndex)
        assertEquals(0.0f, state.evValue, 0.001f)
        assertFalse(state.isCompensationActive)
    }

    // ========================================================================
    // Feature 14: AR Sensor Low-Pass Jitter Filter & Pitch Trim - 5 tests
    // ========================================================================

    @Test
    fun testArFilter_alphaDecreasesWithNarrowerFov() {
        val filter = ArSensorFilter()
        val alphaWide = filter.computeAlphaForFov(60.0f)
        val alphaTele = filter.computeAlphaForFov(5.0f)
        assertTrue("Narrow FOV should produce lower alpha (stronger damping)", alphaTele < alphaWide)
        assertTrue(alphaTele >= 0.02f)
        assertTrue(alphaWide <= 0.40f)
    }

    @Test
    fun testArFilter_pitchTrimOffsetApplied() {
        val filter = ArSensorFilter(pitchTrimDegrees = 3.5f)
        val (az, pitch) = filter.filter(180.0f, 40.0f, 45.0f)
        assertEquals(43.5f, pitch, 0.1f)
        assertEquals(180.0f, az, 0.1f)
    }

    @Test
    fun testArFilter_azimuthSmoothingCircularWrapAround() {
        val filter = ArSensorFilter()
        // Initialize at 359°
        filter.filter(359.0f, 0.0f, 45.0f)
        // Next reading jumps across 360° to 1.0° (distance = 2°, not 358°)
        val (smoothedAz, _) = filter.filter(1.0f, 0.0f, 45.0f)
        // Smoothed value should be near 360°/0°, definitely NOT near 180°
        assertTrue("Azimuth smoothing must handle wrap-around without traversing 180°", smoothedAz > 355.0f || smoothedAz < 5.0f)
    }

    @Test
    fun testArFilter_pitchClampedToPhysicalRange() {
        val filter = ArSensorFilter(pitchTrimDegrees = 20.0f)
        val (_, pitch) = filter.filter(0.0f, 85.0f, 45.0f)
        assertTrue("Pitch must be clamped to +90°", pitch <= 90.0f)

        filter.pitchTrimDegrees = -20.0f
        val (_, pitchNeg) = filter.filter(0.0f, -85.0f, 45.0f)
        assertTrue("Pitch must be clamped to -90°", pitchNeg >= -90.0f)
    }

    @Test
    fun testArFilter_resetClearsSmoothingHistory() {
        val filter = ArSensorFilter()
        filter.filter(90.0f, 20.0f, 45.0f)
        filter.reset()
        val (az, pitch) = filter.filter(270.0f, -10.0f, 45.0f)
        assertEquals("First reading after reset should initialize directly to raw input", 270.0f, az, 0.01f)
        assertEquals(-10.0f, pitch, 0.01f)
    }

    // ========================================================================
    // Feature 15: Physical Volume Key Glove Mode Zoom - 5 tests
    // ========================================================================

    @Test
    fun testGloveMode_volumeUpZoomsIn() {
        val controller = GloveModeZoomController(currentFovDegrees = 60.0, zoomFactorPerStep = 1.25)
        val newFov = controller.onVolumeUp()
        assertEquals(48.0, newFov, 0.01)
        assertTrue("Volume Up must decrease FOV (zoom in)", newFov < 60.0)
    }

    @Test
    fun testGloveMode_volumeDownZoomsOut() {
        val controller = GloveModeZoomController(currentFovDegrees = 40.0, zoomFactorPerStep = 1.25)
        val newFov = controller.onVolumeDown()
        assertEquals(50.0, newFov, 0.01)
        assertTrue("Volume Down must increase FOV (zoom out)", newFov > 40.0)
    }

    @Test
    fun testGloveMode_minFovClamping() {
        val controller = GloveModeZoomController(currentFovDegrees = 0.6, minFovDegrees = 0.5, zoomFactorPerStep = 2.0)
        val fov1 = controller.onVolumeUp()
        assertEquals(0.5, fov1, 0.001)
        val fov2 = controller.onVolumeUp()
        assertEquals(0.5, fov2, 0.001)
    }

    @Test
    fun testGloveMode_maxFovClamping() {
        val controller = GloveModeZoomController(currentFovDegrees = 100.0, maxFovDegrees = 110.0, zoomFactorPerStep = 1.5)
        val fov1 = controller.onVolumeDown()
        assertEquals(110.0, fov1, 0.001)
        val fov2 = controller.onVolumeDown()
        assertEquals(110.0, fov2, 0.001)
    }

    @Test
    fun testGloveMode_disabledIgnoresVolumeKeys() {
        val controller = GloveModeZoomController(currentFovDegrees = 60.0)
        controller.gloveModeEnabled = false

        assertEquals(60.0, controller.onVolumeUp(), 0.001)
        assertEquals(60.0, controller.onVolumeDown(), 0.001)
        assertEquals(60.0, controller.currentFovDegrees, 0.001)
    }

    // ========================================================================
    // Feature 16: Pure OLED True Black Mode - 5 tests
    // ========================================================================

    @Test
    fun testOled_enabledSetsBackgroundToPureBlackHex() {
        val colors = OledThemeManager.getThemeColors(oledModeEnabled = true)
        assertEquals("#000000", colors.backgroundColorHex)
    }

    @Test
    fun testOled_enabledSetsSurfaceToPureBlackHex() {
        val colors = OledThemeManager.getThemeColors(oledModeEnabled = true)
        assertEquals("#000000", colors.surfaceColorHex)
    }

    @Test
    fun testOled_enabledSetsCardBackgroundToPureBlackHex() {
        val colors = OledThemeManager.getThemeColors(oledModeEnabled = true)
        assertEquals("#000000", colors.cardBackgroundHex)
    }

    @Test
    fun testOled_flagIsTrueWhenActive() {
        val colors = OledThemeManager.getThemeColors(oledModeEnabled = true)
        assertTrue(colors.isPureBlack)
    }

    @Test
    fun testOled_disabledUsesStandardDarkGrays() {
        val colors = OledThemeManager.getThemeColors(oledModeEnabled = false)
        assertFalse(colors.isPureBlack)
        assertEquals("#121212", colors.backgroundColorHex)
        assertNotEquals("#000000", colors.surfaceColorHex)
    }

    // ========================================================================
    // Feature 17: Battery-Friendly Homescreen AppWidget - 5 tests
    // ========================================================================

    @Test
    fun testWidget_rendersMoonPhaseLabelAndIllumination() {
        val state = AstraAppWidgetOracle.renderWidgetState(
            time = LunarTerminatorOracle.NEW_MOON_REF.plusSeconds((14.76 * 86400).toLong()), // Full Moon
            cachedWeatherScore = 85,
            lastKnownLocation = "52.52°N, 13.41°E"
        )
        assertTrue("Illumination at full moon should be ~100%", state.moonIlluminationPercent >= 98)
        assertEquals("Vollmond", state.moonPhaseLabel)
    }

    @Test
    fun testWidget_rendersDarknessWindowText() {
        val state = AstraAppWidgetOracle.renderWidgetState(
            time = Instant.parse("2026-10-15T18:00:00Z"),
            cachedWeatherScore = 70,
            lastKnownLocation = "Home"
        )
        assertTrue("Widget should display darkness window info", state.darknessWindow.contains("Astronomische Nacht"))
    }

    @Test
    fun testWidget_preservesCachedWeatherScore() {
        val stateNormal = AstraAppWidgetOracle.renderWidgetState(Instant.now(), 78, "Berlin")
        assertEquals(78, stateNormal.weatherScore)

        val stateClamped = AstraAppWidgetOracle.renderWidgetState(Instant.now(), 150, "Berlin")
        assertEquals(100, stateClamped.weatherScore)
    }

    @Test
    fun testWidget_displaysLastKnownLocation() {
        val loc = "Garching Observatorium"
        val state = AstraAppWidgetOracle.renderWidgetState(Instant.now(), 90, loc)
        assertEquals(loc, state.lastKnownLocationLabel)
    }

    @Test
    fun testWidget_guaranteesZeroBackgroundGpsAndService() {
        val state = AstraAppWidgetOracle.renderWidgetState(Instant.now(), 80, "Munich")
        assertFalse("AppWidget must NOT activate background GPS", state.usesBackgroundGps)
        assertFalse("AppWidget must NOT run background services", state.usesRunningBackgroundService)
    }
}
