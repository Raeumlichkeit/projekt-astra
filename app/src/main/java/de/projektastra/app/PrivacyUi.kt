package de.projektastra.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

@Composable
internal fun OnlineConsentDialog(onChoice: (Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = { onChoice(false) },
        title = { Text("Online-Daten für deine Nacht") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Sternkarte, AR und der gespeicherte Kalender funktionieren offline.")
                Text("Für Wetter, Wolken und Karten wird dein Standort auf 0,01° gerundet (etwa 1 km). Open-Meteo und Kartenanbieter sehen diesen Bereich, deine IP-Adresse und die Anfragezeit. Open-Meteo kann solche Serverprotokolle bis zu 90 Tage aufbewahren.")
                Text("Karten stammen von OpenStreetMap, RainViewer und NASA GIBS. Kalenderupdates kommen von IMO, Objektbilder von CDS. Auch diese Anbieter sehen deine IP-Adresse. Öffentliche Grundkarten werden privat zwischengespeichert (maximal 32 MB, Gültigkeit bis 30 Tage). Abgelaufene Dateien werden beim nächsten Start oder Kartenabruf gelöscht. Diese Bilder lassen betrachtete Regionen erkennen. Wetter- und Geländeantworten bleiben im Arbeitsspeicher.")
                Text("Das genaue Geländeprofil bleibt zunächst aus und benötigt eine eigene Freigabe. Du kannst Online-Daten unter Info jederzeit abschalten; bereits übertragene Anbieterprotokolle werden dadurch nicht gelöscht.")
            }
        },
        confirmButton = { TextButton(onClick = { onChoice(true) }) { Text("Online-Daten erlauben") } },
        dismissButton = { TextButton(onClick = { onChoice(false) }) { Text("Offline bleiben") } }
    )
}

@Composable
internal fun PrivacyControls(
    options: PrivacyOptions,
    rememberLocation: Boolean,
    onRememberLocationChange: (Boolean) -> Unit,
    requestOnline: () -> Unit,
    setOptions: (PrivacyOptions) -> Unit,
    clearMaps: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Deine Daten", style = MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Standort für die Sternkarte merken", Modifier.weight(1f))
                Switch(rememberLocation, onCheckedChange = onRememberLocationChange)
            }
            Text("Speichert deinen letzten Beobachtungsort lokal auf diesem Gerät, sodass die Sternkarte sofort passend ausgerichtet startet. Kein Cloud-Backup. Bei Ausschalten wird der Ort sofort gelöscht.")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Online-Wetter und Karten", Modifier.weight(1f))
                Switch(options.online, onCheckedChange = {
                    if (it) requestOnline() else setOptions(PrivacyOptions())
                })
            }
            Text("Wetter und Karten verwenden einen auf 0,01° gerundeten Ort. Anbieter erhalten IP-Adresse und Kartenbereich; Open-Meteo bewahrt mögliche Standortprotokolle bis zu 90 Tage auf.")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Genaues Online-Geländeprofil", Modifier.weight(1f))
                Switch(options.terrain, enabled = options.online, onCheckedChange = {
                    setOptions(options.copy(terrain = it))
                })
            }
            Text("Wenn du diesen Schalter aktivierst, erhält Open-Meteo deinen genauen Standort und umliegende Geländepunkte. Auch dafür können Serverprotokolle bis zu 90 Tage bestehen. Ohne Freigabe zeigt die Karte einen flachen Horizont.")
            Text("Favoriten und Ereignisse bleiben auf diesem Gerät. Der private Grundkartencache enthält höchstens 32 MB (Gültigkeit bis 30 Tage, Bereinigung beim nächsten Start/Abruf). Er enthält keine GPS-Parameter, kann aber betrachtete Regionen erkennen lassen. Keine App-Backups.")
            Button(onClick = clearMaps) { Text("Kartencache löschen") }
        }
    }
}

@Composable
internal fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val fontScale = LocalConfiguration.current.fontScale
    val view = remember(context) {
        PrivateWebViews.create(context, javascript = false).apply {
            val html = context.assets.open("privacy-policy.html").bufferedReader().use { it.readText() }
            PrivateWebViews.loadHtml(this, html)
        }
    }
    DisposableEffect(view) { onDispose { PrivateWebViews.dispose(view) } }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(Modifier.padding(16.dp).widthIn(max = 720.dp).fillMaxWidth()
            .heightIn(max = 680.dp).fillMaxHeight(0.9f)
            .semantics { paneTitle = "Datenschutzerklärung" }) {
            Column(Modifier.fillMaxSize()) {
                // The document already includes its heading. Keep it inside the scrolling WebView
                // so large text cannot push the close action out of a short landscape window.
                AndroidView(factory = { view },
                    update = { it.settings.textZoom = (fontScale * 100).roundToInt() },
                    modifier = Modifier.fillMaxWidth().weight(1f))
                TextButton(onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End).padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Text("Schließen")
                }
            }
        }
    }
}

@Composable
internal fun OfflineNotice() {
    Text("Online-Daten sind ausgeschaltet. Unter Info kannst du Wetter, Karten und Aktualisierungen freigeben.",
        color = MaterialTheme.colorScheme.secondary)
}
