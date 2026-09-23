package de.projektastra.app

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.projektastra.app.widget.AstraWidgetUpdater
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.time.Instant
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

/** Inflates the actual RemoteViews layout; no launcher settings or user preferences are changed. */
@RunWith(AndroidJUnit4::class)
class WidgetLayoutTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test fun initialLayoutDoesNotInventObservations() {
        instrumentation.runOnMainSync {
            val context = instrumentation.targetContext
            val view = LayoutInflater.from(context).inflate(R.layout.widget_astra_tonight, FrameLayout(context), false)
            assertEquals("—", view.findViewById<TextView>(R.id.widget_weather_score).text.toString())
            assertEquals("Standort: —", view.findViewById<TextView>(R.id.widget_location).text.toString())
            assertEquals("Nacht: —", view.findViewById<TextView>(R.id.widget_darkness_window).text.toString())
        }
    }

    @Test fun remoteViewsFitMinimumSizeAtNormalFont() = checkLayout(fontScale = 1f)

    @Test fun remoteViewsFitMinimumSizeAtDoubleFont() = checkLayout(fontScale = 2f)

    @Test fun missingWeatherIsNotReplacedByAnInventedScore() {
        val app = instrumentation.targetContext
        val prefix = "widget_test_${UUID.randomUUID()}_"
        val context = object : ContextWrapper(app) {
            override fun getSharedPreferences(name: String, mode: Int) =
                super.getSharedPreferences(prefix + name, mode)
        }
        try {
            assertNull(AstraWidgetUpdater.getCachedWeatherScore(context))
            val state = AstraWidgetUpdater.calculateState(savedLocation = null)
            assertNull(state.weatherScore)
            instrumentation.runOnMainSync {
                val view = AstraWidgetUpdater.buildRemoteViews(context, state).apply(context, FrameLayout(context))
                assertEquals("—", view.findViewById<TextView>(R.id.widget_weather_score).text.toString())
            }
            AstraWidgetUpdater.saveWeatherScore(context, 83)
            assertEquals(83, AstraWidgetUpdater.getCachedWeatherScore(context))
        } finally {
            app.deleteSharedPreferences(prefix + "astra_widget_prefs")
        }
    }

    private fun checkLayout(fontScale: Float) {
        instrumentation.runOnMainSync {
            val app = instrumentation.targetContext
            val configuration = Configuration(app.resources.configuration).apply { this.fontScale = fontScale }
            val context = app.createConfigurationContext(configuration)
            val state = AstraWidgetUpdater.calculateState(GeoPoint(52.52, 13.405, 34.0),
                Instant.parse("2026-09-20T22:00:00Z"), weatherScore = 100)
                .copy(moonPhaseLabel = "Abnehmender Mond", moonIlluminationPercent = 100,
                    lastKnownLocationLabel = "33,9°S, 170,7°W")
            val root = AstraWidgetUpdater.buildRemoteViews(context, state)
                .apply(context, FrameLayout(context)) as ViewGroup
            assertEquals(fontScale, root.resources.configuration.fontScale, 0.01f)
            val density = context.resources.displayMetrics.density
            val width = (180 * density).roundToInt()
            val height = (110 * density).roundToInt()
            root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
            root.layout(0, 0, width, height)

            val visibleText = mutableListOf<TextView>()
            fun collect(view: View) {
                if (view.visibility != View.VISIBLE) return
                if (view is TextView) visibleText += view
                if (view is ViewGroup) repeat(view.childCount) { collect(view.getChildAt(it)) }
            }
            collect(root)
            val rectangles = visibleText.map { text ->
                val rect = Rect(0, 0, text.width, text.height)
                root.offsetDescendantRectToMyCoords(text, rect)
                assertTrue("No room for ${text.resources.getResourceEntryName(text.id)}: $rect", rect.width() > 0 && rect.height() > 0)
                assertTrue("Text escaped widget bounds: $rect", rect.left >= 0 && rect.top >= 0 && rect.right <= width && rect.bottom <= height)
                assertTrue("Text must respect at least 11sp", text.textSize + 0.5f >=
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 11f, text.resources.displayMetrics))
                assertTrue("Text line was clipped vertically", text.layout.height <= text.height - text.compoundPaddingTop - text.compoundPaddingBottom)
                rect
            }
            for (first in rectangles.indices) for (second in first + 1 until rectangles.size) {
                assertFalse("Widget text fields overlap", Rect.intersects(rectangles[first], rectangles[second]))
            }
            listOf(R.id.widget_location, R.id.widget_moon_phase, R.id.widget_weather_score, R.id.widget_darkness_window)
                .forEach { assertEquals(View.VISIBLE, root.findViewById<View>(it).visibility) }
            assertEquals(if (fontScale > 1.3f) "☁ 100" else "100/100",
                root.findViewById<TextView>(R.id.widget_weather_score).text.toString())
            val darkness = root.findViewById<TextView>(R.id.widget_darkness_window)
            assertTrue(darkness.text.matches(Regex("\\d{2}:\\d{2}–\\d{2}:\\d{2}")))
            assertEquals("Both real night times must be visible, not only stored in TextView.text", 0,
                darkness.layout.getEllipsisCount(0))
            assertEquals(darkness.text.length, darkness.layout.getLineVisibleEnd(0))
            assertEquals(state.darknessWindow, darkness.contentDescription.toString())
            assertTrue(root.contentDescription.contains("Beleuchtung: 100%"))
            assertTrue(root.contentDescription.contains(state.darknessWindow))
            assertTrue(root.contentDescription.contains(state.lastKnownLocationLabel))
            // Only this synthetic fixture is rendered; no launcher/user state is captured.
            val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            try {
                root.draw(Canvas(image))
                FileOutputStream(File(requireNotNull(app.externalCacheDir), "widget-layout-$fontScale.png")).use {
                    assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it))
                }
            } finally {
                image.recycle()
            }
        }
    }
}
