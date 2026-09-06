package de.projektastra.app

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

// Platform input/PixelCopy avoid Espresso's removed InputManager.getInstance API on Android 17.
@RunWith(AndroidJUnit4::class)
class SkyRenderingTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var scenario: ActivityScenario<ComponentActivity>? = null
    private var bounds = Rect()

    @After fun closeActivity() { scenario?.close() }

    private fun render(content: @Composable (Modifier) -> Unit) {
        val ready = CountDownLatch(1)
        scenario = ActivityScenario.launch(ComponentActivity::class.java).also { scene ->
            scene.onActivity { activity ->
                activity.setContent {
                    Box(Modifier.requiredSize(300.dp, 200.dp)) {
                        content(Modifier.onGloballyPositioned { layout ->
                            val position = layout.positionInWindow()
                            bounds = Rect(position.x.roundToInt(), position.y.roundToInt(),
                                position.x.roundToInt() + layout.size.width, position.y.roundToInt() + layout.size.height)
                        })
                    }
                }
                val view = activity.window.decorView
                view.viewTreeObserver.addOnDrawListener(object : ViewTreeObserver.OnDrawListener {
                    override fun onDraw() {
                        if (bounds.width() > 0) view.post {
                            view.viewTreeObserver.removeOnDrawListener(this)
                            view.postOnAnimation { ready.countDown() }
                        }
                    }
                })
            }
        }
        assertTrue("Sky canvas did not draw", ready.await(8, TimeUnit.SECONDS))
        instrumentation.waitForIdleSync()
    }

    private fun capture(): Bitmap {
        val bitmap = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888)
        val done = CountDownLatch(1)
        var result = -1
        scenario!!.onActivity { activity ->
            PixelCopy.request(activity.window, bounds, bitmap, { code -> result = code; done.countDown() }, Handler(Looper.getMainLooper()))
        }
        assertTrue("PixelCopy timed out", done.await(5, TimeUnit.SECONDS))
        assertEquals(PixelCopy.SUCCESS, result)
        return bitmap
    }

    private fun touch(action: Int, point: Offset, downTime: Long) {
        scenario!!.onActivity { activity ->
            val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, bounds.left + point.x, bounds.top + point.y, 0)
            try { activity.window.decorView.dispatchTouchEvent(event) } finally { event.recycle() }
        }
        instrumentation.waitForIdleSync()
    }

    @Test fun milkyWayReachesBothEdgesWithBothEndpointsOutside() {
        render { modifier ->
            SkyCanvas(emptyList(), 0.0, 0.0, 40.0, false,
                listOf(HorizontalCoordinates(330.0, 8.0), HorizontalCoordinates(30.0, 8.0)),
                emptyList(), false, null, false, { _, _, _ -> }, {}, modifier)
        }
        val pixels = capture()
        val projection = SkyProjection(0.0, 0.0, pixels.width.toFloat(), pixels.height.toFloat(), 40.0, true)
        val line = projection.segments(HorizontalCoordinates(330.0, 8.0), HorizontalCoordinates(30.0, 8.0)).single()
        val y = line.start.y.roundToInt()
        listOf(5, pixels.width - 6).forEach { x ->
            val bandBlue = (y - 1..y + 1).maxOf { Color.blue(pixels.getPixel(x, it)) }
            assertTrue("Milky Way missing at x=$x", bandBlue > Color.blue(pixels.getPixel(x, y + 45)) + 20)
        }
    }

    @Test fun partlyVisibleStarRemainsSelectableAtRightEdge() {
        val star = VisibleObject(CelestialObject("Randstern", "TEST", 0.0, 0.0, 0.0, 1.0, "A1V"),
            HorizontalCoordinates(20.05, 8.0))
        var selected: VisibleObject? = null
        render { modifier ->
            SkyCanvas(listOf(star), 0.0, 0.0, 40.0, false, emptyList(), emptyList(),
                false, null, true, { _, _, _ -> }, { selected = it }, modifier)
        }
        val point = SkyProjection(0.0, 0.0, bounds.width().toFloat(), bounds.height().toFloat(), 40.0, true)
            .point(star.position, 32f)!!
        val tap = Offset(bounds.width() - 2f, point.y)
        val time = SystemClock.uptimeMillis()
        touch(MotionEvent.ACTION_DOWN, tap, time)
        touch(MotionEvent.ACTION_UP, tap, time)
        assertEquals(star, selected)
    }

    @Test fun draggingStillUpdatesTheManualView() {
        var azimuth by mutableDoubleStateOf(0.0)
        render { modifier ->
            SkyCanvas(emptyList(), azimuth, 0.0, 40.0, false, emptyList(), emptyList(),
                false, null, true, { nextAzimuth, _, _ -> azimuth = nextAzimuth }, {}, modifier)
        }
        val time = SystemClock.uptimeMillis()
        touch(MotionEvent.ACTION_DOWN, Offset(bounds.width() * 0.8f, bounds.height() / 2f), time)
        (1..10).forEach { step ->
            touch(MotionEvent.ACTION_MOVE, Offset(bounds.width() * (0.8f - step * 0.06f), bounds.height() / 2f), time)
        }
        touch(MotionEvent.ACTION_UP, Offset(bounds.width() * 0.2f, bounds.height() / 2f), time)
        assertTrue(azimuth > 0.0 && azimuth < 40.0)
    }

    @Test fun groundStillCoversViewWhenHorizonIsAboveTheTopEdge() {
        render { modifier ->
            SkyCanvas(emptyList(), 0.0, -40.0, 40.0, false, emptyList(), emptyList(),
                false, null, false, { _, _, _ -> }, {}, modifier)
        }
        val pixels = capture()
        val ground = pixels.getPixel(pixels.width / 3 + 10, pixels.height / 3)
        assertTrue("Off-screen horizon must not reveal sky", Color.blue(ground) < 23)
    }

    @Test fun starBeyondZenithIsRenderedInsteadOfAnEmptyTopRegion() {
        // Facing north at 80°: this south-facing star is above the zenith in the view.
        val star = VisibleObject(CelestialObject("Zenittest", "TEST", 0.0, 0.0, 0.0, 1.0, "A1V"),
            HorizontalCoordinates(180.0, 85.0))
        render { modifier ->
            SkyCanvas(listOf(star), 0.0, 80.0, 95.0, false, emptyList(), emptyList(),
                false, null, false, { _, _, _ -> }, {}, modifier)
        }
        val pixels = capture()
        val point = SkyProjection(0.0, 80.0, pixels.width.toFloat(), pixels.height.toFloat(), 95.0, true).point(star.position)!!
        assertTrue(point.y < pixels.height / 2)
        assertTrue(Color.blue(pixels.getPixel(point.x.roundToInt(), point.y.roundToInt())) > 180)
    }
}
