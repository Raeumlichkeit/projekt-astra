package de.projektastra.app

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.ParcelFileDescriptor
import android.os.Process
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.IOException
import java.io.Writer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

internal object PdfLimits {
    const val BYTES = 4_194_304
    const val PAGES = 64
    const val TEXT_CHARS = 131_072
    const val TIMEOUT_MS = 20_000L
}

/** Downloads stay in the app; untrusted PDF parsing has a separate UID, no app data or network. */
internal object IsolatedPdfParser {
    @Synchronized fun extract(context: Context, bytes: ByteArray): String {
        check(Looper.myLooper() != Looper.getMainLooper()) { "PDF-Auswertung benötigt Hintergrund-Thread" }
        require(bytes.size in 5..PdfLimits.BYTES && bytes.take(5).toByteArray().decodeToString() == "%PDF-")
        val pipe = ParcelFileDescriptor.createPipe()
        val complete = CountDownLatch(1)
        var result: String? = null
        val reply = Messenger(Handler(Looper.getMainLooper()) { message ->
            result = message.data.getString("text")?.takeIf { it.length <= PdfLimits.TEXT_CHARS }
            complete.countDown()
            true
        })
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                try {
                    Messenger(binder).send(Message.obtain(null, 1).apply {
                        replyTo = reply
                        data = Bundle().apply { putParcelable("pdf", pipe[0]) }
                    })
                    thread(name = "astra-pdf-input", isDaemon = true) {
                        runCatching { ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { it.write(bytes) } }
                    }
                } catch (_: Exception) { complete.countDown() }
            }
            override fun onServiceDisconnected(name: ComponentName) { complete.countDown() }
            override fun onNullBinding(name: ComponentName) { complete.countDown() }
            override fun onBindingDied(name: ComponentName) { complete.countDown() }
        }
        var bound = false
        try {
            bound = context.bindService(Intent(context, IsolatedPdfService::class.java), connection, Context.BIND_AUTO_CREATE)
            if (!bound || !complete.await(PdfLimits.TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                throw IOException("PDF-Auswertung abgebrochen")
            }
            return result ?: throw IOException("Kalender-PDF nicht lesbar")
        } finally {
            pipe.forEach { runCatching { it.close() } }
            if (bound) context.unbindService(connection)
        }
    }
}

class IsolatedPdfService : Service() {
    private val main = Handler(Looper.getMainLooper())
    private var started = false
    private val terminate = Runnable {
        // Fail closed if manifest isolation is accidentally removed.
        if (Process.myUid() != applicationInfo.uid) Process.killProcess(Process.myPid())
        else stopSelf()
    }
    private val messenger = Messenger(Handler(Looper.getMainLooper()) { message ->
        if (!started && message.what == 1 && Process.myUid() != applicationInfo.uid) {
            started = true
            @Suppress("DEPRECATION")
            val descriptor = message.data.getParcelable<ParcelFileDescriptor>("pdf")
            val reply = message.replyTo
            thread(name = "astra-isolated-pdf") {
                val text = runCatching {
                    checkNotNull(descriptor)
                    val bytes = ParcelFileDescriptor.AutoCloseInputStream(descriptor).use {
                        NetworkPolicy.readBounded(it, PdfLimits.BYTES)
                    }
                    check(bytes.size >= 5 && bytes.copyOfRange(0, 5).decodeToString() == "%PDF-")
                    PDFBoxResourceLoader.init(applicationContext)
                    PDDocument.load(bytes).use { document ->
                        check(!document.isEncrypted && document.numberOfPages in 1..PdfLimits.PAGES)
                        val writer = LimitedTextWriter()
                        PDFTextStripper().writeText(document, writer)
                        writer.toString()
                    }
                }.getOrNull()
                runCatching {
                    reply?.send(Message.obtain(null, 2).apply {
                        data = Bundle().apply { if (text != null) putString("text", text) }
                    })
                }
            }
        }
        true
    })
    override fun onBind(intent: Intent): IBinder {
        main.postDelayed(terminate, PdfLimits.TIMEOUT_MS)
        return messenger.binder
    }
    override fun onDestroy() {
        main.removeCallbacksAndMessages(null)
        super.onDestroy()
        terminate.run()
    }
}

internal class LimitedTextWriter : Writer() {
    private val text = StringBuilder()
    override fun write(chars: CharArray, offset: Int, length: Int) {
        if (length > PdfLimits.TEXT_CHARS - text.length) throw IOException("PDF-Text zu lang")
        text.append(chars, offset, length)
    }
    override fun flush() = Unit
    override fun close() = Unit
    override fun toString(): String = text.toString()
}
