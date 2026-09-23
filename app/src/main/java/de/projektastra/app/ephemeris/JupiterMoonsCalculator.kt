package de.projektastra.app.ephemeris

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.C_AUDAY
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.Vector
import io.github.cosinekitty.astronomy.geoVector
import io.github.cosinekitty.astronomy.helioVector
import io.github.cosinekitty.astronomy.jupiterMoons
import io.github.cosinekitty.astronomy.rotationAxis
import java.time.Instant
import kotlin.math.max
import kotlin.math.sqrt

internal enum class JupiterMoonEvent(val label: String) {
    NONE("Kein Ereignis"),
    TRANSIT("Mondtransit vor Planetenscheibe"),
    SHADOW_TRANSIT("Schattentransit auf Wolkendecke"),
    OCCULTATION("Okkultation hinter Planetenscheibe"),
    ECLIPSE("Verfinsterung im Jupiterschatten")
}

internal data class MoonState(
    val name: String,
    val offsetRJ: Double,                   // Offset along Jupiter's equator in Jupiter equatorial radii (RJ)
    val zAU: Double,                        // Line-of-sight distance relative to Jupiter (+ = behind, - = in front)
    val event: JupiterMoonEvent,            // Primary event classification
    val shadowOffsetRJ: Double? = null,     // Projected offset of shadow on cloud deck in RJ
    val isShadowTransiting: Boolean = false,// True if shadow falls on visible disk
    val isInEclipse: Boolean = false,       // True if moon is immersed in Jupiter's umbral shadow
    val xArcsec: Double = 0.0,              // Projected offset in sky plane East/West (arcsec)
    val yArcsec: Double = 0.0,              // Projected offset in sky plane North/South (arcsec)
    val shadowXArcsec: Double? = null,
    val shadowYArcsec: Double? = null
)

internal data class JupiterSystemState(
    val time: Instant,
    val distanceAu: Double,
    val angularDiameterArcsec: Double,
    val moons: List<MoonState>
) {
    val io: MoonState get() = moons[0]
    val europa: MoonState get() = moons[1]
    val ganymede: MoonState get() = moons[2]
    val callisto: MoonState get() = moons[3]

    val activeEvents: List<Pair<MoonState, JupiterMoonEvent>>
        get() = moons.filter { it.event != JupiterMoonEvent.NONE }.map { it to it.event }
}

internal object JupiterMoonsCalculator {
    private const val KM_PER_AU = 149597870.696
    private const val RJ_EQ_KM = 71492.0
    private const val RJ_EQ_AU = RJ_EQ_KM / KM_PER_AU
    private const val R_SUN_KM = 696340.0
    private const val R_SUN_AU = R_SUN_KM / KM_PER_AU
    private const val ARCSEC_PER_RAD = 206264.80624709636

    fun calculate(time: Instant): JupiterSystemState {
        val astroTime = Time.fromMillisecondsSince1970(time.toEpochMilli())
        // geoVector is already backdated for Jupiter's light travel time.
        // jupiterMoons is geometric; its documented caller must backdate explicitly.
        // https://github.com/cosinekitty/astronomy/blob/v2.1.19/source/kotlin/src/main/kotlin/io/github/cosinekitty/astronomy/astronomy.kt
        val rJup = geoVector(Body.Jupiter, astroTime, Aberration.Corrected)
        val delta = rJup.length()
        val emissionMillis = time.toEpochMilli() - (delta / C_AUDAY * 86_400_000.0).toLong()
        val emissionTime = Time.fromMillisecondsSince1970(emissionMillis)
        val moonsInfo = jupiterMoons(emissionTime)

        // 1. Line-of-sight from Earth to Jupiter
        // Align Vector.t with the other emission-time vectors; its spatial
        // components still describe the observed Earth -> Jupiter direction.
        val eLos = Vector(rJup.x / delta, rJup.y / delta, rJup.z / delta, emissionTime)

        // 2. Vector Sun -> Jupiter (shadow cone axis)
        val rSunJup = helioVector(Body.Jupiter, emissionTime)
        val rHelio = rSunJup.length()
        val sAxis = rSunJup.div(rHelio) // Unit vector Sun -> Jupiter

        // 3. Jupiter orientation on sky plane
        val axisInfo = rotationAxis(Body.Jupiter, emissionTime)
        val north = axisInfo.north
        val northDotLos = north.dot(eLos)
        val northProj = north.minus(scale(eLos, northDotLos))
        val northProjLen = northProj.length()
        val uY = if (northProjLen > 1e-6) northProj.div(northProjLen) else Vector(0.0, 1.0, 0.0, emissionTime)
        val uX = cross(uY, eLos)

        val rjArcsec = (RJ_EQ_AU / delta) * ARCSEC_PER_RAD

        val rawMoons = listOf(
            "Io" to moonsInfo.io,
            "Europa" to moonsInfo.europa,
            "Ganymede" to moonsInfo.ganymede,
            "Callisto" to moonsInfo.callisto
        )

        val moonStates = rawMoons.map { (name, state) ->
            val pos = state.position()

            val zLos = pos.dot(eLos)
            val xAu = pos.dot(uX)
            val yAu = pos.dot(uY)
            val offsetRJ = xAu / RJ_EQ_AU
            val dPerpObs = sqrt(xAu * xAu + yAu * yAu)

            // Moon inside Jupiter's shadow cone (Eclipse)
            val sBehind = pos.dot(sAxis)
            val dPerpShadow = sqrt(max(0.0, pos.dot(pos) - sBehind * sBehind))
            val umbraRadiusAu = RJ_EQ_AU - sBehind * (R_SUN_AU - RJ_EQ_AU) / rHelio
            val isInEclipse = sBehind > 0.0 && dPerpShadow <= umbraRadiusAu

            // Moon's shadow on Jupiter's cloud deck (Shadow Transit)
            val b = pos.dot(sAxis)
            val c = pos.dot(pos) - RJ_EQ_AU * RJ_EQ_AU
            val discr = b * b - c
            var shadowOnDisk = false
            var shadowOffsetRJ: Double? = null
            var shadowXArcsec: Double? = null
            var shadowYArcsec: Double? = null

            if (discr >= 0.0) {
                val lambda = -b - sqrt(discr)
                if (lambda > 0.0) {
                    val pShadow = pos.plus(scale(sAxis, lambda))
                    if (pShadow.dot(eLos) < 0.0) {
                        shadowOnDisk = true
                        val sX = pShadow.dot(uX)
                        val sY = pShadow.dot(uY)
                        shadowOffsetRJ = sX / RJ_EQ_AU
                        shadowXArcsec = (sX / delta) * ARCSEC_PER_RAD
                        shadowYArcsec = (sY / delta) * ARCSEC_PER_RAD
                    }
                }
            }

            val isBehindDisk = dPerpObs <= RJ_EQ_AU && zLos > 0.0
            val isInFrontOfDisk = dPerpObs <= RJ_EQ_AU && zLos < 0.0

            val event = when {
                isInFrontOfDisk -> JupiterMoonEvent.TRANSIT
                isBehindDisk -> JupiterMoonEvent.OCCULTATION
                isInEclipse -> JupiterMoonEvent.ECLIPSE
                shadowOnDisk -> JupiterMoonEvent.SHADOW_TRANSIT
                else -> JupiterMoonEvent.NONE
            }

            MoonState(
                name = name,
                offsetRJ = offsetRJ,
                zAU = zLos,
                event = event,
                shadowOffsetRJ = shadowOffsetRJ,
                isShadowTransiting = shadowOnDisk,
                isInEclipse = isInEclipse,
                xArcsec = (xAu / delta) * ARCSEC_PER_RAD,
                yArcsec = (yAu / delta) * ARCSEC_PER_RAD,
                shadowXArcsec = shadowXArcsec,
                shadowYArcsec = shadowYArcsec
            )
        }

        return JupiterSystemState(
            time = time,
            distanceAu = delta,
            angularDiameterArcsec = rjArcsec * 2.0,
            moons = moonStates
        )
    }

    private fun cross(a: Vector, b: Vector): Vector {
        return Vector(
            a.y * b.z - a.z * b.y,
            a.z * b.x - a.x * b.z,
            a.x * b.y - a.y * b.x,
            a.t
        )
    }

    private fun scale(v: Vector, s: Double): Vector = Vector(v.x * s, v.y * s, v.z * s, v.t)
}
