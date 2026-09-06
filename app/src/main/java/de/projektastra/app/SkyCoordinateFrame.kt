package de.projektastra.app

import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.Vector
import io.github.cosinekitty.astronomy.rotationEqjHor
import java.time.Instant
import kotlin.math.cos
import kotlin.math.sin

/** One observer/time frame shared by J2000 catalogue objects and the photographic sky layer. */
internal class SkyCoordinateFrame(observer: GeoPoint, instant: Instant) {
    private val time = Time.fromMillisecondsSince1970(instant.toEpochMilli())
    private val j2000ToHorizontal = rotationEqjHor(
        time, Observer(observer.latitude, observer.longitude, observer.altitudeMeters)
    )

    /**
     * Column-major 3×3 matrix for `glUniformMatrix3fv(..., false, ...)`.
     * Input: local unit vector [east, north, up]. Output: J2000 equatorial [x, y, z],
     * with x toward RA 0h/Dec 0°, y toward RA 6h/Dec 0°, z toward Dec +90°.
     * Astronomy Engine's horizontal vector basis is [north, west, up], so east
     * corresponds to its negative y axis. Do not transpose or negate azimuth again.
     * No atmospheric refraction: stars, texture and terrain share geometric altitude.
     */
    val horizontalToJ2000: FloatArray = run {
        val inverse = j2000ToHorizontal.inverse()
        val east = inverse.rotate(Vector(0.0, -1.0, 0.0, time))
        val north = inverse.rotate(Vector(1.0, 0.0, 0.0, time))
        val up = inverse.rotate(Vector(0.0, 0.0, 1.0, time))
        floatArrayOf(
            east.x.toFloat(), east.y.toFloat(), east.z.toFloat(),
            north.x.toFloat(), north.y.toFloat(), north.z.toFloat(),
            up.x.toFloat(), up.y.toFloat(), up.z.toFloat()
        )
    }

    /** J2000 right ascension in hours and declination in degrees; azimuth is north=0°, east=90°. */
    fun horizontal(raHours: Double, decDegrees: Double): HorizontalCoordinates {
        val ra = Math.toRadians(raHours * 15.0)
        val dec = Math.toRadians(decDegrees)
        val cosDec = cos(dec)
        val equatorial = Vector(cosDec * cos(ra), cosDec * sin(ra), sin(dec), time)
        val position = j2000ToHorizontal.rotate(equatorial).toHorizontal(Refraction.None)
        return HorizontalCoordinates(position.lon, position.lat)
    }
}
