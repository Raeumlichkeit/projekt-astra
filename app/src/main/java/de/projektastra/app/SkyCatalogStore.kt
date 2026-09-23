package de.projektastra.app

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Only bundled, location-independent data. Never retains an Activity, query, or observer. */
internal data class SkyCatalogSnapshot(
    val stars: List<CelestialObject>? = null,
    val deepSky: List<CelestialObject>? = null,
    val boundaries: List<IauConstellationBoundary>? = null,
    val searchIndex: SkySearchIndex? = null,
    val failed: Boolean = false
) {
    val complete: Boolean get() = searchIndex != null
    val objectsReady: Boolean get() = stars != null && deepSky != null
}

/** Serializes concurrent consumers; completed stages survive cancellation and tab changes. */
internal class SkyCatalogStore(
    private val loadStars: () -> List<CelestialObject>,
    private val loadDeepSky: () -> List<CelestialObject>,
    private val loadBoundaries: () -> List<IauConstellationBoundary>
) {
    private val mutex = Mutex()
    private val mutableSnapshot = MutableStateFlow(SkyCatalogSnapshot())
    val snapshot = mutableSnapshot.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (snapshot.value.complete) return@withLock
            mutableSnapshot.value = snapshot.value.copy(failed = false)
            try {
                if (snapshot.value.stars == null) {
                    mutableSnapshot.value = snapshot.value.copy(stars = loadStars())
                }
                currentCoroutineContext().ensureActive()
                if (snapshot.value.deepSky == null) {
                    mutableSnapshot.value = snapshot.value.copy(deepSky = loadDeepSky())
                }
                currentCoroutineContext().ensureActive()
                if (snapshot.value.boundaries == null) {
                    mutableSnapshot.value = snapshot.value.copy(boundaries = loadBoundaries())
                }
                currentCoroutineContext().ensureActive()
                val stars = checkNotNull(snapshot.value.stars)
                val objects = stars + checkNotNull(snapshot.value.deepSky)
                val index = SkySearchIndex(objects.map { SkySearchTarget(it) } + constellationSearchTargets(stars))
                mutableSnapshot.value = snapshot.value.copy(searchIndex = index)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Keep the base sky usable and retry only unfinished stages on demand.
                mutableSnapshot.value = snapshot.value.copy(failed = true)
            }
        }
    }
}

internal object ProcessSkyCatalogs {
    @Volatile private var store: SkyCatalogStore? = null

    fun get(context: Context): SkyCatalogStore = store ?: synchronized(this) {
        store ?: context.applicationContext.let { app ->
            SkyCatalogStore({ StarCatalog.load(app) }, { DeepSkyCatalog.load(app) },
                { IauBoundaryCatalog.load(app) }).also { store = it }
        }
    }
}

@Composable
internal fun rememberSkyCatalogs(store: SkyCatalogStore, retry: Int): SkyCatalogSnapshot {
    val catalogs by store.snapshot.collectAsState()
    LaunchedEffect(store, retry) { store.load() }
    return catalogs
}
