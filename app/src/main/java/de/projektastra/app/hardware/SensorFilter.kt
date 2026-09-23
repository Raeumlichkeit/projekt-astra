package de.projektastra.app.hardware

/**
 * Adaptive low-pass jitter filter for AR compass and gyroscope orientation sensors.
 * Features narrower FOV damping to stabilize sightlines and ±15° manual pitch trim.
 */
class ArSensorFilter(
    private val baseAlpha: Float = 0.15f,
    var pitchTrimDegrees: Float = 0.0f
) {
    private var smoothedAzimuth: Float? = null
    private var smoothedPitch: Float? = null

    /**
     * Adaptive low-pass filter: Narrower FOV needs stronger smoothing (lower alpha)
     * to eliminate hand jitter when looking at tiny targets.
     */
    fun computeAlphaForFov(fovDegrees: Float): Float {
        // FOV 60° (wide) -> alpha ~ 0.20 (responsive)
        // FOV 5° (tele/zoomed) -> alpha ~ 0.02 (heavily damped)
        val clampedFov = fovDegrees.coerceIn(2.0f, 90.0f)
        return (baseAlpha * (clampedFov / 45.0f)).coerceIn(0.02f, 0.40f)
    }

    /**
     * Smooths raw azimuth and pitch inputs taking into account FOV and pitch trim.
     * Returns Pair(smoothedAzimuth, smoothedPitch).
     */
    fun filter(
        rawAzimuthDegrees: Float,
        rawPitchDegrees: Float,
        fovDegrees: Float
    ): Pair<Float, Float> {
        val alpha = computeAlphaForFov(fovDegrees)

        // Handle Azimuth circular wrap-around (0° <-> 360°)
        val prevAz = smoothedAzimuth ?: rawAzimuthDegrees
        var deltaAz = rawAzimuthDegrees - prevAz
        while (deltaAz > 180.0f) deltaAz -= 360.0f
        while (deltaAz < -180.0f) deltaAz += 360.0f
        val newAz = ((prevAz + alpha * deltaAz) + 360.0f) % 360.0f
        smoothedAzimuth = newAz

        // Pitch linear smoothing with pitch trim offset (clamped to ±15°) applied
        val clampedTrim = pitchTrimDegrees.coerceIn(-15.0f, 15.0f)
        val trimmedRawPitch = (rawPitchDegrees + clampedTrim).coerceIn(-90.0f, 90.0f)
        val prevPitch = smoothedPitch ?: trimmedRawPitch
        val newPitch = prevPitch + alpha * (trimmedRawPitch - prevPitch)
        smoothedPitch = newPitch

        return Pair(newAz, newPitch)
    }

    fun reset() {
        smoothedAzimuth = null
        smoothedPitch = null
    }
}
