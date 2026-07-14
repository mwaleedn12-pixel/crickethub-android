package com.crickethub.ui.match.analytics


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import com.crickethub.ui.components.CricketAnimatedBackground
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crickethub.data.model.Ball
import com.crickethub.data.model.Team
import com.crickethub.data.remote.SupabaseClient
import com.crickethub.data.repository.MatchRepository
import com.crickethub.data.repository.ScoringRepository
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import com.crickethub.ui.theme.*

@Composable
fun AnalyticsScreen(
    matchId: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var inn1Balls by remember { mutableStateOf<List<Ball>>(emptyList()) }
    var inn2Balls by remember { mutableStateOf<List<Ball>>(emptyList()) }
    var team1Name by remember { mutableStateOf("Team 1") }
    var team2Name by remember { mutableStateOf("Team 2") }
    var inn1BattingTeamName by remember { mutableStateOf("Team 1") }
    var inn2BattingTeamName by remember { mutableStateOf("Team 2") }
    var totalOvers by remember { mutableStateOf(20) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf("Batting", "Bowling", "Team", "Summary")

    LaunchedEffect(matchId) {
        scope.launch {
            try {
                val matchRepo = MatchRepository()
                val scoringRepo = ScoringRepository()
                val match = matchRepo.getMatchById(matchId)
                totalOvers = match?.totalOvers ?: 20

                val allInnings = scoringRepo.getInningsByMatch(matchId)
                val innings1 = allInnings.firstOrNull { it.inningsNo == 1 }
                val innings2 = allInnings.firstOrNull { it.inningsNo == 2 }

                inn1Balls = if (innings1 != null) scoringRepo.getBallsByInnings(innings1.id) else emptyList()
                inn2Balls = if (innings2 != null) scoringRepo.getBallsByInnings(innings2.id) else emptyList()

                // Team names
                val t1Id = match?.team1Id ?: ""
                val t2Id = match?.team2Id ?: ""
                val battingFirstId = match?.battingFirstId ?: t1Id

                team1Name = try {
                    com.crickethub.data.remote.SupabaseClient.client.postgrest["teams"]
                        .select { filter { eq("id", t1Id) } }
                        .decodeSingleOrNull<com.crickethub.data.model.Team>()?.name ?: "Team 1"
                } catch (e: Exception) { "Team 1" }

                team2Name = try {
                    com.crickethub.data.remote.SupabaseClient.client.postgrest["teams"]
                        .select { filter { eq("id", t2Id) } }
                        .decodeSingleOrNull<com.crickethub.data.model.Team>()?.name ?: "Team 2"
                } catch (e: Exception) { "Team 2" }

                inn1BattingTeamName = if (innings1?.battingTeamId == t1Id) team1Name else team2Name
                inn2BattingTeamName = if (innings2?.battingTeamId == t1Id) team1Name else team2Name

                isLoading = false
            } catch (e: Exception) {
                isLoading = false
            }
        }
    }

    CricketAnimatedBackground(modifier = Modifier.fillMaxSize()) {
Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text("Analytics", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
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

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonGreen)
            }
        } else {
            when (selectedTab) {
                0 -> BattingAnalyticsTab(inn1Balls, inn2Balls, inn1BattingTeamName, inn2BattingTeamName, totalOvers)
                1 -> BowlingAnalyticsTab(inn1Balls, inn2Balls, inn1BattingTeamName, inn2BattingTeamName, totalOvers)
                2 -> TeamAnalyticsTab(inn1Balls, inn2Balls, inn1BattingTeamName, inn2BattingTeamName, totalOvers)
                3 -> SummaryAnalyticsTab(inn1Balls, inn2Balls, inn1BattingTeamName, inn2BattingTeamName, totalOvers)
            }
        }
    }
}
} // CricketAnimatedBackground

// ── BATTING TAB ──────────────────────────────────────────────

@Composable
fun BattingAnalyticsTab(
    inn1Balls: List<Ball>, inn2Balls: List<Ball>,
    inn1Name: String, inn2Name: String, totalOvers: Int
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dismissal Types
        item {
            AnalyticsCard("Dismissal Types") {
                val wickets1 = inn1Balls.filter { it.isWicket && it.wicketType != "retired_hurt" }
                val wickets2 = inn2Balls.filter { it.isWicket && it.wicketType != "retired_hurt" }
                val allWickets = (wickets1 + wickets2)
                val dismissalGroups = allWickets.groupBy {
                    it.wicketType?.replace("_", " ")?.replaceFirstChar { c -> c.uppercase() } ?: "Unknown"
                }
                if (dismissalGroups.isEmpty()) {
                    Text("No wickets yet", color = TextSecondary, fontSize = 12.sp)
                } else {
                    dismissalGroups.forEach { (type, balls) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(type, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .height(20.dp)
                                    .width((balls.size * 40).dp.coerceAtMost(120.dp))
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ErrorRed.copy(alpha = 0.7f))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${balls.size}", color = ErrorRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Boundary Timeline
        item {
            AnalyticsCard("Boundary Timeline") {
                val allBalls = inn1Balls + inn2Balls
                val boundaries = allBalls.filter { it.isBoundary || it.isSix }
                if (boundaries.isEmpty()) {
                    Text("No boundaries yet", color = TextSecondary, fontSize = 12.sp)
                } else {
                    // Over by over boundaries
                    val overGroups = allBalls.groupBy { it.overNo }
                    val maxOver = (overGroups.keys.maxOrNull() ?: 0) + 1
                    Row(
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        (0 until maxOver).forEach { over ->
                            val overBalls = overGroups[over] ?: emptyList()
                            val fours = overBalls.count { it.isBoundary && !it.isSix }
                            val sixes = overBalls.count { it.isSix }
                            val total = fours + sixes
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                if (total > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height((total * 16).dp.coerceAtMost(64.dp))
                                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                            .background(if (sixes > fours) NeonGreen else NeonBlue)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(10.dp).background(NeonBlue, RoundedCornerShape(2.dp)))
                            Text("4s", color = TextSecondary, fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(10.dp).background(NeonGreen, RoundedCornerShape(2.dp)))
                            Text("6s", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 4s and 6s breakdown
        item {
            AnalyticsCard("Fours & Sixes") {
                listOf(inn1Balls to inn1Name, inn2Balls to inn2Name).forEach { (balls, name) ->
                    if (balls.isEmpty()) return@forEach
                    val fours = balls.count { it.isBoundary && !it.isSix }
                    val sixes = balls.count { it.isSix }
                    val boundaryRuns = (fours * 4) + (sixes * 6)
                    val totalRuns = balls.sumOf { it.runsOffBat }
                    val boundaryPct = if (totalRuns > 0) (boundaryRuns * 100.0 / totalRuns) else 0.0

                    Text(name, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatBox("4s", "$fours", NeonBlue, Modifier.weight(1f))
                        StatBox("6s", "$sixes", NeonGreen, Modifier.weight(1f))
                        StatBox("Runs", "$boundaryRuns", AmberColor, Modifier.weight(1f))
                        StatBox("Bdry%", "${"%.1f".format(boundaryPct)}%", PurpleColor, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Dot Ball %
        item {
            AnalyticsCard("Dot Ball Analysis") {
                listOf(inn1Balls to inn1Name, inn2Balls to inn2Name).forEach { (balls, name) ->
                    if (balls.isEmpty()) return@forEach
                    val legalBalls = balls.filter { it.extrasType != "wide" }
                    val dotBalls = legalBalls.count { it.runsOffBat == 0 && it.extrasRuns == null }
                    val dotPct = if (legalBalls.isNotEmpty()) dotBalls * 100.0 / legalBalls.size else 0.0

                    Text(name, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatBox("Dot Balls", "$dotBalls", TextSecondary, Modifier.weight(1f))
                        StatBox("Dot %", "${"%.1f".format(dotPct)}%", ErrorRed, Modifier.weight(1f))
                        StatBox("Legal Balls", "${legalBalls.size}", TextPrimary, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Phase Performance
        item {
            AnalyticsCard("Phase Performance") {
                listOf(inn1Balls to inn1Name, inn2Balls to inn2Name).forEach { (balls, name) ->
                    if (balls.isEmpty()) return@forEach
                    Text(name, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    val phases = listOf("powerplay", "middle", "death")
                    val phaseLabels = listOf("Powerplay", "Middle", "Death")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        phases.forEachIndexed { i, phase ->
                            val phaseBalls = balls.filter { it.inningsPhase == phase }
                            val runs = phaseBalls.sumOf { b ->
                                when {
                                    b.extrasType == "wide" -> (b.extrasRuns ?: 1) + b.runsOffBat
                                    b.extrasType == "no_ball" -> 1 + b.runsOffBat + (b.extrasRuns ?: 0)
                                    else -> b.runsOffBat + (b.extrasRuns ?: 0)
                                }
                            }
                            val legal = phaseBalls.count { it.extrasType != "wide" && it.extrasType != "no_ball" }
                            val wkts = phaseBalls.count { it.isWicket && it.wicketType != "retired_hurt" }
                            val rr = if (legal > 0) runs * 6.0 / legal else 0.0
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSystemInDarkTheme()) Color(0xFF030F08) else Color(0xFFF0FDF8))
                                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(phaseLabels[i], color = TextSecondary, fontSize = 10.sp, textAlign = TextAlign.Center)
                                Text("$runs/$wkts", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                Text("RR: ${"%.1f".format(rr)}", color = NeonGreen, fontSize = 11.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

// ── BOWLING TAB ──────────────────────────────────────────────

@Composable
fun BowlingAnalyticsTab(
    inn1Balls: List<Ball>, inn2Balls: List<Ball>,
    inn1Name: String, inn2Name: String, totalOvers: Int
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Economy Rate by Over
        item {
            AnalyticsCard("Economy Rate by Over") {
                listOf(inn1Balls to inn1Name, inn2Balls to inn2Name).forEach { (balls, name) ->
                    if (balls.isEmpty()) return@forEach
                    val overGroups = balls.groupBy { it.overNo }.toSortedMap()
                    if (overGroups.isEmpty()) return@forEach

                    Text(name, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    val maxEconomy = 18.0
                    Row(
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        overGroups.forEach { (_, overBalls) ->
                            val runs = overBalls.sumOf { b ->
                                when {
                                    b.extrasType == "wide" -> (b.extrasRuns ?: 1) + b.runsOffBat
                                    b.extrasType == "no_ball" -> 1 + b.runsOffBat + (b.extrasRuns ?: 0)
                                    else -> b.runsOffBat + (b.extrasRuns ?: 0)
                                }
                            }
                            val legal = overBalls.count { it.extrasType != "wide" && it.extrasType != "no_ball" }
                            val eco = if (legal > 0) runs * 6.0 / legal else 0.0
                            val heightFraction = (eco / maxEconomy).coerceIn(0.0, 1.0)
                            val barColor = when {
                                eco >= 12 -> ErrorRed; eco >= 9 -> AmberColor
                                eco >= 6 -> NeonGreen; else -> NeonBlue
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height((heightFraction * 70).dp.coerceAtLeast(4.dp))
                                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                        .background(barColor)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Wickets Timeline
        item {
            AnalyticsCard("Wickets Timeline") {
                listOf(inn1Balls to inn1Name, inn2Balls to inn2Name).forEach { (balls, name) ->
                    if (balls.isEmpty()) return@forEach
                    val wickets = balls.filter { it.isWicket && it.wicketType != "retired_hurt" }
                    Text(name, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (wickets.isEmpty()) {
                        Text("No wickets", color = TextSecondary, fontSize = 12.sp)
                    } else {
                        wickets.forEachIndexed { index, ball ->
                            val runsSoFar = balls
                                .filter { b -> b.overNo < ball.overNo || (b.overNo == ball.overNo && b.ballNo <= ball.ballNo) }
                                .sumOf { b ->
                                    when {
                                        b.extrasType == "wide" -> (b.extrasRuns ?: 1) + b.runsOffBat
                                        b.extrasType == "no_ball" -> 1 + b.runsOffBat + (b.extrasRuns ?: 0)
                                        else -> b.runsOffBat + (b.extrasRuns ?: 0)
                                    }
                                }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${index + 1}/${index + 1}",
                                    color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(32.dp)
                                )
                                Text(
                                    "${ball.overNo}.${ball.ballNo} ov",
                                    color = TextSecondary, fontSize = 12.sp,
                                    modifier = Modifier.width(56.dp)
                                )
                                Text(
                                    "$runsSoFar runs",
                                    color = TextPrimary, fontSize = 12.sp,
                                    modifier = Modifier.width(64.dp)
                                )
                                Text(
                                    ball.wicketType?.replace("_", " ") ?: "out",
                                    color = TextSecondary, fontSize = 11.sp,
                                    modifier = Modifier.weight(1f), textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Extras Distribution
        item {
            AnalyticsCard("Extras Distribution") {
                listOf(inn1Balls to inn1Name, inn2Balls to inn2Name).forEach { (balls, name) ->
                    if (balls.isEmpty()) return@forEach
                    val wides = balls.count { it.extrasType == "wide" }
                    val noBalls = balls.count { it.extrasType == "no_ball" }
                    val byes = balls.count { it.extrasType == "bye" }
                    val legByes = balls.count { it.extrasType == "leg_bye" }
                    val totalExtras = balls.sumOf { it.extrasRuns ?: 0 } + wides + noBalls

                    Text(name, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatBox("Wides", "$wides", AmberColor, Modifier.weight(1f))
                        StatBox("NB", "$noBalls", ErrorRed, Modifier.weight(1f))
                        StatBox("Byes", "$byes", NeonBlue, Modifier.weight(1f))
                        StatBox("LB", "$legByes", PurpleColor, Modifier.weight(1f))
                        StatBox("Total", "$totalExtras", TextPrimary, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

// ── TEAM TAB ─────────────────────────────────────────────────

@Composable
fun TeamAnalyticsTab(
    inn1Balls: List<Ball>, inn2Balls: List<Ball>,
    inn1Name: String, inn2Name: String, totalOvers: Int
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Worm Graph
        item {
            AnalyticsCard("Worm Graph (Cumulative Runs)") {
                WormGraph(
                    inn1Balls = inn1Balls,
                    inn2Balls = inn2Balls,
                    inn1Name = inn1Name,
                    inn2Name = inn2Name,
                    totalOvers = totalOvers
                )
            }
        }

        // Manhattan Chart
        item {
            AnalyticsCard("Manhattan Chart (Runs per Over)") {
                ManhattanChart(
                    inn1Balls = inn1Balls,
                    inn2Balls = inn2Balls,
                    inn1Name = inn1Name,
                    inn2Name = inn2Name,
                    totalOvers = totalOvers
                )
            }
        }

        // Partnership Graph
        item {
            AnalyticsCard("Partnership Graph") {
                PartnershipGraph(
                    inn1Balls = inn1Balls,
                    inn2Balls = inn2Balls,
                    inn1Name = inn1Name,
                    inn2Name = inn2Name
                )
            }
        }

        // Win Probability
        item {
            AnalyticsCard("Win Probability") {
                WinProbabilityGraph(
                    inn1Balls = inn1Balls,
                    inn2Balls = inn2Balls,
                    inn1Name = inn1Name,
                    inn2Name = inn2Name,
                    totalOvers = totalOvers
                )
            }
        }
    }
}

// ── SUMMARY TAB ──────────────────────────────────────────────

@Composable
fun SummaryAnalyticsTab(
    inn1Balls: List<Ball>, inn2Balls: List<Ball>,
    inn1Name: String, inn2Name: String, totalOvers: Int
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Innings Summary
        item {
            AnalyticsCard("Innings Summary") {
                listOf(inn1Balls to inn1Name, inn2Balls to inn2Name).forEach { (balls, name) ->
                    if (balls.isEmpty()) return@forEach
                    val runs = balls.sumOf { b ->
                        when {
                            b.extrasType == "wide" -> (b.extrasRuns ?: 1) + b.runsOffBat
                            b.extrasType == "no_ball" -> 1 + b.runsOffBat + (b.extrasRuns ?: 0)
                            else -> b.runsOffBat + (b.extrasRuns ?: 0)
                        }
                    }
                    val wickets = balls.count { it.isWicket && it.wicketType != "retired_hurt" }
                    val legalBalls = balls.count { it.extrasType != "wide" && it.extrasType != "no_ball" }
                    val overs = "${legalBalls / 6}.${legalBalls % 6}"
                    val rr = if (legalBalls > 0) runs * 6.0 / legalBalls else 0.0
                    val fours = balls.count { it.isBoundary && !it.isSix }
                    val sixes = balls.count { it.isSix }
                    val dotBalls = balls.filter { it.extrasType != "wide" }.count {
                        it.runsOffBat == 0 && it.extrasRuns == null
                    }
                    val extras = balls.sumOf { it.extrasRuns ?: 0 } +
                            balls.count { it.extrasType == "wide" } +
                            balls.count { it.extrasType == "no_ball" }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSystemInDarkTheme()) Color(0xFF030F08) else Color(0xFFF0FDF8))
                            .padding(12.dp)
                    ) {
                        Text(name, color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Score", color = TextSecondary, fontSize = 12.sp)
                            Text("$runs/$wickets ($overs ov)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        SummaryRow("Run Rate", "${"%.2f".format(rr)}")
                        SummaryRow("Fours", "$fours")
                        SummaryRow("Sixes", "$sixes")
                        SummaryRow("Dot Balls", "$dotBalls")
                        SummaryRow("Extras", "$extras")
                        val boundaryRuns = (fours * 4) + (sixes * 6)
                        val totalRunsFromBat = balls.sumOf { it.runsOffBat }
                        val bPct = if (totalRunsFromBat > 0) boundaryRuns * 100.0 / totalRunsFromBat else 0.0
                        SummaryRow("Boundary %", "${"%.1f".format(bPct)}%")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Head to Head
        if (inn1Balls.isNotEmpty() && inn2Balls.isNotEmpty()) {
            item {
                AnalyticsCard("Head to Head") {
                    val inn1Runs = inn1Balls.sumOf { b ->
                        when {
                            b.extrasType == "wide" -> (b.extrasRuns ?: 1) + b.runsOffBat
                            b.extrasType == "no_ball" -> 1 + b.runsOffBat + (b.extrasRuns ?: 0)
                            else -> b.runsOffBat + (b.extrasRuns ?: 0)
                        }
                    }
                    val inn2Runs = inn2Balls.sumOf { b ->
                        when {
                            b.extrasType == "wide" -> (b.extrasRuns ?: 1) + b.runsOffBat
                            b.extrasType == "no_ball" -> 1 + b.runsOffBat + (b.extrasRuns ?: 0)
                            else -> b.runsOffBat + (b.extrasRuns ?: 0)
                        }
                    }
                    val inn1Wkts = inn1Balls.count { it.isWicket && it.wicketType != "retired_hurt" }
                    val inn2Wkts = inn2Balls.count { it.isWicket && it.wicketType != "retired_hurt" }
                    val inn1Fours = inn1Balls.count { it.isBoundary && !it.isSix }
                    val inn2Fours = inn2Balls.count { it.isBoundary && !it.isSix }
                    val inn1Sixes = inn1Balls.count { it.isSix }
                    val inn2Sixes = inn2Balls.count { it.isSix }

                    // Header
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(inn1Name, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("", modifier = Modifier.width(60.dp))
                        Text(inn2Name, color = NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    }
                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 6.dp))

                    CompareRow("$inn1Runs", "Runs", "$inn2Runs", inn1Runs > inn2Runs)
                    CompareRow("$inn1Wkts", "Wickets", "$inn2Wkts", inn1Wkts < inn2Wkts)
                    CompareRow("$inn1Fours", "Fours", "$inn2Fours", inn1Fours > inn2Fours)
                    CompareRow("$inn1Sixes", "Sixes", "$inn2Sixes", inn1Sixes > inn2Sixes)
                }
            }
        }
    }
}

// ── GRAPHS ───────────────────────────────────────────────────

@Composable
fun WormGraph(
    inn1Balls: List<Ball>, inn2Balls: List<Ball>,
    inn1Name: String, inn2Name: String, totalOvers: Int
) {
    if (inn1Balls.isEmpty()) {
        Text("No data yet", color = TextSecondary, fontSize = 12.sp)
        return
    }

    fun cumulativeRuns(balls: List<Ball>): List<Pair<Float, Float>> {
        var totalRuns = 0
        val points = mutableListOf(Pair(0f, 0f))
        balls.sortedWith(compareBy({ it.overNo }, { it.ballNo })).forEach { ball ->
            val legalBalls = balls.count { b ->
                (b.overNo < ball.overNo || (b.overNo == ball.overNo && b.ballNo <= ball.ballNo)) &&
                        b.extrasType != "wide" && b.extrasType != "no_ball"
            }
            val runs = when {
                ball.extrasType == "wide" -> (ball.extrasRuns ?: 1) + ball.runsOffBat
                ball.extrasType == "no_ball" -> 1 + ball.runsOffBat + (ball.extrasRuns ?: 0)
                else -> ball.runsOffBat + (ball.extrasRuns ?: 0)
            }
            totalRuns += runs
            val overPoint = legalBalls / 6f
            points.add(Pair(overPoint, totalRuns.toFloat()))
        }
        return points
    }

    val inn1Points = cumulativeRuns(inn1Balls)
    val inn2Points = cumulativeRuns(inn2Balls)
    val maxRuns = ((inn1Points.maxOfOrNull { it.second } ?: 0f).coerceAtLeast(
        inn2Points.maxOfOrNull { it.second } ?: 0f
    ) * 1.1f).coerceAtLeast(1f)

    Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
        val w = size.width
        val h = size.height
        val padLeft = 8.dp.toPx()
        val padBottom = 8.dp.toPx()
        val graphW = w - padLeft
        val graphH = h - padBottom

        // Grid lines
        (0..4).forEach { i ->
            val y = graphH - (i / 4f) * graphH
            drawLine(BorderColor, Offset(padLeft, y), Offset(w, y), strokeWidth = 0.5f)
        }

        // Inn1 line
        if (inn1Points.size >= 2) {
            val path = Path()
            inn1Points.forEachIndexed { i, (over, runs) ->
                val x = padLeft + (over / totalOvers) * graphW
                val y = graphH - (runs / maxRuns) * graphH
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, NeonGreen, style = Stroke(width = 2.dp.toPx()))
        }

        // Inn2 line
        if (inn2Points.size >= 2) {
            val path = Path()
            inn2Points.forEachIndexed { i, (over, runs) ->
                val x = padLeft + (over / totalOvers) * graphW
                val y = graphH - (runs / maxRuns) * graphH
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, NeonBlue, style = Stroke(width = 2.dp.toPx()))
        }
    }

    Spacer(modifier = Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(12.dp, 3.dp).background(NeonGreen))
            Text(inn1Name, color = TextSecondary, fontSize = 11.sp)
        }
        if (inn2Balls.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(12.dp, 3.dp).background(NeonBlue))
                Text(inn2Name, color = TextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun ManhattanChart(
    inn1Balls: List<Ball>, inn2Balls: List<Ball>,
    inn1Name: String, inn2Name: String, totalOvers: Int
) {
    if (inn1Balls.isEmpty()) {
        Text("No data yet", color = TextSecondary, fontSize = 12.sp)
        return
    }

    fun runsPerOver(balls: List<Ball>): Map<Int, Int> {
        return balls.groupBy { it.overNo }.mapValues { (_, overBalls) ->
            overBalls.sumOf { b ->
                when {
                    b.extrasType == "wide" -> (b.extrasRuns ?: 1) + b.runsOffBat
                    b.extrasType == "no_ball" -> 1 + b.runsOffBat + (b.extrasRuns ?: 0)
                    else -> b.runsOffBat + (b.extrasRuns ?: 0)
                }
            }
        }
    }

    val inn1OverRuns = runsPerOver(inn1Balls)
    val inn2OverRuns = runsPerOver(inn2Balls)
    val maxRuns = ((inn1OverRuns.values.maxOrNull() ?: 0).coerceAtLeast(
        inn2OverRuns.values.maxOrNull() ?: 0
    ) + 2).coerceAtLeast(1)
    val maxOver = totalOvers

    Row(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        (0 until maxOver).forEach { over ->
            val runs1 = inn1OverRuns[over] ?: 0
            val runs2 = inn2OverRuns[over] ?: 0
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                if (runs2 > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height((runs2.toFloat() / maxRuns * 100).dp.coerceAtLeast(2.dp))
                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                            .background(NeonBlue.copy(alpha = 0.7f))
                    )
                }
                if (runs1 > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height((runs1.toFloat() / maxRuns * 100).dp.coerceAtLeast(2.dp))
                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                            .background(NeonGreen.copy(alpha = 0.7f))
                    )
                }
            }
        }
    }

    // Y-axis labels
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("0", color = TextSecondary, fontSize = 9.sp)
        Text("$maxOver overs", color = TextSecondary, fontSize = 9.sp)
    }

    Spacer(modifier = Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(10.dp).background(NeonGreen, RoundedCornerShape(2.dp)))
            Text(inn1Name, color = TextSecondary, fontSize = 11.sp)
        }
        if (inn2Balls.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(10.dp).background(NeonBlue, RoundedCornerShape(2.dp)))
                Text(inn2Name, color = TextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun PartnershipGraph(
    inn1Balls: List<Ball>, inn2Balls: List<Ball>,
    inn1Name: String, inn2Name: String
) {
    fun computePartnerships(balls: List<Ball>): List<Pair<String, Int>> {
        val partnerships = mutableListOf<Pair<String, Int>>()
        var currentBatter1 = ""
        var currentBatter2 = ""
        var pRuns = 0
        var pNo = 1

        balls.sortedWith(compareBy({ it.overNo }, { it.ballNo })).forEach { ball ->
            if (currentBatter1.isEmpty()) currentBatter1 = ball.batsmanId ?: ""
            if (currentBatter2.isEmpty() && ball.nonStrikerId != null) currentBatter2 = ball.nonStrikerId

            val runs = when {
                ball.extrasType == "wide" -> (ball.extrasRuns ?: 1) + ball.runsOffBat
                ball.extrasType == "no_ball" -> 1 + ball.runsOffBat + (ball.extrasRuns ?: 0)
                else -> ball.runsOffBat + (ball.extrasRuns ?: 0)
            }
            pRuns += runs

            if (ball.isWicket && ball.wicketType != "retired_hurt") {
                partnerships.add(Pair("P$pNo", pRuns))
                pNo++
                pRuns = 0
                currentBatter1 = ""
                currentBatter2 = ball.nonStrikerId ?: ""
            }
        }
        if (pRuns > 0) partnerships.add(Pair("P$pNo*", pRuns))
        return partnerships
    }

    val inn1P = computePartnerships(inn1Balls)
    val inn2P = computePartnerships(inn2Balls)

    if (inn1P.isEmpty() && inn2P.isEmpty()) {
        Text("No partnership data yet", color = TextSecondary, fontSize = 12.sp)
        return
    }

    val maxRuns = ((inn1P.maxOfOrNull { it.second } ?: 0).coerceAtLeast(
        inn2P.maxOfOrNull { it.second } ?: 0
    ) + 5).coerceAtLeast(1)

    listOf(inn1P to inn1Name to NeonGreen, inn2P to inn2Name to NeonBlue).forEach { (pairName, color) ->
        val (partnerships, name) = pairName
        if (partnerships.isEmpty()) return@forEach
        Text(name, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        partnerships.forEach { (label, runs) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(label, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(32.dp))
                Box(
                    modifier = Modifier
                        .height(18.dp)
                        .width((runs.toFloat() / maxRuns * 160).dp.coerceAtLeast(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .background(color.copy(alpha = 0.7f))
                )
                Text("$runs", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun WinProbabilityGraph(
    inn1Balls: List<Ball>, inn2Balls: List<Ball>,
    inn1Name: String, inn2Name: String, totalOvers: Int
) {
    if (inn2Balls.isEmpty() || inn1Balls.isEmpty()) {
        Text("Win probability available after both innings start", color = TextSecondary, fontSize = 12.sp)
        return
    }

    val inn1Total = inn1Balls.sumOf { b ->
        when {
            b.extrasType == "wide" -> (b.extrasRuns ?: 1) + b.runsOffBat
            b.extrasType == "no_ball" -> 1 + b.runsOffBat + (b.extrasRuns ?: 0)
            else -> b.runsOffBat + (b.extrasRuns ?: 0)
        }
    }
    val target = inn1Total + 1
    val totalBalls = totalOvers * 6

    // Compute win probability over each ball of 2nd innings
    val probPoints = mutableListOf<Float>()
    var runsSoFar = 0
    var wicketsSoFar = 0
    val maxWickets = 10

    inn2Balls.sortedWith(compareBy({ it.overNo }, { it.ballNo })).forEachIndexed { index, ball ->
        val runs = when {
            ball.extrasType == "wide" -> (ball.extrasRuns ?: 1) + ball.runsOffBat
            ball.extrasType == "no_ball" -> 1 + ball.runsOffBat + (ball.extrasRuns ?: 0)
            else -> ball.runsOffBat + (ball.extrasRuns ?: 0)
        }
        runsSoFar += runs
        if (ball.isWicket && ball.wicketType != "retired_hurt") wicketsSoFar++

        val ballsLeft = (totalBalls - (index + 1)).coerceAtLeast(0)
        val runsNeeded = (target - runsSoFar).coerceAtLeast(0)
        val wicketsLeft = (maxWickets - wicketsSoFar).coerceAtLeast(0)

        // Simple probability model
        val prob = when {
            runsSoFar >= target -> 1.0f
            ballsLeft == 0 -> 0.0f
            wicketsLeft == 0 -> 0.0f
            else -> {
                val rrRequired = runsNeeded.toFloat() / (ballsLeft / 6f).coerceAtLeast(0.1f)
                val currentRR = if (index > 0) runsSoFar * 6f / (index + 1) else 6f
                val wicketFactor = wicketsLeft.toFloat() / maxWickets
                val rrFactor = (currentRR / rrRequired.coerceAtLeast(0.1f)).coerceIn(0f, 2f)
                (rrFactor * 0.5f * wicketFactor).coerceIn(0.05f, 0.95f)
            }
        }
        probPoints.add(prob)
    }

    if (probPoints.isEmpty()) {
        Text("No data yet", color = TextSecondary, fontSize = 12.sp)
        return
    }

    val currentProb = probPoints.lastOrNull() ?: 0.5f
    val inn2Name2 = inn2Name

    Text(
        "Current: $inn2Name2 ${"%.0f".format(currentProb * 100)}% | $inn1Name ${"%.0f".format((1 - currentProb) * 100)}%",
        color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))

    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
        val w = size.width
        val h = size.height

        // Grid
        listOf(0.25f, 0.5f, 0.75f).forEach { frac ->
            val y = h - frac * h
            drawLine(BorderColor, Offset(0f, y), Offset(w, y), strokeWidth = 0.5f)
        }

        // 50% line
        drawLine(TextSecondary.copy(alpha = 0.5f), Offset(0f, h * 0.5f), Offset(w, h * 0.5f), strokeWidth = 1f)

        if (probPoints.size >= 2) {
            val path = Path()
            probPoints.forEachIndexed { i, prob ->
                val x = (i.toFloat() / (probPoints.size - 1)) * w
                val y = h - prob * h
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, NeonGreen, style = Stroke(width = 2.dp.toPx()))
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Start", color = TextSecondary, fontSize = 9.sp)
        Text("50%", color = TextSecondary, fontSize = 9.sp)
        Text("Now", color = TextSecondary, fontSize = 9.sp)
    }
}

// ── HELPER COMPOSABLES ───────────────────────────────────────

@Composable
fun AnalyticsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 8.dp))
        content()
    }
}

@Composable
fun StatBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSystemInDarkTheme()) Color(0xFF030F08) else Color(0xFFF0FDF8))
            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(label, color = TextSecondary, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Text(value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun CompareRow(val1: String, label: String, val2: String, val1Better: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            val1, color = if (val1Better) NeonGreen else TextPrimary,
            fontSize = 13.sp, fontWeight = if (val1Better) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Text(label, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text(
            val2, color = if (!val1Better) NeonGreen else TextPrimary,
            fontSize = 13.sp, fontWeight = if (!val1Better) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f), textAlign = TextAlign.End
        )
    }
}