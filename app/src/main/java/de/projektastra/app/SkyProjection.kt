package de.projektastra.app

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

internal data class SkySegment(val start: Offset, val end: Offset)

/** Spherical perspective for the manual map; the existing AR mapping remains selectable. */
internal class SkyProjection(
    private val centerAzimuth: Double,
    private val centerAltitude: Double,
    val width: Float,
    val height: Float,
    private val horizontalFov: Double,
    val perspective: Boolean = false
) {
    private val valid = width.isFinite() && height.isFinite() && width > 0f && height > 0f &&
        centerAzimuth.isFinite() && centerAltitude.isFinite() &&
        horizontalFov.isFinite() && horizontalFov >= 1.0 && horizontalFov < 180.0
    private val pitchSin = sin(Math.toRadians(centerAltitude))
    private val pitchCos = cos(Math.toRadians(centerAltitude))
    private val focalLength = width / (2.0 * tan(Math.toRadians(horizontalFov / 2)))

    private data class Vector(val x: Double, val y: Double, val z: Double) {
        fun between(other: Vector, fraction: Double) = Vector(
            x + (other.x - x) * fraction, y + (other.y - y) * fraction, z + (other.z - z) * fraction)
    }

    private fun camera(position: HorizontalCoordinates): Vector {
        val azimuth = Math.toRadians(delta(position.azimuth - centerAzimuth))
        val altitude = Math.toRadians(position.altitude)
        val forward = cos(altitude) * cos(azimuth)
        return Vector(cos(altitude) * sin(azimuth),
            sin(altitude) * pitchCos - forward * pitchSin,
            sin(altitude) * pitchSin + forward * pitchCos)
    }

    private fun screen(vector: Vector) = Offset(
        (width / 2.0 + focalLength * vector.x / vector.z).toFloat(),
        (height / 2.0 - focalLength * vector.y / vector.z).toFloat())

    fun coordinates(point: Offset): HorizontalCoordinates? {
        if (!valid || !point.x.isFinite() || !point.y.isFinite()) return null
        if (!perspective) return HorizontalCoordinates(
            (centerAzimuth + (point.x / width - 0.5) * horizontalFov + 360.0) % 360.0,
            centerAltitude + (height / 2.0 - point.y) * horizontalFov / width)
        val right = (point.x - width / 2.0) / focalLength
        val up = (height / 2.0 - point.y) / focalLength
        val forward = pitchCos - up * pitchSin
        val vertical = pitchSin + up * pitchCos
        return HorizontalCoordinates(
            ((centerAzimuth + Math.toDegrees(atan2(right, forward))) % 360.0 + 360.0) % 360.0,
            Math.toDegrees(asin((vertical / sqrt(1.0 + right * right + up * up)).coerceIn(-1.0, 1.0))))
    }

    fun point(position: HorizontalCoordinates, padding: Float = 0f): Offset? {
        if (!valid || !position.valid() || !padding.isFinite() || padding < 0f) return null
        val point = if (perspective) {
            val vector = camera(position)
            if (vector.z <= 1e-6) return null
            screen(vector)
        } else screen(delta(position.azimuth - centerAzimuth), position.altitude)
        return point.takeIf { contains(it, padding) }
    }

    fun contains(point: Offset, padding: Float = 0f) =
        point.x in -padding..width + padding && point.y in -padding..height + padding

    /** Clip the short azimuth arc, including its wrapped copies, never a diagonal across the back of the sky. */
    fun segments(from: HorizontalCoordinates, to: HorizontalCoordinates, padding: Float = 0f): List<SkySegment> {
        if (!valid || !from.valid() || !to.valid() || !padding.isFinite() || padding < 0f) return emptyList()
        if (perspective) {
            var start = camera(from)
            var end = camera(to)
            for (plane in 0..4) {
                val a = planeDistance(start, plane, padding)
                val b = planeDistance(end, plane, padding)
                if (a < 0 && b < 0) return emptyList()
                if (a < 0 || b < 0) {
                    val intersection = start.between(end, a / (a - b))
                    if (a < 0) start = intersection else end = intersection
                }
            }
            return listOf(SkySegment(screen(start), screen(end)))
        }
        val startDelta = delta(from.azimuth - centerAzimuth)
        val endDelta = startDelta + delta(to.azimuth - from.azimuth)
        return (-1..1).mapNotNull { turn ->
            clip(screen(startDelta + turn * 360.0, from.altitude),
                screen(endDelta + turn * 360.0, to.altitude), padding)
        }
    }

    /** Clip ground triangles before perspective division, including crossings behind the camera. */
    fun polygon(points: List<HorizontalCoordinates>): List<Offset> {
        if (!valid || !perspective || points.size < 3 || points.any { !it.valid() }) return emptyList()
        var vertices = points.map(::camera)
        for (plane in 0..4) {
            if (vertices.isEmpty()) return emptyList()
            val clipped = mutableListOf<Vector>()
            var previous = vertices.last()
            var previousDistance = planeDistance(previous, plane, 0f)
            vertices.forEach { current ->
                val currentDistance = planeDistance(current, plane, 0f)
                if ((previousDistance < 0) != (currentDistance < 0)) {
                    clipped += previous.between(current, previousDistance / (previousDistance - currentDistance))
                }
                if (currentDistance >= 0) clipped += current
                previous = current
                previousDistance = currentDistance
            }
            vertices = clipped
        }
        return vertices.map(::screen)
    }

    private fun planeDistance(point: Vector, plane: Int, padding: Float): Double = when (plane) {
        0 -> point.z - 1e-6
        1 -> point.x * focalLength + (width / 2.0 + padding) * point.z
        2 -> -point.x * focalLength + (width / 2.0 + padding) * point.z
        3 -> point.y * focalLength + (height / 2.0 + padding) * point.z
        else -> -point.y * focalLength + (height / 2.0 + padding) * point.z
    }

    private fun screen(azimuthDelta: Double, altitude: Double) = Offset(
        (width * (0.5 + azimuthDelta / horizontalFov)).toFloat(),
        (height * 0.5 - (altitude - centerAltitude) * width / horizontalFov).toFloat()
    )

    // Liang–Barsky clipping: both endpoints may be off-screen while the middle is visible.
    private fun clip(start: Offset, end: Offset, padding: Float): SkySegment? {
        if (!start.x.isFinite() || !start.y.isFinite() || !end.x.isFinite() || !end.y.isFinite()) return null
        val dx = (end.x - start.x).toDouble()
        val dy = (end.y - start.y).toDouble()
        var enter = 0.0
        var leave = 1.0
        fun edge(p: Double, q: Double): Boolean {
            if (abs(p) < 1e-12) return q >= 0.0
            val ratio = q / p
            if (p < 0) enter = maxOf(enter, ratio) else leave = minOf(leave, ratio)
            return enter <= leave
        }
        if (!edge(-dx, (start.x + padding).toDouble()) ||
            !edge(dx, (width + padding - start.x).toDouble()) ||
            !edge(-dy, (start.y + padding).toDouble()) ||
            !edge(dy, (height + padding - start.y).toDouble())) return null
        return SkySegment(
            Offset((start.x + enter * dx).toFloat(), (start.y + enter * dy).toFloat()),
            Offset((start.x + leave * dx).toFloat(), (start.y + leave * dy).toFloat())
        )
    }

    private fun HorizontalCoordinates.valid() = azimuth.isFinite() && altitude.isFinite()
    private fun delta(degrees: Double) = ((degrees % 360.0 + 540.0) % 360.0) - 180.0
}
