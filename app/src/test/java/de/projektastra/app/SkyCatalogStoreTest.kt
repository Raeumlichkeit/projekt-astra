package de.projektastra.app

import io.github.cosinekitty.astronomy.Body
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class SkyCatalogStoreTest {
    private val star = CelestialObject("Wega", "HIP 91262", 18.6156, 38.7837, 0.03, 25.0, "A0V")

    @Test fun invalidBoundaryDataCannotBecomeAnEmptySuccessfulStage() = runBlocking {
        for (broken in listOf("", "# empty", "not a boundary", "0 0 AND\n1 1 AND TUC", "NaN 0 AND")) {
            val store = SkyCatalogStore({ listOf(star) }, { emptyList() }, { IauBoundaryCatalog.parse(broken) })
            store.load()
            assertTrue(store.snapshot.value.failed)
            assertNull(store.snapshot.value.boundaries)
            assertFalse(store.snapshot.value.complete)
            assertEquals(listOf(star), store.snapshot.value.stars)
        }
    }

    @Test fun completedSnapshotIsReusedAcrossConcurrentAndRepeatedOpens() = runBlocking {
        val calls = AtomicInteger()
        val callerThread = Thread.currentThread()
        val store = SkyCatalogStore({
            assertNotSame(callerThread, Thread.currentThread())
            calls.incrementAndGet()
            listOf(star)
        }, { calls.incrementAndGet(); emptyList() }, { calls.incrementAndGet(); emptyList() })
        (1..4).map { async { store.load() } }.awaitAll()
        val first = store.snapshot.value
        store.load()
        assertSame(first, store.snapshot.value)
        assertEquals(3, calls.get())
        assertTrue(first.complete)
        assertTrue(first.objectsReady)
        assertEquals("HIP 91262", first.searchIndex!!.search("wega").single().id)
    }

    @Test fun laterFailureRetainsStarsAndRetryOnlyLoadsUnfinishedStages() = runBlocking {
        val starCalls = AtomicInteger()
        val deepCalls = AtomicInteger()
        val store = SkyCatalogStore({ starCalls.incrementAndGet(); listOf(star) }, {
            if (deepCalls.incrementAndGet() == 1) error("disk")
            emptyList()
        }, { emptyList() })
        store.load()
        assertEquals(listOf(star), store.snapshot.value.stars)
        assertNull(store.snapshot.value.deepSky)
        assertTrue(store.snapshot.value.failed)
        assertFalse(store.snapshot.value.objectsReady)
        store.load()
        assertTrue(store.snapshot.value.complete)
        assertFalse(store.snapshot.value.failed)
        assertEquals(1, starCalls.get())
        assertEquals(2, deepCalls.get())
    }

    @Test fun starsArePublishedWhileOptionalStageIsBlockedAndCancellationKeepsProgress() = runBlocking {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val starCalls = AtomicInteger()
        val deepCalls = AtomicInteger()
        val store = SkyCatalogStore({ starCalls.incrementAndGet(); listOf(star) }, {
            deepCalls.incrementAndGet()
            entered.countDown()
            check(release.await(5, TimeUnit.SECONDS))
            emptyList()
        }, { emptyList() })
        val job = launch(Dispatchers.Default) { store.load() }
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            assertEquals(listOf(star), store.snapshot.value.stars)
            assertFalse(store.snapshot.value.complete)
            assertNull(store.snapshot.value.deepSky)
            job.cancel()
        } finally {
            release.countDown()
            job.join()
        }
        assertFalse(store.snapshot.value.failed)
        store.load()
        assertTrue(store.snapshot.value.complete)
        assertEquals(1, starCalls.get())
        assertEquals(1, deepCalls.get())
    }

    @Test fun cancellationIsNotConvertedToFailure() = runBlocking {
        val store = SkyCatalogStore({ throw CancellationException("closed") }, { emptyList() }, { emptyList() })
        try {
            store.load()
            fail("Cancellation must propagate")
        } catch (_: CancellationException) {
            assertFalse(store.snapshot.value.failed)
            assertFalse(store.snapshot.value.complete)
        }
    }
}

class SkySearchAdditionalTargetsTest {
    private val saturnNebula = CelestialObject("Saturnnebel", "NGC 7009", 21.0689, -11.3869, 8.0, 0.0, "",
        objectType = CelestialType.PLANETARY_NEBULA)
    private val neptune = CelestialObject("Neptun", "Neptun", 0.0, 0.0, 7.8, 30.1, "",
        solarBody = Body.Neptune)

    @Test
    fun movingBodiesAreSearchedWhileBundledIndexIsIncomplete() {
        val index = SkySearchIndex(emptyList())
        assertEquals("Neptun", index.search("neptun", 40, listOf(SkySearchTarget(neptune))).single().id)
        assertTrue(index.search("saturn", 40, listOf(SkySearchTarget(neptune))).isEmpty())
    }

    @Test
    fun exactMovingBodyBeatsPartialCatalogMatchLikeTheBundledCase() {
        val saturnNebulaOnly = SkySearchIndex(listOf(SkySearchTarget(saturnNebula)))
        val results = saturnNebulaOnly.search("saturn", 40, listOf(SkySearchTarget(
            CelestialObject("Saturn", "Saturn", 0.0, 0.0, 0.6, 9.5, "", solarBody = Body.Saturn))))
        assertEquals("Saturn", results.first().id)
    }
}
