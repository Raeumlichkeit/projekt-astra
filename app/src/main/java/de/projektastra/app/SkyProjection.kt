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

/** View-independent direction; build once per object/time, reuse across pan frames. */
internal class PreparedSkyPosition(val horizontal: HorizontalCoordinates) {
    val east: Double
    val north: Double
    val up: Double
    init {
        val azimuth = Math.toRadians(horizontal.azimuth)
        val altitude = Math.toRadians(horizontal.altitude)
        val cosAltitude = cos(altitude)
        east = cosAltitude * sin(azimuth)
        north = cosAltitude * cos(azimuth)
        up = sin(altitude)
    }
}

/** Conformal stereographic perspective for the manual map; the existing AR mapping remains selectable. */
internal class SkyProjection(
    private val centerAzimuth: Double,
    private val centerAltitude: Double,
    val width: Float,
    val height: Float,
    private val horizontalFov: Double,
    val perspective: Boolean = false,
    private val clipPadding: Float = 0f
) {
    private val valid = width.isFinite() && height.isFinite() && width > 0f && height > 0f &&
        centerAzimuth.isFinite() && centerAltitude.isFinite() &&
        horizontalFov.isFinite() && horizontalFov >= 0.5 && horizontalFov < 180.0 &&
        clipPadding.isFinite() && clipPadding >= 0f
    private val pitchSin = sin(Math.toRadians(centerAltitude))
    private val pitchCos = cos(Math.toRadians(centerAltitude))
    private val azimuthSin = sin(Math.toRadians(centerAzimuth))
    private val azimuthCos = cos(Math.toRadians(centerAzimuth))
    private val focalLength = width / (2.0 * tan(Math.toRadians(horizontalFov / 4)))

    private data class Vector(val x: Double, val y: Double, val z: Double) {
        fun between(other: Vector, fraction: Double) = Vector(
            x + (other.x - x) * fraction, y + (other.y - y) * fraction, z + (other.z - z) * fraction)
    }

    private fun camera(position: HorizontalCoordinates): Vector {
        val azimuth = Math.toRadians(position.azimuth - centerAzimuth)
        val altitude = Math.toRadians(position.altitude)
        val sinAltitude = sin(altitude)
        val cosAltitude = cos(altitude)
        val forward = cosAltitude * cos(azimuth)
        return Vector(cosAltitude * sin(azimuth),
            sinAltitude * pitchCos - forward * pitchSin,
            sinAltitude * pitchSin + forward * pitchCos)
    }

    private fun camera(position: PreparedSkyPosition): Vector {
        val east = position.east * azimuthCos - position.north * azimuthSin
        val forward = position.east * azimuthSin + position.north * azimuthCos
        return Vector(east,
            position.up * pitchCos - forward * pitchSin,
            position.up * pitchSin + forward * pitchCos)
    }

    private fun screen(vector: Vector): Offset {
        val w = 1.0 + vector.z
        return Offset(
            (width / 2.0 + focalLength * vector.x / w).toFloat(),
            (height / 2.0 - focalLength * vector.y / w).toFloat()
        )
    }

    fun coordinates(point: Offset): HorizontalCoordinates? {
        if (!valid || !point.x.isFinite() || !point.y.isFinite()) return null
        if (!perspective) return HorizontalCoordinates(
            (centerAzimuth + (point.x / width - 0.5) * horizontalFov + 360.0) % 360.0,
            centerAltitude + (height / 2.0 - point.y) * horizontalFov / width)
        val u = (point.x - width / 2.0) / focalLength
        val v = (height / 2.0 - point.y) / focalLength
        val r2 = u * u + v * v
        val denom = 1.0 + r2
        val x = 2.0 * u / denom
        val y = 2.0 * v / denom
        val z = (1.0 - r2) / denom
        val forward = z * pitchCos - y * pitchSin
        val vertical = z * pitchSin + y * pitchCos
        return HorizontalCoordinates(
            ((centerAzimuth + Math.toDegrees(atan2(x, forward))) % 360.0 + 360.0) % 360.0,
            Math.toDegrees(asin(vertical.coerceIn(-1.0, 1.0))))
    }

    fun point(position: HorizontalCoordinates, padding: Float = 0f): Offset? {
        if (!padding.isFinite() || padding < 0f) return null
        return projectedPoint(position)?.takeIf { contains(it, padding + clipPadding) }
    }

    fun point(position: PreparedSkyPosition, padding: Float = 0f): Offset? {
        if (!valid || !position.horizontal.valid() || !padding.isFinite() || padding < 0f) return null
        val projected = if (perspective) camera(position).let { if (it.z <= -0.999) null else screen(it) }
            else screen(delta(position.horizontal.azimuth - centerAzimuth), position.horizontal.altitude)
        return projected?.takeIf { contains(it, padding + clipPadding) }
    }

    /** Horizon contours may run beyond the viewport before re-entering it. */
    fun projectedPoint(position: HorizontalCoordinates): Offset? {
        if (!valid || !position.valid()) return null
        return if (perspective) {
            val vector = camera(position)
            if (vector.z <= -0.999) return null
            screen(vector)
        } else screen(delta(position.azimuth - centerAzimuth), position.altitude)
    }

    fun contains(point: Offset, padding: Float = 0f) =
        point.x in -padding..width + padding && point.y in -padding..height + padding

    /** Clip the short azimuth arc, including its wrapped copies, never a diagonal across the back of the sky. */
    fun segments(from: HorizontalCoordinates, to: HorizontalCoordinates, padding: Float = 0f): List<SkySegment> {
        if (!valid || !from.valid() || !to.valid() || !padding.isFinite() || padding < 0f) return emptyList()
        val extent = padding + clipPadding
        if (perspective) {
            var start = camera(from)
            var end = camera(to)
            for (plane in 0..4) {
                val a = planeDistance(start, plane, extent)
                val b = planeDistance(end, plane, extent)
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
                screen(endDelta + turn * 360.0, to.altitude), extent)
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
            var previousDistance = planeDistance(previous, plane, clipPadding)
            vertices.forEach { current ->
                val currentDistance = planeDistance(current, plane, clipPadding)
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

    private fun planeDistance(point: Vector, plane: Int, padding: Float): Double {
        val w = 1.0 + point.z
        return when (plane) {
            0 -> point.z + 0.8
            1 -> point.x * focalLength + (width / 2.0 + padding) * w
            2 -> -point.x * focalLength + (width / 2.0 + padding) * w
            3 -> point.y * focalLength + (height / 2.0 + padding) * w
            else -> -point.y * focalLength + (height / 2.0 + padding) * w
        }
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
