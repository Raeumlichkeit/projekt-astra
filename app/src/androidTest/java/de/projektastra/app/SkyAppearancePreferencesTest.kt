package de.projektastra.app

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SkyAppearancePreferencesTest {
    @Test fun satelliteChoiceDefaultsOffAndPersistsBothStates() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.createDeviceProtectedStorageContext()
        val preferences = context.getSharedPreferences("astra_settings", Context.MODE_PRIVATE)
        val key = "sky_appearance_show_satellites"
        val hadValue = preferences.contains(key)
        val oldValue = preferences.getBoolean(key, false)
        try {
            preferences.edit().remove(key).commit()
            assertFalse(SkyAppearancePreferences.load(context).showSatellites)

            SkyAppearancePreferences.save(context, SkyAppearancePreferences.load(context).copy(showSatellites = true))
            assertTrue(SkyAppearancePreferences.load(context).showSatellites)

            SkyAppearancePreferences.save(context, SkyAppearancePreferences.load(context).copy(showSatellites = false))
            assertFalse(SkyAppearancePreferences.load(context).showSatellites)
        } finally {
            val editor = preferences.edit()
            if (hadValue) editor.putBoolean(key, oldValue) else editor.remove(key)
            editor.commit()
        }
    }
}
