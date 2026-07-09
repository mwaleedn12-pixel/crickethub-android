package com.crickethub.ui.match.postmatch

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun PostMatchScreen(
    matchId: String,
    onBack: () -> Unit,
    onGoToMatches: () -> Unit,
    viewModel: PostMatchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Result", "1st Inn", "2nd Inn", "MOTM")

    LaunchedEffect(matchId) {
        viewModel.loadPostMatch(matchId)
    }

    LaunchedEffect(uiState.matchSaved) {
        if (uiState.matchSaved) onGoToMatches()
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
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(
                "Match Summary",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                clipboardManager.setText(AnnotatedString(uiState.resultText))
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
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    uiState.resultText,
                    color = NeonGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        tab,
                        color = if (selected) Color.Black else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonGreen)
            }
        } else {
            when (selectedTab) {
                0 -> ResultTab(uiState = uiState)
                1 -> InningsTab(
                    battingTeamName = uiState.innings1BattingTeamName,
                    bowlingTeamName = uiState.innings1BowlingTeamName,
                    innings = uiState.innings1,
                    batting = uiState.innings1Batting,
                    bowling = uiState.innings1Bowling,
                    bowlerMap = uiState.innings1Bowling.associate { it.player.id to it.player.fullName }
                )
                2 -> InningsTab(
                    battingTeamName = uiState.innings2BattingTeamName,
                    bowlingTeamName = uiState.innings2BowlingTeamName,
                    innings = uiState.innings2,
                    batting = uiState.innings2Batting,
                    bowling = uiState.innings2Bowling,
                    bowlerMap = uiState.innings2Bowling.associate { it.player.id to it.player.fullName }
                )
                3 -> MotmTab(uiState = uiState, onSelectMotm = { viewModel.selectMotm(it) })
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save button
            Button(
                onClick = { viewModel.saveMatchResult(matchId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
            ) {
                Text("Save & Complete Match", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun ResultTab(uiState: PostMatchUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Innings 1
                uiState.innings1?.let { inn ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                uiState.innings1BattingTeamName,
                                color = NeonGreen,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("1st Innings", color = TextSecondary, fontSize = 11.sp)
                        }
                        Text(
                            "${inn.totalRuns}/${inn.totalWickets}",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "(${inn.totalBalls / 6}.${inn.totalBalls % 6} ov)",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }

                HorizontalDivider(color = BorderColor)

                // Innings 2
                uiState.innings2?.let { inn ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                uiState.innings2BattingTeamName,
                                color = NeonBlue,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("2nd Innings", color = TextSecondary, fontSize = 11.sp)
                        }
                        Text(
                            "${inn.totalRuns}/${inn.totalWickets}",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "(${inn.totalBalls / 6}.${inn.totalBalls % 6} ov)",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Player of the Match
        uiState.selectedMotm?.let { motm ->
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AmberColor.copy(alpha = 0.1f))
                        .border(1.dp, AmberColor, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("⭐", fontSize = 28.sp)
                    Text("Player of the Match", color = AmberColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(motm.fullName, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun InningsTab(
    battingTeamName: String,
    bowlingTeamName: String,
    innings: com.crickethub.data.model.Innings?,
    batting: List<BatsmanScorecard>,
    bowling: List<BowlerScorecard>,
    bowlerMap: Map<String, String>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Batting header
        item {
            Column(modifier = Modifier.fillMaxWidth().background(SurfaceCard)) {
                Text(
                    battingTeamName,
                    color = NeonGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
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

        // Batting rows
        val battedList = batting.filter { it.balls > 0 || it.isOut }
        items(battedList) { stats ->
            val dismissalText = buildPostMatchDismissalText(stats, bowlerMap)
            Column(modifier = Modifier.fillMaxWidth().background(BackgroundDark)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stats.player.fullName,
                            color = if (stats.isOut) TextSecondary else TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = if (!stats.isOut) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Text(dismissalText, color = TextSecondary, fontSize = 11.sp)
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

        // Did Not Bat
        val didNotBat = batting.filter { it.balls == 0 && !it.isOut }
        if (didNotBat.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().background(BackgroundDark).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Did Not Bat", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                    Text(didNotBat.joinToString(", ") { it.player.fullName }, color = TextSecondary, fontSize = 12.sp)
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
                        Text("(w ${inn.wides}, nb ${inn.noBalls}, b 0, lb 0)", color = TextSecondary, fontSize = 12.sp)
                        Text("${inn.extrasTotal}", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                    bowlingTeamName,
                    color = NeonBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
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
                    Text(stats.player.fullName, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text(stats.overs, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("${stats.maidens}", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text("${stats.runs}", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                    Text(
                        "${stats.wickets}",
                        color = if (stats.wickets > 0) NeonGreen else TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = if (stats.wickets > 0) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.width(28.dp),
                        textAlign = TextAlign.End
                    )
                    Text("${"%.2f".format(stats.economy)}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
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
        else -> stats.dismissalType?.replace("_", " ") ?: "out"
    }
}

@Composable
fun MotmTab(uiState: PostMatchUiState, onSelectMotm: (com.crickethub.data.model.Player) -> Unit) {
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
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
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
                    Text(candidate.player.fullName, color = if (isSelected) AmberColor else TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        buildString {
                            if (candidate.runs > 0) append("${candidate.runs} runs")
                            if (candidate.runs > 0 && candidate.wickets > 0) append(", ")
                            if (candidate.wickets > 0) append("${candidate.wickets} wkts")
                        },
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                if (isSelected) Text("⭐", fontSize = 20.sp)
            }
        }
    }
}