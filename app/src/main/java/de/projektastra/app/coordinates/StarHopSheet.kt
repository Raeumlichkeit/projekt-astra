package de.projektastra.app.coordinates

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AltRoute
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Star
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AstraBlue = Color(0xFF6DA8FF)
private val StarGold = Color(0xFFFFD98A)
private val RedAccent = Color(0xFFFF5252)
private val DarkCard = Color(0xFF131A26)
private val DarkCardBorder = Color(0x334A688F)

/**
 * Bottom Sheet providing catalog browsing and active waypoint navigation for star-hopping.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarHopSheet(
    session: StarHopSessionState,
    onSessionChange: (StarHopSessionState) -> Unit,
    onCenterWaypoint: (EquatorialCoordinates, Double) -> Unit,
    onDismissRequest: () -> Unit,
    onAddLogEntry: ((String) -> Unit)? = null,
    redLightMode: Boolean = false
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = if (redLightMode) Color(0xFF140000) else Color(0xFF0C1322)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val route = session.activeRoute
            if (route == null) {
                RouteBrowserView(
                    onSelectRoute = { selected ->
                        onSessionChange(
                            StarHopSessionState(
                                activeRoute = selected,
                                activeStepIndex = 0,
                                reticleMode = session.reticleMode
                            )
                        )
                    },
                    redLightMode = redLightMode
                )
            } else {
                ActiveRouteDetailView(
                    route = route,
                    session = session,
                    onSessionChange = onSessionChange,
                    onCenterWaypoint = onCenterWaypoint,
                    onSwitchRoute = { onSessionChange(session.copy(activeRoute = null)) },
                    onAddLogEntry = onAddLogEntry,
                    redLightMode = redLightMode
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Route catalog browser view allowing search and filtering of curated star-hops.
 */
@Composable
private fun RouteBrowserView(
    onSelectRoute: (StarHopRoute) -> Unit,
    redLightMode: Boolean
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf<StarHopDifficulty?>(null) }

    val allRoutes = remember { StarHopCatalog.getAllRoutes() }
    val filteredRoutes = remember(searchQuery, selectedDifficulty) {
        allRoutes.filter { route ->
            val matchesDifficulty = selectedDifficulty == null || route.difficulty == selectedDifficulty
            val matchesSearch = searchQuery.isBlank() ||
                route.targetName.contains(searchQuery, ignoreCase = true) ||
                route.targetCatalogId.contains(searchQuery, ignoreCase = true) ||
                route.guideStar.contains(searchQuery, ignoreCase = true) ||
                route.constellation.contains(searchQuery, ignoreCase = true)
            matchesDifficulty && matchesSearch
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Rounded.AltRoute,
                contentDescription = null,
                tint = if (redLightMode) RedAccent else StarGold,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Star-Hopping Routen",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (redLightMode) RedAccent else Color.White
            )
        }

        Text(
            "Schrittweise Aufsuchhilfen von hellen Leitsternen zu lichtschwachen Deep-Sky-Objekten.",
            style = MaterialTheme.typography.bodySmall,
            color = if (redLightMode) Color(0xFFFF8888) else Color(0xFFB0C4DE)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Ziel, Leitstern oder Sternbild suchen...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Difficulty filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedDifficulty == null,
                onClick = { selectedDifficulty = null },
                label = { Text("Alle (${allRoutes.size})") }
            )
            StarHopDifficulty.entries.forEach { diff ->
                FilterChip(
                    selected = selectedDifficulty == diff,
                    onClick = { selectedDifficulty = if (selectedDifficulty == diff) null else diff },
                    label = { Text(diff.label) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.heightIn(max = 480.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredRoutes, key = { it.id }) { r ->
                RouteCard(route = r, onSelect = { onSelectRoute(r) }, redLightMode = redLightMode)
            }
        }
    }
}

/**
 * Individual card representation for a star-hop route in browser mode.
 */
@Composable
private fun RouteCard(
    route: StarHopRoute,
    onSelect: () -> Unit,
    redLightMode: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (redLightMode) Color(0xFF220000) else DarkCard
        ),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (redLightMode) Color(0xFF551111) else DarkCardBorder
            )
        )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    route.targetName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (redLightMode) RedAccent else Color.White
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (redLightMode) Color(0xFF440000) else Color(0xFF1E293B)
                ) {
                    Text(
                        route.difficulty.label,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (redLightMode) RedAccent else AstraBlue
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Star,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (redLightMode) RedAccent else StarGold
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Start: ${route.guideStar} · ${route.constellation}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (redLightMode) Color(0xFFFF8888) else Color(0xFF94A3B8)
                )
            }

            Text(
                route.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (redLightMode) Color(0xFFFFAAAA) else Color(0xFFCBD5E1)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${route.steps.size} Hopps · ~${route.estimatedHops * 2} Min",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (redLightMode) Color(0xFF882222) else Color(0xFF64748B)
                )

                Button(
                    onClick = onSelect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (redLightMode) RedAccent else AstraBlue,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = ButtonDefaults.TextButtonContentPadding
                ) {
                    Text("Route starten", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Detailed active route checklist view with interactive step completion and FOV centering.
 */
@Composable
private fun ActiveRouteDetailView(
    route: StarHopRoute,
    session: StarHopSessionState,
    onSessionChange: (StarHopSessionState) -> Unit,
    onCenterWaypoint: (EquatorialCoordinates, Double) -> Unit,
    onSwitchRoute: () -> Unit,
    onAddLogEntry: ((String) -> Unit)?,
    redLightMode: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    route.targetName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (redLightMode) RedAccent else Color.White
                )
                Text(
                    "Start: ${route.guideStar} · ${route.constellation}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (redLightMode) Color(0xFFFF8888) else AstraBlue
                )
            }

            OutlinedButton(
                onClick = onSwitchRoute,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Wechseln", fontSize = 12.sp)
            }
        }

        // Progress bar
        val progress = if (route.steps.isNotEmpty()) {
            route.completedStepsCount.toFloat() / route.steps.size.toFloat()
        } else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (redLightMode) RedAccent else StarGold,
            trackColor = if (redLightMode) Color(0xFF330000) else Color(0xFF1E293B)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Fortschritt: ${route.completedStepsCount} von ${route.steps.size} Hopps erledigt",
                style = MaterialTheme.typography.labelSmall,
                color = if (redLightMode) Color(0xFFFF8888) else Color(0xFF94A3B8)
            )
            Text(
                "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (redLightMode) RedAccent else StarGold
            )
        }

        // Reticle selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Fadenkreuz:", style = MaterialTheme.typography.labelSmall)
            StarHopReticleMode.entries.forEach { mode ->
                FilterChip(
                    selected = session.reticleMode == mode,
                    onClick = { onSessionChange(session.copy(reticleMode = mode)) },
                    label = { Text(mode.label, fontSize = 11.sp) }
                )
            }
        }

        HorizontalDivider(color = if (redLightMode) Color(0xFF330000) else DarkCardBorder)

        // Waypoint checklist
        LazyColumn(
            modifier = Modifier.heightIn(max = 380.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(route.steps, key = { it.stepIndex }) { step ->
                StepChecklistItem(
                    step = step,
                    isActive = session.activeStepIndex == step.stepIndex,
                    isCompleted = step.isCompleted,
                    onSelect = { onSessionChange(session.jumpToStep(step.stepIndex)) },
                    onToggleComplete = { onSessionChange(session.toggleStepCompleted(step.stepIndex)) },
                    onCenter = { onCenterWaypoint(step.fieldCenterCoords, step.recommendedFovDegrees) },
                    redLightMode = redLightMode
                )
            }
        }

        // Celebration card when completed
        if (session.isFinished) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (redLightMode) Color(0xFF330000) else Color(0xFF1E3A2F)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = if (redLightMode) RedAccent else Color(0xFF4ADE80)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Zielobjekt erreicht! Glückwunsch!",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        "Du hast den Star-Hop zu ${route.targetName} erfolgreich abgeschlossen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD1FAE5)
                    )
                    if (onAddLogEntry != null) {
                        Button(
                            onClick = { onAddLogEntry(route.targetCatalogId) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (redLightMode) RedAccent else StarGold,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Rounded.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Im Logbuch beobachten")
                        }
                    }
                }
            }
        }
    }
}

/**
 * An individual checkable waypoint item in the active route checklist.
 */
@Composable
private fun StepChecklistItem(
    step: StarHopStep,
    isActive: Boolean,
    isCompleted: Boolean,
    onSelect: () -> Unit,
    onToggleComplete: () -> Unit,
    onCenter: () -> Unit,
    redLightMode: Boolean
) {
    var expanded by remember { mutableStateOf(isActive) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable {
                onSelect()
                expanded = !expanded
            }
            .border(
                width = if (isActive) 1.5.dp else 0.5.dp,
                color = when {
                    isActive -> if (redLightMode) RedAccent else StarGold
                    isCompleted -> if (redLightMode) Color(0xFF661111) else Color(0xFF22C55E).copy(alpha = 0.5f)
                    else -> if (redLightMode) Color(0xFF330000) else DarkCardBorder
                },
                shape = RoundedCornerShape(10.dp)
            ),
        color = when {
            isActive -> if (redLightMode) Color(0xFF330000) else Color(0xFF1E293B)
            else -> if (redLightMode) Color(0xFF180000) else Color(0xFF0F172A)
        }
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Step number / check badge
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isCompleted -> if (redLightMode) RedAccent else Color(0xFF22C55E)
                                    isActive -> if (redLightMode) RedAccent.copy(alpha = 0.3f) else StarGold.copy(alpha = 0.3f)
                                    else -> Color.Transparent
                                }
                            )
                            .border(
                                1.dp,
                                if (isCompleted) Color.Transparent else if (redLightMode) RedAccent else AstraBlue,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = "Erledigt",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                "${step.stepIndex + 1}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (redLightMode) RedAccent else Color.White
                            )
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    Text(
                        step.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (redLightMode) Color.White else Color(0xFFE2E8F0)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (redLightMode) Color(0xFF330000) else Color(0xFF334155)
                    ) {
                        Text(
                            "FOV ${step.recommendedFovDegrees}°",
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = if (redLightMode) RedAccent else StarGold
                        )
                    }
                    IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded || isActive) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        step.instruction,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (redLightMode) Color(0xFFFFAAAA) else Color(0xFF94A3B8)
                    )

                    if (step.hint != null) {
                        Text(
                            "Tipp: ${step.hint}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (redLightMode) RedAccent else StarGold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onCenter,
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = ButtonDefaults.TextButtonContentPadding
                        ) {
                            Icon(Icons.Rounded.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("In Karte zentrieren", fontSize = 11.sp)
                        }

                        Button(
                            onClick = onToggleComplete,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCompleted) Color(0xFF22C55E) else if (redLightMode) RedAccent else AstraBlue,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = ButtonDefaults.TextButtonContentPadding
                        ) {
                            Text(if (isCompleted) "✓ Erledigt" else "Als erledigt abhaken", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Floating HUD banner on the sky map for hands-free navigation between star-hop waypoints.
 */
@Composable
fun StarHopHud(
    session: StarHopSessionState,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit,
    onCenterWaypoint: (EquatorialCoordinates, Double) -> Unit,
    onOpenSheet: () -> Unit,
    onDismissSession: () -> Unit,
    redLightMode: Boolean,
    modifier: Modifier = Modifier
) {
    val route = session.activeRoute ?: return
    val currentStep = session.currentStep ?: return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (redLightMode) Color(0xEE220000) else Color(0xEE0C1322),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (redLightMode) Color(0xFF661111) else DarkCardBorder
            )
        ),
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Rounded.AltRoute,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (redLightMode) RedAccent else StarGold
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Hopp ${session.activeStepIndex + 1} / ${route.totalSteps}: ${route.targetName}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (redLightMode) RedAccent else Color.White
                    )
                }

                IconButton(onClick = onDismissSession, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Star-Hop beenden",
                        modifier = Modifier.size(16.dp),
                        tint = if (redLightMode) RedAccent else Color.White
                    )
                }
            }

            Text(
                currentStep.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (redLightMode) Color.White else Color(0xFFE2E8F0),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onPreviousStep,
                        enabled = session.activeStepIndex > 0,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.NavigateBefore, contentDescription = "Vorheriger Hopp")
                    }

                    OutlinedButton(
                        onClick = {
                            onCenterWaypoint(currentStep.fieldCenterCoords, currentStep.recommendedFovDegrees)
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) {
                        Icon(Icons.Rounded.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Zentrieren", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onNextStep,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (redLightMode) RedAccent else StarGold,
                            contentColor = Color.Black
                        ),
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) {
                        Text(
                            if (session.activeStepIndex >= route.totalSteps - 1) "Abschließen" else "✓ Hopp erledigt",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(2.dp))
                        Icon(Icons.AutoMirrored.Rounded.NavigateNext, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }

                TextButton(onClick = onOpenSheet) {
                    Text("Liste", fontSize = 11.sp, color = if (redLightMode) RedAccent else AstraBlue)
                }
            }
        }
    }
}
