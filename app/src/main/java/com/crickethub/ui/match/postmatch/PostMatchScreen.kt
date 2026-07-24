package com.crickethub.ui.match.postmatch

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import com.crickethub.ui.components.CricketAnimatedBackground
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crickethub.data.model.Ball
import com.crickethub.data.model.Innings
import com.crickethub.data.model.Player
import com.crickethub.ui.theme.*


@Composable
fun PostMatchScreen(
    matchId: String,
    onBack: () -> Unit,
    onGoToMatches: () -> Unit,
    viewModel: PostMatchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        "Result", "1st Inn", "2nd Inn",
        "Commentary", "Overs", "Analytics", "POTM"
    )

    LaunchedEffect(matchId) {
        viewModel.loadPostMatch(matchId)
    }

    LaunchedEffect(uiState.matchSaved) {
        if (uiState.matchSaved) onGoToMatches()
    }

    CricketAnimatedBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text(
                    "Match Summary",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = TextPrimary, modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    val shareText = buildString {
                        appendLine("🏏 Match Result")
                        appendLine(uiState.resultText)
                        uiState.innings1?.let {
                            appendLine("${uiState.innings1BattingTeamName}: ${it.totalRuns}/${it.totalWickets} (${it.totalBalls / 6}.${it.totalBalls % 6} ov)")
                        }
                        uiState.innings2?.let {
                            appendLine("${uiState.innings2BattingTeamName}: ${it.totalRuns}/${it.totalWickets} (${it.totalBalls / 6}.${it.totalBalls % 6} ov)")
                        }
                        uiState.selectedMotm?.let { appendLine("POTM: ${it.fullName}") }
                    }
                    clipboardManager.setText(AnnotatedString(shareText))
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = NeonGreen)
                }
            }

            // Result banner
            if (uiState.resultText.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NeonGreen.copy(alpha = 0.15f))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        uiState.resultText,
                        color = NeonGreen, fontSize = 16.sp,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                    )
                }
            }

            // Tabs
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
                                tab, fontSize = 12.sp,
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
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> PMResultTab(uiState)
                        1 -> PMInningsTab(
                            battingTeamName = uiState.innings1BattingTeamName,
                            bowlingTeamName = uiState.innings1BowlingTeamName,
                            innings = uiState.innings1,
                            batting = uiState.innings1Batting,
                            bowling = uiState.innings1Bowling,
                            bowlerMap = uiState.innings1Bowling.associate { it.player.id to it.player.fullName }
                        )
                        2 -> PMInningsTab(
                            battingTeamName = uiState.innings2BattingTeamName,
                            bowlingTeamName = uiState.innings2BowlingTeamName,
                            innings = uiState.innings2,
                            batting = uiState.innings2Batting,
                            bowling = uiState.innings2Bowling,
                            bowlerMap = uiState.innings2Bowling.associate { it.player.id to it.player.fullName }
                        )
                        3 -> PMCommentaryTab(uiState)
                        4 -> PMOversTab(uiState)
                        5 -> PMAnalyticsTab(uiState)
                        6 -> PMPotmTab(uiState, onSelectMotm = { viewModel.selectMotm(it) })
                    }
                }

                // Save button
                Button(
                    onClick = { viewModel.saveMatchResult(matchId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Text(
                        "Save & Complete Match",
                        color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp
                    )
                }
            }
        }
    }
} // CricketAnimatedBackground

// ── RESULT TAB ───────────────────────────────────────────────

@Composable
fun PMResultTab(uiState: PostMatchUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Scores
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                uiState.innings1?.let { inn ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(uiState.innings1BattingTeamName, color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("1st Innings", color = TextSecondary, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${inn.totalRuns}/${inn.totalWickets}", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("(${inn.totalBalls / 6}.${inn.totalBalls % 6} ov)", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider(color = BorderColor)

                uiState.innings2?.let { inn ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(uiState.innings2BattingTeamName, color = NeonBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("2nd Innings", color = TextSecondary, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${inn.totalRuns}/${inn.totalWickets}", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("(${inn.totalBalls / 6}.${inn.totalBalls % 6} ov)", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Awards
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("AWARDS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                HorizontalDivider(color = BorderColor)

                // POTM
                uiState.selectedMotm?.let { motm ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("⭐ Player of the Match", color = AmberColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(motm.fullName, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        val motmCandidate = uiState.motmCandidates.find { it.player.id == motm.id }
                        motmCandidate?.let {
                            Column(horizontalAlignment = Alignment.End) {
                                if (it.runs > 0) Text("${it.runs} runs", color = TextSecondary, fontSize = 12.sp)
                                if (it.wickets > 0) Text("${it.wickets} wkts", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Best Batter
                val bestBatter = (uiState.innings1Batting + uiState.innings2Batting).maxByOrNull { it.runs }
                bestBatter?.let {
                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("🏏 Best Batter", color = NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(it.player.fullName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text("${it.runs}(${it.balls})", color = NeonBlue, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Best Bowler
                val bestBowler = (uiState.innings1Bowling + uiState.innings2Bowling).filter { it.wickets > 0 }.maxByOrNull { it.wickets }
                bestBowler?.let {
                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("🎳 Best Bowler", color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(it.player.fullName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text("${it.wickets}/${it.runs}", color = ErrorRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── INNINGS TAB ──────────────────────────────────────────────

@Composable
fun PMInningsTab(
    battingTeamName: String,
    bowlingTeamName: String,
    innings: Innings?,
    batting: List<BatsmanScorecard>,
    bowling: List<BowlerScorecard>,
    bowlerMap: Map<String, String>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Batting header
        item {
            Column(modifier = Modifier.fillMaxWidth().background(SurfaceCard)) {
                Text(
                    battingTeamName, color = NeonGreen, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text("BATTING", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("R", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("B", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("4s", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("6s", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("SR", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
                }
                HorizontalDivider(color = BorderColor)
            }
        }

        // Batters
        val battedList = batting.filter { it.balls > 0 || it.isOut }
        items(battedList) { stats ->
            Column(modifier = Modifier.fillMaxWidth().background(BackgroundDark)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stats.player.fullName,
                            color = if (stats.isOut) TextSecondary else TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (!stats.isOut) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Text(buildPostMatchDismissalText(stats, bowlerMap), color = TextSecondary, fontSize = 10.sp)
                    }
                    Text("${stats.runs}", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("${stats.balls}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("${stats.fours}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("${stats.sixes}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("${"%.1f".format(stats.strikeRate)}", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
                }
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            }
        }

        // Did Not Bat
        val didNotBat = batting.filter { it.balls == 0 && !it.isOut }
        if (didNotBat.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().background(BackgroundDark).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Did Not Bat", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(didNotBat.joinToString(", ") { it.player.fullName }, color = TextSecondary, fontSize = 11.sp)
                }
                HorizontalDivider(color = BorderColor)
            }
        }

        // Extras + Total
        item {
            innings?.let { inn ->
                Column(modifier = Modifier.fillMaxWidth().background(SurfaceCard).padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Extras", color = TextSecondary, fontSize = 13.sp)
                        Text("(w ${inn.wides}, nb ${inn.noBalls}, b 0, lb 0)", color = TextSecondary, fontSize = 11.sp)
                        Text("${inn.extrasTotal}", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("${inn.totalBalls / 6}.${inn.totalBalls % 6} Ov", color = TextSecondary, fontSize = 12.sp)
                        Text("${inn.totalRuns}/${inn.totalWickets}", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                HorizontalDivider(color = BorderColor)
            }
        }

        // Bowling header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.fillMaxWidth().background(SurfaceCard)) {
                Text(
                    bowlingTeamName, color = NeonBlue, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text("BOWLING", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("O", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("M", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("R", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("W", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("Eco", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                }
                HorizontalDivider(color = BorderColor)
            }
        }

        items(bowling) { stats ->
            Column(modifier = Modifier.fillMaxWidth().background(BackgroundDark)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stats.player.fullName, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(stats.overs, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("${stats.maidens}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("${stats.runs}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text(
                        "${stats.wickets}",
                        color = if (stats.wickets > 0) NeonGreen else TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = if (stats.wickets > 0) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.width(28.dp), textAlign = TextAlign.End
                    )
                    Text("${"%.2f".format(stats.economy)}", color = when {
                        stats.economy < 6 -> NeonGreen; stats.economy < 9 -> AmberColor; else -> ErrorRed
                    }, fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                }
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            }
        }
    }
}

fun buildPostMatchDismissalText(stats: BatsmanScorecard, bowlerMap: Map<String, String>): String {
    if (!stats.isOut) return "not out"
    val bowlerName = stats.bowlerOnWicket?.let { bowlerMap[it] } ?: ""
    val fielder = stats.fielderName ?: ""
    return when (stats.dismissalType) {
        "bowled" -> "b $bowlerName"
        "caught" -> if (fielder.isNotBlank()) "c $fielder b $bowlerName" else "c & b $bowlerName"
        "lbw" -> "lbw b $bowlerName"
        "run_out" -> if (fielder.isNotBlank()) "run out ($fielder)" else "run out"
        "stumped" -> if (fielder.isNotBlank()) "st $fielder b $bowlerName" else "st Keeper b $bowlerName"
        "hit_wicket" -> "hit wicket b $bowlerName"
        "retired_out" -> "retired out"
        "retired_hurt" -> "retired hurt"
        "obstructing" -> "obstructing the field"
        "timed_out" -> "timed out"
        "handled_ball" -> "handled the ball"
        "hit_ball_twice" -> "hit the ball twice"
        else -> stats.dismissalType?.replace("_", " ") ?: "out"
    }
}

// ── COMMENTARY TAB ───────────────────────────────────────────

@Composable
fun PMCommentaryTab(uiState: PostMatchUiState) {
    val allBalls = (uiState.inn1Balls + uiState.inn2Balls)
        .sortedWith(compareBy({ it.inningsId }, { it.overNo }, { it.ballNo }))

    if (allBalls.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No commentary available", color = TextSecondary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Group by innings
        val inn1Balls = uiState.inn1Balls.sortedWith(compareBy({ it.overNo }, { it.ballNo }))
        val inn2Balls = uiState.inn2Balls.sortedWith(compareBy({ it.overNo }, { it.ballNo }))

        if (inn1Balls.isNotEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().background(NeonGreen.copy(alpha = 0.1f)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("${uiState.innings1BattingTeamName} — 1st Innings", color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            items(inn1Balls.reversed()) { ball ->
                PMCommentaryRow(ball)
            }
        }

        if (inn2Balls.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().background(NeonBlue.copy(alpha = 0.1f)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("${uiState.innings2BattingTeamName} — 2nd Innings", color = NeonBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            items(inn2Balls.reversed()) { ball ->
                PMCommentaryRow(ball)
            }
        }
    }
}

@Composable
fun PMCommentaryRow(ball: Ball) {
    val isWicket = ball.isWicket && ball.wicketType != "retired_hurt"
    val isFour = ball.isBoundary && !ball.isSix
    val isSix = ball.isSix
    val isWide = ball.extrasType == "wide"
    val isNoBall = ball.extrasType == "no_ball"

    val outcomeText = when {
        isWicket -> "W"; isSix -> "6"; isFour -> "4"
        isWide -> "Wd"; isNoBall -> "Nb"
        ball.runsOffBat == 0 && ball.extrasRuns == null -> "•"
        else -> "${ball.runsOffBat + (ball.extrasRuns ?: 0)}"
    }
    val outcomeColor = when {
        isWicket -> ErrorRed; isSix -> NeonGreen; isFour -> NeonBlue
        isWide || isNoBall -> AmberColor; else -> SurfaceCard
    }

    Column(modifier = Modifier.fillMaxWidth().background(if (isWicket) ErrorRed.copy(alpha = 0.05f) else BackgroundDark)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(48.dp)) {
                if (ball.ballNo > 0) {
                    Text("${ball.overNo}.${ball.ballNo}", color = TextSecondary, fontSize = 10.sp)
                }
                Box(
                    modifier = Modifier.size(30.dp).clip(RoundedCornerShape(4.dp)).background(outcomeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(outcomeText, color = if (outcomeColor == SurfaceCard) TextSecondary else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                ball.commentary ?: "Ball ${ball.overNo}.${ball.ballNo}",
                color = if (isWicket) ErrorRed else TextSecondary,
                fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f)
            )
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
    }
}

// ── OVERS TAB ────────────────────────────────────────────────

@Composable
fun PMOversTab(uiState: PostMatchUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // 1st Innings Overs
        if (uiState.inn1Balls.isNotEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().background(NeonGreen.copy(alpha = 0.1f)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("${uiState.innings1BattingTeamName} — 1st Innings", color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            val inn1OverGroups = uiState.inn1Balls.groupBy { it.overNo }.toSortedMap()
            items(inn1OverGroups.entries.toList()) { (overNo, overBalls) ->
                PMOverRow(overNo, overBalls, uiState)
            }
        }

        // 2nd Innings Overs
        if (uiState.inn2Balls.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().background(NeonBlue.copy(alpha = 0.1f)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("${uiState.innings2BattingTeamName} — 2nd Innings", color = NeonBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            val inn2OverGroups = uiState.inn2Balls.groupBy { it.overNo }.toSortedMap()
            items(inn2OverGroups.entries.toList()) { (overNo, overBalls) ->
                PMOverRow(overNo, overBalls, uiState)
            }
        }
    }
}

@Composable
fun PMOverRow(overNo: Int, overBalls: List<Ball>, uiState: PostMatchUiState) {
    val runsInOver = overBalls.sumOf { b ->
        when {
            b.extrasType == "wide" -> (b.extrasRuns ?: 1) + b.runsOffBat
            b.extrasType == "no_ball" -> 1 + b.runsOffBat + (b.extrasRuns ?: 0)
            else -> b.runsOffBat + (b.extrasRuns ?: 0)
        }
    }
    val wicketsInOver = overBalls.count { it.isWicket && it.wicketType != "retired_hurt" }
    val legalBalls = overBalls.filter { it.extrasType != "wide" && it.extrasType != "no_ball" }
    val rr = if (legalBalls.isNotEmpty()) runsInOver.toDouble() / legalBalls.size * 6 else 0.0
    val bowlerId = overBalls.firstOrNull()?.bowlerId
    val bowlerName = (uiState.innings1Bowling + uiState.innings2Bowling)
        .find { it.player.id == bowlerId }?.player?.fullName ?: ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Ov ${overNo + 1}",
                color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.width(48.dp)
            )
            Text(
                "$runsInOver${if (wicketsInOver > 0) "/$wicketsInOver" else ""}",
                color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.width(48.dp)
            )
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                overBalls.sortedBy { it.ballNo }.forEach { ball ->
                    val label = when {
                        ball.isWicket -> "W"; ball.isSix -> "6"
                        ball.isBoundary -> "4"
                        ball.extrasType == "wide" -> "Wd"
                        ball.extrasType == "no_ball" -> "Nb"
                        ball.runsOffBat == 0 && ball.extrasRuns == null -> "•"
                        else -> "${ball.runsOffBat + (ball.extrasRuns ?: 0)}"
                    }
                    val bgColor = when {
                        ball.isWicket -> ErrorRed; ball.isSix -> NeonGreen
                        ball.isBoundary -> NeonBlue
                        ball.extrasType in listOf("wide", "no_ball") -> AmberColor
                        else -> BorderColor
                    }
                    Box(
                        modifier = Modifier.size(24.dp).clip(CircleShape).background(bgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                }
            }
            Text(
                "${"%.1f".format(rr)}",
                color = when { rr >= 12 -> ErrorRed; rr >= 9 -> AmberColor; rr >= 6 -> NeonGreen; else -> TextSecondary },
                fontSize = 12.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End
            )
        }
        if (bowlerName.isNotEmpty()) {
            Text("🎳 $bowlerName", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
}

// ── ANALYTICS TAB ────────────────────────────────────────────

@Composable
fun PMAnalyticsTab(uiState: PostMatchUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Innings comparison
        item {
            PMAnalyticsCard("Innings Comparison") {
                if (uiState.inn1Balls.isNotEmpty() && uiState.inn2Balls.isNotEmpty()) {
                    val stats1 = computeInningsStats(uiState.inn1Balls)
                    val stats2 = computeInningsStats(uiState.inn2Balls)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(uiState.innings1BattingTeamName, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("", modifier = Modifier.width(80.dp), textAlign = TextAlign.Center)
                        Text(uiState.innings2BattingTeamName, color = NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    }
                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 6.dp))

                    PMCompareRow(stats1.runs.toString(), "Runs", stats2.runs.toString(), stats1.runs > stats2.runs)
                    PMCompareRow(stats1.fours.toString(), "Fours", stats2.fours.toString(), stats1.fours > stats2.fours)
                    PMCompareRow(stats1.sixes.toString(), "Sixes", stats2.sixes.toString(), stats1.sixes > stats2.sixes)
                    PMCompareRow("${"%.2f".format(stats1.runRate)}", "Run Rate", "${"%.2f".format(stats2.runRate)}", stats1.runRate > stats2.runRate)
                    PMCompareRow("${stats1.dotBalls}", "Dot Balls", "${stats2.dotBalls}", stats1.dotBalls < stats2.dotBalls)
                    PMCompareRow("${stats1.extras}", "Extras", "${stats2.extras}", stats1.extras < stats2.extras)
                    PMCompareRow("${"%.1f".format(stats1.boundaryPct)}%", "Boundary%", "${"%.1f".format(stats2.boundaryPct)}%", stats1.boundaryPct > stats2.boundaryPct)
                } else {
                    Text("Need both innings for comparison", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }

        // Phase analysis
        item {
            PMAnalyticsCard("Phase Analysis") {
                listOf(
                    uiState.inn1Balls to uiState.innings1BattingTeamName,
                    uiState.inn2Balls to uiState.innings2BattingTeamName
                ).forEach { (balls, name) ->
                    if (balls.isEmpty()) return@forEach
                    Text(name, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    val phases = listOf("powerplay" to "PP", "middle" to "Mid", "death" to "Death")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        phases.forEach { (phase, label) ->
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
                                    .background(BackgroundDark)
                                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(label, color = TextSecondary, fontSize = 10.sp)
                                Text("$runs/$wkts", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("RR ${"%.1f".format(rr)}", color = NeonGreen, fontSize = 10.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Dismissal types
        item {
            PMAnalyticsCard("Dismissal Types") {
                val allWickets = (uiState.inn1Balls + uiState.inn2Balls)
                    .filter { it.isWicket && it.wicketType != "retired_hurt" }
                val groups = allWickets.groupBy {
                    it.wicketType?.replace("_", " ")?.replaceFirstChar { c -> c.uppercase() } ?: "Unknown"
                }
                if (groups.isEmpty()) {
                    Text("No wickets yet", color = TextSecondary, fontSize = 12.sp)
                } else {
                    groups.forEach { (type, balls) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(type, color = TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .height(16.dp)
                                    .width((balls.size * 30).dp.coerceAtMost(100.dp))
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ErrorRed.copy(alpha = 0.6f))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${balls.size}", color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp), textAlign = TextAlign.End)
                        }
                    }
                }
            }
        }

        // Extras distribution
        item {
            PMAnalyticsCard("Extras Distribution") {
                listOf(
                    uiState.inn1Balls to uiState.innings1BattingTeamName,
                    uiState.inn2Balls to uiState.innings2BattingTeamName
                ).forEach { (balls, name) ->
                    if (balls.isEmpty()) return@forEach
                    val wides = balls.count { it.extrasType == "wide" }
                    val noBalls = balls.count { it.extrasType == "no_ball" }
                    val byes = balls.count { it.extrasType == "bye" }
                    val legByes = balls.count { it.extrasType == "leg_bye" }
                    val total = wides + noBalls + byes + legByes
                    Text(name, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PMStatBox("W", "$wides", AmberColor, Modifier.weight(1f))
                        PMStatBox("NB", "$noBalls", ErrorRed, Modifier.weight(1f))
                        PMStatBox("B", "$byes", NeonBlue, Modifier.weight(1f))
                        PMStatBox("LB", "$legByes", PurpleColor, Modifier.weight(1f))
                        PMStatBox("Tot", "$total", TextPrimary, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

data class InningsQuickStats(
    val runs: Int, val fours: Int, val sixes: Int,
    val runRate: Double, val dotBalls: Int,
    val extras: Int, val boundaryPct: Double
)

fun computeInningsStats(balls: List<Ball>): InningsQuickStats {
    val runs = balls.sumOf { b ->
        when {
            b.extrasType == "wide" -> (b.extrasRuns ?: 1) + b.runsOffBat
            b.extrasType == "no_ball" -> 1 + b.runsOffBat + (b.extrasRuns ?: 0)
            else -> b.runsOffBat + (b.extrasRuns ?: 0)
        }
    }
    val fours = balls.count { it.isBoundary && !it.isSix }
    val sixes = balls.count { it.isSix }
    val legal = balls.count { it.extrasType != "wide" && it.extrasType != "no_ball" }
    val rr = if (legal > 0) runs * 6.0 / legal else 0.0
    // Dot ball: a LEGAL delivery conceding nothing. Must exclude no-balls (they always
    // concede the penalty) and treat extrasRuns 0 the same as null.
    val dots = balls.count {
        it.extrasType != "wide" && it.extrasType != "no_ball" &&
                it.runsOffBat == 0 && (it.extrasRuns ?: 0) == 0
    }
    val extras = balls.sumOf { it.extrasRuns ?: 0 } + balls.count { it.extrasType == "wide" } + balls.count { it.extrasType == "no_ball" }
    val batRuns = balls.sumOf { it.runsOffBat }
    val bdryRuns = (fours * 4) + (sixes * 6)
    val bdryPct = if (batRuns > 0) bdryRuns * 100.0 / batRuns else 0.0
    return InningsQuickStats(runs, fours, sixes, rr, dots, extras, bdryPct)
}

// ── POTM TAB ─────────────────────────────────────────────────

@Composable
fun PMPotmTab(uiState: PostMatchUiState, onSelectMotm: (Player) -> Unit) {
    if (uiState.motmCandidates.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No candidates available", color = TextSecondary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "Select Player of the Match",
                color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(uiState.motmCandidates) { candidate ->
            val isSelected = uiState.selectedMotm?.id == candidate.player.id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) AmberColor.copy(alpha = 0.15f) else SurfaceCard)
                    .border(1.dp, if (isSelected) AmberColor else BorderColor, RoundedCornerShape(10.dp))
                    .clickable { onSelectMotm(candidate.player) }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        candidate.player.fullName,
                        color = if (isSelected) AmberColor else TextPrimary,
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        buildString {
                            if (candidate.runs > 0) append("${candidate.runs} runs (${"%.1f".format(candidate.strikeRate)} SR)")
                            if (candidate.runs > 0 && candidate.wickets > 0) append("  •  ")
                            if (candidate.wickets > 0) append("${candidate.wickets} wkts (${"%.1f".format(candidate.economy)} eco)")
                        },
                        color = TextSecondary, fontSize = 12.sp
                    )
                }
                if (isSelected) Text("⭐", fontSize = 20.sp)
                else Text("${"%.1f".format(candidate.score)}", color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

// ── HELPER COMPOSABLES ───────────────────────────────────────

@Composable
fun PMAnalyticsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
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
fun PMStatBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BackgroundDark)
            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(label, color = TextSecondary, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun PMCompareRow(val1: String, label: String, val2: String, val1Better: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            val1,
            color = if (val1Better) NeonGreen else TextPrimary,
            fontSize = 13.sp,
            fontWeight = if (val1Better) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Text(label, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text(
            val2,
            color = if (!val1Better) NeonGreen else TextPrimary,
            fontSize = 13.sp,
            fontWeight = if (!val1Better) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f), textAlign = TextAlign.End
        )
    }
}