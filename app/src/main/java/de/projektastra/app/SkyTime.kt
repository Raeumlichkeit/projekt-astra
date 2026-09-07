package de.projektastra.app

import java.time.DateTimeException
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs

/**
 * In-memory sky clock. Simulated time advances only by elapsed foreground time supplied by
 * the caller, so wall-clock corrections and time spent in the background do not cause jumps.
 */
internal data class SkyTimeState(
    val instant: Instant,
    val live: Boolean = true,
    val rate: Int = 0
) {
    init {
        require(rate == 0 || rate in PLAYBACK_RATES) { "Unsupported sky playback rate: $rate" }
        require(!live || rate == 0) { "Live time cannot have a simulation playback rate" }
        require(live || instant in MIN_INSTANT..MAX_INSTANT) { "Sky simulation date is outside 1900–2100" }
    }

    /** An explicit selection is validated instead of silently changing the requested date. */
    fun at(instant: Instant): SkyTimeState {
        require(instant in MIN_INSTANT..MAX_INSTANT) { "Sky simulation date is outside 1900–2100" }
        return SkyTimeState(instant, live = false)
    }

    fun play(rate: Int): SkyTimeState {
        require(rate in PLAYBACK_RATES) { "Unsupported sky playback rate: $rate" }
        val bounded = instant.coerceIn(MIN_INSTANT, MAX_INSTANT)
        val outwardAtLimit = bounded == MIN_INSTANT && rate < 0 || bounded == MAX_INSTANT && rate > 0
        return SkyTimeState(bounded, live = false, rate = if (outwardAtLimit) 0 else rate)
    }

    fun pause(): SkyTimeState = SkyTimeState(instant.coerceIn(MIN_INSTANT, MAX_INSTANT), live = false)

    fun resumeLive(now: Instant): SkyTimeState = SkyTimeState(now)

    fun tick(now: Instant, elapsedMillis: Long): SkyTimeState {
        if (live) return SkyTimeState(now)
        if (rate == 0 || elapsedMillis <= 0) return this

        val limit = if (rate > 0) MAX_INSTANT else MIN_INSTANT
        // The 201-year simulation interval fits into a Long in nanoseconds. Compare the
        // elapsed time against the remaining interval before multiplication to also handle
        // Long.MAX_VALUE ticks without overflow or loss of sub-millisecond precision.
        val remainingNanos = if (rate > 0) Duration.between(instant, limit).toNanos()
            else Duration.between(limit, instant).toNanos()
        val nanosPerMillisecond = rate.toLong() * 1_000_000L
        if (elapsedMillis > remainingNanos / abs(nanosPerMillisecond)) {
            return SkyTimeState(limit, live = false)
        }
        val next = instant.plusNanos(elapsedMillis * nanosPerMillisecond)
        return copy(instant = next, rate = if (next == limit) 0 else rate)
    }

    companion object {
        val MIN_INSTANT: Instant = Instant.parse("1900-01-01T00:00:00Z")
        val MAX_INSTANT: Instant = Instant.parse("2100-12-31T23:59:59Z")
        val PLAYBACK_RATES: List<Int> = listOf(-3600, -600, -60, -1, 1, 60, 600, 3600)
    }
}

/**
 * Resolves a local picker value without java.time's automatic shift across a DST gap.
 * For an autumn overlap, the first occurrence is the default; the UI can choose the second.
 */
internal fun resolveSkyLocalTime(
    local: LocalDateTime,
    zone: ZoneId,
    laterOffset: Boolean = false
): Instant {
    val offsets = zone.rules.getValidOffsets(local)
    if (offsets.isEmpty()) throw DateTimeException("Diese Ortszeit existiert wegen der Zeitumstellung nicht.")
    return local.toInstant(if (laterOffset) offsets.last() else offsets.first())
}
