package de.projektastra.app

import android.content.Context
import androidx.core.content.edit

/**
 * Persists the user's observation location locally in private preferences (astra_settings)
 * so that the sky map starts directly aligned to the user's sky without requiring repeated prompts.
 *
 * Privacy & Security:
 * - Coordinates are stored purely on-device and never transferred off-device or backed up.
 * - Backups are disabled via data_extraction_rules.xml and allowBackup="false".
 * - Users can opt out and clear stored coordinates at any time (via SkyScreen or PrivacyControls).
 */
internal object LocationStore {
    const val PREFS = "astra_settings"
    const val KEY_REMEMBER = "location_remember_enabled"
    const val KEY_HAS_SAVED = "location_has_saved"
    const val KEY_LATITUDE = "location_latitude"
    const val KEY_LONGITUDE = "location_longitude"
    const val KEY_ALTITUDE = "location_altitude"

    /**
     * Pure validation and parsing function for saved coordinates.
     * Returns null if remembering is disabled, no location is saved, or coordinates are invalid.
     */
    fun parseLocation(
        rememberEnabled: Boolean,
        hasSaved: Boolean,
        latStr: String?,
        lonStr: String?,
        altStr: String?
    ): GeoPoint? {
        if (!rememberEnabled || !hasSaved) return null
        val lat = latStr?.toDoubleOrNull() ?: return null
        val lon = lonStr?.toDoubleOrNull() ?: return null
        val alt = altStr?.toDoubleOrNull() ?: 0.0
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0 || !alt.isFinite()) return null
        return GeoPoint(lat, lon, alt)
    }

    /**
     * Returns whether saving the observation location is enabled by user preference.
     * Defaults to false: permission to use GPS is not consent to persist coordinates.
     */
    fun isRememberEnabled(context: Context): Boolean {
        val prefs = runCatching { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }.getOrNull()
            ?: return false
        return prefs.getBoolean(KEY_REMEMBER, false)
    }

    /**
     * Enables or disables remembering the location.
     * When disabled, any existing saved coordinates are immediately and completely deleted.
     */
    fun setRememberEnabled(context: Context, enabled: Boolean) {
        val prefs = runCatching { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }.getOrNull()
            ?: return
        prefs.edit {
            putBoolean(KEY_REMEMBER, enabled)
            if (!enabled) {
                remove(KEY_HAS_SAVED)
                remove(KEY_LATITUDE)
                remove(KEY_LONGITUDE)
                remove(KEY_ALTITUDE)
            }
        }
    }

    /**
     * Retrieves the saved observation location, or null if none is saved or remembering is disabled.
     */
    fun getSavedLocation(context: Context): GeoPoint? {
        val prefs = runCatching { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }.getOrNull()
            ?: return null
        val remember = prefs.getBoolean(KEY_REMEMBER, false)
        if (!remember) {
            // Older builds stored coordinates even without an explicit preference.
            clearLocation(context)
            return null
        }
        val hasSaved = prefs.getBoolean(KEY_HAS_SAVED, false)
        val lat = prefs.getString(KEY_LATITUDE, null)
        val lon = prefs.getString(KEY_LONGITUDE, null)
        val alt = prefs.getString(KEY_ALTITUDE, null)
        return parseLocation(remember, hasSaved, lat, lon, alt)
    }

    /**
     * Persists the given observation location if remembering is enabled.
     */
    fun saveLocation(context: Context, point: GeoPoint) {
        val prefs = runCatching { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }.getOrNull()
            ?: return
        if (!prefs.getBoolean(KEY_REMEMBER, false)) return
        require(point.latitude in -90.0..90.0 && point.longitude in -180.0..180.0 &&
            point.altitudeMeters.isFinite()) { "Ungültiger Standort" }
        prefs.edit {
            putBoolean(KEY_HAS_SAVED, true)
            putString(KEY_LATITUDE, point.latitude.toString())
            putString(KEY_LONGITUDE, point.longitude.toString())
            putString(KEY_ALTITUDE, point.altitudeMeters.toString())
        }
    }

    /**
     * Clears any saved observation location from private preferences.
     */
    fun clearLocation(context: Context) {
        val prefs = runCatching { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }.getOrNull()
            ?: return
        prefs.edit {
            remove(KEY_HAS_SAVED)
            remove(KEY_LATITUDE)
            remove(KEY_LONGITUDE)
            remove(KEY_ALTITUDE)
        }
    }
}
