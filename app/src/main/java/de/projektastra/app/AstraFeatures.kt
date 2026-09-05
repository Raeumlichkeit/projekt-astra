package de.projektastra.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.get
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

internal data class EquatorialBoundaryPoint(val raHours: Double, val decDegrees: Double)

internal data class IauConstellationBoundary(
    val abbreviation: String,
    val points: List<EquatorialBoundaryPoint>
)

internal object IauBoundaryCatalog {
    fun load(context: Context): List<IauConstellationBoundary> = runCatching {
        parse(context.assets.open("iau_constellation_boundaries_b1875.dat").bufferedReader().use { it.readText() })
    }.getOrDefault(emptyList())

    internal fun parse(text: String): List<IauConstellationBoundary> {
        val boundaries = mutableListOf<IauConstellationBoundary>()
        var currentCode = ""
        var currentPoints = mutableListOf<EquatorialBoundaryPoint>()
        text.lineSequence().forEach { line ->
            val columns = line.trim().split(Regex("\\s+"))
            if (columns.size < 3) return@forEach
            val ra = columns[0].toDoubleOrNull() ?: return@forEach
            val dec = columns[1].toDoubleOrNull() ?: return@forEach
            val rawCode = columns[2].uppercase(Locale.US)
            val code = if (rawCode.startsWith("SER")) "SER" else rawCode
            if (columns.size == 3) {
                if (currentPoints.size > 2) {
                    boundaries += IauConstellationBoundary(currentCode, currentPoints)
                }
                currentCode = code
                currentPoints = mutableListOf()
            }
            currentPoints += precessB1875ToJ2000(ra, dec)
        }
        if (currentPoints.size > 2) boundaries += IauConstellationBoundary(currentCode, currentPoints)
        return boundaries
    }

    /** IAU 1976 precession, from B1875 catalogue coordinates to J2000. */
    internal fun precessB1875ToJ2000(raHours: Double, decDegrees: Double): EquatorialBoundaryPoint {
        val epochCenturies = -1.25
        val intervalCenturies = 1.25
        val zetaArcsec =
            (2306.2181 + 1.39656 * epochCenturies - 0.000139 * epochCenturies * epochCenturies) * intervalCenturies +
                (0.30188 - 0.000344 * epochCenturies) * intervalCenturies.pow(2) +
                0.017998 * intervalCenturies.pow(3)
        val zArcsec =
            (2306.2181 + 1.39656 * epochCenturies - 0.000139 * epochCenturies * epochCenturies) * intervalCenturies +
                (1.09468 + 0.000066 * epochCenturies) * intervalCenturies.pow(2) +
                0.018203 * intervalCenturies.pow(3)
        val thetaArcsec =
            (2004.3109 - 0.85330 * epochCenturies - 0.000217 * epochCenturies * epochCenturies) * intervalCenturies -
                (0.42665 + 0.000217 * epochCenturies) * intervalCenturies.pow(2) -
                0.041833 * intervalCenturies.pow(3)
        val zeta = Math.toRadians(zetaArcsec / 3600.0)
        val z = Math.toRadians(zArcsec / 3600.0)
        val theta = Math.toRadians(thetaArcsec / 3600.0)
        val ra = Math.toRadians(raHours * 15.0)
        val dec = Math.toRadians(decDegrees)
        val a = cos(dec) * sin(ra + zeta)
        val b = cos(theta) * cos(dec) * cos(ra + zeta) - sin(theta) * sin(dec)
        val c = sin(theta) * cos(dec) * cos(ra + zeta) + cos(theta) * sin(dec)
        val raJ2000 = ((Math.toDegrees(atan2(a, b) + z) % 360.0) + 360.0) % 360.0
        return EquatorialBoundaryPoint(raJ2000 / 15.0, Math.toDegrees(asin(c)))
    }
}

internal data class LightPollutionEstimate(
    val index: Int,
    val bortleClass: Int,
    val skyBrightnessMag: Double,
    val sourceYear: Int = 2016
) {
    val qualityLabel: String
        get() = when (bortleClass) {
            1, 2 -> "sehr dunkler Himmel"
            3, 4 -> "ländlicher Himmel"
            5, 6 -> "Vorstadt-Himmel"
            else -> "stark aufgehellter Himmel"
        }
}

internal sealed interface LightPollutionState {
    data object Loading : LightPollutionState
    data class Ready(val estimate: LightPollutionEstimate) : LightPollutionState
    data object Unavailable : LightPollutionState
}

internal object LightPollutionRepository {
    private var cachedPoint: GeoPoint? = null
    private var cachedEstimate: LightPollutionEstimate? = null

    fun clear() { cachedPoint = null; cachedEstimate = null }

    fun load(point: GeoPoint, callback: (LightPollutionState) -> Unit) {
        if (!SecureNetwork.available) {
            callback(LightPollutionState.Unavailable)
            return
        }
        val previousPoint = cachedPoint
        val previous = cachedEstimate
        if (previousPoint != null && previous != null &&
            kotlin.math.abs(previousPoint.latitude - point.latitude) < 0.01 &&
            kotlin.math.abs(previousPoint.longitude - point.longitude) < 0.01
        ) {
            callback(LightPollutionState.Ready(previous))
            return
        }
        thread(name = "astra-light-pollution") {
            val result = runCatching {
                val tile = tileCoordinate(NetworkPolicy.roundedLocation(point), 8)
                val url = URL(
                    "https://gibs.earthdata.nasa.gov/wmts/epsg3857/best/VIIRS_Night_Lights/" +
                        "default/2016-01-01/GoogleMapsCompatible_Level8/8/${tile.tileY}/${tile.tileX}.png"
                )
                run {
                    val bytes = SecureNetwork.get(url.toString(), 1_048_576).bytes
                    val dimensions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, dimensions)
                    check(dimensions.outWidth in 1..512 && dimensions.outHeight in 1..512)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: error("Ungültige Kachel")
                    try {
                        var sum = 0.0
                        var count = 0
                        for (dy in -4..4) for (dx in -4..4) {
                            val color = bitmap[
                                (tile.pixelX + dx).coerceIn(0, bitmap.width - 1),
                                (tile.pixelY + dy).coerceIn(0, bitmap.height - 1)
                            ]
                            sum += 0.2126 * android.graphics.Color.red(color) +
                                0.7152 * android.graphics.Color.green(color) +
                                0.0722 * android.graphics.Color.blue(color)
                            count++
                        }
                        estimateFromLuminance(sum / count)
                    } finally {
                        bitmap.recycle()
                    }
                }
            }
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                callback(result.fold(
                    onSuccess = {
                        if (SecureNetwork.available) {
                            cachedPoint = point
                            cachedEstimate = it
                            LightPollutionState.Ready(it)
                        } else LightPollutionState.Unavailable
                    },
                    onFailure = { LightPollutionState.Unavailable }
                ))
            }
        }
    }

    internal fun estimateFromLuminance(luminance: Double): LightPollutionEstimate {
        val index = ((luminance / 255.0).coerceIn(0.0, 1.0).pow(0.62) * 100.0).toInt()
        val bortle = when {
            index < 3 -> 1
            index < 7 -> 2
            index < 13 -> 3
            index < 22 -> 4
            index < 35 -> 5
            index < 50 -> 6
            index < 68 -> 7
            index < 85 -> 8
            else -> 9
        }
        val skyBrightness = 22.0 - (index / 100.0) * 4.0
        return LightPollutionEstimate(index, bortle, skyBrightness)
    }

    private data class TileCoordinate(val tileX: Int, val tileY: Int, val pixelX: Int, val pixelY: Int)

    private fun tileCoordinate(point: GeoPoint, zoom: Int): TileCoordinate {
        val tileCount = 1 shl zoom
        val latitude = point.latitude.coerceIn(-85.0511, 85.0511)
        val latitudeRadians = Math.toRadians(latitude)
        val worldX = (point.longitude + 180.0) / 360.0 * tileCount * 256.0
        val worldY = (1.0 - ln(tan(latitudeRadians) + 1.0 / cos(latitudeRadians)) / PI) / 2.0 *
            tileCount * 256.0
        return TileCoordinate(
            floor(worldX / 256.0).toInt().coerceIn(0, tileCount - 1),
            floor(worldY / 256.0).toInt().coerceIn(0, tileCount - 1),
            floor(worldX % 256.0).toInt().coerceIn(0, 255),
            floor(worldY % 256.0).toInt().coerceIn(0, 255)
        )
    }
}

internal data class MeteorDefinition(
    val year: Int,
    val name: String,
    val code: String,
    val month: Int,
    val day: Int,
    val radiantRaHours: Double,
    val radiantDecDegrees: Double,
    val zhr: Int
)

internal data class MeteorCalendarSnapshot(
    val showers: List<MeteorDefinition>,
    val updated: String,
    val online: Boolean
)

internal object MeteorCalendarRepository {
    private const val CACHE_NAME = "imo_calendar_cache"
    private val monthNumbers = mapOf(
        "Jan" to 1, "Feb" to 2, "Mar" to 3, "Apr" to 4, "May" to 5, "Jun" to 6,
        "Jul" to 7, "Aug" to 8, "Sep" to 9, "Oct" to 10, "Nov" to 11, "Dec" to 12
    )
    private val germanNames = mapOf(
        "QUA" to "Quadrantiden",
        "LYR" to "Lyriden",
        "ETA" to "Eta-Aquariiden",
        "SDA" to "Südliche Delta-Aquariiden",
        "PER" to "Perseiden",
        "SPE" to "September-Epsilon-Perseiden",
        "DRA" to "Draconiden",
        "ORI" to "Orioniden",
        "LEO" to "Leoniden",
        "GEM" to "Geminiden",
        "URS" to "Ursiden"
    )

    fun bundled(context: Context): MeteorCalendarSnapshot = parse(
        context.assets.open("imo_meteor_showers.json").bufferedReader().use { it.readText() },
        online = false
    )

    fun refresh(context: Context, callback: (MeteorCalendarSnapshot) -> Unit) {
        val preferences = context.getSharedPreferences("astra_imo", Context.MODE_PRIVATE)
        val cachedJson = preferences.getString(CACHE_NAME, null)
        val lastFetch = preferences.getLong("last_fetch", 0L)
        val cached = cachedJson?.let { runCatching { parse(it, true) }.getOrNull() }
        val fallback = cached ?: bundled(context)
        callback(fallback)
        if (!SecureNetwork.available || (cached != null && System.currentTimeMillis() - lastFetch < TimeUnit.DAYS.toMillis(7))) {
            return
        }
        thread(name = "astra-imo-calendar") {
            val snapshot = runCatching {
                val currentYear = LocalDate.now(ZoneOffset.UTC).year
                var downloadedYears = 0
                val showers = (currentYear..currentYear + 1).flatMap { year ->
                    runCatching { parseOfficialYear(year, downloadOfficialCalendar(context, year)) }
                        .onSuccess { downloadedYears++ }
                        .getOrElse { fallback.showers.filter { it.year == year } }
                }
                check(SecureNetwork.available && showers.isNotEmpty() && downloadedYears > 0)
                MeteorCalendarSnapshot(
                    showers = showers,
                    updated = LocalDate.now(ZoneOffset.UTC).toString(),
                    online = true
                ).also {
                    preferences.edit {
                        putString(CACHE_NAME, serialize(it))
                        putLong("last_fetch", System.currentTimeMillis())
                    }
                }
            }.getOrElse { fallback }
            android.os.Handler(android.os.Looper.getMainLooper()).post { callback(snapshot) }
        }
    }

    private fun downloadOfficialCalendar(context: Context, year: Int): String {
        val bytes = SecureNetwork.get("https://www.imo.net/files/meteor-shower/cal$year.pdf", PdfLimits.BYTES).bytes
        return IsolatedPdfParser.extract(context.applicationContext, bytes)
    }

    internal fun parseOfficialYear(year: Int, pdfText: String): List<MeteorDefinition> {
        require(year in 2020..2099 && pdfText.length <= PdfLimits.TEXT_CHARS)
        val normalized = pdfText
            .replace('−', '-')
            .replace('–', '-')
            .replace("η", "Eta")
            .replace("δ", "Delta")
            .replace("ε", "Epsilon")
        val codePattern = Regex("""\(\d{3}\s+([A-Z0-9]{3})\)""")
        val datePattern = Regex("""\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\s+(\d{2})\b""")
        val coordinatePattern = Regex("""(\d{1,3})\s*[°◦]\s*([+-]\s*\d{1,2})\s*[°◦]""")
        val zhrPattern = Regex("""(?:\s|^)(\d+)\+?\s*$""")
        val byCode = linkedMapOf<String, MeteorDefinition>()
        normalized.lineSequence().forEach { line ->
            val code = codePattern.find(line)?.groupValues?.get(1) ?: return@forEach
            val name = germanNames[code] ?: return@forEach
            val dates = datePattern.findAll(line).toList()
            val coordinates = coordinatePattern.find(line) ?: return@forEach
            val zhr = zhrPattern.find(line)?.groupValues?.get(1)?.toIntOrNull() ?: return@forEach
            if (dates.size < 3) return@forEach
            val peak = dates[2].groupValues
            byCode[code] = MeteorDefinition(
                year = year,
                name = name,
                code = code,
                month = monthNumbers.getValue(peak[1]),
                day = peak[2].toInt(),
                radiantRaHours = coordinates.groupValues[1].toDouble() / 15.0,
                radiantDecDegrees = coordinates.groupValues[2].replace(" ", "").toDouble(),
                zhr = zhr
            ).also(::validate)
        }
        val missing = germanNames.keys - byCode.keys
        check(missing.isEmpty()) { "IMO $year nicht vollständig: $missing" }
        return byCode.values.sortedWith(compareBy({ it.month }, { it.day }))
    }

    private fun serialize(snapshot: MeteorCalendarSnapshot): String = JSONObject().apply {
        put("source", "International Meteor Organization annual Meteor Shower Calendar")
        put("updated", snapshot.updated)
        put("years", JSONArray().apply {
            snapshot.showers.groupBy { it.year }.toSortedMap().forEach { (year, showers) ->
                put(JSONObject().apply {
                    put("year", year)
                    put("showers", JSONArray().apply {
                        showers.forEach { shower ->
                            put(JSONObject().apply {
                                put("name", shower.name)
                                put("code", shower.code)
                                put("month", shower.month)
                                put("day", shower.day)
                                put("raDegrees", shower.radiantRaHours * 15.0)
                                put("decDegrees", shower.radiantDecDegrees)
                                put("zhr", shower.zhr)
                            })
                        }
                    })
                })
            }
        })
    }.toString()

    internal fun parse(jsonText: String, online: Boolean): MeteorCalendarSnapshot {
        require(jsonText.length <= PdfLimits.TEXT_CHARS)
        val root = JSONObject(jsonText)
        val showers = buildList {
            val years = root.getJSONArray("years")
            require(years.length() in 1..10)
            for (yearIndex in 0 until years.length()) {
                val yearObject = years.getJSONObject(yearIndex)
                val year = yearObject.getInt("year")
                val entries = yearObject.getJSONArray("showers")
                require(entries.length() in 1..32)
                for (entryIndex in 0 until entries.length()) {
                    val entry = entries.getJSONObject(entryIndex)
                    add(MeteorDefinition(
                        year = year,
                        name = entry.getString("name"),
                        code = entry.getString("code"),
                        month = entry.getInt("month"),
                        day = entry.getInt("day"),
                        radiantRaHours = entry.getDouble("raDegrees") / 15.0,
                        radiantDecDegrees = entry.getDouble("decDegrees"),
                        zhr = entry.optInt("zhr", 0)
                    ).also(::validate))
                }
            }
        }
        return MeteorCalendarSnapshot(showers, root.optString("updated", "unbekannt"), online)
    }

    private fun validate(shower: MeteorDefinition) {
        require(shower.year in 2020..2099 && shower.code in germanNames && shower.name.length in 1..80)
        LocalDate.of(shower.year, shower.month, shower.day)
        require(shower.radiantRaHours.isFinite() && shower.radiantRaHours in 0.0..24.0)
        require(shower.radiantDecDegrees.isFinite() && shower.radiantDecDegrees in -90.0..90.0)
        require(shower.zhr in 0..100_000)
    }
}

internal data class SavedSkyEvent(
    val key: String,
    val title: String,
    val kind: String,
    val instantEpochSeconds: Long
)

internal object ObservationStore {
    private const val PREFS = "astra_observations"

    fun favoriteObjectIds(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet("favorite_objects", emptySet()).orEmpty().toSet()

    fun setObjectFavorite(context: Context, id: String, favorite: Boolean): Set<String> {
        val next = favoriteObjectIds(context).toMutableSet().apply {
            if (favorite) add(id) else remove(id)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putStringSet("favorite_objects", next)
        }
        return next
    }

    fun savedEvents(context: Context): List<SavedSkyEvent> {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("saved_events", "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                SavedSkyEvent(
                    key = item.getString("key"),
                    title = item.getString("title"),
                    kind = item.getString("kind"),
                    instantEpochSeconds = item.getLong("instant")
                )
            }
        }.getOrDefault(emptyList())
    }

    fun setEventSaved(context: Context, event: SavedSkyEvent, saved: Boolean): List<SavedSkyEvent> {
        val next = savedEvents(context).filterNot { it.key == event.key }.toMutableList()
        if (saved) next += event
        val array = JSONArray().apply {
            next.sortedBy { it.instantEpochSeconds }.forEach { item ->
                put(JSONObject().apply {
                    put("key", item.key)
                    put("title", item.title)
                    put("kind", item.kind)
                    put("instant", item.instantEpochSeconds)
                })
            }
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString("saved_events", array.toString())
        }
        return next
    }

    fun reminderHours(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("reminder_hours", 24)

    fun setReminderHours(context: Context, hours: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putInt("reminder_hours", hours) }
    }
}

internal object EventReminderScheduler {
    fun schedule(context: Context, event: SavedSkyEvent, leadHours: Int) {
        val reminderAt = Instant.ofEpochSecond(event.instantEpochSeconds).minusSeconds(leadHours * 3600L)
        val delay = reminderAt.toEpochMilli() - System.currentTimeMillis()
        if (delay <= 0L) return
        val request = OneTimeWorkRequestBuilder<EventReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder()
                .putString("title", event.title)
                .putString("kind", event.kind)
                .putLong("instant", event.instantEpochSeconds)
                .build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "astra-event-${event.key}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context, key: String) {
        WorkManager.getInstance(context).cancelUniqueWork("astra-event-$key")
    }

    fun rescheduleAll(context: Context, events: List<SavedSkyEvent>, leadHours: Int) {
        events.forEach { schedule(context, it, leadHours) }
    }
}

class EventReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        val title = inputData.getString("title") ?: return Result.failure()
        val kind = inputData.getString("kind") ?: "Himmelsereignis"
        createChannel()
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            title.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText("$kind steht bald an. Öffne Projekt Astra für die lokale Einschätzung.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(title.hashCode(), notification)
        return Result.success()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Himmelsereignisse",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Erinnerungen an vorgemerkte Finsternisse und Meteorschauer"
        }
        applicationContext.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "astra_sky_events"
    }
}
