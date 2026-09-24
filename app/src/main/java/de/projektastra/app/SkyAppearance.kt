package de.projektastra.app

import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import kotlin.math.roundToInt

internal enum class MilkyWayMode { OFF, NATURAL, PHOTO }

/** A sheet uses its own Android window, outside the main canvas' red-light multiplier. */
@Composable
internal fun SkySheetTheme(redLight: Boolean, content: @Composable () -> Unit) {
    val ink = Color(0xFFBE342A)
    val muted = Color(0xFF9A2920)
    val dark = Color(0xFF100000)
    val container = Color(0xFF290400)
    val palette = if (!redLight) MaterialTheme.colorScheme else darkColorScheme(
        primary = ink, onPrimary = dark, primaryContainer = container, onPrimaryContainer = ink,
        inversePrimary = muted, secondary = ink, onSecondary = dark,
        secondaryContainer = container, onSecondaryContainer = ink,
        tertiary = ink, onTertiary = dark, tertiaryContainer = container, onTertiaryContainer = ink,
        background = dark, onBackground = ink, surface = dark, onSurface = ink,
        surfaceVariant = container, onSurfaceVariant = muted, surfaceTint = ink,
        inverseSurface = muted, inverseOnSurface = dark, outline = muted, outlineVariant = container,
        error = ink, onError = dark, errorContainer = container, onErrorContainer = ink,
        scrim = Color.Black, surfaceBright = container, surfaceDim = dark,
        surfaceContainer = dark, surfaceContainerHigh = container, surfaceContainerHighest = container,
        surfaceContainerLow = dark, surfaceContainerLowest = Color.Black
    )
    MaterialTheme(colorScheme = palette, content = content)
}

@Composable
internal fun SkySheetSystemBars(redLight: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, redLight) {
        val window = (view.parent as? DialogWindowProvider)?.window
        if (redLight && window != null) WindowCompat.getInsetsController(window, view)
            .hide(WindowInsetsCompat.Type.systemBars())
        onDispose { }
    }
}

/** Display preferences only: no location, observation history or image data is persisted here. */
internal data class SkyAppearance(
    val mode: MilkyWayMode = MilkyWayMode.NATURAL,
    val intensity: Float = 1f,
    val showInAr: Boolean = false,
    val showGrid: Boolean = false,
    val showEquatorialGrid: Boolean = false,
    val showHorizontalGrid: Boolean = false,
    val showCelestialEquator: Boolean = false,
    val showEcliptic: Boolean = false,
    val showGalacticEquator: Boolean = false,
    val showSatellites: Boolean = false,
    val labelDensity: SkyLabelDensity = SkyLabelDensity.NORMAL,
    val oledBlackMode: Boolean = false,
    val gloveModeZoom: Boolean = true
) {
    fun normalized(): SkyAppearance = copy(
        intensity = if (intensity.isFinite()) intensity.coerceIn(MIN_INTENSITY, MAX_INTENSITY) else 1f
    )

    companion object {
        const val MIN_INTENSITY = 0.25f
        const val MAX_INTENSITY = 1.75f

        fun restore(
            modeName: String? = null,
            intensity: Float = 1f,
            showInAr: Boolean = false,
            showGrid: Boolean = false,
            showEquatorialGrid: Boolean = false,
            showHorizontalGrid: Boolean = false,
            showCelestialEquator: Boolean = false,
            showEcliptic: Boolean = false,
            showGalacticEquator: Boolean = false,
            showSatellites: Boolean = false,
            labelDensityName: String? = null,
            oledBlackMode: Boolean = false,
            gloveModeZoom: Boolean = true
        ): SkyAppearance = SkyAppearance(
            mode = MilkyWayMode.entries.firstOrNull { it.name == modeName } ?: MilkyWayMode.NATURAL,
            intensity = intensity,
            showInAr = showInAr,
            showGrid = showGrid,
            showEquatorialGrid = showEquatorialGrid,
            showHorizontalGrid = showHorizontalGrid,
            showCelestialEquator = showCelestialEquator,
            showEcliptic = showEcliptic,
            showGalacticEquator = showGalacticEquator,
            showSatellites = showSatellites,
            labelDensity = SkyLabelDensity.entries.firstOrNull { it.name == labelDensityName }
                ?: SkyLabelDensity.NORMAL,
            oledBlackMode = oledBlackMode,
            gloveModeZoom = gloveModeZoom
        ).normalized()
    }
}

internal object SkyAppearancePreferences {
    private const val MODE = "sky_appearance_mode"
    private const val INTENSITY = "sky_appearance_intensity"
    private const val SHOW_IN_AR = "sky_appearance_show_in_ar"
    private const val SHOW_GRID = "sky_appearance_show_grid"
    private const val SHOW_EQUATORIAL_GRID = "sky_appearance_show_equatorial_grid"
    private const val SHOW_HORIZONTAL_GRID = "sky_appearance_show_horizontal_grid"
    private const val SHOW_CELESTIAL_EQUATOR = "sky_appearance_show_celestial_equator"
    private const val SHOW_ECLIPTIC = "sky_appearance_show_ecliptic"
    private const val SHOW_GALACTIC_EQUATOR = "sky_appearance_show_galactic_equator"
    private const val SHOW_SATELLITES = "sky_appearance_show_satellites"
    private const val LABEL_DENSITY = "sky_appearance_label_density"
    private const val OLED_BLACK_MODE = "oled_black_mode"
    private const val GLOVE_MODE_ZOOM = "glove_mode_zoom"

    fun load(context: Context): SkyAppearance {
        val preferences = context.getSharedPreferences("astra_settings", Context.MODE_PRIVATE)
        // A malformed or old preference must not prevent opening the sky map.
        return SkyAppearance.restore(
            modeName = runCatching { preferences.getString(MODE, null) }.getOrNull(),
            intensity = runCatching { preferences.getFloat(INTENSITY, 1f) }.getOrDefault(1f),
            showInAr = runCatching { preferences.getBoolean(SHOW_IN_AR, false) }.getOrDefault(false),
            showGrid = runCatching { preferences.getBoolean(SHOW_GRID, false) }.getOrDefault(false),
            showEquatorialGrid = runCatching { preferences.getBoolean(SHOW_EQUATORIAL_GRID, false) }.getOrDefault(false),
            showHorizontalGrid = runCatching { preferences.getBoolean(SHOW_HORIZONTAL_GRID, false) }.getOrDefault(false),
            showCelestialEquator = runCatching { preferences.getBoolean(SHOW_CELESTIAL_EQUATOR, false) }.getOrDefault(false),
            showEcliptic = runCatching { preferences.getBoolean(SHOW_ECLIPTIC, false) }.getOrDefault(false),
            showGalacticEquator = runCatching { preferences.getBoolean(SHOW_GALACTIC_EQUATOR, false) }.getOrDefault(false),
            showSatellites = runCatching { preferences.getBoolean(SHOW_SATELLITES, false) }.getOrDefault(false),
            labelDensityName = runCatching { preferences.getString(LABEL_DENSITY, null) }.getOrNull(),
            oledBlackMode = runCatching { preferences.getBoolean(OLED_BLACK_MODE, false) }.getOrDefault(false),
            gloveModeZoom = runCatching { preferences.getBoolean(GLOVE_MODE_ZOOM, true) }.getOrDefault(true)
        )
    }

    fun save(context: Context, appearance: SkyAppearance) {
        val normalized = appearance.normalized()
        context.getSharedPreferences("astra_settings", Context.MODE_PRIVATE).edit {
            putString(MODE, normalized.mode.name)
            putFloat(INTENSITY, normalized.intensity)
            putBoolean(SHOW_IN_AR, normalized.showInAr)
            putBoolean(SHOW_GRID, normalized.showGrid)
            putBoolean(SHOW_EQUATORIAL_GRID, normalized.showEquatorialGrid)
            putBoolean(SHOW_HORIZONTAL_GRID, normalized.showHorizontalGrid)
            putBoolean(SHOW_CELESTIAL_EQUATOR, normalized.showCelestialEquator)
            putBoolean(SHOW_ECLIPTIC, normalized.showEcliptic)
            putBoolean(SHOW_GALACTIC_EQUATOR, normalized.showGalacticEquator)
            putBoolean(SHOW_SATELLITES, normalized.showSatellites)
            putString(LABEL_DENSITY, normalized.labelDensity.name)
            putBoolean(OLED_BLACK_MODE, normalized.oledBlackMode)
            putBoolean(GLOVE_MODE_ZOOM, normalized.gloveModeZoom)
        }
    }
}

/** The sheet host owns scrolling, padding and dismissal. */
@Composable
internal fun SkyAppearanceControls(
    appearance: SkyAppearance,
    onChange: (SkyAppearance) -> Unit,
    showBoundaries: Boolean,
    onBoundariesChange: (Boolean) -> Unit,
    showIllustrations: Boolean,
    onIllustrationsChange: (Boolean) -> Unit
) {
    val current = appearance.normalized()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Milchstraße", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                MilkyWayMode.OFF to "Aus",
                MilkyWayMode.NATURAL to "Natürlich",
                MilkyWayMode.PHOTO to "Verstärkt"
            ).forEach { (mode, label) ->
                FilterChip(
                    selected = current.mode == mode,
                    onClick = { onChange(current.copy(mode = mode)) },
                    label = { Text(label) },
                    modifier = Modifier.semantics { contentDescription = "Milchstraße: $label" }
                )
            }
        }
        Text(
            when (current.mode) {
                MilkyWayMode.OFF -> "Die Milchstraße ist ausgeblendet. Sterne und andere Kartenebenen bleiben sichtbar."
                MilkyWayMode.NATURAL -> "Dezente Darstellung mit zurückhaltendem Kontrast, angelehnt an einen dunklen Nachthimmel."
                MilkyWayMode.PHOTO -> "Mehr Kontrast und deutlichere Strukturen, ähnlich einer Langzeitbelichtung. Keine Vorhersage der tatsächlichen Sichtbarkeit."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val percentage = (current.intensity * 100).roundToInt()
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Helligkeit der Milchstraße", style = MaterialTheme.typography.bodyMedium)
                Text("$percentage %", style = MaterialTheme.typography.bodyMedium)
            }
            Slider(
                value = current.intensity,
                onValueChange = { onChange(current.copy(intensity = it).normalized()) },
                valueRange = SkyAppearance.MIN_INTENSITY..SkyAppearance.MAX_INTENSITY,
                enabled = current.mode != MilkyWayMode.OFF,
                modifier = Modifier.semantics {
                    contentDescription = "Helligkeit der Milchstraße"
                    stateDescription = "$percentage Prozent"
                }
            )
        }
        AppearanceSwitch(
            title = "Milchstraße auch in AR",
            detail = "Dezente Überlagerung des Kamerabilds; keine Erkennung der Milchstraße durch die Kamera.",
            checked = current.showInAr,
            onCheckedChange = { onChange(current.copy(showInAr = it)) }
        )
        Text(
            "Die Himmelsdarstellung ist offline verfügbar und basiert auf astronomischen Daten. Sie ist kein Livebild; Wetter, Mondlicht und Lichtverschmutzung verändern den tatsächlichen Anblick.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider()
        Text("Beschriftungen", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                SkyLabelDensity.MINIMAL to "Wenige",
                SkyLabelDensity.NORMAL to "Normal",
                SkyLabelDensity.RICH to "Viele"
            ).forEach { (density, label) ->
                FilterChip(
                    selected = current.labelDensity == density,
                    onClick = { onChange(current.copy(labelDensity = density)) },
                    label = { Text(label, maxLines = 1) },
                    modifier = Modifier.semantics { contentDescription = "Beschriftungen: $label" }
                )
            }
        }
        Text(
            "Ausgewählte Objekte und Orientierungspunkte haben Vorrang. Wenn es eng wird, erscheinen weniger Namen, damit die Karte lesbar bleibt.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AppearanceSwitch(
            title = "Satellitenbahnen und Beschriftungen",
            detail = "Zeigt vorhergesagte Bahnen und Namen heller Satelliten am Himmel.",
            checked = current.showSatellites,
            onCheckedChange = { onChange(current.copy(showSatellites = it)) }
        )
        HorizontalDivider()
        Text("Orientierung", style = MaterialTheme.typography.titleMedium)
        AppearanceSwitch(
            title = "Orientierungsgitter",
            detail = "Klassisches Raster zur Orientierung in der Sternkarte.",
            checked = current.showGrid,
            onCheckedChange = { onChange(current.copy(showGrid = it)) }
        )
        AppearanceSwitch(
            title = "Äquatoriales Koordinatengitter",
            detail = "Rektaszension (RA) und Deklination (Dec).",
            checked = current.showEquatorialGrid,
            onCheckedChange = { onChange(current.copy(showEquatorialGrid = it)) }
        )
        AppearanceSwitch(
            title = "Horizontales Koordinatengitter",
            detail = "Azimut und Höhe über dem Horizont.",
            checked = current.showHorizontalGrid,
            onCheckedChange = { onChange(current.copy(showHorizontalGrid = it)) }
        )
        AppearanceSwitch(
            title = "Himmelsäquator",
            detail = "Projektion des Erdäquators auf die Himmelssphäre (Dec 0°).",
            checked = current.showCelestialEquator,
            onCheckedChange = { onChange(current.copy(showCelestialEquator = it)) }
        )
        AppearanceSwitch(
            title = "Ekliptik",
            detail = "Scheinbare Bahn der Sonne und Planeten über das Jahr.",
            checked = current.showEcliptic,
            onCheckedChange = { onChange(current.copy(showEcliptic = it)) }
        )
        AppearanceSwitch(
            title = "Galaktischer Äquator",
            detail = "Zentrale Symmetrieebene der Milchstraßenscheibe.",
            checked = current.showGalacticEquator,
            onCheckedChange = { onChange(current.copy(showGalacticEquator = it)) }
        )
        AppearanceSwitch(
            title = "IAU-Sternbildgrenzen",
            detail = "Offizielle Grenzen der 88 Sternbilder.",
            checked = showBoundaries,
            onCheckedChange = onBoundariesChange
        )
        AppearanceSwitch(
            title = "Sternbildillustrationen",
            detail = "Optionale schematische Figuren als Orientierungshilfe.",
            checked = showIllustrations,
            onCheckedChange = onIllustrationsChange
        )
        AppearanceSwitch(
            title = "OLED Reinstschwarz (#000000)",
            detail = "Schaltet alle UI-Flächen auf echtes Reinstschwarz um (#000000).",
            checked = current.oledBlackMode,
            onCheckedChange = { onChange(current.copy(oledBlackMode = it)) }
        )
        AppearanceSwitch(
            title = "Handschuh-Modus (Lautstärketasten)",
            detail = "Zoomt die Sternkarte über die Lautstärketasten (+/-) bei Kälte.",
            checked = current.gloveModeZoom,
            onCheckedChange = { onChange(current.copy(gloveModeZoom = it)) }
        )
    }
}

@Composable
private fun AppearanceSwitch(
    title: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
