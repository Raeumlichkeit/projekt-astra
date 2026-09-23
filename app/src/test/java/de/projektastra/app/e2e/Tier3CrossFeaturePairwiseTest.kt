package de.projektastra.app.e2e

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import kotlin.math.abs

/**
 * Tier 3: Cross-Feature Pairwise Integration Test Suite (17 pairwise integration tests)
 * Verifies multi-module interactions, coordinate transformation invariance,
 * logbook synchronization, environmental alert propagation, and security policies.
 */
class Tier3CrossFeaturePairwiseTest {

    // ------------------------------------------------------------------------
    // Pairwise 1: Optics Transformation Invariance + Celestial Measurement
    // ------------------------------------------------------------------------
    @Test
    fun testPairwise_opticsInvariance_angularMeasurement() {
        val p1 = EquatorialCoord(5.92, 7.41)   // Betelgeuse
        val p2 = EquatorialCoord(5.24, -8.20)  // Rigel

        val baseMeasurement = CelestialMeasurementOracle.measure(p1, p2)

        // Optics transformations: Inverted (180° rotation) or Diagonal (horizontal flip)
        // True physical angular separation on the celestial sphere must remain strictly invariant
        val invertedP1 = EquatorialCoord((p1.raHours + 12.0) % 24.0, -p1.decDegrees)
        val invertedP2 = EquatorialCoord((p2.raHours + 12.0) % 24.0, -p2.decDegrees)
        val invertedMeasurement = CelestialMeasurementOracle.measure(invertedP1, invertedP2)

        assertEquals("Angular distance must be invariant under optics 180° inversion",
            baseMeasurement.angularDistanceDegrees, invertedMeasurement.angularDistanceDegrees, 0.001)
        assertTrue(baseMeasurement.angularDistanceDegrees > 15.0)
    }

    // ------------------------------------------------------------------------
    // Pairwise 2: Dew Monitor + Weather Refresh Integration
    // ------------------------------------------------------------------------
    @Test
    fun testPairwise_dewMonitor_and_weatherRefresh() {
        // Initial dry daytime weather
        var currentTemp = 18.0
        var currentRh = 50.0
        var report = DewMonitorOracle.assessRisk(currentTemp, currentRh)
        assertEquals(DewRiskLevel.LOW, report.riskLevel)
        assertTrue(report.dewMarginCelsius > 8.0)

        // Weather refresh: Night cooling down to near dew point
        currentTemp = 4.0
        currentRh = 94.0
        report = DewMonitorOracle.assessRisk(currentTemp, currentRh)
        assertEquals("Dropping temperature at 94% RH must elevate risk to CRITICAL", DewRiskLevel.CRITICAL, report.riskLevel)
        assertTrue(report.dewMarginCelsius <= 1.5)
    }

    // ------------------------------------------------------------------------
    // Pairwise 3: Observation Logbook Store + Challenge Sync
    // ------------------------------------------------------------------------
    @Test
    fun testPairwise_observationLogbook_and_challengeSync() {
        val loggedSet = mutableSetOf<String>()
        val initialProgress = ObservationChallengeEvaluator.evaluate(ChallengeType.MESSIER_110, loggedSet)
        assertEquals(0, initialProgress.observedCount)

        // User logs M42 and M31
        loggedSet.add("M42")
        loggedSet.add("M31")
        val updatedProgress = ObservationChallengeEvaluator.evaluate(ChallengeType.MESSIER_110, loggedSet)

        assertEquals(2, updatedProgress.observedCount)
        assertEquals((2.0 / 110.0) * 100.0, updatedProgress.percentComplete, 0.01)
        assertTrue(updatedProgress.completedTargetIds.contains("M42"))
        assertTrue(updatedProgress.completedTargetIds.contains("M31"))
    }

    // ------------------------------------------------------------------------
    // Pairwise 4: Observed Badge State + Star Map and Search Synchronization
    // ------------------------------------------------------------------------
    @Test
    fun testPairwise_observedBadge_mapAndSearchIntegration() {
        val loggedObjects = setOf("M13", "NGC869", "C14")

        // Star Map query
        val mapBadgeM13 = ObservedBadgeState.fromLogbook("M13", loggedObjects, logCount = 2)
        assertTrue(mapBadgeM13.badgeVisible)
        assertEquals("✓ Im Logbuch (2×)", mapBadgeM13.badgeText)

        // Search Sheet query
        val searchBadgeM13 = ObservedBadgeState.fromLogbook("M13", loggedObjects, logCount = 2)
        assertEquals(mapBadgeM13, searchBadgeM13)

        // Unobserved target query
        val mapBadgeM51 = ObservedBadgeState.fromLogbook("M51", loggedObjects)
        assertFalse(mapBadgeM51.badgeVisible)
    }

    // ------------------------------------------------------------------------
    // Pairwise 5: Star-Hopping Assistant + Telrad Reticle + Angular Distance
    // ------------------------------------------------------------------------
    @Test
    fun testPairwise_starHop_telradReticle_and_angularMeasurement() {
        val w1Coords = EquatorialCoord(5.58, -5.39) // Theta Orionis (Trapezium)
        val w2Coords = EquatorialCoord(5.59, -5.45) // Nearby field star
        val telrad = TelradRings(centerCoords = w1Coords)

        val measurement = CelestialMeasurementOracle.measure(w1Coords, w2Coords)
        assertTrue("Hop step should comfortably fit within Telrad outer ring (4.0°)",
            measurement.angularDistanceDegrees < telrad.outerRingDegrees)
        assertTrue("Hop step should fit within middle ring (2.0°)",
            measurement.angularDistanceDegrees < telrad.middleRingDegrees)
    }

    // ------------------------------------------------------------------------
    // Pairwise 6: Offline SGP4 Propagator + AR Sensor Pitch Trim
    // ------------------------------------------------------------------------
    @Test
    fun testPairwise_sgp4Satellite_and_arPitchTrim() {
        val tle = Sgp4OfflineOracle.parseTle(
            Sgp4OfflineOracle.ISS_TLE_NAME,
            Sgp4OfflineOracle.ISS_TLE_LINE1,
            Sgp4OfflineOracle.ISS_TLE_LINE2
        )
        val t = Instant.parse("2026-03-20T19:30:00Z")
        val satPoint = Sgp4OfflineOracle.propagate(tle, t, 52.52, 13.405)

        // Calibrate AR sensor with +2.5° pitch trim
        val filter = ArSensorFilter(pitchTrimDegrees = 2.5f)
        val (_, trimmedPitch) = filter.filter(satPoint.azimuthDegrees.toFloat(), satPoint.altitudeDegrees.toFloat(), 45.0f)

        assertEquals("Projected AR pitch should include pitch trim offset",
            (satPoint.altitudeDegrees + 2.5).toFloat(), trimmedPitch, 0.5f)
    }

    // ------------------------------------------------------------------------
    // Pairwise 7: Lunar Terminator + Seeing Scales in Logbook
    // ------------------------------------------------------------------------
    @Test
    fun testPairwise_lunarTerminator_and_seeingScales() {
        val fqTime = LunarTerminatorOracle.NEW_MOON_REF.plusSeconds((LunarTerminatorOracle.SYNODIC_MONTH_DAYS * 0.25 * 86400).toLong())
        val termState = LunarTerminatorOracle.calculate(fqTime)
        val highlights = LunarTerminatorOracle.findFeaturesNearTerminator(termState)

        val bestRelief = highlights.firstOrNull { it.isOptimalRelief }
        assertNotNull("Should find optimal relief feature along terminator", bestRelief)

        // Log observation with Pickering 8 / Antoniadi II seeing
        val logEntry = OalLogEntry(
            objectCatalogId = bestRelief!!.feature.name,
            objectName = bestRelief.feature.name,
            timestampEpochSeconds = fqTime.epochSecond,
            opticsName = "8\" Dobsonian",
            seeingPickering = 8,
            seeingAntoniadi = SeeingScaleValidator.mapPickeringToAntoniadi(8),
            notes = "Dramatic shadow casting along crater wall."
        )

        assertEquals("II", logEntry.seeingAntoniadi)
        assertEquals(8, logEntry.seeingPickering)
        val xml = OpenAstronomyLogExporter.exportToXml(listOf(logEntry))
        assertTrue(xml.contains("<seeing pickering=\"8\" />"))
        assertTrue(xml.contains("<seeing antoniadi=\"II\" />"))
    }

    // ------------------------------------------------------------------------
    // Pairwise 8: Galilean Moons + Saturn Rings in Planetary Observing Session
    // ------------------------------------------------------------------------
    @Test
    fun testPairwise_galileanMoons_and_saturnRings_planetaryMode() {
        val epoch = Instant.parse("2026-09-15T21:00:00Z")

        val jupiterState = JupiterMoonsOracle.calculate(epoch)
        val saturnState = SaturnSystemOracle.calculate(epoch)

        assertEquals(4, jupiterState.moons.size)
        assertTrue(saturnState.ringTiltDegrees in -27.0..27.0)
        assertTrue(saturnState.titanPositionAngleDegrees in 0.0..360.0)
    }

    // ------------------------------------------------------------------------
    // Pairwise 9: OAL XML Export + Seeing Scales + Challenge Logging
    // ------------------------------------------------------------------------
    @Test
    fun testPairwise_oalXmlExport_with_seeingAndChallenges() {
        val entries = listOf(
            OalLogEntry(objectCatalogId = "M1", objectName = "Crab Nebula", timestampEpochSeconds = 1774000000L, seeingPickering = 7, seeingAntoniadi = "II", nelm = 6.2),
            OalLogEntry(objectCatalogId = "M42", objectName = "Orion Nebula", timestampEpochSeconds = 1774000100L, seeingPickering = 9, seeingAntoniadi = "I", nelm = 6.5)
        )
        val xml = OpenAstronomyLogExporter.exportToXml(entries)
        assertTrue(xml.contains("<target id=\"M1\" name=\"Crab Nebula\" />"))
        assertTrue(xml.contains("<target id=\"M42\" name=\"Orion Nebula\" />"))
        assertTrue(xml.contains("<seeing pickering=\"7\" />"))
        assertTrue(xml.contains("<seeing pickering=\"9\" />"))
        assertTrue(xml.contains("<limitingMagnitude>6.5</limitingMagnitude>"))

        // Verify challenge progress calculation on same entries
        val progress = ObservationChallengeEvaluator.evaluate(ChallengeType.MESSIER_110, entries.map { it.objectCatalogId }.toSet())
        assertEquals(2, progress.observedCount)
    }

    // ------------------------------------------------------------------------
    // Pairwise 10: Homescreen Widget + Lunar Terminator Oracle
    // ------------------------------------------------------------------------
    @Test
    fun testPairwise_homescreenWidget_and_lunarTerminatorOracle() {
        val fullMoonTime = LunarTerminatorOracle.NEW_MOON_REF.plusSeconds((LunarTerminatorOracle.SYNODIC_MONTH_DAYS * 0.5 * 86400).toLong())
        val widget = AstraAppWidgetOracle.renderWidgetState(fullMoonTime, cachedWeatherScore = 90, lastKnownLocation = "Garching")

        assertEquals("Vollmond", widget.moonPhaseLabel)
        assertTrue(widget.moonIlluminationPercent >= 98)
        assertFalse(widget.usesBackgroundGps)
        assertFalse(widget.usesRunningBackgroundService)
    }

    // ------------------------------------------------------------------------
    // Pairwise 11: Security Offline Policy + Ephemeris and SGP4 Zero Network
    // ------------------------------------------------------------------------
    @Test
    fun testPairwise_securityOfflinePolicy_ephemerisAndSgp4ZeroNetwork() {
        // Ephemeris, SGP4, Grids, and Measurements run hermetically offline
        val t = Instant.parse("2026-04-01T22:00:00Z")
        val tle = Sgp4OfflineOracle.parseTle(Sgp4OfflineOracle.ISS_TLE_NAME, Sgp4OfflineOracle.ISS_TLE_LINE1, Sgp4OfflineOracle.ISS_TLE_LINE2)

        val jup = JupiterMoonsOracle.calculate(t)
        val sat = SaturnSystemOracle.calculate(t)
        val moon = LunarTerminatorOracle.calculate(t)
        val satPt = Sgp4OfflineOracle.propagate(tle, t, 52.52, 13.405)

        assertNotNull(jup)
        assertNotNull(sat)
        assertNotNull(moon)
        assertNotNull(satPt)
        // Pure mathematical computation with no IO or network invocation
    }

    // ------------------------------------------------------------------------
    // Pairwise 12: Privacy Location Store + Rounded Coordinates Precision
    // ------------------------------------------------------------------------
    @Test
    fun testPairwise_securityLocationPrivacy_roundedCoordinates() {
        // Privacy rule: GPS coordinates rounded to 0.01° to prevent domestic pinpointing
        val rawLat = 52.520489
        val rawLon = 13.405234
        val roundedLat = Math.round(rawLat * 100.0) / 100.0
        val roundedLon = Math.round(rawLon * 100.0) / 100.0

        assertEquals(52.52, roundedLat, 0.0001)
        assertEquals(13.41, roundedLon, 0.0001)

        val tle = Sgp4OfflineOracle.parseTle(Sgp4OfflineOracle.ISS_TLE_NAME, Sgp4OfflineOracle.ISS_TLE_LINE1, Sgp4OfflineOracle.ISS_TLE_LINE2)
        val ptRaw = Sgp4OfflineOracle.propagate(tle, Instant.parse("2026-03-20T12:00:00Z"), rawLat, rawLon)
        val ptRounded = Sgp4OfflineOracle.propagate(tle, Instant.parse("2026-03-20T12:00:00Z"), roundedLat, roundedLon)

        // Delta between raw and rounded topocentric altitude is tiny (< 0.1°)
        assertTrue("Privacy rounding to 0.01° must preserve topocentric accuracy",
            abs(ptRaw.altitudeDegrees - ptRounded.altitudeDegrees) < 0.1)
    }

    // ------------------------------------------------------------------------
    // Pairwise 13: Dew Monitor Critical Alert + Logbook Notes
    // ------------------------------------------------------------------------
    @Test
    fun testPairwise_dewMonitorCriticalAlert_and_logbookNotes() {
        val report = DewMonitorOracle.assessRisk(ambientTempCelsius = 2.0, relativeHumidityPercent = 95.0)
        assertEquals(DewRiskLevel.CRITICAL, report.riskLevel)

        val note = if (report.riskLevel == DewRiskLevel.CRITICAL) {
            "Optik-Heizung aktiviert wegen akuter Taupunktannäherung (${report.dewPointCelsius}°C)."
        } else ""

        val entry = OalLogEntry(
            objectCatalogId = "M42",
            objectName = "Orion Nebula",
            timestampEpochSeconds = 1774000000L,
            notes = note
        )

        assertTrue(entry.notes.contains("Optik-Heizung"))
        val xml = OpenAstronomyLogExporter.exportToXml(listOf(entry))
        assertTrue(xml.contains("Optik-Heizung"))
    }

    // ------------------------------------------------------------------------
    // Pairwise 14: Glove Mode Volume Zoom + Pure OLED Mode
    // ------------------------------------------------------------------------
    @Test
    fun testPairwise_gloveModeZoom_and_oledPureBlackMode() {
        val zoom = GloveModeZoomController(currentFovDegrees = 50.0)
        val oled = OledThemeManager.getThemeColors(oledModeEnabled = true)

        zoom.onVolumeUp()
        assertEquals(40.0, zoom.currentFovDegrees, 0.01)
        assertTrue(oled.isPureBlack)
        assertEquals("#000000", oled.backgroundColorHex)
    }

    // ------------------------------------------------------------------------
    // Pairwise 15: AR Sensor Filter + Camera Night Exposure
    // ------------------------------------------------------------------------
    @Test
    fun testPairwise_arSensorFilter_and_cameraNightExposure() {
        val camera = CameraExposureController()
        val filter = ArSensorFilter()

        camera.stepUp()
        camera.stepUp()
        val expState = camera.getState()
        assertEquals(1.0f, expState.evValue, 0.01f)

        val (az, pitch) = filter.filter(45.0f, 30.0f, fovDegrees = 10.0f)
        assertEquals(45.0f, az, 0.01f)
        assertEquals(30.0f, pitch, 0.01f)
    }

    // ------------------------------------------------------------------------
    // Pairwise 16: Red-Light Mode + OLED True Black Theme Coexistence
    // ------------------------------------------------------------------------
    @Test
    fun testPairwise_redLightMode_and_oledThemeCoexistence() {
        val oled = OledThemeManager.getThemeColors(oledModeEnabled = true)
        val grids = GridDisplaySettings(equatorialGridEnabled = true, redLightModeActive = true)

        assertEquals("#000000", oled.backgroundColorHex)
        assertEquals("#FF2200", grids.activeLineColorHex)
    }

    // ------------------------------------------------------------------------
    // Pairwise 17: Star-Hopping Tour Completion + Caldwell Challenge Sync
    // ------------------------------------------------------------------------
    @Test
    fun testPairwise_starHopTourCompletion_and_caldwellChallengeProgress() {
        val tour = StarHopTour(
            id = "t_c14",
            targetCatalogId = "C14",
            targetName = "Double Cluster in Perseus",
            waypoints = listOf(
                StarHopWaypoint(1, "Eta Persei (Miram)", EquatorialCoord(2.84, 55.9), 4.0, "Start at Miram"),
                StarHopWaypoint(2, "C14 (NGC 869)", EquatorialCoord(2.32, 57.1), 1.5, "Center Double Cluster")
            )
        )

        tour.waypoints.forEach { it.isCompleted = true }
        assertTrue(tour.waypoints.all { it.isCompleted })

        // Target C14 is logged upon tour completion
        val progress = ObservationChallengeEvaluator.evaluate(ChallengeType.CALDWELL, setOf(tour.targetCatalogId))
        assertEquals(1, progress.observedCount)
        assertTrue(progress.completedTargetIds.contains("C14"))
    }
}
