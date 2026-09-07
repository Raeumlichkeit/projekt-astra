package de.projektastra.app

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale
import kotlin.math.abs

private val skyDateInputFormat = DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.GERMAN)
    .withResolverStyle(ResolverStyle.STRICT)
private val skyTimeInputFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN)
    .withResolverStyle(ResolverStyle.STRICT)
private val skyTimeDisplayFormat = DateTimeFormatter.ofPattern("dd.MM.uuuu · HH:mm:ss", Locale.GERMAN)

private fun timeOffsetLabel(offset: ZoneOffset): String = "UTC${if (offset == ZoneOffset.UTC) "+00:00" else offset.id}"

/** Text fields are a draft: advancing the map clock never overwrites an unfinished edit. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SkyTimeSheet(
    state: SkyTimeState,
    zone: ZoneId,
    redLight: Boolean,
    onSelect: (Instant) -> Unit,
    onRate: (Int) -> Unit,
    onPause: () -> Unit,
    onNow: () -> Unit,
    onDismiss: () -> Unit
) {
    val initialTime = remember(zone) { state.instant.atZone(zone) }
    var dateText by remember(zone) { mutableStateOf(initialTime.format(skyDateInputFormat)) }
    var timeText by remember(zone) { mutableStateOf(initialTime.format(skyTimeInputFormat)) }
    var inputError by remember { mutableStateOf<String?>(null) }
    var speed by remember { mutableStateOf(abs(state.rate).takeIf { it in listOf(1, 60, 600, 3600) } ?: 60) }
    val focusManager = LocalFocusManager.current
    val draft = remember(dateText, timeText) {
        runCatching {
            require(Regex("\\d{2}\\.\\d{2}\\.\\d{4}").matches(dateText.trim()))
            require(Regex("\\d{2}:\\d{2}").matches(timeText.trim()))
            val date = LocalDate.parse(dateText.trim(), skyDateInputFormat)
            require(date.year in 1900..2100)
            LocalDateTime.of(date, LocalTime.parse(timeText.trim(), skyTimeInputFormat))
        }.getOrNull()
    }
    val offsets = remember(draft, zone) { draft?.let { zone.rules.getValidOffsets(it) }.orEmpty() }
    var laterOffset by remember(dateText, timeText, zone) {
        mutableStateOf(offsets.size == 2 && offsets.last() == initialTime.offset &&
            draft == initialTime.toLocalDateTime().withSecond(0).withNano(0))
    }
    val selectedOffset = if (laterOffset) offsets.lastOrNull() else offsets.firstOrNull()
    val applyDraft: () -> Unit = {
        if (draft == null) {
            inputError = "Bitte ein gültiges Datum von 1900 bis 2100 (TT.MM.JJJJ) und eine Uhrzeit von 00:00 bis 23:59 (HH:mm) eingeben."
        } else if (offsets.isEmpty()) {
            inputError = "Diese Ortszeit gibt es wegen der Zeitumstellung nicht. Bitte eine frühere oder spätere Uhrzeit wählen."
        } else {
            val chosen = runCatching { resolveSkyLocalTime(draft, zone, laterOffset) }.getOrNull()
            if (chosen == null || chosen !in SkyTimeState.MIN_INSTANT..SkyTimeState.MAX_INSTANT) {
                inputError = "Diese Zeit liegt außerhalb des unterstützten Bereichs von 1900 bis 2100 (UTC)."
            } else {
                inputError = null
                focusManager.clearFocus()
                onSelect(chosen)
            }
        }
    }
    val currentTime = state.instant.atZone(zone)
    val status = when {
        state.live -> "Echtzeit"
        state.rate == 0 -> "Pausiert"
        state.rate < 0 -> "Rücklauf · ${abs(state.rate)}×"
        else -> "Vorlauf · ${state.rate}×"
    }

    SkySheetTheme(redLight) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            scrimColor = Color.Black.copy(alpha = 0.7f)
        ) {
            SkySheetSystemBars(redLight)
            Column(
                Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Zeit der Sternkarte", style = MaterialTheme.typography.titleLarge)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(currentTime.format(skyTimeDisplayFormat), style = MaterialTheme.typography.titleMedium)
                    Text("${zone.id} · ${timeOffsetLabel(currentTime.offset)}", style = MaterialTheme.typography.bodyMedium)
                    Text(status, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { stateDescription = status })
                }
                Button(
                    onClick = { focusManager.clearFocus(); onNow() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Jetzt · zurück zur Echtzeit") }

                HorizontalDivider()
                Text("Datum und Uhrzeit wählen", style = MaterialTheme.typography.titleMedium)
                Text("Ortszeit in ${zone.id} · 1900–2100", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it.take(16); inputError = null },
                        label = { Text("Datum") },
                        placeholder = { Text("TT.MM.JJJJ") },
                        singleLine = true,
                        isError = inputError != null && draft == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                        modifier = Modifier.weight(1.3f).semantics { contentDescription = "Datum, TT.MM.JJJJ" }
                    )
                    OutlinedTextField(
                        value = timeText,
                        onValueChange = { timeText = it.take(8); inputError = null },
                        label = { Text("Uhrzeit") },
                        placeholder = { Text("HH:mm") },
                        singleLine = true,
                        isError = inputError != null && (draft == null || offsets.isEmpty()),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { applyDraft() }),
                        modifier = Modifier.weight(1f).semantics { contentDescription = "Uhrzeit, HH:mm" }
                    )
                }
                if (offsets.size == 2) {
                    Text("Diese Uhrzeit kommt bei der Zeitumstellung zweimal vor. Welchen Zeitpunkt möchtest du anzeigen?",
                        style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !laterOffset, onClick = { laterOffset = false },
                            label = { Text("Früher · ${timeOffsetLabel(offsets.first())}") })
                        FilterChip(selected = laterOffset, onClick = { laterOffset = true },
                            label = { Text("Später · ${timeOffsetLabel(offsets.last())}") })
                    }
                } else if (selectedOffset != null) {
                    Text("Gewählter Zeitpunkt: ${timeOffsetLabel(selectedOffset)}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                inputError?.let { Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall) }
                OutlinedButton(onClick = applyDraft, modifier = Modifier.fillMaxWidth()) { Text("Zeit übernehmen · pausiert") }

                HorizontalDivider()
                Text("Zeit ablaufen lassen", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 60, 600, 3600).forEach { value ->
                        FilterChip(
                            selected = speed == value,
                            onClick = {
                                speed = value
                                if (!state.live && state.rate != 0) onRate(if (state.rate < 0) -value else value)
                            },
                            label = { Text("$value×") },
                            modifier = Modifier.semantics { contentDescription = "Zeitgeschwindigkeit $value-fach" }
                        )
                    }
                }
                Text(when (speed) {
                    1 -> "Eine Sekunde entspricht einer Sekunde auf der Sternkarte."
                    60 -> "Eine Sekunde entspricht einer Minute auf der Sternkarte."
                    600 -> "Eine Sekunde entspricht zehn Minuten auf der Sternkarte."
                    else -> "Eine Sekunde entspricht einer Stunde auf der Sternkarte."
                }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { focusManager.clearFocus(); onRate(-speed) }) { Text("Rückwärts") }
                    OutlinedButton(onClick = { focusManager.clearFocus(); onPause() }, enabled = state.live || state.rate != 0) { Text("Pause") }
                    OutlinedButton(onClick = { focusManager.clearFocus(); onRate(speed) }) { Text("Vorwärts") }
                }
                Text("Die Zeitsteuerung verändert die Sternkarte. Wetter und Ereignisbenachrichtigungen verwenden weiterhin die aktuelle Zeit.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Schließen") }
            }
        }
    }
}
