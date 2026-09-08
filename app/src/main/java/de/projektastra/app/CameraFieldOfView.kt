package de.projektastra.app

import kotlin.math.atan
import kotlin.math.hypot

/**
 * Visible horizontal field, after CameraX has mapped the view edges into sensor coordinates.
 * The sensor deltas already include preview rotation, stream cropping and FILL_CENTER cropping.
 * Physical dimensions describe the full pixel array, not just its active/cropped region.
 */
internal fun cameraHorizontalFovDegrees(
    sensorDeltaX: Double,
    sensorDeltaY: Double,
    physicalWidthMm: Double,
    physicalHeightMm: Double,
    pixelWidth: Int,
    pixelHeight: Int,
    focalLengthMm: Double
): Double? {
    if (!sensorDeltaX.isFinite() || !sensorDeltaY.isFinite() ||
        !physicalWidthMm.isFinite() || physicalWidthMm <= 0.0 ||
        !physicalHeightMm.isFinite() || physicalHeightMm <= 0.0 ||
        pixelWidth <= 0 || pixelHeight <= 0 ||
        !focalLengthMm.isFinite() || focalLengthMm <= 0.0) return null
    val sensorWidthMm = hypot(
        sensorDeltaX * physicalWidthMm / pixelWidth,
        sensorDeltaY * physicalHeightMm / pixelHeight
    )
    if (!sensorWidthMm.isFinite() || sensorWidthMm <= 0.0) return null
    return Math.toDegrees(2.0 * atan(sensorWidthMm / (2.0 * focalLengthMm)))
        .takeIf { it.isFinite() && it in 1.0..<180.0 }
}
