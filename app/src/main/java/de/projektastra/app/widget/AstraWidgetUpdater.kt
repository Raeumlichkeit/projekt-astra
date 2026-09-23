package de.projektastra.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import de.projektastra.app.GeoPoint
import de.projektastra.app.LocationStore
import de.projektastra.app.MainActivity
import de.projektastra.app.R
import de.projektastra.app.TonightWindowCalculator
import de.projektastra.app.ephemeris.LunarTerminatorCalculator
import java.time.Instant
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Data contract representing the battery-friendly homescreen widget state.
 */
data class WidgetState(
    val moonPhaseLabel: String,
    val moonIlluminationPercent: Int,
    val darknessWindow: String,
    val weatherScore: Int,
    val lastKnownLocationLabel: String,
    val usesBackgroundGps: Boolean = false,
    val usesRunningBackgroundService: Boolean = false
)

/**
 * Battery-friendly widget updater for Projekt Astra.
 * Strictly enforces ZERO background GPS and ZERO running background services.
 */
object AstraWidgetUpdater {

    private const val PREFS_NAME = "astra_widget_prefs"
    private const val KEY_CACHED_WEATHER_SCORE = "cached_weather_score"

    fun saveWeatherScore(context: Context, score: Int) {
        runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_CACHED_WEATHER_SCORE, score.coerceIn(0, 100))
                .apply()
        }
    }

    fun getCachedWeatherScore(context: Context): Int {
        return runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_CACHED_WEATHER_SCORE, 80)
        }.getOrDefault(80)
    }

    /**
     * Overload for testing and headless calculation without Android Context dependencies.
     */
    internal fun calculateState(
        savedLocation: GeoPoint?,
        time: Instant = Instant.now(),
        weatherScore: Int = 80
    ): WidgetState {
        val termState = LunarTerminatorCalculator.calculateTerminator(time)
        val illumPct = (termState.phaseFraction * 100.0).roundToInt().coerceIn(0, 100)
        val phaseName = when {
            illumPct < 2 -> "Neumond"
            illumPct in 45..55 && termState.colongitude < 180.0 -> "Erstes Viertel"
            illumPct > 98 -> "Vollmond"
            illumPct in 45..55 -> "Letztes Viertel"
            termState.colongitude < 180.0 -> "Zunehmender Mond"
            else -> "Abnehmender Mond"
        }

        val locationLabel = if (savedLocation != null) {
            String.format(
                Locale.GERMAN,
                "%.1f°%s, %.1f°%s",
                abs(savedLocation.latitude), if (savedLocation.latitude < 0) "S" else "N",
                abs(savedLocation.longitude), if (savedLocation.longitude < 0) "W" else "E"
            )
        } else {
            "Berlin (52.5°N)"
        }

        val observer = savedLocation ?: GeoPoint(52.5200, 13.4050, 34.0)
        val calculatedDarkness = runCatching {
            val tonight = TonightWindowCalculator.calculate(observer, time)
            if (tonight.hasAstronomicalDarkness) "Astronomische Nacht: ${tonight.darknessText}"
            else tonight.darknessText
        }.getOrDefault("Astronomische Dunkelheit: nicht verfügbar")

        return WidgetState(
            moonPhaseLabel = phaseName,
            moonIlluminationPercent = illumPct,
            darknessWindow = calculatedDarkness,
            weatherScore = weatherScore.coerceIn(0, 100),
            lastKnownLocationLabel = locationLabel,
            usesBackgroundGps = false,
            usesRunningBackgroundService = false
        )
    }

    fun calculateState(
        context: Context,
        time: Instant = Instant.now(),
        weatherScore: Int = getCachedWeatherScore(context)
    ): WidgetState {
        val savedLocation = runCatching { LocationStore.getSavedLocation(context) }.getOrNull()
        return calculateState(savedLocation, time, weatherScore)
    }

    fun buildRemoteViews(context: Context, state: WidgetState): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_astra_tonight)

        views.setTextViewText(R.id.widget_location, state.lastKnownLocationLabel)
        views.setTextViewText(R.id.widget_moon_phase, state.moonPhaseLabel)
        views.setTextViewText(R.id.widget_moon_illumination, "Beleuchtung: ${state.moonIlluminationPercent}%")
        views.setTextViewText(R.id.widget_weather_score, "Wetter: ${state.weatherScore}/100")
        views.setTextViewText(R.id.widget_darkness_window, state.darknessWindow)

        // PendingIntent to launch MainActivity on tap
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, flags)
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        return views
    }

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
        val component = ComponentName(context, AstraAppWidgetProvider::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(component)
        if (widgetIds.isEmpty()) return

        val state = calculateState(context)
        val views = buildRemoteViews(context, state)
        for (id in widgetIds) {
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
