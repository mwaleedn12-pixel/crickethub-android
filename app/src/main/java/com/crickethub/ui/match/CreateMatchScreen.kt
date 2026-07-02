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
import com.crickethub.data.model.Team

private val BackgroundDark = Color(0xFF030712)
private val SurfaceCard = Color(0xFF111827)
private val BorderColor = Color(0xFF1F2937)
private val NeonGreen = Color(0xFF10B981)
private val NeonBlue = Color(0xFF3B82F6)
private val TextPrimary = Color(0xFFF9FAFB)
private val TextSecondary = Color(0xFF9CA3AF)
private val ErrorRed = Color(0xFFEF4444)

@Composable
fun CreateMatchScreen(
    onBack: () -> Unit,
    onMatchCreated: (String) -> Unit,
    viewModel: MatchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedTeam1: Team? by remember { mutableStateOf(null) }
    var selectedTeam2: Team? by remember { mutableStateOf(null) }
    var venue by remember { mutableStateOf("") }
    var selectedOvers by remember { mutableStateOf(20) }
    var showTeam1Dropdown by remember { mutableStateOf(false) }
    var showTeam2Dropdown by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.matchCreated) {
        if (uiState.matchCreated) {
            uiState.currentMatch?.let { onMatchCreated(it.id) }
            viewModel.resetMatchCreated()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text("Create match", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        if (uiState.error != null) {
            Text(uiState.error ?: "", color = ErrorRed, fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 12.dp))
        }

        // Team 1 selector
        SectionLabel("Team 1")
        TeamSelector(
            selectedTeam = selectedTeam1,
            teams = uiState.teams.filter { it.id != selectedTeam2?.id },
            expanded = showTeam1Dropdown,
            onExpand = { showTeam1Dropdown = true },
            onDismiss = { showTeam1Dropdown = false },
            onTeamSelected = { selectedTeam1 = it; showTeam1Dropdown = false }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Team 2 selector
        SectionLabel("Team 2")
        TeamSelector(
            selectedTeam = selectedTeam2,
            teams = uiState.teams.filter { it.id != selectedTeam1?.id },
            expanded = showTeam2Dropdown,
            onExpand = { showTeam2Dropdown = true },
            onDismiss = { showTeam2Dropdown = false },
            onTeamSelected = { selectedTeam2 = it; showTeam2Dropdown = false }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Venue
        SectionLabel("Venue (optional)")
        OutlinedTextField(
            value = venue,
            onValueChange = { venue = it },
            placeholder = { Text("e.g. National Stadium", color = TextSecondary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                focusedBorderColor = NeonGreen, unfocusedBorderColor = BorderColor,
                cursorColor = NeonGreen
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Overs
        SectionLabel("Overs format")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 20, 50).forEach { overs ->
                val selected = selectedOvers == overs
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) NeonGreen.copy(alpha = 0.2f) else SurfaceCard)
                        .border(1.dp, if (selected) NeonGreen else BorderColor, RoundedCornerShape(8.dp))
                        .clickable { selectedOvers = overs }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$overs", color = if (selected) NeonGreen else TextSecondary,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val t1 = selectedTeam1 ?: return@Button
                val t2 = selectedTeam2 ?: return@Button
                viewModel.createMatch(t1.id, t2.id, venue.ifBlank { null }, selectedOvers)
            },
            enabled = selectedTeam1 != null && selectedTeam2 != null && !uiState.isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp),
                    color = Color.Black, strokeWidth = 2.dp)
            } else {
                Text("Create match", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text, color = TextSecondary, fontSize = 13.sp,
        modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
fun TeamSelector(
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
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceCard)
                .border(1.dp, if (selectedTeam != null) NeonGreen else BorderColor, RoundedCornerShape(8.dp))
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