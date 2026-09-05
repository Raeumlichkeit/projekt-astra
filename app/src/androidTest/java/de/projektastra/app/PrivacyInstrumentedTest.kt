package de.projektastra.app

import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ServiceInfo
import android.webkit.CookieManager
import android.webkit.WebSettings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class PrivacyInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test fun bundledHtmlLoadsWithoutAnyOnlinePermission() {
        SecureNetwork.configure(PrivacyOptions())
        var view: android.webkit.WebView? = null
        instrumentation.runOnMainSync {
            view = PrivateWebViews.create(context, javascript = true)
            PrivateWebViews.loadHtml(view!!, "<!doctype html><title>Astra offline document</title><p>Local document</p>")
        }
        try {
            var title = ""
            val end = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5)
            while (title != "Astra offline document" && System.nanoTime() < end) {
                instrumentation.runOnMainSync { title = view!!.title.orEmpty() }
                if (title != "Astra offline document") Thread.sleep(100)
            }
            assertEquals("Astra offline document", title)
            assertFalse(SecureNetwork.available)
        } finally { instrumentation.runOnMainSync { PrivateWebViews.dispose(view!!) } }
    }

    @Test fun webViewsDisablePersistenceAndSensitiveCapabilities() {
        instrumentation.runOnMainSync {
            val view = PrivateWebViews.create(context, javascript = true)
            try {
                assertFalse(view.settings.domStorageEnabled)
                assertFalse(view.settings.allowFileAccess)
                assertFalse(view.settings.allowContentAccess)
                assertFalse(CookieManager.getInstance().acceptCookie())
                assertFalse(CookieManager.getInstance().acceptThirdPartyCookies(view))
                assertEquals(WebSettings.LOAD_NO_CACHE, view.settings.cacheMode)
                assertEquals(WebSettings.MIXED_CONTENT_NEVER_ALLOW, view.settings.mixedContentMode)
            } finally { PrivateWebViews.dispose(view) }
        }
    }

    @Test fun pdfServiceIsPrivateAndIsolatedAndParsesValidInput() {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getServiceInfo(ComponentName(context, IsolatedPdfService::class.java), 0)
        assertFalse(info.exported)
        assertTrue(info.flags and ServiceInfo.FLAG_ISOLATED_PROCESS != 0)
        PDFBoxResourceLoader.init(context)
        val bytes = ByteArrayOutputStream().also { output ->
            PDDocument().use { document ->
                val page = PDPage()
                document.addPage(page)
                PDPageContentStream(document, page).use {
                    it.beginText()
                    it.setFont(PDType1Font.HELVETICA, 12f)
                    it.newLineAtOffset(20f, 700f)
                    it.showText("Astra isolated parser test")
                    it.endText()
                }
                document.save(output)
            }
        }.toByteArray()
        assertTrue(IsolatedPdfParser.extract(context, bytes).contains("Astra isolated parser test"))
    }

    @Test fun malformedPdfCannotCrashTheAppProcess() {
        assertThrows(IOException::class.java) { IsolatedPdfParser.extract(context, "%PDF-invalid".toByteArray()) }
    }

    @Test fun publicTileCacheCanBeClearedWithoutDeletingObservations() {
        val favorites = ObservationStore.favoriteObjectIds(context)
        val dir = java.io.File(context.noBackupFilesDir, "public_map_tiles").apply { mkdirs() }
        java.io.File(dir, "security-test").writeBytes(byteArrayOf(1))
        PublicTileCache.clear(context)
        assertTrue(dir.listFiles().orEmpty().isEmpty())
        assertEquals(favorites, ObservationStore.favoriteObjectIds(context))
    }

    @Test fun legacyMigrationRemovesBrowserDataButPreservesOtherData() {
        val fixture = java.io.File(context.cacheDir, "security-migration-fixture").apply { mkdirs() }
        val scoped = object : ContextWrapper(context) {
            override fun getDataDir() = fixture
            override fun getCacheDir() = java.io.File(fixture, "cache").apply { mkdirs() }
            override fun getSharedPreferences(name: String, mode: Int) =
                super.getSharedPreferences("security_migration_$name", mode)
        }
        scoped.getSharedPreferences("astra_privacy", Context.MODE_PRIVATE).edit().clear().commit()
        val browser = java.io.File(fixture, "app_webview").apply { mkdirs() }
        java.io.File(browser, "legacy-query").writeText("latitude=52.523456&longitude=13.406789")
        val observation = java.io.File(fixture, "observation").apply { writeText("favorite-test") }
        try {
            PrivacySettings.migrateLegacyBrowserData(scoped)
            assertFalse(browser.exists())
            assertEquals("favorite-test", observation.readText())
            assertFalse(PrivacySettings.load(scoped).online)
        } finally {
            check(fixture.canonicalPath.startsWith(context.cacheDir.canonicalPath + java.io.File.separator))
            fixture.deleteRecursively()
            scoped.getSharedPreferences("astra_privacy", Context.MODE_PRIVATE).edit().clear().commit()
        }
    }
}
