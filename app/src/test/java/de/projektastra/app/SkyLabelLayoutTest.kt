package de.projektastra.app

import org.junit.Assert.*
import org.junit.Test

class SkyLabelLayoutTest {
    private fun label(
        id: String, x: Float = 100f, y: Float = 80f,
        kind: SkyLabelKind = SkyLabelKind.BRIGHT_STAR, size: Float = 16f,
        text: String = id, rank: Double = 0.0
    ) = SkyLabelCandidate(id, text, x, y, kind, size, -size * 0.8f, size * 0.2f, 6f, rank)

    private fun layout(
        labels: List<SkyLabelCandidate>, width: Float = 300f, height: Float = 200f,
        density: SkyLabelDensity = SkyLabelDensity.NORMAL,
        canPlace: (SkyLabelCandidate, SkyLabelBounds) -> Boolean = { _, _ -> true }
    ) = layoutSkyLabels(labels, width, height, density, 8f, 4f,
        { candidate, text -> text.codePointCount(0, text.length) * candidate.textSize * 0.6f }, canPlace)

    private fun assertFits(labels: List<PlacedSkyLabel>, width: Float, height: Float) {
        labels.forEach { label ->
            assertTrue(label.bounds.left >= 8f && label.bounds.right <= width - 8f)
            assertTrue(label.bounds.top >= 8f && label.bounds.bottom <= height - 8f)
        }
        labels.forEachIndexed { index, label ->
            labels.drop(index + 1).forEach { other ->
                assertFalse("${label.text} overlaps ${other.text}", label.bounds.overlaps(other.bounds, 4f))
            }
        }
    }

    @Test fun selectedTargetWinsWhenOnlyOneLabelCanFit() {
        val labels = listOf(
            label("bright", 50f, 20f, text = "Sternname"),
            label("direction", 50f, 20f, SkyLabelKind.ORIENTATION, text = "Horizont"),
            label("target", 50f, 20f, SkyLabelKind.TARGET, text = "Zielname")
        )
        val result = layout(labels, 100f, 32f)
        assertEquals(listOf("target"), result.map { it.candidate.id })
        assertFits(result, 100f, 32f)
    }

    @Test fun crowdedLabelsTryAlternativeSidesBeforeBeingOmitted() {
        val result = layout(listOf(label("Vega"), label("Deneb"), label("Altair"), label("Sirius")))
        assertTrue("Several labels should survive in the same field", result.size >= 3)
        assertFits(result, 300f, 200f)
    }

    @Test fun longNamesAtEveryEdgeAreEllipsizedWithinTheViewport() {
        listOf(0f to 0f, 80f to 0f, 0f to 80f, 80f to 80f).forEach { (x, y) ->
            val result = layout(listOf(label("target", x, y, SkyLabelKind.TARGET,
                text = "Sehr langer Sternkatalogname")), 80f, 80f)
            assertEquals(1, result.size)
            assertTrue(result.single().text.endsWith("…"))
            assertFits(result, 80f, 80f)
        }
    }

    @Test fun largeFontStillPreservesTargetAndDoesNotOverlapOrClip() {
        val ordinary = (0..24).map { index ->
            label("Stern $index", 30f + index % 5 * 60f, 35f + index / 5 * 33f)
        } + label("target", 150f, 100f, SkyLabelKind.TARGET, text = "Zielstern")
        val enlarged = ordinary.map { it.copy(textSize = it.textSize * 2f, ascent = it.ascent * 2f, descent = it.descent * 2f) }
        val normalResult = layout(ordinary, 320f, 200f)
        val largeResult = layout(enlarged, 320f, 200f)
        assertEquals("target", largeResult.first().candidate.id)
        assertTrue(largeResult.size < normalResult.size)
        assertEquals(32f, largeResult.first().bounds.bottom - largeResult.first().bounds.top, 0.01f)
        assertFits(largeResult, 320f, 200f)
    }

    @Test fun densityKeepsTargetAndOrientationAndAddsFainterNamesOnlyInRichMode() {
        val labels = listOf(
            label("target", 50f, 50f, SkyLabelKind.TARGET),
            label("north", 50f, 120f, SkyLabelKind.ORIENTATION),
            label("moon", 50f, 190f, SkyLabelKind.SOLAR_SYSTEM),
            label("bright", 50f, 260f, SkyLabelKind.BRIGHT_STAR),
            label("faint", 50f, 330f, SkyLabelKind.STAR)
        )
        assertEquals(listOf("target", "north", "moon"), layout(labels, 500f, 500f,
            SkyLabelDensity.MINIMAL).map { it.candidate.id })
        assertEquals(listOf("target", "north", "moon", "bright"), layout(labels, 500f, 500f,
            SkyLabelDensity.NORMAL).map { it.candidate.id })
        assertEquals(5, layout(labels, 500f, 500f, SkyLabelDensity.RICH).size)
    }

    @Test fun priorityAndPlacementDoNotDependOnCatalogIterationOrder() {
        val labels = listOf(label("z", rank = 1.0), label("a", rank = 1.0), label("b", rank = -1.0),
            label("moon", kind = SkyLabelKind.SOLAR_SYSTEM))
        val forward = layout(labels)
        val reverse = layout(labels.reversed())
        assertEquals(forward, reverse)
        assertEquals("moon", forward.first().candidate.id)
        assertEquals("b", forward[1].candidate.id)
    }

    @Test fun blockedTerrainPlacementTriesSkySideAndNeverDrawsInBlockedArea() {
        val labels = listOf(label("target", 120f, 98f, SkyLabelKind.TARGET), label("Vega", 120f, 95f))
        val result = layout(labels, canPlace = { _, bounds -> bounds.bottom < 100f })
        assertTrue(result.any { it.candidate.kind == SkyLabelKind.TARGET })
        assertTrue(result.all { it.bounds.bottom < 100f })
        assertTrue(layout(labels, canPlace = { _, _ -> false }).isEmpty())
    }

    @Test fun offscreenAnchorsDoNotBecomeMisleadingEdgeLabels() {
        val labels = listOf(label("left", -1f), label("right", 301f),
            label("top", y = -1f), label("bottom", y = 201f), label("invalid", Float.NaN))
        assertTrue(layout(labels).isEmpty())
    }

    @Test fun tooSmallViewportAndInvalidMetricsAreSkipped() {
        assertTrue(layout(listOf(label("Vega")), height = 20f).isEmpty())
        assertTrue(layout(listOf(label("Vega")), width = Float.NaN).isEmpty())
        assertTrue(layout(listOf(label("Vega").copy(ascent = Float.NaN))).isEmpty())
    }

    @Test fun ellipsisDoesNotSplitSupplementaryUnicodeCharacters() {
        val result = layout(listOf(label("target", 20f, 30f, SkyLabelKind.TARGET,
            text = "🌟🌟🌟🌟🌟🌟")), width = 55f, height = 60f).single()
        assertEquals("🌟🌟🌟…", result.text)
        assertFits(listOf(result), 55f, 60f)
    }
}
