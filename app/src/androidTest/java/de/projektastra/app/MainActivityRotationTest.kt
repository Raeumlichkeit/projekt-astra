package de.projektastra.app

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.junit.Test

@RunWith(AndroidJUnit4::class)
class MainActivityRotationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test fun weatherTabRemainsSelectedAndUsableAcrossRotation() {
        val context = instrumentation.targetContext
        val privacy = context.getSharedPreferences("astra_privacy", Context.MODE_PRIVATE)
        val originalPrivacy = privacy.all.mapValues { (_, value) ->
            if (value is Set<*>) value.toSet() else value
        }
        try {
            assertTrue(privacy.edit().putBoolean("online", false).putBoolean("terrain", false)
                .putBoolean("decision_v1", true).commit())
            SecureNetwork.configure(PrivacyOptions())
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                try {
                    scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
                    val portrait = awaitActivity(scenario, Configuration.ORIENTATION_PORTRAIT)
                    click("Wetter")
                    awaitUi("Weather must open in portrait") { weatherSelected() }

                    scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
                    val landscape = awaitActivity(scenario, Configuration.ORIENTATION_LANDSCAPE, portrait)
                    awaitUi("Weather must survive Activity recreation in landscape") { weatherSelected() }
                    click("Info")
                    awaitUi("Navigation must work in landscape") { tabSelected("Info") }
                    click("Wetter")
                    awaitUi("Weather must reopen in landscape") { weatherSelected() }

                    scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
                    awaitActivity(scenario, Configuration.ORIENTATION_PORTRAIT, landscape)
                    awaitUi("Weather must survive return to portrait") { weatherSelected() }
                } finally {
                    scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
                }
            }
        } finally {
            val editor = privacy.edit().clear()
            originalPrivacy.forEach { (key, value) ->
                when (value) {
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is String -> editor.putString(key, value)
                    is Set<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        editor.putStringSet(key, value as Set<String>)
                    }
                    else -> error("Unsupported privacy preference type for $key")
                }
            }
            assertTrue("Restore original privacy settings", editor.commit())
            SecureNetwork.configure(PrivacySettings.load(context))
        }
    }

    private fun awaitActivity(
        scenario: ActivityScenario<MainActivity>, orientation: Int, previous: MainActivity? = null
    ): MainActivity {
        var match: MainActivity? = null
        awaitUi("Activity must recreate in orientation $orientation") {
            scenario.onActivity { activity ->
                if (activity.resources.configuration.orientation == orientation && activity !== previous) match = activity
            }
            match != null
        }
        return match!!
    }

    private fun weatherSelected() = tabSelected("Wetter") && nodes().any {
        it.text?.toString() == "Beobachtungswetter" && it.isVisibleToUser
    }

    private fun labeled(node: AccessibilityNodeInfo, label: String) = node.isVisibleToUser &&
        (node.text?.toString() == label || node.contentDescription?.toString() == label)

    private fun tabSelected(label: String): Boolean {
        return nodes().filter { labeled(it, label) }.any { item ->
            var node: AccessibilityNodeInfo? = item
            while (node != null) {
                if (node.isSelected) return@any true
                node = node.parent
            }
            false
        }
    }

    private fun click(label: String) {
        awaitUi("$label must be visible") {
            nodes().any { labeled(it, label) }
        }
        var node: AccessibilityNodeInfo? = nodes().firstOrNull { labeled(it, label) }
            ?: throw AssertionError("Missing $label: ${visibleTexts()}")
        while (node != null && !node.isClickable) node = node.parent
        assertTrue("$label must be clickable", node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true)
    }

    private fun nodes(): List<AccessibilityNodeInfo> {
        fun collect(node: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> =
            if (node == null) emptyList() else listOf(node) +
                (0 until node.childCount).flatMap { collect(node.getChild(it)) }
        return collect(instrumentation.uiAutomation.rootInActiveWindow)
    }

    private fun visibleTexts() = nodes().mapNotNull { it.text?.toString() ?: it.contentDescription?.toString() }
        .joinToString(" | ")

    private fun awaitUi(message: String, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            SystemClock.sleep(100)
        }
        throw AssertionError("$message: ${visibleTexts()}")
    }
}
