package com.crickethub.ui.match

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crickethub.data.model.MATCH_TYPES
import com.crickethub.data.model.MatchInsert
import com.crickethub.data.model.Team

private val BackgroundDark = Color(0xFF030712)
private val SurfaceCard = Color(0xFF111827)
private val BorderColor = Color(0xFF1F2937)
private val NeonGreen = Color(0xFF10B981)
private val TextPrimary = Color(0xFFF9FAFB)
private val TextSecondary = Color(0xFF9CA3AF)
private val ErrorRed = Color(0xFFEF4444)
private val AmberColor = Color(0xFFF59E0B)

@Composable
fun CreateMatchScreen(
    onBack: () -> Unit,
    onMatchCreated: (String) -> Unit,
    viewModel: MatchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentStep by remember { mutableIntStateOf(0) }
    val steps = listOf("Match Info", "Teams", "Rules", "Officials", "Review")

    // Step 1 — Match Info
    var title by remember { mutableStateOf("") }
    var selectedMatchType by remember { mutableStateOf("T20") }
    var matchDate by remember { mutableStateOf("") }
    var matchTime by remember { mutableStateOf("") }
    var venue by remember { mutableStateOf("") }
    var customOvers by remember { mutableStateOf("20") }

    // Step 2 — Teams
    var team1Id by remember { mutableStateOf("") }
    var team2Id by remember { mutableStateOf("") }
    var playersPerSide by remember { mutableStateOf("11") }

    // Step 3 — Rules
    var powerplayOvers by remember { mutableStateOf("6") }
    var powerplay2Overs by remember { mutableStateOf("0") }
    var powerplay3Overs by remember { mutableStateOf("0") }
    var maxOversPerBowler by remember { mutableStateOf("") }
    var inningsBreak by remember { mutableStateOf("20") }
    var freeHitOnNoball by remember { mutableStateOf(true) }
    var superOverEnabled by remember { mutableStateOf(false) }
    var dlsEnabled by remember { mutableStateOf(false) }
    var liveSharingEnabled by remember { mutableStateOf(false) }
    var isPublic by remember { mutableStateOf(true) }

    // Step 4 — Officials
    var umpire1 by remember { mutableStateOf("") }
    var umpire2 by remember { mutableStateOf("") }
    var thirdUmpire by remember { mutableStateOf("") }
    var matchReferee by remember { mutableStateOf("") }
    var scorerName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadTeams() }

    LaunchedEffect(uiState.matchCreated) {
        if (uiState.matchCreated) {
            uiState.currentMatch?.let { onMatchCreated(it.id) }
            viewModel.resetMatchCreated()
        }
    }

    val totalOvers = when (selectedMatchType) {
        "T5" -> 5; "T10" -> 10; "T20" -> 20; "ODI" -> 50; "Test" -> 90
        "Custom" -> customOvers.toIntOrNull() ?: 20
        else -> 20
    }

    val defaultMaxOversPerBowler = totalOvers / 5

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
        focusedBorderColor = NeonGreen, unfocusedBorderColor = BorderColor,
        cursorColor = NeonGreen, focusedLabelColor = NeonGreen, unfocusedLabelColor = TextSecondary,
        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
    )

    Column(
        modifier = Modifier.fillMaxSize().background(BackgroundDark)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text("New Match", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
        }

        // Step indicators
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            steps.forEachIndexed { index, step ->
                val isActive = index == currentStep
                val isDone = index < currentStep
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                when {
                                    isDone -> NeonGreen
                                    isActive -> NeonGreen.copy(alpha = 0.5f)
                                    else -> BorderColor
                                }
                            )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        step,
                        color = if (isActive) NeonGreen else TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step content
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            when (currentStep) {

                // ── STEP 0: Match Info ──────────────────────
                0 -> {
                    item {
                        Text("Match Information", color = NeonGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    item {
                        OutlinedTextField(
                            value = title, onValueChange = { title = it },
                            label = { Text("Match Title (optional)") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth(), colors = fieldColors
                        )
                    }
                    item {
                        Text("Match Type", color = TextSecondary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MATCH_TYPES.take(3).forEach { type ->
                                MatchTypeChip(type, selectedMatchType) { selectedMatchType = it }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MATCH_TYPES.drop(3).forEach { type ->
                                MatchTypeChip(type, selectedMatchType) { selectedMatchType = it }
                            }
                        }
                    }
                    if (selectedMatchType == "Custom") {
                        item {
                            OutlinedTextField(
                                value = customOvers,
                                onValueChange = { if (it.all { c -> c.isDigit() }) customOvers = it },
                                label = { Text("Custom Overs") }, singleLine = true,
                                modifier = Modifier.fillMaxWidth(), colors = fieldColors
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = matchDate, onValueChange = { matchDate = it },
                                label = { Text("Date (YYYY-MM-DD)") }, singleLine = true,
                                modifier = Modifier.weight(1f), colors = fieldColors
                            )
                            OutlinedTextField(
                                value = matchTime, onValueChange = { matchTime = it },
                                label = { Text("Time (HH:MM)") }, singleLine = true,
                                modifier = Modifier.weight(1f), colors = fieldColors
                            )
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = venue, onValueChange = { venue = it },
                            label = { Text("Venue / Ground") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth(), colors = fieldColors
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = playersPerSide,
                            onValueChange = { if (it.all { c -> c.isDigit() } && (it.toIntOrNull() ?: 0) <= 15) playersPerSide = it },
                            label = { Text("Players per Side") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth(), colors = fieldColors
                        )
                    }
                }

                // ── STEP 1: Teams ───────────────────────────
                1 -> {
                    item {
                        Text("Select Teams", color = NeonGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    item {
                        Text("Team A", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    items(uiState.teams) { team ->
                        TeamSelectCard(
                            team = team,
                            isSelected = team1Id == team.id,
                            isDisabled = team2Id == team.id,
                            onClick = { team1Id = if (team1Id == team.id) "" else team.id }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Team B", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    items(uiState.teams) { team ->
                        TeamSelectCard(
                            team = team,
                            isSelected = team2Id == team.id,
                            isDisabled = team1Id == team.id,
                            onClick = { team2Id = if (team2Id == team.id) "" else team.id }
                        )
                    }
                }

                // ── STEP 2: Rules ───────────────────────────
                2 -> {
                    item {
                        Text("Match Rules", color = NeonGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    item {
                        InfoCard("Total Overs: $totalOvers  •  Default Max/Bowler: $defaultMaxOversPerBowler")
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = powerplayOvers,
                                onValueChange = { if (it.all { c -> c.isDigit() }) powerplayOvers = it },
                                label = { Text("Powerplay 1 (PP)") }, singleLine = true,
                                modifier = Modifier.weight(1f), colors = fieldColors
                            )
                            OutlinedTextField(
                                value = powerplay2Overs,
                                onValueChange = { if (it.all { c -> c.isDigit() }) powerplay2Overs = it },
                                label = { Text("Middle (P2)") }, singleLine = true,
                                modifier = Modifier.weight(1f), colors = fieldColors
                            )
                            OutlinedTextField(
                                value = powerplay3Overs,
                                onValueChange = { if (it.all { c -> c.isDigit() }) powerplay3Overs = it },
                                label = { Text("Death (P3)") }, singleLine = true,
                                modifier = Modifier.weight(1f), colors = fieldColors
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = maxOversPerBowler,
                                onValueChange = { if (it.all { c -> c.isDigit() }) maxOversPerBowler = it },
                                label = { Text("Max Overs/Bowler (default: $defaultMaxOversPerBowler)") },
                                singleLine = true, modifier = Modifier.weight(1f), colors = fieldColors
                            )
                            OutlinedTextField(
                                value = inningsBreak,
                                onValueChange = { if (it.all { c -> c.isDigit() }) inningsBreak = it },
                                label = { Text("Innings Break (min)") }, singleLine = true,
                                modifier = Modifier.weight(1f), colors = fieldColors
                            )
                        }
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ToggleRow("Free Hit on No Ball", freeHitOnNoball) { freeHitOnNoball = it }
                            ToggleRow("Super Over Enabled", superOverEnabled) { superOverEnabled = it }
                            ToggleRow("DLS / Target Revision", dlsEnabled) { dlsEnabled = it }
                            ToggleRow("Live Score Sharing", liveSharingEnabled) { liveSharingEnabled = it }
                            ToggleRow("Public Match", isPublic) { isPublic = it }
                        }
                    }
                }

                // ── STEP 3: Officials ───────────────────────
                3 -> {
                    item {
                        Text("Match Officials", color = NeonGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("All fields are optional", color = TextSecondary, fontSize = 12.sp)
                    }
                    item {
                        OutlinedTextField(
                            value = umpire1, onValueChange = { umpire1 = it },
                            label = { Text("Umpire 1") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth(), colors = fieldColors
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = umpire2, onValueChange = { umpire2 = it },
                            label = { Text("Umpire 2") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth(), colors = fieldColors
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = thirdUmpire, onValueChange = { thirdUmpire = it },
                            label = { Text("Third Umpire") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth(), colors = fieldColors
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = matchReferee, onValueChange = { matchReferee = it },
                            label = { Text("Match Referee") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth(), colors = fieldColors
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = scorerName, onValueChange = { scorerName = it },
                            label = { Text("Scorer Name") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth(), colors = fieldColors
                        )
                    }
                }

                // ── STEP 4: Review ──────────────────────────
                4 -> {
                    item {
                        Text("Match Summary", color = NeonGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    item {
                        ReviewCard {
                            ReviewRow("Type", selectedMatchType)
                            ReviewRow("Overs", totalOvers.toString())
                            if (title.isNotBlank()) ReviewRow("Title", title)
                            if (venue.isNotBlank()) ReviewRow("Venue", venue)
                            if (matchDate.isNotBlank()) ReviewRow("Date", matchDate)
                            ReviewRow("Players/Side", playersPerSide)
                        }
                    }
                    item {
                        val t1 = uiState.teams.find { it.id == team1Id }
                        val t2 = uiState.teams.find { it.id == team2Id }
                        ReviewCard {
                            ReviewRow("Team A", t1?.name ?: "Not selected")
                            ReviewRow("Team B", t2?.name ?: "Not selected")
                        }
                    }
                    item {
                        ReviewCard {
                            ReviewRow("Powerplay", "${powerplayOvers} ov (PP)")
                            if (powerplay2Overs != "0") ReviewRow("Middle", "${powerplay2Overs} ov (P2)")
                            if (powerplay3Overs != "0") ReviewRow("Death", "${powerplay3Overs} ov (P3)")
                            ReviewRow("Max Overs/Bowler", maxOversPerBowler.ifBlank { defaultMaxOversPerBowler.toString() })
                            ReviewRow("Free Hit", if (freeHitOnNoball) "Yes" else "No")
                            ReviewRow("Super Over", if (superOverEnabled) "Yes" else "No")
                        }
                    }
                    if (umpire1.isNotBlank() || umpire2.isNotBlank()) {
                        item {
                            ReviewCard {
                                if (umpire1.isNotBlank()) ReviewRow("Umpire 1", umpire1)
                                if (umpire2.isNotBlank()) ReviewRow("Umpire 2", umpire2)
                                if (thirdUmpire.isNotBlank()) ReviewRow("3rd Umpire", thirdUmpire)
                                if (matchReferee.isNotBlank()) ReviewRow("Referee", matchReferee)
                            }
                        }
                    }
                    if (uiState.error != null) {
                        item {
                            Text(uiState.error ?: "", color = ErrorRed, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (currentStep > 0) {
                OutlinedButton(
                    onClick = { currentStep-- },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) { Text("Back") }
            }

            Button(
                onClick = {
                    if (currentStep < steps.size - 1) {
                        currentStep++
                    } else {
                        // Create match
                        viewModel.createMatch(MatchInsert(
                            title = title.trim().ifBlank { null },
                            matchType = selectedMatchType,
                            matchDate = matchDate.trim().ifBlank { null },
                            matchTime = matchTime.trim().ifBlank { null },
                            venue = venue.trim().ifBlank { null },
                            team1Id = team1Id,
                            team2Id = team2Id,
                            playersPerSide = playersPerSide.toIntOrNull() ?: 11,
                            totalOvers = totalOvers,
                            powerplayOvers = powerplayOvers.toIntOrNull() ?: 6,
                            powerplay2Overs = powerplay2Overs.toIntOrNull() ?: 0,
                            powerplay3Overs = powerplay3Overs.toIntOrNull() ?: 0,
                            inningsBreakMinutes = inningsBreak.toIntOrNull() ?: 20,
                            superOverEnabled = superOverEnabled,
                            freeHitOnNoball = freeHitOnNoball,
                            dlsEnabled = dlsEnabled,
                            maxOversPerBowler = maxOversPerBowler.toIntOrNull() ?: defaultMaxOversPerBowler,
                            umpire1 = umpire1.trim().ifBlank { null },
                            umpire2 = umpire2.trim().ifBlank { null },
                            thirdUmpire = thirdUmpire.trim().ifBlank { null },
                            matchReferee = matchReferee.trim().ifBlank { null },
                            scorerName = scorerName.trim().ifBlank { null },
                            liveSharingEnabled = liveSharingEnabled,
                            isPublic = isPublic
                        ))
                    }
                },
                enabled = when (currentStep) {
                    1 -> team1Id.isNotBlank() && team2Id.isNotBlank()
                    else -> true
                } && !uiState.isLoading,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        if (currentStep == steps.size - 1) "Create Match" else "Next",
                        color = Color.Black, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── Helper Composables ──────────────────────────────────────

@Composable
fun MatchTypeChip(type: String, selected: String, onClick: (String) -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected == type) NeonGreen.copy(alpha = 0.2f) else SurfaceCard)
            .border(1.dp, if (selected == type) NeonGreen else BorderColor, RoundedCornerShape(8.dp))
            .clickable { onClick(type) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(type, color = if (selected == type) NeonGreen else TextSecondary, fontSize = 13.sp, fontWeight = if (selected == type) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun TeamSelectCard(team: Team, isSelected: Boolean, isDisabled: Boolean, onClick: () -> Unit) {
    val jerseyColor = try {
        Color(android.graphics.Color.parseColor(team.jerseyColor ?: "#10B981"))
    } catch (e: Exception) { NeonGreen }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) NeonGreen.copy(alpha = 0.1f) else SurfaceCard)
            .border(1.dp, if (isSelected) NeonGreen else if (isDisabled) BorderColor.copy(alpha = 0.3f) else BorderColor, RoundedCornerShape(10.dp))
            .then(if (!isDisabled) Modifier.clickable { onClick() } else Modifier)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(androidx.compose.foundation.shape.CircleShape).background(jerseyColor.copy(alpha = if (isDisabled) 0.05f else 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(team.shortName?.take(3) ?: team.name.take(2).uppercase(), color = if (isDisabled) TextSecondary else jerseyColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(team.name, color = if (isDisabled) TextSecondary else TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            team.category?.let { Text(it, color = TextSecondary, fontSize = 11.sp) }
        }
        if (isSelected) {
            Text("✓", color = NeonGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        if (isDisabled) {
            Text("Used", color = TextSecondary, fontSize = 11.sp)
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
fun ToggleRow(label: String, value: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceCard).padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextPrimary, fontSize = 14.sp)
        Switch(
            checked = value, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = NeonGreen, uncheckedTrackColor = BorderColor)
        )
    }
}

@Composable
fun InfoCard(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(NeonGreen.copy(alpha = 0.1f)).border(1.dp, NeonGreen, RoundedCornerShape(8.dp)).padding(12.dp)
    ) {
        Text(text, color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ReviewCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceCard).border(1.dp, BorderColor, RoundedCornerShape(10.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content
    )
}

@Composable
fun ReviewRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}