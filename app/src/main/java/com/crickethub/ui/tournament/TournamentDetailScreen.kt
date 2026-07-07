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
import com.crickethub.data.model.Match

private val BackgroundDark = Color(0xFF030712)
private val SurfaceCard = Color(0xFF111827)
private val BorderColor = Color(0xFF1F2937)
private val NeonGreen = Color(0xFF10B981)
private val NeonBlue = Color(0xFF3B82F6)
private val TextPrimary = Color(0xFFF9FAFB)
private val TextSecondary = Color(0xFF9CA3AF)
private val AmberColor = Color(0xFFF59E0B)
private val ErrorRed = Color(0xFFEF4444)

@Composable
fun TournamentDetailScreen(
    tournamentId: String,
    onBack: () -> Unit,
    onMatchClick: (String) -> Unit,
    viewModel: TournamentViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Points Table", "Fixtures")

    LaunchedEffect(tournamentId) {
        viewModel.loadAllTeams()
        viewModel.loadTournamentDetail(tournamentId)
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
            Column {
                Text(
                    uiState.currentTournament?.name ?: "Tournament",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    "${uiState.tournamentTeams.size} teams • ${uiState.fixtures.size} matches",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) NeonGreen else SurfaceCard)
                        .border(1.dp, if (selected) NeonGreen else BorderColor, RoundedCornerShape(8.dp))
                        .clickable { selectedTab = index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        tab,
                        color = if (selected) Color.Black else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonGreen)
            }
        } else {
            when (selectedTab) {
                0 -> PointsTableTab(uiState = uiState)
                1 -> FixturesTab(
                    fixtures = uiState.fixtures,
                    teamDetails = uiState.teamDetails,
                    onMatchClick = onMatchClick
                )
            }
        }
    }
}

@Composable
fun PointsTableTab(uiState: TournamentUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        // Table header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceCard)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("#", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(24.dp))
                Text("Team", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("P", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("W", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("L", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("Pts", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
                Text("NRR", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(48.dp), textAlign = TextAlign.End)
            }
        }

        // Sorted by points then NRR
        val sortedTeams = uiState.tournamentTeams.sortedWith(
            compareByDescending<com.crickethub.data.model.TournamentTeam> { it.points }
                .thenByDescending { it.nrr }
        )

        items(sortedTeams.withIndex().toList()) { (index, tt) ->
            val teamName = uiState.teamDetails.find { it.id == tt.teamId }?.name ?: tt.teamId.take(8)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${index + 1}",
                    color = if (index < 4) NeonGreen else TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(24.dp)
                )
                Text(
                    teamName,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${tt.matchesPlayed}",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.width(28.dp),
                    textAlign = TextAlign.End
                )
                Text(
                    "${tt.wins}",
                    color = NeonGreen,
                    fontSize = 13.sp,
                    modifier = Modifier.width(28.dp),
                    textAlign = TextAlign.End
                )
                Text(
                    "${tt.losses}",
                    color = ErrorRed,
                    fontSize = 13.sp,
                    modifier = Modifier.width(28.dp),
                    textAlign = TextAlign.End
                )
                Text(
                    "${tt.points}",
                    color = NeonBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.End
                )
                Text(
                    "${"%.3f".format(tt.nrr)}",
                    color = if (tt.nrr >= 0) NeonGreen else ErrorRed,
                    fontSize = 12.sp,
                    modifier = Modifier.width(48.dp),
                    textAlign = TextAlign.End
                )
            }
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        }
    }
}

@Composable
fun FixturesTab(
    fixtures: List<Match>,
    teamDetails: List<com.crickethub.data.model.Team>,
    onMatchClick: (String) -> Unit
) {
    if (fixtures.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No fixtures yet", color = TextSecondary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(fixtures) { match ->
            val team1Name = teamDetails.find { it.id == match.team1Id }?.name ?: "Team 1"
            val team2Name = teamDetails.find { it.id == match.team2Id }?.name ?: "Team 2"

            val statusColor = when (match.status) {
                "live" -> NeonGreen
                "completed" -> TextSecondary
                else -> AmberColor
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .clickable { onMatchClick(match.id) }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        match.status.uppercase(),
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("${match.totalOvers} overs", color = TextSecondary, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        team1Name,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "vs",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Text(
                        team2Name,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                }

                match.resultText?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = NeonGreen, fontSize = 12.sp)
                }
            }
        }
    }
}