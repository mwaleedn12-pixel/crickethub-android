package com.crickethub.ui.match.scoring

import android.content.Intent
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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Score
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crickethub.data.model.Player
import com.crickethub.data.model.ScoringUiState
import com.crickethub.ui.match.getDLSResourceExact
import com.crickethub.ui.theme.*


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
    var resumeKey by remember { mutableStateOf(0) }

    // Detect when we return to this screen
    // navBackStackEntry removed






    val context = LocalContext.current

    var showWicketDialog by remember { mutableStateOf(false) }
    var showExtrasDialog by remember { mutableStateOf(false) }
    var showSelectBatsman by remember { mutableStateOf(false) }
    var showSelectBowler by remember { mutableStateOf(false) }
    var showSelectNonStriker by remember { mutableStateOf(false) }
    var showPenaltyDialog by remember { mutableStateOf(false) }
    var showManualEditDialog by remember { mutableStateOf(false) }
    var showDLSDialog by remember { mutableStateOf(false) }
    var isFreeHit by remember { mutableStateOf(false) }

    LaunchedEffect(matchId, resumeKey) { viewModel.resumeMatch(matchId) }

    // Re-run resumeMatch every time screen becomes visible again
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                resumeKey++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.inningsComplete) {
        if (uiState.inningsComplete) {
            viewModel.checkAndStartNextInnings(matchId) { onInningsComplete() }
        }
    }

    LaunchedEffect(uiState.matchComplete) {
        if (uiState.matchComplete) onInningsComplete()
    }

    LaunchedEffect(uiState.balls.size) {
        val lastBall = uiState.balls.lastOrNull()
        isFreeHit = lastBall?.extrasType == "no_ball" && (uiState.match?.freeHitOnNoball == true)
        if (uiState.dlsEnabled) viewModel.updateDLSParScore()
    }

    val needStriker = uiState.striker == null && !uiState.isLoading && uiState.innings != null
    val needBowler = uiState.currentBowler == null && !uiState.isLoading && uiState.innings != null
    val needNonStriker = uiState.nonStriker == null && !uiState.isLoading && uiState.innings != null
    val isSecondInnings = uiState.innings?.inningsNo == 2
    val currentWickets = uiState.innings?.totalWickets ?: 0

    // ── Ball popup (+1, SIX!, WICKET, WIDE ...) ───────────────────────────────
    var popupLabel by remember { mutableStateOf<String?>(null) }
    var popupColor by remember { mutableStateOf(NeonGreen) }
    var popupKey by remember { mutableStateOf(0) }
    var lastPopupBallCount by remember { mutableStateOf(-1) }
    // Fire only when the ball count grows by exactly 1 (a freshly scored ball) — not
    // on resume, which loads many balls at once, and not on undo, which shrinks it.
    LaunchedEffect(uiState.balls.size) {
        val size = uiState.balls.size
        val prev = lastPopupBallCount
        lastPopupBallCount = size
        if (prev == -1) return@LaunchedEffect          // first load / resume: no popup
        if (size != prev + 1) return@LaunchedEffect     // undo or bulk change: no popup
        val last = uiState.balls.lastOrNull() ?: return@LaunchedEffect
        val (label, color) = ballPopupLabel(last)
        popupLabel = label
        popupColor = color
        popupKey++
    }

    fun shareScore() {
        val text = buildString {
            appendLine("🏏 CricketHub LIVE")
            appendLine("Score: ${uiState.totalRuns}/${uiState.totalWickets} (${uiState.currentOver}.${uiState.currentBall} ov)")
            appendLine("CRR: ${"%.2f".format(uiState.runRate)}")
            if (uiState.last6Balls.isNotEmpty()) appendLine("This over: ${uiState.last6Balls.joinToString(" ")}")
            uiState.striker?.let {
                appendLine("🏏 ${it.fullName}*: ${uiState.batsmanStats[it.id]?.runs ?: 0}(${uiState.batsmanStats[it.id]?.balls ?: 0})")
            }
            uiState.currentBowler?.let {
                appendLine("🎳 ${it.fullName}: ${uiState.bowlerStats[it.id]?.overs ?: "0.0"}-${uiState.bowlerStats[it.id]?.runs ?: 0}-${uiState.bowlerStats[it.id]?.wickets ?: 0}")
            }
            if (uiState.dlsEnabled && uiState.dlsParScore != null) {
                appendLine("🌧️ DLS Par: ${uiState.dlsParScore} | Target: ${uiState.dlsTarget}")
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share Score"))
    }

    CricketAnimatedBackground(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── TOP BAR ──────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
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
                    // DLS Button
                    IconButton(onClick = { showDLSDialog = true }) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (uiState.dlsEnabled) AmberColor.copy(alpha = 0.35f) else AmberColor.copy(alpha = 0.1f))
                                .border(1.dp, if (uiState.dlsEnabled) AmberColor else AmberColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text("🌧️", fontSize = 16.sp)
                        }
                    }
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
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text("FREE HIT", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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

                    ScoreHeader(uiState = uiState, onShare = { shareScore() },
                        popupLabel = popupLabel, popupColor = popupColor, popupKey = popupKey)

                    // ── DLS LIVE BANNER ───────────────────────────────────
                    if (uiState.dlsEnabled && uiState.dlsParScore != null) {
                        val currentRuns = uiState.innings?.totalRuns ?: 0
                        val parScore = uiState.dlsParScore!!
                        val isAhead = currentRuns > parScore
                        val isBehind = currentRuns < parScore
                        val bannerColor = when {
                            isAhead -> NeonGreen
                            isBehind -> ErrorRed
                            else -> AmberColor
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bannerColor.copy(alpha = 0.1f))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("🌧️ DLS PAR SCORE", color = AmberColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        when {
                                            isAhead -> "AHEAD by ${currentRuns - parScore}"
                                            isBehind -> "BEHIND by ${parScore - currentRuns}"
                                            else -> "ON PAR"
                                        },
                                        color = bannerColor, fontSize = 13.sp, fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "Par: $parScore",
                                        color = AmberColor, fontSize = 18.sp, fontWeight = FontWeight.Bold
                                    )
                                    uiState.dlsTarget?.let {
                                        Text("Target: $it", color = TextPrimary, fontSize = 12.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val progress = if (parScore > 0) (currentRuns.toFloat() / parScore).coerceIn(0f, 1.5f) else 0f
                            LinearProgressIndicator(
                                progress = { progress.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                                color = bannerColor,
                                trackColor = BorderColor
                            )
                        }
                    }

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
                            modifier = Modifier.fillMaxWidth().background(NeonGreen.copy(alpha = 0.15f)).padding(16.dp),
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

            // ── DIALOGS ───────────────────────────────────────────────────

            if (needStriker || showSelectBatsman) {
                PlayerSelectDialog(
                    title = "Select Striker",
                    players = uiState.battingTeamPlayers.filter { player ->
                        val stats = uiState.batsmanStats[player.id]
                        stats?.isOut != true && player.id != uiState.nonStriker?.id
                    },
                    onPlayerSelected = { viewModel.setStriker(it); showSelectBatsman = false },
                    onDismiss = { showSelectBatsman = false }
                )
            }

            if (needNonStriker || showSelectNonStriker) {
                PlayerSelectDialog(
                    title = "Select Non-Striker",
                    players = uiState.battingTeamPlayers.filter { player ->
                        val stats = uiState.batsmanStats[player.id]
                        stats?.isOut != true && player.id != uiState.striker?.id
                    },
                    onPlayerSelected = { viewModel.setNonStriker(it); showSelectNonStriker = false },
                    onDismiss = { showSelectNonStriker = false }
                )
            }

            if (needBowler || showSelectBowler) {
                val totalOvers = uiState.match?.totalOvers ?: 20
                val lastBowlerId = uiState.balls
                    .filter { it.extrasType != "wide" && it.extrasType != "no_ball" }
                    .lastOrNull()?.bowlerId
                PlayerSelectDialog(
                    title = "Select Bowler (Max ${viewModel.getMaxOversPerBowler(totalOvers)} overs)",
                    players = uiState.bowlingTeamPlayers.filter { player ->
                        player.id != lastBowlerId &&
                                viewModel.canBowlerBowl(player.id, totalOvers) &&
                                player.role?.lowercase() !in listOf("wicketkeeper", "wicket keeper", "wicket_keeper")
                    },
                    onPlayerSelected = { viewModel.setBowler(it); showSelectBowler = false },
                    onDismiss = { showSelectBowler = false }
                )
            }

            if (showWicketDialog) {
                val keeper = uiState.bowlingTeamPlayers.firstOrNull {
                    it.role?.lowercase() in listOf("wicketkeeper", "wicket keeper", "wicket_keeper")
                }
                WicketDialog(
                    onDismiss = { showWicketDialog = false },
                    onConfirm = { wicketType, runs, fielderName, nonStrikerOut ->
                        viewModel.recordBall(runsOffBat = runs, isWicket = true, wicketType = wicketType,
                            fielderName = fielderName, nonStrikerOut = nonStrikerOut)
                        showWicketDialog = false
                    },
                    onRetiredHurt = {
                        viewModel.recordBall(runsOffBat = 0, isWicket = true, wicketType = "retired_hurt")
                        showWicketDialog = false
                    },
                    keeperName = keeper?.fullName,
                    bowlingTeamPlayers = uiState.bowlingTeamPlayers
                )
            }

            if (showExtrasDialog) {
                ExtrasDialog(
                    onDismiss = { showExtrasDialog = false },
                    onConfirm = { extrasType, runs ->
                        // No-ball runs belong to the batsman -> runsOffBat. Wide/bye/leg-bye
                        // runs are extras -> extrasRuns.
                        if (extrasType == "no_ball") {
                            viewModel.recordBall(runsOffBat = runs, extrasType = extrasType, extrasRuns = 0)
                        } else {
                            viewModel.recordBall(runsOffBat = 0, extrasType = extrasType, extrasRuns = runs)
                        }
                        showExtrasDialog = false
                    }
                )
            }

            if (showPenaltyDialog) {
                PenaltyRunsDialog(
                    onDismiss = { showPenaltyDialog = false },
                    onConfirm = { team -> viewModel.addPenaltyRuns(team); showPenaltyDialog = false }
                )
            }

            if (showManualEditDialog) {
                ManualEditDialog(
                    currentRuns = uiState.totalRuns,
                    currentWickets = uiState.totalWickets,
                    onDismiss = { showManualEditDialog = false },
                    onConfirm = { runs, wickets -> viewModel.manualEdit(runs, wickets); showManualEditDialog = false }
                )
            }

            if (showDLSDialog) {
                val totalOvers = uiState.match?.totalOvers ?: 20
                val team1Score = if (isSecondInnings) {
                    (uiState.dlsTarget ?: 1) - 1
                } else {
                    uiState.innings?.totalRuns ?: 0
                }
                DLSDialog(
                    uiState = uiState,
                    isSecondInnings = isSecondInnings,
                    currentWickets = currentWickets,
                    totalOvers = totalOvers,
                    team1Score = team1Score,
                    onApply = { t1Score, t1TotalOvers, t2TotalOvers, oversAtStop, wktsAtStop, oversAtRestart ->
                        viewModel.enableDLS(
                            team1Score = t1Score,
                            team1TotalOvers = t1TotalOvers,
                            team2TotalOvers = t2TotalOvers,
                            oversRemainingAtStop = oversAtStop,
                            wicketsLostAtStop = wktsAtStop,
                            oversRemainingAtRestart = oversAtRestart
                        )
                        showDLSDialog = false
                    },
                    onDisable = { viewModel.disableDLS(); showDLSDialog = false },
                    onDismiss = { showDLSDialog = false }
                )
            }

        } // end overlay Box
    }
}


@Composable
fun DLSDialog(
    uiState: ScoringUiState,
    isSecondInnings: Boolean,
    currentWickets: Int,
    totalOvers: Int,
    team1Score: Int,
    onApply: (Int, Int, Int, Double, Int, Double) -> Unit,
    onDisable: () -> Unit,
    onDismiss: () -> Unit
) {
    // sirf legal balls — wides/no-balls DLS mein count nahi hote
    val ballsBowled = uiState.balls.count { it.extrasType != "wide" && it.extrasType != "no_ball" }
    val oversBowledInt = ballsBowled / 6
    val ballsInCurrentOver = ballsBowled % 6
    val oversBowledDisplay = if (ballsInCurrentOver == 0) "$oversBowledInt.0" else "$oversBowledInt.$ballsInCurrentOver"

    // Overs remaining BEFORE rain
    val oversRemainingAtStop = totalOvers - oversBowledInt - (ballsInCurrentOver / 6.0)

    var newTotalOversStr by remember { mutableStateOf("") }

    val newTotalOvers = newTotalOversStr.toIntOrNull()
    val oversAtRestart = if (newTotalOvers != null) {
        (newTotalOvers - oversBowledInt - ballsInCurrentOver / 6.0).coerceAtLeast(0.0)
    } else null

    // Preview calculation
    val previewResult = if (newTotalOvers != null && oversAtRestart != null && oversAtRestart >= 0) {
        com.crickethub.ui.match.calculateDLS(
            team1Score = team1Score,
            team1TotalOvers = totalOvers,
            team1Interruptions = emptyList(),
            team2TotalOvers = totalOvers, // original overs — rain reduction interruption se hoti hai
            team2Interruptions = listOf(
                com.crickethub.ui.match.Interruption(
                    id = 1,
                    oversRemainingAtStop = oversRemainingAtStop,
                    wicketsLostAtStop = currentWickets,
                    oversRemainingAtRestart = oversAtRestart
                )
            )
        )
    } else null

    val canApply = newTotalOvers != null && newTotalOvers < totalOvers && newTotalOvers > oversBowledInt

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111827),
        title = { Text("🌧️ Rain Delay — DLS", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 560.dp)
            ) {
                if (uiState.dlsEnabled) {
                    // ── Active ───────────────────────────────────────
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.08f))
                                .border(1.dp, Color(0xFF10B981), RoundedCornerShape(10.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("✅ DLS Active", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                            HorizontalDivider(color = Color(0xFF1F2937))
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Column {
                                    Text("Current Par Score", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                                    Text("(Updates every ball)", color = Color(0xFF9CA3AF), fontSize = 10.sp)
                                }
                                Text("${uiState.dlsParScore}", color = Color(0xFFF59E0B), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Text("Revised Target", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                                Text("${uiState.dlsTarget}", color = Color(0xFF10B981), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    item {
                        Button(
                            onClick = onDisable, modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) { Text("Disable DLS", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                } else {
                    // ── Match Info ───────────────────────────────────
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF030712))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text("Match at Interruption", color = Color(0xFF9CA3AF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            HorizontalDivider(color = Color(0xFF1F2937))
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text("Original overs", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                                Text("$totalOvers", color = Color(0xFFF9FAFB), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text("Overs bowled", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                                Text(oversBowledDisplay, color = Color(0xFFF9FAFB), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text("Wickets fallen", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                                Text("$currentWickets", color = Color(0xFFF9FAFB), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text(if (isSecondInnings) "Chasing" else "Score", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                                Text(
                                    if (isSecondInnings) "Target: ${team1Score + 1}" else "$team1Score",
                                    color = Color(0xFFF9FAFB), fontSize = 12.sp, fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // ── Input ────────────────────────────────────────
                    item {
                        OutlinedTextField(
                            value = newTotalOversStr,
                            onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) newTotalOversStr = it },
                            label = { Text("Revised overs (after rain reduction)") },
                            placeholder = { Text("e.g. 35", color = Color(0xFF9CA3AF)) },
                            supportingText = {
                                Text(
                                    "Match referee reduces overs from $totalOvers to how many?",
                                    color = Color(0xFF9CA3AF), fontSize = 11.sp
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFFF9FAFB), unfocusedTextColor = Color(0xFFF9FAFB),
                                focusedBorderColor = Color(0xFFF59E0B), unfocusedBorderColor = Color(0xFF1F2937),
                                cursorColor = Color(0xFFF59E0B), focusedLabelColor = Color(0xFFF59E0B),
                                unfocusedLabelColor = Color(0xFF9CA3AF)
                            )
                        )
                    }

                    // ── Preview ──────────────────────────────────────
                    if (previewResult != null) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.06f))
                                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("DLS Calculation Preview", color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                HorizontalDivider(color = Color(0xFF1F2937))
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                    Text("R1 — Team 1 resource", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                                    Text("${"%.1f".format(previewResult.team1Resource)}%", color = Color(0xFFF9FAFB), fontSize = 12.sp)
                                }
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                    Text("R2 — Team 2 resource", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                                    Text(
                                        "${"%.1f".format(previewResult.team2Resource)}%",
                                        color = if (previewResult.team2Resource < previewResult.team1Resource) Color(0xFFEF4444) else Color(0xFF10B981),
                                        fontSize = 12.sp, fontWeight = FontWeight.Medium
                                    )
                                }
                                HorizontalDivider(color = Color(0xFF1F2937))
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Column {
                                        Text("Revised Target", color = Color(0xFF10B981), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text("in $newTotalOvers overs", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                                    }
                                    Text("${previewResult.targetScore}", color = Color(0xFF10B981), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text("Par Score (tie)", color = Color(0xFFF59E0B), fontSize = 13.sp)
                                    Text("${previewResult.parScore}", color = Color(0xFFF59E0B), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                                // Step by step
                                previewResult.explanation.forEach { step ->
                                    Text("• $step", color = Color(0xFF9CA3AF), fontSize = 10.sp)
                                }
                            }
                        }
                    } else if (newTotalOversStr.isNotEmpty()) {
                        item {
                            Text(
                                when {
                                    newTotalOvers == null -> "Valid number enter karo"
                                    newTotalOvers >= totalOvers -> "Revised overs must be less than original ($totalOvers)"
                                    newTotalOvers <= oversBowledInt -> "Revised overs must be more than overs already bowled ($oversBowledInt)"
                                    else -> ""
                                },
                                color = Color(0xFFEF4444), fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!uiState.dlsEnabled && canApply && oversAtRestart != null && newTotalOvers != null) {
                Button(
                    onClick = {
                        onApply(
                            team1Score,
                            totalOvers,
                            totalOvers, // team2TotalOvers = original overs
                            oversRemainingAtStop,
                            currentWickets,
                            oversAtRestart
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                ) { Text("Apply DLS", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF9CA3AF)) }
        }
    )
}


// ── SCORE HEADER ──────────────────────────────────────────────────────────────

@Composable
fun ScoreHeader(uiState: ScoringUiState, onShare: () -> Unit,
                popupLabel: String? = null, popupColor: androidx.compose.ui.graphics.Color = NeonGreen, popupKey: Int = 0) {
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

    Box(modifier = Modifier.fillMaxWidth().background(SurfaceCard)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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
                        Text("${uiState.currentOver}.${uiState.currentBall}", fontSize = 24.sp, color = NeonGreen, fontWeight = FontWeight.Medium)
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
                    TextButton(onClick = onShare, colors = ButtonDefaults.textButtonColors(contentColor = NeonGreen)) {
                        Text("📤 Share", fontSize = 12.sp)
                    }
                }
            }
        }
        // Celebratory ball-result overlay across the header band
        BallPopup(
            label = popupLabel,
            color = popupColor,
            triggerKey = popupKey,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

// ── LAST 6 BALLS ──────────────────────────────────────────────────────────────

@Composable
fun Last6BallsRow(balls: List<String>) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("This over:", color = TextSecondary, fontSize = 12.sp)
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
                modifier = Modifier.size(32.dp).clip(CircleShape).background(bgColor).border(1.dp, BorderColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(ball, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── CURRENT BATSMEN ───────────────────────────────────────────────────────────

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
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
            modifier = Modifier
                .weight(1f).clip(RoundedCornerShape(8.dp)).background(SurfaceCard)
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
                batsmanStats[s.id]?.let { stats ->
                    Text("${stats.runs}(${stats.balls}) 4s:${stats.fours} 6s:${stats.sixes} SR:${"%.1f".format(stats.strikeRate)}", color = TextSecondary, fontSize = 10.sp)
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f).clip(RoundedCornerShape(8.dp)).background(SurfaceCard)
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
                batsmanStats[ns.id]?.let { stats ->
                    Text("${stats.runs}(${stats.balls}) 4s:${stats.fours} 6s:${stats.sixes}", color = TextSecondary, fontSize = 10.sp)
                }
            }
        }
    }
}

// ── CURRENT BOWLER ────────────────────────────────────────────────────────────

@Composable
fun CurrentBowlerRow(
    bowler: Player?,
    bowlerStats: Map<String, com.crickethub.data.model.BowlerStats>,
    bowlerClickable: Boolean,
    onChangeBowler: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(8.dp))
            .background(SurfaceCard)
            .border(1.dp, if (bowlerClickable) NeonGreen else BorderColor, RoundedCornerShape(8.dp))
            .then(if (bowlerClickable) Modifier.clickable { onChangeBowler() } else Modifier)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(bowler?.fullName ?: "Select Bowler", color = if (bowler != null) TextPrimary else TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        bowler?.let { b ->
            bowlerStats[b.id]?.let { stats ->
                Text("${stats.overs} M:0 R:${stats.runs} W:${stats.wickets} Eco:${"%.1f".format(stats.economy)}", color = TextSecondary, fontSize = 11.sp)
            }
        }
    }
}

// ── SCORING BUTTONS ───────────────────────────────────────────────────────────

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
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 1, 2, 3, 4, 5, 6).forEach { runs ->
                val bgColor = when (runs) { 4 -> NeonBlue.copy(alpha = 0.8f); 6 -> NeonGreen.copy(alpha = 0.8f); 5 -> PurpleColor.copy(alpha = 0.8f); else -> Color(0xFF1A3828) }
                val textColor = when (runs) { 4, 5, 6 -> Color.White; 0 -> Color(0xFF6EE7B7); else -> Color(0xFFECFDF5) }
                Button(
                    onClick = { onRuns(runs) }, enabled = !isLoading,
                    modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = bgColor, contentColor = textColor, disabledContainerColor = bgColor.copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(0.dp)
                ) { Text(runs.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White) }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onWicket, enabled = !isLoading && !isFreeHit,
                modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFreeHit) Color(0xFF7F1D1D).copy(alpha = 0.3f) else Color(0xFF7F1D1D),
                    contentColor = Color(0xFFFCA5A5)
                )
            ) { Text(if (isFreeHit) "NO OUT" else "WICKET", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            Button(
                onClick = onExtras, enabled = !isLoading,
                modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF78350F), contentColor = Color(0xFFFCD34D))
            ) { Text("EXTRAS", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            Button(
                onClick = onUndo, enabled = !isLoading,
                modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard, contentColor = TextSecondary)
            ) { Text("UNDO", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onPenalty, enabled = !isLoading,
                modifier = Modifier.weight(1f).height(40.dp), shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberColor)
            ) { Text("+5 Penalty", fontSize = 11.sp) }
            OutlinedButton(
                onClick = onManualEdit, enabled = !isLoading,
                modifier = Modifier.weight(1f).height(40.dp), shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonBlue)
            ) { Text("Manual Edit", fontSize = 11.sp) }
        }
    }
}

// ── PLAYER SELECT DIALOG ──────────────────────────────────────────────────────

@Composable
fun PlayerSelectDialog(title: String, players: List<Player>, onPlayerSelected: (Player) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = SurfaceCard,
        title = { Text(title, color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.heightIn(max = 400.dp)) {
                items(players) { player ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(BackgroundDark).clickable { onPlayerSelected(player) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(NeonGreen.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Text(player.jerseyNo?.toString() ?: "-", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(player.fullName, color = TextPrimary, fontSize = 14.sp)
                            player.role?.let { Text(it.replace("_", " ").replaceFirstChar { c -> c.uppercase() }, color = TextSecondary, fontSize = 11.sp) }
                        }
                    }
                }
                if (players.isEmpty()) {
                    item { Text("No players available", color = TextSecondary, modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
    )
}

// ── WICKET DIALOG ─────────────────────────────────────────────────────────────

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun WicketDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int, String?, Boolean) -> Unit,
    onRetiredHurt: () -> Unit,
    keeperName: String? = null,
    bowlingTeamPlayers: List<com.crickethub.data.model.Player> = emptyList()
) {
    var selectedType by remember { mutableStateOf<String?>(null) }
    var runsBeforeWicket by remember { mutableStateOf(0) }
    var fielderName by remember { mutableStateOf("") }
    var nonStrikerOut by remember { mutableStateOf(false) }
    var dropExpanded by remember { mutableStateOf(false) }
    val showFielderInput = selectedType in listOf("caught", "run_out", "stumped")

    // Auto-fill keeper for stumped
    androidx.compose.runtime.LaunchedEffect(selectedType) {
        if (selectedType == "stumped" && keeperName != null) fielderName = keeperName
        else if (selectedType != "stumped") fielderName = ""
    }
    val wicketTypes = listOf(
        "bowled" to "Bowled", "caught" to "Caught", "lbw" to "LBW", "run_out" to "Run Out",
        "stumped" to "Stumped", "hit_wicket" to "Hit Wicket", "retired_out" to "Retired Out",
        "obstructing" to "Obstructing", "timed_out" to "Timed Out", "handled_ball" to "Handled Ball",
        "hit_ball_twice" to "Hit Ball Twice"
    )
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = SurfaceCard,
        title = { Text("Wicket!", color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 480.dp)) {
                item { Text("How was the batsman dismissed?", color = TextSecondary, fontSize = 13.sp) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        wicketTypes.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { (value, label) ->
                                    val selected = selectedType == value
                                    Box(
                                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                            .background(if (selected) ErrorRed.copy(alpha = 0.2f) else BackgroundDark)
                                            .border(1.dp, if (selected) ErrorRed else BorderColor, RoundedCornerShape(8.dp))
                                            .clickable { selectedType = value }.padding(10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(label, color = if (selected) ErrorRed else TextSecondary, fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center)
                                    }
                                }
                                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                if (selectedType == "run_out") {
                    item {
                        Text("Who got run out?", color = TextSecondary, fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { nonStrikerOut = false }, modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = if (!nonStrikerOut) ErrorRed else SurfaceCard)
                            ) { Text("Striker", color = if (!nonStrikerOut) Color.White else TextSecondary, fontSize = 12.sp) }
                            Button(onClick = { nonStrikerOut = true }, modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = if (nonStrikerOut) ErrorRed else SurfaceCard)
                            ) { Text("Non-Striker", color = if (nonStrikerOut) Color.White else TextSecondary, fontSize = 12.sp) }
                        }
                    }
                }
                if (showFielderInput) {
                    item {
                        if (selectedType == "stumped") {
                            OutlinedTextField(value = fielderName, onValueChange = {}, readOnly = true,
                                label = { Text("Stumped by (Keeper) ✓") }, singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = NeonGreen, unfocusedBorderColor = NeonGreen,
                                    focusedLabelColor = NeonGreen, unfocusedLabelColor = NeonGreen
                                )
                            )
                        } else if (bowlingTeamPlayers.isNotEmpty()) {
                            ExposedDropdownMenuBox(expanded = dropExpanded, onExpandedChange = { dropExpanded = it }) {
                                OutlinedTextField(
                                    value = fielderName, onValueChange = {}, readOnly = true,
                                    label = { Text(if (selectedType == "caught") "Caught by *" else "Run out by *") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    isError = fielderName.isBlank(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = ErrorRed, unfocusedBorderColor = BorderColor,
                                        focusedLabelColor = ErrorRed, unfocusedLabelColor = TextSecondary
                                    )
                                )
                                ExposedDropdownMenu(expanded = dropExpanded, onDismissRequest = { dropExpanded = false },
                                    modifier = Modifier.background(SurfaceCard)) {
                                    bowlingTeamPlayers.forEach { player ->
                                        DropdownMenuItem(
                                            text = { Text(player.fullName, color = TextPrimary) },
                                            onClick = { fielderName = player.fullName; dropExpanded = false }
                                        )
                                    }
                                }
                            }
                        } else {
                            OutlinedTextField(value = fielderName, onValueChange = { fielderName = it },
                                label = { Text(if (selectedType == "caught") "Caught by *" else "Run out by *") },
                                singleLine = true, isError = fielderName.isBlank(),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = ErrorRed, unfocusedBorderColor = BorderColor,
                                    focusedLabelColor = ErrorRed, unfocusedLabelColor = TextSecondary
                                )
                            )
                        }
                    }
                }
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(AmberColor.copy(alpha = 0.1f)).border(1.dp, AmberColor, RoundedCornerShape(8.dp))
                            .clickable { onRetiredHurt() }.padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("🤕 Retired Hurt (can bat again)", color = AmberColor, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                }
                item {
                    Text("Runs before wicket:", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (0..4).forEach { r ->
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape)
                                    .background(if (runsBeforeWicket == r) NeonGreen.copy(alpha = 0.3f) else BackgroundDark)
                                    .border(1.dp, if (runsBeforeWicket == r) NeonGreen else BorderColor, CircleShape)
                                    .clickable { runsBeforeWicket = r },
                                contentAlignment = Alignment.Center
                            ) { Text(r.toString(), color = if (runsBeforeWicket == r) NeonGreen else TextSecondary, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedType?.let { type ->
                    val needsFielder = type in listOf("caught", "run_out", "stumped")
                    if (!needsFielder || fielderName.isNotBlank()) {
                        onConfirm(type, runsBeforeWicket, fielderName.ifBlank { null }, nonStrikerOut)
                    }
                }},
                enabled = selectedType != null &&
                        (selectedType !in listOf("caught", "run_out", "stumped") || fielderName.isNotBlank()),
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
            ) { Text("Confirm", color = Color.White) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
    )
}

// ── EXTRAS DIALOG ─────────────────────────────────────────────────────────────

@Composable
fun ExtrasDialog(onDismiss: () -> Unit, onConfirm: (String, Int) -> Unit) {
    var selectedType by remember { mutableStateOf<String?>(null) }
    var runs by remember { mutableStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = SurfaceCard,
        title = { Text("Extras", color = AmberColor, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("wide" to "Wide", "no_ball" to "No Ball", "bye" to "Bye", "leg_bye" to "Leg Bye").forEach { (value, label) ->
                        val selected = selectedType == value
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                .background(if (selected) AmberColor.copy(alpha = 0.2f) else BackgroundDark)
                                .border(1.dp, if (selected) AmberColor else BorderColor, RoundedCornerShape(8.dp))
                                .clickable { selectedType = value; if (value == "no_ball" && runs == 5) runs = 4 }.padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (selected) AmberColor else TextSecondary, fontSize = 12.sp,
                                textAlign = TextAlign.Center, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                Text("Runs:", color = TextSecondary, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // No-ball: 0,1,2,3,4,6 (can't run 5, but can hit a six).
                    // Others: 0..6.
                    val runOptions = if (selectedType == "no_ball") listOf(0,1,2,3,4,6) else (0..6).toList()
                    runOptions.forEach { r ->
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                                .background(if (runs == r) AmberColor.copy(alpha = 0.3f) else BackgroundDark)
                                .border(1.dp, if (runs == r) AmberColor else BorderColor, CircleShape)
                                .clickable { runs = r },
                            contentAlignment = Alignment.Center
                        ) { Text(r.toString(), color = if (runs == r) AmberColor else TextSecondary, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { selectedType?.let { onConfirm(it, runs) } }, enabled = selectedType != null,
                colors = ButtonDefaults.buttonColors(containerColor = AmberColor)) { Text("Confirm", color = Color.Black) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
    )
}

// ── PENALTY DIALOG ────────────────────────────────────────────────────────────

@Composable
fun PenaltyRunsDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var selectedTeam by remember { mutableStateOf("batting") }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = SurfaceCard,
        title = { Text("+5 Penalty Runs", color = AmberColor, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Award 5 penalty runs to:", color = TextSecondary, fontSize = 13.sp)
                listOf("batting" to "Batting Team", "bowling" to "Bowling Team").forEach { (value, label) ->
                    val selected = selectedTeam == value
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(if (selected) AmberColor.copy(alpha = 0.15f) else BackgroundDark)
                            .border(1.dp, if (selected) AmberColor else BorderColor, RoundedCornerShape(8.dp))
                            .clickable { selectedTeam = value }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) { Text(label, color = if (selected) AmberColor else TextSecondary, fontSize = 14.sp) }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedTeam) }, colors = ButtonDefaults.buttonColors(containerColor = AmberColor)) {
                Text("Add Penalty", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
    )
}

// ── MANUAL EDIT DIALOG ────────────────────────────────────────────────────────

@Composable
fun ManualEditDialog(currentRuns: Int, currentWickets: Int, onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    var runs by remember { mutableStateOf(currentRuns.toString()) }
    var wickets by remember { mutableStateOf(currentWickets.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = SurfaceCard,
        title = { Text("Manual Score Edit", color = NeonBlue, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("⚠️ Use carefully — this directly edits the score", color = AmberColor, fontSize = 12.sp)
                OutlinedTextField(
                    value = runs, onValueChange = { if (it.all { c -> c.isDigit() }) runs = it },
                    label = { Text("Total Runs") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonBlue, unfocusedBorderColor = BorderColor,
                        cursorColor = NeonBlue, focusedLabelColor = NeonBlue, unfocusedLabelColor = TextSecondary
                    )
                )
                OutlinedTextField(
                    value = wickets, onValueChange = { if (it.all { c -> c.isDigit() } && (it.toIntOrNull() ?: 0) <= 10) wickets = it },
                    label = { Text("Wickets") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonBlue, unfocusedBorderColor = BorderColor,
                        cursorColor = NeonBlue, focusedLabelColor = NeonBlue, unfocusedLabelColor = TextSecondary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(runs.toIntOrNull() ?: currentRuns, wickets.toIntOrNull() ?: currentWickets) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
            ) { Text("Update Score", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
    )
}
// ── Ball popup label + colour, derived from the recorded ball ──────────────────
private fun ballPopupLabel(ball: com.crickethub.data.model.Ball): Pair<String, androidx.compose.ui.graphics.Color> {
    val green = androidx.compose.ui.graphics.Color(0xFF34D399)
    val blue = androidx.compose.ui.graphics.Color(0xFF3B82F6)
    val purple = androidx.compose.ui.graphics.Color(0xFF8B5CF6)
    val red = androidx.compose.ui.graphics.Color(0xFFEF4444)
    val amber = androidx.compose.ui.graphics.Color(0xFFF59E0B)
    val grey = androidx.compose.ui.graphics.Color(0xFF9CA3AF)

    // Wicket takes priority
    if (ball.isWicket) {
        val label = when (ball.wicketType) {
            "bowled" -> "BOWLED!"
            "caught" -> "CAUGHT!"
            "lbw" -> "LBW!"
            "run_out" -> "RUN OUT!"
            "stumped" -> "STUMPED!"
            "hit_wicket" -> "HIT WICKET!"
            "retired_hurt" -> "RETIRED HURT"
            "retired_out" -> "RETIRED OUT"
            "obstructing" -> "OBSTRUCTING!"
            "timed_out" -> "TIMED OUT!"
            "handled_ball" -> "HANDLED BALL!"
            "hit_ball_twice" -> "HIT TWICE!"
            else -> "OUT!"
        }
        return label to (if (ball.wicketType == "retired_hurt") amber else red)
    }

    // Extras
    when (ball.extrasType) {
        "wide" -> {
            // extrasRuns = 1 penalty + runs ran. Show ran runs only when > 0.
            val ran = (ball.extrasRuns ?: 1) - 1
            return (if (ran > 0) "WIDE +$ran" else "WIDE") to amber
        }
        "no_ball" -> {
            // runsOffBat = runs the batsman scored. Show only when > 0.
            val ran = ball.runsOffBat
            return (if (ran > 0) "NO BALL +$ran" else "NO BALL") to amber
        }
        "bye" -> return ("BYE +${ball.extrasRuns ?: 1}") to grey
        "leg_bye" -> return ("LEG BYE +${ball.extrasRuns ?: 1}") to grey
        "penalty" -> return ("PENALTY +${ball.extrasRuns ?: 5}") to amber
    }

    // Runs off bat
    return when (ball.runsOffBat) {
        0 -> "DOT" to grey
        4 -> "FOUR!" to blue
        6 -> "SIX!" to green
        else -> "+${ball.runsOffBat}" to green
    }
}

@Composable
private fun BallPopup(
    label: String?,
    color: androidx.compose.ui.graphics.Color,
    triggerKey: Int,
    modifier: Modifier = Modifier
) {
    if (label == null) return
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(triggerKey) {
        if (triggerKey == 0) return@LaunchedEffect
        visible = true
        kotlinx.coroutines.delay(850)
        visible = false
    }

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0.4f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.5f,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "popupScale"
    )
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(180),
        label = "popupAlpha"
    )

    if (alpha > 0.01f) {
        Box(
            modifier = modifier
                .graphicsLayer {
                    this.scaleX = scale; this.scaleY = scale; this.alpha = alpha
                }
                .clip(RoundedCornerShape(20.dp))
                .background(color.copy(alpha = 0.18f))
                .border(2.dp, color, RoundedCornerShape(20.dp))
                .padding(horizontal = 32.dp, vertical = 18.dp)
        ) {
            Text(
                text = label,
                color = color,
                fontSize = 40.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}