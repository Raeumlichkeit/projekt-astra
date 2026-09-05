package de.projektastra.app

import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
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

private val Night = Color(0xFF07101F)
private val NightBlue = Color(0xFF0D1C34)
private val AstraSurface = Color(0xFF10243F)
private val AstraSurfaceHigh = Color(0xFF163252)
private val AstraBlue = Color(0xFF6DA8FF)
private val StarGold = Color(0xFFFFD98A)
private val AstraTextMuted = Color(0xFFAAB8CE)
private val AstraOutline = Color(0xFF294466)
private val AstraSuccess = Color(0xFF76E0A0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AstraRoot() }
    }
}

@Composable
private fun AstraRoot() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("astra_settings", Context.MODE_PRIVATE) }
    var redLightMode by remember { mutableStateOf(preferences.getBoolean("red_light_mode", false)) }
    val setRedLightMode: (Boolean) -> Unit = {
        redLightMode = it
        preferences.edit { putBoolean("red_light_mode", it) }
    }
    SideEffect {
        (context as? android.app.Activity)?.window?.let { window ->
            val systemBarColor = if (redLightMode) android.graphics.Color.rgb(24, 0, 0)
            else android.graphics.Color.rgb(7, 16, 31)
            @Suppress("DEPRECATION")
            window.statusBarColor = systemBarColor
            @Suppress("DEPRECATION")
            window.navigationBarColor = systemBarColor
            WindowCompat.getInsetsController(window, window.decorView).let { controller ->
                if (redLightMode) controller.hide(WindowInsetsCompat.Type.systemBars())
                else controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    AstraTheme(redLightMode) {
        Box(
            Modifier.fillMaxSize().drawWithContent {
                drawContent()
                if (redLightMode) {
                    drawRect(Color(0xFFC40000), blendMode = BlendMode.Multiply)
                }
            }
        ) {
            AstraApp(redLightMode, setRedLightMode)
        }
    }
}

@Composable
private fun AstraTheme(redLightMode: Boolean, content: @Composable () -> Unit) {
    val primary = if (redLightMode) Color(0xFFD35A4A) else AstraBlue
    val secondary = if (redLightMode) Color(0xFFFF725C) else StarGold
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = primary,
            secondary = secondary,
            background = Night,
            surface = NightBlue,
            surfaceVariant = AstraSurface,
            outline = AstraOutline,
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

private enum class AstraTab { SKY, WEATHER, EVENTS, ABOUT }

@Composable
private fun AstraApp(redLightMode: Boolean, setRedLightMode: (Boolean) -> Unit) {
    var tab by remember { mutableStateOf(AstraTab.SKY) }
    var location by remember { mutableStateOf<GeoPoint?>(null) }
    var permissionGranted by remember { mutableStateOf(false) }
    var cameraGranted by remember { mutableStateOf(false) }
    var arEnabled by remember { mutableStateOf(false) }
    var locationRefreshKey by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraGranted = granted
        if (granted) arEnabled = true
    }

    LaunchedEffect(Unit) {
        permissionGranted = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        cameraGranted = context.checkSelfPermission(Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }

    LocationEffect(permissionGranted, locationRefreshKey) { location = it }

    Scaffold(
        containerColor = Night,
        bottomBar = {
            val navigationColors = NavigationBarItemDefaults.colors(
                selectedIconColor = Night,
                selectedTextColor = StarGold,
                indicatorColor = StarGold,
                unselectedIconColor = AstraTextMuted,
                unselectedTextColor = AstraTextMuted
            )
            NavigationBar(containerColor = Color(0xFF091426), tonalElevation = 0.dp) {
                NavigationBarItem(
                    selected = tab == AstraTab.SKY,
                    onClick = { tab = AstraTab.SKY },
                    icon = { Icon(Icons.Rounded.Public, null) },
                    label = { Text("Sternkarte") },
                    colors = navigationColors
                )
                NavigationBarItem(
                    selected = tab == AstraTab.WEATHER,
                    onClick = { tab = AstraTab.WEATHER },
                    icon = { Icon(Icons.Rounded.Cloud, null) },
                    label = { Text("Wetter") },
                    colors = navigationColors
                )
                NavigationBarItem(
                    selected = tab == AstraTab.EVENTS,
                    onClick = { tab = AstraTab.EVENTS },
                    icon = { Icon(Icons.Rounded.CalendarMonth, null) },
                    label = { Text("Kalender") },
                    colors = navigationColors
                )
                NavigationBarItem(
                    selected = tab == AstraTab.ABOUT,
                    onClick = { tab = AstraTab.ABOUT },
                    icon = { Icon(Icons.Rounded.Info, null) },
                    label = { Text("Info") },
                    colors = navigationColors
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            key(tab) {
                when (tab) {
                    AstraTab.SKY -> SkyScreen(
                    location = location,
                    locationPermissionGranted = permissionGranted,
                    cameraPermissionGranted = cameraGranted,
                    arEnabled = arEnabled,
                    redLightMode = redLightMode,
                    toggleRedLightMode = { setRedLightMode(!redLightMode) },
                    onLocationPermissionResult = { permissionGranted = it },
                    toggleAr = {
                        if (arEnabled) arEnabled = false
                        else if (cameraGranted) arEnabled = true
                        else cameraLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
                    AstraTab.WEATHER -> WeatherScreen(
                        location = location,
                        refreshLocation = { locationRefreshKey++ }
                    )
                    AstraTab.EVENTS -> EventsScreen(location)
                    AstraTab.ABOUT -> AboutScreen(redLightMode, setRedLightMode)
                }
            }
        }
    }
}

@Composable
private fun LocationEffect(enabled: Boolean, refreshKey: Int, onLocation: (GeoPoint) -> Unit) {
    val context = LocalContext.current
    DisposableEffect(enabled, refreshKey) {
        if (!enabled) return@DisposableEffect onDispose { }
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasLocationPermission) return@DisposableEffect onDispose { }
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = LocationListener { value ->
            onLocation(GeoPoint(value.latitude, value.longitude, value.altitude))
        }
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        providers.forEach { provider ->
            runCatching {
                manager.getLastKnownLocation(provider)?.let(listener::onLocationChanged)
                manager.requestLocationUpdates(provider, 30_000L, 100f, listener)
            }
        }
        onDispose { manager.removeUpdates(listener) }
    }
}

@Composable
private fun rememberOrientation(observer: GeoPoint): OrientationState {
    val context = LocalContext.current
    var azimuth by remember { mutableFloatStateOf(180f) }
    var pitch by remember { mutableFloatStateOf(35f) }
    var available by remember { mutableStateOf(false) }
    var accuracy by remember { mutableIntStateOf(SensorManager.SENSOR_STATUS_UNRELIABLE) }

    val magneticDeclination = remember(observer) {
        GeomagneticField(
            observer.latitude.toFloat(),
            observer.longitude.toFloat(),
            observer.altitudeMeters.toFloat(),
            System.currentTimeMillis()
        ).declination
    }

    DisposableEffect(observer, magneticDeclination) {
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
                val delta = ((targetAzimuth - azimuth + 540f) % 360f) - 180f
                azimuth = (azimuth + delta * 0.18f + 360f) % 360f
                pitch += (targetPitch - pitch) * 0.18f
            }

            override fun onAccuracyChanged(sensor: Sensor?, value: Int) {
                accuracy = value
            }
        }
        sensor?.let { manager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        onDispose { manager.unregisterListener(listener) }
    }
    return OrientationState(azimuth, pitch, available, accuracy)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkyScreen(
    location: GeoPoint?,
    locationPermissionGranted: Boolean,
    cameraPermissionGranted: Boolean,
    arEnabled: Boolean,
    redLightMode: Boolean,
    toggleRedLightMode: () -> Unit,
    onLocationPermissionResult: (Boolean) -> Unit,
    toggleAr: () -> Unit
) {
    val observer = location ?: GeoPoint(52.52, 13.405, 34.0)
    val orientation = rememberOrientation(observer)
    val context = LocalContext.current
    val stars = remember { StarCatalog.load(context) }
    val deepSkyObjects = remember { DeepSkyCatalog.load(context) }
    val cameraFov = rememberCameraHorizontalFov()
    var selected by remember { mutableStateOf<VisibleObject?>(null) }
    var showDeepSky by remember { mutableStateOf(false) }
    var showCalibration by remember { mutableStateOf(false) }
    var terrainState by remember { mutableStateOf<TerrainState>(TerrainState.Loading) }
    var manualAzimuth by remember { mutableFloatStateOf(180f) }
    var manualAltitude by remember { mutableFloatStateOf(35f) }
    var manualFov by remember { mutableFloatStateOf(95f) }
    LaunchedEffect(arEnabled) {
        if (!arEnabled) {
            manualAzimuth = orientation.azimuth
            manualAltitude = orientation.altitude
        }
    }
    val viewAzimuth = if (arEnabled) orientation.azimuth else manualAzimuth
    val viewAltitude = if (arEnabled) orientation.altitude else manualAltitude
    LaunchedEffect(
        (observer.latitude * 1_000).roundToInt(),
        (observer.longitude * 1_000).roundToInt()
    ) {
        terrainState = TerrainState.Loading
        TerrainRepository.load(observer) { terrainState = it }
    }
    val milkyWay = remember(observer) {
        MilkyWayModel.horizontalBand(observer, Instant.now())
    }
    val visible = remember(observer, orientation.azimuth, orientation.altitude, showDeepSky) {
        val solarSystem = SolarSystemCatalog.at(observer, Instant.now())
        val catalog = if (showDeepSky) stars + solarSystem + deepSkyObjects else stars + solarSystem
        catalog.map {
            VisibleObject(it, coordinatesAt(it, observer, Instant.now()))
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("PROJEKT ASTRA", color = AstraBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Live-Himmel", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = toggleRedLightMode) {
                    Icon(
                        Icons.Rounded.DarkMode,
                        contentDescription = if (redLightMode) "Rotlicht ausschalten" else "Rotlicht einschalten",
                        tint = if (redLightMode) StarGold else AstraTextMuted
                    )
                }
                Icon(Icons.Rounded.GpsFixed, null, tint = if (location != null) Color(0xFF76E0A0) else StarGold)
                Spacer(Modifier.width(6.dp))
                Text(if (location != null) "GPS" else "Berlin Demo", fontSize = 12.sp)
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (arEnabled && cameraPermissionGranted) CameraPreview()
            SkyCanvas(
                objects = visible,
                viewAzimuth = viewAzimuth.toDouble(),
                viewAltitude = viewAltitude.toDouble(),
                horizontalFov = if (arEnabled) cameraFov else manualFov.toDouble(),
                arMode = arEnabled,
                milkyWay = milkyWay,
                terrainProfile = (terrainState as? TerrainState.Ready)?.profile,
                gesturesEnabled = !arEnabled,
                onViewChange = { azimuth, altitude, fov ->
                    manualAzimuth = azimuth.toFloat()
                    manualAltitude = altitude.toFloat()
                    manualFov = fov.toFloat()
                },
                onSelect = { selected = it }
            )
            Column(Modifier.align(Alignment.TopCenter), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(cardinalDirection(viewAzimuth), color = StarGold, fontWeight = FontWeight.Bold)
                Text("${viewAzimuth.toInt()}° · ${viewAltitude.toInt()}° Höhe", fontSize = 12.sp)
            }
            Column(
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                Button(
                    onClick = toggleAr,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (arEnabled) StarGold else NightBlue.copy(alpha = 0.88f),
                        contentColor = if (arEnabled) Night else Color.White
                    )
                ) {
                    Icon(if (arEnabled) Icons.Rounded.Map else Icons.Rounded.CameraAlt, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (arEnabled) "Karte" else "AR")
                }
                Button(
                    onClick = { showDeepSky = !showDeepSky },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showDeepSky) AstraBlue else NightBlue.copy(alpha = 0.88f),
                        contentColor = Color.White
                    )
                ) {
                    Text(if (showDeepSky) "Deep Sky ✓" else "Deep Sky")
                }
                if (!arEnabled) {
                    Button(
                        onClick = {
                            manualAzimuth = orientation.azimuth
                            manualAltitude = orientation.altitude
                            manualFov = 95f
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NightBlue.copy(alpha = 0.88f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Rounded.GpsFixed, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Ausrichten")
                    }
                }
                Button(
                    onClick = { showCalibration = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NightBlue.copy(alpha = 0.88f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Rounded.Explore, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Kalibrieren")
                }
            }
            if (!arEnabled) {
                Text(
                    "Wischen zum Bewegen · Zwei Finger zum Zoomen",
                    modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
                        .background(Night.copy(alpha = 0.78f), RoundedCornerShape(12.dp)).padding(9.dp),
                    color = Color(0xFFD8E4F7),
                    fontSize = 12.sp
                )
            }
            Text(
                when (terrainState) {
                    TerrainState.Loading -> "Geländeprofil wird geladen …"
                    is TerrainState.Ready -> "Geländehorizont · GLO-90"
                    TerrainState.Unavailable -> "Flacher Horizont · Gelände offline"
                },
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                    .background(Night.copy(alpha = 0.72f), RoundedCornerShape(10.dp)).padding(7.dp),
                color = Color(0xFFAAB8CE),
                fontSize = 10.sp
            )
            if (!orientation.available) {
                Text(
                    "Kein Richtungssensor – statische Ansicht",
                    modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                    color = StarGold,
                    fontSize = 12.sp
                )
            }
        }

        if (!locationPermissionGranted) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Dein Standort richtet Himmel, Wetter und Ereignisse lokal aus. Ohne Freigabe bleibt Berlin als Demo aktiv.",
                    color = Color(0xFFAAB8CE),
                    fontSize = 12.sp
                )
                LocationButton(
                    onPermissionResult = onLocationPermissionResult,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    textType = LocationButtonTextType.UsePreciseLocation,
                    backgroundColor = if (redLightMode) Color(0xFF5A0000) else AstraBlue,
                    textColor = if (redLightMode) Color(0xFFFF7868) else Night,
                    iconTint = if (redLightMode) Color(0xFFFF7868) else Night,
                    cornerRadius = 20.dp,
                    pressedCornerRadius = 12.dp
                )
            }
        }
    }

    selected?.let { item ->
        ModalBottomSheet(onDismissRequest = { selected = null }, containerColor = NightBlue) {
            ObjectDetails(item, observer)
        }
    }
    if (showCalibration) {
        ModalBottomSheet(onDismissRequest = { showCalibration = false }, containerColor = NightBlue) {
            CalibrationGuide(orientation) { showCalibration = false }
        }
    }
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

@Composable
private fun CameraPreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

    DisposableEffect(lifecycleOwner, previewView) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        var disposed = false
        providerFuture.addListener({
            if (!disposed) {
                runCatching {
                    provider = providerFuture.get().also { cameraProvider ->
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview
                        )
                    }
                }
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            disposed = true
            provider?.unbindAll()
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
private fun SkyCanvas(
    objects: List<VisibleObject>,
    viewAzimuth: Double,
    viewAltitude: Double,
    horizontalFov: Double,
    arMode: Boolean,
    milkyWay: List<HorizontalCoordinates>,
    terrainProfile: TerrainProfile?,
    gesturesEnabled: Boolean,
    onViewChange: (azimuth: Double, altitude: Double, fov: Double) -> Unit,
    onSelect: (VisibleObject) -> Unit
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val latestAzimuth by rememberUpdatedState(viewAzimuth)
    val latestAltitude by rememberUpdatedState(viewAltitude)
    val latestFov by rememberUpdatedState(horizontalFov)
    Canvas(
        Modifier.fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .pointerInput(objects, viewAzimuth, viewAltitude, canvasSize, horizontalFov) {
                detectTapGestures { tap ->
                    val closest = objects.mapNotNull { item ->
                        project(
                            item.position,
                            viewAzimuth,
                            viewAltitude,
                            canvasSize.width.toFloat(),
                            canvasSize.height.toFloat(),
                            horizontalFov
                        )?.let { point -> item to hypot((point.x - tap.x).toDouble(), (point.y - tap.y).toDouble()) }
                    }.minByOrNull { it.second }
                    if (closest != null && closest.second <= 42.0) onSelect(closest.first)
                }
            }
            .pointerInput(gesturesEnabled, canvasSize) {
                if (!gesturesEnabled) return@pointerInput
                detectTransformGestures { _, pan, zoom, _ ->
                    if (canvasSize.width <= 0 || canvasSize.height <= 0) return@detectTransformGestures
                    val verticalFov = latestFov * canvasSize.height / canvasSize.width
                    val nextAzimuth = normalizeDegrees(
                        latestAzimuth - pan.x / canvasSize.width * latestFov
                    )
                    val nextAltitude = (
                        latestAltitude + pan.y / canvasSize.height * verticalFov
                    ).coerceIn(-90.0, 90.0)
                    val nextFov = (latestFov / zoom).coerceIn(25.0, 150.0)
                    onViewChange(nextAzimuth, nextAltitude, nextFov)
                }
            }
            .then(
                if (arMode) Modifier.background(Color.Black.copy(alpha = 0.28f))
                else Modifier.background(Brush.radialGradient(listOf(Color(0xFF142D50), Night)))
            )
    ) {
        drawSkyGrid(viewAzimuth, viewAltitude)
        drawMilkyWay(milkyWay, viewAzimuth, viewAltitude, horizontalFov)
        val projected = objects.mapNotNull { item ->
            project(item.position, viewAzimuth, viewAltitude, size.width, size.height, horizontalFov)?.let { item to it }
        }
        val byHip = projected.mapNotNull { (item, point) -> item.celestial.hipId?.let { it to point } }.toMap()
        ConstellationLines.connections.forEach { (from, to) ->
            val start = byHip[from]
            val end = byHip[to]
            if (start != null && end != null) {
                drawLine(AstraBlue.copy(alpha = if (arMode) 0.75f else 0.42f), start, end, 2f)
            }
        }
        ConstellationLines.labels.forEach { label ->
            byHip[label.anchorHip]?.let { point ->
                drawContext.canvas.nativeCanvas.drawText(
                    label.name.uppercase(Locale.GERMAN),
                    point.x + 14f,
                    point.y + 34f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.rgb(109, 168, 255)
                        textSize = 24f
                        alpha = if (arMode) 205 else 135
                    }
                )
            }
        }
        projected.forEach { (item, point) ->
            val objectData = item.celestial
            val radius = when (objectData.objectType) {
                CelestialType.SUN -> 14f
                CelestialType.MOON -> 13f
                CelestialType.PLANET -> (10f - objectData.magnitude.toFloat() * 0.45f).coerceIn(5f, 12f)
                CelestialType.STAR -> (7.5f - objectData.magnitude.toFloat()).coerceIn(1.4f, 10f)
                else -> (6f + (objectData.majorAxisArcMinutes ?: 0.0).toFloat() / 12f).coerceIn(6f, 15f)
            }
            when (objectData.objectType) {
                CelestialType.STAR -> drawCircle(starColor(objectData.colorIndex), radius, point)
                CelestialType.SUN, CelestialType.MOON, CelestialType.PLANET -> {
                    drawCircle(solarSystemColor(objectData.solarBody), radius * 1.8f, point, alpha = 0.18f)
                    drawCircle(solarSystemColor(objectData.solarBody), radius, point)
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
            val shouldLabel = when (objectData.objectType) {
                CelestialType.STAR -> objectData.magnitude < 1.5 && !objectData.name.startsWith("HIP ")
                CelestialType.SUN, CelestialType.MOON, CelestialType.PLANET -> true
                else -> objectData.messierId.isNotBlank() || objectData.magnitude < 7.0
            }
            if (shouldLabel) {
                drawContext.canvas.nativeCanvas.drawText(
                    objectData.name,
                    point.x + 10f,
                    point.y - 8f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 30f
                        alpha = 210
                    }
                )
            }
        }
        drawTerrainHorizon(terrainProfile, viewAzimuth, viewAltitude, horizontalFov, arMode)
    }
}

private fun DrawScope.drawMilkyWay(
    band: List<HorizontalCoordinates>,
    viewAzimuth: Double,
    viewAltitude: Double,
    horizontalFov: Double
) {
    if (band.size < 2) return
    band.zipWithNext().forEach { (from, to) ->
        val start = project(from, viewAzimuth, viewAltitude, size.width, size.height, horizontalFov)
        val end = project(to, viewAzimuth, viewAltitude, size.width, size.height, horizontalFov)
        if (start != null && end != null && abs(start.x - end.x) < size.width * 0.25f) {
            drawLine(Color(0xFF96BFFF).copy(alpha = 0.035f), start, end, 76f)
            drawLine(Color(0xFFB8D2FF).copy(alpha = 0.07f), start, end, 34f)
            drawLine(Color(0xFFD5E3FF).copy(alpha = 0.23f), start, end, 2f)
        }
    }
    band.asSequence().filterIndexed { index, _ -> index % 20 == 0 }
        .mapNotNull { project(it, viewAzimuth, viewAltitude, size.width, size.height, horizontalFov) }
        .firstOrNull { it.x in 70f..size.width - 210f && it.y in 40f..size.height - 40f }
        ?.let { point ->
            drawContext.canvas.nativeCanvas.drawText(
                "MILCHSTRASSE",
                point.x,
                point.y - 18f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.rgb(184, 210, 255)
                    textSize = 23f
                    alpha = 165
                }
            )
        }
}

private fun DrawScope.drawTerrainHorizon(
    profile: TerrainProfile?,
    viewAzimuth: Double,
    viewAltitude: Double,
    horizontalFov: Double,
    arMode: Boolean
) {
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
    if (points.none { it.y in -20f..size.height + 20f }) return
    val ground = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(ground, Color(0xFF03070D).copy(alpha = if (arMode) 0.30f else 0.97f))
    points.zipWithNext().forEach { (start, end) ->
        drawLine(StarGold.copy(alpha = 0.72f), start, end, 2.2f)
    }
    val labelPoint = points[3]
    if (labelPoint.y in 20f..size.height - 10f) {
        drawContext.canvas.nativeCanvas.drawText(
            if (profile == null) "HORIZONT" else "GELÄNDEHORIZONT",
            18f,
            labelPoint.y - 10f,
            android.graphics.Paint().apply {
                color = android.graphics.Color.rgb(255, 217, 138)
                textSize = 24f
                alpha = 200
            }
        )
    }
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

private fun project(
    horizontal: HorizontalCoordinates,
    centerAzimuth: Double,
    centerAltitude: Double,
    width: Float,
    height: Float,
    horizontalFov: Double
): Offset? {
    val azDelta = normalizeSignedDegrees(horizontal.azimuth - centerAzimuth)
    val altDelta = horizontal.altitude - centerAltitude
    val verticalFov = horizontalFov * height / width
    if (abs(azDelta) > horizontalFov / 2 || abs(altDelta) > verticalFov / 2) return null
    return Offset(
        (width * (0.5 + azDelta / horizontalFov)).toFloat(),
        (height * (0.5 - altDelta / verticalFov)).toFloat()
    )
}

@Composable
private fun ObjectDetails(item: VisibleObject, observer: GeoPoint) {
    val objectData = item.celestial
    val path = remember(item.celestial, observer) {
        (0..12).map { hours ->
            val at = Instant.now().plusSeconds(hours * 3600L)
            at to coordinatesAt(item.celestial, observer, at)
        }
    }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(objectData.name, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(objectData.catalogId, color = AstraBlue)
        Spacer(Modifier.height(18.dp))
        DetailRow("Objekttyp", objectData.objectType.label)
        DetailRow(
            if (objectData.solarBody == null) "Koordinaten J2000" else "Aktuelle Koordinaten",
            "RA ${formatRa(objectData.raHours)} · Dec ${formatDec(objectData.decDegrees)}"
        )
        DetailRow("Aktuelle Position", "Az ${item.position.azimuth.format(1)}° · Höhe ${item.position.altitude.format(1)}°")
        DetailRow("Sichtbarkeit", if (item.position.altitude >= 0.0) "über dem Horizont" else "unter dem Horizont")
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
                "Echter Himmelsausschnitt an der Objektkoordinate; Sterne erscheinen in Aufnahmen als Lichtpunkte.",
                color = Color(0xFFAAB8CE),
                fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))
            SkySurveyImage(objectData)
            Text("Bild: DSS2 via CDS HiPS2FITS", fontSize = 11.sp, color = Color(0xFFAAB8CE))
        }
        Spacer(Modifier.height(18.dp))
        Text("Bahn in den nächsten 12 Stunden", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        path.forEach { (time, position) ->
            val localTime = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(time)
            Text("$localTime   Az ${position.azimuth.format(0)}°   Höhe ${position.altitude.format(0)}°", fontSize = 13.sp)
        }
    }
}

@Composable
private fun SkySurveyImage(objectData: CelestialObject) {
    val context = LocalContext.current
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
    val webView = remember(context, imageUrl) {
        WebView(context).apply {
            webViewClient = WebViewClient()
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            settings.javaScriptEnabled = false
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            loadUrl(imageUrl)
        }
    }
    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth().height(230.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Night)
    ) {
        AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
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
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AstraSurface),
        border = BorderStroke(1.dp, AstraOutline.copy(alpha = 0.75f))
    ) {
        Row(
            Modifier.fillMaxWidth().background(
                Brush.horizontalGradient(
                    listOf(AstraBlue.copy(alpha = 0.13f), Color.Transparent, StarGold.copy(alpha = 0.06f))
                )
            ).padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(52.dp).background(AstraBlue.copy(alpha = 0.16f), RoundedCornerShape(17.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = StarGold, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(15.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(eyebrow, color = AstraBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(title, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = AstraTextMuted, fontSize = 12.sp)
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
private fun WeatherScreen(location: GeoPoint?, refreshLocation: () -> Unit) {
    var state by remember { mutableStateOf<WeatherState>(WeatherState.Idle) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    val observer = location ?: GeoPoint(52.52, 13.405, 34.0)

    fun refresh() {
        isRefreshing = true
        refreshKey++
        refreshLocation()
    }

    LaunchedEffect(observer, refreshKey) {
        state = WeatherState.Loading
        WeatherRepository.load(observer) {
            state = it
            isRefreshing = false
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = ::refresh,
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Night, Color(0xFF09172A), Night))
        )
    ) {
        Column(
            Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AstraScreenHeader(
                eyebrow = "ASTRA FORECAST",
                title = "Beobachtungswetter",
                subtitle = if (location == null) "Demo-Standort Berlin"
                else "Standort ${observer.latitude.format(3)}, ${observer.longitude.format(3)}",
                icon = Icons.Rounded.Cloud
            )
            Text("Nach unten ziehen zum Aktualisieren", color = AstraTextMuted, fontSize = 12.sp)
            when (val value = state) {
                WeatherState.Idle, WeatherState.Loading -> Text("Aktuelle Daten werden geladen …")
                is WeatherState.Error -> {
                    Text(value.message, color = StarGold)
                    Button(onClick = ::refresh) { Text("Erneut versuchen") }
                }
                is WeatherState.Ready -> {
                    ObservationScore(value.weather, observer)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        WeatherTile("Temperatur", "${value.weather.temperature.format(1)} °C", Modifier.weight(1f))
                        WeatherTile("Bewölkung", "${value.weather.cloudCover}%", Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        WeatherTile("Wind", "${value.weather.windSpeed.format(1)} km/h", Modifier.weight(1f))
                        WeatherTile("Sichtweite", "${(value.weather.visibility / 1000.0).format(1)} km", Modifier.weight(1f))
                    }
                    Text("Quelle: Open-Meteo · zuletzt ${value.weather.updatedAt}", fontSize = 12.sp, color = Color(0xFFAAB8CE))
                    AstraSectionTitle("24-Stunden-Ausblick", "Stündliche Bedingungen am aktuellen Standort")
                    ForecastTimeline(value.weather.forecast, observer)
                    AstraSectionTitle("Wolken- und Regenkarte", "Radar, Niederschlag und Bewölkung")
                    Text(
                        "Regenradar mit 2-Stunden-Zeitleiste und aktuelle Bewölkung. Ebenen lassen sich direkt in der Karte umschalten.",
                        color = Color(0xFFAAB8CE),
                        fontSize = 13.sp
                    )
                    WeatherMap(observer, refreshKey)
                }
            }
        }
    }
}

@Composable
private fun ForecastTimeline(forecast: List<HourlyForecast>, observer: GeoPoint) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        forecast.forEach { hour ->
            val score = remember(hour, observer) {
                AstraScoreCalculator.calculate(
                    cloudCover = hour.cloudCover,
                    rainProbability = hour.rainProbability,
                    windSpeed = hour.windSpeed,
                    visibilityMeters = hour.visibility,
                    observer = observer,
                    instant = Instant.now().plusSeconds(hour.hoursFromNow * 3_600L)
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

@Composable
@SuppressLint("SetJavaScriptEnabled")
private fun WeatherMap(observer: GeoPoint, refreshKey: Int) {
    val context = LocalContext.current
    val webView = remember(context) {
        WebView(context).apply {
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    view.postDelayed({
                        view.evaluateJavascript("window.astraMap && window.astraMap.invalidateSize(true)", null)
                        view.invalidate()
                    }, 500L)
                }
            }
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.userAgentString = settings.userAgentString + " ProjektAstra/1.0"
        }
    }

    LaunchedEffect(observer, refreshKey) {
        val leafletCss = context.assets.open("leaflet-1.9.4.css").bufferedReader().use { it.readText() }
        val leafletJs = context.assets.open("leaflet-1.9.4.js").bufferedReader().use { it.readText() }
        val html = context.assets.open("weather_map.html").bufferedReader().use { it.readText() }
            .replace("__LEAFLET_CSS__", leafletCss)
            .replace("__LEAFLET_JS__", leafletJs)
            .replace("__ASTRA_LAT__", observer.latitude.toString())
            .replace("__ASTRA_LON__", observer.longitude.toString())
        webView.loadDataWithBaseURL(
            "https://projekt-astra.local/",
            html,
            "text/html",
            "UTF-8",
            null
        )
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().height(400.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NightBlue)
    ) {
        AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
    }
}

private enum class SkyEventKind(val label: String) {
    METEOR("Sternschnuppen"),
    SOLAR_ECLIPSE("Sonnenfinsternis"),
    LUNAR_ECLIPSE("Mondfinsternis")
}

private data class MeteorDefinition(
    val name: String,
    val month: Int,
    val day: Int,
    val radiantRaHours: Double,
    val radiantDecDegrees: Double,
    val zhr: Int
)

private data class SkyEvent(
    val title: String,
    val kind: SkyEventKind,
    val instant: Instant,
    val timeIsApproximate: Boolean,
    val status: String,
    val statusColor: Color,
    val facts: String,
    val description: String
)

private val meteorShowers = listOf(
    MeteorDefinition("Quadrantiden", 1, 3, 15.33, 49.0, 80),
    MeteorDefinition("Lyriden", 4, 22, 18.12, 34.0, 18),
    MeteorDefinition("Eta-Aquariiden", 5, 6, 22.53, -1.0, 50),
    MeteorDefinition("Südliche Delta-Aquariiden", 7, 30, 22.67, -16.0, 25),
    MeteorDefinition("Perseiden", 8, 12, 3.13, 58.0, 100),
    MeteorDefinition("September-Epsilon-Perseiden", 9, 9, 3.20, 40.0, 8),
    MeteorDefinition("Draconiden", 10, 8, 17.47, 54.0, 10),
    MeteorDefinition("Orioniden", 10, 21, 6.35, 16.0, 20),
    MeteorDefinition("Leoniden", 11, 17, 10.13, 22.0, 15),
    MeteorDefinition("Geminiden", 12, 14, 7.47, 33.0, 150),
    MeteorDefinition("Ursiden", 12, 22, 14.47, 76.0, 10)
)

@Composable
private fun EventsScreen(location: GeoPoint?) {
    val observer = location ?: GeoPoint(52.52, 13.405, 34.0)
    val events = remember(observer) { buildUpcomingEvents(observer) }
    Column(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Night, Color(0xFF09172A), Night))
        ).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(6.dp))
        AstraScreenHeader(
            eyebrow = "ASTRA EVENTS",
            title = "Himmelskalender",
            subtitle = if (location == null) "Berechnet für Demo-Standort Berlin"
            else "Für ${observer.latitude.format(3)}, ${observer.longitude.format(3)}",
            icon = Icons.Rounded.CalendarMonth
        )
        Text(
            "Die Einschätzung berücksichtigt Standort, Radiantenhöhe und ungefähres Mondlicht. " +
                "Das Maximum eines Meteorschauers kann sich leicht verschieben.",
            color = Color(0xFFAAB8CE),
            fontSize = 13.sp
        )

        AstraSectionTitle("Lichtverschmutzung", "Dunkle Beobachtungsplätze in deiner Umgebung finden")
        Text(
            "Helle Flächen zeigen starkes künstliches Nachtlicht. Verschiebe und zoome die Karte, " +
                "um einen dunkleren Beobachtungsplatz in deiner Nähe zu finden.",
            color = Color(0xFFAAB8CE),
            fontSize = 13.sp
        )
        LightPollutionMap(observer)
        Text(
            "NASA-VIIRS-Nachtlichtaufnahme (2012). Sie ist eine Orientierungshilfe für " +
                "Lichtverschmutzung, keine aktuelle Bortle-Messung.",
            color = Color(0xFFAAB8CE),
            fontSize = 11.sp
        )

        AstraSectionTitle("Kommende Ereignisse", "Finsternisse und Meteorschauer chronologisch sortiert")
        events.forEach { EventCard(it) }
        Text(
            "Quellen: NASA/GSFC Eclipse Catalog · International Meteor Organization (IMO)",
            color = Color(0xFFAAB8CE),
            fontSize = 11.sp
        )
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun EventCard(event: SkyEvent) {
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(event.kind.label.uppercase(Locale.GERMAN), color = AstraBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (event.timeIsApproximate) "Maximum-Nacht" else timeFormat.format(event.instant),
                    color = StarGold,
                    fontSize = 12.sp
                )
            }
            Text(event.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(dateFormat.format(event.instant), color = Color(0xFFD8E4F7))
            Text(event.status, color = event.statusColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(event.facts, color = StarGold, fontSize = 13.sp)
            Text(event.description, color = Color(0xFFAAB8CE), fontSize = 13.sp)
        }
    }
}

private fun buildUpcomingEvents(observer: GeoPoint): List<SkyEvent> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val meteorEvents = (today.year..today.year + 1).flatMap { year ->
        meteorShowers.map { shower ->
            val localPeak = ZonedDateTime.of(LocalDate.of(year, shower.month, shower.day), LocalTime.of(2, 0), zone)
            val instant = localPeak.toInstant()
            val radiant = CelestialObject(
                name = shower.name,
                catalogId = "IMO-Radiant",
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
                facts = "Radiant ca. ${altitude.format(0)}° hoch · Mond ca. ${moon.format(0)} % · ZHR ${shower.zhr}",
                description = "${quality.third} Die ZHR gilt nur unter ideal dunklem Himmel."
            )
        }
    }

    val eclipseEvents = exactLocalEclipseEvents(observer)

    val now = Instant.now().minus(1, ChronoUnit.DAYS)
    return (meteorEvents + eclipseEvents)
        .filter { it.instant.isAfter(now) }
        .sortedBy { it.instant }
        .take(14)
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

@Composable
@SuppressLint("SetJavaScriptEnabled")
private fun LightPollutionMap(observer: GeoPoint) {
    val context = LocalContext.current
    val webView = remember(context) {
        WebView(context).apply {
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    view.postDelayed({
                        view.evaluateJavascript("window.astraMap && window.astraMap.invalidateSize(true)", null)
                        view.invalidate()
                    }, 500L)
                }
            }
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.userAgentString = settings.userAgentString + " ProjektAstra/1.0"
        }
    }

    LaunchedEffect(observer) {
        val leafletCss = context.assets.open("leaflet-1.9.4.css").bufferedReader().use { it.readText() }
        val leafletJs = context.assets.open("leaflet-1.9.4.js").bufferedReader().use { it.readText() }
        val html = context.assets.open("light_pollution_map.html").bufferedReader().use { it.readText() }
            .replace("__LEAFLET_CSS__", leafletCss)
            .replace("__LEAFLET_JS__", leafletJs)
            .replace("__ASTRA_LAT__", observer.latitude.toString())
            .replace("__ASTRA_LON__", observer.longitude.toString())
        webView.loadDataWithBaseURL("https://projekt-astra.local/", html, "text/html", "UTF-8", null)
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().height(400.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NightBlue)
    ) {
        AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
    }
}

private data class ScorePenalty(
    val label: String,
    val points: Int,
    val explanation: String
)

private data class AstraScoreBreakdown(
    val score: Int,
    val penalties: List<ScorePenalty>
)

private object AstraScoreCalculator {
    fun calculate(weather: WeatherSnapshot, observer: GeoPoint, instant: Instant): AstraScoreBreakdown {
        return calculate(
            cloudCover = weather.cloudCover,
            rainProbability = weather.forecast.firstOrNull()?.rainProbability ?: 0,
            windSpeed = weather.windSpeed,
            visibilityMeters = weather.visibility,
            observer = observer,
            instant = instant
        )
    }

    fun calculate(
        cloudCover: Int,
        rainProbability: Int,
        windSpeed: Double,
        visibilityMeters: Double,
        observer: GeoPoint,
        instant: Instant
    ): AstraScoreBreakdown {
        val visibilityKilometers = visibilityMeters / 1_000.0
        val moon = SolarSystemCatalog.horizontal(Body.Moon, observer, instant)
        val moonIllumination = illumination(Body.Moon, instant.toAstroTime()).phaseFraction

        val penalties = listOf(
            ScorePenalty(
                "Bewölkung",
                (cloudCover * 0.45).roundToInt().coerceIn(0, 45),
                "$cloudCover % Wolken · maximal −45"
            ),
            ScorePenalty(
                "Regenrisiko",
                (rainProbability * 0.15).roundToInt().coerceIn(0, 15),
                "$rainProbability % in der nächsten Prognosestufe · maximal −15"
            ),
            ScorePenalty(
                "Wind",
                (((windSpeed - 5.0).coerceAtLeast(0.0) / 25.0) * 15.0)
                    .roundToInt().coerceIn(0, 15),
                "${windSpeed.format(1)} km/h; bis 5 km/h ohne Abzug · maximal −15"
            ),
            ScorePenalty(
                "Sichtweite",
                (((20.0 - visibilityKilometers).coerceAtLeast(0.0) / 20.0) * 15.0)
                    .roundToInt().coerceIn(0, 15),
                "${visibilityKilometers.format(1)} km; ab 20 km ohne Abzug · maximal −15"
            ),
            ScorePenalty(
                "Mondlicht",
                (moonIllumination * sin(Math.toRadians(moon.altitude)).coerceAtLeast(0.0) * 10.0)
                    .roundToInt().coerceIn(0, 10),
                "${(moonIllumination * 100.0).format(0)} % beleuchtet, ${moon.altitude.format(0)}° hoch · maximal −10"
            )
        )
        return AstraScoreBreakdown(
            score = (100 - penalties.sumOf { it.points }).coerceIn(0, 100),
            penalties = penalties
        )
    }
}

@Composable
private fun ObservationScore(weather: WeatherSnapshot, observer: GeoPoint) {
    var detailsVisible by remember { mutableStateOf(false) }
    val breakdown = remember(weather, observer) {
        AstraScoreCalculator.calculate(weather, observer, Instant.now())
    }
    val score = breakdown.score
    val label = when {
        score >= 75 -> "Sehr gute Sicht"
        score >= 50 -> "Brauchbare Sicht"
        score >= 25 -> "Eingeschränkt"
        else -> "Ungünstig"
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF122A49)),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, AstraBlue.copy(alpha = 0.28f))
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
                        "Die Lichtverschmutzung ist derzeit nur auf der Nachtlichtkarte sichtbar und noch nicht als Messwert im Score enthalten. Das Geländeprofil beeinflusst die sichtbare Horizontlinie, aber nicht das Wetter.",
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
private fun AboutScreen(redLightMode: Boolean, setRedLightMode: (Boolean) -> Unit) {
    Column(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Night, Color(0xFF09172A), Night))
        ).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
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
        AboutInfoCard(
            title = "Für die Nacht gebaut",
            icon = Icons.Rounded.Public,
            body = "5.041 reale Sterne, Sonne, Mond, Planeten, die Milchstraße und optional 1.016 Deep-Sky-Objekte werden passend zu Standort und Uhrzeit berechnet. Wetter, Ereigniskalender und Nachtlichtkarte helfen bei der Planung."
        )
        AboutInfoCard(
            title = "Datenschutz",
            icon = Icons.Rounded.GpsFixed,
            body = "Standortzugriff erfolgt erst nach deiner bewussten Freigabe und nur während der Nutzung. Koordinaten werden verschlüsselt an Open-Meteo übertragen, um Wetter und Geländehöhen abzurufen. Kamerabilder bleiben auf dem Gerät und werden weder gespeichert noch übertragen. Projekt Astra enthält keine Konten, Werbung, Analyse-SDKs oder Tracker."
        )
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
                Text("Deep Sky: OpenNGC · CC BY-SA 4.0", color = AstraTextMuted, fontSize = 12.sp)
                Text("Himmelsaufnahmen: DSS2 via CDS HiPS2FITS", color = AstraTextMuted, fontSize = 12.sp)
                Text("Wetter: RainViewer, Open-Meteo und OpenStreetMap", color = AstraTextMuted, fontSize = 12.sp)
                Text("Ereignisse: NASA/GSFC und IMO", color = AstraTextMuted, fontSize = 12.sp)
                Text("Nachtlicht: NASA GIBS / VIIRS", color = AstraTextMuted, fontSize = 12.sp)
                Text("Gelände: Open-Meteo / Copernicus GLO-90", color = AstraTextMuted, fontSize = 12.sp)
                Text("Ephemeriden: Astronomy Engine 2.1.19 · MIT", color = AstraTextMuted, fontSize = 12.sp)
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
    val astronomyDescription: String = ""
)
internal data class HorizontalCoordinates(val azimuth: Double, val altitude: Double)
private data class VisibleObject(val celestial: CelestialObject, val position: HorizontalCoordinates)

private object StarCatalog {
    private val fallbackObjects = listOf(
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

    fun load(context: Context): List<CelestialObject> = runCatching {
        context.assets.open("hyg_bright_stars.tsv").bufferedReader().useLines { lines ->
            lines.filterNot { it.startsWith("#") || it.isBlank() }
                .mapNotNull { line ->
                    val fields = line.split('\t')
                    if (fields.size < 9) return@mapNotNull null
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
                        luminositySolar = fields.getOrNull(13)?.toDoubleOrNull()
                    )
                }.toList()
        }.ifEmpty { fallbackObjects }
    }.getOrDefault(fallbackObjects)
}

private object DeepSkyCatalog {
    fun load(context: Context): List<CelestialObject> = runCatching {
        context.assets.open("openngc_deep_sky.tsv").bufferedReader().useLines { lines ->
            lines.filterNot { it.startsWith("#") || it.isBlank() }
                .mapNotNull { line ->
                    val fields = line.split('\t')
                    if (fields.size < 7) return@mapNotNull null
                    CelestialObject(
                        name = fields[0],
                        catalogId = fields[1],
                        raHours = fields[2].toDoubleOrNull() ?: return@mapNotNull null,
                        decDegrees = fields[3].toDoubleOrNull() ?: return@mapNotNull null,
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
        }
    }.getOrDefault(emptyList())
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

private fun starColor(colorIndex: Double): Color = when {
    colorIndex < -0.05 -> Color(0xFFB8D6FF)
    colorIndex < 0.45 -> Color(0xFFF4F7FF)
    colorIndex < 1.0 -> Color(0xFFFFEDC2)
    else -> Color(0xFFFFBF8A)
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

    fun load(point: GeoPoint, callback: (TerrainState) -> Unit) {
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
                val connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    requestMethod = "GET"
                }
                try {
                    if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
                    val json = org.json.JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
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
                } finally {
                    connection.disconnect()
                }
            }
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                callback(
                    result.fold(
                        onSuccess = {
                            cachedPoint = point
                            cachedProfile = it
                            TerrainState.Ready(it)
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
        val jd = instant.epochSecond / 86400.0 + 2440587.5
        val daysSinceJ2000 = jd - 2451545.0
        val gmst = normalizeDegrees(280.46061837 + 360.98564736629 * daysSinceJ2000)
        val localSidereal = normalizeDegrees(gmst + observer.longitude)
        val hourAngle = Math.toRadians(normalizeSignedDegrees(localSidereal - objectData.raHours * 15.0))
        val latitude = Math.toRadians(observer.latitude)
        val declination = Math.toRadians(objectData.decDegrees)
        val altitude = asin(
            sin(declination) * sin(latitude) + cos(declination) * cos(latitude) * cos(hourAngle)
        )
        val azimuth = atan2(
            -sin(hourAngle) * cos(declination),
            sin(declination) * cos(latitude) - cos(declination) * sin(latitude) * cos(hourAngle)
        )
        return HorizontalCoordinates(
            normalizeDegrees(Math.toDegrees(azimuth)),
            Math.toDegrees(altitude)
        )
    }
}

private data class HourlyForecast(
    val time: String,
    val hoursFromNow: Int,
    val cloudCover: Int,
    val rainProbability: Int,
    val windSpeed: Double,
    val visibility: Double
)

private data class WeatherSnapshot(
    val temperature: Double,
    val cloudCover: Int,
    val windSpeed: Double,
    val visibility: Double,
    val updatedAt: String,
    val forecast: List<HourlyForecast>
)

private sealed interface WeatherState {
    data object Idle : WeatherState
    data object Loading : WeatherState
    data class Ready(val weather: WeatherSnapshot) : WeatherState
    data class Error(val message: String) : WeatherState
}

private object WeatherRepository {
    fun load(point: GeoPoint, callback: (WeatherState) -> Unit) {
        thread(name = "astra-weather") {
            val result = runCatching {
                val url = java.net.URL(
                    "https://api.open-meteo.com/v1/forecast?latitude=${point.latitude}" +
                        "&longitude=${point.longitude}" +
                        "&current=temperature_2m,cloud_cover,wind_speed_10m" +
                        "&hourly=visibility,cloud_cover,precipitation_probability,wind_speed_10m" +
                        "&forecast_days=2&timezone=auto"
                )
                val connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                    connectTimeout = 8_000
                    readTimeout = 8_000
                    requestMethod = "GET"
                }
                try {
                    if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
                    val json = org.json.JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                    val current = json.getJSONObject("current")
                    val hourly = json.getJSONObject("hourly")
                    val times = hourly.getJSONArray("time")
                    val visibilities = hourly.getJSONArray("visibility")
                    val cloudCover = hourly.getJSONArray("cloud_cover")
                    val rainProbability = hourly.getJSONArray("precipitation_probability")
                    val hourlyWind = hourly.getJSONArray("wind_speed_10m")
                    val currentHour = current.getString("time").take(13)
                    var index = 0
                    for (i in 0 until times.length()) {
                        if (times.getString(i).startsWith(currentHour)) { index = i; break }
                    }
                    val forecast = (index until min(index + 24, times.length())).map { i ->
                        HourlyForecast(
                            time = times.getString(i).takeLast(5),
                            hoursFromNow = i - index,
                            cloudCover = cloudCover.optInt(i, 0),
                            rainProbability = rainProbability.optInt(i, 0),
                            windSpeed = hourlyWind.optDouble(i, 0.0),
                            visibility = visibilities.optDouble(i, 10_000.0)
                        )
                    }
                    WeatherSnapshot(
                        temperature = current.getDouble("temperature_2m"),
                        cloudCover = current.getInt("cloud_cover"),
                        windSpeed = current.getDouble("wind_speed_10m"),
                        visibility = visibilities.optDouble(index, 10_000.0),
                        updatedAt = current.getString("time").takeLast(5),
                        forecast = forecast
                    )
                } finally {
                    connection.disconnect()
                }
            }
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                callback(
                    result.fold(
                        onSuccess = { WeatherState.Ready(it) },
                        onFailure = { WeatherState.Error("Wetterdaten konnten nicht geladen werden.") }
                    )
                )
            }
        }
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
