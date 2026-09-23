package de.projektastra.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.util.Locale
import java.util.UUID

private data class OpticsColors(
    val nightBlue: Color,
    val night: Color,
    val astraSurface: Color,
    val astraSurfaceHigh: Color,
    val astraBlue: Color,
    val starGold: Color,
    val astraTextMuted: Color,
    val astraOutline: Color
)

private val LocalOpticsColors = androidx.compose.runtime.staticCompositionLocalOf {
    OpticsColors(
        nightBlue = Color(0xFF0D1C34),
        night = Color(0xFF07101F),
        astraSurface = Color(0xFF10243F),
        astraSurfaceHigh = Color(0xFF163252),
        astraBlue = Color(0xFF6DA8FF),
        starGold = Color(0xFFFFD98A),
        astraTextMuted = Color(0xFFAAB8CE),
        astraOutline = Color(0xFF294466)
    )
}

private val NightBlue: Color @Composable get() = LocalOpticsColors.current.nightBlue
private val Night: Color @Composable get() = LocalOpticsColors.current.night
private val AstraSurface: Color @Composable get() = LocalOpticsColors.current.astraSurface
private val AstraSurfaceHigh: Color @Composable get() = LocalOpticsColors.current.astraSurfaceHigh
private val AstraBlue: Color @Composable get() = LocalOpticsColors.current.astraBlue
private val StarGold: Color @Composable get() = LocalOpticsColors.current.starGold
private val AstraTextMuted: Color @Composable get() = LocalOpticsColors.current.astraTextMuted
private val AstraOutline: Color @Composable get() = LocalOpticsColors.current.astraOutline

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun OpticsFovSheet(
    settings: OpticsSettings,
    profiles: List<OpticsProfile>,
    onUpdateSettings: (OpticsSettings) -> Unit,
    onSaveProfile: (OpticsProfile) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onDismiss: () -> Unit,
    redLightMode: Boolean = false,
    oledMode: Boolean = false
) {
    val colors = remember(redLightMode, oledMode) {
        if (redLightMode) {
            OpticsColors(
                nightBlue = Color(0xFF1A0000),
                night = Color(0xFF100000),
                astraSurface = Color(0xFF2A0202),
                astraSurfaceHigh = Color(0xFF3B0505),
                astraBlue = Color(0xFFFF5252),
                starGold = Color(0xFFFF8A80),
                astraTextMuted = Color(0xFFCC6666),
                astraOutline = Color(0xFF551111)
            )
        } else if (oledMode) {
            OpticsColors(
                nightBlue = Color.Black,
                night = Color.Black,
                astraSurface = Color.Black,
                astraSurfaceHigh = Color.Black,
                astraBlue = Color(0xFF6DA8FF),
                starGold = Color(0xFFFFD98A),
                astraTextMuted = Color(0xFFAAB8CE),
                astraOutline = Color(0xFF1E1E1E)
            )
        } else {
            OpticsColors(
                nightBlue = Color(0xFF0D1C34),
                night = Color(0xFF07101F),
                astraSurface = Color(0xFF10243F),
                astraSurfaceHigh = Color(0xFF163252),
                astraBlue = Color(0xFF6DA8FF),
                starGold = Color(0xFFFFD98A),
                astraTextMuted = Color(0xFFAAB8CE),
                astraOutline = Color(0xFF294466)
            )
        }
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalOpticsColors provides colors) {
        var showNewProfileDialog by remember { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = NightBlue
        ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Optik & Sichtfeld", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Sichtfeld-Kreis, Geräteprofile und Ausrichtung", fontSize = 12.sp, color = AstraTextMuted)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, "Schließen", tint = Color.White)
                }
            }

            // 1. Sichtfeld-Kreis (FOV Overlay)
            Card(colors = CardDefaults.cardColors(containerColor = AstraSurface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Sichtfeld-Kreis (FOV)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            Text("Zeigt das wahre Gesichtsfeld im Zentrum der Sternkarte", fontSize = 11.sp, color = AstraTextMuted)
                        }
                        Switch(
                            checked = settings.fovCircleEnabled,
                            onCheckedChange = { onUpdateSettings(settings.copy(fovCircleEnabled = it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AstraBlue,
                                checkedTrackColor = AstraSurfaceHigh
                            )
                        )
                    }

                    if (settings.fovCircleEnabled) {
                        HorizontalDivider(color = AstraOutline.copy(alpha = 0.5f))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Sucher-Modus:", fontSize = 13.sp, color = Color.White)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = !settings.telradMode,
                                    onClick = { onUpdateSettings(settings.copy(telradMode = false)) },
                                    label = { Text("Einzelkreis", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AstraBlue,
                                        selectedLabelColor = Night
                                    )
                                )
                                FilterChip(
                                    selected = settings.telradMode,
                                    onClick = { onUpdateSettings(settings.copy(telradMode = true)) },
                                    label = { Text("Telrad (0.5°/2°/4°)", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = StarGold,
                                        selectedLabelColor = Night
                                    )
                                )
                            }
                        }

                        if (!settings.telradMode) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Durchmesser:", fontSize = 13.sp, color = Color.White)
                                Text(
                                    "${String.format(Locale.GERMANY, "%.2f", settings.currentFovDegrees)}° (${(settings.currentFovDegrees * 60).toInt()}′)",
                                    fontWeight = FontWeight.Bold,
                                    color = StarGold,
                                    fontSize = 14.sp
                                )
                            }

                            Slider(
                                value = settings.currentFovDegrees.toFloat(),
                                onValueChange = { onUpdateSettings(settings.copy(currentFovDegrees = it.toDouble())) },
                                valueRange = 0.2f..10.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = StarGold,
                                    activeTrackColor = StarGold,
                                    inactiveTrackColor = AstraOutline
                                )
                            )

                            // Quick FOV Presets
                            Text("Schnellauswahl:", fontSize = 11.sp, color = AstraTextMuted)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                val presets = listOf(
                                    0.5 to "0.5° (Mond / 100x)",
                                    1.0 to "1.0° (Dobson 50x)",
                                    2.0 to "2.0° (Übersicht)",
                                    6.5 to "6.5° (10x50)",
                                    7.5 to "7.5° (8x42)"
                                )
                                presets.forEach { (fov, label) ->
                                    val isSelected = kotlin.math.abs(settings.currentFovDegrees - fov) < 0.05
                                    OutlinedButton(
                                        onClick = { onUpdateSettings(settings.copy(currentFovDegrees = fov)) },
                                        border = BorderStroke(1.dp, if (isSelected) StarGold else AstraOutline),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isSelected) AstraSurfaceHigh else Color.Transparent,
                                            contentColor = if (isSelected) StarGold else Color.White
                                        )
                                    ) {
                                        Text(label, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Geräteprofile (Teleskope & Okulare)
            Card(colors = CardDefaults.cardColors(containerColor = AstraSurface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Geräteprofile", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        TextButton(onClick = { showNewProfileDialog = true }) {
                            Icon(Icons.Rounded.Add, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Profil anlegen", fontSize = 12.sp)
                        }
                    }

                    Text(
                        "Wähle ein Profil, um das Sichtfeld automatisch nach Brennweite und Okular einzustellen.",
                        fontSize = 11.sp,
                        color = AstraTextMuted
                    )

                    profiles.forEach { profile ->
                        val isActive = settings.activeProfileId == profile.id
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isActive) AstraSurfaceHigh else NightBlue.copy(alpha = 0.8f)
                            ),
                            border = BorderStroke(1.dp, if (isActive) StarGold else AstraOutline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newFov = profile.effectiveFovDegrees
                                    onUpdateSettings(
                                        settings.copy(
                                            activeProfileId = profile.id,
                                            currentFovDegrees = newFov,
                                            fovCircleEnabled = true,
                                            telradMode = false
                                        )
                                    )
                                }
                        ) {
                            Row(
                                Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(profile.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                    val infoParts = buildList {
                                        if (profile.magnification != null) add("${profile.magnification!!.toInt()}× Vergrößerung")
                                        add("Sichtfeld ≈ ${String.format(Locale.GERMANY, "%.2f", profile.effectiveFovDegrees)}°")
                                        if (profile.exitPupilMm != null) add("AP ${String.format(Locale.GERMANY, "%.1f", profile.exitPupilMm)} mm")
                                    }
                                    Text(infoParts.joinToString(" · "), fontSize = 11.sp, color = AstraBlue)
                                }
                                if (profile.id !in OpticsStore.DEFAULT_PROFILES.map { it.id }) {
                                    IconButton(onClick = { onDeleteProfile(profile.id) }, Modifier.size(28.dp)) {
                                        Icon(Icons.Rounded.Delete, "Löschen", tint = Color(0xFFFF8B8B), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        "Berechnete Werte sind Näherungen nach Standard-Formel (TFOV ≈ AFOV / V).",
                        fontSize = 10.sp,
                        color = AstraTextMuted
                    )
                }
            }

            // 3. Ausrichtung & Optik (Spiegelung & Drehung)
            Card(colors = CardDefaults.cardColors(containerColor = AstraSurface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Karten-Ausrichtung", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            Text("Passe die Ansicht an dein Instrument an", fontSize = 11.sp, color = AstraTextMuted)
                        }
                        if (settings.isCustomized) {
                            TextButton(onClick = {
                                onUpdateSettings(settings.copy(mirrored = false, rotationDegrees = 0f))
                            }) {
                                Icon(Icons.Rounded.RestartAlt, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Zurücksetzen", fontSize = 12.sp)
                            }
                        }
                    }

                    // Horizontale Spiegelung (Zenitspiegel)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Horizontal spiegeln (Zenitspiegel)", fontSize = 13.sp, color = Color.White)
                            Text("Für Refraktoren und SC/Maksutov mit Zenitspiegel", fontSize = 11.sp, color = AstraTextMuted)
                        }
                        Switch(
                            checked = settings.mirrored,
                            onCheckedChange = { onUpdateSettings(settings.copy(mirrored = it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AstraBlue,
                                checkedTrackColor = AstraSurfaceHigh
                            )
                        )
                    }

                    HorizontalDivider(color = AstraOutline.copy(alpha = 0.5f))

                    // Drehung
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Drehung der Ansicht:", fontSize = 13.sp, color = Color.White)
                            Text("${settings.rotationDegrees.toInt()}°", fontWeight = FontWeight.Bold, color = StarGold, fontSize = 13.sp)
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val rotPresets = listOf(
                                0f to "0° (Normal)",
                                90f to "90°",
                                180f to "180° (Newton / Invertiert)",
                                270f to "270°"
                            )
                            rotPresets.forEach { (rot, label) ->
                                val isSelected = (settings.rotationDegrees % 360f) == rot
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onUpdateSettings(settings.copy(rotationDegrees = rot)) },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AstraBlue,
                                        selectedLabelColor = Night
                                    )
                                )
                            }
                        }
                    }

                    // Schalter für aufrechte Beschriftungen
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Beschriftungen aufrecht halten", fontSize = 13.sp, color = Color.White)
                            Text("Namen lesbar halten, auch wenn die Karte gedreht ist", fontSize = 11.sp, color = AstraTextMuted)
                        }
                        Switch(
                            checked = !settings.rotateLabels,
                            onCheckedChange = { onUpdateSettings(settings.copy(rotateLabels = !it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AstraBlue,
                                checkedTrackColor = AstraSurfaceHigh
                            )
                        )
                    }

                    Text(
                        "Hinweis: AR (Kameramodus) bleibt von diesen optischen Transformationen unbeeinflusst.",
                        fontSize = 11.sp,
                        color = AstraTextMuted
                    )
                }
            }
        }
    }

    if (showNewProfileDialog) {
        NewOpticsProfileDialog(
            onDismiss = { showNewProfileDialog = false },
            onSave = { newProfile ->
                onSaveProfile(newProfile)
                showNewProfileDialog = false
            }
        )
    }
    }
}

@Composable
private fun NewOpticsProfileDialog(
    onDismiss: () -> Unit,
    onSave: (OpticsProfile) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var isBinocular by remember { mutableStateOf(false) }
    var telescopeFocalLengthStr by remember { mutableStateOf("1000") }
    var telescopeApertureStr by remember { mutableStateOf("150") }
    var eyepieceFocalLengthStr by remember { mutableStateOf("25") }
    var eyepieceAfovStr by remember { mutableStateOf("52") }
    var customFovStr by remember { mutableStateOf("6.5") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = NightBlue),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Neues Optik-Profil", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profilname (z. B. Mein Teleskop)") },
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isBinocular,
                        onClick = { isBinocular = false },
                        label = { Text("Teleskop + Okular") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AstraBlue,
                            selectedLabelColor = Night
                        )
                    )
                    FilterChip(
                        selected = isBinocular,
                        onClick = { isBinocular = true },
                        label = { Text("Fernglas") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AstraBlue,
                            selectedLabelColor = Night
                        )
                    )
                }

                if (!isBinocular) {
                    OutlinedTextField(
                        value = telescopeFocalLengthStr,
                        onValueChange = { telescopeFocalLengthStr = it },
                        label = { Text("Teleskop-Brennweite (mm)") },
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
                        value = telescopeApertureStr,
                        onValueChange = { telescopeApertureStr = it },
                        label = { Text("Öffnung / Spiegeldurchmesser (mm)") },
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
                        value = eyepieceFocalLengthStr,
                        onValueChange = { eyepieceFocalLengthStr = it },
                        label = { Text("Okular-Brennweite (mm)") },
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
                        value = eyepieceAfovStr,
                        onValueChange = { eyepieceAfovStr = it },
                        label = { Text("Scheinbares Gesichtsfeld Okular (AFOV in °)") },
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
                } else {
                    OutlinedTextField(
                        value = customFovStr,
                        onValueChange = { customFovStr = it },
                        label = { Text("Wahres Gesichtsfeld Fernglas (°)") },
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
                }

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
                            val profile = if (!isBinocular) {
                                val tf = telescopeFocalLengthStr.toDoubleOrNull() ?: 1000.0
                                val ap = telescopeApertureStr.toDoubleOrNull() ?: 150.0
                                val ef = eyepieceFocalLengthStr.toDoubleOrNull() ?: 25.0
                                val af = eyepieceAfovStr.toDoubleOrNull() ?: 52.0
                                OpticsProfile(
                                    name = name.ifBlank { "Teleskop ${tf.toInt()}mm / ${ef.toInt()}mm" },
                                    isBinocular = false,
                                    telescopeFocalLengthMm = tf,
                                    telescopeApertureMm = ap,
                                    eyepieceFocalLengthMm = ef,
                                    eyepieceAfovDegrees = af
                                )
                            } else {
                                val fov = customFovStr.toDoubleOrNull() ?: 6.5
                                OpticsProfile(
                                    name = name.ifBlank { "Fernglas ${fov}°" },
                                    isBinocular = true,
                                    customFovDegrees = fov
                                )
                            }
                            onSave(profile)
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
