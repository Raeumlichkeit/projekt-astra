package de.projektastra.app.observation

import de.projektastra.app.ObservationLogEntry
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Standard Astronomical Seeing Scales Validator & Converter.
 * Supports Pickering (1–10), Antoniadi (I–V), and Naked-Eye Limiting Magnitude (NELM / fst).
 */
object SeeingScaleValidator {
    val ANTONIADI_VALID_GRADES = setOf("I", "II", "III", "IV", "V")

    fun isValidPickering(rating: Int): Boolean = rating in 1..10

    fun isValidAntoniadi(grade: String): Boolean = ANTONIADI_VALID_GRADES.contains(grade.trim().uppercase())

    fun isValidNelm(mag: Double): Boolean = mag in 0.0..8.5

    fun mapPickeringToAntoniadi(pickering: Int): String {
        require(isValidPickering(pickering)) { "Pickering must be 1..10, got $pickering" }
        return when (pickering) {
            10, 9 -> "I"     // Perfect seeing without a quiver
            8, 7 -> "II"     // Slight quivering with moments of calm
            6, 5 -> "III"    // Moderate seeing with larger tremors
            4, 3 -> "IV"     // Poor seeing with constant billows
            else -> "V"      // Very bad seeing
        }
    }
}

/**
 * Exporter for OpenAstronomyLog (OAL 2.1 XML schema) and Red-Light formatted summaries.
 */
object OpenAstronomyLogExport {

    fun escapeXml(text: String): String {
        val sanitized = text.filter { ch ->
            ch == '\t' || ch == '\n' || ch == '\r' || (ch >= ' ' && ch != '\u007F')
        }
        return sanitized.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    internal fun exportToXml(entries: List<ObservationLogEntry>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<oal xmlns=\"http://www.astronomy.org/OpenAstronomyLog/2.1\" version=\"2.1\">\n")
        sb.append("  <observations>\n")

        for (e in entries) {
            val isoDate = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochSecond(e.timestampEpochSeconds))
            val observer = if (e.locationName.isNullOrBlank()) "Astra Observer" else "Observer"
            val site = e.locationName?.takeIf { it.isNotBlank() } ?: "Home Observatory"
            val optics = e.equipment.takeIf { it.isNotBlank() } ?: "Telescope / Optics"

            sb.append("    <observation id=\"${escapeXml(e.id)}\">\n")
            sb.append("      <target id=\"${escapeXml(e.objectCatalogId)}\" name=\"${escapeXml(e.objectName)}\" />\n")
            sb.append("      <observer name=\"${escapeXml(observer)}\" />\n")
            sb.append("      <site name=\"${escapeXml(site)}\" />\n")
            sb.append("      <optics name=\"${escapeXml(optics)}\" />\n")
            sb.append("      <begin>${isoDate}</begin>\n")
            if (e.notes.isNotBlank()) {
                sb.append("      <result notes=\"${escapeXml(e.notes)}\" />\n")
            }
            if (e.seeingPickering != null || e.seeingAntoniadi != null || e.nelm != null) {
                sb.append("      <assessment>\n")
                e.seeingPickering?.let { sb.append("        <seeing pickering=\"${it}\" />\n") }
                e.seeingAntoniadi?.let { sb.append("        <seeing antoniadi=\"${escapeXml(it)}\" />\n") }
                e.nelm?.let { sb.append("        <limitingMagnitude>${it}</limitingMagnitude>\n") }
                sb.append("      </assessment>\n")
            }
            sb.append("    </observation>\n")
        }

        sb.append("  </observations>\n")
        sb.append("</oal>")
        return sb.toString()
    }

    internal fun exportFormattedTextSummary(entries: List<ObservationLogEntry>): String {
        val sb = StringBuilder()
        sb.append("=== PROJEKT ASTRA BEOBACHTUNGS-TAGEBUCH ===\n")
        sb.append("Gesamtanzahl Beobachtungen: ${entries.size}\n\n")
        entries.forEachIndexed { idx, e ->
            val date = ZonedDateTime.ofInstant(Instant.ofEpochSecond(e.timestampEpochSeconds), ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'"))
            val site = e.locationName?.takeIf { it.isNotBlank() } ?: "Keine Angabe"
            val optics = e.equipment.takeIf { it.isNotBlank() } ?: "Keine Angabe"

            sb.append("[${idx + 1}] ${e.objectCatalogId} - ${e.objectName}\n")
            sb.append("    Zeit: $date | Standort: $site | Optik: $optics\n")
            val seeingParts = mutableListOf<String>()
            e.seeingPickering?.let { seeingParts.add("Pickering $it/10") }
            e.seeingAntoniadi?.let { seeingParts.add("Antoniadi $it") }
            e.nelm?.let { seeingParts.add("NELM ${it}m") }
            if (seeingParts.isNotEmpty()) {
                sb.append("    Bedingungen: ${seeingParts.joinToString(", ")}\n")
            }
            if (e.notes.isNotBlank()) {
                sb.append("    Notiz: ${e.notes}\n")
            }
            sb.append("\n")
        }
        return sb.toString()
    }
}
