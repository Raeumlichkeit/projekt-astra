package de.projektastra.app

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleStartEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

internal fun weatherMapHtml(context: Context, observer: GeoPoint): String {
    val approximate = NetworkPolicy.roundedLocation(observer)
    fun asset(name: String) = context.assets.open(name).bufferedReader().use { it.readText() }
    return asset("weather_map.html")
        .replace("__LEAFLET_CSS__", asset("leaflet-1.9.4.css"))
        .replace("__LEAFLET_JS__", asset("leaflet-1.9.4.js"))
        .replace("__ASTRA_LAT__", approximate.latitude.toString())
        .replace("__ASTRA_LON__", approximate.longitude.toString())
        .replace("__SCRIPT_NONCE__", UUID.randomUUID().toString())
}

@Composable
internal fun WeatherMap(observer: GeoPoint, refreshKey: Int) {
    val context = LocalContext.current
    var ready by remember { mutableStateOf(false) }
    var active by remember { mutableStateOf(false) }
    val view = remember(context) { PrivateWebViews.create(context, javascript = true, onDocumentReady = { ready = true }) }
    LaunchedEffect(view) {
        val html = withContext(Dispatchers.IO) { weatherMapHtml(context, observer) }
        PrivateWebViews.loadHtml(view, html)
    }
    LifecycleStartEffect(view) {
        active = true
        view.onResume()
        onStopOrDispose {
            active = false
            view.evaluateJavascript("window.astraWeather && window.astraWeather.suspend()", null)
            view.onPause()
        }
    }
    LaunchedEffect(view, ready, active, observer, refreshKey) {
        if (ready && active && SecureNetwork.available) {
            val point = NetworkPolicy.roundedLocation(observer)
            view.evaluateJavascript("window.astraWeather && window.astraWeather.refresh(${point.latitude},${point.longitude})", null)
        }
    }
    DisposableEffect(view) { onDispose { PrivateWebViews.dispose(view) } }
    Card(Modifier.fillMaxWidth().height(440.dp), shape = RoundedCornerShape(18.dp)) {
        AndroidView(factory = {
            FrameLayout(it).apply {
                addView(view, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            }
        }, modifier = Modifier.fillMaxSize())
    }
}
