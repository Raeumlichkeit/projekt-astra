package de.projektastra.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class SkyTimeTest {
    private val initial = Instant.parse("2026-09-07T12:00:00Z")
    private val arbitraryWallClock = Instant.parse("2020-01-01T00:00:00Z")

    @Test fun liveFollowsWallClockAndResumeDiscardsSimulationOffset() {
        val live = SkyTimeState(initial).tick(initial.plusSeconds(7), -1)
        assertEquals(initial.plusSeconds(7), live.instant)
        assertTrue(live.live)
        val replay = live.at(Instant.parse("2000-01-01T00:00:00Z")).play(3600)
        assertEquals(SkyTimeState(initial), replay.resumeLive(initial))
    }

    @Test fun choosingDateStopsPlaybackAndPauseFreezesLiveOrSimulation() {
        val chosen = SkyTimeState(initial).play(60).at(initial.plusSeconds(300))
        assertFalse(chosen.live)
        assertEquals(0, chosen.rate)
        assertEquals(chosen, chosen.tick(initial.plusSeconds(900), 900_000))
        val paused = SkyTimeState(initial).play(-600).pause()
        assertEquals(SkyTimeState(initial, live = false), paused)
        assertEquals(paused, SkyTimeState(initial).pause())
        assertEquals(paused, paused.tick(arbitraryWallClock, Long.MAX_VALUE))
    }

    @Test fun playbackUsesElapsedTimeInsteadOfChangingSystemClock() {
        val playing = SkyTimeState(initial).play(60)
        assertEquals(initial.plusSeconds(30), playing.tick(arbitraryWallClock, 500).instant)
        assertEquals(initial.plusSeconds(30), playing.tick(Instant.MAX, 500).instant)
        assertEquals(playing, playing.tick(initial, 0))
        assertEquals(playing, playing.tick(initial, -10))
        assertEquals(playing, playing.tick(initial, Long.MIN_VALUE))
    }

    @Test fun everySupportedForwardAndReverseRatePreservesFractionalSeconds() {
        val subsecondStart = initial.plusNanos(123_456_789)
        SkyTimeState.PLAYBACK_RATES.forEach { rate ->
            val result = SkyTimeState(subsecondStart).play(rate).tick(arbitraryWallClock, 250)
            assertEquals(subsecondStart.plusNanos(rate * 250_000_000L), result.instant)
            assertEquals(rate, result.rate)
            assertFalse(result.live)
        }
    }

    @Test fun playbackCrossesUtcDayMonthLeapDayAndYearInBothDirections() {
        listOf(
            "2026-09-07T23:59:59Z" to "2026-09-08T00:00:01Z",
            "2026-04-30T23:59:59Z" to "2026-05-01T00:00:01Z",
            "2028-02-28T23:59:59Z" to "2028-02-29T00:00:01Z",
            "2028-02-29T23:59:59Z" to "2028-03-01T00:00:01Z",
            "2026-12-31T23:59:59Z" to "2027-01-01T00:00:01Z"
        ).forEach { (before, after) ->
            val first = Instant.parse(before)
            val last = Instant.parse(after)
            assertEquals(last, SkyTimeState(first).play(1).tick(arbitraryWallClock, 2_000).instant)
            assertEquals(first, SkyTimeState(last).play(-1).tick(arbitraryWallClock, 2_000).instant)
        }
    }

    @Test fun invalidRatesAndOutOfBoundsSelectionsAreRejected() {
        listOf(0, 2, -2, 100, Int.MIN_VALUE, Int.MAX_VALUE).forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) { SkyTimeState(initial).play(invalid) }
        }
        listOf(SkyTimeState.MIN_INSTANT.minusNanos(1), SkyTimeState.MAX_INSTANT.plusNanos(1)).forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) { SkyTimeState(initial).at(invalid) }
            assertThrows(IllegalArgumentException::class.java) { SkyTimeState(invalid, live = false) }
        }
        assertThrows(IllegalArgumentException::class.java) { SkyTimeState(initial, live = true, rate = 60) }
        assertThrows(IllegalArgumentException::class.java) { SkyTimeState(initial, live = false, rate = 2) }
    }

    @Test fun inclusiveLimitsStopOnExactArrivalAndCanBePlayedBackInward() {
        val upper = SkyTimeState(SkyTimeState.MAX_INSTANT.minusSeconds(60))
            .play(60).tick(arbitraryWallClock, 1_000)
        assertEquals(SkyTimeState(SkyTimeState.MAX_INSTANT, live = false), upper)
        assertEquals(0, upper.play(1).rate)
        assertEquals(SkyTimeState.MAX_INSTANT.minusSeconds(1), upper.play(-1).tick(initial, 1_000).instant)

        val lower = SkyTimeState(SkyTimeState.MIN_INSTANT.plusSeconds(60))
            .play(-60).tick(arbitraryWallClock, 1_000)
        assertEquals(SkyTimeState(SkyTimeState.MIN_INSTANT, live = false), lower)
        assertEquals(0, lower.play(-1).rate)
        assertEquals(SkyTimeState.MIN_INSTANT.plusSeconds(1), lower.play(1).tick(initial, 1_000).instant)
    }

    @Test fun overshootIncludingLongMaxElapsedClampsWithoutOverflow() {
        listOf(1, 60, 600, 3600).forEach { speed ->
            assertEquals(SkyTimeState(SkyTimeState.MAX_INSTANT, live = false),
                SkyTimeState(SkyTimeState.MIN_INSTANT).play(speed).tick(initial, Long.MAX_VALUE))
            assertEquals(SkyTimeState(SkyTimeState.MIN_INSTANT, live = false),
                SkyTimeState(SkyTimeState.MAX_INSTANT).play(-speed).tick(initial, Long.MAX_VALUE))
        }
    }

    @Test fun boundaryClampingRetainsSubMillisecondPrecisionUntilTheActualCrossing() {
        val upperStart = SkyTimeState.MAX_INSTANT.minusNanos(1_500_000)
        val upperTick = SkyTimeState(upperStart).play(1).tick(initial, 1)
        assertEquals(SkyTimeState.MAX_INSTANT.minusNanos(500_000), upperTick.instant)
        assertEquals(1, upperTick.rate)
        assertEquals(SkyTimeState(SkyTimeState.MAX_INSTANT, live = false), upperTick.tick(initial, 1))

        val lowerStart = SkyTimeState.MIN_INSTANT.plusNanos(1_500_000)
        val lowerTick = SkyTimeState(lowerStart).play(-1).tick(initial, 1)
        assertEquals(SkyTimeState.MIN_INSTANT.plusNanos(500_000), lowerTick.instant)
        assertEquals(-1, lowerTick.rate)
        assertEquals(SkyTimeState(SkyTimeState.MIN_INSTANT, live = false), lowerTick.tick(initial, 1))
    }

    @Test fun outOfRangeLiveClockRemainsUsableAndSimulationStartsAtNearestBoundary() {
        assertEquals(Instant.MAX, SkyTimeState(initial).tick(Instant.MAX, 1).instant)
        assertEquals(SkyTimeState(Instant.MIN), SkyTimeState(initial).resumeLive(Instant.MIN))
        assertEquals(SkyTimeState(SkyTimeState.MAX_INSTANT, live = false), SkyTimeState(Instant.MAX).pause())
        assertEquals(SkyTimeState(SkyTimeState.MIN_INSTANT, live = false), SkyTimeState(Instant.MIN).pause())
        assertEquals(SkyTimeState.MIN_INSTANT.plusSeconds(1),
            SkyTimeState(Instant.MIN).play(1).tick(initial, 1_000).instant)
        assertEquals(SkyTimeState.MAX_INSTANT.minusSeconds(1),
            SkyTimeState(Instant.MAX).play(-1).tick(initial, 1_000).instant)
    }

    @Test fun berlinSpringGapIsRejectedRatherThanSilentlyShifted() {
        val berlin = ZoneId.of("Europe/Berlin")
        assertThrows(DateTimeException::class.java) {
            resolveSkyLocalTime(LocalDateTime.parse("2026-03-29T02:30:00"), berlin)
        }
        assertEquals(Instant.parse("2026-03-29T00:59:59Z"),
            resolveSkyLocalTime(LocalDateTime.parse("2026-03-29T01:59:59"), berlin))
        assertEquals(Instant.parse("2026-03-29T01:00:00Z"),
            resolveSkyLocalTime(LocalDateTime.parse("2026-03-29T03:00:00"), berlin))
    }

    @Test fun berlinAutumnOverlapCanResolveBothOccurrencesExplicitly() {
        val local = LocalDateTime.parse("2026-10-25T02:30:00")
        val berlin = ZoneId.of("Europe/Berlin")
        assertEquals(Instant.parse("2026-10-25T00:30:00Z"), resolveSkyLocalTime(local, berlin))
        assertEquals(Instant.parse("2026-10-25T01:30:00Z"), resolveSkyLocalTime(local, berlin, laterOffset = true))
    }

    @Test fun explicitZonesIncludeFractionalOffsetsAndLocalDateChanges() {
        val local = LocalDateTime.parse("2026-01-01T00:15:00")
        assertEquals(Instant.parse("2026-01-01T00:15:00Z"), resolveSkyLocalTime(local, ZoneOffset.UTC))
        assertEquals(Instant.parse("2025-12-31T18:30:00Z"), resolveSkyLocalTime(local, ZoneId.of("Asia/Kathmandu")))
        assertEquals(Instant.parse("2026-01-01T10:15:00Z"), resolveSkyLocalTime(local, ZoneId.of("Pacific/Honolulu")))
        assertEquals(resolveSkyLocalTime(local, ZoneOffset.UTC), resolveSkyLocalTime(local, ZoneOffset.UTC, true))
    }

    @Test fun playbackAcrossDstTransitionsAdvancesPhysicalTimeContinuously() {
        val berlin = ZoneId.of("Europe/Berlin")
        val spring = SkyTimeState(Instant.parse("2026-03-29T00:59:59Z")).play(1).tick(initial, 1_000)
        assertEquals(LocalDateTime.parse("2026-03-29T03:00:00"), spring.instant.atZone(berlin).toLocalDateTime())
        val autumn = SkyTimeState(Instant.parse("2026-10-25T00:59:59Z")).play(1).tick(initial, 1_000)
        assertEquals(LocalDateTime.parse("2026-10-25T02:00:00"), autumn.instant.atZone(berlin).toLocalDateTime())
        assertEquals(ZoneOffset.ofHours(1), autumn.instant.atZone(berlin).offset)
    }
}
