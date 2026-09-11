package de.projektastra.app

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.webkit.CookieManager
import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions
import android.webkit.JsResult
import android.webkit.JsPromptResult
import android.webkit.PermissionRequest
import android.webkit.ServiceWorkerClient
import android.webkit.ServiceWorkerController
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/** Separate, session-only policy for the provider's official embed. Never used by SecureNetwork. */
internal object ExternalLightMapPolicy {
    const val DOCUMENT_URL = NetworkPolicy.LOCAL_ORIGIN + "/external-light-map"
    // WebView does not intercept every subresource redirect. A response CSP on the remote iframe
    // document enforces the approved destinations in Chromium, including redirected requests.
    val EMBED_CSP = listOf(
        "default-src 'none'",
        "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js",
        "style-src 'self' 'unsafe-inline'",
        "img-src 'self' data: https://api.lightpollutionmap.app https://tile.openstreetmap.org https://a.basemaps.cartocdn.com https://b.basemaps.cartocdn.com https://c.basemaps.cartocdn.com https://d.basemaps.cartocdn.com https://server.arcgisonline.com",
        "connect-src https://api.lightpollutionmap.app https://lightpollutionmap.app/sky/",
        "font-src 'self'",
        "frame-src 'none'", "worker-src 'none'", "object-src 'none'", "media-src 'none'",
        "form-action 'none'", "base-uri 'self'", "frame-ancestors ${NetworkPolicy.LOCAL_ORIGIN}"
    ).joinToString("; ")
    private val astroAsset = Regex("/_astro/[A-Za-z0-9_-][A-Za-z0-9_.-]*\\.(?:js|css)")
    private val skyAsset = Regex("/sky/(?:[A-Za-z0-9_-]+/)*[A-Za-z0-9_.-]+\\.(?:js|json)")
    private val previews = Regex("/layer-previews/(?:classic|hyperlight|glow|scientific)\\.png")
    private val osmTile = Regex("/\\d{1,2}/\\d+/\\d+\\.png")
    private val cartoTile = Regex("/dark_all/\\d{1,2}/\\d+/\\d+(?:@2x)?\\.png")
    private val imageryTile = Regex("/ArcGIS/rest/services/World_Imagery/MapServer/tile/\\d{1,2}/\\d+/\\d+")
    private val lightTile = Regex("/api/lightpollution/(?:image-tiles/[A-Za-z0-9_-]{1,40}|style-tiles/[A-Za-z0-9_-]{1,40}/[A-Za-z0-9_-]{1,40})/\\d{1,2}/\\d+/\\d+\\.png")
    private val details = setOf("/api/lightpollution", "/api/lightpollution/all", "/api/lightpollution/monthly", "/api/lightpollution/sky-profile")

    fun embedUrl(observer: GeoPoint): String {
        require(observer.latitude.isFinite() && observer.longitude.isFinite())
        val point = NetworkPolicy.roundedLocation(observer)
        return String.format(Locale.US, "https://lightpollutionmap.app/de/embed/?lat=%.2f&lng=%.2f&zoom=8", point.latitude, point.longitude)
    }

    fun permits(address: String, method: String = "GET"): Boolean = runCatching {
        val uri = URI(address)
        if (uri.scheme != "https" || uri.userInfo != null || uri.fragment != null ||
            (uri.port != -1 && uri.port != 443) || uri.rawPath != uri.normalize().rawPath) return false
        val host = uri.host?.lowercase(Locale.US) ?: return false
        val path = uri.rawPath.orEmpty()
        val api = host == "api.lightpollutionmap.app" && (path in details || lightTile.matches(path))
        // Only the browser's CORS preflight may use another HTTP method.
        if (method == "OPTIONS") return api
        if (method != "GET") return false
        api || when (host) {
            "lightpollutionmap.app" -> path == "/de/embed/" || astroAsset.matches(path) || skyAsset.matches(path) ||
                previews.matches(path) || path == "/stargazinghub.webp"
            "cdn.jsdelivr.net" -> path == "/npm/chart.js@4.4.0/dist/chart.umd.min.js"
            "tile.openstreetmap.org" -> osmTile.matches(path)
            "a.basemaps.cartocdn.com", "b.basemaps.cartocdn.com", "c.basemaps.cartocdn.com", "d.basemaps.cartocdn.com" -> cartoTile.matches(path)
            "server.arcgisonline.com" -> imageryTile.matches(path)
            else -> false
        }
    }.getOrDefault(false)

    fun isEmbedDocument(address: String): Boolean = runCatching {
        val uri = URI(address)
        permits(address) && uri.host == "lightpollutionmap.app" && uri.rawPath == "/de/embed/"
    }.getOrDefault(false)

    fun document(observer: GeoPoint): String = """
        <!doctype html><html lang="de"><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <meta http-equiv="Content-Security-Policy" content="default-src 'none'; frame-src https://lightpollutionmap.app; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'">
        <meta name="referrer" content="no-referrer"><title>Externe Lichtverschmutzungskarte</title>
        <style>html,body{margin:0;width:100%;min-height:100vh;background:#071020}iframe{display:block;position:fixed;inset:0;width:100%;height:100vh;border:0;background:#071020}</style>
        </head><body><iframe title="Light Pollution Map" src="${embedUrl(observer).replace("&", "&amp;")}"
        referrerpolicy="no-referrer" sandbox="allow-scripts allow-same-origin"
        allow="geolocation 'none'; camera 'none'; microphone 'none'; payment 'none'; clipboard-read 'none'; clipboard-write 'none'"></iframe></body></html>
    """.trimIndent()

    fun documentResponse(bytes: ByteArray) = WebResourceResponse("text/html", "UTF-8", 200, "OK", mapOf(
        "Cache-Control" to "no-store", "X-Content-Type-Options" to "nosniff",
        "Referrer-Policy" to "no-referrer", "Content-Security-Policy" to EMBED_CSP,
        "Permissions-Policy" to "geolocation=(), camera=(), microphone=(), payment=(), usb=(), serial=(), bluetooth=()"
    ), ByteArrayInputStream(bytes))
}

internal enum class ExternalLightMapState { IDLE, LOADING, READY, ERROR, SUSPENDED }

/**
 * Construct only after the user opts into the external provider. No remote request is issued by
 * construction; load() additionally requires foreground online access. The iframe retains its own
 * origin and browser CORS behavior, and its data is never consumed by Astra's score calculation.
 */
internal class ExternalLightMapView(
    context: Context,
    observer: GeoPoint,
    private val onState: (ExternalLightMapState) -> Unit = {}
) {
    private val html = ExternalLightMapPolicy.document(observer)
    private val userAgent = WebSettings.getDefaultUserAgent(context)
    private val requests = ConcurrentHashMap.newKeySet<HttpURLConnection>()
    private val generation = AtomicLong()
    @Volatile private var active = false
    private var disposed = false
    private var documentFailed = false
    var state: ExternalLightMapState = ExternalLightMapState.IDLE
        private set

    @SuppressLint("SetJavaScriptEnabled")
    val view = WebView(context).apply {
        setBackgroundColor(Color.rgb(7, 16, 32))
        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        isSaveEnabled = false
        CookieManager.getInstance().setAcceptCookie(false)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = false
            cacheMode = WebSettings.LOAD_NO_CACHE
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            allowFileAccess = false
            allowContentAccess = false
            setGeolocationEnabled(false)
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            mediaPlaybackRequiresUserGesture = true
            blockNetworkLoads = true
        }
        // Service-worker requests do not use this view's request interceptor. Astra has no workers.
        ServiceWorkerController.getInstance().apply {
            serviceWorkerWebSettings.apply {
                blockNetworkLoads = true
                allowFileAccess = false
                allowContentAccess = false
                cacheMode = WebSettings.LOAD_NO_CACHE
            }
            setServiceWorkerClient(object : ServiceWorkerClient() {
                override fun shouldInterceptRequest(request: WebResourceRequest) = blocked()
            })
        }
        setDownloadListener { _, _, _, _, _ -> }
        webChromeClient = object : WebChromeClient() {
            // Provider errors may contain selected coordinates in URLs. Do not forward its
            // JavaScript console to Android's system log.
            override fun onConsoleMessage(consoleMessage: ConsoleMessage) = true
            override fun onPermissionRequest(request: PermissionRequest) = request.deny()
            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) =
                callback.invoke(origin, false, false)
            override fun onShowFileChooser(webView: WebView, callback: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
                callback.onReceiveValue(null)
                return true
            }
            override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
                result.cancel()
                return true
            }
            override fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean {
                result.cancel()
                return true
            }
            override fun onJsPrompt(view: WebView, url: String, message: String, defaultValue: String, result: JsPromptResult): Boolean {
                result.cancel()
                return true
            }
        }
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                if (url == "about:blank") return false
                if (!active || !SecureNetwork.available || request.method != "GET") return true
                return if (request.isForMainFrame) url != ExternalLightMapPolicy.DOCUMENT_URL
                else !ExternalLightMapPolicy.isEmbedDocument(url)
            }

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                if (!active || !SecureNetwork.available) return blocked()
                val url = request.url.toString()
                if (request.isForMainFrame) {
                    if (request.method != "GET" || url != ExternalLightMapPolicy.DOCUMENT_URL) return blocked()
                    return WebResourceResponse("text/html", "UTF-8", 200, "OK",
                        mapOf("Cache-Control" to "no-store", "X-Content-Type-Options" to "nosniff", "Referrer-Policy" to "no-referrer"),
                        ByteArrayInputStream(html.toByteArray(Charsets.UTF_8)))
                }
                if (request.method == "GET" && ExternalLightMapPolicy.isEmbedDocument(url)) {
                    return fetchEmbedDocument(url)
                }
                // Preserve the provider's origin and CORS, including its own embed's preflight.
                return if (ExternalLightMapPolicy.permits(url, request.method)) null else blocked()
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame || ExternalLightMapPolicy.isEmbedDocument(request.url.toString())) documentError()
            }

            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
                if (request.isForMainFrame || ExternalLightMapPolicy.isEmbedDocument(request.url.toString())) documentError()
            }

            override fun onPageFinished(view: WebView, url: String?) {
                if (active && !documentFailed && url == ExternalLightMapPolicy.DOCUMENT_URL) updateState(ExternalLightMapState.READY)
            }
        }
    }

    fun load() {
        check(!disposed)
        if (!SecureNetwork.available) {
            suspend()
            return
        }
        active = true
        documentFailed = false
        updateState(ExternalLightMapState.LOADING)
        view.onResume()
        view.settings.blockNetworkLoads = false
        view.loadUrl(ExternalLightMapPolicy.DOCUMENT_URL)
    }

    fun reload() {
        if (disposed) return
        suspend()
        load()
    }

    fun suspend() {
        if (disposed) return
        active = false
        generation.incrementAndGet()
        requests.forEach { it.disconnect() }
        view.settings.blockNetworkLoads = true
        view.stopLoading()
        view.loadUrl("about:blank")
        view.clearHistory()
        view.clearCache(true)
        view.onPause()
        updateState(ExternalLightMapState.SUSPENDED)
    }

    fun dispose() {
        if (disposed) return
        suspend()
        disposed = true
        view.destroy()
    }

    private fun documentError() {
        if (active) {
            documentFailed = true
            updateState(ExternalLightMapState.ERROR)
        }
    }

    private fun updateState(value: ExternalLightMapState) {
        if (state != value) {
            state = value
            onState(value)
        }
    }

    private fun fetchEmbedDocument(address: String): WebResourceResponse {
        val token = generation.get()
        fun permitted() = active && SecureNetwork.available && token == generation.get()
        if (!permitted()) return blocked()
        val connection = (URL(address).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            instanceFollowRedirects = false
            useCaches = false
            requestMethod = "GET"
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept", "text/html")
            setRequestProperty("Accept-Language", "de,en;q=0.5")
        }
        requests += connection
        return try {
            if (!permitted()) throw IOException("External map session ended")
            if (connection.responseCode !in 200..299 ||
                connection.contentType?.substringBefore(';')?.trim() != "text/html" ||
                connection.contentLengthLong > 1_048_576) throw IOException("External map document unavailable")
            val bytes = connection.inputStream.use { NetworkPolicy.readBounded(it, 1_048_576) }
            if (!permitted()) throw IOException("External map session ended")
            ExternalLightMapPolicy.documentResponse(bytes)
        } catch (_: IOException) {
            view.post { if (permitted()) documentError() }
            blocked()
        } finally {
            requests -= connection
            connection.disconnect()
        }
    }

    private fun blocked() = WebResourceResponse("text/plain", "UTF-8", 403, "Blocked",
        mapOf("Cache-Control" to "no-store", "X-Content-Type-Options" to "nosniff"), ByteArrayInputStream(ByteArray(0)))
}
