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
 * Tier 4: Real-World Observation Scenarios Test Suite (5 realistic multi-step observation workflows)
 * Verifies end-to-end integration across entire user observation sessions under realistic conditions:
 * 1. Winter Sub-Zero Dobsonian Observation
 * 2. High-Power Planetary & Lunar Night
 * 3. Satellite Pass Tracking in AR Mode
 * 4. Deep-Sky Challenge Marathon & Logging
 * 5. Homescreen Quick Assessment & Planning
 */
class Tier4RealWorldObservationScenariosTest {

    // ========================================================================
    // Scenario 1: Winter Sub-Zero Dobsonian Observation
    // Features: Dew Monitor, OLED Mode, Red-Light, Optics 180° Inversion,
    //           Glove Mode Volume Zoom, Star-Hopping with Telrad, Logbook.
    // ========================================================================
    @Test
    fun testScenario1_subZeroDobsonianWinterObservation() {
        // Step 1: Environmental check on cold winter night (-6°C, 93% RH)
        val ambientTemp = -6.0
        val relativeHumidity = 93.0
        val dewReport = DewMonitorOracle.assessRisk(ambientTemp, relativeHumidity)
        assertTrue("Sub-zero dew point must be calculated correctly", dewReport.dewPointCelsius < ambientTemp)
        assertTrue("Dew margin must be <= 1.5°C", dewReport.dewMarginCelsius <= 1.5)
        assertEquals(DewRiskLevel.CRITICAL, dewReport.riskLevel)

        // Step 2: Dark adaptation setup: Pure OLED True Black + Red-Light overlay
        val oledTheme = OledThemeManager.getThemeColors(oledModeEnabled = true)
        val gridSettings = GridDisplaySettings(
            equatorialGridEnabled = true,
            redLightModeActive = true
        )
        assertEquals("#000000", oledTheme.backgroundColorHex)
        assertEquals("#000000", oledTheme.surfaceColorHex)
        assertEquals("#FF2200", gridSettings.activeLineColorHex)

        // Step 3: Telescope Optics Configuration (8" Dobsonian with 180° inverted view)
        val pAlphaOri = EquatorialCoord(5.92, 7.41)
        val pRigel = EquatorialCoord(5.24, -8.20)
        val directDist = CelestialMeasurementOracle.measure(pAlphaOri, pRigel).angularDistanceDegrees

        // Inverted field maintains exact angular measurement
        val invAlpha = EquatorialCoord((pAlphaOri.raHours + 12.0) % 24.0, -pAlphaOri.decDegrees)
        val invRigel = EquatorialCoord((pRigel.raHours + 12.0) % 24.0, -pRigel.decDegrees)
        val invertedDist = CelestialMeasurementOracle.measure(invAlpha, invRigel).angularDistanceDegrees
        assertEquals(directDist, invertedDist, 0.001)

        // Step 4: Glove Mode interaction with physical volume keys
        val zoomController = GloveModeZoomController(currentFovDegrees = 60.0, zoomFactorPerStep = 1.25)
        assertTrue(zoomController.gloveModeEnabled)
        zoomController.onVolumeUp() // Zoom in: 60 / 1.25 = 48.0°
        assertEquals(48.0, zoomController.currentFovDegrees, 0.01)
        zoomController.onVolumeUp() // Zoom in: 48 / 1.25 = 38.4°
        assertEquals(38.4, zoomController.currentFovDegrees, 0.01)

        // Step 5: Star-Hopping to M42 with Telrad Reticle
        val waypoints = listOf(
            StarHopWaypoint(1, "Betelgeuse", pAlphaOri, 6.0, "Start at Betelgeuse"),
            StarHopWaypoint(2, "Alnitak", EquatorialCoord(5.68, -1.94), 3.0, "Hop south to eastern belt star"),
            StarHopWaypoint(3, "M42 (Orion Nebula)", EquatorialCoord(5.59, -5.39), 1.5, "Center on Trapezium")
        )
        val tour = StarHopTour("tour_m42_winter", "M42", "Orion Nebula", waypoints)
        val telrad = TelradRings(centerCoords = waypoints[2].coords)
        assertEquals(0.5, telrad.innerRingDegrees, 0.001)
        assertEquals(4.0, telrad.outerRingDegrees, 0.001)

        // User checks off each hop waypoint in the cold
        tour.waypoints.forEach { it.isCompleted = true }
        assertTrue("All star-hopping waypoints must be marked completed", tour.waypoints.all { it.isCompleted })

        // Step 6: Observation Log Entry & Badge Verification
        val logEntry = OalLogEntry(
            objectCatalogId = "M42",
            objectName = "Orion Nebula",
            timestampEpochSeconds = 1774000000L,
            opticsName = "8\" Dobsonian f/6",
            notes = "Trapezium resolved at 150x in sub-zero cold. Dew heater band active.",
            seeingPickering = 6,
            nelm = 6.0
        )
        val badge = ObservedBadgeState.fromLogbook("M42", setOf(logEntry.objectCatalogId), logCount = 1)
        assertTrue(badge.badgeVisible)
        assertEquals("✓ Im Logbuch (1×)", badge.badgeText)

        val xml = OpenAstronomyLogExporter.exportToXml(listOf(logEntry))
        assertTrue(xml.contains("<target id=\"M42\" name=\"Orion Nebula\" />"))
        assertTrue(xml.contains("<optics name=\"8&quot; Dobsonian f/6\" />"))
    }

    // ========================================================================
    // Scenario 2: High-Power Planetary & Lunar Night
    // Features: Galilean Moons Transit/Shadow, Saturn Ring Tilt & Titan Orbit,
    //           Terminator Low-Sun Craters, Pickering / Antoniadi Seeing, OAL XML.
    // ========================================================================
    @Test
    fun testScenario2_highPowerPlanetaryAndLunarNight() {
        val sessionEpoch = Instant.parse("2026-10-15T20:30:00Z")

        // Step 1: Jupiter System State & Event Tracking
        val jupiter = JupiterMoonsOracle.calculate(sessionEpoch)
        assertEquals(4, jupiter.moons.size)
        assertEquals(45.0, jupiter.jupiterAngularDiameterArcsec, 0.1)
        for ((moon, pos) in jupiter.moons) {
            val maxR = JupiterMoonsOracle.ORBITAL_RADII_RJ.getValue(moon)
            assertTrue("Moon $moon x offset within orbital bound", abs(pos.xOffsetRJ) <= maxR + 0.01)
        }

        // Step 2: Saturn System State (Ring Tilt & Titan)
        val saturn = SaturnSystemOracle.calculate(sessionEpoch)
        assertTrue("Saturn ring opening angle in physical range", saturn.ringTiltDegrees in -27.0..27.0)
        assertTrue("Titan offset in RS", abs(saturn.titanOffsetRS) <= SaturnSystemOracle.TITAN_ORBIT_RS)
        assertTrue("Titan position angle in [0°, 360°)", saturn.titanPositionAngleDegrees in 0.0..360.0)

        // Step 3: Lunar Terminator Relief Identification
        val terminatorState = LunarTerminatorOracle.calculate(sessionEpoch)
        val highlights = LunarTerminatorOracle.findFeaturesNearTerminator(terminatorState)
        assertFalse("Landmark lunar features must be evaluated", highlights.isEmpty())

        // Find features under dramatic low-sun relief (0° to 12° altitude)
        val optimalReliefFeatures = highlights.filter { it.isOptimalRelief }
        // Verify optimal relief criteria for all identified formations
        for (f in optimalReliefFeatures) {
            assertTrue("Sun altitude must be between 0° and 12°", f.sunAltitudeDegrees in 0.0..12.0)
        }

        // Step 4: High-Power Seeing Scale Assessment
        val pickeringRating = 8
        assertTrue(SeeingScaleValidator.isValidPickering(pickeringRating))
        val antoniadiGrade = SeeingScaleValidator.mapPickeringToAntoniadi(pickeringRating)
        assertEquals("II", antoniadiGrade)

        // Step 5: Multi-Object Logbook Recording & OAL Export
        val entries = listOf(
            OalLogEntry(
                objectCatalogId = "Jupiter",
                objectName = "Jupiter & Galilean Moons",
                timestampEpochSeconds = sessionEpoch.epochSecond,
                opticsName = "5\" Apo Refractor",
                seeingPickering = pickeringRating,
                seeingAntoniadi = antoniadiGrade,
                notes = "Great Red Spot transit observed; Io shadow sharp on cloud deck."
            ),
            OalLogEntry(
                objectCatalogId = "Saturn",
                objectName = "Saturn",
                timestampEpochSeconds = sessionEpoch.epochSecond + 1800,
                opticsName = "5\" Apo Refractor",
                seeingPickering = pickeringRating,
                seeingAntoniadi = antoniadiGrade,
                notes = "Cassini division steady; Titan bright at PA ${saturn.titanPositionAngleDegrees.toInt()}°."
            )
        )

        val xml = OpenAstronomyLogExporter.exportToXml(entries)
        assertTrue(xml.contains("<target id=\"Jupiter\""))
        assertTrue(xml.contains("<target id=\"Saturn\""))
        assertTrue(xml.contains("<seeing pickering=\"8\" />"))
        assertTrue(xml.contains("<seeing antoniadi=\"II\" />"))
    }

    // ========================================================================
    // Scenario 3: Satellite Pass Tracking in AR Mode
    // Features: SGP4 ISS Prediction, Topocentric Coordinates, AR Sensor Filter,
    //           Pitch Trim Offset, Camera Stepped Night Exposure.
    // ========================================================================
    @Test
    fun testScenario3_satellitePassTrackingInArMode() {
        // Step 1: Ingest bundled offline TLE fixture
        val tle = Sgp4OfflineOracle.parseTle(
            Sgp4OfflineOracle.ISS_TLE_NAME,
            Sgp4OfflineOracle.ISS_TLE_LINE1,
            Sgp4OfflineOracle.ISS_TLE_LINE2
        )
        val observerLat = 52.52
        val observerLon = 13.41

        // Step 2: Predict upcoming passes over 24 hours
        val startTime = Instant.parse("2026-03-20T18:00:00Z")
        val passes = Sgp4OfflineOracle.predictPasses(tle, startTime, 24, observerLat, observerLon)
        assertFalse("Must find predicted ISS passes within 24 hours", passes.isEmpty())

        val selectedPass = passes.first()
        assertTrue("Pass culmination must reach at least 10° elevation", selectedPass.maxAltitudeDegrees >= 10.0)
        assertFalse("Pass trajectory track points must be populated", selectedPass.trackPoints.isEmpty())

        // Step 3: AR Camera Exposure Optimization (+2 EV stepping for faint horizon)
        val camera = CameraExposureController(maxEvSteps = 4, evStepSize = 0.5f)
        camera.stepUp()
        camera.stepUp()
        val expState = camera.getState()
        assertEquals(2, expState.currentStepIndex)
        assertEquals(1.0f, expState.evValue, 0.001f)
        assertTrue(expState.isCompensationActive)

        // Step 4: AR Sensor Low-Pass Jitter Filtering & Pitch Trim
        // Observer telephoto FOV is 12° -> requires strong tremor damping
        val arFilter = ArSensorFilter(pitchTrimDegrees = 2.0f)
        val alpha = arFilter.computeAlphaForFov(12.0f)
        assertTrue("Alpha at 12° FOV should be strongly damped (< 0.10)", alpha < 0.10f)

        // Feed predicted satellite trajectory points through the AR filter
        for (pt in selectedPass.trackPoints) {
            val (smoothedAz, smoothedPitch) = arFilter.filter(
                rawAzimuthDegrees = pt.azimuthDegrees.toFloat(),
                rawPitchDegrees = pt.altitudeDegrees.toFloat(),
                fovDegrees = 12.0f
            )
            assertTrue("Smoothed Azimuth in [0°, 360°)", smoothedAz in 0.0f..360.0f)
            assertTrue("Smoothed Pitch in [-90°, 90°]", smoothedPitch in -90.0f..90.0f)
        }
    }

    // ========================================================================
    // Scenario 4: Deep-Sky Challenge Marathon & Logging
    // Features: Messier 110, Caldwell, Herschel 400 Challenges,
    //           Observed Badges on Map/Search, OAL XML Export, NELM / Antoniadi.
    // ========================================================================
    @Test
    fun testScenario4_deepSkyChallengeMarathon() {
        val loggedTargets = mutableSetOf<String>()
        val logEntries = mutableListOf<OalLogEntry>()

        // Step 1: Initial Challenge State
        var mProgress = ObservationChallengeEvaluator.evaluate(ChallengeType.MESSIER_110, loggedTargets)
        var cProgress = ObservationChallengeEvaluator.evaluate(ChallengeType.CALDWELL, loggedTargets)
        var hProgress = ObservationChallengeEvaluator.evaluate(ChallengeType.HERSCHEL_400, loggedTargets)
        assertEquals(0, mProgress.observedCount)
        assertEquals(0, cProgress.observedCount)
        assertEquals(0, hProgress.observedCount)

        // Step 2: Observing Messier Marathon Targets
        val messierTargets = listOf("M1", "M31", "M33", "M42", "M45", "M51", "M81", "M82", "M101", "M110")
        messierTargets.forEachIndexed { i, id ->
            loggedTargets.add(id)
            logEntries.add(
                OalLogEntry(
                    objectCatalogId = id,
                    objectName = "Messier Target $id",
                    timestampEpochSeconds = 1774000000L + i * 600,
                    seeingPickering = 7,
                    seeingAntoniadi = "II",
                    nelm = 6.4,
                    notes = "Observed during autumn marathon."
                )
            )
        }

        // Step 3: Observing Caldwell & Herschel Targets
        val caldwellTargets = listOf("C14", "C33")
        caldwellTargets.forEach { id ->
            loggedTargets.add(id)
            logEntries.add(
                OalLogEntry(
                    objectCatalogId = id,
                    objectName = "Caldwell Target $id",
                    timestampEpochSeconds = 1774010000L,
                    seeingAntoniadi = "II",
                    nelm = 6.4
                )
            )
        }

        val herschelTargets = listOf("NGC869", "NGC884")
        herschelTargets.forEach { id ->
            loggedTargets.add(id)
            logEntries.add(
                OalLogEntry(
                    objectCatalogId = id,
                    objectName = "Herschel Target $id",
                    timestampEpochSeconds = 1774015000L,
                    nelm = 6.4
                )
            )
        }

        // Step 4: Verify Synchronized Challenge Progress
        mProgress = ObservationChallengeEvaluator.evaluate(ChallengeType.MESSIER_110, loggedTargets)
        cProgress = ObservationChallengeEvaluator.evaluate(ChallengeType.CALDWELL, loggedTargets)
        hProgress = ObservationChallengeEvaluator.evaluate(ChallengeType.HERSCHEL_400, loggedTargets)

        assertEquals(10, mProgress.observedCount)
        assertEquals((10.0 / 110.0) * 100.0, mProgress.percentComplete, 0.01)

        assertEquals(2, cProgress.observedCount)
        assertEquals((2.0 / 109.0) * 100.0, cProgress.percentComplete, 0.01)

        assertEquals(2, hProgress.observedCount)
        assertEquals((2.0 / 400.0) * 100.0, hProgress.percentComplete, 0.01)

        // Step 5: Verify Observed Badges across Star Map and Search
        val badgeM42 = ObservedBadgeState.fromLogbook("M42", loggedTargets)
        val badgeC14 = ObservedBadgeState.fromLogbook("C14", loggedTargets)
        val badgeUnloggedM13 = ObservedBadgeState.fromLogbook("M13", loggedTargets)

        assertTrue(badgeM42.badgeVisible)
        assertTrue(badgeC14.badgeVisible)
        assertFalse(badgeUnloggedM13.badgeVisible)

        // Step 6: Batch Export to OAL XML and Printable Summary
        val xml = OpenAstronomyLogExporter.exportToXml(logEntries)
        assertTrue(xml.contains("<oal xmlns=\"http://www.astronomy.org/OpenAstronomyLog/2.1\" version=\"2.1\">"))
        assertTrue(xml.contains("<target id=\"M1\""))
        assertTrue(xml.contains("<target id=\"C14\""))
        assertTrue(xml.contains("<limitingMagnitude>6.4</limitingMagnitude>"))

        val textSummary = OpenAstronomyLogExporter.exportFormattedTextSummary(logEntries)
        assertTrue(textSummary.contains("Gesamtanzahl Beobachtungen: 14"))
        assertTrue(textSummary.contains("NELM 6.4m"))
    }

    // ========================================================================
    // Scenario 5: Homescreen Quick Assessment & Planning
    // Features: Android Glance / AppWidget, Moon Phase, Darkness Window,
    //           Weather Score, Offline Location Cache (Zero Background GPS).
    // ========================================================================
    @Test
    fun testScenario5_homescreenQuickAssessmentAndPlanning() {
        val planningTime = Instant.parse("2026-10-20T17:00:00Z")
        val cachedWeatherScore = 88
        val cachedLocation = "52.52°N, 13.41°E (Berlin)"

        // Step 1: Render Homescreen AppWidget state
        val widgetState = AstraAppWidgetOracle.renderWidgetState(
            time = planningTime,
            cachedWeatherScore = cachedWeatherScore,
            lastKnownLocation = cachedLocation
        )

        // Step 2: Validate Moon Phase and Illumination
        assertNotNull(widgetState.moonPhaseLabel)
        assertFalse(widgetState.moonPhaseLabel.isEmpty())
        assertTrue("Moon illumination percentage in 0..100%", widgetState.moonIlluminationPercent in 0..100)

        // Step 3: Validate Astronomical Darkness Window
        assertTrue("Darkness window must indicate astronomical night interval",
            widgetState.darknessWindow.contains("Astronomische Nacht"))

        // Step 4: Validate Cached Weather Score Display
        assertEquals(88, widgetState.weatherScore)

        // Step 5: Strict Battery & Privacy Guarantee Enforcement
        assertFalse("Widget MUST NEVER trigger background GPS", widgetState.usesBackgroundGps)
        assertFalse("Widget MUST NEVER keep background services running", widgetState.usesRunningBackgroundService)
        assertEquals(cachedLocation, widgetState.lastKnownLocationLabel)
    }
}
