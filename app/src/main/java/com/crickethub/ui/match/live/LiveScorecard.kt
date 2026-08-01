package com.crickethub.ui.match.live

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import com.crickethub.ui.components.CricketAnimatedBackground
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import com.crickethub.ui.theme.*


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

    CricketAnimatedBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = CH.textPrimary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${uiState.battingTeamName} vs ${uiState.bowlingTeamName}",
                        fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CH.textPrimary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        uiState.matchStatus,
                        fontSize = 11.sp,
                        color = when (uiState.matchStatus) {
                            "LIVE" -> NeonGreen
                            "COMPLETED" -> CH.textSecondary
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
                    .background(CH.surface)
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
                            fontSize = 32.sp, fontWeight = FontWeight.Bold, color = CH.textPrimary
                        )
                        Text(
                            "(${uiState.currentOver}.${uiState.currentBall} ov)",
                            color = CH.textSecondary, fontSize = 13.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CRR", color = CH.textSecondary, fontSize = 11.sp)
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
                            Text("T: ${uiState.target}", color = CH.textSecondary, fontSize = 11.sp)
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
                    Text("This over:", color = CH.textSecondary, fontSize = 12.sp)
                    uiState.last6Balls.forEach { ball ->
                        val (bgColor, textColor) = when {
                            ball.startsWith("W") -> ErrorRed to Color.White
                            ball == "4" -> NeonBlue to Color.White
                            ball == "6" -> NeonGreen to Color.Black
                            ball.startsWith("Wd") || ball.startsWith("Nb") -> AmberColor to Color.Black
                            ball == "0" || ball == "•" -> CH.surface to CH.textSecondary
                            else -> CH.surface to CH.textPrimary
                        }
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(bgColor)
                                .border(1.dp, CH.border, CircleShape),
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
                                tab, fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) NeonGreen else CH.textSecondary
                            )
                        }
                    )
                }
            }

            if (uiState.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚠️", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(uiState.error ?: "Unknown error", color = CH.textSecondary, fontSize = 14.sp,
                            textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                    }
                }
            } else if (uiState.isLoading) {
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
} // CricketAnimatedBackground

// ══════════════════════════════════════════════════════════════
// HELPER: Determine batting order from ball data
// Returns list of player IDs in the order they first appeared
// at the crease (as batsmanId or nonStrikerId).
// ══════════════════════════════════════════════════════════════

/**
 * Returns player IDs in crease-arrival order (batting number).
 * Scans balls chronologically; the first time a player appears
 * as batsmanId or nonStrikerId, that's their batting position.
 */
private fun battingOrderFromBalls(balls: List<Ball>, strikerId: String? = null, nonStrikerId: String? = null): List<String> {
    val order = mutableListOf<String>()
    val seen = mutableSetOf<String>()
    // If striker/nonStriker are set but have no balls yet, add them first
    listOfNotNull(strikerId, nonStrikerId).forEach { id ->
        if (id !in seen) { seen.add(id); order.add(id) }
    }
    // Walk through balls chronologically
    balls.sortedWith(compareBy({ it.overNo }, { it.ballNo })).forEach { ball ->
        ball.batsmanId.let { id -> if (id !in seen) { seen.add(id); order.add(id) } }
        ball.nonStrikerId?.let { id -> if (id !in seen) { seen.add(id); order.add(id) } }
    }
    return order
}

/**
 * Returns bowler IDs in the order they first bowled.
 */
private fun bowlingOrderFromBalls(balls: List<Ball>): List<String> {
    val order = mutableListOf<String>()
    val seen = mutableSetOf<String>()
    balls.sortedWith(compareBy({ it.overNo }, { it.ballNo })).forEach { ball ->
        val id = ball.bowlerId
        if (id !in seen) { seen.add(id); order.add(id) }
    }
    return order
}

// ══════════════════════════════════════════════════════════════
// SCORECARD TAB — shows completed innings ABOVE current innings
// ══════════════════════════════════════════════════════════════

@Composable
fun LiveScorecardTab(uiState: LiveScorecardUiState) {
    val bowlerMap = uiState.bowlerStats.values.associate { it.player.id to it.player.fullName }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // ── Completed innings (1st innings when viewing 2nd) ──
        uiState.completedInnings.forEach { completed ->
            completedInningsSection(completed)
        }

        // ── Current (live) innings ──

        // Ball-by-ball timeline (reverse chronological, over separators)
        item { LiveBallTimeline(uiState) }

        // Batting header
        item {
            Column(modifier = Modifier.fillMaxWidth().background(CH.surface)) {
                Text(
                    uiState.battingTeamName,
                    color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text("BATTING", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("R", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("B", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("4s", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("6s", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("SR", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
                }
                HorizontalDivider(color = CH.border)
            }
        }

        // Batsmen — sorted by batting order (crease arrival)
        val appearedIds = (uiState.balls.flatMap { listOfNotNull(it.batsmanId, it.nonStrikerId) } +
                listOfNotNull(uiState.strikerId, uiState.nonStrikerId)).toSet()
        val battedList = uiState.batsmanStats.values.filter { it.balls > 0 || it.isOut || it.player.id in appearedIds }
        // Sort by batting order (first to crease = position 1)
        val batOrder = battingOrderFromBalls(uiState.balls, uiState.strikerId, uiState.nonStrikerId)
        val sortedBattedList = battedList.sortedBy { stat ->
            val idx = batOrder.indexOf(stat.player.id)
            if (idx >= 0) idx else Int.MAX_VALUE
        }
        items(sortedBattedList) { stats ->
            Column(modifier = Modifier.fillMaxWidth().background(if (isSystemInDarkTheme()) Color(0xFF0A0A0A) else Color(0xFFF7F3EA))) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stats.player.fullName,
                            color = if (stats.isOut) CH.textSecondary else CH.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (!stats.isOut) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Text(
                            buildDismissalText(stats, bowlerMap),
                            color = CH.textSecondary, fontSize = 10.sp
                        )
                    }
                    Text("${stats.runs}", color = CH.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("${stats.balls}", color = CH.textSecondary, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("${stats.fours}", color = CH.textSecondary, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("${stats.sixes}", color = CH.textSecondary, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("${"%.1f".format(stats.strikeRate)}", color = CH.textSecondary, fontSize = 12.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
                }
                HorizontalDivider(color = CH.border, thickness = 0.5.dp)
            }
        }

        // Did not bat
        val didNotBat = uiState.batsmanStats.values.filter {
            it.balls == 0 && !it.isOut && it.player.id !in appearedIds
        }
        if (didNotBat.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().background(if (isSystemInDarkTheme()) Color(0xFF0A0A0A) else Color(0xFFF7F3EA))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Did Not Bat", color = CH.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(didNotBat.joinToString(", ") { it.player.fullName }, color = CH.textSecondary, fontSize = 11.sp)
                }
                HorizontalDivider(color = CH.border)
            }
        }

        // Extras + Total
        item {
            Column(
                modifier = Modifier.fillMaxWidth().background(CH.surface)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Extras", color = CH.textSecondary, fontSize = 13.sp)
                    Text("(w ${uiState.wides}, nb ${uiState.noBalls}, b 0, lb 0)", color = CH.textSecondary, fontSize = 11.sp)
                    Text("${uiState.extrasTotal}", color = CH.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total", color = CH.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("${uiState.currentOver}.${uiState.currentBall} Ov", color = CH.textSecondary, fontSize = 12.sp)
                    Text("${uiState.totalRuns}/${uiState.totalWickets}", color = CH.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(color = CH.border)
        }

        // Fall of Wickets
        val fow = buildFallOfWickets(uiState)
        if (fow.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().background(if (isSystemInDarkTheme()) Color(0xFF0A0A0A) else Color(0xFFF7F3EA))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Fall of Wickets", color = CH.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(fow, color = CH.textSecondary, fontSize = 11.sp, lineHeight = 18.sp)
                }
                HorizontalDivider(color = CH.border)
            }
        }

        // Bowling header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.fillMaxWidth().background(CH.surface)) {
                Text(
                    uiState.bowlingTeamName,
                    color = NeonBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text("BOWLING", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("O", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("M", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("R", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("W", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("Eco", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                }
                HorizontalDivider(color = CH.border)
            }
        }

        // Bowlers — sorted by bowling order (first to bowl = top)
        val bowlOrder = bowlingOrderFromBalls(uiState.balls)
        val sortedBowlingList = uiState.bowlerStats.values.toList().sortedBy { stat ->
            val idx = bowlOrder.indexOf(stat.player.id)
            if (idx >= 0) idx else Int.MAX_VALUE
        }
        items(sortedBowlingList) { stats ->
            Column(modifier = Modifier.fillMaxWidth().background(if (isSystemInDarkTheme()) Color(0xFF0A0A0A) else Color(0xFFF7F3EA))) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stats.player.fullName, color = CH.textPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(stats.overs, color = CH.textSecondary, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("${stats.maidens}", color = CH.textSecondary, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("${stats.runs}", color = CH.textSecondary, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text(
                        "${stats.wickets}",
                        color = if (stats.wickets > 0) NeonGreen else CH.textSecondary,
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
                HorizontalDivider(color = CH.border, thickness = 0.5.dp)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// Completed innings section (reusable for each completed innings)
// ══════════════════════════════════════════════════════════════

private fun LazyListScope.completedInningsSection(data: CompletedInningsData) {
    val inn = data.innings
    val bowlerMap = data.bowlerStats.values.associate { it.player.id to it.player.fullName }
    val inningsLabel = when (inn.inningsNo) {
        1 -> "1st Innings"; 2 -> "2nd Innings"; else -> "${inn.inningsNo}th Innings"
    }
    val legalBalls = inn.totalBalls
    val oversText = "${legalBalls / 6}.${legalBalls % 6}"

    // Innings banner
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NeonGreen.copy(alpha = 0.10f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${data.battingTeamName} — $inningsLabel",
                color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold
            )
            Text(
                "${inn.totalRuns}/${inn.totalWickets} ($oversText)",
                color = CH.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold
            )
        }
    }

    // Batting header
    item {
        Column(modifier = Modifier.fillMaxWidth().background(CH.surface)) {
            Text(
                data.battingTeamName,
                color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text("BATTING", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text("R", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("B", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("4s", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("6s", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("SR", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
            }
            HorizontalDivider(color = CH.border)
        }
    }

    // Batsmen in batting order (0(0) batsmen included via appearedIds)
    val appearedIds = data.balls.flatMap { listOfNotNull(it.batsmanId, it.nonStrikerId) }.toSet()
    val battedList = data.batsmanStats.values.filter { it.balls > 0 || it.isOut || it.player.id in appearedIds }
    val batOrder = battingOrderFromBalls(data.balls)
    val sortedBattedList = battedList.sortedBy { stat ->
        val idx = batOrder.indexOf(stat.player.id)
        if (idx >= 0) idx else Int.MAX_VALUE
    }
    items(sortedBattedList) { stats ->
        Column(modifier = Modifier.fillMaxWidth().background(if (isSystemInDarkTheme()) Color(0xFF0A0A0A) else Color(0xFFF7F3EA))) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stats.player.fullName,
                        color = if (stats.isOut) CH.textSecondary else CH.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = if (!stats.isOut) FontWeight.SemiBold else FontWeight.Normal
                    )
                    Text(buildDismissalText(stats, bowlerMap), color = CH.textSecondary, fontSize = 10.sp)
                }
                Text("${stats.runs}", color = CH.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("${stats.balls}", color = CH.textSecondary, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("${stats.fours}", color = CH.textSecondary, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("${stats.sixes}", color = CH.textSecondary, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("${"%.1f".format(stats.strikeRate)}", color = CH.textSecondary, fontSize = 12.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
            }
            HorizontalDivider(color = CH.border, thickness = 0.5.dp)
        }
    }

    // Did not bat
    val didNotBat = data.batsmanStats.values.filter { it.balls == 0 && !it.isOut && it.player.id !in appearedIds }
    if (didNotBat.isNotEmpty()) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().background(if (isSystemInDarkTheme()) Color(0xFF0A0A0A) else Color(0xFFF7F3EA))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Did Not Bat", color = CH.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(didNotBat.joinToString(", ") { it.player.fullName }, color = CH.textSecondary, fontSize = 11.sp)
            }
            HorizontalDivider(color = CH.border)
        }
    }

    // Extras + Total
    item {
        Column(
            modifier = Modifier.fillMaxWidth().background(CH.surface)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Extras", color = CH.textSecondary, fontSize = 13.sp)
                Text("(w ${data.wides}, nb ${data.noBalls}, b 0, lb 0)", color = CH.textSecondary, fontSize = 11.sp)
                Text("${data.extrasTotal}", color = CH.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", color = CH.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("$oversText Ov", color = CH.textSecondary, fontSize = 12.sp)
                Text("${inn.totalRuns}/${inn.totalWickets}", color = CH.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        HorizontalDivider(color = CH.border)
    }

    // Bowling header
    item {
        Spacer(modifier = Modifier.height(8.dp))
        Column(modifier = Modifier.fillMaxWidth().background(CH.surface)) {
            Text(
                data.bowlingTeamName,
                color = NeonBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text("BOWLING", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text("O", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("M", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("R", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("W", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("Eco", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
            }
            HorizontalDivider(color = CH.border)
        }
    }

    // Bowlers in bowling order
    val bowlOrder = bowlingOrderFromBalls(data.balls)
    val sortedBowlingList = data.bowlerStats.values.toList().sortedBy { stat ->
        val idx = bowlOrder.indexOf(stat.player.id)
        if (idx >= 0) idx else Int.MAX_VALUE
    }
    items(sortedBowlingList) { stats ->
        Column(modifier = Modifier.fillMaxWidth().background(if (isSystemInDarkTheme()) Color(0xFF0A0A0A) else Color(0xFFF7F3EA))) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stats.player.fullName, color = CH.textPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(stats.overs, color = CH.textSecondary, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("${stats.maidens}", color = CH.textSecondary, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("${stats.runs}", color = CH.textSecondary, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text(
                    "${stats.wickets}",
                    color = if (stats.wickets > 0) NeonGreen else CH.textSecondary,
                    fontSize = 14.sp,
                    fontWeight = if (stats.wickets > 0) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.width(28.dp), textAlign = TextAlign.End
                )
                Text("${"%.2f".format(stats.economy)}", color = when {
                    stats.economy < 6 -> NeonGreen; stats.economy < 9 -> AmberColor; else -> ErrorRed
                }, fontSize = 12.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
            }
            HorizontalDivider(color = CH.border, thickness = 0.5.dp)
        }
    }

    // Spacer between completed innings and the next section
    item { Spacer(modifier = Modifier.height(16.dp)) }
}

fun buildDismissalText(stats: BatsmanStats, bowlerMap: Map<String, String>): String {
    if (!stats.isOut) {
        // A retired-hurt batsman is not out, but should read "retired hurt" while off.
        // Once he returns to bat his dismissalType clears, so this falls back to not out.
        return if (stats.dismissalType == "retired_hurt") "retired hurt" else "not out"
    }
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

// ── COMMENTARY TAB — shows both innings ─────────────────────

@Composable
fun LiveCommentaryTab(uiState: LiveScorecardUiState) {
    val allCommentary = mutableListOf<Pair<String?, String>>() // label, comment
    // Current innings first (latest ball at top)
    if (uiState.completedInnings.isNotEmpty() && uiState.commentary.isNotEmpty()) {
        allCommentary.add("header" to "── ${uiState.battingTeamName} — Current Innings ──")
    }
    uiState.commentary.reversed().forEach { c -> allCommentary.add(null to c) }
    // Completed innings after (most recent completed first, 1st innings at bottom)
    uiState.completedInnings.reversed().forEach { completed ->
        val inningsLabel = when (completed.innings.inningsNo) {
            1 -> "1st Innings"; 2 -> "2nd Innings"; else -> "${completed.innings.inningsNo}th Innings"
        }
        allCommentary.add("header" to "── ${completed.battingTeamName} — $inningsLabel ──")
        completed.commentary.reversed().forEach { c -> allCommentary.add(null to c) }
    }

    if (allCommentary.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No commentary yet", color = CH.textSecondary)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(allCommentary) { (label, text) ->
            if (label == "header") {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(NeonGreen.copy(alpha = 0.08f))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(text, color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            } else {
                LiveCommentaryRow(text = text)
            }
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
            .background(if (isWicket) ErrorRed.copy(alpha = 0.05f) else CH.bg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(48.dp)) {
                if (overBall.contains(".")) {
                    Text(overBall, color = CH.textSecondary, fontSize = 11.sp)
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
                                else -> CH.surface
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        outcomeText,
                        color = when {
                            isWicket || isSix || isFour -> Color.White
                            outcome in listOf("Wd", "Nb") -> Color.Black
                            else -> CH.textSecondary
                        },
                        fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                description.ifEmpty { text },
                color = if (isWicket) ErrorRed else CH.textSecondary,
                fontSize = 13.sp, lineHeight = 18.sp,
                modifier = Modifier.weight(1f)
            )
        }
        HorizontalDivider(color = CH.border, thickness = 0.5.dp)
    }
}

// ── OVERS TAB — shows both innings ──────────────────────────

@Composable
fun LiveOversTab(uiState: LiveScorecardUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Completed innings overs first
        uiState.completedInnings.forEach { completed ->
            val inningsLabel = when (completed.innings.inningsNo) {
                1 -> "1st Innings"; 2 -> "2nd Innings"; else -> "${completed.innings.inningsNo}th Innings"
            }
            item {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonGreen.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("${completed.battingTeamName} — $inningsLabel",
                        color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            oversContent(completed.balls, completed.bowlerStats)
        }

        // Current innings overs
        if (uiState.completedInnings.isNotEmpty() && uiState.balls.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonGreen.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("${uiState.battingTeamName} — Current Innings",
                        color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        oversContent(uiState.balls, uiState.bowlerStats)
    }
}

private fun LazyListScope.oversContent(balls: List<Ball>, bowlerStats: Map<String, BowlerStats>) {
    val overGroups = balls.groupBy { it.overNo }.toSortedMap()
    if (overGroups.isEmpty()) return

    item {
        Row(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CH.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text("Ov", color = CH.textSecondary, fontSize = 12.sp, modifier = Modifier.width(32.dp))
            Text("Runs", color = CH.textSecondary, fontSize = 12.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
            Text("Wkts", color = CH.textSecondary, fontSize = 12.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
            Text("Balls", color = CH.textSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("RR", color = CH.textSecondary, fontSize = 12.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
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
        val bowlerName = bowlerStats.values.find { stats ->
            overBalls.any { it.bowlerId == stats.player.id }
        }?.player?.fullName ?: ""

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CH.surface)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${overNo + 1}",
                    color = CH.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(32.dp)
                )
                Text(
                    "$runsInOver",
                    color = CH.textPrimary, fontSize = 14.sp,
                    modifier = Modifier.width(40.dp), textAlign = TextAlign.Center
                )
                Text(
                    if (wicketsInOver > 0) "$wicketsInOver" else "-",
                    color = if (wicketsInOver > 0) ErrorRed else CH.textSecondary,
                    fontSize = 14.sp,
                    fontWeight = if (wicketsInOver > 0) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.width(40.dp), textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    overBalls.reversed().forEach { ball ->
                        val totalR = ball.runsOffBat + (ball.extrasRuns ?: 0)
                        val label = when {
                            ball.isWicket && ball.wicketType != "retired_hurt" -> if (totalR > 0) "W+$totalR" else "W"
                            ball.isSix -> "6"; ball.isBoundary -> "4"
                            ball.extrasType == "wide" -> { val r = (ball.extrasRuns ?: 1) - 1; if (r > 0) "Wd+$r" else "Wd" }
                            ball.extrasType == "no_ball" -> if (ball.runsOffBat > 0) "Nb+${ball.runsOffBat}" else "Nb"
                            ball.extrasType == "bye" -> { val b = ball.extrasRuns ?: 0; if (b <= 1) "B" else "${b}B" }
                            ball.extrasType == "leg_bye" -> { val lb = ball.extrasRuns ?: 0; if (lb <= 1) "LB" else "${lb}LB" }
                            ball.runsOffBat == 0 && ball.extrasRuns == null -> "•"
                            else -> "$totalR"
                        }
                        val bgColor = when {
                            ball.isWicket -> ErrorRed; ball.isSix -> NeonGreen
                            ball.isBoundary -> NeonBlue
                            ball.extrasType in listOf("wide", "no_ball") -> AmberColor
                            else -> CH.border
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
                        rr >= 6 -> NeonGreen; else -> CH.textSecondary
                    },
                    fontSize = 13.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End
                )
            }
            if (bowlerName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("🎳 $bowlerName", color = CH.textSecondary, fontSize = 10.sp)
            }
        }
    }
}

// ── PARTNERSHIP TAB ──────────────────────────────────────────

@Composable
fun LivePartnershipTab(uiState: LiveScorecardUiState) {
    if (uiState.balls.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No partnership data yet", color = CH.textSecondary)
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
                        Text("$b1Name & $b2Name", color = CH.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("$pRuns ($pBalls)", color = NeonGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("4s: $pFours", color = NeonBlue, fontSize = 12.sp)
                        Text("6s: $pSixes", color = NeonGreen, fontSize = 12.sp)
                        val rr = if (pBalls > 0) pRuns.toDouble() / pBalls * 6 else 0.0
                        Text("RR: ${"%.2f".format(rr)}", color = CH.textSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        // Previous partnerships
        if (partnerships.isNotEmpty()) {
            item {
                Text(
                    "Previous Partnerships",
                    color = CH.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            items(partnerships.reversed()) { p ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CH.surface)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${p.batter1} & ${p.batter2}", color = CH.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("4s: ${p.fours}  6s: ${p.sixes}", color = CH.textSecondary, fontSize = 11.sp)
                    }
                    Text(
                        "${p.runs} (${p.balls})",
                        color = CH.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold
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
            Text("No MVP data yet", color = CH.textSecondary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            Text("Impact Players", color = CH.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(CH.surface).padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Player", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text("Impact", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
                Text("Runs", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                Text("Wkts", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                Text("SR/Eco", color = CH.textSecondary, fontSize = 11.sp, modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
            }
        }
        items(mvpList) { entry ->
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CH.surface)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(entry.name, color = CH.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${"%.1f".format(entry.score)}", color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
                Text("${entry.runs}", color = if (entry.runs >= 50) AmberColor else CH.textPrimary, fontSize = 13.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                Text(if (entry.wickets > 0) "${entry.wickets}" else "-", color = if (entry.wickets >= 3) ErrorRed else CH.textPrimary, fontSize = 13.sp, fontWeight = if (entry.wickets >= 3) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                Text(
                    if (entry.wickets > 0 && entry.eco < 99) "${"%.1f".format(entry.eco)}" else "${"%.0f".format(entry.sr)}",
                    color = CH.textSecondary, fontSize = 12.sp, modifier = Modifier.width(52.dp), textAlign = TextAlign.End
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
                    .background(CH.surface)
                    .padding(16.dp)
            ) {
                Text("STATUS", color = CH.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    uiState.resultText.ifEmpty {
                        if (uiState.matchStatus == "LIVE") "Match in progress..." else uiState.matchStatus
                    },
                    color = if (uiState.resultText.isNotEmpty()) NeonGreen else CH.textSecondary,
                    fontSize = 16.sp, fontWeight = FontWeight.Bold
                )
            }
        }

        // Score summary
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CH.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("SCORE SUMMARY", color = CH.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                HorizontalDivider(color = CH.border)

                // Show completed innings scores first
                uiState.completedInnings.forEach { completed ->
                    val inn = completed.innings
                    val oversText = "${inn.totalBalls / 6}.${inn.totalBalls % 6}"
                    val inningsLabel = when (inn.inningsNo) {
                        1 -> "1st Inn"; 2 -> "2nd Inn"; else -> "${inn.inningsNo}th Inn"
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${completed.battingTeamName} ($inningsLabel)", color = CH.textSecondary, fontSize = 13.sp)
                        Text(
                            "${inn.totalRuns}/${inn.totalWickets} ($oversText ov)",
                            color = CH.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Current innings
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(uiState.battingTeamName, color = CH.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${uiState.totalRuns}/${uiState.totalWickets} (${uiState.currentOver}.${uiState.currentBall} ov)",
                        color = CH.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                }
                if (uiState.target != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Target", color = CH.textSecondary, fontSize = 13.sp)
                        Text("${uiState.target}", color = AmberColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Runs needed", color = CH.textSecondary, fontSize = 13.sp)
                        Text("${uiState.target!! - uiState.totalRuns} off ${uiState.ballsLeft} balls", color = ErrorRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                HorizontalDivider(color = CH.border)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CRR", color = CH.textSecondary, fontSize = 11.sp)
                        Text("${"%.2f".format(uiState.currentRunRate)}", color = NeonGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    uiState.requiredRunRate?.let {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("RRR", color = CH.textSecondary, fontSize = 11.sp)
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
                    .background(CH.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("TOP PERFORMERS", color = CH.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                HorizontalDivider(color = CH.border)
                val topBat = uiState.batsmanStats.values.maxByOrNull { it.runs }
                topBat?.let {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("🏏 Top Scorer", color = CH.textSecondary, fontSize = 11.sp)
                            Text(it.player.fullName, color = CH.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text("${it.runs}(${it.balls})", color = AmberColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                val topBowl = uiState.bowlerStats.values.filter { it.wickets > 0 }.maxByOrNull { it.wickets }
                topBowl?.let {
                    HorizontalDivider(color = CH.border, thickness = 0.5.dp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("🎳 Top Bowler", color = CH.textSecondary, fontSize = 11.sp)
                            Text(it.player.fullName, color = CH.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
                    .background(CH.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("EXTRAS", color = CH.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                HorizontalDivider(color = CH.border)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Wides", color = CH.textSecondary, fontSize = 11.sp)
                        Text("${uiState.wides}", color = CH.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No Balls", color = CH.textSecondary, fontSize = 11.sp)
                        Text("${uiState.noBalls}", color = CH.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Extras", color = CH.textSecondary, fontSize = 11.sp)
                        Text("${uiState.extrasTotal}", color = AmberColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
// ── BALL-BY-BALL TIMELINE (reverse chronological) ────────────

@Composable
fun LiveBallTimeline(uiState: LiveScorecardUiState) {
    if (uiState.balls.isEmpty()) return
    val oversDesc = uiState.balls.groupBy { it.overNo }.entries.sortedByDescending { it.key }

    fun ballRuns(b: Ball) = when {
        b.extrasType == "wide" -> (b.extrasRuns ?: 1) + b.runsOffBat
        b.extrasType == "no_ball" -> 1 + b.runsOffBat + (b.extrasRuns ?: 0)
        else -> b.runsOffBat + (b.extrasRuns ?: 0)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CH.bg)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        oversDesc.forEachIndexed { idx, entry ->
            if (idx > 0) {
                val overRuns = entry.value.sumOf { ballRuns(it) }
                LiveOverSeparator(entry.key + 1, overRuns)
            }
            entry.value.reversed().forEach { ball ->
                LiveBallChip(ball)
            }
        }
    }
}

@Composable
private fun LiveBallChip(ball: Ball) {
    val runs = ball.runsOffBat + (ball.extrasRuns ?: 0)
    val label: String
    val color: androidx.compose.ui.graphics.Color
    when {
        ball.isWicket && ball.wicketType != "retired_hurt" -> { label = if (runs > 0) "W+$runs" else "W"; color = ErrorRed }
        ball.isSix -> { label = "6"; color = PurpleColor }
        ball.isBoundary -> { label = "4"; color = NeonBlue }
        ball.extrasType == "wide" -> { val r = (ball.extrasRuns ?: 1) - 1; label = if (r > 0) "Wd+$r" else "Wd"; color = AmberColor }
        ball.extrasType == "no_ball" -> { label = if (ball.runsOffBat > 0) "Nb+${ball.runsOffBat}" else "Nb"; color = AmberColor }
        ball.extrasType == "bye" -> { val b = ball.extrasRuns ?: 0; label = if (b <= 1) "B" else "${b}B"; color = CH.surface }
        ball.extrasType == "leg_bye" -> { val lb = ball.extrasRuns ?: 0; label = if (lb <= 1) "LB" else "${lb}LB"; color = CH.surface }
        ball.runsOffBat == 0 && ball.extrasRuns == null -> { label = "•"; color = CH.surface }
        else -> { label = "$runs"; color = CH.surface }
    }
    Box(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .heightIn(min = 34.dp)
            .widthIn(min = 34.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (color == CH.surface) CH.textPrimary else Color.White,
            fontSize = 13.sp, fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LiveOverSeparator(overNumber: Int, runs: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .width(1.dp)
                .height(34.dp)
                .background(CH.border)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(ordinalOver(overNumber), color = CH.textSecondary, fontSize = 10.sp)
            Text("$runs RUNS", color = CH.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(6.dp))
    }
}

private fun ordinalOver(n: Int): String {
    val suffix = when {
        n % 100 in 11..13 -> "th"
        n % 10 == 1 -> "st"
        n % 10 == 2 -> "nd"
        n % 10 == 3 -> "rd"
        else -> "th"
    }
    return "$n$suffix"
}