package de.projektastra.app

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.projektastra.app.ephemeris.LunarFeatureCatalog
import de.projektastra.app.ephemeris.LunarFeatureHighlight
import de.projektastra.app.ephemeris.LunarFeatureType
import de.projektastra.app.ephemeris.LunarTerminatorCalculator
import de.projektastra.app.ephemeris.LunarTerminatorState
import io.github.cosinekitty.astronomy.Body
import java.time.Instant
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

private fun Double.format(decimals: Int): String = String.format(Locale.GERMANY, "%.${decimals}f", this)

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFFAAB8CE), fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.White)
    }
}

internal enum class LunarOpticsMode(val label: String) {
    NORMAL("Normal (Fernglas)"),
    NEWTON("Newton (180°)"),
    ZENITH("Zenitspiegel (gespiegelt)")
}

internal enum class LunarFeatureFilter(val label: String) {
    TERMINATOR("Am Terminator"),
    ALL_VISIBLE("Alle sichtbaren"),
    CRATERS("Krater"),
    RILLES("Rillen & Täler"),
    MARIA("Meere & Gebirge")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun MoonDetailSheet(
    skyInstant: Instant,
    onDismissRequest: () -> Unit,
    onLogFeature: ((CelestialObject) -> Unit)? = null
) {
    val terminatorState = remember(skyInstant) {
        LunarTerminatorCalculator.calculateTerminator(skyInstant)
    }
    val allHighlights = remember(terminatorState) {
        LunarTerminatorCalculator.featuresNearTerminator(terminatorState, LunarFeatureCatalog.allFeatures)
    }
    val visibleTerminatorPoints = remember(terminatorState) {
        LunarTerminatorCalculator.generateVisibleTerminatorPath(terminatorState, stepDegrees = 2.0)
    }

    var opticsMode by remember { mutableStateOf(LunarOpticsMode.NORMAL) }
    var activeFilter by remember { mutableStateOf(LunarFeatureFilter.TERMINATOR) }

    val filteredHighlights = remember(allHighlights, activeFilter) {
        when (activeFilter) {
            LunarFeatureFilter.TERMINATOR -> allHighlights.filter { it.inOptimalRelief }
            LunarFeatureFilter.ALL_VISIBLE -> allHighlights.filter { it.isVisibleFromEarth }
            LunarFeatureFilter.CRATERS -> allHighlights.filter { it.feature.type == LunarFeatureType.CRATER && it.isVisibleFromEarth }
            LunarFeatureFilter.RILLES -> allHighlights.filter { it.feature.type == LunarFeatureType.RILLE && it.isVisibleFromEarth }
            LunarFeatureFilter.MARIA -> allHighlights.filter {
                (it.feature.type == LunarFeatureType.MARE || it.feature.type == LunarFeatureType.MOUNTAIN) && it.isVisibleFromEarth
            }
        }
    }

    var selectedHighlight by remember {
        mutableStateOf(allHighlights.firstOrNull { it.inOptimalRelief } ?: allHighlights.firstOrNull())
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF070F1E),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Mond-Terminator & Relief",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Beleuchtung: ${(terminatorState.phaseFraction * 100.0).format(0)} % · C₀: ${terminatorState.colongitude.format(1)}°",
                        fontSize = 12.sp,
                        color = Color(0xFFFFD54F)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF131E33),
                    border = BorderStroke(1.dp, Color(0xFF4FA3E3).copy(alpha = 0.3f))
                ) {
                    Text(
                        "L ${if (terminatorState.subEarthLon >= 0) "+" else ""}${terminatorState.subEarthLon.format(1)}° · B ${if (terminatorState.subEarthLat >= 0) "+" else ""}${terminatorState.subEarthLat.format(1)}°",
                        fontSize = 11.sp,
                        color = Color(0xFFAAB8CE),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Optics Mode Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LunarOpticsMode.values().forEach { mode ->
                    FilterChip(
                        selected = opticsMode == mode,
                        onClick = { opticsMode = mode },
                        label = { Text(mode.label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF4FA3E3),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF0F172A),
                            labelColor = Color(0xFFAAB8CE)
                        )
                    )
                }
            }

            // Interactive Moon Disc Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(260.dp)
                        .pointerInput(opticsMode, filteredHighlights) {
                            detectTapGestures { tapOffset ->
                                val sizePx = size.width.toFloat()
                                val cx = sizePx / 2f
                                val cy = sizePx / 2f
                                val r = (sizePx / 2f) * 0.9f

                                val clicked = filteredHighlights.minByOrNull { h ->
                                    val (px, py) = transformPoint(h.diskX.toFloat(), h.diskY.toFloat(), opticsMode, cx, cy, r)
                                    val dx = px - tapOffset.x
                                    val dy = py - tapOffset.y
                                    sqrt(dx * dx + dy * dy)
                                }
                                if (clicked != null) {
                                    val (px, py) = transformPoint(clicked.diskX.toFloat(), clicked.diskY.toFloat(), opticsMode, cx, cy, r)
                                    val dist = sqrt((px - tapOffset.x) * (px - tapOffset.x) + (py - tapOffset.y) * (py - tapOffset.y))
                                    if (dist <= 30.dp.toPx()) {
                                        selectedHighlight = clicked
                                    }
                                }
                            }
                        }
                ) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = (size.width / 2f) * 0.9f

                    // Lunar background disc
                    drawCircle(color = Color(0xFF151D28), radius = r, center = Offset(cx, cy))
                    drawCircle(color = Color(0xFF334155), radius = r, center = Offset(cx, cy), style = Stroke(1.5f))

                    // Draw visible terminator polyline
                    if (visibleTerminatorPoints.size >= 2) {
                        val termPath = Path()
                        val (firstX, firstY) = transformPoint(
                            visibleTerminatorPoints[0].first,
                            visibleTerminatorPoints[0].second,
                            opticsMode, cx, cy, r
                        )
                        termPath.moveTo(firstX, firstY)
                        for (i in 1 until visibleTerminatorPoints.size) {
                            val (px, py) = transformPoint(
                                visibleTerminatorPoints[i].first,
                                visibleTerminatorPoints[i].second,
                                opticsMode, cx, cy, r
                            )
                            termPath.lineTo(px, py)
                        }
                        drawPath(termPath, color = Color(0xFFFFD54F).copy(alpha = 0.85f), style = Stroke(width = 2.0f))
                    }

                    // Feature markers
                    filteredHighlights.forEach { h ->
                        val (px, py) = transformPoint(h.diskX.toFloat(), h.diskY.toFloat(), opticsMode, cx, cy, r)
                        val isSelected = h.feature.id == selectedHighlight?.feature?.id

                        if (h.inOptimalRelief) {
                            // Pulsing amber halo for optimal relief
                            drawCircle(
                                color = Color(0xFFFFB300).copy(alpha = pulseAlpha),
                                radius = 7f * pulseScale,
                                center = Offset(px, py)
                            )
                            drawCircle(color = Color(0xFFFFD54F), radius = 4f, center = Offset(px, py))
                        } else if (h.sunElevationDegrees > 0.0) {
                            drawCircle(color = Color.White.copy(alpha = 0.7f), radius = 2.5f, center = Offset(px, py))
                        } else {
                            drawCircle(color = Color(0xFF64748B).copy(alpha = 0.4f), radius = 2.0f, center = Offset(px, py))
                        }

                        if (isSelected) {
                            drawCircle(
                                color = Color(0xFF38BDF8),
                                radius = 10f,
                                center = Offset(px, py),
                                style = Stroke(2f)
                            )
                        }
                    }
                }
            }

            // Feature Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LunarFeatureFilter.values().forEach { filter ->
                    FilterChip(
                        selected = activeFilter == filter,
                        onClick = { activeFilter = filter },
                        label = {
                            val count = when (filter) {
                                LunarFeatureFilter.TERMINATOR -> allHighlights.count { it.inOptimalRelief }
                                LunarFeatureFilter.ALL_VISIBLE -> allHighlights.count { it.isVisibleFromEarth }
                                LunarFeatureFilter.CRATERS -> allHighlights.count { it.feature.type == LunarFeatureType.CRATER && it.isVisibleFromEarth }
                                LunarFeatureFilter.RILLES -> allHighlights.count { it.feature.type == LunarFeatureType.RILLE && it.isVisibleFromEarth }
                                LunarFeatureFilter.MARIA -> allHighlights.count { (it.feature.type == LunarFeatureType.MARE || it.feature.type == LunarFeatureType.MOUNTAIN) && it.isVisibleFromEarth }
                            }
                            Text("${filter.label} ($count)", fontSize = 11.sp)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFD54F),
                            selectedLabelColor = Color(0xFF0F172A),
                            containerColor = Color(0xFF131E33),
                            labelColor = Color(0xFFAAB8CE)
                        )
                    )
                }
            }

            // Selected Feature Details Card
            selectedHighlight?.let { highlight ->
                val feat = highlight.feature
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, if (highlight.inOptimalRelief) Color(0xFFFFD54F).copy(alpha = 0.5f) else Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    feat.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    "${feat.type.label} · Durchmesser: ${feat.diameterKm.format(0)} km",
                                    fontSize = 12.sp,
                                    color = Color(0xFFAAB8CE)
                                )
                            }
                            if (highlight.inOptimalRelief) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFFD54F).copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Color(0xFFFFD54F))
                                ) {
                                    Text(
                                        "Optimales Relief (${(highlight.reliefScore * 100).toInt()} %)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFD54F),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        // Details grid
                        val latStr = "${abs(feat.selenographicLat).format(1)}° ${if (feat.selenographicLat >= 0) "N" else "S"}"
                        val lonStr = "${abs(feat.selenographicLon).format(1)}° ${if (feat.selenographicLon >= 0) "O" else "W"}"
                        val elevPrefix = if (highlight.sunElevationDegrees >= 0) "+" else ""
                        val elevDesc = when {
                            highlight.inOptimalRelief -> "Optimaler Schattenwurf"
                            highlight.sunElevationDegrees in 12.0..30.0 -> "Flaches Sonnenlicht"
                            highlight.sunElevationDegrees > 30.0 -> "Hoher Sonnenstand (wenig Schatten)"
                            else -> "In der Mondnacht (unsichtbar)"
                        }

                        DetailRow("Selenographische Lage", "$latStr, $lonStr")
                        DetailRow("Sonnenhöhe (h_sun)", "$elevPrefix${highlight.sunElevationDegrees.format(1)}° ($elevDesc, ${highlight.eventType.label})")
                        DetailRow("Terminator-Abstand", "${highlight.distanceToTerminatorDegrees.format(1)}° selenographisch (~${(highlight.distanceToTerminatorDegrees * 30.3).format(0)} km)")

                        Spacer(Modifier.height(4.dp))
                        Text(feat.description, fontSize = 13.sp, color = Color(0xFFCBD5E1))

                        if (feat.observationTip.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B).copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Beobachtungstipp: ${feat.observationTip}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        if (onLogFeature != null) {
                            Spacer(Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    val celestial = CelestialObject(
                                        name = feat.name,
                                        catalogId = "MoonFeature · ${feat.name}",
                                        raHours = 0.0,
                                        decDegrees = 0.0,
                                        magnitude = -12.0,
                                        distanceLightYears = 0.0,
                                        spectralClass = "",
                                        objectType = CelestialType.MOON,
                                        solarBody = Body.Moon,
                                        astronomyDescription = "${feat.type.label} am Mondterminator: ${feat.description}"
                                    )
                                    onLogFeature(celestial)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4FA3E3)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Im Logbuch beobachten", fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Quick list of highlights matching filter
            Text("Objekte (${filteredHighlights.size})", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                filteredHighlights.take(20).forEach { item ->
                    val isSelected = item.feature.id == selectedHighlight?.feature?.id
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF4FA3E3) else Color(0xFF1E293B).copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedHighlight = item }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(item.feature.name, fontWeight = FontWeight.Medium, color = Color.White, fontSize = 13.sp)
                                Text("${item.feature.type.label} · ${item.feature.diameterKm.format(0)} km", fontSize = 11.sp, color = Color(0xFFAAB8CE))
                            }
                            if (item.inOptimalRelief) {
                                Text("Relief ${(item.reliefScore * 100).toInt()}%", fontSize = 11.sp, color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold)
                            } else {
                                Text("${item.sunElevationDegrees.format(0)}°", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun transformPoint(
    diskX: Float,
    diskY: Float,
    opticsMode: LunarOpticsMode,
    cx: Float,
    cy: Float,
    r: Float
): Pair<Float, Float> {
    val (tx, ty) = when (opticsMode) {
        LunarOpticsMode.NORMAL -> diskX to diskY
        LunarOpticsMode.NEWTON -> -diskX to -diskY
        LunarOpticsMode.ZENITH -> -diskX to diskY
    }
    // diskX: + is East (right)
    // diskY: + is North (up on sky)
    val screenX = cx + tx * r
    val screenY = cy - ty * r
    return screenX to screenY
}
