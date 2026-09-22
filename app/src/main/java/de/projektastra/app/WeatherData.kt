package de.projektastra.app

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread

internal data class HourlyForecast(
    val time: String,
    val hoursFromNow: Int,
    val cloudCover: Int,
    val rainProbability: Int,
    val windSpeed: Double,
    val visibility: Double,
    val instant: Instant = Instant.EPOCH
)

internal data class WeatherSnapshot(
    val temperature: Double,
    val cloudCover: Int,
    val windSpeed: Double,
    val visibility: Double,
    val updatedAt: String,
    val forecast: List<HourlyForecast>,
    val observedAt: Instant = Instant.EPOCH,
    val fetchedAt: Instant = Instant.EPOCH,
    val relativeHumidity: Double = 70.0
)

internal data class WeatherTarget(val point: GeoPoint, val demo: Boolean)
internal data class DisplayedWeather(
    val target: WeatherTarget,
    val weather: WeatherSnapshot,
    val light: LightPollutionEstimate?
)
internal data class WeatherUiState(
    val displayed: DisplayedWeather? = null,
    val refreshing: Boolean = false,
    val lightRefreshing: Boolean = false,
    val error: Boolean = false,
    val lightError: Boolean = false
)

/** Session memory only. All mutations and callbacks run on the main thread. */
internal class WeatherRefreshSession {
    var state by mutableStateOf(WeatherUiState())
        private set
    private var generation = 0L
    private var active = false
    private var target: WeatherTarget? = null
    private var pendingLight: LightPollutionEstimate? = null
    private var weatherCommitted = false

    fun begin(next: WeatherTarget): Long {
        generation++
        active = true
        target = next
        pendingLight = null
        weatherCommitted = false
        state = state.copy(refreshing = true, lightRefreshing = true, error = false, lightError = false)
        return generation
    }

    fun accepts(ticket: Long): Boolean = active && ticket == generation

    fun weather(ticket: Long, result: Result<WeatherSnapshot>) {
        if (!accepts(ticket) || !state.refreshing) return
        val snapshot = result.getOrNull()
        weatherCommitted = snapshot != null
        state = state.copy(
            refreshing = false,
            error = snapshot == null,
            displayed = if (snapshot == null) state.displayed else DisplayedWeather(
                checkNotNull(target), snapshot,
                pendingLight ?: state.displayed?.takeIf { it.target == target }?.light
            )
        )
    }

    fun light(ticket: Long, estimate: LightPollutionEstimate?) {
        if (!accepts(ticket) || !state.lightRefreshing) return
        pendingLight = estimate
        state = state.copy(
            lightRefreshing = false,
            lightError = estimate == null,
            displayed = if (weatherCommitted && estimate != null) state.displayed?.copy(light = estimate)
                else state.displayed
        )
    }

    fun stop() {
        active = false
        generation++
        state = state.copy(refreshing = false, lightRefreshing = false)
    }
}

internal object WeatherParser {
    /** Epoch seconds avoid ambiguous local times at midnight and DST transitions. */
    fun parse(json: JSONObject, fetchedAt: Instant): WeatherSnapshot {
        val zone = ZoneId.of(json.getString("timezone"))
        val current = json.getJSONObject("current")
        val observedAt = Instant.ofEpochSecond(current.getLong("time"))
        val hourly = json.getJSONObject("hourly")
        val times = hourly.getJSONArray("time")
        val fields = listOf("cloud_cover", "precipitation_probability", "wind_speed_10m", "visibility")
            .associateWith { hourly.getJSONArray(it) }
        require(times.length() >= 24 && fields.values.all { it.length() == times.length() })
        val instants = (0 until times.length()).map { Instant.ofEpochSecond(times.getLong(it)) }
        require(instants.zipWithNext().all { (a, b) -> b.epochSecond - a.epochSecond == 3_600L })
        val index = instants.indexOfLast { it <= observedAt }
        require(index >= 0 && observedAt.epochSecond - instants[index].epochSecond < 3_600L)
        require(index + 24 <= instants.size)
        fun number(value: Double, min: Double, max: Double): Double {
            require(value.isFinite() && value in min..max)
            return value
        }
        fun hourlyNumber(field: String, i: Int, max: Double) = number(fields.getValue(field).getDouble(i), 0.0, max)
        val clock = DateTimeFormatter.ofPattern("HH:mm").withZone(zone)
        val forecast = (index until index + 24).map { i ->
            HourlyForecast(clock.format(instants[i]), i - index,
                hourlyNumber("cloud_cover", i, 100.0).toInt(),
                hourlyNumber("precipitation_probability", i, 100.0).toInt(),
                hourlyNumber("wind_speed_10m", i, 500.0),
                hourlyNumber("visibility", i, 1_000_000.0), instants[i])
        }
        val humidity = if (current.has("relative_humidity_2m")) {
            number(current.getDouble("relative_humidity_2m"), 0.0, 100.0)
        } else {
            70.0
        }
        return WeatherSnapshot(
            number(current.getDouble("temperature_2m"), -150.0, 100.0),
            number(current.getDouble("cloud_cover"), 0.0, 100.0).toInt(),
            number(current.getDouble("wind_speed_10m"), 0.0, 500.0),
            forecast.first().visibility,
            DateTimeFormatter.ofPattern("dd.MM. HH:mm z").withZone(zone).format(observedAt),
            forecast, observedAt, fetchedAt, humidity
        )
    }
}

internal object WeatherRepository {
    fun load(point: GeoPoint, callback: (Result<WeatherSnapshot>) -> Unit) {
        thread(name = "astra-weather") {
            val result = runCatching {
                val approximate = NetworkPolicy.roundedLocation(point)
                val url = "https://api.open-meteo.com/v1/forecast?latitude=${approximate.latitude}" +
                    "&longitude=${approximate.longitude}&current=temperature_2m,relative_humidity_2m,cloud_cover,wind_speed_10m" +
                    "&hourly=visibility,cloud_cover,precipitation_probability,wind_speed_10m" +
                    "&forecast_days=2&timezone=auto&timeformat=unixtime"
                WeatherParser.parse(JSONObject(String(SecureNetwork.get(url).bytes, Charsets.UTF_8)), Instant.now())
            }
            Handler(Looper.getMainLooper()).post { callback(result) }
        }
    }
}
