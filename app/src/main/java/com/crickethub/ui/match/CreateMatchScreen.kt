package com.crickethub.ui.match

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.crickethub.data.model.MatchInsert
import com.crickethub.data.model.Team

private val BackgroundDark = Color(0xFF030712)
private val SurfaceCard = Color(0xFF111827)
private val BorderColor = Color(0xFF1F2937)
private val NeonGreen = Color(0xFF10B981)
private val NeonBlue = Color(0xFF3B82F6)
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

    var matchTitle by remember { mutableStateOf("") }
    var selectedMatchType by remember { mutableStateOf("T20") }
    var selectedOvers by remember { mutableStateOf(20) }
    var customOvers by remember { mutableStateOf("") }
    var selectedPlayers by remember { mutableStateOf(11) }
    var venue by remember { mutableStateOf("") }
    var matchDate by remember { mutableStateOf("") }
    var matchTime by remember { mutableStateOf("") }
    var selectedTeam1 by remember { mutableStateOf<Team?>(null) }
    var selectedTeam2 by remember { mutableStateOf<Team?>(null) }
    var showTeam1Dropdown by remember { mutableStateOf(false) }
    var showTeam2Dropdown by remember { mutableStateOf(false) }

    // Match rules
    var powerplayOvers by remember { mutableStateOf(6) }
    var superOverEnabled by remember { mutableStateOf(false) }
    var followOnEnabled by remember { mutableStateOf(false) }
    var freeHitOnNoball by remember { mutableStateOf(true) }
    var liveSharingEnabled by remember { mutableStateOf(true) }

    // Officials
    var showOfficialsSection by remember { mutableStateOf(false) }
    var umpire1 by remember { mutableStateOf("") }
    var umpire2 by remember { mutableStateOf("") }
    var thirdUmpire by remember { mutableStateOf("") }
    var matchReferee by remember { mutableStateOf("") }
    var scorerName by remember { mutableStateOf("") }

    // Current step
    var currentStep by remember { mutableIntStateOf(0) }
    val steps = listOf("Match Info", "Teams", "Rules", "Officials", "Review")

    val matchTypes = listOf(
        "T20" to "T20 (20 overs)",
        "ODI" to "ODI (50 overs)",
        "T10" to "T10 (10 overs)",
        "Test" to "Test Match",
        "Custom" to "Custom Overs"
    )

    val playersOptions = listOf(6, 7, 8, 9, 10, 11)

    LaunchedEffect(uiState.matchCreated) {
        if (uiState.matchCreated) {
            uiState.currentMatch?.let { onMatchCreated(it.id) }
            viewModel.resetMatchCreated()
        }
    }

    // Auto set overs based on match type
    LaunchedEffect(selectedMatchType) {
        selectedOvers = when (selectedMatchType) {
            "T20" -> 20
            "ODI" -> 50
            "T10" -> 10
            "Test" -> 90
            else -> selectedOvers
        }
        powerplayOvers = when (selectedMatchType) {
            "T20" -> 6
            "ODI" -> 10
            "T10" -> 2
            else -> 6
        }
        followOnEnabled = selectedMatchType == "Test"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(
                "Create Match",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        // Step indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
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
                                    isActive -> NeonBlue
                                    else -> BorderColor
                                }
                            )
                    )
                    Text(
                        step,
                        color = when {
                            isDone -> NeonGreen
                            isActive -> NeonBlue
                            else -> TextSecondary
                        },
                        fontSize = 9.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Step content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (currentStep) {

                // ========================
                // STEP 0 — Match Info
                // ========================
                0 -> {
                    SectionTitle("Match Information")

                    MatchTextField(
                        value = matchTitle,
                        onValueChange = { matchTitle = it },
                        label = "Match Title (optional)"
                    )

                    SectionTitle("Match Type")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        matchTypes.forEach { (type, label) ->
                            val selected = selectedMatchType == type
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (selected) NeonGreen.copy(alpha = 0.15f) else SurfaceCard
                                    )
                                    .border(
                                        1.dp,
                                        if (selected) NeonGreen else BorderColor,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedMatchType = type }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    label,
                                    color = if (selected) NeonGreen else TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                                if (selected) Text("✓", color = NeonGreen, fontSize = 16.sp)
                            }
                        }
                    }

                    if (selectedMatchType == "Custom") {
                        MatchTextField(
                            value = customOvers,
                            onValueChange = {
                                if (it.all { c -> c.isDigit() } && it.length <= 3) {
                                    customOvers = it
                                    it.toIntOrNull()?.let { o -> selectedOvers = o }
                                }
                            },
                            label = "Custom Overs"
                        )
                    }

                    SectionTitle("Players per Side")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        playersOptions.forEach { count ->
                            val selected = selectedPlayers == count
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selected) NeonBlue.copy(alpha = 0.2f) else SurfaceCard
                                    )
                                    .border(
                                        1.dp,
                                        if (selected) NeonBlue else BorderColor,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedPlayers = count }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$count",
                                    color = if (selected) NeonBlue else TextSecondary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }

                    SectionTitle("Match Details")
                    MatchTextField(value = venue, onValueChange = { venue = it }, label = "Venue/Ground")
                    MatchTextField(value = matchDate, onValueChange = { matchDate = it }, label = "Match Date (YYYY-MM-DD)")
                    MatchTextField(value = matchTime, onValueChange = { matchTime = it }, label = "Match Time (HH:MM)")
                }

                // ========================
                // STEP 1 — Teams
                // ========================
                1 -> {
                    SectionTitle("Select Teams")

                    Text("Team A", color = TextSecondary, fontSize = 13.sp)
                    MatchTeamSelector(
                        selectedTeam = selectedTeam1,
                        teams = uiState.teams.filter { it.id != selectedTeam2?.id },
                        expanded = showTeam1Dropdown,
                        onExpand = { showTeam1Dropdown = true },
                        onDismiss = { showTeam1Dropdown = false },
                        onTeamSelected = { selectedTeam1 = it; showTeam1Dropdown = false }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text("Team B", color = TextSecondary, fontSize = 13.sp)
                    MatchTeamSelector(
                        selectedTeam = selectedTeam2,
                        teams = uiState.teams.filter { it.id != selectedTeam1?.id },
                        expanded = showTeam2Dropdown,
                        onExpand = { showTeam2Dropdown = true },
                        onDismiss = { showTeam2Dropdown = false },
                        onTeamSelected = { selectedTeam2 = it; showTeam2Dropdown = false }
                    )
                }

                // ========================
                // STEP 2 — Rules
                // ========================
                2 -> {
                    SectionTitle("Match Rules")

                    InfoRow("Match Type", selectedMatchType)
                    InfoRow("Total Overs", "$selectedOvers overs")

                    SectionTitle("Powerplay Overs")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Powerplay 1 overs", color = TextSecondary, fontSize = 13.sp)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IconButton(
                                onClick = { if (powerplayOvers > 1) powerplayOvers-- },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("-", color = NeonGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "$powerplayOvers",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { if (powerplayOvers < selectedOvers) powerplayOvers++ },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("+", color = NeonGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    SectionTitle("Match Options")

                    ToggleRow(
                        label = "Super Over Enabled",
                        description = "Enable super over for tie situations",
                        checked = superOverEnabled,
                        onCheckedChange = { superOverEnabled = it }
                    )

                    ToggleRow(
                        label = "Free Hit on No Ball",
                        description = "Next ball is free hit after no ball",
                        checked = freeHitOnNoball,
                        onCheckedChange = { freeHitOnNoball = it }
                    )

                    if (selectedMatchType == "Test") {
                        ToggleRow(
                            label = "Follow-on Enabled",
                            description = "Team can enforce follow-on (200+ run lead)",
                            checked = followOnEnabled,
                            onCheckedChange = { followOnEnabled = it }
                        )
                    }

                    ToggleRow(
                        label = "Live Score Sharing",
                        description = "Allow sharing live score via WhatsApp/SMS",
                        checked = liveSharingEnabled,
                        onCheckedChange = { liveSharingEnabled = it }
                    )
                }

                // ========================
                // STEP 3 — Officials
                // ========================
                3 -> {
                    SectionTitle("Match Officials")

                    ToggleRow(
                        label = "Add Officials",
                        description = "Name umpires, referee, and scorer",
                        checked = showOfficialsSection,
                        onCheckedChange = { showOfficialsSection = it }
                    )

                    if (showOfficialsSection) {
                        Spacer(modifier = Modifier.height(8.dp))
                        MatchTextField(value = umpire1, onValueChange = { umpire1 = it }, label = "Umpire 1 (optional)")
                        MatchTextField(value = umpire2, onValueChange = { umpire2 = it }, label = "Umpire 2 (optional)")
                        MatchTextField(value = thirdUmpire, onValueChange = { thirdUmpire = it }, label = "Third Umpire (optional)")
                        MatchTextField(value = matchReferee, onValueChange = { matchReferee = it }, label = "Match Referee (optional)")
                        MatchTextField(value = scorerName, onValueChange = { scorerName = it }, label = "Scorer (optional)")
                    }
                }

                // ========================
                // STEP 4 — Review
                // ========================
                4 -> {
                    SectionTitle("Match Summary")

                    ReviewCard {
                        if (matchTitle.isNotBlank()) ReviewRow("Title", matchTitle)
                        ReviewRow("Type", selectedMatchType)
                        ReviewRow("Overs", "$selectedOvers overs")
                        ReviewRow("Players", "$selectedPlayers per side")
                        if (venue.isNotBlank()) ReviewRow("Venue", venue)
                        if (matchDate.isNotBlank()) ReviewRow("Date", matchDate)
                        if (matchTime.isNotBlank()) ReviewRow("Time", matchTime)
                    }

                    ReviewCard {
                        ReviewRow("Team A", selectedTeam1?.name ?: "Not selected")
                        ReviewRow("Team B", selectedTeam2?.name ?: "Not selected")
                    }

                    ReviewCard {
                        ReviewRow("Powerplay", "$powerplayOvers overs")
                        ReviewRow("Super Over", if (superOverEnabled) "✅ Yes" else "❌ No")
                        ReviewRow("Free Hit", if (freeHitOnNoball) "✅ Yes" else "❌ No")
                        ReviewRow("Live Sharing", if (liveSharingEnabled) "✅ Yes" else "❌ No")
                        if (selectedMatchType == "Test") ReviewRow("Follow-on", if (followOnEnabled) "✅ Yes" else "❌ No")
                    }

                    if (showOfficialsSection) {
                        ReviewCard {
                            if (umpire1.isNotBlank()) ReviewRow("Umpire 1", umpire1)
                            if (umpire2.isNotBlank()) ReviewRow("Umpire 2", umpire2)
                            if (thirdUmpire.isNotBlank()) ReviewRow("3rd Umpire", thirdUmpire)
                            if (matchReferee.isNotBlank()) ReviewRow("Referee", matchReferee)
                            if (scorerName.isNotBlank()) ReviewRow("Scorer", scorerName)
                        }
                    }

                    if (uiState.error != null) {
                        Text(uiState.error ?: "", color = ErrorRed, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Bottom navigation buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (currentStep > 0) {
                OutlinedButton(
                    onClick = { currentStep-- },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Back")
                }
            }

            Button(
                onClick = {
                    if (currentStep < steps.size - 1) {
                        currentStep++
                    } else {
                        // Create match
                        val t1 = selectedTeam1 ?: return@Button
                        val t2 = selectedTeam2 ?: return@Button
                        val overs = if (selectedMatchType == "Custom")
                            customOvers.toIntOrNull() ?: 20 else selectedOvers

                        viewModel.createMatch(
                            MatchInsert(
                                title = matchTitle.ifBlank { null },
                                matchType = selectedMatchType,
                                team1Id = t1.id,
                                team2Id = t2.id,
                                venue = venue.ifBlank { null },
                                matchDate = matchDate.ifBlank { null },
                                matchTime = matchTime.ifBlank { null },
                                totalOvers = overs,
                                playersPerSide = selectedPlayers,
                                powerplayOvers = powerplayOvers,
                                superOverEnabled = superOverEnabled,
                                followOnEnabled = followOnEnabled,
                                freeHitOnNoball = freeHitOnNoball,
                                liveSharingEnabled = liveSharingEnabled,
                                umpire1 = umpire1.ifBlank { null },
                                umpire2 = umpire2.ifBlank { null },
                                thirdUmpire = thirdUmpire.ifBlank { null },
                                matchReferee = matchReferee.ifBlank { null },
                                scorerName = scorerName.ifBlank { null },
                                createdBy = "",
                                status = "scheduled"
                            )
                        )
                    }
                },
                enabled = when (currentStep) {
                    1 -> selectedTeam1 != null && selectedTeam2 != null
                    4 -> selectedTeam1 != null && selectedTeam2 != null && !uiState.isLoading
                    else -> true
                },
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isLoading && currentStep == 4) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        if (currentStep == steps.size - 1) "Start Match" else "Next →",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

// ========================
// Helper Composables
// ========================

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        color = NeonGreen,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun MatchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = NeonGreen,
            unfocusedBorderColor = BorderColor,
            focusedLabelColor = NeonGreen,
            unfocusedLabelColor = TextSecondary,
            cursorColor = NeonGreen
        )
    )
}

@Composable
fun MatchTeamSelector(
    selectedTeam: Team?,
    teams: List<Team>,
    expanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    onTeamSelected: (Team) -> Unit
) {
    Box {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceCard)
                .border(
                    1.dp,
                    if (selectedTeam != null) NeonGreen else BorderColor,
                    RoundedCornerShape(10.dp)
                )
                .clickable { onExpand() }
                .padding(16.dp)
        ) {
            Text(
                selectedTeam?.name ?: "Select team",
                color = if (selectedTeam != null) TextPrimary else TextSecondary
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            modifier = Modifier.background(SurfaceCard)
        ) {
            if (teams.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No teams available", color = TextSecondary) },
                    onClick = onDismiss
                )
            }
            teams.forEach { team ->
                DropdownMenuItem(
                    text = { Text(team.name, color = TextPrimary) },
                    onClick = { onTeamSelected(team) }
                )
            }
        }
    }
}

@Composable
fun ToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(description, color = TextSecondary, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = NeonGreen,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = BorderColor
            )
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ReviewCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content
    )
}

@Composable
fun ReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}