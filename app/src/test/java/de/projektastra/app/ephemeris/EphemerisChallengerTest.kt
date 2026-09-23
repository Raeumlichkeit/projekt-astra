package de.projektastra.app.ephemeris

import de.projektastra.app.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.Instant
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class EphemerisChallengerTest {

    // =========================================================================
    // CHALLENGE 1: Galilean Moons Event Geometry
    // =========================================================================

    @Test
    fun `challengeIoTransitAndShadowTransitGeometry`() {
        // Sample across Io's ~42.5h orbit starting 2026-09-20T00:00:00Z in 30-second steps
        val start = Instant.parse("2026-09-20T00:00:00Z")
        val stepSec = 30L
        val totalSteps = (48 * 3600 / stepSec).toInt()

        var moonTransitIngress: Instant? = null
        var moonTransitEgress: Instant? = null
        var shadowTransitIngress: Instant? = null
        var shadowTransitEgress: Instant? = null

        var wasMoonInTransit = false
        var wasShadowInTransit = false

        for (i in 0 until totalSteps) {
            val t = start.plusSeconds(i * stepSec)
            val state = JupiterMoonsCalculator.calculate(t)
            val io = state.io

            val isMoonInTransit = io.zAU < 0.0 && io.offsetRJ in -1.05..1.05 && io.event == JupiterMoonEvent.TRANSIT
            val isShadowInTransit = io.isShadowTransiting

            // Detect first moon transit
            if (isMoonInTransit && !wasMoonInTransit && moonTransitIngress == null) {
                moonTransitIngress = t
            }
            if (!isMoonInTransit && wasMoonInTransit && moonTransitIngress != null && moonTransitEgress == null) {
                moonTransitEgress = t
            }

            // Detect first shadow transit
            if (isShadowInTransit && !wasShadowInTransit && shadowTransitIngress == null) {
                shadowTransitIngress = t
            }
            if (!isShadowInTransit && wasShadowInTransit && shadowTransitIngress != null && shadowTransitEgress == null) {
                shadowTransitEgress = t
            }

            wasMoonInTransit = isMoonInTransit
            wasShadowInTransit = isShadowInTransit

            // If in shadow transit, assert shadow geometric integrity
            if (isShadowInTransit) {
                assertNotNull("Io shadowOffsetRJ must not be null during shadow transit at $t", io.shadowOffsetRJ)
                assertTrue("Io shadowOffsetRJ must be on visible disk (|offset| <= 1.05), was ${io.shadowOffsetRJ}",
                    abs(io.shadowOffsetRJ!!) <= 1.05)
                assertNotNull("Io shadowXArcsec must not be null", io.shadowXArcsec)
                assertNotNull("Io shadowYArcsec must not be null", io.shadowYArcsec)
                val rjArcsec = state.angularDiameterArcsec / 2.0
                val sx = io.shadowXArcsec ?: 0.0
                val sy = io.shadowYArcsec ?: 0.0
                val shadowDistArcsec = sqrt(sx * sx + sy * sy)
                assertTrue("Shadow position on sky must be within Jupiter disk ($rjArcsec arcsec): was $shadowDistArcsec",
                    shadowDistArcsec <= rjArcsec * 1.05)
            }
        }

        assertNotNull("Io moon transit ingress must be detected within 48h", moonTransitIngress)
        assertNotNull("Io moon transit egress must be detected within 48h", moonTransitEgress)
        assertNotNull("Io shadow transit ingress must be detected within 48h", shadowTransitIngress)
        assertNotNull("Io shadow transit egress must be detected within 48h", shadowTransitEgress)

        // Validate Transit Timestamps and Physical Duration
        val moonTransitDurationSec = moonTransitEgress!!.epochSecond - moonTransitIngress!!.epochSecond
        assertTrue("Io moon transit duration must be 2.0 to 2.5 hours (~7200..9000s), was $moonTransitDurationSec s",
            moonTransitDurationSec in 7000..9200)

        val shadowTransitDurationSec = shadowTransitEgress!!.epochSecond - shadowTransitIngress!!.epochSecond
        assertTrue("Io shadow transit duration must be 2.0 to 2.5 hours (~7200..9000s), was $shadowTransitDurationSec s",
            shadowTransitDurationSec in 7000..9200)

        // Ingress boundary assertions: moon enters at limb
        val stateIngress = JupiterMoonsCalculator.calculate(moonTransitIngress)
        assertTrue("At ingress, Io must be in front of Jupiter (zAU < 0)", stateIngress.io.zAU < 0.0)
        assertTrue("At ingress, Io offsetRJ must be near +/-1.0 (limb), was ${stateIngress.io.offsetRJ}",
            abs(abs(stateIngress.io.offsetRJ) - 1.0) < 0.15)

        // Egress boundary assertions
        val stateEgress = JupiterMoonsCalculator.calculate(moonTransitEgress)
        assertTrue("At egress, Io must be near limb, was ${stateEgress.io.offsetRJ}",
            abs(abs(stateEgress.io.offsetRJ) - 1.0) < 0.15)

        // Midpoint assertion: moon must be near central meridian
        val midTransit = moonTransitIngress.plusSeconds(moonTransitDurationSec / 2)
        val stateMid = JupiterMoonsCalculator.calculate(midTransit)
        assertTrue("At mid-transit, Io must be near Jupiter center (|offsetRJ| < 0.3), was ${stateMid.io.offsetRJ}",
            abs(stateMid.io.offsetRJ) < 0.3)

        // Phase angle challenge: in September 2026, Jupiter is approaching opposition (Sun is East of Earth-Jupiter line)
        // Shadow transit MUST precede moon transit ingress
        assertTrue("Shadow transit ingress must occur before moon transit ingress",
            shadowTransitIngress.isBefore(moonTransitIngress))
        assertTrue("Shadow transit egress must occur before moon transit egress",
            shadowTransitEgress.isBefore(moonTransitEgress))

        val shadowLeadSec = moonTransitIngress.epochSecond - shadowTransitIngress.epochSecond
        assertTrue("Shadow ingress lead time must be between 0.5 and 2.0 hours, was ${shadowLeadSec / 3600.0}h",
            shadowLeadSec in 1800..7200)
    }

    @Test
    fun `challengeIoOccultationAndEclipseGeometry`() {
        val start = Instant.parse("2026-09-20T00:00:00Z")
        val stepSec = 30L
        val totalSteps = (48 * 3600 / stepSec).toInt()

        var occIngress: Instant? = null
        var occEgress: Instant? = null
        var eclIngress: Instant? = null
        var eclEgress: Instant? = null

        var wasOcc = false
        var wasEcl = false

        for (i in 0 until totalSteps) {
            val t = start.plusSeconds(i * stepSec)
            val state = JupiterMoonsCalculator.calculate(t)
            val io = state.io

            val isOcc = io.zAU > 0.0 && io.event == JupiterMoonEvent.OCCULTATION
            val isEcl = io.event == JupiterMoonEvent.ECLIPSE

            if (isOcc && !wasOcc && occIngress == null) occIngress = t
            if (!isOcc && wasOcc && occIngress != null && occEgress == null) occEgress = t

            if (isEcl && !wasEcl && eclIngress == null) eclIngress = t
            if (!isEcl && wasEcl && eclIngress != null && eclEgress == null) eclEgress = t

            wasOcc = isOcc
            wasEcl = isEcl
        }

        assertNotNull("Io occultation ingress must be detected within 48h", occIngress)
        assertNotNull("Io occultation egress must be detected within 48h", occEgress)
        assertNotNull("Io eclipse ingress must be detected within 48h", eclIngress)
        assertNotNull("Io eclipse egress must be detected within 48h", eclEgress)

        val occDuration = occEgress!!.epochSecond - occIngress!!.epochSecond
        assertTrue("Io occultation duration must be ~2.0..2.5h, was $occDuration s", occDuration in 6800..9200)

        // The visible ECLIPSE event occurs when Io emerges from behind Jupiter's disk but is still in Jupiter's shadow cone
        val eclDuration = eclEgress!!.epochSecond - eclIngress!!.epochSecond
        assertTrue("Io visible eclipse egress duration must be ~40..60 min, was $eclDuration s", eclDuration in 2000..4000)

        // Occultation geometry: moon is behind Jupiter disk
        val occState = JupiterMoonsCalculator.calculate(occIngress)
        assertTrue("Io must be behind Jupiter during occultation (zAU > 0)", occState.io.zAU > 0.0)
        assertTrue("At occultation ingress, Io must be at planetary limb, was ${occState.io.offsetRJ}",
            abs(abs(occState.io.offsetRJ) - 1.0) < 0.15)
    }

    @Test
    fun `challengeEuropaTransitShadowAndOccultationGeometry`() {
        // Europa events centered around 2026-09-21: shadow transit ~01:00, transit ~03:30
        val start = Instant.parse("2026-09-20T22:00:00Z")
        val stepSec = 30L
        val totalSteps = (12 * 3600 / stepSec).toInt()

        var shIngress: Instant? = null
        var shEgress: Instant? = null
        var trIngress: Instant? = null
        var trEgress: Instant? = null

        var wasSh = false
        var wasTr = false

        for (i in 0 until totalSteps) {
            val t = start.plusSeconds(i * stepSec)
            val state = JupiterMoonsCalculator.calculate(t)
            val europa = state.europa

            val isSh = europa.isShadowTransiting
            val isTr = europa.zAU < 0.0 && europa.event == JupiterMoonEvent.TRANSIT

            if (isSh && !wasSh && shIngress == null) shIngress = t
            if (!isSh && wasSh && shIngress != null && shEgress == null) shEgress = t

            if (isTr && !wasTr && trIngress == null) trIngress = t
            if (!isTr && wasTr && trIngress != null && trEgress == null) trEgress = t

            wasSh = isSh
            wasTr = isTr

            if (isSh) {
                assertNotNull("Europa shadowOffsetRJ must not be null", europa.shadowOffsetRJ)
                assertTrue("Europa shadow offset must be on disk: ${europa.shadowOffsetRJ}",
                    abs(europa.shadowOffsetRJ!!) <= 1.05)
            }
        }

        assertNotNull("Europa shadow transit ingress must be detected", shIngress)
        assertNotNull("Europa shadow transit egress must be detected", shEgress)
        assertNotNull("Europa transit ingress must be detected", trIngress)
        assertNotNull("Europa transit egress must be detected", trEgress)

        val shDuration = shEgress!!.epochSecond - shIngress!!.epochSecond
        val trDuration = trEgress!!.epochSecond - trIngress!!.epochSecond

        // Europa orbital speed is lower than Io; transit duration is ~2.5 to 3.2 hours
        assertTrue("Europa shadow transit duration should be 2.5 to 3.2 hours, was $shDuration s",
            shDuration in 9000..12000)
        assertTrue("Europa transit duration should be 2.5 to 3.2 hours, was $trDuration s",
            trDuration in 9000..12000)

        // Shadow transit precedes Europa transit
        assertTrue("Europa shadow ingress must precede moon ingress", shIngress.isBefore(trIngress))
        assertTrue("Europa shadow egress must precede moon egress", shEgress.isBefore(trEgress))

        val leadSec = trIngress.epochSecond - shIngress.epochSecond
        assertTrue("Europa shadow lead time must be between 1.5 and 4.0 hours, was ${leadSec / 3600.0}h",
            leadSec in 5400..14400)
    }

    @Test
    fun `challengeGalileanMoonsSpatialContinuityAndExclusivity`() {
        // High-resolution trajectory sampling over 24h: step every 60s
        val start = Instant.parse("2026-09-20T00:00:00Z")
        var prevIoOffset = JupiterMoonsCalculator.calculate(start).io.offsetRJ

        for (minute in 1..1440) {
            val t = start.plusSeconds(minute * 60L)
            val state = JupiterMoonsCalculator.calculate(t)

            // Assert finite numbers
            assertFalse(state.distanceAu.isNaN())
            assertFalse(state.angularDiameterArcsec.isNaN())
            assertTrue(state.angularDiameterArcsec in 30.0..60.0)

            state.moons.forEach { moon ->
                assertFalse("${moon.name} offsetRJ is NaN at $t", moon.offsetRJ.isNaN())
                assertFalse("${moon.name} zAU is NaN at $t", moon.zAU.isNaN())
                assertFalse("${moon.name} xArcsec is NaN at $t", moon.xArcsec.isNaN())
                assertFalse("${moon.name} yArcsec is NaN at $t", moon.yArcsec.isNaN())

                // Physical exclusivity: cannot be simultaneously TRANSIT and OCCULTATION
                assertFalse(moon.event == JupiterMoonEvent.TRANSIT && moon.zAU > 0.0)
                assertFalse(moon.event == JupiterMoonEvent.OCCULTATION && moon.zAU < 0.0)
            }

            // Smoothness: Io speed ~17.3 km/s -> in 60s moves ~1040 km = ~0.0145 RJ
            val curIoOffset = state.io.offsetRJ
            val deltaOffset = abs(curIoOffset - prevIoOffset)
            assertTrue("Io offsetRJ jumped too abruptly: $deltaOffset RJ in 60s at $t", deltaOffset < 0.04)
            prevIoOffset = curIoOffset
        }
    }

    // =========================================================================
    // CHALLENGE 2: Saturn Ring Tilt B & Titan Orbit
    // =========================================================================

    @Test
    fun `challengeSaturnRingTiltAcrossDecadesAndPlaneCrossing`() {
        // 1. Ring plane crossing (edge-on): March 23, 2025 (|B| < 1.0°)
        val tEdge = Instant.parse("2025-03-23T12:00:00Z")
        val sEdge = SaturnSystemCalculator.calculate(tEdge)
        assertTrue("Saturn ring opening angle |B| during March 2025 plane crossing must be < 1.0 deg: was ${sEdge.ringTiltDegrees}",
            abs(sEdge.ringTiltDegrees) < 1.0)

        // 2. Northern solstice opening: ~2017 (|B| ≈ 26.7°)
        // IAU convention: positive when northern hemisphere tilted toward Earth
        val tMaxNorth = Instant.parse("2017-07-01T00:00:00Z")
        val sMaxNorth = SaturnSystemCalculator.calculate(tMaxNorth)
        assertTrue("Saturn ring tilt magnitude at 2017 northern maximum must be 25..28 deg: was ${sMaxNorth.ringTiltDegrees}",
            abs(sMaxNorth.ringTiltDegrees) in 25.0..28.0)
        assertTrue("IAU convention defines northern tilt as positive", sMaxNorth.ringTiltDegrees > 0.0)

        // 3. Current epoch: September 2026 (B ≈ -7.96°, southern face opening)
        val tNow = Instant.parse("2026-09-20T00:00:00Z")
        val sNow = SaturnSystemCalculator.calculate(tNow)
        assertEquals(-7.96, sNow.ringTiltDegrees, 0.1)

        // 4. Southern solstice opening: ~2032 (|B| ≈ 26.7°, southern face fully open)
        val tMaxSouth = Instant.parse("2032-05-01T00:00:00Z")
        val sMaxSouth = SaturnSystemCalculator.calculate(tMaxSouth)
        assertTrue("Saturn ring tilt at 2032 southern maximum must be -25..-28 deg: was ${sMaxSouth.ringTiltDegrees}",
            sMaxSouth.ringTiltDegrees in -28.0..-25.0)

        // 5. Continuous derivative over 365 daily steps in 2026
        var prevB = sNow.ringTiltDegrees
        for (day in 1..365) {
            val t = tNow.plusSeconds(day * 86400L)
            val st = SaturnSystemCalculator.calculate(t)
            val dB = abs(st.ringTiltDegrees - prevB)
            assertTrue("Ring tilt dB/day cannot exceed 0.08 deg/day: was $dB at $t", dB < 0.08)
            assertTrue("Ring position angle must be in [0, 360): ${st.ringPositionAngle}",
                st.ringPositionAngle in 0.0..360.0)
            assertTrue("Saturn distance must be 8..11 AU: ${st.distanceAu}", st.distanceAu in 8.0..11.0)
            prevB = st.ringTiltDegrees
        }
    }

    @Test
    fun `challengeTitanKeplerianOrbitAndProjectedEllipseOverFullCycle`() {
        val start = Instant.parse("2026-09-20T00:00:00Z")

        // Titan semi-major axis ~20.274 RS, eccentricity ~0.0288
        val aRS = 1221870.0 / 60268.0 // 20.27394
        val e = 0.0288
        val minR = aRS * (1.0 - e) // 19.689
        val maxR = aRS * (1.0 + e) // 20.858

        var minProjectedOffset = Double.MAX_VALUE
        var maxProjectedOffset = Double.MIN_VALUE

        // Sample 384 hourly steps = 16 days (covering the 15.945-day period)
        var cumulativeOrbitalAngleDeg = 0.0
        var prevOrbitalAngle = run {
            val s = SaturnSystemCalculator.calculate(start)
            val bRad = Math.toRadians(s.ringTiltDegrees)
            atan2(s.titanXRS, s.titanYRS / sin(bRad)) * 180.0 / PI
        }

        var cumulativeTopocentricAngleDeg = 0.0
        var prevPa = SaturnSystemCalculator.calculate(start).titanPositionAngle

        for (h in 1..383) {
            val t = start.plusSeconds(h * 3600L)
            val st = SaturnSystemCalculator.calculate(t)
            val bRad = Math.toRadians(st.ringTiltDegrees)

            // Reconstructed 3D orbital radius from (xRS, yRS / sinB)
            val trueY = st.titanYRS / sin(bRad)
            val reconstructedRadius = sqrt(st.titanXRS * st.titanXRS + trueY * trueY)

            assertTrue("Titan reconstructed Keplerian radius ($reconstructedRadius RS) must be within [$minR, $maxR] at $t",
                reconstructedRadius in (minR - 0.05)..(maxR + 0.05))

            // Projected offset bounds on sky plane
            if (st.titanOffsetRS < minProjectedOffset) minProjectedOffset = st.titanOffsetRS
            if (st.titanOffsetRS > maxProjectedOffset) maxProjectedOffset = st.titanOffsetRS

            // Continuous unwrapped orbital longitude increment in ring plane
            val curOrbitalAngle = atan2(st.titanXRS, trueY) * 180.0 / PI
            var dOrb = curOrbitalAngle - prevOrbitalAngle
            if (dOrb > 180.0) dOrb -= 360.0
            if (dOrb < -180.0) dOrb += 360.0
            cumulativeOrbitalAngleDeg += dOrb
            prevOrbitalAngle = curOrbitalAngle

            // Topocentric sky position angle
            var dPa = st.titanPositionAngle - prevPa
            if (dPa > 180.0) dPa -= 360.0
            if (dPa < -180.0) dPa += 360.0
            cumulativeTopocentricAngleDeg += dPa
            prevPa = st.titanPositionAngle
        }

        // Projected offset extrema:
        // Conjunction offset ≈ a * sin(B) ≈ 20.27 * sin(7.96°) ≈ 2.81 RS
        assertTrue("Min projected offset must be near 2.8 RS (conjunction): was $minProjectedOffset",
            minProjectedOffset in 2.5..3.2)
        // Elongation offset ≈ a ≈ 20.3 RS
        assertTrue("Max projected offset must be near 20.3 RS (elongation): was $maxProjectedOffset",
            maxProjectedOffset in 19.5..21.0)

        // Topological Winding Number Challenge:
        // In the orbital plane, in 382.7 hours Titan completes EXACTLY 360° of orbital motion.
        // Over 383 hours (383/382.69 * 360° = 360.29°), cumulative angle must equal 360° (+/- 1.0°)
        assertTrue("Titan orbital plane winding angle over 15.95 days must equal 360 deg: was $cumulativeOrbitalAngleDeg",
            abs(abs(cumulativeOrbitalAngleDeg) - 360.0) < 1.0)

        // Topocentric sky position angle includes Earth/Saturn orbital parallax (~3.3° over 16 days)
        assertTrue("Titan topocentric PA winding over 15.95 days must equal 360 deg +/- 5 deg: was $cumulativeTopocentricAngleDeg",
            abs(abs(cumulativeTopocentricAngleDeg) - 360.0) < 5.0)

        // Exact Periodicity Challenge:
        val periodSec = (15.94542 * 86400).toLong()
        val s0 = SaturnSystemCalculator.calculate(start)
        val sEnd = SaturnSystemCalculator.calculate(start.plusSeconds(periodSec))

        assertEquals("Titan xRS after 1 orbit", s0.titanXRS, sEnd.titanXRS, 0.05)
        // In the orbital plane, true Y = yRS / sin(B) must match within 0.05 RS
        val trueY0 = s0.titanYRS / sin(Math.toRadians(s0.ringTiltDegrees))
        val trueYEnd = sEnd.titanYRS / sin(Math.toRadians(sEnd.ringTiltDegrees))
        assertEquals("Titan orbital plane true Y after 1 orbit", trueY0, trueYEnd, 0.05)

        // Projected yRS on sky reflects changing ring tilt B(t) over 16 days (delta < 0.25 RS)
        assertEquals("Titan projected yRS after 1 orbit", s0.titanYRS, sEnd.titanYRS, 0.25)
        assertEquals("Titan offsetRS after 1 orbit", s0.titanOffsetRS, sEnd.titanOffsetRS, 0.25)

        // In Titan's orbital plane, the true orbital angle after exactly 1 period returns to starting value:
        val orbAngle0 = ((atan2(s0.titanXRS, trueY0) * 180.0 / PI) % 360.0 + 360.0) % 360.0
        val orbAngleEnd = ((atan2(sEnd.titanXRS, trueYEnd) * 180.0 / PI) % 360.0 + 360.0) % 360.0
        val orbAngleDiff = abs(orbAngle0 - orbAngleEnd).let { if (it > 180.0) 360.0 - it else it }
        assertTrue("Titan intrinsic orbital angle after 1 orbit matches within 0.1 deg: diff was $orbAngleDiff", orbAngleDiff < 0.1)

        // Position angle relative to Saturn's ring major axis reflects projected B(t) variation (~2.0 deg):
        val relPa0 = ((s0.titanPositionAngle - s0.ringPositionAngle) % 360.0 + 360.0) % 360.0
        val relPaEnd = ((sEnd.titanPositionAngle - sEnd.ringPositionAngle) % 360.0 + 360.0) % 360.0
        val relPaDiff = abs(relPa0 - relPaEnd).let { if (it > 180.0) 360.0 - it else it }
        assertTrue("Titan PA relative to ring axis after 1 orbit matches within 2.5 deg: diff was $relPaDiff", relPaDiff < 2.5)

        // Topocentric PA relative to celestial north includes ~3.3 deg planetary parallax shift over 16 days:
        val paDiff = abs(s0.titanPositionAngle - sEnd.titanPositionAngle).let { if (it > 180.0) 360.0 - it else it }
        assertTrue("Titan topocentric PA drift after 1 period must be < 5.0 deg: diff was $paDiff", paDiff < 5.0)
    }

    // =========================================================================
    // CHALLENGE 3: SGP4 Propagation Across Epochs, Singularities & Frames
    // =========================================================================

    private val issTle = TleParser.parseTle(
        "1 25544U 98067A   26263.51829341  .00016717  00000-0  10270-3 0  9993",
        "2 25544  51.6416 247.4627 0006703 130.5360 325.0288 15.72125391563537",
        "ISS (ZARYA)"
    )

    private val propagator = Sgp4Propagator()

    @Test
    fun `challengeSgp4AcrossVariedEpochsAndDrag`() {
        val tEpoch = issTle.epochInstant

        // Test propagation across multiple temporal horizons: [-7 days, -1 day, 0, +1 day, +7 days]
        val timeDeltasMinutes = listOf(-10080L, -1440L, -60L, 0L, 60L, 1440L, 10080L)

        for (dtMin in timeDeltasMinutes) {
            val t = tEpoch.plusSeconds(dtMin * 60L)
            val posTeme = propagator.propagateTeme(issTle, t)

            assertFalse("TEME X cannot be NaN at dt=$dtMin", posTeme.x.isNaN())
            assertFalse("TEME Y cannot be NaN at dt=$dtMin", posTeme.y.isNaN())
            assertFalse("TEME Z cannot be NaN at dt=$dtMin", posTeme.z.isNaN())

            val radiusKm = posTeme.magnitude
            val altKm = radiusKm - Sgp4Propagator.RE_KM
            assertTrue("Altitude at dt=$dtMin min must remain in LEO bounds [300..520 km]: was $altKm km",
                altKm in 300.0..520.0)

            // Velocity estimation via finite difference
            val dt = 1.0 // 1 second
            val posNext = propagator.propagateTeme(issTle, t.plusSeconds(1))
            val velVec = (posNext - posTeme) * (1.0 / dt)
            val speedKmS = velVec.magnitude
            assertTrue("Orbital speed at dt=$dtMin min must be in [7.4..7.9 km/s]: was $speedKmS km/s",
                speedKmS in 7.4..7.9)

            // Specific orbital energy E = v^2 / 2 - mu / r < 0 (bound orbit)
            val specificEnergy = (speedKmS * speedKmS) / 2.0 - Sgp4Propagator.MU / radiusKm
            assertTrue("Orbital energy must be negative for bound orbit: was $specificEnergy", specificEnergy < -25.0)
        }
    }

    @Test
    fun `challengeZeroEccentricitySingularity`() {
        // Construct exact zero-eccentricity circular orbit (e = 0.0000000)
        val circTle = TleParser.parseTle(
            "1 99999U 26001A   26263.50000000  .00000000  00000-0  00000-0 0  9991",
            "2 99999  45.0000 120.0000 0000000  60.0000 180.0000 15.00000000000018",
            "CIRCULAR TEST"
        )

        assertEquals(0.0, circTle.eccentricity, 1e-12)

        // Propagate across 5 complete circular orbits (5 * 96 min = 480 min) in 1-minute steps
        val t0 = circTle.epochInstant
        var minR = Double.MAX_VALUE
        var maxR = Double.MIN_VALUE

        for (m in 0..480) {
            val t = t0.plusSeconds(m * 60L)
            val pos = propagator.propagateTeme(circTle, t)

            assertFalse("Circular orbit X must not be NaN", pos.x.isNaN())
            assertFalse("Circular orbit Y must not be NaN", pos.y.isNaN())
            assertFalse("Circular orbit Z must not be NaN", pos.z.isNaN())

            val r = pos.magnitude
            if (r < minR) minR = r
            if (r > maxR) maxR = r
        }

        // python-sgp4 2.24 (WGS-72/Vallado), same TLE sampled at 0..480 min: 10.584664881 km.
        // A zero mean eccentricity still has J2 short-period radial variation in SGP4.
        val deltaR = maxR - minR
        assertEquals("Circular orbit SGP4 radial variation", 10.584664881257595, deltaR, 0.05)
    }

    @Test
    fun `challengeHighEccentricityMolniyaOrbit`() {
        // This 12-hour Molniya orbit needs SDP4 lunar/solar and resonance terms.
        val molniyaTle = TleParser.parseTle(
            "1 88888U 26002A   26263.50000000  .00000100  00000-0  10000-4 0  9992",
            "2 88888  63.4000  40.0000 7200000 270.0000  20.0000  2.00600000000015",
            "MOLNIYA TEST"
        )

        assertEquals(0.72, molniyaTle.eccentricity, 1e-4)

        assertTrue(molniyaTle.isDeepSpace)
        try {
            propagator.propagateTeme(molniyaTle, molniyaTle.epochInstant)
            fail("Deep-space orbit must be rejected explicitly")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("Deep-space"))
        }
    }

    @Test
    fun `challengeTemeToEcefOrthogonalityAndInvariance`() {
        // Orthogonal transformation preserves vector norms and dot products
        val v1 = Vector3D(1234.5, -6789.0, 3456.7)
        val v2 = Vector3D(-9876.5, 4321.0, 1111.1)

        val gmst = propagator.gmst(Instant.parse("2026-09-20T12:00:00Z"))
        val r1 = propagator.temeToEcef(v1, gmst)
        val r2 = propagator.temeToEcef(v2, gmst)

        assertEquals("Length of v1 must be strictly preserved under TEME->ECEF",
            v1.magnitude, r1.magnitude, 1e-10)
        assertEquals("Length of v2 must be strictly preserved under TEME->ECEF",
            v2.magnitude, r2.magnitude, 1e-10)
        assertEquals("Inner product v1 . v2 must be strictly preserved",
            v1.dot(v2), r1.dot(r2), 1e-10)

        // Inverse rotation by -gmst must recover v1
        val invR1 = propagator.temeToEcef(r1, -gmst)
        assertEquals("Inverse rotation by -gmst recovers v1.x", v1.x, invR1.x, 1e-10)
        assertEquals("Inverse rotation by -gmst recovers v1.y", v1.y, invR1.y, 1e-10)
        assertEquals("Inverse rotation by -gmst recovers v1.z", v1.z, invR1.z, 1e-10)
    }

    @Test
    fun `challengeWgs84EllipsoidGeometryAndTopocentricDirections`() {
        // Observer at equator: lat 0°, lon 0°
        val eqObs = GeoPoint(0.0, 0.0, 0.0)
        val eqEcef = propagator.observerEcef(eqObs)
        assertEquals(6378.137, eqEcef.x, 1e-4)
        assertEquals(0.0, eqEcef.y, 1e-4)
        assertEquals(0.0, eqEcef.z, 1e-4)

        // Observer at North Pole: lat 90°, lon 0°
        val npObs = GeoPoint(90.0, 0.0, 0.0)
        val npEcef = propagator.observerEcef(npObs)
        assertEquals(0.0, npEcef.x, 1e-4)
        assertEquals(0.0, npEcef.y, 1e-4)
        // b = a * (1 - f) = 6378.137 * (1 - 1/298.257223563) = 6356.752314 km
        assertEquals(6356.7523, npEcef.z, 1e-4)

        // Directional test for Berlin (52.52° N, 13.405° E)
        val berlin = GeoPoint(52.52, 13.405, 0.0)
        val bEcef = propagator.observerEcef(berlin)

        val latRad = Math.toRadians(berlin.latitude)
        val lonRad = Math.toRadians(berlin.longitude)

        // Geodetic surface normal (Zenith)
        val zenith = Vector3D(cos(latRad) * cos(lonRad), cos(latRad) * sin(lonRad), sin(latRad))
        // Geodetic North vector
        val north = Vector3D(-sin(latRad) * cos(lonRad), -sin(latRad) * sin(lonRad), cos(latRad))
        // East vector
        val east = Vector3D(-sin(lonRad), cos(lonRad), 0.0)

        // 1. Target 500 km at Zenith -> alt=90°, range=500 km
        val (azZ, altZ, rangeZ) = propagator.ecefToTopocentric(bEcef + (zenith * 500.0), bEcef, berlin)
        assertEquals(90.0, altZ, 0.01)
        assertEquals(500.0, rangeZ, 0.01)

        // 2. Target 100 km due North on horizon -> az=0°, alt=0°
        val (azN, altN, rangeN) = propagator.ecefToTopocentric(bEcef + (north * 100.0), bEcef, berlin)
        assertTrue("Azimuth due North should be near 0° or 360°: was $azN", azN < 0.01 || azN > 359.99)
        assertEquals(0.0, altN, 0.01)
        assertEquals(100.0, rangeN, 0.01)

        // 3. Target 100 km due East on horizon -> az=90°, alt=0°
        val (azE, altE, rangeE) = propagator.ecefToTopocentric(bEcef + (east * 100.0), bEcef, berlin)
        assertEquals(90.0, azE, 0.01)
        assertEquals(0.0, altE, 0.01)
        assertEquals(100.0, rangeE, 0.01)

        // 4. Target 100 km due South on horizon -> az=180°, alt=0°
        val (azS, altS, rangeS) = propagator.ecefToTopocentric(bEcef + (north * -100.0), bEcef, berlin)
        assertEquals(180.0, azS, 0.01)
        assertEquals(0.0, altS, 0.01)
        assertEquals(100.0, rangeS, 0.01)

        // 5. Target 100 km due West on horizon -> az=270°, alt=0°
        val (azW, altW, rangeW) = propagator.ecefToTopocentric(bEcef + (east * -100.0), bEcef, berlin)
        assertEquals(270.0, azW, 0.01)
        assertEquals(0.0, altW, 0.01)
        assertEquals(100.0, rangeW, 0.01)
    }
}
