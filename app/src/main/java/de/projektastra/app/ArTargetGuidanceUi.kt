package de.projektastra.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.South
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.roundToInt

private val AstraBlue = Color(0xFF6DA8FF)
private val StarGold = Color(0xFFFFD98A)
private val AstraSurfaceHigh = Color(0xFF163252)
private val AlertOrange = Color(0xFFFF9800)
private val AlertRed = Color(0xFFFF5252)

/**
 * AR-Zielhilfe-Overlay: Zeichnet Retikel, Richtungszeiger am Bildschirmrand und Statusbanner.
 */
@Composable
internal fun ArTargetGuidanceOverlay(
    guidance: ArTargetGuidance,
    onOpenCalibration: () -> Unit,
    onDismissTarget: () -> Unit,
    redLightMode: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val primaryColor = if (redLightMode) Color(0xFFFF5252) else StarGold
    val accentColor = if (redLightMode) Color(0xFFFF7868) else AstraBlue

    Box(modifier.fillMaxSize()) {
        // 1. Wenn das Ziel im Kamerasichtfeld liegt: Retikel am Zielpunkt zeichnen
        if (guidance.isInView && guidance.screenPosition != null) {
            ArTargetReticle(
                screenPosition = guidance.screenPosition,
                targetName = guidance.targetName,
                angularDistance = guidance.angularDistanceDegrees,
                color = primaryColor,
                accentColor = accentColor
            )
        } else {
            // 2. Ziel außerhalb des Sichtfelds: Richtungspfeil am Bildschirmrand
            ArEdgePointer(
                edgePosition = guidance.edgePosition,
                arrowAngle = guidance.arrowAngleDegrees,
                angularDistance = guidance.angularDistanceDegrees,
                isBehind = guidance.isBehind,
                isBelowHorizon = guidance.isBelowHorizon,
                isBelowTerrain = guidance.isBelowTerrain,
                color = if (guidance.isBelowHorizon || guidance.isBelowTerrain) AlertOrange else primaryColor
            )
        }

        // 3. Status-Banner am oberen Bildschirmrand
        ArGuidanceBanner(
            guidance = guidance,
            onOpenCalibration = onOpenCalibration,
            onDismissTarget = onDismissTarget,
            redLightMode = redLightMode,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp, start = 12.dp, end = 12.dp)
        )
    }
}

/**
 * Zielsuch-Retikel direkt über dem anvisierten Himmelsobjekt im AR-Kamerabild.
 */
@Composable
private fun ArTargetReticle(
    screenPosition: Offset,
    targetName: String,
    angularDistance: Double,
    color: Color,
    accentColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "reticle_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val reticleRadiusDp = 26.dp
    val density = LocalDensity.current
    val radiusPx = with(density) { reticleRadiusDp.toPx() } * pulseScale

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val center = screenPosition
            // Äußerer Zielkreis
            drawCircle(
                color = color.copy(alpha = 0.85f),
                radius = radiusPx,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Innerer Punkt
            drawCircle(
                color = accentColor,
                radius = 3.dp.toPx(),
                center = center
            )

            // 4 Markierungslinien (Kreuzecken)
            val tickLen = 8.dp.toPx()
            val gap = radiusPx + 2.dp.toPx()
            drawLine(color, center - Offset(0f, gap), center - Offset(0f, gap + tickLen), 2.dp.toPx(), StrokeCap.Round)
            drawLine(color, center + Offset(0f, gap), center + Offset(0f, gap + tickLen), 2.dp.toPx(), StrokeCap.Round)
            drawLine(color, center - Offset(gap, 0f), center - Offset(gap + tickLen, 0f), 2.dp.toPx(), StrokeCap.Round)
            drawLine(color, center + Offset(gap, 0f), center + Offset(gap + tickLen, 0f), 2.dp.toPx(), StrokeCap.Round)
        }

        // Beschriftung neben dem Retikel
        Surface(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (screenPosition.x + radiusPx + with(density) { 10.dp.toPx() }).roundToInt(),
                        (screenPosition.y - with(density) { 14.dp.toPx() }).roundToInt()
                    )
                },
            shape = RoundedCornerShape(6.dp),
            color = Color(0xDD0A1526),
            border = BorderStroke(1.dp, color.copy(alpha = 0.6f))
        ) {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(
                    text = targetName,
                    color = color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${String.format(Locale.GERMAN, "%.1f°", angularDistance)} vom Zentrum",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Zeiger am Bildschirmrand, der in die Richtung des Ziels weist.
 */
@Composable
private fun ArEdgePointer(
    edgePosition: Offset,
    arrowAngle: Float,
    angularDistance: Double,
    isBehind: Boolean,
    isBelowHorizon: Boolean,
    isBelowTerrain: Boolean,
    color: Color
) {
    val density = LocalDensity.current
    val chipWidthDp = 52.dp
    val chipHeightDp = 52.dp

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (edgePosition.x - with(density) { chipWidthDp.toPx() / 2f }).roundToInt(),
                    (edgePosition.y - with(density) { chipHeightDp.toPx() / 2f }).roundToInt()
                )
            }
            .size(chipWidthDp, chipHeightDp)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = Color(0xEE0D1C34),
            border = BorderStroke(2.dp, color),
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isBehind) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Umdrehen",
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Navigation,
                        contentDescription = "Richtung",
                        tint = color,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(arrowAngle)
                    )
                }
                Text(
                    text = "${angularDistance.roundToInt()}°",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Kompaktes Statusbanner am oberen Rand mit Objektinfo, Winkelabstand und Kalibrierungswarnung.
 */
@Composable
private fun ArGuidanceBanner(
    guidance: ArTargetGuidance,
    onOpenCalibration: () -> Unit,
    onDismissTarget: () -> Unit,
    redLightMode: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xEE091424),
        border = BorderStroke(1.dp, if (redLightMode) Color(0xFFFF5252).copy(alpha = 0.5f) else AstraSurfaceHigh)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = when {
                            guidance.isBelowHorizon -> Icons.Rounded.South
                            guidance.isBehind -> Icons.Rounded.Refresh
                            guidance.isInView -> Icons.Rounded.CheckCircle
                            else -> Icons.Rounded.Explore
                        },
                        contentDescription = null,
                        tint = when {
                            guidance.isBelowHorizon || guidance.isBelowTerrain -> AlertOrange
                            guidance.isInView -> if (redLightMode) Color(0xFFFF5252) else Color(0xFF81C784)
                            else -> if (redLightMode) Color(0xFFFF5252) else StarGold
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${guidance.targetName} · ${guidance.targetTypeLabel}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismissTarget,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Zielhilfe beenden", tint = Color.White.copy(alpha = 0.7f))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = guidance.statusText,
                    color = when {
                        guidance.isBelowHorizon || guidance.isBelowTerrain -> AlertOrange
                        guidance.isInView -> if (redLightMode) Color(0xFFFF7868) else Color(0xFF81C784)
                        else -> if (redLightMode) Color(0xFFFF7868) else StarGold
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                // Kalibrierungswarnung bei ungenauem Magnetfeldsensor
                if (!guidance.sensorQuality.isAccurate) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onOpenCalibration() }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = AlertOrange,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = "Sensor ungenau (Kalibrieren)",
                            color = AlertOrange,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
