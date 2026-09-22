package de.projektastra.app.hardware

import androidx.camera.core.Camera

/**
 * State representing current night exposure compensation.
 */
data class CameraExposureState(
    val currentStepIndex: Int,          // 0 = 0 EV, 1 = +1 EV, 2 = +2 EV, 3 = Max EV
    val evValue: Float,
    val isCompensationActive: Boolean
)

/**
 * Stepped Night Exposure Compensation Controller for Camera2 / CameraX in AR mode.
 * Brightens faint horizon silhouettes during astronomical night.
 */
class CameraExposureController(
    val maxEvSteps: Int = 4,
    val evStepSize: Float = 0.5f
) {
    var currentStep: Int = 0
        private set

    fun stepUp(): CameraExposureState {
        currentStep = (currentStep + 1).coerceAtMost(maxEvSteps)
        return getState()
    }

    fun stepDown(): CameraExposureState {
        currentStep = (currentStep - 1).coerceAtLeast(0)
        return getState()
    }

    fun reset(): CameraExposureState {
        currentStep = 0
        return getState()
    }

    fun setStep(step: Int): CameraExposureState {
        currentStep = step.coerceIn(0, maxEvSteps)
        return getState()
    }

    fun getState(): CameraExposureState = CameraExposureState(
        currentStepIndex = currentStep,
        evValue = currentStep * evStepSize,
        isCompensationActive = currentStep > 0
    )

    /**
     * Applies the current exposure compensation to the CameraX instance if supported.
     */
    fun applyToCamera(camera: Camera?): Boolean {
        if (camera == null) return false
        return runCatching {
            val exposureState = camera.cameraInfo.exposureState
            if (!exposureState.isExposureCompensationSupported) return false
            val range = exposureState.exposureCompensationRange
            // Clamp target index into device-supported range
            val targetIndex = currentStep.coerceIn(range.lower, range.upper)
            camera.cameraControl.setExposureCompensationIndex(targetIndex)
            true
        }.getOrDefault(false)
    }
}
