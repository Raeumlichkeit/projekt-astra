package de.projektastra.app

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleStartEffect
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal data class SkyTextureState(
    val frame: SkyCoordinateFrame,
    val azimuth: Double,
    val altitude: Double,
    val fov: Double,
    val arMode: Boolean,
    val appearance: SkyAppearance
)

@Composable
internal fun SkyTextureLayer(state: SkyTextureState, modifier: Modifier = Modifier, onStatus: (Boolean) -> Unit = {}) {
    val context = LocalContext.current
    val view = remember(context) { SkyTextureView(context) }
    LifecycleStartEffect(view) {
        view.setRenderingActive(true)
        onStopOrDispose { view.setRenderingActive(false) }
    }
    AndroidView(factory = { view }, modifier = modifier,
        update = { it.onStatus = onStatus; it.update(state) }, onRelease = { it.releaseRenderer() })
}

/** GPU inverse projection, including the zenith and all edges. No network or pixel work on the UI thread. */
internal class SkyTextureView(context: Context) : TextureView(context), TextureView.SurfaceTextureListener {
    private var worker: SkyTextureWorker? = null
    private var state: SkyTextureState? = null
    private var renderingActive = false
    private var released = false
    private var generation = 0
    var onStatus: (Boolean) -> Unit = {}
    val framesRendered = AtomicInteger()
    @Volatile var textureBytes: Int = 0
        private set

    init {
        isOpaque = false // Remains part of the normal view tree: Compose clipping and red-light multiply apply.
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        surfaceTextureListener = this
    }

    fun update(value: SkyTextureState) {
        if (released) return
        state = value.copy(appearance = value.appearance.normalized())
        worker?.update(state, width, height, renderingActive)
    }

    fun setRenderingActive(active: Boolean) {
        renderingActive = active
        worker?.update(state, width, height, active)
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        val currentGeneration = ++generation
        worker = SkyTextureWorker(context.applicationContext, surface, { bytes ->
            post { if (!released && generation == currentGeneration && worker != null) {
                textureBytes = bytes
                onStatus(true)
            } }
        }, { post { if (!released && generation == currentGeneration && worker != null) onStatus(false) } }, framesRendered).also {
            it.update(state, width, height, renderingActive)
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        worker?.update(state, width, height, renderingActive)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        val previous = worker
        worker = null
        generation++
        textureBytes = 0
        // Keep this SurfaceTexture alive until its EGL work has stopped; then release it on that thread.
        previous?.close(releaseTexture = true)
        return previous == null
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    fun releaseRenderer() {
        released = true
        setRenderingActive(false)
        // AndroidView removal detaches this TextureView. Its destruction callback owns shutdown;
        // do not drop that worker early or the framework could release an in-flight EGL surface.
        textureBytes = 0
    }
}

private class SkyTextureWorker(
    private val context: Context,
    private val surfaceTexture: SurfaceTexture,
    private val ready: (Int) -> Unit,
    private val failed: () -> Unit,
    private val framesRendered: AtomicInteger
) {
    private data class Request(val state: SkyTextureState?, val width: Int, val height: Int, val active: Boolean)
    private val thread = HandlerThread("Astra-sky-render").apply { start() }
    private val handler = Handler(thread.looper)
    private val closed = AtomicBoolean(false)
    private val queued = AtomicBoolean(false)
    @Volatile private var request = Request(null, 0, 0, false)
    private var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var nativeSurface: Surface? = null
    private var program = 0
    private var texture = 0
    private var textureWidth = 0
    private var textureHeight = 0
    private var initializationFailed = false
    private val uniforms = mutableMapOf<String, Int>()
    private val quad = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
        put(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)); position(0)
    }
    private val draw = Runnable {
        queued.set(false)
        val next = request
        if (!closed.get() && next.active && next.width > 0 && next.height > 0 && next.state != null && !initializationFailed) {
            try {
                if (program == 0) initialize()
                if (!closed.get()) render(next)
            } catch (_: Exception) {
                initializationFailed = true
                disposeGl()
                failed() // Never log coordinates or use an online fallback.
            }
        }
    }

    fun update(state: SkyTextureState?, width: Int, height: Int, active: Boolean) {
        request = Request(state, width, height, active)
        if (!closed.get() && queued.compareAndSet(false, true)) handler.post(draw)
    }

    fun close(releaseTexture: Boolean) {
        if (closed.compareAndSet(false, true)) {
            handler.removeCallbacks(draw)
            handler.post {
                disposeGl()
                if (releaseTexture) surfaceTexture.release()
                thread.quitSafely()
            }
        }
    }

    private fun initialize() {
        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(display != EGL14.EGL_NO_DISPLAY)
        val version = IntArray(2)
        check(EGL14.eglInitialize(display, version, 0, version, 1))
        val configs = arrayOfNulls<EGLConfig>(1)
        val count = IntArray(1)
        check(EGL14.eglChooseConfig(display, intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
            EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8, EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_NONE), 0, configs, 0, 1, count, 0) && count[0] > 0)
        val config = checkNotNull(configs[0])
        eglContext = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT,
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0)
        check(eglContext != EGL14.EGL_NO_CONTEXT)
        nativeSurface = Surface(surfaceTexture)
        eglSurface = EGL14.eglCreateWindowSurface(display, config, nativeSurface,
            intArrayOf(EGL14.EGL_NONE), 0)
        check(eglSurface != EGL14.EGL_NO_SURFACE)
        check(EGL14.eglMakeCurrent(display, eglSurface, eglSurface, eglContext))
        val vertex = compile(GLES20.GL_VERTEX_SHADER,
            "attribute vec2 position; void main() { gl_Position = vec4(position, 0.0, 1.0); }")
        val fragmentText = context.assets.open("milky_way.frag").bufferedReader().use { it.readText() }
        val fragment = try { compile(GLES20.GL_FRAGMENT_SHADER, fragmentText) }
            catch (_: IllegalStateException) { compile(GLES20.GL_FRAGMENT_SHADER, fragmentText.replace("precision highp float", "precision mediump float")) }
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertex)
        GLES20.glAttachShader(program, fragment)
        GLES20.glBindAttribLocation(program, 0, "position")
        GLES20.glLinkProgram(program)
        GLES20.glDeleteShader(vertex)
        GLES20.glDeleteShader(fragment)
        val linked = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0)
        check(linked[0] != 0)

        val maximum = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maximum, 0)
        var sample = 1
        val memory = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        if (memory.isLowRamDevice || memory.memoryClass < 192) sample = 2
        while (3840 / sample > maximum[0] && sample < 8) sample *= 2
        check(maximum[0] >= 512)
        val bitmap = decodeTexture(sample)
        try {
            check(bitmap.width <= maximum[0] && bitmap.height <= maximum[0])
            textureWidth = bitmap.width
            textureHeight = bitmap.height
            val ids = IntArray(1)
            GLES20.glGenTextures(1, ids, 0)
            texture = ids[0]
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
            // NPOT works on ES2 with clamping and no mipmaps. The shader wraps/bilinearly blends the RA seam.
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            check(GLES20.glGetError() == GLES20.GL_NO_ERROR)
            ready(bitmap.allocationByteCount)
        } finally { bitmap.recycle() }
    }

    private fun decodeTexture(sample: Int): android.graphics.Bitmap = try {
        context.assets.open("milkyway_gaia_2020.jpg").use {
            checkNotNull(BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply {
                inScaled = false
                inSampleSize = sample
                inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
            }))
        }
    } catch (_: OutOfMemoryError) {
        if (sample >= 8) error("Insufficient memory for sky texture")
        decodeTexture(sample * 2)
    }

    private fun compile(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            GLES20.glDeleteShader(shader)
            error("Sky shader could not be compiled")
        }
        return shader
    }

    private fun uniform(name: String) = uniforms.getOrPut(name) { GLES20.glGetUniformLocation(program, name) }

    private fun render(next: Request) {
        val state = checkNotNull(next.state)
        val appearance = state.appearance.normalized()
        GLES20.glViewport(0, 0, next.width, next.height)
        GLES20.glUseProgram(program)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glUniform2f(uniform("resolution"), next.width.toFloat(), next.height.toFloat())
        GLES20.glUniform2f(uniform("textureSize"), textureWidth.toFloat(), textureHeight.toFloat())
        GLES20.glUniform3f(uniform("view"), Math.toRadians(state.azimuth).toFloat(),
            Math.toRadians(state.altitude).toFloat(), Math.toRadians(state.fov).toFloat())
        GLES20.glUniformMatrix3fv(uniform("horizontalToJ2000"), 1, false, state.frame.horizontalToJ2000, 0)
        GLES20.glUniform1f(uniform("arMode"), if (state.arMode) 1f else 0f)
        GLES20.glUniform1f(uniform("enhanced"), if (appearance.mode == MilkyWayMode.PHOTO && !state.arMode) 1f else 0f)
        val strength = if (appearance.mode == MilkyWayMode.OFF || (state.arMode && !appearance.showInAr)) 0f else appearance.intensity
        GLES20.glUniform1f(uniform("strength"), strength)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
        GLES20.glUniform1i(uniform("skyTexture"), 0)
        quad.position(0)
        GLES20.glEnableVertexAttribArray(0)
        GLES20.glVertexAttribPointer(0, 2, GLES20.GL_FLOAT, false, 0, quad)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        check(EGL14.eglSwapBuffers(display, eglSurface))
        framesRendered.incrementAndGet()
    }

    private fun disposeGl() {
        if (display != EGL14.EGL_NO_DISPLAY) {
            if (program != 0) GLES20.glDeleteProgram(program)
            if (texture != 0) GLES20.glDeleteTextures(1, intArrayOf(texture), 0)
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, eglSurface)
            if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, eglContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(display)
        }
        nativeSurface?.release()
        nativeSurface = null
        program = 0
        texture = 0
        display = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT
        eglSurface = EGL14.EGL_NO_SURFACE
    }
}
