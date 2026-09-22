package de.projektastra.app.observation

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class DewMonitorTest {

    @Test
    fun testDewPointCalculation_standardAtmosphere() {
        // At 20°C and 50% RH, dew point is approximately 9.3°C
        val dewPoint = DewMonitor.calculateDewPoint(20.0, 50.0)
        assertEquals(9.28, dewPoint, 0.15)
    }

    @Test
    fun testDewPointCalculation_highHumidity() {
        // At 10°C and 90% RH, dew point is approximately 8.4°C
        val dewPoint = DewMonitor.calculateDewPoint(10.0, 90.0)
        assertEquals(8.43, dewPoint, 0.15)
    }

    @Test
    fun testDewPointCalculation_saturation() {
        // At 100% RH, dew point equals temperature
        val temp = 15.0
        val dewPoint = DewMonitor.calculateDewPoint(temp, 100.0)
        assertEquals(temp, dewPoint, 0.01)
    }

    @Test
    fun testDewPointCalculation_subzeroTemperatures() {
        // At -5°C and 70% RH
        val dewPoint = DewMonitor.calculateDewPoint(-5.0, 70.0)
        assertTrue(dewPoint < -5.0)
        assertTrue(dewPoint > -15.0)
    }

    @Test
    fun testDryAirBoundary() {
        val dewPoint = DewMonitor.calculateDewPoint(20.0, 0.0)
        assertEquals(-100.0, dewPoint, 0.01)
    }

    @Test
    fun testAssessRisk_criticalRisk() {
        // 10°C, 95% RH -> dew point ~9.2°C -> margin ~0.8°C <= 1.5°C -> CRITICAL
        val report = DewMonitor.assessRisk(10.0, 95.0)
        assertEquals(DewRiskLevel.CRITICAL, report.riskLevel)
        assertTrue(report.dewMarginCelsius <= 1.5)
        assertEquals(3, report.riskLevel.alertSeverity)
    }

    @Test
    fun testAssessRisk_highRisk() {
        // 10°C, 85% RH -> dew point ~7.6°C -> margin ~2.4°C (1.5..3.0) -> HIGH
        val report = DewMonitor.assessRisk(10.0, 85.0)
        assertEquals(DewRiskLevel.HIGH, report.riskLevel)
        assertTrue(report.dewMarginCelsius in 1.5..3.0)
        assertEquals(2, report.riskLevel.alertSeverity)
    }

    @Test
    fun testAssessRisk_moderateRisk() {
        // 15°C, 75% RH -> dew point ~10.5°C -> margin ~4.5°C (3.0..5.0) -> MODERATE
        val report = DewMonitor.assessRisk(15.0, 75.0)
        assertEquals(DewRiskLevel.MODERATE, report.riskLevel)
        assertTrue(report.dewMarginCelsius in 3.0..5.0)
        assertEquals(1, report.riskLevel.alertSeverity)
    }

    @Test
    fun testAssessRisk_lowRisk() {
        // 20°C, 40% RH -> dew point ~6.0°C -> margin ~14.0°C > 5.0 -> LOW
        val report = DewMonitor.assessRisk(20.0, 40.0)
        assertEquals(DewRiskLevel.LOW, report.riskLevel)
        assertTrue(report.dewMarginCelsius > 5.0)
        assertEquals(0, report.riskLevel.alertSeverity)
    }
}
