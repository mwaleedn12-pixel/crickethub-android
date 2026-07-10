package com.crickethub.ui.match.live

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crickethub.data.model.Ball
import com.crickethub.data.model.BatsmanStats
import com.crickethub.data.model.BowlerStats

private val BackgroundDark = Color(0xFF030712)
private val SurfaceCard = Color(0xFF111827)
private val BorderColor = Color(0xFF1F2937)
private val NeonGreen = Color(0xFF10B981)
private val NeonBlue = Color(0xFF3B82F6)
private val TextPrimary = Color(0xFFF9FAFB)
private val TextSecondary = Color(0xFF9CA3AF)
private val ErrorRed = Color(0xFFEF4444)
private val AmberColor = Color(0xFFF59E0B)

@Composable
fun LiveScorecardScreen(
    matchId: String,
    onBack: () -> Unit,
    viewModel: LiveScorecardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Scorecard", "Commentary", "Overs", "Partnership", "MVP", "Summary")

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {

        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${uiState.battingTeamName} vs ${uiState.bowlingTeamName}",
                    fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    uiState.matchStatus,
                    fontSize = 11.sp,
                    color = when (uiState.matchStatus) {
                        "LIVE" -> NeonGreen
                        "COMPLETED" -> TextSecondary
                        else -> AmberColor
                    }
                )
            }
            if (uiState.matchStatus == "LIVE") {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(ErrorRed)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("● LIVE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            IconButton(onClick = {
                val shareText = buildString {
                    appendLine("🏏 ${uiState.battingTeamName} vs ${uiState.bowlingTeamName}")
                    appendLine("${uiState.totalRuns}/${uiState.totalWickets} (${uiState.currentOver}.${uiState.currentBall} ov)")
                    appendLine("CRR: ${"%.2f".format(uiState.currentRunRate)}")
                    uiState.target?.let { appendLine("Target: $it | RRR: ${"%.2f".format(uiState.requiredRunRate ?: 0.0)}") }
                }
                clipboardManager.setText(AnnotatedString(shareText))
            }) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = NeonGreen)
            }
        }

        // Score box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceCard)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "${uiState.totalRuns}/${uiState.totalWickets}",
                        fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                    )
                    Text(
                        "(${uiState.currentOver}.${uiState.currentBall} ov)",
                        color = TextSecondary, fontSize = 13.sp
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CRR", color = TextSecondary, fontSize = 11.sp)
                    Text(
                        "${"%.2f".format(uiState.currentRunRate)}",
                        color = NeonGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold
                    )
                }
                if (uiState.target != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Need ${uiState.target!! - uiState.totalRuns} off ${uiState.ballsLeft}b",
                            color = AmberColor, fontSize = 13.sp, fontWeight = FontWeight.Bold
                        )
                        Text(
                            "RRR: ${"%.2f".format(uiState.requiredRunRate ?: 0.0)}",
                            color = if ((uiState.requiredRunRate ?: 0.0) > uiState.currentRunRate) ErrorRed else NeonGreen,
                            fontSize = 12.sp
                        )
                        Text("T: ${uiState.target}", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }

        // This over
        if (uiState.last6Balls.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("This over:", color = TextSecondary, fontSize = 12.sp)
                uiState.last6Balls.forEach { ball ->
                    val (bgColor, textColor) = when (ball) {
                        "W" -> ErrorRed to Color.White
                        "4" -> NeonBlue to Color.White
                        "6" -> NeonGreen to Color.Black
                        "Wd", "Nb" -> AmberColor to Color.Black
                        "0" -> SurfaceCard to TextSecondary
                        else -> SurfaceCard to TextPrimary
                    }
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(bgColor)
                            .border(1.dp, BorderColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(ball, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
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
            when (selectedTab) {
                0 -> LiveScorecardTab(uiState)
                1 -> LiveCommentaryTab(uiState)
                2 -> LiveOversTab(uiState)
                3 -> LivePartnershipTab(uiState)
                4 -> LiveMvpTab(uiState)
                5 -> LiveSummaryTab(uiState)
            }
        }
    }
}

// ── SCORECARD TAB ────────────────────────────────────────────

@Composable
fun LiveScorecardTab(uiState: LiveScorecardUiState) {
    val bowlerMap = uiState.bowlerStats.values.associate { it.player.id to it.player.fullName }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Batting header
        item {
            Column(modifier = Modifier.fillMaxWidth().background(SurfaceCard)) {
                Text(
                    uiState.battingTeamName,
                    color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold,
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

        // Batsmen
        val battedList = uiState.batsmanStats.values.filter { it.balls > 0 || it.isOut }
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
                        Text(
                            buildDismissalText(stats, bowlerMap),
                            color = TextSecondary, fontSize = 10.sp
                        )
                    }
                    Text("${stats.runs}", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("${stats.balls}", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("${stats.fours}", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("${stats.sixes}", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("${"%.1f".format(stats.strikeRate)}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
                }
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            }
        }

        // Did not bat
        val didNotBat = uiState.batsmanStats.values.filter { it.balls == 0 && !it.isOut }
        if (didNotBat.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().background(BackgroundDark)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Did Not Bat", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(didNotBat.joinToString(", ") { it.player.fullName }, color = TextSecondary, fontSize = 11.sp)
                }
                HorizontalDivider(color = BorderColor)
            }
        }

        // Extras + Total
        item {
            Column(
                modifier = Modifier.fillMaxWidth().background(SurfaceCard)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Extras", color = TextSecondary, fontSize = 13.sp)
                    Text("(w ${uiState.wides}, nb ${uiState.noBalls}, b 0, lb 0)", color = TextSecondary, fontSize = 11.sp)
                    Text("${uiState.extrasTotal}", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("${uiState.currentOver}.${uiState.currentBall} Ov", color = TextSecondary, fontSize = 12.sp)
                    Text("${uiState.totalRuns}/${uiState.totalWickets}", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(color = BorderColor)
        }

        // Fall of Wickets
        val fow = buildFallOfWickets(uiState)
        if (fow.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().background(BackgroundDark)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Fall of Wickets", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(fow, color = TextSecondary, fontSize = 11.sp, lineHeight = 18.sp)
                }
                HorizontalDivider(color = BorderColor)
            }
        }

        // Bowling header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.fillMaxWidth().background(SurfaceCard)) {
                Text(
                    uiState.bowlingTeamName,
                    color = NeonBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold,
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

        // Bowlers
        val bowlingList = uiState.bowlerStats.values.toList()
        items(bowlingList) { stats ->
            Column(modifier = Modifier.fillMaxWidth().background(BackgroundDark)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stats.player.fullName, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(stats.overs, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("0", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("${stats.runs}", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text(
                        "${stats.wickets}",
                        color = if (stats.wickets > 0) NeonGreen else TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = if (stats.wickets > 0) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.width(28.dp), textAlign = TextAlign.End
                    )
                    Text(
                        "${"%.2f".format(stats.economy)}",
                        color = when {
                            stats.economy < 6 -> NeonGreen
                            stats.economy < 9 -> AmberColor
                            else -> ErrorRed
                        },
                        fontSize = 12.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End
                    )
                }
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            }
        }
    }
}

fun buildDismissalText(stats: BatsmanStats, bowlerMap: Map<String, String>): String {
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

fun buildFallOfWickets(uiState: LiveScorecardUiState): String {
    if (uiState.balls.isEmpty()) return ""
    var runningRuns = 0
    var wicketNo = 0
    val fowList = mutableListOf<String>()
    uiState.balls.sortedWith(compareBy({ it.overNo }, { it.ballNo })).forEach { ball ->
        val runsThisBall = when {
            ball.extrasType == "wide" -> (ball.extrasRuns ?: 1) + ball.runsOffBat
            ball.extrasType == "no_ball" -> 1 + ball.runsOffBat + (ball.extrasRuns ?: 0)
            else -> ball.runsOffBat + (ball.extrasRuns ?: 0)
        }
        runningRuns += runsThisBall
        if (ball.isWicket && ball.wicketType != "retired_hurt") {
            wicketNo++
            val batsmanName = uiState.batsmanStats.values
                .find { it.player.id == ball.batsmanId }?.player?.fullName ?: "Batsman"
            fowList.add("$wicketNo-$runningRuns ($batsmanName, ${ball.overNo}.${ball.ballNo} ov)")
        }
    }
    return fowList.joinToString("\n")
}

// ── COMMENTARY TAB ───────────────────────────────────────────

@Composable
fun LiveCommentaryTab(uiState: LiveScorecardUiState) {
    if (uiState.commentary.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No commentary yet", color = TextSecondary)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(uiState.commentary.reversed()) { comment ->
            LiveCommentaryRow(text = comment)
        }
    }
}

@Composable
fun LiveCommentaryRow(text: String) {
    val parts = text.split("|").map { it.trim() }
    val overBall = parts.getOrNull(0) ?: ""
    val outcome = parts.getOrNull(1) ?: ""
    val description = parts.getOrNull(2) ?: text

    val isWicket = outcome == "W" || description.contains("BOWLED") ||
            description.contains("CAUGHT") || description.contains("LBW") ||
            description.contains("RUN OUT") || description.contains("STUMPED") ||
            description.contains("OUT")
    val isFour = outcome == "4" || description.contains("FOUR")
    val isSix = outcome == "6" || description.contains("SIX")

    val outcomeText = when {
        isWicket -> "W"; isSix -> "6"; isFour -> "4"
        outcome.isNotEmpty() -> outcome; else -> "•"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isWicket) ErrorRed.copy(alpha = 0.05f) else BackgroundDark)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(48.dp)) {
                if (overBall.contains(".")) {
                    Text(overBall, color = TextSecondary, fontSize = 11.sp)
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when {
                                isWicket -> ErrorRed; isSix -> NeonGreen
                                isFour -> NeonBlue
                                outcome in listOf("Wd", "Nb") -> AmberColor
                                else -> SurfaceCard
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        outcomeText,
                        color = when {
                            isWicket || isSix || isFour -> Color.White
                            outcome in listOf("Wd", "Nb") -> Color.Black
                            else -> TextSecondary
                        },
                        fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                description.ifEmpty { text },
                color = if (isWicket) ErrorRed else TextSecondary,
                fontSize = 13.sp, lineHeight = 18.sp,
                modifier = Modifier.weight(1f)
            )
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
    }
}

// ── OVERS TAB ────────────────────────────────────────────────

@Composable
fun LiveOversTab(uiState: LiveScorecardUiState) {
    val overGroups = uiState.balls.groupBy { it.overNo }.toSortedMap()

    if (overGroups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No overs data yet", color = TextSecondary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceCard)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("Ov", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(32.dp))
                Text("Runs", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
                Text("Wkts", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
                Text("Balls", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("RR", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
            }
        }

        items(overGroups.entries.toList()) { (overNo, overBalls) ->
            val runsInOver = overBalls.sumOf { ball ->
                when {
                    ball.extrasType == "wide" -> (ball.extrasRuns ?: 1) + ball.runsOffBat
                    ball.extrasType == "no_ball" -> 1 + ball.runsOffBat + (ball.extrasRuns ?: 0)
                    else -> ball.runsOffBat + (ball.extrasRuns ?: 0)
                }
            }
            val wicketsInOver = overBalls.count { it.isWicket && it.wicketType != "retired_hurt" }
            val legalBalls = overBalls.filter { it.extrasType != "wide" && it.extrasType != "no_ball" }
            val rr = if (legalBalls.isNotEmpty()) runsInOver.toDouble() / legalBalls.size * 6 else 0.0
            val bowlerName = uiState.bowlerStats.values.find { stats ->
                overBalls.any { it.bowlerId == stats.player.id }
            }?.player?.fullName ?: ""

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceCard)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${overNo + 1}",
                        color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(32.dp)
                    )
                    Text(
                        "$runsInOver",
                        color = TextPrimary, fontSize = 14.sp,
                        modifier = Modifier.width(40.dp), textAlign = TextAlign.Center
                    )
                    Text(
                        if (wicketsInOver > 0) "$wicketsInOver" else "-",
                        color = if (wicketsInOver > 0) ErrorRed else TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = if (wicketsInOver > 0) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.width(40.dp), textAlign = TextAlign.Center
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
                        color = when {
                            rr >= 12 -> ErrorRed; rr >= 8 -> AmberColor
                            rr >= 6 -> NeonGreen; else -> TextSecondary
                        },
                        fontSize = 13.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End
                    )
                }
                if (bowlerName.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("🎳 $bowlerName", color = TextSecondary, fontSize = 10.sp)
                }
            }
        }
    }
}

// ── PARTNERSHIP TAB ──────────────────────────────────────────

@Composable
fun LivePartnershipTab(uiState: LiveScorecardUiState) {
    if (uiState.balls.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No partnership data yet", color = TextSecondary)
        }
        return
    }

    // Compute partnerships from balls
    data class Partnership(
        val batter1: String, val batter2: String,
        val runs: Int, val balls: Int,
        val fours: Int, val sixes: Int,
        val wicketNo: Int
    )

    val partnerships = mutableListOf<Partnership>()
    var currentBatter1 = ""
    var currentBatter2 = ""
    var pRuns = 0; var pBalls = 0; var pFours = 0; var pSixes = 0
    var wicketNo = 0

    uiState.balls.sortedWith(compareBy({ it.overNo }, { it.ballNo })).forEach { ball ->
        if (currentBatter1.isEmpty()) currentBatter1 = ball.batsmanId ?: ""
        if (currentBatter2.isEmpty() && ball.nonStrikerId != null) currentBatter2 = ball.nonStrikerId

        val runs = when {
            ball.extrasType == "wide" -> (ball.extrasRuns ?: 1) + ball.runsOffBat
            ball.extrasType == "no_ball" -> 1 + ball.runsOffBat + (ball.extrasRuns ?: 0)
            else -> ball.runsOffBat + (ball.extrasRuns ?: 0)
        }
        pRuns += runs
        if (ball.extrasType != "wide") pBalls++
        if (ball.isBoundary && !ball.isSix) pFours++
        if (ball.isSix) pSixes++

        if (ball.isWicket && ball.wicketType != "retired_hurt") {
            wicketNo++
            val b1Name = uiState.batsmanStats.values.find { it.player.id == currentBatter1 }?.player?.fullName ?: currentBatter1.take(8)
            val b2Name = uiState.batsmanStats.values.find { it.player.id == currentBatter2 }?.player?.fullName ?: currentBatter2.take(8)
            partnerships.add(Partnership(b1Name, b2Name, pRuns, pBalls, pFours, pSixes, wicketNo))
            pRuns = 0; pBalls = 0; pFours = 0; pSixes = 0
            currentBatter1 = ""
            currentBatter2 = ball.nonStrikerId ?: ""
        }
    }

    // Current partnership
    val b1Name = uiState.batsmanStats.values.find { it.player.id == currentBatter1 }?.player?.fullName ?: ""
    val b2Name = uiState.batsmanStats.values.find { it.player.id == currentBatter2 }?.player?.fullName ?: ""

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Current partnership
        if (pBalls > 0 || pRuns > 0) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonGreen.copy(alpha = 0.1f))
                        .border(1.dp, NeonGreen, RoundedCornerShape(10.dp))
                        .padding(14.dp)
                ) {
                    Text("Current Partnership", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("$b1Name & $b2Name", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("$pRuns ($pBalls)", color = NeonGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("4s: $pFours", color = NeonBlue, fontSize = 12.sp)
                        Text("6s: $pSixes", color = NeonGreen, fontSize = 12.sp)
                        val rr = if (pBalls > 0) pRuns.toDouble() / pBalls * 6 else 0.0
                        Text("RR: ${"%.2f".format(rr)}", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        // Previous partnerships
        if (partnerships.isNotEmpty()) {
            item {
                Text(
                    "Previous Partnerships",
                    color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            items(partnerships.reversed()) { p ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceCard)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${p.batter1} & ${p.batter2}", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("4s: ${p.fours}  6s: ${p.sixes}", color = TextSecondary, fontSize = 11.sp)
                    }
                    Text(
                        "${p.runs} (${p.balls})",
                        color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── MVP TAB ──────────────────────────────────────────────────

@Composable
fun LiveMvpTab(uiState: LiveScorecardUiState) {
    data class MvpEntry(val name: String, val score: Double, val runs: Int, val wickets: Int, val sr: Double, val eco: Double)

    val mvpList = mutableListOf<MvpEntry>()
    uiState.batsmanStats.values.forEach { bat ->
        val bowl = uiState.bowlerStats[bat.player.id]
        val wickets = bowl?.wickets ?: 0
        val eco = bowl?.economy ?: 99.0
        val impact = bat.runs * (bat.strikeRate / 100.0) + wickets * 25.0 -
                if (eco < 99 && (bowl?.balls ?: 0) >= 6) eco * (bowl!!.balls / 6.0) else 0.0
        if (bat.runs > 0 || wickets > 0) {
            mvpList.add(MvpEntry(bat.player.fullName, impact, bat.runs, wickets, bat.strikeRate, eco))
        }
    }
    mvpList.sortByDescending { it.score }

    if (mvpList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No MVP data yet", color = TextSecondary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            Text("Impact Players", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(SurfaceCard).padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Player", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text("Impact", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
                Text("Runs", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                Text("Wkts", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                Text("SR/Eco", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
            }
        }
        items(mvpList) { entry ->
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceCard)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(entry.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${"%.1f".format(entry.score)}", color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
                Text("${entry.runs}", color = if (entry.runs >= 50) AmberColor else TextPrimary, fontSize = 13.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                Text(if (entry.wickets > 0) "${entry.wickets}" else "-", color = if (entry.wickets >= 3) ErrorRed else TextPrimary, fontSize = 13.sp, fontWeight = if (entry.wickets >= 3) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                Text(
                    if (entry.wickets > 0 && entry.eco < 99) "${"%.1f".format(entry.eco)}" else "${"%.0f".format(entry.sr)}",
                    color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(52.dp), textAlign = TextAlign.End
                )
            }
        }
    }
}

// ── SUMMARY TAB ──────────────────────────────────────────────

@Composable
fun LiveSummaryTab(uiState: LiveScorecardUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Result / Status
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .padding(16.dp)
            ) {
                Text("STATUS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    uiState.resultText.ifEmpty {
                        if (uiState.matchStatus == "LIVE") "Match in progress..." else uiState.matchStatus
                    },
                    color = if (uiState.resultText.isNotEmpty()) NeonGreen else TextSecondary,
                    fontSize = 16.sp, fontWeight = FontWeight.Bold
                )
            }
        }

        // Score summary
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("SCORE SUMMARY", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                HorizontalDivider(color = BorderColor)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(uiState.battingTeamName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${uiState.totalRuns}/${uiState.totalWickets} (${uiState.currentOver}.${uiState.currentBall} ov)",
                        color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                }
                if (uiState.target != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Target", color = TextSecondary, fontSize = 13.sp)
                        Text("${uiState.target}", color = AmberColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Runs needed", color = TextSecondary, fontSize = 13.sp)
                        Text("${uiState.target!! - uiState.totalRuns} off ${uiState.ballsLeft} balls", color = ErrorRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                HorizontalDivider(color = BorderColor)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CRR", color = TextSecondary, fontSize = 11.sp)
                        Text("${"%.2f".format(uiState.currentRunRate)}", color = NeonGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    uiState.requiredRunRate?.let {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("RRR", color = TextSecondary, fontSize = 11.sp)
                            Text("${"%.2f".format(it)}", color = if (it > uiState.currentRunRate) ErrorRed else NeonGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Top performers
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("TOP PERFORMERS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                HorizontalDivider(color = BorderColor)
                val topBat = uiState.batsmanStats.values.maxByOrNull { it.runs }
                topBat?.let {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("🏏 Top Scorer", color = TextSecondary, fontSize = 11.sp)
                            Text(it.player.fullName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text("${it.runs}(${it.balls})", color = AmberColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                val topBowl = uiState.bowlerStats.values.filter { it.wickets > 0 }.maxByOrNull { it.wickets }
                topBowl?.let {
                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("🎳 Top Bowler", color = TextSecondary, fontSize = 11.sp)
                            Text(it.player.fullName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text("${it.wickets}/${it.runs}", color = NeonGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Extras
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("EXTRAS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                HorizontalDivider(color = BorderColor)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Wides", color = TextSecondary, fontSize = 11.sp)
                        Text("${uiState.wides}", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No Balls", color = TextSecondary, fontSize = 11.sp)
                        Text("${uiState.noBalls}", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Extras", color = TextSecondary, fontSize = 11.sp)
                        Text("${uiState.extrasTotal}", color = AmberColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}