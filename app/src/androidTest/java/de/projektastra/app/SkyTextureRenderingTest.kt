package de.projektastra.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.SystemClock
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin

/** Real EGL/TextureView tests; no Espresso input APIs, screenshots, network or saved user settings. */
@RunWith(AndroidJUnit4::class)
class SkyTextureRenderingTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val frame = SkyCoordinateFrame(GeoPoint(52.52, 13.405, 34.0), Instant.parse("2026-09-06T21:00:00Z"))
    private var scenario: ActivityScenario<ComponentActivity>? = null
    private lateinit var view: SkyTextureView

    @After fun closeActivity() {
        scenario?.close()
        scenario = null
    }

    @Test fun enhancedMilkyWayIsBrighterAndOffKeepsOnlyTheBackground() {
        val naturalState = state(17.76033, -28.93617, MilkyWayMode.NATURAL)
        launch(naturalState)
        val natural = capture().useBitmap { meanLuminance(it) }
        update(naturalState.copy(appearance = SkyAppearance(MilkyWayMode.PHOTO)))
        val photo = capture().useBitmap { meanLuminance(it) }
        update(naturalState.copy(appearance = SkyAppearance(MilkyWayMode.OFF)))
        val off = capture().useBitmap { meanLuminance(it) }

        assertTrue("Photographic styling must visibly increase contrast", photo > natural + 0.02)
        assertTrue("Natural styling must still contain the Milky Way", natural > off + 0.01)
        assertTrue("OFF must not leave the photographic layer behind", off < 0.06)
        assertTrue("The texture must be loaded", view.textureBytes > 0)
        assertTrue("Texture memory must stay bounded", view.textureBytes <= 3840 * 1920 * 4)
    }

    @Test fun gpuTextureMatchesJ2000CoordinatesIncludingBothSidesOfTheRaSeam() {
        val coordinates = listOf(
            17.76033 to -28.93617, // Galactic centre: negative atan2 RA and southern declination.
            6.75248 to -16.71612,  // Opposite RA orientation, away from the centre.
            20.69053 to 45.28034,  // Northern Milky Way near Deneb.
            0.712 to 41.269,
            11.999 to 0.0,        // Explicitly exercise the wrapped RA=12h image edge.
            12.001 to 0.0
        )
        launch(state(coordinates.first().first, coordinates.first().second, MilkyWayMode.PHOTO))
        // Match the renderer's supported texture size, including GLES2 devices limited to 2048px.
        var sample = 1
        while ((3840 / sample) * (1920 / sample) * 4 > view.textureBytes && sample < 8) sample *= 2
        val source = instrumentation.targetContext.assets.open("milkyway_gaia_2020.jpg").use {
            requireNotNull(BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply {
                inScaled = false
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }))
        }
        source.useBitmap {
            coordinates.forEach { (ra, dec) ->
                val next = state(ra, dec, MilkyWayMode.PHOTO)
                update(next)
                capture().useBitmap { image ->
                    val actual = image.getPixel(image.width / 2, image.height / 2)
                    val expected = expectedPhotoPixel(source, ra, dec, next.altitude)
                    val channels = doubleArrayOf(Color.red(actual) / 255.0, Color.green(actual) / 255.0, Color.blue(actual) / 255.0)
                    channels.indices.forEach { channel ->
                        assertEquals("Texture registration at RA=$ra Dec=$dec channel=$channel",
                            expected[channel], channels[channel], 12.0 / 255.0)
                    }
                    assertEquals("Normal sky is opaque", 255, Color.alpha(actual))
                }
            }
        }
    }

    @Test fun arTextureIsTransparentUnlessExplicitlyEnabled() {
        val disabled = state(17.76033, -28.93617, MilkyWayMode.NATURAL).copy(arMode = true)
        launch(disabled)
        capture().useBitmap { bitmap ->
            assertEquals(0, Color.alpha(bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)))
        }
        update(disabled.copy(appearance = disabled.appearance.copy(showInAr = true)))
        val enabledAlpha = capture().useBitmap { bitmap ->
            Color.alpha(bitmap.getPixel(bitmap.width / 2, bitmap.height / 2))
        }
        assertTrue("AR opt-in must reveal a subtle, not opaque overlay", enabledAlpha in 1..97)
        update(disabled.copy(appearance = SkyAppearance(MilkyWayMode.OFF, showInAr = true)))
        capture().useBitmap { bitmap ->
            assertEquals("OFF must also disable an opted-in AR layer", 0,
                Color.alpha(bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)))
        }
    }

    @Test fun pausedRendererDoesNotDrawAndResumesWithTheLatestState() {
        val initial = state(17.76033, -28.93617, MilkyWayMode.PHOTO)
        launch(initial)
        val before = capture().useBitmap { meanLuminance(it) }
        instrumentation.runOnMainSync { view.setRenderingActive(false) }
        // Allow a frame already submitted before the pause to finish, then test the paused state.
        awaitStableFrameCount()
        val pausedCount = view.framesRendered.get()
        instrumentation.runOnMainSync {
            view.update(initial.copy(appearance = SkyAppearance(MilkyWayMode.OFF)))
        }
        SystemClock.sleep(300)
        assertEquals("Updates while paused must not submit GPU frames", pausedCount, view.framesRendered.get())

        instrumentation.runOnMainSync { view.setRenderingActive(true) }
        awaitFrameAfter(pausedCount)
        val resumed = capture().useBitmap { meanLuminance(it) }
        assertTrue("Resume must apply the last state received during pause", resumed < before - 0.02)
        assertTrue("The resumed OFF state must contain only the dark background", resumed < 0.06)
        awaitStableFrameCount()
        val idleCount = view.framesRendered.get()
        SystemClock.sleep(200)
        assertEquals("An unchanged view should render on demand only", idleCount, view.framesRendered.get())
    }

    private fun state(ra: Double, dec: Double, mode: MilkyWayMode): SkyTextureState {
        val horizontal = frame.horizontal(ra, dec)
        return SkyTextureState(frame, horizontal.azimuth, horizontal.altitude, 60.0, false, SkyAppearance(mode))
    }

    private fun launch(initial: SkyTextureState) {
        scenario = ActivityScenario.launch(ComponentActivity::class.java).also { scene ->
            scene.onActivity { activity ->
                view = SkyTextureView(activity)
                // Odd physical dimensions put the centre fragment exactly on the requested direction.
                val container = FrameLayout(activity)
                container.addView(view, FrameLayout.LayoutParams(301, 201))
                activity.setContentView(container)
                view.update(initial)
                view.setRenderingActive(true)
            }
        }
        awaitFrameAfter(0)
    }

    private fun update(next: SkyTextureState) {
        val previous = view.framesRendered.get()
        instrumentation.runOnMainSync { view.update(next) }
        awaitFrameAfter(previous)
    }

    private fun awaitFrameAfter(previous: Int) {
        val deadline = SystemClock.uptimeMillis() + 8_000
        while (view.framesRendered.get() <= previous && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(50)
        }
        assertTrue("EGL renderer did not submit a frame", view.framesRendered.get() > previous)
        // eglSwapBuffers precedes TextureView's UI-thread acquisition. Wait for two view frames.
        val acquired = CountDownLatch(1)
        instrumentation.runOnMainSync {
            view.postOnAnimation { view.postOnAnimation { acquired.countDown() } }
        }
        assertTrue("TextureView did not acquire its frame", acquired.await(3, TimeUnit.SECONDS))
        instrumentation.waitForIdleSync()
    }

    private fun awaitStableFrameCount() {
        val deadline = SystemClock.uptimeMillis() + 2_000
        var count = view.framesRendered.get()
        var stableSince = SystemClock.uptimeMillis()
        while (SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(50)
            val next = view.framesRendered.get()
            if (next != count) {
                count = next
                stableSince = SystemClock.uptimeMillis()
            }
            if (SystemClock.uptimeMillis() - stableSince >= 200) return
        }
        throw AssertionError("Renderer did not settle")
    }

    private fun capture(): Bitmap {
        var result: Bitmap? = null
        instrumentation.runOnMainSync { result = view.bitmap }
        return requireNotNull(result) { "TextureView did not provide a bitmap" }
    }

    private fun meanLuminance(bitmap: Bitmap): Double {
        var sum = 0.0
        val cx = bitmap.width / 2
        val cy = bitmap.height / 2
        for (y in cy - 10..cy + 10) for (x in cx - 10..cx + 10) {
            val pixel = bitmap.getPixel(x, y)
            sum += (0.2126 * Color.red(pixel) + 0.7152 * Color.green(pixel) + 0.0722 * Color.blue(pixel)) / 255.0
        }
        return sum / 441
    }

    /** Independent CPU reference: source-image coordinates, wrapped bilinear sampling and display tone. */
    private fun expectedPhotoPixel(source: Bitmap, ra: Double, dec: Double, altitude: Double): DoubleArray {
        val u = ((0.5 - ra / 24.0) % 1.0 + 1.0) % 1.0
        val px = u * source.width - 0.5
        val py = ((0.5 - dec / 180.0) * source.height - 0.5).coerceIn(0.0, source.height - 1.0)
        val x0 = floor(px).toInt()
        val y0 = floor(py).toInt()
        val tx = px - floor(px)
        val ty = py - floor(py)
        fun rgb(x: Int, y: Int): DoubleArray {
            val pixel = source.getPixel(((x % source.width) + source.width) % source.width, y.coerceIn(0, source.height - 1))
            return doubleArrayOf(Color.red(pixel) / 255.0, Color.green(pixel) / 255.0, Color.blue(pixel) / 255.0)
        }
        val a = rgb(x0, y0)
        val b = rgb(x0 + 1, y0)
        val c = rgb(x0, y0 + 1)
        val d = rgb(x0 + 1, y0 + 1)
        val sampled = DoubleArray(3) { i ->
            (a[i] * (1 - tx) + b[i] * tx) * (1 - ty) + (c[i] * (1 - tx) + d[i] * tx) * ty
        }
        val luminance = sampled[0] * 0.2126 + sampled[1] * 0.7152 + sampled[2] * 0.0722
        val glow = (1 - abs(sin(Math.toRadians(altitude)))).pow(5)
        val dark = doubleArrayOf(0.007, 0.012, 0.021)
        val horizon = doubleArrayOf(0.018, 0.028, 0.044)
        return DoubleArray(3) { i ->
            val background = dark[i] * (1 - glow) + horizon[i] * glow
            (background + (luminance * 0.15 + sampled[i] * 0.85).pow(0.9) * 0.94).coerceIn(0.0, 1.0)
        }
    }

    private inline fun <T> Bitmap.useBitmap(block: (Bitmap) -> T): T = try { block(this) } finally { recycle() }
}
