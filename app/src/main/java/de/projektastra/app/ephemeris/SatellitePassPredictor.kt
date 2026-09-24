package de.projektastra.app.ephemeris

import de.projektastra.app.GeoPoint
import de.projektastra.app.HorizontalCoordinates
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.log10

internal data class SatelliteTimeMarker(
    val time: Instant,
    val timeText: String,
    val position: HorizontalCoordinates,
    val elevationDegrees: Double
)

internal data class SatellitePass(
    val satelliteName: String,
    val noradId: Int,
    val riseTime: Instant,
    val riseAzimuth: Double,
    val maxElevationTime: Instant,
    val maxElevation: Double,
    val maxElevationAzimuth: Double,
    val setTime: Instant,
    val setAzimuth: Double,
    val durationSeconds: Long,
    val maxMagnitude: Double,
    val isVisible: Boolean,
    val shadowEntryTime: Instant?,
    val trackPoints: List<SatelliteTrackPoint>,
    val timeMarkers: List<SatelliteTimeMarker>
)

internal class SatellitePassPredictor(private val propagator: Sgp4Propagator = Sgp4Propagator()) {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

    fun predictPasses(
        tle: TleData,
        observer: GeoPoint,
        startTime: Instant,
        durationHours: Int = 48,
        minElevationDegrees: Double = 10.0,
        checkCancellation: () -> Unit = {}
    ): List<SatellitePass> {
        val passes = mutableListOf<SatellitePass>()
        val endTime = startTime.plusSeconds(durationHours * 3600L)
        val coarseStep = 30L // seconds

        var t = startTime
        var inPass = false
        var passRiseTime: Instant? = null

        while (t < endTime) {
            checkCancellation()
            val pos = propagator.propagate(tle, t, observer)
            val alt = pos?.coordinates?.altitude ?: -90.0

            if (!inPass && alt >= minElevationDegrees) {
                val rise = refineBoundary(tle, observer, t.minusSeconds(coarseStep), t, minElevationDegrees, isRise = true)
                inPass = true
                passRiseTime = rise
            } else if (inPass && alt < minElevationDegrees) {
                val set = refineBoundary(tle, observer, t.minusSeconds(coarseStep), t, minElevationDegrees, isRise = false)
                val start = passRiseTime ?: t.minusSeconds(coarseStep)

                val pass = buildPassRecord(tle, observer, start, set, checkCancellation)
                passes.add(pass)
                inPass = false
                passRiseTime = null
            }
            t = t.plusSeconds(coarseStep)
        }
        return passes
    }

    private fun refineBoundary(
        tle: TleData,
        observer: GeoPoint,
        t0: Instant,
        t1: Instant,
        targetAlt: Double,
        isRise: Boolean
    ): Instant {
        var low = t0.toEpochMilli()
        var high = t1.toEpochMilli()
        for (i in 0 until 7) { // 30s / 2^7 = ~0.23 seconds precision
            val mid = (low + high) / 2
            val alt = propagator.propagate(tle, Instant.ofEpochMilli(mid), observer)?.coordinates?.altitude ?: -90.0
            if (isRise) {
                if (alt < targetAlt) low = mid else high = mid
            } else {
                if (alt >= targetAlt) low = mid else high = mid
            }
        }
        return Instant.ofEpochMilli((low + high) / 2)
    }

    private fun buildPassRecord(
        tle: TleData,
        observer: GeoPoint,
        riseTime: Instant,
        setTime: Instant,
        checkCancellation: () -> Unit
    ): SatellitePass {
        val durationSeconds = (setTime.epochSecond - riseTime.epochSecond).coerceAtLeast(10L)
        val stepCount = (durationSeconds / 10).toInt().coerceAtLeast(2)
        val points = mutableListOf<SatelliteTrackPoint>()
        val markers = mutableListOf<SatelliteTimeMarker>()

        var maxAlt = -90.0
        var maxAltTime = riseTime
        var maxAltAz = 0.0
        var minRange = Double.MAX_VALUE
        var anyVisible = false
        var shadowEntry: Instant? = null
        var prevIllum = IlluminationStatus.SUNLIT

        for (i in 0..stepCount) {
            checkCancellation()
            val curTime = riseTime.plusSeconds(i * 10L).coerceAtMost(setTime)
            val pos = propagator.propagate(tle, curTime, observer) ?: continue
            val alt = pos.coordinates.altitude
            val az = pos.coordinates.azimuth

            if (alt > maxAlt) {
                maxAlt = alt
                maxAltTime = curTime
                maxAltAz = az
            }
            if (pos.rangeKm < minRange) minRange = pos.rangeKm
            if (pos.isVisible) anyVisible = true

            // Detect shadow entry during pass
            if (prevIllum == IlluminationStatus.SUNLIT && pos.illumination == IlluminationStatus.ECLIPSED && shadowEntry == null) {
                shadowEntry = curTime
            }
            prevIllum = pos.illumination

            val trackPoint = SatelliteTrackPoint(
                time = curTime,
                position = pos.coordinates,
                rangeKm = pos.rangeKm,
                illumination = pos.illumination,
                isVisible = pos.isVisible
            )
            points.add(trackPoint)

            // Add time markers every 60 seconds
            if (i % 6 == 0 || i == stepCount) {
                val timeStr = timeFormatter.format(curTime)
                markers.add(SatelliteTimeMarker(curTime, timeStr, pos.coordinates, alt))
            }
        }

        val intrinsicMag = when (tle.noradCatalogNumber) {
            25544 -> -1.8 // ISS
            48274 -> -1.0 // Tiangong
            20580 -> 1.5  // Hubble
            else -> 2.0
        }
        val safeRange = if (minRange < 1.0) 1000.0 else minRange
        val estimatedMag = intrinsicMag + 5.0 * log10(safeRange / 1000.0)

        val risePos = propagator.propagate(tle, riseTime, observer)?.coordinates
        val setPos = propagator.propagate(tle, setTime, observer)?.coordinates

        return SatellitePass(
            satelliteName = tle.satelliteName,
            noradId = tle.noradCatalogNumber,
            riseTime = riseTime,
            riseAzimuth = risePos?.azimuth ?: 0.0,
            maxElevationTime = maxAltTime,
            maxElevation = maxAlt,
            maxElevationAzimuth = maxAltAz,
            setTime = setTime,
            setAzimuth = setPos?.azimuth ?: 0.0,
            durationSeconds = durationSeconds,
            maxMagnitude = estimatedMag,
            isVisible = anyVisible,
            shadowEntryTime = shadowEntry,
            trackPoints = points,
            timeMarkers = markers
        )
    }
}
