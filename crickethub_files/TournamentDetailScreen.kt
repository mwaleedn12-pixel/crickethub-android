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
import com.crickethub.data.model.Ball
import com.crickethub.data.model.Match
import com.crickethub.data.model.Player
import com.crickethub.data.model.Team
import com.crickethub.data.model.TournamentTeam
import com.crickethub.data.repository.ScoringRepository
import kotlinx.coroutines.launch


val TOURNAMENT_FORMATS = listOf(
    "League", "Round Robin", "Double Round Robin",
    "Knockout", "Group + Knockout", "Hybrid"
)

fun totalMatches(format: String, n: Int): Int = when (format) {
    "League", "Round Robin" -> if (n >= 2) n * (n - 1) / 2 else 0
    "Double Round Robin" -> if (n >= 2) n * (n - 1) else 0
    "Knockout" -> if (n >= 2) n - 1 else 0
    "Group + Knockout" -> if (n >= 2) (n * (n - 1) / 2) + (n / 2) else 0
    "Hybrid" -> if (n >= 2) n * (n - 1) / 2 + 3 else 0
    else -> 0
}

fun formatDescription(format: String): String = when (format) {
    "League" -> "Every team plays every other team once.\nTop team on points wins the tournament."
    "Round Robin" -> "Complete round where every team faces each other once.\nSame as League format."
    "Double Round Robin" -> "Every team plays every other team twice.\nMore matches, better representation of team strength."
    "Knockout" -> "Single elimination — lose once and you're out.\nDirect knockout from first round to final."
    "Group + Knockout" -> "Teams play group stage first.\nTop teams from each group advance to knockouts."
    "Hybrid" -> "League stage to determine standings.\nTop 4 teams play Semi-Finals then Final."
    else -> ""
}

fun formatStages(format: String, n: Int): List<String> = when (format) {
    "League", "Round Robin" -> listOf(
        "Round Robin: ${totalMatches(format, n)} matches",
        "Every team plays every other team once",
        "Winner: Team with most points"
    )
    "Double Round Robin" -> listOf(
        "Round 1: ${n * (n - 1) / 2} matches",
        "Round 2: ${n * (n - 1) / 2} matches",
        "Total: ${n * (n - 1)} matches",
        "Winner: Most points after 2 rounds"
    )
    "Knockout" -> {
        val rounds = mutableListOf<String>()
        var remaining = n
        var round = 1
        while (remaining > 1) {
            rounds.add("Round $round: ${remaining / 2} matches")
            remaining /= 2
            round++
        }
        rounds.add("Total: ${n - 1} matches")
        rounds
    }
    "Group + Knockout" -> listOf(
        "Group Stage: ${n * (n - 1) / 2} matches",
        "Semi Finals: ${n / 2} matches",
        "Final: 1 match",
        "Total: ${totalMatches(format, n)} matches"
    )
    "Hybrid" -> listOf(
        "League Stage: ${n * (n - 1) / 2} matches",
        "Semi Finals: 2 matches",
        "Final: 1 match",
        "Total: ${totalMatches(format, n)} matches"
    )
    else -> emptyList()
}

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
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Fixtures", "Points", "Stats", "Awards")

    var allBalls by remember { mutableStateOf<List<Pair<Ball, String>>>(emptyList()) }
    var playerTeamMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var playerMap by remember { mutableStateOf<Map<String, Player>>(emptyMap()) }
    var statsLoading by remember { mutableStateOf(false) }

    LaunchedEffect(tournamentId) {
        viewModel.loadAllTeams()
        viewModel.loadTournamentDetail(tournamentId)
    }

    LaunchedEffect(selectedTab) {
        if ((selectedTab == 2 || selectedTab == 3) && allBalls.isEmpty() && !statsLoading) {
            statsLoading = true
            scope.launch {
                try {
                    val scoringRepo = ScoringRepository()
                    val tempBalls = mutableListOf<Pair<Ball, String>>()
                    val tempPlayerTeam = mutableMapOf<String, String>()
                    val tempPlayerMap = mutableMapOf<String, Player>()

                    uiState.fixtures.forEach { match ->
                        val allInnings = scoringRepo.getInningsByMatch(match.id)
                        allInnings.forEach { inn ->
                            val balls = scoringRepo.getBallsByInnings(inn.id)
                            val teamName = uiState.teamDetails.find { it.id == inn.battingTeamId }?.name ?: ""
                            val bowlTeamName = uiState.teamDetails.find { it.id == inn.bowlingTeamId }?.name ?: ""
                            balls.forEach { ball -> tempBalls.add(Pair(ball, teamName)) }
                            val batPlayers = scoringRepo.getPlayingXIPlayers(match.id, inn.battingTeamId)
                            val bowlPlayers = scoringRepo.getPlayingXIPlayers(match.id, inn.bowlingTeamId)
                            batPlayers.forEach { p ->
                                tempPlayerMap[p.id] = p
                                tempPlayerTeam[p.id] = teamName
                            }
                            bowlPlayers.forEach { p ->
                                tempPlayerMap[p.id] = p
                                if (!tempPlayerTeam.containsKey(p.id)) tempPlayerTeam[p.id] = bowlTeamName
                            }
                        }
                    }
                    allBalls = tempBalls
                    playerTeamMap = tempPlayerTeam
                    playerMap = tempPlayerMap
                } catch (e: Exception) {
                    android.util.Log.e("CricketHub", "Stats error: ${e.message}", e)
                } finally {
                    statsLoading = false
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    uiState.currentTournament?.name ?: "Tournament",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                )
                Text(
                    "${uiState.tournamentTeams.size} teams • ${uiState.fixtures.size} matches",
                    fontSize = 12.sp, color = TextSecondary
                )
            }
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = SurfaceCard,
            contentColor = NeonGreen,
            edgePadding = 0.dp
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            tab, fontSize = 13.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) NeonGreen else TextSecondary
                        )
                    }
                )
            }
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonGreen)
            }
        } else {
            when (selectedTab) {
                0 -> TFixturesTab(
                    uiState = uiState,
                    tournamentId = tournamentId,
                    onMatchClick = onMatchClick,
                    onViewScorecard = onViewScorecard,
                    onViewAnalytics = onViewAnalytics,
                    viewModel = viewModel
                )
                1 -> TPointsTab(uiState = uiState)
                2 -> TStatsTab(allBalls, playerTeamMap, playerMap, statsLoading)
                3 -> TAwardsTab(allBalls, playerTeamMap, playerMap, statsLoading)
            }
        }
    }
}

// ── FIXTURES TAB ─────────────────────────────────────────────

@Composable
fun TFixturesTab(
    uiState: TournamentUiState,
    tournamentId: String,
    onMatchClick: (String) -> Unit,
    onViewScorecard: (String) -> Unit,
    onViewAnalytics: (String) -> Unit,
    viewModel: TournamentViewModel
) {
    var selectedFormat by remember { mutableStateOf("League") }
    var showTeamPicker by remember { mutableStateOf(false) }
    var showFormatInfo by remember { mutableStateOf(false) }
    val fixturesGenerated = uiState.fixtures.isNotEmpty()
    val teamCount = uiState.tournamentTeams.size

    // Format info dialog
    if (showFormatInfo) {
        AlertDialog(
            onDismissRequest = { showFormatInfo = false },
            containerColor = SurfaceCard,
            title = {
                Text(
                    "Format: $selectedFormat",
                    color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp
                )
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(max = 500.dp)
                ) {
                    item {
                        Text(
                            formatDescription(selectedFormat),
                            color = TextSecondary, fontSize = 13.sp, lineHeight = 20.sp
                        )
                    }
                    item { HorizontalDivider(color = BorderColor) }
                    item {
                        Text("How it works:", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        formatStages(selectedFormat, teamCount.coerceAtLeast(2)).forEach { stage ->
                            Row(
                                modifier = Modifier.padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("•", color = NeonGreen, fontSize = 13.sp)
                                Text(stage, color = TextPrimary, fontSize = 13.sp)
                            }
                        }
                    }
                    item { HorizontalDivider(color = BorderColor) }
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonGreen.copy(alpha = 0.1f))
                                .border(1.dp, NeonGreen, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "With $teamCount teams",
                                    color = TextSecondary, fontSize = 12.sp
                                )
                                Text(
                                    "Total Matches: ${totalMatches(selectedFormat, teamCount)}",
                                    color = NeonGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold
                                )
                            }
                            Text("🏏", fontSize = 24.sp)
                        }
                    }
                    item {
                        HorizontalDivider(color = BorderColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "All Formats ($teamCount teams):",
                            color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        TOURNAMENT_FORMATS.forEach { format ->
                            val isCurrent = format == selectedFormat
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isCurrent) NeonGreen.copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable { selectedFormat = format }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    format,
                                    color = if (isCurrent) NeonGreen else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    "${totalMatches(format, teamCount)} matches",
                                    color = if (isCurrent) NeonGreen else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showFormatInfo = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) { Text("Got it", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Show setup only before fixtures generated
        if (!fixturesGenerated) {
            // Teams section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceCard)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Teams (${uiState.tournamentTeams.size})",
                            color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(
                            onClick = { showTeamPicker = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Text("+ Add Team", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (uiState.tournamentTeams.isEmpty()) {
                        Text("No teams added yet — tap + Add Team", color = TextSecondary, fontSize = 12.sp)
                    } else {
                        uiState.tournamentTeams.forEach { tt ->
                            val teamName = uiState.teamDetails.find { it.id == tt.teamId }?.name ?: "Team"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BackgroundDark)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(teamName, color = TextPrimary, fontSize = 13.sp)
                                TextButton(
                                    onClick = { viewModel.removeTeamFromTournament(tournamentId, tt.teamId) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) { Text("Remove", fontSize = 11.sp) }
                            }
                        }
                    }
                }
            }

            // Format selector
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceCard)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Select Format",
                            color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = { showFormatInfo = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = NeonBlue),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("ℹ How formats work", fontSize = 12.sp)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        TOURNAMENT_FORMATS.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { format ->
                                    val isSelected = selectedFormat == format
                                    val matches = totalMatches(format, teamCount)
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) NeonGreen.copy(alpha = 0.2f) else BackgroundDark)
                                            .border(1.dp, if (isSelected) NeonGreen else BorderColor, RoundedCornerShape(8.dp))
                                            .clickable { selectedFormat = format }
                                            .padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            format,
                                            color = if (isSelected) NeonGreen else TextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            textAlign = TextAlign.Center
                                        )
                                        if (teamCount >= 2) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                "$matches matches",
                                                color = if (isSelected) NeonGreen.copy(alpha = 0.8f) else TextSecondary.copy(alpha = 0.6f),
                                                fontSize = 10.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // Format preview
                    if (teamCount >= 2) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonGreen.copy(alpha = 0.08f))
                                .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    "$selectedFormat • $teamCount teams • ${totalMatches(selectedFormat, teamCount)} matches",
                                    color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    formatDescription(selectedFormat).lines().first(),
                                    color = TextSecondary, fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { viewModel.generateFixtures(tournamentId, selectedFormat) },
                        enabled = teamCount >= 2 && !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating...", color = Color.Black, fontWeight = FontWeight.Bold)
                        } else {
                            Text(
                                if (teamCount < 2) "Add at least 2 teams first"
                                else "Generate ${totalMatches(selectedFormat, teamCount)} Fixtures",
                                color = Color.Black, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            // Schedule generated summary
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonGreen.copy(alpha = 0.1f))
                        .border(1.dp, NeonGreen, RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "✅ Schedule Generated",
                            color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${uiState.tournamentTeams.size} teams • ${uiState.fixtures.size} matches",
                            color = TextSecondary, fontSize = 12.sp
                        )
                    }
                    Text("🏏", fontSize = 24.sp)
                }
            }
        }

        // Fixtures list
        if (uiState.fixtures.isNotEmpty()) {
            item {
                Text(
                    "Fixtures (${uiState.fixtures.size})",
                    color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
            }
            items(uiState.fixtures) { match ->
                TFixtureCard(
                    match = match,
                    teamDetails = uiState.teamDetails,
                    onMatchClick = onMatchClick,
                    onViewScorecard = onViewScorecard,
                    onViewAnalytics = onViewAnalytics
                )
            }
        }
    }

    // Team picker dialog
    if (showTeamPicker) {
        val availableTeams = uiState.allTeams.filter { team ->
            uiState.tournamentTeams.none { it.teamId == team.id }
        }
        AlertDialog(
            onDismissRequest = { showTeamPicker = false },
            containerColor = SurfaceCard,
            title = { Text("Add Team", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                if (availableTeams.isEmpty()) {
                    Text("No teams available. Create teams first.", color = TextSecondary)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        items(availableTeams) { team ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BackgroundDark)
                                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.addTeamToTournament(tournamentId, team.id)
                                        showTeamPicker = false
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(team.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    team.category?.let { Text(it, color = TextSecondary, fontSize = 11.sp) }
                                }
                                Text("+", color = NeonGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTeamPicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

// ── FIXTURE CARD ─────────────────────────────────────────────

@Composable
fun TFixtureCard(
    match: Match,
    teamDetails: List<Team>,
    onMatchClick: (String) -> Unit,
    onViewScorecard: (String) -> Unit,
    onViewAnalytics: (String) -> Unit
) {
    val team1Name = teamDetails.find { it.id == match.team1Id }?.name ?: "Team 1"
    val team2Name = teamDetails.find { it.id == match.team2Id }?.name ?: "Team 2"
    val statusColor = when (match.status) {
        "live" -> NeonGreen; "completed" -> TextSecondary
        "abandoned" -> AmberColor; "cancelled" -> ErrorRed
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(match.status.uppercase(), color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("${match.matchType} • ${match.totalOvers} ov", color = TextSecondary, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(team1Name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text("vs", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
            Text(team2Name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
        match.resultText?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Text(it, color = NeonGreen, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
            ) { Text("Stats", fontSize = 11.sp) }
        }
    }
}

// ── POINTS TABLE ─────────────────────────────────────────────

@Composable
fun TPointsTab(uiState: TournamentUiState) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceCard).padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("#", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(24.dp))
                Text("Team", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text("P", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                Text("W", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                Text("L", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                Text("Pts", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                Text("NRR", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(48.dp), textAlign = TextAlign.End)
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        val sortedTeams = uiState.tournamentTeams
            .sortedWith(compareByDescending<TournamentTeam> { it.points }.thenByDescending { it.nrr })

        items(sortedTeams.withIndex().toList()) { (index, tt) ->
            val teamName = uiState.teamDetails.find { it.id == tt.teamId }?.name ?: "Team"
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${index + 1}", color = if (index < 4) NeonGreen else TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                Text(teamName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${tt.matchesPlayed}", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                Text("${tt.wins}", color = NeonGreen, fontSize = 13.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                Text("${tt.losses}", color = ErrorRed, fontSize = 13.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                Text("${tt.points}", color = NeonBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                Text("${"%.3f".format(tt.nrr)}", color = if (tt.nrr >= 0) NeonGreen else ErrorRed, fontSize = 12.sp, modifier = Modifier.width(48.dp), textAlign = TextAlign.End)
            }
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        }
    }
}

// ── STATS TAB ────────────────────────────────────────────────

@Composable
fun TStatsTab(
    allBalls: List<Pair<Ball, String>>,
    playerTeamMap: Map<String, String>,
    playerMap: Map<String, Player>,
    isLoading: Boolean
) {
    var selectedStat by remember { mutableIntStateOf(0) }
    val statTabs = listOf("Orange Cap", "Purple Cap", "Best SR", "Boundaries")

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NeonGreen)
        }
        return
    }

    if (allBalls.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No match data yet", color = TextSecondary)
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedStat,
            containerColor = BackgroundDark,
            contentColor = AmberColor,
            edgePadding = 0.dp
        ) {
            statTabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedStat == index,
                    onClick = { selectedStat = index },
                    text = {
                        Text(
                            tab, fontSize = 12.sp,
                            fontWeight = if (selectedStat == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedStat == index) AmberColor else TextSecondary
                        )
                    }
                )
            }
        }

        when (selectedStat) {
            0 -> OrangeCapTab(allBalls, playerTeamMap, playerMap)
            1 -> PurpleCapTab(allBalls, playerTeamMap, playerMap)
            2 -> BestSRTab(allBalls, playerTeamMap, playerMap)
            3 -> MostBoundariesTab(allBalls, playerTeamMap, playerMap)
        }
    }
}

@Composable
fun OrangeCapTab(
    allBalls: List<Pair<Ball, String>>,
    playerTeamMap: Map<String, String>,
    playerMap: Map<String, Player>
) {
    data class BatStat(val player: Player, val team: String, val runs: Int, val balls: Int, val fours: Int, val sixes: Int, val avg: Double, val sr: Double)

    val stats = allBalls.groupBy { it.first.batsmanId ?: "" }
        .filter { it.key.isNotEmpty() && playerMap.containsKey(it.key) }
        .mapNotNull { (playerId, pBalls) ->
            val player = playerMap[playerId] ?: return@mapNotNull null
            val balls = pBalls.map { it.first }
            val runs = balls.sumOf { it.runsOffBat }
            if (runs == 0) return@mapNotNull null
            val ballsFaced = balls.count { it.extrasType != "wide" }.coerceAtLeast(1)
            val fours = balls.count { it.isBoundary && !it.isSix }
            val sixes = balls.count { it.isSix }
            val sr = runs * 100.0 / ballsFaced
            val groups = balls.groupBy { it.inningsId }
            val notOuts = groups.values.count { ib -> ib.none { it.isWicket && it.wicketType != "run_out" && it.wicketType != "retired_hurt" } }
            val dismissals = (groups.size - notOuts).coerceAtLeast(1)
            BatStat(player, playerTeamMap[playerId] ?: "", runs, ballsFaced, fours, sixes, runs.toDouble() / dismissals, sr)
        }.sortedByDescending { it.runs }.take(10)

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item {
            Text("🟠 Orange Cap", color = AmberColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(SurfaceCard).padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text("#", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.width(20.dp))
                Text("Player", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.weight(1f))
                Text("R", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("B", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("Avg", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                Text("SR", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                Text("4s", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.End)
                Text("6s", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.End)
            }
        }
        items(stats.withIndex().toList()) { (index, s) ->
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${index + 1}", color = if (index == 0) AmberColor else TextSecondary, fontSize = 12.sp, fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(s.player.fullName, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(s.team, color = TextSecondary, fontSize = 10.sp)
                }
                Text("${s.runs}", color = if (index == 0) AmberColor else TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("${s.balls}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("${"%.1f".format(s.avg)}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                Text("${"%.1f".format(s.sr)}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                Text("${s.fours}", color = NeonBlue, fontSize = 12.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.End)
                Text("${s.sixes}", color = NeonGreen, fontSize = 12.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.End)
            }
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        }
    }
}

@Composable
fun PurpleCapTab(
    allBalls: List<Pair<Ball, String>>,
    playerTeamMap: Map<String, String>,
    playerMap: Map<String, Player>
) {
    data class BowlStat(val player: Player, val team: String, val wickets: Int, val runs: Int, val economy: Double, val avg: Double, val sr: Double, val dots: Int, val maidens: Int)

    val stats = allBalls.groupBy { it.first.bowlerId ?: "" }
        .filter { it.key.isNotEmpty() && playerMap.containsKey(it.key) }
        .mapNotNull { (playerId, pBalls) ->
            val player = playerMap[playerId] ?: return@mapNotNull null
            val balls = pBalls.map { it.first }
            val legal = balls.count { it.extrasType != "wide" && it.extrasType != "no_ball" }
            val runs = balls.sumOf { b -> when (b.extrasType) { "bye", "leg_bye" -> 0; else -> b.runsOffBat + (b.extrasRuns ?: 0) } }
            val wickets = balls.count { it.isWicket && it.wicketType !in listOf("run_out", "obstructing", "retired_hurt", "timed_out") }
            if (wickets == 0) return@mapNotNull null
            val eco = if (legal > 0) runs * 6.0 / legal else 0.0
            val avg = runs.toDouble() / wickets
            val sr = if (wickets > 0) legal.toDouble() / wickets else 0.0
            val dots = balls.count { it.runsOffBat == 0 && it.extrasRuns == null && it.extrasType == null }
            val maidens = balls.groupBy { it.overNo }.values.count { ob ->
                ob.count { it.extrasType != "wide" && it.extrasType != "no_ball" } == 6 &&
                        ob.sumOf { it.runsOffBat + (it.extrasRuns ?: 0) } == 0
            }
            BowlStat(player, playerTeamMap[playerId] ?: "", wickets, runs, eco, avg, sr, dots, maidens)
        }.sortedByDescending { it.wickets }.take(10)

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item {
            Text("🟣 Purple Cap", color = PurpleColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(SurfaceCard).padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text("#", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.width(20.dp))
                Text("Player", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.weight(1f))
                Text("W", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.End)
                Text("R", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("Avg", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
                Text("Eco", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
                Text("SR", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("Dot", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.End)
            }
        }
        items(stats.withIndex().toList()) { (index, s) ->
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${index + 1}", color = if (index == 0) PurpleColor else TextSecondary, fontSize = 12.sp, modifier = Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(s.player.fullName, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(s.team, color = TextSecondary, fontSize = 10.sp)
                }
                Text("${s.wickets}", color = if (index == 0) PurpleColor else TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp), textAlign = TextAlign.End)
                Text("${s.runs}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("${"%.1f".format(s.avg)}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
                Text("${"%.2f".format(s.economy)}", color = when { s.economy < 6 -> NeonGreen; s.economy < 9 -> AmberColor; else -> ErrorRed }, fontSize = 12.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
                Text("${"%.1f".format(s.sr)}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("${s.dots}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.End)
            }
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        }
    }
}

@Composable
fun BestSRTab(
    allBalls: List<Pair<Ball, String>>,
    playerTeamMap: Map<String, String>,
    playerMap: Map<String, Player>
) {
    data class SRStat(val player: Player, val team: String, val runs: Int, val balls: Int, val sr: Double)

    val stats = allBalls.groupBy { it.first.batsmanId ?: "" }
        .filter { it.key.isNotEmpty() && playerMap.containsKey(it.key) }
        .mapNotNull { (playerId, pBalls) ->
            val player = playerMap[playerId] ?: return@mapNotNull null
            val balls = pBalls.map { it.first }
            val runs = balls.sumOf { it.runsOffBat }
            val ballsFaced = balls.count { it.extrasType != "wide" }
            if (ballsFaced < 10 || runs < 20) return@mapNotNull null
            SRStat(player, playerTeamMap[playerId] ?: "", runs, ballsFaced, runs * 100.0 / ballsFaced)
        }.sortedByDescending { it.sr }.take(10)

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Text("⚡ Best Strike Rate (min 20 runs, 10 balls)", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(stats.withIndex().toList()) { (index, s) ->
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceCard).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${index + 1}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(24.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(s.player.fullName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(s.team, color = TextSecondary, fontSize = 11.sp)
                }
                Text("${s.runs}(${s.balls})", color = TextPrimary, fontSize = 12.sp, modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
                Text("${"%.1f".format(s.sr)}", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
            }
        }
    }
}

@Composable
fun MostBoundariesTab(
    allBalls: List<Pair<Ball, String>>,
    playerTeamMap: Map<String, String>,
    playerMap: Map<String, Player>
) {
    var selectedBdry by remember { mutableIntStateOf(0) }
    val bdryTabs = listOf("Total", "Most 6s", "Most 4s")

    data class BdryStat(val player: Player, val team: String, val fours: Int, val sixes: Int)

    val allStats = allBalls.groupBy { it.first.batsmanId ?: "" }
        .filter { it.key.isNotEmpty() && playerMap.containsKey(it.key) }
        .mapNotNull { (pid, pb) ->
            val player = playerMap[pid] ?: return@mapNotNull null
            val balls = pb.map { it.first }
            BdryStat(player, playerTeamMap[pid] ?: "", balls.count { it.isBoundary && !it.isSix }, balls.count { it.isSix })
        }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedBdry, containerColor = BackgroundDark, contentColor = NeonGreen) {
            bdryTabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedBdry == index,
                    onClick = { selectedBdry = index },
                    text = { Text(tab, fontSize = 12.sp, color = if (selectedBdry == index) NeonGreen else TextSecondary) }
                )
            }
        }
        val sortedStats = when (selectedBdry) {
            1 -> allStats.sortedByDescending { it.sixes }
            2 -> allStats.sortedByDescending { it.fours }
            else -> allStats.sortedByDescending { it.fours + it.sixes }
        }.take(10)

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(sortedStats.withIndex().toList()) { (index, s) ->
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceCard).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${index + 1}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(24.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(s.player.fullName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(s.team, color = TextSecondary, fontSize = 11.sp)
                    }
                    Text("4s: ${s.fours}", color = NeonBlue, fontSize = 12.sp, modifier = Modifier.width(56.dp), textAlign = TextAlign.End)
                    Text("6s: ${s.sixes}", color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
                }
            }
        }
    }
}

// ── AWARDS TAB ───────────────────────────────────────────────

@Composable
fun TAwardsTab(
    allBalls: List<Pair<Ball, String>>,
    playerTeamMap: Map<String, String>,
    playerMap: Map<String, Player>,
    isLoading: Boolean
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NeonGreen)
        }
        return
    }

    if (allBalls.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No match data yet", color = TextSecondary)
        }
        return
    }

    data class MVPStat(val player: Player, val team: String, val score: Double, val runs: Int, val wickets: Int)

    val playerBatBalls = allBalls.groupBy { it.first.batsmanId ?: "" }
    val playerBowlBalls = allBalls.groupBy { it.first.bowlerId ?: "" }

    val mvpList = playerMap.keys.mapNotNull { pid ->
        val player = playerMap[pid] ?: return@mapNotNull null
        val batBalls = playerBatBalls[pid]?.map { it.first } ?: emptyList()
        val bowlBalls = playerBowlBalls[pid]?.map { it.first } ?: emptyList()
        val runs = batBalls.sumOf { it.runsOffBat }
        val balls = batBalls.count { it.extrasType != "wide" }.coerceAtLeast(1)
        val sr = runs * 100.0 / balls
        val legal = bowlBalls.count { it.extrasType != "wide" && it.extrasType != "no_ball" }
        val runsConceded = bowlBalls.sumOf { b -> when (b.extrasType) { "bye", "leg_bye" -> 0; else -> b.runsOffBat + (b.extrasRuns ?: 0) } }
        val wickets = bowlBalls.count { it.isWicket && it.wicketType !in listOf("run_out", "obstructing", "retired_hurt") }
        val eco = if (legal > 0) runsConceded * 6.0 / legal else 99.0
        if (runs == 0 && wickets == 0) return@mapNotNull null
        var score = runs.toDouble()
        score += when { sr > 150 -> 15.0; sr > 120 -> 8.0; else -> 0.0 }
        score += when { runs >= 100 -> 25.0; runs >= 50 -> 10.0; else -> 0.0 }
        score += wickets * 25.0
        if (wickets >= 5) score += 20.0
        if (legal >= 6) score += when { eco < 6 -> 15.0; eco < 7.5 -> 8.0; else -> 0.0 }
        MVPStat(player, playerTeamMap[pid] ?: "", score, runs, wickets)
    }.sortedByDescending { it.score }

    val inningsBatStats = allBalls.groupBy { it.first.inningsId }.mapValues { (_, balls) ->
        balls.groupBy { it.first.batsmanId ?: "" }.mapValues { (_, pb) -> pb.sumOf { it.first.runsOffBat } }
    }
    val highestScore = inningsBatStats.values.flatMap { it.entries }.maxByOrNull { it.value }
    val highestScorer = highestScore?.let { playerMap[it.key] }

    val inningsBowlStats = allBalls.groupBy { it.first.inningsId }.mapValues { (_, balls) ->
        balls.groupBy { it.first.bowlerId ?: "" }.mapValues { (_, pb) ->
            pb.count { it.first.isWicket && it.first.wicketType !in listOf("run_out", "obstructing", "retired_hurt") } to
                    pb.sumOf { b -> when (b.first.extrasType) { "bye", "leg_bye" -> 0; else -> b.first.runsOffBat + (b.first.extrasRuns ?: 0) } }
        }
    }
    val bestBowlingInnings = inningsBowlStats.values.flatMap { it.entries }.maxByOrNull { it.value.first }
    val bestBowler = bestBowlingInnings?.let { playerMap[it.key] }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        mvpList.firstOrNull()?.let { mvp ->
            item { AwardCard("🏆", "Tournament MVP", mvp.player.fullName, mvp.team, "${mvp.runs} runs | ${mvp.wickets} wickets", AmberColor) }
        }
        mvpList.getOrNull(1)?.let { pot ->
            item { AwardCard("⭐", "Player of Tournament", pot.player.fullName, pot.team, "${pot.runs} runs | ${pot.wickets} wickets", NeonGreen) }
        }
        highestScorer?.let { p ->
            item { AwardCard("🏏", "Best Batter", p.fullName, playerTeamMap[p.id] ?: "", "HS: ${highestScore?.value ?: 0} runs", NeonBlue) }
        }
        bestBowler?.let { p ->
            item { AwardCard("🎳", "Best Bowler", p.fullName, playerTeamMap[p.id] ?: "", "${bestBowlingInnings?.value?.first ?: 0}/${bestBowlingInnings?.value?.second ?: 0}", ErrorRed) }
        }
        item {
            Text("MVP Leaderboard", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Spacer(modifier = Modifier.height(6.dp))
        }
        items(mvpList.take(10).withIndex().toList()) { (index, s) ->
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceCard).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${index + 1}",
                    color = when (index) { 0 -> AmberColor; 2 -> Color(0xFFCD7F32); else -> TextSecondary },
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(s.player.fullName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(s.team, color = TextSecondary, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${"%.1f".format(s.score)} pts", color = AmberColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("${s.runs}R ${s.wickets}W", color = TextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun AwardCard(emoji: String, title: String, playerName: String, team: String, detail: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(emoji, fontSize = 28.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(playerName, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(team, color = TextSecondary, fontSize = 11.sp)
        }
        Text(detail, color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.End)
    }
}