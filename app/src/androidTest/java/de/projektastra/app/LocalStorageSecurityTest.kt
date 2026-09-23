package de.projektastra.app

import android.content.Context
import android.content.ContextWrapper
import android.util.AtomicFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Real Android storage checks, isolated from user observations and preferences. */
@RunWith(AndroidJUnit4::class)
class LocalStorageSecurityTest {
    private val app = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefix = "storage_test_${UUID.randomUUID()}_"
    private lateinit var fixture: File
    private lateinit var context: Context

    @Before fun prepare() {
        fixture = File(app.cacheDir, prefix).apply { check(mkdirs()) }
        context = object : ContextWrapper(app) {
            override fun getFilesDir() = File(fixture, "files").apply { mkdirs() }
            override fun getSharedPreferences(name: String, mode: Int) =
                super.getSharedPreferences(prefix + name, mode)
        }
    }

    @After fun clean() {
        context.getSharedPreferences(LocationStore.PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        check(fixture.canonicalFile.parentFile == app.cacheDir.canonicalFile)
        fixture.deleteRecursively()
    }

    private fun entry(id: String = "one", photo: String? = null) = ObservationLogEntry(
        id = id, objectCatalogId = "HIP 91262", objectName = "Wega", photoFileName = photo
    )

    @Test fun traversalImportAndPhotoAccessCannotTouchOtherPrivateFiles() {
        val protected = File(fixture, "protected.xml").apply { writeText("must survive") }
        val json = entry().toJsonObject().put("photoFileName", "../../protected.xml")
        val result = ObservationLogbookStore.importJson(context, "[$json]")
        assertTrue(result.message, result.success)
        assertNull(ObservationLogbookStore.loadEntries(context).single().photoFileName)
        assertThrows(IllegalArgumentException::class.java) {
            ObservationLogbookStore.deletePhotoFile(context, "../../protected.xml")
        }
        ObservationLogbookStore.deleteEntry(context, "one")
        assertEquals("must survive", protected.readText())
    }

    @Test fun importedJsonCannotClaimAnExistingPhotoByName() {
        val photo = ObservationLogbookStore.getPhotoFile(context, "existing.jpg").apply { writeText("photo") }
        ObservationLogbookStore.saveEntry(context, entry("original", photo.name))
        val result = ObservationLogbookStore.importJson(context, "[${entry("imported", photo.name).toJsonObject()}]")
        assertTrue(result.success)
        ObservationLogbookStore.deleteEntry(context, "imported")
        assertTrue(photo.exists())
        assertEquals(photo.name, ObservationLogbookStore.loadEntries(context).single().photoFileName)
    }

    @Test fun interruptedAtomicWriteKeepsTheLastCommittedJournal() {
        ObservationLogbookStore.saveEntry(context, entry())
        val atomic = AtomicFile(File(context.filesDir, "logbook_entries.json"))
        val partial = atomic.startWrite()
        partial.write("{\"entries\":[".toByteArray())
        atomic.failWrite(partial)
        assertEquals("one", ObservationLogbookStore.loadEntries(context).single().id)
    }

    @Test fun corruptJournalBlocksMutationInsteadOfLosingOlderData() {
        val file = File(context.filesDir, "logbook_entries.json").apply { writeText("{damaged but recoverable") }
        assertThrows(Exception::class.java) { ObservationLogbookStore.loadEntries(context) }
        assertThrows(Exception::class.java) { ObservationLogbookStore.saveEntry(context, entry()) }
        assertFalse(ObservationLogbookStore.importJson(context, "[${entry().toJsonObject()}]", merge = true).success)
        assertEquals("{damaged but recoverable", file.readText())
    }

    @Test fun failedJournalDeleteDoesNotDeleteItsPhoto() {
        val photo = ObservationLogbookStore.getPhotoFile(context, "owned.jpg").apply { writeText("keep") }
        ObservationLogbookStore.saveEntry(context, entry(photo = photo.name))
        File(context.filesDir, "logbook_entries.json").writeText("corrupt")
        assertThrows(Exception::class.java) { ObservationLogbookStore.deleteEntry(context, "one") }
        assertEquals("keep", photo.readText())
    }

    @Test fun sharedPhotoIsRemovedOnlyAfterItsLastEntryIsDeleted() {
        val photo = ObservationLogbookStore.getPhotoFile(context, "shared.jpg").apply { writeText("photo") }
        ObservationLogbookStore.saveEntry(context, entry("one", photo.name))
        ObservationLogbookStore.saveEntry(context, entry("two", photo.name))
        ObservationLogbookStore.deleteEntry(context, "one")
        assertTrue(photo.exists())
        ObservationLogbookStore.deleteEntry(context, "two")
        assertFalse(photo.exists())
    }

    @Test fun impossibleTimestampImportFailsWithoutChangingExistingData() {
        ObservationLogbookStore.saveEntry(context, entry())
        val bad = entry("bad").toJsonObject().put("timestampEpochSeconds", Long.MAX_VALUE)
        assertFalse(ObservationLogbookStore.importJson(context, "[$bad]").success)
        assertEquals("one", ObservationLogbookStore.loadEntries(context).single().id)
    }

    @Test fun gpsPermissionAloneDoesNotPersistLocation() {
        val point = GeoPoint(52.52, 13.405, 34.0)
        assertFalse(LocationStore.isRememberEnabled(context))
        LocationStore.saveLocation(context, point)
        assertNull(LocationStore.getSavedLocation(context))
        LocationStore.setRememberEnabled(context, true)
        LocationStore.saveLocation(context, point)
        assertEquals(point, LocationStore.getSavedLocation(context))
        LocationStore.setRememberEnabled(context, false)
        assertNull(LocationStore.getSavedLocation(context))
        val prefs = context.getSharedPreferences(LocationStore.PREFS, Context.MODE_PRIVATE)
        assertFalse(prefs.contains(LocationStore.KEY_LATITUDE))
        assertFalse(prefs.contains(LocationStore.KEY_LONGITUDE))
    }

    @Test fun legacyCoordinatesWithoutExplicitOptInAreRemoved() {
        val prefs = context.getSharedPreferences(LocationStore.PREFS, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(LocationStore.KEY_HAS_SAVED, true)
            .putString(LocationStore.KEY_LATITUDE, "52.52")
            .putString(LocationStore.KEY_LONGITUDE, "13.405").commit()
        assertNull(LocationStore.getSavedLocation(context))
        assertFalse(prefs.contains(LocationStore.KEY_LATITUDE))
        assertFalse(prefs.contains(LocationStore.KEY_LONGITUDE))
    }
}
