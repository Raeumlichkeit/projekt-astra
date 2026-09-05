package de.projektastra.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.edit
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.round

internal data class PrivacyOptions(val online: Boolean = false, val terrain: Boolean = false)

internal object PrivacySettings {
    private const val PREFS = "astra_privacy"
    fun load(context: Context): PrivacyOptions {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return PrivacyOptions(prefs.getBoolean("online", false), prefs.getBoolean("terrain", false))
    }
    fun hasDecision(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean("decision_v1", false)

    fun save(context: Context, options: PrivacyOptions) {
        SecureNetwork.configure(options)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putBoolean("online", options.online)
            putBoolean("terrain", options.online && options.terrain)
            putBoolean("decision_v1", true)
        }
    }

    // Runs before the first WebView is created. Removes only old browser data, never observations.
    fun migrateLegacyBrowserData(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean("legacy_browser_removed", false)) return
        val targets = listOf(File(context.dataDir, "app_webview"), File(context.cacheDir, "WebView"))
        var complete = true
        targets.forEach { target ->
            check(target.canonicalPath.startsWith(context.dataDir.canonicalPath + File.separator))
            if (target.exists() && !target.deleteRecursively()) complete = false
        }
        if (complete) prefs.edit { putBoolean("legacy_browser_removed", true) }
        else throw IOException("Alte Kartendaten konnten nicht entfernt werden")
    }
}

internal object NetworkPolicy {
    const val LOCAL_ORIGIN = "https://projekt-astra.local"
    private val paths = mapOf(
        "api.open-meteo.com" to Regex("/v1/(forecast|elevation)"),
        "tile.openstreetmap.org" to Regex("/\\d{1,2}/\\d+/\\d+\\.png"),
        "api.rainviewer.com" to Regex("/public/weather-maps\\.json"),
        "tilecache.rainviewer.com" to Regex("/v2/radar/[A-Za-z0-9_-]{1,64}/256/\\d{1,2}/\\d+/\\d+/2/1_1\\.png"),
        "gibs.earthdata.nasa.gov" to Regex("/wmts/epsg3857/best/VIIRS_Night_Lights/default/2016-01-01/GoogleMapsCompatible_Level8/[0-8]/\\d+/\\d+\\.png"),
        "alasky.u-strasbg.fr" to Regex("/hips-image-services/hips2fits"),
        "www.imo.net" to Regex("/files/meteor-shower/cal20\\d{2}\\.pdf")
    )
    fun permits(address: String): Boolean = runCatching {
        val uri = URI(address)
        uri.scheme == "https" && uri.userInfo == null && uri.fragment == null &&
            (uri.port == -1 || uri.port == 443) &&
            paths[uri.host?.lowercase(Locale.US)]?.matches(uri.rawPath.orEmpty()) == true
    }.getOrDefault(false)

    fun roundedLocation(point: GeoPoint) = GeoPoint(
        round(point.latitude.coerceIn(-90.0, 90.0) * 100.0) / 100.0,
        round(point.longitude.coerceIn(-180.0, 180.0) * 100.0) / 100.0,
        0.0
    )

    fun readBounded(input: InputStream, maxBytes: Int): ByteArray {
        require(maxBytes in 1..8_388_608)
        val output = ByteArrayOutputStream(minOf(maxBytes, 8192))
        val buffer = ByteArray(8192)
        while (true) {
            val size = input.read(buffer)
            if (size == -1) break
            if (output.size() + size > maxBytes) throw IOException("Antwort überschreitet Größenlimit")
            output.write(buffer, 0, size)
        }
        return output.toByteArray()
    }
}

internal data class NetworkResponse(val bytes: ByteArray, val mime: String, val maxAgeSeconds: Long)

internal object SecureNetwork {
    @Volatile var options = PrivacyOptions()
        private set
    @Volatile private var foreground = false
    private val generation = AtomicLong()
    private val connections = ConcurrentHashMap.newKeySet<HttpURLConnection>()
    val available: Boolean get() = options.online && foreground
    fun configure(value: PrivacyOptions) {
        options = value.copy(terrain = value.online && value.terrain)
        cancelRequests()
    }
    fun setForeground(value: Boolean) {
        foreground = value
        if (!value) cancelRequests()
    }
    private fun cancelRequests() {
        generation.incrementAndGet()
        connections.forEach { it.disconnect() }
    }
    fun get(address: String, maxBytes: Int = 2_097_152): NetworkResponse {
        if (!available || !NetworkPolicy.permits(address)) throw IOException("Online-Zugriff gesperrt")
        if (URI(address).path == "/v1/elevation" && !options.terrain) throw IOException("Gelände nicht freigegeben")
        val token = generation.get()
        val connection = (URL(address).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            instanceFollowRedirects = false
            useCaches = false
            requestMethod = "GET"
            setRequestProperty("User-Agent", "ProjektAstra/${BuildConfig.VERSION_NAME}")
            setRequestProperty("Referer", NetworkPolicy.LOCAL_ORIGIN + "/")
        }
        connections += connection
        try {
            if (!available || token != generation.get()) throw IOException("Anfrage abgebrochen")
            if (connection.responseCode !in 200..299) throw IOException("Datenquelle nicht verfügbar")
            if (connection.contentLengthLong > maxBytes) throw IOException("Antwort zu groß")
            val bytes = connection.inputStream.use { NetworkPolicy.readBounded(it, maxBytes) }
            if (!available || token != generation.get()) throw IOException("Anfrage abgebrochen")
            val cacheControl = connection.getHeaderField("Cache-Control").orEmpty().lowercase(Locale.US)
            val maxAge = if (cacheControl.contains("no-store") || cacheControl.contains("no-cache")) 0L
            else Regex("(?:^|[, ]+)max-age=(\\d+)").find(cacheControl)
                ?.groupValues?.get(1)?.toLongOrNull()?.minus(connection.getHeaderFieldLong("Age", 0L))?.coerceAtLeast(0)
                ?: 604_800L
            return NetworkResponse(bytes, connection.contentType?.substringBefore(';') ?: "application/octet-stream", maxAge)
        } finally {
            connections -= connection
            connection.disconnect()
        }
    }
}

// Only public OSM image tiles are persisted, with no raw URLs or GPS parameters.
// The cache is app-private, excluded from backup, bounded, and user-clearable.
internal object PublicTileCache {
    private const val MAX_BYTES = 32L * 1024 * 1024
    private val revision = AtomicLong()
    @Synchronized fun prune(context: Context) {
        File(context.noBackupFilesDir, "public_map_tiles").listFiles().orEmpty()
            .filter { it.lastModified() <= System.currentTimeMillis() }.forEach { it.delete() }
    }
    fun get(context: Context, address: String): NetworkResponse {
        check(SecureNetwork.available && NetworkPolicy.permits(address) && URI(address).host == "tile.openstreetmap.org")
        val token = revision.get()
        val dir = File(context.noBackupFilesDir, "public_map_tiles").apply { mkdirs() }
        val key = MessageDigest.getInstance("SHA-256").digest(address.toByteArray()).joinToString("") { "%02x".format(it) }
        val file = File(dir, key)
        synchronized(this) {
            if (file.exists() && file.lastModified() > System.currentTimeMillis() && file.length() <= 1_048_576) {
                return NetworkResponse(file.readBytes(), "image/png", 0)
            }
        }
        // Never hold the cache monitor during network I/O: clearing from the UI must not wait for a server.
        val response = SecureNetwork.get(address, 1_048_576)
        synchronized(this) {
            if (token != revision.get() || !SecureNetwork.available) return response
            val files = dir.listFiles().orEmpty().sortedBy { it.lastModified() }
            var used = files.sumOf { it.length() }
            files.forEach { old ->
                if (old.lastModified() <= System.currentTimeMillis() || used + response.bytes.size > MAX_BYTES) {
                    val size = old.length()
                    if (old.delete()) used -= size
                }
            }
            if (response.maxAgeSeconds > 0) {
                file.writeBytes(response.bytes)
                file.setLastModified(System.currentTimeMillis() + response.maxAgeSeconds.coerceAtMost(2_592_000) * 1000)
            } else file.delete()
        }
        return response
    }
    @Synchronized fun clear(context: Context) {
        revision.incrementAndGet()
        val dir = File(context.noBackupFilesDir, "public_map_tiles")
        dir.listFiles().orEmpty().forEach { check(it.delete()) { "Kartencache konnte nicht gelöscht werden" } }
    }
}

internal object PrivateWebViews {
    private val documents = java.util.Collections.synchronizedMap(java.util.WeakHashMap<WebView, String>())
    fun loadHtml(view: WebView, html: String) {
        documents[view] = html
        view.loadUrl(NetworkPolicy.LOCAL_ORIGIN + "/")
    }
    @SuppressLint("SetJavaScriptEnabled")
    fun create(context: Context, javascript: Boolean): WebView = WebView(context).apply {
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        CookieManager.getInstance().setAcceptCookie(false)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
        settings.javaScriptEnabled = javascript
        settings.domStorageEnabled = false
        settings.cacheMode = WebSettings.LOAD_NO_CACHE
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.setGeolocationEnabled(false)
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.userAgentString = "ProjektAstra/${BuildConfig.VERSION_NAME}"
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (request.isForMainFrame && request.url.toString() == NetworkPolicy.LOCAL_ORIGIN + "/" &&
                    documents.containsKey(view)) return false
                if (request.isForMainFrame && request.hasGesture() &&
                    (request.url.scheme == "https" || request.url.toString() == "mailto:jeregrez@gmail.com")) {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, request.url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
                return true
            }
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                if (request.isForMainFrame && request.method == "GET" && request.url.toString() == NetworkPolicy.LOCAL_ORIGIN + "/") {
                    documents[view]?.let { html ->
                        return WebResourceResponse("text/html", "UTF-8", 200, "OK",
                            mapOf("Cache-Control" to "no-store", "X-Content-Type-Options" to "nosniff"),
                            ByteArrayInputStream(html.toByteArray(Charsets.UTF_8)))
                    }
                }
                val response = runCatching {
                    if (!SecureNetwork.available || request.method != "GET") throw IOException("Offline")
                    val url = request.url.toString()
                    if (!NetworkPolicy.permits(url)) throw IOException("Quelle gesperrt")
                    if (request.url.host == "tile.openstreetmap.org") PublicTileCache.get(context, url)
                    else SecureNetwork.get(url)
                }.getOrNull()
                return WebResourceResponse(
                    response?.mime ?: "text/plain", "UTF-8", if (response == null) 403 else 200,
                    if (response == null) "Blocked" else "OK",
                    mapOf("Cache-Control" to "no-store", "Access-Control-Allow-Origin" to NetworkPolicy.LOCAL_ORIGIN,
                        "X-Content-Type-Options" to "nosniff"),
                    ByteArrayInputStream(response?.bytes ?: ByteArray(0))
                )
            }
            override fun onPageFinished(view: WebView, url: String?) {
                if (javascript) view.evaluateJavascript("window.astraMap && window.astraMap.invalidateSize(true)", null)
            }
        }
    }
    fun dispose(view: WebView) {
        documents.remove(view)
        view.stopLoading()
        view.loadUrl("about:blank")
        view.clearHistory()
        view.clearCache(true)
        view.destroy()
    }
    fun clearBrowserStorage() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
    }
}
