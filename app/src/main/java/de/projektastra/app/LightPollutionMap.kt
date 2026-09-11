package de.projektastra.app

import android.content.Context
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LifecycleStartEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

/** All map coordinates, including the observer marker, use the online consent's coarse precision. */
internal fun lightPollutionMapHtml(
    context: Context,
    observer: GeoPoint,
    redLight: Boolean,
    currentYear: Int = LocalDate.now().year
): String {
    require(observer.latitude.isFinite() && observer.longitude.isFinite())
    val approximate = NetworkPolicy.roundedLocation(observer)
    fun asset(name: String) = context.assets.open(name).bufferedReader().use { it.readText() }
    return asset("light_pollution_map.html")
        .replace("__LEAFLET_CSS__", asset("leaflet-1.9.4.css"))
        .replace("__LEAFLET_JS__", asset("leaflet-1.9.4.js"))
        .replace("__ASTRA_LAT__", approximate.latitude.toString())
        .replace("__ASTRA_LON__", approximate.longitude.toString())
        .replace("__ASTRA_RED_LIGHT__", redLight.toString())
        .replace("__ASTRA_YEAR__", currentYear.toString())
        .replace("__SCRIPT_NONCE__", UUID.randomUUID().toString())
}

@Composable
internal fun LightPollutionSection(observer: GeoPoint, demoLocation: Boolean, redLight: Boolean) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    var cacheRevision by remember { mutableIntStateOf(0) }
    var estimate by remember(observer) { mutableStateOf<LightPollutionState>(LightPollutionState.Loading) }
    var cacheMessage by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    LifecycleStartEffect(observer, refresh, cacheRevision) {
        var active = true
        LightPollutionRepository.load(observer, forceRefresh = refresh > 0) {
            if (active) estimate = it
        }
        onStopOrDispose { active = false }
    }
    Text(
        if (demoLocation) "Demo-Standort Berlin · Online-Karte auf 0,01° gerundet"
        else "Dein Beobachtungsort · Online-Karte auf 0,01° gerundet",
        style = MaterialTheme.typography.labelLarge
    )
    when (val result = estimate) {
        LightPollutionState.Loading -> Text("Lichtindex für den Beobachtungsort wird geladen …")
        LightPollutionState.Unavailable -> Text(
            "Lichtindex derzeit nicht verfügbar. Die Karte lässt sich unabhängig davon bedienen.",
            color = MaterialTheme.colorScheme.secondary
        )
        is LightPollutionState.Ready -> LightPollutionEstimateSummary(result.estimate)
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { expanded = true }) {
            Icon(Icons.Rounded.Fullscreen, contentDescription = null)
            Text("Karte vergrößern")
        }
        TextButton(onClick = { refresh++ }) { Text("Erneut laden") }
        TextButton(onClick = {
            cacheMessage = if (runCatching { PublicTileCache.clear(context) }.isSuccess) {
                LightPollutionRepository.clear()
                cacheRevision++
                "Lokaler Kartencache gelöscht. Die Karte lädt neu."
            } else "Kartencache konnte nicht vollständig gelöscht werden. Bitte erneut versuchen."
        }) { Text("Kartencache löschen") }
    }
    cacheMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    key(cacheRevision) {
        // Keep one document during fullscreen transitions, so zoom and comparisons stay in memory.
        val document = rememberLightPollutionDocument(observer, redLight, refresh)
        if (expanded) {
            SkySheetTheme(redLight) {
                Dialog(onDismissRequest = { expanded = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                    SkySheetSystemBars(redLight)
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                            Row(Modifier.fillMaxWidth().padding(start = 16.dp)) {
                                Text("Lichtverschmutzung", Modifier.weight(1f).padding(vertical = 14.dp), fontWeight = FontWeight.Bold)
                                IconButton(onClick = { expanded = false }) {
                                    Icon(Icons.Rounded.Close, "Vergrößerte Lichtkarte schließen")
                                }
                            }
                            LightPollutionMapHost(document, Modifier.fillMaxWidth().weight(1f))
                        }
                    }
                }
            }
        } else {
            Card(
                Modifier.fillMaxWidth().height(560.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
            ) {
                LightPollutionMapHost(document, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun LightPollutionEstimateSummary(estimate: LightPollutionEstimate) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Astra-Lichtindex ${estimate.index}/100", fontWeight = FontWeight.Bold)
            Text(estimate.qualityLabel.replaceFirstChar { it.uppercase() })
            Text(
                "Bortle ≈ ${estimate.bortleClass} · Himmelshelligkeit ≈ " +
                    String.format(Locale.GERMAN, "%.1f", estimate.skyBrightnessMag) + " mag/arcsec²",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Unvalidierte Orientierung aus der Helligkeit des Kartenbildes; keine gemessene Strahldichte oder Himmelshelligkeit. " +
                    "Datenjahr ${estimate.sourceYear} · ${estimate.sourceMetadata.ageYears()} Jahre alt · historisch. " +
                    "Mond, Wetter und Licht aus der weiteren Umgebung fehlen in dieser Näherung.",
                style = MaterialTheme.typography.bodySmall
            )
            if (estimate.sampleCoverageFraction < 1.0) Text(
                "Unvollständige Bilddaten im ausgewerteten Ausschnitt; die Einordnung ist zusätzlich eingeschränkt.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun rememberLightPollutionDocument(observer: GeoPoint, redLight: Boolean, refresh: Int): LightMapDocument {
    val context = LocalContext.current
    val view = remember(context) { PrivateWebViews.create(context, javascript = true) }
    val fontScale = LocalDensity.current.fontScale
    SideEffect { view.settings.textZoom = (fontScale * 100).toInt().coerceIn(50, 300) }
    var error by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(observer, redLight, view) {
        error = false
        loaded = false
        val html = withContext(Dispatchers.IO) { runCatching { lightPollutionMapHtml(context, observer, redLight) } }
        html.fold(onSuccess = { PrivateWebViews.loadHtml(view, it); loaded = true }, onFailure = { error = true })
    }
    LaunchedEffect(refresh) {
        if (refresh > 0) {
            if (error) {
                val html = withContext(Dispatchers.IO) { runCatching { lightPollutionMapHtml(context, observer, redLight) } }
                html.onSuccess { PrivateWebViews.loadHtml(view, it); error = false; loaded = true }
            } else view.evaluateJavascript("window.astraLightMap && window.astraLightMap.retry()", null)
        }
    }
    LifecycleStartEffect(view) {
        view.onResume()
        view.evaluateJavascript("window.astraLightMap && window.astraLightMap.retry()", null)
        onStopOrDispose { view.onPause() }
    }
    DisposableEffect(view) { onDispose { PrivateWebViews.dispose(view) } }
    return LightMapDocument(view, loaded, error)
}

private data class LightMapDocument(val view: WebView, val loaded: Boolean, val error: Boolean)

@Composable
private fun LightPollutionMapHost(document: LightMapDocument, modifier: Modifier) {
    Column(modifier) {
        if (document.error) Text("Die lokale Karte konnte nicht geöffnet werden. Bitte erneut laden.", Modifier.padding(16.dp))
        else if (!document.loaded) Text("Karte wird vorbereitet …", Modifier.padding(16.dp))
        AndroidView(factory = { FrameLayout(it) }, modifier = Modifier.fillMaxWidth().weight(1f),
            onRelease = { holder -> holder.removeAllViews() },
            update = { holder ->
                if (document.view.parent !== holder) {
                    (document.view.parent as? ViewGroup)?.removeView(document.view)
                    holder.addView(document.view, FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                    ))
                }
            }
        )
    }
}
