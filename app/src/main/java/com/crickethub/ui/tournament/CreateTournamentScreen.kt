package com.crickethub.ui.tournament

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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

private val BackgroundDark = Color(0xFF030712)
private val SurfaceCard = Color(0xFF111827)
private val BorderColor = Color(0xFF1F2937)
private val NeonGreen = Color(0xFF10B981)
private val NeonBlue = Color(0xFF3B82F6)
private val TextPrimary = Color(0xFFF9FAFB)
private val TextSecondary = Color(0xFF9CA3AF)
private val ErrorRed = Color(0xFFEF4444)

@Composable
fun CreateTournamentScreen(
    onBack: () -> Unit,
    onTournamentCreated: (String) -> Unit,
    viewModel: TournamentViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var tournamentName by remember { mutableStateOf("") }
    var selectedTeamIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedOvers by remember { mutableStateOf(20) }

    LaunchedEffect(uiState.tournamentCreated) {
        if (uiState.tournamentCreated) {
            uiState.currentTournament?.let { onTournamentCreated(it.id) }
            viewModel.resetTournamentCreated()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
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
                "Create Tournament",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tournament name
            item {
                Text("Tournament Name", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = tournamentName,
                    onValueChange = { tournamentName = it },
                    placeholder = { Text("e.g. IPL 2026", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = BorderColor,
                        cursorColor = NeonGreen
                    )
                )
            }

            // Overs format
            item {
                Text("Match Format", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 20, 50).forEach { overs ->
                        val selected = selectedOvers == overs
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selected) NeonGreen.copy(alpha = 0.2f) else SurfaceCard
                                )
                                .border(
                                    1.dp,
                                    if (selected) NeonGreen else BorderColor,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedOvers = overs }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$overs ov",
                                color = if (selected) NeonGreen else TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Team selection
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Teams", color = TextSecondary, fontSize = 13.sp)
                    Text(
                        "${selectedTeamIds.size} selected",
                        color = if (selectedTeamIds.size >= 2) NeonGreen else TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            items(uiState.allTeams) { team ->
                val isSelected = team.id in selectedTeamIds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) NeonGreen.copy(alpha = 0.1f) else SurfaceCard)
                        .border(
                            1.dp,
                            if (isSelected) NeonGreen else BorderColor,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            selectedTeamIds = if (isSelected) {
                                selectedTeamIds - team.id
                            } else {
                                selectedTeamIds + team.id
                            }
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        team.name,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = NeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Create button
        Button(
            onClick = {
                if (tournamentName.isNotBlank() && selectedTeamIds.size >= 2) {
                    viewModel.createTournament(
                        name = tournamentName,
                        selectedTeamIds = selectedTeamIds.toList(),
                        totalOvers = selectedOvers
                    )
                }
            },
            enabled = tournamentName.isNotBlank() && selectedTeamIds.size >= 2 && !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    "Create & Generate Fixtures",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}