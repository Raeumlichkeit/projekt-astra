package de.projektastra.app

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import java.io.File
import java.util.UUID
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.ImportExport
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.automirrored.rounded.AltRoute
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.AlertDialog
import android.view.KeyEvent
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material3.LinearProgressIndicator
import de.projektastra.app.observation.ChallengeType
import de.projektastra.app.observation.ObservationChallengeRegistry
import de.projektastra.app.observation.ObservationCatalogMatcher
import de.projektastra.app.observation.DewMonitor
import de.projektastra.app.observation.DewRiskLevel
import de.projektastra.app.observation.OpenAstronomyLogExport
import de.projektastra.app.observation.SeeingScaleValidator
import de.projektastra.app.hardware.CameraExposureController
import de.projektastra.app.hardware.ArSensorFilter
import de.projektastra.app.hardware.GloveModeZoomController
import de.projektastra.app.hardware.OledThemeManager
import de.projektastra.app.widget.AstraWidgetUpdater
import de.projektastra.app.coordinates.CelestialMeasurement
import de.projektastra.app.coordinates.CelestialMeasurementState
import de.projektastra.app.coordinates.GridLineType
import de.projektastra.app.coordinates.MeasurementPoint
import de.projektastra.app.coordinates.SkyGridRenderer
import de.projektastra.app.coordinates.StarHopCatalog
import de.projektastra.app.coordinates.StarHopHud
import de.projektastra.app.coordinates.StarHopReticleMode
import de.projektastra.app.coordinates.StarHopRoute
import de.projektastra.app.coordinates.StarHopSessionState
import de.projektastra.app.coordinates.StarHopSheet
import de.projektastra.app.coordinates.toEquatorial
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.locationbutton.compose.LocationButton
import androidx.core.locationbutton.compose.LocationButtonTextType
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.LifecycleStartEffect
import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.EclipseKind
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time as AstroTime
import io.github.cosinekitty.astronomy.Vector
import io.github.cosinekitty.astronomy.constellation
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.horizon
import io.github.cosinekitty.astronomy.illumination
import io.github.cosinekitty.astronomy.localSolarEclipsesAfter
import io.github.cosinekitty.astronomy.lunarEclipsesAfter
import io.github.cosinekitty.astronomy.rotationEqjEqd
import io.github.cosinekitty.astronomy.rotationGalEqj
import de.projektastra.app.ephemeris.JupiterMoonsCalculator
import de.projektastra.app.ephemeris.JupiterMoonEvent
import de.projektastra.app.ephemeris.JupiterSystemState
import de.projektastra.app.ephemeris.MoonState
import de.projektastra.app.ephemeris.SaturnSystemCalculator
import de.projektastra.app.ephemeris.SaturnSystemState
import de.projektastra.app.ephemeris.LunarTerminatorCalculator
import de.projektastra.app.ephemeris.LunarTerminatorState
import de.projektastra.app.ephemeris.LunarFeatureCatalog
import de.projektastra.app.ephemeris.LunarFeatureHighlight
import de.projektastra.app.ephemeris.TerminatorEventType
import de.projektastra.app.ephemeris.Sgp4Propagator
import de.projektastra.app.ephemeris.SatelliteCatalog
import de.projektastra.app.ephemeris.SatellitePass
import de.projektastra.app.ephemeris.SatellitePassPredictor
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

private val BaseNight = Color(0xFF07101F)
private val BaseNightBlue = Color(0xFF0D1C34)
private val BaseAstraSurface = Color(0xFF10243F)
private val BaseAstraSurfaceHigh = Color(0xFF163252)
private val BaseAstraOutline = Color(0xFF294466)

internal val LocalOledMode = staticCompositionLocalOf { false }

private val Night: Color
    @Composable get() = if (LocalOledMode.current) Color.Black else BaseNight

private val NightBlue: Color
    @Composable get() = if (LocalOledMode.current) Color.Black else BaseNightBlue

private val AstraSurface: Color
    @Composable get() = if (LocalOledMode.current) Color.Black else BaseAstraSurface

private val AstraSurfaceHigh: Color
    @Composable get() = if (LocalOledMode.current) Color.Black else BaseAstraSurfaceHigh

private val AstraOutline: Color
    @Composable get() = if (LocalOledMode.current) Color(0xFF1E1E1E) else BaseAstraOutline

private val AstraBlue = Color(0xFF6DA8FF)
private val StarGold = Color(0xFFFFD98A)
private val AstraTextMuted = Color(0xFFAAB8CE)
private val AstraSuccess = Color(0xFF76E0A0)

private val screenBackgroundBrush: Brush
    @Composable get() = if (LocalOledMode.current) {
        Brush.verticalGradient(listOf(Color.Black, Color.Black))
    } else {
        Brush.verticalGradient(listOf(Night, Color(0xFF09172A), Night))
    }

class MainActivity : ComponentActivity() {
    companion object {
        var volumeKeyZoomHandler: ((Boolean) -> Boolean)? = null
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (volumeKeyZoomHandler?.invoke(true) == true) return true
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (volumeKeyZoomHandler?.invoke(false) == true) return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) && volumeKeyZoomHandler != null) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PrivacySettings.migrateLegacyBrowserData(this)
        PublicTileCache.prune(this)
        SecureNetwork.configure(PrivacySettings.load(this))
        setContent { AstraRoot() }
    }

    override fun onStart() {
        SecureNetwork.setForeground(true)
        super.onStart()
    }

    override fun onStop() {
        SecureNetwork.setForeground(false)
        TerrainRepository.clear()
        LightPollutionRepository.clear()
        super.onStop()
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_CRITICAL || level >= TRIM_MEMORY_UI_HIDDEN) {
            TerrainRepository.clear()
            LightPollutionRepository.clear()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        TerrainRepository.clear()
        LightPollutionRepository.clear()
    }
}

@Composable
private fun AstraRoot() {
    val context = LocalContext.current
    val window = LocalActivity.current?.window
    val preferences = remember { context.getSharedPreferences("astra_settings", Context.MODE_PRIVATE) }
    var redLightMode by remember { mutableStateOf(preferences.getBoolean("red_light_mode", false)) }
    var oledBlackMode by remember {
        mutableStateOf(
            preferences.getBoolean("oled_black_mode", false) ||
            runCatching { SkyAppearancePreferences.load(context).oledBlackMode }.getOrDefault(false)
        )
    }
    var skyFullscreen by rememberSaveable { mutableStateOf(false) }
    val setRedLightMode: (Boolean) -> Unit = {
        redLightMode = it
        preferences.edit { putBoolean("red_light_mode", it) }
    }
    val setOledBlackMode: (Boolean) -> Unit = {
        oledBlackMode = it
        preferences.edit { putBoolean("oled_black_mode", it) }
        runCatching {
            SkyAppearancePreferences.save(context, SkyAppearancePreferences.load(context).copy(oledBlackMode = it))
        }
    }
    SideEffect {
        window?.let { window ->
            val systemBarColor = if (redLightMode) android.graphics.Color.rgb(24, 0, 0)
            else if (oledBlackMode) android.graphics.Color.BLACK
            else android.graphics.Color.rgb(7, 16, 31)
            @Suppress("DEPRECATION")
            window.statusBarColor = systemBarColor
            @Suppress("DEPRECATION")
            window.navigationBarColor = systemBarColor
            WindowCompat.getInsetsController(window, window.decorView).let { controller ->
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (redLightMode || skyFullscreen) controller.hide(WindowInsetsCompat.Type.systemBars())
                else controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    AstraTheme(redLightMode, oledBlackMode) {
        Box(
            Modifier.fillMaxSize().drawWithContent {
                drawContent()
                if (redLightMode) {
                    drawRect(Color(0xFFC40000), blendMode = BlendMode.Multiply)
                }
            }
        ) {
            AstraApp(redLightMode, setRedLightMode, oledBlackMode, setOledBlackMode, skyFullscreen) { skyFullscreen = it }
        }
    }
}

@Composable
private fun AstraTheme(redLightMode: Boolean, oledMode: Boolean = false, content: @Composable () -> Unit) {
    val primary = if (redLightMode) Color(0xFFD35A4A) else AstraBlue
    val secondary = if (redLightMode) Color(0xFFFF725C) else StarGold
    val bgColor = if (oledMode) Color.Black else BaseNight
    val surfaceColor = if (oledMode) Color.Black else BaseNightBlue
    val surfaceVarColor = if (oledMode) Color.Black else BaseAstraSurface
    val outlineColor = if (oledMode) Color(0xFF1E1E1E) else BaseAstraOutline
    CompositionLocalProvider(LocalOledMode provides oledMode) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = primary,
                secondary = secondary,
                background = bgColor,
                surface = surfaceColor,
                surfaceVariant = surfaceVarColor,
                outline = outlineColor,
                onBackground = Color(0xFFEAF1FF),
                onSurface = Color(0xFFEAF1FF)
            ),
            shapes = Shapes(
                small = RoundedCornerShape(10.dp),
                medium = RoundedCornerShape(18.dp),
                large = RoundedCornerShape(26.dp)
            ),
            content = content
        )
    }
}

private enum class AstraTab { SKY, WEATHER, EVENTS, PLAN, ABOUT }

@Composable
private fun isCompactLandscape(): Boolean {
    val size = LocalWindowInfo.current.containerSize
    val height = with(LocalDensity.current) { size.height.toDp() }
    return size.width > size.height && height < 480.dp
}

@Composable
private fun AstraApp(
    redLightMode: Boolean,
    setRedLightMode: (Boolean) -> Unit,
    oledBlackMode: Boolean = false,
    setOledBlackMode: (Boolean) -> Unit = {},
    skyFullscreen: Boolean,
    setSkyFullscreen: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val compactLandscape = isCompactLandscape()
    var tab by rememberSaveable { mutableStateOf(AstraTab.SKY) }
    var location by remember { mutableStateOf(LocationStore.getSavedLocation(context)) }
    var rememberLocation by remember { mutableStateOf(LocationStore.isRememberEnabled(context)) }
    var useSessionLocation by rememberSaveable { mutableStateOf(true) }
    var permissionGranted by remember { mutableStateOf(false) }
    var cameraGranted by remember { mutableStateOf(false) }
    var arEnabled by rememberSaveable { mutableStateOf(false) }
    var pendingSkyObjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingSkyPosition by remember { mutableStateOf<HorizontalCoordinates?>(null) }
    var skyNotice by remember { mutableStateOf<String?>(null) }
    val skyClock = rememberSaveable(saver = Saver<SkyClock, String>(
        save = { if (it.state.live) "live" else it.state.instant.toString() },
        restore = { saved ->
            SkyClock(Instant::now, SystemClock::elapsedRealtime).also { clock ->
                if (saved != "live") runCatching { clock.select(Instant.parse(saved)) }
            }
        }
    )) { SkyClock(Instant::now, SystemClock::elapsedRealtime) }
    LifecycleStartEffect(tab) {
        val isSkyVisible = tab == AstraTab.SKY
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val tick = object : Runnable {
            override fun run() {
                skyClock.tick()
                handler.postDelayed(this, 250)
            }
        }
        if (isSkyVisible) {
            skyClock.start()
            handler.post(tick)
        }
        onStopOrDispose {
            handler.removeCallbacks(tick)
            if (isSkyVisible) skyClock.stop()
        }
    }
    var locationRefreshKey by remember { mutableIntStateOf(0) }
    var favoriteObjectIds by remember { mutableStateOf(ObservationStore.favoriteObjectIds(context)) }
    var privacy by remember { mutableStateOf(PrivacySettings.load(context)) }
    var showOnlineConsent by remember { mutableStateOf(!PrivacySettings.hasDecision(context)) }
    val updatePrivacy: (PrivacyOptions) -> Unit = { value ->
        PrivacySettings.save(context, value)
        privacy = value
        TerrainRepository.clear()
        LightPollutionRepository.clear()
    }
    var savedEvents by remember { mutableStateOf(ObservationStore.savedEvents(context)) }
    val initialLogbook = remember { runCatching { ObservationLogbookStore.loadEntries(context) } }
    var logbookError by remember {
        mutableStateOf(initialLogbook.exceptionOrNull()?.let { "Logbuch konnte nicht geladen werden: ${it.message}" })
    }
    var logbookEntries by remember { mutableStateOf(initialLogbook.getOrDefault(emptyList())) }
    var reminderHours by remember { mutableIntStateOf(ObservationStore.reminderHours(context)) }
    var notificationsGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraGranted = granted
        if (granted) {
            if (!skyClock.state.live) skyNotice = "AR verwendet Jetzt. Die Simulation wurde beendet."
            skyClock.now()
            arEnabled = true
        }
    }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsGranted = granted
        if (granted) EventReminderScheduler.rescheduleAll(context, savedEvents, reminderHours)
    }

    LaunchedEffect(Unit) {
        permissionGranted = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        cameraGranted = context.checkSelfPermission(Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }

    LocationEffect(permissionGranted && useSessionLocation, locationRefreshKey) { newLoc ->
        if (useSessionLocation) {
            location = newLoc
            if (rememberLocation) {
                LocationStore.saveLocation(context, newLoc)
                AstraWidgetUpdater.updateAllWidgets(context)
            }
        }
    }
    LaunchedEffect(tab) { if (tab != AstraTab.SKY) setSkyFullscreen(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (!skyFullscreen) {
            val navigationColors = NavigationBarItemDefaults.colors(
                selectedIconColor = if (oledBlackMode) Color.Black else Night,
                selectedTextColor = StarGold,
                indicatorColor = StarGold,
                unselectedIconColor = AstraTextMuted,
                unselectedTextColor = AstraTextMuted
            )
            NavigationBar(containerColor = if (oledBlackMode) Color.Black else Color(0xFF091426), tonalElevation = 0.dp) {
                NavigationBarItem(
                    selected = tab == AstraTab.SKY,
                    onClick = { tab = AstraTab.SKY },
                    icon = { Icon(Icons.Rounded.Public, if (compactLandscape) "Sternkarte" else null) },
                    label = if (compactLandscape) null else ({ Text("Sternkarte") }),
                    colors = navigationColors
                )
                NavigationBarItem(
                    selected = tab == AstraTab.WEATHER,
                    onClick = { tab = AstraTab.WEATHER },
                    icon = { Icon(Icons.Rounded.Cloud, if (compactLandscape) "Wetter" else null) },
                    label = if (compactLandscape) null else ({ Text("Wetter") }),
                    colors = navigationColors
                )
                NavigationBarItem(
                    selected = tab == AstraTab.EVENTS,
                    onClick = { tab = AstraTab.EVENTS },
                    icon = { Icon(Icons.Rounded.CalendarMonth, if (compactLandscape) "Kalender" else null) },
                    label = if (compactLandscape) null else ({ Text("Kalender") }),
                    colors = navigationColors
                )
                NavigationBarItem(
                    selected = tab == AstraTab.PLAN,
                    onClick = { tab = AstraTab.PLAN },
                    icon = { Icon(Icons.Rounded.Bookmarks, if (compactLandscape) "Plan" else null) },
                    label = if (compactLandscape) null else ({ Text("Plan") }),
                    colors = navigationColors
                )
                NavigationBarItem(
                    selected = tab == AstraTab.ABOUT,
                    onClick = { tab = AstraTab.ABOUT },
                    icon = { Icon(Icons.Rounded.Info, if (compactLandscape) "Info" else null) },
                    label = if (compactLandscape) null else ({ Text("Info") }),
                    colors = navigationColors
                )
            }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            key(tab, privacy) {
                when (tab) {
                    AstraTab.SKY -> SkyScreen(
                    location = location,
                    locationPermissionGranted = permissionGranted,
                    cameraPermissionGranted = cameraGranted,
                    arEnabled = arEnabled,
                    redLightMode = redLightMode,
                    oledBlackMode = oledBlackMode,
                    setOledBlackMode = setOledBlackMode,
                    fullscreen = skyFullscreen,
                    setFullscreen = setSkyFullscreen,
                    favoriteObjectIds = favoriteObjectIds,
                    requestedObjectId = pendingSkyObjectId,
                    consumeObjectRequest = { pendingSkyObjectId = null },
                    requestedPosition = pendingSkyPosition,
                    consumePositionRequest = { pendingSkyPosition = null },
                    skyTime = skyClock.state,
                    selectTime = { skyClock.select(it); arEnabled = false; skyNotice = null },
                    playTime = { skyClock.play(it); skyNotice = null },
                    pauseTime = { skyClock.pause() },
                    nowTime = { skyClock.now(); skyNotice = null },
                    timeNotice = skyNotice,
                    clearTimeNotice = { skyNotice = null },
                    useManualMap = { arEnabled = false },
                    toggleRedLightMode = { setRedLightMode(!redLightMode) },
                    toggleFavorite = { objectData ->
                        favoriteObjectIds = ObservationStore.setObjectFavorite(
                            context,
                            objectData.catalogId,
                            objectData.catalogId !in favoriteObjectIds
                        )
                    },
                    onLocationPermissionResult = { granted ->
                        permissionGranted = granted
                        if (granted) { useSessionLocation = true; locationRefreshKey++ }
                    },
                    onResetLocation = {
                        useSessionLocation = false
                        LocationStore.setRememberEnabled(context, false)
                        rememberLocation = false
                        LocationStore.clearLocation(context)
                        location = null
                        AstraWidgetUpdater.updateAllWidgets(context)
                    },
                    toggleAr = {
                        if (arEnabled) arEnabled = false
                        else if (cameraGranted) {
                            if (!skyClock.state.live) skyNotice = "AR verwendet Jetzt. Die Simulation wurde beendet."
                            skyClock.now()
                            arEnabled = true
                        }
                        else cameraLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onSaveLogEntry = { entry ->
                        try {
                            logbookEntries = ObservationLogbookStore.saveEntry(context, entry)
                            logbookError = null
                        } catch (error: Exception) {
                            logbookError = "Logbuch konnte nicht gespeichert werden: ${error.message}"
                            throw error
                        }
                    },
                    logbookEntries = logbookEntries
                )
                    AstraTab.WEATHER -> WeatherScreen(
                        location = location,
                        simulatedSkyTime = skyClock.state.instant.takeUnless { skyClock.state.live },
                        refreshLocation = { useSessionLocation = true; locationRefreshKey++ }
                    )
                    AstraTab.EVENTS -> EventsScreen(
                        location = location,
                        redLightMode = redLightMode,
                        openInSky = { event ->
                            skyClock.select(event.instant)
                            arEnabled = false
                            val body = when (event.kind) {
                                SkyEventKind.SOLAR_ECLIPSE -> Body.Sun
                                SkyEventKind.LUNAR_ECLIPSE -> Body.Moon
                                else -> null
                            }
                            pendingSkyObjectId = body?.let { "Astronomy Engine · $it" }
                            pendingSkyPosition = event.radiant?.let {
                                SkyCoordinateFrame(location ?: GeoPoint(52.52, 13.405, 34.0), event.instant)
                                    .horizontal(it.raHours, it.decDegrees)
                            }
                            skyNotice = event.title + if (event.timeIsApproximate) " · Beispielzeit der Maximum-Nacht, kein exakter Peak" else " · zum Maximum"
                            tab = AstraTab.SKY
                        },
                        savedEventKeys = savedEvents.mapTo(mutableSetOf()) { it.key },
                        toggleSavedEvent = { event ->
                            val saved = event.key !in savedEvents.map { it.key }.toSet()
                            val stored = event.toSavedEvent()
                            savedEvents = ObservationStore.setEventSaved(context, stored, saved)
                            if (saved) {
                                EventReminderScheduler.schedule(context, stored, reminderHours)
                                if (!notificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else EventReminderScheduler.cancel(context, stored.key)
                        }
                    )
                    AstraTab.PLAN -> ObservationPlanScreen(
                        location = location,
                        favoriteIds = favoriteObjectIds,
                        openEventTime = { event ->
                            skyClock.select(Instant.ofEpochSecond(event.instantEpochSeconds))
                            arEnabled = false
                            skyNotice = "${event.title} · gespeicherter Ereigniszeitpunkt"
                            tab = AstraTab.SKY
                        },
                        openObject = { id ->
                            pendingSkyObjectId = id
                            arEnabled = false
                            tab = AstraTab.SKY
                        },
                        savedEvents = savedEvents,
                        reminderHours = reminderHours,
                        notificationsGranted = notificationsGranted,
                        setReminderHours = { hours ->
                            reminderHours = hours
                            ObservationStore.setReminderHours(context, hours)
                            EventReminderScheduler.rescheduleAll(context, savedEvents, hours)
                        },
                        removeFavorite = { id ->
                            favoriteObjectIds = ObservationStore.setObjectFavorite(context, id, false)
                        },
                        removeEvent = { event ->
                            savedEvents = ObservationStore.setEventSaved(context, event, false)
                            EventReminderScheduler.cancel(context, event.key)
                        },
                        requestNotifications = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        toggleFavorite = { objectData ->
                            favoriteObjectIds = ObservationStore.setObjectFavorite(
                                context,
                                objectData.catalogId,
                                objectData.catalogId !in favoriteObjectIds
                            )
                        },
                        privacy = privacy,
                        logbookEntries = logbookEntries,
                        logbookError = logbookError,
                        onSaveLogEntry = { entry ->
                            try {
                                logbookEntries = ObservationLogbookStore.saveEntry(context, entry)
                                logbookError = null
                            } catch (error: Exception) {
                                logbookError = "Logbuch konnte nicht gespeichert werden: ${error.message}"
                                throw error
                            }
                        },
                        onDeleteLogEntry = { entryId ->
                            runCatching { ObservationLogbookStore.deleteEntry(context, entryId) }
                                .onSuccess { logbookEntries = it; logbookError = null }
                                .onFailure { logbookError = "Eintrag konnte nicht gelöscht werden: ${it.message}" }
                        },
                        onDeleteAllLogEntries = {
                            runCatching { ObservationLogbookStore.deleteAllEntries(context) }
                                .onSuccess { logbookEntries = emptyList(); logbookError = null }
                                .onFailure { logbookError = "Logbuch konnte nicht gelöscht werden: ${it.message}" }
                        },
                        onImportLogbook = { jsonText, merge ->
                            runCatching {
                                val result = ObservationLogbookStore.importJson(context, jsonText, merge)
                                if (result.success) logbookEntries = ObservationLogbookStore.loadEntries(context)
                                result
                            }.getOrElse { error ->
                                logbookError = "Import fehlgeschlagen: ${error.message}"
                                LogbookImportResult(false, 0, 0, logbookError!!)
                            }
                        }
                    )
                    AstraTab.ABOUT -> AboutScreen(
                        redLightMode = redLightMode,
                        setRedLightMode = setRedLightMode,
                        oledBlackMode = oledBlackMode,
                        setOledBlackMode = setOledBlackMode,
                        privacy = privacy,
                        rememberLocation = rememberLocation,
                        onRememberLocationChange = { enabled ->
                            LocationStore.setRememberEnabled(context, enabled)
                            rememberLocation = enabled
                            if (enabled) location?.let { LocationStore.saveLocation(context, it) }
                            AstraWidgetUpdater.updateAllWidgets(context)
                        },
                        requestOnline = { showOnlineConsent = true },
                        setPrivacy = updatePrivacy
                    )
                }
            }
        }
    }
    if (showOnlineConsent) OnlineConsentDialog { online ->
        updatePrivacy(PrivacyOptions(online = online))
        showOnlineConsent = false
    }
}

@Composable
private fun LocationEffect(enabled: Boolean, refreshKey: Int, onLocation: (GeoPoint) -> Unit) {
    val context = LocalContext.current
    val currentCallback by rememberUpdatedState(onLocation)
    LifecycleStartEffect(enabled, refreshKey) {
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = LocationListener { value ->
            currentCallback(GeoPoint(value.latitude, value.longitude, value.altitude))
        }
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        if (enabled && hasLocationPermission) providers.forEach { provider ->
            runCatching {
                manager.getLastKnownLocation(provider)?.let(listener::onLocationChanged)
                manager.requestLocationUpdates(provider, 30_000L, 100f, listener)
            }
        }
        onStopOrDispose { manager.removeUpdates(listener) }
    }
}

@Composable
private fun rememberOrientation(
    observer: GeoPoint,
    fovDegrees: Float = 45f,
    pitchTrimDegrees: Float = 0f,
    dampingEnabled: Boolean = true
): OrientationState {
    val context = LocalContext.current
    var azimuth by remember { mutableFloatStateOf(180f) }
    var pitch by remember { mutableFloatStateOf(35f) }
    var available by remember { mutableStateOf(false) }
    var accuracy by remember { mutableIntStateOf(SensorManager.SENSOR_STATUS_UNRELIABLE) }
    val sensorFilter = remember { ArSensorFilter() }

    val magneticDeclination = remember(observer) {
        GeomagneticField(
            observer.latitude.toFloat(),
            observer.longitude.toFloat(),
            observer.altitudeMeters.toFloat(),
            System.currentTimeMillis()
        ).declination
    }

    val currentFov by rememberUpdatedState(fovDegrees)
    val currentTrim by rememberUpdatedState(pitchTrimDegrees)
    val currentDamping by rememberUpdatedState(dampingEnabled)

    LifecycleStartEffect(observer, magneticDeclination) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: manager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
        available = sensor != null
        val listener = object : SensorEventListener {
            private val rotation = FloatArray(9)
            private val remapped = FloatArray(9)
            private val angles = FloatArray(3)

            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                SensorManager.remapCoordinateSystem(
                    rotation,
                    SensorManager.AXIS_X,
                    SensorManager.AXIS_Z,
                    remapped
                )
                SensorManager.getOrientation(remapped, angles)
                val targetAzimuth = ((Math.toDegrees(angles[0].toDouble()) + magneticDeclination + 360.0) % 360.0).toFloat()
                val targetPitch = (-Math.toDegrees(angles[1].toDouble())).toFloat().coerceIn(-90f, 90f)

                val trim = currentTrim
                val fov = currentFov
                if (currentDamping) {
                    sensorFilter.pitchTrimDegrees = trim
                    val (filteredAz, filteredPitch) = sensorFilter.filter(targetAzimuth, targetPitch, fov)
                    azimuth = filteredAz
                    pitch = filteredPitch
                } else {
                    val delta = ((targetAzimuth - azimuth + 540f) % 360f) - 180f
                    azimuth = (azimuth + delta * 0.18f + 360f) % 360f
                    pitch += (targetPitch + trim.coerceIn(-15f, 15f) - pitch) * 0.18f
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, value: Int) {
                accuracy = value
            }
        }
        sensor?.let { manager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        onStopOrDispose { manager.unregisterListener(listener) }
    }
    return OrientationState(azimuth, pitch, available, accuracy)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SkyScreen(
    location: GeoPoint?,
    locationPermissionGranted: Boolean,
    cameraPermissionGranted: Boolean,
    arEnabled: Boolean,
    redLightMode: Boolean,
    oledBlackMode: Boolean = false,
    setOledBlackMode: (Boolean) -> Unit = {},
    fullscreen: Boolean,
    setFullscreen: (Boolean) -> Unit,
    favoriteObjectIds: Set<String>,
    requestedObjectId: String?,
    consumeObjectRequest: () -> Unit,
    requestedPosition: HorizontalCoordinates?,
    consumePositionRequest: () -> Unit,
    skyTime: SkyTimeState,
    selectTime: (Instant) -> Unit,
    playTime: (Int) -> Unit,
    pauseTime: () -> Unit,
    nowTime: () -> Unit,
    timeNotice: String?,
    clearTimeNotice: () -> Unit,
    useManualMap: () -> Unit,
    toggleRedLightMode: () -> Unit,
    toggleFavorite: (CelestialObject) -> Unit,
    onLocationPermissionResult: (Boolean) -> Unit,
    onResetLocation: () -> Unit,
    toggleAr: () -> Unit,
    onSaveLogEntry: (ObservationLogEntry) -> Unit = {},
    logbookEntries: List<ObservationLogEntry> = emptyList()
) {
    val observer = location ?: GeoPoint(52.52, 13.405, 34.0)
    val initialCameraFov = rememberCameraHorizontalFov()
    var cameraFov by remember { mutableDoubleStateOf(initialCameraFov) }
    var cameraExposureStep by rememberSaveable { mutableIntStateOf(0) }
    var pitchTrimDegrees by rememberSaveable { mutableFloatStateOf(0f) }
    var arDampingEnabled by rememberSaveable { mutableStateOf(true) }
    val orientation = rememberOrientation(observer, cameraFov.toFloat(), pitchTrimDegrees, arDampingEnabled)
    val loggedCatalogIds = remember(logbookEntries) {
        ObservationCatalogMatcher.buildLoggedTokens(logbookEntries)
    }
    val context = LocalContext.current
    val catalogStore = remember(context) { ProcessSkyCatalogs.get(context) }
    var catalogRetry by remember { mutableIntStateOf(0) }
    val catalogs = rememberSkyCatalogs(catalogStore, catalogRetry)
    val stars = catalogs.stars ?: StarCatalog.fallbackObjects
    val deepSkyObjects = catalogs.deepSky.orEmpty()
    val iauBoundaries = catalogs.boundaries.orEmpty()
    var selectedCatalogId by rememberSaveable { mutableStateOf<String?>(null) }
    var logEntryTarget by remember { mutableStateOf<CelestialObject?>(null) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var showTimeControls by rememberSaveable { mutableStateOf(false) }
    var controlsExpanded by rememberSaveable { mutableStateOf(false) }
    var showLocationDialog by rememberSaveable { mutableStateOf(false) }
    val controlsScrollState = rememberScrollState()
    LaunchedEffect(controlsExpanded) { controlsScrollState.scrollTo(0) }
    BackHandler(enabled = fullscreen) { setFullscreen(false) }
    var displayZone by remember { mutableStateOf(ZoneId.systemDefault()) }
    // Save identifiers, not catalog objects or observer coordinates, across Activity recreation.
    var selectionTargetId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectionTracking by rememberSaveable { mutableStateOf(false) }
    var targetMessage by remember { mutableStateOf<String?>(null) }
    var showDeepSky by rememberSaveable { mutableStateOf(false) }
    var showBoundaries by rememberSaveable { mutableStateOf(false) }
    var showIllustrations by rememberSaveable { mutableStateOf(false) }
    var showLayersPanel by rememberSaveable { mutableStateOf(false) }
    var manualAzimuth by rememberSaveable { mutableFloatStateOf(180f) }
    var manualAltitude by rememberSaveable { mutableFloatStateOf(35f) }
    var manualFov by rememberSaveable { mutableFloatStateOf(95f) }
    var appearance by remember { mutableStateOf(SkyAppearancePreferences.load(context).copy(oledBlackMode = oledBlackMode)) }
    LaunchedEffect(oledBlackMode) {
        if (appearance.oledBlackMode != oledBlackMode) {
            appearance = appearance.copy(oledBlackMode = oledBlackMode)
        }
    }
    val isOled = LocalOledMode.current || oledBlackMode || appearance.oledBlackMode
    DisposableEffect(appearance.gloveModeZoom, arEnabled) {
        if (appearance.gloveModeZoom && !arEnabled) {
            MainActivity.volumeKeyZoomHandler = { zoomIn ->
                if (zoomIn) {
                    manualFov = (manualFov / 1.25f).coerceAtLeast(0.5f)
                } else {
                    manualFov = (manualFov * 1.25f).coerceAtMost(150f)
                }
                true
            }
        } else {
            MainActivity.volumeKeyZoomHandler = null
        }
        onDispose {
            MainActivity.volumeKeyZoomHandler = null
        }
    }
    var textureReady by remember { mutableStateOf<Boolean?>(null) }
    var textureRetry by remember { mutableIntStateOf(0) }
    var firstMapFrameRendered by remember { mutableStateOf(false) }
    var showCalibration by rememberSaveable { mutableStateOf(false) }
    var terrainState by remember { mutableStateOf<TerrainState>(TerrainState.Loading) }
    var opticsSettings by remember { mutableStateOf(OpticsStore.loadSettings(context)) }
    var opticsProfiles by remember { mutableStateOf(OpticsStore.loadProfiles(context)) }
    var showOpticsSheet by rememberSaveable { mutableStateOf(false) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var showMoonDetailSheet by rememberSaveable { mutableStateOf(false) }
    var measurementState by remember { mutableStateOf(CelestialMeasurementState()) }
    var starHopSession by remember { mutableStateOf(StarHopSessionState()) }
    var showStarHopSheet by remember { mutableStateOf(false) }
    val skyInstant = skyTime.instant
    val satelliteCatalog = remember(context) { SatelliteCatalog.loadActiveSatellites(context) }
    val passPredictor = remember { SatellitePassPredictor(Sgp4Propagator()) }
    val predictionMinute = Math.floorDiv(skyInstant.epochSecond, 60L)
    var satellitePasses by remember(satelliteCatalog, observer, predictionMinute) {
        mutableStateOf(emptyList<SatellitePass>())
    }
    LaunchedEffect(satelliteCatalog, observer, predictionMinute) {
        satellitePasses = withContext(Dispatchers.Default) {
            satelliteCatalog.take(3).flatMap { tle ->
                currentCoroutineContext().ensureActive()
                passPredictor.predictPasses(tle, observer, Instant.ofEpochSecond(predictionMinute * 60), durationHours = 12)
            }
        }
    }
    LifecycleStartEffect(Unit) {
        displayZone = ZoneId.systemDefault()
        onStopOrDispose { }
    }
    var terrainRetry by remember { mutableIntStateOf(0) }
    LaunchedEffect(
        (observer.latitude * 1_000).roundToInt(),
        (observer.longitude * 1_000).roundToInt(),
        terrainRetry
    ) {
        if (SecureNetwork.options.terrain) {
            terrainState = TerrainState.Loading
            TerrainRepository.load(observer) { terrainState = it }
        } else terrainState = TerrainState.Unavailable
    }
    val coordinateFrame = remember(observer, skyInstant) { SkyCoordinateFrame(observer, skyInstant) }
    val solarSystem = remember(observer, skyInstant) { SolarSystemCatalog.at(observer, skyInstant) }
    val searchIndex = catalogs.searchIndex ?: remember { SkySearchIndex(emptyList()) }
    val movingSearchTargets = remember(solarSystem) { solarSystem.map { SkySearchTarget(it) } }
    val selectionTarget = remember(selectionTargetId, stars, deepSkyObjects) {
        val id = selectionTargetId
        if (id == null) null
        else (stars + deepSkyObjects).firstOrNull { it.catalogId == id }?.let { SkySearchTarget(it) }
            ?: constellationSearchTargets(stars).firstOrNull { it.id == id }
    } ?: movingSearchTargets.firstOrNull { it.id == selectionTargetId }
    val selectedObject = remember(selectedCatalogId, stars, deepSkyObjects) {
        selectedCatalogId?.let { id -> (stars + deepSkyObjects).firstOrNull { it.catalogId == id } }
    } ?: solarSystem.firstOrNull { it.catalogId == selectedCatalogId }
    val selection = SkyTargetSelection(selectionTarget, selectionTracking && selectionTarget != null)
    fun updateSelection(value: SkyTargetSelection) {
        selectionTargetId = value.target?.id
        selectionTracking = value.tracking
    }
    var wasArEnabled by remember { mutableStateOf(arEnabled) }
    LaunchedEffect(arEnabled) {
        if (wasArEnabled && !arEnabled && selectionTargetId == null) {
            manualAzimuth = orientation.azimuth
            manualAltitude = orientation.altitude
        }
        if (arEnabled) selectionTracking = false
        wasArEnabled = arEnabled
    }
    val resolveTarget: (SkySearchTarget) -> CelestialObject = { target ->
        solarSystem.firstOrNull { it.catalogId == target.objectData.catalogId } ?: target.objectData
    }
    val positionOf: (SkySearchTarget) -> HorizontalCoordinates = { target ->
        val data = resolveTarget(target)
        data.solarBody?.let { SolarSystemCatalog.horizontal(it, observer, skyInstant) }
            ?: coordinateFrame.horizontal(data.raHours, data.decDegrees)
    }
    val targetPosition = selection.target?.let(positionOf)
    val viewAzimuth = if (arEnabled) orientation.azimuth
        else if (selection.tracking && targetPosition != null) targetPosition.azimuth.toFloat() else manualAzimuth
    val viewAltitude = if (arEnabled) orientation.altitude
        else if (selection.tracking && targetPosition != null) targetPosition.altitude.toFloat() else manualAltitude
    val openTarget: (SkySearchTarget) -> Unit = { target ->
        val position = positionOf(target)
        useManualMap()
        updateSelection(selection.select(target))
        manualAzimuth = position.azimuth.toFloat()
        manualAltitude = position.altitude.toFloat()
        if (target.needsDeepSky) showDeepSky = true
        if (target.regionName != null) showBoundaries = true
        targetMessage = when {
            target.needsDeepSky -> "Deep-Sky-Ebene eingeschaltet"
            target.regionName != null -> "Sternbildgrenzen eingeschaltet · Referenzpunkt: ${target.objectData.name}"
            else -> null
        }
        showSearch = false
        selectedCatalogId = null
    }
    LaunchedEffect(requestedObjectId, solarSystem, catalogs.objectsReady) {
        if (requestedObjectId != null && catalogs.objectsReady) {
            val data = (stars + deepSkyObjects + solarSystem).firstOrNull {
                it.catalogId == requestedObjectId ||
                    it.catalogId.split("·").any { part -> part.trim().equals(requestedObjectId, ignoreCase = true) } ||
                    (it.messierId.isNotEmpty() && it.messierId.equals(requestedObjectId, ignoreCase = true))
            }
            if (data != null) openTarget(SkySearchTarget(data))
            else targetMessage = "Das vorgemerkte Objekt ist im Offline-Katalog nicht verfügbar."
            consumeObjectRequest()
        }
    }
    LaunchedEffect(requestedPosition) {
        requestedPosition?.let {
            manualAzimuth = it.azimuth.toFloat()
            manualAltitude = it.altitude.toFloat()
            updateSelection(SkyTargetSelection())
            consumePositionRequest()
        }
    }
    val horizontalBoundaries = remember(coordinateFrame, iauBoundaries) {
        iauBoundaries.map { boundary ->
            HorizontalConstellationBoundary(
                boundary.abbreviation,
                boundary.points.map { point ->
                    coordinateFrame.horizontal(point.raHours, point.decDegrees)
                }
            )
        }
    }
    val visible = remember(observer, skyInstant, showDeepSky, stars, deepSkyObjects) {
        val catalog = if (showDeepSky) stars + solarSystem + deepSkyObjects else stars + solarSystem
        catalog.map {
            VisibleObject(it, it.solarBody?.let { body -> SolarSystemCatalog.horizontal(body, observer, skyInstant) }
                ?: coordinateFrame.horizontal(it.raHours, it.decDegrees))
        }
    }
    val textureState = remember(coordinateFrame, viewAzimuth, viewAltitude, arEnabled, manualFov, cameraFov, appearance, opticsSettings) {
        SkyTextureState(coordinateFrame, viewAzimuth.toDouble(), viewAltitude.toDouble(),
            if (arEnabled) cameraFov else manualFov.toDouble(), arEnabled, appearance, opticsSettings)
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(if (isOled) Color.Black else Color.Transparent)) {
    val compactHeight = maxHeight < 420.dp
    val compactLandscape = compactHeight && maxWidth > maxHeight
    // Bound the combined chrome, including notices, so short landscape screens retain sky space.
    val headerMaxHeight = maxHeight * 0.12f
    val orientationMaxHeight = (maxHeight * 0.18f).coerceAtMost(110.dp)
    val controlsMaxHeight = (maxHeight * if (compactHeight) 0.26f else 0.34f).coerceAtMost(270.dp)
    val targetMaxHeight = (maxHeight * 0.18f).coerceAtMost(148.dp)
    Column(Modifier.fillMaxSize().background(if (isOled) Color.Black else Color.Transparent)) {
        if (!fullscreen && !compactHeight) {
        Row(
            Modifier.fillMaxWidth().heightIn(max = headerMaxHeight).verticalScroll(rememberScrollState())
                .background(if (isOled) Color.Black else NightBlue)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(if (skyTime.live) "Sternkarte" else "Simulation", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                if (!catalogs.complete) {
                    Text(if (catalogs.failed) "Offline-Katalog unvollständig · Suche zum Wiederholen öffnen"
                        else "Offline-Katalog wird geladen …", fontSize = 12.sp, color = AstraTextMuted)
                }
                Text(
                    if (location != null) (if (locationPermissionGranted) "GPS-Standort" else "Gespeicherter Standort")
                    else "Standort: Berlin Demo",
                    fontSize = 12.sp,
                    color = AstraTextMuted
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showSearch = true }) {
                    Icon(Icons.Rounded.Search, "Objekte suchen", tint = AstraTextMuted)
                }
                IconButton(onClick = { setOledBlackMode(!isOled) }) {
                    Icon(
                        Icons.Rounded.Contrast,
                        contentDescription = if (isOled) "OLED-Reinstschwarz ausschalten" else "OLED-Reinstschwarz einschalten",
                        tint = if (isOled) StarGold else AstraTextMuted
                    )
                }
                IconButton(onClick = toggleRedLightMode) {
                    Icon(
                        Icons.Rounded.DarkMode,
                        contentDescription = if (redLightMode) "Rotlicht ausschalten" else "Rotlicht einschalten",
                        tint = if (redLightMode) StarGold else AstraTextMuted
                    )
                }
            }
        }
        }

        Row(Modifier.fillMaxWidth().background(if (isOled) Color.Black else NightBlue).padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).heightIn(max = orientationMaxHeight)
                .verticalScroll(rememberScrollState()).padding(vertical = 6.dp)) {
                Text("${cardinalDirection(viewAzimuth)} ${viewAzimuth.toInt()}° · Höhe ${viewAltitude.toInt()}°",
                    color = StarGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("Sichtfeld ${(if (arEnabled) cameraFov else manualFov.toDouble()).format(0)}° horizontal" +
                    if (compactHeight && location == null) " · Berlin Demo" else "",
                    fontSize = 12.sp, color = AstraTextMuted, modifier = Modifier.testTag("sky-fov"))
                if (fullscreen && !compactHeight && location == null) Text("Berlin Demo", fontSize = 11.sp, color = AstraTextMuted)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row {
            IconButton(onClick = { controlsExpanded = !controlsExpanded },
                modifier = Modifier.testTag("sky-controls-toggle").semantics {
                    stateDescription = if (controlsExpanded) "Ausgeklappt" else "Eingeklappt"
                }) {
                Icon(if (controlsExpanded) Icons.Rounded.Close else Icons.Rounded.Tune,
                    if (controlsExpanded) "Kartenbedienung einklappen" else "Kartenbedienung ausklappen")
            }
            IconButton(onClick = { controlsExpanded = false; setFullscreen(!fullscreen) },
                modifier = Modifier.testTag("sky-fullscreen-toggle")) {
                Icon(if (fullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                    if (fullscreen) "Vollbild beenden" else "Sternkarte im Vollbild")
            }
            }
            if (!compactHeight) Text(if (!skyTime.live) "Simulation" else if (arEnabled) "AR · Jetzt" else "Jetzt",
                fontSize = 11.sp, maxLines = 1, color = if (skyTime.live) AstraTextMuted else StarGold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
        Column(Modifier.fillMaxWidth().heightIn(max = controlsMaxHeight).testTag("sky-controls-panel")) {
        Column(Modifier.fillMaxWidth().weight(1f, fill = false).clipToBounds()
            .verticalScroll(controlsScrollState)) {
        Row(Modifier.fillMaxWidth().background(if (isOled) Color.Black else NightBlue).padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text((when {
                arEnabled -> "AR · Jetzt"
                skyTime.live -> "Jetzt"
                skyTime.rate == 0 -> "Simulation · Pause"
                else -> "Simulation · ${skyTime.rate}×"
            }) + " · " + DateTimeFormatter.ofPattern("dd.MM.yy HH:mm XXX", Locale.GERMAN)
                .withZone(displayZone).format(skyInstant), Modifier.weight(1f),
                color = if (skyTime.live) AstraTextMuted else StarGold, fontSize = 11.sp)
            if (!skyTime.live) TextButton(onClick = nowTime) { Text("Jetzt") }
        }
        if (controlsExpanded) {
            Column(Modifier.fillMaxWidth().background(if (isOled) Color.Black else NightBlue)
                .padding(horizontal = 12.dp).padding(top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { showSearch = true }) {
                        Icon(Icons.Rounded.Search, null, Modifier.size(18.dp)); Text(" Suchen")
                    }
                    if (!arEnabled) TextButton(onClick = { showTimeControls = true }) {
                        Icon(Icons.Rounded.Schedule, null, Modifier.size(18.dp)); Text(" Zeit")
                    }
                    TextButton(onClick = { showLayersPanel = true }) { Text("Ebenen & Namen") }
                    if (!arEnabled) TextButton(onClick = { showOpticsSheet = true }) {
                        Icon(Icons.Rounded.Explore, null, Modifier.size(18.dp)); Text(" Optik & FOV")
                    }
                    if (!arEnabled) TextButton(onClick = {
                        measurementState = if (measurementState.isActive) measurementState.close() else CelestialMeasurementState(isActive = true)
                    }) {
                        Icon(Icons.Rounded.Straighten, null, Modifier.size(18.dp))
                        Text(if (measurementState.isActive) " Messen ✓" else " Messen")
                    }
                    if (!arEnabled) TextButton(onClick = { showStarHopSheet = true }) {
                        Icon(Icons.AutoMirrored.Rounded.AltRoute, null, Modifier.size(18.dp))
                        Text(if (starHopSession.activeRoute != null) " Star-Hop (${starHopSession.activeStepIndex + 1}/${starHopSession.activeRoute!!.totalSteps})" else " Star-Hop")
                    }
                    TextButton(onClick = { showDeepSky = !showDeepSky },
                        modifier = Modifier.semantics { stateDescription = if (showDeepSky) "Ein" else "Aus" }) {
                        Text(if (showDeepSky) "Deep Sky ✓" else "Deep Sky")
                    }
                    TextButton(onClick = {
                        manualAzimuth = viewAzimuth; manualAltitude = viewAltitude
                        updateSelection(selection.release()); toggleAr()
                    }) {
                        Icon(if (arEnabled) Icons.Rounded.Map else Icons.Rounded.CameraAlt, null, Modifier.size(18.dp))
                        Text(if (arEnabled) " Karte" else " AR · Jetzt")
                    }
                    if (arEnabled) {
                        TextButton(onClick = { cameraExposureStep = (cameraExposureStep + 1) % 4 }) {
                            Icon(Icons.Rounded.CameraAlt, null, Modifier.size(18.dp))
                            Text(" Belichtung: +${cameraExposureStep} EV")
                        }
                        TextButton(onClick = {
                            pitchTrimDegrees = if (pitchTrimDegrees >= 15f) -15f else pitchTrimDegrees + 5f
                        }) {
                            Icon(Icons.Rounded.Tune, null, Modifier.size(18.dp))
                            Text(" Trimm: ${if (pitchTrimDegrees >= 0) "+" else ""}${pitchTrimDegrees.toInt()}°")
                        }
                        TextButton(onClick = { arDampingEnabled = !arDampingEnabled }) {
                            Icon(Icons.Rounded.Explore, null, Modifier.size(18.dp))
                            Text(if (arDampingEnabled) " Dämpfung ✓" else " Dämpfung")
                        }
                    }
                    if (!arEnabled) {
                        TextButton(onClick = {
                            updateSelection(selection.release())
                            manualAzimuth = orientation.azimuth; manualAltitude = orientation.altitude
                            manualFov = 95f
                        }) { Text("Ausrichten") }
                        TextButton(onClick = {
                            updateSelection(SkyTargetSelection()); targetMessage = null
                            manualAzimuth = 180f; manualAltitude = 35f; manualFov = 95f
                        }, modifier = Modifier.testTag("sky-reset")) {
                            Icon(Icons.Rounded.RestartAlt, null, Modifier.size(18.dp)); Text(" Ansicht zurücksetzen")
                        }
                        IconButton(onClick = {
                            manualAzimuth = viewAzimuth; manualAltitude = viewAltitude
                            updateSelection(selection.release()); manualFov = (manualFov / 1.25f).coerceAtLeast(0.5f)
                        }, enabled = manualFov > 0.5f) { Icon(Icons.Rounded.Add, "Sternkarte vergrößern") }
                        IconButton(onClick = {
                            manualAzimuth = viewAzimuth; manualAltitude = viewAltitude
                            updateSelection(selection.release()); manualFov = (manualFov * 1.25f).coerceAtMost(150f)
                        }, enabled = manualFov < 150f) { Icon(Icons.Rounded.Remove, "Sternkarte verkleinern") }
                    }
                    TextButton(onClick = { showCalibration = true }) { Text("Kalibrieren") }
                    TextButton(onClick = toggleRedLightMode) {
                        Text(if (redLightMode) "Rotlicht ausschalten" else "Rotlicht einschalten")
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(when (terrainState) {
                        TerrainState.Loading -> "Geländeprofil wird geladen …"
                        is TerrainState.Ready -> "Geländehorizont · GLO-90"
                        TerrainState.Unavailable -> "Flacher Horizont · Gelände offline"
                    }, color = AstraTextMuted, fontSize = 11.sp)
                    if (terrainState == TerrainState.Unavailable && SecureNetwork.available && SecureNetwork.options.terrain) {
                        Text(
                            "Erneut versuchen",
                            color = AstraBlue,
                            fontSize = 11.sp,
                            modifier = Modifier.clickable { terrainRetry++ }
                        )
                    }
                }
                if (!orientation.available && arEnabled) Text("Kein Richtungssensor – statische Ansicht",
                    color = StarGold, fontSize = 12.sp)
                if (!arEnabled) Text("Wischen zum Bewegen · Zwei Finger zum Zoomen · Zurücksetzen: Süden, 35° Höhe, 95° Sichtfeld",
                    color = AstraTextMuted, fontSize = 11.sp)
                if (location == null) {
                    Text("Ohne Standortfreigabe zeigt die Karte Berlin als Demo. Dein Standort richtet den Himmel auf dem Gerät aus.",
                        color = AstraTextMuted, fontSize = 12.sp)
                    if (compactLandscape) TextButton(onClick = { showLocationDialog = true }) {
                        Icon(Icons.Rounded.GpsFixed, null, Modifier.size(18.dp))
                        Text(" Standort verwenden")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (locationPermissionGranted) "Standort: GPS aktiv" else "Standort: Lokal gespeichert",
                                color = if (redLightMode) Color(0xFFFF7868) else StarGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Wird auf diesem Gerät gemerkt · Kein Cloud-Backup",
                                color = AstraTextMuted,
                                fontSize = 11.sp
                            )
                        }
                        TextButton(
                            onClick = onResetLocation,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (redLightMode) Color(0xFFFF7868) else StarGold
                            )
                        ) {
                            Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Auf Demo zurücksetzen", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        timeNotice?.let { notice ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(notice, Modifier.weight(1f), color = StarGold, fontSize = 12.sp)
                TextButton(onClick = clearTimeNotice) { Text("OK") }
            }
        }
        }
        if (controlsExpanded && location == null && !compactLandscape && !showLocationDialog) {
            Box(Modifier.fillMaxWidth().background(if (isOled) Color.Black else NightBlue)
                .padding(horizontal = 12.dp, vertical = 4.dp)) {
                PreciseLocationButton(redLightMode, onLocationPermissionResult)
            }
        }
        }

        Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().background(if (isOled) Color.Black else Color(0xFF02050A))
            .testTag("sky-viewport")
            .onSizeChanged { viewportSize = it }) {
            if (arEnabled && cameraPermissionGranted) CameraPreview(exposureStep = cameraExposureStep) { cameraFov = it }
            val shouldRenderTexture = firstMapFrameRendered && (!arEnabled || (appearance.showInAr && appearance.mode != MilkyWayMode.OFF))
            if (shouldRenderTexture) {
                SkyTextureLayer(textureState, Modifier.fillMaxSize(), retryTrigger = textureRetry, onStatus = { textureReady = it })
            }
            SkyCanvas(
                objects = visible,
                viewAzimuth = viewAzimuth.toDouble(),
                viewAltitude = viewAltitude.toDouble(),
                horizontalFov = if (arEnabled) cameraFov else manualFov.toDouble(),
                arMode = arEnabled,
                milkyWay = emptyList(),
                constellationBoundaries = if (showBoundaries) horizontalBoundaries else emptyList(),
                showConstellationIllustrations = showIllustrations,
                terrainProfile = (terrainState as? TerrainState.Ready)?.profile,
                gesturesEnabled = !arEnabled,
                onTransform = { panX, panY, zoom ->
                    if (selectionTracking) {
                        targetMessage = "Nachführen durch manuelle Bedienung beendet"
                        manualAzimuth = viewAzimuth
                        manualAltitude = viewAltitude
                        updateSelection(selection.release())
                    }
                    val fov = manualFov.toDouble()
                    manualAzimuth = normalizeDegrees(manualAzimuth - panX * fov).toFloat()
                    manualAltitude = (manualAltitude + panY * fov).coerceIn(-90.0, 90.0).toFloat()
                    manualFov = (fov / zoom).coerceIn(0.5, 150.0).toFloat()
                },
                onSelect = {
                    manualAzimuth = viewAzimuth
                    manualAltitude = viewAltitude
                    updateSelection(selection.select(SkySearchTarget(it.celestial)))
                    targetMessage = null
                    selectedCatalogId = it.celestial.catalogId
                },
                drawBackground = arEnabled || textureReady != true,
                showGrid = appearance.showGrid,
                targetPosition = targetPosition.takeUnless { selection.target?.needsDeepSky == true && !showDeepSky },
                targetLabel = selection.target?.name,
                labelDensity = appearance.labelDensity,
                opticsSettings = opticsSettings,
                redLightMode = redLightMode,
                onFirstFrameRendered = { firstMapFrameRendered = true },
                skyInstant = skyInstant,
                satellitePasses = satellitePasses,
                coordinateFrame = coordinateFrame,
                skyAppearance = appearance,
                measurementState = measurementState,
                onMeasurementPointSelected = { pt ->
                    measurementState = measurementState.selectPoint(pt)
                },
                starHopSession = starHopSession,
                loggedCatalogIds = loggedCatalogIds
            )
            if (!arEnabled && starHopSession.activeRoute != null) {
                StarHopHud(
                    session = starHopSession,
                    onPreviousStep = { starHopSession = starHopSession.previousStep() },
                    onNextStep = { starHopSession = starHopSession.nextStep() },
                    onCenterWaypoint = { coords, fov ->
                        val horiz = coordinateFrame.horizontal(coords.raHours, coords.decDegrees)
                        manualAzimuth = horiz.azimuth.toFloat()
                        manualAltitude = horiz.altitude.toFloat()
                        manualFov = fov.toFloat().coerceIn(0.5f, 150f)
                        updateSelection(selection.release())
                    },
                    onOpenSheet = { showStarHopSheet = true },
                    onDismissSession = { starHopSession = StarHopSessionState() },
                    redLightMode = redLightMode,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
            if (!arEnabled && measurementState.isActive && measurementState.isComplete) {
                val result = measurementState.result
                val orig = measurementState.origin
                val targ = measurementState.target
                if (result != null && orig != null && targ != null) {
                    val cardinalDir = when (((result.positionAngleDegrees + 22.5) / 45.0).toInt() % 8) {
                        0 -> "N"
                        1 -> "NO"
                        2 -> "O"
                        3 -> "SO"
                        4 -> "S"
                        5 -> "SW"
                        6 -> "W"
                        7 -> "NW"
                        else -> "N"
                    }
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = if (redLightMode) Color(0xEE220000) else Color(0xEE0C1322),
                        border = BorderStroke(1.dp, if (redLightMode) Color(0xFFFF5252).copy(alpha = 0.5f) else Color(0x334A688F)),
                        shadowElevation = 8.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.Straighten,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (redLightMode) Color(0xFFFF5252) else StarGold
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Winkelmessung",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (redLightMode) Color(0xFFFF5252) else Color.White
                                    )
                                }
                                IconButton(onClick = { measurementState = measurementState.close() }, modifier = Modifier.size(24.dp)) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Messung schließen",
                                        modifier = Modifier.size(16.dp),
                                        tint = if (redLightMode) Color(0xFFFF5252) else Color.White
                                    )
                                }
                            }

                            Text(
                                "${orig.displayName}  ➔  ${targ.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (redLightMode) Color(0xFFFF8888) else Color(0xFFB0C4DE),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        result.formattedDistance,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (redLightMode) Color(0xFFFF5252) else StarGold
                                    )
                                    Text(
                                        "${String.format(Locale.US, "%.4f°", result.angularDistanceDegrees)}  ·  PW ${String.format(Locale.US, "%.1f° (%s)", result.positionAngleDegrees, cardinalDir)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (redLightMode) Color(0xFFFF8888) else Color(0xFF94A3B8)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { measurementState = measurementState.swapDirection() },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("1 ↔ 2", fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = { measurementState = measurementState.reset() },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (redLightMode) Color(0xFFFF5252) else AstraBlue,
                                            contentColor = Color.Black
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Neu", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (!arEnabled && measurementState.isActive && !measurementState.isComplete) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (redLightMode) Color(0xEE220000) else Color(0xEE0C1322),
                    border = BorderStroke(1.dp, if (redLightMode) Color(0xFFFF5252).copy(alpha = 0.5f) else Color(0x334A688F)),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Straighten,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (redLightMode) Color(0xFFFF5252) else StarGold
                        )
                        Text(
                            text = if (measurementState.origin == null) "Punkt 1 (Start) am Himmel antippen"
                                   else "Punkt 2 (Ziel) am Himmel antippen",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (redLightMode) Color(0xFFFF8888) else Color.White
                        )
                        IconButton(onClick = { measurementState = measurementState.close() }, modifier = Modifier.size(20.dp)) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Messung abbrechen",
                                modifier = Modifier.size(14.dp),
                                tint = if (redLightMode) Color(0xFFFF5252) else Color.White
                            )
                        }
                    }
                }
            }
            if (!arEnabled && opticsSettings.isCustomized) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clickable { showOpticsSheet = true },
                    shape = RoundedCornerShape(8.dp),
                    color = if (redLightMode) Color(0xDD1A0000) else Color(0xDD0A1526),
                    border = BorderStroke(1.dp, if (redLightMode) Color(0xFFFF5252).copy(alpha = 0.6f) else Color(0xFF3B82F6).copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val statusParts = buildList {
                            val activeProfile = opticsProfiles.firstOrNull { it.id == opticsSettings.activeProfileId }
                            if (activeProfile != null) add(activeProfile.name)
                            if (opticsSettings.mirrored) add("Gespiegelt")
                            if (opticsSettings.rotationDegrees % 360f != 0f) add("${opticsSettings.rotationDegrees.toInt()}°")
                            if (opticsSettings.telradMode) add("Telrad")
                            else if (opticsSettings.fovCircleEnabled) add("FOV ${String.format(Locale.US, "%.1f°", opticsSettings.currentFovDegrees)}")
                        }
                        Text(
                            text = statusParts.joinToString(" · "),
                            color = if (redLightMode) Color(0xFFFF7868) else StarGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        TextButton(
                            onClick = {
                                val reset = opticsSettings.copy(
                                    mirrored = false,
                                    rotationDegrees = 0f,
                                    fovCircleEnabled = false,
                                    telradMode = false,
                                    activeProfileId = null
                                )
                                opticsSettings = reset
                                OpticsStore.saveSettings(context, reset)
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Rounded.RestartAlt, contentDescription = "Optik zurücksetzen", modifier = Modifier.size(14.dp), tint = if (redLightMode) Color(0xFFFF7868) else AstraBlue)
                            Spacer(Modifier.width(2.dp))
                            Text("Standard", fontSize = 11.sp, color = if (redLightMode) Color(0xFFFF7868) else AstraBlue)
                        }
                    }
                }
            }
            val currentTarget = selection.target
            val currentTargetPos = targetPosition
            if (currentTarget != null && currentTargetPos != null && (arEnabled || selection.tracking) &&
                viewportSize.width > 0 && viewportSize.height > 0) {
                val guidance = remember(
                    currentTarget,
                    currentTargetPos,
                    viewAzimuth,
                    viewAltitude,
                    arEnabled,
                    cameraFov,
                    manualFov,
                    viewportSize,
                    orientation.accuracy,
                    orientation.available,
                    terrainState
                ) {
                    ArTargetGuidanceCalculator.calculateGuidance(
                        targetName = currentTarget.name,
                        targetTypeLabel = currentTarget.typeLabel,
                        targetPosition = currentTargetPos,
                        currentAzimuth = viewAzimuth.toDouble(),
                        currentAltitude = viewAltitude.toDouble(),
                        horizontalFov = if (arEnabled) cameraFov else manualFov.toDouble(),
                        width = viewportSize.width.toFloat(),
                        height = viewportSize.height.toFloat(),
                        terrainProfile = (terrainState as? TerrainState.Ready)?.profile,
                        sensorAccuracy = orientation.accuracy,
                        sensorAvailable = orientation.available
                    )
                }
                ArTargetGuidanceOverlay(
                    guidance = guidance,
                    onOpenCalibration = { showCalibration = true },
                    onDismissTarget = {
                        updateSelection(selection.release())
                        targetMessage = null
                    },
                    redLightMode = redLightMode
                )
            }
        }

        selection.target?.let { target ->
            Column(Modifier.fillMaxWidth().heightIn(max = targetMaxHeight).verticalScroll(rememberScrollState())
                .background(NightBlue).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("${target.name} · ${target.typeLabel}", fontWeight = FontWeight.Bold)
                targetPosition?.let { position ->
                    Text("${targetVisibility(position, (terrainState as? TerrainState.Ready)?.profile).label} · Höhe ${position.altitude.format(1)}°",
                        fontSize = 12.sp, color = StarGold)
                }
                targetMessage?.let { Text(it, fontSize = 11.sp, color = AstraTextMuted) }
                if (target.needsDeepSky && !showDeepSky) Text("Deep-Sky-Ebene ausgeblendet", fontSize = 12.sp, color = StarGold)
                if (selection.tracking) Text("Nachführen aktiv · Wischen beendet es", fontSize = 12.sp, color = AstraBlue)
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    if (!arEnabled) TextButton(onClick = {
                        manualAzimuth = viewAzimuth
                        manualAltitude = viewAltitude
                        updateSelection(if (selection.tracking) selection.release() else selection.follow())
                        targetMessage = null
                    }) { Text(if (selection.tracking) "Nachführen stoppen" else "Nachführen") }
                    TextButton(onClick = { openTarget(target) }) { Text("Zentrieren") }
                    if (target.regionName == null) TextButton(onClick = {
                        selectedCatalogId = target.objectData.catalogId
                    }) { Text("Infos") }
                    TextButton(onClick = {
                        manualAzimuth = viewAzimuth
                        manualAltitude = viewAltitude
                        updateSelection(SkyTargetSelection())
                        targetMessage = null
                    }) { Text("Schließen") }
                }
            }
        }
        if (selection.target == null) targetMessage?.let { Text(it, Modifier.padding(12.dp), color = StarGold) }

    }
    }

    if (showLocationDialog) Dialog(onDismissRequest = { showLocationDialog = false }) {
        Surface(shape = RoundedCornerShape(20.dp), color = if (isOled) Color.Black else NightBlue) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Standort verwenden", fontWeight = FontWeight.Bold)
                PreciseLocationButton(redLightMode) { granted ->
                    showLocationDialog = false
                    onLocationPermissionResult(granted)
                }
                TextButton(onClick = { showLocationDialog = false }) { Text("Abbrechen") }
            }
        }
    }

    if (showLayersPanel) {
        SkySheetTheme(redLightMode) {
        ModalBottomSheet(onDismissRequest = { showLayersPanel = false }, containerColor = if (isOled) Color.Black else MaterialTheme.colorScheme.surface,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            contentColor = MaterialTheme.colorScheme.onSurface, scrimColor = Color.Black.copy(alpha = 0.7f)) {
            SkySheetSystemBars(redLightMode)
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Himmel & Ebenen", Modifier.weight(1f), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showLayersPanel = false }) {
                        Icon(Icons.Rounded.Close, "Ebenen schließen")
                    }
                }
                SkyAppearanceControls(appearance, { updated ->
                    val normalized = updated.normalized()
                    appearance = normalized
                    SkyAppearancePreferences.save(context, normalized)
                    if (normalized.oledBlackMode != oledBlackMode) {
                        setOledBlackMode(normalized.oledBlackMode)
                    }
                }, showBoundaries, { showBoundaries = it }, showIllustrations, { showIllustrations = it })
                if (textureReady == false) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Die Milchstraßentextur ist auf diesem Gerät gerade nicht verfügbar. Sterne und Objektinformationen bleiben nutzbar.",
                            color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                        TextButton(onClick = { textureRetry++ }) {
                            Text("Textur erneut laden")
                        }
                    }
                }
                Text("Milchstraßenhintergrund: NASA/Goddard SVS (Ernie Wright) · Gaia DR2: ESA/Gaia/DPAC. JPEG-Fassung: Wikimedia Commons / PantheraLeo1359531. Offline gebündelte Visualisierung aus Sterndaten, keine Kameraaufnahme.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
        }
    }

    if (showOpticsSheet) {
        OpticsFovSheet(
            settings = opticsSettings,
            profiles = opticsProfiles,
            onUpdateSettings = {
                opticsSettings = it
                OpticsStore.saveSettings(context, it)
            },
            onSaveProfile = { profile ->
                opticsProfiles = OpticsStore.saveProfile(context, profile)
            },
            onDeleteProfile = { id ->
                opticsProfiles = OpticsStore.deleteProfile(context, id)
                if (opticsSettings.activeProfileId == id) {
                    val updated = opticsSettings.copy(activeProfileId = null)
                    opticsSettings = updated
                    OpticsStore.saveSettings(context, updated)
                }
            },
            onDismiss = { showOpticsSheet = false },
            redLightMode = redLightMode,
            oledMode = LocalOledMode.current || appearance.oledBlackMode
        )
    }

    if (showTimeControls) SkyTimeSheet(skyTime, displayZone, redLightMode,
        onSelect = { selectTime(it); showTimeControls = false }, onRate = playTime,
        onPause = pauseTime, onNow = { nowTime(); showTimeControls = false },
        onDismiss = { showTimeControls = false })

    if (showSearch) SkySearchSheet(searchIndex, redLightMode, location == null,
        (terrainState as? TerrainState.Ready)?.profile, positionOf,
        timeLabel = (if (skyTime.live) "Jetzt · " else "Simulation · ") +
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm XXX").withZone(displayZone).format(skyInstant),
        dismiss = { showSearch = false }, open = openTarget,
        loading = !catalogs.complete && !catalogs.failed, failed = catalogs.failed,
        retry = { catalogRetry++ }, movingTargets = movingSearchTargets,
        loggedObjectIds = loggedCatalogIds)

    selectedObject?.let { original ->
        val target = SkySearchTarget(original)
        val item = VisibleObject(resolveTarget(target), positionOf(target))
        ModalBottomSheet(onDismissRequest = { selectedCatalogId = null }, containerColor = if (isOled) Color.Black else NightBlue) {
            TextButton(onClick = { openTarget(target) }) { Text("In Karte zentrieren") }
            ObjectDetails(
                item = item,
                observer = observer,
                skyInstant = skyInstant,
                simulated = !skyTime.live,
                terrain = (terrainState as? TerrainState.Ready)?.profile,
                isFavorite = item.celestial.catalogId in favoriteObjectIds,
                toggleFavorite = { toggleFavorite(item.celestial) },
                onAddLogEntry = { logEntryTarget = it },
                onOpenMoonDetail = {
                    showMoonDetailSheet = true
                    selectedCatalogId = null
                },
                onStartMeasurement = { obj ->
                    measurementState = CelestialMeasurementState(
                        isActive = true,
                        origin = MeasurementPoint.ObjectPoint(obj.celestial)
                    )
                    selectedCatalogId = null
                },
                onStartStarHop = { route ->
                    starHopSession = StarHopSessionState(activeRoute = route)
                    showStarHopSheet = true
                    selectedCatalogId = null
                },
                isObservedInLogbook = ObservationCatalogMatcher.isObserved(item.celestial.catalogId, loggedCatalogIds)
            )
        }
    }
    if (showMoonDetailSheet) {
        MoonDetailSheet(
            skyInstant = skyInstant,
            onDismissRequest = { showMoonDetailSheet = false },
            onLogFeature = { celestial ->
                logEntryTarget = celestial
                showMoonDetailSheet = false
            },
            oledMode = LocalOledMode.current || appearance.oledBlackMode
        )
    }
    if (showStarHopSheet) {
        StarHopSheet(
            session = starHopSession,
            onSessionChange = { starHopSession = it },
            onCenterWaypoint = { coords, fov ->
                val horiz = coordinateFrame.horizontal(coords.raHours, coords.decDegrees)
                manualAzimuth = horiz.azimuth.toFloat()
                manualAltitude = horiz.altitude.toFloat()
                manualFov = fov.toFloat().coerceIn(0.5f, 150f)
                updateSelection(selection.release())
            },
            onDismissRequest = { showStarHopSheet = false },
            onAddLogEntry = { catalogId ->
                val targetObj = (stars + deepSkyObjects + solarSystem).firstOrNull {
                    it.catalogId == catalogId || it.catalogId.contains(catalogId)
                } ?: CelestialObject(name = catalogId, catalogId = catalogId, raHours = 0.0, decDegrees = 0.0, magnitude = 0.0, distanceLightYears = 0.0, spectralClass = "")
                logEntryTarget = targetObj
                showStarHopSheet = false
            },
            redLightMode = redLightMode,
            oledMode = LocalOledMode.current || appearance.oledBlackMode
        )
    }
    if (showCalibration) {
        ModalBottomSheet(onDismissRequest = { showCalibration = false }, containerColor = if (isOled) Color.Black else NightBlue) {
            CalibrationGuide(orientation) { showCalibration = false }
        }
    }
    logEntryTarget?.let { target ->
        ObservationLogEntryDialog(
            initialObject = target,
            currentLocation = location,
            onDismiss = { logEntryTarget = null },
            onSave = { entry ->
                onSaveLogEntry(entry)
                logEntryTarget = null
            }
        )
    }
}

@Composable
private fun PreciseLocationButton(redLightMode: Boolean, onPermissionResult: (Boolean) -> Unit) {
    LocationButton(onPermissionResult = onPermissionResult,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        textType = LocationButtonTextType.UsePreciseLocation,
        backgroundColor = if (redLightMode) Color(0xFF5A0000) else AstraBlue,
        textColor = if (redLightMode) Color(0xFFFF7868) else Night,
        iconTint = if (redLightMode) Color(0xFFFF7868) else Night,
        cornerRadius = 20.dp, pressedCornerRadius = 12.dp)
}

@Composable
private fun CalibrationGuide(orientation: OrientationState, close: () -> Unit) {
    val (quality, qualityColor) = when (orientation.accuracy) {
        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "Sehr gut" to Color(0xFF76E0A0)
        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Brauchbar" to AstraBlue
        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "Niedrig" to StarGold
        else -> "Noch unzuverlässig" to Color(0xFFFF9C8F)
    }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Kompass & AR kalibrieren", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Sensorqualität: $quality", color = qualityColor, fontWeight = FontWeight.Bold)
        CalibrationStep("1", "Entferne das Handy von Magneten, Lautsprechern, Metallflächen und Stromkabeln.")
        CalibrationStep("2", "Bewege das Handy mehrmals langsam in einer großen Acht – jeweils um alle drei Achsen.")
        CalibrationStep("3", "Halte es anschließend aufrecht und drehe dich einmal langsam um 360°.")
        CalibrationStep("4", "Vergleiche Norden mit einer bekannten Richtung. Wiederhole die Acht, falls die Anzeige springt.")
        Text(
            "Die Genauigkeitsanzeige stammt direkt vom Android-Richtungssensor. GPS allein bestimmt keine Blickrichtung.",
            color = Color(0xFFAAB8CE),
            fontSize = 13.sp
        )
        Button(onClick = close, modifier = Modifier.fillMaxWidth()) { Text("Fertig") }
    }
}

@Composable
private fun CalibrationStep(number: String, text: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(34.dp).background(AstraBlue.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) { Text(number, color = AstraBlue, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(12.dp))
        Text(text, modifier = Modifier.weight(1f), color = Color(0xFFD8E4F7))
    }
}

@androidx.annotation.OptIn(markerClass = [androidx.camera.camera2.interop.ExperimentalCamera2Interop::class])
@Composable
private fun CameraPreview(exposureStep: Int = 0, onHorizontalFov: (Double) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnHorizontalFov by rememberUpdatedState(onHorizontalFov)
    val latestExposureStep by rememberUpdatedState(exposureStep)
    val previewView = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

    var activeCamera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    LaunchedEffect(latestExposureStep, activeCamera) {
        activeCamera?.let { camera ->
            runCatching {
                val expController = CameraExposureController()
                expController.setStep(latestExposureStep)
                expController.applyToCamera(camera)
            }
        }
    }

    DisposableEffect(lifecycleOwner, previewView) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        var disposed = false
        var preview: Preview? = null
        var boundCharacteristics: CameraCharacteristics? = null
        var focalLength: Float? = null
        var lastReportedFov: Double? = null
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val lastCaptureFocalLength = java.util.concurrent.atomic.AtomicReference<Float?>(null)
        val reportFieldOfView: () -> Unit = report@{
            if (disposed || previewView.width <= 0 || previewView.height <= 0) return@report
            // Keep dimensions in the same bound-camera coordinate system as CameraX's matrix.
            val characteristics = boundCharacteristics ?: return@report
            val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) ?: return@report
            val pixelSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE) ?: return@report
            val focal = focalLength ?: characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                ?.firstOrNull() ?: return@report
            // CameraX owns this transform: do not assume that sensor height means portrait width.
            val sensorToView = previewView.sensorToViewTransform ?: return@report
            val viewToSensor = android.graphics.Matrix()
            if (!sensorToView.invert(viewToSensor)) return@report
            val edges = floatArrayOf(0f, previewView.height / 2f,
                previewView.width.toFloat(), previewView.height / 2f)
            viewToSensor.mapPoints(edges)
            val fov = cameraHorizontalFovDegrees(
                (edges[2] - edges[0]).toDouble(), (edges[3] - edges[1]).toDouble(),
                sensorSize.width.toDouble(), sensorSize.height.toDouble(),
                pixelSize.width, pixelSize.height, focal.toDouble()
            ) ?: return@report
            if (lastReportedFov == null || kotlin.math.abs(fov - lastReportedFov!!) > 0.05) {
                lastReportedFov = fov
                latestOnHorizontalFov(fov)
            }
        }
        val layoutListener = android.view.View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            previewView.display?.let { preview?.targetRotation = it.rotation }
            previewView.post { reportFieldOfView() }
        }
        val streamObserver = androidx.lifecycle.Observer<PreviewView.StreamState> {
            if (it == PreviewView.StreamState.STREAMING) reportFieldOfView()
        }
        previewView.addOnLayoutChangeListener(layoutListener)
        previewView.previewStreamState.observe(lifecycleOwner, streamObserver)
        providerFuture.addListener({
            if (!disposed) {
                runCatching {
                    provider = providerFuture.get().also { cameraProvider ->
                        provider = cameraProvider
                        val builder = Preview.Builder()
                        previewView.display?.let { builder.setTargetRotation(it.rotation) }
                        // Only optical metadata is read; no camera frame is copied or stored.
                        androidx.camera.camera2.interop.Camera2Interop.Extender(builder)
                            .setSessionCaptureCallback(object : android.hardware.camera2.CameraCaptureSession.CaptureCallback() {
                                override fun onCaptureCompleted(
                                    session: android.hardware.camera2.CameraCaptureSession,
                                    request: android.hardware.camera2.CaptureRequest,
                                    result: android.hardware.camera2.TotalCaptureResult
                                ) {
                                    val measuredFocalLength = result.get(android.hardware.camera2.CaptureResult.LENS_FOCAL_LENGTH)
                                    if (lastCaptureFocalLength.getAndSet(measuredFocalLength) == measuredFocalLength) return
                                    previewView.post {
                                        if (!disposed) {
                                            focalLength = measuredFocalLength
                                            reportFieldOfView()
                                        }
                                    }
                                }
                            })
                        val boundPreview = builder.build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        preview = boundPreview
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            boundPreview
                        )
                        activeCamera = camera
                        runCatching {
                            val expController = CameraExposureController()
                            expController.setStep(latestExposureStep)
                            expController.applyToCamera(camera)
                        }
                        boundCharacteristics = runCatching {
                            val cameraInfo = androidx.camera.camera2.interop.Camera2CameraInfo.from(camera.cameraInfo)
                            manager.getCameraCharacteristics(cameraInfo.cameraId)
                        }.getOrNull()
                        previewView.post { reportFieldOfView() }
                    }
                }
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            disposed = true
            activeCamera = null
            previewView.removeOnLayoutChangeListener(layoutListener)
            previewView.previewStreamState.removeObserver(streamObserver)
            preview?.let { provider?.unbind(it) }
        }
    }
}

@Composable
private fun rememberCameraHorizontalFov(): Double {
    val context = LocalContext.current
    return remember {
        runCatching {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = manager.cameraIdList.first { id ->
                manager.getCameraCharacteristics(id)[CameraCharacteristics.LENS_FACING] ==
                    CameraCharacteristics.LENS_FACING_BACK
            }
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val sensorSize = characteristics[CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE]
                ?: error("Sensorgröße nicht verfügbar")
            val focalLength = characteristics[CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS]
                ?.firstOrNull() ?: error("Brennweite nicht verfügbar")
            Math.toDegrees(2.0 * atan(sensorSize.height / (2.0 * focalLength)))
                .coerceIn(35.0, 80.0)
        }.getOrDefault(55.0)
    }
}

@Composable
internal fun SkyCanvas(
    objects: List<VisibleObject>,
    viewAzimuth: Double,
    viewAltitude: Double,
    horizontalFov: Double,
    arMode: Boolean,
    milkyWay: List<HorizontalCoordinates>,
    constellationBoundaries: List<HorizontalConstellationBoundary>,
    showConstellationIllustrations: Boolean,
    terrainProfile: TerrainProfile?,
    gesturesEnabled: Boolean,
    onTransform: (panX: Double, panY: Double, zoom: Double) -> Unit,
    onSelect: (VisibleObject) -> Unit,
    modifier: Modifier = Modifier,
    drawBackground: Boolean = true,
    showGrid: Boolean = false,
    targetPosition: HorizontalCoordinates? = null,
    labelDensity: SkyLabelDensity = SkyLabelDensity.NORMAL,
    targetLabel: String? = null,
    opticsSettings: OpticsSettings = OpticsSettings(),
    redLightMode: Boolean = false,
    onFirstFrameRendered: () -> Unit = {},
    skyInstant: Instant = Instant.now(),
    satellitePasses: List<SatellitePass> = emptyList(),
    coordinateFrame: SkyCoordinateFrame? = null,
    skyAppearance: SkyAppearance = SkyAppearance(),
    measurementState: CelestialMeasurementState = CelestialMeasurementState(),
    onMeasurementPointSelected: (MeasurementPoint) -> Unit = {},
    starHopSession: StarHopSessionState = StarHopSessionState(),
    loggedCatalogIds: Set<String> = emptySet()
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var firstFrameReported by remember { mutableStateOf(false) }
    val currentOnTransform by rememberUpdatedState(onTransform)
    val currentAzimuth by rememberUpdatedState(viewAzimuth)
    val currentAltitude by rememberUpdatedState(viewAltitude)
    val currentFov by rememberUpdatedState(horizontalFov)
    val currentMeasurementState by rememberUpdatedState(measurementState)
    val currentOnMeasurementPointSelected by rememberUpdatedState(onMeasurementPointSelected)
    val currentCoordinateFrame by rememberUpdatedState(coordinateFrame)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val isOled = skyAppearance.oledBlackMode || LocalOledMode.current
    val objectsByHip = remember(objects) { objects.mapNotNull { item -> item.celestial.hipId?.let { it to item } }.toMap() }
    val preparedObjects = remember(objects) { objects.map { it to PreparedSkyPosition(it.position) } }
    Canvas(
        modifier.fillMaxSize().clipToBounds()
            .onSizeChanged { canvasSize = it }
            .pointerInput(objects, canvasSize, arMode, opticsSettings, measurementState.isActive) {
                detectTapGestures { tap ->
                    val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
                    val effectiveTap = if (!arMode && opticsSettings.isCustomized) {
                        opticsSettings.inverseTransformScreenPoint(tap, center)
                    } else {
                        tap
                    }
                    val projection = SkyProjection(currentAzimuth, currentAltitude,
                        canvasSize.width.toFloat(), canvasSize.height.toFloat(), currentFov,
                        perspective = !arMode,
                        clipPadding = if (!arMode && opticsSettings.isCustomized)
                            hypot(canvasSize.width.toDouble(), canvasSize.height.toDouble()).toFloat() else 0f)
                    val closest = objects.mapNotNull { item ->
                        projection.point(item.position, padding = 32f)
                            ?.let { point -> item to hypot((point.x - effectiveTap.x).toDouble(), (point.y - effectiveTap.y).toDouble()) }
                    }.minByOrNull { it.second }
                    if (currentMeasurementState.isActive) {
                        if (closest != null && closest.second <= 42.0) {
                            currentOnMeasurementPointSelected(MeasurementPoint.ObjectPoint(closest.first.celestial))
                        } else {
                            val horiz = projection.coordinates(effectiveTap)
                            val frame = currentCoordinateFrame
                            if (horiz != null && frame != null) {
                                val eq = frame.toEquatorial(horiz)
                                currentOnMeasurementPointSelected(MeasurementPoint.CoordinatePoint(eq, horiz))
                            }
                        }
                    } else {
                        if (closest != null && closest.second <= 42.0) currentOnSelect(closest.first)
                    }
                }
            }
            .pointerInput(gesturesEnabled, canvasSize, arMode, opticsSettings) {
                if (!gesturesEnabled) return@pointerInput
                detectTransformGestures { _, rawPan, zoom, _ ->
                    if (canvasSize.width <= 0 || canvasSize.height <= 0) return@detectTransformGestures
                    val pan = if (!arMode && opticsSettings.isCustomized) {
                        opticsSettings.transformPanDelta(rawPan)
                    } else {
                        rawPan
                    }
                    // Apply every touch delta to the parent's current state; recomposition may lag input.
                    currentOnTransform(pan.x.toDouble() / canvasSize.width, pan.y.toDouble() / canvasSize.width,
                        zoom.toDouble())
                }
            }
            .then(
                if (!drawBackground) Modifier
                else if (arMode) Modifier.background(Color.Black.copy(alpha = 0.28f))
                else Modifier.background(if (isOled) Color.Black else Color(0xFF03070D))
            )
    ) {
        if (!firstFrameReported && size.width > 0f && size.height > 0f) {
            firstFrameReported = true
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onFirstFrameRendered()
            }
        }
        val projection = SkyProjection(viewAzimuth, viewAltitude, size.width, size.height, horizontalFov,
            perspective = !arMode,
            clipPadding = if (!arMode && opticsSettings.isCustomized) hypot(size.width.toDouble(), size.height.toDouble()).toFloat() else 0f)
        val opticsPadding = 32f
        val canvasCenter = Offset(size.width / 2f, size.height / 2f)
        val applyOptics = !arMode && opticsSettings.isCustomized
        val referenceLines = if (coordinateFrame != null &&
            (skyAppearance.showCelestialEquator || skyAppearance.showEcliptic || skyAppearance.showGalacticEquator)) {
            SkyGridRenderer.generateReferenceLines()
        } else null
        val eclipticLine = if (skyAppearance.showEcliptic && coordinateFrame != null) {
            referenceLines?.ecliptic?.let { SkyGridRenderer.toHorizontal(it, coordinateFrame) }
        } else null

        val labels = mutableListOf<SkyLabelCandidate>()
        val labelPaints = mutableMapOf<String, android.graphics.Paint>()
        fun addLabel(
            id: String, text: String, point: Offset, kind: SkyLabelKind,
            textSize: Float, color: Int, alpha: Int = 225, rank: Double = 0.0,
            anchorGap: Float = 6.dp.toPx()
        ) {
            val anchor = if (applyOptics) opticsSettings.transformScreenPoint(point, canvasCenter) else point
            if (!projection.contains(anchor)) return
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                this.textSize = textSize
                this.color = color
                this.alpha = alpha
                setShadowLayer(1.dp.toPx(), 0f, 0f, android.graphics.Color.BLACK)
            }
            val metrics = paint.fontMetrics
            labels += SkyLabelCandidate(id, text, anchor.x, anchor.y, kind, textSize,
                metrics.top, metrics.bottom, anchorGap, rank)
            labelPaints[id] = paint
        }

        val withOpticsTransform: (DrawScope.() -> Unit) -> Unit = { block ->
            if (applyOptics) {
                withTransform({
                    if (opticsSettings.rotationDegrees % 360f != 0f) {
                        rotate(opticsSettings.rotationDegrees, canvasCenter)
                    }
                    if (opticsSettings.mirrored) {
                        scale(-1f, 1f, canvasCenter)
                    }
                }) {
                    block()
                }
            } else {
                block()
            }
        }

        // Collect labels
        milkyWay.asSequence().filterIndexed { index, _ -> index % 20 == 0 }
            .filter { targetVisibility(it, terrainProfile) == TargetVisibility.ABOVE }
            .mapNotNull { projection.point(it, padding = opticsPadding) }.firstOrNull()?.let { point ->
                addLabel("milky-way", "MILCHSTRASSE", point, SkyLabelKind.MILKY_WAY,
                    10.sp.toPx(), android.graphics.Color.rgb(184, 210, 255), 180)
            }

        val projected = preparedObjects.mapNotNull { (item, direction) ->
            // Manual ground is opaque; skip objects safely below it before the costly projection.
            // AR keeps them because its camera horizon is translucent.
            if (!arMode && item.position.altitude <= -5.0) return@mapNotNull null
            projection.point(direction, padding = opticsPadding)?.let { point ->
                if (!applyOptics || projection.contains(opticsSettings.transformScreenPoint(point, canvasCenter), 32f)) item to point else null
            }
        }
        val byHip = projected.mapNotNull { (item, point) -> item.celestial.hipId?.let { it to point } }.toMap()

        ConstellationLines.labels.forEach { label ->
            byHip[label.anchorHip]?.let { point ->
                val position = objectsByHip[label.anchorHip]?.position
                if (position != null && targetVisibility(position, terrainProfile) == TargetVisibility.ABOVE) {
                    addLabel("constellation-${label.name}", label.name.uppercase(Locale.GERMAN), point,
                        SkyLabelKind.CONSTELLATION, 10.sp.toPx(), android.graphics.Color.rgb(109, 168, 255),
                        if (arMode) 220 else 160, anchorGap = 12.dp.toPx())
                }
            }
        }

        projected.forEach { (item, point) ->
            val objectData = item.celestial
            val shouldLabel = when (objectData.objectType) {
                CelestialType.STAR -> objectData.magnitude < (if (labelDensity == SkyLabelDensity.RICH) 4.0 else 1.5) &&
                    !objectData.name.startsWith("HIP ")
                CelestialType.SUN, CelestialType.MOON, CelestialType.PLANET -> true
                else -> objectData.messierId.isNotBlank() || objectData.magnitude <
                    (if (labelDensity == SkyLabelDensity.RICH) 10.0 else 7.0)
            }
            if (shouldLabel && targetVisibility(item.position, terrainProfile) == TargetVisibility.ABOVE &&
                !(targetPosition != null && objectData.name == targetLabel)) {
                val kind = when (objectData.objectType) {
                    CelestialType.SUN, CelestialType.MOON, CelestialType.PLANET -> SkyLabelKind.SOLAR_SYSTEM
                    CelestialType.STAR -> if (objectData.magnitude < 1.5) SkyLabelKind.BRIGHT_STAR else SkyLabelKind.STAR
                    else -> SkyLabelKind.DEEP_SKY
                }
                addLabel("object-${objectData.catalogId}", objectData.name, point, kind,
                    12.sp.toPx(), android.graphics.Color.WHITE, rank = objectData.magnitude)
            }
        }

        eclipticLine?.horizontalPoints?.asSequence()
            ?.filter { targetVisibility(it, terrainProfile) == TargetVisibility.ABOVE }
            ?.mapNotNull { projection.point(it) }
            ?.minByOrNull { point -> (point - canvasCenter).getDistance() }
            ?.let { point ->
                addLabel("ecliptic", "EKLIPTIK", point, SkyLabelKind.ORIENTATION,
                    10.sp.toPx(), if (redLightMode) android.graphics.Color.rgb(255, 120, 104)
                    else android.graphics.Color.rgb(255, 202, 40), rank = 2.0)
            }
        satellitePasses.forEach { pass ->
            pass.trackPoints.asSequence()
                .filter { targetVisibility(it.position, terrainProfile) == TargetVisibility.ABOVE }
                .mapNotNull { projection.point(it.position) }
                .minByOrNull { point -> (point - canvasCenter).getDistance() }
                ?.let { point ->
                    addLabel("satellite-${pass.noradId}-${pass.riseTime.epochSecond}",
                        "SAT · ${pass.satelliteName.substringBefore(" (")} · max. ${formatEventTime(pass.maxElevationTime)}",
                        point, SkyLabelKind.SOLAR_SYSTEM, 11.sp.toPx(),
                        if (redLightMode) android.graphics.Color.rgb(255, 120, 104)
                        else android.graphics.Color.rgb(255, 217, 138),
                        rank = (pass.riseTime.epochSecond - skyInstant.epochSecond).toDouble(),
                        anchorGap = 9.dp.toPx())
                }
        }

        val horizonPoints = (0..120).mapNotNull { step ->
            val azimuth = step * 3.0
            projection.point(HorizontalCoordinates(azimuth, terrainProfile?.altitudeAt(azimuth) ?: 0.0), padding = opticsPadding)
        }
        val orientationColor = android.graphics.Color.rgb(255, 217, 138)
        horizonPoints.minByOrNull { it.x }?.let { point ->
            addLabel("horizon", if (terrainProfile == null) "HORIZONT" else "GELÄNDEHORIZONT", point,
                SkyLabelKind.ORIENTATION, 10.sp.toPx(), orientationColor, rank = 1.0)
        }
        listOf(0.0 to "N", 90.0 to "O", 180.0 to "S", 270.0 to "W").forEach { (azimuth, name) ->
            projection.point(HorizontalCoordinates(azimuth, terrainProfile?.altitudeAt(azimuth) ?: 0.0), padding = opticsPadding)?.let { point ->
                addLabel("direction-$name", name, point, SkyLabelKind.ORIENTATION, 13.sp.toPx(), orientationColor)
            }
        }
        val selectedPoint = targetPosition?.takeIf {
            targetVisibility(it, terrainProfile) == TargetVisibility.ABOVE
        }?.let { projection.point(it, padding = opticsPadding) }
        if (selectedPoint != null && !targetLabel.isNullOrBlank()) {
            addLabel("selected-target", targetLabel, selectedPoint, SkyLabelKind.TARGET,
                13.sp.toPx(), orientationColor, anchorGap = 22.dp.toPx())
        }

        val placedLabels = layoutSkyLabels(labels, size.width, size.height, labelDensity,
            padding = 8.dp.toPx(), separation = 5.dp.toPx(),
            measureText = { label, text -> labelPaints.getValue(label.id).measureText(text) },
            canPlace = { label, bounds ->
                (label.kind == SkyLabelKind.ORIENTATION && label.id != "ecliptic") ||
                    listOf(bounds.left, (bounds.left + bounds.right) / 2f, bounds.right).all { x ->
                        listOf(bounds.top, bounds.bottom).all { y ->
                            val pt = if (applyOptics) opticsSettings.inverseTransformScreenPoint(Offset(x, y), canvasCenter) else Offset(x, y)
                            projection.coordinates(pt)?.let {
                                targetVisibility(it, terrainProfile) == TargetVisibility.ABOVE
                            } == true
                        }
                    }
            })

        // Draw celestial graphics (mirrored/rotated when active)
        withOpticsTransform {
            drawMilkyWay(milkyWay, projection)

            // Astronomical grids and reference lines
            if (showGrid && !skyAppearance.showHorizontalGrid && !skyAppearance.showEquatorialGrid) {
                drawSkyGrid(viewAzimuth, viewAltitude)
            }
            if (skyAppearance.showHorizontalGrid) {
                val lod = SkyGridRenderer.getHorizontalLod(horizontalFov)
                val lines = SkyGridRenderer.generateHorizontalGrid(lod.stepHoursOrAzDegrees, lod.stepDecOrAltDegrees)
                lines.forEach { line ->
                    val style = SkyGridRenderer.getLineStyle(line.type, line.isPrimary, redLightMode)
                    SkyGridRenderer.renderGridLine(this, line, projection, style)
                }
            }
            if (skyAppearance.showEquatorialGrid && coordinateFrame != null) {
                val lod = SkyGridRenderer.getEquatorialLod(horizontalFov)
                val lines = SkyGridRenderer.generateEquatorialGrid(lod.stepHoursOrAzDegrees, lod.stepDecOrAltDegrees)
                val horizLines = SkyGridRenderer.toHorizontal(lines, coordinateFrame)
                horizLines.forEach { line ->
                    val style = SkyGridRenderer.getLineStyle(line.type, line.isPrimary, redLightMode)
                    SkyGridRenderer.renderGridLine(this, line, projection, style)
                }
            }
            if (referenceLines != null && coordinateFrame != null) {
                val ref = referenceLines
                if (skyAppearance.showCelestialEquator) {
                    val horiz = SkyGridRenderer.toHorizontal(ref.celestialEquator, coordinateFrame)
                    val style = SkyGridRenderer.getLineStyle(horiz.type, horiz.isPrimary, redLightMode)
                    SkyGridRenderer.renderGridLine(this, horiz, projection, style)
                }
                if (eclipticLine != null) {
                    val style = SkyGridRenderer.getLineStyle(eclipticLine.type, eclipticLine.isPrimary, redLightMode)
                    SkyGridRenderer.renderGridLine(this, eclipticLine, projection, style)
                }
                if (skyAppearance.showGalacticEquator) {
                    val horiz = SkyGridRenderer.toHorizontal(ref.galacticEquator, coordinateFrame)
                    val style = SkyGridRenderer.getLineStyle(horiz.type, horiz.isPrimary, redLightMode)
                    SkyGridRenderer.renderGridLine(this, horiz, projection, style)
                }
            }

            drawIauBoundaries(constellationBoundaries, projection, arMode)

            satellitePasses.forEach { pass ->
                drawSatellitePassTrack(pass, projection, skyInstant, arMode)
            }

            ConstellationLines.connections.forEach { (from, to) ->
                val start = objectsByHip[from]?.position
                val end = objectsByHip[to]?.position
                if (start != null && end != null) {
                    projection.segments(start, end, padding = opticsPadding).forEach { segment ->
                        drawLine(AstraBlue.copy(alpha = if (arMode) 0.65f else 0.23f), segment.start, segment.end, if (arMode) 2f else 1.1f)
                    }
                }
            }
            ConstellationLines.labels.forEach { label ->
                byHip[label.anchorHip]?.let { point ->
                    if (showConstellationIllustrations) drawConstellationIllustration(label.name, point, arMode)
                }
            }

            // Star-Hop active route visualization
            val activeRoute = starHopSession.activeRoute
            if (activeRoute != null && activeRoute.steps.isNotEmpty() && coordinateFrame != null) {
                val stepHorizs = activeRoute.steps.map { step ->
                    coordinateFrame.horizontal(step.fieldCenterCoords.raHours, step.fieldCenterCoords.decDegrees)
                }
                stepHorizs.zipWithNext().forEach { (from, to) ->
                    projection.segments(from, to, padding = opticsPadding).forEach { segment ->
                        drawLine(
                            color = if (redLightMode) Color(0xFFFF5252).copy(alpha = 0.75f) else Color(0xFF64B5F6).copy(alpha = 0.65f),
                            start = segment.start,
                            end = segment.end,
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                        )
                    }
                }
                stepHorizs.forEachIndexed { idx, horiz ->
                    projection.point(horiz, padding = opticsPadding)?.let { pt ->
                        val isActive = idx == starHopSession.activeStepIndex
                        val isCompleted = activeRoute.steps[idx].isCompleted
                        val waypointColor = when {
                            isActive -> if (redLightMode) Color(0xFFFF1744) else StarGold
                            isCompleted -> if (redLightMode) Color(0xFFB71C1C) else AstraSuccess
                            else -> if (redLightMode) Color(0xFFEF5350).copy(alpha = 0.6f) else Color(0xFF90CAF9).copy(alpha = 0.6f)
                        }
                        drawCircle(
                            color = waypointColor,
                            radius = if (isActive) 12.dp.toPx() else 8.dp.toPx(),
                            center = pt,
                            style = Stroke(width = if (isActive) 2.5.dp.toPx() else 1.5.dp.toPx())
                        )
                        drawCircle(
                            color = waypointColor.copy(alpha = if (isActive) 0.35f else 0.15f),
                            radius = if (isActive) 12.dp.toPx() else 8.dp.toPx(),
                            center = pt
                        )
                        if (isActive) {
                            val rOuter = 20.dp.toPx()
                            drawCircle(
                                color = waypointColor.copy(alpha = 0.6f),
                                radius = rOuter,
                                center = pt,
                                style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                            )
                            val crossLen = 6.dp.toPx()
                            drawLine(waypointColor, pt - Offset(0f, rOuter + crossLen), pt - Offset(0f, rOuter - crossLen), 1.5.dp.toPx())
                            drawLine(waypointColor, pt + Offset(0f, rOuter - crossLen), pt + Offset(0f, rOuter + crossLen), 1.5.dp.toPx())
                            drawLine(waypointColor, pt - Offset(rOuter + crossLen, 0f), pt - Offset(rOuter - crossLen, 0f), 1.5.dp.toPx())
                            drawLine(waypointColor, pt + Offset(rOuter - crossLen, 0f), pt + Offset(rOuter + crossLen, 0f), 1.5.dp.toPx())
                        }
                        val stepPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            textSize = 10.sp.toPx()
                            isFakeBoldText = true
                            color = if (redLightMode) android.graphics.Color.rgb(255, 82, 82) else android.graphics.Color.WHITE
                            setShadowLayer(2.dp.toPx(), 0f, 0f, android.graphics.Color.BLACK)
                        }
                        val stepNumStr = "${idx + 1}"
                        val textW = stepPaint.measureText(stepNumStr)
                        drawContext.canvas.nativeCanvas.drawText(
                            stepNumStr,
                            pt.x - textW / 2f,
                            pt.y + 3.5.dp.toPx(),
                            stepPaint
                        )
                    }
                }
            }

            // Celestial measurement visualization
            if (measurementState.isActive && coordinateFrame != null) {
                val originPt = measurementState.origin
                val targetPt = measurementState.target
                val measureColor = if (redLightMode) Color(0xFFFF5252) else Color(0xFF00E5FF)
                val textMeasureColor = if (redLightMode) android.graphics.Color.rgb(255, 82, 82) else android.graphics.Color.rgb(0, 229, 255)

                if (originPt != null) {
                    val origHoriz = coordinateFrame.horizontal(originPt.equatorial.raHours, originPt.equatorial.decDegrees)
                    projection.point(origHoriz, padding = opticsPadding)?.let { pt ->
                        drawCircle(measureColor, 7.dp.toPx(), pt, style = Stroke(2.dp.toPx()))
                        drawCircle(measureColor, 2.5.dp.toPx(), pt)
                    }
                }

                if (targetPt != null) {
                    val targHoriz = coordinateFrame.horizontal(targetPt.equatorial.raHours, targetPt.equatorial.decDegrees)
                    projection.point(targHoriz, padding = opticsPadding)?.let { pt ->
                        drawCircle(measureColor, 7.dp.toPx(), pt, style = Stroke(2.dp.toPx()))
                        drawCircle(measureColor, 2.5.dp.toPx(), pt)
                    }
                }

                if (originPt != null && targetPt != null) {
                    val arc = CelestialMeasurement.interpolateGreatCircle(originPt.equatorial, targetPt.equatorial, numSegments = 24)
                    val arcHoriz = arc.map { coordinateFrame.horizontal(it.raHours, it.decDegrees) }
                    arcHoriz.zipWithNext().forEach { (from, to) ->
                        projection.segments(from, to, padding = opticsPadding).forEach { seg ->
                            drawLine(
                                color = measureColor,
                                start = seg.start,
                                end = seg.end,
                                strokeWidth = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                            )
                        }
                    }

                    val midEq = CelestialMeasurement.interpolateMidpoint(originPt.equatorial, targetPt.equatorial)
                    val midHoriz = coordinateFrame.horizontal(midEq.raHours, midEq.decDegrees)
                    projection.point(midHoriz, padding = opticsPadding)?.let { midPt ->
                        val result = measurementState.result
                        if (result != null) {
                            val badgeText = "${result.formattedDistance} (${String.format(Locale.US, "%.1f°", result.positionAngleDegrees)})"
                            val badgePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                textSize = 11.sp.toPx()
                                color = textMeasureColor
                                isFakeBoldText = true
                                setShadowLayer(3.dp.toPx(), 0f, 0f, android.graphics.Color.BLACK)
                            }
                            val textW = badgePaint.measureText(badgeText)
                            drawContext.canvas.nativeCanvas.drawText(
                                badgeText,
                                midPt.x - textW / 2f,
                                midPt.y - 8.dp.toPx(),
                                badgePaint
                            )
                        }
                    }
                }
            }

            projected.forEach { (item, point) ->
                val objectData = item.celestial
                val radius = when (objectData.objectType) {
                    CelestialType.SUN -> 14f
                    CelestialType.MOON -> 13f
                    CelestialType.PLANET -> (10f - objectData.magnitude.toFloat() * 0.45f).coerceIn(5f, 12f)
                    CelestialType.STAR -> (3.4f * 10.0.pow(-0.10 * objectData.magnitude).toFloat()).coerceIn(0.72f, 5.2f)
                    else -> (6f + (objectData.majorAxisArcMinutes ?: 0.0).toFloat() / 12f).coerceIn(6f, 15f)
                }
                when (objectData.objectType) {
                    CelestialType.STAR -> {
                        val color = starColor(objectData.colorIndex)
                        val alpha = (1f - (objectData.magnitude.toFloat() - 1.5f).coerceAtLeast(0f) * 0.115f).coerceIn(0.42f, 1f)
                        if (objectData.magnitude < 2.0) {
                            drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0f)),
                                center = point, radius = radius * 4.5f), radius * 4.5f, point)
                        }
                        drawCircle(color, radius, point, alpha = alpha)
                        if (radius > 2f) drawCircle(Color.White, radius * 0.4f, point, alpha = 0.78f)
                    }
                    CelestialType.SUN, CelestialType.MOON, CelestialType.PLANET -> {
                        if (horizontalFov <= 45.0 && objectData.solarBody == Body.Jupiter) {
                            drawZoomedJupiter(point, radius, skyInstant, projection)
                        } else if (horizontalFov <= 45.0 && objectData.solarBody == Body.Saturn) {
                            drawZoomedSaturn(point, radius, skyInstant, projection)
                        } else if (horizontalFov <= 45.0 && objectData.solarBody == Body.Moon) {
                            drawZoomedMoon(point, radius, skyInstant)
                        } else {
                            drawCircle(solarSystemColor(objectData.solarBody), radius * 1.8f, point, alpha = 0.18f)
                            drawCircle(solarSystemColor(objectData.solarBody), radius, point)
                        }
                    }
                    else -> {
                        drawCircle(deepSkyColor(objectData.objectType), radius, point, style = Stroke(2.5f))
                        drawLine(
                            deepSkyColor(objectData.objectType).copy(alpha = 0.7f),
                            point - Offset(radius * 0.55f, 0f),
                            point + Offset(radius * 0.55f, 0f),
                            1.5f
                        )
                    }
                }
                if (ObservationCatalogMatcher.isObserved(objectData.catalogId, loggedCatalogIds)) {
                    drawCircle(
                        color = AstraSuccess,
                        radius = 3.5.dp.toPx(),
                        center = point + Offset(radius + 5f, -radius * 0.5f)
                    )
                }
            }
        }

        // Draw celestial labels
        val drawCelestialLabels = {
            placedLabels.filter { it.candidate.kind != SkyLabelKind.ORIENTATION }.forEach { label ->
                val point = if (applyOptics && opticsSettings.rotateLabels)
                    opticsSettings.inverseTransformScreenPoint(Offset(label.x, label.baseline), canvasCenter)
                else Offset(label.x, label.baseline)
                drawContext.canvas.nativeCanvas.drawText(label.text, point.x, point.y,
                    labelPaints.getValue(label.candidate.id))
            }
        }
        if (applyOptics && opticsSettings.rotateLabels) {
            withOpticsTransform { drawCelestialLabels() }
        } else {
            drawCelestialLabels()
        }

        // Draw terrain & selection ring
        withOpticsTransform {
            if (arMode) drawTerrainHorizon(terrainProfile, viewAzimuth, viewAltitude, horizontalFov, arMode, oledMode = isOled)
            else drawSphericalTerrainHorizon(terrainProfile, projection, viewAzimuth, viewAltitude,
                padding = opticsPadding, oledMode = isOled)
            selectedPoint?.let { point ->
                drawCircle(StarGold, 18.dp.toPx(), point, style = Stroke(1.5.dp.toPx()))
            }
        }

        // Draw orientation labels
        val drawOrientationLabels = {
            placedLabels.filter { it.candidate.kind == SkyLabelKind.ORIENTATION }.forEach { label ->
                val point = if (applyOptics && opticsSettings.rotateLabels)
                    opticsSettings.inverseTransformScreenPoint(Offset(label.x, label.baseline), canvasCenter)
                else Offset(label.x, label.baseline)
                drawContext.canvas.nativeCanvas.drawText(label.text, point.x, point.y,
                    labelPaints.getValue(label.candidate.id))
            }
        }
        if (applyOptics && opticsSettings.rotateLabels) {
            withOpticsTransform { drawOrientationLabels() }
        } else {
            drawOrientationLabels()
        }

        // FOV Circle / Telrad reticle overlay (always centered in view, outside transform)
        if (!arMode && (opticsSettings.fovCircleEnabled || opticsSettings.telradMode)) {
            fun fovDiameterToRadiusPx(deg: Double): Float {
                val halfAngle = Math.toRadians(deg.coerceAtLeast(0.001) / 4.0)
                val screenQuarterFov = Math.toRadians(horizontalFov.coerceIn(0.5, 179.0) / 4.0)
                return ((size.width / (2.0 * tan(screenQuarterFov))) * tan(halfAngle)).toFloat()
            }

            if (opticsSettings.telradMode) {
                val ringColor = Color(0xFFFF5252).copy(alpha = 0.85f)
                val ringStroke = Stroke(width = 1.5.dp.toPx())
                val r05 = fovDiameterToRadiusPx(0.5)
                val r20 = fovDiameterToRadiusPx(2.0)
                val r40 = fovDiameterToRadiusPx(4.0)

                listOf(r05, r20, r40).forEach { r ->
                    if (r > 0.5f) {
                        drawCircle(ringColor, radius = r, center = canvasCenter, style = ringStroke)
                    }
                }

                val crosshairColor = Color(0xFFFF5252).copy(alpha = 0.65f)
                val tickLen = 8.dp.toPx()
                val gap = 4.dp.toPx()
                drawLine(crosshairColor, canvasCenter - Offset(0f, gap + tickLen), canvasCenter - Offset(0f, gap), strokeWidth = 1.5.dp.toPx())
                drawLine(crosshairColor, canvasCenter + Offset(0f, gap), canvasCenter + Offset(0f, gap + tickLen), strokeWidth = 1.5.dp.toPx())
                drawLine(crosshairColor, canvasCenter - Offset(gap + tickLen, 0f), canvasCenter - Offset(gap, 0f), strokeWidth = 1.5.dp.toPx())
                drawLine(crosshairColor, canvasCenter + Offset(gap, 0f), canvasCenter + Offset(gap + tickLen, 0f), strokeWidth = 1.5.dp.toPx())

                val telradPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 11.sp.toPx()
                    color = android.graphics.Color.rgb(255, 82, 82)
                    setShadowLayer(2.dp.toPx(), 0f, 0f, android.graphics.Color.BLACK)
                }
                val labelText = "Telrad 0.5° / 2° / 4°"
                val textWidth = telradPaint.measureText(labelText)
                val textY = (canvasCenter.y - r40 - 8.dp.toPx()).coerceAtLeast(18.dp.toPx())
                drawContext.canvas.nativeCanvas.drawText(labelText, canvasCenter.x - textWidth / 2f, textY, telradPaint)
            } else if (opticsSettings.fovCircleEnabled) {
                val fovDeg = opticsSettings.currentFovDegrees
                val radiusPx = fovDiameterToRadiusPx(fovDeg)
                if (radiusPx > 1f) {
                    val fovColor = if (redLightMode) Color(0xFFFF5252).copy(alpha = 0.85f) else Color(0xFF64B5F6).copy(alpha = 0.75f)
                    val stroke = Stroke(width = 1.5.dp.toPx())
                    drawCircle(fovColor, radius = radiusPx, center = canvasCenter, style = stroke)

                    val crosshairColor = fovColor.copy(alpha = 0.5f)
                    val tickLen = 6.dp.toPx()
                    val gap = 3.dp.toPx()
                    drawLine(crosshairColor, canvasCenter - Offset(0f, gap + tickLen), canvasCenter - Offset(0f, gap), strokeWidth = 1.dp.toPx())
                    drawLine(crosshairColor, canvasCenter + Offset(0f, gap), canvasCenter + Offset(0f, gap + tickLen), strokeWidth = 1.dp.toPx())
                    drawLine(crosshairColor, canvasCenter - Offset(gap + tickLen, 0f), canvasCenter - Offset(gap, 0f), strokeWidth = 1.dp.toPx())
                    drawLine(crosshairColor, canvasCenter + Offset(gap, 0f), canvasCenter + Offset(gap + tickLen, 0f), strokeWidth = 1.dp.toPx())

                    val fovPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = 11.sp.toPx()
                        color = if (redLightMode) android.graphics.Color.rgb(255, 82, 82) else android.graphics.Color.rgb(100, 181, 246)
                        setShadowLayer(2.dp.toPx(), 0f, 0f, android.graphics.Color.BLACK)
                    }
                    val labelText = "FOV ${String.format(Locale.US, "%.2f°", fovDeg)}"
                    val textWidth = fovPaint.measureText(labelText)
                    val textY = (canvasCenter.y - radiusPx - 8.dp.toPx()).coerceAtLeast(18.dp.toPx())
                    drawContext.canvas.nativeCanvas.drawText(labelText, canvasCenter.x - textWidth / 2f, textY, fovPaint)
                }
            }
        }
    }
}

internal data class HorizontalConstellationBoundary(
    val abbreviation: String,
    val points: List<HorizontalCoordinates>
)

private fun DrawScope.drawIauBoundaries(
    boundaries: List<HorizontalConstellationBoundary>,
    projection: SkyProjection,
    arMode: Boolean
) {
    boundaries.forEach { boundary ->
        val closed = boundary.points + boundary.points.firstOrNull().orEmpty()
        drawPath(projectedPath(closed, projection, padding = 1f),
            StarGold.copy(alpha = if (arMode) 0.34f else 0.19f),
            style = Stroke(width = if (arMode) 1.5f else 1.1f, join = StrokeJoin.Round))
    }
}

private fun HorizontalCoordinates?.orEmpty(): List<HorizontalCoordinates> =
    if (this == null) emptyList() else listOf(this)

private fun DrawScope.drawConstellationIllustration(name: String, anchor: Offset, arMode: Boolean) {
    val color = StarGold.copy(alpha = if (arMode) 0.26f else 0.14f)
    val stroke = Stroke(width = if (arMode) 3.0f else 2.2f)
    val path = Path()
    when (name) {
        "Orion", "Perseus", "Andromeda" -> {
            drawCircle(color, 13f, anchor - Offset(0f, 58f), style = stroke)
            path.moveTo(anchor.x, anchor.y - 43f)
            path.lineTo(anchor.x, anchor.y + 42f)
            path.moveTo(anchor.x - 42f, anchor.y - 14f)
            path.lineTo(anchor.x + 42f, anchor.y - 4f)
            path.moveTo(anchor.x, anchor.y + 42f)
            path.lineTo(anchor.x - 34f, anchor.y + 86f)
            path.moveTo(anchor.x, anchor.y + 42f)
            path.lineTo(anchor.x + 34f, anchor.y + 86f)
        }
        "Zwillinge" -> {
            listOf(-24f, 24f).forEach { x ->
                drawCircle(color, 10f, anchor + Offset(x, -48f), style = stroke)
                path.moveTo(anchor.x + x, anchor.y - 36f)
                path.lineTo(anchor.x + x, anchor.y + 45f)
                path.moveTo(anchor.x + x, anchor.y - 8f)
                path.lineTo(anchor.x + x + if (x < 0) -25f else 25f, anchor.y + 12f)
            }
        }
        "Großer Wagen", "Löwe", "Pegasus" -> {
            drawOval(color, anchor - Offset(55f, 24f), androidx.compose.ui.geometry.Size(110f, 58f), style = stroke)
            path.moveTo(anchor.x - 52f, anchor.y)
            path.cubicTo(anchor.x - 90f, anchor.y - 12f, anchor.x - 102f, anchor.y - 48f, anchor.x - 116f, anchor.y - 62f)
            path.moveTo(anchor.x - 25f, anchor.y + 26f)
            path.lineTo(anchor.x - 34f, anchor.y + 68f)
            path.moveTo(anchor.x + 28f, anchor.y + 26f)
            path.lineTo(anchor.x + 38f, anchor.y + 68f)
        }
        "Schwan", "Adler" -> {
            path.moveTo(anchor.x - 78f, anchor.y + 12f)
            path.quadraticTo(anchor.x - 30f, anchor.y - 42f, anchor.x, anchor.y)
            path.quadraticTo(anchor.x + 32f, anchor.y - 42f, anchor.x + 82f, anchor.y + 12f)
            path.moveTo(anchor.x, anchor.y - 58f)
            path.lineTo(anchor.x, anchor.y + 72f)
        }
        "Leier" -> {
            path.moveTo(anchor.x - 48f, anchor.y - 58f)
            path.quadraticTo(anchor.x - 34f, anchor.y + 52f, anchor.x, anchor.y + 62f)
            path.quadraticTo(anchor.x + 34f, anchor.y + 52f, anchor.x + 48f, anchor.y - 58f)
            path.moveTo(anchor.x - 42f, anchor.y - 36f)
            path.lineTo(anchor.x + 42f, anchor.y - 36f)
        }
        "Kassiopeia" -> {
            path.moveTo(anchor.x - 65f, anchor.y + 42f)
            path.lineTo(anchor.x - 42f, anchor.y - 40f)
            path.lineTo(anchor.x, anchor.y + 12f)
            path.lineTo(anchor.x + 42f, anchor.y - 40f)
            path.lineTo(anchor.x + 65f, anchor.y + 42f)
        }
        else -> return
    }
    drawPath(path, color, style = stroke)
}

private fun DrawScope.drawMilkyWay(
    band: List<HorizontalCoordinates>,
    projection: SkyProjection
) {
    if (band.size < 2) return
    // Keep the glow visible even when its centerline is just beyond the viewport.
    val path = projectedPath(band, projection, padding = 38f)
    drawPath(path, Color(0xFF96BFFF).copy(alpha = 0.035f),
        style = Stroke(76f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, Color(0xFFB8D2FF).copy(alpha = 0.07f),
        style = Stroke(34f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, Color(0xFFD5E3FF).copy(alpha = 0.23f),
        style = Stroke(2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawTerrainHorizon(
    profile: TerrainProfile?,
    viewAzimuth: Double,
    viewAltitude: Double,
    horizontalFov: Double,
    arMode: Boolean,
    oledMode: Boolean = false
) {
    if (size.width <= 0f || size.height <= 0f) return
    val verticalFov = horizontalFov * size.height / size.width
    val points = (0..120).map { step ->
        val fraction = step / 120.0
        val azimuth = normalizeDegrees(viewAzimuth - horizontalFov / 2.0 + horizontalFov * fraction)
        val altitude = profile?.altitudeAt(azimuth) ?: 0.0
        Offset(
            (size.width * fraction).toFloat(),
            (size.height * (0.5 - (altitude - viewAltitude) / verticalFov)).toFloat()
        )
    }
    // An off-screen horizon above the viewport still covers the view with ground.
    if (points.all { it.y > size.height + 20f }) return
    val ground = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    val groundBaseColor = if (oledMode) Color.Black else Color(0xFF03070D)
    drawPath(ground, groundBaseColor.copy(alpha = if (arMode) 0.30f else 0.97f))
    points.zipWithNext().forEach { (start, end) ->
        drawLine(StarGold.copy(alpha = 0.72f), start, end, 2.2f)
    }
}

private fun DrawScope.drawSphericalTerrainHorizon(
    profile: TerrainProfile?,
    projection: SkyProjection,
    viewAzimuth: Double,
    viewAltitude: Double,
    padding: Float = 20f,
    oledMode: Boolean = false
) {
    if (size.width <= 0f || size.height <= 0f) return

    val horizon = (0..120).map { step ->
        val azimuth = step * 3.0
        HorizontalCoordinates(azimuth, profile?.altitudeAt(azimuth) ?: 0.0)
    }

    val groundColor = if (oledMode) Color.Black else Color(0xFF03070D)

    // Away from the horizon, the entire 360° contour divides sky and ground.
    // It can cross both the top and bottom when a portrait view looks near the zenith.
    val contour = if (abs(viewAltitude) >= 5.0) horizon.mapNotNull(projection::projectedPoint) else emptyList()
    if (contour.size == horizon.size) {
        val ground = Path().apply {
            val oppositeAzimuth = normalizeDegrees(viewAzimuth + 180.0)
            // The camera antipode maps to the contour's outside; optics may rotate off-screen pixels into view.
            if (-viewAltitude <= (profile?.altitudeAt(oppositeAzimuth) ?: 0.0)) {
                val margin = hypot(size.width.toDouble(), size.height.toDouble()).toFloat()
                fillType = PathFillType.EvenOdd
                moveTo(-margin, -margin)
                lineTo(size.width + margin, -margin)
                lineTo(size.width + margin, size.height + margin)
                lineTo(-margin, size.height + margin)
                close()
            }
            moveTo(contour.first().x, contour.first().y)
            contour.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }
        drawPath(ground, groundColor)
    } else {
        // At the horizon the opposite point approaches the projection singularity.
        val points = (0..160).mapNotNull { step ->
            val relAz = -120.0 + step * (240.0 / 160.0)
            val sampleAz = ((viewAzimuth + relAz) % 360.0 + 360.0) % 360.0
            val alt = profile?.altitudeAt(sampleAz) ?: 0.0
            projection.point(HorizontalCoordinates(sampleAz, alt), padding = padding)
        }
        if (points.isNotEmpty()) {
            val ground = Path().apply {
                moveTo(-20f, points.first().y)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(size.width + 20f, points.last().y)
                lineTo(size.width + 20f, size.height + 20f)
                lineTo(-20f, size.height + 20f)
                close()
            }
            drawPath(ground, groundColor)
        }
    }

    drawPath(
        projectedPath(horizon, projection, padding = 1.1f),
        StarGold.copy(alpha = 0.72f),
        style = Stroke(2.2f, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawSkyGrid(viewAzimuth: Double, viewAltitude: Double) {
    val grid = Color.White.copy(alpha = 0.10f)
    repeat(7) { i ->
        val x = size.width * i / 6f
        drawLine(grid, Offset(x, 0f), Offset(x, size.height), 1f)
    }
    repeat(5) { i ->
        val y = size.height * i / 4f
        drawLine(grid, Offset(0f, y), Offset(size.width, y), 1f)
    }
    drawCircle(Color.White.copy(alpha = 0.7f), 4f, center)
    drawLine(Color.White.copy(alpha = 0.35f), center - Offset(12f, 0f), center + Offset(12f, 0f), 1f)
    drawLine(Color.White.copy(alpha = 0.35f), center - Offset(0f, 12f), center + Offset(0f, 12f), 1f)
}

private fun projectedPath(
    points: List<HorizontalCoordinates>,
    projection: SkyProjection,
    padding: Float
): Path = Path().apply {
    var previousEnd: Offset? = null
    points.zipWithNext().forEach { (from, to) ->
        val segments = projection.segments(from, to, padding)
        if (segments.isEmpty()) previousEnd = null
        segments.forEach { segment ->
            if (previousEnd?.let { (it - segment.start).getDistance() < 0.25f } != true) {
                moveTo(segment.start.x, segment.start.y)
            }
            lineTo(segment.end.x, segment.end.y)
            previousEnd = segment.end
        }
    }
}

@Composable
private fun ObjectDetails(
    item: VisibleObject,
    observer: GeoPoint,
    skyInstant: Instant,
    simulated: Boolean,
    terrain: TerrainProfile?,
    isFavorite: Boolean,
    toggleFavorite: () -> Unit,
    onAddLogEntry: ((CelestialObject) -> Unit)? = null,
    onOpenMoonDetail: (() -> Unit)? = null,
    onStartMeasurement: ((VisibleObject) -> Unit)? = null,
    onStartStarHop: ((StarHopRoute) -> Unit)? = null,
    isObservedInLogbook: Boolean = false
) {
    val objectData = item.celestial
    val path = remember(item.celestial, observer, skyInstant) {
        (0..12).map { hours ->
            val at = skyInstant.plusSeconds(hours * 3600L)
            at to coordinatesAt(item.celestial, observer, at)
        }
    }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(objectData.name, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            if (isObservedInLogbook) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF163228)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "✓ Im Logbuch",
                        color = AstraSuccess,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        Text(objectData.catalogId, color = AstraBlue)
        Text((if (simulated) "Simulation · " else "Jetzt · ") +
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss XXX").withZone(ZoneId.systemDefault()).format(skyInstant),
            fontSize = 12.sp, color = StarGold)
        Spacer(Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = toggleFavorite,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFavorite) StarGold else AstraSurfaceHigh,
                    contentColor = if (isFavorite) Night else Color.White
                )
            ) {
                Icon(if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder, null)
                Spacer(Modifier.width(8.dp))
                Text(if (isFavorite) "In Beobachtungsliste ✓" else "Zur Beobachtungsliste")
            }
            if (onAddLogEntry != null) {
                Button(
                    onClick = { onAddLogEntry(objectData) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AstraSurfaceHigh,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Rounded.EditNote, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Beobachten")
                }
            }
            if (onStartMeasurement != null) {
                Button(
                    onClick = { onStartMeasurement(item) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AstraSurfaceHigh,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Rounded.Straighten, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Abstand messen")
                }
            }
            if (onStartStarHop != null) {
                val matchingRoutes = remember<List<StarHopRoute>>(objectData.catalogId) {
                    StarHopCatalog.findRoutesForTarget(objectData.catalogId)
                }
                if (matchingRoutes.isNotEmpty()) {
                    Button(
                        onClick = { onStartStarHop(matchingRoutes.first()) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AstraSurfaceHigh,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.AltRoute, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Star-Hop starten")
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        DetailRow("Objekttyp", objectData.objectType.label)
        DetailRow(
            if (objectData.solarBody == null) "Koordinaten J2000" else "Koordinaten zur Kartenzeit",
            "RA ${formatRa(objectData.raHours)} · Dec ${formatDec(objectData.decDegrees)}"
        )
        DetailRow("Position zur Kartenzeit", "Az ${item.position.azimuth.format(1)}° · Höhe ${item.position.altitude.format(1)}°")
        DetailRow("Höhenlage", targetVisibility(item.position, terrain).label)
        DetailRow("Helligkeit", "${objectData.magnitude.format(2)} mag")
        if (objectData.constellation.isNotBlank()) DetailRow("Sternbild", objectData.constellation)
        if (objectData.solarBody != null) {
            objectData.distanceAu?.let { distance ->
                DetailRow(
                    "Entfernung",
                    if (objectData.solarBody == Body.Moon) "${(distance * 149_597_870.7).format(0)} km"
                    else "${distance.format(3)} AE"
                )
            }
            objectData.phaseFraction?.let { DetailRow("Beleuchtete Fläche", "${(it * 100.0).format(0)} %") }
            if (objectData.astronomyDescription.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(objectData.astronomyDescription, color = Color(0xFFAAB8CE))
            }
            if (objectData.solarBody == Body.Jupiter) {
                val jupiter = remember(skyInstant) { SolarSystemCatalog.jupiterSystem(skyInstant) }
                Spacer(Modifier.height(18.dp))
                Text("Galileische Monde", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                JupiterMoonsDiagram(jupiter)
                Spacer(Modifier.height(8.dp))
                JupiterMoonEventsBadges(jupiter)
            } else if (objectData.solarBody == Body.Saturn) {
                val saturn = remember(skyInstant) { SolarSystemCatalog.saturnSystem(skyInstant) }
                Spacer(Modifier.height(18.dp))
                Text("Ringsystem & Titan", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                DetailRow("Ringöffnungswinkel B", "${saturn.ringTiltDegrees.format(1)}° (${if (saturn.ringTiltDegrees >= 0) "Nordseite" else "Südseite"} sichtbar)")
                DetailRow("Titan-Abstand", "${saturn.titanOffsetRS.format(1)} Saturnradien")
                DetailRow("Titan-Positionswinkel", "${saturn.titanPositionAngle.format(0)}°")
                Spacer(Modifier.height(8.dp))
                SaturnRingsDiagram(saturn)
            } else if (objectData.solarBody == Body.Moon) {
                val terminator = remember(skyInstant) { LunarTerminatorCalculator.calculateTerminator(skyInstant) }
                Spacer(Modifier.height(18.dp))
                MoonTerminatorSummary(terminator, onOpenMoonDetail)
            }
        } else if (objectData.objectType == CelestialType.STAR) {
            DetailRow(
                "Entfernung",
                if (objectData.distanceLightYears > 0.0) {
                    "${objectData.distanceLightYears.format(1)} Lichtjahre"
                } else "unbekannt"
            )
            DetailRow("Spektralklasse", objectData.spectralClass)
            objectData.absoluteMagnitude?.let { DetailRow("Absolute Helligkeit", "${it.format(2)} mag") }
            objectData.luminositySolar?.let { DetailRow("Leuchtkraft", "${it.format(2)} × Sonne") }
            DetailRow("Farbindex B−V", objectData.colorIndex.format(3))
            if (objectData.properMotionRa != null || objectData.properMotionDec != null) {
                DetailRow(
                    "Eigenbewegung",
                    "RA ${objectData.properMotionRa?.format(2) ?: "–"} · Dec ${objectData.properMotionDec?.format(2) ?: "–"} mas/Jahr"
                )
            }
            objectData.radialVelocity?.let { DetailRow("Radialgeschwindigkeit", "${it.format(1)} km/s") }
        } else {
            objectData.majorAxisArcMinutes?.let { major ->
                val minor = objectData.minorAxisArcMinutes
                DetailRow("Winkelausdehnung", if (minor != null) "${major.format(1)}′ × ${minor.format(1)}′" else "${major.format(1)}′")
            }
            objectData.positionAngleDegrees?.let { DetailRow("Positionswinkel", "${it.format(0)}°") }
            objectData.redshift?.let { DetailRow("Rotverschiebung z", it.format(6)) }
        }
        if (objectData.solarBody == null) {
            Spacer(Modifier.height(18.dp))
            Text("DSS2-Himmelsaufnahme", fontWeight = FontWeight.Bold)
            Text(
                "Archivaufnahme an der Objektkoordinate; kein Bild zum gewählten Simulationsdatum. Sterne erscheinen als Lichtpunkte.",
                color = Color(0xFFAAB8CE),
                fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))
            SkySurveyImage(objectData)
            Text("Bild: DSS2 via CDS HiPS2FITS", fontSize = 11.sp, color = Color(0xFFAAB8CE))
        }
        Spacer(Modifier.height(18.dp))
        Text("Bahn: 12 Stunden ab Kartenzeit", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        path.forEach { (time, position) ->
            val localTime = DateTimeFormatter.ofPattern("dd.MM. HH:mm XXX").withZone(ZoneId.systemDefault()).format(time)
            Text("$localTime   Az ${position.azimuth.format(0)}°   Höhe ${position.altitude.format(0)}°", fontSize = 13.sp)
        }
    }
}

@Composable
private fun SkySurveyImage(objectData: CelestialObject) {
    if (!SecureNetwork.options.online) {
        OfflineNotice()
        return
    }
    val context = LocalContext.current
    var loadError by remember(objectData) { mutableStateOf(false) }
    var isLoading by remember(objectData) { mutableStateOf(true) }
    var retryKey by remember(objectData) { mutableIntStateOf(0) }

    val fieldOfView = remember(objectData) {
        if (objectData.objectType == CelestialType.STAR) 0.35
        else (((objectData.majorAxisArcMinutes ?: 12.0) / 60.0) * 2.4).coerceIn(0.25, 4.0)
    }
    val imageUrl = remember(objectData, fieldOfView) {
        "https://alasky.u-strasbg.fr/hips-image-services/hips2fits" +
            "?hips=CDS%2FP%2FDSS2%2Fcolor&width=900&height=520&projection=TAN" +
            "&fov=$fieldOfView&coordsys=icrs&ra=${objectData.raHours * 15.0}" +
            "&dec=${objectData.decDegrees}&format=jpg&stretch=asinh"
    }
    val webView = remember(context, imageUrl, retryKey) {
        loadError = false
        isLoading = true
        PrivateWebViews.create(
            context = context,
            javascript = false,
            onError = { loadError = true; isLoading = false },
            onFinished = { isLoading = false }
        ).apply {
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            loadUrl(imageUrl)
        }
    }
    DisposableEffect(webView) {
        onDispose {
            PrivateWebViews.dispose(webView)
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth().height(230.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Night)
    ) {
        Box(Modifier.fillMaxSize()) {
            if (!loadError) {
                AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
            }
            if (isLoading && !loadError) {
                Box(Modifier.fillMaxSize().background(Night.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                    Text("Himmelsaufnahme wird geladen …", color = AstraBlue, fontSize = 12.sp)
                }
            }
            if (loadError) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Himmelsaufnahme konnte nicht geladen werden.",
                        color = StarGold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { retryKey++ }) {
                        Text("Erneut versuchen")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFFAAB8CE), modifier = Modifier.weight(0.42f))
        Text(value, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, modifier = Modifier.weight(0.58f))
    }
    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
}

@Composable
private fun AstraScreenHeader(
    eyebrow: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    keepSubtitleInCompact: Boolean = false
) {
    val compactLandscape = isCompactLandscape()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AstraSurface),
        border = BorderStroke(1.dp, AstraOutline.copy(alpha = 0.75f))
    ) {
        Row(
            Modifier.fillMaxWidth().background(
                if (LocalOledMode.current) Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                else Brush.horizontalGradient(
                    listOf(AstraBlue.copy(alpha = 0.13f), Color.Transparent, StarGold.copy(alpha = 0.06f))
                )
            ).padding(horizontal = if (compactLandscape) 12.dp else 20.dp,
                vertical = if (compactLandscape) 8.dp else 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!compactLandscape) {
            Box(
                Modifier.size(52.dp).background(AstraBlue.copy(alpha = 0.16f), RoundedCornerShape(17.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = StarGold, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(15.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                if (!compactLandscape) Text(eyebrow, color = AstraBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(title, fontSize = if (compactLandscape) 20.sp else 27.sp, fontWeight = FontWeight.Bold,
                    maxLines = if (compactLandscape) 1 else Int.MAX_VALUE, overflow = TextOverflow.Ellipsis)
                if (!compactLandscape || keepSubtitleInCompact) Text(subtitle, color = AstraTextMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AstraSectionTitle(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        subtitle?.let { Text(it, color = AstraTextMuted, fontSize = 12.sp) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WeatherScreen(
    location: GeoPoint?,
    simulatedSkyTime: Instant?,
    refreshLocation: () -> Unit,
    loadWeather: (GeoPoint, (Result<WeatherSnapshot>) -> Unit) -> Unit = WeatherRepository::load,
    loadLight: (GeoPoint, Boolean, (LightPollutionState) -> Unit) -> Unit = LightPollutionRepository::load,
    mapContent: @Composable (GeoPoint, Int) -> Unit = { point, refresh -> WeatherMap(point, refresh) }
) {
    val context = LocalContext.current
    val session = remember { WeatherRefreshSession() }
    val state = session.state
    var refreshKey by remember { mutableIntStateOf(0) }
    var foreground by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(Instant.now()) }
    val observer = NetworkPolicy.roundedLocation(location ?: GeoPoint(52.52, 13.405, 34.0))
    val target = WeatherTarget(observer, demo = location == null)

    fun refresh() {
        refreshKey++
        refreshLocation()
    }

    LifecycleStartEffect(target, refreshKey) {
        foreground = true
        if (SecureNetwork.available) {
            val ticket = session.begin(target)
            loadWeather(observer) {
                if (SecureNetwork.available) {
                    session.weather(ticket, it)
                    it.getOrNull()?.let { snapshot ->
                        val score = (100 - snapshot.cloudCover).coerceIn(0, 100)
                        AstraWidgetUpdater.saveWeatherScore(context, score)
                        AstraWidgetUpdater.updateAllWidgets(context)
                    }
                }
            }
            loadLight(observer, refreshKey > 0) {
                if (SecureNetwork.available) session.light(ticket, (it as? LightPollutionState.Ready)?.estimate)
            }
        }
        onStopOrDispose {
            foreground = false
            session.stop()
        }
    }
    LaunchedEffect(foreground) {
        while (foreground) {
            now = Instant.now()
            kotlinx.coroutines.delay(30_000)
        }
    }

    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = ::refresh,
        modifier = Modifier.fillMaxSize().background(screenBackgroundBrush)
    ) {
        Column(
            Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AstraScreenHeader(
                eyebrow = "ASTRA FORECAST",
                title = "Beobachtungswetter",
                subtitle = if (location == null) "Demo-Standort Berlin"
                else "Wetter für deinen ungefähren Standort",
                icon = Icons.Rounded.Cloud,
                keepSubtitleInCompact = true
            )
            Text("Nach unten ziehen zum Aktualisieren", color = AstraTextMuted, fontSize = 12.sp)
            simulatedSkyTime?.let {
                Text("Sternkarte: Simulation vom " + DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm XXX")
                    .withZone(ZoneId.systemDefault()).format(it) + ". Hier werden Wetter und Astra-Score für Jetzt bzw. die aktuelle Vorhersage angezeigt. Für die Simulationszeit sind hier keine zugeordneten Wetterdaten verfügbar.",
                    color = StarGold, fontSize = 13.sp)
            }
            if (!SecureNetwork.options.online) OfflineNotice()
            if (state.refreshing) {
                Text(if (state.displayed == null) "Wetter wird geladen …" else "Wetter wird aktualisiert · bisherige Daten bleiben sichtbar",
                    color = AstraBlue, fontSize = 12.sp)
            }
            Text(if (target.demo) "Ohne verfügbaren Standort wird Berlin verwendet."
                else "Bis zu einem neuen GPS-Fix wird der letzte verfügbare Standort verwendet.",
                color = AstraTextMuted, fontSize = 12.sp)
            if (state.error) {
                Text(if (state.displayed == null) "Wetterdaten konnten nicht geladen werden."
                    else "Aktualisierung fehlgeschlagen · bisherige Wetterdaten werden weiter angezeigt.", color = StarGold)
                TextButton(onClick = ::refresh) { Text("Erneut versuchen") }
            }
            state.displayed?.let { value ->
                if (value.target != target) {
                    Text("Noch Wetter für den bisherigen Standort${if (value.target.demo) " (Demo Berlin)" else ""}. Neue Standortdaten sind noch nicht verfügbar.",
                        color = StarGold, fontSize = 13.sp)
                }
                ObservationScore(
                    value.weather,
                    value.target.point,
                    value.light
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WeatherTile("Temperatur", "${value.weather.temperature.format(1)} °C", Modifier.weight(1f))
                    WeatherTile("Bewölkung", "${value.weather.cloudCover}%", Modifier.weight(1f))
                }
                value.light?.let {
                    WeatherTile(
                        "Lichtverschmutzung · NASA ${it.sourceYear}",
                        "Index ${it.index}/100 · Bortle ≈ ${it.bortleClass}",
                        Modifier.fillMaxWidth()
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WeatherTile("Wind", "${value.weather.windSpeed.format(1)} km/h", Modifier.weight(1f))
                    WeatherTile("Sichtweite", "${(value.weather.visibility / 1000.0).format(1)} km", Modifier.weight(1f))
                }
                val age = java.time.Duration.between(value.weather.fetchedAt, now).toMinutes().coerceAtLeast(0)
                Text("Open-Meteo · Datenstand ${value.weather.updatedAt}\nAbgerufen ${if (age == 0L) "vor weniger als einer Minute" else "vor $age Min."}", fontSize = 12.sp, color = AstraTextMuted)
                if (state.lightRefreshing) Text("Lichtschätzung wird separat aktualisiert …", fontSize = 12.sp, color = AstraTextMuted)
                else if (state.lightError) Text("Lichtschätzung nicht aktualisiert${if (value.light != null) " · bisheriger Wert bleibt erhalten" else " · im Score nicht verfügbar"}.", fontSize = 12.sp, color = StarGold)
                AstraSectionTitle("24-Stunden-Ausblick", "Vorhersage des oben angegebenen Datenstands")
                ForecastTimeline(
                    value.weather.forecast,
                    value.target.point,
                    value.light
                )
            }
            if (SecureNetwork.options.online) {
                AstraSectionTitle("Wolken- und Regenkarte", "Radar und Bewölkung · unabhängig vom Wetterabruf")
                Text("Vorhandene Ebenen bleiben beim Aktualisieren sichtbar. Den jeweiligen Datenstand siehst du direkt in der Karte.",
                    color = AstraTextMuted, fontSize = 13.sp)
                mapContent(observer, refreshKey)
            }
        }
    }
}

@Composable
private fun ForecastTimeline(
    forecast: List<HourlyForecast>,
    observer: GeoPoint,
    lightPollution: LightPollutionEstimate?
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        forecast.forEach { hour ->
            val score = remember(hour, observer, lightPollution) {
                AstraScoreCalculator.calculate(
                    cloudCover = hour.cloudCover,
                    rainProbability = hour.rainProbability,
                    windSpeed = hour.windSpeed,
                    visibilityMeters = hour.visibility,
                    observer = observer,
                    instant = hour.instant,
                    lightPollution = lightPollution
                ).score
            }
            Card(
                modifier = Modifier.width(132.dp),
                colors = CardDefaults.cardColors(containerColor = AstraSurface),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(
                    1.dp,
                    (if (score >= 70) AstraSuccess else if (score >= 45) AstraBlue else StarGold).copy(alpha = 0.30f)
                )
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(hour.time, color = StarGold, fontWeight = FontWeight.Bold)
                    Text("☁ ${hour.cloudCover} %", fontSize = 13.sp)
                    Text("Regen ${hour.rainProbability} %", fontSize = 12.sp, color = Color(0xFFAAB8CE))
                    Text("Wind ${hour.windSpeed.format(0)} km/h", fontSize = 12.sp, color = Color(0xFFAAB8CE))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Text(
                        "Astra $score/100",
                        color = if (score >= 70) AstraSuccess else if (score >= 45) AstraBlue else StarGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}


private enum class SkyEventKind(val label: String) {
    METEOR("Sternschnuppen"),
    SOLAR_ECLIPSE("Sonnenfinsternis"),
    LUNAR_ECLIPSE("Mondfinsternis")
}

private data class SkyEvent(
    val title: String,
    val kind: SkyEventKind,
    val instant: Instant,
    val timeIsApproximate: Boolean,
    val status: String,
    val statusColor: Color,
    val facts: String,
    val description: String,
    val radiant: EquatorialBoundaryPoint? = null
) {
    val key: String get() = "${kind.name}:$title:${instant.atZone(ZoneId.systemDefault()).year}"
}

private fun SkyEvent.toSavedEvent() = SavedSkyEvent(
    key = key,
    title = title,
    kind = kind.label,
    instantEpochSeconds = instant.epochSecond
)

@Composable
private fun EventsScreen(
    location: GeoPoint?,
    redLightMode: Boolean,
    openInSky: (SkyEvent) -> Unit,
    savedEventKeys: Set<String>,
    toggleSavedEvent: (SkyEvent) -> Unit
) {
    val observer = location ?: GeoPoint(52.52, 13.405, 34.0)
    val context = LocalContext.current
    var meteorCalendar by remember { mutableStateOf(MeteorCalendarRepository.bundled(context)) }
    LaunchedEffect(observer) {
        MeteorCalendarRepository.refresh(context) { meteorCalendar = it }
    }
    val events = remember(observer, meteorCalendar) { buildUpcomingEvents(observer, meteorCalendar.showers) }
    Column(
        Modifier.fillMaxSize().background(screenBackgroundBrush).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(6.dp))
        AstraScreenHeader(
            eyebrow = "ASTRA EVENTS",
            title = "Himmelskalender",
            subtitle = if (location == null) "Berechnet für Demo-Standort Berlin"
            else "Lokal für deinen Beobachtungsort berechnet",
            icon = Icons.Rounded.CalendarMonth,
            keepSubtitleInCompact = true
        )
        Text(
            "Die Einschätzung berücksichtigt Standort, Radiantenhöhe und ungefähres Mondlicht. " +
                "Das Maximum eines Meteorschauers kann sich leicht verschieben.",
            color = Color(0xFFAAB8CE),
            fontSize = 13.sp
        )
        Text(
            if (meteorCalendar.online) "IMO-Kalender online aktualisiert · Stand ${meteorCalendar.updated}"
            else "IMO-Kalender offline verfügbar · Stand ${meteorCalendar.updated}",
            color = if (meteorCalendar.online) AstraSuccess else AstraTextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        AstraSectionTitle("Lichtverschmutzung", "Dunkle Beobachtungsplätze in deiner Umgebung finden")
        Text(
            "Vergleiche künstliches Nachtlicht in deiner Umgebung. Über die Kartenebenen kannst du " +
                "eine grobe Bortle-Einordnung und den Platzvergleich einblenden.",
            color = Color(0xFFAAB8CE),
            fontSize = 13.sp
        )
        if (SecureNetwork.options.online) {
            ExternalLightMapEntry(observer, demoLocation = location == null, redLight = redLightMode)
            Text("Astra-Karte · NASA-Nachtlicht 2016", fontWeight = FontWeight.Bold)
            LightPollutionSection(observer, demoLocation = location == null, redLight = redLightMode)
        } else OfflineNotice()

        AstraSectionTitle("Kommende Ereignisse", "Finsternisse und Meteorschauer chronologisch sortiert")
        events.forEach { event ->
            EventCard(
                event = event,
                openInSky = { openInSky(event) },
                saved = event.key in savedEventKeys,
                toggleSaved = { toggleSavedEvent(event) }
            )
        }
        Text(
            "Quellen: NASA/GSFC Eclipse Catalog · International Meteor Organization (IMO)",
            color = Color(0xFFAAB8CE),
            fontSize = 11.sp
        )
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun EventCard(event: SkyEvent, saved: Boolean, toggleSaved: () -> Unit, openInSky: () -> Unit) {
    val zone = ZoneId.systemDefault()
    val dateFormat = DateTimeFormatter.ofPattern("EEE, d. MMM yyyy", Locale.GERMAN).withZone(zone)
    val timeFormat = DateTimeFormatter.ofPattern("HH:mm 'Uhr'", Locale.GERMAN).withZone(zone)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AstraSurface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, event.statusColor.copy(alpha = 0.28f))
    ) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(event.kind.label.uppercase(Locale.GERMAN), color = AstraBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (event.timeIsApproximate) "Maximum-Nacht" else timeFormat.format(event.instant),
                        color = StarGold,
                        fontSize = 12.sp
                    )
                    IconButton(onClick = toggleSaved) {
                        Icon(
                            if (saved) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            contentDescription = if (saved) "Vormerkung entfernen" else "Ereignis vormerken",
                            tint = if (saved) StarGold else AstraTextMuted
                        )
                    }
                }
            }
            Text(event.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(dateFormat.format(event.instant), color = Color(0xFFD8E4F7))
            Text(event.status, color = event.statusColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(event.facts, color = StarGold, fontSize = 13.sp)
            Text(event.description, color = Color(0xFFAAB8CE), fontSize = 13.sp)
            TextButton(onClick = openInSky) {
                Text(if (event.timeIsApproximate) "Maximum-Nacht in Karte öffnen" else "Maximum in Karte öffnen")
            }
        }
    }
}

private fun buildUpcomingEvents(observer: GeoPoint, meteorShowers: List<MeteorDefinition>): List<SkyEvent> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val meteorEvents = (today.year..today.year + 1).flatMap { year ->
        meteorShowers.filter { it.year == year }.map { shower ->
            val instant = ZonedDateTime.of(
                LocalDate.of(year, shower.month, shower.day),
                LocalTime.MIDNIGHT,
                java.time.ZoneOffset.UTC
            ).toInstant()
            val radiant = CelestialObject(
                name = shower.name,
                catalogId = "IMO ${shower.code}",
                raHours = shower.radiantRaHours,
                decDegrees = shower.radiantDecDegrees,
                magnitude = 0.0,
                distanceLightYears = 0.0,
                spectralClass = ""
            )
            val altitude = AstronomyEngine.horizontalCoordinates(radiant, observer, instant).altitude
            val moon = moonIlluminationPercent(instant)
            val quality = when {
                altitude < 0.0 -> Triple("Radiant gegen 02:00 unter dem Horizont", StarGold, "Beobachtung zu einer anderen Uhrzeit prüfen.")
                altitude >= 40.0 && moon < 45.0 -> Triple("Gute Bedingungen am Standort", Color(0xFF76E0A0), "Hoher Radiant und wenig Mondlicht.")
                altitude >= 20.0 && moon < 75.0 -> Triple("Brauchbare Bedingungen am Standort", AstraBlue, "Radiant sichtbar; Mondlicht kann etwas stören.")
                else -> Triple("Eingeschränkte Bedingungen am Standort", StarGold, "Niedriger Radiant oder deutliches Mondlicht.")
            }
            SkyEvent(
                title = shower.name,
                kind = SkyEventKind.METEOR,
                instant = instant,
                timeIsApproximate = true,
                status = quality.first,
                statusColor = quality.second,
                facts = "Radiant ca. ${altitude.format(0)}° hoch · Mond ca. ${moon.format(0)} % · " +
                    if (shower.zhr > 0) "ZHR ${shower.zhr}" else "ZHR variabel",
                description = "${quality.third} Maximum nach dem jährlichen IMO-Kalender; die ZHR gilt nur unter ideal dunklem Himmel.",
                radiant = EquatorialBoundaryPoint(shower.radiantRaHours, shower.radiantDecDegrees)
            )
        }
    }

    val eclipseEvents = exactLocalEclipseEvents(observer)

    val now = Instant.now().minus(1, ChronoUnit.DAYS)
    return (meteorEvents + eclipseEvents)
        .filter { it.instant.isAfter(now) }
        .sortedBy { it.instant }
        .take(18)
}

private fun exactLocalEclipseEvents(observer: GeoPoint): List<SkyEvent> {
    val start = Instant.now().toAstroTime()
    val place = observer.toAstroObserver()
    val solar = localSolarEclipsesAfter(start, place).take(2).map { eclipse ->
        val maximum = eclipse.peak.time.toInstant()
        val totalBegin = eclipse.totalBegin
        val totalEnd = eclipse.totalEnd
        val corePhase = if (totalBegin != null && totalEnd != null) {
            " · Kernphase ${formatEventTime(totalBegin.time.toInstant())}–${formatEventTime(totalEnd.time.toInstant())}"
        } else ""
        SkyEvent(
            title = eclipse.kind.germanEclipseTitle("Sonnenfinsternis"),
            kind = SkyEventKind.SOLAR_ECLIPSE,
            instant = maximum,
            timeIsApproximate = false,
            status = "An deinem Standort sichtbar",
            statusColor = Color(0xFF76E0A0),
            facts = "Bedeckung ${(eclipse.obscuration * 100.0).format(1)} % · Sonne ${eclipse.peak.altitude.format(0)}° hoch",
            description = "Kontakt ${formatEventTime(eclipse.partialBegin.time.toInstant())} · Maximum ${formatEventTime(maximum)} · Ende ${formatEventTime(eclipse.partialEnd.time.toInstant())}$corePhase"
        )
    }
    val lunar = lunarEclipsesAfter(start).take(5).mapNotNull { eclipse ->
        val maximum = eclipse.peak.toInstant()
        val altitude = SolarSystemCatalog.horizontal(Body.Moon, observer, maximum).altitude
        if (altitude <= -1.0) return@mapNotNull null
        val penumbralStart = maximum.minusSeconds((eclipse.sdPenum * 60.0).toLong())
        val penumbralEnd = maximum.plusSeconds((eclipse.sdPenum * 60.0).toLong())
        val obscurationText = if (eclipse.kind == EclipseKind.Penumbral) {
            "nur Halbschatten"
        } else {
            "${(eclipse.obscuration * 100.0).format(1)} % im Kernschatten"
        }
        SkyEvent(
            title = eclipse.kind.germanEclipseTitle("Mondfinsternis"),
            kind = SkyEventKind.LUNAR_ECLIPSE,
            instant = maximum,
            timeIsApproximate = false,
            status = "Zum Maximum an deinem Standort sichtbar",
            statusColor = Color(0xFF76E0A0),
            facts = "$obscurationText · Mond ${altitude.format(0)}° hoch",
            description = "Halbschatten ab ${formatEventTime(penumbralStart)} · Maximum ${formatEventTime(maximum)} · Ende ${formatEventTime(penumbralEnd)}"
        )
    }
    return (solar + lunar).toList()
}

private fun EclipseKind.germanEclipseTitle(noun: String): String = when (this) {
    EclipseKind.Penumbral -> "Halbschatten-$noun"
    EclipseKind.Partial -> "Partielle $noun"
    EclipseKind.Annular -> "Ringförmige $noun"
    EclipseKind.Total -> "Totale $noun"
}

private fun formatEventTime(instant: Instant): String = DateTimeFormatter.ofPattern("HH:mm")
    .withZone(ZoneId.systemDefault()).format(instant)

private fun AstroTime.toInstant(): Instant = Instant.ofEpochMilli(toMillisecondsSince1970())

private fun moonIlluminationPercent(instant: Instant): Double {
    val julianDate = instant.epochSecond / 86400.0 + 2440587.5
    val cycles = (julianDate - 2451550.1) / 29.53058867
    val phase = cycles - floor(cycles)
    return (1.0 - cos(2.0 * PI * phase)) * 50.0
}

internal data class ScorePenalty(
    val label: String,
    val points: Int,
    val explanation: String
)

internal data class AstraScoreBreakdown(
    val score: Int,
    val penalties: List<ScorePenalty>
)

internal object AstraScoreCalculator {
    fun calculate(
        weather: WeatherSnapshot,
        observer: GeoPoint,
        instant: Instant,
        lightPollution: LightPollutionEstimate? = null
    ): AstraScoreBreakdown {
        return calculate(
            cloudCover = weather.cloudCover,
            rainProbability = weather.forecast.firstOrNull()?.rainProbability ?: 0,
            windSpeed = weather.windSpeed,
            visibilityMeters = weather.visibility,
            observer = observer,
            instant = instant,
            lightPollution = lightPollution
        )
    }

    fun calculate(
        cloudCover: Int,
        rainProbability: Int,
        windSpeed: Double,
        visibilityMeters: Double,
        observer: GeoPoint,
        instant: Instant,
        lightPollution: LightPollutionEstimate? = null
    ): AstraScoreBreakdown {
        val visibilityKilometers = visibilityMeters / 1_000.0
        val moon = SolarSystemCatalog.horizontal(Body.Moon, observer, instant)
        val moonIllumination = illumination(Body.Moon, instant.toAstroTime()).phaseFraction

        val penalties = listOf(
            ScorePenalty(
                "Bewölkung",
                (cloudCover * 0.40).roundToInt().coerceIn(0, 40),
                "$cloudCover % Wolken · maximal −40"
            ),
            ScorePenalty(
                "Regenrisiko",
                (rainProbability * 0.10).roundToInt().coerceIn(0, 10),
                "$rainProbability % in der nächsten Prognosestufe · maximal −10"
            ),
            ScorePenalty(
                "Wind",
                (((windSpeed - 5.0).coerceAtLeast(0.0) / 25.0) * 10.0)
                    .roundToInt().coerceIn(0, 10),
                "${windSpeed.format(1)} km/h; bis 5 km/h ohne Abzug · maximal −10"
            ),
            ScorePenalty(
                "Sichtweite",
                (((20.0 - visibilityKilometers).coerceAtLeast(0.0) / 20.0) * 10.0)
                    .roundToInt().coerceIn(0, 10),
                "${visibilityKilometers.format(1)} km; ab 20 km ohne Abzug · maximal −10"
            ),
            ScorePenalty(
                "Mondlicht",
                (moonIllumination * sin(Math.toRadians(moon.altitude)).coerceAtLeast(0.0) * 10.0)
                    .roundToInt().coerceIn(0, 10),
                "${(moonIllumination * 100.0).format(0)} % beleuchtet, ${moon.altitude.format(0)}° hoch · maximal −10"
            ),
            ScorePenalty(
                "Lichtverschmutzung",
                lightPollution?.let { (it.index * 0.20).roundToInt().coerceIn(0, 20) } ?: 0,
                lightPollution?.let {
                    "NASA ${it.sourceYear} (veraltet) · Astra-Bildlichtindex ${it.index}/100 · Bortle-Hinweis ≈ ${it.bortleClass} (unvalidiert) · maximal −20"
                } ?: "Noch kein Bildlichtindex verfügbar · kein Abzug"
            )
        )
        return AstraScoreBreakdown(
            score = (100 - penalties.sumOf { it.points }).coerceIn(0, 100),
            penalties = penalties
        )
    }
}

@Composable
private fun ObservationScore(
    weather: WeatherSnapshot,
    observer: GeoPoint,
    lightPollution: LightPollutionEstimate?
) {
    var detailsVisible by remember { mutableStateOf(false) }
    val breakdown = remember(weather, observer, lightPollution) {
        AstraScoreCalculator.calculate(weather, observer, weather.observedAt, lightPollution)
    }
    val score = breakdown.score
    val label = when {
        score >= 75 -> "Sehr gute Sicht"
        score >= 50 -> "Brauchbare Sicht"
        score >= 25 -> "Eingeschränkt"
        else -> "Ungünstig"
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = if (LocalOledMode.current) Color.Black else Color(0xFF122A49)),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, if (LocalOledMode.current) Color(0xFF1E1E1E) else AstraBlue.copy(alpha = 0.28f))
    ) {
        Column {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(64.dp).background(AstraBlue.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text("$score", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AstraBlue) }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Astra-Score von 100 Punkten", color = Color(0xFFAAB8CE))
                }
            }
            Text(
                "Veraltete Lichtdaten · NASA ${lightPollution?.sourceYear ?: LightPollutionModel.sourceMetadata.dataYear}\n" +
                    "Die Datenbasis für den Lichtanteil ist keine aktuelle Erfassung. Die heutige Beleuchtung kann abweichen. " +
                    "Werte der externen Lichtkarte fließen nicht in den Score ein.",
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 16.dp),
                color = StarGold,
                fontSize = 13.sp
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Row(
                Modifier.fillMaxWidth().clickable { detailsVisible = !detailsVisible }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Info, contentDescription = null, tint = AstraBlue)
                Spacer(Modifier.width(10.dp))
                Text(
                    if (detailsVisible) "Berechnung ausblenden" else "Wie setzt sich der Score zusammen?",
                    color = AstraBlue,
                    fontWeight = FontWeight.Bold
                )
            }
            if (detailsVisible) {
                Column(
                    Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    Text("Startwert 100", fontWeight = FontWeight.Bold)
                    breakdown.penalties.forEach { penalty ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(penalty.label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(penalty.explanation, color = Color(0xFFAAB8CE), fontSize = 12.sp)
                            }
                            Text("−${penalty.points}", color = if (penalty.points > 0) StarGold else Color(0xFF76E0A0))
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Text("Ergebnis: $score / 100", fontWeight = FontWeight.Bold, color = AstraBlue)
                    Text(
                        "Der Lichtabzug nutzt die Bildhelligkeit des NASA-VIIRS-Komposits von 2016. Index und Bortle-Einordnung sind unvalidierte Näherungen, keine SQM-Messung. Das Geländeprofil beeinflusst die sichtbare Horizontlinie, aber nicht das Wetter.",
                        color = Color(0xFFAAB8CE),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier,
        colors = CardDefaults.cardColors(containerColor = AstraSurface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, AstraOutline.copy(alpha = 0.65f))
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(label, color = Color(0xFFAAB8CE), fontSize = 13.sp)
            Spacer(Modifier.height(5.dp))
            Text(value, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun ObservationPlanScreen(
    location: GeoPoint?,
    favoriteIds: Set<String>,
    openObject: (String) -> Unit,
    openEventTime: (SavedSkyEvent) -> Unit,
    savedEvents: List<SavedSkyEvent>,
    reminderHours: Int,
    notificationsGranted: Boolean,
    setReminderHours: (Int) -> Unit,
    removeFavorite: (String) -> Unit,
    removeEvent: (SavedSkyEvent) -> Unit,
    requestNotifications: () -> Unit,
    toggleFavorite: ((CelestialObject) -> Unit)? = null,
    privacy: PrivacyOptions = PrivacyOptions(),
    catalogStore: SkyCatalogStore? = null,
    logbookEntries: List<ObservationLogEntry> = emptyList(),
    logbookError: String? = null,
    onSaveLogEntry: (ObservationLogEntry) -> Unit = {},
    onDeleteLogEntry: (String) -> Unit = {},
    onDeleteAllLogEntries: () -> Unit = {},
    onImportLogbook: (String, Boolean) -> LogbookImportResult = { _, _ -> LogbookImportResult(false, 0, 0, "") }
) {
    val context = LocalContext.current
    val observer = location ?: GeoPoint(52.52, 13.405, 34.0)
    val store = catalogStore ?: remember(context) { ProcessSkyCatalogs.get(context) }
    var catalogRetry by remember { mutableIntStateOf(0) }
    val catalogs = rememberSkyCatalogs(store, catalogRetry)
    val instant = remember(observer) { Instant.now() }
    val solarSystem = remember(observer, instant) { SolarSystemCatalog.at(observer, instant) }
    val catalog = remember(catalogs.stars, catalogs.deepSky, solarSystem) {
        (catalogs.stars ?: StarCatalog.fallbackObjects) + catalogs.deepSky.orEmpty() + solarSystem
    }
    val favorites = remember(catalog, favoriteIds) { catalog.filter { it.catalogId in favoriteIds } }
    val zone = ZoneId.systemDefault()
    val dateFormat = DateTimeFormatter.ofPattern("EEE, d. MMM yyyy · HH:mm", Locale.GERMAN).withZone(zone)
    val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN).withZone(zone)

    var showNewLogDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<ObservationLogEntry?>(null) }
    var showExportImportDialog by remember { mutableStateOf(false) }
    var viewingPhotoFile by remember { mutableStateOf<File?>(null) }
    var showDeleteAllConfirmDialog by remember { mutableStateOf(false) }

    var weatherSnapshot by remember { mutableStateOf<WeatherSnapshot?>(null) }
    LaunchedEffect(observer, privacy.online) {
        if (privacy.online) {
            WeatherRepository.load(observer) { result ->
                val snapshot = result.getOrNull()
                weatherSnapshot = snapshot
                if (snapshot != null) {
                    val score = (100 - snapshot.cloudCover).coerceIn(0, 100)
                    AstraWidgetUpdater.saveWeatherScore(context, score)
                    AstraWidgetUpdater.updateAllWidgets(context)
                }
            }
        } else {
            weatherSnapshot = null
        }
    }

    val loggedCatalogIds = remember(logbookEntries) {
        ObservationCatalogMatcher.buildLoggedTokens(logbookEntries)
    }
    val challengeProgressList = remember(logbookEntries) {
        ObservationChallengeRegistry.evaluateAll(logbookEntries)
    }
    val dewReport = remember(weatherSnapshot) {
        weatherSnapshot?.let { DewMonitor.assessRisk(it.temperature, it.relativeHumidity) }
    }

    val tonightWindow = remember(observer, instant, weatherSnapshot, zone) {
        TonightWindowCalculator.calculate(
            observer = observer,
            now = instant,
            weather = weatherSnapshot,
            zone = zone
        )
    }

    var selectedEquipment by remember { mutableStateOf(ObservationEquipment.ALL) }
    val recommendedTargets = remember(observer, tonightWindow, instant, selectedEquipment, catalogs.objectsReady) {
        TonightTargetEngine.evaluate(
            observer = observer,
            window = tonightWindow,
            now = instant,
            equipmentFilter = selectedEquipment,
            zone = zone
        )
    }

    Column(
        Modifier.fillMaxSize().background(screenBackgroundBrush).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(6.dp))
        AstraScreenHeader(
            eyebrow = "ASTRA PLAN",
            title = "Beobachtungsplaner",
            subtitle = "${favoriteIds.size} vorgemerkte Objekte · ${savedEvents.size} Ereignisse",
            icon = Icons.Rounded.Bookmarks
        )
        logbookError?.let { Text(it, color = Color(0xFFFF8B8B), modifier = Modifier.testTag("logbook-error")) }
        if (catalogs.failed) {
            Text("Offline-Katalog unvollständig. Deine Vormerkungen bleiben erhalten.", color = StarGold)
            TextButton(onClick = { catalogRetry++ }) { Text("Katalog erneut laden") }
        } else if (!catalogs.complete) {
            Text("Offline-Katalog wird geladen · Vormerkungen bleiben erhalten", color = AstraTextMuted)
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = AstraSurface),
            border = BorderStroke(1.dp, AstraOutline.copy(alpha = 0.75f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.DarkMode, null, tint = AstraBlue)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Beobachtungsfenster heute Nacht", fontWeight = FontWeight.Bold)
                        Text(
                            tonightWindow.nightDateText,
                            color = AstraTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
                HorizontalDivider(color = AstraOutline.copy(alpha = 0.4f))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Dunkelheit: ${tonightWindow.darknessText}",
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Text(
                        "Sonnenuntergang: ${tonightWindow.sunsetText} · Sonnenaufgang: ${tonightWindow.sunrisesText}",
                        fontSize = 12.sp,
                        color = AstraTextMuted
                    )
                    Text(
                        "Mond: ${tonightWindow.moonSummary} · ${tonightWindow.moonPhaseName} (${tonightWindow.moonPhasePercent}%)",
                        fontSize = 13.sp,
                        color = StarGold
                    )
                    Text(
                        "Bedingungen: ${tonightWindow.bestWindowSummary}",
                        fontSize = 13.sp,
                        color = AstraSuccess
                    )
                    Text(
                        tonightWindow.weatherNotice,
                        fontSize = 12.sp,
                        color = AstraTextMuted
                    )
                }
                Text(
                    "Alle Berechnungen erfolgen lokal auf deinem Gerät. Reale Sichtbarkeit hängt von Wetter, Dunst und Lichtverschmutzung ab.",
                    color = AstraTextMuted,
                    fontSize = 11.sp
                )
            }
        }

        dewReport?.let { dew ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Cloud, null, tint = AstraBlue)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Taupunkt & Beschlagsrisiko", fontWeight = FontWeight.Bold)
                            Text("Sonntag (1990) Magnus-Tetens Formel · 100% offline", color = AstraTextMuted, fontSize = 11.sp)
                        }
                        val (chipBg, chipText) = when (dew.riskLevel) {
                            DewRiskLevel.LOW -> Color(0xFF1B382B) to AstraSuccess
                            DewRiskLevel.MODERATE -> Color(0xFF3E3317) to StarGold
                            DewRiskLevel.HIGH -> Color(0xFF4A2218) to Color(0xFFFF9E80)
                            DewRiskLevel.CRITICAL -> Color(0xFF4D1414) to Color(0xFFFF6E6E)
                        }
                        Surface(shape = RoundedCornerShape(8.dp), color = chipBg) {
                            Text(
                                text = dew.riskLevel.label,
                                color = chipText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = AstraOutline.copy(alpha = 0.4f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Lufttemperatur", fontSize = 11.sp, color = AstraTextMuted)
                            Text("${dew.ambientTempCelsius.format(1)} °C", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                        Column {
                            Text("Relative Feuchte", fontSize = 11.sp, color = AstraTextMuted)
                            Text("${dew.relativeHumidityPercent.format(0)} %", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                        Column {
                            Text("Taupunkt", fontSize = 11.sp, color = AstraTextMuted)
                            Text("${dew.dewPointCelsius.format(1)} °C", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = StarGold)
                        }
                        val chipText = when (dew.riskLevel) {
                            DewRiskLevel.LOW -> AstraSuccess
                            DewRiskLevel.MODERATE -> StarGold
                            DewRiskLevel.HIGH -> Color(0xFFFF9E80)
                            DewRiskLevel.CRITICAL -> Color(0xFFFF6E6E)
                        }
                        Column {
                            Text("Taumarge (ΔT)", fontSize = 11.sp, color = AstraTextMuted)
                            Text("${dew.dewMarginCelsius.format(1)} K", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = chipText)
                        }
                    }
                    Text(
                        text = dew.riskLevel.description,
                        fontSize = 11.sp,
                        color = AstraTextMuted
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Star, null, tint = StarGold)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Beobachtungs-Challenges", fontWeight = FontWeight.Bold)
                        Text("Fortschritt synchronisiert mit deinem Beobachtungstagebuch", color = AstraTextMuted, fontSize = 11.sp)
                    }
                }
                HorizontalDivider(color = AstraOutline.copy(alpha = 0.4f))
                challengeProgressList.forEach { progress ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(progress.type.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Text("${progress.observedCount} / ${progress.totalCount} (${progress.percentComplete.format(1)}%)", fontSize = 12.sp, color = StarGold)
                        }
                        LinearProgressIndicator(
                            progress = { (progress.percentComplete / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clipToBounds(),
                            color = StarGold,
                            trackColor = AstraSurfaceHigh
                        )
                        Text(progress.type.description, fontSize = 10.sp, color = AstraTextMuted)
                    }
                }
            }
        }

        AstraSectionTitle("Was lohnt sich heute Nacht?", "Ausgewählte Highlights über 16° Höhe")
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ObservationEquipment.values().forEach { eq ->
                FilterChip(
                    selected = selectedEquipment == eq,
                    onClick = { selectedEquipment = eq },
                    label = { Text(eq.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AstraBlue,
                        selectedLabelColor = Night,
                        containerColor = AstraSurfaceHigh,
                        labelColor = Color.White
                    )
                )
            }
        }

        if (recommendedTargets.isEmpty()) {
            EmptyPlanCard("Keine geeigneten Objekte für diesen Ausrüstungsfilter über 16° Höhe gefunden.")
        } else {
            recommendedTargets.take(8).forEach { target ->
                val isFav = target.objectData.catalogId in favoriteIds
                Card(
                    colors = CardDefaults.cardColors(containerColor = AstraSurface),
                    border = BorderStroke(1.dp, AstraOutline.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(target.objectData.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                    if (ObservationCatalogMatcher.isObserved(target.objectData.catalogId, loggedCatalogIds)) {
                                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF163228)) {
                                            Text("✓ Im Logbuch", color = AstraSuccess, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                                Text(
                                    target.highlightTitle,
                                    color = AstraBlue,
                                    fontSize = 12.sp
                                )
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = AstraSurfaceHigh),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "${target.equipment.label} · Score ${target.qualityScore}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    color = StarGold,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Text(
                            "Beste Zeit: ${timeFormat.format(target.peakInstant)} Uhr (Höhe: ${target.peakAltitudeDegrees.format(0)}°)" +
                                if (target.moonSeparationDegrees >= 0) " · Mondabstand: ${target.moonSeparationDegrees.format(0)}°" else "",
                            fontSize = 12.sp,
                            color = AstraSuccess
                        )
                        Text(
                            target.reason,
                            fontSize = 12.sp,
                            color = AstraTextMuted
                        )

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { openObject(target.objectData.catalogId) }) {
                                Text("In Karte öffnen")
                            }
                            IconButton(
                                onClick = {
                                    if (isFav) {
                                        removeFavorite(target.objectData.catalogId)
                                    } else {
                                        toggleFavorite?.invoke(target.objectData)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isFav) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                    contentDescription = if (isFav) "Aus Favoriten entfernen" else "Zu Favoriten hinzufügen",
                                    tint = if (isFav) StarGold else AstraTextMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = AstraSurface),
            border = BorderStroke(1.dp, AstraOutline.copy(alpha = 0.75f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Notifications, null, tint = StarGold)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Ereigniserinnerungen", fontWeight = FontWeight.Bold)
                        Text(
                            if (notificationsGranted) "Benachrichtigungen sind freigegeben"
                            else "Benachrichtigungen sind noch nicht freigegeben",
                            color = if (notificationsGranted) AstraSuccess else StarGold,
                            fontSize = 12.sp
                        )
                    }
                }
                Text("Vorlauf", color = AstraTextMuted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 6, 24, 48).forEach { hours ->
                        Button(
                            onClick = { setReminderHours(hours) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (reminderHours == hours) StarGold else AstraSurfaceHigh,
                                contentColor = if (reminderHours == hours) Night else Color.White
                            )
                        ) { Text(if (hours < 24) "${hours} h" else "${hours / 24} T") }
                    }
                }
                if (!notificationsGranted) {
                    Button(
                        onClick = requestNotifications,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AstraBlue,
                            contentColor = Night
                        )
                    ) { Text("Benachrichtigungen erlauben") }
                }
                Text(
                    "Android kann die Zustellung zur Schonung des Akkus leicht verzögern.",
                    color = AstraTextMuted,
                    fontSize = 11.sp
                )
            }
        }

        AstraSectionTitle("Vorgemerkte Ereignisse", "Sternsymbol im Kalender zum Hinzufügen")
        if (savedEvents.isEmpty()) {
            EmptyPlanCard("Noch keine Ereignisse vorgemerkt.")
        } else savedEvents.sortedBy { it.instantEpochSeconds }.forEach { event ->
            Card(colors = CardDefaults.cardColors(containerColor = AstraSurface)) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(event.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(event.kind, color = AstraBlue, fontSize = 12.sp)
                        Text(dateFormat.format(Instant.ofEpochSecond(event.instantEpochSeconds)), color = AstraTextMuted)
                        TextButton(onClick = { openEventTime(event) }) { Text("Zeit in Karte öffnen") }
                    }
                    IconButton(onClick = { removeEvent(event) }) {
                        Icon(Icons.Rounded.Star, "Vormerkung entfernen", tint = StarGold)
                    }
                }
            }
        }

        AstraSectionTitle("Favorisierte Himmelsobjekte", "Sterne und Deep-Sky-Ziele für deine Nacht")
        if (favoriteIds.isEmpty()) {
            EmptyPlanCard("Tippe ein Objekt in der Sternkarte an und füge es zur Liste hinzu.")
        } else if (favorites.size < favoriteIds.size) {
            EmptyPlanCard(if (!catalogs.objectsReady) "Einige Vormerkungen warten noch auf den vollständigen Katalog."
                else "Einige vorgemerkte Objekte sind im aktuellen Katalog nicht verfügbar. Deine Vormerkungen bleiben erhalten.")
        }
        favorites.forEach { objectData ->
            val position = remember(objectData, observer, instant) { coordinatesAt(objectData, observer, instant) }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(objectData.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            if (ObservationCatalogMatcher.isObserved(objectData.catalogId, loggedCatalogIds)) {
                                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF163228)) {
                                    Text("✓ Im Logbuch", color = AstraSuccess, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Text("${objectData.objectType.label} · ${objectData.catalogId}", color = AstraBlue, fontSize = 12.sp)
                        Text(
                            "Jetzt: Az ${position.azimuth.format(0)}° · Höhe ${position.altitude.format(0)}°",
                            color = if (position.altitude > 0) AstraSuccess else AstraTextMuted,
                            fontSize = 12.sp
                        )
                        TextButton(onClick = { openObject(objectData.catalogId) }) { Text("In Karte öffnen") }
                    }
                    IconButton(onClick = { removeFavorite(objectData.catalogId) }) {
                        Icon(Icons.Rounded.Star, "Favorit entfernen", tint = StarGold)
                    }
                }
            }
        }

        AstraSectionTitle("Beobachtungstagebuch", "Eigene Beobachtungen, Notizen & Fotos (100% lokal)")
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { showNewLogDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AstraBlue,
                    contentColor = Night
                )
            ) {
                Icon(Icons.Rounded.EditNote, null)
                Spacer(Modifier.width(6.dp))
                Text("Eintrag verfassen")
            }
            OutlinedButton(
                onClick = { showExportImportDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, AstraOutline)
            ) {
                Icon(Icons.Rounded.ImportExport, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Export / Import")
            }
        }

        if (logbookEntries.isEmpty()) {
            EmptyPlanCard("Noch keine Beobachtungen erfasst. Halte deine Nächte, Teleskopnotizen und Seeing-Bewertungen hier fest – 100% lokal auf diesem Gerät.")
        } else {
            logbookEntries.forEach { entry ->
                LogbookEntryCard(
                    entry = entry,
                    context = context,
                    dateFormat = dateFormat,
                    onOpenInSky = {
                        if (entry.objectCatalogId.isNotBlank()) openObject(entry.objectCatalogId)
                    },
                    onEdit = { editingEntry = entry },
                    onDelete = { onDeleteLogEntry(entry.id) },
                    onShowPhoto = { viewingPhotoFile = it }
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { showDeleteAllConfirmDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF8B8B))
                ) {
                    Icon(Icons.Rounded.Delete, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Alle Einträge löschen", fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }

    if (showNewLogDialog) {
        ObservationLogEntryDialog(
            initialEntry = null,
            currentLocation = location,
            onDismiss = { showNewLogDialog = false },
            onSave = { newEntry ->
                onSaveLogEntry(newEntry)
                showNewLogDialog = false
            }
        )
    }

    editingEntry?.let { entryToEdit ->
        ObservationLogEntryDialog(
            initialEntry = entryToEdit,
            currentLocation = location,
            onDismiss = { editingEntry = null },
            onSave = { updatedEntry ->
                onSaveLogEntry(updatedEntry)
                editingEntry = null
            }
        )
    }

    if (showExportImportDialog) {
        LogbookExportImportDialog(
            entries = logbookEntries,
            onImport = onImportLogbook,
            onDismiss = { showExportImportDialog = false }
        )
    }

    viewingPhotoFile?.let { photoFile ->
        FullPhotoDialog(
            photoFile = photoFile,
            onDismiss = { viewingPhotoFile = null }
        )
    }

    if (showDeleteAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirmDialog = false },
            containerColor = NightBlue,
            title = { Text("Alle Tagebucheinträge löschen?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Möchtest du wirklich alle ${logbookEntries.size} Einträge und die zugehörigen lokalen Fotos unwiderruflich von diesem Gerät löschen?",
                    color = AstraTextMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAllLogEntries()
                        showDeleteAllConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC43828), contentColor = Color.White)
                ) {
                    Text("Unwiderruflich löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllConfirmDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun EmptyPlanCard(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Text(text, Modifier.fillMaxWidth().padding(18.dp), color = AstraTextMuted)
    }
}

@Composable
private fun LogbookEntryCard(
    entry: ObservationLogEntry,
    context: Context,
    dateFormat: DateTimeFormatter,
    onOpenInSky: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShowPhoto: (File) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(entry.objectName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                    val subtitle = buildList {
                        if (entry.objectCatalogId.isNotBlank()) add(entry.objectCatalogId)
                        add(entry.objectType.label)
                    }.joinToString(" · ")
                    Text(subtitle, color = AstraBlue, fontSize = 12.sp)
                    val dateStr = runCatching {
                        dateFormat.format(Instant.ofEpochSecond(entry.timestampEpochSeconds))
                    }.getOrDefault("")
                    if (dateStr.isNotBlank()) {
                        Text(dateStr, color = AstraTextMuted, fontSize = 12.sp)
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, "Bearbeiten", tint = AstraBlue, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.Delete, "Löschen", tint = Color(0xFFFF8B8B), modifier = Modifier.size(20.dp))
                    }
                }
            }

            if (entry.seeingRating > 0) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Seeing:", fontSize = 12.sp, color = AstraTextMuted)
                    repeat(5) { index ->
                        val active = index < entry.seeingRating
                        Icon(
                            imageVector = if (active) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            contentDescription = null,
                            tint = if (active) StarGold else AstraOutline,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            val conditions = buildList {
                entry.seeingPickering?.let { add("Pickering: $it/10") }
                entry.seeingAntoniadi?.let { add("Antoniadi: $it") }
                entry.nelm?.let { add("Grenzgröße: ${it.format(1)} mag") }
            }.joinToString(" · ")
            if (conditions.isNotBlank()) {
                Text("Bedingungen: $conditions", fontSize = 12.sp, color = StarGold)
            }

            if (entry.equipment.isNotBlank()) {
                Text("Ausrüstung: ${entry.equipment}", fontSize = 13.sp, color = Color(0xFFD0DCF0))
            }

            if (entry.notes.isNotBlank()) {
                Text(entry.notes, fontSize = 14.sp, color = Color.White)
            }

            if (entry.locationName != null || entry.latitude != null) {
                val locStr = buildString {
                    append("Ort: ")
                    if (!entry.locationName.isNullOrBlank()) append(entry.locationName)
                    if (entry.latitude != null && entry.longitude != null) {
                        if (!entry.locationName.isNullOrBlank()) append(" (")
                        append("${entry.latitude.format(2)}°, ${entry.longitude.format(2)}°")
                        if (!entry.locationName.isNullOrBlank()) append(")")
                    }
                }
                Text(locStr, fontSize = 11.sp, color = AstraTextMuted)
            }

            entry.photoFileName?.let { fileName ->
                val photoFile = remember(fileName) { ObservationLogbookStore.getPhotoFile(context, fileName) }
                if (photoFile.exists()) {
                    val bitmap = remember(photoFile) {
                        runCatching { BitmapFactory.decodeFile(photoFile.absolutePath) }.getOrNull()
                    }
                    if (bitmap != null) {
                        Row(
                            Modifier
                                .clickable { onShowPhoto(photoFile) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Beobachtungsfoto",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clipToBounds()
                                    .background(NightBlue, RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Column {
                                Text("Foto ansehen", color = AstraBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("Tippen zum Vergrößern", color = AstraTextMuted, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            if (entry.objectCatalogId.isNotBlank()) {
                TextButton(
                    onClick = onOpenInSky,
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text("In Karte öffnen")
                }
            }
        }
    }
}

@Composable
private fun ObservationLogEntryDialog(
    initialEntry: ObservationLogEntry? = null,
    initialObject: CelestialObject? = null,
    currentLocation: GeoPoint?,
    onDismiss: () -> Unit,
    onSave: (ObservationLogEntry) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialEntry?.objectName ?: initialObject?.name ?: "") }
    var catalogId by remember { mutableStateOf(initialEntry?.objectCatalogId ?: initialObject?.catalogId ?: "") }
    var objectType by remember { mutableStateOf(initialEntry?.objectType ?: initialObject?.objectType ?: CelestialType.STAR) }
    var notes by remember { mutableStateOf(initialEntry?.notes ?: "") }
    var equipment by remember { mutableStateOf(initialEntry?.equipment ?: "") }
    var seeing by remember { mutableIntStateOf(initialEntry?.seeingRating ?: 0) }
    var attachLocation by remember { mutableStateOf(initialEntry?.latitude != null || initialEntry?.longitude != null || initialEntry?.locationName != null) }
    var locationCustomName by remember { mutableStateOf(initialEntry?.locationName ?: "") }
    var photoFileName by remember { mutableStateOf(initialEntry?.photoFileName) }
    val draftPhotos = remember { mutableSetOf<String>() }
    var saveError by remember { mutableStateOf<String?>(null) }
    var dialogActive by remember { mutableStateOf(true) }
    DisposableEffect(Unit) {
        onDispose {
            dialogActive = false
            draftPhotos.forEach { ObservationLogbookStore.deletePhotoFile(context, it) }
        }
    }
    var seeingPickeringText by remember { mutableStateOf(initialEntry?.seeingPickering?.toString() ?: "") }
    var seeingAntoniadi by remember { mutableStateOf(initialEntry?.seeingAntoniadi ?: "") }
    var nelmText by remember { mutableStateOf(initialEntry?.nelm?.let { "%.1f".format(Locale.US, it) } ?: "") }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val saved = ObservationLogbookStore.savePhotoFromUri(context, it)
            if (saved != null) {
                if (dialogActive) {
                    draftPhotos.add(saved)
                    photoFileName = saved
                } else ObservationLogbookStore.deletePhotoFile(context, saved)
            } else saveError = "Foto konnte nicht hinzugefügt werden."
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 620.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    if (initialEntry == null) "Beobachtung eintragen" else "Beobachtung bearbeiten",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Objektname") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AstraBlue,
                        unfocusedBorderColor = AstraOutline,
                        focusedLabelColor = AstraBlue,
                        unfocusedLabelColor = AstraTextMuted
                    )
                )

                OutlinedTextField(
                    value = catalogId,
                    onValueChange = { catalogId = it },
                    label = { Text("Katalog-ID (optional, z. B. M 31)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AstraBlue,
                        unfocusedBorderColor = AstraOutline,
                        focusedLabelColor = AstraBlue,
                        unfocusedLabelColor = AstraTextMuted
                    )
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Seeing-Bewertung (1 bis 5 Sterne)", fontSize = 12.sp, color = AstraTextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        (1..5).forEach { star ->
                            IconButton(
                                onClick = { seeing = if (seeing == star) 0 else star },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (star <= seeing) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                    contentDescription = "$star Sterne",
                                    tint = if (star <= seeing) StarGold else AstraOutline,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        if (seeing > 0) {
                            val seeingLabel = when (seeing) {
                                1 -> "Sehr schlecht"
                                2 -> "Mäßig"
                                3 -> "Durchschnittlich"
                                4 -> "Gut"
                                5 -> "Exzellent"
                                else -> ""
                            }
                            Text(seeingLabel, fontSize = 12.sp, color = StarGold)
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Standardisierte Seeing- & Himmelsbedingungen (optional)", fontSize = 12.sp, color = AstraTextMuted)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = seeingPickeringText,
                            onValueChange = { seeingPickeringText = it },
                            label = { Text("Pickering (1–10)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedBorderColor = AstraBlue, unfocusedBorderColor = AstraOutline,
                                focusedLabelColor = AstraBlue, unfocusedLabelColor = AstraTextMuted
                            )
                        )
                        OutlinedTextField(
                            value = seeingAntoniadi,
                            onValueChange = { seeingAntoniadi = it },
                            label = { Text("Antoniadi (I–V)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedBorderColor = AstraBlue, unfocusedBorderColor = AstraOutline,
                                focusedLabelColor = AstraBlue, unfocusedLabelColor = AstraTextMuted
                            )
                        )
                    }
                    OutlinedTextField(
                        value = nelmText,
                        onValueChange = { nelmText = it },
                        label = { Text("Grenzgröße NELM / fst (z. B. 6.2 mag)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = AstraBlue, unfocusedBorderColor = AstraOutline,
                            focusedLabelColor = AstraBlue, unfocusedLabelColor = AstraTextMuted
                        )
                    )
                }

                OutlinedTextField(
                    value = equipment,
                    onValueChange = { equipment = it },
                    label = { Text("Ausrüstung (z. B. 8\" Dobson, 10x50)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AstraBlue,
                        unfocusedBorderColor = AstraOutline,
                        focusedLabelColor = AstraBlue,
                        unfocusedLabelColor = AstraTextMuted
                    )
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Beobachtungsnotizen") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AstraBlue,
                        unfocusedBorderColor = AstraOutline,
                        focusedLabelColor = AstraBlue,
                        unfocusedLabelColor = AstraTextMuted
                    )
                )

                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(AstraSurface, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = attachLocation,
                            onCheckedChange = { attachLocation = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = AstraBlue,
                                checkmarkColor = Night
                            )
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Standort an Eintrag anhängen", fontSize = 13.sp, color = Color.White)
                    }
                    if (attachLocation) {
                        val entryLocation = initialEntry?.latitude?.let { latitude ->
                            initialEntry.longitude?.let { longitude -> latitude to longitude }
                        }
                        if (entryLocation != null) {
                            Text("Koordinaten dieses Eintrags: ${entryLocation.first.format(3)}°, ${entryLocation.second.format(3)}°",
                                fontSize = 11.sp, color = AstraSuccess)
                        } else if (currentLocation != null) {
                            Text(
                                "Aktuelle Koordinaten: ${currentLocation.latitude.format(3)}°, ${currentLocation.longitude.format(3)}°",
                                fontSize = 11.sp,
                                color = AstraSuccess
                            )
                        } else {
                            Text("Kein Live-GPS aktiv. Optional Ortsnamen angeben.", fontSize = 11.sp, color = AstraTextMuted)
                        }
                        OutlinedTextField(
                            value = locationCustomName,
                            onValueChange = { locationCustomName = it },
                            label = { Text("Ortsname (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = AstraBlue,
                                unfocusedBorderColor = AstraOutline,
                                focusedLabelColor = AstraBlue,
                                unfocusedLabelColor = AstraTextMuted
                            )
                        )
                        Text(
                            "Standortdaten werden ausschließlich lokal auf diesem Gerät gespeichert.",
                            fontSize = 10.sp,
                            color = AstraTextMuted
                        )
                    }
                }

                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(AstraSurface, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Foto (optional)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    photoFileName?.let { currentPhoto ->
                        val photoFile = ObservationLogbookStore.getPhotoFile(context, currentPhoto)
                        if (photoFile.exists()) {
                            val bitmap = remember(currentPhoto) {
                                runCatching { BitmapFactory.decodeFile(photoFile.absolutePath) }.getOrNull()
                            }
                            if (bitmap != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(60.dp).clipToBounds(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Button(
                                        onClick = {
                                            photoFileName = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5A2020), contentColor = Color(0xFFFFB4B4))
                                    ) {
                                        Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Entfernen", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    } ?: run {
                        OutlinedButton(
                            onClick = { photoLauncher.launch("image/*") },
                            border = BorderStroke(1.dp, AstraOutline),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Rounded.AddPhotoAlternate, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Foto auswählen", fontSize = 12.sp)
                        }
                        Text(
                            "Das Bild wird in den isolierten App-Speicher kopiert.",
                            fontSize = 10.sp,
                            color = AstraTextMuted
                        )
                    }
                }

                saveError?.let { Text(it, color = Color(0xFFFF8B8B), fontSize = 12.sp) }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Abbrechen")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val finalLat = if (attachLocation) initialEntry?.latitude ?: currentLocation?.latitude else null
                            val finalLon = if (attachLocation) initialEntry?.longitude ?: currentLocation?.longitude else null
                            val finalLocName = if (attachLocation) locationCustomName.ifBlank { null } else null
                            val parsedPickering = seeingPickeringText.toIntOrNull()?.takeIf { SeeingScaleValidator.isValidPickering(it) }
                            val parsedAntoniadi = seeingAntoniadi.trim().uppercase().takeIf { SeeingScaleValidator.isValidAntoniadi(it) }
                            val parsedNelm = nelmText.replace(',', '.').toDoubleOrNull()?.takeIf { SeeingScaleValidator.isValidNelm(it) }
                            val newEntry = ObservationLogEntry(
                                id = initialEntry?.id ?: UUID.randomUUID().toString(),
                                objectCatalogId = catalogId.trim(),
                                objectName = name.ifBlank { "Unbekannte Beobachtung" }.trim(),
                                objectType = objectType,
                                timestampEpochSeconds = initialEntry?.timestampEpochSeconds ?: Instant.now().epochSecond,
                                notes = notes.trim(),
                                equipment = equipment.trim(),
                                seeingRating = seeing,
                                locationName = finalLocName,
                                latitude = finalLat,
                                longitude = finalLon,
                                photoFileName = photoFileName,
                                seeingPickering = parsedPickering,
                                seeingAntoniadi = parsedAntoniadi,
                                nelm = parsedNelm
                            )
                            try {
                                onSave(newEntry)
                                photoFileName?.let { draftPhotos.remove(it) }
                            } catch (error: Exception) {
                                saveError = "Speichern fehlgeschlagen: ${error.message}"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AstraBlue, contentColor = Night)
                    ) {
                        Text("Speichern")
                    }
                }
            }
        }
    }
}

@Composable
private fun LogbookExportImportDialog(
    entries: List<ObservationLogEntry>,
    onImport: (String, Boolean) -> LogbookImportResult,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var importText by remember { mutableStateOf("") }
    var mergeMode by remember { mutableStateOf(true) }
    var importResultMsg by remember { mutableStateOf<String?>(null) }
    val zone = ZoneId.systemDefault()
    val dateFormat = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN).withZone(zone) }

    val previewResult = remember(importText) {
        runCatching { if (importText.isBlank()) emptyList() else ObservationLogbookStore.parseEntriesJson(importText) }
    }
    val parsedPreview = previewResult.getOrDefault(emptyList())

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 620.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Tagebuch Export & Import", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)

                Card(
                    colors = CardDefaults.cardColors(containerColor = AstraSurface),
                    border = BorderStroke(1.dp, AstraOutline)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Datenschutz & EXIF-Hinweis", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StarGold)
                        Text(
                            "Exportiertes JSON enthält deine Beobachtungsnotizen im Klartext. Angehängte Fotos verbleiben auf dem Gerät. Bitte beachte: Falls du Fotodateien manuell weitergibst, können sie EXIF-Kameradaten (z. B. GPS-Koordinaten) enthalten.",
                            fontSize = 11.sp,
                            color = AstraTextMuted
                        )
                    }
                }

                Text("Export (${entries.size} Einträge lokal)", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)

                Text("1. JSON-Backup (Vollständige Sicherung)", fontSize = 12.sp, color = AstraTextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                val json = ObservationLogbookStore.exportJson(context)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Astra Beobachtungstagebuch", json))
                                Toast.makeText(context, "JSON in Zwischenablage kopiert", Toast.LENGTH_SHORT).show()
                            }.onFailure { importResultMsg = "Export fehlgeschlagen: ${it.message}" }
                        },
                        border = BorderStroke(1.dp, AstraOutline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("JSON Kopieren", fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            runCatching {
                                val json = ObservationLogbookStore.exportJson(context)
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, json)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Astra Beobachtungstagebuch JSON"))
                            }.onFailure { importResultMsg = "Export fehlgeschlagen: ${it.message}" }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AstraSurfaceHigh, contentColor = Color.White)
                    ) {
                        Icon(Icons.Rounded.Share, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("JSON Teilen", fontSize = 12.sp)
                    }
                }

                Text("2. OpenAstronomyLog 2.1 (OAL XML Standard)", fontSize = 12.sp, color = AstraTextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val xml = ObservationLogbookStore.exportOalXml(entries)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Astra OAL 2.1 XML", xml))
                            Toast.makeText(context, "OAL XML in Zwischenablage kopiert", Toast.LENGTH_SHORT).show()
                        },
                        border = BorderStroke(1.dp, AstraOutline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("OAL XML Kopieren", fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            val xml = ObservationLogbookStore.exportOalXml(entries)
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, xml)
                                type = "application/xml"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Astra OAL XML Export"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AstraSurfaceHigh, contentColor = Color.White)
                    ) {
                        Icon(Icons.Rounded.Share, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("OAL Teilen", fontSize = 12.sp)
                    }
                }

                Text("3. Rotlicht-Textzusammenfassung", fontSize = 12.sp, color = AstraTextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val text = ObservationLogbookStore.exportFormattedText(entries)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Astra Beobachtungszusammenfassung", text))
                            Toast.makeText(context, "Zusammenfassung kopiert", Toast.LENGTH_SHORT).show()
                        },
                        border = BorderStroke(1.dp, AstraOutline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Text Kopieren", fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            val text = ObservationLogbookStore.exportFormattedText(entries)
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, text)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Astra Beobachtungszusammenfassung"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AstraSurfaceHigh, contentColor = Color.White)
                    ) {
                        Icon(Icons.Rounded.Share, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Text Teilen", fontSize = 12.sp)
                    }
                }

                HorizontalDivider(color = AstraOutline)

                Text("Import", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("JSON-Daten einfügen:", fontSize = 12.sp, color = AstraTextMuted)
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                        if (text.isNotBlank()) importText = text
                    }) {
                        Text("Aus Zwischenablage", fontSize = 12.sp)
                    }
                }

                OutlinedTextField(
                    value = importText,
                    onValueChange = {
                        importText = it
                        importResultMsg = null
                    },
                    label = { Text("JSON-Inhalt") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AstraBlue,
                        unfocusedBorderColor = AstraOutline,
                        focusedLabelColor = AstraBlue,
                        unfocusedLabelColor = AstraTextMuted
                    )
                )

                if (importText.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AstraSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Vorschau:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = StarGold)
                            if (parsedPreview.isEmpty()) {
                                Text(previewResult.exceptionOrNull()?.message ?: "Keine gültigen Einträge erkannt.",
                                    fontSize = 11.sp, color = Color(0xFFFF8B8B))
                            } else {
                                Text("Erkannte Einträge: ${parsedPreview.size}", fontSize = 12.sp, color = AstraSuccess)
                                val minTime = parsedPreview.minOf { it.timestampEpochSeconds }
                                val maxTime = parsedPreview.maxOf { it.timestampEpochSeconds }
                                val minDate = dateFormat.format(Instant.ofEpochSecond(minTime))
                                val maxDate = dateFormat.format(Instant.ofEpochSecond(maxTime))
                                Text("Zeitraum: $minDate bis $maxDate", fontSize = 11.sp, color = AstraTextMuted)
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = mergeMode,
                            onCheckedChange = { mergeMode = it },
                            colors = CheckboxDefaults.colors(checkedColor = AstraBlue, checkmarkColor = Night)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (mergeMode) "Mit vorhandenen Einträgen zusammenführen" else "Vorhandene Einträge überschreiben",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = {
                            val res = onImport(importText, mergeMode)
                            importResultMsg = res.message
                            if (res.success) {
                                Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        },
                        enabled = parsedPreview.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = AstraBlue, contentColor = Night),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Jetzt ${parsedPreview.size} Einträge importieren")
                    }
                }

                importResultMsg?.let { msg ->
                    Text(msg, color = Color(0xFFFF8B8B), fontSize = 12.sp)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Schließen")
                    }
                }
            }
        }
    }
}

@Composable
private fun FullPhotoDialog(
    photoFile: File,
    onDismiss: () -> Unit
) {
    val bitmap = remember(photoFile) {
        runCatching { BitmapFactory.decodeFile(photoFile.absolutePath) }.getOrNull()
    }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AstraSurfaceHigh, contentColor = Color.White)
                ) {
                    Text("Schließen")
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Beobachtungsfoto",
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .clipToBounds(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text("Foto konnte nicht geladen werden.", color = AstraTextMuted)
                }
            }
        }
    }
}

@Composable
private fun AboutScreen(
    redLightMode: Boolean,
    setRedLightMode: (Boolean) -> Unit,
    oledBlackMode: Boolean = false,
    setOledBlackMode: (Boolean) -> Unit = {},
    privacy: PrivacyOptions,
    rememberLocation: Boolean,
    onRememberLocationChange: (Boolean) -> Unit,
    requestOnline: () -> Unit,
    setPrivacy: (PrivacyOptions) -> Unit
) {
    val context = LocalContext.current
    var showPolicy by remember { mutableStateOf(false) }
    if (showPolicy) PrivacyPolicyDialog { showPolicy = false }
    Column(
        Modifier.fillMaxSize().background(screenBackgroundBrush).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(6.dp))
        AstraScreenHeader(
            eyebrow = "VERSION ${BuildConfig.VERSION_NAME}",
            title = "Projekt Astra",
            subtitle = "Dein Begleiter für einen klaren Blick in den Nachthimmel",
            icon = Icons.Rounded.Explore
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AstraSurfaceHigh),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, StarGold.copy(alpha = 0.30f))
        ) {
            Row(
                Modifier.fillMaxWidth().clickable { setRedLightMode(!redLightMode) }.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.DarkMode, contentDescription = null, tint = StarGold)
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text("Rotlichtmodus", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Schont die Dunkeladaption bei der Beobachtung", color = AstraTextMuted, fontSize = 12.sp)
                }
                Switch(checked = redLightMode, onCheckedChange = setRedLightMode)
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AstraSurfaceHigh),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, StarGold.copy(alpha = 0.30f))
        ) {
            Row(
                Modifier.fillMaxWidth().clickable { setOledBlackMode(!oledBlackMode) }.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.DarkMode, contentDescription = null, tint = StarGold)
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text("OLED Reinstschwarz (#000000)", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Schaltet UI-Hintergründe auf reines Schwarz zur maximalen Akku- und Dunkeladaptation", color = AstraTextMuted, fontSize = 12.sp)
                }
                Switch(checked = oledBlackMode, onCheckedChange = setOledBlackMode)
            }
        }
        AboutInfoCard(
            title = "Für die Nacht gebaut",
            icon = Icons.Rounded.Public,
            body = "5.041 reale Sterne, Sonne, Mond, Planeten, Milchstraße, alle 88 IAU-Sternbildgrenzen und optional 1.016 Deep-Sky-Objekte werden passend zu Standort und Uhrzeit berechnet. Favoriten, Beobachtungslisten, Wetter und Ereignisse helfen bei der Planung."
        )
        PrivacyControls(privacy, rememberLocation, onRememberLocationChange, requestOnline, setPrivacy) {
            val cleared = runCatching { PublicTileCache.clear(context); PrivateWebViews.clearBrowserStorage() }.isSuccess
            android.widget.Toast.makeText(context,
                if (cleared) "Kartencache gelöscht" else "Kartencache konnte nicht vollständig gelöscht werden",
                android.widget.Toast.LENGTH_SHORT).show()
        }
        AboutInfoCard("Datenschutz", Icons.Rounded.GpsFixed,
            "GPS wird nur bei geöffneter App genutzt. Kamerabilder werden weder gespeichert noch übertragen. Es gibt keine Konten, Werbung oder Analyse-SDKs. Online-Anbieter können technische Protokolle führen. Details und Aufbewahrungsfristen stehen in der mitgelieferten Datenschutzerklärung.")
        TextButton(onClick = { showPolicy = true }) { Text("Datenschutzerklärung öffnen") }
        AboutInfoCard(
            title = "Genauigkeit und Sicherheit",
            icon = Icons.Rounded.Info,
            body = "AR und Sensoranzeige sind Orientierungshilfen. Kalibriere den Kompass und halte Abstand zu Magneten oder Metall. Blicke niemals ohne geeigneten Sonnenfilter direkt in die Sonne oder durch ein optisches Instrument."
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = AstraSurface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, AstraOutline.copy(alpha = 0.65f))
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Datenquellen und Lizenzen", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Sternkatalog: HYG v4.1 · CC BY-SA 4.0", color = AstraTextMuted, fontSize = 12.sp)
                Text("Milchstraße: NASA/Goddard SVS (Ernie Wright) · Gaia DR2: ESA/Gaia/DPAC · JPEG: Wikimedia Commons / PantheraLeo1359531. Nutzung mit Namensnennung; offline gebündelt.", color = AstraTextMuted, fontSize = 12.sp)
                Text("Sternbildgrenzen: CDS/VizieR VI/49 · IAU/Delporte", color = AstraTextMuted, fontSize = 12.sp)
                Text("Deep Sky: OpenNGC · CC BY-SA 4.0", color = AstraTextMuted, fontSize = 12.sp)
                Text("Himmelsaufnahmen: DSS2 via CDS HiPS2FITS", color = AstraTextMuted, fontSize = 12.sp)
                Text("Wetter: RainViewer, Open-Meteo und OpenStreetMap", color = AstraTextMuted, fontSize = 12.sp)
                Text("Ereignisse: NASA/GSFC und IMO", color = AstraTextMuted, fontSize = 12.sp)
                Text("Nachtlicht: NASA GIBS / VIIRS", color = AstraTextMuted, fontSize = 12.sp)
                Text("Gelände: Open-Meteo / Copernicus GLO-90", color = AstraTextMuted, fontSize = 12.sp)
                Text("Ephemeriden: Astronomy Engine 2.1.19 · MIT", color = AstraTextMuted, fontSize = 12.sp)
                Text("Satelliten: satellite.js 6.0.0 · MIT · Near-Earth-SGP4-Port", color = AstraTextMuted, fontSize = 12.sp)
            }
        }
        Text(
            "Projekt Astra ${BuildConfig.VERSION_NAME} · Keine Werbung · Kein Benutzerkonto",
            color = AstraBlue,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun AboutInfoCard(title: String, icon: ImageVector, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AstraSurface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, AstraOutline.copy(alpha = 0.65f))
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(40.dp).background(AstraBlue.copy(alpha = 0.15f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = StarGold) }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(body, color = AstraTextMuted, fontSize = 13.sp)
            }
        }
    }
}

internal data class GeoPoint(val latitude: Double, val longitude: Double, val altitudeMeters: Double)
private data class OrientationState(
    val azimuth: Float,
    val altitude: Float,
    val available: Boolean,
    val accuracy: Int
)
internal enum class CelestialType(val label: String) {
    STAR("Stern"),
    SUN("Sonne"),
    MOON("Mond"),
    PLANET("Planet"),
    GALAXY("Galaxie"),
    GALAXY_GROUP("Galaxiengruppe"),
    OPEN_CLUSTER("Offener Sternhaufen"),
    GLOBULAR_CLUSTER("Kugelsternhaufen"),
    NEBULA("Nebel"),
    PLANETARY_NEBULA("Planetarischer Nebel"),
    SUPERNOVA_REMNANT("Supernova-Überrest"),
    STAR_ASSOCIATION("Sternassoziation"),
    OTHER("Deep-Sky-Objekt");

    companion object {
        fun fromOpenNgc(code: String): CelestialType = when (code) {
            "G" -> GALAXY
            "GPair", "GTrpl", "GGroup" -> GALAXY_GROUP
            "OCl" -> OPEN_CLUSTER
            "GCl" -> GLOBULAR_CLUSTER
            "PN" -> PLANETARY_NEBULA
            "SNR" -> SUPERNOVA_REMNANT
            "*Ass" -> STAR_ASSOCIATION
            "Neb", "HII", "DrkN", "EmN", "RfN", "Cl+N" -> NEBULA
            else -> OTHER
        }
    }
}
internal data class CelestialObject(
    val name: String,
    val catalogId: String,
    val raHours: Double,
    val decDegrees: Double,
    val magnitude: Double,
    val distanceLightYears: Double,
    val spectralClass: String,
    val hipId: Int? = null,
    val colorIndex: Double = 0.45,
    val constellation: String = "",
    val objectType: CelestialType = CelestialType.STAR,
    val properMotionRa: Double? = null,
    val properMotionDec: Double? = null,
    val radialVelocity: Double? = null,
    val absoluteMagnitude: Double? = null,
    val luminositySolar: Double? = null,
    val majorAxisArcMinutes: Double? = null,
    val minorAxisArcMinutes: Double? = null,
    val positionAngleDegrees: Double? = null,
    val redshift: Double? = null,
    val messierId: String = "",
    val solarBody: Body? = null,
    val distanceAu: Double? = null,
    val phaseFraction: Double? = null,
    val astronomyDescription: String = "",
    val searchAliases: List<String> = emptyList()
)
internal data class HorizontalCoordinates(val azimuth: Double, val altitude: Double)
internal data class VisibleObject(val celestial: CelestialObject, val position: HorizontalCoordinates)

internal object StarCatalog {
    // Display-only fallback: never mark these few objects as a successfully loaded catalog.
    val fallbackObjects = listOf(
        CelestialObject("Sirius", "HIP 32349", 6.7525, -16.7161, -1.46, 8.6, "A1V"),
        CelestialObject("Canopus", "HIP 30438", 6.3992, -52.6957, -0.74, 310.0, "A9II"),
        CelestialObject("Arktur", "HIP 69673", 14.2610, 19.1824, -0.05, 36.7, "K1.5III"),
        CelestialObject("Wega", "HIP 91262", 18.6156, 38.7837, 0.03, 25.0, "A0V"),
        CelestialObject("Capella", "HIP 24608", 5.2782, 45.9980, 0.08, 42.9, "G8III"),
        CelestialObject("Rigel", "HIP 24436", 5.2423, -8.2016, 0.13, 860.0, "B8Ia"),
        CelestialObject("Prokyon", "HIP 37279", 7.6550, 5.2250, 0.34, 11.5, "F5IV-V"),
        CelestialObject("Beteigeuze", "HIP 27989", 5.9195, 7.4071, 0.50, 548.0, "M1-2Ia"),
        CelestialObject("Altair", "HIP 97649", 19.8464, 8.8683, 0.76, 16.7, "A7V"),
        CelestialObject("Aldebaran", "HIP 21421", 4.5987, 16.5093, 0.86, 65.3, "K5III"),
        CelestialObject("Antares", "HIP 80763", 16.4901, -26.4320, 0.96, 550.0, "M1.5Iab"),
        CelestialObject("Spica", "HIP 65474", 13.4199, -11.1613, 0.98, 250.0, "B1V")
    )

    fun load(context: Context): List<CelestialObject> =
        context.assets.open("hyg_bright_stars.tsv").bufferedReader().useLines { lines ->
            lines.filterNot { it.startsWith("#") || it.isBlank() }
                .map { line ->
                    val fields = line.split('\t')
                    require(fields.size >= 9) { "Unvollständiger HYG-Datensatz" }
                    val hip = fields[0].toIntOrNull()
                    val distanceParsec = fields[5].toDoubleOrNull() ?: 0.0
                    CelestialObject(
                        name = fields[1],
                        catalogId = hip?.let { "HIP $it" } ?: fields[1],
                        raHours = fields[2].toDouble(),
                        decDegrees = fields[3].toDouble(),
                        magnitude = fields[4].toDouble(),
                        distanceLightYears = if (distanceParsec in 0.0001..99_999.0) {
                            distanceParsec * 3.26156
                        } else 0.0,
                        spectralClass = fields[6].ifBlank { "unbekannt" },
                        hipId = hip,
                        colorIndex = fields[7].toDoubleOrNull() ?: 0.45,
                        constellation = fields[8],
                        properMotionRa = fields.getOrNull(9)?.toDoubleOrNull(),
                        properMotionDec = fields.getOrNull(10)?.toDoubleOrNull(),
                        radialVelocity = fields.getOrNull(11)?.toDoubleOrNull(),
                        absoluteMagnitude = fields.getOrNull(12)?.toDoubleOrNull(),
                        luminositySolar = fields.getOrNull(13)?.toDoubleOrNull(),
                        searchAliases = fields.getOrNull(14)?.takeIf { it.isNotBlank() }
                            ?.let { listOf("$it ${fields[8]}") }.orEmpty()
                    )
                }.toList()
        }.also { check(it.isNotEmpty()) { "Leerer HYG-Katalog" } }
}

internal object DeepSkyCatalog {
    fun load(context: Context): List<CelestialObject> =
        context.assets.open("openngc_deep_sky.tsv").bufferedReader().useLines { lines ->
            lines.filterNot { it.startsWith("#") || it.isBlank() }
                .map { line ->
                    val fields = line.split('\t')
                    require(fields.size >= 7) { "Unvollständiger OpenNGC-Datensatz" }
                    CelestialObject(
                        name = fields[0],
                        catalogId = fields[1],
                        raHours = fields[2].toDouble(),
                        decDegrees = fields[3].toDouble(),
                        magnitude = fields[4].toDoubleOrNull() ?: 12.0,
                        distanceLightYears = 0.0,
                        spectralClass = "",
                        constellation = fields[6],
                        objectType = CelestialType.fromOpenNgc(fields[5]),
                        majorAxisArcMinutes = fields.getOrNull(7)?.toDoubleOrNull(),
                        minorAxisArcMinutes = fields.getOrNull(8)?.toDoubleOrNull(),
                        positionAngleDegrees = fields.getOrNull(9)?.toDoubleOrNull(),
                        redshift = fields.getOrNull(10)?.toDoubleOrNull(),
                        messierId = fields.getOrNull(11).orEmpty()
                    )
                }.toList()
        }.also { check(it.isNotEmpty()) { "Leerer OpenNGC-Katalog" } }
}

private object ConstellationLines {
    data class Label(val name: String, val anchorHip: Int)

    val connections = listOf(
        // Großer Wagen
        54061 to 53910, 53910 to 58001, 58001 to 59774, 59774 to 54061,
        59774 to 62956, 62956 to 65378, 65378 to 67301,
        // Orion
        27989 to 25336, 25336 to 25930, 25930 to 26311, 26311 to 26727,
        26727 to 27366, 27366 to 24436, 24436 to 25930, 27989 to 26727,
        // Kassiopeia
        746 to 3179, 3179 to 4427, 4427 to 6686, 6686 to 8886,
        // Sommerdreieck
        91262 to 102098, 102098 to 97649, 97649 to 91262,
        // Schwan
        102098 to 100453, 100453 to 95947, 100453 to 97165, 100453 to 102488,
        // Leier
        91262 to 91971, 91971 to 92420, 92420 to 93194, 93194 to 92791, 92791 to 91971,
        // Adler
        97278 to 97649, 97649 to 98036,
        // Löwe
        49669 to 50583, 50583 to 54879, 54879 to 57632, 57632 to 54872, 54872 to 50583,
        // Zwillinge
        36850 to 37826, 36850 to 32246, 32246 to 30343, 37826 to 35550, 35550 to 31681,
        // Pegasus und Andromeda
        677 to 113881, 113881 to 113963, 113963 to 1067, 1067 to 677,
        677 to 5447, 5447 to 9640,
        // Perseus
        15863 to 14576, 15863 to 17358, 15863 to 14328
    )

    val labels = listOf(
        Label("Großer Wagen", 58001),
        Label("Orion", 25930),
        Label("Kassiopeia", 4427),
        Label("Schwan", 100453),
        Label("Leier", 91262),
        Label("Adler", 97649),
        Label("Löwe", 50583),
        Label("Zwillinge", 36850),
        Label("Pegasus", 113881),
        Label("Andromeda", 5447),
        Label("Perseus", 15863)
    )
}

private fun starColor(colorIndex: Double): Color {
    val index = if (colorIndex.isFinite()) colorIndex.coerceIn(-0.4, 2.0) else 0.45
    val white = Color(0xFFF5F4EE)
    return if (index < 0.45) androidx.compose.ui.graphics.lerp(Color(0xFFC7DAFF), white, ((index + 0.4) / 0.85).toFloat())
    else androidx.compose.ui.graphics.lerp(white, Color(0xFFFFCA94), ((index - 0.45) / 1.55).toFloat())
}

private fun deepSkyColor(type: CelestialType): Color = when (type) {
    CelestialType.GALAXY, CelestialType.GALAXY_GROUP -> Color(0xFFBFA7FF)
    CelestialType.OPEN_CLUSTER, CelestialType.GLOBULAR_CLUSTER, CelestialType.STAR_ASSOCIATION -> StarGold
    CelestialType.NEBULA, CelestialType.PLANETARY_NEBULA, CelestialType.SUPERNOVA_REMNANT -> Color(0xFF79E7D2)
    else -> AstraBlue
}

private fun solarSystemColor(body: Body?): Color = when (body) {
    Body.Sun -> Color(0xFFFFD45E)
    Body.Moon -> Color(0xFFE9EEF7)
    Body.Mercury -> Color(0xFFB8B1A9)
    Body.Venus -> Color(0xFFFFE0A0)
    Body.Mars -> Color(0xFFFF8666)
    Body.Jupiter -> Color(0xFFFFC18A)
    Body.Saturn -> Color(0xFFFFDEA0)
    Body.Uranus -> Color(0xFF99E9ED)
    Body.Neptune -> Color(0xFF7398FF)
    else -> AstraBlue
}

internal object MilkyWayModel {
    fun horizontalBand(observer: GeoPoint, instant: Instant): List<HorizontalCoordinates> {
        val time = instant.toAstroTime()
        val place = observer.toAstroObserver()
        val galacticToJ2000 = rotationGalEqj()
        val j2000ToDate = rotationEqjEqd(time)
        return (0..360 step 3).map { longitude ->
            val angle = Math.toRadians(longitude.toDouble())
            val galactic = Vector(cos(angle), sin(angle), 0.0, time)
            val equatorial = j2000ToDate.rotate(galacticToJ2000.rotate(galactic)).toEquatorial()
            val topocentric = horizon(
                time,
                place,
                equatorial.ra,
                equatorial.dec,
                Refraction.None
            )
            HorizontalCoordinates(topocentric.azimuth, topocentric.altitude)
        }
    }
}

internal data class TerrainSample(val azimuth: Double, val altitude: Double)

internal data class TerrainProfile(
    val samples: List<TerrainSample>,
    val observerElevationMeters: Double
) {
    fun altitudeAt(azimuth: Double): Double {
        if (samples.isEmpty()) return 0.0
        val normalized = normalizeDegrees(azimuth)
        val step = 360.0 / samples.size
        val lower = floor(normalized / step).toInt().coerceIn(samples.indices)
        val upper = (lower + 1) % samples.size
        val fraction = (normalized - lower * step) / step
        return samples[lower].altitude * (1.0 - fraction) + samples[upper].altitude * fraction
    }
}

private sealed interface TerrainState {
    data object Loading : TerrainState
    data class Ready(val profile: TerrainProfile) : TerrainState
    data object Unavailable : TerrainState
}

private object TerrainRepository {
    private val distancesMeters = doubleArrayOf(1_000.0, 3_000.0, 8_000.0, 20_000.0)
    private val bearings = (0 until 360 step 15).toList()
    private var cachedPoint: GeoPoint? = null
    private var cachedProfile: TerrainProfile? = null

    fun clear() { cachedPoint = null; cachedProfile = null }

    fun load(point: GeoPoint, callback: (TerrainState) -> Unit) {
        if (!SecureNetwork.available || !SecureNetwork.options.terrain) {
            callback(TerrainState.Unavailable)
            return
        }
        val cached = cachedProfile
        val origin = cachedPoint
        if (cached != null && origin != null &&
            abs(origin.latitude - point.latitude) < 0.002 &&
            abs(origin.longitude - point.longitude) < 0.002
        ) {
            callback(TerrainState.Ready(cached))
            return
        }
        thread(name = "astra-terrain") {
            val result = runCatching {
                val targets = buildList {
                    add(point.latitude to point.longitude)
                    bearings.forEach { bearing ->
                        distancesMeters.forEach { distance ->
                            add(destinationPoint(point, bearing.toDouble(), distance))
                        }
                    }
                }
                val latitudes = targets.joinToString(",") { String.format(Locale.US, "%.5f", it.first) }
                val longitudes = targets.joinToString(",") { String.format(Locale.US, "%.5f", it.second) }
                val url = java.net.URL(
                    "https://api.open-meteo.com/v1/elevation?latitude=$latitudes&longitude=$longitudes"
                )
                run {
                    val json = org.json.JSONObject(String(SecureNetwork.get(url.toString()).bytes, Charsets.UTF_8))
                    val elevations = json.getJSONArray("elevation")
                    if (elevations.length() != targets.size) error("Unvollständiges Höhenprofil")
                    val observerElevation = elevations.getDouble(0)
                    val samples = bearings.mapIndexed { bearingIndex, bearing ->
                        val maxAngle = distancesMeters.indices.maxOf { distanceIndex ->
                            val distance = distancesMeters[distanceIndex]
                            val elevationIndex = 1 + bearingIndex * distancesMeters.size + distanceIndex
                            val terrainElevation = elevations.getDouble(elevationIndex)
                            val curvatureDrop = distance * distance / (2.0 * 6_371_000.0 * (7.0 / 6.0))
                            Math.toDegrees(
                                atan2(terrainElevation - observerElevation - 1.7 - curvatureDrop, distance)
                            )
                        }
                        TerrainSample(bearing.toDouble(), maxAngle.coerceIn(-5.0, 35.0))
                    }
                    TerrainProfile(samples, observerElevation)
                }
            }
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                callback(
                    result.fold(
                        onSuccess = {
                            if (SecureNetwork.available && SecureNetwork.options.terrain) {
                                cachedPoint = point
                                cachedProfile = it
                                TerrainState.Ready(it)
                            } else TerrainState.Unavailable
                        },
                        onFailure = { TerrainState.Unavailable }
                    )
                )
            }
        }
    }

    private fun destinationPoint(origin: GeoPoint, bearingDegrees: Double, distanceMeters: Double): Pair<Double, Double> {
        val angularDistance = distanceMeters / 6_371_000.0
        val bearing = Math.toRadians(bearingDegrees)
        val latitude = Math.toRadians(origin.latitude)
        val longitude = Math.toRadians(origin.longitude)
        val targetLatitude = asin(
            sin(latitude) * cos(angularDistance) +
                cos(latitude) * sin(angularDistance) * cos(bearing)
        )
        val targetLongitude = longitude + atan2(
            sin(bearing) * sin(angularDistance) * cos(latitude),
            cos(angularDistance) - sin(latitude) * sin(targetLatitude)
        )
        return Math.toDegrees(targetLatitude) to Math.toDegrees(targetLongitude)
    }
}

internal object SolarSystemCatalog {
    private data class Definition(
        val body: Body,
        val name: String,
        val type: CelestialType,
        val fallbackMagnitude: Double,
        val description: String
    )

    private val definitions = listOf(
        Definition(Body.Sun, "Sonne", CelestialType.SUN, -26.74, "Unser Zentralstern. Nicht ohne geeigneten Sonnenfilter beobachten."),
        Definition(Body.Moon, "Mond", CelestialType.MOON, -12.7, "Der natürliche Satellit der Erde; seine scheinbare Position enthält die örtliche Parallaxe."),
        Definition(Body.Mercury, "Merkur", CelestialType.PLANET, -0.4, "Sonnennächster Planet; meist nur kurz in der Morgen- oder Abenddämmerung sichtbar."),
        Definition(Body.Venus, "Venus", CelestialType.PLANET, -4.2, "Sehr heller innerer Planet mit deutlich wechselnder Phase."),
        Definition(Body.Mars, "Mars", CelestialType.PLANET, 0.5, "Der rote Planet; Helligkeit und scheinbare Größe ändern sich stark mit seiner Erdentfernung."),
        Definition(Body.Jupiter, "Jupiter", CelestialType.PLANET, -2.2, "Größter Planet des Sonnensystems; schon im Fernglas sind seine vier hellsten Monde interessant."),
        Definition(Body.Saturn, "Saturn", CelestialType.PLANET, 0.7, "Gasriese mit einem ausgeprägten Ringsystem."),
        Definition(Body.Uranus, "Uranus", CelestialType.PLANET, 5.7, "Äußerer Eisriese nahe der Sichtbarkeitsgrenze des bloßen Auges."),
        Definition(Body.Neptune, "Neptun", CelestialType.PLANET, 7.8, "Äußerster Planet; für die Beobachtung ist mindestens ein Fernglas oder Teleskop nötig.")
    )

    fun at(observer: GeoPoint, instant: Instant): List<CelestialObject> {
        val time = instant.toAstroTime()
        val place = observer.toAstroObserver()
        return definitions.mapNotNull { definition ->
            runCatching {
                val current = equator(definition.body, time, place, EquatorEpoch.OfDate, Aberration.Corrected)
                val j2000 = equator(definition.body, time, place, EquatorEpoch.J2000, Aberration.Corrected)
                val light = if (definition.body == Body.Sun) null else illumination(definition.body, time)
                CelestialObject(
                    name = definition.name,
                    catalogId = "Astronomy Engine · ${definition.body}",
                    raHours = current.ra,
                    decDegrees = current.dec,
                    magnitude = light?.mag ?: definition.fallbackMagnitude,
                    distanceLightYears = 0.0,
                    spectralClass = "",
                    constellation = constellation(j2000.ra, j2000.dec).name,
                    objectType = definition.type,
                    solarBody = definition.body,
                    distanceAu = current.dist,
                    phaseFraction = light?.phaseFraction,
                    astronomyDescription = definition.description
                )
            }.getOrNull()
        }
    }

    fun horizontal(body: Body, observer: GeoPoint, instant: Instant): HorizontalCoordinates {
        val time = instant.toAstroTime()
        val place = observer.toAstroObserver()
        val current = equator(body, time, place, EquatorEpoch.OfDate, Aberration.Corrected)
        val topocentric = horizon(time, place, current.ra, current.dec, Refraction.Normal)
        return HorizontalCoordinates(topocentric.azimuth, topocentric.altitude)
    }

    fun jupiterSystem(instant: Instant): JupiterSystemState =
        JupiterMoonsCalculator.calculate(instant)

    fun saturnSystem(instant: Instant): SaturnSystemState =
        SaturnSystemCalculator.calculate(instant)
}

internal fun coordinatesAt(
    objectData: CelestialObject,
    observer: GeoPoint,
    instant: Instant
): HorizontalCoordinates = objectData.solarBody?.let {
    SolarSystemCatalog.horizontal(it, observer, instant)
} ?: AstronomyEngine.horizontalCoordinates(objectData, observer, instant)

private fun Instant.toAstroTime(): AstroTime = AstroTime.fromMillisecondsSince1970(toEpochMilli())
private fun GeoPoint.toAstroObserver(): Observer = Observer(latitude, longitude, altitudeMeters)

internal object AstronomyEngine {
    fun horizontalCoordinates(
        objectData: CelestialObject,
        observer: GeoPoint,
        instant: Instant
    ): HorizontalCoordinates {
        return horizontalCoordinates(
            objectData.raHours,
            objectData.decDegrees,
            observer,
            instant
        )
    }

    fun horizontalCoordinates(
        raHours: Double,
        decDegrees: Double,
        observer: GeoPoint,
        instant: Instant
    ): HorizontalCoordinates {
        return SkyCoordinateFrame(observer, instant).horizontal(raHours, decDegrees)
    }
}


private fun normalizeDegrees(value: Double): Double = ((value % 360.0) + 360.0) % 360.0
private fun normalizeSignedDegrees(value: Double): Double = ((value + 540.0) % 360.0) - 180.0
private fun Double.format(decimals: Int): String = String.format(Locale.GERMANY, "%.${decimals}f", this)
private fun formatRa(hours: Double): String {
    val h = floor(hours).toInt()
    val minutes = floor((hours - h) * 60.0).toInt()
    return "%02dh %02dm".format(h, minutes)
}
private fun formatDec(degrees: Double): String {
    val sign = if (degrees >= 0) "+" else "−"
    val absolute = abs(degrees)
    return "$sign${floor(absolute).toInt()}° ${floor((absolute % 1) * 60).toInt()}′"
}
private fun cardinalDirection(azimuth: Float): String = when (((azimuth + 22.5f) / 45f).toInt() % 8) {
    0 -> "N"
    1 -> "NO"
    2 -> "O"
    3 -> "SO"
    4 -> "S"
    5 -> "SW"
    6 -> "W"
    else -> "NW"
}

internal fun DrawScope.drawSatellitePassTrack(
    pass: SatellitePass,
    projection: SkyProjection,
    currentTime: Instant,
    arMode: Boolean
) {
    val points = pass.trackPoints
    if (points.size < 2) return

    for (i in 0 until points.size - 1) {
        val p1 = points[i]
        val p2 = points[i + 1]
        val segments = projection.segments(p1.position, p2.position, padding = 2f)

        val color = if (p1.isVisible) {
            StarGold
        } else {
            Color(0xFF78909C).copy(alpha = 0.4f)
        }
        val strokeWidth = if (p1.isVisible) (if (arMode) 3.5f else 2.5f) else 1.2f

        segments.forEach { seg ->
            drawLine(color = color, start = seg.start, end = seg.end, strokeWidth = strokeWidth)
        }
    }

    pass.timeMarkers.forEach { marker ->
        val screenPos = projection.point(marker.position)
        if (screenPos != null) {
            drawCircle(
                color = StarGold,
                radius = 3.5f,
                center = screenPos
            )
        }
    }

    if (currentTime >= pass.riseTime && currentTime <= pass.setTime) {
        val activePoint = points.minByOrNull { abs(it.time.toEpochMilli() - currentTime.toEpochMilli()) }
        if (activePoint != null) {
            val screenPos = projection.point(activePoint.position)
            if (screenPos != null) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(StarGold.copy(alpha = 0.45f), Color.Transparent),
                        center = screenPos,
                        radius = 24f
                    ),
                    radius = 24f,
                    center = screenPos
                )
                drawCircle(color = StarGold, radius = 6f, center = screenPos)
                drawCircle(color = Color.White, radius = 3f, center = screenPos)
            }
        }
    }
}

internal fun DrawScope.drawZoomedJupiter(
    center: Offset,
    baseRadius: Float,
    skyInstant: Instant,
    projection: SkyProjection
) {
    val planetR = baseRadius.coerceIn(12f, 32f)
    drawCircle(Color(0xFFD4A373), planetR, center)
    val bandColor = Color(0xFF9C6644).copy(alpha = 0.6f)
    drawLine(bandColor, center + Offset(-planetR * 0.9f, -planetR * 0.35f), center + Offset(planetR * 0.9f, -planetR * 0.35f), strokeWidth = planetR * 0.22f)
    drawLine(bandColor, center + Offset(-planetR * 0.9f, planetR * 0.35f), center + Offset(planetR * 0.9f, planetR * 0.35f), strokeWidth = planetR * 0.22f)

    val jupiter = JupiterMoonsCalculator.calculate(skyInstant)
    jupiter.moons.forEach { moon ->
        val scale = planetR * 1.5f
        val moonX = center.x + (moon.offsetRJ * scale / 5.0).toFloat()
        val moonY = center.y

        if (moon.isShadowTransiting && moon.shadowOffsetRJ != null) {
            val shadowX = center.x + (moon.shadowOffsetRJ * scale / 5.0).toFloat()
            drawCircle(Color.Black, 2f, Offset(shadowX, moonY))
        }

        if (moon.event != JupiterMoonEvent.OCCULTATION && moon.event != JupiterMoonEvent.ECLIPSE) {
            val moonColor = when (moon.name) {
                "Io" -> Color(0xFFFFD54F)
                "Europa" -> Color(0xFFE0F7FA)
                "Ganymede" -> Color(0xFFCFD8DC)
                else -> Color(0xFF90A4AE)
            }
            drawCircle(moonColor, 2.5f, Offset(moonX, moonY))
        }
    }
}

internal fun DrawScope.drawZoomedSaturn(
    center: Offset,
    baseRadius: Float,
    skyInstant: Instant,
    projection: SkyProjection
) {
    val planetR = baseRadius.coerceIn(10f, 26f)
    val saturn = SaturnSystemCalculator.calculate(skyInstant)
    val ringR = planetR * 2.3f
    val bRad = Math.toRadians(saturn.ringTiltDegrees)
    val ringMinorR = max(2f, ringR * abs(sin(bRad)).toFloat())

    val ringColor = Color(0xFFE6D2B5).copy(alpha = 0.85f)
    val ringInnerColor = Color(0xFFCBB282).copy(alpha = 0.7f)

    drawCircle(Color(0xFFE8D39E), planetR, center)

    drawOval(
        color = ringColor,
        topLeft = Offset(center.x - ringR, center.y - ringMinorR),
        size = androidx.compose.ui.geometry.Size(ringR * 2f, ringMinorR * 2f),
        style = Stroke(width = max(2.5f, planetR * 0.35f))
    )
    drawOval(
        color = ringInnerColor,
        topLeft = Offset(center.x - ringR * 0.75f, center.y - ringMinorR * 0.75f),
        size = androidx.compose.ui.geometry.Size(ringR * 1.5f, ringMinorR * 1.5f),
        style = Stroke(width = max(1.5f, planetR * 0.2f))
    )

    val titanScale = planetR * 3.5f
    val paRad = Math.toRadians(saturn.titanPositionAngle - 90.0)
    val titanDist = (saturn.titanOffsetRS / 20.0 * titanScale).toFloat()
    val titanPos = center + Offset(titanDist * cos(paRad).toFloat(), titanDist * sin(paRad).toFloat())
    drawCircle(Color(0xFFFFB74D), 2.5f, titanPos)
}

internal fun DrawScope.drawZoomedMoon(
    center: Offset,
    baseRadius: Float,
    skyInstant: Instant
) {
    val moonR = baseRadius.coerceIn(16f, 38f)
    val terminator = LunarTerminatorCalculator.calculateTerminator(skyInstant)

    drawCircle(Color(0xFF1E232A), moonR, center)

    val phase = terminator.phaseFraction.toFloat()
    val brightColor = Color(0xFFECEFF1)
    if (phase > 0.02f) {
        drawCircle(brightColor, moonR, center)
        if (phase < 0.98f) {
            val isWaxing = terminator.colongitude in 270.0..360.0 || terminator.colongitude in 0.0..90.0
            val shadowColor = Color(0xFF1E232A)
            val shadowOvalWidth = moonR * 2f * abs(2f * phase - 1f)
            if (phase < 0.5f) {
                drawRect(
                    color = shadowColor,
                    topLeft = if (isWaxing) Offset(center.x - moonR, center.y - moonR) else Offset(center.x, center.y - moonR),
                    size = androidx.compose.ui.geometry.Size(moonR, moonR * 2f)
                )
                drawOval(
                    color = shadowColor,
                    topLeft = Offset(center.x - shadowOvalWidth / 2f, center.y - moonR),
                    size = androidx.compose.ui.geometry.Size(shadowOvalWidth, moonR * 2f)
                )
            } else {
                drawRect(
                    color = shadowColor,
                    topLeft = if (isWaxing) Offset(center.x - moonR, center.y - moonR) else Offset(center.x, center.y - moonR),
                    size = androidx.compose.ui.geometry.Size(moonR, moonR * 2f)
                )
                drawOval(
                    color = brightColor,
                    topLeft = Offset(center.x - shadowOvalWidth / 2f, center.y - moonR),
                    size = androidx.compose.ui.geometry.Size(shadowOvalWidth, moonR * 2f)
                )
            }
        }
    }
    drawCircle(Color.White.copy(alpha = 0.2f), moonR, center, style = Stroke(1f))
}

@Composable
internal fun JupiterMoonsDiagram(jupiter: JupiterSystemState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("Konfiguration der Galileischen Monde (Ost ↔ West)", fontSize = 11.sp, color = AstraTextMuted)
            Spacer(Modifier.height(8.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                val centerY = size.height / 2f
                val centerX = size.width / 2f
                val maxOffsetRJ = 28.0
                val scale = (size.width / 2f - 24f) / maxOffsetRJ

                drawLine(
                    color = Color.White.copy(alpha = 0.12f),
                    start = Offset(12f, centerY),
                    end = Offset(size.width - 12f, centerY),
                    strokeWidth = 1f
                )

                val jupW = 20.dp.toPx()
                val jupH = 18.dp.toPx()
                drawOval(
                    color = Color(0xFFD4A373),
                    topLeft = Offset(centerX - jupW / 2f, centerY - jupH / 2f),
                    size = androidx.compose.ui.geometry.Size(jupW, jupH)
                )
                drawLine(
                    color = Color(0xFF9C6644).copy(alpha = 0.7f),
                    start = Offset(centerX - jupW * 0.45f, centerY - jupH * 0.2f),
                    end = Offset(centerX + jupW * 0.45f, centerY - jupH * 0.2f),
                    strokeWidth = 2.5f
                )
                drawLine(
                    color = Color(0xFF9C6644).copy(alpha = 0.7f),
                    start = Offset(centerX - jupW * 0.45f, centerY + jupH * 0.2f),
                    end = Offset(centerX + jupW * 0.45f, centerY + jupH * 0.2f),
                    strokeWidth = 2.5f
                )

                jupiter.moons.forEach { moon ->
                    val moonX = (centerX + moon.offsetRJ * scale).toFloat()
                    val moonColor = when (moon.name) {
                        "Io" -> Color(0xFFFFD54F)
                        "Europa" -> Color(0xFFE0F7FA)
                        "Ganymede" -> Color(0xFFCFD8DC)
                        else -> Color(0xFF90A4AE)
                    }

                    if (moon.isShadowTransiting && moon.shadowOffsetRJ != null) {
                        val shadowX = (centerX + moon.shadowOffsetRJ * scale).toFloat()
                        drawCircle(Color.Black, 3f, Offset(shadowX, centerY))
                    }

                    val isHidden = moon.event == JupiterMoonEvent.OCCULTATION || moon.event == JupiterMoonEvent.ECLIPSE || moon.isInEclipse
                    val dotAlpha = if (isHidden) 0.35f else 1.0f
                    drawCircle(moonColor.copy(alpha = dotAlpha), 4.5f, Offset(moonX, centerY))

                    val textPaint = android.graphics.Paint().apply {
                        color = if (isHidden) 0x66FFFFFF else android.graphics.Color.WHITE
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    val labelY = if (moon.name == "Io" || moon.name == "Ganymede") centerY - 10f else centerY + 18f
                    drawContext.canvas.nativeCanvas.drawText(moon.name.take(1), moonX, labelY, textPaint)
                }
            }
        }
    }
}

@Composable
internal fun JupiterMoonEventsBadges(jupiter: JupiterSystemState) {
    val events = jupiter.activeEvents
    if (events.isEmpty()) {
        Text("Alle 4 Galileischen Monde frei sichtbar (keine Transite/Okkultationen)", fontSize = 12.sp, color = AstraTextMuted)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            events.forEach { (moon, event) ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AstraSurfaceHigh,
                    border = BorderStroke(1.dp, StarGold.copy(alpha = 0.5f))
                ) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Info, null, tint = StarGold, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${moon.name}: ${event.label}", fontSize = 12.sp, color = StarGold, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
internal fun SaturnRingsDiagram(saturn: SaturnSystemState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("Saturn Ringsystem & Titan-Position", fontSize = 11.sp, color = AstraTextMuted)
            Spacer(Modifier.height(8.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val planetR = 14.dp.toPx()
                val ringMajor = 34.dp.toPx()
                val bRad = Math.toRadians(saturn.ringTiltDegrees)
                val ringMinor = max(3f, ringMajor * abs(sin(bRad)).toFloat())

                drawCircle(Color(0xFFE8D39E), planetR, center)

                drawOval(
                    color = Color(0xFFE6D2B5).copy(alpha = 0.9f),
                    topLeft = Offset(center.x - ringMajor, center.y - ringMinor),
                    size = androidx.compose.ui.geometry.Size(ringMajor * 2f, ringMinor * 2f),
                    style = Stroke(width = 4f)
                )
                drawOval(
                    color = Color(0xFFCBB282).copy(alpha = 0.7f),
                    topLeft = Offset(center.x - ringMajor * 0.78f, center.y - ringMinor * 0.78f),
                    size = androidx.compose.ui.geometry.Size(ringMajor * 1.56f, ringMinor * 1.56f),
                    style = Stroke(width = 2f)
                )

                val titanScale = (size.width / 2f - 24f) / 22.0f
                val paRad = Math.toRadians(saturn.titanPositionAngle - 90.0)
                val titanDist = (saturn.titanOffsetRS * titanScale).toFloat()
                val titanX = center.x + titanDist * cos(paRad).toFloat()
                val titanY = center.y + titanDist * sin(paRad).toFloat()

                drawCircle(Color(0xFFFFB74D), 4f, Offset(titanX, titanY))
                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                drawContext.canvas.nativeCanvas.drawText("Titan", titanX, titanY - 8f, textPaint)
            }
        }
    }
}

@Composable
internal fun MoonTerminatorSummary(
    terminator: LunarTerminatorState,
    onOpenMoonDetail: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Mondterminator & Oberflächenrelief", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            DetailRow("Colongitude C₀", "${terminator.colongitude.format(1)}°")
            DetailRow(
                "Optische Libration",
                "L ${if (terminator.subEarthLon >= 0) "+" else ""}${terminator.subEarthLon.format(1)}° · B ${if (terminator.subEarthLat >= 0) "+" else ""}${terminator.subEarthLat.format(1)}°"
            )
            DetailRow("Morgenterminator", "${terminator.morningTerminatorLon(0.0).format(1)}° selenographische Länge")

            val topFeatures = remember(terminator) {
                LunarTerminatorCalculator.featuresNearTerminator(terminator).take(3)
            }
            if (topFeatures.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Im besten Relief am Terminator:", fontSize = 12.sp, color = StarGold, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    topFeatures.forEach { highlight ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("• ${highlight.feature.name} (${highlight.feature.type.label})", fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Text("Sonnenhöhe ${highlight.sunElevationDegrees.format(1)}°", fontSize = 11.sp, color = AstraBlue)
                        }
                    }
                }
            }

            if (onOpenMoonDetail != null) {
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = onOpenMoonDetail,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AstraBlue)
                ) {
                    Icon(Icons.Rounded.Explore, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Mondkarte & Relief-Details öffnen")
                }
            }
        }
    }
}

