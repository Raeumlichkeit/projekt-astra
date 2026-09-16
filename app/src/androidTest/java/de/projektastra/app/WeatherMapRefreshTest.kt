package de.projektastra.app

import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Real bundled Leaflet/WebView; every external response is a deterministic local fixture. */
@RunWith(AndroidJUnit4::class)
class WeatherMapRefreshTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private var scenario: ActivityScenario<ComponentActivity>? = null
    private lateinit var view: WebView
    @Volatile private var stamp = 1_789_560_000L
    @Volatile private var delay = 0L
    @Volatile private var failRadar = false
    @Volatile private var failTiles = false
    @Volatile private var partialClouds = false

    @After fun close() {
        if (::view.isInitialized) instrumentation.runOnMainSync { PrivateWebViews.dispose(view) }
        scenario?.close()
        SecureNetwork.configure(PrivacyOptions())
        SecureNetwork.setForeground(false)
    }

    @Test fun slowRefreshRetainsBothLayersZoomAndToggleChoicesWithoutReload() {
        launch()
        ready()
        val old = stamp
        js("window.testSentinel=17; window.astraMap.setZoom(6); document.getElementById('rainToggle').click(); document.getElementById('cloudToggle').click()")
        stamp += 3600; delay = 900
        js("window.astraWeather.refresh()")
        assertEquals(old.toString(), js("window.astraWeather.state().radarTime"))
        assertEquals(old.toString(), js("window.astraWeather.state().cloudTime"))
        assertEquals("49", js("window.astraWeather.state().cloudCount"))
        assertEquals("true", js("window.astraWeather.state().cloudLoading"))
        ready()
        assertEquals("17", js("window.testSentinel"))
        assertEquals("6", js("window.astraMap.getZoom()"))
        assertEquals("false", js("window.astraWeather.state().radarVisible"))
        assertEquals("false", js("window.astraWeather.state().cloudVisible"))
    }

    @Test fun failedRadarMetadataDoesNotRemoveRadarOrPreventFreshClouds() {
        launch(); ready()
        val old = stamp
        stamp += 3600; failRadar = true
        js("window.astraWeather.refresh()")
        awaitJs("!window.astraWeather.state().radarLoading && window.astraWeather.state().cloudTime === $stamp")
        assertEquals(old.toString(), js("window.astraWeather.state().radarTime"))
        assertEquals("true", js("window.astraWeather.state().radarVisible"))
        assertEquals("true", js("document.getElementById('radarStatus').innerText.includes('fehlgeschlagen')"))
        assertEquals("false", js("document.getElementById('cloudStatus').classList.contains('error')"))
        capture("weather-refresh-error.png")
        failRadar = false
        js("window.astraWeather.refresh()")
        ready()
    }

    @Test fun failedReplacementTilesAndPartialCloudsKeepWholeOldLayers() {
        launch(); ready()
        val old = stamp
        stamp += 3600; failTiles = true; partialClouds = true
        js("window.astraWeather.refresh()")
        awaitJs("!window.astraWeather.state().radarLoading && !window.astraWeather.state().cloudLoading")
        assertEquals(old.toString(), js("window.astraWeather.state().radarTime"))
        assertEquals(old.toString(), js("window.astraWeather.state().cloudTime"))
        assertEquals("49", js("window.astraWeather.state().cloudCount"))
        assertEquals("true", js("window.astraWeather.state().radarVisible && window.astraWeather.state().cloudVisible"))
        assertEquals("true", js("document.getElementById('radarStatus').classList.contains('error') && document.getElementById('cloudStatus').classList.contains('error')"))
    }

    @Test fun failedLocationChangeMarksCloudLayerAsPreviousLocation() {
        launch(); ready()
        partialClouds = true
        js("window.astraWeather.refresh(48.14,11.58)")
        awaitJs("!window.astraWeather.state().cloudLoading")
        assertEquals("[52.52,13.41]", js("window.astraWeather.state().cloudPoint"))
        assertEquals("true", js("document.getElementById('cloudStatus').innerText.includes('bisheriger Standort')"))
        partialClouds = false
        js("window.astraWeather.refresh(48.14,11.58)")
        awaitJs("window.astraWeather.state().cloudPoint[0] === 48.14")
        assertEquals("false", js("document.getElementById('cloudStatus').innerText.includes('bisheriger Standort')"))
    }

    @Test fun backgroundDiscardsDelayedResponseAndResumeKeepsDataUntilReplacement() {
        launch(); ready()
        val old = stamp
        stamp += 3600; delay = 900
        js("window.astraWeather.refresh(); window.astraWeather.suspend()")
        SystemClock.sleep(1200)
        assertEquals("false", js("window.astraWeather.state().active"))
        assertEquals(old.toString(), js("window.astraWeather.state().radarTime"))
        assertEquals(old.toString(), js("window.astraWeather.state().cloudTime"))
        delay = 0
        js("window.astraWeather.refresh()")
        ready()
    }

    @Test fun noConsentBlocksNetworkAndLeavesIndependentRetryableStatuses() {
        SecureNetwork.configure(PrivacyOptions())
        SecureNetwork.setForeground(true)
        launch(fixtures = false)
        awaitJs("!window.astraWeather.state().radarLoading && !window.astraWeather.state().cloudLoading")
        assertFalse(SecureNetwork.available)
        assertEquals("null", js("window.astraWeather.state().radarTime"))
        assertEquals("0", js("window.astraWeather.state().cloudCount"))
        assertEquals("true", js("document.getElementById('radarStatus').innerText.includes('noch keine Daten') && document.getElementById('cloudStatus').innerText.includes('noch keine Daten')"))
        js("window.astraWeather.refresh()")
        awaitJs("!window.astraWeather.state().radarLoading")
    }

    @Test fun mapDocumentContainsOnlyCoarseLocationAndNoPersistentWeatherStorage() {
        val html = weatherMapHtml(context, GeoPoint(52.523456, 13.406789, 123.0))
        assertFalse(html.contains("52.523456"))
        assertFalse(html.contains("13.406789"))
        assertFalse(Regex("__[A-Z_]+__").containsMatchIn(html))
        val app = context.assets.open("weather_map.html").bufferedReader().use { it.readText() }
        assertFalse(app.contains("localStorage"))
        assertFalse(app.contains("navigator.geolocation"))
        assertTrue(app.contains("AbortController"))
    }

    private fun ready() = awaitJs("window.astraWeather.state().radarTime === $stamp && window.astraWeather.state().cloudTime === $stamp && !window.astraWeather.state().radarLoading && !window.astraWeather.state().cloudLoading")

    private fun launch(fixtures: Boolean = true) {
        val html = weatherMapHtml(context, GeoPoint(52.523456, 13.406789, 0.0))
        val tile = ByteArrayOutputStream().also { output ->
            val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.rgb(40, 65, 80))
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            bitmap.recycle()
        }.toByteArray()
        scenario = ActivityScenario.launch(ComponentActivity::class.java).also { scene ->
            scene.onActivity { activity ->
                view = PrivateWebViews.create(activity, javascript = true)
                if (fixtures) view.webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                        val host = request.url.host
                        val main = request.url.toString() == NetworkPolicy.LOCAL_ORIGIN + "/"
                        val moment = stamp
                        val failed = !main && !NetworkPolicy.permits(request.url.toString()) ||
                            (failRadar && host == "api.rainviewer.com") || (failTiles && host == "tilecache.rainviewer.com")
                        val data = when {
                            main -> html.toByteArray()
                            host == "api.rainviewer.com" -> """{"host":"https://tilecache.rainviewer.com","radar":{"past":[{"time":$moment,"path":"/v2/radar/$moment"}]}}""".toByteArray()
                            host == "api.open-meteo.com" -> List(if (partialClouds) 12 else 49) {
                                """{"current":{"time":$moment,"cloud_cover":50}}"""
                            }.joinToString(",", "[", "]").toByteArray()
                            else -> tile
                        }
                        if (!main && delay > 0) SystemClock.sleep(delay)
                        return WebResourceResponse(if (main) "text/html" else if (host?.startsWith("api.") == true) "application/json" else "image/png", "UTF-8",
                            if (failed) 503 else 200, if (failed) "Unavailable" else "OK",
                            mapOf("Access-Control-Allow-Origin" to NetworkPolicy.LOCAL_ORIGIN, "Cache-Control" to "no-store"),
                            ByteArrayInputStream(if (failed) ByteArray(0) else data))
                    }
                }
                val density = activity.resources.displayMetrics.density
                val container = FrameLayout(activity)
                container.addView(view, FrameLayout.LayoutParams((320 * density).toInt(), (440 * density).toInt()))
                activity.setContentView(container)
                PrivateWebViews.loadHtml(view, html)
            }
        }
        awaitJs("!!window.astraWeather")
        js("window.astraWeather.refresh()")
    }

    private fun js(script: String): String {
        val done = CountDownLatch(1)
        var value = ""
        instrumentation.runOnMainSync { view.evaluateJavascript(script) { value = it; done.countDown() } }
        assertTrue("JavaScript callback timed out", done.await(5, TimeUnit.SECONDS))
        return value
    }

    private fun awaitJs(condition: String) {
        val deadline = SystemClock.uptimeMillis() + 15_000
        do {
            if (js(condition) == "true") return
            SystemClock.sleep(100)
        } while (SystemClock.uptimeMillis() < deadline)
        fail("WebView condition not reached: $condition; body=" + js("document.body.innerText"))
    }

    private fun capture(name: String) {
        instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
            java.io.File(context.cacheDir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}
