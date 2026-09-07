package de.projektastra.app

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class SkyClockTest {
    private var elapsed = 100L
    private var wall = Instant.parse("2026-09-07T20:00:00Z")
    private val clock = SkyClock({ wall }, { elapsed })

    @Test fun backgroundPausesSimulationWithoutCatchingUpOnReturn() {
        clock.start()
        clock.play(60)
        elapsed += 2_000
        clock.stop()
        assertEquals(wall.plusSeconds(120), clock.state.instant)
        assertEquals(0, clock.state.rate)
        val paused = clock.state.instant
        elapsed += 3_600_000
        wall = wall.plusSeconds(3600)
        clock.start()
        clock.tick()
        assertEquals(paused, clock.state.instant)
        assertFalse(clock.state.live)
    }

    @Test fun liveReturnsToWallClockAfterBackground() {
        clock.start()
        clock.stop()
        wall = wall.plusSeconds(3600)
        elapsed += 3_600_000
        clock.start()
        assertTrue(clock.state.live)
        assertEquals(wall, clock.state.instant)
    }

    @Test fun systemClockCorrectionDoesNotMoveSimulation() {
        clock.start()
        clock.play(-60)
        wall = wall.plusSeconds(7200)
        elapsed += 1_000
        clock.tick()
        assertEquals(Instant.parse("2026-09-07T19:59:00Z"), clock.state.instant)
    }

    @Test fun newDateStartsAtSelectionAndRateChangesDoNotApplyRetroactively() {
        clock.start()
        elapsed += 4_000
        val chosen = Instant.parse("2027-08-02T10:00:00Z")
        clock.select(chosen)
        clock.play(60)
        elapsed += 500
        clock.play(600)
        elapsed += 500
        clock.tick()
        assertEquals(chosen.plusSeconds(330), clock.state.instant)
        clock.now()
        assertTrue(clock.state.live)
        assertEquals(wall, clock.state.instant)
    }
}
