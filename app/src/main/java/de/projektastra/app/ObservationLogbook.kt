package de.projektastra.app

import android.content.Context
import android.net.Uri
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
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
    val photoFileName: String? = null,
    val seeingPickering: Int? = null, // 1..10
    val seeingAntoniadi: String? = null, // I..V
    val nelm: Double? = null // Naked Eye Limiting Magnitude (fst)
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
        seeingPickering?.let { put("seeingPickering", it) }
        seeingAntoniadi?.let { put("seeingAntoniadi", it) }
        nelm?.let { put("nelm", it) }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): ObservationLogEntry {
            val timestamp = if (json.has("timestampEpochSeconds")) json.getLong("timestampEpochSeconds")
                else Instant.now().epochSecond
            require(timestamp in -62135596800L..253402300799L) { "Ungültiger Beobachtungszeitpunkt" }
            val latitude = json.optDouble("latitude", Double.NaN).takeIf { it in -90.0..90.0 }
            val longitude = json.optDouble("longitude", Double.NaN).takeIf { it in -180.0..180.0 }
            val typeName = json.optString("objectType", "STAR")
            val type = runCatching { CelestialType.valueOf(typeName) }.getOrDefault(CelestialType.STAR)
            val pickering = if (json.has("seeingPickering") && !json.isNull("seeingPickering")) {
                json.getInt("seeingPickering").coerceIn(1, 10)
            } else null
            val antoniadi = if (json.has("seeingAntoniadi") && !json.isNull("seeingAntoniadi")) {
                json.getString("seeingAntoniadi").takeIf { it.isNotBlank() }
            } else null
            val nelm = if (json.has("nelm") && !json.isNull("nelm")) {
                json.getDouble("nelm").coerceIn(0.0, 9.0)
            } else null

            return ObservationLogEntry(
                id = json.optString("id", UUID.randomUUID().toString()),
                objectCatalogId = json.optString("objectCatalogId", ""),
                objectName = json.optString("objectName", "Unbekanntes Objekt"),
                objectType = type,
                timestampEpochSeconds = timestamp,
                notes = json.optString("notes", ""),
                equipment = json.optString("equipment", ""),
                seeingRating = json.optInt("seeingRating", 0).coerceIn(0, 5),
                locationName = if (json.has("locationName") && !json.isNull("locationName")) json.getString("locationName") else null,
                latitude = latitude.takeIf { longitude != null },
                longitude = longitude.takeIf { latitude != null },
                photoFileName = json.optString("photoFileName").takeIf(ObservationLogbookStore::isSafePhotoFileName),
                seeingPickering = pickering,
                seeingAntoniadi = antoniadi,
                nelm = nelm
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

    @Synchronized
    fun loadEntries(context: Context): List<ObservationLogEntry> {
        val file = getStorageFile(context)
        if (!file.exists() && !File(file.path + ".bak").exists()) return emptyList()
        return AtomicFile(file).openRead().use { input ->
            if (input.channel.size() > MAX_JSON_BYTES) throw IOException("Logbuch überschreitet 10 MB")
            parseEntriesJson(input.readBytes().toString(Charsets.UTF_8), strict = true)
        }
    }

    fun parseEntriesJson(jsonText: String, strict: Boolean = false): List<ObservationLogEntry> {
        require(jsonText.toByteArray(Charsets.UTF_8).size <= MAX_JSON_BYTES) { "Die Datei überschreitet 10 MB." }
        if (jsonText.isBlank()) {
            require(!strict) { "Das gespeicherte Logbuch ist leer oder beschädigt." }
            return emptyList()
        }
        val array = if (jsonText.trimStart().startsWith("[")) {
            JSONArray(jsonText)
        } else {
            val root = JSONObject(jsonText)
            root.getJSONArray("entries")
        }
        val result = mutableListOf<ObservationLogEntry>()
        val count = minOf(array.length(), MAX_ENTRIES)
        require(!strict || array.length() <= MAX_ENTRIES) { "Zu viele gespeicherte Einträge" }
        for (i in 0 until count) {
            val parsed = runCatching {
                val obj = array.getJSONObject(i)
                ObservationLogEntry.fromJsonObject(obj)
            }
            if (strict) result.add(parsed.getOrThrow()) else parsed.getOrNull()?.let(result::add)
        }
        return result
    }

    @Synchronized
    fun saveEntry(context: Context, entry: ObservationLogEntry): List<ObservationLogEntry> {
        require(entry.photoFileName == null || isSafePhotoFileName(entry.photoFileName)) { "Ungültiger Foto-Dateiname" }
        ObservationLogEntry.fromJsonObject(entry.toJsonObject()) // Validate before touching storage.
        val previous = loadEntries(context)
        val current = previous.filterNot { it.id == entry.id }.toMutableList()
        current.add(0, entry)
        val trimmed = current.sortedByDescending { it.timestampEpochSeconds }.take(MAX_ENTRIES)
        writeEntries(context, trimmed)
        deleteUnreferencedPhotos(context, previous, trimmed)
        return trimmed
    }

    @Synchronized
    fun deleteEntry(context: Context, entryId: String): List<ObservationLogEntry> {
        val current = loadEntries(context)
        val next = current.filterNot { it.id == entryId }
        writeEntries(context, next)
        deleteUnreferencedPhotos(context, current, next)
        return next
    }

    @Synchronized
    fun deleteAllEntries(context: Context) {
        // Commit the empty journal first; never remove photos before the journal write succeeds.
        writeEntries(context, emptyList())
        val photosDir = getPhotosDir(context)
        photosDir.listFiles()?.filter { isSafePhotoFileName(it.name) }?.forEach {
            deletePhotoFile(context, it.name)
        }
    }

    private fun writeEntries(context: Context, entries: List<ObservationLogEntry>) {
        val array = JSONArray()
        entries.forEach { array.put(it.toJsonObject()) }
        val root = JSONObject().apply {
            put("version", 1)
            put("exportedAt", Instant.now().toString())
            put("entries", array)
        }
        val bytes = root.toString(2).toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_JSON_BYTES) { "Das Logbuch überschreitet 10 MB. Bitte zuerst Einträge exportieren." }
        val file = AtomicFile(getStorageFile(context))
        val output = file.startWrite()
        try {
            output.write(bytes)
            file.finishWrite(output)
        } catch (error: Throwable) {
            file.failWrite(output)
            throw error
        }
    }

    fun savePhotoFromUri(context: Context, sourceUri: Uri): String? {
        val fileName = "${UUID.randomUUID()}.jpg"
        val targetFile = runCatching { getPhotoFile(context, fileName) }.getOrNull() ?: return null
        return runCatching {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                targetFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= 20 * 1024 * 1024) { "Foto überschreitet 20 MB" }
                        output.write(buffer, 0, count)
                    }
                }
                fileName
            }
        }.onFailure { targetFile.delete() }.getOrNull()
    }

    private val photoFileNamePattern = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,127}")
    fun isSafePhotoFileName(fileName: String): Boolean = photoFileNamePattern.matches(fileName)

    fun getPhotoFile(context: Context, fileName: String): File {
        require(isSafePhotoFileName(fileName)) { "Ungültiger Foto-Dateiname" }
        val directory = getPhotosDir(context).canonicalFile
        return File(directory, fileName).also {
            require(it.canonicalFile.parentFile == directory && !it.isDirectory) { "Foto liegt außerhalb des Logbuchs" }
        }
    }

    fun deletePhotoFile(context: Context, fileName: String) {
        val targetFile = getPhotoFile(context, fileName)
        if (targetFile.exists()) targetFile.delete()
    }

    private fun deleteUnreferencedPhotos(context: Context, previous: List<ObservationLogEntry>, next: List<ObservationLogEntry>) {
        val retained = next.mapNotNull { it.photoFileName }.toSet()
        previous.mapNotNull { it.photoFileName }.distinct().filterNot { it in retained }.forEach {
            // The journal is already committed; a failed cleanup must not turn success into a retry.
            runCatching { deletePhotoFile(context, it) }
        }
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

    fun exportOalXml(entries: List<ObservationLogEntry>): String {
        return de.projektastra.app.observation.OpenAstronomyLogExport.exportToXml(entries)
    }

    fun exportFormattedText(entries: List<ObservationLogEntry>): String {
        return de.projektastra.app.observation.OpenAstronomyLogExport.exportFormattedTextSummary(entries)
    }

    @Synchronized
    fun importJson(context: Context, jsonText: String, merge: Boolean = true): LogbookImportResult {
        if (jsonText.toByteArray(Charsets.UTF_8).size > MAX_JSON_BYTES) {
            return LogbookImportResult(false, 0, 0, "Die Datei überschreitet das Limit von 10 MB.")
        }
        return runCatching {
            // JSON exports contain no image bytes and cannot claim ownership of local photos.
            val parsed = parseEntriesJson(jsonText).map { it.copy(photoFileName = null) }
            if (parsed.isEmpty()) {
                return LogbookImportResult(false, 0, 0, "Keine gültigen Beobachtungseinträge in den Daten gefunden.")
            }
            val previous = if (merge) loadEntries(context) else runCatching { loadEntries(context) }.getOrDefault(emptyList())
            val existing = if (merge) previous else emptyList()
            val map = existing.associateBy { it.id }.toMutableMap()
            parsed.forEach { map[it.id] = it }
            val combined = map.values.sortedByDescending { it.timestampEpochSeconds }.take(MAX_ENTRIES)
            writeEntries(context, combined)
            deleteUnreferencedPhotos(context, previous, combined)
            LogbookImportResult(true, parsed.size, combined.size, "${parsed.size} Einträge erfolgreich ${if (merge) "zusammengeführt" else "importiert"}.")
        }.getOrElse { e ->
            LogbookImportResult(false, 0, 0, "Fehler beim Einlesen der JSON-Daten: ${e.message}")
        }
    }
}
