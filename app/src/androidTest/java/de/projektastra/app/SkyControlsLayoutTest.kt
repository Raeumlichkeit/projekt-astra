package de.projektastra.app

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 37) // Android 17 renders this permission CTA in a remote SurfaceView.
class SkyControlsLayoutTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @After fun clearPermissionDialog() = dismissSystemPermissionDialog()

    @Test fun locationButtonDoesNotCoverSkyWhenControlsAreScrolled() {
        dismissSystemPermissionDialog()
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            showSky(scenario)
            await("Kartenbedienung ausklappen") {
                nodes().firstOrNull { it.contentDescription?.toString() == "Kartenbedienung ausklappen" }
            }
            click("Kartenbedienung ausklappen")
            awaitLocationButtonBounds(scenario)
            instrumentation.waitForIdleSync()
            val mapTop = await("expanded sky below fixed location button") {
                val skyTop = awaitMapTop(scenario)
                skyTop.takeIf { it >= awaitLocationButtonBounds(scenario).bottom }
            }
            assertNoLocationBlueOverSky(mapTop)
            repeat(6) {
                swipeControlsUp(scenario, mapTop, 120f, 200f)
                SystemClock.sleep(100)
                assertNoLocationBlueOverSky(mapTop)
            }
            val bounds = awaitLocationButtonBounds(scenario)
            assertTrue("Location button must remain fully above the sky: $bounds, sky starts at $mapTop",
                bounds.height() > 0 && bounds.bottom <= mapTop)
        }
    }

    @Test fun compactLandscapeOpensLocationButtonInDismissibleDialog() {
        dismissSystemPermissionDialog()
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            await("landscape activity") {
                var landscape = false
                scenario.onActivity { landscape = it.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
                landscape.takeIf { it }
            }
            showSky(scenario, compact = true)
            await("Kartenbedienung ausklappen") { visibleNode("Kartenbedienung ausklappen") }
            val collapsedMapTop = awaitMapTop(scenario)
            click("Kartenbedienung ausklappen")
            val mapTop = await("expanded landscape viewport") {
                awaitMapTop(scenario).takeIf { it > collapsedMapTop + 20 }
            }
            instrumentation.waitForIdleSync()
            assertTrue("Compact landscape must not render a fixed native location CTA",
                locationButtonBounds(scenario) == null)
            var swipes = 0
            while (visibleNode("Standort verwenden") == null && swipes++ < 24) {
                swipeControlsUp(scenario, awaitMapTop(scenario), 12f, 45f)
                SystemClock.sleep(50)
            }
            await("compact location action (sky top $mapTop; visible: ${nodes().mapNotNull { it.text?.toString() }.joinToString(" | ")})") {
                visibleNode("Standort verwenden")
            }
            assertTrue("Compact landscape must not keep a remote SurfaceView over the map",
                locationButtonBounds(scenario) == null)
            click("Standort verwenden")
            await("location dialog cancel action") { visibleNode("Abbrechen") }
            click("Abbrechen")
            await("location dialog dismissed") { true.takeIf { visibleNode("Abbrechen") == null } }
        }
    }

    private fun showSky(scenario: ActivityScenario<ComponentActivity>, compact: Boolean = false) {
        scenario.onActivity { activity ->
            activity.setContent {
                if (compact) Box(Modifier.fillMaxWidth().height(320.dp)) { TestSkyScreen() }
                else TestSkyScreen()
            }
        }
    }

    @Composable
    private fun TestSkyScreen() {
        SkyScreen(
            location = null,
            locationPermissionGranted = false,
            cameraPermissionGranted = false,
            arEnabled = false,
            redLightMode = false,
            fullscreen = false,
            setFullscreen = {},
            favoriteObjectIds = emptySet(),
            requestedObjectId = "HIP 32349",
            consumeObjectRequest = {},
            requestedPosition = null,
            consumePositionRequest = {},
            skyTime = SkyTimeState(Instant.parse("2026-09-24T13:52:00Z")),
            selectTime = {}, playTime = {}, pauseTime = {}, nowTime = {},
            timeNotice = null, clearTimeNotice = {}, useManualMap = {},
            toggleRedLightMode = {}, toggleFavorite = {},
            onLocationPermissionResult = {}, onResetLocation = {}, toggleAr = {}
        )
    }

    private fun swipeControlsUp(scenario: ActivityScenario<ComponentActivity>, mapTop: Int,
                                fromDp: Float = 76f, toDp: Float = 180f) {
        var density = 1f
        var x = 0f
        scenario.onActivity { activity ->
            val decor = activity.window.decorView
            density = activity.resources.displayMetrics.density
            x = IntArray(2).also(decor::getLocationOnScreen)[0] + decor.width * 0.9f
        }
        // Portrait offsets avoid the fixed CTA; landscape passes offsets within its compact scroll area.
        val fromY = mapTop - fromDp * density
        val toY = mapTop - toDp * density
        val downTime = SystemClock.uptimeMillis()
        fun send(action: Int, y: Float) {
            val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
            try {
                assertTrue("Could not swipe sky controls", instrumentation.uiAutomation.injectInputEvent(event, true))
            } finally {
                event.recycle()
            }
        }
        send(MotionEvent.ACTION_DOWN, fromY)
        (1..8).forEach { step ->
            SystemClock.sleep(12)
            send(MotionEvent.ACTION_MOVE, fromY + (toY - fromY) * step / 8f)
        }
        send(MotionEvent.ACTION_UP, toY)
    }

    private fun assertNoLocationBlueOverSky(mapTop: Int) {
        val screenshot = instrumentation.uiAutomation.takeScreenshot()
            ?: throw AssertionError("Unable to capture sky controls")
        try {
            val firstX = screenshot.width / 4
            val lastX = screenshot.width * 3 / 4
            val firstY = (mapTop + 8).coerceAtMost(screenshot.height - 1)
            val lastY = (mapTop + 40).coerceAtMost(screenshot.height - 1)
            val bluePixels = (firstY..lastY).sumOf { y ->
                (firstX..lastX).count { x ->
                    val pixel = screenshot.getPixel(x, y)
                    Color.red(pixel) in 85..135 && Color.green(pixel) in 145..190 && Color.blue(pixel) > 220
                }
            }
            val sampledPixels = (lastX - firstX + 1) * (lastY - firstY + 1)
            assertTrue("Location CTA blue must not be painted over the map ($bluePixels/$sampledPixels pixels)",
                bluePixels < sampledPixels / 10)
        } finally {
            screenshot.recycle()
        }
    }

    private fun awaitMapTop(scenario: ActivityScenario<ComponentActivity>): Int {
        return await("Sky texture viewport") {
            var top: Int? = null
            scenario.onActivity { activity ->
                fun find(view: View): SkyTextureView? = when {
                    view is SkyTextureView -> view
                    view is ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { find(view.getChildAt(it)) }
                    else -> null
                }
                top = find(activity.window.decorView)?.takeIf { it.width > 0 && it.height > 0 }?.let { view ->
                    IntArray(2).also(view::getLocationOnScreen)[1]
                }
            }
            top
        }
    }

    private fun awaitLocationButtonBounds(scenario: ActivityScenario<ComponentActivity>): Rect {
        return await("LocationButton SurfaceView") { locationButtonBounds(scenario) }
    }

    private fun locationButtonBounds(scenario: ActivityScenario<ComponentActivity>): Rect? {
        var bounds: Rect? = null
        scenario.onActivity { activity ->
            fun find(view: View): SurfaceView? = when {
                view is SurfaceView -> view
                view is ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { find(view.getChildAt(it)) }
                else -> null
            }
            bounds = find(activity.window.decorView)?.takeIf { it.width > 0 && it.height > 0 }?.let { view ->
                IntArray(2).also(view::getLocationOnScreen).let { position ->
                    Rect(position[0], position[1], position[0] + view.width, position[1] + view.height)
                }
            }
        }
        return bounds
    }

    private fun click(label: String) {
        val child = visibleNode(label) ?: throw AssertionError("Missing $label")
        var node: AccessibilityNodeInfo? = child
        while (node != null && !node.isClickable) node = node.parent
        assertTrue("$label must be clickable", node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true)
    }

    private fun visibleNode(label: String) = nodes().firstOrNull {
        it.isVisibleToUser && (it.contentDescription?.toString()?.contains(label) == true ||
            it.text?.toString()?.contains(label) == true)
    }

    private fun dismissSystemPermissionDialog() {
        repeat(2) {
            val packageName = instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString().orEmpty()
            if (!packageName.contains("permissioncontroller", ignoreCase = true)) return
            val downTime = SystemClock.uptimeMillis()
            listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP).forEach { action ->
                val event = KeyEvent(downTime, SystemClock.uptimeMillis(), action, KeyEvent.KEYCODE_BACK, 0)
                instrumentation.uiAutomation.injectInputEvent(event, true)
            }
            SystemClock.sleep(150)
        }
    }

    private fun nodes(): List<AccessibilityNodeInfo> {
        fun collect(node: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> =
            if (node == null) emptyList() else listOf(node) +
                (0 until node.childCount).flatMap { collect(node.getChild(it)) }
        return collect(instrumentation.uiAutomation.rootInActiveWindow)
    }

    private fun <T : Any> await(message: String, result: () -> T?): T {
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (SystemClock.uptimeMillis() < deadline) {
            result()?.let { return it }
            SystemClock.sleep(100)
        }
        throw AssertionError("Timed out waiting for $message")
    }
}
