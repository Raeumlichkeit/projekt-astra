package de.projektastra.app

import java.text.Normalizer
import java.util.Locale

/** Search is an in-memory index only: no history, preferences or network access. */
internal data class SkySearchTarget(
    val objectData: CelestialObject,
    val regionName: String? = null,
    val regionAliases: List<String> = emptyList(),
    val regionId: String? = null,
    val asterism: Boolean = false
) {
    val id: String get() = regionId ?: objectData.catalogId
    val name: String get() = regionName ?: objectData.name
    val typeLabel: String get() = if (regionName == null) objectData.objectType.label
        else if (asterism) "Sternmuster" else "Sternbild"
    val needsDeepSky: Boolean get() = regionName == null && objectData.solarBody == null &&
        objectData.objectType != CelestialType.STAR
}

internal class SkySearchIndex(targets: List<SkySearchTarget>) {
    private data class Entry(val target: SkySearchTarget, val terms: List<String>)
    private val entries = targets.distinctBy { it.id }.map { target ->
        val data = target.objectData
        val terms = if (target.regionName != null) listOf(target.name) + target.regionAliases
        else listOf(data.name, data.catalogId, data.messierId) +
            data.catalogId.split('·') + data.searchAliases +
            aliases[data.catalogId].orEmpty() + aliases[data.messierId].orEmpty() +
            listOfNotNull(data.solarBody?.name)
        Entry(target, terms.map(::normalize).filter { it.isNotEmpty() }.distinct())
    }

    fun search(query: String, limit: Int = 40): List<SkySearchTarget> {
        val needle = normalize(query)
        if (needle.isEmpty() || limit <= 0) return emptyList()
        // A complete numeric identifier must not match M310 or HIP 912620.
        val catalogQuery = Regex("(?:hip|ngc|ic|m|mel)\\d+[a-z]?").matches(needle)
        return entries.mapNotNull { entry ->
            val rank = when {
                needle in entry.terms -> 0
                catalogQuery -> return@mapNotNull null
                entry.terms.any { it.startsWith(needle) } -> 1
                entry.terms.any { needle in it } -> 2
                else -> return@mapNotNull null
            }
            entry to rank
        }.sortedWith(compareBy<Pair<Entry, Int>> { it.second }
            .thenBy { it.first.target.name.lowercase(Locale.ROOT) }.thenBy { it.first.target.id })
            .take(limit).map { it.first.target }
    }

    companion object {
        internal fun normalize(value: String): String = Normalizer.normalize(
            value.lowercase(Locale.ROOT).replace("ß", "ss"), Normalizer.Form.NFD
        ).replace(Regex("\\p{M}+"), "").replace(Regex("[^\\p{L}\\p{N}]"), "")

        private val aliases = mapOf(
            "HIP 91262" to listOf("Wega", "Vega"),
            "HIP 69673" to listOf("Arktur", "Arcturus"),
            "HIP 27989" to listOf("Beteigeuze", "Betelgeuse"),
            "HIP 11767" to listOf("Polarstern", "Nordstern", "Polaris"),
            "HIP 37279" to listOf("Prokyon", "Procyon"),
            "M 31" to listOf("Andromeda", "Andromedagalaxie", "Andromedanebel"),
            "M 42" to listOf("Orionnebel"),
            "M 45" to listOf("Plejaden", "Siebengestirn"),
            "M 13" to listOf("Herkuleshaufen"),
            "M 57" to listOf("Ringnebel"),
            "M 1" to listOf("Krebsnebel", "Krabbennebel"),
            "M 51" to listOf("Whirlpoolgalaxie", "Strudelgalaxie")
        )
    }
}

internal enum class TargetVisibility(val label: String) {
    ABOVE("Über dem Horizont"), BELOW("Unter dem Horizont"), TERRAIN("Vom Gelände verdeckt")
}

internal fun targetVisibility(position: HorizontalCoordinates, terrain: TerrainProfile?): TargetVisibility = when {
    position.altitude <= 0.0 -> TargetVisibility.BELOW
    position.altitude <= (terrain?.altitudeAt(position.azimuth) ?: 0.0) -> TargetVisibility.TERRAIN
    else -> TargetVisibility.ABOVE
}

/** A target remains selected after a gesture, but the manual camera is no longer locked to it. */
internal data class SkyTargetSelection(val target: SkySearchTarget? = null, val tracking: Boolean = false) {
    fun select(value: SkySearchTarget) = SkyTargetSelection(value)
    fun follow() = copy(tracking = target != null)
    fun release() = copy(tracking = false)
}

/** Use an actual catalog star as a clearly named reference point, not a fictitious constellation object. */
internal fun constellationSearchTargets(stars: List<CelestialObject>): List<SkySearchTarget> {
    val germanNames = mapOf(
        "UMa" to "Großer Bär", "UMi" to "Kleiner Bär", "Cas" to "Kassiopeia",
        "Cyg" to "Schwan", "Lyr" to "Leier", "Aql" to "Adler", "Leo" to "Löwe",
        "Gem" to "Zwillinge", "Tau" to "Stier", "Vir" to "Jungfrau", "Lib" to "Waage",
        "Sco" to "Skorpion", "Sgr" to "Schütze", "Cap" to "Steinbock", "Aqr" to "Wassermann",
        "Psc" to "Fische", "Ari" to "Widder", "Cnc" to "Krebs", "Dra" to "Drache",
        "Her" to "Herkules", "Boo" to "Bärenhüter", "Oph" to "Schlangenträger",
        "Ser" to "Schlange", "Aur" to "Fuhrmann", "CMa" to "Großer Hund", "CMi" to "Kleiner Hund"
    )
    val regions = stars.filter { it.constellation.isNotBlank() }.groupBy { it.constellation }.map { (abbreviation, members) ->
        val anchor = members.minBy { it.magnitude }
        val latin = io.github.cosinekitty.astronomy.constellation(anchor.raHours, anchor.decDegrees).name
        SkySearchTarget(anchor, germanNames[abbreviation] ?: latin, listOf(latin, abbreviation), "constellation:$abbreviation")
    }
    val bigDipper = stars.firstOrNull { it.hipId == 58001 }?.let {
        SkySearchTarget(it, "Großer Wagen", listOf("Big Dipper"), "asterism:big-dipper", asterism = true)
    }
    return regions + listOfNotNull(bigDipper)
}
