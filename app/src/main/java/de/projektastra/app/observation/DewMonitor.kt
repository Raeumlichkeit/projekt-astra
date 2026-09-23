package de.projektastra.app.observation

import kotlin.math.ln

/**
 * 4-stage Dew Risk Level for telescope optics, mirrors, and corrector plates.
 */
enum class DewRiskLevel(val label: String, val description: String, val alertSeverity: Int) {
    LOW("Gering", "Gering - Keine Beschlagsgefahr", 0),
    MODERATE("Mäßig", "Mäßig - Taubildung möglich", 1),
    HIGH("Hoch", "Hoch - Taukappe/Heizband empfohlen", 2),
    CRITICAL("Akut", "Akut - Sofortige Taugefahr auf Optik", 3)
}

/**
 * Dew Point report containing temperature metrics and assessed condensation risk.
 */
data class DewPointReport(
    val ambientTempCelsius: Double,
    val relativeHumidityPercent: Double,
    val dewPointCelsius: Double,
    val dewMarginCelsius: Double, // Delta = T_ambient - T_dew
    val riskLevel: DewRiskLevel
)

/**
 * Local Dew Point & Condensation Monitor based on the Sonntag (1990) Magnus-Tetens approximation.
 * 100% offline, privacy-friendly.
 */
object DewMonitor {

    // Standard Magnus-Tetens coefficients for water vapor (Sonntag 1990)
    const val A = 17.27
    const val B = 237.7 // °C

    /**
     * Calculates the dew point in °C from temperature (°C) and relative humidity (0..100%).
     */
    fun calculateDewPoint(tempCelsius: Double, relativeHumidityPercent: Double): Double {
        val clampedRh = relativeHumidityPercent.coerceIn(0.0, 100.0)
        if (clampedRh <= 0.0) return -100.0 // dry air limit
        val gamma = (A * tempCelsius) / (B + tempCelsius) + ln(clampedRh / 100.0)
        return (B * gamma) / (A - gamma)
    }

    /**
     * Assesses condensation risk for telescope optics based on ambient temperature and relative humidity.
     */
    fun assessRisk(ambientTempCelsius: Double, relativeHumidityPercent: Double): DewPointReport {
        val clampedRh = relativeHumidityPercent.coerceIn(0.0, 100.0)
        val dewPoint = calculateDewPoint(ambientTempCelsius, clampedRh)
        val margin = ambientTempCelsius - dewPoint

        val risk = when {
            margin <= 1.5 -> DewRiskLevel.CRITICAL
            margin <= 3.0 -> DewRiskLevel.HIGH
            margin <= 5.0 -> DewRiskLevel.MODERATE
            else -> DewRiskLevel.LOW
        }

        return DewPointReport(
            ambientTempCelsius = ambientTempCelsius,
            relativeHumidityPercent = clampedRh,
            dewPointCelsius = dewPoint,
            dewMarginCelsius = margin,
            riskLevel = risk
        )
    }

    fun calculate(tempCelsius: Double, relativeHumidityPercent: Double): DewPointReport =
        assessRisk(tempCelsius, relativeHumidityPercent)
}
