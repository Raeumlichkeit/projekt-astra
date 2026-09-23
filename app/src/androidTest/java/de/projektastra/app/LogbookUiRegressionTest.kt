package de.projektastra.app

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.UUID

/** Exercises the real plan card and edit dialog against isolated logbook storage. */
@RunWith(AndroidJUnit4::class)
class LogbookUiRegressionTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = instrumentation.targetContext
    private lateinit var fixture: File
    private lateinit var context: Context
    private lateinit var original: ObservationLogEntry
    private lateinit var photo: File
    private var scenario: ActivityScenario<ComponentActivity>? = null
    private var planWindowId = -1
    private val entries = mutableStateOf(emptyList<ObservationLogEntry>())
    private val preferences = mutableSetOf<String>()

    @Before fun prepare() {
        fixture = File(app.cacheDir, "logbook_ui_${UUID.randomUUID()}").apply { check(mkdirs()) }
        context = object : ContextWrapper(app) {
            override fun getFilesDir() = File(fixture, "files").apply { mkdirs() }
            override fun getCacheDir() = File(fixture, "cache").apply { mkdirs() }
            override fun getSharedPreferences(name: String, mode: Int): android.content.SharedPreferences {
                val scopedName = "${fixture.name}_$name"
                preferences.add(scopedName)
                return super.getSharedPreferences(scopedName, mode)
            }
        }
        photo = ObservationLogbookStore.getPhotoFile(context, "original.jpg")
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        try { photo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) } }
        finally { bitmap.recycle() }
        original = ObservationLogEntry(
            id = "historical-entry", objectCatalogId = "HIP 91262", objectName = "Wega",
            timestampEpochSeconds = 1_725_000_000L, notes = "Alte Notiz", locationName = "Alter Ort",
            latitude = 48.137, longitude = 11.576, photoFileName = photo.name
        )
        entries.value = ObservationLogbookStore.saveEntry(context, original)
    }

    @After fun clean() {
        scenario?.close()
        preferences.forEach { app.deleteSharedPreferences(it) }
        if (::fixture.isInitialized) {
            check(fixture.canonicalFile.parentFile == app.cacheDir.canonicalFile)
            check(fixture.deleteRecursively())
        }
    }

    @Test fun removePhotoThenCancelPreservesOriginalEntryAndPhoto() {
        openEditor()
        scrollUntil { it.text?.toString() == "Entfernen" }
        clickText("Entfernen")
        await { visibleText().contains("Foto auswählen") }
        scrollUntil { it.text?.toString() == "Abbrechen" }
        clickText("Abbrechen")
        await { activeWindowId() == planWindowId }
        assertEquals(original, ObservationLogbookStore.loadEntries(context).single())
        assertTrue("Cancelling must retain the original photo", photo.exists())
        assertTrue("The plan must still render the attached photo", visibleText().contains("Foto ansehen"))
    }

    @Test fun editingOnlyNotesKeepsHistoricalCoordinatesWhenCurrentLocationDiffers() {
        openEditor()
        scrollUntil { it.isEditable && it.text?.toString() == "Alte Notiz" }
        val notes = findNode { it.isEditable && it.text?.toString() == "Alte Notiz" }
        val replacement = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "Neue Notiz")
        }
        assertTrue(notes.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, replacement))
        scrollUntil { it.text?.toString() == "Speichern" }
        clickText("Speichern")
        await { activeWindowId() == planWindowId }
        val saved = ObservationLogbookStore.loadEntries(context).single()
        assertEquals("Neue Notiz", saved.notes)
        assertEquals(original.latitude, saved.latitude)
        assertEquals(original.longitude, saved.longitude)
        assertEquals(original.locationName, saved.locationName)
        assertEquals(original.photoFileName, saved.photoFileName)
        assertTrue(photo.exists())
    }

    @Test fun failedSaveLeavesOriginalAndPhotoAndShowsError() {
        openEditor(failSave = true)
        scrollUntil { it.text?.toString() == "Speichern" }
        clickText("Speichern")
        await { visibleText().contains("Speichern fehlgeschlagen: fixture failure") }
        assertNotEquals("Failed save must keep the editor open", planWindowId, activeWindowId())
        assertTrue(visibleText().contains("Speichern"))
        assertEquals(original, ObservationLogbookStore.loadEntries(context).single())
        assertTrue(photo.exists())
    }

    private fun openEditor(failSave: Boolean = false) {
        val catalogs = SkyCatalogStore({ emptyList() }, { emptyList() }, { emptyList() })
        scenario = ActivityScenario.launch(ComponentActivity::class.java).also { scene ->
            scene.onActivity { activity ->
                activity.setContent {
                    CompositionLocalProvider(
                        LocalContext provides context,
                        LocalActivityResultRegistryOwner provides activity
                    ) {
                        MaterialTheme(colorScheme = darkColorScheme()) {
                            Surface {
                                ObservationPlanScreen(
                                    location = GeoPoint(52.52, 13.405, 34.0), favoriteIds = emptySet(),
                                    openObject = {}, openEventTime = {}, savedEvents = emptyList(),
                                    reminderHours = 1, notificationsGranted = true, setReminderHours = {},
                                    removeFavorite = {}, removeEvent = {}, requestNotifications = {},
                                    catalogStore = catalogs, logbookEntries = entries.value,
                                    onSaveLogEntry = { updated ->
                                        if (failSave) throw IOException("fixture failure")
                                        entries.value = ObservationLogbookStore.saveEntry(context, updated)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        await { visibleText().contains("Beobachtungsplaner") }
        scrollUntil { it.contentDescription?.toString() == "Bearbeiten" }
        planWindowId = activeWindowId()
        clickNode(findNode { it.contentDescription?.toString() == "Bearbeiten" })
        await { activeWindowId() != planWindowId && visibleText().contains("Beobachtung bearbeiten") }
    }

    private fun nodes(node: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> =
        if (node == null) emptyList() else listOf(node) + (0 until node.childCount).flatMap { nodes(node.getChild(it)) }

    private fun visibleNodes() = nodes(instrumentation.uiAutomation.rootInActiveWindow)
    private fun activeWindowId() = instrumentation.uiAutomation.rootInActiveWindow?.windowId ?: -1
    private fun visibleText() = visibleNodes().mapNotNull { it.text?.toString() }.joinToString("\n")
    private fun findNode(match: (AccessibilityNodeInfo) -> Boolean) =
        checkNotNull(visibleNodes().firstOrNull { it.isVisibleToUser && match(it) }) { "UI node missing: ${visibleText()}" }

    private fun clickText(label: String) = clickNode(findNode { it.text?.toString() == label })

    private fun clickNode(node: AccessibilityNodeInfo) {
        var clickable = node
        while (!clickable.isClickable) clickable = checkNotNull(clickable.parent) { "Not clickable: ${node.text}" }
        assertTrue("Click failed: ${node.text}", clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        instrumentation.waitForIdleSync()
    }

    private fun scrollUntil(match: (AccessibilityNodeInfo) -> Boolean) {
        repeat(40) {
            if (visibleNodes().any { node -> node.isVisibleToUser && match(node) }) return
            val scrollable = visibleNodes().filter { it.isScrollable }.maxByOrNull {
                Rect().also(it::getBoundsInScreen).height()
            }
            checkNotNull(scrollable) { "No scrollable UI: ${visibleText()}" }
            val bounds = Rect().also(scrollable::getBoundsInScreen)
            val x = bounds.centerX().toFloat()
            val top = bounds.top + bounds.height() * .25f
            val bottom = bounds.top + bounds.height() * .75f
            val downTime = SystemClock.uptimeMillis()
            fun touch(action: Int, y: Float) {
                val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
                event.source = InputDevice.SOURCE_TOUCHSCREEN
                try { check(instrumentation.uiAutomation.injectInputEvent(event, true)) { "Swipe injection failed" } }
                finally { event.recycle() }
            }
            touch(MotionEvent.ACTION_DOWN, bottom)
            (1..16).forEach { step ->
                SystemClock.sleep(16)
                touch(MotionEvent.ACTION_MOVE, bottom + (top - bottom) * step / 16f)
            }
            touch(MotionEvent.ACTION_UP, top)
            instrumentation.waitForIdleSync()
            SystemClock.sleep(80)
        }
        fail("UI target not found after scrolling: ${visibleText()}")
    }

    private fun await(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 8000
        while (!condition()) {
            check(SystemClock.uptimeMillis() < deadline) { "UI timed out: ${visibleText()}" }
            SystemClock.sleep(100)
        }
    }
}
