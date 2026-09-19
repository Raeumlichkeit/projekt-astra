package de.projektastra.app

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.util.UUID

internal data class ObservationLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val objectCatalogId: String,
    val objectName: String,
    val objectType: CelestialType = CelestialType.STAR,
    val timestampEpochSeconds: Long = Instant.now().epochSecond,
    val notes: String = "",
    val equipment: String = "",
    val seeingRating: Int = 0, // 1..5 stars, 0 = unrated
    val locationName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photoFileName: String? = null
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("id", id)
        put("objectCatalogId", objectCatalogId)
        put("objectName", objectName)
        put("objectType", objectType.name)
        put("timestampEpochSeconds", timestampEpochSeconds)
        put("notes", notes)
        put("equipment", equipment)
        put("seeingRating", seeingRating)
        locationName?.let { put("locationName", it) }
        latitude?.let { put("latitude", it) }
        longitude?.let { put("longitude", it) }
        photoFileName?.let { put("photoFileName", it) }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): ObservationLogEntry {
            val typeName = json.optString("objectType", "STAR")
            val type = runCatching { CelestialType.valueOf(typeName) }.getOrDefault(CelestialType.STAR)
            return ObservationLogEntry(
                id = json.optString("id", UUID.randomUUID().toString()),
                objectCatalogId = json.optString("objectCatalogId", ""),
                objectName = json.optString("objectName", "Unbekanntes Objekt"),
                objectType = type,
                timestampEpochSeconds = json.optLong("timestampEpochSeconds", Instant.now().epochSecond),
                notes = json.optString("notes", ""),
                equipment = json.optString("equipment", ""),
                seeingRating = json.optInt("seeingRating", 0).coerceIn(0, 5),
                locationName = if (json.has("locationName") && !json.isNull("locationName")) json.getString("locationName") else null,
                latitude = if (json.has("latitude") && !json.isNull("latitude")) json.getDouble("latitude") else null,
                longitude = if (json.has("longitude") && !json.isNull("longitude")) json.getDouble("longitude") else null,
                photoFileName = if (json.has("photoFileName") && !json.isNull("photoFileName")) json.getString("photoFileName") else null
            )
        }
    }
}

internal data class LogbookImportResult(
    val success: Boolean,
    val importedCount: Int,
    val totalCount: Int,
    val message: String
)

internal object ObservationLogbookStore {
    private const val FILE_NAME = "logbook_entries.json"
    private const val PHOTOS_DIR = "logbook_photos"
    const val MAX_ENTRIES = 5000
    const val MAX_JSON_BYTES = 10 * 1024 * 1024 // 10 MB

    private fun getStorageFile(context: Context): File = File(context.filesDir, FILE_NAME)
    fun getPhotosDir(context: Context): File = File(context.filesDir, PHOTOS_DIR).apply { if (!exists()) mkdirs() }

    fun loadEntries(context: Context): List<ObservationLogEntry> {
        val file = getStorageFile(context)
        if (!file.exists()) return emptyList()
        return runCatching {
            val content = file.readText(Charsets.UTF_8)
            parseEntriesJson(content)
        }.getOrDefault(emptyList())
    }

    fun parseEntriesJson(jsonText: String): List<ObservationLogEntry> {
        if (jsonText.isBlank()) return emptyList()
        val array = if (jsonText.trimStart().startsWith("[")) {
            JSONArray(jsonText)
        } else {
            val root = JSONObject(jsonText)
            root.optJSONArray("entries") ?: JSONArray()
        }
        val result = mutableListOf<ObservationLogEntry>()
        val count = minOf(array.length(), MAX_ENTRIES)
        for (i in 0 until count) {
            runCatching {
                val obj = array.getJSONObject(i)
                result.add(ObservationLogEntry.fromJsonObject(obj))
            }
        }
        return result
    }

    @Synchronized
    fun saveEntry(context: Context, entry: ObservationLogEntry): List<ObservationLogEntry> {
        val current = loadEntries(context).filterNot { it.id == entry.id }.toMutableList()
        current.add(0, entry)
        val trimmed = current.sortedByDescending { it.timestampEpochSeconds }.take(MAX_ENTRIES)
        writeEntries(context, trimmed)
        return trimmed
    }

    @Synchronized
    fun deleteEntry(context: Context, entryId: String): List<ObservationLogEntry> {
        val current = loadEntries(context)
        val toDelete = current.firstOrNull { it.id == entryId }
        toDelete?.photoFileName?.let { fileName ->
            deletePhotoFile(context, fileName)
        }
        val next = current.filterNot { it.id == entryId }
        writeEntries(context, next)
        return next
    }

    @Synchronized
    fun deleteAllEntries(context: Context) {
        val file = getStorageFile(context)
        if (file.exists()) file.delete()
        val photosDir = getPhotosDir(context)
        photosDir.listFiles()?.forEach { it.delete() }
    }

    private fun writeEntries(context: Context, entries: List<ObservationLogEntry>) {
        val array = JSONArray()
        entries.forEach { array.put(it.toJsonObject()) }
        val root = JSONObject().apply {
            put("version", 1)
            put("exportedAt", Instant.now().toString())
            put("entries", array)
        }
        val file = getStorageFile(context)
        file.writeText(root.toString(2), Charsets.UTF_8)
    }

    fun savePhotoFromUri(context: Context, sourceUri: Uri): String? {
        return runCatching {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                val fileName = "${UUID.randomUUID()}.jpg"
                val targetFile = File(getPhotosDir(context), fileName)
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
                fileName
            }
        }.getOrNull()
    }

    fun savePhotoBytes(context: Context, bytes: ByteArray): String {
        val fileName = "${UUID.randomUUID()}.jpg"
        val targetFile = File(getPhotosDir(context), fileName)
        targetFile.writeBytes(bytes)
        return fileName
    }

    fun getPhotoFile(context: Context, fileName: String): File {
        return File(getPhotosDir(context), fileName)
    }

    fun deletePhotoFile(context: Context, fileName: String) {
        val targetFile = File(getPhotosDir(context), fileName)
        if (targetFile.exists()) targetFile.delete()
    }

    fun exportJson(context: Context): String {
        val entries = loadEntries(context)
        val array = JSONArray()
        entries.forEach { array.put(it.toJsonObject()) }
        val root = JSONObject().apply {
            put("app", "Projekt Astra")
            put("version", 1)
            put("exportedAt", Instant.now().toString())
            put("count", entries.size)
            put("entries", array)
        }
        return root.toString(2)
    }

    @Synchronized
    fun importJson(context: Context, jsonText: String, merge: Boolean = true): LogbookImportResult {
        if (jsonText.length > MAX_JSON_BYTES) {
            return LogbookImportResult(false, 0, 0, "Die Datei überschreitet das Limit von 10 MB.")
        }
        return runCatching {
            val parsed = parseEntriesJson(jsonText)
            if (parsed.isEmpty()) {
                return LogbookImportResult(false, 0, 0, "Keine gültigen Beobachtungseinträge in den Daten gefunden.")
            }
            val existing = if (merge) loadEntries(context) else emptyList()
            val map = existing.associateBy { it.id }.toMutableMap()
            parsed.forEach { map[it.id] = it }
            val combined = map.values.sortedByDescending { it.timestampEpochSeconds }.take(MAX_ENTRIES)
            writeEntries(context, combined)
            LogbookImportResult(true, parsed.size, combined.size, "${parsed.size} Einträge erfolgreich ${if (merge) "zusammengeführt" else "importiert"}.")
        }.getOrElse { e ->
            LogbookImportResult(false, 0, 0, "Fehler beim Einlesen der JSON-Daten: ${e.message}")
        }
    }
}
