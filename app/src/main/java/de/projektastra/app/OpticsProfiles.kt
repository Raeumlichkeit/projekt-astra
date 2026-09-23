package de.projektastra.app

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

/**
 * Optik-Profil für Teleskope oder Ferngläser.
 * Berechnete Werte wie Vergrößerung und wahres Gesichtsfeld sind Näherungen.
 */
internal data class OpticsProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isBinocular: Boolean = false,
    val telescopeFocalLengthMm: Double? = null,
    val telescopeApertureMm: Double? = null,
    val eyepieceFocalLengthMm: Double? = null,
    val eyepieceAfovDegrees: Double? = null,
    val customFovDegrees: Double = 1.0
) {
    /**
     * Berechnet die Vergrößerung V = F_teleskop / F_okular.
     */
    val magnification: Double?
        get() {
            if (isBinocular) return null
            val tf = telescopeFocalLengthMm ?: return null
            val ef = eyepieceFocalLengthMm ?: return null
            if (ef <= 0.0) return null
            return tf / ef
        }

    /**
     * Berechnet das wahre Gesichtsfeld (TFOV) in Grad.
     * Für Teleskope näherungsweise TFOV = AFOV / V.
     */
    val effectiveFovDegrees: Double
        get() {
            if (isBinocular) return customFovDegrees
            val mag = magnification
            val afov = eyepieceAfovDegrees
            if (mag != null && afov != null && mag > 0.0 && afov > 0.0) {
                return afov / mag
            }
            return customFovDegrees
        }

    /**
     * Austrittspupille in mm = Öffnung / Vergrößerung.
     */
    val exitPupilMm: Double?
        get() {
            val mag = magnification ?: return null
            val ap = telescopeApertureMm ?: return null
            if (mag <= 0.0) return null
            return ap / mag
        }

    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("isBinocular", isBinocular)
        telescopeFocalLengthMm?.let { put("telescopeFocalLengthMm", it) }
        telescopeApertureMm?.let { put("telescopeApertureMm", it) }
        eyepieceFocalLengthMm?.let { put("eyepieceFocalLengthMm", it) }
        eyepieceAfovDegrees?.let { put("eyepieceAfovDegrees", it) }
        put("customFovDegrees", customFovDegrees)
    }

    companion object {
        fun fromJsonObject(json: JSONObject): OpticsProfile {
            return OpticsProfile(
                id = json.optString("id", UUID.randomUUID().toString()),
                name = json.optString("name", "Unbekanntes Profil"),
                isBinocular = json.optBoolean("isBinocular", false),
                telescopeFocalLengthMm = if (json.has("telescopeFocalLengthMm")) json.getDouble("telescopeFocalLengthMm") else null,
                telescopeApertureMm = if (json.has("telescopeApertureMm")) json.getDouble("telescopeApertureMm") else null,
                eyepieceFocalLengthMm = if (json.has("eyepieceFocalLengthMm")) json.getDouble("eyepieceFocalLengthMm") else null,
                eyepieceAfovDegrees = if (json.has("eyepieceAfovDegrees")) json.getDouble("eyepieceAfovDegrees") else null,
                customFovDegrees = json.optDouble("customFovDegrees", 1.0)
            )
        }
    }
}

/**
 * Einstellungen für Sichtfeld, Drehung und Spiegelung der Sternkarte.
 */
internal data class OpticsSettings(
    val fovCircleEnabled: Boolean = false,
    val currentFovDegrees: Double = 1.0,
    val activeProfileId: String? = null,
    val mirrored: Boolean = false, // Horizontale Spiegelung (z. B. Zenitspiegel)
    val rotationDegrees: Float = 0f, // 0°, 90°, 180° (Newton-Invertierung), 270°
    val rotateLabels: Boolean = false, // false = Beschriftungen aufrecht halten (Standard)
    val telradMode: Boolean = false // Telrad 0.5°, 2.0°, 4.0° Kreise
) {
    val isCustomized: Boolean
        get() = mirrored || (rotationDegrees % 360f != 0f)

    /**
     * Transformiert einen Punkt im Canvas-Koordinatensystem anhand von Spiegelung und Drehung.
     */
    fun transformScreenPoint(point: Offset, center: Offset): Offset {
        var x = point.x - center.x
        var y = point.y - center.y

        if (mirrored) {
            x = -x
        }

        if (rotationDegrees % 360f != 0f) {
            val rad = Math.toRadians(rotationDegrees.toDouble())
            val cosR = cos(rad)
            val sinR = sin(rad)
            val rx = x * cosR - y * sinR
            val ry = x * sinR + y * cosR
            x = rx.toFloat()
            y = ry.toFloat()
        }

        return Offset(center.x + x, center.y + y)
    }

    /**
     * Inverse Transformation: Wandelt einen Touch-Punkt (Bildschirmkoordinate) zurück in das
     * unverzerrte Projektions-Koordinatensystem, sodass die getroffene Objektauswahl millimetergenau übereinstimmt.
     */
    fun inverseTransformScreenPoint(tap: Offset, center: Offset): Offset {
        var x = tap.x - center.x
        var y = tap.y - center.y

        if (rotationDegrees % 360f != 0f) {
            val rad = Math.toRadians(-rotationDegrees.toDouble())
            val cosR = cos(rad)
            val sinR = sin(rad)
            val rx = x * cosR - y * sinR
            val ry = x * sinR + y * cosR
            x = rx.toFloat()
            y = ry.toFloat()
        }

        if (mirrored) {
            x = -x
        }

        return Offset(center.x + x, center.y + y)
    }

    /**
     * Passt Wischgesten (Pan Delta) so an, dass sich die Karte unter dem Finger immer natürlich bewegt,
     * selbst wenn sie um 180° gedreht oder gespiegelt ist.
     */
    fun transformPanDelta(pan: Offset): Offset {
        return inverseTransformScreenPoint(pan, Offset.Zero)
    }

    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("fovCircleEnabled", fovCircleEnabled)
        put("currentFovDegrees", currentFovDegrees)
        activeProfileId?.let { put("activeProfileId", it) }
        put("mirrored", mirrored)
        put("rotationDegrees", rotationDegrees.toDouble())
        put("rotateLabels", rotateLabels)
        put("telradMode", telradMode)
    }

    companion object {
        fun fromJsonObject(json: JSONObject): OpticsSettings {
            return OpticsSettings(
                fovCircleEnabled = json.optBoolean("fovCircleEnabled", false),
                currentFovDegrees = json.optDouble("currentFovDegrees", 1.0).coerceIn(0.05, 30.0),
                activeProfileId = if (json.has("activeProfileId") && !json.isNull("activeProfileId")) json.getString("activeProfileId") else null,
                mirrored = json.optBoolean("mirrored", false),
                rotationDegrees = json.optDouble("rotationDegrees", 0.0).toFloat(),
                rotateLabels = json.optBoolean("rotateLabels", false),
                telradMode = json.optBoolean("telradMode", false)
            )
        }
    }
}

/**
 * Lokale Verwaltung und Persistenz von Optik-Einstellungen und -Profilen.
 */
internal object OpticsStore {
    private const val PREFS_NAME = "astra_optics"
    private const val KEY_SETTINGS = "settings_json"
    private const val KEY_PROFILES = "profiles_json"

    val DEFAULT_PROFILES = listOf(
        OpticsProfile(
            id = "default_binocular_10x50",
            name = "10×50 Fernglas",
            isBinocular = true,
            customFovDegrees = 6.5
        ),
        OpticsProfile(
            id = "default_binocular_8x42",
            name = "8×42 Fernglas",
            isBinocular = true,
            customFovDegrees = 7.5
        ),
        OpticsProfile(
            id = "default_telescope_dobson_25mm",
            name = "8\" Dobson · 25 mm Plössl",
            isBinocular = false,
            telescopeFocalLengthMm = 1200.0,
            telescopeApertureMm = 200.0,
            eyepieceFocalLengthMm = 25.0,
            eyepieceAfovDegrees = 52.0,
            customFovDegrees = 1.08
        ),
        OpticsProfile(
            id = "default_telescope_dobson_10mm",
            name = "8\" Dobson · 10 mm Plössl",
            isBinocular = false,
            telescopeFocalLengthMm = 1200.0,
            telescopeApertureMm = 200.0,
            eyepieceFocalLengthMm = 10.0,
            eyepieceAfovDegrees = 52.0,
            customFovDegrees = 0.43
        )
    )

    fun loadSettings(context: Context): OpticsSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_SETTINGS, null) ?: return OpticsSettings()
        return runCatching {
            OpticsSettings.fromJsonObject(JSONObject(jsonStr))
        }.getOrDefault(OpticsSettings())
    }

    fun saveSettings(context: Context, settings: OpticsSettings) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(KEY_SETTINGS, settings.toJsonObject().toString())
        }
    }

    fun loadProfiles(context: Context): List<OpticsProfile> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_PROFILES, null) ?: return DEFAULT_PROFILES
        return runCatching {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<OpticsProfile>()
            for (i in 0 until array.length()) {
                list.add(OpticsProfile.fromJsonObject(array.getJSONObject(i)))
            }
            if (list.isEmpty()) DEFAULT_PROFILES else list
        }.getOrDefault(DEFAULT_PROFILES)
    }

    @Synchronized
    fun saveProfile(context: Context, profile: OpticsProfile): List<OpticsProfile> {
        val current = loadProfiles(context).filterNot { it.id == profile.id }.toMutableList()
        current.add(profile)
        saveProfilesList(context, current)
        return current
    }

    @Synchronized
    fun deleteProfile(context: Context, profileId: String): List<OpticsProfile> {
        val current = loadProfiles(context).filterNot { it.id == profileId }
        val toSave = if (current.isEmpty()) DEFAULT_PROFILES else current
        saveProfilesList(context, toSave)
        return toSave
    }

    private fun saveProfilesList(context: Context, profiles: List<OpticsProfile>) {
        val array = JSONArray()
        profiles.forEach { array.put(it.toJsonObject()) }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(KEY_PROFILES, array.toString())
        }
    }
}
