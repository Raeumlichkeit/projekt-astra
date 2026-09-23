package de.projektastra.app.observation

import de.projektastra.app.ObservationLogEntry
import java.util.Locale

/**
 * Standardized Astronomical Observation Challenges.
 * Supports Messier 110, Caldwell Catalog (109), and Herschel 400.
 */
enum class ChallengeType(val title: String, val totalCount: Int, val description: String) {
    MESSIER_110(
        "Messier 110 Challenge",
        110,
        "Beobachte alle 110 Deep-Sky-Objekte des klassischen Messier-Katalogs."
    ),
    CALDWELL(
        "Caldwell Catalog Challenge",
        109,
        "Patrick Caldwell-Moores Ergänzungskatalog mit 109 spektakulären Deep-Sky-Objekten."
    ),
    HERSCHEL_400(
        "Herschel 400 Challenge",
        400,
        "Die ultimative Amateur-Beobachtungsliste aus William Herschels Entdeckungen."
    )
}

data class ChallengeProgress(
    val type: ChallengeType,
    val observedCount: Int,
    val totalCount: Int,
    val percentComplete: Double,
    val completedTargetIds: Set<String>
)

/**
 * Matches and normalizes catalog identifiers across composite strings (e.g., "M 31 · NGC 224",
 * "Astronomy Engine · Jupiter", "HIP 91262") between logbook entries and sky objects.
 */
internal object ObservationCatalogMatcher {

    fun normalizeToken(token: String): String =
        token.trim().lowercase(Locale.ROOT)
            .replace(" ", "")
            .replace("-", "")
            .replace("_", "")

    /**
     * Extracts all search and matching tokens from a catalog ID or object name.
     */
    fun extractTokens(rawId: String): Set<String> {
        val trimmed = rawId.trim()
        if (trimmed.isEmpty()) return emptySet()
        val tokens = mutableSetOf<String>()
        tokens.add(trimmed.lowercase(Locale.ROOT))
        tokens.add(normalizeToken(trimmed))

        // Split by middle dot, comma, semicolon, slash, pipe
        val parts = trimmed.split('·', ',', ';', '/', '|')
        for (part in parts) {
            val p = part.trim()
            if (p.isNotEmpty()) {
                tokens.add(p.lowercase(Locale.ROOT))
                tokens.add(normalizeToken(p))
            }
        }
        return tokens
    }

    /**
     * Builds a comprehensive set of normalized tokens from all logbook entries.
     */
    internal fun buildLoggedTokens(entries: List<ObservationLogEntry>): Set<String> {
        val tokens = mutableSetOf<String>()
        for (entry in entries) {
            if (entry.objectCatalogId.isNotBlank()) {
                tokens.addAll(extractTokens(entry.objectCatalogId))
            }
            if (entry.objectName.isNotBlank()) {
                tokens.addAll(extractTokens(entry.objectName))
            }
        }
        return tokens
    }

    /**
     * Checks if a celestial object's catalog ID matches any logged entry.
     */
    fun isObserved(targetCatalogId: String, loggedTokens: Set<String>): Boolean {
        if (targetCatalogId.isBlank() || loggedTokens.isEmpty()) return false
        val tokens = extractTokens(targetCatalogId)
        return tokens.any { it in loggedTokens }
    }
}

object ObservationChallengeRegistry {

    private val MESSIER_EXACT_REGEX = Regex("^(?:MESSIER\\s*|M\\s*)([1-9]|[1-9][0-9]|10[0-9]|110)$", RegexOption.IGNORE_CASE)
    private val CALDWELL_EXACT_REGEX = Regex("^(?:CALDWELL\\s*|C\\s*)([1-9]|[1-9][0-9]|10[0-9])$", RegexOption.IGNORE_CASE)

    // Regex to match Messier and Caldwell designations inside composite strings like "M 31 · NGC 224"
    private val MESSIER_EMBEDDED_REGEX = Regex("""(?:^|[·,\s(])(?:MESSIER\s*|M\s*)([1-9]|[1-9][0-9]|10[0-9]|110)(?=[·,\s)]|$)""", RegexOption.IGNORE_CASE)
    private val CALDWELL_EMBEDDED_REGEX = Regex("""(?:^|[·,\s(])(?:CALDWELL\s*|C\s*)([1-9]|[1-9][0-9]|10[0-9])(?=[·,\s)]|$)""", RegexOption.IGNORE_CASE)

    fun normalizeTargetId(id: String): String {
        val trimmed = id.trim()
        val exactM = MESSIER_EXACT_REGEX.matchEntire(trimmed)
        if (exactM != null) return "M${exactM.groupValues[1]}"

        val exactC = CALDWELL_EXACT_REGEX.matchEntire(trimmed)
        if (exactC != null) return "C${exactC.groupValues[1]}"

        val embeddedM = MESSIER_EMBEDDED_REGEX.find(trimmed)
        if (embeddedM != null) return "M${embeddedM.groupValues[1]}"

        val embeddedC = CALDWELL_EMBEDDED_REGEX.find(trimmed)
        if (embeddedC != null) return "C${embeddedC.groupValues[1]}"

        return trimmed
    }

    fun isMessierObject(catalogId: String): Boolean {
        val trimmed = catalogId.trim()
        return MESSIER_EXACT_REGEX.matches(trimmed) || MESSIER_EMBEDDED_REGEX.containsMatchIn(trimmed)
    }

    fun isCaldwellObject(catalogId: String): Boolean {
        val trimmed = catalogId.trim()
        return CALDWELL_EXACT_REGEX.matches(trimmed) || CALDWELL_EMBEDDED_REGEX.containsMatchIn(trimmed)
    }

    fun isHerschel400Object(catalogId: String): Boolean {
        val trimmed = catalogId.trim().uppercase(Locale.ROOT)
        val parts = trimmed.split("·", ",", ";").map { it.trim() }
        return parts.any { part ->
            part.startsWith("NGC") || part.startsWith("H400") || part.startsWith("H 400") || part.startsWith("HERSCHEL")
        }
    }

    fun evaluateProgress(
        challenge: ChallengeType,
        observedCatalogIds: Set<String>
    ): ChallengeProgress {
        val normalizedObserved = observedCatalogIds.map { normalizeTargetId(it) }.toSet()

        val matchingTargets = when (challenge) {
            ChallengeType.MESSIER_110 -> {
                normalizedObserved.filter { isMessierObject(it) }.toSet()
            }
            ChallengeType.CALDWELL -> {
                normalizedObserved.filter { isCaldwellObject(it) }.toSet()
            }
            ChallengeType.HERSCHEL_400 -> {
                normalizedObserved.filter { isHerschel400Object(it) }.take(400).toSet()
            }
        }

        val count = matchingTargets.size.coerceAtMost(challenge.totalCount)
        val pct = if (challenge.totalCount > 0) {
            (count.toDouble() / challenge.totalCount) * 100.0
        } else {
            0.0
        }

        return ChallengeProgress(
            type = challenge,
            observedCount = count,
            totalCount = challenge.totalCount,
            percentComplete = pct,
            completedTargetIds = matchingTargets
        )
    }

    internal fun evaluateProgressFromLogbook(
        challenge: ChallengeType,
        entries: List<ObservationLogEntry>
    ): ChallengeProgress {
        val observedIds = entries.map { it.objectCatalogId }.filter { it.isNotBlank() }.toSet()
        return evaluateProgress(challenge, observedIds)
    }

    internal fun evaluateAll(entries: List<ObservationLogEntry>): List<ChallengeProgress> {
        val observedIds = entries.map { it.objectCatalogId }.filter { it.isNotBlank() }.toSet()
        return ChallengeType.values().map { evaluateProgress(it, observedIds) }
    }
}
