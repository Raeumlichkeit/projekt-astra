package de.projektastra.app

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/** Exercises the real pull gesture and lifecycle without servers, saved consent, or Espresso input. */
@RunWith(AndroidJUnit4::class)
class WeatherScreenRefreshTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var scenario: ActivityScenario<ComponentActivity>? = null
    private val requests = CopyOnWriteArrayList<(Result<WeatherSnapshot>) -> Unit>()
    private val mapMounts = AtomicInteger()
    private val locationRequests = AtomicInteger()
    private val snapshot = WeatherSnapshot(12.0, 20, 5.0, 12000.0, "16.09. 20:00 MESZ", emptyList(),
        Instant.parse("2026-09-16T18:00:00Z"), Instant.now().minusSeconds(600))

    @After fun close() {
        scenario?.close()
        SecureNetwork.configure(PrivacyOptions())
        SecureNetwork.setForeground(false)
    }

    @Test fun pullingKeepsScoreVisibleAndDoesNotRemountMapEvenOnFailure() {
        launch()
        await { requests.size == 1 }
        complete(0, Result.success(snapshot))
        awaitText("Astra-Score von 100 Punkten")
        pull()
        await { requests.size == 2 }
        awaitText("bisherige Daten bleiben sichtbar")
        assertTrue(hasText("Astra-Score von 100 Punkten"))
        assertEquals(1, mapMounts.get())
        assertEquals(1, locationRequests.get())
        complete(1, Result.failure(IllegalStateException("offline")))
        awaitText("Aktualisierung fehlgeschlagen")
        assertTrue(hasText("Astra-Score von 100 Punkten"))
        assertEquals(1, mapMounts.get())
        instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
            java.io.File(instrumentation.targetContext.cacheDir, "weather-retained-ui.png").outputStream()
                .use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    @Test fun resumeStartsFreshRequestAndIgnoresOldCallback() {
        launch()
        await { requests.size == 1 }
        complete(0, Result.success(snapshot))
        awaitText("Astra-Score von 100 Punkten")
        pull()
        await { requests.size == 2 }
        SecureNetwork.setForeground(false)
        scenario!!.moveToState(Lifecycle.State.CREATED)
        SecureNetwork.setForeground(true)
        scenario!!.moveToState(Lifecycle.State.RESUMED)
        await { requests.size == 3 }
        // The previous callback is delivered after the new foreground request has begun.
        complete(1, Result.failure(IllegalStateException("late failure")))
        awaitText("bisherige Daten bleiben sichtbar")
        assertFalse(hasText("Aktualisierung fehlgeschlagen"))
        assertEquals(1, mapMounts.get())
        complete(2, Result.success(snapshot.copy(temperature = 15.0)))
        await { !hasText("bisherige Daten bleiben sichtbar") }
        assertTrue(hasText("Astra-Score von 100 Punkten"))
    }

    private fun launch() {
        SecureNetwork.configure(PrivacyOptions(online = true))
        SecureNetwork.setForeground(true)
        scenario = ActivityScenario.launch(ComponentActivity::class.java).also { scene ->
            scene.onActivity { activity ->
                activity.setContent {
                    MaterialTheme(colorScheme = darkColorScheme()) {
                        Surface {
                            WeatherScreen(null, null, { locationRequests.incrementAndGet() },
                                loadWeather = { _, callback -> requests.add(callback) },
                                loadLight = { _, _, callback -> callback(LightPollutionState.Unavailable) },
                                mapContent = { _, _ ->
                                    DisposableEffect(Unit) { mapMounts.incrementAndGet(); onDispose {} }
                                    Text("Lokale Testkarte")
                                })
                        }
                    }
                }
            }
        }
    }

    private fun complete(index: Int, result: Result<WeatherSnapshot>) {
        instrumentation.runOnMainSync { requests[index](result) }
        instrumentation.waitForIdleSync()
    }

    private fun pull() {
        val start = SystemClock.uptimeMillis()
        fun touch(action: Int, fraction: Float) {
            scenario!!.onActivity { activity ->
                val decor = activity.window.decorView
                val event = MotionEvent.obtain(start, SystemClock.uptimeMillis(), action,
                    decor.width * .5f, decor.height * fraction, 0)
                try { decor.dispatchTouchEvent(event) } finally { event.recycle() }
            }
            SystemClock.sleep(24)
        }
        touch(MotionEvent.ACTION_DOWN, .14f)
        (1..15).forEach { touch(MotionEvent.ACTION_MOVE, .14f + it * .036f) }
        touch(MotionEvent.ACTION_UP, .68f)
        instrumentation.waitForIdleSync()
    }

    private fun visibleText(): String {
        fun collect(node: android.view.accessibility.AccessibilityNodeInfo?, depth: Int): String {
            if (node == null || depth > 30) return ""
            return node.text?.toString().orEmpty() + "\n" + (0 until node.childCount).joinToString("\n") {
                collect(node.getChild(it), depth + 1)
            }
        }
        return collect(instrumentation.uiAutomation.rootInActiveWindow, 0)
    }
    private fun hasText(text: String) = visibleText().contains(text)
    private fun awaitText(text: String) = await { hasText(text) }
    private fun await(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 8000
        do {
            if (condition()) return
            SystemClock.sleep(100)
        } while (SystemClock.uptimeMillis() < deadline)
        fail("Weather UI condition timed out (requests=${requests.size}, map mounts=${mapMounts.get()}): ${visibleText()}")
    }
}
