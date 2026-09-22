package de.projektastra.app.ephemeris

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.pow

internal data class TleData(
    val satelliteName: String,
    val noradCatalogNumber: Int,
    val classification: Char,
    val internationalDesignator: String,
    val epochYear: Int,
    val epochDayOfYear: Double,
    val epochInstant: Instant,
    val bstarDrag: Double,
    val inclinationDegrees: Double,
    val raanDegrees: Double,
    val eccentricity: Double,
    val argumentOfPerigeeDegrees: Double,
    val meanAnomalyDegrees: Double,
    val meanMotionRevsPerDay: Double,
    val revolutionNumberAtEpoch: Int
) {
    val meanMotionRadPerMin: Double get() = meanMotionRevsPerDay * (2.0 * Math.PI / 1440.0)
    val orbitalPeriodMinutes: Double get() = 1440.0 / meanMotionRevsPerDay
    val isDeepSpace: Boolean get() = orbitalPeriodMinutes >= 225.0
}

internal object TleParser {

    fun validateChecksum(line: String): Boolean {
        if (line.length < 69) return false
        var sum = 0
        for (i in 0 until 68) {
            val c = line[i]
            when {
                c in '0'..'9' -> sum += (c - '0')
                c == '-' -> sum += 1
            }
        }
        val expected = line[68]
        return (expected in '0'..'9') && (sum % 10 == (expected - '0'))
    }

    fun parseDecimalWithExponent(str: String): Double {
        val trimmed = str.trim()
        if (trimmed.isEmpty() || trimmed == "0" || trimmed == "00000-0" || trimmed == "00000+0") return 0.0

        var sign = 1.0
        var s = trimmed
        if (s.startsWith("-")) {
            sign = -1.0
            s = s.substring(1)
        } else if (s.startsWith("+")) {
            s = s.substring(1)
        }

        val expIndex = s.lastIndexOfAny(charArrayOf('+', '-'))
        if (expIndex <= 0) {
            return runCatching { ("0.$s").toDouble() * sign }.getOrDefault(0.0)
        }

        val mantissaStr = s.substring(0, expIndex)
        val expStr = s.substring(expIndex)
        val mantissa = ("0.$mantissaStr").toDoubleOrNull() ?: 0.0
        val exp = expStr.toIntOrNull() ?: 0
        return sign * mantissa * 10.0.pow(exp)
    }

    fun parseTle(line1: String, line2: String, name: String = "Unknown"): TleData {
        require(line1.startsWith("1 ")) { "Invalid TLE Line 1 prefix: $line1" }
        require(line2.startsWith("2 ")) { "Invalid TLE Line 2 prefix: $line2" }

        val noradId = line1.substring(2, 7).trim().toInt()
        val classification = line1.getOrElse(7) { 'U' }
        val intDesignator = line1.substring(9, 17).trim()

        val year2 = line1.substring(18, 20).trim().toInt()
        val epochYear = if (year2 < 57) 2000 + year2 else 1900 + year2
        val epochDay = line1.substring(20, 32).trim().toDouble()

        val startOfYear = LocalDate.of(epochYear, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant()
        val totalSeconds = (epochDay - 1.0) * 86400.0
        val seconds = totalSeconds.toLong()
        val nanos = ((totalSeconds - seconds) * 1_000_000_000.0).toLong()
        val epochInstant = startOfYear.plusSeconds(seconds).plusNanos(nanos)

        val bstar = parseDecimalWithExponent(line1.substring(53, 61))

        val inc = line2.substring(8, 16).trim().toDouble()
        val raan = line2.substring(17, 25).trim().toDouble()
        val eccStr = line2.substring(26, 33).trim()
        val ecc = ("0.$eccStr").toDouble()
        val argPerigee = line2.substring(34, 42).trim().toDouble()
        val meanAnomaly = line2.substring(43, 51).trim().toDouble()
        val meanMotion = line2.substring(52, 63).trim().toDouble()
        val revNum = line2.substring(63, 68).trim().toIntOrNull() ?: 0

        return TleData(
            satelliteName = name.trim(),
            noradCatalogNumber = noradId,
            classification = classification,
            internationalDesignator = intDesignator,
            epochYear = epochYear,
            epochDayOfYear = epochDay,
            epochInstant = epochInstant,
            bstarDrag = bstar,
            inclinationDegrees = inc,
            raanDegrees = raan,
            eccentricity = ecc,
            argumentOfPerigeeDegrees = argPerigee,
            meanAnomalyDegrees = meanAnomaly,
            meanMotionRevsPerDay = meanMotion,
            revolutionNumberAtEpoch = revNum
        )
    }

    fun parseMultiple(lines: List<String>): List<TleData> {
        val result = mutableListOf<TleData>()
        val filtered = lines.map { it.trim() }.filter { it.isNotEmpty() }
        var i = 0
        while (i < filtered.size) {
            val line = filtered[i]
            if (line.startsWith("1 ") && i + 1 < filtered.size && filtered[i + 1].startsWith("2 ")) {
                val name = if (i > 0 && !filtered[i - 1].startsWith("1 ") && !filtered[i - 1].startsWith("2 ")) {
                    filtered[i - 1]
                } else {
                    "Satellite ${line.substring(2, 7).trim()}"
                }
                result.add(parseTle(line, filtered[i + 1], name))
                i += 2
            } else if (i + 2 < filtered.size && filtered[i + 1].startsWith("1 ") && filtered[i + 2].startsWith("2 ")) {
                result.add(parseTle(filtered[i + 1], filtered[i + 2], line))
                i += 3
            } else {
                i++
            }
        }
        return result
    }
}
