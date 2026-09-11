package de.projektastra.app

import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

internal data class LightPollutionSourceMetadata(
    val name: String = "NASA GIBS · VIIRS Night Lights",
    val dataYear: Int = 2016
) {
    fun ageYears(currentYear: Int = LocalDate.now().year): Int =
        (currentYear - dataYear).coerceAtLeast(0)
}

/** Interpretation of a rendered night-light image, not a measured local sky brightness. */
internal enum class LightPollutionInterpretation(val label: String, val description: String) {
    LOW("wenig Nachtlicht im Kartenbild", "Das historische Satellitenbild zeigt hier wenig sichtbares Nachtlicht."),
    MODERATE("mäßiges Nachtlicht im Kartenbild", "Das historische Satellitenbild zeigt hier eine mäßige Aufhellung."),
    HIGH("viel Nachtlicht im Kartenbild", "Das historische Satellitenbild zeigt hier deutliches Nachtlicht."),
    VERY_HIGH("sehr viel Nachtlicht im Kartenbild", "Das historische Satellitenbild zeigt hier besonders helles Nachtlicht.")
}

internal data class LightPollutionTileCoordinate(
    val tileX: Int,
    val tileY: Int,
    val pixelX: Int,
    val pixelY: Int
)

internal data class LightPollutionSample(
    val luminance: Double,
    val validPixelCount: Int,
    val sampledPixelCount: Int
) {
    val coverageFraction: Double get() = validPixelCount.toDouble() / sampledPixelCount
}

/** Only a coarse, altitude-free position is retained in memory. Never persisted to disk. */
internal data class LightPollutionLocationKey(val latitudeHundredths: Int, val longitudeHundredths: Int) {
    fun toPoint() = GeoPoint(latitudeHundredths / 100.0, longitudeHundredths / 100.0, 0.0)
}

internal object LightPollutionModel {
    const val MAX_MERCATOR_LATITUDE = 85.0511287798066
    const val TILE_SIZE = 256
    const val SOURCE_ZOOM = 8
    val sourceMetadata = LightPollutionSourceMetadata()

    fun isSourceCovered(point: GeoPoint): Boolean =
        point.latitude.isFinite() && point.latitude in -MAX_MERCATOR_LATITUDE..MAX_MERCATOR_LATITUDE &&
            point.longitude.isFinite() && point.longitude in -180.0..180.0

    fun locationKey(point: GeoPoint): LightPollutionLocationKey {
        require(point.latitude.isFinite() && point.latitude in -90.0..90.0)
        require(point.longitude.isFinite() && point.longitude in -180.0..180.0)
        val rounded = NetworkPolicy.roundedLocation(point)
        // Both representations of the antimeridian refer to the same location/tile.
        val longitude = if (rounded.longitude == 180.0) -180.0 else rounded.longitude
        return LightPollutionLocationKey(
            kotlin.math.round(rounded.latitude * 100.0).toInt(),
            kotlin.math.round(longitude * 100.0).toInt()
        )
    }

    fun tileCoordinate(point: GeoPoint, zoom: Int = SOURCE_ZOOM): LightPollutionTileCoordinate {
        require(point.latitude.isFinite() && point.latitude in -90.0..90.0)
        require(point.longitude.isFinite() && point.longitude in -180.0..180.0)
        require(zoom in 0..SOURCE_ZOOM)
        val tileCount = 1 shl zoom
        val worldSize = tileCount * TILE_SIZE
        val longitude = if (point.longitude == 180.0) -180.0 else point.longitude
        val latitude = Math.toRadians(point.latitude.coerceIn(-MAX_MERCATOR_LATITUDE, MAX_MERCATOR_LATITUDE))
        // Clamp the complete world pixel before splitting it. Clamping tile and pixel
        // independently would accidentally sample the opposite pixel at the south pole.
        val worldX = floor((longitude + 180.0) / 360.0 * worldSize).toInt().coerceIn(0, worldSize - 1)
        val worldY = floor((1.0 - ln(tan(latitude) + 1.0 / cos(latitude)) / PI) / 2.0 * worldSize)
            .toInt().coerceIn(0, worldSize - 1)
        return LightPollutionTileCoordinate(
            worldX / TILE_SIZE, worldY / TILE_SIZE, worldX % TILE_SIZE, worldY % TILE_SIZE
        )
    }

    /**
     * Samples available pixels once each. A tile edge truncates the window rather than
     * duplicating its border. Transparent no-data pixels do not become "dark sky";
     * opaque black is valid. Alpha weights partially transparent pixels without making
     * their RGB values darker. This is display-image luminance, not VIIRS radiance.
     */
    fun sampleLuminance(
        width: Int,
        height: Int,
        centerX: Int,
        centerY: Int,
        radius: Int = 4,
        colorAt: (x: Int, y: Int) -> Int
    ): LightPollutionSample? {
        require(width > 0 && height > 0 && centerX in 0 until width && centerY in 0 until height)
        require(radius in 0..16)
        var weightedSum = 0.0
        var alphaSum = 0.0
        var validCount = 0
        var sampledCount = 0
        for (y in maxOf(0, centerY - radius)..minOf(height - 1, centerY + radius)) {
            for (x in maxOf(0, centerX - radius)..minOf(width - 1, centerX + radius)) {
                sampledCount++
                val argb = colorAt(x, y)
                val alpha = (argb ushr 24) and 255
                if (alpha == 0) continue
                val red = (argb ushr 16) and 255
                val green = (argb ushr 8) and 255
                val blue = argb and 255
                // Integer coefficients keep uniform white exactly at 255, so flooring
                // the resulting score does not turn an all-white sample into 99/100.
                weightedSum += (2126 * red + 7152 * green + 722 * blue) / 10000.0 * alpha
                alphaSum += alpha
                validCount++
            }
        }
        return if (validCount == 0) null else LightPollutionSample(weightedSum / alphaSum, validCount, sampledCount)
    }

    /**
     * Existing Astra score mapping, deliberately kept stable. The exponent and class
     * thresholds are an uncalibrated visual heuristic. The compatibility Bortle and
     * magnitude fields must not be presented as physical measurements or conversions.
     */
    fun estimateFromLuminance(luminance: Double, coverageFraction: Double = 1.0): LightPollutionEstimate {
        require(luminance.isFinite())
        require(coverageFraction.isFinite() && coverageFraction > 0.0 && coverageFraction <= 1.0)
        val index = ((luminance / 255.0).coerceIn(0.0, 1.0).pow(0.62) * 100.0).toInt()
        val bortle = when {
            index < 3 -> 1
            index < 7 -> 2
            index < 13 -> 3
            index < 22 -> 4
            index < 35 -> 5
            index < 50 -> 6
            index < 68 -> 7
            index < 85 -> 8
            else -> 9
        }
        return LightPollutionEstimate(
            index, bortle, 22.0 - (index / 100.0) * 4.0,
            sourceYear = sourceMetadata.dataYear, sampleCoverageFraction = coverageFraction
        )
    }

    fun interpretation(index: Int): LightPollutionInterpretation = when {
        index < 22 -> LightPollutionInterpretation.LOW
        index < 50 -> LightPollutionInterpretation.MODERATE
        index < 85 -> LightPollutionInterpretation.HIGH
        else -> LightPollutionInterpretation.VERY_HIGH
    }
}

/** Invalidating the cache also invalidates already-running downloads, including callbacks. */
internal class LightPollutionMemoryCache {
    internal data class Ticket(val generation: Long, val request: Long, val key: LightPollutionLocationKey)
    private var generation = 0L
    private var request = 0L
    private var cachedKey: LightPollutionLocationKey? = null
    private var cachedEstimate: LightPollutionEstimate? = null

    @Synchronized fun get(key: LightPollutionLocationKey): LightPollutionEstimate? =
        if (cachedKey == key) cachedEstimate else null

    @Synchronized fun begin(key: LightPollutionLocationKey): Ticket = Ticket(generation, ++request, key)

    @Synchronized fun isCurrent(ticket: Ticket): Boolean =
        ticket.generation == generation

    @Synchronized fun store(ticket: Ticket, estimate: LightPollutionEstimate): Boolean {
        if (!isCurrent(ticket) || ticket.request != request) return false
        cachedKey = ticket.key
        cachedEstimate = estimate
        return true
    }

    @Synchronized fun clear() {
        generation++
        cachedKey = null
        cachedEstimate = null
    }
}
