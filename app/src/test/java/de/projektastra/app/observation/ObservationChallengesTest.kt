package de.projektastra.app.observation

import de.projektastra.app.ObservationLogEntry
import org.junit.Assert.*
import org.junit.Test

class ObservationChallengesTest {

    @Test
    fun testChallengeTypesAndCounts() {
        assertEquals(110, ChallengeType.MESSIER_110.totalCount)
        assertEquals(109, ChallengeType.CALDWELL.totalCount)
        assertEquals(400, ChallengeType.HERSCHEL_400.totalCount)
    }

    @Test
    fun testTargetNormalization() {
        assertEquals("M31", ObservationChallengeRegistry.normalizeTargetId("M 31"))
        assertEquals("M42", ObservationChallengeRegistry.normalizeTargetId("Messier 42"))
        assertEquals("M110", ObservationChallengeRegistry.normalizeTargetId("m110"))
        assertEquals("C14", ObservationChallengeRegistry.normalizeTargetId("C 14"))
        assertEquals("C1", ObservationChallengeRegistry.normalizeTargetId("Caldwell 1"))
        assertEquals("NGC 7000", ObservationChallengeRegistry.normalizeTargetId("NGC 7000"))

        // Composite OpenNGC / catalog designations
        assertEquals("M31", ObservationChallengeRegistry.normalizeTargetId("M 31 · NGC 224"))
        assertEquals("M110", ObservationChallengeRegistry.normalizeTargetId("M 110 · NGC 205"))
        assertEquals("C20", ObservationChallengeRegistry.normalizeTargetId("C 20 · NGC 7000"))
    }

    @Test
    fun testMessierChallengeEvaluation_empty() {
        val progress = ObservationChallengeRegistry.evaluateProgress(
            ChallengeType.MESSIER_110,
            emptySet()
        )
        assertEquals(0, progress.observedCount)
        assertEquals(110, progress.totalCount)
        assertEquals(0.0, progress.percentComplete, 0.001)
        assertTrue(progress.completedTargetIds.isEmpty())
    }

    @Test
    fun testMessierChallengeEvaluation_partial() {
        val observed = setOf("M1", "M31", "M42", "M45", "M51", "NGC 7000", "HIP 1234")
        val progress = ObservationChallengeRegistry.evaluateProgress(
            ChallengeType.MESSIER_110,
            observed
        )
        assertEquals(5, progress.observedCount)
        assertEquals(110, progress.totalCount)
        assertEquals((5.0 / 110.0) * 100.0, progress.percentComplete, 0.01)
        assertTrue(progress.completedTargetIds.contains("M31"))
        assertFalse(progress.completedTargetIds.contains("NGC 7000"))
    }

    @Test
    fun testCaldwellChallengeEvaluation() {
        val observed = setOf("C14", "C 1", "Caldwell 109", "M31")
        val progress = ObservationChallengeRegistry.evaluateProgress(
            ChallengeType.CALDWELL,
            observed
        )
        assertEquals(3, progress.observedCount)
        assertEquals(109, progress.totalCount)
        assertTrue(progress.completedTargetIds.contains("C14"))
        assertTrue(progress.completedTargetIds.contains("C1"))
        assertTrue(progress.completedTargetIds.contains("C109"))
    }

    @Test
    fun testHerschel400ChallengeEvaluation() {
        val ngcTargets = (1..50).map { "NGC $it" }.toSet()
        val h400Targets = setOf("H400_01", "H400_02")
        val progress = ObservationChallengeRegistry.evaluateProgress(
            ChallengeType.HERSCHEL_400,
            ngcTargets + h400Targets + setOf("M31", "Sirius")
        )
        assertEquals(52, progress.observedCount)
        assertEquals(400, progress.totalCount)
    }

    @Test
    fun testEvaluateProgressFromLogbook() {
        val entries = listOf(
            ObservationLogEntry(objectCatalogId = "M13", objectName = "Herkuleshaufen"),
            ObservationLogEntry(objectCatalogId = "M 31 · NGC 224", objectName = "Andromedagalaxie"),
            ObservationLogEntry(objectCatalogId = "M31", objectName = "Andromeda 2nd run"),
            ObservationLogEntry(objectCatalogId = "Jupiter", objectName = "Jupiter")
        )
        val progress = ObservationChallengeRegistry.evaluateProgressFromLogbook(
            ChallengeType.MESSIER_110,
            entries
        )
        assertEquals(2, progress.observedCount)
        assertEquals(110, progress.totalCount)
        assertTrue(progress.completedTargetIds.contains("M13"))
        assertTrue(progress.completedTargetIds.contains("M31"))
    }

    @Test
    fun testObservationCatalogMatcher() {
        val entries = listOf(
            ObservationLogEntry(objectCatalogId = "Jupiter", objectName = "Jupiter"),
            ObservationLogEntry(objectCatalogId = "M 31 · NGC 224", objectName = "Andromedagalaxie"),
            ObservationLogEntry(objectCatalogId = "C 20", objectName = "Nordamerikanebel")
        )
        val tokens = ObservationCatalogMatcher.buildLoggedTokens(entries)

        // Exact matches
        assertTrue(ObservationCatalogMatcher.isObserved("Jupiter", tokens))
        assertTrue(ObservationCatalogMatcher.isObserved("M31", tokens))
        assertTrue(ObservationCatalogMatcher.isObserved("C20", tokens))

        // OpenNGC composite target matches
        assertTrue(ObservationCatalogMatcher.isObserved("M 31 · NGC 224", tokens))
        assertTrue(ObservationCatalogMatcher.isObserved("NGC 224", tokens))
        assertTrue(ObservationCatalogMatcher.isObserved("Astronomy Engine · Jupiter", tokens))

        // Non-matching target
        assertFalse(ObservationCatalogMatcher.isObserved("Saturn", tokens))
        assertFalse(ObservationCatalogMatcher.isObserved("M42", tokens))
    }
}
