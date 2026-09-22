package de.projektastra.app.ephemeris

import de.projektastra.app.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class LunarAndSatelliteChallengerTest {

    private val issTle = TleParser.parseTle(
        "1 25544U 98067A   26263.51829341  .00016717  00000-0  10270-3 0  9997",
        "2 25544  51.6416 247.4627 0006703 130.5360 325.0288 15.72125391563537",
        "ISS (ZARYA)"
    )

    // Polar satellite (NOAA 19: inclination 98.7°, period ~101 min, 14.19 rev/day)
    private val polarTle = TleParser.parseTle(
        "1 33591U 09005A   26263.50000000  .00000100  00000-0  50000-4 0  9997",
        "2 33591  98.7000  45.0000 0014000  90.0000 270.0000 14.19000000000016",
        "NOAA 19 (POLAR)"
    )

    private val propagator = Sgp4Propagator()
    private val predictor = SatellitePassPredictor(propagator)

    // =========================================================================
    // SECTION 1: LUNAR TERMINATOR & CARDINAL PHASES ACROSS ERAS
    // =========================================================================

    @Test
    fun `challengeColongitudeAtFourCardinalPhasesAcrossEras`() {
        // Test 1: Year 2026 Cardinal Phases
        // -------------------------------------------------------------
        // New Moon: ~2026-09-11T14:00:00Z
        val nm2026 = LunarTerminatorCalculator.calculateTerminator(Instant.parse("2026-09-11T14:00:00Z"))
        assertTrue("2026 New Moon phase fraction must be near 0.0: was ${nm2026.phaseFraction}",
            nm2026.phaseFraction <= 0.03)
        assertTrue("2026 New Moon colongitude must be near 270°: was ${nm2026.colongitude}",
            nm2026.colongitude in 260.0..280.0)

        // First Quarter: ~2026-09-18T22:00:00Z
        val fq2026 = LunarTerminatorCalculator.calculateTerminator(Instant.parse("2026-09-18T22:00:00Z"))
        assertTrue("2026 First Quarter phase fraction must be ~0.50: was ${fq2026.phaseFraction}",
            fq2026.phaseFraction in 0.45..0.55)
        val fqColong = fq2026.colongitude
        assertTrue("2026 First Quarter colongitude must be near 0° or 360°: was $fqColong",
            fqColong in 350.0..360.0 || fqColong in 0.0..10.0)
        assertTrue("2026 First Quarter subsolar lon must be near +90°: was ${fq2026.subSolarLon}",
            fq2026.subSolarLon in 80.0..95.0)

        // Full Moon: ~2026-09-26T16:00:00Z
        val fm2026 = LunarTerminatorCalculator.calculateTerminator(Instant.parse("2026-09-26T16:00:00Z"))
        assertTrue("2026 Full Moon phase fraction must be near 1.0: was ${fm2026.phaseFraction}",
            fm2026.phaseFraction >= 0.97)
        assertTrue("2026 Full Moon colongitude must be near 90°: was ${fm2026.colongitude}",
            fm2026.colongitude in 80.0..100.0)
        assertTrue("2026 Full Moon subsolar lon must be near 0°: was ${fm2026.subSolarLon}",
            abs(fm2026.subSolarLon) < 10.0)

        // Last Quarter: ~2026-10-03T14:00:00Z
        val lq2026 = LunarTerminatorCalculator.calculateTerminator(Instant.parse("2026-10-03T14:00:00Z"))
        assertTrue("2026 Last Quarter phase fraction must be ~0.50: was ${lq2026.phaseFraction}",
            lq2026.phaseFraction in 0.45..0.55)
        assertTrue("2026 Last Quarter colongitude must be near 180°: was ${lq2026.colongitude}",
            lq2026.colongitude in 170.0..190.0)
        assertTrue("2026 Last Quarter subsolar lon must be near -90°: was ${lq2026.subSolarLon}",
            lq2026.subSolarLon in -95.0..-80.0)

        // Test 2: Historical Epoch J2000.0 (January 2000)
        // -------------------------------------------------------------
        // Full Moon: 2000-01-21T04:40:00Z
        val fm2000 = LunarTerminatorCalculator.calculateTerminator(Instant.parse("2000-01-21T04:40:00Z"))
        assertTrue("2000 Full Moon phase fraction >= 0.97: was ${fm2000.phaseFraction}",
            fm2000.phaseFraction >= 0.97)
        assertTrue("2000 Full Moon colongitude near 90°: was ${fm2000.colongitude}",
            fm2000.colongitude in 80.0..100.0)

        // New Moon: 2000-01-06T18:14:00Z
        val nm2000 = LunarTerminatorCalculator.calculateTerminator(Instant.parse("2000-01-06T18:14:00Z"))
        assertTrue("2000 New Moon phase fraction <= 0.03: was ${nm2000.phaseFraction}",
            nm2000.phaseFraction <= 0.03)
        assertTrue("2000 New Moon colongitude near 270°: was ${nm2000.colongitude}",
            nm2000.colongitude in 260.0..280.0)

        // Test 3: Future Epoch (June 2050)
        // -------------------------------------------------------------
        // Full Moon: 2050-06-05T03:00:00Z
        val fm2050 = LunarTerminatorCalculator.calculateTerminator(Instant.parse("2050-06-05T03:00:00Z"))
        assertTrue("2050 Full Moon phase fraction >= 0.95: was ${fm2050.phaseFraction}",
            fm2050.phaseFraction >= 0.95)
        assertTrue("2050 Full Moon colongitude near 90°: was ${fm2050.colongitude}",
            fm2050.colongitude in 75.0..105.0)

        // Test 4: Historical Epoch (1950)
        // -------------------------------------------------------------
        val fm1950 = LunarTerminatorCalculator.calculateTerminator(Instant.parse("1950-01-04T08:00:00Z"))
        assertTrue("1950 Full Moon phase fraction >= 0.95: was ${fm1950.phaseFraction}",
            fm1950.phaseFraction >= 0.95)
        assertTrue("1950 Full Moon colongitude near 90°: was ${fm1950.colongitude}",
            fm1950.colongitude in 75.0..105.0)
    }

    @Test
    fun `challengeColongitudeContinuityAndMonotonicityOverSynodicMonth`() {
        val start = Instant.parse("2026-09-01T00:00:00Z")
        val totalHours = 720 // 30 days
        var prevColong = LunarTerminatorCalculator.calculateTerminator(start).colongitude
        var unwrappedColong = prevColong

        for (h in 1..totalHours) {
            val t = start.plusSeconds(h * 3600L)
            val state = LunarTerminatorCalculator.calculateTerminator(t)

            // Assert finite numbers
            assertFalse("Colongitude cannot be NaN at h=$h", state.colongitude.isNaN())
            assertFalse("Subsolar Lon cannot be NaN at h=$h", state.subSolarLon.isNaN())
            assertFalse("Subsolar Lat cannot be NaN at h=$h", state.subSolarLat.isNaN())
            assertFalse("SubEarth Lon cannot be NaN at h=$h", state.subEarthLon.isNaN())
            assertFalse("SubEarth Lat cannot be NaN at h=$h", state.subEarthLat.isNaN())

            // Strict range bounds
            assertTrue("Colongitude must be in [0, 360): ${state.colongitude}",
                state.colongitude in 0.0..360.0)
            assertTrue("Subsolar Lon in [-180, 180]: ${state.subSolarLon}",
                state.subSolarLon in -180.0..180.0)

            // Moon axial tilt to ecliptic is ~1.54°: subsolar lat must never exceed physical obliquity
            assertTrue("Subsolar Lat (|lat| <= 1.65°) exceeded at h=$h: was ${state.subSolarLat}",
                abs(state.subSolarLat) <= 1.65)

            // Libration boundaries: optical libration in lon <= 9.0°, lat <= 7.5°
            assertTrue("SubEarth Lon (|lon| <= 9.0°) exceeded at h=$h: was ${state.subEarthLon}",
                abs(state.subEarthLon) <= 9.0)
            assertTrue("SubEarth Lat (|lat| <= 7.5°) exceeded at h=$h: was ${state.subEarthLat}",
                abs(state.subEarthLat) <= 7.5)

            // Moon distance strictly within perigee/apogee bounds
            assertTrue("Moon distance in [355000..407000 km]: was ${state.moonDistanceKm}",
                state.moonDistanceKm in 355000.0..407000.0)

            // Monotonic advancement of Colongitude:
            // Moon rotates at ~0.508°/hour. Over 1 hour, dC0 must be positive and in [0.45°..0.60°]
            var dColong = state.colongitude - prevColong
            if (dColong < -300.0) dColong += 360.0 // wrap around 360° -> 0°
            assertTrue("Colongitude hourly rate must be positive and physically realistic: was $dColong deg/h at h=$h",
                dColong in 0.40..0.65)

            unwrappedColong += dColong
            prevColong = state.colongitude
        }

        // Over 29.53 days (708.73 hours), colongitude advances by exactly 360°
        val totalDelta30Days = unwrappedColong - LunarTerminatorCalculator.calculateTerminator(start).colongitude
        // In 30 days (30 / 29.53059 * 360° ≈ 365.7°)
        assertTrue("Total colongitude advance over 30 days should be ~365.7°: was $totalDelta30Days",
            totalDelta30Days in 360.0..372.0)
    }

    @Test
    fun `challengeTerminatorElevationZeroConstraintAtAllLatitudes`() {
        val state = LunarTerminatorCalculator.calculateTerminator(Instant.parse("2026-09-20T12:00:00Z"))

        // Morning and evening terminator points across latitudes -85° to +85°
        for (lat in listOf(-85.0, -60.0, -30.0, -10.0, 0.0, 10.0, 30.0, 60.0, 85.0)) {
            val mTerm = state.morningTerminatorLon(lat)
            val hM = state.sunElevationDegrees(mTerm, lat)
            assertEquals("Solar elevation on morning terminator at lat $lat must be 0°", 0.0, hM, 0.1)

            val eTerm = state.eveningTerminatorLon(lat)
            val hE = state.sunElevationDegrees(eTerm, lat)
            assertEquals("Solar elevation on evening terminator at lat $lat must be 0°", 0.0, hE, 0.1)
        }
    }

    // =========================================================================
    // SECTION 2: LOW-SUN GRAZING RELIEF & LIBRATION FILTERING
    // =========================================================================

    @Test
    fun `challengeLowSunGrazingReliefThresholdsAndScores`() {
        val state = LunarTerminatorCalculator.calculateTerminator(Instant.parse("2026-09-19T06:00:00Z"))

        // Create synthetic test features at various distances from the morning terminator at equator
        val mTermLon = state.morningTerminatorLon(0.0)

        // 1. Feature exactly on terminator: h_sun = 0.0°
        val featAtTerm = LunarFeature("f_0", "At Terminator", LunarFeatureType.CRATER, 0.0, mTermLon, 20.0, "Test")
        val hl0 = LunarTerminatorCalculator.featuresNearTerminator(state, listOf(featAtTerm)).first()
        assertEquals("h_sun at terminator should be 0.0°", 0.0, hl0.sunElevationDegrees, 0.1)
        assertTrue("At h_sun=0.0°, must be inOptimalRelief", hl0.inOptimalRelief)
        assertEquals("Relief score at h_sun=0° is 1.0 - 4.5/7.5 = 0.4", 0.4, hl0.reliefScore, 0.05)

        // 2. Feature at optimal peak: h_sun = 4.5° (offset lon = mTermLon + 4.5°)
        val featAtPeak = LunarFeature("f_peak", "Peak Relief", LunarFeatureType.CRATER, 0.0, mTermLon + 4.5, 20.0, "Test")
        val hlPeak = LunarTerminatorCalculator.featuresNearTerminator(state, listOf(featAtPeak)).first()
        assertEquals("h_sun should be ~4.5°", 4.5, hlPeak.sunElevationDegrees, 0.1)
        assertTrue("Must be inOptimalRelief", hlPeak.inOptimalRelief)
        assertEquals("Relief score at h_sun=4.5° must be 1.0", 1.0, hlPeak.reliefScore, 0.02)

        // 3. Feature at upper threshold: h_sun = 12.0°
        val featAt12 = LunarFeature("f_12", "At 12 deg", LunarFeatureType.CRATER, 0.0, mTermLon + 12.0, 20.0, "Test")
        val hl12 = LunarTerminatorCalculator.featuresNearTerminator(state, listOf(featAt12)).first()
        assertEquals("h_sun should be ~12.0°", 12.0, hl12.sunElevationDegrees, 0.1)
        assertTrue("At h_sun=12.0°, must be inOptimalRelief", hl12.inOptimalRelief)
        assertEquals("Relief score at h_sun=12.0° must be 0.0", 0.0, hl12.reliefScore, 0.05)

        // 4. Feature above threshold: h_sun = 12.5°
        val featAbove12 = LunarFeature("f_above", "Above 12 deg", LunarFeatureType.CRATER, 0.0, mTermLon + 12.5, 20.0, "Test")
        val hlAbove = LunarTerminatorCalculator.featuresNearTerminator(state, listOf(featAbove12)).first()
        assertFalse("Above 12°, inOptimalRelief must be false", hlAbove.inOptimalRelief)
        assertEquals("Relief score above 12° must be 0.0", 0.0, hlAbove.reliefScore, 1e-6)

        // 5. Feature in night side: h_sun = -1.0°
        val featNight = LunarFeature("f_night", "Night side", LunarFeatureType.CRATER, 0.0, mTermLon - 1.0, 20.0, "Test")
        val hlNight = LunarTerminatorCalculator.featuresNearTerminator(state, listOf(featNight)).first()
        assertFalse("Night side inOptimalRelief must be false", hlNight.inOptimalRelief)
        assertEquals("Relief score on night side must be 0.0", 0.0, hlNight.reliefScore, 1e-6)
    }

    @Test
    fun `challengeFarSideFeaturesStrictExclusionFromEarthRelief`() {
        // Far side features:
        // Tsiolkovskiy (-20.4° N, +129.1° E)
        // Mare Moscoviense (+27.3° N, +147.9° E)
        // Hertzsprung (+1.4° N, -128.7° E)
        val farSideFeatures = listOf(
            LunarFeature("tsiolkovskiy", "Tsiolkovskiy", LunarFeatureType.CRATER, -20.4, 129.1, 180.0, "Far side"),
            LunarFeature("mare_moscoviense", "Mare Moscoviense", LunarFeatureType.MARE, 27.3, 147.9, 275.0, "Far side"),
            LunarFeature("hertzsprung", "Hertzsprung", LunarFeatureType.CRATER, 1.4, -128.7, 536.0, "Far side")
        )

        // Sample across a full month: whenever terminator passes over these far-side features,
        // h_sun will be in 0..12°. BUT earth visibility must strictly be false, so inOptimalRelief must NEVER be true!
        val start = Instant.parse("2026-09-01T00:00:00Z")
        for (day in 0..29) {
            val t = start.plusSeconds(day * 86400L)
            val state = LunarTerminatorCalculator.calculateTerminator(t)
            val highlights = LunarTerminatorCalculator.featuresNearTerminator(state, farSideFeatures)

            highlights.forEach { hl ->
                assertFalse("Far-side feature ${hl.feature.name} must never be visible from Earth: diskX=${hl.diskX}, diskY=${hl.diskY}",
                    hl.isVisibleFromEarth)
                assertFalse("Far-side feature ${hl.feature.name} must NEVER be inOptimalRelief: h_sun=${hl.sunElevationDegrees}",
                    hl.inOptimalRelief)
            }
        }
    }

    @Test
    fun `challengeLimbLibrationVisibilityFiltering`() {
        // Mare Orientale centered at selenographic Lon = -95.0°, Lat = -19.0°
        // Just beyond the mean lunar limb (-90°).
        // It becomes visible from Earth ONLY when lunar libration in longitude l_E is negative (tilted West toward Earth).
        val mareOrientale = LunarFeature("mare_orientale", "Mare Orientale", LunarFeatureType.MARE, -19.0, -95.0, 327.0, "Limb mare")

        // Search for a time with favorable negative longitude libration (l_E < -5.0°) vs positive libration (l_E > +5.0°)
        val start = Instant.parse("2026-01-01T00:00:00Z")
        var foundVisible = false
        var foundHidden = false

        for (day in 0..180) {
            val t = start.plusSeconds(day * 86400L)
            val state = LunarTerminatorCalculator.calculateTerminator(t)
            val vis = state.earthVisibility(mareOrientale.selenographicLon, mareOrientale.selenographicLat)

            if (state.subEarthLon < -6.0 && vis.isVisibleFromEarth) {
                foundVisible = true
                val rSq = vis.diskX * vis.diskX + vis.diskY * vis.diskY
                assertTrue("Projected position on limb must be on disk: $rSq", rSq <= 1.0001)
                assertTrue("Foreshortening cos(psi) must be positive: ${vis.foreshortening}", vis.foreshortening > 0.0)
            } else if (state.subEarthLon > 5.0 && !vis.isVisibleFromEarth) {
                foundHidden = true
                assertTrue("Foreshortening must be non-positive: ${vis.foreshortening}", vis.foreshortening <= 0.0)
            }
        }

        assertTrue("Mare Orientale must be visible under favorable negative libration", foundVisible)
        assertTrue("Mare Orientale must be hidden on far side under positive libration", foundHidden)
    }

    @Test
    fun `challengeDiskOrthographicProjectionGeometricInvariance`() {
        val state = LunarTerminatorCalculator.calculateTerminator(Instant.parse("2026-09-20T00:00:00Z"))

        // Disk center test: at sub-Earth point, diskX=0, diskY=0, foreshortening=1.0
        val centerVis = state.earthVisibility(state.subEarthLon, state.subEarthLat)
        assertEquals(0.0, centerVis.diskX, 1e-5)
        assertEquals(0.0, centerVis.diskY, 1e-5)
        assertEquals(1.0, centerVis.foreshortening, 1e-5)
        assertTrue(centerVis.isVisibleFromEarth)

        // All catalog features must satisfy diskX^2 + diskY^2 <= 1.0001
        LunarFeatureCatalog.allFeatures.forEach { feat ->
            val vis = state.earthVisibility(feat.selenographicLon, feat.selenographicLat)
            if (vis.isVisibleFromEarth) {
                val rSq = vis.diskX * vis.diskX + vis.diskY * vis.diskY
                assertTrue("Feature ${feat.name} projected radius squared ($rSq) must be <= 1.0", rSq <= 1.0001)
            }
        }
    }

    // =========================================================================
    // SECTION 3: SGP4 PASS PREDICTOR UNDER EXTREME LOCATIONS & BOUNDARY TLEs
    // =========================================================================

    @Test
    fun `challengeSgp4PassPredictionAtNorthAndSouthPoles`() {
        val northPole = GeoPoint(90.0, 0.0, 0.0)
        val southPole = GeoPoint(-90.0, 0.0, 2800.0)
        val startTime = issTle.epochInstant

        // 1. ISS (inc = 51.64°) from North Pole:
        // Angular distance to ISS max sub-satellite point is 90° - 51.64° = 38.36°.
        // Horizon angular radius for 400 km orbit is ~19.8°.
        // ISS is permanently ~18° below the horizon from the North Pole!
        // Pass predictor MUST return zero passes with minElevation = 10.0°
        val northPasses = predictor.predictPasses(issTle, northPole, startTime, durationHours = 48, minElevationDegrees = 10.0)
        assertTrue("ISS must NEVER produce passes >= 10° at North Pole: got ${northPasses.size}",
            northPasses.isEmpty())

        // 2. ISS from South Pole:
        val southPasses = predictor.predictPasses(issTle, southPole, startTime, durationHours = 48, minElevationDegrees = 10.0)
        assertTrue("ISS must NEVER produce passes >= 10° at South Pole: got ${southPasses.size}",
            southPasses.isEmpty())

        // 3. Polar satellite (NOAA 19, inc = 98.7°) at North Pole:
        // Polar satellite MUST produce frequent passes over the pole (approx every ~101 min orbit)!
        val polarPasses = predictor.predictPasses(polarTle, northPole, startTime, durationHours = 24, minElevationDegrees = 10.0)
        assertTrue("Polar satellite must produce multiple passes at North Pole over 24h: got ${polarPasses.size}",
            polarPasses.size >= 10)

        polarPasses.forEach { pass ->
            assertEquals("NOAA 19 (POLAR)", pass.satelliteName)
            assertTrue("Rise before max: ${pass.riseTime} vs ${pass.maxElevationTime}",
                pass.riseTime <= pass.maxElevationTime)
            assertTrue("Max before set: ${pass.maxElevationTime} vs ${pass.setTime}",
                pass.maxElevationTime <= pass.setTime)
            assertTrue("Peak elevation above threshold: ${pass.maxElevation}",
                pass.maxElevation >= 10.0)
            assertTrue("Azimuth finite and in [0, 360): ${pass.riseAzimuth}",
                pass.riseAzimuth in 0.0..360.0)
            assertTrue("Track points non-empty", pass.trackPoints.isNotEmpty())
        }
    }

    @Test
    fun `challengeSgp4PassPredictionAtEquatorAndNullIsland`() {
        val nullIsland = GeoPoint(0.0, 0.0, 0.0) // Equator, Prime Meridian
        val startTime = issTle.epochInstant

        val passes = predictor.predictPasses(issTle, nullIsland, startTime, durationHours = 48, minElevationDegrees = 10.0)
        assertTrue("ISS must produce passes at Equator over 48h: got ${passes.size}", passes.isNotEmpty())

        var hasHighPass = false
        passes.forEach { pass ->
            if (pass.maxElevation > 50.0) hasHighPass = true
            assertTrue("Duration within physical limits [60..1200s]: ${pass.durationSeconds}",
                pass.durationSeconds in 60..1200)
            assertTrue("Track points non-empty", pass.trackPoints.isNotEmpty())
            assertTrue("Time markers non-empty", pass.timeMarkers.isNotEmpty())
        }
        assertTrue("Equatorial observer should see at least one high-elevation pass (>50°)", hasHighPass)

        // Symmetry test across the International Date Line:
        // GeoPoint(0, +180) vs GeoPoint(0, -180) must give identical topocentric positions
        val pEast = GeoPoint(0.0, 180.0, 0.0)
        val pWest = GeoPoint(0.0, -180.0, 0.0)
        val posEast = propagator.propagate(issTle, startTime, pEast)
        val posWest = propagator.propagate(issTle, startTime, pWest)

        assertNotNull(posEast)
        assertNotNull(posWest)
        assertEquals("Azimuth at +180° vs -180° lon", posEast!!.coordinates.azimuth, posWest!!.coordinates.azimuth, 1e-4)
        assertEquals("Altitude at +180° vs -180° lon", posEast.coordinates.altitude, posWest.coordinates.altitude, 1e-4)
        assertEquals("Range at +180° vs -180° lon", posEast.rangeKm, posWest.rangeKm, 1e-4)
    }

    @Test
    fun `challengePathologicalAndCorruptedTleHandling`() {
        val validLine1 = "1 25544U 98067A   26263.51829341  .00016717  00000-0  10270-3 0  9997"
        val validLine2 = "2 25544  51.6416 247.4627 0006703 130.5360 325.0288 15.72125391563537"

        // 1. Checksum validation: corrupted digit
        val badCheckLine = validLine1.substring(0, 68) + "1"
        assertFalse("Bad checksum must fail validation", TleParser.validateChecksum(badCheckLine))
        assertTrue("Valid checksum must pass", TleParser.validateChecksum(validLine1))

        // 2. Invalid prefix rejection:
        var threwPrefix1 = false
        try {
            TleParser.parseTle("3 25544U ...", validLine2)
        } catch (e: IllegalArgumentException) {
            threwPrefix1 = true
        }
        assertTrue("Must throw IllegalArgumentException for line 1 prefix != '1 '", threwPrefix1)

        var threwPrefix2 = false
        try {
            TleParser.parseTle(validLine1, "1 25544 ...")
        } catch (e: IllegalArgumentException) {
            threwPrefix2 = true
        }
        assertTrue("Must throw IllegalArgumentException for line 2 prefix != '2 '", threwPrefix2)

        // 3. Multi-line parser resilience: empty / garbage input
        val emptyList = TleParser.parseMultiple(listOf("", "   ", "\n"))
        assertTrue("Empty text must yield empty list without throwing", emptyList.isEmpty())

        val garbageList = TleParser.parseMultiple(listOf("Random junk line", "Another line without TLE format"))
        assertTrue("Garbage text must yield empty list", garbageList.isEmpty())

        // 4. Negative B* drag:
        val negBstarTle = issTle.copy(bstarDrag = -0.00005)
        val posNegBstar = propagator.propagate(negBstarTle, issTle.epochInstant, GeoPoint(52.52, 13.4, 0.0))
        assertNotNull("Negative B* drag must propagate successfully", posNegBstar)
        assertFalse("Range cannot be NaN", posNegBstar!!.rangeKm.isNaN())

        // 5. Deep space classification:
        // Geostationary satellite (mean motion ~1.0027 rev/day -> period ~1436 minutes >= 225 min)
        val geoTle = issTle.copy(meanMotionRevsPerDay = 1.0027379)
        assertTrue("Geostationary period ~1436m must be classified as deep space", geoTle.isDeepSpace)
        assertEquals(1436.07, geoTle.orbitalPeriodMinutes, 1.0)

        // LEO satellite (mean motion 15.72 rev/day -> period ~91.6 min < 225 min)
        assertFalse("LEO period ~91.6m must not be deep space", issTle.isDeepSpace)

        // 6. Zero eccentricity handling:
        val zeroEccTle = issTle.copy(eccentricity = 0.0)
        val posZeroEcc = propagator.propagate(zeroEccTle, issTle.epochInstant, GeoPoint(0.0, 0.0, 0.0))
        assertNotNull("Zero eccentricity must propagate without singularity", posZeroEcc)
        assertFalse("Azimuth cannot be NaN", posZeroEcc!!.coordinates.azimuth.isNaN())
        assertFalse("Altitude cannot be NaN", posZeroEcc.coordinates.altitude.isNaN())
    }

    @Test
    fun `challengeTopocentricSEZCoordinateSingularities`() {
        val observer = GeoPoint(0.0, 0.0, 0.0)
        val obsEcef = propagator.observerEcef(observer)

        // Test 1: Satellite directly at the observer's exact location (range = 0)
        // System must avoid division by zero or NaN
        val (az0, alt0, r0) = propagator.ecefToTopocentric(obsEcef, obsEcef, observer)
        assertFalse("Azimuth at zero range must not be NaN: $az0", az0.isNaN())
        assertFalse("Altitude at zero range must not be NaN: $alt0", alt0.isNaN())
        assertEquals(0.0, r0, 1e-6)

        // Test 2: Satellite at nadir (directly below observer through the center of the Earth)
        val nadirSat = obsEcef * 0.5
        val (_, altNadir, rNadir) = propagator.ecefToTopocentric(nadirSat, obsEcef, observer)
        assertEquals(-90.0, altNadir, 0.01)
        assertTrue("Range to nadir point must be positive: $rNadir", rNadir > 0.0)
    }

    @Test
    fun `challengeRangeRateDopplerSignConventionDuringPass`() {
        // Berlin observer, ISS pass
        val observer = GeoPoint(52.52, 13.405, 34.0)
        val startTime = issTle.epochInstant
        val passes = predictor.predictPasses(issTle, observer, startTime, durationHours = 48, minElevationDegrees = 20.0)
        assertTrue("Expected passes above 20°", passes.isNotEmpty())

        val pass = passes.first()
        val tRise = pass.riseTime
        val tMax = pass.maxElevationTime
        val tSet = pass.setTime

        val posRise = propagator.propagate(issTle, tRise, observer)
        val posMax = propagator.propagate(issTle, tMax, observer)
        val posSet = propagator.propagate(issTle, tSet, observer)

        assertNotNull(posRise)
        assertNotNull(posMax)
        assertNotNull(posSet)

        // Approaching phase: range rate must be negative (distance decreasing)
        assertTrue("Range rate at rise must be negative (closing): was ${posRise!!.rangeRateKmS}",
            posRise.rangeRateKmS < -1.0)

        // Closest approach (max elevation): range rate must be near zero
        assertTrue("Range rate at culmination should be near zero (+/- 2.5 km/s): was ${posMax!!.rangeRateKmS}",
            abs(posMax.rangeRateKmS) < 2.5)

        // Receding phase: range rate must be positive (distance increasing)
        assertTrue("Range rate at set must be positive (opening): was ${posSet!!.rangeRateKmS}",
            posSet.rangeRateKmS > 1.0)
    }

    @Test
    fun `challengeVisibleTerminatorPathIntegrityAcrossAllPhases`() {
        val phases = listOf(
            "New Moon" to Instant.parse("2026-09-11T14:00:00Z"),
            "First Quarter" to Instant.parse("2026-09-18T22:00:00Z"),
            "Full Moon" to Instant.parse("2026-09-26T16:00:00Z"),
            "Last Quarter" to Instant.parse("2026-10-03T14:00:00Z")
        )

        phases.forEach { (phaseName, instant) ->
            val state = LunarTerminatorCalculator.calculateTerminator(instant)
            val path = LunarTerminatorCalculator.generateVisibleTerminatorPath(state, stepDegrees = 2.0)

            // Polyline points must be generated
            assertTrue("Terminator path points for $phaseName should not be empty", path.isNotEmpty())

            path.forEach { (x, y) ->
                assertFalse("Terminator x cannot be NaN in $phaseName", x.isNaN())
                assertFalse("Terminator y cannot be NaN in $phaseName", y.isNaN())
                val rSq = x * x + y * y
                assertTrue("Terminator point ($x, $y) in $phaseName must lie within unit disc: was $rSq",
                    rSq <= 1.001f)
            }
        }
    }

    @Test
    fun `challengeFeaturesNearTerminatorStrictOrdering`() {
        val state = LunarTerminatorCalculator.calculateTerminator(Instant.parse("2026-09-19T06:00:00Z"))
        val highlights = LunarTerminatorCalculator.featuresNearTerminator(state)

        assertTrue("Highlights list must not be empty", highlights.isNotEmpty())

        var foundNonOptimal = false
        var prevReliefScore = Double.MAX_VALUE
        var prevDist = -1.0

        highlights.forEach { hl ->
            if (hl.inOptimalRelief) {
                assertFalse("Optimal relief feature found after non-optimal feature (bad partitioning)",
                    foundNonOptimal)
                assertTrue("Relief score must be non-increasing within optimal relief group",
                    hl.reliefScore <= prevReliefScore + 1e-9)

                if (abs(hl.reliefScore - prevReliefScore) < 1e-6 && prevDist >= 0.0) {
                    assertTrue("Distance to terminator must be non-decreasing for identical relief score",
                        hl.distanceToTerminatorDegrees >= prevDist - 1e-9)
                }
                prevReliefScore = hl.reliefScore
                prevDist = hl.distanceToTerminatorDegrees
            } else {
                foundNonOptimal = true
            }
        }
    }

    @Test
    fun `challengePredictPassesBoundaryParameters`() {
        val observer = GeoPoint(52.52, 13.405, 34.0)
        val startTime = issTle.epochInstant

        // 1. Duration = 0 hours -> must return empty list
        val zeroPasses = predictor.predictPasses(issTle, observer, startTime, durationHours = 0)
        assertTrue("Zero duration must return zero passes", zeroPasses.isEmpty())

        // 2. High elevation threshold: minElevation = 85.0°
        // Over 48 hours, an 85° pass is very rare or at most 1
        val highAltPasses = predictor.predictPasses(issTle, observer, startTime, durationHours = 48, minElevationDegrees = 85.0)
        highAltPasses.forEach { pass ->
            assertTrue("Peak elevation must be >= 85°: was ${pass.maxElevation}", pass.maxElevation >= 85.0)
        }

        // 3. High satellite velocity: ISS orbital speed ~7.67 km/s
        val pos = propagator.propagate(issTle, startTime, observer)
        assertNotNull(pos)
        val nextPos = propagator.propagate(issTle, startTime.plusSeconds(1), observer)
        assertNotNull(nextPos)
    }
}
