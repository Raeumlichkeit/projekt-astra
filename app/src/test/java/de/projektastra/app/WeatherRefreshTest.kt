package de.projektastra.app

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class WeatherRefreshTest {
    private val berlin = WeatherTarget(GeoPoint(52.52, 13.4, 0.0), false)
    private val other = WeatherTarget(GeoPoint(48.14, 11.58, 0.0), false)
    private val light = LightPollutionEstimate(50, 5, 20.0)
    private val snapshot = WeatherSnapshot(10.0, 20, 5.0, 10000.0, "16.09. 20:00", emptyList(),
        Instant.parse("2026-09-16T18:00:00Z"), Instant.parse("2026-09-16T18:01:00Z"))
    private fun populated(): WeatherRefreshSession = WeatherRefreshSession().apply {
        val ticket = begin(berlin)
        light(ticket, this@WeatherRefreshTest.light)
        weather(ticket, Result.success(snapshot))
    }

    @Test fun pullRefreshKeepsWholeSnapshotAndLightUntilComplete() {
        val session = populated()
        val displayed = session.state.displayed
        val ticket = session.begin(berlin)
        assertTrue(session.state.refreshing)
        assertSame(displayed, session.state.displayed)
        session.light(ticket, light.copy(index = 80))
        assertSame(displayed, session.state.displayed)
        session.weather(ticket, Result.success(snapshot.copy(temperature = 15.0)))
        assertFalse(session.state.refreshing)
        assertEquals(15.0, session.state.displayed!!.weather.temperature, 0.0)
        assertEquals(80, session.state.displayed!!.light!!.index)
    }

    @Test fun offlineOrFailedRefreshKeepsOldValuesAndShowsError() {
        val session = populated()
        val displayed = session.state.displayed
        val ticket = session.begin(berlin)
        session.weather(ticket, Result.failure(IllegalStateException("offline")))
        session.light(ticket, null)
        assertSame(displayed, session.state.displayed)
        assertTrue(session.state.error)
        assertTrue(session.state.lightError)
        assertFalse(session.state.refreshing)
    }

    @Test fun firstFailureHasNoInventedWeatherAndRetryRecovers() {
        val session = WeatherRefreshSession()
        session.weather(session.begin(berlin), Result.failure(IllegalStateException()))
        assertNull(session.state.displayed)
        assertTrue(session.state.error)
        session.weather(session.begin(berlin), Result.success(snapshot))
        assertNotNull(session.state.displayed)
        assertFalse(session.state.error)
    }

    @Test fun newLocationCannotRelabelOldWeatherOrReuseOldLight() {
        val session = populated()
        val ticket = session.begin(other)
        assertEquals(berlin, session.state.displayed!!.target)
        session.light(ticket, null)
        session.weather(ticket, Result.success(snapshot))
        assertEquals(other, session.state.displayed!!.target)
        assertNull(session.state.displayed!!.light)
    }

    @Test fun failedLocationChangeKeepsOldLocationAndScoreInputs() {
        val session = populated()
        val displayed = session.state.displayed
        val ticket = session.begin(other)
        session.light(ticket, light.copy(index = 80))
        session.weather(ticket, Result.failure(IllegalStateException()))
        assertSame(displayed, session.state.displayed)
        assertEquals(berlin, session.state.displayed!!.target)
    }

    @Test fun locationResponsesOutOfOrderCannotOverwriteLatestRequest() {
        val session = populated()
        val oldTicket = session.begin(berlin)
        val ticket = session.begin(other)
        session.weather(ticket, Result.success(snapshot.copy(temperature = 15.0)))
        session.light(ticket, light.copy(index = 80))
        session.weather(oldTicket, Result.success(snapshot))
        session.light(oldTicket, light)
        assertEquals(other, session.state.displayed!!.target)
        assertEquals(15.0, session.state.displayed!!.weather.temperature, 0.0)
        assertEquals(80, session.state.displayed!!.light!!.index)
    }

    @Test fun backgroundCancelsPendingResultsAndResumeRetainsDisplay() {
        val session = populated()
        val displayed = session.state.displayed
        val oldTicket = session.begin(other)
        session.stop()
        session.weather(oldTicket, Result.success(snapshot.copy(temperature = 30.0)))
        session.light(oldTicket, light.copy(index = 90))
        assertSame(displayed, session.state.displayed)
        assertFalse(session.state.refreshing)
        val ticket = session.begin(berlin)
        assertSame(displayed, session.state.displayed)
        assertFalse(session.accepts(oldTicket))
        assertTrue(session.accepts(ticket))
    }

    @Test fun lateLightFailureDoesNotRemoveExistingSameLocationLight() {
        val session = populated()
        val ticket = session.begin(berlin)
        session.weather(ticket, Result.success(snapshot))
        session.light(ticket, null)
        assertEquals(light, session.state.displayed!!.light)
        assertTrue(session.state.lightError)
    }

    @Test fun demoLocationIsNotSilentlyRelabeledAsLiveLocation() {
        val session = WeatherRefreshSession()
        val demo = berlin.copy(demo = true)
        session.weather(session.begin(demo), Result.success(snapshot))
        session.begin(berlin)
        assertTrue(session.state.displayed!!.target.demo)
    }

    @Test fun parserKeepsEveryHourAcrossMidnightAndUsesFixedInstants() {
        val start = Instant.parse("2026-09-16T21:00:00Z")
        val parsed = WeatherParser.parse(fixture(start), start.plusSeconds(120))
        assertEquals(listOf("23:00", "00:00", "01:00", "02:00", "03:00", "04:00", "05:00"), parsed.forecast.take(7).map { it.time })
        assertEquals(start.plusSeconds(3600), parsed.forecast[1].instant)
        assertEquals(24, parsed.forecast.size)
        assertEquals(start, parsed.observedAt)
        assertEquals(start.plusSeconds(120), parsed.fetchedAt)
    }

    @Test fun repeatedAutumnHourHasTwoDifferentActualInstants() {
        val start = Instant.parse("2026-10-25T00:00:00Z")
        val parsed = WeatherParser.parse(fixture(start), start)
        assertEquals("02:00", parsed.forecast[0].time)
        assertEquals("02:00", parsed.forecast[1].time)
        assertEquals(3600L, parsed.forecast[1].instant.epochSecond - parsed.forecast[0].instant.epochSecond)
    }

    @Test fun partialOrInvalidForecastCannotReplacePreviousSnapshot() {
        val start = Instant.parse("2026-09-16T18:00:00Z")
        val invalid = listOf<(JSONObject) -> Unit>(
            { it.getJSONObject("hourly").getJSONArray("cloud_cover").put(3, JSONObject.NULL) },
            { it.getJSONObject("hourly").getJSONArray("visibility").remove(0) },
            { it.getJSONObject("hourly").getJSONArray("time").put(3, start.epochSecond) },
            { it.getJSONObject("hourly").getJSONArray("precipitation_probability").put(0, 130) },
            { it.getJSONObject("current").put("time", start.minusSeconds(3600).epochSecond) }
        )
        invalid.forEach { mutate ->
            val json = fixture(start).also(mutate)
            assertTrue(runCatching { WeatherParser.parse(json, start) }.isFailure)
        }
    }

    private fun fixture(start: Instant): JSONObject = JSONObject().apply {
        put("timezone", "Europe/Berlin")
        put("current", JSONObject().put("time", start.epochSecond).put("temperature_2m", 10.0)
            .put("cloud_cover", 20).put("wind_speed_10m", 5.0))
        put("hourly", JSONObject().apply {
            put("time", JSONArray((0..47).map { start.plusSeconds(it * 3600L).epochSecond }))
            put("cloud_cover", JSONArray(List(48) { 20 }))
            put("precipitation_probability", JSONArray(List(48) { 5 }))
            put("wind_speed_10m", JSONArray(List(48) { 5.0 }))
            put("visibility", JSONArray(List(48) { 10000.0 }))
        })
    }
}
