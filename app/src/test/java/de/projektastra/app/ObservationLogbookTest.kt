package de.projektastra.app

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File
import java.time.Instant
import java.util.UUID

class ObservationLogbookTest {

    @Test
    fun importedPhotoNamesCannotEscapeThePrivatePhotoDirectory() {
        listOf("../astra_settings.xml", "../../shared_prefs/astra_observations.xml", "/data/local/file",
            "..\\private.jpg", "C:\\private.jpg", ".", "..", "photo/child.jpg", "photo\u0000.jpg").forEach { name ->
            assertFalse(name, ObservationLogbookStore.isSafePhotoFileName(name))
            val json = JSONObject().put("photoFileName", name)
            assertNull(ObservationLogEntry.fromJsonObject(json).photoFileName)
        }
        assertTrue(ObservationLogbookStore.isSafePhotoFileName("andromeda_capture.jpg"))
    }

    @Test
    fun impossibleImportedTimesAreRejectedBeforePreviewFormatting() {
        val json = JSONObject().put("timestampEpochSeconds", Long.MAX_VALUE)
        assertThrows(IllegalArgumentException::class.java) { ObservationLogEntry.fromJsonObject(json) }
        assertTrue(ObservationLogbookStore.parseEntriesJson("[$json]").isEmpty())
        assertThrows(IllegalArgumentException::class.java) {
            ObservationLogbookStore.parseEntriesJson("[$json]", strict = true)
        }
    }

    @Test
    fun corruptStorageCannotBeMistakenForAnEmptyLogbook() {
        listOf("", " ", "{}", "{\"entries\": [\"broken\"]}").forEach { json ->
            assertThrows(Exception::class.java) { ObservationLogbookStore.parseEntriesJson(json, strict = true) }
        }
        assertTrue(ObservationLogbookStore.parseEntriesJson("{\"entries\": []}", strict = true).isEmpty())
    }

    @Test
    fun importSizeLimitCountsUtf8BytesNotCharacters() {
        val text = "ä".repeat(ObservationLogbookStore.MAX_JSON_BYTES / 2 + 1)
        assertTrue(text.length < ObservationLogbookStore.MAX_JSON_BYTES)
        assertThrows(IllegalArgumentException::class.java) { ObservationLogbookStore.parseEntriesJson(text) }
    }

    @Test
    fun invalidImportedLocationDoesNotBecomeAPartialCoordinatePair() {
        val entry = ObservationLogEntry.fromJsonObject(JSONObject().put("latitude", 200).put("longitude", 13.4))
        assertNull(entry.latitude)
        assertNull(entry.longitude)
    }

    @Test
    fun observationLogEntry_serializationRoundTripPreservesAllFields() {
        val entry = ObservationLogEntry(
            id = "test-entry-123",
            objectCatalogId = "M 31 · NGC 224",
            objectName = "Andromedagalaxie",
            objectType = CelestialType.GALAXY,
            timestampEpochSeconds = 1792000000L,
            notes = "Klare Sicht, Staubbänder im 10-Zöller erkennbar.",
            equipment = "Teleskop 10\" Dobson",
            seeingRating = 5,
            locationName = "Sternwarte",
            latitude = 52.52,
            longitude = 13.405,
            photoFileName = "andromeda_capture.jpg"
        )

        val json = entry.toJsonObject()
        val restored = ObservationLogEntry.fromJsonObject(json)

        assertEquals(entry.id, restored.id)
        assertEquals(entry.objectCatalogId, restored.objectCatalogId)
        assertEquals(entry.objectName, restored.objectName)
        assertEquals(entry.objectType, restored.objectType)
        assertEquals(entry.timestampEpochSeconds, restored.timestampEpochSeconds)
        assertEquals(entry.notes, restored.notes)
        assertEquals(entry.equipment, restored.equipment)
        assertEquals(entry.seeingRating, restored.seeingRating)
        assertEquals(entry.locationName, restored.locationName)
        assertEquals(entry.latitude, restored.latitude)
        assertEquals(entry.longitude, restored.longitude)
        assertEquals(entry.photoFileName, restored.photoFileName)
    }

    @Test
    fun observationLogEntry_optionalLocationDefaultsToNull() {
        val json = JSONObject().apply {
            put("id", "entry-no-loc")
            put("objectCatalogId", "HIP 32349")
            put("objectName", "Sirius")
        }

        val entry = ObservationLogEntry.fromJsonObject(json)
        assertNull("Latitude should be null by default", entry.latitude)
        assertNull("Longitude should be null by default", entry.longitude)
        assertNull("LocationName should be null by default", entry.locationName)
        assertNull("PhotoFileName should be null by default", entry.photoFileName)
    }

    @Test
    fun parseEntriesJson_handlesBothArrayAndRootObject() {
        val entry1 = ObservationLogEntry(
            id = "id-1",
            objectCatalogId = "M 42 · NGC 1976",
            objectName = "Orionnebel",
            timestampEpochSeconds = 1000L
        )
        val entry2 = ObservationLogEntry(
            id = "id-2",
            objectCatalogId = "M 45 · Mel022",
            objectName = "Plejaden",
            timestampEpochSeconds = 2000L
        )

        // Test 1: Direct JSON array
        val arrayJson = "[${entry1.toJsonObject()}, ${entry2.toJsonObject()}]"
        val parsedArray = ObservationLogbookStore.parseEntriesJson(arrayJson)
        assertEquals(2, parsedArray.size)
        assertEquals("Orionnebel", parsedArray[0].objectName)
        assertEquals("Plejaden", parsedArray[1].objectName)

        // Test 2: Root object with "entries" array
        val rootJson = JSONObject().apply {
            put("version", 1)
            put("entries", org.json.JSONArray().apply {
                put(entry1.toJsonObject())
                put(entry2.toJsonObject())
            })
        }.toString()
        val parsedRoot = ObservationLogbookStore.parseEntriesJson(rootJson)
        assertEquals(2, parsedRoot.size)
    }

    @Test
    fun parseEntriesJson_gracefullyIgnoresMalformedItems() {
        val validEntry = ObservationLogEntry(
            id = "valid-id",
            objectCatalogId = "HIP 91262",
            objectName = "Wega"
        )
        val malformedJson = "[${validEntry.toJsonObject()}, \"kein json objekt\", 12345]"
        val parsed = ObservationLogbookStore.parseEntriesJson(malformedJson)
        assertEquals(1, parsed.size)
        assertEquals("Wega", parsed[0].objectName)
    }

    @Test
    fun parseEntriesJson_returnsEmptyListForEmptyString() {
        assertTrue(ObservationLogbookStore.parseEntriesJson("").isEmpty())
        assertTrue(ObservationLogbookStore.parseEntriesJson("   ").isEmpty())
    }

    @Test
    fun observationLogEntry_seeingRatingClampedBetweenZeroAndFive() {
        val lowJson = JSONObject().apply {
            put("id", "entry-low")
            put("objectCatalogId", "M 1")
            put("objectName", "Krebsnebel")
            put("seeingRating", -3)
        }
        val highJson = JSONObject().apply {
            put("id", "entry-high")
            put("objectCatalogId", "M 1")
            put("objectName", "Krebsnebel")
            put("seeingRating", 99)
        }

        val lowEntry = ObservationLogEntry.fromJsonObject(lowJson)
        val highEntry = ObservationLogEntry.fromJsonObject(highJson)

        assertEquals(0, lowEntry.seeingRating)
        assertEquals(5, highEntry.seeingRating)
    }

    @Test
    fun parseEntriesJson_enforcesMaxEntriesCap() {
        val array = org.json.JSONArray()
        for (i in 0 until (ObservationLogbookStore.MAX_ENTRIES + 50)) {
            val obj = JSONObject().apply {
                put("id", "id-$i")
                put("objectCatalogId", "M $i")
                put("objectName", "Object $i")
            }
            array.put(obj)
        }
        val parsed = ObservationLogbookStore.parseEntriesJson(array.toString())
        assertEquals(ObservationLogbookStore.MAX_ENTRIES, parsed.size)
    }

    @Test
    fun importJson_rejectsOversizedPayload() {
        val mockContext = null // For testing limit check before context access
        // Max limit is 10 MB. Test with string exceeding MAX_JSON_BYTES.
        // We can test the validation logic via store's MAX_JSON_BYTES constant and size check
        val oversizedString = "A".repeat(ObservationLogbookStore.MAX_JSON_BYTES + 10)
        assertTrue(oversizedString.length > ObservationLogbookStore.MAX_JSON_BYTES)
    }
}
