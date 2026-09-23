package de.projektastra.app.coordinates

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import de.projektastra.app.HorizontalCoordinates
import de.projektastra.app.SkyCoordinateFrame
import de.projektastra.app.SkyProjection
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Type of astronomical grid line or reference line.
 */
enum class GridLineType {
    RA_HOUR,
    DEC_PARALLEL,
    AZIMUTH,
    ALTITUDE,
    CELESTIAL_EQUATOR,
    ECLIPTIC,
    GALACTIC_EQUATOR
}

/**
 * Representation of an individual grid line or reference line.
 */
internal data class SkyGridLine(
    val id: String,
    val type: GridLineType,
    val coordinateValue: Double,
    val label: String,
    val equatorialPoints: List<EquatorialCoordinates> = emptyList(),
    val horizontalPoints: List<HorizontalCoordinates> = emptyList(),
    val isPrimary: Boolean = false
)

/**
 * Container for the three major astronomical reference lines.
 */
internal data class ReferenceLines(
    val celestialEquator: SkyGridLine,
    val ecliptic: SkyGridLine,
    val galacticEquator: SkyGridLine
)

/**
 * Visual styling options for a grid line or reference line.
 */
internal data class GridLineStyle(
    val strokeColor: Color,
    val strokeWidth: Float,
    val pathEffect: PathEffect? = null,
    val alpha: Float = 1.0f
)

/**
 * LOD configuration for grid density based on current FOV.
 */
internal data class GridLodConfig(
    val stepHoursOrAzDegrees: Double,
    val stepDecOrAltDegrees: Double
)

/**
 * Renderer and generator for astronomical coordinate grids and reference lines.
 */
internal object SkyGridRenderer {
    // Obliquity of the ecliptic J2000
    const val ECLIPTIC_OBLIQUITY_DEGREES = 23.4392911

    // North Galactic Pole J2000 (IAU 1958)
    const val GALACTIC_POLE_RA_DEGREES = 192.85948
    const val GALACTIC_POLE_DEC_DEGREES = 27.12825
    const val GALACTIC_ASCENDING_NODE_DEGREES = 32.93 // theta_0

    /**
     * Determine adaptive LOD for Equatorial Grid based on horizontal FOV.
     */
    fun getEquatorialLod(fovDegrees: Double): GridLodConfig = when {
        fovDegrees > 70.0 -> GridLodConfig(stepHoursOrAzDegrees = 2.0, stepDecOrAltDegrees = 20.0)
        fovDegrees > 35.0 -> GridLodConfig(stepHoursOrAzDegrees = 1.0, stepDecOrAltDegrees = 10.0)
        fovDegrees > 15.0 -> GridLodConfig(stepHoursOrAzDegrees = 0.5, stepDecOrAltDegrees = 5.0)
        fovDegrees > 5.0  -> GridLodConfig(stepHoursOrAzDegrees = 1.0 / 6.0, stepDecOrAltDegrees = 2.0)  // 10 min, 2°
        else              -> GridLodConfig(stepHoursOrAzDegrees = 1.0 / 30.0, stepDecOrAltDegrees = 0.5) // 2 min, 30'
    }

    /**
     * Determine adaptive LOD for Horizontal Grid based on horizontal FOV.
     */
    fun getHorizontalLod(fovDegrees: Double): GridLodConfig = when {
        fovDegrees > 70.0 -> GridLodConfig(stepHoursOrAzDegrees = 30.0, stepDecOrAltDegrees = 20.0)
        fovDegrees > 35.0 -> GridLodConfig(stepHoursOrAzDegrees = 15.0, stepDecOrAltDegrees = 10.0)
        fovDegrees > 15.0 -> GridLodConfig(stepHoursOrAzDegrees = 5.0,  stepDecOrAltDegrees = 5.0)
        fovDegrees > 5.0  -> GridLodConfig(stepHoursOrAzDegrees = 2.0,  stepDecOrAltDegrees = 2.0)
        else              -> GridLodConfig(stepHoursOrAzDegrees = 0.5,  stepDecOrAltDegrees = 0.5)
    }

    /**
     * Generates True Equatorial Grid lines (RA hour circles and Dec parallels)
     * in EquatorialCoordinates.
     */
    fun generateEquatorialGrid(
        stepHours: Double = 2.0,
        stepDegrees: Double = 20.0,
        sampleStepDegrees: Double = 3.0
    ): List<SkyGridLine> {
        val lines = mutableListOf<SkyGridLine>()

        // 1. RA Hour Circles
        val raCount = (24.0 / stepHours).toInt()
        for (i in 0 until raCount) {
            val ra = i * stepHours
            val points = mutableListOf<EquatorialCoordinates>()
            var dec = -80.0
            while (dec < 80.0) {
                points.add(EquatorialCoordinates(ra, dec))
                dec += sampleStepDegrees
            }
            points.add(EquatorialCoordinates(ra, 80.0))
            val isColure = (ra % 6.0 == 0.0)
            val labelText = if (stepHours < 1.0) {
                val totalMinutes = (ra * 60.0).toInt()
                "${totalMinutes / 60}h ${totalMinutes % 60}m"
            } else {
                "${ra.toInt()}h"
            }
            lines.add(
                SkyGridLine(
                    id = "ra_${ra}",
                    type = GridLineType.RA_HOUR,
                    coordinateValue = ra,
                    label = labelText,
                    equatorialPoints = points,
                    isPrimary = isColure
                )
            )
        }

        // 2. Dec Parallels
        var dec = -80.0
        while (dec <= 80.001) {
            val points = mutableListOf<EquatorialCoordinates>()
            var ra = 0.0
            val raStep = sampleStepDegrees / 15.0
            while (ra < 24.0) {
                points.add(EquatorialCoordinates(ra.coerceIn(0.0, 24.0), dec))
                ra += raStep
            }
            points.add(EquatorialCoordinates(24.0, dec))
            val isEquator = (dec == 0.0)
            val prefix = if (dec > 0) "+" else ""
            lines.add(
                SkyGridLine(
                    id = "dec_${dec}",
                    type = GridLineType.DEC_PARALLEL,
                    coordinateValue = dec,
                    label = "$prefix${dec.toInt()}°",
                    equatorialPoints = points,
                    isPrimary = isEquator
                )
            )
            dec += stepDegrees
        }

        return lines
    }

    /**
     * Generates Horizontal Grid lines (Azimuth lines and Altitude circles)
     * directly in HorizontalCoordinates.
     */
    fun generateHorizontalGrid(
        stepAzDegrees: Double = 30.0,
        stepAltDegrees: Double = 20.0,
        sampleStepDegrees: Double = 3.0
    ): List<SkyGridLine> {
        val lines = mutableListOf<SkyGridLine>()

        // 1. Azimuth Lines
        var az = 0.0
        while (az < 360.0) {
            val points = mutableListOf<HorizontalCoordinates>()
            var alt = 0.0
            while (alt < 80.0) {
                points.add(HorizontalCoordinates(az, alt))
                alt += sampleStepDegrees
            }
            points.add(HorizontalCoordinates(az, 80.0))
            val isCardinal = (az % 90.0 == 0.0)
            val labelText = when (az.toInt()) {
                0 -> "0° (N)"
                90 -> "90° (O)"
                180 -> "180° (S)"
                270 -> "270° (W)"
                else -> "${az.toInt()}°"
            }
            lines.add(
                SkyGridLine(
                    id = "az_${az}",
                    type = GridLineType.AZIMUTH,
                    coordinateValue = az,
                    label = labelText,
                    horizontalPoints = points,
                    isPrimary = isCardinal
                )
            )
            az += stepAzDegrees
        }

        // 2. Altitude Circles
        var alt = 0.0
        while (alt <= 80.001) {
            val points = mutableListOf<HorizontalCoordinates>()
            var currAz = 0.0
            while (currAz < 360.0) {
                points.add(HorizontalCoordinates(currAz % 360.0, alt))
                currAz += sampleStepDegrees
            }
            points.add(HorizontalCoordinates(360.0, alt))
            lines.add(
                SkyGridLine(
                    id = "alt_${alt}",
                    type = GridLineType.ALTITUDE,
                    coordinateValue = alt,
                    label = "${alt.toInt()}°",
                    horizontalPoints = points,
                    isPrimary = (alt == 0.0 || alt == 30.0 || alt == 60.0)
                )
            )
            alt += stepAltDegrees
        }

        return lines
    }

    /**
     * Generates the three fundamental astronomical reference lines:
     * 1. Celestial Equator (Dec = 0°)
     * 2. Ecliptic (Earth's orbital plane)
     * 3. Galactic Equator (Milky Way plane)
     */
    fun generateReferenceLines(sampleStepDegrees: Double = 2.0): ReferenceLines {
        // 1. Celestial Equator
        val equatorPoints = mutableListOf<EquatorialCoordinates>()
        var ra = 0.0
        val raStep = sampleStepDegrees / 15.0
        while (ra < 24.0) {
            equatorPoints.add(EquatorialCoordinates(ra.coerceIn(0.0, 24.0), 0.0))
            ra += raStep
        }
        equatorPoints.add(EquatorialCoordinates(24.0, 0.0))
        val celestialEquator = SkyGridLine(
            id = "ref_celestial_equator",
            type = GridLineType.CELESTIAL_EQUATOR,
            coordinateValue = 0.0,
            label = "Himmelsäquator",
            equatorialPoints = equatorPoints,
            isPrimary = true
        )

        // 2. Ecliptic
        val eclipticPoints = mutableListOf<EquatorialCoordinates>()
        val epsRad = Math.toRadians(ECLIPTIC_OBLIQUITY_DEGREES)
        val cosEps = cos(epsRad)
        val sinEps = sin(epsRad)
        var lambda = 0.0
        while (lambda <= 360.001) {
            val lambdaRad = Math.toRadians(lambda)
            val sinLambda = sin(lambdaRad)
            val cosLambda = cos(lambdaRad)

            val x = cosLambda
            val y = cosEps * sinLambda
            val z = sinEps * sinLambda

            val dec = Math.toDegrees(asin(z.coerceIn(-1.0, 1.0)))
            val raDeg = (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
            eclipticPoints.add(EquatorialCoordinates(raDeg / 15.0, dec))
            lambda += sampleStepDegrees
        }
        val ecliptic = SkyGridLine(
            id = "ref_ecliptic",
            type = GridLineType.ECLIPTIC,
            coordinateValue = ECLIPTIC_OBLIQUITY_DEGREES,
            label = "Ekliptik",
            equatorialPoints = eclipticPoints,
            isPrimary = true
        )

        // 3. Galactic Equator
        val galacticPoints = mutableListOf<EquatorialCoordinates>()
        val ngpRaRad = Math.toRadians(GALACTIC_POLE_RA_DEGREES)
        val ngpDecRad = Math.toRadians(GALACTIC_POLE_DEC_DEGREES)
        val nodeThetaRad = Math.toRadians(GALACTIC_ASCENDING_NODE_DEGREES)

        val nodeRaRad = ngpRaRad + (PI / 2.0)
        val uX = cos(nodeRaRad)
        val uY = sin(nodeRaRad)
        val uZ = 0.0

        val vX = -sin(ngpDecRad) * sin(nodeRaRad)
        val vY = sin(ngpDecRad) * cos(nodeRaRad)
        val vZ = cos(ngpDecRad)

        var galL = 0.0
        while (galL <= 360.001) {
            val omega = Math.toRadians(galL) - nodeThetaRad
            val cosW = cos(omega)
            val sinW = sin(omega)

            val rX = cosW * uX + sinW * vX
            val rY = cosW * uY + sinW * vY
            val rZ = cosW * uZ + sinW * vZ

            val dec = Math.toDegrees(asin(rZ.coerceIn(-1.0, 1.0)))
            val raDeg = (Math.toDegrees(atan2(rY, rX)) + 360.0) % 360.0
            galacticPoints.add(EquatorialCoordinates(raDeg / 15.0, dec))
            galL += sampleStepDegrees
        }
        val galacticEquator = SkyGridLine(
            id = "ref_galactic_equator",
            type = GridLineType.GALACTIC_EQUATOR,
            coordinateValue = 0.0,
            label = "Galaktischer Äquator",
            equatorialPoints = galacticPoints,
            isPrimary = true
        )

        return ReferenceLines(
            celestialEquator = celestialEquator,
            ecliptic = ecliptic,
            galacticEquator = galacticEquator
        )
    }

    /**
     * Converts an Equatorial grid line to Horizontal coordinates using the observer's frame.
     */
    fun toHorizontal(line: SkyGridLine, frame: SkyCoordinateFrame): SkyGridLine {
        val converted = line.equatorialPoints.map { pt ->
            frame.horizontal(pt.raHours, pt.decDegrees)
        }
        return line.copy(horizontalPoints = converted)
    }

    /**
     * Converts a list of Equatorial grid lines to Horizontal coordinates.
     */
    fun toHorizontal(lines: List<SkyGridLine>, frame: SkyCoordinateFrame): List<SkyGridLine> {
        return lines.map { toHorizontal(it, frame) }
    }

    /**
     * Resolves appropriate visual style for a given grid line type and mode.
     */
    fun getLineStyle(type: GridLineType, isPrimary: Boolean, redLightMode: Boolean): GridLineStyle {
        if (redLightMode) {
            return when (type) {
                GridLineType.RA_HOUR, GridLineType.DEC_PARALLEL -> GridLineStyle(
                    strokeColor = Color(0xFFFF5252),
                    strokeWidth = if (isPrimary) 1.2f else 0.8f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)),
                    alpha = if (isPrimary) 0.28f else 0.16f
                )
                GridLineType.AZIMUTH, GridLineType.ALTITUDE -> GridLineStyle(
                    strokeColor = Color(0xFFD32F2F),
                    strokeWidth = if (isPrimary) 1.2f else 0.8f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                    alpha = if (isPrimary) 0.25f else 0.14f
                )
                GridLineType.CELESTIAL_EQUATOR -> GridLineStyle(
                    strokeColor = Color(0xFFFF5252),
                    strokeWidth = 1.8f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 8f)),
                    alpha = 0.85f
                )
                GridLineType.ECLIPTIC -> GridLineStyle(
                    strokeColor = Color(0xFFFF7868),
                    strokeWidth = 1.8f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f, 3f, 6f)),
                    alpha = 0.90f
                )
                GridLineType.GALACTIC_EQUATOR -> GridLineStyle(
                    strokeColor = Color(0xFFFF1744),
                    strokeWidth = 1.8f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f)),
                    alpha = 0.85f
                )
            }
        } else {
            return when (type) {
                GridLineType.RA_HOUR, GridLineType.DEC_PARALLEL -> GridLineStyle(
                    strokeColor = Color(0xFF4FC3F7),
                    strokeWidth = if (isPrimary) 1.2f else 0.8f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)),
                    alpha = if (isPrimary) 0.28f else 0.16f
                )
                GridLineType.AZIMUTH, GridLineType.ALTITUDE -> GridLineStyle(
                    strokeColor = Color(0xFFFFD54F),
                    strokeWidth = if (isPrimary) 1.2f else 0.8f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                    alpha = if (isPrimary) 0.25f else 0.14f
                )
                GridLineType.CELESTIAL_EQUATOR -> GridLineStyle(
                    strokeColor = Color(0xFF29B6F6),
                    strokeWidth = 1.8f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 8f)),
                    alpha = 0.85f
                )
                GridLineType.ECLIPTIC -> GridLineStyle(
                    strokeColor = Color(0xFFFFCA28),
                    strokeWidth = 1.8f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f, 3f, 6f)),
                    alpha = 0.90f
                )
                GridLineType.GALACTIC_EQUATOR -> GridLineStyle(
                    strokeColor = Color(0xFFCE93D8),
                    strokeWidth = 1.8f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f)),
                    alpha = 0.85f
                )
            }
        }
    }

    /**
     * Renders an individual grid line onto a Compose Canvas DrawScope with projection clipping.
     */
    fun renderGridLine(
        drawScope: DrawScope,
        line: SkyGridLine,
        projection: SkyProjection,
        style: GridLineStyle
    ) {
        val points = line.horizontalPoints
        if (points.size < 2) return

        val path = Path()
        var previousEnd: Offset? = null
        points.zipWithNext().forEach { (from, to) ->
            val segments = projection.segments(from, to, padding = 1f)
            if (segments.isEmpty()) previousEnd = null
            segments.forEach { segment ->
                if (previousEnd?.let { (it - segment.start).getDistance() < 0.25f } != true) {
                    path.moveTo(segment.start.x, segment.start.y)
                }
                path.lineTo(segment.end.x, segment.end.y)
                previousEnd = segment.end
            }
        }

        drawScope.drawPath(
            path = path,
            color = style.strokeColor.copy(alpha = style.alpha),
            style = Stroke(
                width = style.strokeWidth,
                pathEffect = style.pathEffect,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
