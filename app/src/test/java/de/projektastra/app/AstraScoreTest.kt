package de.projektastra.app

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class AstraScoreTest {
    @Test fun historicalDataWarningDoesNotChangeTheLightPenalty() {
        fun score(light: LightPollutionEstimate?) = AstraScoreCalculator.calculate(
            cloudCover = 0,
            rainProbability = 0,
            windSpeed = 0.0,
            visibilityMeters = 25_000.0,
            observer = GeoPoint(52.52, 13.40, 0.0),
            instant = Instant.parse("2026-09-11T00:00:00Z"),
            lightPollution = light
        )
        val withoutLight = score(null)
        val withLight = score(LightPollutionEstimate(100, 9, 18.0, sourceYear = 2016))
        val penalty = withLight.penalties.single { it.label == "Lichtverschmutzung" }
        assertEquals(20, penalty.points)
        assertEquals(withoutLight.score - 20, withLight.score)
        assertTrue(penalty.explanation.contains("2016 (veraltet)"))
        assertTrue(withoutLight.penalties.last().explanation.contains("kein Abzug"))
    }
}
