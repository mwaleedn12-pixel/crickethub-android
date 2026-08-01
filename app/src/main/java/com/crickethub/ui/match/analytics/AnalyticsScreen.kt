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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
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

// ── SHARED HELPERS ───────────────────────────────────────────

/** Total runs from a single ball including extras and penalties. */
private fun Ball.totalRuns(): Int = when {
    extrasType == "wide" -> (extrasRuns ?: 1) + runsOffBat
    extrasType == "no_ball" -> 1 + runsOffBat + (extrasRuns ?: 0)
    else -> runsOffBat + (extrasRuns ?: 0)
}

private fun Ball.isLegal(): Boolean =
    extrasType != "wide" && extrasType != "no_ball"

private fun Ball.isDot(): Boolean =
    isLegal() && runsOffBat == 0 && (extrasRuns ?: 0) == 0

private fun Ball.isRealWicket(): Boolean =
    isWicket && wicketType != "retired_hurt"

/** Draw axis label text on Canvas using native Paint. */
private fun DrawScope.drawAxisText(
    text: String, x: Float, y: Float,
    textSize: Float = 24f, color: Long = 0xFF9E9E9E,
    align: android.graphics.Paint.Align = android.graphics.Paint.Align.CENTER
) {
    drawContext.canvas.nativeCanvas.drawText(
        text, x, y,
        android.graphics.Paint().apply {
            this.textSize = textSize
            this.color = color.toInt()
            this.textAlign = align
            isAntiAlias = true
        }
    )
}

/** Compute runs per over from ball list. Key = overNo, value = total runs. */
private fun runsPerOver(balls: List<Ball>): Map<Int, Int> =
    balls.groupBy { it.overNo }.mapValues { (_, ob) -> ob.sumOf { it.totalRuns() } }

/** Cumulative runs at end of each over. */
private fun cumulativeRunsPerOver(balls: List<Ball>): List<Pair<Int, Int>> {
    val rpo = runsPerOver(balls).toSortedMap()
    var cum = 0
    return rpo.map { (over, runs) -> cum += runs; Pair(over, cum) }
}

// ── MAIN SCREEN ──────────────────────────────────────────────

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

                val t1Id = match?.team1Id ?: ""
                val t2Id = match?.team2Id ?: ""

                team1Name = try {
                    SupabaseClient.client.postgrest["teams"]
                        .select { filter { eq("id", t1Id) } }
                        .decodeSingleOrNull<Team>()?.name ?: "Team 1"
                } catch (e: Exception) { "Team 1" }

                team2Name = try {
                    SupabaseClient.client.postgrest["teams"]
                        .select { filter { eq("id", t2Id) } }
                        .decodeSingleOrNull<Team>()?.name ?: "Team 2"
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
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = CH.textPrimary)
                }
                Text("Analytics", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CH.textPrimary)
            }

            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = CH.surface,
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
                                color = if (selectedTab == index) NeonGreen else CH.textSecondary
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
} // AnalyticsScreen

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
                val wickets1 = inn1Balls.filter { it.isRealWicket() }
                val wickets2 = inn2Balls.filter { it.isRealWicket() }
                val allWickets = (wickets1 + wickets2)
                val dismissalGroups = allWickets.groupBy {
                    it.wicketType?.replace("_", " ")?.replaceFirstChar { c -> c.uppercase() } ?: "Unknown"
                }
                if (dismissalGroups.isEmpty()) {
                    Text("No wickets yet", color = CH.textSecondary, fontSize = 12.sp)
                } else {
                    dismissalGroups.forEach { (type, balls) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(type, color = CH.textPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
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
                    Text("No boundaries yet", color = CH.textSecondary, fontSize = 12.sp)
                } else {
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
                            Text("4s", color = CH.textSecondary, fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(10.dp).background(NeonGreen, RoundedCornerShape(2.dp)))
                            Text("6s", color = CH.textSecondary, fontSize = 11.sp)
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
                    val legalBalls = balls.filter { it.isLegal() }
                    val dotBalls = legalBalls.count { it.isDot() }
                    val dotPct = if (legalBalls.isNotEmpty()) dotBalls * 100.0 / legalBalls.size else 0.0

                    Text(name, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatBox("Dot Balls", "$dotBalls", CH.textSecondary, Modifier.weight(1f))
                        StatBox("Dot %", "${"%.1f".format(dotPct)}%", ErrorRed, Modifier.weight(1f))
                        StatBox("Legal Balls", "${legalBalls.size}", CH.textPrimary, Modifier.weight(1f))
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
                            val runs = phaseBalls.sumOf { it.totalRuns() }
                            val legal = phaseBalls.count { it.isLegal() }
                            val wkts = phaseBalls.count { it.isRealWicket() }
                            val rr = if (legal > 0) runs * 6.0 / legal else 0.0
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSystemInDarkTheme()) Color(0xFF0A0A0A) else Color(0xFFF7F3EA))
                                    .border(1.dp, CH.border, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(phaseLabels[i], color = CH.textSecondary, fontSize = 10.sp, textAlign = TextAlign.Center)
                                Text("$runs/$wkts", color = CH.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
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

                    val ecoData = overGroups.map { (over, overBalls) ->
                        val runs = overBalls.sumOf { it.totalRuns() }
                        val legal = overBalls.count { it.isLegal() }
                        val eco = if (legal > 0) runs * 6.0 / legal else 0.0
                        Pair(over, eco)
                    }
                    val maxEco = (ecoData.maxOfOrNull { it.second } ?: 12.0).coerceAtLeast(6.0)

                    // Axis-labeled bar chart via Canvas
                    val canvasBorder = CH.border
                    val canvasTextSec = CH.textSecondary
                    Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                        val padLeft = 28.dp.toPx()
                        val padBottom = 20.dp.toPx()
                        val padTop = 4.dp.toPx()
                        val graphW = size.width - padLeft
                        val graphH = size.height - padBottom - padTop
                        val barCount = ecoData.size.coerceAtLeast(1)
                        val barW = (graphW / barCount) * 0.7f
                        val gap = (graphW / barCount) * 0.3f

                        // Y-axis grid + labels
                        val ySteps = 3
                        for (i in 0..ySteps) {
                            val v = maxEco * i / ySteps
                            val y = padTop + graphH - (i.toFloat() / ySteps) * graphH
                            drawLine(canvasBorder, Offset(padLeft, y), Offset(size.width, y), strokeWidth = 0.5f)
                            drawAxisText("%.0f".format(v), padLeft - 4.dp.toPx(), y + 4.dp.toPx(), 20f, align = android.graphics.Paint.Align.RIGHT)
                        }

                        // Bars + X labels
                        ecoData.forEachIndexed { idx, (over, eco) ->
                            val x = padLeft + idx * (barW + gap) + gap / 2
                            val h = ((eco / maxEco) * graphH).toFloat().coerceAtLeast(2.dp.toPx())
                            val y = padTop + graphH - h
                            val barColor = when {
                                eco >= 12 -> ErrorRed; eco >= 9 -> AmberColor
                                eco >= 6 -> NeonGreen; else -> NeonBlue
                            }
                            drawRect(barColor, Offset(x, y), Size(barW, h))
                            // X label every 2 overs or if < 10 overs show all
                            if (barCount <= 10 || over % 2 == 0) {
                                drawAxisText("${over + 1}", x + barW / 2, size.height - 2.dp.toPx(), 18f)
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
                    val wickets = balls.filter { it.isRealWicket() }
                    Text(name, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (wickets.isEmpty()) {
                        Text("No wickets", color = CH.textSecondary, fontSize = 12.sp)
                    } else {
                        wickets.forEachIndexed { index, ball ->
                            val runsSoFar = balls
                                .filter { b -> b.overNo < ball.overNo || (b.overNo == ball.overNo && b.ballNo <= ball.ballNo) }
                                .sumOf { it.totalRuns() }
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
                                    color = CH.textSecondary, fontSize = 12.sp,
                                    modifier = Modifier.width(56.dp)
                                )
                                Text(
                                    "$runsSoFar runs",
                                    color = CH.textPrimary, fontSize = 12.sp,
                                    modifier = Modifier.width(64.dp)
                                )
                                Text(
                                    ball.wicketType?.replace("_", " ") ?: "out",
                                    color = CH.textSecondary, fontSize = 11.sp,
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
                        StatBox("Total", "$totalExtras", CH.textPrimary, Modifier.weight(1f))
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
                WormGraph(inn1Balls, inn2Balls, inn1Name, inn2Name, totalOvers)
            }
        }

        // Manhattan Chart
        item {
            AnalyticsCard("Manhattan Chart (Runs per Over)") {
                ManhattanChart(inn1Balls, inn2Balls, inn1Name, inn2Name, totalOvers)
            }
        }

        // Run-Rate Graph (NEW)
        item {
            AnalyticsCard("Run Rate Graph") {
                RunRateGraph(inn1Balls, inn2Balls, inn1Name, inn2Name, totalOvers)
            }
        }

        // Partnership Graph
        item {
            AnalyticsCard("Partnership Graph") {
                PartnershipGraph(inn1Balls, inn2Balls, inn1Name, inn2Name)
            }
        }

        // Wicket Progression (NEW)
        item {
            AnalyticsCard("Wicket Progression") {
                WicketProgressionGraph(inn1Balls, inn2Balls, inn1Name, inn2Name, totalOvers)
            }
        }

        // Powerplay Analysis (NEW)
        item {
            AnalyticsCard("Powerplay Analysis") {
                PhaseAnalysisChart(inn1Balls, inn2Balls, inn1Name, inn2Name, "powerplay", "Powerplay")
            }
        }

        // Death Overs Analysis (NEW)
        item {
            AnalyticsCard("Death Overs Analysis") {
                PhaseAnalysisChart(inn1Balls, inn2Balls, inn1Name, inn2Name, "death", "Death Overs")
            }
        }

        // Win Probability
        item {
            AnalyticsCard("Win Probability") {
                WinProbabilityGraph(inn1Balls, inn2Balls, inn1Name, inn2Name, totalOvers)
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
                    val runs = balls.sumOf { it.totalRuns() }
                    val wickets = balls.count { it.isRealWicket() }
                    val legalBalls = balls.count { it.isLegal() }
                    val overs = "${legalBalls / 6}.${legalBalls % 6}"
                    val rr = if (legalBalls > 0) runs * 6.0 / legalBalls else 0.0
                    val fours = balls.count { it.isBoundary && !it.isSix }
                    val sixes = balls.count { it.isSix }
                    val dotBalls = balls.count { it.isDot() }
                    val extras = balls.sumOf { it.extrasRuns ?: 0 } +
                            balls.count { it.extrasType == "wide" } +
                            balls.count { it.extrasType == "no_ball" }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSystemInDarkTheme()) Color(0xFF0A0A0A) else Color(0xFFF7F3EA))
                            .padding(12.dp)
                    ) {
                        Text(name, color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Score", color = CH.textSecondary, fontSize = 12.sp)
                            Text("$runs/$wickets ($overs ov)", color = CH.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                    val inn1Runs = inn1Balls.sumOf { it.totalRuns() }
                    val inn2Runs = inn2Balls.sumOf { it.totalRuns() }
                    val inn1Wkts = inn1Balls.count { it.isRealWicket() }
                    val inn2Wkts = inn2Balls.count { it.isRealWicket() }
                    val inn1Fours = inn1Balls.count { it.isBoundary && !it.isSix }
                    val inn2Fours = inn2Balls.count { it.isBoundary && !it.isSix }
                    val inn1Sixes = inn1Balls.count { it.isSix }
                    val inn2Sixes = inn2Balls.count { it.isSix }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(inn1Name, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("", modifier = Modifier.width(60.dp))
                        Text(inn2Name, color = NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    }
                    HorizontalDivider(color = CH.border, modifier = Modifier.padding(vertical = 6.dp))

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
        Text("No data yet", color = CH.textSecondary, fontSize = 12.sp)
        return
    }

    val inn1Cum = cumulativeRunsPerOver(inn1Balls)
    val inn2Cum = cumulativeRunsPerOver(inn2Balls)
    val maxRuns = ((inn1Cum.maxOfOrNull { it.second } ?: 0)
        .coerceAtLeast(inn2Cum.maxOfOrNull { it.second } ?: 0) + 10)
        .coerceAtLeast(1)
    val maxOver = totalOvers

    val canvasBorder = CH.border
    val canvasTextSec = CH.textSecondary
    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        val padLeft = 32.dp.toPx()
        val padBottom = 22.dp.toPx()
        val padTop = 6.dp.toPx()
        val graphW = size.width - padLeft
        val graphH = size.height - padBottom - padTop

        // Y-axis grid + labels (5 steps)
        val ySteps = 4
        for (i in 0..ySteps) {
            val v = maxRuns * i / ySteps
            val y = padTop + graphH - (i.toFloat() / ySteps) * graphH
            drawLine(canvasBorder, Offset(padLeft, y), Offset(size.width, y), strokeWidth = 0.5f)
            drawAxisText("$v", padLeft - 4.dp.toPx(), y + 4.dp.toPx(), 20f, align = android.graphics.Paint.Align.RIGHT)
        }

        // X-axis labels (over numbers)
        val xStep = if (maxOver <= 10) 1 else if (maxOver <= 25) 2 else 5
        for (ov in 0..maxOver step xStep) {
            val x = padLeft + (ov.toFloat() / maxOver) * graphW
            drawAxisText("$ov", x, size.height - 2.dp.toPx(), 20f)
            if (ov > 0) drawLine(canvasBorder, Offset(x, padTop), Offset(x, padTop + graphH), strokeWidth = 0.3f)
        }

        // Inn1 line
        if (inn1Cum.size >= 1) {
            val path = Path()
            path.moveTo(padLeft, padTop + graphH) // start at 0,0
            inn1Cum.forEach { (over, runs) ->
                val x = padLeft + ((over + 1).toFloat() / maxOver) * graphW
                val y = padTop + graphH - (runs.toFloat() / maxRuns) * graphH
                path.lineTo(x, y)
            }
            drawPath(path, NeonGreen, style = Stroke(width = 2.5.dp.toPx()))
        }

        // Inn2 line
        if (inn2Cum.size >= 1) {
            val path = Path()
            path.moveTo(padLeft, padTop + graphH)
            inn2Cum.forEach { (over, runs) ->
                val x = padLeft + ((over + 1).toFloat() / maxOver) * graphW
                val y = padTop + graphH - (runs.toFloat() / maxRuns) * graphH
                path.lineTo(x, y)
            }
            drawPath(path, NeonBlue, style = Stroke(width = 2.5.dp.toPx()))
        }
    }

    Spacer(modifier = Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(12.dp, 3.dp).background(NeonGreen))
            Text(inn1Name, color = CH.textSecondary, fontSize = 11.sp)
        }
        if (inn2Balls.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(12.dp, 3.dp).background(NeonBlue))
                Text(inn2Name, color = CH.textSecondary, fontSize = 11.sp)
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
        Text("No data yet", color = CH.textSecondary, fontSize = 12.sp)
        return
    }

    val inn1OverRuns = runsPerOver(inn1Balls)
    val inn2OverRuns = runsPerOver(inn2Balls)
    val allOvers = (inn1OverRuns.keys + inn2OverRuns.keys)
    val maxOverIdx = (allOvers.maxOrNull() ?: 0)
    val maxRuns = ((inn1OverRuns.values.maxOrNull() ?: 0)
        .coerceAtLeast(inn2OverRuns.values.maxOrNull() ?: 0) + 2).coerceAtLeast(1)
    val overCount = (maxOverIdx + 1).coerceAtLeast(1)

    val canvasBorder = CH.border
    val canvasTextSec = CH.textSecondary
    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
        val padLeft = 28.dp.toPx()
        val padBottom = 22.dp.toPx()
        val padTop = 6.dp.toPx()
        val graphW = size.width - padLeft
        val graphH = size.height - padBottom - padTop

        // Y grid + labels
        val ySteps = 3
        for (i in 0..ySteps) {
            val v = maxRuns * i / ySteps
            val y = padTop + graphH - (i.toFloat() / ySteps) * graphH
            drawLine(canvasBorder, Offset(padLeft, y), Offset(size.width, y), strokeWidth = 0.5f)
            drawAxisText("$v", padLeft - 4.dp.toPx(), y + 4.dp.toPx(), 20f, align = android.graphics.Paint.Align.RIGHT)
        }

        val slotW = graphW / overCount
        val barW = slotW * 0.35f

        (0 until overCount).forEach { over ->
            val r1 = inn1OverRuns[over] ?: 0
            val r2 = inn2OverRuns[over] ?: 0
            val cx = padLeft + over * slotW + slotW / 2f

            // Inn1 bar (left of center)
            if (r1 > 0) {
                val h = (r1.toFloat() / maxRuns) * graphH
                drawRect(
                    NeonGreen.copy(alpha = 0.8f),
                    Offset(cx - barW - 1, padTop + graphH - h),
                    Size(barW, h)
                )
            }
            // Inn2 bar (right of center)
            if (r2 > 0) {
                val h = (r2.toFloat() / maxRuns) * graphH
                drawRect(
                    NeonBlue.copy(alpha = 0.8f),
                    Offset(cx + 1, padTop + graphH - h),
                    Size(barW, h)
                )
            }

            // X label
            val xStep = if (overCount <= 10) 1 else if (overCount <= 25) 2 else 5
            if (over % xStep == 0) {
                drawAxisText("${over + 1}", cx, size.height - 2.dp.toPx(), 18f)
            }
        }
    }

    Spacer(modifier = Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(10.dp).background(NeonGreen, RoundedCornerShape(2.dp)))
            Text(inn1Name, color = CH.textSecondary, fontSize = 11.sp)
        }
        if (inn2Balls.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(10.dp).background(NeonBlue, RoundedCornerShape(2.dp)))
                Text(inn2Name, color = CH.textSecondary, fontSize = 11.sp)
            }
        }
    }
}

// ── NEW: Run-Rate Graph ──────────────────────────────────────

@Composable
fun RunRateGraph(
    inn1Balls: List<Ball>, inn2Balls: List<Ball>,
    inn1Name: String, inn2Name: String, totalOvers: Int
) {
    if (inn1Balls.isEmpty()) {
        Text("No data yet", color = CH.textSecondary, fontSize = 12.sp)
        return
    }

    // CRR at end of each over = cumRuns / overs_completed
    fun crrPerOver(balls: List<Ball>): List<Pair<Int, Float>> {
        val cum = cumulativeRunsPerOver(balls)
        return cum.map { (over, runs) -> Pair(over, runs.toFloat() / (over + 1)) }
    }

    val inn1RR = crrPerOver(inn1Balls)
    val inn2RR = crrPerOver(inn2Balls)
    val maxRR = ((inn1RR.maxOfOrNull { it.second } ?: 6f)
        .coerceAtLeast(inn2RR.maxOfOrNull { it.second } ?: 0f) + 2f).coerceAtLeast(2f)

    val canvasBorder = CH.border
    val canvasTextSec = CH.textSecondary
    Canvas(modifier = Modifier.fillMaxWidth().height(170.dp)) {
        val padLeft = 32.dp.toPx()
        val padBottom = 22.dp.toPx()
        val padTop = 6.dp.toPx()
        val graphW = size.width - padLeft
        val graphH = size.height - padBottom - padTop

        // Y grid
        val ySteps = 4
        for (i in 0..ySteps) {
            val v = maxRR * i / ySteps
            val y = padTop + graphH - (i.toFloat() / ySteps) * graphH
            drawLine(canvasBorder, Offset(padLeft, y), Offset(size.width, y), strokeWidth = 0.5f)
            drawAxisText("%.0f".format(v), padLeft - 4.dp.toPx(), y + 4.dp.toPx(), 20f, align = android.graphics.Paint.Align.RIGHT)
        }

        // X labels
        val xStep = if (totalOvers <= 10) 1 else if (totalOvers <= 25) 2 else 5
        for (ov in 0..totalOvers step xStep) {
            val x = padLeft + (ov.toFloat() / totalOvers) * graphW
            drawAxisText("$ov", x, size.height - 2.dp.toPx(), 20f)
        }

        // Inn1 line
        if (inn1RR.isNotEmpty()) {
            val path = Path()
            inn1RR.forEachIndexed { i, (over, rr) ->
                val x = padLeft + ((over + 1).toFloat() / totalOvers) * graphW
                val y = padTop + graphH - (rr / maxRR) * graphH
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, NeonGreen, style = Stroke(width = 2.5.dp.toPx()))
        }

        // Inn2 line
        if (inn2RR.isNotEmpty()) {
            val path = Path()
            inn2RR.forEachIndexed { i, (over, rr) ->
                val x = padLeft + ((over + 1).toFloat() / totalOvers) * graphW
                val y = padTop + graphH - (rr / maxRR) * graphH
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, NeonBlue, style = Stroke(width = 2.5.dp.toPx()))
        }
    }

    Spacer(modifier = Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(12.dp, 3.dp).background(NeonGreen))
            Text(inn1Name, color = CH.textSecondary, fontSize = 11.sp)
        }
        if (inn2Balls.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(12.dp, 3.dp).background(NeonBlue))
                Text(inn2Name, color = CH.textSecondary, fontSize = 11.sp)
            }
        }
    }
}

// ── NEW: Wicket Progression Graph ────────────────────────────

@Composable
fun WicketProgressionGraph(
    inn1Balls: List<Ball>, inn2Balls: List<Ball>,
    inn1Name: String, inn2Name: String, totalOvers: Int
) {
    val inn1Wkts = inn1Balls.filter { it.isRealWicket() }
    val inn2Wkts = inn2Balls.filter { it.isRealWicket() }

    if (inn1Wkts.isEmpty() && inn2Wkts.isEmpty()) {
        Text("No wickets yet", color = CH.textSecondary, fontSize = 12.sp)
        return
    }

    // Build cumulative wicket points: (overNo as float, cumWickets)
    fun wicketPoints(balls: List<Ball>): List<Pair<Float, Int>> {
        val wkts = balls.filter { it.isRealWicket() }
            .sortedWith(compareBy({ it.overNo }, { it.ballNo }))
        val pts = mutableListOf(Pair(0f, 0))
        var cum = 0
        wkts.forEach { b ->
            cum++
            val overPt = b.overNo + b.ballNo / 6f
            pts.add(Pair(overPt, cum))
        }
        return pts
    }

    val inn1Pts = wicketPoints(inn1Balls)
    val inn2Pts = wicketPoints(inn2Balls)
    val maxWkts = (inn1Pts.maxOfOrNull { it.second } ?: 0)
        .coerceAtLeast(inn2Pts.maxOfOrNull { it.second } ?: 0)
        .coerceAtLeast(1)

    val canvasBorder = CH.border
    val canvasTextSec = CH.textSecondary
    Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
        val padLeft = 24.dp.toPx()
        val padBottom = 22.dp.toPx()
        val padTop = 6.dp.toPx()
        val graphW = size.width - padLeft
        val graphH = size.height - padBottom - padTop

        // Y grid (wicket count)
        for (i in 0..maxWkts) {
            val y = padTop + graphH - (i.toFloat() / maxWkts) * graphH
            drawLine(canvasBorder, Offset(padLeft, y), Offset(size.width, y), strokeWidth = 0.5f)
            drawAxisText("$i", padLeft - 4.dp.toPx(), y + 4.dp.toPx(), 20f, align = android.graphics.Paint.Align.RIGHT)
        }

        // X labels
        val xStep = if (totalOvers <= 10) 1 else if (totalOvers <= 25) 2 else 5
        for (ov in 0..totalOvers step xStep) {
            val x = padLeft + (ov.toFloat() / totalOvers) * graphW
            drawAxisText("$ov", x, size.height - 2.dp.toPx(), 20f)
        }

        // Step-line for inn1
        if (inn1Pts.size >= 2) {
            val path = Path()
            inn1Pts.forEachIndexed { i, (over, wkts) ->
                val x = padLeft + (over / totalOvers) * graphW
                val y = padTop + graphH - (wkts.toFloat() / maxWkts) * graphH
                if (i == 0) path.moveTo(x, y) else {
                    // Step: horizontal then vertical
                    val prevY = padTop + graphH - (inn1Pts[i - 1].second.toFloat() / maxWkts) * graphH
                    path.lineTo(x, prevY)
                    path.lineTo(x, y)
                }
            }
            drawPath(path, NeonGreen, style = Stroke(width = 2.dp.toPx()))
            // Dot at each wicket
            inn1Pts.drop(1).forEach { (over, wkts) ->
                val x = padLeft + (over / totalOvers) * graphW
                val y = padTop + graphH - (wkts.toFloat() / maxWkts) * graphH
                drawCircle(NeonGreen, 4.dp.toPx(), Offset(x, y))
            }
        }

        // Step-line for inn2
        if (inn2Pts.size >= 2) {
            val path = Path()
            inn2Pts.forEachIndexed { i, (over, wkts) ->
                val x = padLeft + (over / totalOvers) * graphW
                val y = padTop + graphH - (wkts.toFloat() / maxWkts) * graphH
                if (i == 0) path.moveTo(x, y) else {
                    val prevY = padTop + graphH - (inn2Pts[i - 1].second.toFloat() / maxWkts) * graphH
                    path.lineTo(x, prevY)
                    path.lineTo(x, y)
                }
            }
            drawPath(path, NeonBlue, style = Stroke(width = 2.dp.toPx()))
            inn2Pts.drop(1).forEach { (over, wkts) ->
                val x = padLeft + (over / totalOvers) * graphW
                val y = padTop + graphH - (wkts.toFloat() / maxWkts) * graphH
                drawCircle(NeonBlue, 4.dp.toPx(), Offset(x, y))
            }
        }
    }

    Spacer(modifier = Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(12.dp, 3.dp).background(NeonGreen))
            Text(inn1Name, color = CH.textSecondary, fontSize = 11.sp)
        }
        if (inn2Wkts.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(12.dp, 3.dp).background(NeonBlue))
                Text(inn2Name, color = CH.textSecondary, fontSize = 11.sp)
            }
        }
    }
}

// ── NEW: Phase Analysis Chart (Powerplay / Death) ────────────

@Composable
fun PhaseAnalysisChart(
    inn1Balls: List<Ball>, inn2Balls: List<Ball>,
    inn1Name: String, inn2Name: String,
    phase: String, phaseLabel: String
) {
    val p1 = inn1Balls.filter { it.inningsPhase == phase }
    val p2 = inn2Balls.filter { it.inningsPhase == phase }

    if (p1.isEmpty() && p2.isEmpty()) {
        Text("No $phaseLabel data yet", color = CH.textSecondary, fontSize = 12.sp)
        return
    }

    // Build per-over breakdown within the phase
    data class OverStat(val over: Int, val runs: Int, val wickets: Int, val dots: Int, val boundaries: Int)

    fun phaseOverStats(balls: List<Ball>): List<OverStat> =
        balls.groupBy { it.overNo }.toSortedMap().map { (over, ob) ->
            OverStat(
                over = over,
                runs = ob.sumOf { it.totalRuns() },
                wickets = ob.count { it.isRealWicket() },
                dots = ob.count { it.isDot() },
                boundaries = ob.count { it.isBoundary || it.isSix }
            )
        }

    // Stats summary boxes per innings
    listOf(p1 to inn1Name, p2 to inn2Name).forEach { (balls, name) ->
        if (balls.isEmpty()) return@forEach
        val runs = balls.sumOf { it.totalRuns() }
        val wkts = balls.count { it.isRealWicket() }
        val legal = balls.count { it.isLegal() }
        val dots = balls.count { it.isDot() }
        val bdry = balls.count { it.isBoundary || it.isSix }
        val rr = if (legal > 0) runs * 6.0 / legal else 0.0

        Text(name, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatBox("Runs", "$runs", NeonGreen, Modifier.weight(1f))
            StatBox("Wkts", "$wkts", ErrorRed, Modifier.weight(1f))
            StatBox("RR", "${"%.1f".format(rr)}", NeonBlue, Modifier.weight(1f))
            StatBox("Dots", "$dots", CH.textSecondary, Modifier.weight(1f))
            StatBox("Bdry", "$bdry", AmberColor, Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(6.dp))

        // Per-over bar chart within this phase
        val overStats = phaseOverStats(balls)
        if (overStats.isNotEmpty()) {
            val maxR = (overStats.maxOfOrNull { it.runs } ?: 1).coerceAtLeast(1)
            val canvasBorder = CH.border
            val canvasTextSec = CH.textSecondary
            Canvas(modifier = Modifier.fillMaxWidth().height(90.dp)) {
                val padLeft = 22.dp.toPx()
                val padBottom = 18.dp.toPx()
                val padTop = 4.dp.toPx()
                val graphW = size.width - padLeft
                val graphH = size.height - padBottom - padTop
                val barCount = overStats.size.coerceAtLeast(1)
                val slotW = graphW / barCount
                val barW = slotW * 0.6f

                // Y labels
                for (i in listOf(0, maxR / 2, maxR)) {
                    val y = padTop + graphH - (i.toFloat() / maxR) * graphH
                    drawLine(canvasBorder, Offset(padLeft, y), Offset(size.width, y), strokeWidth = 0.3f)
                    drawAxisText("$i", padLeft - 3.dp.toPx(), y + 4.dp.toPx(), 18f, align = android.graphics.Paint.Align.RIGHT)
                }

                overStats.forEachIndexed { idx, os ->
                    val cx = padLeft + idx * slotW + slotW / 2f
                    val h = (os.runs.toFloat() / maxR) * graphH
                    val barColor = if (os.wickets > 0) ErrorRed else NeonGreen
                    drawRect(barColor.copy(alpha = 0.8f), Offset(cx - barW / 2, padTop + graphH - h), Size(barW, h.coerceAtLeast(1f)))
                    // Wicket dot on top
                    if (os.wickets > 0) {
                        drawCircle(ErrorRed, 3.dp.toPx(), Offset(cx, padTop + graphH - h - 4.dp.toPx()))
                    }
                    // X label
                    drawAxisText("${os.over + 1}", cx, size.height - 1.dp.toPx(), 17f)
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
fun PartnershipGraph(
    inn1Balls: List<Ball>, inn2Balls: List<Ball>,
    inn1Name: String, inn2Name: String
) {
    fun computePartnerships(balls: List<Ball>): List<Pair<String, Int>> {
        val partnerships = mutableListOf<Pair<String, Int>>()
        var pRuns = 0
        var pNo = 1

        balls.sortedWith(compareBy({ it.overNo }, { it.ballNo })).forEach { ball ->
            pRuns += ball.totalRuns()
            if (ball.isRealWicket()) {
                partnerships.add(Pair("P$pNo", pRuns))
                pNo++
                pRuns = 0
            }
        }
        if (pRuns > 0) partnerships.add(Pair("P$pNo*", pRuns))
        return partnerships
    }

    val inn1P = computePartnerships(inn1Balls)
    val inn2P = computePartnerships(inn2Balls)

    if (inn1P.isEmpty() && inn2P.isEmpty()) {
        Text("No partnership data yet", color = CH.textSecondary, fontSize = 12.sp)
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
                Text(label, color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(32.dp))
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
        Text("Win probability available after both innings start", color = CH.textSecondary, fontSize = 12.sp)
        return
    }

    val inn1Total = inn1Balls.sumOf { it.totalRuns() }
    val target = inn1Total + 1
    val totalBallsInMatch = totalOvers * 6

    val probPoints = mutableListOf<Float>()
    var runsSoFar = 0
    var wicketsSoFar = 0
    val maxWickets = 10

    inn2Balls.sortedWith(compareBy({ it.overNo }, { it.ballNo })).forEachIndexed { index, ball ->
        runsSoFar += ball.totalRuns()
        if (ball.isRealWicket()) wicketsSoFar++

        val ballsLeft = (totalBallsInMatch - (index + 1)).coerceAtLeast(0)
        val runsNeeded = (target - runsSoFar).coerceAtLeast(0)
        val wicketsLeft = (maxWickets - wicketsSoFar).coerceAtLeast(0)

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
        Text("No data yet", color = CH.textSecondary, fontSize = 12.sp)
        return
    }

    val currentProb = probPoints.lastOrNull() ?: 0.5f

    Text(
        "Current: $inn2Name ${"%.0f".format(currentProb * 100)}% | $inn1Name ${"%.0f".format((1 - currentProb) * 100)}%",
        color = CH.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))

    val canvasBorder = CH.border
    val canvasTextSec = CH.textSecondary
    Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
        val padLeft = 30.dp.toPx()
        val padBottom = 22.dp.toPx()
        val padTop = 6.dp.toPx()
        val graphW = size.width - padLeft
        val graphH = size.height - padBottom - padTop

        // Y grid + labels (0%, 25%, 50%, 75%, 100%)
        for (pct in listOf(0, 25, 50, 75, 100)) {
            val frac = pct / 100f
            val y = padTop + graphH - frac * graphH
            drawLine(
                if (pct == 50) canvasTextSec.copy(alpha = 0.5f) else canvasBorder,
                Offset(padLeft, y), Offset(size.width, y),
                strokeWidth = if (pct == 50) 1f else 0.5f
            )
            drawAxisText("$pct%", padLeft - 4.dp.toPx(), y + 4.dp.toPx(), 18f, align = android.graphics.Paint.Align.RIGHT)
        }

        // X labels (over numbers based on ball index)
        val totalBalls2 = probPoints.size
        if (totalBalls2 > 0) {
            val xStep = if (totalOvers <= 10) 1 else if (totalOvers <= 25) 2 else 5
            for (ov in 0..totalOvers step xStep) {
                val ballIdx = ov * 6
                if (ballIdx <= totalBalls2) {
                    val x = padLeft + (ballIdx.toFloat() / totalBalls2.coerceAtLeast(1)) * graphW
                    drawAxisText("$ov", x, size.height - 2.dp.toPx(), 18f)
                }
            }
        }

        // Line
        if (probPoints.size >= 2) {
            val path = Path()
            probPoints.forEachIndexed { i, prob ->
                val x = padLeft + (i.toFloat() / (probPoints.size - 1)) * graphW
                val y = padTop + graphH - prob * graphH
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, NeonGreen, style = Stroke(width = 2.dp.toPx()))
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(12.dp, 3.dp).background(NeonGreen))
            Text("$inn2Name win %", color = CH.textSecondary, fontSize = 11.sp)
        }
    }
}

// ── HELPER COMPOSABLES ───────────────────────────────────────

@Composable
fun AnalyticsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CH.surface)
            .border(1.dp, CH.border, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(title, color = CH.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        HorizontalDivider(color = CH.border, modifier = Modifier.padding(vertical = 8.dp))
        content()
    }
}

@Composable
fun StatBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSystemInDarkTheme()) Color(0xFF0A0A0A) else Color(0xFFF7F3EA))
            .border(1.dp, CH.border, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(label, color = CH.textSecondary, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = CH.textSecondary, fontSize = 12.sp)
        Text(value, color = CH.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
            val1, color = if (val1Better) NeonGreen else CH.textPrimary,
            fontSize = 13.sp, fontWeight = if (val1Better) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Text(label, color = CH.textSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text(
            val2, color = if (!val1Better) NeonGreen else CH.textPrimary,
            fontSize = 13.sp, fontWeight = if (!val1Better) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f), textAlign = TextAlign.End
        )
    }
}