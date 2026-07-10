package com.crickethub.ui.match.scoring

import android.content.Intent
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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Score
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crickethub.data.model.Player
import com.crickethub.data.model.ScoringUiState

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
fun ScoringScreen(
    matchId: String,
    onBack: () -> Unit,
    onInningsComplete: () -> Unit = {},
    onViewScorecard: () -> Unit = {},
    onViewAnalytics: () -> Unit = {},
    viewModel: ScoringViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showWicketDialog by remember { mutableStateOf(false) }
    var showExtrasDialog by remember { mutableStateOf(false) }
    var showSelectBatsman by remember { mutableStateOf(false) }
    var showSelectBowler by remember { mutableStateOf(false) }
    var showSelectNonStriker by remember { mutableStateOf(false) }
    var showPenaltyDialog by remember { mutableStateOf(false) }
    var showManualEditDialog by remember { mutableStateOf(false) }
    var isFreeHit by remember { mutableStateOf(false) }

    LaunchedEffect(matchId) {
        viewModel.loadMatch(matchId)
    }

    LaunchedEffect(uiState.inningsComplete) {
        if (uiState.inningsComplete) {
            viewModel.checkAndStartNextInnings(matchId) {
                onInningsComplete()
            }
        }
    }

    LaunchedEffect(uiState.matchComplete) {
        if (uiState.matchComplete) {
            onInningsComplete()
        }
    }

    LaunchedEffect(uiState.balls.size) {
        val lastBall = uiState.balls.lastOrNull()
        isFreeHit = lastBall?.extrasType == "no_ball" &&
                (uiState.match?.freeHitOnNoball == true)
    }

    val needStriker = uiState.striker == null && !uiState.isLoading && uiState.innings != null
    val needBowler = uiState.currentBowler == null && !uiState.isLoading && uiState.innings != null
    val needNonStriker = uiState.nonStriker == null && !uiState.isLoading && uiState.innings != null

    fun shareScore() {
        val text = buildString {
            appendLine("🏏 CricketHub LIVE")
            appendLine("Score: ${uiState.totalRuns}/${uiState.totalWickets} (${uiState.currentOver}.${uiState.currentBall} ov)")
            appendLine("CRR: ${"%.2f".format(uiState.runRate)}")
            uiState.striker?.let {
                appendLine("🏏 ${it.fullName}*: ${uiState.batsmanStats[it.id]?.runs ?: 0}(${uiState.batsmanStats[it.id]?.balls ?: 0})")
            }
            uiState.currentBowler?.let {
                appendLine("🎳 ${it.fullName}: ${uiState.bowlerStats[it.id]?.overs ?: "0.0"}-${uiState.bowlerStats[it.id]?.runs ?: 0}-${uiState.bowlerStats[it.id]?.wickets ?: 0}")
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share Score"))
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text(
                    "Live Scoring",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = TextPrimary, modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onViewScorecard) {
                    Icon(Icons.Default.Score, contentDescription = "Scorecard", tint = NeonBlue, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onViewAnalytics) {
                    Icon(Icons.Default.BarChart, contentDescription = "Analytics", tint = PurpleColor, modifier = Modifier.size(20.dp))
                }
                if (isFreeHit) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonGreen)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("FREE HIT", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(ErrorRed)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("● LIVE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonGreen)
                }
            } else {
                ScoreHeader(uiState = uiState, onShare = { shareScore() })
                Last6BallsRow(balls = uiState.last6Balls)
                Spacer(modifier = Modifier.height(8.dp))

                CurrentBatsmenRow(
                    striker = uiState.striker,
                    nonStriker = uiState.nonStriker,
                    batsmanStats = uiState.batsmanStats,
                    strikerClickable = needStriker,
                    nonStrikerClickable = needNonStriker,
                    onChangeStriker = { if (needStriker) showSelectBatsman = true },
                    onChangeNonStriker = { if (needNonStriker) showSelectNonStriker = true }
                )

                Spacer(modifier = Modifier.height(8.dp))

                CurrentBowlerRow(
                    bowler = uiState.currentBowler,
                    bowlerStats = uiState.bowlerStats,
                    bowlerClickable = needBowler,
                    onChangeBowler = { if (needBowler) showSelectBowler = true }
                )

                Spacer(modifier = Modifier.weight(1f))

                uiState.error?.let {
                    Text(it, color = ErrorRed, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
                }

                if (uiState.inningsComplete || uiState.matchComplete) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NeonGreen.copy(alpha = 0.15f))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = NeonGreen, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (uiState.matchComplete) "Match Complete! Loading summary..."
                                else "Innings Complete! Starting next innings...",
                                color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (!needStriker && !needBowler && !needNonStriker &&
                    !uiState.inningsComplete && !uiState.matchComplete
                ) {
                    ScoringButtons(
                        isLoading = uiState.isLoading,
                        isFreeHit = isFreeHit,
                        onRuns = { runs -> viewModel.recordBall(runsOffBat = runs) },
                        onWicket = { if (!isFreeHit) showWicketDialog = true },
                        onExtras = { showExtrasDialog = true },
                        onUndo = { viewModel.undoLastBall() },
                        onPenalty = { showPenaltyDialog = true },
                        onManualEdit = { showManualEditDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Striker select
        if (needStriker || showSelectBatsman) {
            PlayerSelectDialog(
                title = "Select Striker",
                players = uiState.battingTeamPlayers.filter { player ->
                    val stats = uiState.batsmanStats[player.id]
                    stats?.isOut != true && player.id != uiState.nonStriker?.id
                },
                onPlayerSelected = {
                    viewModel.setStriker(it)
                    showSelectBatsman = false
                },
                onDismiss = { showSelectBatsman = false }
            )
        }

        // Non-striker select
        if (needNonStriker || showSelectNonStriker) {
            PlayerSelectDialog(
                title = "Select Non-Striker",
                players = uiState.battingTeamPlayers.filter { player ->
                    val stats = uiState.batsmanStats[player.id]
                    stats?.isOut != true && player.id != uiState.striker?.id
                },
                onPlayerSelected = {
                    viewModel.setNonStriker(it)
                    showSelectNonStriker = false
                },
                onDismiss = { showSelectNonStriker = false }
            )
        }

        // Bowler select
        if (needBowler || showSelectBowler) {
            val totalOvers = uiState.match?.totalOvers ?: 20
            val lastBowlerId = uiState.balls
                .filter { it.extrasType != "wide" && it.extrasType != "no_ball" }
                .lastOrNull()?.bowlerId

            PlayerSelectDialog(
                title = "Select Bowler (Max ${viewModel.getMaxOversPerBowler(totalOvers)} overs)",
                players = uiState.bowlingTeamPlayers.filter { player ->
                    player.id != lastBowlerId && viewModel.canBowlerBowl(player.id, totalOvers)
                },
                onPlayerSelected = {
                    viewModel.setBowler(it)
                    showSelectBowler = false
                },
                onDismiss = { showSelectBowler = false }
            )
        }

        // Wicket dialog
        if (showWicketDialog) {
            WicketDialog(
                onDismiss = { showWicketDialog = false },
                onConfirm = { wicketType, runs, fielderName ->
                    viewModel.recordBall(
                        runsOffBat = runs,
                        isWicket = true,
                        wicketType = wicketType,
                        fielderName = fielderName
                    )
                    showWicketDialog = false
                },
                onRetiredHurt = {
                    viewModel.recordBall(
                        runsOffBat = 0,
                        isWicket = true,
                        wicketType = "retired_hurt"
                    )
                    showWicketDialog = false
                }
            )
        }

        // Extras dialog
        if (showExtrasDialog) {
            ExtrasDialog(
                onDismiss = { showExtrasDialog = false },
                onConfirm = { extrasType, runs ->
                    viewModel.recordBall(runsOffBat = 0, extrasType = extrasType, extrasRuns = runs)
                    showExtrasDialog = false
                }
            )
        }

        // Penalty dialog
        if (showPenaltyDialog) {
            PenaltyRunsDialog(
                onDismiss = { showPenaltyDialog = false },
                onConfirm = { team ->
                    viewModel.addPenaltyRuns(team)
                    showPenaltyDialog = false
                }
            )
        }

        // Manual edit dialog
        if (showManualEditDialog) {
            ManualEditDialog(
                currentRuns = uiState.totalRuns,
                currentWickets = uiState.totalWickets,
                onDismiss = { showManualEditDialog = false },
                onConfirm = { runs, wickets ->
                    viewModel.manualEdit(runs, wickets)
                    showManualEditDialog = false
                }
            )
        }
    }
}

@Composable
fun ScoreHeader(uiState: ScoringUiState, onShare: () -> Unit) {
    val match = uiState.match
    val currentOver = uiState.currentOver
    val powerplayOvers = match?.powerplayOvers ?: 6
    val totalOvers = match?.totalOvers ?: 20

    val phaseLabel = when {
        match == null -> null
        currentOver < powerplayOvers -> "PP"
        currentOver >= (totalOvers - (match.powerplay3Overs.takeIf { it > 0 } ?: 4)) -> "P3"
        else -> "P2"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${uiState.totalRuns}/${uiState.totalWickets}",
                    fontSize = 48.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        "${uiState.currentOver}.${uiState.currentBall}",
                        fontSize = 24.sp, color = NeonGreen, fontWeight = FontWeight.Medium
                    )
                    phaseLabel?.let {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AmberColor.copy(alpha = 0.2f))
                                .border(1.dp, AmberColor, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(it, color = AmberColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("CRR: ${"%.2f".format(uiState.runRate)}", color = TextSecondary, fontSize = 13.sp)
                match?.let { Text("${it.totalOvers} overs", color = TextSecondary, fontSize = 12.sp) }
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = onShare,
                    colors = ButtonDefaults.textButtonColors(contentColor = NeonGreen)
                ) { Text("📤 Share", fontSize = 12.sp) }
            }
        }
    }
}

@Composable
fun Last6BallsRow(balls: List<String>) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("This over:", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterVertically))
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
                Text(ball, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CurrentBatsmenRow(
    striker: Player?,
    nonStriker: Player?,
    batsmanStats: Map<String, com.crickethub.data.model.BatsmanStats>,
    strikerClickable: Boolean,
    nonStrikerClickable: Boolean,
    onChangeStriker: () -> Unit,
    onChangeNonStriker: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceCard)
                .border(1.dp, if (strikerClickable) NeonGreen else BorderColor, RoundedCornerShape(8.dp))
                .then(if (strikerClickable) Modifier.clickable { onChangeStriker() } else Modifier)
                .padding(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("*", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    striker?.fullName ?: "Select Batsman",
                    color = if (striker != null) TextPrimary else TextSecondary,
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1
                )
            }
            striker?.let { s ->
                val stats = batsmanStats[s.id]
                if (stats != null) {
                    Text(
                        "${stats.runs}(${stats.balls}) 4s:${stats.fours} 6s:${stats.sixes} SR:${"%.1f".format(stats.strikeRate)}",
                        color = TextSecondary, fontSize = 10.sp
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceCard)
                .border(1.dp, if (nonStrikerClickable) NeonGreen else BorderColor, RoundedCornerShape(8.dp))
                .then(if (nonStrikerClickable) Modifier.clickable { onChangeNonStriker() } else Modifier)
                .padding(10.dp)
        ) {
            Text(
                nonStriker?.fullName ?: "Select Non-Striker",
                color = if (nonStriker != null) TextPrimary else TextSecondary,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1
            )
            nonStriker?.let { ns ->
                val stats = batsmanStats[ns.id]
                if (stats != null) {
                    Text(
                        "${stats.runs}(${stats.balls}) 4s:${stats.fours} 6s:${stats.sixes}",
                        color = TextSecondary, fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CurrentBowlerRow(
    bowler: Player?,
    bowlerStats: Map<String, com.crickethub.data.model.BowlerStats>,
    bowlerClickable: Boolean,
    onChangeBowler: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCard)
            .border(1.dp, if (bowlerClickable) NeonGreen else BorderColor, RoundedCornerShape(8.dp))
            .then(if (bowlerClickable) Modifier.clickable { onChangeBowler() } else Modifier)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            bowler?.fullName ?: "Select Bowler",
            color = if (bowler != null) TextPrimary else TextSecondary,
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold
        )
        bowler?.let { b ->
            val stats = bowlerStats[b.id]
            if (stats != null) {
                Text(
                    "${stats.overs} M:0 R:${stats.runs} W:${stats.wickets} Eco:${"%.1f".format(stats.economy)}",
                    color = TextSecondary, fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun ScoringButtons(
    isLoading: Boolean,
    isFreeHit: Boolean = false,
    onRuns: (Int) -> Unit,
    onWicket: () -> Unit,
    onExtras: () -> Unit,
    onUndo: () -> Unit,
    onPenalty: () -> Unit,
    onManualEdit: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(0, 1, 2, 3, 4, 5, 6).forEach { runs ->
                val bgColor = when (runs) {
                    4 -> NeonBlue.copy(alpha = 0.8f)
                    6 -> NeonGreen.copy(alpha = 0.8f)
                    5 -> PurpleColor.copy(alpha = 0.8f)
                    else -> SurfaceCard
                }
                val textColor = when (runs) {
                    4, 5, 6 -> Color.White
                    0 -> TextSecondary
                    else -> TextPrimary
                }
                Button(
                    onClick = { onRuns(runs) },
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = bgColor,
                        contentColor = textColor,
                        disabledContainerColor = bgColor.copy(alpha = 0.4f)
                    )
                ) {
                    Text(
                        if (runs == 0) "•" else runs.toString(),
                        fontSize = 18.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onWicket,
                enabled = !isLoading && !isFreeHit,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFreeHit) Color(0xFF7F1D1D).copy(alpha = 0.3f) else Color(0xFF7F1D1D),
                    contentColor = Color(0xFFFCA5A5)
                )
            ) {
                Text(if (isFreeHit) "NO OUT" else "WICKET", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Button(
                onClick = onExtras,
                enabled = !isLoading,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF78350F),
                    contentColor = Color(0xFFFCD34D)
                )
            ) { Text("EXTRAS", fontWeight = FontWeight.Bold, fontSize = 13.sp) }

            Button(
                onClick = onUndo,
                enabled = !isLoading,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceCard,
                    contentColor = TextSecondary
                )
            ) { Text("UNDO", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onPenalty,
                enabled = !isLoading,
                modifier = Modifier.weight(1f).height(40.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberColor)
            ) { Text("+5 Penalty", fontSize = 11.sp) }

            OutlinedButton(
                onClick = onManualEdit,
                enabled = !isLoading,
                modifier = Modifier.weight(1f).height(40.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonBlue)
            ) { Text("Manual Edit", fontSize = 11.sp) }
        }
    }
}

@Composable
fun PlayerSelectDialog(
    title: String,
    players: List<Player>,
    onPlayerSelected: (Player) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = { Text(title, color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(players) { player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BackgroundDark)
                            .clickable { onPlayerSelected(player) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(NeonGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                player.jerseyNo?.toString() ?: "-",
                                color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(player.fullName, color = TextPrimary, fontSize = 14.sp)
                            player.role?.let {
                                Text(
                                    it.replace("_", " ").replaceFirstChar { c -> c.uppercase() },
                                    color = TextSecondary, fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
                if (players.isEmpty()) {
                    item {
                        Text(
                            "No players available",
                            color = TextSecondary,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
fun WicketDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int, String?) -> Unit,
    onRetiredHurt: () -> Unit
) {
    var selectedType by remember { mutableStateOf<String?>(null) }
    var runsBeforeWicket by remember { mutableStateOf(0) }
    var fielderName by remember { mutableStateOf("") }

    val showFielderInput = selectedType in listOf("caught", "run_out", "stumped")

    val wicketTypes = listOf(
        "bowled" to "Bowled",
        "caught" to "Caught",
        "lbw" to "LBW",
        "run_out" to "Run Out",
        "stumped" to "Stumped",
        "hit_wicket" to "Hit Wicket",
        "retired_out" to "Retired Out",
        "obstructing" to "Obstructing",
        "timed_out" to "Timed Out",
        "handled_ball" to "Handled Ball",
        "hit_ball_twice" to "Hit Ball Twice"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = { Text("Wicket!", color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 480.dp)
            ) {
                item {
                    Text("How was the batsman dismissed?", color = TextSecondary, fontSize = 13.sp)
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        wicketTypes.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { (value, label) ->
                                    val selected = selectedType == value
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selected) ErrorRed.copy(alpha = 0.2f) else BackgroundDark)
                                            .border(1.dp, if (selected) ErrorRed else BorderColor, RoundedCornerShape(8.dp))
                                            .clickable { selectedType = value }
                                            .padding(10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            label,
                                            color = if (selected) ErrorRed else TextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                if (showFielderInput) {
                    item {
                        OutlinedTextField(
                            value = fielderName,
                            onValueChange = { fielderName = it },
                            label = {
                                Text(
                                    when (selectedType) {
                                        "caught" -> "Caught by (fielder name)"
                                        "run_out" -> "Run out by (fielder name)"
                                        "stumped" -> "Stumped by (keeper name)"
                                        else -> "Fielder name"
                                    }
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = ErrorRed,
                                unfocusedBorderColor = BorderColor,
                                cursorColor = ErrorRed,
                                focusedLabelColor = ErrorRed,
                                unfocusedLabelColor = TextSecondary
                            )
                        )
                    }
                }
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AmberColor.copy(alpha = 0.1f))
                            .border(1.dp, AmberColor, RoundedCornerShape(8.dp))
                            .clickable { onRetiredHurt() }
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "🤕 Retired Hurt (can bat again)",
                            color = AmberColor, fontSize = 13.sp, fontWeight = FontWeight.Medium
                        )
                    }
                }
                item {
                    Text("Runs before wicket:", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (0..4).forEach { r ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (runsBeforeWicket == r) NeonGreen.copy(alpha = 0.3f) else BackgroundDark)
                                    .border(1.dp, if (runsBeforeWicket == r) NeonGreen else BorderColor, CircleShape)
                                    .clickable { runsBeforeWicket = r },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    r.toString(),
                                    color = if (runsBeforeWicket == r) NeonGreen else TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedType?.let { onConfirm(it, runsBeforeWicket, fielderName.ifBlank { null }) }
                },
                enabled = selectedType != null,
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
            ) { Text("Confirm", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
fun ExtrasDialog(onDismiss: () -> Unit, onConfirm: (String, Int) -> Unit) {
    var selectedType by remember { mutableStateOf<String?>(null) }
    var runs by remember { mutableStateOf(1) }

    val extrasTypes = listOf(
        "wide" to "Wide", "no_ball" to "No Ball",
        "bye" to "Bye", "leg_bye" to "Leg Bye"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = { Text("Extras", color = AmberColor, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    extrasTypes.forEach { (value, label) ->
                        val selected = selectedType == value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) AmberColor.copy(alpha = 0.2f) else BackgroundDark)
                                .border(1.dp, if (selected) AmberColor else BorderColor, RoundedCornerShape(8.dp))
                                .clickable { selectedType = value }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (selected) AmberColor else TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                Text("Runs:", color = TextSecondary, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..6).forEach { r ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (runs == r) AmberColor.copy(alpha = 0.3f) else BackgroundDark)
                                .border(1.dp, if (runs == r) AmberColor else BorderColor, CircleShape)
                                .clickable { runs = r },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                r.toString(),
                                color = if (runs == r) AmberColor else TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedType?.let { onConfirm(it, runs) } },
                enabled = selectedType != null,
                colors = ButtonDefaults.buttonColors(containerColor = AmberColor)
            ) { Text("Confirm", color = Color.Black) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
fun PenaltyRunsDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var selectedTeam by remember { mutableStateOf("batting") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = { Text("+5 Penalty Runs", color = AmberColor, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Award 5 penalty runs to:", color = TextSecondary, fontSize = 13.sp)
                listOf("batting" to "Batting Team", "bowling" to "Bowling Team").forEach { (value, label) ->
                    val selected = selectedTeam == value
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) AmberColor.copy(alpha = 0.15f) else BackgroundDark)
                            .border(1.dp, if (selected) AmberColor else BorderColor, RoundedCornerShape(8.dp))
                            .clickable { selectedTeam = value }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, color = if (selected) AmberColor else TextSecondary, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedTeam) },
                colors = ButtonDefaults.buttonColors(containerColor = AmberColor)
            ) { Text("Add Penalty", color = Color.Black, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
fun ManualEditDialog(
    currentRuns: Int,
    currentWickets: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var runs by remember { mutableStateOf(currentRuns.toString()) }
    var wickets by remember { mutableStateOf(currentWickets.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = { Text("Manual Score Edit", color = NeonBlue, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("⚠️ Use carefully — this directly edits the score", color = AmberColor, fontSize = 12.sp)
                OutlinedTextField(
                    value = runs,
                    onValueChange = { if (it.all { c -> c.isDigit() }) runs = it },
                    label = { Text("Total Runs") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonBlue, unfocusedBorderColor = BorderColor,
                        cursorColor = NeonBlue, focusedLabelColor = NeonBlue,
                        unfocusedLabelColor = TextSecondary
                    )
                )
                OutlinedTextField(
                    value = wickets,
                    onValueChange = { if (it.all { c -> c.isDigit() } && (it.toIntOrNull() ?: 0) <= 10) wickets = it },
                    label = { Text("Wickets") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonBlue, unfocusedBorderColor = BorderColor,
                        cursorColor = NeonBlue, focusedLabelColor = NeonBlue,
                        unfocusedLabelColor = TextSecondary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(runs.toIntOrNull() ?: currentRuns, wickets.toIntOrNull() ?: currentWickets)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
            ) { Text("Update Score", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}