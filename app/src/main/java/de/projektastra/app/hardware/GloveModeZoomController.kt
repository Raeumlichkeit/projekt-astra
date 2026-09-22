package de.projektastra.app.hardware

/**
 * Controller for stepped zoom via physical hardware buttons (Volume +/-)
 * designed for winter glove mode operation.
 */
class GloveModeZoomController(
    var currentFovDegrees: Double = 60.0,
    val minFovDegrees: Double = 0.5,
    val maxFovDegrees: Double = 110.0,
    val zoomFactorPerStep: Double = 1.25
) {
    var gloveModeEnabled: Boolean = true

    /**
     * Volume UP = Zoom IN (Decrease FOV)
     */
    fun onVolumeUp(): Double {
        if (!gloveModeEnabled) return currentFovDegrees
        currentFovDegrees = (currentFovDegrees / zoomFactorPerStep).coerceAtLeast(minFovDegrees)
        return currentFovDegrees
    }

    /**
     * Volume DOWN = Zoom OUT (Increase FOV)
     */
    fun onVolumeDown(): Double {
        if (!gloveModeEnabled) return currentFovDegrees
        currentFovDegrees = (currentFovDegrees * zoomFactorPerStep).coerceAtMost(maxFovDegrees)
        return currentFovDegrees
    }
}
