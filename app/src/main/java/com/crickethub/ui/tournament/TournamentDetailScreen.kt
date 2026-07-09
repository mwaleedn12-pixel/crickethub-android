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
import com.crickethub.data.model.Team
import com.crickethub.data.model.TournamentTeam

private val BackgroundDark = Color(0xFF030712)
private val SurfaceCard = Color(0xFF111827)
private val BorderColor = Color(0xFF1F2937)
private val NeonGreen = Color(0xFF10B981)
private val NeonBlue = Color(0xFF3B82F6)
private val TextPrimary = Color(0xFFF9FAFB)
private val TextSecondary = Color(0xFF9CA3AF)
private val AmberColor = Color(0xFFF59E0B)
private val ErrorRed = Color(0xFFEF4444)
private val PurpleColor = Color(0xFF8B5CF6)

@Composable
fun TournamentDetailScreen(
    tournamentId: String,
    onBack: () -> Unit,
    onMatchClick: (String) -> Unit,
    onViewScorecard: (String) -> Unit = {},
    onViewAnalytics: (String) -> Unit = {},
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
                0 -> TournamentPointsTable(tournamentTeams = uiState.tournamentTeams, teamDetails = uiState.teamDetails)
                1 -> TournamentFixtures(
                    fixtures = uiState.fixtures,
                    teamDetails = uiState.teamDetails,
                    onMatchClick = onMatchClick,
                    onViewScorecard = onViewScorecard,
                    onViewAnalytics = onViewAnalytics
                )
            }
        }
    }
}

@Composable
fun TournamentPointsTable(
    tournamentTeams: List<TournamentTeam>,
    teamDetails: List<Team>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
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

        val sortedTeams = tournamentTeams.sortedWith(
            compareByDescending<TournamentTeam> { it.points }.thenByDescending { it.nrr }
        )

        items(sortedTeams.withIndex().toList()) { (index, tt) ->
            val teamName = teamDetails.find { it.id == tt.teamId }?.name ?: "Team"
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
                Text(teamName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${tt.matchesPlayed}", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("${tt.wins}", color = NeonGreen, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("${tt.losses}", color = ErrorRed, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("${tt.points}", color = NeonBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
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
fun TournamentFixtures(
    fixtures: List<Match>,
    teamDetails: List<Team>,
    onMatchClick: (String) -> Unit,
    onViewScorecard: (String) -> Unit,
    onViewAnalytics: (String) -> Unit
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
                "abandoned" -> AmberColor
                "cancelled" -> ErrorRed
                else -> AmberColor
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(match.status.uppercase(), color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("${match.matchType} • ${match.totalOvers} ov", color = TextSecondary, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(team1Name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Text("vs", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 8.dp))
                    Text(team2Name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                }

                match.resultText?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(it, color = NeonGreen, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { onMatchClick(match.id) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("Score", fontSize = 11.sp) }

                    OutlinedButton(
                        onClick = { onViewScorecard(match.id) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("Live", fontSize = 11.sp) }

                    OutlinedButton(
                        onClick = { onViewAnalytics(match.id) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleColor),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("Analytics", fontSize = 11.sp) }
                }
            }
        }
    }
}