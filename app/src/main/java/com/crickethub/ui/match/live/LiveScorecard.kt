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

private val BackgroundDark = Color(0xFF030712)
private val SurfaceCard = Color(0xFF111827)
private val BorderColor = Color(0xFF1F2937)
private val NeonGreen = Color(0xFF10B981)
private val NeonBlue = Color(0xFF3B82F6)
private val TextPrimary = Color(0xFFF9FAFB)
private val TextSecondary = Color(0xFF9CA3AF)
private val ErrorRed = Color(0xFFEF4444)
private val AmberColor = Color(0xFFF59E0B)
private val PurpleColor = Color(0xFF8B5CF6)

@Composable
fun LiveScorecardScreen(
    matchId: String,
    onBack: () -> Unit,
    viewModel: LiveScorecardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    var showCopied by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Scorecard", "Commentary", "Overs", "MVP", "Summary")

    LaunchedEffect(showCopied) {
        if (showCopied) {
            kotlinx.coroutines.delay(2000)
            showCopied = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
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
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    uiState.matchStatus, fontSize = 11.sp,
                    color = when (uiState.matchStatus) {
                        "LIVE" -> NeonGreen; "COMPLETED" -> TextSecondary; else -> AmberColor
                    }
                )
            }
            if (uiState.matchStatus == "LIVE") {
                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(ErrorRed).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("● LIVE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            IconButton(onClick = {
                clipboardManager.setText(AnnotatedString("CricketHub: ${uiState.shareableSlug ?: matchId}"))
                showCopied = true
            }) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = NeonGreen)
            }
        }

        if (showCopied) {
            Box(modifier = Modifier.fillMaxWidth().background(NeonGreen.copy(alpha = 0.1f)).padding(8.dp), contentAlignment = Alignment.Center) {
                Text("Link copied!", color = NeonGreen, fontSize = 13.sp)
            }
        }

        Box(modifier = Modifier.fillMaxWidth().background(SurfaceCard).padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${uiState.totalRuns}/${uiState.totalWickets}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("(${uiState.currentOver}.${uiState.currentBall} ov)", color = TextSecondary, fontSize = 13.sp)
                    Text("CRR: ${"%.2f".format(uiState.currentRunRate)}", color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                if (uiState.target != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("T: ${uiState.target}", color = AmberColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        uiState.requiredRunRate?.let {
                            Text("RRR: ${"%.2f".format(it)}", color = if (it > uiState.currentRunRate) ErrorRed else NeonGreen, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        ScrollableTabRow(selectedTabIndex = selectedTab, containerColor = SurfaceCard, contentColor = NeonGreen, edgePadding = 0.dp) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(tab, fontSize = 13.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) NeonGreen else TextSecondary)
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
                0 -> ScorecardTab(uiState = uiState)
                1 -> CommentaryTab(uiState = uiState)
                2 -> OversTab(uiState = uiState)
                3 -> MvpTab(uiState = uiState)
                4 -> SummaryTab(uiState = uiState)
            }
        }
    }
}

@Composable
fun ScorecardTab(uiState: LiveScorecardUiState) {
    val bowlerMap = uiState.bowlerStats.values.associate { it.player.id to it.player.fullName }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("This over:", color = TextSecondary, fontSize = 12.sp)
                uiState.last6Balls.forEach { ball ->
                    val (bgColor, textColor) = when (ball) {
                        "W" -> ErrorRed to Color.White; "4" -> NeonBlue to Color.White
                        "6" -> NeonGreen to Color.Black; "Wd", "Nb" -> AmberColor to Color.Black
                        "0" -> SurfaceCard to TextSecondary; else -> SurfaceCard to TextPrimary
                    }
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(bgColor).border(1.dp, BorderColor, CircleShape), contentAlignment = Alignment.Center) {
                        Text(ball, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth().background(SurfaceCard)) {
                Text(uiState.battingTeamName, color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text("BATTING", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    listOf("R", "B", "4s", "6s").forEach { Text(it, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End) }
                    Text("SR", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
                }
                HorizontalDivider(color = BorderColor)
            }
        }

        val battedList = uiState.batsmanStats.values.filter { it.balls > 0 || it.isOut }
        items(battedList) { stats ->
            Column(modifier = Modifier.fillMaxWidth().background(BackgroundDark)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stats.player.fullName, color = if (stats.isOut) TextSecondary else TextPrimary, fontSize = 14.sp, fontWeight = if (!stats.isOut) FontWeight.SemiBold else FontWeight.Normal)
                        Text(buildDismissalText(stats, bowlerMap), color = TextSecondary, fontSize = 11.sp)
                    }
                    Text("${stats.runs}", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("${stats.balls}", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("${stats.fours}", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("${stats.sixes}", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("${"%.2f".format(stats.strikeRate)}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
                }
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            }
        }

        val didNotBatList = uiState.batsmanStats.values.filter { it.balls == 0 && !it.isOut }
        if (didNotBatList.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().background(BackgroundDark).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Did Not Bat", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                    Text(didNotBatList.joinToString(", ") { it.player.fullName }, color = TextSecondary, fontSize = 12.sp)
                }
                HorizontalDivider(color = BorderColor)
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth().background(BackgroundDark).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Extras", color = TextSecondary, fontSize = 13.sp)
                Text("(w ${uiState.wides}, nb ${uiState.noBalls}, b 0, lb 0)", color = TextSecondary, fontSize = 12.sp)
                Text("${uiState.extrasTotal}", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = BorderColor)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth().background(SurfaceCard).padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("${uiState.currentOver}.${uiState.currentBall} Ov (RR: ${"%.2f".format(uiState.currentRunRate)})", color = TextSecondary, fontSize = 12.sp)
                Text("${uiState.totalRuns}/${uiState.totalWickets}", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        val fow = buildFallOfWickets(uiState)
        if (fow.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().background(BackgroundDark).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Fall of Wickets", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                    Text(fow, color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
                }
                HorizontalDivider(color = BorderColor)
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.fillMaxWidth().background(SurfaceCard)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text("BOWLING", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    listOf("O", "M", "R", "W").forEach { Text(it, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End) }
                    Text("ECON", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                }
                HorizontalDivider(color = BorderColor)
            }
        }

        val bowlingList = uiState.bowlerStats.values.filter { it.balls > 0 || it.wides > 0 || it.noBalls > 0 }
        items(bowlingList) { stats ->
            Column(modifier = Modifier.fillMaxWidth().background(BackgroundDark)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stats.player.fullName, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text(stats.overs, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("0", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("${stats.runs}", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("${stats.wickets}", color = if (stats.wickets > 0) NeonGreen else TextSecondary, fontSize = 14.sp, fontWeight = if (stats.wickets > 0) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("${"%.2f".format(stats.economy)}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                }
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            }
        }
    }
}

fun buildDismissalText(stats: com.crickethub.data.model.BatsmanStats, bowlerMap: Map<String, String>): String {
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
        else -> stats.dismissalType?.replace("_", " ") ?: "out"
    }
}

fun buildFallOfWickets(uiState: LiveScorecardUiState): String {
    if (uiState.balls.isEmpty()) return ""
    val fowList = mutableListOf<String>()
    var runningRuns = 0
    var wicketNo = 0
    uiState.balls.sortedWith(compareBy({ it.overNo }, { it.ballNo })).forEach { ball ->
        val runsThisBall = when {
            ball.extrasType == "wide" -> (ball.extrasRuns ?: 1) + ball.runsOffBat
            ball.extrasType == "no_ball" -> 1 + ball.runsOffBat + (ball.extrasRuns ?: 0)
            else -> ball.runsOffBat + (ball.extrasRuns ?: 0)
        }
        runningRuns += runsThisBall
        if (ball.isWicket && ball.wicketType != "retired_hurt") {
            wicketNo++
            val batsmanName = uiState.batsmanStats.values.find { it.player.id == ball.batsmanId }?.player?.fullName ?: "Batsman"
            fowList.add("$wicketNo-$runningRuns ($batsmanName, ${ball.overNo}.${ball.ballNo} ov)")
        }
    }
    return fowList.joinToString("\n")
}

@Composable
fun CommentaryTab(uiState: LiveScorecardUiState) {
    if (uiState.commentary.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No commentary yet", color = TextSecondary) }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        items(uiState.commentary.reversed()) { comment -> CommentaryItemRow(text = comment) }
    }
}

@Composable
fun CommentaryItemRow(text: String) {
    val parts = text.split("|").map { it.trim() }
    val overBall = parts.getOrNull(0)?.trim() ?: ""
    val outcome = parts.getOrNull(1)?.trim() ?: ""
    val description = parts.getOrNull(2)?.trim() ?: text
    val isWicket = outcome == "W" || text.contains("BOWLED") || text.contains("CAUGHT") || text.contains("LBW") || text.contains("RUN OUT") || text.contains("STUMPED") || text.contains("OUT!")
    val isFour = outcome == "4" || text.contains("FOUR")
    val isSix = outcome == "6" || text.contains("SIX")
    val outcomeText = when { isWicket -> "W"; isSix -> "6"; isFour -> "4"; outcome.isNotEmpty() -> outcome; else -> "•" }

    Column(modifier = Modifier.fillMaxWidth().background(if (isWicket) ErrorRed.copy(alpha = 0.05f) else BackgroundDark)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(44.dp)) {
                if (overBall.isNotEmpty() && overBall.contains(".")) Text(overBall, color = TextSecondary, fontSize = 12.sp)
                Box(
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(4.dp)).background(when { isWicket -> ErrorRed; isSix -> NeonGreen; isFour -> NeonBlue; outcome in listOf("Wd", "Nb") -> AmberColor; else -> SurfaceCard }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(outcomeText, color = when { isWicket || isSix || isFour -> Color.White; outcome in listOf("Wd", "Nb") -> Color.Black; else -> TextSecondary }, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(description.ifEmpty { text }, color = if (isWicket) ErrorRed else TextSecondary, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
    }
}

@Composable
fun OversTab(uiState: LiveScorecardUiState) {
    val overGroups = uiState.balls.groupBy { it.overNo }.toSortedMap()
    if (overGroups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No overs data yet", color = TextSecondary) }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceCard).padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text("Ov", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(32.dp))
                Text("R", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                Text("W", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                Text("Balls", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("RR", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
            }
        }
        items(overGroups.entries.toList()) { (overNo, overBalls) ->
            val runsInOver = overBalls.sumOf { ball ->
                when { ball.extrasType == "wide" -> (ball.extrasRuns ?: 1) + ball.runsOffBat; ball.extrasType == "no_ball" -> 1 + ball.runsOffBat + (ball.extrasRuns ?: 0); else -> ball.runsOffBat + (ball.extrasRuns ?: 0) }
            }
            val wicketsInOver = overBalls.count { it.isWicket && it.wicketType != "retired_hurt" }
            val legalBalls = overBalls.filter { it.extrasType != "wide" && it.extrasType != "no_ball" }
            val rr = if (legalBalls.isNotEmpty()) runsInOver.toDouble() / legalBalls.size * 6 else 0.0

            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceCard).padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${overNo + 1}", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
                    Text("$runsInOver", color = TextPrimary, fontSize = 14.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                    Text(if (wicketsInOver > 0) "$wicketsInOver" else "-", color = if (wicketsInOver > 0) ErrorRed else TextSecondary, fontSize = 14.sp, fontWeight = if (wicketsInOver > 0) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        overBalls.sortedWith(compareBy({ it.ballNo }, { it.createdAt })).forEach { ball ->
                            val label = when { ball.isWicket -> "W"; ball.isSix -> "6"; ball.isBoundary -> "4"; ball.extrasType == "wide" -> "Wd"; ball.extrasType == "no_ball" -> "Nb"; ball.runsOffBat == 0 && ball.extrasRuns == null -> "•"; else -> "${ball.runsOffBat + (ball.extrasRuns ?: 0)}" }
                            val bgColor = when { ball.isWicket -> ErrorRed; ball.isSix -> NeonGreen; ball.isBoundary -> NeonBlue; ball.extrasType in listOf("wide", "no_ball") -> AmberColor; else -> BorderColor }
                            Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(bgColor), contentAlignment = Alignment.Center) {
                                Text(label, fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(2.dp))
                        }
                    }
                    Text("${"%.1f".format(rr)}", color = when { rr >= 12 -> ErrorRed; rr >= 8 -> AmberColor; rr >= 6 -> NeonGreen; else -> TextSecondary }, fontSize = 13.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                }
            }
        }
    }
}

@Composable
fun MvpTab(uiState: LiveScorecardUiState) {
    data class MvpEntry(val name: String, val tiScore: Double, val runs: Int, val wickets: Int, val economy: Double)
    val mvpList = mutableListOf<MvpEntry>()
    uiState.batsmanStats.values.forEach { bat ->
        val bowl = uiState.bowlerStats[bat.player.id]
        val wickets = bowl?.wickets ?: 0
        val economy = bowl?.economy ?: 99.0
        val iRuns = bat.runs * (bat.strikeRate / 100.0)
        val bImpact = iRuns * (if (bat.sixes > 0 || bat.fours > 0) 1.2 else 1.0)
        val boImpact = if (wickets > 0) wickets * 25.0 - (economy * (bowl?.balls ?: 0) / 6) else 0.0
        val tiScore = bImpact + boImpact.coerceAtLeast(0.0)
        if (bat.runs > 0 || wickets > 0) mvpList.add(MvpEntry(bat.player.fullName, tiScore, bat.runs, wickets, economy))
    }
    mvpList.sortByDescending { it.tiScore }
    if (mvpList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No MVP data yet", color = TextSecondary) }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Text("Most Valuable Players", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(SurfaceCard).padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text("Player", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text("TI↓", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
                Text("Runs", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                Text("Wkts", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                Text("Eco", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
            }
        }
        items(mvpList) { entry ->
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceCard).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(entry.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${"%.2f".format(entry.tiScore)}", color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
                Text("${entry.runs}", color = if (entry.runs >= 50) AmberColor else TextPrimary, fontSize = 13.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                Text(if (entry.wickets > 0) "${entry.wickets}" else "-", color = if (entry.wickets >= 3) ErrorRed else TextPrimary, fontSize = 13.sp, fontWeight = if (entry.wickets >= 3) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                Text(if (entry.economy < 99) "${"%.2f".format(entry.economy)}" else "-", color = when { entry.economy < 6 -> NeonGreen; entry.economy < 9 -> TextPrimary; entry.economy < 99 -> ErrorRed; else -> TextSecondary }, fontSize = 12.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
            }
        }
    }
}

@Composable
fun SummaryTab(uiState: LiveScorecardUiState) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceCard).padding(16.dp)) {
                Text("RESULT", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(uiState.resultText.ifEmpty { "Match in progress..." }, color = if (uiState.resultText.isNotEmpty()) NeonGreen else TextSecondary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceCard).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("MATCH SUMMARY", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                HorizontalDivider(color = BorderColor)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(uiState.battingTeamName, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("${uiState.totalRuns}/${uiState.totalWickets} (${uiState.currentOver}.${uiState.currentBall} ov)", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                if (uiState.target != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(uiState.bowlingTeamName, color = TextSecondary, fontSize = 14.sp)
                        Text("Target: ${uiState.target}", color = AmberColor, fontSize = 14.sp)
                    }
                }
                HorizontalDivider(color = BorderColor)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CRR", color = TextSecondary, fontSize = 11.sp)
                        Text("${"%.2f".format(uiState.currentRunRate)}", color = NeonGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    uiState.requiredRunRate?.let {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("RRR", color = TextSecondary, fontSize = 11.sp)
                            Text("${"%.2f".format(it)}", color = if (it > uiState.currentRunRate) ErrorRed else NeonGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (uiState.target != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Runs Req", color = TextSecondary, fontSize = 11.sp)
                            Text("${uiState.target - uiState.totalRuns}", color = AmberColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Balls Rem", color = TextSecondary, fontSize = 11.sp)
                            Text("${uiState.ballsLeft}", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item {
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceCard).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("TOP PERFORMERS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                HorizontalDivider(color = BorderColor)
                val topBat = uiState.batsmanStats.values.maxByOrNull { it.runs }
                topBat?.let {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("🏏 Top Scorer", color = TextSecondary, fontSize = 11.sp); Text(it.player.fullName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                        Text("${it.runs}(${it.balls})", color = AmberColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                val topBowl = uiState.bowlerStats.values.filter { it.wickets > 0 }.maxByOrNull { it.wickets }
                topBowl?.let {
                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("🎳 Top Bowler", color = TextSecondary, fontSize = 11.sp); Text(it.player.fullName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                        Text("${it.wickets}/${it.runs}", color = NeonGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceCard).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("EXTRAS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                HorizontalDivider(color = BorderColor)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Wides", color = TextSecondary, fontSize = 11.sp); Text("${uiState.wides}", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("No Balls", color = TextSecondary, fontSize = 11.sp); Text("${uiState.noBalls}", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Total", color = TextSecondary, fontSize = 11.sp); Text("${uiState.extrasTotal}", color = AmberColor, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}