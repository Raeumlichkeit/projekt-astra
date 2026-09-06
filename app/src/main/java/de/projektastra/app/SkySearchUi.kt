package de.projektastra.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SkySearchSheet(
    index: SkySearchIndex,
    redLight: Boolean,
    demoLocation: Boolean,
    terrain: TerrainProfile?,
    positionOf: (SkySearchTarget) -> HorizontalCoordinates,
    dismiss: () -> Unit,
    open: (SkySearchTarget) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val results = remember(index, query) { index.search(query, 41) }
    SkySheetTheme(redLight) {
        ModalBottomSheet(onDismissRequest = dismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            scrimColor = Color.Black.copy(alpha = 0.7f)) {
            SkySheetSystemBars(redLight)
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Objektsuche", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = dismiss) { Text("Schließen") }
                }
                OutlinedTextField(value = query, onValueChange = { query = it.take(100) },
                    label = { Text("Name oder Katalognummer") },
                    placeholder = { Text("Saturn, Andromeda, M31, HIP 91262 …") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Offline · keine Suchhistorie" + if (demoLocation) " · Standort: Berlin Demo" else "",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Text("Die Höhenlage allein garantiert keine Sichtbarkeit bei Tageslicht, Wolken oder hellem Mond."
                    + if (terrain == null) " Geländeprofil nicht verfügbar." else "",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                if (query.isBlank()) Text("Suche in Sternen, Sonnensystem, Deep Sky und Sternbildern.")
                else if (results.isEmpty()) Text("Keine Treffer im Offline-Katalog. Prüfe den Namen oder die Katalognummer.")
                else {
                    Text(if (results.size > 40) "Mehr als 40 Treffer – bitte genauer suchen." else "${results.size} Treffer",
                        fontSize = 12.sp)
                    LazyColumn(Modifier.heightIn(max = 420.dp)) {
                        items(results.take(40), key = { it.id }) { target ->
                            val position = positionOf(target)
                            Column(Modifier.fillMaxWidth().clickable { open(target) }
                                .padding(vertical = 13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(target.name, fontWeight = FontWeight.Bold)
                                Text("${target.typeLabel} · ${if (target.regionName == null) target.objectData.catalogId else "Referenzpunkt: ${target.objectData.name}"}",
                                    color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                Text("${targetVisibility(position, terrain).label} · Höhe ${String.format(Locale.GERMAN, "%.1f", position.altitude)}°",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                Text("In manueller Karte öffnen", fontSize = 12.sp)
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
