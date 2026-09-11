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

/** Runs the bundled Leaflet document in a real WebView, using local raster fixtures, never a server. */
@RunWith(AndroidJUnit4::class)
class LightPollutionMapTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private var scenario: ActivityScenario<ComponentActivity>? = null
    private lateinit var view: WebView
    @Volatile private var failNightTiles = false
    private val observer = GeoPoint(52.523456, 13.406789, 99.0)

    @After fun close() {
        if (::view.isInitialized) instrumentation.runOnMainSync { PrivateWebViews.dispose(view) }
        scenario?.close()
        SecureNetwork.configure(PrivacyOptions())
        SecureNetwork.setForeground(false)
    }

    @Test fun documentContainsOnlyRoundedObserverAndBundledScripts() {
        val html = lightPollutionMapHtml(context, observer, redLight = false, currentYear = 2026)
        assertFalse(html.contains("52.523456"))
        assertFalse(html.contains("13.406789"))
        assertFalse(Regex("__[A-Z_]+__").containsMatchIn(html))
        assertTrue(html.contains("52.52"))
        assertTrue(html.contains("13.41"))
        // Leaflet bundles an unused locate() API. Check the app document never invokes it;
        // WebView geolocation is independently disabled by PrivateWebViews.
        val appDocument = context.assets.open("light_pollution_map.html").bufferedReader().use { it.readText() }
        assertFalse(appDocument.contains("navigator.geolocation"))
        assertFalse(appDocument.contains(".locate("))
        assertFalse(appDocument.contains("localStorage"))
    }

    @Test fun deniedNetworkStillShowsUsableLocalMapAndRetry() {
        SecureNetwork.configure(PrivacyOptions())
        SecureNetwork.setForeground(true)
        launch(fixtures = false)
        awaitJs("!!window.astraLightMap")
        awaitJs("document.body.innerText.includes('2016')")
        awaitJs("document.querySelectorAll('button').length >= 3")
        awaitJs("window.astraLightMap.state().failedTiles > 0")
        assertFalse(SecureNetwork.available)
        js("window.astraLightMap.retry()")
        assertEquals("true", js("!!window.astraMap && !!window.astraLightMap"))
    }

    @Test fun controlsAndLegendFitSmallViewportAndLargeText() {
        launch(widthDp = 320, heightDp = 480)
        awaitJs("!!window.astraLightMap")
        instrumentation.runOnMainSync { view.settings.textZoom = 150 }
        js("window.dispatchEvent(new Event('resize'))")
        awaitJs("document.documentElement.scrollWidth <= window.innerWidth + 1")
        assertEquals("true", js("document.body.innerText.includes('2016')"))
        assertEquals("true", js("!!document.querySelector('.leaflet-control-scale')"))
        js("window.astraLightMap.setRadius(50)")
        assertEquals("50", js("window.astraLightMap.state().radiusKm"))
        capture("light-map-small.png")
    }

    @Test fun proxyMatchesNativeScaleAndTransparentPixelsHaveNoData() {
        launch()
        awaitJs("!!window.astraLightMap")
        assertEquals("true", js("window.astraLightMap.proxyFromRgba(0,0,0,0) === null"))
        assertEquals("true", js("window.astraLightMap.proxyFromRgba(0,0,0,255).index === 0"))
        val expected = LightPollutionModel.estimateFromLuminance(80.0).index
        assertEquals(expected.toString(), js("window.astraLightMap.proxyFromRgba(80,80,80,255).index"))
        js("window.astraLightMap.setLayer('compare',true); window.astraLightMap.selectPlace(52.53,13.42)")
        awaitJs("window.astraLightMap.state().selectedSample?.status === 'ready'")
        assertEquals(expected.toString(), js("window.astraLightMap.state().selectedSample.index"))
        assertEquals("true", js("window.astraLightMap.tilePoint(0,180).x === window.astraLightMap.tilePoint(0,-180).x"))
        capture("light-map-compare.png")
    }

    @Test fun partialTileFailureRecoversOnRetryWithoutRemovingTheBaseMap() {
        failNightTiles = true
        launch()
        awaitJs("!!window.astraLightMap && window.astraLightMap.state().failedTiles > 0 && window.astraLightMap.state().loadedTiles > 0")
        js("window.astraLightMap.setLayer('compare',true)")
        awaitJs("window.astraLightMap.state().observerSample?.status === 'unavailable'")
        failNightTiles = false
        js("window.astraLightMap.retry()")
        assertEquals("true", js("window.astraLightMap.state().loadedTiles > 0"))
        awaitJs("!window.astraLightMap.state().retrying && window.astraLightMap.state().failedTiles === 0")
        awaitJs("window.astraLightMap.state().observerSample?.status === 'ready'")
    }

    @Test fun transparentSourceTileIsNotReportedAsADarkObservingPlace() {
        launch(tileColor = Color.TRANSPARENT)
        awaitJs("!!window.astraLightMap")
        js("window.astraLightMap.setLayer('compare',true); window.astraLightMap.selectPlace(52.53,13.42)")
        awaitJs("window.astraLightMap.state().selectedSample?.status === 'no-data'")
        assertEquals("true", js("document.getElementById('comparison-value').innerText.includes('Keine Daten')"))
    }

    @Test fun redLightModeIsAppliedInsideTheWebDocument() {
        launch(redLight = true)
        awaitJs("!!window.astraLightMap")
        assertEquals("true", js("document.body.classList.contains('red-light') || document.documentElement.classList.contains('red-light')"))
        js("window.astraMap.eachLayer(layer => { if (layer.getTooltip && layer.getTooltip()) layer.openTooltip(); })")
        awaitJs("!!document.querySelector('.leaflet-tooltip')")
        assertEquals("true", js("getComputedStyle(document.querySelector('.leaflet-tooltip')).backgroundColor === 'rgb(34, 3, 3)'"))
        capture("light-map-red.png")
    }

    private fun launch(fixtures: Boolean = true, redLight: Boolean = false, widthDp: Int = 360, heightDp: Int = 560,
                       tileColor: Int = Color.rgb(80, 80, 80)) {
        val html = lightPollutionMapHtml(context, observer, redLight, 2026)
        val tile = ByteArrayOutputStream().also { output ->
            val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(tileColor)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            bitmap.recycle()
        }.toByteArray()
        scenario = ActivityScenario.launch(ComponentActivity::class.java).also { scene ->
            scene.onActivity { activity ->
                view = PrivateWebViews.create(activity, javascript = true)
                if (fixtures) view.webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                        val main = request.url.toString() == NetworkPolicy.LOCAL_ORIGIN + "/"
                        val allowed = main || NetworkPolicy.permits(request.url.toString())
                        val failed = !allowed || (failNightTiles && request.url.host == "gibs.earthdata.nasa.gov")
                        return WebResourceResponse(if (main) "text/html" else "image/png", "UTF-8",
                            if (failed) 503 else 200, if (failed) "Unavailable" else "OK",
                            mapOf("Access-Control-Allow-Origin" to NetworkPolicy.LOCAL_ORIGIN, "Cache-Control" to "no-store"),
                            ByteArrayInputStream(if (failed) ByteArray(0) else if (main) html.toByteArray() else tile))
                    }
                }
                val density = activity.resources.displayMetrics.density
                val container = FrameLayout(activity)
                container.addView(view, FrameLayout.LayoutParams((widthDp * density).toInt(), (heightDp * density).toInt()))
                activity.setContentView(container)
                PrivateWebViews.loadHtml(view, html)
            }
        }
    }

    private fun js(script: String): String {
        val done = CountDownLatch(1)
        var value = ""
        instrumentation.runOnMainSync { view.evaluateJavascript(script) { value = it; done.countDown() } }
        assertTrue("JavaScript callback timed out", done.await(5, TimeUnit.SECONDS))
        return value
    }

    private fun awaitJs(condition: String) {
        val deadline = SystemClock.uptimeMillis() + 10_000
        do {
            if (js(condition) == "true") return
            SystemClock.sleep(100)
        } while (SystemClock.uptimeMillis() < deadline)
        fail("WebView condition not reached: $condition; body=" + js("document.body.innerText"))
    }

    private fun capture(name: String) {
        instrumentation.waitForIdleSync()
        instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
            java.io.File(context.cacheDir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}
