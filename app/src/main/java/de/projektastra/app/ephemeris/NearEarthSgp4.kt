package de.projektastra.app.ephemeris

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * Near-Earth branch adapted from satellite.js 6.0.0, propagation/initl.ts,
 * sgp4init.ts and sgp4.ts:
 * https://github.com/shashwatak/satellite-js/tree/6.0.0/src/propagation
 * MIT License, Copyright (C) 2013 Shashwat Kandadai, UCSC Jack Baskin School of Engineering.
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
internal class NearEarthSgp4(tle: TleData) {
    private companion object {
        const val R = 6378.135
        const val J2 = 0.001082616
        const val J3 = -0.00000253881
        const val J4 = -0.00000165597
        val XKE = 60.0 / sqrt(R * R * R / 398600.8)
        val J3OJ2 = J3 / J2
    }

    private val e0 = tle.eccentricity
    private val i0 = Math.toRadians(tle.inclinationDegrees)
    private val node0 = Math.toRadians(tle.raanDegrees)
    private val arg0 = Math.toRadians(tle.argumentOfPerigeeDegrees)
    private val m0 = Math.toRadians(tle.meanAnomalyDegrees)
    private val bstar = tle.bstarDrag
    private val n: Double
    private val c1: Double
    private val c4: Double
    private val c5: Double
    private val eta: Double
    private val mdot: Double
    private val argdot: Double
    private val nodedot: Double
    private val omgcof: Double
    private val xmcof: Double
    private val nodecf: Double
    private val t2cof: Double
    private val xlcof: Double
    private val aycof: Double
    private val delmo: Double
    private val sinm0: Double
    private val con41: Double
    private val x1mth2: Double
    private val x7thm1: Double
    private val simple: Boolean
    private val d2: Double
    private val d3: Double
    private val d4: Double
    private val t3cof: Double
    private val t4cof: Double
    private val t5cof: Double

    init {
        require(!tle.isDeepSpace) { "Deep-space SDP4 input is unsupported" }
        val n0 = tle.meanMotionRadPerMin
        require(n0.isFinite() && n0 > 0.0 && e0.isFinite() && e0 >= 0.0 && e0 < 1.0 && bstar.isFinite()) {
            "Invalid SGP4 mean elements"
        }
        val cosi = cos(i0)
        val sini = sin(i0)
        val cosi2 = cosi * cosi
        val omeosq = 1.0 - e0 * e0
        val rteosq = sqrt(omeosq)
        val ak = (XKE / n0).pow(2.0 / 3.0)
        val d1 = 0.75 * J2 * (3.0 * cosi2 - 1.0) / (rteosq * omeosq)
        var del = d1 / (ak * ak)
        val adel = ak * (1.0 - del * del - del * (1.0 / 3.0 + 134.0 * del * del / 81.0))
        del = d1 / (adel * adel)
        n = n0 / (1.0 + del)
        require(2.0 * PI / n < 225.0) { "Deep-space SDP4 input is unsupported" }
        val ao = (XKE / n).pow(2.0 / 3.0)
        val po = ao * omeosq
        val pinvsq = 1.0 / (po * po)
        val rp = ao * (1.0 - e0)
        simple = rp < 1.0 + 220.0 / R
        val perige = (rp - 1.0) * R
        val sfourKm = if (perige < 98.0) 20.0 else if (perige < 156.0) perige - 78.0 else 78.0
        val sfour = 1.0 + sfourKm / R
        val qzms24 = ((120.0 - sfourKm) / R).pow(4.0)
        val tsi = 1.0 / (ao - sfour)
        eta = ao * e0 * tsi
        val etasq = eta * eta
        val eeta = e0 * eta
        val psisq = abs(1.0 - etasq)
        val coef = qzms24 * tsi.pow(4.0)
        val coef1 = coef / psisq.pow(3.5)
        con41 = 3.0 * cosi2 - 1.0
        x1mth2 = 1.0 - cosi2
        x7thm1 = 7.0 * cosi2 - 1.0
        val c2 = coef1 * n * (ao * (1.0 + 1.5 * etasq + eeta * (4.0 + etasq)) +
            0.375 * J2 * tsi / psisq * con41 * (8.0 + 3.0 * etasq * (8.0 + etasq)))
        c1 = bstar * c2
        val c3 = if (e0 > 1e-4) -2.0 * coef * tsi * J3OJ2 * n * sini / e0 else 0.0
        c4 = 2.0 * n * coef1 * ao * omeosq * (eta * (2.0 + 0.5 * etasq) +
            e0 * (0.5 + 2.0 * etasq) - J2 * tsi / (ao * psisq) *
            (-3.0 * con41 * (1.0 - 2.0 * eeta + etasq * (1.5 - 0.5 * eeta)) +
                0.75 * x1mth2 * (2.0 * etasq - eeta * (1.0 + etasq)) * cos(2.0 * arg0)))
        c5 = 2.0 * coef1 * ao * omeosq * (1.0 + 2.75 * (etasq + eeta) + eeta * etasq)
        val temp1 = 1.5 * J2 * pinvsq * n
        val temp2 = 0.5 * temp1 * J2 * pinvsq
        val temp3 = -0.46875 * J4 * pinvsq * pinvsq * n
        val cosi4 = cosi2 * cosi2
        mdot = n + 0.5 * temp1 * rteosq * con41 + 0.0625 * temp2 * rteosq *
            (13.0 - 78.0 * cosi2 + 137.0 * cosi4)
        argdot = -0.5 * temp1 * (1.0 - 5.0 * cosi2) + 0.0625 * temp2 *
            (7.0 - 114.0 * cosi2 + 395.0 * cosi4) + temp3 * (3.0 - 36.0 * cosi2 + 49.0 * cosi4)
        val xhdot1 = -temp1 * cosi
        nodedot = xhdot1 + (0.5 * temp2 * (4.0 - 19.0 * cosi2) +
            2.0 * temp3 * (3.0 - 7.0 * cosi2)) * cosi
        omgcof = bstar * c3 * cos(arg0)
        xmcof = if (e0 > 1e-4) -(2.0 / 3.0) * coef * bstar / eeta else 0.0
        nodecf = 3.5 * omeosq * xhdot1 * c1
        t2cof = 1.5 * c1
        xlcof = -0.25 * J3OJ2 * sini * (3.0 + 5.0 * cosi) /
            if (abs(1.0 + cosi) > 1.5e-12) (1.0 + cosi) else 1.5e-12
        aycof = -0.5 * J3OJ2 * sini
        delmo = (1.0 + eta * cos(m0)).pow(3.0)
        sinm0 = sin(m0)
        if (simple) {
            d2 = 0.0; d3 = 0.0; d4 = 0.0; t3cof = 0.0; t4cof = 0.0; t5cof = 0.0
        } else {
            val c1sq = c1 * c1
            d2 = 4.0 * ao * tsi * c1sq
            val temp = d2 * tsi * c1 / 3.0
            d3 = (17.0 * ao + sfour) * temp
            d4 = 0.5 * temp * ao * tsi * (221.0 * ao + 31.0 * sfour) * c1
            t3cof = d2 + 2.0 * c1sq
            t4cof = 0.25 * (3.0 * d3 + c1 * (12.0 * d2 + 10.0 * c1sq))
            t5cof = 0.2 * (3.0 * d4 + 12.0 * c1 * d3 + 6.0 * d2 * d2 +
                15.0 * c1sq * (2.0 * d2 + c1sq))
        }
    }

    fun position(minutesSinceEpoch: Double): Vector3D {
        require(minutesSinceEpoch.isFinite()) { "Invalid propagation time" }
        val t = minutesSinceEpoch
        val t2 = t * t
        val xmdf = m0 + mdot * t
        val argpdf = arg0 + argdot * t
        var argpm = argpdf
        var mm = xmdf
        val nodem = node0 + nodedot * t + nodecf * t2
        var tempa = 1.0 - c1 * t
        var tempe = bstar * c4 * t
        var templ = t2cof * t2
        if (!simple) {
            val temp = omgcof * t + xmcof * ((1.0 + eta * cos(xmdf)).pow(3.0) - delmo)
            mm += temp
            argpm -= temp
            val t3 = t2 * t
            val t4 = t3 * t
            tempa -= d2 * t2 + d3 * t3 + d4 * t4
            tempe += bstar * c5 * (sin(mm) - sinm0)
            templ += t3cof * t3 + t4 * (t4cof + t * t5cof)
        }
        val am = (XKE / n).pow(2.0 / 3.0) * tempa * tempa
        var em = e0 - tempe
        require(am.isFinite() && am > 0.0 && em >= -0.001 && em < 1.0) { "SGP4 mean elements out of range" }
        em = em.coerceAtLeast(1e-6)
        mm += n * templ
        val axnl = em * cos(argpm)
        val temp = 1.0 / (am * (1.0 - em * em))
        val aynl = em * sin(argpm) + temp * aycof
        val u = (mm + argpm + nodem + temp * xlcof * axnl - nodem) % (2.0 * PI)
        var eo1 = u
        for (k in 0 until 10) {
            val se = sin(eo1)
            val ce = cos(eo1)
            val correction = ((u - aynl * ce + axnl * se) - eo1) /
                (1.0 - ce * axnl - se * aynl)
            val bounded = correction.coerceIn(-0.95, 0.95)
            eo1 += bounded
            if (abs(bounded) < 1e-12) break
        }
        val se = sin(eo1)
        val ce = cos(eo1)
        val ecose = axnl * ce + aynl * se
        val esine = axnl * se - aynl * ce
        val el2 = axnl * axnl + aynl * aynl
        val pl = am * (1.0 - el2)
        require(pl > 0.0) { "SGP4 semi-latus rectum out of range" }
        val rl = am * (1.0 - ecose)
        val betal = sqrt(1.0 - el2)
        val su0 = atan2(
            am / rl * (se - aynl - axnl * esine / (1.0 + betal)),
            am / rl * (ce - axnl + aynl * esine / (1.0 + betal))
        )
        val sinu = sin(su0)
        val cosu = cos(su0)
        val sin2u = 2.0 * sinu * cosu
        val cos2u = 1.0 - 2.0 * sinu * sinu
        val temp1 = 0.5 * J2 / pl
        val temp2 = temp1 / pl
        val mrt = rl * (1.0 - 1.5 * temp2 * betal * con41) + 0.5 * temp1 * x1mth2 * cos2u
        require(mrt >= 1.0 && mrt.isFinite()) { "SGP4 satellite has decayed" }
        val su = su0 - 0.25 * temp2 * x7thm1 * sin2u
        val node = nodem + 1.5 * temp2 * cos(i0) * sin2u
        val inc = i0 + 1.5 * temp2 * cos(i0) * sin(i0) * cos2u
        val snod = sin(node)
        val cnod = cos(node)
        val sinsu = sin(su)
        val cossu = cos(su)
        val cosinc = cos(inc)
        val sininc = sin(inc)
        val xmx = -snod * cosinc
        val xmy = cnod * cosinc
        return Vector3D(
            mrt * (xmx * sinsu + cnod * cossu) * R,
            mrt * (xmy * sinsu + snod * cossu) * R,
            mrt * sininc * sinsu * R
        )
    }
}
