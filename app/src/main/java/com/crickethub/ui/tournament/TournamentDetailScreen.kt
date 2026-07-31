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
import com.crickethub.data.repository.TournamentRepository
import kotlinx.coroutines.launch
import com.crickethub.ui.theme.*
import com.crickethub.export.*
import androidx.compose.ui.platform.LocalContext
import kotlin.math.log2

// ── FORMATS & HELPERS ────────────────────────────────────────

val TOURNAMENT_FORMATS = listOf(
    "Round Robin", "Double Round Robin", "Single Knockout",
    "Group + Knockout", "League + Playoffs", "Bilateral Series",
    "Tri-Series", "Custom Tournament"
)

data class KnockoutPlaceholder(
    val label: String,
    val team1Desc: String,
    val team2Desc: String,
    val matchNumber: Int
)

fun hasKnockoutStage(format: String): Boolean = format in listOf(
    "Single Knockout", "Group + Knockout", "League + Playoffs", "Tri-Series"
)

fun isKnockoutTitle(title: String?): Boolean {
    if (title == null) return false
    return title.startsWith("Final") || title.startsWith("Semi Final") ||
            title.startsWith("Quarter Final") || title.startsWith("Round of") ||
            title.startsWith("Qualifier") || title.startsWith("Eliminator")
}

fun leagueMatchCount(format: String, n: Int): Int = when (format) {
    "Round Robin", "Custom Tournament" -> if (n >= 2) n * (n - 1) / 2 else 0
    "Double Round Robin" -> if (n >= 2) n * (n - 1) else 0
    "Group + Knockout" -> {
        if (n < 4) { if (n >= 2) n * (n - 1) / 2 else 0 }
        else {
            val h = n / 2; val r = n - h
            h * (h - 1) / 2 + r * (r - 1) / 2
        }
    }
    "League + Playoffs" -> if (n >= 2) n * (n - 1) / 2 else 0
    "Bilateral Series" -> 0 // uses seriesMatches
    "Tri-Series" -> if (n >= 3) 6 else 0
    "Single Knockout" -> 0
    else -> 0
}

fun knockoutMatchCount(format: String, n: Int): Int = when (format) {
    "Single Knockout" -> if (n >= 2) n - 1 else 0
    "Group + Knockout" -> if (n >= 4) 3 else 0 // SF1, SF2, Final
    "League + Playoffs" -> if (n >= 4) 4 else 0 // Q1, Elim, Q2, Final
    "Tri-Series" -> if (n >= 3) 1 else 0 // Final
    else -> 0
}

fun totalMatches(format: String, n: Int, seriesCount: Int = 3): Int = when (format) {
    "Bilateral Series" -> if (n >= 2) seriesCount else 0
    "Single Knockout" -> if (n >= 2) n - 1 else 0
    else -> leagueMatchCount(format, n) + knockoutMatchCount(format, n)
}

/** First-round knockout matches that get created as real DB rows */
fun firstRoundKnockoutCount(n: Int): Int {
    if (n <= 1) return 0
    if (n == 2) return 1
    var bs = 1; while (bs < n) bs *= 2
    return (n - bs / 2) // playing teams / 2 but simplified: n - bracketSize/2
}

fun getKnockoutPlaceholders(format: String, n: Int, realMatchCount: Int): List<KnockoutPlaceholder> {
    val placeholders = mutableListOf<KnockoutPlaceholder>()
    var num = realMatchCount

    when (format) {
        "Single Knockout" -> {
            if (n <= 2) return emptyList() // no placeholders, final is a real match
            var bs = 1; while (bs < n) bs *= 2
            val totalRounds = log2(bs.toDouble()).toInt()
            // Skip round 1 (already real matches), generate rest
            var matchesInRound = bs / 4
            for (r in 1 until totalRounds) {
                val fromFinal = totalRounds - 1 - r
                val label = TournamentRepository.knockoutLabel(fromFinal)
                for (i in 0 until matchesInRound.coerceAtLeast(1)) {
                    num++
                    val suffix = if (matchesInRound > 1) " ${i + 1}" else ""
                    val desc1 = if (fromFinal == 0) "Winner SF 1" else "Winner ${i * 2 + 1}"
                    val desc2 = if (fromFinal == 0) "Winner SF 2" else "Winner ${i * 2 + 2}"
                    placeholders.add(KnockoutPlaceholder("$label$suffix", desc1, desc2, num))
                }
                matchesInRound = (matchesInRound / 2).coerceAtLeast(0)
                if (fromFinal == 0) break
            }
        }

        "Group + Knockout" -> {
            if (n >= 4) {
                num++; placeholders.add(KnockoutPlaceholder("Semi Final 1", "1st Group A", "2nd Group B", num))
                num++; placeholders.add(KnockoutPlaceholder("Semi Final 2", "1st Group B", "2nd Group A", num))
                num++; placeholders.add(KnockoutPlaceholder("Final", "Winner SF 1", "Winner SF 2", num))
            }
        }

        "League + Playoffs" -> {
            if (n >= 4) {
                num++; placeholders.add(KnockoutPlaceholder("Qualifier 1", "1st on Table", "2nd on Table", num))
                num++; placeholders.add(KnockoutPlaceholder("Eliminator", "3rd on Table", "4th on Table", num))
                num++; placeholders.add(KnockoutPlaceholder("Qualifier 2", "Loser Q1", "Winner Eliminator", num))
                num++; placeholders.add(KnockoutPlaceholder("Final", "Winner Q1", "Winner Q2", num))
            }
        }

        "Tri-Series" -> {
            if (n >= 3) {
                num++; placeholders.add(KnockoutPlaceholder("Final", "1st on Table", "2nd on Table", num))
            }
        }
    }
    return placeholders
}

fun formatDescription(format: String): String = when (format) {
    "Round Robin" -> "Every team plays every other team once.\nTop team on points wins."
    "Double Round Robin" -> "Every team plays every other team twice (home & away).\nMore matches, fairer standings."
    "Single Knockout" -> "Lose once and you're out.\nDirect elimination from first round to final."
    "Group + Knockout" -> "Teams split into 2 groups, play round robin.\nTop 2 from each group go to semi-finals."
    "League + Playoffs" -> "Full round robin league stage.\nTop 4 play IPL/PSL-style playoffs (Q1, Eliminator, Q2, Final)."
    "Bilateral Series" -> "Two teams play a fixed series.\nChoose 3, 5, or 7 match series."
    "Tri-Series" -> "Three teams, each pair plays twice.\nTop 2 play the final."
    "Custom Tournament" -> "Round robin with full control.\nManage fixtures as needed."
    else -> ""
}

fun formatStages(format: String, n: Int, series: Int = 3): List<String> = when (format) {
    "Round Robin", "Custom Tournament" -> listOf(
        "Round Robin: ${leagueMatchCount(format, n)} matches",
        "Winner: Most points"
    )
    "Double Round Robin" -> listOf(
        "Leg 1: ${n * (n - 1) / 2} matches",
        "Leg 2: ${n * (n - 1) / 2} matches",
        "Total: ${n * (n - 1)} matches"
    )
    "Single Knockout" -> {
        val lines = mutableListOf<String>()
        if (n >= 2) {
            var bs = 1; while (bs < n) bs *= 2
            val byes = bs - n
            if (byes > 0) lines.add("$byes team(s) get bye to next round")
            lines.add("Total: ${n - 1} matches to decide winner")
        }
        lines
    }
    "Group + Knockout" -> {
        val h = n / 2; val r = n - h
        listOf(
            "Group A: $h teams, ${h * (h - 1) / 2} matches",
            "Group B: $r teams, ${r * (r - 1) / 2} matches",
            "Semi Finals: 2 matches",
            "Final: 1 match",
            "Total: ${totalMatches(format, n)} matches"
        )
    }
    "League + Playoffs" -> listOf(
        "League: ${leagueMatchCount(format, n)} matches",
        "Qualifier 1: 1st vs 2nd",
        "Eliminator: 3rd vs 4th",
        "Qualifier 2: Loser Q1 vs Winner Elim",
        "Final: 1 match",
        "Total: ${totalMatches(format, n)} matches"
    )
    "Bilateral Series" -> listOf(
        "$series-match series between 2 teams",
        "Alternating home/away"
    )
    "Tri-Series" -> listOf(
        "League: 6 matches (each pair plays twice)",
        "Final: 1st vs 2nd",
        "Total: 7 matches"
    )
    else -> emptyList()
}

// ── MAIN SCREEN ──────────────────────────────────────────────

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
    var showExport by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
                val fmt = uiState.currentTournament?.format ?: ""
                Text(
                    "${uiState.tournamentTeams.size} teams • ${uiState.fixtures.size} matches" +
                            if (fmt.isNotBlank()) " • $fmt" else "",
                    fontSize = 12.sp, color = TextSecondary
                )
            }
            ExportButton(onClick = { showExport = true })
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
                0 -> TFixturesTab(uiState, tournamentId, onMatchClick, onViewScorecard, onViewAnalytics, viewModel)
                1 -> TPointsTab(uiState = uiState)
                2 -> TStatsTab(allBalls, playerTeamMap, playerMap, statsLoading)
                3 -> TAwardsTab(allBalls, playerTeamMap, playerMap, statsLoading)
            }
        }
    }

    ExportDialog(
        show = showExport,
        onDismiss = { showExport = false },
        title = "Export Tournament Report",
        onExport = { format ->
            val t = uiState.currentTournament!!
            val pointsRows = uiState.tournamentTeams
                .sortedWith(compareByDescending<TournamentTeam> { it.points }.thenByDescending { it.nrr })
                .map { tt ->
                    val name = uiState.teamDetails.find { it.id == tt.teamId }?.name ?: "Team"
                    PointsRow(name, tt.matchesPlayed, tt.wins, tt.losses, 0, 0, tt.nrr, tt.points)
                }
            val pNames = playerMap.mapValues { it.value.fullName }
            val data = TournamentReportData(t, uiState.teamDetails, uiState.fixtures, pointsRows, pNames)
            when (format) {
                ExportFormat.PDF -> TournamentReportGenerator.generatePdf(context, data)
                ExportFormat.CSV -> TournamentReportGenerator.generateCsv(context, data)
            }
        }
    )
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
    // Default to tournament's saved format
    val savedFormat = uiState.currentTournament?.format
    var selectedFormat by remember(savedFormat) {
        mutableStateOf(
            if (savedFormat != null && savedFormat in TOURNAMENT_FORMATS) savedFormat else "Round Robin"
        )
    }
    var showTeamPicker by remember { mutableStateOf(false) }
    var showFormatInfo by remember { mutableStateOf(false) }
    var seriesCount by remember { mutableIntStateOf(3) }
    val fixturesGenerated = uiState.fixtures.isNotEmpty()
    val teamCount = uiState.tournamentTeams.size

    // Stage sub-tab for formats with league + knockout
    var stageTab by remember { mutableIntStateOf(0) } // 0=All, 1=League, 2=Knockout
    val showStageTabs = fixturesGenerated && hasKnockoutStage(selectedFormat)

    val leagueFixtures = uiState.fixtures.filter { !isKnockoutTitle(it.title) }
    val knockoutFixtures = uiState.fixtures.filter { isKnockoutTitle(it.title) }
    val knockoutPlaceholders = if (fixturesGenerated) {
        getKnockoutPlaceholders(selectedFormat, teamCount, uiState.fixtures.size)
    } else emptyList()

    // Format info dialog
    if (showFormatInfo) {
        AlertDialog(
            onDismissRequest = { showFormatInfo = false },
            containerColor = SurfaceCard,
            title = { Text("Format: $selectedFormat", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.heightIn(max = 500.dp)) {
                    item { Text(formatDescription(selectedFormat), color = TextSecondary, fontSize = 13.sp, lineHeight = 20.sp) }
                    item { HorizontalDivider(color = BorderColor) }
                    item {
                        Text("How it works:", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        formatStages(selectedFormat, teamCount.coerceAtLeast(2), seriesCount).forEach { stage ->
                            Row(modifier = Modifier.padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("•", color = NeonGreen, fontSize = 13.sp)
                                Text(stage, color = TextPrimary, fontSize = 13.sp)
                            }
                        }
                    }
                    item { HorizontalDivider(color = BorderColor) }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .background(NeonGreen.copy(alpha = 0.1f)).border(1.dp, NeonGreen, RoundedCornerShape(8.dp)).padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("With $teamCount teams", color = TextSecondary, fontSize = 12.sp)
                                Text("Total Matches: ${totalMatches(selectedFormat, teamCount, seriesCount)}", color = NeonGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("🏏", fontSize = 24.sp)
                        }
                    }
                    item {
                        HorizontalDivider(color = BorderColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("All Formats ($teamCount teams):", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        TOURNAMENT_FORMATS.forEach { format ->
                            val isCurrent = format == selectedFormat
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(6.dp))
                                    .background(if (isCurrent) NeonGreen.copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable { selectedFormat = format }.padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(format, color = if (isCurrent) NeonGreen else TextSecondary, fontSize = 12.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal)
                                Text("${totalMatches(format, teamCount, seriesCount)} matches", color = if (isCurrent) NeonGreen else TextSecondary, fontSize = 12.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showFormatInfo = false }, colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)) {
                    Text("Got it", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Setup section (before fixtures generated, only after data has loaded) ──
        if (!fixturesGenerated && uiState.detailLoaded) {
            // Teams
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceCard).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Teams (${uiState.tournamentTeams.size})", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        OutlinedButton(
                            onClick = { showTeamPicker = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                            shape = RoundedCornerShape(8.dp), modifier = Modifier.height(34.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) { Text("+ Add Team", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                    if (uiState.tournamentTeams.isEmpty()) {
                        Text("No teams added yet — tap + Add Team", color = TextSecondary, fontSize = 12.sp)
                    } else {
                        uiState.tournamentTeams.forEach { tt ->
                            val teamName = uiState.teamDetails.find { it.id == tt.teamId }?.name ?: "Team"
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(BackgroundDark).padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
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
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceCard).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Select Format", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        TextButton(
                            onClick = { showFormatInfo = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = NeonBlue),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) { Text("ℹ How formats work", fontSize = 12.sp) }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        TOURNAMENT_FORMATS.chunked(2).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { format ->
                                    val isSelected = selectedFormat == format
                                    val matches = totalMatches(format, teamCount, seriesCount)
                                    Column(
                                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) NeonGreen.copy(alpha = 0.2f) else BackgroundDark)
                                            .border(1.dp, if (isSelected) NeonGreen else BorderColor, RoundedCornerShape(8.dp))
                                            .clickable { selectedFormat = format }.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(format, color = if (isSelected) NeonGreen else TextSecondary, fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center)
                                        if (teamCount >= 2) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("$matches matches", color = if (isSelected) NeonGreen.copy(alpha = 0.8f) else TextSecondary.copy(alpha = 0.6f),
                                                fontSize = 10.sp, textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // Bilateral series count selector
                    if (selectedFormat == "Bilateral Series") {
                        Text("Series Length", color = TextSecondary, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(3, 5, 7).forEach { count ->
                                val isSel = seriesCount == count
                                Box(
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) NeonGreen.copy(alpha = 0.2f) else BackgroundDark)
                                        .border(1.dp, if (isSel) NeonGreen else BorderColor, RoundedCornerShape(8.dp))
                                        .clickable { seriesCount = count }.padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text("$count Matches", color = if (isSel) NeonGreen else TextSecondary, fontSize = 12.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }

                    // Validation warnings
                    if (selectedFormat == "Bilateral Series" && teamCount != 2) {
                        Text("⚠ Bilateral Series requires exactly 2 teams", color = AmberColor, fontSize = 11.sp)
                    }
                    if (selectedFormat == "Tri-Series" && teamCount != 3) {
                        Text("⚠ Tri-Series requires exactly 3 teams", color = AmberColor, fontSize = 11.sp)
                    }
                    if (selectedFormat == "Group + Knockout" && teamCount < 4) {
                        Text("⚠ Group + Knockout needs at least 4 teams", color = AmberColor, fontSize = 11.sp)
                    }
                    if (selectedFormat == "League + Playoffs" && teamCount < 4) {
                        Text("⚠ League + Playoffs needs at least 4 teams for playoffs", color = AmberColor, fontSize = 11.sp)
                    }

                    // Preview
                    if (teamCount >= 2) {
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .background(NeonGreen.copy(alpha = 0.08f)).border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(10.dp)
                        ) {
                            Column {
                                Text("$selectedFormat • $teamCount teams • ${totalMatches(selectedFormat, teamCount, seriesCount)} matches",
                                    color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(formatDescription(selectedFormat).lines().first(), color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }

                    val canGenerate = when (selectedFormat) {
                        "Bilateral Series" -> teamCount == 2
                        "Tri-Series" -> teamCount == 3
                        else -> teamCount >= 2
                    }

                    Button(
                        onClick = { viewModel.generateFixtures(tournamentId, selectedFormat, seriesCount) },
                        enabled = canGenerate && !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating...", color = Color.Black, fontWeight = FontWeight.Bold)
                        } else {
                            Text(
                                if (!canGenerate) when (selectedFormat) {
                                    "Bilateral Series" -> "Need exactly 2 teams"
                                    "Tri-Series" -> "Need exactly 3 teams"
                                    else -> "Add at least 2 teams first"
                                } else "Generate ${totalMatches(selectedFormat, teamCount, seriesCount)} Fixtures",
                                color = Color.Black, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            // ── Schedule generated summary ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(NeonGreen.copy(alpha = 0.1f)).border(1.dp, NeonGreen, RoundedCornerShape(10.dp)).padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("✅ Schedule Generated", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        val fmt = uiState.currentTournament?.format ?: selectedFormat
                        val total = uiState.fixtures.size + knockoutPlaceholders.size
                        Text("$fmt • ${uiState.tournamentTeams.size} teams • $total matches", color = TextSecondary, fontSize = 12.sp)
                    }
                    Text("🏏", fontSize = 24.sp)
                }
            }

            // ── Stage sub-tabs ──
            if (showStageTabs) {
                item {
                    val stageTabs = listOf("All", "League", "Knockout")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        stageTabs.forEachIndexed { index, label ->
                            val isSel = stageTab == index
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) NeonGreen.copy(alpha = 0.2f) else SurfaceCard)
                                    .border(1.dp, if (isSel) NeonGreen else BorderColor, RoundedCornerShape(8.dp))
                                    .clickable { stageTab = index }.padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                val count = when (index) {
                                    1 -> leagueFixtures.size
                                    2 -> knockoutFixtures.size + knockoutPlaceholders.size
                                    else -> uiState.fixtures.size + knockoutPlaceholders.size
                                }
                                Text("$label ($count)", color = if (isSel) NeonGreen else TextSecondary, fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
        }

        // ── Fixture cards ──
        val displayFixtures = when {
            !fixturesGenerated -> emptyList()
            !showStageTabs -> uiState.fixtures
            stageTab == 1 -> leagueFixtures
            stageTab == 2 -> knockoutFixtures
            else -> uiState.fixtures
        }

        if (displayFixtures.isNotEmpty()) {
            // Group by stage label
            val grouped = displayFixtures.groupBy { it.title ?: "Match" }
            grouped.forEach { (stage, matches) ->
                item {
                    Text(stage, color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp))
                }
                items(matches) { match ->
                    TFixtureCard(match, uiState.teamDetails, onMatchClick, onViewScorecard, onViewAnalytics)
                }
            }
        }

        // ── Knockout placeholders ──
        val showPlaceholders = fixturesGenerated && (stageTab == 0 || stageTab == 2)
        if (showPlaceholders && knockoutPlaceholders.isNotEmpty()) {
            item {
                Text("🏆 Knockout Stage", color = AmberColor, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp))
                Text("Teams will be filled from standings", color = TextSecondary, fontSize = 11.sp)
            }
            items(knockoutPlaceholders) { placeholder ->
                KnockoutPlaceholderCard(placeholder)
            }
        }
    }

    // Team picker dialog
    if (showTeamPicker) {
        val availableTeams = uiState.allTeams.filter { team -> uiState.tournamentTeams.none { it.teamId == team.id } }
        AlertDialog(
            onDismissRequest = { showTeamPicker = false },
            containerColor = SurfaceCard,
            title = { Text("Add Team", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                if (availableTeams.isEmpty()) {
                    Text("No teams available. Create teams first.", color = TextSecondary)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.heightIn(max = 400.dp)) {
                        items(availableTeams) { team ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                    .background(BackgroundDark).border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.addTeamToTournament(tournamentId, team.id); showTeamPicker = false }.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
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
            dismissButton = { TextButton(onClick = { showTeamPicker = false }) { Text("Cancel", color = TextSecondary) } }
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
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard).border(1.dp, BorderColor, RoundedCornerShape(12.dp)).padding(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Match ${match.matchNumber ?: ""}", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(match.status.uppercase(), color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text("${match.matchType} • ${match.totalOvers} ov", color = TextSecondary, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
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
                onClick = { onMatchClick(match.id) }, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen), shape = RoundedCornerShape(8.dp)
            ) { Text("Score", fontSize = 11.sp) }
            OutlinedButton(
                onClick = { onViewScorecard(match.id) }, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonBlue), shape = RoundedCornerShape(8.dp)
            ) { Text("Live", fontSize = 11.sp) }
            OutlinedButton(
                onClick = { onViewAnalytics(match.id) }, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleColor), shape = RoundedCornerShape(8.dp)
            ) { Text("Stats", fontSize = 11.sp) }
        }
    }
}

// ── KNOCKOUT PLACEHOLDER CARD ────────────────────────────────

@Composable
fun KnockoutPlaceholderCard(placeholder: KnockoutPlaceholder) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard.copy(alpha = 0.6f))
            .border(1.dp, AmberColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Match ${placeholder.matchNumber}", color = AmberColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(placeholder.label, color = AmberColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(placeholder.team1Desc, color = TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text("vs", color = TextSecondary.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
            Text(placeholder.team2Desc, color = TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text("🔒 Teams decided from standings", color = TextSecondary.copy(alpha = 0.5f), fontSize = 10.sp)
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
            val qualifyColor = when {
                uiState.currentTournament?.format == "League + Playoffs" && index < 4 -> NeonGreen
                uiState.currentTournament?.format == "Group + Knockout" && index < 4 -> NeonGreen
                else -> if (index == 0) NeonGreen else TextSecondary
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${index + 1}", color = qualifyColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
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
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = NeonGreen) }
        return
    }
    if (allBalls.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No match data yet", color = TextSecondary) }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selectedStat, containerColor = BackgroundDark, contentColor = AmberColor, edgePadding = 0.dp) {
            statTabs.forEachIndexed { index, tab ->
                Tab(selected = selectedStat == index, onClick = { selectedStat = index },
                    text = { Text(tab, fontSize = 12.sp, fontWeight = if (selectedStat == index) FontWeight.Bold else FontWeight.Normal, color = if (selectedStat == index) AmberColor else TextSecondary) })
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
fun OrangeCapTab(allBalls: List<Pair<Ball, String>>, playerTeamMap: Map<String, String>, playerMap: Map<String, Player>) {
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
fun PurpleCapTab(allBalls: List<Pair<Ball, String>>, playerTeamMap: Map<String, String>, playerMap: Map<String, Player>) {
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
            val dots = balls.count { it.extrasType != "wide" && it.extrasType != "no_ball" && it.runsOffBat == 0 && (it.extrasRuns ?: 0) == 0 }
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
fun BestSRTab(allBalls: List<Pair<Ball, String>>, playerTeamMap: Map<String, String>, playerMap: Map<String, Player>) {
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
fun MostBoundariesTab(allBalls: List<Pair<Ball, String>>, playerTeamMap: Map<String, String>, playerMap: Map<String, Player>) {
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
                Tab(selected = selectedBdry == index, onClick = { selectedBdry = index },
                    text = { Text(tab, fontSize = 12.sp, color = if (selectedBdry == index) NeonGreen else TextSecondary) })
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
fun TAwardsTab(allBalls: List<Pair<Ball, String>>, playerTeamMap: Map<String, String>, playerMap: Map<String, Player>, isLoading: Boolean) {
    if (isLoading) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = NeonGreen) }; return }
    if (allBalls.isEmpty()) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No match data yet", color = TextSecondary) }; return }

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
            item { AwardCard("🏆", "Player of the Tournament", mvp.player.fullName, mvp.team, "${"%.1f".format(mvp.score)} pts • ${mvp.runs} runs | ${mvp.wickets} wickets", AmberColor) }
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
                Text("${index + 1}", color = when (index) { 0 -> AmberColor; 2 -> Color(0xFFCD7F32); else -> TextSecondary }, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
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
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f)).border(1.dp, color, RoundedCornerShape(12.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
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