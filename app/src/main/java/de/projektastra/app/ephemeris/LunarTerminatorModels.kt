package de.projektastra.app.ephemeris

import java.time.Instant

/**
 * Classification of lunar surface formations.
 */
internal enum class LunarFeatureType(val label: String) {
    CRATER("Krater"),
    RILLE("Rille / Graben"),
    MARE("Mondmeer"),
    MOUNTAIN("Berg / Gebirge"),
    SCARP("Verwerfung / Geländestufe"),
    SPECIAL("Sonderformation")
}

/**
 * Illumination condition relative to local horizon.
 */
internal enum class TerminatorEventType(val label: String) {
    SUNRISE("Sonnenaufgang"),
    SUNSET("Sonnenuntergang"),
    DAYLIGHT("Tagseite"),
    NIGHT("Nachtseite")
}

/**
 * A prominent lunar feature defined by IAU selenographic coordinates.
 */
internal data class LunarFeature(
    val id: String,
    val name: String,
    val type: LunarFeatureType,
    val selenographicLat: Double,       // [-90..+90] degrees (+ = North)
    val selenographicLon: Double,       // [-180..+180] degrees (+ = East / Mare Crisium)
    val diameterKm: Double,
    val description: String,
    val observationTip: String = ""
)

/**
 * Evaluates Earth visibility and projects feature onto apparent 2D lunar disc.
 */
internal data class EarthVisibility(
    val isVisibleFromEarth: Boolean,
    val foreshortening: Double,         // cos(psi): 1.0 = face-on, 0.0 = limb
    val diskX: Double,                  // [-1..1] on apparent disk (+ = East)
    val diskY: Double                   // [-1..1] on apparent disk (+ = North)
)

/**
 * Complete orientation, illumination, and terminator state of the Moon.
 */
internal data class LunarTerminatorState(
    val instant: Instant,
    val subSolarLon: Double,            // lambda_S in degrees [-180..+180]
    val subSolarLat: Double,            // phi_S in degrees [-90..+90]
    val colongitude: Double,            // C_0 in degrees [0..360)
    val subEarthLon: Double,            // l_E in degrees (libration in longitude)
    val subEarthLat: Double,            // b_E in degrees (libration in latitude)
    val moonDistanceKm: Double,
    val moonDiameterDeg: Double,
    val phaseFraction: Double,          // 0.0 (New) .. 1.0 (Full)
    val phaseAngleDegrees: Double,      // 0..180
    val axisPositionAngle: Double       // P in degrees (rotation axis tilt)
) {
    /**
     * Exact local solar elevation angle h_sun in degrees above horizontal.
     * h_sun = 0° at terminator, > 0° illuminated, < 0° unilluminated.
     */
    fun sunElevationDegrees(lon: Double, lat: Double): Double {
        val latRad = Math.toRadians(lat)
        val lonRad = Math.toRadians(lon)
        val sLatRad = Math.toRadians(subSolarLat)
        val sLonRad = Math.toRadians(subSolarLon)
        val sinH = kotlin.math.sin(latRad) * kotlin.math.sin(sLatRad) +
            kotlin.math.cos(latRad) * kotlin.math.cos(sLatRad) * kotlin.math.cos(lonRad - sLonRad)
        return Math.toDegrees(kotlin.math.asin(sinH.coerceIn(-1.0, 1.0)))
    }

    /** Morning (sunrise) terminator selenographic longitude at specified latitude. */
    fun morningTerminatorLon(lat: Double): Double {
        val latRad = Math.toRadians(lat)
        val sLatRad = Math.toRadians(subSolarLat)
        val cosD = (-kotlin.math.tan(latRad) * kotlin.math.tan(sLatRad)).coerceIn(-1.0, 1.0)
        val delta = Math.toDegrees(kotlin.math.acos(cosD))
        return normalizeLongitude(subSolarLon - delta)
    }

    /** Evening (sunset) terminator selenographic longitude at specified latitude. */
    fun eveningTerminatorLon(lat: Double): Double {
        val latRad = Math.toRadians(lat)
        val sLatRad = Math.toRadians(subSolarLat)
        val cosD = (-kotlin.math.tan(latRad) * kotlin.math.tan(sLatRad)).coerceIn(-1.0, 1.0)
        val delta = Math.toDegrees(kotlin.math.acos(cosD))
        return normalizeLongitude(subSolarLon + delta)
    }

    /**
     * Evaluates Earth visibility and projects feature onto the apparent 2D lunar disc.
     */
    fun earthVisibility(lon: Double, lat: Double): EarthVisibility {
        val latRad = Math.toRadians(lat)
        val lonRad = Math.toRadians(lon)
        val eLatRad = Math.toRadians(subEarthLat)
        val eLonRad = Math.toRadians(subEarthLon)
        val cosPsi = kotlin.math.sin(latRad) * kotlin.math.sin(eLatRad) +
            kotlin.math.cos(latRad) * kotlin.math.cos(eLatRad) * kotlin.math.cos(lonRad - eLonRad)
        val isVisible = cosPsi > 0.0
        val diskX = kotlin.math.cos(latRad) * kotlin.math.sin(lonRad - eLonRad)
        val diskY = kotlin.math.cos(eLatRad) * kotlin.math.sin(latRad) -
            kotlin.math.sin(eLatRad) * kotlin.math.cos(latRad) * kotlin.math.cos(lonRad - eLonRad)
        return EarthVisibility(
            isVisibleFromEarth = isVisible,
            foreshortening = cosPsi.coerceIn(-1.0, 1.0),
            diskX = diskX.coerceIn(-1.0, 1.0),
            diskY = diskY.coerceIn(-1.0, 1.0)
        )
    }

    companion object {
        fun normalizeLongitude(deg: Double): Double {
            var norm = deg % 360.0
            if (norm > 180.0) norm -= 360.0
            if (norm <= -180.0) norm += 360.0
            return norm
        }
    }
}

internal data class LunarFeatureHighlight(
    val feature: LunarFeature,
    val sunElevationDegrees: Double,
    val eventType: TerminatorEventType,
    val inOptimalRelief: Boolean,        // 0.0 <= sunElevation <= 12.0 && isVisibleFromEarth
    val reliefScore: Double,             // 0.0 .. 1.0
    val distanceToTerminatorDegrees: Double,
    val isVisibleFromEarth: Boolean,
    val diskX: Double,
    val diskY: Double
)
