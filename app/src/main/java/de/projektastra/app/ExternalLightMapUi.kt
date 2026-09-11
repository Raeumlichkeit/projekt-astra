package de.projektastra.app

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LifecycleStartEffect

/** The explicit load button is the separate, session-only consent for this external provider. */
@Composable
internal fun ExternalLightMapEntry(observer: GeoPoint, demoLocation: Boolean, redLight: Boolean) {
    var open by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("LightPollutionMap.app", style = MaterialTheme.typography.titleMedium)
            Text("Externe Lichtkarte mit farbigen Himmelshelligkeits- und Bortle-Modellen. Datenjahr und Modellstand stehen in der Karte.")
            Text(
                "Erst mit „Externe Karte laden“ erlaubst du für diese Ansicht Anfragen an LightPollutionMap.app, seine Karten-API, " +
                    "jsDelivr und die freigeschalteten Grundkartenanbieter OpenStreetMap, CARTO und Esri. " +
                    "Sie erhalten IP-Adresse, Anfragezeit, Browser-/Geräteangaben und den betrachteten Kartenbereich. " +
                    if (demoLocation) "Als Startort wird Berlin verwendet." else "Dein Startort wird auf 0,01° gerundet.",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Kein GPS-, Kamera- oder Dateizugriff für die Website. Ihre Daten sind Modellwerte und werden nicht in Astras Score übernommen. " +
                    "Zusätzliche Anbieterfunktionen wie Wetter, Adresssuche und Foto-Upload sind nicht freigeschaltet.",
                style = MaterialTheme.typography.bodySmall
            )
            Button(onClick = { open = true }, modifier = Modifier.testTag("external-light-map-open")) {
                Text("Externe Karte laden")
            }
        }
    }
    if (open) ExternalLightMapDialog(observer, redLight) { open = false }
}

@Composable
private fun ExternalLightMapDialog(observer: GeoPoint, redLight: Boolean, close: () -> Unit) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(ExternalLightMapState.IDLE) }
    // Keep the chosen start position for this dialog session. GPS updates must not replace a
    // WebView that AndroidView has already attached or reset the user's map interaction.
    val session = remember(context) { ExternalLightMapView(context, observer) { state = it } }
    LifecycleStartEffect(session) {
        session.load()
        onStopOrDispose { session.suspend() }
    }
    DisposableEffect(session) { onDispose { session.dispose() } }
    SkySheetTheme(redLight) {
        Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            SkySheetSystemBars(redLight)
            Surface(Modifier.fillMaxSize().drawWithContent {
                drawContent()
                if (redLight) drawRect(Color(0xFFC40000), blendMode = BlendMode.Multiply)
            }, color = MaterialTheme.colorScheme.background) {
                Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp)) {
                        Text("LightPollutionMap.app", Modifier.weight(1f).padding(vertical = 14.dp), fontWeight = FontWeight.Bold)
                        IconButton(onClick = close, modifier = Modifier.testTag("external-light-map-close")) {
                            Icon(Icons.Rounded.Close, "Externe Lichtkarte schließen")
                        }
                    }
                    FlowRow(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { session.reload() }) { Text("Neu laden") }
                        TextButton(onClick = close) { Text("Zur Astra-Karte") }
                    }
                    if (state == ExternalLightMapState.LOADING) Text("Externe Karte wird geladen …", Modifier.padding(horizontal = 16.dp))
                    if (state == ExternalLightMapState.ERROR) Text(
                        "Die externe Karte ist derzeit nicht erreichbar. Erneut laden oder die Astra-Karte verwenden.",
                        Modifier.padding(16.dp), color = MaterialTheme.colorScheme.secondary
                    )
                    AndroidView(factory = { hostContext ->
                        FrameLayout(hostContext).apply {
                            addView(session.view, FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                            ))
                        }
                    }, modifier = Modifier.fillMaxWidth().weight(1f), onRelease = { it.removeAllViews() }, update = {})
                }
            }
        }
    }
}
