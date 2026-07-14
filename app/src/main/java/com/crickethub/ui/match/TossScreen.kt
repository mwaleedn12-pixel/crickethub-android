package com.crickethub.ui.match

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crickethub.ui.theme.*


@Composable
fun TossScreen(
    matchId: String,
    onTossComplete: (String) -> Unit,
    viewModel: MatchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var tossWinnerId by remember { mutableStateOf<String?>(null) }
    var tossDecision by remember { mutableStateOf<String?>(null) }
    var matchLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(matchId) {
        viewModel.loadMatchById(matchId)
        viewModel.loadTeams()
    }

    // Sirf tab check karo jab match actually load ho jaye
    LaunchedEffect(uiState.currentMatch?.tossWinnerId) {
        if (uiState.currentMatch?.tossWinnerId != null) {
            onTossComplete(matchId)
        }
    }

    LaunchedEffect(uiState.currentMatch, uiState.isLoading) {
        val match = uiState.currentMatch
        // Loading khatam ho, match load ho gaya, aur toss already hai
        if (!uiState.isLoading && match != null && match.id == matchId) {
            matchLoaded = true
            if (match.tossWinnerId != null) {
                // Toss already ho chuka — matches list par wapas jao
                onTossComplete(matchId)
            }
        }
    }

    val match = uiState.currentMatch
    val teams = uiState.teams
    val team1 = teams.find { it.id == match?.team1Id }
    val team2 = teams.find { it.id == match?.team2Id }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text("🪙", fontSize = 64.sp, textAlign = TextAlign.Center)
        Text(
            "Toss",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        // Loading state
        if (uiState.isLoading || !matchLoaded) {
            CircularProgressIndicator(color = NeonGreen)
        } else if (match != null && match.tossWinnerId == null) {
            // Sirf tab dikho jab toss nahi hua

            Text(
                "Who won the toss?",
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(team1, team2).forEach { team ->
                    if (team != null) {
                        val selected = tossWinnerId == team.id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) NeonGreen.copy(alpha = 0.2f) else SurfaceCard)
                                .border(
                                    2.dp,
                                    if (selected) NeonGreen else BorderColor,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { tossWinnerId = team.id }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                team.name,
                                color = if (selected) NeonGreen else TextPrimary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (tossWinnerId != null) {
                val winnerName = teams.find { it.id == tossWinnerId }?.name ?: ""
                Text(
                    "$winnerName chose to...",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("bat" to "BAT", "bowl" to "BOWL").forEach { (value, label) ->
                        val selected = tossDecision == value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) NeonGreen.copy(alpha = 0.2f) else SurfaceCard)
                                .border(
                                    2.dp,
                                    if (selected) NeonGreen else BorderColor,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { tossDecision = value }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (selected) NeonGreen else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val winner = tossWinnerId ?: return@Button
                    val decision = tossDecision ?: return@Button
                    viewModel.recordToss(matchId, winner, decision)
                },
                enabled = tossWinnerId != null && tossDecision != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Confirm toss",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}