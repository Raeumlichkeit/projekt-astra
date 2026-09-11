package de.projektastra.app

import org.junit.Assert.*
import org.junit.Test

class LightPollutionModelTest {
    @Test fun antimeridianUsesOneCanonicalTileAndCoarseCacheKey() {
        val east = GeoPoint(0.0, 180.0, 100.0)
        val west = GeoPoint(0.0, -180.0, 0.0)
        assertEquals(LightPollutionModel.tileCoordinate(west), LightPollutionModel.tileCoordinate(east))
        assertEquals(LightPollutionTileCoordinate(0, 128, 0, 0), LightPollutionModel.tileCoordinate(east))
        assertEquals(LightPollutionModel.locationKey(west), LightPollutionModel.locationKey(east))
        assertEquals(LightPollutionLocationKey(5252, 1341), LightPollutionModel.locationKey(GeoPoint(52.523456, 13.406789, 71.4)))
        assertEquals(0.0, LightPollutionModel.locationKey(east).toPoint().altitudeMeters, 0.0)
    }

    @Test fun mercatorNorthAndSouthClampToTheNearestWorldPixel() {
        val north = LightPollutionModel.tileCoordinate(GeoPoint(90.0, 0.0, 0.0))
        val south = LightPollutionModel.tileCoordinate(GeoPoint(-90.0, 0.0, 0.0))
        assertEquals(0, north.tileY)
        assertEquals(0, north.pixelY)
        assertEquals(255, south.tileY)
        assertEquals(255, south.pixelY)
        assertEquals(south, LightPollutionModel.tileCoordinate(GeoPoint(-LightPollutionModel.MAX_MERCATOR_LATITUDE, 0.0, 0.0)))
    }

    @Test fun locationsBeyondTheSourceExtentCannotReceiveAPolarEdgeEstimate() {
        assertTrue(LightPollutionModel.isSourceCovered(GeoPoint(LightPollutionModel.MAX_MERCATOR_LATITUDE, 180.0, 0.0)))
        assertTrue(LightPollutionModel.isSourceCovered(GeoPoint(-LightPollutionModel.MAX_MERCATOR_LATITUDE, -180.0, 0.0)))
        assertFalse(LightPollutionModel.isSourceCovered(GeoPoint(85.052, 0.0, 0.0)))
        assertFalse(LightPollutionModel.isSourceCovered(GeoPoint(-85.052, 0.0, 0.0)))
        assertFalse(LightPollutionModel.isSourceCovered(GeoPoint(90.0, 0.0, 0.0)))
        assertFalse(LightPollutionModel.isSourceCovered(GeoPoint(Double.NaN, 0.0, 0.0)))
    }

    @Test fun projectionNeverEmitsOutOfRangeTilesOrPixels() {
        for (zoom in 0..8) for (latitude in listOf(-90.0, -85.0, 0.0, 52.52, 85.0, 90.0)) {
            for (longitude in listOf(-180.0, -179.99999, 0.0, 13.41, 179.99999, 180.0)) {
                val tile = LightPollutionModel.tileCoordinate(GeoPoint(latitude, longitude, 0.0), zoom)
                assertTrue(tile.tileX in 0 until (1 shl zoom))
                assertTrue(tile.tileY in 0 until (1 shl zoom))
                assertTrue(tile.pixelX in 0..255)
                assertTrue(tile.pixelY in 0..255)
            }
        }
    }

    @Test fun invalidLocationsAndNonFiniteLuminanceAreRejected() {
        for (invalid in listOf(Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)) {
            assertThrows(IllegalArgumentException::class.java) { LightPollutionModel.locationKey(GeoPoint(invalid, 0.0, 0.0)) }
            assertThrows(IllegalArgumentException::class.java) { LightPollutionModel.tileCoordinate(GeoPoint(0.0, invalid, 0.0)) }
            assertThrows(IllegalArgumentException::class.java) { LightPollutionModel.estimateFromLuminance(invalid) }
        }
        assertThrows(IllegalArgumentException::class.java) { LightPollutionModel.locationKey(GeoPoint(90.1, 0.0, 0.0)) }
        assertThrows(IllegalArgumentException::class.java) { LightPollutionModel.tileCoordinate(GeoPoint(0.0, 180.1, 0.0)) }
        assertThrows(IllegalArgumentException::class.java) { LightPollutionModel.estimateFromLuminance(1.0, 0.0) }
    }

    @Test fun transparentTilesAreUnavailableButOpaqueBlackHasAValidZeroIndex() {
        assertNull(LightPollutionModel.sampleLuminance(3, 3, 1, 1) { _, _ -> 0x00FFFFFF })
        val black = LightPollutionModel.sampleLuminance(3, 3, 1, 1) { _, _ -> 0xFF000000.toInt() }!!
        assertEquals(0.0, black.luminance, 0.0)
        assertEquals(9, black.validPixelCount)
        assertEquals(1.0, black.coverageFraction, 0.0)
        assertEquals(0, LightPollutionModel.estimateFromLuminance(black.luminance).index)
    }

    @Test fun partialCoverageDoesNotArtificiallyDarkenValidPixels() {
        val sample = LightPollutionModel.sampleLuminance(3, 3, 1, 1) { x, y ->
            if (x == 1 && y == 1) 0xFFFFFFFF.toInt() else 0
        }!!
        assertEquals(255.0, sample.luminance, 0.00001)
        assertEquals(100, LightPollutionModel.estimateFromLuminance(sample.luminance).index)
        assertEquals(1, sample.validPixelCount)
        assertEquals(9, sample.sampledPixelCount)
        assertEquals(1.0 / 9.0, sample.coverageFraction, 0.00001)
    }

    @Test fun edgeWindowSamplesPixelsOnceInsteadOfRepeatingTheBorder() {
        val visits = mutableSetOf<Pair<Int, Int>>()
        val sample = LightPollutionModel.sampleLuminance(3, 3, 0, 0, radius = 1) { x, y ->
            assertTrue(visits.add(x to y))
            if (x == 0 && y == 0) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        }!!
        assertEquals(4, sample.sampledPixelCount)
        assertEquals(255.0 / 4.0, sample.luminance, 0.00001)
    }

    @Test fun alphaWeightsPartialPixelsWithoutTreatingTheirTransparencyAsDarkness() {
        val sample = LightPollutionModel.sampleLuminance(2, 1, 0, 0) { x, _ ->
            if (x == 0) 0x80FFFFFF.toInt() else 0xFF000000.toInt()
        }!!
        assertEquals(255.0 * 128.0 / (128.0 + 255.0), sample.luminance, 0.00001)
    }

    @Test fun heuristicIsFiniteBoundedAndMonotonicWithoutChangingScoreScale() {
        assertEquals(0, LightPollutionModel.estimateFromLuminance(-100.0).index)
        assertEquals(100, LightPollutionModel.estimateFromLuminance(1000.0).index)
        var previous = -1
        for (luminance in 0..255) {
            val estimate = LightPollutionModel.estimateFromLuminance(luminance.toDouble())
            assertTrue(estimate.index in previous..100)
            assertTrue(estimate.bortleClass in 1..9)
            assertTrue(estimate.skyBrightnessMag in 18.0..22.0)
            previous = estimate.index
        }
        assertEquals(42, LightPollutionModel.estimateFromLuminance(64.0).index)
    }

    @Test fun metadataAndInterpretationDescribeHistoricImageInsteadOfMeasuredSky() {
        val estimate = LightPollutionModel.estimateFromLuminance(0.0)
        assertEquals(2016, estimate.sourceMetadata.dataYear)
        assertEquals(10, estimate.sourceMetadata.ageYears(2026))
        assertEquals(0, estimate.sourceMetadata.ageYears(2010))
        assertEquals(LightPollutionInterpretation.LOW, estimate.interpretation)
        assertTrue(estimate.qualityLabel.contains("Kartenbild"))
        assertEquals(LightPollutionInterpretation.VERY_HIGH, LightPollutionModel.estimateFromLuminance(255.0).interpretation)
    }

    @Test fun clearingCacheInvalidatesInflightResultsEvenWhenTheSameLocationIsRequestedAgain() {
        val cache = LightPollutionMemoryCache()
        val key = LightPollutionLocationKey(5252, 1341)
        val estimate = LightPollutionModel.estimateFromLuminance(20.0)
        val old = cache.begin(key)
        assertTrue(cache.store(old, estimate))
        cache.clear()
        assertNull(cache.get(key))
        val next = cache.begin(key)
        assertFalse(cache.isCurrent(old))
        assertFalse(cache.store(old, estimate))
        assertNull(cache.get(key))
        assertTrue(cache.store(next, estimate))
        assertEquals(estimate, cache.get(key))
    }

    @Test fun earlierDownloadCannotReplaceLatestLocationCacheButItsConsumerCanStillReceiveIt() {
        val cache = LightPollutionMemoryCache()
        val first = cache.begin(LightPollutionLocationKey(5252, 1341))
        val second = cache.begin(LightPollutionLocationKey(5000, 1000))
        val estimate = LightPollutionModel.estimateFromLuminance(20.0)
        assertTrue(cache.store(second, estimate))
        assertTrue(cache.isCurrent(first))
        assertFalse(cache.store(first, estimate))
        assertEquals(estimate, cache.get(second.key))
        assertNull(cache.get(first.key))
    }
}
