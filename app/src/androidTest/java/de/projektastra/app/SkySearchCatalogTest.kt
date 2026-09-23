package de.projektastra.app

import android.content.ContextWrapper
import android.content.res.AssetManager
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class SkySearchCatalogTest {
    @Test fun realLoaderErrorsStayRetryableForEveryCatalogStage() = kotlinx.coroutines.runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val unavailable = object : ContextWrapper(context) {
            override fun getAssets(): AssetManager = throw IOException("test: asset unavailable")
        }
        for (brokenStage in 0..2) {
            var failing = true
            val calls = IntArray(3)
            fun source(stage: Int): android.content.Context {
                calls[stage]++
                return if (failing && stage == brokenStage) unavailable else context
            }
            val store = SkyCatalogStore({ StarCatalog.load(source(0)) }, { DeepSkyCatalog.load(source(1)) },
                { IauBoundaryCatalog.load(source(2)) })
            store.load()
            val partial = store.snapshot.value
            assertTrue("Stage $brokenStage failure must reach the store", partial.failed)
            assertFalse(partial.complete)
            assertNull(partial.searchIndex)
            assertNull(listOf(partial.stars, partial.deepSky, partial.boundaries)[brokenStage])
            failing = false
            store.load()
            assertTrue(store.snapshot.value.complete)
            assertFalse(store.snapshot.value.failed)
            assertTrue(store.snapshot.value.stars!!.size > 5000)
            assertTrue(store.snapshot.value.deepSky!!.size > 1000)
            assertEquals(88, store.snapshot.value.boundaries!!.map { it.abbreviation }.distinct().size)
            for (stage in 0..2) assertEquals(if (stage == brokenStage) 2 else 1, calls[stage])
        }
    }

    @Test fun observationPlanRetriesRealLoaderAndReusesCacheWhenReopened() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val attempts = AtomicInteger()
        val removals = AtomicInteger()
        val unavailable = object : ContextWrapper(context) {
            override fun getAssets(): AssetManager = throw IOException("test: asset unavailable")
        }
        val store = SkyCatalogStore({
            StarCatalog.load(if (attempts.incrementAndGet() == 1) unavailable else context)
        }, { DeepSkyCatalog.load(context) }, { IauBoundaryCatalog.load(context) })
        val showingPlan = mutableStateOf(true)
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    MaterialTheme(colorScheme = darkColorScheme()) {
                        Surface {
                            if (showingPlan.value) ObservationPlanScreen(
                                location = null, favoriteIds = setOf("HIP 11767"), openObject = {}, openEventTime = {},
                                savedEvents = emptyList(), reminderHours = 1, notificationsGranted = true,
                                setReminderHours = {}, removeFavorite = { removals.incrementAndGet() }, removeEvent = {},
                                requestNotifications = {}, catalogStore = store)
                            else Text("Andere Ansicht")
                        }
                    }
                }
            }
            fun nodes(node: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> =
                if (node == null) emptyList() else listOf(node) + (0 until node.childCount).flatMap { nodes(node.getChild(it)) }
            fun texts() = nodes(instrumentation.uiAutomation.rootInActiveWindow).mapNotNull { it.text?.toString() }.joinToString("\n")
            fun await(condition: () -> Boolean) {
                val deadline = SystemClock.uptimeMillis() + 10000
                while (!condition()) {
                    check(SystemClock.uptimeMillis() < deadline) { "Plan UI timed out: ${texts()}" }
                    SystemClock.sleep(100)
                }
            }
            await { store.snapshot.value.failed && texts().contains("Katalog erneut laden") }
            assertTrue(texts().contains("1 vorgemerkte Objekte"))
            assertFalse(texts().contains("Tippe ein Objekt in der Sternkarte"))
            var retry = nodes(instrumentation.uiAutomation.rootInActiveWindow).first { it.text?.toString() == "Katalog erneut laden" }
            while (!retry.isClickable) retry = checkNotNull(retry.parent)
            assertTrue(retry.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            await { store.snapshot.value.complete && texts().contains("Polaris") }
            val complete = store.snapshot.value
            scenario.onActivity { showingPlan.value = false }
            await { texts().contains("Andere Ansicht") }
            scenario.onActivity { showingPlan.value = true }
            await { texts().contains("Polaris") }
            assertSame(complete, store.snapshot.value)
            assertEquals(2, attempts.get())
            assertEquals(0, removals.get())
        }
    }

    @Test fun processCacheLoadsBundledAssetsAndReusesTheSameSnapshot() = kotlinx.coroutines.runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = ProcessSkyCatalogs.get(context)
        store.load()
        val snapshot = store.snapshot.value
        assertTrue(snapshot.complete)
        assertTrue(snapshot.stars!!.size > 5000)
        assertTrue(snapshot.deepSky!!.size > 1000)
        assertEquals(88, snapshot.boundaries!!.map { it.abbreviation }.distinct().size)
        assertEquals("HIP 91262", snapshot.searchIndex!!.search("Wega").single().id)
        assertTrue(snapshot.searchIndex.search("Andromeda").any { it.typeLabel == "Galaxie" })
        assertTrue(snapshot.searchIndex.search("Andromeda").any { it.typeLabel == "Sternbild" })
        val reopened = ProcessSkyCatalogs.get(context)
        reopened.load()
        assertSame(store, reopened)
        assertSame(snapshot, reopened.snapshot.value)
    }


    @Test fun bundledCatalogResolvesAliasesRegionsAndMovingBodiesOffline() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val stars = StarCatalog.load(context)
        val deepSky = DeepSkyCatalog.load(context)
        val planets = SolarSystemCatalog.at(GeoPoint(52.52, 13.405, 34.0), Instant.parse("2026-09-06T20:00:00Z"))
        assertTrue("Full star catalog must load", stars.size > 5000)
        assertTrue("Full deep sky catalog must load", deepSky.size > 1000)
        val regions = constellationSearchTargets(stars)
        val index = SkySearchIndex((stars + deepSky + planets).map { SkySearchTarget(it) } + regions)
        // The Saturn Nebula is also a valid partial match; the exact planet name must lead.
        assertEquals("Saturn", index.search("saturn").first().name)
        assertEquals("HIP 91262", index.search("Wega").single().id)
        assertEquals("HIP 11767", index.search("Polarstern").single().id)
        assertEquals("M 31", index.search("NGC224").single().objectData.messierId)
        assertEquals("M 45", index.search("Plejaden").single().objectData.messierId)
        assertTrue(index.search("Andromeda").any { it.typeLabel == "Galaxie" })
        assertTrue(index.search("Andromeda").any { it.typeLabel == "Sternbild" })
        assertEquals(88, regions.count { !it.asterism })
        assertEquals("Sternmuster", index.search("Großer Wagen").single().typeLabel)
    }
}
