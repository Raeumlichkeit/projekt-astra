package de.projektastra.app

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
import kotlin.math.sin

private val Night = Color(0xFF07101F)
private val NightBlue = Color(0xFF0D1C34)
private val AstraBlue = Color(0xFF6DA8FF)
private val StarGold = Color(0xFFFFD98A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AstraTheme { AstraApp() } }
    }
}

@Composable
private fun AstraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AstraBlue,
            secondary = StarGold,
            background = Night,
            surface = NightBlue,
            onBackground = Color(0xFFEAF1FF),
            onSurface = Color(0xFFEAF1FF)
        ),
        content = content
    )
}

private enum class AstraTab { SKY, WEATHER, ABOUT }

@Composable
private fun AstraApp() {
    var tab by remember { mutableStateOf(AstraTab.SKY) }
    var location by remember { mutableStateOf<GeoPoint?>(null) }
    var permissionGranted by remember { mutableStateOf(false) }
    var cameraGranted by remember { mutableStateOf(false) }
    var arEnabled by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }
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
        if (!permissionGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    LocationEffect(permissionGranted) { location = it }

    Scaffold(
        containerColor = Night,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF091426)) {
                NavigationBarItem(
                    selected = tab == AstraTab.SKY,
                    onClick = { tab = AstraTab.SKY },
                    icon = { Icon(Icons.Rounded.Public, null) },
                    label = { Text("Sternkarte") }
                )
                NavigationBarItem(
                    selected = tab == AstraTab.WEATHER,
                    onClick = { tab = AstraTab.WEATHER },
                    icon = { Icon(Icons.Rounded.Cloud, null) },
                    label = { Text("Wetter") }
                )
                NavigationBarItem(
                    selected = tab == AstraTab.ABOUT,
                    onClick = { tab = AstraTab.ABOUT },
                    icon = { Icon(Icons.Rounded.Info, null) },
                    label = { Text("Info") }
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                AstraTab.SKY -> SkyScreen(
                    location = location,
                    locationPermissionGranted = permissionGranted,
                    cameraPermissionGranted = cameraGranted,
                    arEnabled = arEnabled,
                    requestLocationPermission = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        )
                    },
                    toggleAr = {
                        if (arEnabled) arEnabled = false
                        else if (cameraGranted) arEnabled = true
                        else cameraLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
                AstraTab.WEATHER -> WeatherScreen(location)
                AstraTab.ABOUT -> AboutScreen()
            }
        }
    }
}

@Composable
private fun LocationEffect(enabled: Boolean, onLocation: (GeoPoint) -> Unit) {
    val context = LocalContext.current
    DisposableEffect(enabled) {
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

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensor?.let { manager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        onDispose { manager.unregisterListener(listener) }
    }
    return OrientationState(azimuth, pitch, available)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkyScreen(
    location: GeoPoint?,
    locationPermissionGranted: Boolean,
    cameraPermissionGranted: Boolean,
    arEnabled: Boolean,
    requestLocationPermission: () -> Unit,
    toggleAr: () -> Unit
) {
    val observer = location ?: GeoPoint(52.52, 13.405, 34.0)
    val orientation = rememberOrientation(observer)
    val context = LocalContext.current
    val catalog = remember { StarCatalog.load(context) }
    val cameraFov = rememberCameraHorizontalFov()
    var selected by remember { mutableStateOf<VisibleObject?>(null) }
    val visible = remember(observer, orientation.azimuth, orientation.altitude) {
        catalog.map {
            VisibleObject(it, AstronomyEngine.horizontalCoordinates(it, observer, Instant.now()))
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
                Icon(Icons.Rounded.GpsFixed, null, tint = if (location != null) Color(0xFF76E0A0) else StarGold)
                Spacer(Modifier.width(6.dp))
                Text(if (location != null) "GPS" else "Berlin Demo", fontSize = 12.sp)
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (arEnabled && cameraPermissionGranted) CameraPreview()
            SkyCanvas(
                objects = visible,
                viewAzimuth = orientation.azimuth.toDouble(),
                viewAltitude = orientation.altitude.toDouble(),
                horizontalFov = if (arEnabled) cameraFov else 95.0,
                arMode = arEnabled,
                onSelect = { selected = it }
            )
            Column(Modifier.align(Alignment.TopCenter), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(cardinalDirection(orientation.azimuth), color = StarGold, fontWeight = FontWeight.Bold)
                Text("${orientation.azimuth.toInt()}° · ${orientation.altitude.toInt()}° Höhe", fontSize = 12.sp)
            }
            Button(
                onClick = toggleAr,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (arEnabled) StarGold else NightBlue.copy(alpha = 0.88f),
                    contentColor = if (arEnabled) Night else Color.White
                )
            ) {
                Icon(if (arEnabled) Icons.Rounded.Map else Icons.Rounded.CameraAlt, null)
                Spacer(Modifier.width(6.dp))
                Text(if (arEnabled) "Karte" else "AR")
            }
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
            Button(
                onClick = requestLocationPermission,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AstraBlue)
            ) {
                Icon(Icons.Rounded.MyLocation, null)
                Spacer(Modifier.width(8.dp))
                Text("Standort für meinen Himmel verwenden")
            }
        }
    }

    selected?.let { item ->
        ModalBottomSheet(onDismissRequest = { selected = null }, containerColor = NightBlue) {
            ObjectDetails(item, observer)
        }
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
    onSelect: (VisibleObject) -> Unit
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
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
            .then(
                if (arMode) Modifier.background(Color.Black.copy(alpha = 0.28f))
                else Modifier.background(Brush.radialGradient(listOf(Color(0xFF142D50), Night)))
            )
    ) {
        drawSkyGrid(viewAzimuth, viewAltitude)
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
        val verticalFov = horizontalFov * size.height / size.width
        val horizonY = (size.height * (0.5 - (0.0 - viewAltitude) / verticalFov)).toFloat()
        if (horizonY in 0f..size.height) {
            drawLine(StarGold.copy(alpha = 0.65f), Offset(0f, horizonY), Offset(size.width, horizonY), 2f)
            drawContext.canvas.nativeCanvas.drawText(
                "HORIZONT",
                18f,
                horizonY - 10f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.rgb(255, 217, 138)
                    textSize = 24f
                    alpha = 190
                }
            )
        }
        projected.forEach { (item, point) ->
            val radius = (7.5f - item.celestial.magnitude.toFloat()).coerceIn(1.4f, 10f)
            drawCircle(
                color = starColor(item.celestial.colorIndex),
                radius = radius,
                center = point
            )
            if (item.celestial.magnitude < 1.5 && !item.celestial.name.startsWith("HIP ")) {
                drawContext.canvas.nativeCanvas.drawText(
                    item.celestial.name,
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
    val path = remember(item.celestial, observer) {
        (0..12).map { hours ->
            val at = Instant.now().plusSeconds(hours * 3600L)
            at to AstronomyEngine.horizontalCoordinates(item.celestial, observer, at)
        }
    }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(item.celestial.name, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(item.celestial.catalogId, color = AstraBlue)
        Spacer(Modifier.height(18.dp))
        DetailRow("Koordinaten", "RA ${formatRa(item.celestial.raHours)} · Dec ${formatDec(item.celestial.decDegrees)}")
        DetailRow("Aktuelle Position", "Az ${item.position.azimuth.format(1)}° · Höhe ${item.position.altitude.format(1)}°")
        DetailRow("Helligkeit", "${item.celestial.magnitude.format(2)} mag")
        DetailRow(
            "Entfernung",
            if (item.celestial.distanceLightYears > 0.0) {
                "${item.celestial.distanceLightYears.format(1)} Lichtjahre"
            } else "unbekannt"
        )
        DetailRow("Spektralklasse", item.celestial.spectralClass)
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
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFFAAB8CE))
        Text(value, fontWeight = FontWeight.Medium)
    }
    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
}

@Composable
private fun WeatherScreen(location: GeoPoint?) {
    var state by remember { mutableStateOf<WeatherState>(WeatherState.Idle) }
    val observer = location ?: GeoPoint(52.52, 13.405, 34.0)

    LaunchedEffect(observer) {
        state = WeatherState.Loading
        WeatherRepository.load(observer) { state = it }
    }

    Column(
        Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Beobachtungswetter", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(
            if (location == null) "Demo-Standort Berlin" else "${observer.latitude.format(3)}, ${observer.longitude.format(3)}",
            color = AstraBlue
        )
        when (val value = state) {
            WeatherState.Idle, WeatherState.Loading -> Text("Aktuelle Daten werden geladen …")
            is WeatherState.Error -> {
                Text(value.message, color = StarGold)
                Button(onClick = {
                    state = WeatherState.Loading
                    WeatherRepository.load(observer) { state = it }
                }) { Text("Erneut versuchen") }
            }
            is WeatherState.Ready -> {
                ObservationScore(value.weather)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WeatherTile("Temperatur", "${value.weather.temperature.format(1)} °C", Modifier.weight(1f))
                    WeatherTile("Bewölkung", "${value.weather.cloudCover}%", Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WeatherTile("Wind", "${value.weather.windSpeed.format(1)} km/h", Modifier.weight(1f))
                    WeatherTile("Sichtweite", "${(value.weather.visibility / 1000.0).format(1)} km", Modifier.weight(1f))
                }
                Text("Quelle: Open-Meteo · zuletzt ${value.weather.updatedAt}", fontSize = 12.sp, color = Color(0xFFAAB8CE))
                Text("Wolken- und Regenkarte", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Regenradar mit 2-Stunden-Zeitleiste und aktuelle Bewölkung. Ebenen lassen sich direkt in der Karte umschalten.",
                    color = Color(0xFFAAB8CE),
                    fontSize = 13.sp
                )
                WeatherMap(observer)
            }
        }
    }
}

@Composable
private fun WeatherMap(observer: GeoPoint) {
    val context = LocalContext.current
    val webView = remember(context) {
        WebView(context).apply {
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.userAgentString = settings.userAgentString + " ProjektAstra/0.3"
        }
    }

    LaunchedEffect(observer) {
        val html = context.assets.open("weather_map.html").bufferedReader().use { it.readText() }
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
        modifier = Modifier.fillMaxWidth().height(520.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NightBlue)
    ) {
        AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun ObservationScore(weather: WeatherSnapshot) {
    val score = (100 - weather.cloudCover - min(25.0, weather.windSpeed * 1.4)).toInt().coerceIn(0, 100)
    val label = when {
        score >= 75 -> "Sehr gute Sicht"
        score >= 50 -> "Brauchbare Sicht"
        score >= 25 -> "Eingeschränkt"
        else -> "Ungünstig"
    }
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF122A49))) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(64.dp).background(AstraBlue.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("$score", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AstraBlue) }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Vorläufiger Astra-Score", color = Color(0xFFAAB8CE))
            }
        }
    }
}

@Composable
private fun WeatherTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = NightBlue)) {
        Column(Modifier.padding(18.dp)) {
            Text(label, color = Color(0xFFAAB8CE), fontSize = 13.sp)
            Spacer(Modifier.height(5.dp))
            Text(value, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AboutScreen() {
    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Projekt Astra", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("Dein Begleiter für den Nachthimmel", color = AstraBlue)
        Spacer(Modifier.height(24.dp))
        Text("Die App berechnet die Positionen von 5.070 realen Sternen direkt auf dem Gerät und verbindet sie mit Standort, Kamera und Ausrichtung des Smartphones.")
        Spacer(Modifier.height(18.dp))
        Text("Hinweis", fontWeight = FontWeight.Bold)
        Text("Die Sensoranzeige ist eine Orientierungshilfe. Für präzise Beobachtungen sollte der Kompass kalibriert und magnetische Störquellen vermieden werden.")
        Spacer(Modifier.height(18.dp))
        Text("Sternkatalog: HYG v4.1 · CC BY-SA 4.0", color = Color(0xFFAAB8CE))
        Text("Wetterkarte: RainViewer, Open-Meteo und OpenStreetMap", color = Color(0xFFAAB8CE))
        Text("Version 0.3.0 · AR und Wetterkarte", color = Color(0xFFAAB8CE))
    }
}

internal data class GeoPoint(val latitude: Double, val longitude: Double, val altitudeMeters: Double)
private data class OrientationState(val azimuth: Float, val altitude: Float, val available: Boolean)
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
    val constellation: String = ""
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
                        constellation = fields[8]
                    )
                }.toList()
        }.ifEmpty { fallbackObjects }
    }.getOrDefault(fallbackObjects)
}

private object ConstellationLines {
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
        91262 to 102098, 102098 to 97649, 97649 to 91262
    )
}

private fun starColor(colorIndex: Double): Color = when {
    colorIndex < -0.05 -> Color(0xFFB8D6FF)
    colorIndex < 0.45 -> Color(0xFFF4F7FF)
    colorIndex < 1.0 -> Color(0xFFFFEDC2)
    else -> Color(0xFFFFBF8A)
}

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

private data class WeatherSnapshot(
    val temperature: Double,
    val cloudCover: Int,
    val windSpeed: Double,
    val visibility: Double,
    val updatedAt: String
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
                        "&hourly=visibility&forecast_days=1&timezone=auto"
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
                    val currentHour = current.getString("time").take(13)
                    var index = 0
                    for (i in 0 until times.length()) {
                        if (times.getString(i).startsWith(currentHour)) { index = i; break }
                    }
                    WeatherSnapshot(
                        temperature = current.getDouble("temperature_2m"),
                        cloudCover = current.getInt("cloud_cover"),
                        windSpeed = current.getDouble("wind_speed_10m"),
                        visibility = visibilities.optDouble(index, 10_000.0),
                        updatedAt = current.getString("time").takeLast(5)
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
