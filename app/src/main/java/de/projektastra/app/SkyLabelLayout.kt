package de.projektastra.app

internal enum class SkyLabelDensity { MINIMAL, NORMAL, RICH }

/** Order is intentional: a crowded field must keep its target and orientation readable. */
internal enum class SkyLabelKind {
    TARGET, ORIENTATION, SOLAR_SYSTEM, BRIGHT_STAR, CONSTELLATION, DEEP_SKY, MILKY_WAY, STAR
}

internal data class SkyLabelCandidate(
    val id: String,
    val text: String,
    val anchorX: Float,
    val anchorY: Float,
    val kind: SkyLabelKind,
    val textSize: Float,
    val ascent: Float,
    val descent: Float,
    val anchorGap: Float,
    val rank: Double = 0.0
)

internal data class SkyLabelBounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun overlaps(other: SkyLabelBounds, gap: Float = 0f): Boolean =
        left < other.right + gap && right + gap > other.left &&
            top < other.bottom + gap && bottom + gap > other.top
}

internal data class PlacedSkyLabel(
    val candidate: SkyLabelCandidate,
    val text: String,
    val x: Float,
    val baseline: Float,
    val bounds: SkyLabelBounds
)

/** Pixel geometry is supplied by the renderer, so density/font scaling and Android text measurement
 * remain outside this deterministic layout. No label may displace an already placed higher priority. */
internal fun layoutSkyLabels(
    candidates: List<SkyLabelCandidate>,
    width: Float,
    height: Float,
    density: SkyLabelDensity,
    padding: Float,
    separation: Float,
    measureText: (SkyLabelCandidate, String) -> Float,
    canPlace: (SkyLabelCandidate, SkyLabelBounds) -> Boolean = { _, _ -> true }
): List<PlacedSkyLabel> {
    if (!width.isFinite() || !height.isFinite() || !padding.isFinite() || !separation.isFinite() ||
        padding < 0f || separation < 0f || width <= padding * 2f || height <= padding * 2f) return emptyList()
    val allowed = when (density) {
        SkyLabelDensity.MINIMAL -> SkyLabelKind.SOLAR_SYSTEM.ordinal
        SkyLabelDensity.NORMAL -> SkyLabelKind.MILKY_WAY.ordinal
        SkyLabelDensity.RICH -> SkyLabelKind.STAR.ordinal
    }
    val limit = when (density) {
        SkyLabelDensity.MINIMAL -> 8
        SkyLabelDensity.NORMAL -> 28
        SkyLabelDensity.RICH -> 64
    }
    val placed = mutableListOf<PlacedSkyLabel>()
    var ordinaryCount = 0
    candidates.asSequence().filter { candidate ->
        candidate.kind.ordinal <= allowed && candidate.text.isNotBlank() &&
            candidate.anchorX.isFinite() && candidate.anchorY.isFinite() &&
            candidate.anchorX in 0f..width && candidate.anchorY in 0f..height &&
            candidate.ascent.isFinite() && candidate.descent.isFinite() &&
            candidate.descent > candidate.ascent && candidate.anchorGap.isFinite() && candidate.anchorGap >= 0f
    }.sortedWith(compareBy<SkyLabelCandidate> { it.kind.ordinal }.thenBy { it.rank }.thenBy { it.id })
        .distinctBy { it.id }.forEach { candidate ->
            val essential = candidate.kind <= SkyLabelKind.ORIENTATION
            if (!essential && ordinaryCount >= limit) return@forEach
            val maxWidth = width - 2f * padding
            val text = fitSkyLabel(candidate.text, maxWidth) { measureText(candidate, it) } ?: return@forEach
            val textWidth = measureText(candidate, text)
            val textHeight = candidate.descent - candidate.ascent
            if (!textWidth.isFinite() || textWidth <= 0f || textHeight > height - 2f * padding) return@forEach
            val above = candidate.anchorY - candidate.anchorGap - textHeight
            val below = candidate.anchorY + candidate.anchorGap
            val right = candidate.anchorX + candidate.anchorGap
            val left = candidate.anchorX - candidate.anchorGap - textWidth
            val centered = candidate.anchorX - textWidth / 2f
            val alternatives = listOf(
                right to above, right to below, left to above, left to below,
                centered to above, centered to below
            ).map { (x, y) ->
                val fittedX = x.coerceIn(padding, width - padding - textWidth)
                val fittedY = y.coerceIn(padding, height - padding - textHeight)
                SkyLabelBounds(fittedX, fittedY, fittedX + textWidth, fittedY + textHeight)
            }.distinct()
            val bounds = alternatives.firstOrNull { proposed ->
                placed.none { proposed.overlaps(it.bounds, separation) } && canPlace(candidate, proposed)
            } ?: return@forEach
            placed += PlacedSkyLabel(candidate, text, bounds.left, bounds.top - candidate.ascent, bounds)
            if (!essential) ordinaryCount++
        }
    return placed
}

/** Truncate on Unicode code-point boundaries, including for supplementary catalog symbols. */
private fun fitSkyLabel(text: String, maxWidth: Float, measure: (String) -> Float): String? {
    val fullWidth = measure(text)
    if (!fullWidth.isFinite()) return null
    if (fullWidth <= maxWidth) return text
    if (measure("…") > maxWidth) return null
    var low = 0
    var high = text.codePointCount(0, text.length)
    while (low < high) {
        val mid = (low + high + 1) / 2
        val width = measure(text.substring(0, text.offsetByCodePoints(0, mid)) + "…")
        if (width.isFinite() && width <= maxWidth) low = mid else high = mid - 1
    }
    return text.substring(0, text.offsetByCodePoints(0, low)) + "…"
}
