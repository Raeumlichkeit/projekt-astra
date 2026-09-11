package de.projektastra.app

import android.net.Uri
import android.os.SystemClock
import android.webkit.CookieManager
import android.webkit.ConsoleMessage
import android.webkit.ServiceWorkerController
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class ExternalLightMapSecurityTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val observer = GeoPoint(52.523456, 13.406789, 99.0)

    @After fun resetNetwork() {
        SecureNetwork.configure(PrivacyOptions())
        SecureNetwork.setForeground(false)
    }

    @Test fun blankSessionHasNoNetworkAndDisablesPersistenceAndPermissions() {
        SecureNetwork.configure(PrivacyOptions(online = true))
        SecureNetwork.setForeground(true)
        instrumentation.runOnMainSync {
            val session = ExternalLightMapView(context, observer)
            try {
                val view = session.view
                assertEquals(ExternalLightMapState.IDLE, session.state)
                assertTrue(view.settings.blockNetworkLoads)
                assertFalse(view.settings.domStorageEnabled)
                assertFalse(view.settings.allowFileAccess)
                assertFalse(view.settings.allowContentAccess)
                assertFalse(view.settings.javaScriptCanOpenWindowsAutomatically)
                assertFalse(CookieManager.getInstance().acceptCookie())
                assertFalse(CookieManager.getInstance().acceptThirdPartyCookies(view))
                assertEquals(WebSettings.LOAD_NO_CACHE, view.settings.cacheMode)
                assertEquals(WebSettings.MIXED_CONTENT_NEVER_ALLOW, view.settings.mixedContentMode)
                assertTrue(ServiceWorkerController.getInstance().serviceWorkerWebSettings.blockNetworkLoads)
                assertTrue(view.webChromeClient!!.onConsoleMessage(
                    ConsoleMessage("https://example.test/?lat=52.52", "fixture", 1, ConsoleMessage.MessageLevel.ERROR)
                ))
                val blocked = view.webViewClient.shouldInterceptRequest(view, Request(ExternalLightMapPolicy.embedUrl(observer)))
                assertEquals(403, blocked!!.statusCode)
                assertTrue(view.webViewClient.shouldOverrideUrlLoading(view, Request("https://example.com/")))
            } finally { session.dispose() }
        }
    }

    @Test fun loadWithoutOnlineConsentStaysSuspendedAndBackgroundReloadCannotReconnect() {
        SecureNetwork.configure(PrivacyOptions())
        SecureNetwork.setForeground(true)
        instrumentation.runOnMainSync {
            val session = ExternalLightMapView(context, observer)
            try {
                session.load()
                assertEquals(ExternalLightMapState.SUSPENDED, session.state)
                assertTrue(session.view.settings.blockNetworkLoads)
                SecureNetwork.configure(PrivacyOptions(online = true))
                SecureNetwork.setForeground(false)
                session.reload()
                assertEquals(ExternalLightMapState.SUSPENDED, session.state)
                assertTrue(session.view.settings.blockNetworkLoads)
            } finally { session.dispose() }
        }
    }

    @Test fun remoteIframeResponseCspBlocksUnapprovedResourcesInChromium() {
        val trustedLoaded = CountDownLatch(1)
        val pageFinished = CountDownLatch(1)
        val unapproved = AtomicInteger()
        var view: WebView? = null
        val fixture = """
            <!doctype html><html><body><script>
            var good = new Image(); good.src = 'https://api.lightpollutionmap.app/api/lightpollution/image-tiles/2025/8/137/83.png';
            var bad = new Image(); bad.src = 'https://tracking.example.test/location';
            fetch('https://tracking.example.test/collect').catch(function() {});
            </script><script src="https://tracking.example.test/script.js"></script>
            <iframe src="https://tracking.example.test/frame"></iframe></body></html>
        """.trimIndent().toByteArray()
        instrumentation.runOnMainSync {
            view = PrivateWebViews.create(context, javascript = true).apply {
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                        val uri = request.url
                        return when {
                            uri.toString() == ExternalLightMapPolicy.DOCUMENT_URL -> response(ExternalLightMapPolicy.document(observer).toByteArray())
                            ExternalLightMapPolicy.isEmbedDocument(uri.toString()) -> ExternalLightMapPolicy.documentResponse(fixture)
                            uri.host == "api.lightpollutionmap.app" -> { trustedLoaded.countDown(); response(ByteArray(0)) }
                            else -> { unapproved.incrementAndGet(); response(ByteArray(0)) }
                        }
                    }
                    override fun onPageFinished(view: WebView, url: String?) {
                        if (url == ExternalLightMapPolicy.DOCUMENT_URL) pageFinished.countDown()
                    }
                }
                loadUrl(ExternalLightMapPolicy.DOCUMENT_URL)
            }
        }
        try {
            assertTrue("The iframe's allowed script/image never ran", trustedLoaded.await(10, TimeUnit.SECONDS))
            assertTrue("Fixture did not finish loading", pageFinished.await(10, TimeUnit.SECONDS))
            assertEquals("CSP must block before requests reach even a permissive interceptor", 0, unapproved.get())
        } finally { instrumentation.runOnMainSync { PrivateWebViews.dispose(view!!) } }
    }

    @Test fun attachedIframeGetsVisibleHeightAfterInitialZeroSizeAndAfterResize() {
        val remoteHeight = AtomicInteger(-1)
        val unexpectedRequests = AtomicInteger()
        val pageFinished = CountDownLatch(1)
        lateinit var view: WebView
        lateinit var host: FrameLayout
        var viewCreated = false
        var density = 1f
        val fixture = """
            <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
            <style>html,body{margin:0;width:100%;height:100%;background:#15aa77}</style></head><body>
            <script>
            function reportSize() {
                var probe = new Image();
                probe.src = 'https://api.lightpollutionmap.app/api/lightpollution/all?fixtureHeight=' + innerHeight;
            }
            addEventListener('resize', reportSize); addEventListener('load', reportSize); reportSize();
            </script></body></html>
        """.trimIndent().toByteArray()
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        try {
            scenario.onActivity { activity ->
                density = activity.resources.displayMetrics.density
                host = FrameLayout(activity)
                val root = FrameLayout(activity).apply {
                    // Reproduce the initial layout pass before Compose assigns its final height.
                    addView(host, FrameLayout.LayoutParams((320 * density).toInt(), 0))
                }
                view = PrivateWebViews.create(activity, javascript = true).apply {
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                            val uri = request.url
                            return when {
                                uri.toString() == ExternalLightMapPolicy.DOCUMENT_URL -> response(ExternalLightMapPolicy.document(observer).toByteArray())
                                ExternalLightMapPolicy.isEmbedDocument(uri.toString()) -> ExternalLightMapPolicy.documentResponse(fixture)
                                uri.host == "api.lightpollutionmap.app" -> {
                                    uri.getQueryParameter("fixtureHeight")?.toIntOrNull()?.let(remoteHeight::set)
                                    response(ByteArray(0))
                                }
                                else -> { unexpectedRequests.incrementAndGet(); response(ByteArray(0)) }
                            }
                        }
                        override fun onPageFinished(view: WebView, url: String?) {
                            if (url == ExternalLightMapPolicy.DOCUMENT_URL) pageFinished.countDown()
                        }
                    }
                    // Start loading before attachment, as the lifecycle effect can do in the app.
                    loadUrl(ExternalLightMapPolicy.DOCUMENT_URL)
                }
                viewCreated = true
                host.addView(view, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                activity.setContentView(root)
            }
            assertTrue("Fixture did not finish loading", pageFinished.await(10, TimeUnit.SECONDS))
            for (heightDp in listOf(540, 280, 480)) {
                scenario.onActivity {
                    host.layoutParams = FrameLayout.LayoutParams((320 * density).toInt(), (heightDp * density).toInt())
                }
                val deadline = SystemClock.uptimeMillis() + 10_000
                var iframeHeight = -1
                do {
                    val done = CountDownLatch(1)
                    instrumentation.runOnMainSync {
                        view.evaluateJavascript("Math.round(document.querySelector('iframe').getBoundingClientRect().height)") {
                            iframeHeight = it.toIntOrNull() ?: -1
                            done.countDown()
                        }
                    }
                    assertTrue("WebView measurement timed out", done.await(5, TimeUnit.SECONDS))
                    if (kotlin.math.abs(iframeHeight - heightDp) <= 2 && kotlin.math.abs(remoteHeight.get() - heightDp) <= 2) break
                    SystemClock.sleep(50)
                } while (SystemClock.uptimeMillis() < deadline)
                assertEquals("Outer iframe must fill its visible host after resize", heightDp.toDouble(), iframeHeight.toDouble(), 2.0)
                assertEquals("The remote document must have a nonzero matching viewport", heightDp.toDouble(), remoteHeight.get().toDouble(), 2.0)
                scenario.onActivity { assertTrue("Android WebView itself must be visible", view.isShown && view.height > 0) }
            }
            assertEquals("All fixture requests must remain local", 0, unexpectedRequests.get())
        } finally {
            if (viewCreated) instrumentation.runOnMainSync { PrivateWebViews.dispose(view) }
            scenario.close()
        }
    }

    private fun response(bytes: ByteArray) = WebResourceResponse("text/html", "UTF-8", ByteArrayInputStream(bytes))

    private class Request(address: String) : WebResourceRequest {
        private val uri = Uri.parse(address)
        override fun getUrl() = uri
        override fun isForMainFrame() = false
        override fun isRedirect() = false
        override fun hasGesture() = false
        override fun getMethod() = "GET"
        override fun getRequestHeaders() = emptyMap<String, String>()
    }
}
