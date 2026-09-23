package de.projektastra.app.ephemeris

import android.content.Context
import androidx.core.content.edit
import java.time.Duration
import java.time.Instant

internal sealed interface TleFreshnessStatus {
    data object EXCELLENT : TleFreshnessStatus
    data object GOOD : TleFreshnessStatus
    data class STALE(val daysOld: Int) : TleFreshnessStatus
}

internal object SatelliteCatalog {
    private const val PREFS_NAME = "astra_satellites"
    private const val KEY_CUSTOM_TLES = "custom_tle_data"

    fun loadBundled(context: Context): List<TleData> {
        return context.assets.open("satellites_bright.tle").bufferedReader().useLines { lines ->
            TleParser.parseMultiple(lines.toList())
        }
    }

    fun loadActiveSatellites(context: Context): List<TleData> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val custom = prefs.getString(KEY_CUSTOM_TLES, null)
        val customList = custom?.let { runCatching { TleParser.parseMultiple(it.lines()) }.getOrNull() }
        return if (!customList.isNullOrEmpty()) customList else loadBundled(context)
    }

    fun saveCustomTles(context: Context, tleText: String): Boolean {
        val parsed = runCatching { TleParser.parseMultiple(tleText.lines()) }.getOrNull()
        if (parsed.isNullOrEmpty()) return false
        prefs(context).edit { putString(KEY_CUSTOM_TLES, tleText) }
        return true
    }

    fun clearCustomTles(context: Context) {
        prefs(context).edit { remove(KEY_CUSTOM_TLES) }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun checkFreshness(tle: TleData, now: Instant): TleFreshnessStatus {
        val ageDays = Duration.between(tle.epochInstant, now).toDays()
        return when {
            ageDays < 7 -> TleFreshnessStatus.EXCELLENT
            ageDays < 30 -> TleFreshnessStatus.GOOD
            else -> TleFreshnessStatus.STALE(ageDays.toInt())
        }
    }
}
