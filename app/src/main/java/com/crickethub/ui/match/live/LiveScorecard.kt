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
fun LiveScorecardScreen(
    matchId: String,
    onBack: () -> Unit,
    viewModel: LiveScorecardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    var showCopied by remember { mutableStateOf(false) }

    LaunchedEffect(matchId) {
        viewModel.loadAndSubscribe(matchId)
    }

    LaunchedEffect(showCopied) {
        if (showCopied) {
            kotlinx.coroutines.delay(2000)
            showCopied = false
        }
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Column {
                    Text(
                        "Live Scorecard",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        uiState.matchStatus,
                        fontSize = 12.sp,
                        color = when (uiState.matchStatus) {
                            "LIVE" -> NeonGreen
                            "COMPLETED" -> TextSecondary
                            else -> AmberColor
                        }
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Live indicator
                if (uiState.matchStatus == "LIVE") {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ErrorRed)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "● LIVE",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                // Share button
                IconButton(
                    onClick = {
                        clipboardManager.setText(
                            AnnotatedString("CricketHub Match: ${uiState.shareableSlug ?: matchId}")
                        )
                        showCopied = true
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = NeonGreen)
                }
            }
        }

        if (showCopied) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeonGreen.copy(alpha = 0.1f))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Link copied!", color = NeonGreen, fontSize = 13.sp)
            }
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonGreen)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Score card
                item {
                    ScoreCard(uiState = uiState)
                }

                // Last 6 balls
                item {
                    Last6BallsSection(balls = uiState.last6Balls)
                }

                // Batting scorecard
                if (uiState.batsmanStats.isNotEmpty()) {
                    item {
                        SectionHeader("Batting")
                    }
                    items(uiState.batsmanStats.filter {
                        it.value.balls > 0 || it.value.isOut
                    }.values.toList()) { stats ->
                        BatsmanRow(stats = stats)
                    }
                }

                // Bowling scorecard
                if (uiState.bowlerStats.isNotEmpty()) {
                    item {
                        SectionHeader("Bowling")
                    }
                    items(uiState.bowlerStats.filter {
                        it.value.balls > 0 || it.value.wides > 0 || it.value.noBalls > 0
                    }.values.toList()) { stats ->
                        BowlerRow(stats = stats)
                    }
                }

                // Commentary
                if (uiState.commentary.isNotEmpty()) {
                    item {
                        SectionHeader("Commentary")
                    }
                    items(uiState.commentary) { comment ->
                        CommentaryRow(text = comment)
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreCard(uiState: LiveScorecardUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard)
            .padding(16.dp)
    ) {
        // Team names
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                uiState.battingTeamName,
                color = NeonGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "vs ${uiState.bowlingTeamName}",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Score
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                "${uiState.totalRuns}/${uiState.totalWickets}",
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "${uiState.currentOver}.${uiState.currentBall}",
                fontSize = 24.sp,
                color = NeonGreen,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Run rates
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column {
                Text("CRR", color = TextSecondary, fontSize = 11.sp)
                Text(
                    "%.2f".format(uiState.currentRunRate),
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (uiState.requiredRunRate != null) {
                Column {
                    Text("RRR", color = TextSecondary, fontSize = 11.sp)
                    Text(
                        "%.2f".format(uiState.requiredRunRate),
                        color = if (uiState.requiredRunRate > uiState.currentRunRate)
                            ErrorRed else NeonGreen,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (uiState.target != null) {
                Column {
                    Text("Target", color = TextSecondary, fontSize = 11.sp)
                    Text(
                        "${uiState.target}",
                        color = AmberColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (uiState.target != null) {
            Spacer(modifier = Modifier.height(8.dp))
            val runsNeeded = uiState.target - uiState.totalRuns
            val ballsLeft = uiState.ballsLeft
            Text(
                "Need $runsNeeded runs from $ballsLeft balls",
                color = AmberColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun Last6BallsSection(balls: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("This over:", color = TextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(4.dp))
        balls.forEach { ball ->
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
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(bgColor)
                    .border(1.dp, BorderColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    ball,
                    color = textColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        title,
        color = NeonGreen,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun BatsmanRow(stats: com.crickethub.data.model.BatsmanStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stats.player.fullName,
                color = if (stats.isOut) TextSecondary else TextPrimary,
                fontSize = 14.sp,
                fontWeight = if (!stats.isOut) FontWeight.SemiBold else FontWeight.Normal
            )
            if (stats.isOut) {
                Text(
                    stats.dismissalType?.replace("_", " ")
                        ?.replaceFirstChar { it.uppercase() } ?: "Out",
                    color = ErrorRed,
                    fontSize = 11.sp
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(horizontalAlignment = Alignment.End) {
                Text("${stats.runs}", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("(${stats.balls})", color = TextSecondary, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("4s: ${stats.fours}", color = NeonBlue, fontSize = 12.sp)
                Text("6s: ${stats.sixes}", color = NeonGreen, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "SR: ${"%.1f".format(stats.strikeRate)}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
    Divider(color = BorderColor, thickness = 0.5.dp)
}

@Composable
fun BowlerRow(stats: com.crickethub.data.model.BowlerStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stats.player.fullName,
            color = TextPrimary,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stats.overs, color = TextSecondary, fontSize = 12.sp)
            Text("${stats.runs}", color = TextPrimary, fontSize = 12.sp)
            Text("${stats.wickets}W", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Eco: ${"%.1f".format(stats.economy)}", color = TextSecondary, fontSize = 12.sp)
        }
    }
    Divider(color = BorderColor, thickness = 0.5.dp)
}

@Composable
fun CommentaryRow(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text, color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
        Divider(color = BorderColor.copy(alpha = 0.5f), thickness = 0.5.dp,
            modifier = Modifier.padding(top = 8.dp))
    }
}