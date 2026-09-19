package de.projektastra.app

import androidx.compose.ui.geometry.Offset
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Sensor-Zuverlässigkeitsstufen für die AR-Zielhilfe.
 */
internal enum class ArSensorQuality(val label: String, val isAccurate: Boolean) {
    HIGH("Sehr genau", true),
    MEDIUM("Gut", true),
    LOW("Niedrig (Kalibrieren empfohlen)", false),
    UNRELIABLE("Ungenau (Kalibrieren)", false),
    UNAVAILABLE("Nicht verfügbar", false);

    companion object {
        fun fromSensorAccuracy(accuracy: Int, available: Boolean): ArSensorQuality {
            if (!available) return UNAVAILABLE
            return when (accuracy) {
                android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> HIGH
                android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> MEDIUM
                android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_LOW -> LOW
                else -> UNRELIABLE
            }
        }
    }
}

/**
 * Berechnete Leitdaten für ein anvisiertes Himmelsobjekt in der AR- und Kartenansicht.
 */
internal data class ArTargetGuidance(
    val targetName: String,
    val targetTypeLabel: String,
    val targetAzimuth: Double,
    val targetAltitude: Double,
    val angularDistanceDegrees: Double,
    val isBehind: Boolean,
    val isBelowHorizon: Boolean,
    val isBelowTerrain: Boolean,
    val terrainAltitudeAtTarget: Double?,
    val isInView: Boolean,
    val screenPosition: Offset?,
    val edgePosition: Offset,
    val arrowAngleDegrees: Float,
    val sensorQuality: ArSensorQuality
) {
    /**
     * Kompakte Beschreibung des Zielstatus für das UI.
     */
    val statusText: String
        get() = when {
            isBelowHorizon -> "Unter Horizont (${String.format(java.util.Locale.GERMAN, "%.1f°", targetAltitude)})"
            isBelowTerrain -> "Hinter Gelände (${String.format(java.util.Locale.GERMAN, "%.1f°", targetAltitude)} vs. ${String.format(java.util.Locale.GERMAN, "%.1f°", terrainAltitudeAtTarget ?: 0.0)})"
            isInView -> "Im Sichtfeld (${String.format(java.util.Locale.GERMAN, "%.1f°", angularDistanceDegrees)} vom Zentrum)"
            isBehind -> "Umdrehen (${String.format(java.util.Locale.GERMAN, "%.0f°", angularDistanceDegrees)} hinter dir)"
            else -> "${String.format(java.util.Locale.GERMAN, "%.1f°", angularDistanceDegrees)} entfernt"
        }
}

/**
 * Mathematische Berechnungen für die AR-Zielhilfe.
 */
internal object ArTargetGuidanceCalculator {

    /**
     * Berechnet den Großkreisabstand zwischen zwei azimutalen/altitudinalen Koordinaten in Grad.
     */
    fun angularDistance(az1: Double, alt1: Double, az2: Double, alt2: Double): Double {
        val phi1 = Math.toRadians(alt1)
        val phi2 = Math.toRadians(alt2)
        val dPhi = Math.toRadians(alt2 - alt1)
        val dLambda = Math.toRadians(deltaDegrees(az2 - az1))

        val a = sin(dPhi / 2.0).pow(2) + cos(phi1) * cos(phi2) * sin(dLambda / 2.0).pow(2)
        val c = 2.0 * asin(sqrt(a).coerceIn(0.0, 1.0))
        return Math.toDegrees(c)
    }

    /**
     * Normalisiert eine Winkeldifferenz auf das Intervall [-180, +180].
     */
    fun deltaDegrees(diff: Double): Double {
        var d = diff % 360.0
        if (d > 180.0) d -= 360.0
        if (d <= -180.0) d += 360.0
        return d
    }

    /**
     * Projiziert die 3D-Richtung des Ziels relativ zur aktuellen Blickrichtung der Kamera.
     * Rückgabe: Vector3D(right, up, depth)
     */
    data class RelativeVector(val right: Double, val up: Double, val depth: Double)

    fun relativeCameraVector(
        currentAzimuth: Double,
        currentAltitude: Double,
        targetAzimuth: Double,
        targetAltitude: Double
    ): RelativeVector {
        val pitchSin = sin(Math.toRadians(currentAltitude))
        val pitchCos = cos(Math.toRadians(currentAltitude))
        val azRel = Math.toRadians(deltaDegrees(targetAzimuth - currentAzimuth))
        val altTarget = Math.toRadians(targetAltitude)

        val forward = cos(altTarget) * cos(azRel)
        val right = cos(altTarget) * sin(azRel)
        val up = sin(altTarget) * pitchCos - forward * pitchSin
        val depth = sin(altTarget) * pitchSin + forward * pitchCos

        return RelativeVector(right, up, depth)
    }

    /**
     * Berechnet den Rotationswinkel des Führungspfeils auf dem Bildschirm in Grad.
     * 0° = Oben, 90° = Rechts, 180° = Unten, 270° = Links.
     */
    fun arrowAngle(right: Double, up: Double): Float {
        val dx = right
        val dy = -up // Bildschirm-Y wächst nach unten
        val angleRad = atan2(dy, dx)
        var angleDeg = Math.toDegrees(angleRad) + 90.0
        if (angleDeg < 0.0) angleDeg += 360.0
        return (angleDeg % 360.0).toFloat()
    }

    /**
     * Berechnet die Position eines Zeigers am Bildschirmrand für Ziele außerhalb des Sichtfelds.
     */
    fun edgePosition(
        right: Double,
        up: Double,
        width: Float,
        height: Float,
        padding: Float = 48f
    ): Offset {
        val cx = width / 2f
        val cy = height / 2f
        val dx = right.toFloat()
        val dy = (-up).toFloat()

        if (dx == 0f && dy == 0f) return Offset(cx, padding)

        val maxX = (cx - padding).coerceAtLeast(10f)
        val maxY = (cy - padding).coerceAtLeast(10f)

        val tx = if (dx != 0f) kotlin.math.abs(maxX / dx) else Float.MAX_VALUE
        val ty = if (dy != 0f) kotlin.math.abs(maxY / dy) else Float.MAX_VALUE
        val t = min(tx, ty)

        val targetX = (cx + dx * t).coerceIn(padding, width - padding)
        val targetY = (cy + dy * t).coerceIn(padding, height - padding)
        return Offset(targetX, targetY)
    }

    /**
     * Führt die vollständige Auswertung der Leitdaten durch.
     */
    fun calculateGuidance(
        targetName: String,
        targetTypeLabel: String,
        targetPosition: HorizontalCoordinates,
        currentAzimuth: Double,
        currentAltitude: Double,
        horizontalFov: Double,
        width: Float,
        height: Float,
        terrainProfile: TerrainProfile?,
        sensorAccuracy: Int,
        sensorAvailable: Boolean
    ): ArTargetGuidance {
        val dist = angularDistance(currentAzimuth, currentAltitude, targetPosition.azimuth, targetPosition.altitude)
        val rel = relativeCameraVector(currentAzimuth, currentAltitude, targetPosition.azimuth, targetPosition.altitude)
        val isBehind = rel.depth <= 0.0 || dist > 90.0

        val isBelowHorizon = targetPosition.altitude < 0.0
        val terrainAlt = terrainProfile?.altitudeAt(targetPosition.azimuth)
        val isBelowTerrain = terrainAlt != null && targetPosition.altitude < terrainAlt

        val arrowAngle = arrowAngle(rel.right, rel.up)
        val edgePos = edgePosition(rel.right, rel.up, width, height)

        val focalLength = (width / (2.0 * kotlin.math.tan(Math.toRadians(horizontalFov / 2.0)))).toFloat()
        var screenPos: Offset? = null
        var inView = false

        if (rel.depth > 0.001) {
            val sx = (width / 2f + focalLength * (rel.right / rel.depth).toFloat())
            val sy = (height / 2f - focalLength * (rel.up / rel.depth).toFloat())
            if (sx in 0f..width && sy in 0f..height) {
                screenPos = Offset(sx, sy)
                inView = true
            }
        }

        val quality = ArSensorQuality.fromSensorAccuracy(sensorAccuracy, sensorAvailable)

        return ArTargetGuidance(
            targetName = targetName,
            targetTypeLabel = targetTypeLabel,
            targetAzimuth = targetPosition.azimuth,
            targetAltitude = targetPosition.altitude,
            angularDistanceDegrees = dist,
            isBehind = isBehind,
            isBelowHorizon = isBelowHorizon,
            isBelowTerrain = isBelowTerrain,
            terrainAltitudeAtTarget = terrainAlt,
            isInView = inView,
            screenPosition = screenPos,
            edgePosition = edgePos,
            arrowAngleDegrees = arrowAngle,
            sensorQuality = quality
        )
    }
}
