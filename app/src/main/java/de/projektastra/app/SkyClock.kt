package de.projektastra.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.Instant

/** Monotonic playback clock. Leaving the visible sky pauses simulation without losing the chosen time. */
internal class SkyClock(
    private val wallTime: () -> Instant,
    private val elapsedMillis: () -> Long
) {
    var state by mutableStateOf(SkyTimeState(wallTime()))
        private set
    private var active = false
    private var previousMillis = elapsedMillis()
    private var lastLiveSample = previousMillis

    fun start() {
        previousMillis = elapsedMillis()
        lastLiveSample = previousMillis
        active = true
        if (state.live) state = state.resumeLive(wallTime())
    }

    fun tick() {
        if (!active) return
        val now = elapsedMillis()
        val elapsed = (now - previousMillis).coerceAtLeast(0)
        previousMillis = now
        if (!state.live || now - lastLiveSample >= 5_000) {
            state = state.tick(wallTime(), elapsed)
            if (state.live) lastLiveSample = now
        }
    }

    fun stop() {
        tick()
        if (!state.live) state = state.pause()
        active = false
    }

    fun select(instant: Instant) {
        state = state.at(instant)
        previousMillis = elapsedMillis()
    }

    fun play(rate: Int) {
        tick()
        if (state.live) state = state.resumeLive(wallTime())
        state = state.play(rate)
        previousMillis = elapsedMillis()
    }

    fun pause() {
        tick()
        if (state.live) state = state.resumeLive(wallTime())
        state = state.pause()
    }

    fun now() {
        state = state.resumeLive(wallTime())
        previousMillis = elapsedMillis()
        lastLiveSample = previousMillis
    }
}
