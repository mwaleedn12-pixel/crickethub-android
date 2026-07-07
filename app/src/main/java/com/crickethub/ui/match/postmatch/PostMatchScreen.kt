package com.crickethub.ui.match.postmatch

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import java.io.FileOutputStream

private val BackgroundDark = ComposeColor(0xFF030712)
private val SurfaceCard = ComposeColor(0xFF111827)
private val BorderColor = ComposeColor(0xFF1F2937)
private val NeonGreen = ComposeColor(0xFF10B981)
private val NeonBlue = ComposeColor(0xFF3B82F6)
private val TextPrimary = ComposeColor(0xFFF9FAFB)
private val TextSecondary = ComposeColor(0xFF9CA3AF)
private val ErrorRed = ComposeColor(0xFFEF4444)
private val AmberColor = ComposeColor(0xFFF59E0B)
private val PurpleColor = ComposeColor(0xFF8B5CF6)

@Composable
fun PostMatchScreen(
    matchId: String,
    onBack: () -> Unit,
    onGoToMatches: () -> Unit,
    viewModel: PostMatchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showMotmDialog by remember { mutableStateOf(false) }
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text(
                    "Match Summary",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Row {
                IconButton(onClick = {
                    exportPdf(context, uiState)
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Export PDF", tint = NeonGreen)
                }
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        color = if (selected) ComposeColor.Black else TextSecondary,
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
                0 -> ResultTab(uiState = uiState, onSaveResult = { viewModel.saveMatchResult(matchId) })
                1 -> ScorecardTab(
                    batting = uiState.innings1Batting,
                    bowling = uiState.innings1Bowling,
                    innings = uiState.innings1,
                    teamName = uiState.team1?.name ?: "Team 1"
                )
                2 -> ScorecardTab(
                    batting = uiState.innings2Batting,
                    bowling = uiState.innings2Bowling,
                    innings = uiState.innings2,
                    teamName = uiState.team2?.name ?: "Team 2"
                )
                3 -> MotmTab(
                    uiState = uiState,
                    onSelectMotm = { viewModel.selectMotm(it) }
                )
            }
        }
    }
}

// =============================================
// RESULT TAB
// =============================================
@Composable
fun ResultTab(uiState: PostMatchUiState, onSaveResult: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Score summary
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                uiState.innings1?.let { i1 ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                uiState.team1?.name ?: "Team 1",
                                color = NeonGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${i1.totalRuns}/${i1.totalWickets}",
                                color = TextPrimary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "(${i1.totalBalls / 6}.${i1.totalBalls % 6} ov)",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }

                uiState.innings2?.let { i2 ->
                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                uiState.team2?.name ?: "Team 2",
                                color = NeonBlue,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${i2.totalRuns}/${i2.totalWickets}",
                                color = TextPrimary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "(${i2.totalBalls / 6}.${i2.totalBalls % 6} ov)",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // MOTM preview
        uiState.selectedMotm?.let { motm ->
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AmberColor.copy(alpha = 0.1f))
                        .border(1.dp, AmberColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = AmberColor)
                    Column {
                        Text("Player of the Match", color = AmberColor, fontSize = 12.sp)
                        Text(motm.fullName, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Save result button
        item {
            Button(
                onClick = onSaveResult,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save & Complete Match", color = ComposeColor.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// =============================================
// SCORECARD TAB
// =============================================
@Composable
fun ScorecardTab(
    batting: List<BatsmanScorecard>,
    bowling: List<BowlerScorecard>,
    innings: com.crickethub.data.model.Innings?,
    teamName: String
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Batting header
        item {
            Text(
                "$teamName — Batting",
                color = NeonGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Batting table header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceCard)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("Batsman", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(2f))
                Text("R", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
                Text("B", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
                Text("4s", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("6s", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("SR", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
            }
        }

        items(batting) { batsman ->
            BatsmanRow(batsman = batsman)
        }

        // Extras & Total
        innings?.let { i ->
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCard)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Extras", color = TextSecondary, fontSize = 12.sp)
                        Text(
                            "${i.extrasTotal} (W:${i.wides}, NB:${i.noBalls}, B:${i.byes}, LB:${i.legByes})",
                            color = TextPrimary,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Total",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${i.totalRuns}/${i.totalWickets} (${i.totalBalls / 6}.${i.totalBalls % 6} ov)",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Bowling header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Bowling",
                color = NeonBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceCard)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("Bowler", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(2f))
                Text("O", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
                Text("M", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("R", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("W", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                Text("Eco", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
            }
        }

        items(bowling) { bowler ->
            BowlerRow(bowler = bowler)
        }
    }
}

@Composable
fun BatsmanRow(batsman: BatsmanScorecard) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(2f)) {
            Text(
                batsman.player.fullName,
                color = if (batsman.isOut) TextSecondary else TextPrimary,
                fontSize = 13.sp,
                fontWeight = if (!batsman.isOut) FontWeight.SemiBold else FontWeight.Normal
            )
            if (batsman.isOut) {
                Text(
                    batsman.dismissalType?.replace("_", " ")
                        ?.replaceFirstChar { it.uppercase() } ?: "Out",
                    color = ErrorRed,
                    fontSize = 10.sp
                )
            } else {
                Text("not out", color = NeonGreen, fontSize = 10.sp)
            }
        }
        Text(
            "${batsman.runs}",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.End
        )
        Text(
            "${batsman.balls}",
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.End
        )
        Text(
            "${batsman.fours}",
            color = NeonBlue,
            fontSize = 13.sp,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.End
        )
        Text(
            "${batsman.sixes}",
            color = NeonGreen,
            fontSize = 13.sp,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.End
        )
        Text(
            "${"%.1f".format(batsman.strikeRate)}",
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.width(44.dp),
            textAlign = TextAlign.End
        )
    }
    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
}

@Composable
fun BowlerRow(bowler: BowlerScorecard) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            bowler.player.fullName,
            color = TextPrimary,
            fontSize = 13.sp,
            modifier = Modifier.weight(2f)
        )
        Text(
            bowler.overs,
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.End
        )
        Text(
            "${bowler.maidens}",
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.End
        )
        Text(
            "${bowler.runs}",
            color = TextPrimary,
            fontSize = 13.sp,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.End
        )
        Text(
            "${bowler.wickets}",
            color = NeonGreen,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.End
        )
        Text(
            "${"%.1f".format(bowler.economy)}",
            color = when {
                bowler.economy < 6 -> NeonGreen
                bowler.economy < 8 -> AmberColor
                else -> ErrorRed
            },
            fontSize = 12.sp,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.End
        )
    }
    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
}

// =============================================
// MOTM TAB
// =============================================
@Composable
fun MotmTab(
    uiState: PostMatchUiState,
    onSelectMotm: (com.crickethub.data.model.Player) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "Player of the Match",
                color = AmberColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Auto-suggested based on performance. Tap to override.",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(uiState.motmCandidates.take(10)) { candidate ->
            val isSelected = uiState.selectedMotm?.id == candidate.player.id

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) AmberColor.copy(alpha = 0.15f) else SurfaceCard
                    )
                    .border(
                        1.dp,
                        if (isSelected) AmberColor else BorderColor,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelectMotm(candidate.player) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = AmberColor,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .border(1.dp, BorderColor, CircleShape)
                        )
                    }
                    Column {
                        Text(
                            candidate.player.fullName,
                            color = if (isSelected) AmberColor else TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            buildString {
                                if (candidate.runs > 0) append("${candidate.runs} runs")
                                if (candidate.runs > 0 && candidate.wickets > 0) append(" | ")
                                if (candidate.wickets > 0) append("${candidate.wickets} wkts")
                            },
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
                Text(
                    "Score: ${candidate.score.toInt()}",
                    color = if (isSelected) AmberColor else TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// =============================================
// PDF EXPORT
// =============================================
fun exportPdf(context: Context, uiState: PostMatchUiState) {
    try {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            isFakeBoldText = true
        }
        val headerPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 14f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
        }
        val smallPaint = Paint().apply {
            color = Color.GRAY
            textSize = 10f
        }

        var y = 50f

        // Title
        canvas.drawText("CricketHub — Match Scorecard", 30f, y, titlePaint)
        y += 30f

        // Result
        canvas.drawText(uiState.resultText, 30f, y, headerPaint)
        y += 25f

        // MOTM
        uiState.selectedMotm?.let {
            canvas.drawText("Player of the Match: ${it.fullName}", 30f, y, headerPaint)
            y += 25f
        }

        y += 10f

        // Innings 1
        uiState.innings1?.let { i1 ->
            canvas.drawText(
                "${uiState.team1?.name ?: "Team 1"}: ${i1.totalRuns}/${i1.totalWickets} (${i1.totalBalls / 6}.${i1.totalBalls % 6} ov)",
                30f, y, headerPaint
            )
            y += 20f

            canvas.drawText("Batsman                    R    B   4s  6s    SR", 30f, y, smallPaint)
            y += 15f

            uiState.innings1Batting.forEach { b ->
                val status = if (b.isOut) b.dismissalType?.replace("_", " ") ?: "out" else "not out"
                canvas.drawText(
                    "${b.player.fullName.take(20).padEnd(20)} ${b.runs.toString().padStart(4)} ${b.balls.toString().padStart(4)} ${b.fours.toString().padStart(4)} ${b.sixes.toString().padStart(3)} ${"%.1f".format(b.strikeRate).padStart(6)}",
                    30f, y, bodyPaint
                )
                y += 14f
            }

            canvas.drawText("Extras: ${i1.extrasTotal}", 30f, y, bodyPaint)
            y += 20f
        }

        // Innings 2
        uiState.innings2?.let { i2 ->
            canvas.drawText(
                "${uiState.team2?.name ?: "Team 2"}: ${i2.totalRuns}/${i2.totalWickets} (${i2.totalBalls / 6}.${i2.totalBalls % 6} ov)",
                30f, y, headerPaint
            )
            y += 20f

            canvas.drawText("Batsman                    R    B   4s  6s    SR", 30f, y, smallPaint)
            y += 15f

            uiState.innings2Batting.forEach { b ->
                canvas.drawText(
                    "${b.player.fullName.take(20).padEnd(20)} ${b.runs.toString().padStart(4)} ${b.balls.toString().padStart(4)} ${b.fours.toString().padStart(4)} ${b.sixes.toString().padStart(3)} ${"%.1f".format(b.strikeRate).padStart(6)}",
                    30f, y, bodyPaint
                )
                y += 14f
            }

            canvas.drawText("Extras: ${i2.extrasTotal}", 30f, y, bodyPaint)
            y += 20f
        }

        document.finishPage(page)

        // Save file
        val file = File(context.cacheDir, "scorecard_${System.currentTimeMillis()}.pdf")
        document.writeTo(FileOutputStream(file))
        document.close()

        // Share
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Scorecard"))

    } catch (e: Exception) {
        android.util.Log.e("CricketHub", "PDF error: ${e.message}", e)
    }
}